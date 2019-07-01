package test;

import connect.network.nio.NioClientTask;
import connect.network.nio.NioReceive;
import connect.network.nio.NioSender;
import connect.network.tcp.TcpClientFactory;
import proxy.SSLClient;
import util.LogDog;

import javax.net.ssl.SSLSocket;

public class HttpClient extends NioClientTask {
//    private SSLNioClient sslNioClient = null;

    public HttpClient(String host, int port) {
        super(host, port);
        setSender(new NioSender());
        setReceive(new NioReceive(this, "onReceive"));
    }

    @Override
    protected void onConnectSocketChannel(boolean isConnect) {
        LogDog.v("==##> HttpClient onConnectSocketChannel isConnect = " + isConnect);
        if (isConnect) {
            SSLSocket sslSocket = getSSLSSocket();
            TcpClientFactory.getFactory().open();
            SSLClient sslClient = new SSLClient(sslSocket);
            TcpClientFactory.getFactory().addTask(sslClient);

//            if (sslNioClient == null) {
//                try {
//                    SocketChannel remoteChannel = SocketChannel.open();
//                    remoteChannel.connect(new InetSocketAddress(getHost(), getPort()));
//                    sslNioClient = new SSLNioClient(remoteChannel, getSender());
//                    NioClientFactory.getFactory().addTask(sslNioClient);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            String sendHtml = "CONNECT www.baidu.com:443 HTTP/1.1\n" +
//                    "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:67.0) Gecko/20100101 Firefox/67.0\n" +
//                    "Proxy-Connection: keep-alive\n" +
//                    "Connection: keep-alive\n" +
//                    "Host: www.baidu.com:443" +
//                    "\n";
//            getSender().sendData(sendHtml.getBytes());
//            sslNioClient.getSender().sendData(sendHtml.getBytes());
        }
    }

    private void onReceive(byte[] data) {
        String html = new String(data);
        LogDog.v("==##> HttpClient onReceive data = " + html);
//        if (Pattern.matches(".* .* HTTP.*", html)) {
//            //当前是客户端请求
//            getSender().sendData(httpsTunnelEstablished());
//        }
//        sslNioClient.getSender().sendData(data);
    }

    public static byte[] httpsTunnelEstablished() {
        StringBuffer sb = new StringBuffer();
        sb.append("HTTP/1.1 200 Connection Established\r\n");
        sb.append("Proxy-agent: https://github.com/arloor/proxyme\r\n");
        sb.append("\r\n");
        return sb.toString().getBytes();
    }
}
