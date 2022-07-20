package com.open.proxy.cryption;


import com.open.proxy.cryption.joggle.EncryptionType;
import com.open.proxy.cryption.joggle.IDecryptComponent;
import com.open.proxy.cryption.joggle.IEncryptComponent;

public class DataSafeManager {

    private IDecryptComponent mDecrypt = null;
    private IEncryptComponent mEncrypt = null;

    public void init(EncryptionType encryption) {
        switch (encryption) {
            case BASE64:
                mDecrypt = new Base64Decrypt();
                mEncrypt = new Base64Encrypt();
                break;
            case RSA:
                mDecrypt = new RSADecrypt();
                mEncrypt = new RSAEncrypt();
                break;
            case AES:
                mDecrypt = new AESDecrypt();
                mEncrypt = new AESEncrypt();
                break;
        }
    }

    public IDecryptComponent getDecrypt() {
        return mDecrypt;
    }

    public IEncryptComponent getEncrypt() {
        return mEncrypt;
    }

    public boolean isInit() {
        return mDecrypt != null && mEncrypt != null;
    }

    public byte[] encode(byte[] src) {
        if (mEncrypt == null) {
            return src;
        }
        return mEncrypt.onEncrypt(src);
    }

    public byte[] decode(byte[] encode) {
        if (mDecrypt == null) {
            return encode;
        }
        return mDecrypt.onDecrypt(encode);
    }
}
