package connect;

import connect.joggle.ICloseListener;
import connect.network.nio.NioClientTask;

public class AbsClient extends NioClientTask {

    protected ICloseListener listener;
    protected byte[] data;

    public void setOnCloseListener(ICloseListener listener) {
        this.listener = listener;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    protected void onCloseClientChannel() {
        if (listener != null) {
            listener.onClose(getHost());
        }
    }
}
