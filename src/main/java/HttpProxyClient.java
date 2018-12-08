import connect.network.nio.NioClientTask;
import util.LogDog;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class HttpProxyClient extends NioClientTask {

    @Override
    protected void onConnectSocketChannel(boolean isConnect) {
        if (isConnect) {
            SocketChannel channel = getSocketChannel();
            LogDog.d("==> HttpProxyClient has client connect success !!! ");
            try {
                InetSocketAddress address = (InetSocketAddress) channel.getRemoteAddress();
                LogDog.d("==>  client address = " + address.getHostName());
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
