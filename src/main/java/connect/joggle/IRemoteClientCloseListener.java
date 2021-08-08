package connect.joggle;

/**
 * 远程代理客户端关闭回调
 */
public interface IRemoteClientCloseListener {

    /**
     * 远程代理客户端关闭
     * @param host
     */
    void onClientClose(String host);
}
