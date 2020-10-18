package connect.client;

import config.AnalysisConfig;
import config.ConfigKey;
import connect.DecryptionReceiver;
import connect.EncryptionSender;
import connect.joggle.ICloseListener;
import connect.network.base.TaskStatus;
import connect.network.base.joggle.INetReceiver;
import connect.network.base.joggle.INetSender;
import connect.network.base.joggle.ISenderFeedback;
import connect.network.nio.NioClientFactory;
import connect.network.nio.NioClientTask;
import connect.network.nio.buf.MultilevelBuf;
import connect.network.xhttp.XMultiplexCacheManger;
import connect.network.xhttp.entity.XResponse;
import connect.network.xhttp.utils.ByteCacheStream;
import connect.network.xhttp.utils.XResponseHelper;
import connect.server.MultipleProxyServer;
import cryption.*;
import cryption.joggle.IDecryptTransform;
import cryption.joggle.IEncryptTransform;
import intercept.InterceptFilterManager;
import intercept.ProxyFilterManager;
import log.LogDog;
import util.StringEnvoy;
import utils.HtmlGenerator;

import java.nio.channels.SocketChannel;

/**
 * 接待需要代理的客户端
 */
public class ReceptionProxyClient extends NioClientTask implements ICloseListener, ISenderFeedback, INetReceiver<XResponse> {

    private String requestHost = null;
    private NioClientTask transmissionProxyClient;
    private boolean isServerMode;

    public ReceptionProxyClient(SocketChannel channel) {
        super(channel, null);

        IDecryptTransform decryptListener = null;
        IEncryptTransform encryptListener = null;

        isServerMode = AnalysisConfig.getInstance().getBooleanValue(ConfigKey.CONFIG_IS_SERVER_MODE);
        if (isServerMode) {
            //如果是运行在服务模式则开启数据加密
            String encryption = AnalysisConfig.getInstance().getValue(ConfigKey.CONFIG_ENCRYPTION_MODE);
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

        //创建接收者
        DecryptionReceiver receiver = new DecryptionReceiver(decryptListener);
        //设置数据回调接口
        receiver.setDataReceiver(this);
        setReceive(receiver);
        //设置发送者
        EncryptionSender sender = new EncryptionSender(encryptListener);
        //设置发送数据状态回调
        sender.setSenderFeedback(this);
        setSender(sender);
    }

    @Override
    protected void onConnectCompleteChannel(SocketChannel channel) throws Exception {
        super.onConnectCompleteChannel(channel);
        getSender().setChannel(selectionKey, channel);
    }

    @Override
    public void onReceiveFullData(XResponse response, Throwable e) {
        String newRequestHost = XResponseHelper.getHost(response);
        if (StringEnvoy.isEmpty(newRequestHost) && StringEnvoy.isEmpty(requestHost)) {
            LogDog.e("==> illegal request host = " + newRequestHost);
            requestHost = newRequestHost;
            NioClientFactory.getFactory().removeTask(ReceptionProxyClient.this);
            return;
        }

        //黑名单过滤
        if (InterceptFilterManager.getInstance().isIntercept(newRequestHost)) {
//                LogDog.e("拦截黑名单 host = " + requestHost);
            if (XResponseHelper.isTLS(response)) {
                getSender().sendData(HtmlGenerator.httpsTunnelEstablished());
            }
            requestHost = newRequestHost;
            getSender().sendData(HtmlGenerator.createInterceptHtml(newRequestHost));
            NioClientFactory.getFactory().removeTask(this);
            return;
        }

        if (StringEnvoy.isNotEmpty(requestHost) && !newRequestHost.equals(requestHost) && isServerMode) {
            //发现当前请求的网站跟上次请求的网站不一样，则关闭之前的链接（只限制运行于服务模式）
            NioClientFactory.getFactory().removeTask(transmissionProxyClient);
            transmissionProxyClient = null;
        }

        if (transmissionProxyClient != null) {
            //如果是同个域名请求则复用链路
            ByteCacheStream raw = response.getRawData();
            INetSender sender = transmissionProxyClient.getSender();
            if (sender != null) {
                sender.sendData(raw.toByteArray());
            }
//            LogDog.d("Browser initiated request = " + raw.toString());
        } else {
            LogDog.d("==> Connect " + newRequestHost);
//            ByteCacheStream stream = response.getRawData();
//            LogDog.d("Browser initiated request = " + stream.toString());
            TransmissionProxyClient client;
            int port = XResponseHelper.getPort(response);
            if (isServerMode) {
                //当前是服务模式，请求指定的域名，需要响应 connect 请求
                client = new TransmissionProxyClient(getSender(), getReceiver(), response);
                client.enableLocalConnect(newRequestHost, port);
            } else {
                //当前是客户端模式
                boolean isNeedProxy = ProxyFilterManager.getInstance().isNeedProxy(newRequestHost);
                if (isNeedProxy) {
                    //走代理服务访问
                    client = new TransmissionProxyClient(getSender(), getReceiver(), response);
                    client.enableProxyConnect(newRequestHost);
                } else {
                    boolean isNoProxy = ProxyFilterManager.getInstance().isNoProxy(newRequestHost);
                    //先尝试使用本地网络,如果不可以则不走代理，需要响应 connect 请求
                    client = new TransmissionProxyClient(getSender(), getReceiver(), response);
                    client.enableLocalConnect(newRequestHost, port);
                    //如果本地网络不能用则走代理
                    client.setCanProxy(!isNoProxy);
                }
            }

            client.setOnCloseListener(this);
            NioClientFactory.getFactory().addTask(client);
            transmissionProxyClient = client;
            requestHost = newRequestHost;
        }
    }


    @Override
    public void onClose(String host) {
        if (StringEnvoy.isNotEmpty(host) && host.equals(requestHost) && getTaskStatus() == TaskStatus.RUN) {
            NioClientFactory.getFactory().removeTask(ReceptionProxyClient.this);
        }
    }

    @Override
    public void onSenderFeedBack(INetSender iNetSender, Object data, Throwable throwable) {
        if (throwable != null) {
            NioClientFactory.getFactory().removeTask(transmissionProxyClient);
        }
        if (data instanceof MultilevelBuf) {
            XMultiplexCacheManger.getInstance().lose((MultilevelBuf) data);
        }
    }

    @Override
    protected void onCloseClientChannel() {
        if (requestHost == null) {
            LogDog.d("==> browser heartbeat !!!" + " [ connect count = " + MultipleProxyServer.localConnectCount.decrementAndGet() + " ] ");
        } else {
            LogDog.d("==> close " + requestHost + " [ connect count = " + MultipleProxyServer.localConnectCount.decrementAndGet() + " ] ");
        }
        NioClientFactory.getFactory().removeTask(transmissionProxyClient);
    }

}
