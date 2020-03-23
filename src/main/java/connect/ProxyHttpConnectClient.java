package connect;

import connect.network.nio.NioSender;

import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * 代理转发客户http请求
 */
public class ProxyHttpConnectClient extends AbsConnectClient {

    private byte[] data;

    public ProxyHttpConnectClient(String host, int port, NioSender localTarget, byte[] data) {
        if (localTarget == null || host == null || port <= 0) {
            throw new NullPointerException("data host port or target is null !!!");
        }
        setAddress(host, port, false);
        this.data = data;
        setSender(new NioSender());
        setReceive(new RemoteRequestReceive(localTarget));
    }

    @Override
    protected void onConnectCompleteChannel(boolean isConnect, SocketChannel channel, SSLEngine sslEngine) throws IOException {
        if (isConnect) {
            getSender().setChannel(channel);
            getSender().sendData(data);
            data = null;
        }
    }

}
