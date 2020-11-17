package connect;

import connect.network.base.SocketChannelCloseException;
import connect.network.base.joggle.INetReceiver;
import connect.network.base.joggle.INetSender;
import connect.network.nio.NioReceiver;
import connect.network.xhttp.entity.XReceiverMode;
import connect.network.xhttp.entity.XReceiverStatus;
import connect.network.xhttp.entity.XResponse;
import connect.network.xhttp.utils.XHttpDecoderProcessor;
import cryption.joggle.IDecryptTransform;
import util.IoEnvoy;
import util.TypeConversion;
import utils.DataPacketManger;
import utils.RequestHelper;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

/**
 * 解密接收者
 */
public class DecryptionReceiver {

    private IDecryptTransform decryptTransform;
    private INetSender localSender;
    private ByteBuffer packetHead;
    private ByteBuffer packetData;
    private DecryptionStatus decryptionStatus;

    private INetSender remoteSender;
    private boolean isTLS = false;
    private boolean isFirstRequest = true;

    private CoreHttpDecoderProcessor httpDecoder;
    private CoreHttpReceiver httpReceiver;

    public DecryptionReceiver(IDecryptTransform decryptTransform) {
        if (decryptTransform != null) {
            this.decryptTransform = decryptTransform;
            packetHead = ByteBuffer.allocate(4);
            decryptionStatus = DecryptionStatus.TAG;
        }
        httpDecoder = new CoreHttpDecoderProcessor();
        httpReceiver = new CoreHttpReceiver();
        httpReceiver.setDataReceiver(httpDecoder);
    }

    public CoreHttpReceiver getHttpReceiver() {
        return httpReceiver;
    }

    /**
     * 工作于服务端模式，设置发送者接收tls数据
     *
     * @param remoteSender
     */
    public void setRequestSender(INetSender remoteSender) {
        this.remoteSender = remoteSender;
    }

    public void setTLS() {
        isTLS = true;
    }

    /**
     * 工作于服务端模式才配置该方法
     *
     * @param receiver
     */
    public void setDataReceiver(INetReceiver<XResponse> receiver) {
        httpDecoder.setDataReceiver(receiver);
        httpDecoder.setMode(XReceiverMode.REQUEST);
    }

    /**
     * 工作于客户端模式，设置发送者接收tls数据
     *
     * @param localTarget
     */
    public void setResponseSender(INetSender localTarget) {
        this.localSender = localTarget;
        httpDecoder.setMode(XReceiverMode.RESPONSE);
    }

    public enum DecryptionStatus {
        TAG, SIZE, DATA
    }

    private class CoreHttpReceiver extends NioReceiver {

        @Override
        protected void onRead(SocketChannel channel) throws Throwable {
            if (decryptTransform != null) {
                if (decryptionStatus == DecryptionStatus.TAG) {
                    packetHead.clear();
                    int ret = IoEnvoy.readToFull(channel, packetHead);
                    if (ret == IoEnvoy.SUCCESS) {
                        byte[] tag = packetHead.array();
                        boolean isSame = Arrays.equals(tag, DataPacketManger.PACK_PROXY_TAG);
                        if (isSame) {
                            decryptionStatus = DecryptionStatus.SIZE;
                        } else {
                            throw new SocketChannelCloseException();
                        }
                    } else if (ret == IoEnvoy.FAIL) {
                        throw new SocketChannelCloseException();
                    }
                }
                if (decryptionStatus == DecryptionStatus.SIZE) {
                    packetHead.clear();
                    int ret = IoEnvoy.readToFull(channel, packetHead);
                    if (ret == IoEnvoy.SUCCESS) {
                        int packetSize = TypeConversion.byteToInt(packetHead.array(), 0);
                        if (packetSize > 0) {
                            packetData = ByteBuffer.allocate(packetSize);
                            decryptionStatus = DecryptionStatus.DATA;
                        } else {
                            throw new SocketChannelCloseException();
                        }
                    } else if (ret == IoEnvoy.FAIL) {
                        throw new SocketChannelCloseException();
                    }
                }
                if (decryptionStatus == DecryptionStatus.DATA) {
                    int ret = IoEnvoy.readToFull(channel, packetData);
                    if (ret == IoEnvoy.SUCCESS) {
                        byte[] decrypt = decryptTransform.onDecrypt(packetData.array());
                        if (httpDecoder.getMode() == XReceiverMode.REQUEST) {
                            httpDecoder.onHttpReceive(decrypt, decrypt.length, null);
                        } else {
                            localSender.sendData(decrypt);
                        }
                        packetData = null;
                        decryptionStatus = DecryptionStatus.TAG;
                    } else if (ret == IoEnvoy.FAIL) {
                        throw new SocketChannelCloseException();
                    }
                }
            } else {
                super.onRead(channel);
            }
        }
    }

    private class CoreHttpDecoderProcessor extends XHttpDecoderProcessor {

        @Override
        protected void onStatusChange(XReceiverStatus status) {
            if (status == XReceiverStatus.NONE) {
                //当前是循环完整个流程
                isFirstRequest = false;
            }
        }


        @Override
        protected void onRequest(byte[] data, int len, Throwable e) {
            if (data != null) {
                if (isFirstRequest || !isTLS || RequestHelper.isRequest(data)) {
                    //当前状态是第一次接收到数据或者非https请求
                    super.onRequest(data, len, e);
                } else {
                    //当前状态是https请求或者走代理请求
                    remoteSender.sendData(data);
                }
            }
        }

        @Override
        protected void onHttpReceive(byte[] data, int len, Throwable e) {
            super.onHttpReceive(data, len, e);
        }
    }

}
