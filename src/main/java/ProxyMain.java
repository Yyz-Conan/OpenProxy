import connect.network.nio.NioServerFactory;
import proxy.HttpProxyServer;
import util.LogDog;
import util.NetUtils;

public class ProxyMain {


    // 183.2.236.16  百度 = 14.215.177.38  czh = 58.67.203.13
    public static void main(String[] args) {
        HttpProxyServer httpProxyServer = new HttpProxyServer();
        String host = NetUtils.getLocalIp("wlan");
        LogDog.d("==> proxy.HttpProxyServer host = " + host);
        httpProxyServer.setAddress(host, 7777);
        NioServerFactory.getFactory().open();
        NioServerFactory.getFactory().addTask(httpProxyServer);
    }
}
