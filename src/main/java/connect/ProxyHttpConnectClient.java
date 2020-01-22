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
    private byte[] data;

    public ProxyHttpConnectClient(String host, int port, NioSender target, byte[] data) {
        if (target == null || host == null || port <= 0) {
            throw new NullPointerException("data host port or target is null !!!");
        }
        setAddress(host, port);
        this.target = target;
        this.data = data;
        setConnectTimeout(0);
        setSender(new RequestSender(this));
        setReceive(new RequestReceive(this, "onReceiveHttpData"));
    }

    @Override
    protected void onConfigSocket(boolean isConnect, SocketChannel channel) {
        if (isConnect) {
            getSender().setChannel(channel);
            getSender().sendData(data);
            data = null;
        }
    }

    @JavKeep
    private void onReceiveHttpData(byte[] data) {
        target.sendData(data);
    }
}
