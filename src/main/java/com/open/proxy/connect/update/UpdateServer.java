package com.open.proxy.connect.update;


import com.jav.common.log.LogDog;
import com.jav.net.nio.NioClientFactory;
import com.jav.net.security.channel.SecurityChannelContext;
import com.jav.net.security.channel.base.AbsSecurityServer;

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
    protected void onBeReadyChannel(ServerSocketChannel channel) {
        super.onBeReadyChannel(channel);
        LogDog.d("==> Update server start success : " + getHost() + ":" + getPort());
        mUpdateClientFactory = new NioClientFactory();
        mUpdateClientFactory.open();
    }

    @Override
    protected void onPassChannel(SecurityChannelContext context, SocketChannel channel) {
        UpdateHandleClient client = new UpdateHandleClient(channel);
        mUpdateClientFactory.getNetTaskComponent().addExecTask(client);
    }


    @Override
    protected void onCloseChannel() {
        LogDog.e("==> Update server close ing = " + getHost() + ":" + getPort());
        mUpdateClientFactory.close();
    }
}
