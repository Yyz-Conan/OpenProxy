package connect;

import connect.network.base.joggle.INetReceiver;
import connect.network.base.joggle.INetSender;
import connect.network.xhttp.XHttpReceiver;
import connect.network.xhttp.entity.XReceiverMode;
import connect.network.xhttp.entity.XReceiverStatus;
import connect.network.xhttp.entity.XResponse;
import cryption.DecryptionStatus;
import cryption.joggle.IDecryptTransform;
import util.IoEnvoy;
import util.TypeConversion;
import utils.RequestHelper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * 解密接收者
 */
public class DecryptionReceiver extends XHttpReceiver {

    private IDecryptTransform decryptTransform;
    private INetSender localSender;
    private ByteBuffer tag;
    private ByteBuffer packetData;
    private DecryptionStatus decryptionStatus;

    private INetSender remoteSender;
    private boolean isTLS = false;
    private boolean isFirstRequest = true;

    public DecryptionReceiver(IDecryptTransform decryptTransform) {
        super(null);
        if (decryptTransform != null) {
            this.decryptTransform = decryptTransform;
            tag = ByteBuffer.allocate(4);
            decryptionStatus = DecryptionStatus.TAG;
        }
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
    @Override
    public void setReceiver(INetReceiver<XResponse> receiver) {
        super.setReceiver(receiver);
        setMode(XReceiverMode.REQUEST);
    }

    /**
     * 工作于客户端模式，设置发送者接收tls数据
     *
     * @param localTarget
     */
    public void setResponseSender(INetSender localTarget) {
        this.localSender = localTarget;
        setMode(XReceiverMode.RESPONSE);
    }

    @Override
    protected void onStatusChange(XReceiverStatus status) {
        if (status == XReceiverStatus.NONE) {
            //当前是循环完整个流程
            reset();
            isFirstRequest = false;
        }
    }

    @Override
    protected void onRead(SocketChannel channel) throws Exception {
        if (decryptTransform != null) {
            if (decryptionStatus == DecryptionStatus.TAG) {
                tag.clear();
                int ret = IoEnvoy.readToFull(channel, tag);
                if (ret == IoEnvoy.SUCCESS) {
                    int packetSize = TypeConversion.byteToInt(tag.array(), 0);
                    if (packetSize > 0) {
                        packetData = ByteBuffer.allocate(packetSize);
                        decryptionStatus = DecryptionStatus.DATA;
                    }
                } else if (ret == IoEnvoy.FAIL) {
                    int packetSize = packetData == null ? -1 : packetData.array().length;
                    throw new IOException("## decryption read tag fail , packetSize = " + packetSize);
                }
            }
            if (decryptionStatus == DecryptionStatus.DATA) {
                int ret = IoEnvoy.readToFull(channel, packetData);
                if (ret == IoEnvoy.SUCCESS) {
                    byte[] decrypt = decryptTransform.onDecrypt(packetData.array());
                    if (getMode() == XReceiverMode.REQUEST) {
                        super.onHttpReceive(decrypt, decrypt.length);
                    } else {
//                        SimpleSendTask.getInstance().sendData(localTarget, decrypt);
                        localSender.sendData(decrypt);
                    }
                    packetData = null;
                    decryptionStatus = DecryptionStatus.END;
                } else if (ret == IoEnvoy.FAIL) {
                    throw new IOException("## decryption read body fail !!!");
                }
            }
            if (decryptionStatus == DecryptionStatus.END) {
                tag.clear();
                int ret = IoEnvoy.readToFull(channel, tag);
                if (ret == IoEnvoy.SUCCESS) {
                    decryptionStatus = DecryptionStatus.TAG;
                } else if (ret == IoEnvoy.FAIL) {
                    throw new IOException("decryption read end tag fail !!!");
                }
            }
        } else {
            super.onRead(channel);
        }
    }

    @Override
    protected void onRequest(byte[] data, int len) {
        if (data != null) {
            if (isFirstRequest || !isTLS || RequestHelper.isRequest(data)) {
                //当前状态是第一次接收到数据或者非https请求
                super.onRequest(data, len);
            } else {
                //当前状态是https请求或者走代理请求
                remoteSender.sendData(data);
            }
        }
    }

}
