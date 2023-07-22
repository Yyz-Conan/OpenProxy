package com.open.proxy.server.sync;

import com.jav.common.log.LogDog;
import com.jav.net.nio.NioClientFactory;
import com.jav.net.security.channel.base.AbsSecurityServer;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * 分布式同步服务,主要是跟服务之间同步获取服务负载信息
 *
 * @author yyz
 */
public class SecuritySyncService extends AbsSecurityServer {

    private NioClientFactory mClientFactory;

    private SecuritySyncContext mContext;

    public SecuritySyncService(SecuritySyncContext context, NioClientFactory clientFactory) {
        mContext = context;
        mClientFactory = clientFactory;
    }

    @Override
    protected void onBeReadyChannel(ServerSocketChannel channel) {
        LogDog.d("Sync server start success : " + getHost() + ":" + getPort());
    }

    @Override
    protected void onPassChannel(SocketChannel channel) {
        SecuritySyncServerReception reception = new SecuritySyncServerReception(mContext, channel);
        mClientFactory.getNetTaskComponent().addExecTask(reception);
    }


    @Override
    protected void onCloseChannel() {
        super.onCloseChannel();
        LogDog.w("Sync server stop !!!");
    }
}
