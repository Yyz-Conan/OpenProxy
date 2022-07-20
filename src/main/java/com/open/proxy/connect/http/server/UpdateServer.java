package com.open.proxy.connect.http.server;


import com.jav.common.log.LogDog;
import com.jav.net.nio.NioClientFactory;
import com.jav.net.nio.NioServerTask;
import com.open.proxy.connect.http.client.UpdateHandleClient;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Update File Server
 */
public class UpdateServer extends NioServerTask {

    @Override
    protected void onBeReadyChannel(ServerSocketChannel channel) {
        LogDog.d("==> Update server start success : " + getHost() + ":" + getPort());
        NioClientFactory.getFactory().open();
    }

    @Override
    protected void onAcceptServerChannel(SocketChannel channel) {
        UpdateHandleClient client = new UpdateHandleClient(channel);
        NioClientFactory.getFactory().getNetTaskContainer().addExecTask(client);
    }

    @Override
    protected void onCloseChannel() {
        LogDog.e("==> Update server close ing = " + getHost() + ":" + getPort());
        NioClientFactory.destroy();
    }
}
