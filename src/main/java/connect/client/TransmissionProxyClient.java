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
import connect.network.xhttp.entity.XResponse;
import connect.network.xhttp.utils.ByteCacheStream;
import connect.network.xhttp.utils.MultiLevelBuf;
import connect.network.xhttp.utils.XResponseHelper;
import cryption.*;
import cryption.joggle.IDecryptTransform;
import cryption.joggle.IEncryptTransform;
import intercept.ProxyFilterManager;
import log.LogDog;
import track.SpiderEnvoy;
import util.StringEnvoy;
import utils.HtmlGenerator;

import java.nio.channels.SocketChannel;

/**
 * 代理转发客户http请求
 */
public class TransmissionProxyClient extends AbsClient implements INetReceiver<MultiLevelBuf> {

    private String realHost = null;
    private boolean isRestartConnect = false;
    private NioSender localSender;
    private DecryptionReceiver localReceiver;
    private byte[] data;
    private boolean isHttps;
    private boolean isDebug;


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
        isDebug = AnalysisConfig.getInstance().getBooleanValue(ConfigKey.KEY_DEBUG_MODE);
        if (isDebug) {
            SpiderEnvoy.getInstance().startWatchKey(TransmissionProxyClient.this.toString());
        }
    }

    public void enableLocalConnect(String host, int port) {
        setAddress(host, port);
    }

    public void enableProxyConnect(String realHost) {
        this.realHost = realHost;
        String remoteHost = AnalysisConfig.getInstance().getValue(ConfigKey.CONFIG_REMOTE_PROXY_HOST);
        int remotePort = AnalysisConfig.getInstance().getIntValue(ConfigKey.CONFIG_REMOTE_PROXY_PORT);
        setAddress(remoteHost, remotePort);
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
            setReceive(receiver.getHttpReceiver());
            setSender(new EncryptionSender(encryptListener));
        } else {
            NioReceiver receiver = new NioReceiver();
            receiver.setDataReceiver(this);
            setReceive(receiver);
            setSender(new NioSender());
        }

        getSender().setChannel(selectionKey, channel);
        getSender().setSenderFeedback(this);
        localReceiver.setRequestSender(getSender());

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
        if (isRestartConnect) {
            if (getHost().equals(realHost)) {
                //代理地址不能填入
                return;
            }
            //添加需要代理访问的域名
            ProxyFilterManager.getInstance().addProxyHost(realHost);
            if (isDebug) {
                SpiderEnvoy.getInstance().pinKeyProbe(TransmissionProxyClient.this.toString(), "addProxyHost = " + realHost);
            }
        }
    }

    @Override
    public void onReceiveFullData(MultiLevelBuf buf, Throwable e) {
        if (e != null) {
            LogDog.e("++> onReceiveException host = " + getHost() + ":" + getPort());
        }
        //对应NioReceiver
        localSender.sendData(buf);
    }

    @Override
    protected void onConnectError(Throwable throwable) {
        //链接失败，如果不是配置强制不走代理则尝试代理链接
        isRestartConnect = isCanProxy;
        if (isDebug) {
            SpiderEnvoy.getInstance().pinKeyProbe(TransmissionProxyClient.this.toString(),
                    "onConnectError host = " + getHost() + ":" + getPort());
        }
    }

    @Override
    protected void onRecovery() {
        String host = getHost();
        super.onRecovery();
        if (isRestartConnect) {
            LogDog.e("==> Local connection failed, start to try to use proxy, host = " + host);
            isCanProxy = false;
            //配置链接代理服务
            enableProxyConnect(host);
            NioClientFactory.getFactory().addTask(this);
            if (isDebug) {
                SpiderEnvoy.getInstance().pinKeyProbe(TransmissionProxyClient.this.toString(),
                        "RestartConnect host = " + getHost() + ":" + getPort());
            }
        }
        if (isDebug) {
            String report = SpiderEnvoy.getInstance().endWatchKey(TransmissionProxyClient.this.toString());
            LogDog.saveLog(report);
        }
    }

    @Override
    protected void onCloseClientChannel() {
        if (listener != null) {
            listener.onClose(realHost);
        }
    }

}
