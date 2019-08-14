import connect.network.nio.NioServerFactory;
import intercept.BuiltInProxyFilter;
import intercept.ProxyFilterManager;
import log.LogDog;
import proxy.HttpProxyServer;
import util.NetUtils;

import java.net.URL;

public class ProxyMain {


    // 183.2.236.16  百度 = 14.215.177.38  czh = 58.67.203.13
    public static void main(String[] args) {
        //初始化地址过滤器
        URL url = ProxyMain.class.getClassLoader().getResource("AddressTable.dat");
        BuiltInProxyFilter proxyFilter = new BuiltInProxyFilter(url.getPath());
        ProxyFilterManager.getInstance().addFilter(proxyFilter);

        //开启代理服务
        HttpProxyServer httpProxyServer = new HttpProxyServer();
        String host = NetUtils.getLocalIp("eth2");
        LogDog.d("==> proxy.HttpProxyServer host = " + host);
        httpProxyServer.setAddress(host, 7777);
        NioServerFactory.getFactory().open();
        NioServerFactory.getFactory().addTask(httpProxyServer);

    }


}
