package com.open.proxy.server.sync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SecuritySyncContext {

    private Builder mBuilder;

    private SecuritySyncContext(Builder builder) {
        mBuilder = builder;
    }


    protected String getSyncHost() {
        return mBuilder.mSyncHost;
    }

    protected int getSyncPort() {
        return mBuilder.mSyncPort;
    }

    protected int getProxyPort() {
        return mBuilder.mProxyPort;
    }

    public String getMachineId() {
        return mBuilder.mMachineId;
    }

    public List<String> getMachineList() {
        return mBuilder.mMachineList;
    }

    public Map<String, String> getSyncServerList() {
        return mBuilder.mSyncServerList;
    }

    public static class Builder {

        /**
         * 机器id
         */
        private String mMachineId;

        /**
         * 同步服务端地址
         */
        private String mSyncHost;

        /**
         * 同步服务端的端口
         */
        private int mSyncPort;

        /**
         * 同步服务端地址
         */
        private String mProxyHost;

        /**
         * 同步服务端的端口
         */
        private int mProxyPort;

        /**
         * 同步服务列表
         */
        private Map<String, String> mSyncServerList = null;

        /**
         * 机器id列表
         */
        private List<String> mMachineList = new ArrayList<>();


        public Builder setMachineId(String machineId) {
            this.mMachineId = machineId;
            return this;
        }

        /**
         * 配置本地同步服务地址和端口信息
         *
         * @param syncHost
         * @param syncPort
         * @return
         */
        public Builder configSyncServer(String syncHost, int syncPort) {
            this.mSyncHost = syncHost;
            this.mSyncPort = syncPort;
            return this;
        }

        /**
         * 配置本地代理服务器地址和端口信息
         *
         * @param prosyHost
         * @param proxyPort
         * @return
         */
        public Builder configProxyServer(String prosyHost, int proxyPort) {
            this.mProxyHost = prosyHost;
            this.mProxyPort = proxyPort;
            return this;
        }


        /**
         * 配置要同步的服务器
         *
         * @param syncServerList
         * @return
         */
        public Builder configSyncServer(Map<String, String> syncServerList) {
            if (syncServerList != null) {
                mSyncServerList = new HashMap<>(syncServerList);
            }
            return this;
        }

        public SecuritySyncContext builder() {
            return new SecuritySyncContext(this);
        }

    }


}
