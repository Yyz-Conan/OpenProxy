package process;

import security.Base64Helper;

import javax.crypto.Cipher;
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

    private static RSADataEnvoy dataPolicy = null;

    public static RSADataEnvoy getInstance() {
        synchronized (RSADataEnvoy.class) {
            if (dataPolicy == null) {
                synchronized (RSADataEnvoy.class) {
                    if (dataPolicy != null) {
                        try {
                            dataPolicy = new RSADataEnvoy();
                        } catch (Exception e) {
                            dataPolicy = null;
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return dataPolicy;
    }

    private RSADataEnvoy() throws Exception {
        //1.初始化密钥
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(512);//密钥长度为64的整数倍，最大是65536
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        rsaPublicKey = (RSAPublicKey) keyPair.getPublic();
        rsaPrivateKey = (RSAPrivateKey) keyPair.getPrivate();
        System.out.println("RSA公钥：" + Base64Helper.getHelper().encodeToString(rsaPublicKey.getEncoded()));
        System.out.println("RSA私钥：" + Base64Helper.getHelper().encodeToString(rsaPrivateKey.getEncoded()));
    }

    /**
     * 公钥加密，私钥解密【加密】
     *
     * @param src
     * @return
     */
    public byte[] publicEncrypt(byte[] src) {
        byte[] result = null;
        try {
            X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(rsaPublicKey.getEncoded());
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(x509EncodedKeySpec);
            Cipher cipher = Cipher.getInstance("ELGamal", "BC");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            result = cipher.doFinal(src);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("JDK RSA公钥加密：" + Base64Helper.getHelper().encodeToString(result));
        return result;
    }

    /**
     * 公钥加密，私钥解密【解密】
     *
     * @return
     */
    public byte[] publicDecrypt(byte[] encrypt) {
        byte[] result = null;
        try {
            PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(rsaPrivateKey.getEncoded());
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(pkcs8EncodedKeySpec);
            Cipher cipher = Cipher.getInstance("ELGamal", "BC");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            result = cipher.doFinal(encrypt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("JDK RSA私钥解密：" + new String(result));
        return result;
    }

    /**
     * 私钥加密，公钥解密【加密】
     *
     * @param src
     * @return
     */
    public byte[] privateEncrypt(byte[] src) {
        byte[] result = null;
        try {
            PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(rsaPrivateKey.getEncoded());
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(pkcs8EncodedKeySpec);
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);
            result = cipher.doFinal(src);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("JDK RSA私钥加密：" + Base64Helper.getHelper().encodeToString(result));
        return result;
    }


    /**
     * 私钥加密，公钥解密【解密】
     *
     * @param encrypt
     * @return
     */
    public byte[] privateDecrypt(byte[] encrypt) {
        byte[] result = null;
        try {
            X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(rsaPublicKey.getEncoded());
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(x509EncodedKeySpec);
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
            result = cipher.doFinal(encrypt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("JDK RSA公钥解密：" + new String(result));
        return result;
    }

}
