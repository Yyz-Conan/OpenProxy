package proxy;

import connect.network.base.joggle.ISender;
import connect.network.nio.NioClientTask;
import connect.network.nio.NioHPCSender;

import java.nio.channels.SocketChannel;

/**
 * 代理转发客户https请求
 */
public class SSLNioClient extends NioClientTask {
    private ISender localSender;


    public SSLNioClient(String host, int port, ISender localSender) {
        super(host, port);
        init(localSender);
    }

    public SSLNioClient(SocketChannel remoteChannel, ISender localSender) {
        super(remoteChannel);
        if (remoteChannel == null) {
            throw new NullPointerException("remoteChannel and localSender is can not be null !!!");
        }
        init(localSender);
    }

    private void init(ISender localSender) {
        if (localSender == null || localSender == null) {
            throw new NullPointerException("proxyClient and localSender is can not be null !!!");
        }
        setConnectTimeout(0);
        this.localSender = localSender;
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

}
