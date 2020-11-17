package connect;

import connect.joggle.ICloseListener;
import connect.network.base.joggle.INetSender;
import connect.network.base.joggle.ISenderFeedback;
import connect.network.nio.NioClientFactory;
import connect.network.nio.NioClientTask;
import connect.network.xhttp.XMultiplexCacheManger;
import connect.network.xhttp.utils.MultilevelBuf;

public class AbsClient extends NioClientTask implements ISenderFeedback {

    protected ICloseListener listener;
    protected boolean isCanProxy = false;

    public void setCanProxy(boolean canProxy) {
        isCanProxy = canProxy;
    }

    public void setOnCloseListener(ICloseListener listener) {
        this.listener = listener;
    }


    @Override
    protected void onCloseClientChannel() {
        if (listener != null) {
            listener.onClose(getHost());
        }
    }

    @Override
    public void onSenderFeedBack(INetSender iNetSender, Object data, Throwable throwable) {
        if (throwable != null) {
            NioClientFactory.getFactory().removeTask(this);
        }
        if (data instanceof MultilevelBuf) {
            XMultiplexCacheManger.getInstance().lose((MultilevelBuf) data);
        }
    }
}
