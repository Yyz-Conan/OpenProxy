package connect;

import connect.joggle.IDecryptionDataListener;
import connect.network.base.joggle.INetReceiver;
import connect.network.base.joggle.INetSender;
import connect.network.nio.NioReceiver;
import connect.network.xhttp.entity.XReceiverMode;
import connect.network.xhttp.entity.XReceiverStatus;
import connect.network.xhttp.entity.XResponse;
import connect.network.xhttp.utils.XHttpDecoderProcessor;
import protocol.DataPacketTag;
import utils.RequestHelper;

/**
 * HttpProxy解密接收者
 */
public class HttpDecryptionReceiver implements IDecryptionDataListener {

    private INetSender localSender;
    private INetSender remoteSender;
    private boolean isTLS = false;
    private boolean isFirstRequest = true;

    private CoreHttpDecoderProcessor httpDecoder;
    private DecryptionReceiver decryptionReceiver;


    public HttpDecryptionReceiver(boolean isEnableDecryption) {
        httpDecoder = new CoreHttpDecoderProcessor();
        decryptionReceiver = new DecryptionReceiver(isEnableDecryption);
        decryptionReceiver.setDecodeTag(DataPacketTag.PACK_PROXY_TAG);
        decryptionReceiver.setDataReceiver(httpDecoder);
        decryptionReceiver.setOnDecryptionDataListener(this);
    }

    public NioReceiver getReceiver() {
        return decryptionReceiver;
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

    @Override
    public void onDecryption(byte[] decrypt) {
        if (httpDecoder.getMode() == XReceiverMode.REQUEST) {
            httpDecoder.onHttpReceive(decrypt, decrypt.length, null);
        } else {
            localSender.sendData(decrypt);
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
