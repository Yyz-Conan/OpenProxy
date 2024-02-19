package com.open.proxy.server.http.client;


import com.jav.common.log.LogDog;
import com.jav.common.state.joggle.IStateMachine;
import com.jav.common.track.SpiderEnvoy;
import com.jav.common.util.ConfigFileEnvoy;
import com.jav.common.util.StringEnvoy;
import com.jav.net.base.MultiBuffer;
import com.jav.net.base.NetTaskStatus;
import com.jav.net.base.RequestMode;
import com.jav.net.base.SocketChannelCloseException;
import com.jav.net.base.joggle.*;
import com.jav.net.nio.NioClientTask;
import com.jav.net.nio.NioReceiver;
import com.jav.net.nio.NioSender;
import com.jav.net.security.channel.SecurityChannelBoot;
import com.jav.net.security.channel.SecurityClientChannelImage;
import com.jav.net.security.channel.base.ConstantCode;
import com.jav.net.security.channel.base.UnusualBehaviorType;
import com.jav.net.security.channel.joggle.IClientChannelStatusListener;
import com.jav.net.xhttp.entity.XHttpDecoderStatus;
import com.jav.net.xhttp.entity.XReceiverMode;
import com.jav.net.xhttp.entity.XResponse;
import com.jav.net.xhttp.utils.ByteCacheStream;
import com.jav.net.xhttp.utils.XHttpDecoderProcessor;
import com.jav.net.xhttp.utils.XResponseHelper;
import com.open.proxy.IConfigKey;
import com.open.proxy.OpContext;
import com.open.proxy.intercept.InterceptFilterManager;
import com.open.proxy.intercept.ProxyFilterManager;
import com.open.proxy.protocol.HtmlGenerator;
import com.open.proxy.server.http.server.MultipleProxyServer;
import com.open.proxy.server.joggle.IBindClientListener;
import com.open.proxy.utils.RequestHelper;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Map;

/**
 * 本地服务端接待需要代理的客户端
 *
 * @author yyz
 */
public class LocalReceptionProxyClient extends NioClientTask implements IClientChannelStatusListener,
        INetReceiver<MultiBuffer>, ISenderFeedback<MultiBuffer>, IBindClientListener {

    private SecurityClientChannelImage mClientChannelImage;
    private XHttpDecoderProcessor mProcessor;

    private TransProxyClient mLocalTransProxyClient;
    private boolean mIsNeedProxy = false;
    private boolean mIsRequestTag = true;
    private String mCurRequestHost = null;
    private String mRequestMethod = null;
    private boolean mIsBlacklist = false;
    private byte[] mConnectData;

    private String httpUrl;


    public LocalReceptionProxyClient(SocketChannel channel) {
        super(channel);
    }

    @Override
    protected void onBeReadyChannel(SelectionKey selectionKey, SocketChannel channel) {
        // 创建http协议解析器
        mProcessor = new XHttpDecoderProcessor();
        mProcessor.setMode(XReceiverMode.REQUEST);

        ConfigFileEnvoy cFileEnvoy = OpContext.getInstance().getConfigFileEnvoy();
        boolean isDebug = cFileEnvoy.getBooleanValue(IConfigKey.KEY_DEBUG_MODE);
        SpiderEnvoy.getInstance().setEnablePrint(isDebug);
        SpiderEnvoy.getInstance().startWatchKey(LocalReceptionProxyClient.this.toString());
        SpiderEnvoy.getInstance().pinKeyProbe(LocalReceptionProxyClient.this.toString(), "onBeReadyChannel");

        // 设置发送者和接收者
        NioSender sender = new NioSender();
        sender.setChannel(selectionKey, channel);
        sender.setSenderFeedback(this);
        setSender(sender);
        NioReceiver receiver = new NioReceiver();
        receiver.setDataReceiver(this);
        setReceiver(receiver);

        // 注册通道镜像
        if (SecurityChannelBoot.getInstance().isInit()) {
            SecurityChannelBoot.getInstance().registerClientChannel(this);
        }
    }

    @Override
    protected void onErrorChannel(NetErrorType errorType, Throwable throwable) {
        if (!(throwable instanceof SocketChannelCloseException)) {
            throwable.printStackTrace();
        }
    }

    @Override
    protected void onCloseChannel() {
        int count = MultipleProxyServer.sLocalConnectCount.decrementAndGet();
        if (mCurRequestHost == null) {
            LogDog.w("browser heartbeat !!!" + " [ connect count = " + count + " ]  object = " + this);
        } else {
            LogDog.w("close " + mCurRequestHost + " [ connect count = " + count + " ]  object = " + this);
        }

        if (mLocalTransProxyClient != null) {
            IStateMachine<Integer> stateMachine = mLocalTransProxyClient.getStatusMachine();
            if (!stateMachine.isAttachState(NetTaskStatus.FINISHING) && stateMachine.getState() != NetTaskStatus.INVALID) {
                INetFactory<NioClientTask> factory = OpContext.getInstance().getBClientFactory();
                INetTaskComponent<NioClientTask> component = factory.getNetTaskComponent();
                component.addUnExecTask(mLocalTransProxyClient);
            }
        }
        SecurityChannelBoot.getInstance().unRegisterClientChannel(mClientChannelImage);

        String report = SpiderEnvoy.getInstance().endWatchKey(LocalReceptionProxyClient.this.toString());
        LogDog.saveLog(report);
    }


    @Override
    public void onRemoteCreateConnect(byte status) {
        if (status == ConstantCode.REP_SUCCESS_CODE) {
            // https 请求才需要响应
            if (RequestMode.CONNECT.getMode().equals(mRequestMethod)) {
                responseTunnelEstablished();
                SpiderEnvoy.getInstance().pinKeyProbe(LocalReceptionProxyClient.this.toString(), "remote 返回CONNECT 响应");
            } else {
                mClientChannelImage.sendTransDataFromClient(mConnectData);
//                mConnectData = null;
            }
        } else {
            INetFactory<NioClientTask> factory = OpContext.getInstance().getBClientFactory();
            INetTaskComponent<NioClientTask> component = factory.getNetTaskComponent();
            component.addUnExecTask(this);
        }
    }

    @Override
    public void onChannelImageReady(SecurityClientChannelImage image) {
        mClientChannelImage = image;
        notifyChannelReady();
    }

    @Override
    public void onRemoteTransData(byte[] data) {
        // 接收代理服务端返回的数据
        getSender().sendData(new MultiBuffer(data));
    }

    @Override
    public void onChannelInvalid() {
        LogDog.w("--> LocalReceptionProxyClient onInvalid");
        INetFactory<NioClientTask> factory = OpContext.getInstance().getBClientFactory();
        INetTaskComponent<NioClientTask> component = factory.getNetTaskComponent();
        component.addUnExecTask(this);
    }

    @Override
    public void onChannelError(UnusualBehaviorType type, Map<String, String> map) {
        LogDog.e("## LocalReceptionProxyClient onChannelError " + type.getErrorMsg());
    }

    @Override
    public void onReceiveFullData(MultiBuffer buffer) {
        // 接收到需要往代理服务中转的客户端数据
        byte[] data = buffer.asByte();
        if (data != null) {
            if (mIsRequestTag || RequestHelper.isRequest(data)) {
                decoderHttpData(data);
            } else {
                // 当前状态非http的request请求体
                if (mIsNeedProxy) {
                    // 当前需要走代理
                    mClientChannelImage.sendTransDataFromClient(data);
                    SpiderEnvoy.getInstance().pinKeyProbe(LocalReceptionProxyClient.this.toString(), "<remote> 一般是https 正式请求数据");
                } else {
                    boolean isFinish = false;
                    if (mLocalTransProxyClient != null) {
                        IStateMachine<Integer> stateMachine = mLocalTransProxyClient.getStatusMachine();
                        isFinish = stateMachine.isAttachState(NetTaskStatus.FINISHING);
                    }
                    if (isFinish || mLocalTransProxyClient == null) {
                        INetFactory<NioClientTask> factory = OpContext.getInstance().getBClientFactory();
                        INetTaskComponent<NioClientTask> component = factory.getNetTaskComponent();
                        component.addUnExecTask(this);
                    } else {
                        SpiderEnvoy.getInstance().pinKeyProbe(LocalReceptionProxyClient.this.toString(), "<local> 一般是https 正式请求数据");
                        // LogDog.d("==> <local> 一般是https 正式请求数据 host = " + mCurRequestHost);
                        mLocalTransProxyClient.getSender().sendData(new MultiBuffer(data));
                    }
                }
            }
        }
    }

    @Override
    public void onReceiveError(Throwable throwable) {

    }

    private void decoderHttpData(byte[] data) {
        mProcessor.decoderData(data, data.length);
        XHttpDecoderStatus status = mProcessor.getStatus();
        mIsRequestTag = true;
        if (status == XHttpDecoderStatus.OVER) {
            XResponse response = mProcessor.getResponse();
            handleRequest(response);
            mProcessor.reset();
            mIsRequestTag = false;
        }
    }

    private void handleRequest(XResponse response) {
        String newRequestHost = XResponseHelper.getHost(response);
        httpUrl = XResponseHelper.getHttpUrl(response);
        // 过滤非法数据
        if (StringEnvoy.isEmpty(newRequestHost) && StringEnvoy.isEmpty(mCurRequestHost)) {
            // 异常数据,暂时认为是客户端的心跳包
            byte[] data = response.getRawData().toByteArray();
            String dataStr = null;
            if (data.length > 0) {
                dataStr = new String(data);
            }
            String msg = "newRequestHost and requestHost is null ,data = " + dataStr;
            SpiderEnvoy.getInstance().pinKeyProbe(LocalReceptionProxyClient.this.toString(), msg);
            INetFactory<NioClientTask> factory = OpContext.getInstance().getBClientFactory();
            INetTaskComponent<NioClientTask> component = factory.getNetTaskComponent();
            component.addUnExecTask(LocalReceptionProxyClient.this);
            LogDog.e("--> [ browser heartbeat ] = " + dataStr);
            return;
        }

        // 黑名单过滤
        if (InterceptFilterManager.getInstance().isIntercept(newRequestHost)) {
            mIsBlacklist = true;
            LogDog.e("++> intercept blacklist host = " + newRequestHost);
            mCurRequestHost = newRequestHost;

            getSender().sendData(new MultiBuffer(HtmlGenerator.headDenyService(newRequestHost)));

            String msg = "newRequestHost in black menu ,host = " + newRequestHost;
            SpiderEnvoy.getInstance().pinKeyProbe(LocalReceptionProxyClient.this.toString(), msg);
            return;
        }

        // 向远程服务请求创建目标链接
        int port = XResponseHelper.getPort(response);
        createTargetConnect(response, newRequestHost, port);
        String msg = "new request host = " + httpUrl + " is inconsistent with the current request host = " + mCurRequestHost;
        SpiderEnvoy.getInstance().pinKeyProbe(LocalReceptionProxyClient.this.toString(), msg);
    }

    private void createTargetConnect(XResponse response, String host, int port) {

        INetFactory<NioClientTask> factory = OpContext.getInstance().getBClientFactory();
        if (mLocalTransProxyClient != null) {
            // 结束上一个链接
            IStateMachine<Integer> stateMachine = mLocalTransProxyClient.getStatusMachine();
            if (!stateMachine.isAttachState(NetTaskStatus.FINISHING) && stateMachine.getState() != NetTaskStatus.INVALID) {
                factory.getNetTaskComponent().addUnExecTask(mLocalTransProxyClient);
            }
        }
        mRequestMethod = XResponseHelper.getRequestMethod(response);
        if (!RequestMode.CONNECT.getMode().equals(mRequestMethod)) {
            ByteCacheStream raw = response.getRawData();
            mConnectData = raw.toByteArray();
        }
        ConfigFileEnvoy cFileEnvoy = OpContext.getInstance().getConfigFileEnvoy();
        boolean isEnableProxy = cFileEnvoy.getBooleanValue(IConfigKey.CONFIG_ENABLE_PROXY);
        if (isEnableProxy) {
            mIsNeedProxy = cFileEnvoy.getBooleanValue(IConfigKey.CONFIG_ALLOW_PROXY);
            if (!mIsNeedProxy) {
                mIsNeedProxy = ProxyFilterManager.getInstance().isNeedProxy(host);
            }
            if (mIsNeedProxy) {
                // 当前需要走代理
                if (mLocalTransProxyClient != null) {
                    factory.getNetTaskComponent().addUnExecTask(mLocalTransProxyClient);
                    mCurRequestHost = null;
                }
                if (mClientChannelImage == null) {
                    waitChannelReady();
                    if (mClientChannelImage == null) {
                        factory.getNetTaskComponent().addUnExecTask(this);
                        return;
                    }
                }
                LogDog.d("--> send proxy target host  = " + host + ":" + port);
                mClientChannelImage.sendRequestDataFromClient(host, port);
                SpiderEnvoy.getInstance().pinKeyProbe(LocalReceptionProxyClient.this.toString(), "<remote> 请求创建目标链接");
                return;
            }
        }


        // 不需要代理
        mIsNeedProxy = false;

        mLocalTransProxyClient = new TransProxyClient(host, port);
        mLocalTransProxyClient.setBindClientListener(this);
        INetTaskComponent<NioClientTask> component = factory.getNetTaskComponent();
        component.addExecTask(mLocalTransProxyClient);
        SpiderEnvoy.getInstance().pinKeyProbe(LocalReceptionProxyClient.this.toString(), "<local> 请求创建目标链接");

        mCurRequestHost = host;
        LogDog.d("++--> new request host = " + host + ":" + port);
    }


    private void waitChannelReady() {
        synchronized (mProcessor) {
            try {
                mProcessor.wait(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void notifyChannelReady() {
        synchronized (mProcessor) {
            mProcessor.notify();
        }
    }

    private void responseTunnelEstablished() {
        // 远程通道创建成功，响应客户端成功
        getSender().sendData(new MultiBuffer(HtmlGenerator.httpsTunnelEstablished()));
    }

    @Override
    public void onSenderFeedBack(INetSender<MultiBuffer> sender, MultiBuffer buffer, Throwable throwable) {
        if (throwable != null || mIsBlacklist) {
            if (throwable != null) {
                throwable.printStackTrace();
            }
            INetFactory<NioClientTask> factory = OpContext.getInstance().getBClientFactory();
            factory.getNetTaskComponent().addUnExecTask(this);
        }
    }

    @Override
    public void onBindClientByReady(String requestId) {
        if (RequestMode.CONNECT.getMode().equals(mRequestMethod)) {
            responseTunnelEstablished();
        } else {
            mLocalTransProxyClient.getSender().sendData(new MultiBuffer(mConnectData));
//            mConnectData = null;
        }
        SpiderEnvoy.getInstance().pinKeyProbe(LocalReceptionProxyClient.this.toString(), "<local> CONNECT 响应");
        //        LogDog.i("--> <local> CONNECT 响应 = " + mCurRequestHost);
    }


    @Override
    public void onBindClientData(String requestId, MultiBuffer buffer) {
//        byte[] byteData = buffer.asByte();
//        StringBuilder dataStr = new StringBuilder();
//        int dataLength = byteData.length;
//        if (byteData.length > 80) {
//            dataLength = 80;
//        }
//        for (int index = 0; index < dataLength; index++) {
//            String hex = Integer.toHexString(byteData[index] & 0xff);
//            if (hex.length() == 1) {
//                hex = "0" + hex;
//            }
//            dataStr.append(hex);
//        }
//        LogDog.d("==> httpUrl = " + httpUrl + " 服务器返回数据 = " + dataStr);
        getSender().sendData(buffer);
    }

    @Override
    public void onBindClientByError(String requestId) {

    }

    @Override
    public void onBindClientClose(String requestId) {
//        INetFactory<NioClientTask> factory = OpContext.getInstance().getBClientFactory();
//        factory.getNetTaskComponent().addUnExecTask(this);
    }
}
