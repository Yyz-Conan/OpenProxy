package com.open.proxy.connect.socks5.server;


import com.jav.common.log.LogDog;
import com.jav.net.nio.NioServerTask;
import com.open.proxy.connect.socks5.Socks5NetFactory;
import com.open.proxy.connect.socks5.client.Socks5InteractiveClient;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * socks5服务
 */
public class Socks5Server extends NioServerTask {

    public static volatile AtomicInteger localConnectCount = new AtomicInteger(0);

    @Override
    protected void onBeReadyChannel(ServerSocketChannel channel) {
        LogDog.d("==> Socks5 proxy server start success : " + getHost() + ":" + getPort());
        Socks5NetFactory.getFactory().open();
    }

    @Override
    protected void onAcceptServerChannel(SocketChannel channel) {
        Socks5InteractiveClient client = new Socks5InteractiveClient(channel);
        Socks5NetFactory.getFactory().getNetTaskComponent().addExecTask(client);
    }

    @Override
    protected void onCloseChannel() {
        LogDog.e("==> Socks5 proxy server close ing = " + getHost() + ":" + getPort());
        Socks5NetFactory.getFactory().close();
    }

}
