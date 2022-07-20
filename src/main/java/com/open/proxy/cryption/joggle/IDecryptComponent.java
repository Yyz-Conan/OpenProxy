package com.open.proxy.cryption.joggle;

public interface IDecryptComponent {

    /**
     * 获取解密实体
     *
     * @return
     */
    <T> T getEncrypt();

    /**
     * 解密
     *
     * @param unpack
     * @return
     */
    byte[] onDecrypt(byte[] unpack);
}
