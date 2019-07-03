package proxy;

import connect.network.base.joggle.ISender;
import connect.network.nio.NioClientTask;
import connect.network.nio.NioSender;

import java.nio.channels.SocketChannel;


public class SSLNioClient extends NioClientTask {
    private ISender localSender;
    private HttpProxyClient proxyClient;

//    public SSLNioClient(String host, int port, ISender localSender) {
//        super(host, port);
//        this.localSender = localSender;
//    }

    public SSLNioClient(HttpProxyClient proxyClient, SocketChannel remoteChannel, ISender localSender) {
        super(remoteChannel);
        if (localSender == null || remoteChannel == null) {
            throw new NullPointerException("remoteChannel and localSender is can not be null !!!");
        }
        this.localSender = localSender;
        this.proxyClient = proxyClient;
        setSender(new NioSender());
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
        localSender.sendData(data);
    }

    private byte[] httpsTunnelEstablished() {
        StringBuffer sb = new StringBuffer();
        sb.append("HTTP/1.1 200 Connection Established\r\n");
        sb.append("Proxy-agent: YYD-HttpProxy\r\n");
        sb.append("\r\n");
        return sb.toString().getBytes();
    }

    @Override
    protected void onCloseSocketChannel() {
        proxyClient.clearSSLNioClient();
    }
}
