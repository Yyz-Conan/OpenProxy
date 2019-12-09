package process;


import connect.RequestSender;
import connect.network.nio.NioClientTask;

/**
 * 加密发送者
 */
public abstract class EncryptionSender extends RequestSender {

    public EncryptionSender(NioClientTask clientTask) {
        super(clientTask);
    }

    @Override
    public void sendData(byte[] data) {
        super.sendData(onEncrypt(data));
    }

    abstract byte[] onEncrypt(byte[] src);
}
