package com.open.proxy.server.sync;

import com.jav.net.nio.NioClientFactory;
import com.jav.net.nio.NioServerFactory;
import com.open.proxy.server.sync.bean.SecuritySyncEntity;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SecuritySyncBoot {

    private SecuritySyncContext mContext;

    private NioServerFactory mServerFactory;
    private NioClientFactory mClientFactory;

    /**
     * 是否已经初始化
     */
    private volatile boolean mIsInit = false;

    /**
     * 存储分布式服务的负载信息
     */
    private Map<String, SecuritySyncEntity> mSyncInfo;


    /**
     * 当前服务的负载信息
     */
    private SecuritySyncEntity mLocalSyncInfo;

    private SecuritySyncBoot() {
        mServerFactory = new NioServerFactory();
        mClientFactory = new NioClientFactory();
    }

    private static final class InnerClass {
        public static final SecuritySyncBoot sManager = new SecuritySyncBoot();
    }

    public static SecuritySyncBoot getInstance() {
        return InnerClass.sManager;
    }


    public void init(SecuritySyncContext context) {
        if (mIsInit) {
            return;
        }
        mContext = context;

        mLocalSyncInfo = new SecuritySyncEntity(context.getMachineId(), context.getSyncHost());
        mLocalSyncInfo.setProxyPort(context.getProxyPort());

        mServerFactory.open();
        mClientFactory.open();
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
        SecuritySyncEntity entity = mSyncInfo.get(mid);
        if (entity != null) {
            entity.updateAvgLoad(avgLoad);
            entity.setProxyPort(proxyPort);
        }
    }

    /**
     * 获取低负载的服务，本地的服务返回null
     *
     * @return 返回低负载的服务地址
     */
    public SecuritySyncEntity getLowLoadServer() {
        if (mSyncInfo == null) {
            return null;
        }
        Collection<SecuritySyncEntity> collection = mSyncInfo.values();
        SecuritySyncEntity bestTarget = mLocalSyncInfo;
        for (SecuritySyncEntity entity : collection) {
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


    protected SecuritySyncEntity getLocalSyncInfo() {
        return mLocalSyncInfo;
    }


    /**
     * 更新本机的负载值
     *
     * @param loadCount
     */
    public void updateLocalServerSyncInfo(long loadCount) {
        if (mLocalSyncInfo != null) {
            mLocalSyncInfo.updateConnectCount(loadCount);
        }
    }

    /**
     * 获取本机负载值
     *
     * @return
     */
    public long getLocalServerLoadCount() {
        if (mLocalSyncInfo != null) {
            return mLocalSyncInfo.getLoadCount();
        }
        return 0;
    }


    /**
     * 加载需要同步的服务列表
     */
    public void connectSyncServer() {
        Map<String, String> syncServer = mContext.getSyncServer();
        if (syncServer == null || syncServer.isEmpty()) {
            return;
        }
        mSyncInfo = new HashMap(syncServer.size());
        Set<Map.Entry<String, String>> entrySet = syncServer.entrySet();
        for (Map.Entry<String, String> entry : entrySet) {
            String value = entry.getValue();
            String[] arrays = value.split(":");
            if (arrays.length != 2) {
                continue;
            }
            String key = entry.getKey();
            int port = Integer.parseInt(arrays[1]);
            SecuritySyncEntity entity = new SecuritySyncEntity(key, mContext.getSyncHost());
            mSyncInfo.put(key, entity);
            SecuritySyncServerReception reception = new SecuritySyncServerReception(mContext);
            reception.setAddress(arrays[0], port);

            mClientFactory.getNetTaskComponent().addExecTask(reception);
        }
    }

    /**
     * 启动同步服务
     */
    public void bootSyncServer() {
        SecuritySyncService syncService = new SecuritySyncService(mContext, mClientFactory);
        syncService.setAddress(mContext.getSyncHost(), mContext.getSyncPort());
        mServerFactory.getNetTaskComponent().addExecTask(syncService);
    }

    /**
     * 资源回收，释放通道组
     */
    public void release() {
        if (mIsInit) {
            if (mClientFactory != null) {
                mClientFactory.close();
            }
            if (mServerFactory != null) {
                mServerFactory.close();
            }
            mSyncInfo.clear();
            mIsInit = false;
        }
    }
}
