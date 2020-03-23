package process;


import connect.network.nio.NioSender;

import java.io.IOException;

/**
 * 加密发送者
 */
public abstract class EncryptionSender extends NioSender {


    @Override
    public void sendData(byte[] data) throws IOException {
        super.sendData(onEncrypt(data));
    }

    abstract byte[] onEncrypt(byte[] src);
}
