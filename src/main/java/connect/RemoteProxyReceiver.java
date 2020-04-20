package connect;

import connect.network.base.joggle.INetSender;
import connect.network.nio.NioReceiver;
import connect.network.nio.NioSender;
import connect.network.nio.buf.MultilevelBuf;

public class RemoteProxyReceiver extends NioReceiver<byte[]> {

    protected NioSender localTarget;

    public RemoteProxyReceiver(NioSender localTarget) {
        super(null);
        if (localTarget == null) {
            throw new NullPointerException("localTarget is null !!!");
        }
        this.localTarget = localTarget;
    }

    public INetSender getLocalTarget() {
        return localTarget;
    }

    @Override
    protected void onInterceptReceive(MultilevelBuf buf, Exception e) throws Exception {
        localTarget.sendData(buf);
    }

}
