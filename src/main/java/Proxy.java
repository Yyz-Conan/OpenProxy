import connect.network.nio.NioServerFactory;
import util.NetUtils;

public class Proxy {

    public static void main(String[] args){
        HttpProxyServer httpProxyServer =  new HttpProxyServer();
        String host = NetUtils.getLocalIp("wlan");
        httpProxyServer.setAddress(host,8888);
        NioServerFactory.getFactory().open();
        NioServerFactory.getFactory().addTask(httpProxyServer);
    }
}
