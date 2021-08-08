package cryption;

import config.AnalysisConfig;
import config.ConfigKey;
import cryption.joggle.IDecryptTransform;
import cryption.joggle.IEncryptTransform;

public class DataSafeManager {

    public static DataSafeManager getInstance() {
        return DataSafeManagerHolder.INSTANCE;
    }

    private static class DataSafeManagerHolder {
        private static final DataSafeManager INSTANCE = new DataSafeManager();
    }

    private IDecryptTransform decryptTransform = null;
    private IEncryptTransform encryptTransform = null;

    public void init() {
        String encryption = AnalysisConfig.getInstance().getValue(ConfigKey.CONFIG_ENCRYPTION_MODE);
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
