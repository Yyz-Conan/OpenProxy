package connect.http.server;

import connect.http.client.ReceptionProxyClient;
import connect.network.nio.NioClientFactory;
import connect.network.nio.NioServerTask;
import log.LogDog;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

public class MultipleProxyServer extends NioServerTask {

    public static volatile AtomicInteger localConnectCount = new AtomicInteger(0);

    @Override
    protected void onBootServerComplete(ServerSocketChannel channel) {
        LogDog.d("==> Proxy server start success : " + getHost() + ":" + getPort());
        NioClientFactory.getFactory().open();
    }

    @Override
    protected void onAcceptServerChannel(SocketChannel channel) {
        ReceptionProxyClient client = new ReceptionProxyClient(channel);
        NioClientFactory.getFactory().addTask(client);
        localConnectCount.incrementAndGet();
    }

    @Override
    protected void onCloseServerChannel() {
        LogDog.e("==> Proxy server close ing = " + getHost() + ":" + getPort());
        NioClientFactory.destroy();
    }
}
