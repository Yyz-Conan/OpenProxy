package com.open.proxy.cryption.joggle;

public interface IEncryptComponent {

    /**
     * 获取加密实体
     *
     * @return
     */
    <T> T getEncrypt();

    /**
     * 加密
     *
     * @param encode
     * @return
     */
    byte[] onEncrypt(byte[] encode);
}
