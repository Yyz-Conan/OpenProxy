package connect.server;

import config.AnalysisConfig;
import connect.AbsClient;
import connect.DecryptionReceiver;
import connect.EncryptionSender;
import connect.LocalRequestReceiver;
import connect.clinet.RemoteProxyClient;
import connect.joggle.ICloseListener;
import connect.network.base.joggle.INetReceiver;
import connect.network.nio.NioClientTask;
import connect.network.nio.NioHPCClientFactory;
import connect.network.xhttp.ByteCacheStream;
import connect.network.xhttp.entity.XResponse;
import connect.network.xhttp.entity.XResponseHelper;
import cryption.*;
import cryption.joggle.IDecryptListener;
import cryption.joggle.IEncryptListener;
import intercept.InterceptFilterManager;
import intercept.ProxyFilterManager;
import log.LogDog;
import util.StringEnvoy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;

/**
 * 接收处理客户请求
 * 需求：远程tls接收者
 */
public class HttpProxyClient extends NioClientTask implements ICloseListener {
    private String requestHost = null;
    private NioClientTask proxyClient;
    private boolean isServerMode;
    private String remoteHost;
    private int remotePort;

    public HttpProxyClient(SocketChannel channel) {
        super(channel, null);
        ReceiveCallBack receiveCallBack = new ReceiveCallBack();
        isServerMode = AnalysisConfig.getInstance().getBooleanValue("isServerMode");

        IDecryptListener decryptListener = null;
        IEncryptListener encryptListener = null;

        if (isServerMode) {
            //如果是运行在服务模式则开启数据加密
            String encryption = AnalysisConfig.getInstance().getValue("encryptionMode");
            if (EncryptionType.RSA.name().equals(encryption)) {
                decryptListener = new RSADecrypt();
                encryptListener = new RSAEncrypt();
            } else if (EncryptionType.AES.name().equals(encryption)) {
                decryptListener = new AESDecrypt();
                encryptListener = new AESEncrypt();
            } else if (EncryptionType.BASE64.name().equals(encryption)) {
                decryptListener = new Base64Decrypt();
                encryptListener = new Base64Encrypt();
            }
        }

        //创建解密接收者
        DecryptionReceiver receiver = new DecryptionReceiver(decryptListener);
        //设置数据回调接口
        receiver.setReceiver(receiveCallBack);
        setReceive(receiver);
        EncryptionSender sender = new EncryptionSender(encryptListener);
        sender.setChannel(channel);
        setSender(sender);
    }

    public void setRemoteServer(String remoteHost, int remotePort) {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

    @Override
    public void onClose(String host) {
        if (StringEnvoy.isNotEmpty(host) && host.equals(requestHost)) {
            NioHPCClientFactory.getFactory().removeTask(HttpProxyClient.this);
        }
    }

    class ReceiveCallBack implements INetReceiver<XResponse> {


        @Override
        public void onReceive(XResponse response, Exception e) {
            if (e != null) {
                return;
            }
            String newRequestHost = XResponseHelper.getHost(response);
            //黑名单过滤
            if (InterceptFilterManager.getInstance().isIntercept(newRequestHost) && StringEnvoy.isNotEmpty(newRequestHost)) {
//                LogDog.e("拦截黑名单 host = " + requestHost);
                NioHPCClientFactory.getFactory().removeTask(HttpProxyClient.this);
                requestHost = newRequestHost;
                return;
            }

            LogDog.d("Connect = " + newRequestHost);
//            ByteCacheStream stream = response.getRawData();
//            LogDog.d("Browser initiated request = " + stream.toString());

            if (StringEnvoy.isNotEmpty(requestHost) && !newRequestHost.equals(requestHost) && isServerMode) {
                //发现当前请求的网站跟上次请求的网站不一样，则关闭之前的链接（只限制运行于服务模式）
                NioHPCClientFactory.getFactory().removeTask(proxyClient);
                proxyClient = null;
            }

            if (proxyClient != null) {
                //如果是同个域名请求则复用链路
                try {
                    ByteCacheStream raw = response.getRawData();
                    proxyClient.getSender().sendData(raw.toByteArray());
                } catch (IOException ex) {
                    //如果复用链路有异常则关闭
                    NioHPCClientFactory.getFactory().removeTask(proxyClient);
                    ex.printStackTrace();
                    proxyClient = null;
                }
            }
            if (proxyClient == null) {
                String method = XResponseHelper.getRequestMethod(response);
                AbsClient client;
                int port = XResponseHelper.getPort(response);
                if (isServerMode) {
                    //当前是服务模式，请求指定的域名，需要响应 connect 请求
                    client = new ProxyConnectClient(newRequestHost, port, getSender());
                } else {
                    //当前是客户端模式
                    boolean isNeedProxy = ProxyFilterManager.getInstance().isNeedProxy(newRequestHost);
                    if (!isNeedProxy) {
                        //本地网络测试要访问的域名是否可以联通
                        isNeedProxy = !isNodeReachable(newRequestHost, port);
                        //添加需要代理访问的域名
                        if (isNeedProxy) {
                            ProxyFilterManager.getInstance().addProxyHost(newRequestHost);
                        }
                    }
                    if (isNeedProxy) {
                        //当本地网络环境访问不成功则走代理服务访问
                        RemoteProxyClient remoteProxyClient = new RemoteProxyClient(getSender(), remoteHost, remotePort);
                        remoteProxyClient.setRealHost(newRequestHost);
                        client = remoteProxyClient;
                    } else {
                        //如果本地网络可以联通则不走代理，需要响应 connect 请求
                        client = new ProxyConnectClient(newRequestHost, port, getSender());
                    }
                }

                LocalRequestReceiver receive = getReceive();
                if ("CONNECT".equals(method)) {
                    receive.setTLS();
                    if (client instanceof RemoteProxyClient) {
                        //如果是代理链路则需要发送 CONNECT 请求,让服务端响应
                        ByteCacheStream raw = response.getRawData();
                        client.setData(raw.toByteArray());
                    }
                } else {
                    //http请求则全部转发
                    ByteCacheStream raw = response.getRawData();
                    client.setData(raw.toByteArray());
                }
                receive.setRequestSender(client.getSender());
                client.setOnCloseListener(HttpProxyClient.this);
                NioHPCClientFactory.getFactory().addTask(client);
                proxyClient = client;
                requestHost = newRequestHost;
            }
        }
    }

    @Override
    protected void onCloseClientChannel() {
        NioHPCClientFactory.getFactory().removeTask(proxyClient);
        LogDog.d("==> close " + requestHost + " [ remover connect count = " + HttpProxyServer.localConnectCount.decrementAndGet() + " ] ");
    }

    private boolean isNodeReachable(String hostname, int port) {
        Socket socket = null;
        try {
            socket = new Socket();
            socket.setSoTimeout(500);
            socket.connect(new InetSocketAddress(hostname, port), 500);
            return socket.isConnected();
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

}
