package com.open.proxy.server.sync;

import com.jav.common.cryption.DataSafeManager;
import com.jav.common.cryption.joggle.EncryptionType;
import com.jav.common.cryption.joggle.ICipherComponent;
import com.jav.common.log.LogDog;
import com.jav.net.security.util.SystemStatusTool;
import com.jav.thread.executor.LoopTask;
import com.jav.thread.executor.LoopTaskExecutor;
import com.jav.thread.executor.TaskContainer;
import com.open.proxy.server.sync.bean.SecuritySyncPayloadData;
import com.open.proxy.server.sync.joggle.IServerSyncStatusListener;
import com.open.proxy.server.sync.joggle.ISyncServerEventCallBack;
import com.open.proxy.server.sync.protocol.base.SyncOperateCode;

import java.net.InetSocketAddress;
import java.util.*;

/**
 * 分布式同步服务meter,主要是代理service的服务提供接口调用
 *
 * @author yyz
 */
public class SecuritySyncMeter {

    private SecuritySyncContext mContext;

    /**
     * 数据安全管理，提供加解密
     */
    private final DataSafeManager mDataSafeManager;

    /**
     * 协议解析器
     */
    private final SecuritySyncProtocolParser mProtocolParser;

    private SecuritySyncSender mSyncSender;

    private TimerSync mTimerSync;


    public SecuritySyncMeter(SecuritySyncContext context) {
        mContext = context;
        mDataSafeManager = new DataSafeManager();
        mProtocolParser = new SecuritySyncProtocolParser(context);
        SecuritySyncPolicyProcessor policyProcessor = new SecuritySyncPolicyProcessor(context);
        mProtocolParser.setSecurityPolicyProcessor(policyProcessor);

        ReceiveProxy receiveProxy = new ReceiveProxy();
        mProtocolParser.setSyncEventCallBack(receiveProxy);
    }


    /**
     * 接收sync事件回调
     */
    private class ReceiveProxy implements ISyncServerEventCallBack {

        @Override
        public void onRespondSyncCallBack(InetSocketAddress remoteAddress, byte operateCode, int proxyPort, String machineId, byte loadCount) {

            // 保存远程服务的负载信息
            SecuritySyncBoot.getInstance().updateSyncData(machineId, proxyPort, loadCount);
            //判断是否是响应端，如果是不再发下面消息，不然就进入死循环
            if (operateCode == SyncOperateCode.RESPOND_SYNC_AVG.getCode()) {
                return;
            }

            SecuritySyncPayloadData localSyncInfo = SecuritySyncBoot.getInstance().getNativeSyncInfo();
            if (localSyncInfo == null) {
                return;
            }
            //获取当前服务链接数量和服务端口号
            String serverMachineId = localSyncInfo.getMachineId();
            int serverProxyPort = localSyncInfo.getPort();
            long serverLoadCount = localSyncInfo.getLoadCount();

            // 响应本地服务的负载信息
            byte loadAvg = SystemStatusTool.getSystemAvgLoad(serverLoadCount);
            localSyncInfo.updateAvgLoad(loadAvg);
            mSyncSender.respondSyncAvg(remoteAddress, serverMachineId, serverProxyPort, loadAvg);
        }

        @Override
        public void onRespondSyncMidCallBack(byte status, String machineId) {
            IServerSyncStatusListener syncListener = SecurityServerSyncImage.getInstance().getListener(machineId);
            if (syncListener != null) {
                syncListener.onSyncMinState(status);
            }
        }
    }

    protected EncryptionType initEncryptionType() {
        return EncryptionType.BASE64;
    }


    protected void onChannelReady(SecuritySyncSender sender, SecuritySyncReceiver receiver) {
        this.mSyncSender = sender;

        //初始化加密方式
        EncryptionType encryptionType = initEncryptionType();
        mDataSafeManager.init(encryptionType);
        ICipherComponent cipherComponent = mDataSafeManager.getCipherComponent();

        // 设置加密方式
        sender.setEncryptComponent(cipherComponent);
        receiver.setDecryptComponent(cipherComponent);

        // 设置协议解析器
        receiver.setProtocolParser(mProtocolParser);

        //初始化镜像
        SecurityServerSyncImage.getInstance().init(mSyncSender);

        //开始与其他服务同步
        connectSyncServer();
    }

    /**
     * 加载需要同步的服务列表
     */
    public void connectSyncServer() {
        Map<String, String> syncServer = mContext.getSyncServerList();
        if (syncServer == null || syncServer.isEmpty()) {
            return;
        }
        Map<String, SecuritySyncPayloadData> syncInfo = SecuritySyncBoot.getInstance().getRemoteSyncInfo();

        List<String> syncMachineList = mContext.getMachineList();
        Set<Map.Entry<String, String>> entrySet = syncServer.entrySet();
        for (Map.Entry<String, String> entry : entrySet) {
            String value = entry.getValue();
            String[] arrays = value.split(":");
            if (arrays.length != 2) {
                continue;
            }
            String machineId = entry.getKey();
            syncMachineList.add(machineId);
            SecuritySyncPayloadData entity = new SecuritySyncPayloadData(machineId, arrays[0]);
            try {
                entity.setPort(Integer.parseInt(arrays[1]));
                syncInfo.put(machineId, entity);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        mTimerSync = new TimerSync();
        mTimerSync.startTimer();
    }

    private class TimerSync extends LoopTask {

        private static final int DELAY_TIME = 5 * 60 * 1000;

        private TaskContainer mTaskContainer;

        @Override
        protected void onRunLoopTask() {
            Map<String, SecuritySyncPayloadData> syncInfo = SecuritySyncBoot.getInstance().getRemoteSyncInfo();
            if (syncInfo.isEmpty()) {
                stopTimer();
                return;
            }

            long loadCount = SecuritySyncBoot.getInstance().getNativeServerLoadCount();
            byte loadAvg = SystemStatusTool.getSystemAvgLoad(loadCount);
            Collection<SecuritySyncPayloadData> values = syncInfo.values();
            Iterator<SecuritySyncPayloadData> iterator = values.iterator();
            while (iterator.hasNext()) {
                SecuritySyncPayloadData payloadData = iterator.next();
                InetSocketAddress target = new InetSocketAddress(payloadData.getHost(), payloadData.getPort());
                mSyncSender.requestSyncAvg(target, mContext.getMachineId(), mContext.getProxyPort(), loadAvg);
            }
            LogDog.i("## >>>> start sync mid = " + mContext.getMachineId() + " loadAvg = " + loadAvg + " <<<<<");

            LoopTaskExecutor loopTaskExecutor = mTaskContainer.getTaskExecutor();
            loopTaskExecutor.waitTask(DELAY_TIME);
        }

        public void startTimer() {
            if (mTaskContainer == null) {
                mTaskContainer = new TaskContainer(this);
                LoopTaskExecutor loopTaskExecutor = mTaskContainer.getTaskExecutor();
                loopTaskExecutor.startTask();
            }
        }

        public void stopTimer() {
            if (mTaskContainer != null) {
                LoopTaskExecutor loopTaskExecutor = mTaskContainer.getTaskExecutor();
                loopTaskExecutor.stopTask();
            }
        }
    }


    protected void onChannelInvalid() {
        if (mTimerSync != null) {
            mTimerSync.stopTimer();
        }
    }

}
