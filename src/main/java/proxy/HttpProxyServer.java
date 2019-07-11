package proxy;

import connect.network.nio.NioHPCClientFactory;
import connect.network.nio.NioServerTask;
import util.LogDog;

import java.nio.channels.SocketChannel;

public class HttpProxyServer extends NioServerTask {

    public static int localConnectCount = 0;

    @Override
    protected void onOpenServerChannel(boolean isSuccess) {
        if (isSuccess) {
            LogDog.d("==> Proxy Server Start Success !!! ");
//            HttpProxyServer.class.getClassLoader().getResource("cacerts").getPath();
//            InputStream inputStream = ProxyMain.class.getClassLoader().getResourceAsStream("ssl_ks");
//            NioClientFactory.getFactory().setSslFactory(new test.TestSSLFactory("SSL", inputStream));
//            try {
//                inputStream.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            try {
//                String keyPath = HttpProxyServer.class.getClassLoader().getResource("cacerts").getPath();
//                String password = "changeit";
//                NioSSLFactory sslFactory = new NioSSLFactory("TLS", "SunX509", "JKS", keyPath, password);
//                NioClientFactory.getFactory().setSSlFactory(sslFactory);
                NioHPCClientFactory.getFactory(2).open();
//                NioClientFactory.getFactory().open();

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
        NioHPCClientFactory.getFactory().addTask(client);
//        NioClientFactory.getFactory().addTask(client);
        HttpProxyServer.localConnectCount++;
        LogDog.d("==========================add==============================> localConnectCount = " + HttpProxyServer.localConnectCount);
    }

    @Override
    protected void onCloseServerChannel() {
        LogDog.e("==> Proxy Server close ing... !!! ");
    }
}
