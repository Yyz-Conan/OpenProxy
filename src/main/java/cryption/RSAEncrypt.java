package cryption;

import cryption.joggle.IEncryptListener;

public class RSAEncrypt implements IEncryptListener {

    @Override
    public byte[] onEncrypt(byte[] src) {
        return RSADataEnvoy.getInstance().superCipher(src, true, true);
    }
}
