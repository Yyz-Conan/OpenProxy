package com.open.proxy.cryption;

import com.open.proxy.cryption.joggle.IEncryptComponent;

import java.util.Base64;

public class Base64Encrypt implements IEncryptComponent {

    @Override
    public Object getEncrypt() {
        return Base64.getEncoder();
    }

    @Override
    public byte[] onEncrypt(byte[] src) {
        return Base64.getEncoder().encode(src);
    }
}
