package connect.http.client;

import config.AnalysisConfig;
import config.ConfigKey;
import connect.EncryptionSender;
import connect.HttpDecryptionReceiver;
import connect.http.server.MultipleProxyServer;
import connect.joggle.IRemoteClientCloseListener;
import connect.network.base.NetTaskStatus;
import connect.network.base.joggle.INetReceiver;
import connect.network.base.joggle.INetSender;
import connect.network.base.joggle.ISenderFeedback;
import connect.network.nio.NioClientFactory;
import connect.network.nio.NioClientTask;
import connect.network.xhttp.XMultiplexCacheManger;
import connect.network.xhttp.entity.XResponse;
import connect.network.xhttp.utils.ByteCacheStream;
import connect.network.xhttp.utils.MultiLevelBuf;
import connect.network.xhttp.utils.XResponseHelper;
import intercept.InterceptFilterManager;
import intercept.ProxyFilterManager;
import log.LogDog;
import protocol.DataPacketTag;
import protocol.HtmlGenerator;
import track.SpiderEnvoy;
import util.StringEnvoy;

import java.nio.channels.SocketChannel;

/**
 * 接待需要代理的客户端
 */
public class ReceptionProxyClient extends NioClientTask implements IRemoteClientCloseListener, ISenderFeedback, INetReceiver<XResponse> {

    private String requestHost = null;
    private boolean isBlacklist = false;
    private NioClientTask transmissionProxyClient;
    private boolean isServerMode;
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
        setReceive(receiver.getReceiver());
        //设置发送者
        EncryptionSender sender = new EncryptionSender(isServerMode);
        sender.setEncodeTag(DataPacketTag.PACK_PROXY_TAG);
        //设置发送数据状态回调
        sender.setSenderFeedback(this);
        setSender(sender);
    }

    @Override
    protected void onConnectCompleteChannel(SocketChannel channel) {
        super.onConnectCompleteChannel(channel);
        getSender().setChannel(selectionKey, channel);
        if (isDebug) {
            SpiderEnvoy.getInstance().pinKeyProbe(ReceptionProxyClient.this.toString(), "onConnectCompleteChannel");
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
            NioClientFactory.getFactory().removeTask(ReceptionProxyClient.this);
            return;
        }

        //黑名单过滤
        if (InterceptFilterManager.getInstance().isIntercept(newRequestHost)) {
            isBlacklist = true;
            LogDog.e("++> intercept blacklist host = " + newRequestHost);
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
            NioClientFactory.getFactory().removeTask(transmissionProxyClient);
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
                //当前是服务模式，请求指定的域名，需要响应 connect 请求
                client.enableLocalConnect(newRequestHost, port);
                if (isDebug) {
                    String msg = "server model create client newRequestHost = " + newRequestHost + " port = " + port;
                    SpiderEnvoy.getInstance().pinKeyProbe(ReceptionProxyClient.this.toString(), msg);
                }
            } else {
                //当前是客户端模式
                boolean isNeedProxy = allowProxy;
                if (!allowProxy) {
                    isNeedProxy = ProxyFilterManager.getInstance().isNeedProxy(newRequestHost);
                }
                if (isNeedProxy && isEnableProxy) {
                    //走代理服务访问
                    client.enableProxyConnect(newRequestHost);
                    if (isDebug) {
                        String msg = "client model need proxy create proxy client realHost = " + newRequestHost;
                        SpiderEnvoy.getInstance().pinKeyProbe(ReceptionProxyClient.this.toString(), msg);
                    }
                } else {
                    client.enableLocalConnect(newRequestHost, port);
                    if (isDebug) {
                        String msg = "client model local net model create client newRequestHost = " + newRequestHost + " port = " + port;
                        SpiderEnvoy.getInstance().pinKeyProbe(ReceptionProxyClient.this.toString(), msg);
                    }
                }
            }
            client.setOnCloseListener(ReceptionProxyClient.this);
            NioClientFactory.getFactory().addTask(client);
            transmissionProxyClient = client;
            requestHost = newRequestHost;
        }
    }


    @Override
    public void onClientClose(String host) {
        if (StringEnvoy.isNotEmpty(host) && host.equals(requestHost) && getTaskStatus() == NetTaskStatus.RUN) {
            NioClientFactory.getFactory().removeTask(ReceptionProxyClient.this);
            if (isDebug) {
                String msg = "onClose requestHost = " + requestHost + " proxy host = " + host;
                SpiderEnvoy.getInstance().pinKeyProbe(ReceptionProxyClient.this.toString(), msg);
            }
        }
    }

    @Override
    public void onSenderFeedBack(INetSender iNetSender, Object data, Throwable throwable) {
        if (throwable != null) {
            NioClientFactory.getFactory().removeTask(transmissionProxyClient);
        }
        if (data instanceof MultiLevelBuf) {
            XMultiplexCacheManger.getInstance().lose((MultiLevelBuf) data);
        }
        if (isBlacklist) {
            NioClientFactory.getFactory().removeTask(ReceptionProxyClient.this);
        }
    }

    @Override
    protected void onCloseClientChannel() {
        if (requestHost == null) {
            LogDog.d("--> browser heartbeat !!!" + " [ connect count = " + MultipleProxyServer.localConnectCount.decrementAndGet() + " ] ");
        } else {
            LogDog.d("--> close " + requestHost + " [ connect count = " + MultipleProxyServer.localConnectCount.decrementAndGet() + " ] ");
        }
        NioClientFactory.getFactory().removeTask(transmissionProxyClient);

        if (isDebug) {
            String report = SpiderEnvoy.getInstance().endWatchKey(ReceptionProxyClient.this.toString());
            LogDog.saveLog(report);
        }
    }

}
