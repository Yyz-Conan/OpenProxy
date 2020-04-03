package connect.clinet;

import connect.network.nio.NioHPCClientFactory;
import connect.network.nio.NioServerTask;
import connect.server.HttpProxyClient;
import log.LogDog;
import util.StringEnvoy;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

public class LocalProxyServer extends NioServerTask {

    public static volatile AtomicInteger localConnectCount = new AtomicInteger(0);

    private String remoteHost;
    private int remotePort;

    public LocalProxyServer(String localHost, int localPort, String remoteHost, int remotePort) {
        if (StringEnvoy.isEmpty(localHost) || localPort < 0 || StringEnvoy.isEmpty(remoteHost) || remotePort < 0) {
            throw new IllegalArgumentException("localHost，localPort，remoteHost or remotePort is Illegal");
        }
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
        setAddress(localHost, localPort, false);
    }

    @Override
    protected void onBootServerComplete(ServerSocketChannel channel) {
        LogDog.d("==> HttpProxy Server address = " + getServerHost() + ":" + getServerPort());
        LogDog.d("==> Remote Server address = " + remoteHost + ":" + remotePort);
        NioHPCClientFactory.getFactory(1).open();
    }

    @Override
    protected void onAcceptServerChannel(SocketChannel channel) {
        HttpProxyClient localProxyClient = new HttpProxyClient(channel);
        localProxyClient.setRemoteServer(remoteHost, remotePort);
        NioHPCClientFactory.getFactory().addTask(localProxyClient);
        localConnectCount.incrementAndGet();
    }

    @Override
    protected void onCloseServerChannel() {
        LogDog.e("==> Local Proxy Server close ing... !!! ");
        NioHPCClientFactory.destroy();
    }
}
