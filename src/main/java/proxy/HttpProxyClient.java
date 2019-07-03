package proxy;

import connect.network.nio.NioClientFactory;
import connect.network.nio.NioClientTask;
import connect.network.nio.NioSender;
import util.LogDog;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.regex.Pattern;

/**
 * 接收处理客户请求
 */
public class HttpProxyClient extends NioClientTask {
    private SSLNioClient remoteSSLClient = null;
    private ProxyConnectClient remoteClient = null;

    public HttpProxyClient(SocketChannel channel) {
        super(channel);
        setReceive(new HttpReceive(this, "onReceive"));
        setSender(new NioSender());
    }

    public void clearRemoteClient() {
        remoteClient = null;
    }

    public void clearSSLNioClient() {
        remoteSSLClient = null;
    }

    private void onReceive(byte[] data) {
        if (data.length == 0) {
            NioClientFactory.getFactory().removeTask(this);
        }

        String proxyData = new String(data);
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
            LogDog.v("==##> HttpProxyClient firsLine = " + firsLine);
            LogDog.v("==##> HttpProxyClient host = " + host);
            String[] requestLineCells = firsLine.split(" ");
            String method = requestLineCells[0];
//            String urlStr = requestLineCells[1];
//            String protocal = requestLineCells[2];
            //过滤google地址
            if (!host.contains("google")) {
                if ("CONNECT".equals(method)) {
                    if (remoteSSLClient == null) {
                        remoteSSLClient = crateSocketChannel(host, port);
                    } else {
                        remoteSSLClient.getSender().sendData(data);
                    }
                } else {
                    if (remoteClient == null) {
                        remoteClient = new ProxyConnectClient(this, data, host, port, getSender());
                        NioClientFactory.getFactory().addTask(remoteClient);
                    } else {
                        remoteClient.getSender().sendData(data);
                    }
                }
            } else {
                NioClientFactory.getFactory().removeTask(this);
            }
        } else {
            remoteSSLClient.getSender().sendData(data);
        }
    }

    private SSLNioClient crateSocketChannel(String host, int port) {
        SSLNioClient remoteSSLClient = null;
        try {
            SocketChannel remoteChannel = SocketChannel.open();
            remoteChannel.connect(new InetSocketAddress(host, port));
            Socket socket = remoteChannel.socket();
            //复用端口
            socket.setReuseAddress(true);
            socket.setKeepAlive(true);
            //关闭Nagle算法
            socket.setTcpNoDelay(true);
            //执行Socket的close方法，该方法也会立即返回
            socket.setSoLinger(true, 0);
            remoteSSLClient = new SSLNioClient(this, remoteChannel, getSender());
            NioClientFactory.getFactory().addTask(remoteSSLClient);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return remoteSSLClient;
    }

    @Override
    protected void onCloseSocketChannel() {
        LogDog.e("==> proxy.HttpProxyClient close !!! ");
        HttpProxyServer.localConnectCount--;
        LogDog.d("===================> localConnectCount = " + HttpProxyServer.localConnectCount);
        NioClientFactory.getFactory().removeTask(remoteSSLClient);
        NioClientFactory.getFactory().removeTask(remoteClient);
    }
}
