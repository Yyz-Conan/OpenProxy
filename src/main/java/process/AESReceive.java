package process;

import connect.network.nio.NioClientTask;

public class AESReceive extends DecryptionReceive {

    public AESReceive(NioClientTask task, String receiveMethodName) {
        super(task, receiveMethodName);
    }

    @Override
    protected byte[] onDecrypt(byte[] src) {
        return AESDataEnvoy.getInstance().decrypt(src);
    }
}
