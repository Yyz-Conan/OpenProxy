package com.open.proxy.connect.socks5;


import com.currency.net.nio.NioClientFactory;

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
