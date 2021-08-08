package connect.socks5;

import connect.network.nio.NioSender;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class Socks5Sender extends NioSender {

    public Socks5Sender(SelectionKey selectionKey, SocketChannel channel) {
        super(selectionKey, channel);
    }
}
