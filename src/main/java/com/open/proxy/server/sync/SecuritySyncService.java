package com.open.proxy.server.sync;

import com.jav.common.log.LogDog;
import com.jav.net.base.AbsNetSender;
import com.jav.net.nio.NioUdpReceiver;
import com.jav.net.nio.NioUdpSender;
import com.jav.net.nio.NioUdpTask;

import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;

/**
 * 分布式同步服务,主要是跟服务之间同步获取服务负载信息
 *
 * @author yyz
 */
public class SecuritySyncService extends NioUdpTask {

    private SecuritySyncMeter mServerMeter;

    public SecuritySyncService(SecuritySyncContext context) {
        mServerMeter = new SecuritySyncMeter(context);
    }


    @Override
    protected void onBeReadyChannel(SelectionKey selectionKey, DatagramChannel channel) {
        LogDog.d("Sync server start success : " + getHost() + ":" + getPort());

        SecuritySyncSender syncSender = new SecuritySyncSender(new NioUdpSender());
        AbsNetSender coreSender = syncSender.getCoreSender();
        coreSender.setChannel(selectionKey, channel);
        setSender((NioUdpSender) coreSender);

        SecuritySyncReceiver syncReceiver = new SecuritySyncReceiver();
        NioUdpReceiver coreReceiver = syncReceiver.getCoreReceiver();
        setReceiver(coreReceiver);

        mServerMeter.onChannelReady(syncSender, syncReceiver);
    }


    @Override
    protected void onCloseChannel() {
        mServerMeter.onChannelInvalid();
        LogDog.w("Sync server stop !!!");
    }
}
