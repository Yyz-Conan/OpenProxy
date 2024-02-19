package com.open.proxy.server.socks5.client;

import com.jav.common.log.LogDog;
import com.jav.common.util.ConfigFileEnvoy;
import com.jav.net.base.MultiBuffer;
import com.jav.net.nio.NioSender;
import com.open.proxy.IConfigKey;
import com.open.proxy.OpContext;
import com.open.proxy.intercept.ProxyFilterManager;
import com.open.proxy.protocol.DataPacketTag;
import com.open.proxy.protocol.Socks5Generator;
import com.open.proxy.server.BindClientTask;
import com.open.proxy.server.joggle.IBindClientListener;
import com.open.proxy.server.joggle.ISocks5ProcessListener;
import com.open.proxy.server.socks5.*;
import com.open.proxy.server.socks5.server.Socks5Server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.List;

/**
 * sock5响应客户（支持用户名和密码校验）
 */
public class Socks5InteractiveClient extends BindClientTask implements ISocks5ProcessListener, IBindClientListener {

    private Socks5TransmissionClient transmissionClient;
    private boolean isServerMode;
    private boolean allowProxy;

    public Socks5InteractiveClient(SocketChannel channel) {
        super(channel);
        ConfigFileEnvoy configFileEnvoy = OpContext.getInstance().getConfigFileEnvoy();
        isServerMode = configFileEnvoy.getBooleanValue(IConfigKey.CONFIG_IS_SERVER_MODE);
        allowProxy = configFileEnvoy.getBooleanValue(IConfigKey.CONFIG_ALLOW_PROXY);
    }

    @Override
    protected void onBeReadyChannel(SelectionKey selectionKey, SocketChannel channel) {
        if (isServerMode) {
            // 当前是服务模式，响应客户端代理转发请求,数据经过加密（非socks5格式数据）
            Socks5DecryptionReceiver receiver = new Socks5DecryptionReceiver(this);
            receiver.setServerMode();
            DecryptionReceiver decryptionReceiver = receiver.getReceiver();
            decryptionReceiver.setDecodeTag(DataPacketTag.PACK_SOCKS5_HELLO_TAG);
            setReceiver(decryptionReceiver);
            EncryptionSender sender = new EncryptionSender(true);
            sender.setEncodeTag(DataPacketTag.PACK_SOCKS5_DATA_TAG);
            sender.setChannel(selectionKey, channel);
            setSender(sender);
        } else {
            // 当前是客户端模式，响应本地client代理,数据没经过加密(socks5格式数据交互)
            try {
                InetSocketAddress address = (InetSocketAddress) channel.getLocalAddress();
                setAddress(address.getAddress().getHostAddress(), address.getPort());
            } catch (IOException e) {
                e.printStackTrace();
            }

            NioSender sender = new NioSender();
            sender.setChannel(selectionKey, channel);
            setSender(sender);

            Socks5Receiver receiver = new Socks5Receiver(this);
            setReceiver(receiver.getReceiver());
        }
        LogDog.d("<-x_proxy_socks5-> Socks5LocalClient has com.open.proxy.connect " + " [ com.open.proxy.connect count = " + Socks5Server.sLocalConnectCount.incrementAndGet() + " ] ");
    }

    @Override
    protected void onCloseChannel() {
        if (transmissionClient != null) {
            LogDog.d("<-x_proxy_socks5-> Socks5LocalClient close host =  " + transmissionClient.getRealHost() + ":" + transmissionClient.getRealPort());
        }
        LogDog.d("<-x_proxy_socks5-> Socks5LocalClient has close " + " [ com.open.proxy.connect count = " + Socks5Server.sLocalConnectCount.decrementAndGet() + " ] ");
        Socks5NetFactory.getFactory().getNetTaskComponent().addUnExecTask(transmissionClient);
    }

    @Override
    public Socks5Generator.Socks5Verification onClientSupportMethod(List<Socks5Generator.Socks5Verification> methods) {
        // 响应不需要用户和密码验证
        Socks5Generator.Socks5Verification choiceMethod = methods.get(0);
        getSender().sendData(new MultiBuffer(Socks5Generator.buildVerVerificationMethodResponse(choiceMethod)));
        return choiceMethod;
    }

    @Override
    public boolean onVerification(String userName, String password) {
        // 响应通过校验
        LogDog.d("<-x_proxy_socks5-> Socks5LocalClient verification client userName = " + userName + " password = " + password);
        getSender().sendData(new MultiBuffer(Socks5Generator.buildVerVerificationResponse()));
        return true;
    }

    @Override
    public void onReportCommandStatus(Socks5Generator.Socks5CommandStatus status) {
        try {
            byte[] response = Socks5Generator.buildCommandResponse(status.rangeStart,
                    Socks5Generator.Socks5AddressType.DOMAIN, getHost(), getPort());
            getSender().sendData(new MultiBuffer(response));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBeginProxy(String targetAddress, int targetPort) {
        LogDog.d("<-x_proxy_socks5-> Socks5LocalClient proxy client address = " + targetAddress + " port = " + targetPort);
        boolean isNeedProxy = allowProxy;
        targetPort = targetPort <= 0 ? 80 : targetPort;
        if (isServerMode && !allowProxy) {
            isNeedProxy = ProxyFilterManager.getInstance().isNeedProxy(targetAddress);
        }
        if (!isServerMode && isNeedProxy) {
            // 当前开启代理，而且是客户端模式,连接远程代理服务,数据经过加密（非socks5格式数据）
            transmissionClient = new Socks5TransmissionClient(this, targetAddress, targetPort);
            // 获取配置数据,连接代理服务端
            ConfigFileEnvoy configFileEnvoy = OpContext.getInstance().getConfigFileEnvoy();
            String remoteHost = configFileEnvoy.getValue(IConfigKey.CONFIG_REMOTE_SOCKS5_PROXY_HOST);
            int remotePort = configFileEnvoy.getIntValue(IConfigKey.CONFIG_REMOTE_SOCKS5_PROXY_PORT);
            transmissionClient.setAddress(remoteHost, remotePort);
        } else {
            // 不走代理（客户端模式或者服务端模式）,连接真实目标服务,数据不经过加密（非socks5格式数据）
            transmissionClient = new Socks5TransmissionClient(this);
            transmissionClient.setAddress(targetAddress, targetPort);
        }
        transmissionClient.setBindClientListener(this);
        Socks5NetFactory.getFactory().getNetTaskComponent().addExecTask(transmissionClient);
    }

    /**
     * 把代理客户端请求的数据中转发送给目标服务（远程代理服务或者真实目标服务）
     *
     * @param buffer
     */
    @Override
    public void onUpstreamData(MultiBuffer buffer) {
        // 如果是服务端模式,则把数据发给真实目录服务端
        transmissionClient.getSender().sendData(buffer);
    }

    /**
     * 把远程服务或者真实目标服务数据回传给代理客户端
     *
     * @param buffer
     */
    @Override
    public void onDownStreamData(MultiBuffer buffer) {
        getSender().sendData(buffer);
    }

    @Override
    public void onBindClientByReady(String requestId) {

    }

    @Override
    public void onBindClientByError(String requestId) {

    }

    @Override
    public void onBindClientData(String requestId, MultiBuffer buffer) {

    }

    @Override
    public void onBindClientClose(String requestId) {
        Socks5NetFactory.getFactory().getNetTaskComponent().addUnExecTask(this);
    }
}
