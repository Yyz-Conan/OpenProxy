package cryption;

import cryption.joggle.IDecryptListener;

import java.util.Base64;

public class Base64Decrypt implements IDecryptListener {

    @Override
    public byte[] onDecrypt(byte[] unpack) {
        if (unpack == null) {
            return null;
        }
        return Base64.getDecoder().decode(unpack);
    }
}
