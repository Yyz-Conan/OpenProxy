package proxy;

import connect.network.nio.NioClientTask;
import connect.network.nio.NioHPCClientFactory;
import connect.network.nio.NioHPCSender;
import intercept.ProxyFilterManager;
import log.LogDog;

import java.nio.channels.SocketChannel;
import java.util.regex.Pattern;

/**
 * 接收处理客户请求
 */
public class HttpProxyClient extends NioClientTask {
    private SSLNioClient remoteSSLClient = null;
    private ProxyConnectClient remoteClient = null;

    private String host = null;

    public HttpProxyClient(SocketChannel channel) {
        super(channel);
        setConnectTimeout(3000);
        setReceive(new HttpReceive(this, "onReceive"));
        setSender(new NioHPCSender());
    }

    private void onReceive(byte[] data) {

        String proxyData = new String(data);
//        LogDog.d("==> proxyData = " + proxyData);

        String[] array = proxyData.split("\r\n");
        String firsLine = array[0];
//        String host = null;
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
                    remoteSSLClient = new SSLNioClient(host, port, getSender());
                    NioHPCClientFactory.getFactory().addTask(remoteSSLClient);
                } else {
                    takeSSLClient(data);
                    LogDog.e("==##> CONNECT remoteSSLClient  = " + host);
                }
            } else {
                if (remoteClient == null) {
                    remoteClient = new ProxyConnectClient(data, host, port, getSender());
                    NioHPCClientFactory.getFactory().addTask(remoteClient);
                } else {
                    if (remoteClient.isCloseing()) {
//                        LogDog.e("==##> e remoteClient 复用链接不存在 = " + host);
                        NioHPCClientFactory.getFactory().removeTask(this);
                    } else {
//                        LogDog.e("==##> s remoteClient 复用请求 = " + host);
                        remoteClient.getSender().sendData(data);
                    }
                }
            }
        } else {
            takeSSLClient(data);
        }
    }

    private void takeSSLClient(byte[] data) {
        if (remoteSSLClient != null && !remoteSSLClient.isCloseing()) {
            remoteSSLClient.getSender().sendData(data);
//            LogDog.e("==##> s remoteSSLClient 复用请求 = " + host);
        } else {
            NioHPCClientFactory.getFactory().removeTask(this);
//            LogDog.e("==##> e remoteSSLClient 复用链接不存在 = " + host);
        }
    }


    @Override
    protected void onCloseSocketChannel() {
        LogDog.e("==> Proxy Local Client close ing !!! " + host);
        LogDog.d("=====================remover=======================> localConnectCount = " + HttpProxyServer.localConnectCount.decrementAndGet());
        NioHPCClientFactory.getFactory().removeTask(remoteSSLClient);
        NioHPCClientFactory.getFactory().removeTask(remoteClient);
    }
}
