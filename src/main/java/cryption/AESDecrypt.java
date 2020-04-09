package cryption;


import cryption.joggle.IDecryptListener;

public class AESDecrypt implements IDecryptListener {

    @Override
    public byte[] onDecrypt(byte[] unpack) {
        return AESDataEnvoy.getInstance().decrypt(unpack);
    }
}
