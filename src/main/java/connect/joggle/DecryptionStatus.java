package connect.joggle;

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
