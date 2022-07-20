package com.open.proxy.cryption;

import com.open.proxy.OPContext;
import com.open.proxy.cryption.joggle.IEncryptComponent;

public class RSAEncrypt implements IEncryptComponent {

    private RSADataEnvoy mRsaDataEnvoy;

    public RSAEncrypt() {
        mRsaDataEnvoy = new RSADataEnvoy();
    }

    @Override
    public Object getEncrypt() {
        return mRsaDataEnvoy;
    }

    @Override
    public byte[] onEncrypt(byte[] src) {
        RSADataEnvoy rsaDataEnvoy = OPContext.getInstance().getRsaDataEnvoy();
        return rsaDataEnvoy.superCipher(src, true, true);
    }
}
