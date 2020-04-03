package connect.clinet;


import config.AnalysisConfig;
import connect.AbsClient;
import connect.DecryptionReceiver;
import connect.EncryptionSender;
import connect.network.base.joggle.INetSender;
import connect.network.nio.NioHPCClientFactory;
import cryption.*;
import cryption.joggle.IDecryptListener;
import cryption.joggle.IEncryptListener;
import log.LogDog;

import java.nio.channels.SocketChannel;

/**
 * 客户端模式-链接代理端
 */
public class RemoteProxyClient extends AbsClient {

    private String realHost = null;

    public RemoteProxyClient(INetSender localSender, String host, int port) {
        setAddress(host, port, false);

        String encryption = AnalysisConfig.getInstance().getValue("encryptionMode");

        IDecryptListener decryptListener = null;
        IEncryptListener encryptListener = null;

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
    }

    public void setRealHost(String realHost) {
        this.realHost = realHost;
    }


    @Override
    protected void onCloseClientChannel() {
        if (listener != null) {
            listener.onClose(realHost);
        }
    }


    @Override
    protected void onConnectCompleteChannel(SocketChannel channel) {
        getSender().setChannel(channel);
        try {
            if (data != null) {
                //当前是http请求
                getSender().sendData(data);
                data = null;
            }
        } catch (Exception e) {
            LogDog.e("==> host = " + getHost() + " port = " + getPort());
            NioHPCClientFactory.getFactory().removeTask(this);
            e.printStackTrace();
        }
    }

}
