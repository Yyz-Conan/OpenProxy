package connect.socks5;

import connect.network.nio.NioClientFactory;

public class Socks5NetFactory {

    private static NioClientFactory sFactory;

    private Socks5NetFactory() {
    }

    public static NioClientFactory getFactory() {
        if (sFactory == null) {
            sFactory = new NioClientFactory();
        }
        return sFactory;
    }
}
