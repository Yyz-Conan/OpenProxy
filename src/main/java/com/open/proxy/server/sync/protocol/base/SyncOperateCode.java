package com.open.proxy.server.sync.protocol.base;

/**
 * sync operate code 定义
 */
public enum SyncOperateCode {

    /**
     * sync avg
     */
    SYNC_AVG((byte) 0),
    /**
     * sync avg
     */
    RESPOND_SYNC_AVG((byte) 1),

    /**
     * sync mid
     */
    SYNC_MID((byte) 2),

    /**
     * respond sync mid
     */
    RESPOND_SYNC_MID((byte) 3);

    private final byte mCode;

    SyncOperateCode(byte type) {
        mCode = type;
    }

    public byte getCode() {
        return mCode;
    }

    public static SyncOperateCode getInstance(byte code) {
        if (code == SYNC_AVG.mCode) {
            return SYNC_AVG;
        } else if (code == SYNC_MID.mCode) {
            return SYNC_MID;
        }
        return null;
    }
}