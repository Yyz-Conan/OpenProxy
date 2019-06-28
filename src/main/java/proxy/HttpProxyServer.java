package proxy;

import connect.network.nio.NioClientFactory;
import connect.network.nio.NioSSLFactory;
import connect.network.nio.NioServerTask;
import util.LogDog;

import java.nio.channels.SocketChannel;

public class HttpProxyServer extends NioServerTask {

    @Override
    protected void onOpenServerChannel(boolean isSuccess) {
        if (isSuccess) {
            LogDog.d("==> proxy.HttpProxyServer start success !!! ");
//            InputStream inputStream = ProxyMain.class.getClassLoader().getResourceAsStream("ssl_ks");
//            NioClientFactory.getFactory().setSslFactory(new test.TestSSLFactory("SSL", inputStream));
//            try {
//                inputStream.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            NioClientFactory.getFactory().setSSlFactory(new NioSSLFactory("SSL"));
            NioClientFactory.getFactory().open();
        }
    }

    @Override
    protected void onAcceptServerChannel(SocketChannel channel) {
        HttpProxyClient client = new HttpProxyClient(channel);
        NioClientFactory.getFactory().addTask(client);
    }

    @Override
    protected void onCloseServerChannel() {
        LogDog.e("==> proxy.HttpProxyServer close ing... !!! ");
    }
}
