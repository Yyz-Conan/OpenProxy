package process;

import connect.network.nio.NioClientTask;

public class AESSender extends EncryptionSender {

    public AESSender(NioClientTask clientTask) {
        super(clientTask);
    }

    @Override
    byte[] onEncrypt(byte[] src) {
        return AESDataEnvoy.getInstance().encrypt(src);
    }
}
