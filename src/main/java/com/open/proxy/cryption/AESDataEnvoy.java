package com.open.proxy.cryption;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Base64;

public class AESDataEnvoy {

    private final String CYPHER = "AES/ECB/PKCS5Padding";

    private final String AES_ALGORITHM = "AES";

    private final byte[] DEFAULT_KEY = "fD*HYc|/c;d309~p".getBytes();

    private byte[] mConfigKey = null;

    private Cipher cipher;

    public AESDataEnvoy() {
        try {
            cipher = Cipher.getInstance(CYPHER);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void initKey(byte[] key) {
        mConfigKey = key;
    }

    public byte[] encrypt(byte[] raw) {
        byte[] key = DEFAULT_KEY;
        if (mConfigKey != null) {
            key = mConfigKey;
        }
        return encrypt(raw, key);
    }

    public byte[] encrypt(byte[] raw, byte[] key) {
        if (raw == null || key == null) {
            return null;
        }
        byte[] encrypted = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] newKey = md.digest(key);
            SecretKeySpec secretKeySpec = new SecretKeySpec(newKey, AES_ALGORITHM);
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
        byte[] key = DEFAULT_KEY;
        if (mConfigKey != null) {
            key = mConfigKey;
        }
        return decrypt(encrypt, key);
    }

    public byte[] decrypt(byte[] encrypt, byte[] key) {
        if (encrypt == null || key == null) {
            return null;
        }
        byte[] original = null;
        try {
            original = Base64.getDecoder().decode(encrypt);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] newKey = md.digest(key);
            SecretKeySpec secretKeySpec = new SecretKeySpec(newKey, AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            original = cipher.doFinal(original);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return original;
    }
}
