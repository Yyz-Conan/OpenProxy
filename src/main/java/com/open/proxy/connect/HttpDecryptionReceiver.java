package com.open.proxy.connect;

import com.currency.net.base.joggle.INetReceiver;
import com.currency.net.base.joggle.INetSender;
import com.currency.net.entity.MultiByteBuffer;
import com.currency.net.nio.NioReceiver;
import com.currency.net.xhttp.entity.XHttpDecoderStatus;
import com.currency.net.xhttp.entity.XReceiverMode;
import com.currency.net.xhttp.entity.XResponse;
import com.currency.net.xhttp.utils.XHttpDecoderProcessor;
import com.open.proxy.connect.joggle.IDecryptionDataListener;
import com.open.proxy.protocol.DataPacketTag;
import com.open.proxy.utils.RequestHelper;

/**
 * HttpProxy解密接收者
 */
public class HttpDecryptionReceiver implements IDecryptionDataListener {

    private INetSender localSender;
    private INetSender remoteSender;

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
        //回调步骤1
        if (httpDecoder.getMode() == XReceiverMode.REQUEST) {
            httpDecoder.onRequest(decrypt, decrypt.length);
        } else {
            remoteSender.sendData(decrypt);
        }
    }


    private class CoreHttpDecoderProcessor extends XHttpDecoderProcessor implements INetReceiver<MultiByteBuffer> {

        private INetReceiver<XResponse> receiver;

        public void setDataReceiver(INetReceiver<XResponse> receiver) {
            this.receiver = receiver;
        }

        @Override
        protected void onStatusChange(XHttpDecoderStatus status) {
            //回调步骤3
            if (status == XHttpDecoderStatus.OVER) {
                if (receiver != null) {
                    receiver.onReceiveFullData(getResponse(), null);
                }
            }
        }


        @Override
        protected void onRequest(byte[] data, int len) {
            //回调步骤2
            if (data != null) {
                if (RequestHelper.isRequest(data)) {
                    //当前状态是http的request请求体
                    super.onRequest(data, len);
                } else {
                    //当前状态非http的request请求体
                    remoteSender.sendData(data);
                }
            }
        }

        @Override
        public void onReceiveFullData(MultiByteBuffer buf, Throwable throwable) {
            byte[] decrypt = buf.array();
            if (decrypt != null) {
                onDecryption(decrypt);
            }
            getReceiver().reuseBuf(buf);
        }
    }

}
