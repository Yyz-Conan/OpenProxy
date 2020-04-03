package cryption.joggle;

public interface IDecryptListener {
    /**
     * 解密
     * @param unpack
     * @return
     */
    byte[][] onDecrypt(byte[][] unpack);
}
