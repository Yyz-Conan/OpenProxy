package process;

import connect.network.base.joggle.INetReceive;

public class RSAReceive extends DecryptionReceive {

    public RSAReceive(INetReceive receive) {
        super(receive);
    }

    @Override
    protected byte[] onDecrypt(byte[] src) {
        return RSADataEnvoy.getInstance().superCipher(src, true, false);
    }
}
