package connect;


import connect.network.nio.NioSender;
import connect.network.xhttp.XMultiplexCacheManger;
import connect.network.xhttp.utils.MultilevelBuf;
import cryption.joggle.IEncryptTransform;
import util.TypeConversion;
import utils.DataPacketManger;

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
            //send protocol head
            super.sendData(DataPacketManger.PACK_PROXY_TAG);
            //send data length
            byte[] length = TypeConversion.intToByte(encrypt.length);
            super.sendData(length);
            //send data
            super.sendData(encrypt);
        } else {
            super.sendData(data);
        }
    }

}
