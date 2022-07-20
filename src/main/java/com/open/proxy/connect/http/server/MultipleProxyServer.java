package com.open.proxy.connect.http.server;


import com.jav.common.log.LogDog;
import com.jav.common.util.ConfigFileEnvoy;
import com.jav.net.nio.NioClientFactory;
import com.jav.net.nio.NioServerTask;
import com.open.proxy.IConfigKey;
import com.open.proxy.OPContext;
import com.open.proxy.connect.http.client.ReceptionProxyClient;
import com.open.proxy.safety.IPBlackListManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 代理服务端，支持ip黑名单拦截
 */
public class MultipleProxyServer extends NioServerTask {

    public static volatile AtomicInteger localConnectCount = new AtomicInteger(0);
    private boolean isEnableIPBlack;

    @Override
    protected void onBeReadyChannel(ServerSocketChannel channel) {
        LogDog.d("==> Proxy server start success : " + getHost() + ":" + getPort());
        NioClientFactory.getFactory().open();
        ConfigFileEnvoy cFileEnvoy = OPContext.getInstance().getConfigFileEnvoy();
        isEnableIPBlack = cFileEnvoy.getBooleanValue(IConfigKey.CONFIG_ENABLE_IP_BLACK);
    }

    @Override
    protected void onAcceptServerChannel(SocketChannel channel) {
        if (isEnableIPBlack) {
            boolean isBlack = false;
            try {
                InetSocketAddress address = (InetSocketAddress) channel.getRemoteAddress();
                //判断ip是否在黑名单
                isBlack = IPBlackListManager.getInstance().isContains(address.getHostString());
                if (isBlack) {
                    //如果是黑名单则直接断开链接
                    channel.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (isBlack) {
                return;
            }
        }

        ReceptionProxyClient client = new ReceptionProxyClient(channel);
        NioClientFactory.getFactory().getNetTaskContainer().addExecTask(client);
        localConnectCount.incrementAndGet();
    }

    @Override
    protected void onCloseChannel() {
        LogDog.e("==> Proxy server close ing = " + getHost() + ":" + getPort());
        NioClientFactory.destroy();
    }
}
