package com.open.proxy.server.socks5.server;


import com.jav.common.log.LogDog;
import com.jav.net.security.channel.base.AbsSecurityServer;
import com.open.proxy.server.socks5.Socks5NetFactory;
import com.open.proxy.server.socks5.client.Socks5InteractiveClient;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * socks5服务
 */
public class Socks5Server extends AbsSecurityServer {

    public static volatile AtomicInteger sLocalConnectCount = new AtomicInteger(0);

    @Override
    protected void onBeReadyChannel(SelectionKey selectionKey, ServerSocketChannel channel) {
        LogDog.d("==> Socks5 proxy server start success : " + getHost() + ":" + getPort());
        try {
            Socks5NetFactory.getFactory().open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPassChannel(SocketChannel socketChannel) {
        Socks5InteractiveClient client = new Socks5InteractiveClient(socketChannel);
        Socks5NetFactory.getFactory().getNetTaskComponent().addExecTask(client);
    }

    @Override
    protected void onCloseChannel() {
        LogDog.e("==> Socks5 proxy server close ing = " + getHost() + ":" + getPort());
        Socks5NetFactory.getFactory().close();
    }

}
