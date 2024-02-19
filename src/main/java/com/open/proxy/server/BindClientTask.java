package com.open.proxy.server;


import com.jav.net.nio.NioClientTask;
import com.open.proxy.server.joggle.IBindClientListener;

import java.nio.channels.SocketChannel;

/**
 * 可绑定的client
 *
 * @author yyz
 */
public class BindClientTask extends NioClientTask {

    protected IBindClientListener mBindListener;

    public BindClientTask() {
    }

    public BindClientTask(SocketChannel channel) {
        super(channel);
    }

    public void setBindClientListener(IBindClientListener listener) {
        this.mBindListener = listener;
    }


    @Override
    protected void onRecovery() {
        super.onRecovery();
        mBindListener = null;
    }
}
