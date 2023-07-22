package com.open.proxy.server.sync;

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

    public Map<String, String> getSyncServer() {
        return mBuilder.mSyncServer;
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
        private Map<String, String> mSyncServer;

        /**
         * 机器id列表
         */
        private List<String> mMachineList;


        public Builder setMachineId(String machineId) {
            this.mMachineId = machineId;
            return this;
        }


        public Builder configSyncServer(String syncHost, int syncPort) {
            this.mSyncHost = syncHost;
            this.mSyncPort = syncPort;
            return this;
        }

        public Builder configProxyServer(String prosyHost, int prxyPort) {
            this.mProxyHost = prosyHost;
            this.mProxyPort = prxyPort;
            return this;
        }


        public Builder setSyncServer(Map<String, String> syncServer) {
            this.mSyncServer = syncServer;
            return this;
        }

        public Builder setMachineList(List<String> machineList) {
            this.mMachineList = machineList;
            return this;
        }

        public SecuritySyncContext builder() {
            return new SecuritySyncContext(this);
        }

    }


}
