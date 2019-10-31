package process;

import connect.network.nio.NioClientTask;

public class RSASender extends EncryptionSender {

    public RSASender(NioClientTask clientTask) {
        super(clientTask);
    }

    @Override
    byte[] onEncrypt(byte[] src) {
        return RSADataEnvoy.getInstance().superCipher(src, true, true);
    }
}
