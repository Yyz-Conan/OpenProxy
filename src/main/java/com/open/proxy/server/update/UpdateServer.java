package com.open.proxy.server.update;


import com.jav.common.log.LogDog;
import com.jav.net.nio.NioClientFactory;
import com.jav.net.security.channel.base.AbsSecurityServer;

import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Update File Server
 *
 * @author yyz
 */
public class UpdateServer extends AbsSecurityServer {

    private NioClientFactory mUpdateClientFactory;


    @Override
    protected void onBeReadyChannel(SelectionKey selectionKey, ServerSocketChannel channel) {
        LogDog.d("==> Update server start success : " + getHost() + ":" + getPort());
        mUpdateClientFactory = new NioClientFactory();
        mUpdateClientFactory.open();
    }

    @Override
    protected void onPassChannel(SocketChannel channel) {
        UpdateHandleClient client = new UpdateHandleClient(channel);
        mUpdateClientFactory.getNetTaskComponent().addExecTask(client);
    }


    @Override
    protected void onCloseChannel() {
        LogDog.e("==> Update server close ing = " + getHost() + ":" + getPort());
        mUpdateClientFactory.close();
    }
}
