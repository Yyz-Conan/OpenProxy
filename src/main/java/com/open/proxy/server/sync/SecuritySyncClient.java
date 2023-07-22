package com.open.proxy.server.sync;

import com.jav.net.base.SocketChannelCloseException;
import com.jav.net.base.joggle.NetErrorType;
import com.jav.net.nio.NioClientTask;
import com.jav.net.nio.NioReceiver;
import com.jav.net.nio.NioSender;
import com.jav.net.security.channel.SecurityReceiver;
import com.jav.net.security.channel.SecuritySender;

import java.nio.channels.SocketChannel;

public class SecuritySyncClient extends NioClientTask {

    /**
     * 通道运行状态信息
     */
    protected SecuritySyncMeter mSyncMeter;

    /**
     * 上下文配置
     */
    protected SecuritySyncContext mContext;

    public SecuritySyncClient(SecuritySyncContext context) {
        this.mContext = context;
        mSyncMeter = initSecuritySyncMeter(context);
    }

    public SecuritySyncClient(SecuritySyncContext context, SocketChannel channel) {
        super(channel, null);
        this.mContext = context;
        mSyncMeter = initSecuritySyncMeter(context);
    }

    /**
     * 初始化channel meter，channel meter主要是控制channel整个生命周期和提供业务接口
     *
     * @param context context 对象
     */
    protected SecuritySyncMeter initSecuritySyncMeter(SecuritySyncContext context) {
        return new SecuritySyncMeter(context);
    }

    protected SecurityReceiver initReceiver() {
        return new SecuritySyncReceiver();
    }

    protected SecuritySender initSender() {
        return new SecuritySyncSender();
    }

    /**
     * 获取channel meter
     *
     * @param <T>
     * @return
     */
    protected <T extends SecuritySyncMeter> T getChanelMeter() {
        return (T) mSyncMeter;
    }

    @Override
    protected void onBeReadyChannel(SocketChannel channel) {

        SecurityReceiver securityReceiver = mSyncMeter.getReceiver();
        if (securityReceiver == null) {
            securityReceiver = initReceiver();
            NioReceiver coreReceiver = securityReceiver.getCoreReceiver();
            setReceiver(coreReceiver);
        }

        SecuritySender securitySender = mSyncMeter.getSender();
        if (securitySender == null) {
            securitySender = initSender();
            NioSender coreSender = securitySender.getCoreSender();
            coreSender.setChannel(getSelectionKey(), channel);
            setSender(coreSender);
        }

        mSyncMeter.onChannelReady(securitySender, securityReceiver);
    }

    @Override
    protected void onCloseChannel() {
        super.onCloseChannel();
        mSyncMeter.onChannelInvalid();
    }

    @Override
    protected void onErrorChannel(NetErrorType errorType, Throwable throwable) {
        if (!(throwable instanceof SocketChannelCloseException)) {
            throwable.printStackTrace();
        }
    }

}
