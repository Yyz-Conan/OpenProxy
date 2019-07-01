package proxy;

import connect.network.nio.NioClientFactory;
import connect.network.nio.NioClientTask;
import connect.network.nio.NioReceive;
import util.LogDog;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.regex.Pattern;

/**
 * 接收处理客户请求
 */
public class HttpProxyClient extends NioClientTask {
    //    private NioSender sender;
    private SSLNioClient remoteClient = null;

    public HttpProxyClient(SocketChannel channel) {
        super(channel);
        setReceive(new NioReceive(this, "onReceive"));
        setSender(new HttpSender(this));
    }

    private void onReceive(byte[] data) {
        if (data.length == 0) {
            NioClientFactory.getFactory().removeTask(this);
        }

        String proxyData = new String(data);
        LogDog.v("==##> HttpProxyClient onReceive proxyData = " + proxyData);
        String[] array = proxyData.split("\r\n");
        String firsLine = array[0];
        String host = null;
        int port = 80;
        for (String tmp : array) {
            if (tmp.startsWith("Host: ")) {
                String urlStr = tmp.split(" ")[1];
                String[] arrayUrl = urlStr.split(":");
                host = arrayUrl[0];
                if (arrayUrl.length > 1) {
                    String portStr = arrayUrl[1];
                    port = Integer.parseInt(portStr);
                }
            }
        }
        if (Pattern.matches(".* .* HTTP.*", firsLine)) {
            String[] requestLineCells = firsLine.split(" ");
            String method = requestLineCells[0];
            String urlStr = requestLineCells[1];
            String protocal = requestLineCells[2];
            //过滤google地址
            if (!host.contains("google")) {
                if ("CONNECT".equals(method)) {
                    try {
                        SocketChannel remoteChannel = SocketChannel.open();
                        remoteChannel.connect(new InetSocketAddress(host, port));
                        remoteClient = new SSLNioClient(remoteChannel, getSender());
                        NioClientFactory.getFactory().addTask(remoteClient);
                        //当前是客户端第一次访问
                        getSender().sendData(httpsTunnelEstablished());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    if (remoteClient != null) {
                        remoteClient.getSender().sendData(data);
                    } else {
                        ProxyConnectClient connectClient = new ProxyConnectClient(data, host, port, getSender());
                        NioClientFactory.getFactory().addTask(connectClient);
                    }
                }
            } else {
                NioClientFactory.getFactory().removeTask(this);
            }
        } else {
            remoteClient.getSender().sendData(data);
        }
    }


    public static byte[] httpsTunnelEstablished() {
        StringBuffer sb = new StringBuffer();
        sb.append("HTTP/1.1 200 Connection Established\r\n");
        sb.append("Proxy-agent: https://github.com/arloor/proxyme\r\n");
        sb.append("\r\n");
        return sb.toString().getBytes();
    }


    @Override
    protected void onConnectSocketChannel(boolean isConnect) {
        if (isConnect) {
            SocketChannel channel = getSocketChannel();
            try {
                InetSocketAddress address = (InetSocketAddress) channel.getRemoteAddress();
                LogDog.d("==> connect client address = " + address.getHostString());
            } catch (Exception e) {
                e.printStackTrace();
            }

//            if (getPort() == 443) {
//                SSLSocket sslSocket = getSSLSSocket();
//                TcpClientFactory.getFactory().open();
//                SSLClient sslClient = new SSLClient(sslSocket);
//                TcpClientFactory.getFactory().addTask(sslClient);
//            }
        }
    }

    @Override
    protected void onCloseSocketChannel() {
        LogDog.e("==> proxy.HttpProxyClient close !!! ");
    }
}
