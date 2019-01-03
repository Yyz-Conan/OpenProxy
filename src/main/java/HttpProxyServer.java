import connect.network.nio.NioClientFactory;
import connect.network.nio.NioSSLFactory;
import connect.network.nio.NioServerTask;
import util.LogDog;

import java.nio.channels.SocketChannel;

public class HttpProxyServer extends NioServerTask {

    @Override
    protected void onOpenServerChannel(boolean isSuccess) {
        if (isSuccess) {
            LogDog.d("==> HttpProxyServer start success !!! ");
            NioClientFactory.getFactory().open();
//            InputStream inputStream = Proxy.class.getClassLoader().getResourceAsStream("ssl_ks");
//            NioClientFactory.getFactory().setSslFactory(new TestSSLFactory("SSL", inputStream));
            NioClientFactory.getFactory().setSSlFactory(new NioSSLFactory("SSL"));
//            try {
//                inputStream.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }
    }

    @Override
    protected void onAcceptServerChannel(SocketChannel channel) {
        HttpProxyClient client = new HttpProxyClient(channel);
        NioClientFactory.getFactory().addTask(client);
    }

    @Override
    protected void onCloseServerChannel() {
        LogDog.e("==> HttpProxyServer close ing... !!! ");
    }
}
