package process;

public class RSASender extends EncryptionSender {

    @Override
    byte[] onEncrypt(byte[] src) {
        return RSADataEnvoy.getInstance().superCipher(src, true, true);
    }
}
