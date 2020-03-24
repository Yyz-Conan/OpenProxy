package process;


import connect.network.base.joggle.INetReceiver;

public class AESReceiver extends DecryptionReceiver {

    public AESReceiver(INetReceiver receiver) {
        super(receiver);
    }

    @Override
    protected byte[] onDecrypt(byte[] src) {
        return AESDataEnvoy.getInstance().decrypt(src);
    }
}
