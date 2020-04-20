package cryption;


import cryption.joggle.IDecryptTransform;

public class AESDecrypt implements IDecryptTransform {

    @Override
    public byte[] onDecrypt(byte[] unpack) {
        return AESDataEnvoy.getInstance().decrypt(unpack);
    }
}
