package cryption;

import cryption.joggle.IEncryptTransform;

public class RSAEncrypt implements IEncryptTransform {

    @Override
    public byte[] onEncrypt(byte[] src) {
        return RSADataEnvoy.getInstance().superCipher(src, true, true);
    }
}
