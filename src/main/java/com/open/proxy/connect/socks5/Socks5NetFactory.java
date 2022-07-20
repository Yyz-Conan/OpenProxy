package com.open.proxy.connect.socks5;


import com.jav.net.nio.NioClientFactory;

public class Socks5NetFactory {

    private Socks5NetFactory() {
    }

    private static final class InnerClass {
        public static final NioClientFactory sFactory = new NioClientFactory();
    }

    public static NioClientFactory getFactory() {
        return InnerClass.sFactory;
    }
}
