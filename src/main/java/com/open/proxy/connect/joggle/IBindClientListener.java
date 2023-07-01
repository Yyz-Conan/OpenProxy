package com.open.proxy.connect.joggle;

import com.jav.net.entity.MultiByteBuffer;

/**
 * 绑定客户端状态
 *
 * @author yyz
 */
public interface IBindClientListener {

    /**
     * 客户端就绪
     *
     * @param requestId
     */
    void onBindClientByReady(String requestId);

    /**
     * 客户端创建链接失败
     *
     * @param requestId
     */
    void onBindClientByError(String requestId);


    /**
     * 客户端接收数据
     *
     * @param requestId
     * @param buffer
     */
    void onBindClientData(String requestId, MultiByteBuffer buffer);

    /**
     * 客户端关闭则回调
     *
     * @param requestId
     */
    void onBindClientClose(String requestId);
}
