package connect;

import connect.network.nio.NioClientTask;
import connect.network.nio.NioHPCClientFactory;
import connect.network.nio.NioSender;

public class RequestSender extends NioSender {
    private NioClientTask clientTask;

    public RequestSender(NioClientTask clientTask) {
        this.clientTask = clientTask;
    }

    @Override
    protected void onSenderErrorCallBack(Throwable e) {
        NioHPCClientFactory.getFactory().removeTask(clientTask);
    }
}
