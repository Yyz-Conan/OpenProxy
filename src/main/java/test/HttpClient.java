package test;

import connect.network.nio.NioClientFactory;
import connect.network.nio.NioClientTask;
import connect.network.nio.NioReceive;
import connect.network.nio.NioSender;
import connect.network.tcp.TcpClientFactory;
import proxy.SSLClient;
import proxy.SSLNioClient;
import util.LogDog;

import javax.net.ssl.SSLSocket;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class HttpClient extends NioClientTask {

    public HttpClient(String host, int port) {
        super(host, port);
        NioSender sender = new NioSender();
        setSender(sender);
        NioReceive receive = new NioReceive(this, "onReceive");
        setReceive(receive);
    }

    @Override
    protected void onConnectSocketChannel(boolean isConnect) {
        LogDog.v("==##> HttpClient onConnectSocketChannel isConnect = " + isConnect);
        if (isConnect) {
            SSLSocket sslSocket = getSSLSSocket();
            TcpClientFactory.getFactory().open();
            SSLClient sslClient = new SSLClient(sslSocket, getSender());
            TcpClientFactory.getFactory().addTask(sslClient);

            SSLNioClient sslNioClient = new SSLNioClient();
            NioClientFactory.getFactory().addTask(sslNioClient);

            getSender().sendData(httpsTunnelEstablished());
        }
    }

    private void onReceive(byte[] data) {
        LogDog.v("==##> HttpClient onReceive data = " + new String(data));
    }

    public static byte[] httpsTunnelEstablished() {
        StringBuffer sb = new StringBuffer();
        sb.append("HTTP/1.1 200 Connection Established\r\n");
        sb.append("Proxy-agent: https://github.com/arloor/proxyme\r\n");
        sb.append("\r\n");
        return sb.toString().getBytes();
    }
}
