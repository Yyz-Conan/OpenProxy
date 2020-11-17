package connect.server;

import connect.client.UpdateHandleClient;
import connect.network.nio.NioClientFactory;
import connect.network.nio.NioServerTask;
import log.LogDog;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Update File Server
 */
public class UpdateServer extends NioServerTask {

    @Override
    protected void onBootServerComplete(ServerSocketChannel channel) {
        LogDog.d("==> update Server Start Success : " + getServerHost() + ":" + getServerPort());
        NioClientFactory.getFactory().open();
    }

    @Override
    protected void onAcceptServerChannel(SocketChannel channel) {
        UpdateHandleClient client = new UpdateHandleClient(channel);
        NioClientFactory.getFactory().addTask(client);
    }

    @Override
    protected void onCloseServerChannel() {
        LogDog.e("==> update Server close ing = " + getServerHost() + ":" + getServerPort());
        NioClientFactory.destroy();
    }
}
