package proxy;

import connect.network.base.joggle.ISender;
import connect.network.nio.NioClientTask;
import connect.network.nio.NioHPCSender;

import java.nio.channels.SocketChannel;


public class SSLNioClient extends NioClientTask {
    private ISender localSender;
    private HttpProxyClient proxyClient;

    public SSLNioClient(HttpProxyClient proxyClient, String host, int port, ISender localSender) {
        super(host, port);
        init(proxyClient, localSender);
    }

    public SSLNioClient(HttpProxyClient proxyClient, SocketChannel remoteChannel, ISender localSender) {
        super(remoteChannel);
        if (remoteChannel == null) {
            throw new NullPointerException("remoteChannel and localSender is can not be null !!!");
        }
        init(proxyClient, localSender);
    }

    private void init(HttpProxyClient proxyClient, ISender localSender) {
        if (localSender == null || localSender == null) {
            throw new NullPointerException("proxyClient and localSender is can not be null !!!");
        }
        setConnectTimeout(3000);
        this.localSender = localSender;
        this.proxyClient = proxyClient;
        setSender(new NioHPCSender());
        setReceive(new HttpReceive(this, "onReceiveSSLNio"));
    }

    @Override
    protected void onConnectSocketChannel(boolean isConnect) {
        if (isConnect) {
            //当前是客户端第一次访问
            localSender.sendData(httpsTunnelEstablished());
        }
    }

    private void onReceiveSSLNio(byte[] data) {
//        String html = new String(data);
//        LogDog.v("==##> SSLNioClient onReceiveSSLNio data = " + html);
        try {
            localSender.sendData(data);
        } catch (Exception e) {
        }
    }

    private byte[] httpsTunnelEstablished() {
        StringBuffer sb = new StringBuffer();
        sb.append("HTTP/1.1 200 Connection established\r\n");
        sb.append("Proxy-agent: YYD-HttpProxy\r\n");
        sb.append("\r\n");
        return sb.toString().getBytes();
    }

    @Override
    protected void onCloseSocketChannel() {
        proxyClient.clearSSLNioClient();
    }
}
