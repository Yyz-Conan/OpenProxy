package process;


import connect.network.nio.NioHPCSender;

/**
 * 加密发送者
 */
public abstract class EncryptionSender extends NioHPCSender {

    @Override
    public void sendData(byte[] data) {
        super.sendDataNow(onEncrypt(data));
    }

    @Override
    public void sendDataNow(byte[] data) {
        super.sendDataNow(onEncrypt(data));
    }


    abstract byte[] onEncrypt(byte[] src);
}
