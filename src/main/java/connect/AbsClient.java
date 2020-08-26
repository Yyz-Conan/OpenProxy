package connect;

import connect.joggle.ICloseListener;
import connect.network.base.joggle.INetSender;
import connect.network.base.joggle.ISenderFeedback;
import connect.network.nio.NioClientFactory;
import connect.network.nio.NioClientTask;

import java.nio.ByteBuffer;

public class AbsClient extends NioClientTask implements ISenderFeedback {

    protected ICloseListener listener;
//    protected byte[] data;
//    protected XResponse response;
    protected boolean isCanProxy = false;

    public void setCanProxy(boolean canProxy) {
        isCanProxy = canProxy;
    }

    public void setOnCloseListener(ICloseListener listener) {
        this.listener = listener;
    }

//    public void setData(byte[] data) {
//        this.data = data;
//    }

//    public void setResponse(XResponse response) {
//        this.response = response;
//    }

    @Override
    protected void onCloseClientChannel() {
        if (listener != null) {
            listener.onClose(getHost());
        }
    }

    @Override
    public void onSenderFeedBack(INetSender iNetSender, ByteBuffer byteBuffer, Throwable throwable) {
        if (throwable != null) {
            NioClientFactory.getFactory().removeTask(this);
        }
    }
}
