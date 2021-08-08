package connect.socks5.server;

import connect.network.nio.NioServerTask;
import connect.socks5.Socks5NetFactory;
import connect.socks5.client.Socks5InteractiveClient;
import log.LogDog;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * socks5服务
 */
public class Socks5Server extends NioServerTask {

    public static volatile AtomicInteger localConnectCount = new AtomicInteger(0);

    @Override
    protected void onBootServerComplete(ServerSocketChannel channel) {
        LogDog.d("==> Socks5 proxy server start success : " + getHost() + ":" + getPort());
        Socks5NetFactory.getFactory().open();
    }

    @Override
    protected void onAcceptServerChannel(SocketChannel channel) {
        Socks5InteractiveClient client = new Socks5InteractiveClient(channel);
        Socks5NetFactory.getFactory().addTask(client);
    }

    @Override
    protected void onCloseServerChannel() {
        LogDog.e("==> Socks5 proxy server close ing = " + getHost() + ":" + getPort());
        Socks5NetFactory.getFactory().close();
    }

}
