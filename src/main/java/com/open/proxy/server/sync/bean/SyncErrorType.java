package com.open.proxy.server.sync.bean;

public enum SyncErrorType {

    EXP_SYNC_DATA(0x300, "illegal sync data"),

    EXP_SYNC_MACHINE_ID(0x301, "illegal sync  no super machineId");

    private String mErrorMsg;
    private int mErrorCode;

    SyncErrorType(int errorCode, String errorMsg) {
        this.mErrorCode = errorCode;
        this.mErrorMsg = errorMsg;
    }

    public int getErrorCode() {
        return mErrorCode;
    }

    public String getErrorMsg() {
        return mErrorMsg;
    }
}
