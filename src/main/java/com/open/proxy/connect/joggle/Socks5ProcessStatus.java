package com.open.proxy.connect.joggle;

public enum Socks5ProcessStatus {
    /**
     * 客户端初次请求
     */
    HELLO,
    /**
     * 校验客户端合法性阶段
     */
    VERIFICATION,
    /**
     * 校验命令阶段
     */
    COMMAND,
    /**
     * 当前模式下不对数据进行任何处理，直接转发
     */
    FORWARD
}
