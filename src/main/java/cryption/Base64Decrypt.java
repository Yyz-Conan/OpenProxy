package cryption;

import cryption.joggle.IDecryptTransform;

import java.util.Base64;

public class Base64Decrypt implements IDecryptTransform {

    @Override
    public byte[] onDecrypt(byte[] unpack) {
        if (unpack == null) {
            return null;
        }
        return Base64.getDecoder().decode(unpack);
    }
}
