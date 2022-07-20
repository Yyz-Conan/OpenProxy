package com.open.proxy.connect.http.client;

import com.jav.common.log.LogDog;
import com.jav.common.track.SpiderEnvoy;
import com.jav.common.util.ConfigFileEnvoy;
import com.jav.common.util.StringEnvoy;
import com.jav.net.base.joggle.INetReceiver;
import com.jav.net.base.joggle.INetSender;
import com.jav.net.base.joggle.ISenderFeedback;
import com.jav.net.entity.MultiByteBuffer;
import com.jav.net.entity.NetTaskStatusCode;
import com.jav.net.nio.NioClientFactory;
import com.jav.net.nio.NioClientTask;
import com.jav.net.xhttp.entity.XResponse;
import com.jav.net.xhttp.utils.ByteCacheStream;
import com.jav.net.xhttp.utils.XResponseHelper;
import com.open.proxy.IConfigKey;
import com.open.proxy.OPContext;
import com.open.proxy.connect.EncryptionSender;
import com.open.proxy.connect.HttpDecryptionReceiver;
import com.open.proxy.connect.http.server.MultipleProxyServer;
import com.open.proxy.connect.joggle.IRemoteClientCloseListener;
import com.open.proxy.intercept.InterceptFilterManager;
import com.open.proxy.intercept.ProxyFilterManager;
import com.open.proxy.protocol.DataPacketTag;
import com.open.proxy.protocol.HtmlGenerator;

import java.nio.channels.SocketChannel;

/**
 * 接待需要代理的客户端
 */
public class LocalClient extends NioClientTask implements IRemoteClientCloseListener, ISenderFeedback, INetReceiver<XResponse> {

    private boolean isServerMode;
    private boolean isBlacklist = false;
    private String requestHost = null;
    private NioClientTask transmissionProxyClient;
    private HttpDecryptionReceiver receiver;

    private boolean isEnableProxy;
    private boolean allowProxy;

    public LocalClient(SocketChannel channel) {
        super(channel, null);

        ConfigFileEnvoy cFileEnvoy = OPContext.getInstance().getConfigFileEnvoy();
        isEnableProxy = cFileEnvoy.getBooleanValue(IConfigKey.CONFIG_ENABLE_PROXY);
        boolean isDebug = cFileEnvoy.getBooleanValue(IConfigKey.KEY_DEBUG_MODE);
        SpiderEnvoy.getInstance().setEnablePrint(isDebug);
        SpiderEnvoy.getInstance().startWatchKey(LocalClient.this.toString());

        isServerMode = cFileEnvoy.getBooleanValue(IConfigKey.CONFIG_IS_SERVER_MODE);
        allowProxy = cFileEnvoy.getBooleanValue(IConfigKey.CONFIG_ALLOW_PROXY);

        //创建接收者
        receiver = new HttpDecryptionReceiver(isServerMode);
        //设置数据回调接口
        receiver.setDataReceiver(this);
        setReceiver(receiver.getReceiver());
        //设置发送者
        EncryptionSender sender = new EncryptionSender(isServerMode);
        sender.setEncodeTag(DataPacketTag.PACK_PROXY_TAG);
        //设置发送数据状态回调
        sender.setSenderFeedback(this);
        setSender(sender);
    }

    @Override
    protected void onBeReadyChannel(SocketChannel channel) {
        getSender().setChannel(getSelectionKey(), channel);
        SpiderEnvoy.getInstance().pinKeyProbe(LocalClient.this.toString(), "onBeReadyChannel");
    }


    @Override
    public void onReceiveFullData(XResponse response, Throwable e) {
        String newRequestHost = XResponseHelper.getHost(response);
        if (StringEnvoy.isEmpty(newRequestHost) && StringEnvoy.isEmpty(requestHost)) {
            String msg = "newRequestHost and requestHost is null ";
            SpiderEnvoy.getInstance().pinKeyProbe(LocalClient.this.toString(), msg);
            byte[] data = response.getRawData().toByteArray();
            if (data != null && data.length > 0) {
                LogDog.d("==>browser heartbeat data = " + new String(data));
            }
            NioClientFactory.getFactory().getNetTaskContainer().addUnExecTask(LocalClient.this);
            return;
        }

        //黑名单过滤
        if (InterceptFilterManager.getInstance().isIntercept(newRequestHost)) {
            isBlacklist = true;
            LogDog.e("++> com.open.proxy.intercept blacklist host = " + newRequestHost);
//            if (XResponseHelper.isTLS(response)) {
//                getSender().sendData(HtmlGenerator.httpsTunnelEstablished());
//            } else {
            getSender().sendData(new MultiByteBuffer(HtmlGenerator.headDenialService()));
//            }
            requestHost = newRequestHost;
            getSender().sendData(new MultiByteBuffer(HtmlGenerator.createInterceptHtml(newRequestHost)));

            String msg = "newRequestHost in black menu";
            SpiderEnvoy.getInstance().pinKeyProbe(toString(), msg);
            return;
        }

        if (StringEnvoy.isNotEmpty(requestHost) && !newRequestHost.equals(requestHost)) {
            //发现当前请求的网站跟上次请求的网站不一样，则关闭之前的链接（只限制运行于服务模式）
            NioClientFactory.getFactory().getNetTaskContainer().addUnExecTask(transmissionProxyClient);
            transmissionProxyClient = null;
            String msg = "the new request host = " + newRequestHost + " is inconsistent with the current request host = " + requestHost;
            SpiderEnvoy.getInstance().pinKeyProbe(LocalClient.this.toString(), msg);
        }

        if (transmissionProxyClient != null) {
            //如果是同个域名请求则复用链路
            ByteCacheStream raw = response.getRawData();
            INetSender sender = transmissionProxyClient.getSender();
            if (sender != null) {
                sender.sendData(new MultiByteBuffer(raw.toByteArray()));
            }
            String msg = "multiplex curt proxy send data, newRequestHost = " + newRequestHost + " requestHost = " + requestHost;
            SpiderEnvoy.getInstance().pinKeyProbe(LocalClient.this.toString(), msg);
            LogDog.d(msg);
        } else {
            LogDog.d("<-x_proxy-> Connect " + newRequestHost);
            TransmissionProxyClient client;
            int port = XResponseHelper.getPort(response);
            client = new TransmissionProxyClient(getSender(), receiver, response);
            if (isServerMode) {
                //当前是服务模式，请求指定的域名，需要响应 com.open.proxy.connect 请求
                client.enableLocalConnect(newRequestHost, port);
                String msg = "server model create client newRequestHost = " + newRequestHost + " port = " + port;
                SpiderEnvoy.getInstance().pinKeyProbe(LocalClient.this.toString(), msg);
            } else {
                //当前是客户端模式
                boolean isNeedProxy = allowProxy;
                if (!allowProxy) {
                    //当前配置没有开启所有链接走代理，则需要判断代理名单列表判断是否需要走代理
                    isNeedProxy = ProxyFilterManager.getInstance().isNeedProxy(newRequestHost);
                }
                if (isNeedProxy && isEnableProxy) {
                    //在代理名单并且开启了代理
                    client.enableProxyConnect(newRequestHost);
                    String msg = "client model need proxy create proxy client realHost = " + newRequestHost;
                    SpiderEnvoy.getInstance().pinKeyProbe(LocalClient.this.toString(), msg);
                } else {
                    //不能走代理
                    client.enableLocalConnect(newRequestHost, port);
                    String msg = "client model local net model create client newRequestHost = " + newRequestHost + " port = " + port;
                    SpiderEnvoy.getInstance().pinKeyProbe(LocalClient.this.toString(), msg);
                }
            }
            client.setOnCloseListener(LocalClient.this);
            NioClientFactory.getFactory().getNetTaskContainer().addExecTask(client);
            transmissionProxyClient = client;
            requestHost = newRequestHost;
        }
    }


    @Override
    public void onClientClose(String host) {
        if (StringEnvoy.isNotEmpty(host) && host.equals(requestHost) && getTaskStatus() == NetTaskStatusCode.RUN) {
            NioClientFactory.getFactory().getNetTaskContainer().addUnExecTask(LocalClient.this);
            String msg = "onClose requestHost = " + requestHost + " proxy host = " + host;
            SpiderEnvoy.getInstance().pinKeyProbe(LocalClient.this.toString(), msg);
        }
    }

    @Override
    public void onSenderFeedBack(INetSender iNetSender, Object data, Throwable throwable) {
        if (throwable != null || isBlacklist) {
            NioClientFactory.getFactory().getNetTaskContainer().addUnExecTask(LocalClient.this);
        }
    }


    @Override
    protected void onCloseChannel() {
        if (requestHost == null) {
            LogDog.d("--> browser heartbeat !!!" + " [ com.open.proxy.connect count = " + MultipleProxyServer.localConnectCount.decrementAndGet() + " ] ");
        } else {
            LogDog.d("--> close " + requestHost + " [ com.open.proxy.connect count = " + MultipleProxyServer.localConnectCount.decrementAndGet() + " ] ");
        }
        NioClientFactory.getFactory().getNetTaskContainer().addUnExecTask(transmissionProxyClient);

        String report = SpiderEnvoy.getInstance().endWatchKey(LocalClient.this.toString());
        LogDog.saveLog(report);
    }

}
