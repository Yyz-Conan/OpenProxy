package com.open.proxy.cryption;

import com.open.proxy.OPContext;
import com.open.proxy.cryption.joggle.IEncryptTransform;

public class RSAEncrypt implements IEncryptTransform {

    @Override
    public byte[] onEncrypt(byte[] src) {
        RSADataEnvoy rsaDataEnvoy = OPContext.getInstance().getRsaDataEnvoy();
        return rsaDataEnvoy.superCipher(src, true, true);
    }
}
