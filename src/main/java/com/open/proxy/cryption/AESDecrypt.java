package com.open.proxy.cryption;


import com.open.proxy.cryption.joggle.IDecryptComponent;

public class AESDecrypt implements IDecryptComponent {

    private AESDataEnvoy mAesDataEnvoy;

    public AESDecrypt() {
        mAesDataEnvoy = new AESDataEnvoy();
    }

    @Override
    public Object getEncrypt() {
        return mAesDataEnvoy;
    }

    @Override
    public byte[] onDecrypt(byte[] unpack) {
        return mAesDataEnvoy.decrypt(unpack);
    }
}
