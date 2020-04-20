package connect;


import connect.network.nio.buf.MultilevelBuf;
import cryption.DataPacketManger;
import cryption.joggle.IEncryptTransform;

import java.io.IOException;

/**
 * 加密发送者
 */
public class EncryptionSender extends CacheNioSender {

    private IEncryptTransform listener;

    public EncryptionSender(IEncryptTransform listener) {
        this.listener = listener;
    }

    @Override
    public void sendData(MultilevelBuf buf) throws IOException {
        if (listener != null) {
            buf.flip();
            byte[] data = buf.array();
            sendData(data);
        } else {
            super.sendData(buf);
        }
    }

    @Override
    public void sendData(byte[] data) throws IOException {
        if (data == null) {
            return;
        }
        if (listener != null) {
            byte[] encrypt = listener.onEncrypt(data);
            data = DataPacketManger.packet(encrypt);
        }
        sendDataImp(data);
    }

}
