package com.open.proxy.server.http.server;


import com.jav.common.log.LogDog;
import com.jav.common.util.ConfigFileEnvoy;
import com.jav.net.base.SocketChannelCloseException;
import com.jav.net.base.joggle.INetFactory;
import com.jav.net.base.joggle.NetErrorType;
import com.jav.net.nio.NioClientTask;
import com.jav.net.security.channel.SecurityChannelContext;
import com.jav.net.security.channel.base.AbsSecurityServer;
import com.open.proxy.IConfigKey;
import com.open.proxy.OpContext;
import com.open.proxy.server.http.client.LocalReceptionProxyClient;
import com.open.proxy.server.http.client.RemoteReceptionProxyClient;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 代理服务端，支持ip黑名单拦截
 *
 * @author yyz
 */
public class MultipleProxyServer extends AbsSecurityServer {

    public static volatile AtomicInteger sLocalConnectCount = new AtomicInteger(0);

    private boolean mIsServerMode;

    private SecurityChannelContext mContext;

    public void setContext(SecurityChannelContext context) {
        mContext = context;
    }

    @Override
    protected void onBeReadyChannel(ServerSocketChannel channel) {
        super.onBeReadyChannel(channel);
        LogDog.d("## localProxy server start success : " + getHost() + ":" + getPort());
        ConfigFileEnvoy cFileEnvoy = OpContext.getInstance().getConfigFileEnvoy();
        mIsServerMode = cFileEnvoy.getBooleanValue(IConfigKey.CONFIG_IS_SERVER_MODE);
    }

    @Override
    protected void onPassChannel(SocketChannel channel) {
        NioClientTask serverReception;
        if (mIsServerMode) {
            serverReception = new RemoteReceptionProxyClient(mContext, channel);
        } else {
            serverReception = new LocalReceptionProxyClient(channel);
        }
        int count = sLocalConnectCount.incrementAndGet();
        LogDog.d("[ Proxy server count = " + count + " ] client = " + serverReception);
        INetFactory<NioClientTask> factory = OpContext.getInstance().getBClientFactory();
        factory.getNetTaskComponent().addExecTask(serverReception);
    }

    @Override
    protected void onErrorChannel(NetErrorType errorType, Throwable throwable) {
        if (!(throwable instanceof SocketChannelCloseException)) {
            throwable.printStackTrace();
        }
    }

    @Override
    protected void onCloseChannel() {
        super.onCloseChannel();
        LogDog.e("Proxy server close ing = " + getHost() + ":" + getPort());
    }
}
