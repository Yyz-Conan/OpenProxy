package com.open.proxy.server.joggle;

/**
 * 解密状态
 */
public enum DecryptionStatus {

    /**
     * tag校验阶段
     */
    TAG,
    /**
     * 数据大小获取阶段
     */
    SIZE,
    /**
     * 获取数据阶段
     */
    DATA
}
