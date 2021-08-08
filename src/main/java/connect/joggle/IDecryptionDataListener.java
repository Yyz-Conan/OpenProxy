package connect.joggle;

/**
 * 解密数据回调接口
 */
public interface IDecryptionDataListener {

    /**
     * 解密数据
     * @param decrypt
     */
    void onDecryption(byte[] decrypt);
}
