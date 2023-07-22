package com.open.proxy.server.joggle;


import com.jav.net.entity.MultiByteBuffer;
import com.open.proxy.protocol.Socks5Generator;

import java.util.List;

/**
 * socks5交互流程回调接口
 *
 * @author yyz
 */
public interface ISocks5ProcessListener {

    /**
     * 回调客户端支持的加密方法
     *
     * @param methods 支持的加密集
     * @return 返回选择后的加密
     */
    Socks5Generator.Socks5Verification onClientSupportMethod(List<Socks5Generator.Socks5Verification> methods);

    /**
     * 校验socks5客户端的合法信息
     *
     * @param userName 用户名
     * @param password 密码
     * @return true为校验通过
     */
    boolean onVerification(String userName, String password);

    /**
     * 回报代理socks5客户端命令处理状态
     *
     * @param status
     */
    void onReportCommandStatus(Socks5Generator.Socks5CommandStatus status);

    /**
     * 提取socks5代理目标地址
     *
     * @param targetAddress
     * @param targetPort
     */
    void onBeginProxy(String targetAddress, int targetPort);

    /**
     * 把远程服务或者真实目标服务数据回传给代理客户端
     *
     * @param buffer
     */
    void onDownStreamData(MultiByteBuffer buffer);

    /**
     * 把代理客户端请求的数据中转发送给目标服务（远程代理服务或者真实目标服务）
     *
     * @param buffer
     */
    void onUpstreamData(MultiByteBuffer buffer);
}
