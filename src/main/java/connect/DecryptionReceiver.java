package connect;

import connect.network.base.joggle.INetReceiver;
import connect.network.base.joggle.INetSender;
import connect.network.xhttp.ByteCacheStream;
import connect.network.xhttp.entity.XReceiverMode;
import connect.network.xhttp.entity.XResponse;
import cryption.DataPacketManger;
import cryption.joggle.IDecryptListener;

/**
 * 解密接收者
 */
public class DecryptionReceiver extends LocalRequestReceiver {

    private ByteCacheStream cacheStream;
    private IDecryptListener listener;
    private INetSender localTarget;

    public DecryptionReceiver(IDecryptListener listener) {
        super(null);
        if (listener != null) {
            this.listener = listener;
            cacheStream = new ByteCacheStream();
        }
    }

    public INetSender getLocalTarget() {
        return localTarget;
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
    protected void onInterceptReceive(byte[] data, Exception e) throws Exception {
        if (data != null) {
            byte[][] decrypt = null;
            if (listener != null) {
                cacheStream.write(data);
                byte[][] unpack = DataPacketManger.unpack(cacheStream.getBuf(), cacheStream.size());
                if (unpack == null) {
                    return;
                }
                decrypt = listener.onDecrypt(unpack);
            }
            if (decrypt == null) {
                decrypt = new byte[][]{data};
            }
            for (byte[] tmp : decrypt) {
                if (getMode() == XReceiverMode.REQUEST) {
                    super.onHttpReceive(tmp, tmp.length, e);
                } else {
                    localTarget.sendData(tmp);
                }
            }
            if (listener != null) {
                cacheStream.reset();
            }
        }
    }
}
