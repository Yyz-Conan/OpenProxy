package com.open.proxy.cryption;

import com.open.proxy.cryption.joggle.IEncryptComponent;

public class AESEncrypt implements IEncryptComponent {

    private AESDataEnvoy mAesDataEnvoy;

    public AESEncrypt() {
        mAesDataEnvoy = new AESDataEnvoy();
    }

    @Override
    public Object getEncrypt() {
        return mAesDataEnvoy;
    }

    @Override
    public byte[] onEncrypt(byte[] src) {
        return mAesDataEnvoy.encrypt(src);
    }
}
