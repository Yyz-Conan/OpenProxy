package connect;

import connect.network.base.joggle.INetSender;
import connect.network.nio.NioClientTask;
import connect.network.nio.NioHPCClientFactory;
import connect.network.xhttp.HttpProtocol;
import connect.network.xhttp.entity.XHttpResponse;
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
    private INetSender remoteSender = null;

    public HttpProxyClient(SocketChannel channel, boolean isEnableRSA) {
        super(channel);
        setConnectTimeout(0);
        if (isEnableRSA) {
            setReceive(new AESReceive(this, "onReceiveRequestData"));
            setSender(new AESSender(this));
        } else {
            setReceive(new RequestReceive(this, "onReceiveRequestData"));
            setSender(new RequestSender(this));
        }
    }

    @Override
    protected void onConfigSocket(boolean isConnect, SocketChannel channel) {
        getSender().setChannel(channel);
//        LogDog.d("==> isConnect = " + isConnect + " obj = " + HttpProxyClient.this.toString());
    }

    private void onReceiveRequestData(XHttpResponse response) {
        String firsLine = response.getHeadForKey(HttpProtocol.XY_RESPONSE_CODE);
        String host = response.getHeadForKey(HttpProtocol.XY_HOST);
        if (ProxyFilterManager.getInstance().isIntercept(host) && StringEnvoy.isNotEmpty(host)) {
//            LogDog.d("black list = " + host + "obj = " + HttpProxyClient.this.toString());
            NioHPCClientFactory.getFactory().removeTask(this);
            return;
        }
        int port = 80;
        String[] arrayUrl = host.split(":");
        if (arrayUrl.length > 1) {
            port = Integer.parseInt(arrayUrl[1]);
        }

        if (ProxyFilterManager.getInstance().isIntercept(host) && StringEnvoy.isNotEmpty(host)) {
//            LogDog.d("black list = " + host + "obj = " + HttpProxyClient.this.toString());
            NioHPCClientFactory.getFactory().removeTask(this);
            return;
        }

    }

    private void onReceiveRequestData(byte[] data) {
        String proxyData = new String(data);
        if ("ping".equals(proxyData)) {
            NioHPCClientFactory.getFactory().removeTask(this);
            return;
        }

        String[] array = proxyData.split("\r\n");
        String firsLine = array[0];


        if (Pattern.matches(".* .* HTTP.*", firsLine)) {

            String host = null;
            int port = 80;
            for (int index = 1; index < array.length; index++) {
                if (array[index].startsWith("Host: ")) {
                    String urlStr = array[index].split(" ")[1];
                    String[] arrayUrl = urlStr.split(":");
                    host = arrayUrl[0];
                    if (arrayUrl.length > 1) {
                        String portStr = arrayUrl[1];
                        port = Integer.parseInt(portStr);
                    }
                    break;
                }
            }

            if (ProxyFilterManager.getInstance().isIntercept(host) || StringEnvoy.isEmpty(host)) {
                NioHPCClientFactory.getFactory().removeTask(this);
                return;
            }

//            LogDog.d("==> data = " + proxyData + " obj = " + HttpProxyClient.this.toString());

            String[] requestLineCells = firsLine.split(" ");
            String method = requestLineCells[0];
//            String urlStr = requestLineCells[1];
            String protocol = requestLineCells[2];

            if ("CONNECT".equals(method)) {
                createNewSSLConnect(host, port, protocol);
            } else {
                createNewConnect(data, host, port);
            }
            LogDog.d("==> Proxy Request host " + host + " [ add connect count = " + HttpProxyServer.localConnectCount.get() + " ] " + " obj = " + HttpProxyClient.this.toString());
        } else {
            if (remoteSender != null) {
                remoteSender.sendData(data);
            }
        }
    }

    private void createNewSSLConnect(String host, int port, String protocol) {
        ProxyHttpsConnectClient sslNioClient = new ProxyHttpsConnectClient(host, port, protocol, getSender());
        NioHPCClientFactory.getFactory().addTask(sslNioClient);
        remoteSender = sslNioClient.getSender();
        lastHost = host;
    }

    private void createNewConnect(byte[] data, String host, int port) {
        ProxyHttpConnectClient connectClient = new ProxyHttpConnectClient(host, port, getSender(), data);
        NioHPCClientFactory.getFactory().addTask(connectClient);
        remoteSender = connectClient.getSender();
        lastHost = host;
    }

    @Override
    protected void onCloseSocketChannel() {
        LogDog.d("==> Connect close " + lastHost + " [ remover connect count = " + HttpProxyServer.localConnectCount.decrementAndGet() + " ] " + " obj = " + HttpProxyClient.this.toString());
    }
}
