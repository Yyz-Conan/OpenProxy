package process;

import connect.network.base.joggle.INetReceiver;
import connect.network.nio.NioReceiver;

public abstract class DecryptionReceiver extends NioReceiver<byte[]> {

    public DecryptionReceiver(INetReceiver receiver) {
        super(receiver);
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
