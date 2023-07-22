package com.open.proxy.server.sync;

import com.jav.common.cryption.DataSafeManager;
import com.jav.common.cryption.joggle.EncryptionType;
import com.jav.common.cryption.joggle.IDecryptComponent;
import com.jav.common.cryption.joggle.IEncryptComponent;
import com.jav.net.nio.NioClientFactory;
import com.jav.net.security.channel.SecurityReceiver;
import com.jav.net.security.channel.SecuritySender;
import com.jav.net.security.channel.base.AbsSecurityMeter;
import com.jav.net.security.util.SystemStatusTool;
import com.open.proxy.server.sync.bean.SecuritySyncEntity;
import com.open.proxy.server.sync.joggle.IServerSyncStatusListener;
import com.open.proxy.server.sync.joggle.ISyncServerEventCallBack;
import com.open.proxy.server.sync.protocol.base.SyncOperateCode;

/**
 * 分布式同步服务meter,主要是代理service的服务提供接口调用
 *
 * @author yyz
 */
public class SecuritySyncMeter extends AbsSecurityMeter {


    /**
     * 数据安全管理，提供加解密
     */
    private final DataSafeManager mDataSafeManager;

    /**
     * 协议解析器
     */
    private SecuritySyncProtocolParser mProtocolParser;


    /**
     * 通讯
     */
    private NioClientFactory mClientFactory;


    public SecuritySyncMeter(SecuritySyncContext context) {

        mDataSafeManager = new DataSafeManager();
        mProtocolParser = new SecuritySyncProtocolParser(context);
        SecuritySyncPolicyProcessor policyProcessor = new SecuritySyncPolicyProcessor(context);
        mProtocolParser.setSecurityPolicyProcessor(policyProcessor);

        ReceiveProxy receiveProxy = new ReceiveProxy();
        mProtocolParser.setSyncEventCallBack(receiveProxy);

        mClientFactory = new NioClientFactory();
        mClientFactory.open();
    }


    /**
     * 接收sync事件回调
     */
    private class ReceiveProxy implements ISyncServerEventCallBack {

        @Override
        public void onRespondSyncCallBack(byte operateCode, int proxyPort, String machineId, byte loadCount) {

            // 保存远程服务的负载信息
            SecuritySyncBoot.getInstance().updateSyncData(machineId, proxyPort, loadCount);
            //获取当前服务链接数量和服务端口号
            int serverProxyPort = 0;
            long serverLoadCount = 0;
            String serverMachineId = null;
            SecuritySyncEntity localSyncInfo = SecuritySyncBoot.getInstance().getLocalSyncInfo();
            if (localSyncInfo != null) {
                serverMachineId = localSyncInfo.getMachineId();
                serverProxyPort = localSyncInfo.getProxyPort();
                serverLoadCount = localSyncInfo.getLoadCount();
            }

            if (operateCode == SyncOperateCode.RESPOND_SYNC_AVG.getCode()) {
                return;
            }
            // 响应本地服务的负载信息
            SecuritySyncSender syncSender = getSender();
            byte loadAvg = SystemStatusTool.getSystemAvgLoad(serverLoadCount);
            localSyncInfo.updateAvgLoad(loadAvg);
            syncSender.respondSyncAvg(serverMachineId, serverProxyPort, loadAvg);
        }

        @Override
        public void onRespondSyncMidCallBack(byte status, String machineId) {
            IServerSyncStatusListener syncListener = SecurityServerSyncImage.getInstance().getListener(machineId);
            if (syncListener != null) {
                syncListener.onSyncMinState(status);
            }
        }
    }

    @Override
    protected EncryptionType initEncryptionType() {
        return EncryptionType.BASE64;
    }


    @Override
    protected void onChannelReady(SecuritySender sender, SecurityReceiver receiver) {
        super.onChannelReady(sender, receiver);

        EncryptionType encryptionType = initEncryptionType();
        initEncryptionType(encryptionType);

        // 设置协议解析器
        receiver.setProtocolParser(mProtocolParser);
        //重置接收,断线重连接需要恢复状态
        mRealReceiver.reset();
    }

    /**
     * 初始化加密方式
     *
     * @param encryption 加密方式
     */
    protected void initEncryptionType(EncryptionType encryption) {
        // 发送完init数据,开始切换加密方式
        mDataSafeManager.init(encryption);
        IDecryptComponent decryptComponent = mDataSafeManager.getDecrypt();
        IEncryptComponent encryptComponent = mDataSafeManager.getEncrypt();

        // 设置加密方式
        mRealSender.setEncryptComponent(encryptComponent);
        mRealReceiver.setDecryptComponent(decryptComponent);
    }


    @Override
    protected void onChannelInvalid() {
        super.onChannelInvalid();
    }
}
