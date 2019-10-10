package connect;

import connect.network.nio.NioHPCClientFactory;
import connect.network.nio.NioHPCSender;

public class RequestSender extends NioHPCSender {
    @Override
    protected void onSenderErrorCallBack() {
        NioHPCClientFactory.getFactory().removeTask(clientTask);
    }
}
