package cryption;

import cryption.joggle.IDecryptListener;

import java.util.Base64;

public class Base64Decrypt implements IDecryptListener {

    @Override
    public byte[][] onDecrypt(byte[][] unpack) {
        if (unpack != null) {
            byte[][] decrypt = new byte[unpack.length][];
            for (int index = 0; index < decrypt.length; index++) {
                decrypt[index] = Base64.getDecoder().decode(unpack[index]);
            }
            return decrypt;
        }
        return null;
    }
}
