package com.open.proxy.cryption.joggle;

public enum EncryptionType {

    BASE64((byte) 1, "base64"), AES((byte) 2, "aes"), RSA((byte) 4, "rsa");

    private String mType;
    private byte mCode;

    EncryptionType(byte code, String type) {
        mType = type;
        mCode = code;
    }

    public String getType() {
        return mType;
    }

    public byte getCode() {
        return mCode;
    }


    public static EncryptionType getInstance(byte code) {
        if (BASE64.getCode() == code) {
            return BASE64;
        } else if (RSA.getCode() == code) {
            return RSA;
        } else if (AES.getCode() == code) {
            return AES;
        }
        return null;
    }
}
