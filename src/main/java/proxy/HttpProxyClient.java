package proxy;

import connect.network.nio.NioClientFactory;
import connect.network.nio.NioClientTask;
import connect.network.nio.NioReceive;
import connect.network.nio.NioSender;
import connect.network.tcp.TcpClientFactory;
import util.LogDog;

import javax.net.ssl.SSLSocket;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

/**
 * 接收处理客户请求
 */
public class HttpProxyClient extends NioClientTask {
//    private NioSender sender;

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

        String[] args = proxyData.split("\r\n");
        if (args == null || args.length <= 1) {
            return;
        }
        String[] tmp = args[1].split(":");
        if (tmp == null || tmp.length == 0) {
            return;
        }
        LogDog.d("==> HttpProxyClient onReceive request address = " + args[1]);

        String host = tmp[1].trim();
        //过滤google地址
        if (!host.contains("google")) {
            int port = tmp.length == 2 ? 80 : Integer.parseInt(tmp[2]);
            ProxyConnectClient connectClient = new ProxyConnectClient(data, host, port, getSender());
            NioClientFactory.getFactory().addTask(connectClient);
        } else {
            NioClientFactory.getFactory().removeTask(this);
        }
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
