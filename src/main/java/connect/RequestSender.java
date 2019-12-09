package connect;

import connect.network.nio.NioClientTask;
import connect.network.nio.NioHPCClientFactory;
import connect.network.nio.NioHPCSender;

public class RequestSender extends NioHPCSender {
    public RequestSender(NioClientTask clientTask) {
        super(clientTask);
    }

    @Override
    protected void onSenderErrorCallBack(Throwable e) {
        NioHPCClientFactory.getFactory().removeTask(clientTask);
    }
}
