package process;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class AESDataEnvoy {

    private final String CYPHER = "AES/ECB/PKCS5Padding";

    private final String AES_ALGORITHM = "AES";

    private final byte[] DEFAULT_KEY = "fD*HYc|/c;d309~{".getBytes();

    private static AESDataEnvoy aesDataEnvoy;

    private Cipher cipher;

    private AESDataEnvoy() {
        try {
            cipher = Cipher.getInstance(CYPHER);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static AESDataEnvoy getInstance() {
        if (aesDataEnvoy == null) {
            synchronized (AESDataEnvoy.class) {
                if (aesDataEnvoy == null) {
                    synchronized (AESDataEnvoy.class) {
                        aesDataEnvoy = new AESDataEnvoy();
                    }
                }
            }
        }
        return aesDataEnvoy;
    }

    public byte[] encrypt(byte[] raw) {
        return encrypt(raw, DEFAULT_KEY);
    }

    public byte[] encrypt(byte[] raw, byte[] key) {
        if (raw == null || key == null) {
            return null;
        }
        byte[] encrypted = null;
        try {
//            MessageDigest md = MessageDigest.getInstance("SHA-256");
//            byte[] newKey = md.digest(DEFAULT_KEY);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, AES_ALGORITHM);
            //"算法/模式/补码方式"
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            encrypted = cipher.doFinal(raw);
            encrypted = Base64.getEncoder().encode(encrypted);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return encrypted;
    }

    public byte[] decrypt(byte[] encrypt) {
        return decrypt(encrypt, DEFAULT_KEY);
    }

    public byte[] decrypt(byte[] encrypt, byte[] key) {
        if (encrypt == null || key == null) {
            return null;
        }
        encrypt = Base64.getDecoder().decode(encrypt);
        if (encrypt == null) {
            return null;
        }
        byte[] original = null;
        try {
//            MessageDigest md = MessageDigest.getInstance("SHA-256");
//            byte[] newKey = md.digest(DEFAULT_KEY);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            original = cipher.doFinal(encrypt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return original;
    }
}
