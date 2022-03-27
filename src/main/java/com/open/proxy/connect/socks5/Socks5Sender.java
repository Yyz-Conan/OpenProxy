package com.open.proxy.connect.socks5;


import com.currency.net.nio.NioSender;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class Socks5Sender extends NioSender {

    public Socks5Sender(SelectionKey selectionKey, SocketChannel channel) {
        setChannel(selectionKey, channel);
    }
}
