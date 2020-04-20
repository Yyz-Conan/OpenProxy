package connect;

import connect.network.base.joggle.INetReceiver;
import connect.network.base.joggle.INetSender;
import connect.network.xhttp.entity.XReceiverMode;
import connect.network.xhttp.entity.XResponse;
import cryption.DecryptionStatus;
import cryption.joggle.IDecryptTransform;
import util.IoEnvoy;
import util.TypeConversion;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * 解密接收者
 */
public class DecryptionReceiver extends LocalRequestReceiver {

    private IDecryptTransform decryptTransform;
    private INetSender localTarget;
    private ByteBuffer tag;
    private ByteBuffer packetData;
    private DecryptionStatus decryptionStatus;

    public DecryptionReceiver(IDecryptTransform decryptTransform) {
        super(null);
        if (decryptTransform != null) {
            this.decryptTransform = decryptTransform;
            tag = ByteBuffer.allocate(4);
            decryptionStatus = DecryptionStatus.TAG;
        }
    }

    /**
     * 工作于服务端模式才配置该方法
     *
     * @param receiver
     */
    @Override
    public void setReceiver(INetReceiver<XResponse> receiver) {
        super.setReceiver(receiver);
        setMode(XReceiverMode.REQUEST);
    }

    /**
     * 工作于客户端模式，设置发送者接收tls数据
     *
     * @param localTarget
     */
    public void setResponseSender(INetSender localTarget) {
        this.localTarget = localTarget;
        setMode(XReceiverMode.RESPONSE);
    }

    @Override
    protected void onRead(SocketChannel channel) throws Exception {
        if (decryptTransform != null) {
            if (decryptionStatus == DecryptionStatus.TAG) {
                tag.clear();
                int ret = IoEnvoy.readToFull(channel, tag);
                if (ret == IoEnvoy.SUCCESS) {
                    int packetSize = TypeConversion.byteToInt(tag.array(), 0);
                    if (packetSize > 0) {
                        packetData = ByteBuffer.allocate(packetSize);
                        decryptionStatus = DecryptionStatus.DATA;
                    }
                } else if (ret == IoEnvoy.FAIL) {
                    throw new IOException("SocketChannel close !!!");
                }
            }
            if (decryptionStatus == DecryptionStatus.DATA) {
                int ret = IoEnvoy.readToFull(channel, packetData);
                if (ret == IoEnvoy.SUCCESS) {
                    byte[] decrypt = decryptTransform.onDecrypt(packetData.array());
                    if (getMode() == XReceiverMode.REQUEST) {
                        super.onHttpReceive(decrypt, decrypt.length, null);
                    } else {
                        localTarget.sendData(decrypt);
                    }
                    packetData = null;
                    decryptionStatus = DecryptionStatus.END;
                } else if (ret == IoEnvoy.FAIL) {
                    throw new IOException("SocketChannel close !!!");
                }
            }
            if (decryptionStatus == DecryptionStatus.END) {
                tag.clear();
                int ret = IoEnvoy.readToFull(channel, tag);
                if (ret == IoEnvoy.SUCCESS) {
                    decryptionStatus = DecryptionStatus.TAG;
                } else if (ret == IoEnvoy.FAIL) {
                    throw new IOException("SocketChannel close !!!");
                }
            }
        } else {
            super.onRead(channel);
        }
    }

}
