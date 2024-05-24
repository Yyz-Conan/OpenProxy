package com.open.proxy.server.sync;

import com.jav.net.nio.NioUdpFactory;
import com.open.proxy.server.sync.bean.SecuritySyncPayloadData;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SecuritySyncBoot {

    private SecuritySyncContext mContext;

    private final NioUdpFactory mUdpFactory;

    /**
     * 是否已经初始化
     */
    private volatile boolean mIsInit = false;

    /**
     * 存储分布式服务的负载信息
     */
    private Map<String, SecuritySyncPayloadData> mSyncInfo;


    /**
     * 当前服务的负载信息
     */
    private SecuritySyncPayloadData mLocalSyncInfo;

    private SecuritySyncBoot() {
        mUdpFactory = new NioUdpFactory();
    }

    private static final class InnerClass {
        public static final SecuritySyncBoot sManager = new SecuritySyncBoot();
    }

    public static SecuritySyncBoot getInstance() {
        return InnerClass.sManager;
    }


    public void init(SecuritySyncContext context) throws IOException {
        if (mIsInit) {
            return;
        }
        mContext = context;

        mSyncInfo = new HashMap<>();
        mLocalSyncInfo = new SecuritySyncPayloadData(context.getMachineId(), context.getSyncHost());
        mLocalSyncInfo.setPort(context.getProxyPort());

        mUdpFactory.open();
        mIsInit = true;
    }

    /**
     * 是否初始化
     *
     * @return true为已经初始化
     */
    public boolean isInit() {
        return mIsInit;
    }

    /**
     * 更新指定机器码的链接数量
     *
     * @param mid       机器码
     * @param proxyPort 代理服务的端口号
     * @param avgLoad   负载值
     */
    public void updateSyncData(String mid, int proxyPort, byte avgLoad) {
        if (mSyncInfo == null) {
            return;
        }
        SecuritySyncPayloadData entity = mSyncInfo.get(mid);
        if (entity != null) {
            entity.updateAvgLoad(avgLoad);
            entity.setPort(proxyPort);
        }
    }

    /**
     * 获取低负载的服务，本地的服务返回null
     *
     * @return 返回低负载的服务地址
     */
    public SecuritySyncPayloadData getLowLoadServer() {
        if (mSyncInfo == null) {
            return null;
        }
        Collection<SecuritySyncPayloadData> collection = mSyncInfo.values();
        SecuritySyncPayloadData bestTarget = mLocalSyncInfo;
        for (SecuritySyncPayloadData entity : collection) {
            if (bestTarget.getAvgLoad() > entity.getAvgLoad()) {
                bestTarget = entity;
            }
        }
        if (bestTarget == mLocalSyncInfo) {
            //本地的服务返回空
            return null;
        }
        return bestTarget;
    }


    protected SecuritySyncPayloadData getNativeSyncInfo() {
        return mLocalSyncInfo;
    }

    protected Map<String, SecuritySyncPayloadData> getRemoteSyncInfo() {
        return mSyncInfo;
    }

    /**
     * 更新本机的负载值
     *
     * @param loadCount
     */
    public void updateNativeServerSyncInfo(long loadCount) {
        if (mLocalSyncInfo != null) {
            mLocalSyncInfo.updateConnectCount(loadCount);
        }
    }

    /**
     * 获取本机负载值
     *
     * @return
     */
    public long getNativeServerLoadCount() {
        if (mLocalSyncInfo != null) {
            return mLocalSyncInfo.getLoadCount();
        }
        return 0;
    }



    /**
     * 启动同步服务
     */
    public void bootSyncServer() {
        SecuritySyncService syncService = new SecuritySyncService(mContext);
        syncService.bindAddress(mContext.getSyncHost(), mContext.getSyncPort());
        mUdpFactory.getNetTaskComponent().addExecTask(syncService);
    }

    /**
     * 资源回收，释放通道组
     */
    public void release() {
        if (mIsInit) {
            if (mUdpFactory != null) {
                mUdpFactory.close();
            }
            mSyncInfo.clear();
            mIsInit = false;
        }
    }
}
