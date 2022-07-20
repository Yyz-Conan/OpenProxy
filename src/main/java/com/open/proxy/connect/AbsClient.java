package com.open.proxy.connect;


import com.jav.net.base.joggle.INetSender;
import com.jav.net.base.joggle.ISenderFeedback;
import com.jav.net.nio.NioClientFactory;
import com.jav.net.nio.NioClientTask;
import com.jav.net.ssl.TLSHandler;
import com.open.proxy.connect.joggle.IRemoteClientCloseListener;

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
    protected void onCloseChannel() {
        if (mCloseListener != null) {
            mCloseListener.onClientClose(getHost());
        }
    }

    @Override
    public void onSenderFeedBack(INetSender iNetSender, Object data, Throwable throwable) {
        if (throwable != null) {
            NioClientFactory.getFactory().getNetTaskContainer().addUnExecTask(this);
        }
    }
}
