package connect;

import connect.network.nio.NioClientTask;
import connect.network.nio.NioHPCClientFactory;
import intercept.ProxyFilterManager;
import log.LogDog;
import process.AESReceive;
import process.AESSender;
import util.StringEnvoy;

import java.nio.channels.SocketChannel;
import java.util.regex.Pattern;

/**
 * 接收处理客户请求
 */
public class HttpProxyClient extends NioClientTask {
    private String lastHost = null;
    //    private String host = null;
    private ConnectPool connectPool;

    public HttpProxyClient(SocketChannel channel, boolean isEnableRSA) {
        super(channel);
        setConnectTimeout(0);
        connectPool = new ConnectPool();
        if (isEnableRSA) {
            setReceive(new AESReceive(this, "onReceiveRequestData"));
            setSender(new AESSender(this));
        } else {
            setReceive(new RequestReceive(this, "onReceiveRequestData"));
            setSender(new RequestSender(this));
        }
    }

//    @Override
//    protected void onConfigSocket(boolean isConnect, SocketChannel channel) {
//        LogDog.d("==> isConnect = " + isConnect + " obj = " + HttpProxyClient.this.toString());
//    }

    private void onReceiveRequestData(byte[] data) {
        String proxyData = new String(data);
        if ("ping".equals(proxyData)) {
            NioHPCClientFactory.getFactory().removeTask(this);
            return;
        }
//        LogDog.d("==> data = " + proxyData + " obj = " + HttpProxyClient.this.toString());

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
                break;
            }
        }

        if (ProxyFilterManager.getInstance().isIntercept(host) && StringEnvoy.isNotEmpty(host)) {
//            LogDog.d("black list = " + host + "obj = " + HttpProxyClient.this.toString());
            NioHPCClientFactory.getFactory().removeTask(this);
            return;
        }

        if (Pattern.matches(".* .* HTTP.*", firsLine)) {
//            LogDog.v("==##> HttpProxyClient firsLine = " + firsLine);
//            LogDog.v("Proxy Request host = " + host + " obj = " + toString());
            String[] requestLineCells = firsLine.split(" ");
            String method = requestLineCells[0];
//            String urlStr = requestLineCells[1];
            String protocol = requestLineCells[2];

            NioClientTask clientTask = connectPool.get(host);
            if ("CONNECT".equals(method)) {
                if (clientTask == null) {
                    createNewSSLConnect(host, port, protocol);
                } else {
                    reuseSSLClient((ProxyHttpsConnectClient) clientTask, data);
                }
            } else {
                String url = "http://" + host + (port != 80 ? ":" + port : "");
                String newData = proxyData.replace(url, "");
                data = newData.getBytes();
                if (clientTask == null) {
                    createNewConnect(data, host, port);
                } else {
                    reuseClient(clientTask, data);
                }
            }
//            LogDog.d("==> Proxy Request host " + host + " proxyData = " + data.length + " obj = " + toString());
            LogDog.d("==> Proxy Request host " + host + " [ add connect count = " + HttpProxyServer.localConnectCount.get() + " ] " + " obj = " + HttpProxyClient.this.toString());
        } else {
            NioClientTask clientTask = connectPool.get(lastHost);
            if (clientTask instanceof ProxyHttpsConnectClient) {
                ProxyHttpsConnectClient httpsClient = (ProxyHttpsConnectClient) clientTask;
                reuseSSLClient(httpsClient, data);
            } else {
                LogDog.d("==? lastHost = " + lastHost + " data = " + proxyData);
                NioHPCClientFactory.getFactory().removeTask(this);
            }
        }
    }

    private void createNewSSLConnect(String host, int port, String protocol) {
        ProxyHttpsConnectClient sslNioClient = new ProxyHttpsConnectClient(host, port, protocol, getSender());
        sslNioClient.setConnectPool(connectPool);
        NioHPCClientFactory.getFactory().addTask(sslNioClient);
        lastHost = host;
    }

    private void createNewConnect(byte[] data, String host, int port) {
        ProxyHttpConnectClient connectClient = new ProxyHttpConnectClient(host, port, getSender(), data);
        connectClient.setConnectPool(connectPool);
        NioHPCClientFactory.getFactory().addTask(connectClient);
        lastHost = host;
    }

    private void reuseSSLClient(ProxyHttpsConnectClient clientTask, byte[] data) {
        if (!clientTask.isTaskNeedClose()) {
            clientTask.getSender().sendData(data);
        } else {
            createNewSSLConnect(clientTask.getHost(), clientTask.getPort(), clientTask.getProtocol());
        }
    }

    private void reuseClient(NioClientTask clientTask, byte[] data) {
        if (clientTask != null && !clientTask.isTaskNeedClose()) {
            clientTask.getSender().sendData(data);
        } else {
            createNewConnect(data, clientTask.getHost(), clientTask.getPort());
        }
    }


    @Override
    protected void onCloseSocketChannel() {
        connectPool.destroy();
        LogDog.d("==> Connect close " + lastHost + " [ remover connect count = " + HttpProxyServer.localConnectCount.decrementAndGet() + " ] " + " obj = " + HttpProxyClient.this.toString());

//        LogDog.d("==> Proxy Local Client close ing !!! " + host + " obj = " + HttpProxyClient.this.toString());
//        LogDog.d("---------- remover() Connect Count = " + HttpProxyServer.localConnectCount.decrementAndGet());
    }
}
