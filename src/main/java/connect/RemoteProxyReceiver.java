package connect;

import connect.network.base.joggle.INetSender;
import connect.network.nio.NioReceiver;

public class RemoteProxyReceiver extends NioReceiver<byte[]> {

    protected INetSender localTarget;

    public RemoteProxyReceiver(INetSender localTarget) {
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
    protected void onInterceptReceive(byte[] data, Exception e) throws Exception {
        localTarget.sendData(data);
    }

}
