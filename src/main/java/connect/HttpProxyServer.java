package connect;

import connect.network.nio.NioHPCClientFactory;
import connect.network.nio.NioServerTask;
import log.LogDog;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

public class HttpProxyServer extends NioServerTask {

    public static volatile AtomicInteger localConnectCount = new AtomicInteger(0);

    @Override
    protected void onConfigServer(boolean isSuccess, ServerSocketChannel channel) {
        if (isSuccess) {
            LogDog.d("==> Proxy Server Start Success !!! ");
            NioHPCClientFactory.getFactory(1).open();
        }
    }

    @Override
    protected void onAcceptServerChannel(SocketChannel channel) {
        HttpProxyClient client = new HttpProxyClient(channel);
        NioHPCClientFactory.getFactory().addTask(client);
        HttpProxyServer.localConnectCount.incrementAndGet();
//        LogDog.d("---------- add() Connect Count = " + HttpProxyServer.localConnectCount.incrementAndGet() + " obj = " + client.toString());
    }

    @Override
    protected void onCloseServerChannel() {
        LogDog.e("==> Proxy Server close ing... !!! ");
    }
}
