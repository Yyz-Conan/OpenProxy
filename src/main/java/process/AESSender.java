package process;

public class AESSender extends EncryptionSender {

    @Override
    byte[] onEncrypt(byte[] src) {
        return AESDataEnvoy.getInstance().encrypt(src);
    }
}
