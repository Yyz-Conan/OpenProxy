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
    private String lastHost = null;
    private ConnectPool connectPool;

    public HttpProxyClient(SocketChannel channel) {
        super(channel);
        setConnectTimeout(0);
        connectPool = new ConnectPool();
        setReceive(new RequestReceive(this, "onReceiveRequestData"));
        setSender(new NioHPCSender());
    }

    private void onReceiveRequestData(byte[] data) {
//        LogDog.d("==========================================================================================================");
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
            return;
        }

        if (Pattern.matches(".* .* HTTP.*", firsLine)) {
//            LogDog.v("==##> HttpProxyClient firsLine = " + firsLine);
            LogDog.v("Proxy Request host = " + host);
            String[] requestLineCells = firsLine.split(" ");
            String method = requestLineCells[0];
//            String urlStr = requestLineCells[1];
            String protocol = requestLineCells[2];

            NioClientTask clientTask = connectPool.get(host);
            if ("CONNECT".equals(method)) {
                if (clientTask == null) {
                    createNewSSLConnect(host, port, protocol);
                } else {
                    takeSSLClient((ProxyHttpsConnectClient) clientTask, data);
                }
            } else {
                if (clientTask == null) {
                    createNewConnect(data, host, port);
                } else {
                    takeClient(clientTask, data);
                }
            }
        } else {
            LogDog.v("Proxy Request last host = " + lastHost);
            ProxyHttpsConnectClient client = (ProxyHttpsConnectClient) connectPool.get(lastHost);
            takeSSLClient(client, data);
        }
//        LogDog.d("==========================================================================================================");
    }

    private void createNewSSLConnect(String host, int port, String protocol) {
        ProxyHttpsConnectClient sslNioClient = new ProxyHttpsConnectClient(host, port, protocol, getSender());
        sslNioClient.setConnectPool(connectPool);
        NioHPCClientFactory.getFactory().addTask(sslNioClient);
        connectPool.put(host, sslNioClient);
        lastHost = host;
    }

    private void createNewConnect(byte[] data, String host, int port) {
        ProxyHttpConnectClient connectClient = new ProxyHttpConnectClient(data, host, port, getSender());
        connectClient.setConnectPool(connectPool);
        NioHPCClientFactory.getFactory().addTask(connectClient);
        connectPool.put(host, connectClient);
    }

    private void takeSSLClient(ProxyHttpsConnectClient clientTask, byte[] data) {
        if (!clientTask.isCloseing()) {
            clientTask.getSender().sendData(data);
        } else {
            createNewSSLConnect(clientTask.getHost(), clientTask.getPort(), clientTask.getProtocol());
        }
    }

    private void takeClient(NioClientTask clientTask, byte[] data) {
        if (!clientTask.isCloseing()) {
            clientTask.getSender().sendData(data);
        } else {
            createNewConnect(data, clientTask.getHost(), clientTask.getPort());
        }
    }


    @Override
    protected void onCloseSocketChannel() {
//        LogDog.e("==> Proxy Local Client close ing !!! " + host);
        connectPool.destroy();
//        LogDog.d("---------- remover() Connect Count = " + HttpProxyServer.localConnectCount.decrementAndGet());
    }
}
