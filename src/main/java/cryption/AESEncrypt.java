package cryption;

import cryption.joggle.IEncryptListener;

public class AESEncrypt implements IEncryptListener {

    @Override
    public byte[] onEncrypt(byte[] src) {
        return AESDataEnvoy.getInstance().encrypt(src);
    }
}
