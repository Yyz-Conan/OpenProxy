package com.open.proxy.cryption;

import com.open.proxy.IConfigKey;
import com.open.proxy.OPContext;
import com.open.proxy.cryption.joggle.IDecryptTransform;
import com.open.proxy.cryption.joggle.IEncryptTransform;
import util.ConfigFileEnvoy;

public class DataSafeManager {

    public static DataSafeManager getInstance() {
        return DataSafeManagerHolder.INSTANCE;
    }

    private static final class DataSafeManagerHolder {
        private static final DataSafeManager INSTANCE = new DataSafeManager();
    }

    private IDecryptTransform decryptTransform = null;
    private IEncryptTransform encryptTransform = null;

    public void init() {
        ConfigFileEnvoy configFileEnvoy = OPContext.getInstance().getConfigFileEnvoy();
        String encryption = configFileEnvoy.getValue(IConfigKey.CONFIG_ENCRYPTION_MODE);
        if (EncryptionType.RSA.name().equals(encryption)) {
            decryptTransform = new RSADecrypt();
            encryptTransform = new RSAEncrypt();
        } else if (EncryptionType.AES.name().equals(encryption)) {
            decryptTransform = new AESDecrypt();
            encryptTransform = new AESEncrypt();
        } else if (EncryptionType.BASE64.name().equals(encryption)) {
            decryptTransform = new Base64Decrypt();
            encryptTransform = new Base64Encrypt();
        }
    }

    public boolean isEnable() {
        return decryptTransform != null && encryptTransform != null;
    }

    public byte[] encode(byte[] src) {
        if (encryptTransform == null) {
            return src;
        }
        return encryptTransform.onEncrypt(src);
    }

    public byte[] decode(byte[] encode) {
        if (decryptTransform == null) {
            return encode;
        }
        return decryptTransform.onDecrypt(encode);
    }
}
