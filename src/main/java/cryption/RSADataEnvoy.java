package cryption;

import log.LogDog;
import storage.FileHelper;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.*;
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

    private byte[] publicEncodedKey;

    private byte[] privateEncodedKey;

    private Cipher cipher;

    //"RSA/ECB/OAEPWITHSHA-512ANDMGF1PADDING"
    private static String RSA_ALGORITHM = "RSA";
    private final static String CYPHER = "RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING";
    private static RSADataEnvoy dataPolicy = null;

    private static final int MAX_ENCRYPT_BLOCK = 117;
    private static final int MAX_DECRYPT_BLOCK = 128;
    private static final int keySize = 1024;

    public static RSADataEnvoy getInstance() {
        synchronized (RSADataEnvoy.class) {
            if (dataPolicy == null) {
                synchronized (RSADataEnvoy.class) {
                    if (dataPolicy == null) {
                        dataPolicy = new RSADataEnvoy();
                    }
                }
            }
        }
        return dataPolicy;
    }

    private RSADataEnvoy() {

    }

    public void init(String pubicKeyPath, String privateKeyPath) {
        loadPublicKey(pubicKeyPath);
        loadPrivateKey(privateKeyPath);
        try {
            if (rsaPublicKey == null || rsaPrivateKey == null) {
                //初始化密钥
                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
                //密钥长度为64的整数倍，最大是65536
                keyPairGenerator.initialize(keySize, new SecureRandom());
                // 生成一个密钥对，保存在keyPair中
                KeyPair keyPair = keyPairGenerator.generateKeyPair();
                // 公钥
                rsaPublicKey = (RSAPublicKey) keyPair.getPublic();
                publicEncodedKey = rsaPublicKey.getEncoded();
                // 私钥
                rsaPrivateKey = (RSAPrivateKey) keyPair.getPrivate();
                privateEncodedKey = rsaPrivateKey.getEncoded();

                //保存公钥
                savePublicKey(pubicKeyPath);
                //保存私钥
                savePrivateKey(privateKeyPath);
            }
            cipher = Cipher.getInstance(RSA_ALGORITHM);
        } catch (Exception e) {
            e.printStackTrace();
        }
//        System.out.println("RSA公钥：" + Base64Helper.getHelper().encodeToString(publicEncodedKey));
//        System.out.println("RSA私钥：" + Base64Helper.getHelper().encodeToString(privateEncodedKey));
    }


    /**
     * 公钥加密，私钥解密【加解密】
     *
     * @param data      要处理的数据
     * @param isPublic  数据是否用公钥加密
     * @param isEncrypt true为加密操作
     * @return
     */
    public byte[] superCipher(byte[] data, boolean isPublic, boolean isEncrypt) {
        if (data == null) {
            LogDog.e("## data can not be null !!!");
            return null;
        }
        if (rsaPublicKey == null || rsaPrivateKey == null) {
            LogDog.e("## init() not called or init error !!!");
            return null;
        }
        byte[] result = null;
        try {
            Key key;
            if (isPublic) {
                key = isEncrypt ? rsaPublicKey : rsaPrivateKey;
            } else {
                key = isEncrypt ? rsaPrivateKey : rsaPublicKey;
            }
            result = cipherDoFinal(key, data, isEncrypt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }


//    /**
//     * 公钥加密，私钥解密【加密】
//     *
//     * @param src
//     * @return
//     */
//    public byte[] publicEncrypt(byte[] src) {
//        boolean isError = isHasError(src);
//        if (isError) {
//            return null;
//        }
//        byte[] result = null;
//        try {
//            result = cipherDoFinal(rsaPublicKey, src, true);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return result;
//    }
//
//    /**
//     * 公钥加密，私钥解密【解密】
//     *
//     * @return
//     */
//    public byte[] publicDecrypt(byte[] encrypt) {
//        boolean isError = isHasError(encrypt);
//        if (isError) {
//            return null;
//        }
//        byte[] result = null;
//        try {
//            result = cipherDoFinal(rsaPrivateKey, encrypt, false);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return result;
//    }


//    /**
//     * 私钥加密，公钥解密【加密】
//     *
//     * @param src
//     * @return
//     */
//    public byte[] privateEncrypt(byte[] src) {
//        boolean isError = isHasError(src);
//        if (isError) {
//            return null;
//        }
//        byte[] result = null;
//        try {
//            result = cipherDoFinal(rsaPrivateKey, src, true);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return result;
//    }
//
//
//    /**
//     * 私钥加密，公钥解密【解密】
//     *
//     * @param encrypt
//     * @return
//     */
//    public byte[] privateDecrypt(byte[] encrypt) {
//        boolean isError = isHasError(encrypt);
//        if (isError) {
//            return null;
//        }
//        byte[] result = null;
//        try {
//            result = cipherDoFinal(rsaPublicKey, encrypt, false);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return result;
//    }

    private byte[] cipherDoFinal(Key key, byte[] data, boolean isEncrypt) throws Exception {
        if (isEncrypt) {
            cipher.init(Cipher.ENCRYPT_MODE, key);
        } else {
            cipher.init(Cipher.DECRYPT_MODE, key);
        }
        return segment(cipher, data, isEncrypt);
    }

    /**
     * 分段处理
     *
     * @param cipher
     * @param srcBytes
     * @return
     */
    private static byte[] segment(Cipher cipher, byte[] srcBytes, boolean isEncrypt) {
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
                publicEncodedKey = rsaPublicKey.getEncoded();
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
                privateEncodedKey = rsaPrivateKey.getEncoded();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void savePublicKey(String savePath) {
        FileHelper.writeFile(savePath, publicEncodedKey);
    }

    private void savePrivateKey(String savePath) {
        FileHelper.writeFile(savePath, privateEncodedKey);
    }

}
