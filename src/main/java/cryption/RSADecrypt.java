package cryption;

import cryption.joggle.IDecryptListener;

public class RSADecrypt implements IDecryptListener {

    @Override
    public byte[] onDecrypt(byte[] unpack) {
        return RSADataEnvoy.getInstance().superCipher(unpack, true, false);
    }
}
