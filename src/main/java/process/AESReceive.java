package process;

import connect.network.base.joggle.INetReceive;

public class AESReceive extends DecryptionReceive {

    public AESReceive(INetReceive receive) {
        super(receive);
    }

    @Override
    protected byte[] onDecrypt(byte[] src) {
        return AESDataEnvoy.getInstance().decrypt(src);
    }
}
