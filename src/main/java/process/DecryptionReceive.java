package process;

import connect.network.base.joggle.INetReceive;
import connect.network.nio.NioReceive;

public abstract class DecryptionReceive extends NioReceive<byte[]> {

    public DecryptionReceive(INetReceive receive) {
        super(receive);
    }

    @Override
    protected void notifyReceiver(byte[] data, Exception exception) {
        if (data != null) {
            data = onDecrypt(data);
        }
        super.notifyReceiver(data, exception);
    }

    protected abstract byte[] onDecrypt(byte[] src);
}
