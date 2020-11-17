package cryption;

import cryption.joggle.IEncryptTransform;

public class AESEncrypt implements IEncryptTransform {

    @Override
    public byte[] onEncrypt(byte[] src) {
        return AESDataEnvoy.getInstance().encrypt(src);
    }
}
