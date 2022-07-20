package com.open.proxy.cryption;

import com.open.proxy.cryption.joggle.IDecryptComponent;

import java.util.Base64;

public class Base64Decrypt implements IDecryptComponent {

    @Override
    public Object getEncrypt() {
        return Base64.getDecoder();
    }

    @Override
    public byte[] onDecrypt(byte[] unpack) {
        if (unpack == null) {
            return null;
        }
        return Base64.getDecoder().decode(unpack);
    }
}
