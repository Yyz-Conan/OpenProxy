package com.open.proxy.server.joggle;

/**
 * 版本更新校验流程回调接口
 *
 * @author yyz
 */
public interface IUpdateAffairsCallBack {

    /**
     * 服务端模式下检查客户端版本信息回调
     *
     * @param version
     */
    void onServerCheckVersion(int version);

    /**
     * 客户端模式下检查服务端版本信息回调
     *
     * @param isHasNewVersion
     * @param callBack
     */
    void onClientCheckVersion(boolean isHasNewVersion, IUpdateConfirmCallBack callBack);

    /**
     * 获取保存更新文件存储路径
     *
     * @return
     */
    String getSaveFile();

    /**
     * 更新流程完成回调
     */
    void onUpdateComplete();
}
