package com.open.proxy.cryption;

import com.open.proxy.cryption.joggle.IDecryptTransform;

public class RSADecrypt implements IDecryptTransform {

    @Override
    public byte[] onDecrypt(byte[] unpack) {
        return RSADataEnvoy.getInstance().superCipher(unpack, true, false);
    }
}
