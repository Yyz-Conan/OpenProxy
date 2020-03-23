package connect;

import connect.network.nio.NioClientTask;
import connect.network.nio.NioSender;
import util.StringEnvoy;

import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * 代理转发客户https请求
 */
public class ProxyHttpsConnectClient extends NioClientTask {
    private NioSender localSender;
    private String protocol;
    private ICloseListener listener;

    public ProxyHttpsConnectClient(String host, int port, NioSender localSender, String protocol) {
        if (localSender == null || host == null || port <= 0) {
            throw new NullPointerException("data host port or target is null !!!");
        }
        setAddress(host, port, false);
        this.localSender = localSender;
        this.protocol = StringEnvoy.isEmpty(protocol) ? "HTTP/1.1" : protocol;
        setReceive(new RemoteRequestReceive(localSender));
        setSender(new NioSender());
    }

    public void setOnCloseListener(ICloseListener listener) {
        this.listener = listener;
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
        sb.append(protocol);
        sb.append(" 200 Connection established\r\n");
        sb.append("Proxy-agent: YYD-HttpProxy\r\n");
        sb.append("\r\n");
        return sb.toString().getBytes();
    }

    @Override
    protected void onCloseClientChannel() {
        if (listener != null) {
            listener.onClose(getHost());
        }
    }
}
