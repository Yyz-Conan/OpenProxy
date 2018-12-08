import connect.network.nio.NioClientFactory;
import connect.network.nio.NioServerTask;
import util.LogDog;

import java.nio.channels.SocketChannel;

public class HttpProxyServer extends NioServerTask {

    @Override
    protected void onOpenServerChannel(boolean isSuccess) {
        if (isSuccess) {
            LogDog.d("==> HttpProxyServer start success !!! ");
            NioClientFactory.getFactory().open();
        }
    }

    @Override
    protected void onAcceptServerChannel(SocketChannel channel) {
        LogDog.d("==> HttpProxyServer has client connect ing... !!! ");
        HttpProxyClient client = new HttpProxyClient();
        client.setChannel(channel);
        NioClientFactory.getFactory().addTask(client);
    }

    @Override
    protected void onCloseServerChannel() {
        LogDog.e("==> HttpProxyServer close ing... !!! ");
    }
}
