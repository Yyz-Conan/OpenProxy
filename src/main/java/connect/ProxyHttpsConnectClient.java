package connect;

import connect.network.nio.NioClientTask;
import connect.network.nio.NioSender;
import util.StringEnvoy;
import util.joggle.JavKeep;

import java.nio.channels.SocketChannel;

/**
 * 代理转发客户https请求
 */
public class ProxyHttpsConnectClient extends NioClientTask {
    private NioSender localSender;
    private String protocol;


    public ProxyHttpsConnectClient(String host, int port, String protocol, NioSender localSender) {
        super(host, port);
        this.protocol = StringEnvoy.isEmpty(protocol) ? "HTTP/1.1" : protocol;
        init(localSender);
    }

    private void init(NioSender localSender) {
        if (localSender == null || localSender == null) {
            throw new NullPointerException("proxyClient and localSender is can not be null !!!");
        }
        setConnectTimeout(0);
        this.localSender = localSender;
        setSender(new RequestSender(this));
        setReceive(new RequestReceive(this, "onReceiveHttpsData"));
    }

    @Override
    protected void onConfigSocket(boolean isConnect, SocketChannel channel) {
        if (isConnect) {
            getSender().setChannel(channel);
            localSender.sendData(httpsTunnelEstablished());
        }
    }

    @JavKeep
    private void onReceiveHttpsData(byte[] data) {
        localSender.sendData(data);
    }

    private byte[] httpsTunnelEstablished() {
        StringBuffer sb = new StringBuffer();
        sb.append(protocol);
        sb.append(" 200 Connection established\r\n");
        sb.append("Proxy-agent: YYD-HttpProxy\r\n");
        sb.append("\r\n");
        return sb.toString().getBytes();
    }

}
