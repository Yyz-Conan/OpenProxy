package connect;

import connect.network.base.joggle.INetReceive;
import connect.network.nio.NioClientTask;
import connect.network.nio.NioHPCClientFactory;
import connect.network.nio.NioSender;
import connect.network.xhttp.entity.XResponse;
import connect.network.xhttp.entity.XResponseHelper;
import intercept.ProxyFilterManager;
import log.LogDog;
import process.AESReceive;
import process.AESSender;
import util.StringEnvoy;

import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.regex.Pattern;

/**
 * 接收处理客户请求
 * 需求：远程tls接收者
 */
public class HttpProxyClient extends NioClientTask implements ICloseListener {
    private String requestHost = null;
    private NioClientTask proxyClient;

    public HttpProxyClient(SocketChannel channel, boolean isEnableRSA) {
        super(channel);
        ReceiveCallBack receiveCallBack = new ReceiveCallBack();
        if (isEnableRSA) {
            setReceive(new AESReceive(receiveCallBack));
            setSender(new AESSender());
        } else {
            setSender(new NioSender());
            setReceive(new LocalRequestReceive(receiveCallBack));
        }
    }


    public String getRequestHost() {
        return requestHost;
    }

    @Override
    protected void onConnectCompleteChannel(boolean isConnect, SocketChannel channel, SSLEngine sslEngine) {
        if (isConnect) {
            getSender().setChannel(channel);
        }
    }

    @Override
    public void onClose(String host) {
        if (StringEnvoy.isNotEmpty(host) && host.equals(requestHost)) {
            NioHPCClientFactory.getFactory().removeTask(HttpProxyClient.this);
        }
    }

    class ReceiveCallBack implements INetReceive<XResponse> {

//        public XResponse response;

        @Override
        public void onReceive(XResponse response, Exception e) {
            if (e != null) {
                return;
            }
//            this.response = response;
            String newRequestHost = XResponseHelper.getHost(response);
            if (ProxyFilterManager.getInstance().isIntercept(newRequestHost) && StringEnvoy.isNotEmpty(newRequestHost)) {
//                LogDog.e("拦截黑名单 host = " + requestHost);
                NioHPCClientFactory.getFactory().removeTask(HttpProxyClient.this);
                requestHost = newRequestHost;
                return;
            }
//            LogDog.d("Browser initiated request = " + new String(response.getRawData()) + this);
            if (!newRequestHost.equals(requestHost)) {
                NioHPCClientFactory.getFactory().removeTask(proxyClient);
                proxyClient = null;
            }
            if (proxyClient != null) {
                try {
                    proxyClient.getSender().sendData(response.getRawData());
                } catch (IOException ex) {
                    ex.printStackTrace();
                    NioHPCClientFactory.getFactory().removeTask(HttpProxyClient.this);
                }
                return;
            }
            requestHost = newRequestHost;
            int port = XResponseHelper.getPort(response);
            String method = XResponseHelper.getRequestMethod(response);
            if ("CONNECT".equals(method)) {
//                LogDog.d("Browser initiated request Host = " + requestHost + "  " + this);
                createNewSSLConnect(requestHost, port, null);
            } else {
                createNewConnect(response.getRawData(), requestHost, port);
            }
//            String encode = response.getHeadForKey(XHttpProtocol.XY_CONTENT_ENCODING);
//            if (StringEnvoy.isNotEmpty(encode) && encode.contains("gzip")) {
//                //需要解压
//                response.setHttpData(GZipUtils.unCompress(response.getHttpData()));
//            }
//            LogDog.d("Browser initiated request = " + new String(response.getRawData()) + this);
        }

        //        @Override
        public void onReceive(byte[] data, Exception e) {
            if (e != null) {
                return;
            }
            String proxyData = new String(data);
            if ("ping".equals(proxyData)) {
                NioHPCClientFactory.getFactory().removeTask(HttpProxyClient.this);
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
                    NioHPCClientFactory.getFactory().removeTask(HttpProxyClient.this);
                    return;
                }

                LogDog.d("==> data = " + proxyData + " obj = " + HttpProxyClient.this.toString());

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
            }
//            else {
//                if (remoteSender != null) {
//                    try {
//                        remoteSender.sendData(data);
//                    } catch (IOException ex) {
//                        ex.printStackTrace();
//                    }
//                }
//            }
        }
    }

    private void createNewSSLConnect(String host, int port, String protocol) {
        ProxyHttpsConnectClient sslNioClient = new ProxyHttpsConnectClient(host, port, getSender(), protocol);
        LocalRequestReceive receive = getReceive();
        receive.setTLS(true);
        receive.setRemoteSender(sslNioClient.getSender());
        sslNioClient.setOnCloseListener(this);
        NioHPCClientFactory.getFactory().addTask(sslNioClient);
        proxyClient = sslNioClient;
    }

    private void createNewConnect(byte[] data, String host, int port) {
        ProxyHttpConnectClient connectClient = new ProxyHttpConnectClient(host, port, getSender(), data);
        LocalRequestReceive receive = getReceive();
        receive.setRemoteSender(connectClient.getSender());
        connectClient.setOnCloseListener(this);
        NioHPCClientFactory.getFactory().addTask(connectClient);
        proxyClient = connectClient;
    }

    @Override
    protected void onCloseClientChannel() {
        NioHPCClientFactory.getFactory().removeTask(proxyClient);
        LogDog.d("==> Connect close " + requestHost + " [ remover connect count = " + HttpProxyServer.localConnectCount.decrementAndGet() + " ] " + " obj = " + HttpProxyClient.this.toString());
    }


}
