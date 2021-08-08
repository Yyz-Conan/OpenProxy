package connect;

import connect.joggle.IRemoteClientCloseListener;
import connect.network.base.joggle.INetSender;
import connect.network.base.joggle.ISenderFeedback;
import connect.network.nio.NioClientFactory;
import connect.network.nio.NioClientTask;
import connect.network.ssl.TLSHandler;
import connect.network.xhttp.XMultiplexCacheManger;
import connect.network.xhttp.utils.MultiLevelBuf;

import java.nio.channels.SocketChannel;

public class AbsClient extends NioClientTask implements ISenderFeedback {

    protected IRemoteClientCloseListener mCloseListener;

    public void setOnCloseListener(IRemoteClientCloseListener listener) {
        this.mCloseListener = listener;
    }

    public AbsClient() {
    }

    public AbsClient(SocketChannel channel, TLSHandler tlsHandler) {
        super(channel, tlsHandler);
    }

    @Override
    protected void onCloseClientChannel() {
        if (mCloseListener != null) {
            mCloseListener.onClientClose(getHost());
        }
    }

    @Override
    public void onSenderFeedBack(INetSender iNetSender, Object data, Throwable throwable) {
        if (throwable != null) {
            NioClientFactory.getFactory().removeTask(this);
        }
        if (data instanceof MultiLevelBuf) {
            XMultiplexCacheManger.getInstance().lose((MultiLevelBuf) data);
        }
    }
}
