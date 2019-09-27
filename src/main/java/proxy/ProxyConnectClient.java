package proxy;

import connect.network.nio.NioClientTask;
import connect.network.nio.NioHPCSender;
import connect.network.nio.NioSender;

/**
 * 代理转发客户http请求
 */
public class ProxyConnectClient extends NioClientTask {

    private NioSender target;
    private byte[] htmlData;

    public ProxyConnectClient(byte[] data, String host, int port, NioSender target) {
        if (data == null || target == null || host == null || port <= 0) {
            throw new NullPointerException("data host port or target is null !!!");
        }
        setAddress(host, port);
        this.target = target;
        this.htmlData = data;
        setConnectTimeout(0);
        setSender(new NioHPCSender());
        setReceive(new HttpReceive(this, "onHttpSubmitCallBack"));
    }

    @Override
    protected void onConnectSocketChannel(boolean isConnect) {
        if (isConnect) {
            try {
                getSender().sendData(htmlData);
            } catch (Exception e) {
            }
            htmlData = null;
        }
    }

    private void onHttpSubmitCallBack(byte[] data) {
//        if (data.length == 0) {
//            NioClientFactory.getFactory().removeTask(this);
//            return;
//        }
//        String html = new String(data);
//        if (html.length() > 20) {
//            LogDog.d("==> ProxyConnectClient onHttpSubmitCallBack = " + html.substring(0, 20));
//        } else {
//            LogDog.d("==> ProxyConnectClient onHttpSubmitCallBack = " + html);
//        }
        target.sendData(data);
    }
}
