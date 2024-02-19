package com.open.proxy.server.sync.joggle;

import java.net.InetSocketAddress;

/**
 * sync 服务事件回调
 *
 * @author yyz
 */
public interface ISyncServerEventCallBack {

    /**
     * 同步负载回调
     *
     * @param operateCode 请求状态还是响应状态
     * @param proxyPort   远程代理服务的端口
     * @param machineId   机器id
     * @param loadCount   负载值
     */
    void onRespondSyncCallBack(InetSocketAddress remoteAddress, byte operateCode, int proxyPort, String machineId, byte loadCount);


    /**
     * 同步machine Id 结果回调
     *
     * @param status    0 为成功，64 为失败
     * @param machineId 机器id
     */
    void onRespondSyncMidCallBack(byte status, String machineId);
}
