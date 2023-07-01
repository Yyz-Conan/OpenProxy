package com.open.proxy.connect.http.client;


import com.jav.common.log.LogDog;
import com.jav.common.track.SpiderEnvoy;
import com.jav.net.base.joggle.INetFactory;
import com.jav.net.base.joggle.NetErrorType;
import com.jav.net.entity.MultiByteBuffer;
import com.jav.net.nio.NioClientTask;
import com.jav.net.nio.NioSender;
import com.jav.net.security.channel.SecurityChannelContext;
import com.jav.net.security.channel.SecurityServerChannelImage;
import com.jav.net.security.channel.SecuritySyncMeter;
import com.jav.net.security.channel.base.AbsSecurityServerReception;
import com.jav.net.security.channel.joggle.InitResult;
import com.jav.net.security.protocol.RequestProtocol;
import com.open.proxy.OpContext;
import com.open.proxy.connect.http.server.MultipleProxyServer;
import com.open.proxy.connect.joggle.IBindClientListener;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

/**
 * 远程服务端接待需要代理的客户端
 *
 * @author yyz
 */
public class RemoteReceptionProxyClient extends AbsSecurityServerReception implements IBindClientListener {

    private String mServerHost;
    private final Map<String, TransProxyClient> mProxyClientMap;
    private INetFactory<NioClientTask> mBClientFactory;

    public RemoteReceptionProxyClient(SecurityChannelContext context, SocketChannel channel, String serverHost) {
        super(context, channel);
        mBClientFactory = OpContext.getInstance().getBClientFactory();
        mProxyClientMap = new HashMap<>();
        mServerHost = serverHost;
    }

    @Override
    protected void onErrorChannel(NetErrorType errorType, Throwable throwable) {
        throwable.printStackTrace();
    }

    @Override
    protected void onCloseChannel() {
        super.onCloseChannel();
        LogDog.d(" close [ connect count = " + MultipleProxyServer.sLocalConnectCount.decrementAndGet() + " ] ");

        synchronized (mProxyClientMap) {
            for (TransProxyClient client : mProxyClientMap.values()) {
                mBClientFactory.getNetTaskComponent().addUnExecTask(client);
            }
        }
        String report = SpiderEnvoy.getInstance().endWatchKey(RemoteReceptionProxyClient.this.toString());
        LogDog.saveLog(report);
    }

    @Override
    public void onChannelReady(SecurityServerChannelImage image) {
        LogDog.d("--> RemoteReceptionProxyClient onReady");
    }

    @Override
    public void onRequestTransData(String requestId, byte pctCount, byte[] data) {
        // 解析出客户端需要中转的数据
        TransProxyClient client;
        synchronized (mProxyClientMap) {
            client = mProxyClientMap.get(requestId);
        }
        if (client != null) {
            // 已存在
            NioSender sender = client.getSender();
            if (sender != null) {
                sender.sendData(new MultiByteBuffer(data));
            }
        }
    }

    @Override
    public void onChannelInvalid() {
        LogDog.w("--> RemoteReceptionProxyClient onInvalid");
    }

    @Override
    public void onCreateConnect(String requestId, String realHost, int port) {
        // 如果不为空表示这是新的请求目标，需要创建目标链接
        TransProxyClient client;
        synchronized (mProxyClientMap) {
            client = mProxyClientMap.get(requestId);
        }
        if (client != null) {
            // 已存在,则关闭
            mBClientFactory.getNetTaskComponent().addUnExecTask(client);
        }
        // 新请求
        LogDog.d("--> remote create connect host = " + realHost + ":" + port);
        TransProxyClient transProxyClient = new TransProxyClient(requestId, realHost, port);
        transProxyClient.setBindClientListener(this);
        mBClientFactory.getNetTaskComponent().addExecTask(transProxyClient);
        synchronized (mProxyClientMap) {
            mProxyClientMap.put(requestId, transProxyClient);
        }
    }

    @Override
    public boolean onRespondInitData(String machineId, String channelId) {
        // 获取最低负载的服务地址
        SecuritySyncMeter syncMeter = mContext.getSyncMeter();
        String lowLoadServerHost = syncMeter.getLowLoadServer();
        if (lowLoadServerHost != null && !lowLoadServerHost.equals(mServerHost)) {
            // 如果当前服务不是最低负载的服务，则让客户端切换,返回低负载的服务地址
            mServerChannelImage.respondInitData(machineId, InitResult.SERVER_IP.getCode(), lowLoadServerHost.getBytes());
            return true;
        }
        return false;
    }

    @Override
    public void onBindClientByReady(String requestId) {
        mServerChannelImage.respondRequestData(requestId, RequestProtocol.REP_SUCCESS_CODE);
    }

    @Override
    public void onBindClientData(String requestId, MultiByteBuffer buffer) {
        byte[] data = buffer.array();
        mServerChannelImage.sendTransDataFromServer(requestId, data);
    }

    @Override
    public void onBindClientByError(String requestId) {
        mServerChannelImage.respondRequestData(requestId, (byte) 1);
    }

    @Override
    public void onBindClientClose(String requestId) {
        synchronized (mProxyClientMap) {
            mProxyClientMap.remove(requestId);
        }
    }
}
