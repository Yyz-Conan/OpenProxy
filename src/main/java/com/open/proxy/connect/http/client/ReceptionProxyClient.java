package com.open.proxy.connect.http.client;

import com.currency.net.base.joggle.INetReceiver;
import com.currency.net.base.joggle.INetSender;
import com.currency.net.base.joggle.ISenderFeedback;
import com.currency.net.entity.NetTaskStatusCode;
import com.currency.net.nio.NioClientFactory;
import com.currency.net.nio.NioClientTask;
import com.currency.net.xhttp.entity.XResponse;
import com.currency.net.xhttp.utils.ByteCacheStream;
import com.currency.net.xhttp.utils.XResponseHelper;
import com.open.proxy.ConfigKey;
import com.open.proxy.connect.EncryptionSender;
import com.open.proxy.connect.HttpDecryptionReceiver;
import com.open.proxy.connect.http.server.MultipleProxyServer;
import com.open.proxy.connect.joggle.IRemoteClientCloseListener;
import com.open.proxy.intercept.InterceptFilterManager;
import com.open.proxy.intercept.ProxyFilterManager;
import log.LogDog;
import com.open.proxy.protocol.DataPacketTag;
import com.open.proxy.protocol.HtmlGenerator;
import track.SpiderEnvoy;
import util.AnalysisConfig;
import util.StringEnvoy;

import java.nio.channels.SocketChannel;

/**
 * 接待需要代理的客户端
 */
public class ReceptionProxyClient extends NioClientTask implements IRemoteClientCloseListener, ISenderFeedback, INetReceiver<XResponse> {

    private boolean isServerMode;
    private boolean isBlacklist = false;
    private String requestHost = null;
    private NioClientTask transmissionProxyClient;
    private HttpDecryptionReceiver receiver;

    private boolean isDebug;
    private boolean isEnableProxy;
    private boolean allowProxy;

    public ReceptionProxyClient(SocketChannel channel) {
        super(channel, null);

        isEnableProxy = AnalysisConfig.getInstance().getBooleanValue(ConfigKey.CONFIG_ENABLE_PROXY);
        isDebug = AnalysisConfig.getInstance().getBooleanValue(ConfigKey.KEY_DEBUG_MODE);
        if (isDebug) {
            SpiderEnvoy.getInstance().startWatchKey(ReceptionProxyClient.this.toString());
        }

        isServerMode = AnalysisConfig.getInstance().getBooleanValue(ConfigKey.CONFIG_IS_SERVER_MODE);
        allowProxy = AnalysisConfig.getInstance().getBooleanValue(ConfigKey.CONFIG_ALLOW_PROXY);
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
        if (isDebug) {
            SpiderEnvoy.getInstance().pinKeyProbe(ReceptionProxyClient.this.toString(), "onBeReadyChannel");
        }
    }


    @Override
    public void onReceiveFullData(XResponse response, Throwable e) {
        String newRequestHost = XResponseHelper.getHost(response);
        if (StringEnvoy.isEmpty(newRequestHost) && StringEnvoy.isEmpty(requestHost)) {
            if (isDebug) {
                String msg = "newRequestHost and requestHost is null ";
                SpiderEnvoy.getInstance().pinKeyProbe(ReceptionProxyClient.this.toString(), msg);
            }
            byte[] data = response.getRawData().toByteArray();
            if (data != null && data.length > 0) {
                LogDog.d("==>browser heartbeat data = " + new String(data));
            }
            NioClientFactory.getFactory().getNetTaskContainer().addUnExecTask(ReceptionProxyClient.this);
            return;
        }

        //黑名单过滤
        if (InterceptFilterManager.getInstance().isIntercept(newRequestHost)) {
            isBlacklist = true;
            LogDog.e("++> com.open.proxy.intercept blacklist host = " + newRequestHost);
//            if (XResponseHelper.isTLS(response)) {
//                getSender().sendData(HtmlGenerator.httpsTunnelEstablished());
//            } else {
            getSender().sendData(HtmlGenerator.headDenialService());
//            }
            requestHost = newRequestHost;
            getSender().sendData(HtmlGenerator.createInterceptHtml(newRequestHost));

            if (isDebug) {
                String msg = "newRequestHost in black menu";
                SpiderEnvoy.getInstance().pinKeyProbe(toString(), msg);
            }
            return;
        }

        if (StringEnvoy.isNotEmpty(requestHost) && !newRequestHost.equals(requestHost)) {
            //发现当前请求的网站跟上次请求的网站不一样，则关闭之前的链接（只限制运行于服务模式）
            NioClientFactory.getFactory().getNetTaskContainer().addUnExecTask(transmissionProxyClient);
            transmissionProxyClient = null;
            String msg = "the new request host = " + newRequestHost + " is inconsistent with the current request host = " + requestHost;
            if (isDebug) {
                SpiderEnvoy.getInstance().pinKeyProbe(ReceptionProxyClient.this.toString(), msg);
            }
        }

        if (transmissionProxyClient != null) {
            //如果是同个域名请求则复用链路
            ByteCacheStream raw = response.getRawData();
            INetSender sender = transmissionProxyClient.getSender();
            if (sender != null) {
                sender.sendData(raw.toByteArray());
            }
            String msg = "multiplex curt proxy send data, newRequestHost = " + newRequestHost + " requestHost = " + requestHost;
            if (isDebug) {
                SpiderEnvoy.getInstance().pinKeyProbe(ReceptionProxyClient.this.toString(), msg);
            }
            LogDog.d(msg);
        } else {
            LogDog.d("<-x_proxy-> Connect " + newRequestHost);
            TransmissionProxyClient client;
            int port = XResponseHelper.getPort(response);
            client = new TransmissionProxyClient(getSender(), receiver, response);
            if (isServerMode) {
                //当前是服务模式，请求指定的域名，需要响应 com.open.proxy.connect 请求
                client.enableLocalConnect(newRequestHost, port);
                if (isDebug) {
                    String msg = "server model create client newRequestHost = " + newRequestHost + " port = " + port;
                    SpiderEnvoy.getInstance().pinKeyProbe(ReceptionProxyClient.this.toString(), msg);
                }
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
                    if (isDebug) {
                        String msg = "client model need proxy create proxy client realHost = " + newRequestHost;
                        SpiderEnvoy.getInstance().pinKeyProbe(ReceptionProxyClient.this.toString(), msg);
                    }
                } else {
                    //不能走代理
                    client.enableLocalConnect(newRequestHost, port);
                    if (isDebug) {
                        String msg = "client model local net model create client newRequestHost = " + newRequestHost + " port = " + port;
                        SpiderEnvoy.getInstance().pinKeyProbe(ReceptionProxyClient.this.toString(), msg);
                    }
                }
            }
            client.setOnCloseListener(ReceptionProxyClient.this);
            NioClientFactory.getFactory().getNetTaskContainer().addExecTask(client);
            transmissionProxyClient = client;
            requestHost = newRequestHost;
        }
    }


    @Override
    public void onClientClose(String host) {
        if (StringEnvoy.isNotEmpty(host) && host.equals(requestHost) && getTaskStatus() == NetTaskStatusCode.RUN) {
            NioClientFactory.getFactory().getNetTaskContainer().addUnExecTask(ReceptionProxyClient.this);
            if (isDebug) {
                String msg = "onClose requestHost = " + requestHost + " proxy host = " + host;
                SpiderEnvoy.getInstance().pinKeyProbe(ReceptionProxyClient.this.toString(), msg);
            }
        }
    }

    @Override
    public void onSenderFeedBack(INetSender iNetSender, Object data, Throwable throwable) {
        if (throwable != null || isBlacklist) {
            NioClientFactory.getFactory().getNetTaskContainer().addUnExecTask(ReceptionProxyClient.this);
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

        if (isDebug) {
            String report = SpiderEnvoy.getInstance().endWatchKey(ReceptionProxyClient.this.toString());
            LogDog.saveLog(report);
        }
    }

}
