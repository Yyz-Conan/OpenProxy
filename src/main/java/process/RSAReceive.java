package process;

import connect.network.nio.NioClientTask;

public class RSAReceive extends DecryptionReceive {

    public RSAReceive(NioClientTask task, String receiveMethodName) {
        super(task, receiveMethodName);
    }

    @Override
    protected byte[] onDecrypt(byte[] src) {
        return RSADataEnvoy.getInstance().superCipher(src, true, false);
    }
}
