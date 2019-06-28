import connect.network.nio.NioClientFactory;
import connect.network.nio.NioSSLFactory;
import connect.network.nio.NioServerFactory;
import proxy.HttpProxyServer;
import test.HttpClient;
import util.LogDog;
import util.NetUtils;

public class ProxyMain {


    // 183.2.236.16  百度 = 14.215.177.38  czh = 58.67.203.13
    public static void main(String[] args) {
        HttpProxyServer httpProxyServer = new HttpProxyServer();
        String host = NetUtils.getLocalIp("eth2");
        LogDog.d("==> proxy.HttpProxyServer host = " + host);
        httpProxyServer.setAddress(host, 7777);
        NioServerFactory.getFactory().open();
        NioServerFactory.getFactory().addTask(httpProxyServer);

//        HttpClient httpClient = new HttpClient("offlintab.firefoxchina.cn", 443);
//        String keyPath = "H:\\Project\\GitHub\\HttpProxy\\src\\main\\resources\\cacerts";
//        String passwrod = "changeit";
//        NioClientFactory.getFactory().setSSlFactory(new NioSSLFactory("TLS", "SunX509", "JKS", keyPath, passwrod));
//        NioClientFactory.getFactory().open();
//        NioClientFactory.getFactory().addTask(httpClient);
    }
}
