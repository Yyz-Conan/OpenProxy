package cryption;


import cryption.joggle.IDecryptListener;

public class AESDecrypt implements IDecryptListener {

    @Override
    public byte[][] onDecrypt(byte[][] unpack) {
        if (unpack != null) {
            byte[][] decrypt = new byte[unpack.length][];
            for (int index = 0; index < decrypt.length; index++) {
                decrypt[index] = AESDataEnvoy.getInstance().decrypt(unpack[index]);
            }
            return decrypt;
        }
        return null;
    }
}
