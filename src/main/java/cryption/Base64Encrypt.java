package cryption;

import cryption.joggle.IEncryptListener;

import java.util.Base64;

public class Base64Encrypt implements IEncryptListener {

    @Override
    public byte[] onEncrypt(byte[] src) {
        return Base64.getEncoder().encode(src);
    }
}
