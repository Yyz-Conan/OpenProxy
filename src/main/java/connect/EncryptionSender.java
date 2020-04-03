package connect;


import cryption.DataPacketManger;
import cryption.joggle.IEncryptListener;

import java.io.IOException;

/**
 * 加密发送者
 */
public class EncryptionSender extends CacheNioSender {

    private IEncryptListener listener;

    public EncryptionSender(IEncryptListener listener) {
        this.listener = listener;
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
