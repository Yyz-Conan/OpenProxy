package cryption.joggle;

public interface IDecryptTransform {
    /**
     * 解密
     * @param unpack
     * @return
     */
    byte[] onDecrypt(byte[] unpack);
}
