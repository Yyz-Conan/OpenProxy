package com.open.proxy.server.http.client;


import com.jav.common.log.LogDog;
import com.jav.common.state.joggle.IStateMachine;
import com.jav.common.track.SpiderEnvoy;
import com.jav.net.base.MultiBuffer;
import com.jav.net.base.NetTaskStatus;
import com.jav.net.base.joggle.INetFactory;
import com.jav.net.base.joggle.NetErrorType;
import com.jav.net.nio.NioClientTask;
import com.jav.net.nio.NioSender;
import com.jav.net.security.channel.SecurityChannelContext;
import com.jav.net.security.channel.SecurityServerChannelImage;
import com.jav.net.security.channel.base.AbsSecurityServerReception;
import com.jav.net.security.channel.base.ConstantCode;
import com.jav.net.security.channel.base.InitRespondResult;
import com.jav.net.security.channel.base.UnusualBehaviorType;
import com.jav.net.security.protocol.base.TransOperateCode;
import com.open.proxy.OpContext;
import com.open.proxy.server.http.server.MultipleProxyServer;
import com.open.proxy.server.joggle.IBindClientListener;
import com.open.proxy.server.sync.SecurityServerSyncImage;
import com.open.proxy.server.sync.SecuritySyncBoot;
import com.open.proxy.server.sync.bean.SecuritySyncPayloadData;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

/**
 * 远程服务端接待需要代理的客户端
 *
 * @author yyz
 */
public class RemoteReceptionProxyClient extends AbsSecurityServerReception implements IBindClientListener {

    private final Map<String, TransProxyClient> mProxyClientMap;
    private INetFactory<NioClientTask> mBClientFactory;

    public RemoteReceptionProxyClient(SecurityChannelContext context, SocketChannel channel) {
        super(context, channel);
        mBClientFactory = OpContext.getInstance().getBClientFactory();
        mProxyClientMap = new HashMap<>();
    }

    @Override
    protected void onErrorChannel(NetErrorType errorType, Throwable throwable) {
        throwable.printStackTrace();
    }

    @Override
    protected void onCloseChannel() {
        super.onCloseChannel();
        SecurityServerSyncImage.getInstance().clearListener();
        LogDog.d(" close [ connect count = " + MultipleProxyServer.sLocalConnectCount.decrementAndGet() + " ] ");
        //更新当前链接数
        SecuritySyncBoot.getInstance().updateNativeServerSyncInfo(MultipleProxyServer.sLocalConnectCount.get());

        synchronized (mProxyClientMap) {
            for (TransProxyClient client : mProxyClientMap.values()) {
                mBClientFactory.getNetTaskComponent().addUnExecTask(client);
            }
        }
        String report = SpiderEnvoy.getInstance().endWatchKey(RemoteReceptionProxyClient.this.toString());
        LogDog.saveLog(report);
    }

    @Override
    public void onChannelImageReady(SecurityServerChannelImage image) {
        LogDog.d("--> RemoteReceptionProxyClient onReady " + this);
        SecuritySyncBoot.getInstance().updateNativeServerSyncInfo(MultipleProxyServer.sLocalConnectCount.get());
    }

    @Override
    public void onRequestTransData(String requestId, byte[] data) {
        // 解析出客户端需要中转的数据
        TransProxyClient client;
        synchronized (mProxyClientMap) {
            client = mProxyClientMap.get(requestId);
        }
        if (client != null) {
            // 已存在
            NioSender sender = client.getSender();
            if (sender != null) {
                sender.sendData(new MultiBuffer(data));
            }
        }
    }

    @Override
    public void onChannelInvalid() {
        LogDog.w("--> RemoteReceptionProxyClient onInvalid " + this);
    }

    @Override
    public void onChannelError(UnusualBehaviorType type, Map<String, String> map) {
        LogDog.e("## RemoteReceptionProxyClient onChannelError " + type.getErrorMsg());
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
    public void onRespondInitData(String machineId, InitRespondResult respondResult) {
        // 获取最低负载的服务地址
        SecuritySyncPayloadData entity = SecuritySyncBoot.getInstance().getLowLoadServer();
        if (entity != null) {
            //先同步machine id
            ServerSyncStatusListener syncListener = new ServerSyncStatusListener(machineId, entity, mServerChannelImage);
            SecurityServerSyncImage.getInstance().addListener(syncListener);
            SecurityServerSyncImage.getInstance().requestSyncMid(mContext.getMachineId(), machineId);
            LogDog.w("## start sync client machine id data to low load server, mid : " + mContext.getMachineId());
            // 如果当前服务不是最低负载的服务，则让客户端切换,返回低负载的服务地址
//            mServerChannelImage.respondInitData(machineId, InitResult.SERVER_IP.getCode(), lowLoadServerHost.getBytes());
        }
        respondResult.finish(entity != null);
    }

    @Override
    public void onRepeatMachine(String machineId) {
        IStateMachine<Integer> stateMachine = getStatusMachine();
        if (stateMachine.getState() == NetTaskStatus.RUN) {
            INetFactory<NioClientTask> factory = OpContext.getInstance().getBClientFactory();
            factory.getNetTaskComponent().addUnExecTask(this);
            LogDog.w("## found repeat machine id, close channel !!! " + machineId);
        }
    }

    @Override
    public void onBindClientByReady(String requestId) {
        mServerChannelImage.respondRequestData(requestId, ConstantCode.REP_SUCCESS_CODE);
    }

    @Override
    public void onBindClientData(String requestId, MultiBuffer buffer) {
        byte[] data = buffer.asByte();
        mServerChannelImage.sendTransDataFromServer(requestId, data);
    }

    @Override
    public void onBindClientByError(String requestId) {
        byte operateCode = (byte) (TransOperateCode.ADDRESS.getCode() | ConstantCode.REP_EXCEPTION_CODE);
        mServerChannelImage.respondRequestData(requestId, operateCode);
    }

    @Override
    public void onBindClientClose(String requestId) {
        synchronized (mProxyClientMap) {
            mProxyClientMap.remove(requestId);
        }
    }
}
