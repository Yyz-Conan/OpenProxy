package com.open.proxy.cryption;


import com.jav.common.log.LogDog;
import com.jav.common.storage.FileHelper;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class RSADataEnvoy {

    /**
     * 公钥
     */
    private RSAPublicKey rsaPublicKey;
    /**
     * 私钥
     */
    private RSAPrivateKey rsaPrivateKey;

    private Cipher mCipher;

    //"RSA/ECB/OAEPWITHSHA-512ANDMGF1PADDING"
    private static final String RSA_ALGORITHM = "RSA";
    private final static String CYPHER = "RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING";

    private static final int MAX_ENCRYPT_BLOCK = 117;
    private static final int MAX_DECRYPT_BLOCK = 128;
    private static final int KEY_SIZE = 1024;

    public void init(String pubicKeyPath, String privateKeyPath) throws NoSuchPaddingException, NoSuchAlgorithmException {
        mCipher = Cipher.getInstance(RSA_ALGORITHM);
        loadPublicKey(pubicKeyPath);
        loadPrivateKey(privateKeyPath);
//        if (rsaPublicKey == null || rsaPrivateKey == null) {
//            //初始化密钥
//            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
//            //密钥长度为64的整数倍，最大是65536
//            keyPairGenerator.initialize(KEY_SIZE, new SecureRandom());
//            // 生成一个密钥对，保存在keyPair中
//            KeyPair keyPair = keyPairGenerator.generateKeyPair();
//            // 公钥
//            rsaPublicKey = (RSAPublicKey) keyPair.getPublic();
////            publicEncodedKey = rsaPublicKey.getEncoded();
//            // 私钥
//            rsaPrivateKey = (RSAPrivateKey) keyPair.getPrivate();
////            privateEncodedKey = rsaPrivateKey.getEncoded();
//
//            //保存公钥
////            savePublicKey(pubicKeyPath);
//            //保存私钥
////            savePrivateKey(privateKeyPath);
//        }
    }


    /**
     * 公钥加密，私钥解密[加解密]
     *
     * @param data      要处理的数据
     * @param isPublic  数据是否用公钥加密
     * @param isEncrypt true为加密操作,false为解密操作
     * @return
     */
    public byte[] superCipher(byte[] data, boolean isPublic, boolean isEncrypt) {
        if (data == null) {
            LogDog.e("## data can not be null !!!");
            return null;
        }
        Key key;
        if (isPublic) {
            key = isEncrypt ? rsaPublicKey : rsaPrivateKey;
        } else {
            key = isEncrypt ? rsaPrivateKey : rsaPublicKey;
        }
        if (key == null || mCipher == null) {
            LogDog.e("## key is null ,init() not called or init() error !!!");
            return null;
        }
        byte[] result = null;
        try {
            result = cipherDoFinal(key, data, isEncrypt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }


    private byte[] cipherDoFinal(Key key, byte[] data, boolean isEncrypt) throws Exception {
        if (isEncrypt) {
            mCipher.init(Cipher.ENCRYPT_MODE, key);
        } else {
            mCipher.init(Cipher.DECRYPT_MODE, key);
        }
        return segment(mCipher, data, isEncrypt);
    }

    /**
     * 分段处理
     *
     * @param cipher
     * @param srcBytes
     * @return
     */
    private byte[] segment(Cipher cipher, byte[] srcBytes, boolean isEncrypt) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int sizeLen = srcBytes.length;
        int segmentNum = isEncrypt ? MAX_ENCRYPT_BLOCK : MAX_DECRYPT_BLOCK;
        int offSet = 0;
        byte[] data;
        try {
            do {
                byte[] cache;
                if (sizeLen - offSet > segmentNum) {
                    cache = cipher.doFinal(srcBytes, offSet, segmentNum);
                } else {
                    cache = cipher.doFinal(srcBytes, offSet, sizeLen - offSet);
                }
                out.write(cache, 0, cache.length);
                offSet += segmentNum;
            } while (sizeLen - offSet > 0);
            data = out.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            data = null;
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return data;
    }

    private void loadPublicKey(String publicKeyPath) {
        try {
            if (FileHelper.isExist(publicKeyPath)) {
                byte[] data = FileHelper.readFileMemMap(publicKeyPath);
                KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(data);
                rsaPublicKey = (RSAPublicKey) keyFactory.generatePublic(keySpec);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadPrivateKey(String privateKeyPath) {
        try {
            if (FileHelper.isExist(privateKeyPath)) {
                byte[] data = FileHelper.readFileMemMap(privateKeyPath);
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(data);
                KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
                rsaPrivateKey = (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
