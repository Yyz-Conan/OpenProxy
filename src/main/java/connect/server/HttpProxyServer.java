package connect.server;

import connect.HttpProxyClient;
import connect.network.nio.NioClientFactory;
import connect.network.nio.NioServerTask;
import log.LogDog;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

public class HttpProxyServer extends NioServerTask {

    public static volatile AtomicInteger localConnectCount = new AtomicInteger(0);

    @Override
    protected void onBootServerComplete(ServerSocketChannel channel) {
        LogDog.d("==> Proxy Server Start Success !!! ");
        NioClientFactory.getFactory().open();
    }

    @Override
    protected void onAcceptServerChannel(SocketChannel channel) {
        HttpProxyClient client = new HttpProxyClient(channel);
        NioClientFactory.getFactory().addTask(client);
        localConnectCount.incrementAndGet();
//        LogDog.d("---------- add() Connect Count = " + HttpProxyServer.localConnectCount.incrementAndGet() + " obj = " + client.toString());
    }

    @Override
    protected void onCloseServerChannel() {
        LogDog.e("==> Proxy Server close ing... !!! ");
        NioClientFactory.destroy();
    }
}
