package connect.server;

import connect.AbsClient;
import connect.RemoteProxyReceiver;
import connect.network.nio.NioClientFactory;
import connect.network.nio.NioSender;
import log.LogDog;

import java.nio.channels.SocketChannel;

/**
 * 代理转发客户http请求
 */
public class ProxyConnectClient extends AbsClient {

    public ProxyConnectClient(String host, int port, NioSender localTarget) {
        if (localTarget == null || host == null || port <= 0) {
            throw new NullPointerException("data host port or target is null !!!");
        }
        setAddress(host, port, false);
        setReceive(new RemoteProxyReceiver(localTarget));
        setSender(new NioSender());
    }

    @Override
    protected void onConnectCompleteChannel(SocketChannel channel) {
        getSender().setChannel(channel);
        try {
            if (data != null) {
                //当前是http请求
                getSender().sendData(data);
            } else {
                //当前是https请求
                RemoteProxyReceiver receiver = getReceive();
                //响应代理请求
                receiver.getLocalTarget().sendData(httpsTunnelEstablished());
            }
        } catch (Exception e) {
            LogDog.e("==> host = " + getHost() + " port = " + getPort());
            NioClientFactory.getFactory().removeTask(this);
            e.printStackTrace();
        }
        data = null;
    }

    private byte[] httpsTunnelEstablished() {
        StringBuffer sb = new StringBuffer();
        sb.append("HTTP/1.1 200 Connection established\r\n");
        sb.append("Proxy-agent: YYD-HttpProxy\r\n");
        sb.append("\r\n");
        return sb.toString().getBytes();
    }

}
