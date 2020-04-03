package cryption;

import cryption.joggle.IDecryptListener;

public class RSADecrypt implements IDecryptListener {

    @Override
    public byte[][] onDecrypt(byte[][] unpack) {
        if (unpack != null) {
            byte[][] decrypt = new byte[unpack.length][];
            for (int index = 0; index < decrypt.length; index++) {
                decrypt[index] = RSADataEnvoy.getInstance().superCipher(unpack[index], true, false);
            }
            return decrypt;
        }
        return null;
    }
}
