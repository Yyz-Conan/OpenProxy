package connect.http.server;

import connect.http.client.UpdateHandleClient;
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
        LogDog.d("==> Update server start success : " + getHost() + ":" + getPort());
        NioClientFactory.getFactory().open();
    }

    @Override
    protected void onAcceptServerChannel(SocketChannel channel) {
        UpdateHandleClient client = new UpdateHandleClient(channel);
        NioClientFactory.getFactory().addTask(client);
    }

    @Override
    protected void onCloseServerChannel() {
        LogDog.e("==> Update server close ing = " + getHost() + ":" + getPort());
        NioClientFactory.destroy();
    }
}
