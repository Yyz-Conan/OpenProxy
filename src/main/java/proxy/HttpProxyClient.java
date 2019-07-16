package proxy;

import connect.network.nio.NioClientTask;
import connect.network.nio.NioHPCClientFactory;
import connect.network.nio.NioHPCSender;
import intercept.ProxyFilterManager;
import util.LogDog;

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
        setConnectTimeout(3000);
        setReceive(new HttpReceive(this, "onReceive"));
        setSender(new NioHPCSender());
    }

    public void clearRemoteClient() {
        remoteClient = null;
    }

    public void clearSSLNioClient() {
        remoteSSLClient = null;
    }

    private void onReceive(byte[] data) {
//        if (data.length == 0) {
//            NioHPCClientFactory.getFactory().removeTask(this);
////            NioClientFactory.getFactory().removeTask(this);
//        }

        String proxyData = new String(data);
//        LogDog.d("==> proxyData = " + proxyData);
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

        if (ProxyFilterManager.getInstance().isIntercept(host)) {
            NioHPCClientFactory.getFactory().removeTask(this);
//            NioClientFactory.getFactory().removeTask(this);
            return;
        }

        if (Pattern.matches(".* .* HTTP.*", firsLine)) {
//            LogDog.v("==##> HttpProxyClient firsLine = " + firsLine);
            LogDog.v("==##> Proxy Local Client host = " + host);
            String[] requestLineCells = firsLine.split(" ");
            String method = requestLineCells[0];
//            String urlStr = requestLineCells[1];
//            String protocal = requestLineCells[2];
            if ("CONNECT".equals(method)) {
                if (remoteSSLClient == null) {
                    remoteSSLClient = new SSLNioClient(this, host, port, getSender());
                    NioHPCClientFactory.getFactory().addTask(remoteSSLClient);
//                        NioClientFactory.getFactory().addTask(remoteSSLClient);
                } else {
                    remoteSSLClient.getSender().sendData(data);
                }
            } else {
                if (remoteClient == null) {
                    remoteClient = new ProxyConnectClient(this, data, host, port, getSender());
                    NioHPCClientFactory.getFactory().addTask(remoteClient);
//                    NioClientFactory.getFactory().addTask(remoteClient);
                } else {
                    remoteClient.getSender().sendData(data);
                }
            }
        } else {
            if (remoteSSLClient != null) {
                remoteSSLClient.getSender().sendData(data);
            }
        }
    }


    @Override
    protected void onCloseSocketChannel() {
        LogDog.e("==> Proxy Local Client close ing !!! ");
        HttpProxyServer.localConnectCount--;
        LogDog.d("=====================remover=======================> localConnectCount = " + HttpProxyServer.localConnectCount);
        NioHPCClientFactory.getFactory().removeTask(remoteSSLClient);
        NioHPCClientFactory.getFactory().removeTask(remoteClient);
//        NioClientFactory.getFactory().removeTask(remoteSSLClient);
//        NioClientFactory.getFactory().removeTask(remoteClient);
    }
}
