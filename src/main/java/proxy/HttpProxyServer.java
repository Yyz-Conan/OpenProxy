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
//            HttpProxyServer.class.getClassLoader().getResource("cacerts").getPath();
//            InputStream inputStream = ProxyMain.class.getClassLoader().getResourceAsStream("ssl_ks");
//            NioClientFactory.getFactory().setSslFactory(new test.TestSSLFactory("SSL", inputStream));
//            try {
//                inputStream.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            try {
                String keyPath = HttpProxyServer.class.getClassLoader().getResource("cacerts").getPath();
                String password = "changeit";
                NioSSLFactory sslFactory = new NioSSLFactory("TLS", "SunX509", "JKS", keyPath, password);
                NioClientFactory.getFactory().setSSlFactory(sslFactory);
                NioClientFactory.getFactory().open();

//                NioSSLFactory sslFactory = new NioSSLFactory("TLS");
//                NioClientFactory.getFactory().setSSlFactory(sslFactory);
//                NioClientFactory.getFactory().open();
            } catch (Exception e) {
                e.printStackTrace();
            }
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
