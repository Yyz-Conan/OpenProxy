package connect.client;

import config.AnalysisConfig;
import config.ConfigKey;
import connect.AbsClient;
import connect.DecryptionReceiver;
import connect.EncryptionSender;
import connect.network.base.joggle.INetReceiver;
import connect.network.nio.NioClientFactory;
import connect.network.nio.NioReceiver;
import connect.network.nio.NioSender;
import connect.network.nio.buf.MultilevelBuf;
import connect.network.xhttp.entity.XResponse;
import connect.network.xhttp.utils.ByteCacheStream;
import connect.network.xhttp.utils.XResponseHelper;
import cryption.*;
import cryption.joggle.IDecryptTransform;
import cryption.joggle.IEncryptTransform;
import intercept.ProxyFilterManager;
import log.LogDog;
import util.StringEnvoy;
import utils.HtmlGenerator;

import java.nio.channels.SocketChannel;

/**
 * 代理转发客户http请求
 */
public class TransmissionProxyClient extends AbsClient implements INetReceiver<MultilevelBuf> {

    private String realHost = null;
    private boolean isRestartConnect = false;
    private NioSender localSender;
    private DecryptionReceiver localReceiver;
    private byte[] data;
    private boolean isHttps;

//    /**
//     * 不走代理服务
//     *
//     * @param host
//     * @param port
//     * @param sender
//     */
//    public TransmissionProxyClient(String host, int port, NioSender sender, DecryptionReceiver receiver, XResponse response) {
//        if (sender == null || receiver == null || host == null || port <= 0) {
//            throw new NullPointerException("host port  sender or receiver is null !!!");
//        }
//        this.localSender = sender;
//        this.localReceiver = receiver;
//        setAddress(host, port, false);
//        isHttps = XResponseHelper.isTLS(response);
//        ByteCacheStream raw = response.getRawData();
//        data = raw.toByteArray();
//    }

    /**
     * 走代理服务
     *
     * @param sender
     */
    public TransmissionProxyClient(NioSender sender, DecryptionReceiver receiver, XResponse response) {
        if (sender == null || receiver == null) {
            throw new NullPointerException("sender or receiver is null !!!");
        }
        this.localSender = sender;
        this.localReceiver = receiver;
        isHttps = XResponseHelper.isTLS(response);
        ByteCacheStream raw = response.getRawData();
        data = raw.toByteArray();

    }

    public void enableLocalConnect(String host, int port) {
        setAddress(host, port, false);
    }

    public void enableProxyConnect(String realHost) {
        this.realHost = realHost;
        String remoteHost = AnalysisConfig.getInstance().getValue(ConfigKey.CONFIG_REMOTE_HOST);
        int remotePort = AnalysisConfig.getInstance().getIntValue(ConfigKey.CONFIG_REMOTE_PORT);
        setAddress(remoteHost, remotePort, false);
    }


    @Override
    protected void onConnectCompleteChannel(SocketChannel channel) {
        //链接成功
        if (StringEnvoy.isNotEmpty(realHost)) {
            String encryption = AnalysisConfig.getInstance().getValue(ConfigKey.CONFIG_ENCRYPTION_MODE);

            IDecryptTransform decryptListener = null;
            IEncryptTransform encryptListener = null;

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

            //创建解密接收者
            DecryptionReceiver receiver = new DecryptionReceiver(decryptListener);
            //配置工作模式为客户端模式
            receiver.setResponseSender(localSender);
            setReceive(receiver);
            setSender(new EncryptionSender(encryptListener));
        } else {
            setReceive(new NioReceiver<>(this));
            setSender(new NioSender());
        }

        localReceiver.setRequestSender(getSender());
        getSender().setSenderFeedback(this);
        getSender().setChannel(channel);

        if (StringEnvoy.isNotEmpty(realHost) || isHttps) {
            //如果当前是https请求或者走代理请求
            localReceiver.setTLS();
        }
        if (StringEnvoy.isEmpty(realHost) && isHttps) {
            //当前是https请求而且不走代理请求，则响应代理请求
            localSender.sendData(HtmlGenerator.httpsTunnelEstablished());
        } else {
            getSender().sendData(data);
        }
    }

    @Override
    public void onReceiveFullData(MultilevelBuf buf) {
        //对应NioReceiver
        localSender.sendData(buf);
    }

    @Override
    public void onReceiveException(Exception e) {
        LogDog.e("++> onReceiveException host = " + getHost() + ":" + getPort());
    }

    @Override
    protected void onConnectError() {
        //链接失败，如果不是配置强制不走代理则尝试代理链接
        isRestartConnect = isCanProxy;
    }

    @Override
    protected void onRecovery() {
        if (isRestartConnect) {
            LogDog.e("==> Local connection failed, start to try to use proxy, host = " + getHost());
            isCanProxy = false;
            //添加需要代理访问的域名
            ProxyFilterManager.getInstance().addProxyHost(getHost());
            enableProxyConnect(getHost());
            //复用本类，准换走代理服务
            NioClientFactory.getFactory().addTask(this);
        }
    }

    @Override
    protected void onCloseClientChannel() {
        if (listener != null) {
            listener.onClose(realHost);
        }
    }


}
