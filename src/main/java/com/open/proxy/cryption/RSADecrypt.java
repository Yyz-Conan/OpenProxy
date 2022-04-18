package com.open.proxy.cryption;

import com.open.proxy.OPContext;
import com.open.proxy.cryption.joggle.IDecryptTransform;

public class RSADecrypt implements IDecryptTransform {

    @Override
    public byte[] onDecrypt(byte[] unpack) {
        RSADataEnvoy rsaDataEnvoy = OPContext.getInstance().getRsaDataEnvoy();
        return rsaDataEnvoy.superCipher(unpack, true, false);
    }
}
