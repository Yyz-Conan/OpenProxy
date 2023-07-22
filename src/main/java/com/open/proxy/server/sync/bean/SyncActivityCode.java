package com.open.proxy.server.sync.bean;

public enum SyncActivityCode {

    /**
     * 同步服务
     */
    SYNC((byte) 4);

    private byte mCode;

    SyncActivityCode(byte code) {
        mCode = code;
    }

    public byte getCode() {
        return mCode;
    }


    public static SyncActivityCode getInstance(byte code) {
        if (code == SYNC.mCode) {
            return SYNC;
        }
        return null;
    }
}
