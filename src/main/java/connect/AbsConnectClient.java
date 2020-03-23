package connect;

import connect.network.nio.NioClientTask;

public class AbsConnectClient extends NioClientTask {

    private ICloseListener listener;

    public void setOnCloseListener(ICloseListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onCloseClientChannel() {
        if (listener != null) {
            listener.onClose(getHost());
        }
    }
}
