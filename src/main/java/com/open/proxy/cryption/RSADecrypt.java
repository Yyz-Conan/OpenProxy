package com.open.proxy.cryption;

import com.open.proxy.cryption.joggle.IDecryptComponent;

public class RSADecrypt implements IDecryptComponent {

    private RSADataEnvoy mRsaDataEnvoy;

    public RSADecrypt() {
        mRsaDataEnvoy = new RSADataEnvoy();
    }

    @Override
    public Object getEncrypt() {
        return mRsaDataEnvoy;
    }

    @Override
    public byte[] onDecrypt(byte[] unpack) {
        return mRsaDataEnvoy.superCipher(unpack, true, false);
    }
}
