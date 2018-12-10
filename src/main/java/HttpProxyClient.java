import connect.network.nio.NioClientFactory;
import connect.network.nio.NioClientTask;
import connect.network.nio.NioReceive;
import connect.network.nio.NioSender;
import util.LogDog;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class HttpProxyClient extends NioClientTask {
    private NioSender sender;

    public HttpProxyClient() {
        setReceive(new NioReceive(this, "onReceive"));
        sender = new HttpSender(this);
        setSender(sender);
    }

    private void onReceive(byte[] data) {
        if (data.length == 0) {
            NioClientFactory.getFactory().removeTask(this);
        }
        ProxyConnectClient connectClient = new ProxyConnectClient(data, sender);
        NioClientFactory.getFactory().addTask(connectClient);
    }



    @Override
    protected void onConnectSocketChannel(boolean isConnect) {
        if (isConnect) {
            SocketChannel channel = getSocketChannel();
            try {
                InetSocketAddress address = (InetSocketAddress) channel.getRemoteAddress();
                LogDog.d("==> connect client address = " + address.getHostString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onCloseSocketChannel() {
        LogDog.e("==> HttpProxyClient close !!! ");
    }
}
