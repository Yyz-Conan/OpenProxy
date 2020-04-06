package connect.clinet;

import connect.LocalRequestReceiver;
import connect.joggle.ICloseListener;
import connect.network.base.joggle.INetReceiver;
import connect.network.nio.NioClientTask;
import connect.network.nio.NioHPCClientFactory;
import connect.network.nio.NioSender;
import connect.network.xhttp.ByteCacheStream;
import connect.network.xhttp.entity.XResponse;
import connect.network.xhttp.entity.XResponseHelper;
import intercept.InterceptFilterManager;
import log.LogDog;
import util.StringEnvoy;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * 处理浏览器请求，转发给代理服务端
 */
public class LocalProxyClient extends NioClientTask implements ICloseListener {

    private RemoteProxyClient connectRemoteProxyServer;
    private String requestHost = null;

    public LocalProxyClient(SocketChannel channel, String remoteHost, int remotePort) {
        super(channel, null);
        setSender(new NioSender(channel));
        setReceive(new LocalRequestReceiver(new ReceiveCallBack()));
        //创建连接代理服务端
        connectRemoteProxyServer = new RemoteProxyClient(getSender(), remoteHost, remotePort);
        connectRemoteProxyServer.setOnCloseListener(this);
        NioHPCClientFactory.getFactory().addTask(connectRemoteProxyServer);
    }

    class ReceiveCallBack implements INetReceiver<XResponse> {


        @Override
        public void onReceive(XResponse response, Exception e) {
            if (e != null) {
                return;
            }
            String newRequestHost = XResponseHelper.getHost(response);
            //黑名单过滤
            if (InterceptFilterManager.getInstance().isIntercept(newRequestHost)) {
                NioHPCClientFactory.getFactory().removeTask(LocalProxyClient.this);
                requestHost = newRequestHost;
                return;
            }

            String method = XResponseHelper.getRequestMethod(response);
            LocalRequestReceiver receiver = getReceive();
            receiver.setRequestSender(connectRemoteProxyServer.getSender());
            if ("CONNECT".equals(method)) {
                receiver.setTLS();
            }

//            ByteCacheStream stream = response.getRawData();
//            LogDog.d("Browser initiated request = " + stream.toString());
            LogDog.d("Browser initiated request host = " + newRequestHost);

            try {
                ByteCacheStream raw = response.getRawData();
                connectRemoteProxyServer.getSender().sendData(raw.toByteArray());
            } catch (IOException ex) {
                ex.printStackTrace();
                NioHPCClientFactory.getFactory().removeTask(LocalProxyClient.this);
            }
            requestHost = newRequestHost;
        }
    }

    @Override
    protected void onCloseClientChannel() {
        LogDog.d("==> Connect close " + requestHost + " [ remover connect count = " + LocalProxyServer.localConnectCount.decrementAndGet() + " ] ");
        NioHPCClientFactory.getFactory().removeTask(connectRemoteProxyServer);
    }

    @Override
    public void onClose(String host) {
        if (StringEnvoy.isNotEmpty(host) && host.equals(requestHost)) {
            NioHPCClientFactory.getFactory().removeTask(LocalProxyClient.this);
        }
    }
}