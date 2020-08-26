package connect.server;

import connect.client.ReceptionProxyClient;
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
        LogDog.d("==> Proxy Server Start Success : " + getServerHost() + ":" + getServerPort());
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
        LogDog.e("==> Proxy Server close ing = " + getServerHost() + ":" + getServerPort());
        NioClientFactory.destroy();
    }
}
