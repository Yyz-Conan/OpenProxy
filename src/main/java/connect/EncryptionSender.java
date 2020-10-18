package connect;


import connect.network.nio.NioSender;
import connect.network.nio.buf.MultilevelBuf;
import connect.network.xhttp.XMultiplexCacheManger;
import cryption.DataPacketManger;
import cryption.joggle.IEncryptTransform;

/**
 * 加密发送者
 */
public class EncryptionSender extends NioSender {

    private IEncryptTransform listener;

    public EncryptionSender(IEncryptTransform listener) {
        this.listener = listener;
    }

    @Override
    public void sendData(MultilevelBuf buf) {
        if (listener != null) {
            buf.flip();
            byte[] data = buf.array();
            sendData(data);
            XMultiplexCacheManger.getInstance().lose(buf);
        } else {
            super.sendData(buf);
        }
    }

    @Override
    public void sendData(byte[] data) {
        if (data == null) {
            return;
        }
        if (listener != null) {
            byte[] encrypt = listener.onEncrypt(data);
            data = DataPacketManger.packet(encrypt);
        }
        super.sendData(data);
    }

}
