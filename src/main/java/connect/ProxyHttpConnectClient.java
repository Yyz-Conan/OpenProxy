package connect;

import connect.network.nio.NioClientTask;
import connect.network.nio.NioSender;
import util.joggle.JavKeep;

import java.nio.channels.SocketChannel;

/**
 * 代理转发客户http请求
 */
public class ProxyHttpConnectClient extends NioClientTask {

    private NioSender target;
    private ConnectPool connectPool = null;

    public ProxyHttpConnectClient(String host, int port, NioSender target) {
        if (target == null || host == null || port <= 0) {
            throw new NullPointerException("data host port or target is null !!!");
        }
        setAddress(host, port);
        this.target = target;
        setConnectTimeout(0);
        setSender(new RequestSender());
        setReceive(new RequestReceive(this, "onReceiveHttpData"));
    }

    @Override
    protected void onConfigSocket(boolean isConnect, SocketChannel channel) {
        if (isConnect) {
            connectPool.put(getHost(), this);
        }
    }

    public void setConnectPool(ConnectPool connectPool) {
        this.connectPool = connectPool;
    }

    @JavKeep
    private void onReceiveHttpData(byte[] data) {
        target.sendData(data);
    }

    @Override
    protected void onCloseSocketChannel() {
        if (connectPool != null) {
            connectPool.remove(getHost());
        }
    }
}
