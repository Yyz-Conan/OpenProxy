package cryption;

import cryption.joggle.IEncryptTransform;

import java.util.Base64;

public class Base64Encrypt implements IEncryptTransform {

    @Override
    public byte[] onEncrypt(byte[] src) {
        return Base64.getEncoder().encode(src);
    }
}
