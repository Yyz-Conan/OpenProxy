package process;

import connect.network.base.joggle.INetReceiver;

public class RSAReceiver extends DecryptionReceiver {

    public RSAReceiver(INetReceiver receiver) {
        super(receiver);
    }

    @Override
    protected byte[] onDecrypt(byte[] src) {
        return RSADataEnvoy.getInstance().superCipher(src, true, false);
    }
}
