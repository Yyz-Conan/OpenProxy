package com.open.proxy.connect;

import com.jav.net.base.joggle.INetReceiver;
import com.jav.net.base.joggle.INetSender;
import com.jav.net.entity.MultiByteBuffer;
import com.jav.net.nio.NioReceiver;
import com.jav.net.xhttp.entity.XHttpDecoderStatus;
import com.jav.net.xhttp.entity.XReceiverMode;
import com.jav.net.xhttp.entity.XResponse;
import com.jav.net.xhttp.utils.XHttpDecoderProcessor;
import com.open.proxy.connect.http.SecurityReceiverProcess;
import com.open.proxy.connect.joggle.IDecryptionDataListener;
import com.open.proxy.utils.RequestHelper;

/**
 * HttpProxy解密接收者
 */
public class HttpDecryptionReceiver implements IDecryptionDataListener {

    private INetSender localSender;
    private INetSender remoteSender;

    private CoreHttpDecoderProcessor mHttpDecoder;
    private SecurityReceiverProcess mSecurityReceiverProcess;
    private SecurityReceiver mDecryptionReceiver;


    public HttpDecryptionReceiver(boolean isEnableDecryption) {
        mHttpDecoder = new CoreHttpDecoderProcessor();
        mDecryptionReceiver = new SecurityReceiver();
        mSecurityReceiverProcess = new SecurityReceiverProcess();
        mSecurityReceiverProcess.setDecryptionDataListener(this);
        mDecryptionReceiver.setProcessListener(mSecurityReceiverProcess);
    }

    public NioReceiver getReceiver() {
        return mDecryptionReceiver;
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
        mHttpDecoder.setDataReceiver(receiver);
        mHttpDecoder.setMode(XReceiverMode.REQUEST);
        mSecurityReceiverProcess.setReceiverMode(XReceiverMode.REQUEST);
    }

    /**
     * 工作于客户端模式，设置发送者接收tls数据
     *
     * @param localTarget
     */
    public void setResponseSender(INetSender localTarget) {
        this.localSender = localTarget;
        mHttpDecoder.setMode(XReceiverMode.RESPONSE);
        mSecurityReceiverProcess.setReceiverMode(XReceiverMode.RESPONSE);
    }

    @Override
    public void onDecryption(byte[] decrypt) {
        //回调步骤1
        if (mHttpDecoder.getMode() == XReceiverMode.REQUEST) {
            mHttpDecoder.onRequest(decrypt, decrypt.length);
        } else {
            remoteSender.sendData(new MultiByteBuffer(decrypt));
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
                    remoteSender.sendData(new MultiByteBuffer(data));
                }
            }
        }

        @Override
        public void onReceiveFullData(MultiByteBuffer buf, Throwable throwable) {
            byte[] decrypt = buf.array();
            if (decrypt != null) {
                onDecryption(decrypt);
            }
            getReceiver().getBufferComponent().reuseBuffer(buf);
        }
    }

}
