package connect;

import connect.network.nio.NioSender;

import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * 代理转发客户https请求
 */
public class ProxyHttpsConnectClient extends AbsConnectClient {
    private NioSender localSender;

    public ProxyHttpsConnectClient(String host, int port, NioSender localSender) {
        if (localSender == null || host == null || port <= 0) {
            throw new NullPointerException("data host port or target is null !!!");
        }
        setAddress(host, port, false);
        this.localSender = localSender;
        setReceive(new RemoteRequestReceiver(localSender));
        setSender(new NioSender());
    }

    @Override
    protected void onConnectCompleteChannel(boolean isConnect, SocketChannel channel, SSLEngine sslEngine) throws IOException {
        if (isConnect) {
            getSender().setChannel(channel);
            localSender.sendData(httpsTunnelEstablished());
        }
    }

    private byte[] httpsTunnelEstablished() {
        StringBuffer sb = new StringBuffer();
        sb.append("HTTP/1.1 200 Connection established\r\n");
        sb.append("Proxy-agent: YYD-HttpProxy\r\n");
        sb.append("\r\n");
        return sb.toString().getBytes();
    }

}
