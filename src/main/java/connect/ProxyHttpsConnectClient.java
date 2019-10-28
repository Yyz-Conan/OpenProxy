package connect;

import connect.network.base.joggle.INetSender;
import connect.network.nio.NioClientTask;
import util.StringEnvoy;
import util.joggle.JavKeep;

import java.nio.channels.SocketChannel;

/**
 * 代理转发客户https请求
 */
public class ProxyHttpsConnectClient extends NioClientTask {
    private INetSender localSender;
    private String protocol;
    private ConnectPool connectPool = null;


    public ProxyHttpsConnectClient(String host, int port, String protocol, INetSender localSender) {
        super(host, port);
        this.protocol = StringEnvoy.isEmpty(protocol) ? "HTTP/1.1" : protocol;
        init(localSender);
    }

//    public SSLNioClient(SocketChannel remoteChannel, ISender localSender) {
//        super(remoteChannel);
//        if (remoteChannel == null) {
//            throw new NullPointerException("remoteChannel and localSender is can not be null !!!");
//        }
//        init(localSender);
//    }


    public String getProtocol() {
        return protocol;
    }

    public void setConnectPool(ConnectPool connectPool) {
        this.connectPool = connectPool;
    }

    private void init(INetSender localSender) {
        if (localSender == null || localSender == null) {
            throw new NullPointerException("proxyClient and localSender is can not be null !!!");
        }
        setConnectTimeout(0);
        this.localSender = localSender;
        setSender(new RequestSender());
        setReceive(new RequestReceive(this, "onReceiveHttpsData"));
    }

    @Override
    protected void onConfigSocket(boolean isConnect, SocketChannel channel) {
        if (isConnect) {
            localSender.sendDataNow(httpsTunnelEstablished());
            connectPool.put(getHost(), this);
        }
    }

    @JavKeep
    private void onReceiveHttpsData(byte[] data) {
        try {
            localSender.sendData(data);
        } catch (Exception e) {
        }
    }

    @Override
    protected void onCloseSocketChannel() {
        if (connectPool != null) {
            connectPool.remove(getHost());
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

}
