package com.open.proxy.connect.http.client;

import com.currency.net.base.joggle.INetReceiver;
import com.currency.net.entity.MultiByteBuffer;
import com.currency.net.nio.NioReceiver;
import com.currency.net.nio.NioSender;
import com.currency.net.xhttp.entity.XResponse;
import com.currency.net.xhttp.utils.ByteCacheStream;
import com.open.proxy.IConfigKey;
import com.open.proxy.OPContext;
import com.open.proxy.connect.AbsClient;
import com.open.proxy.connect.EncryptionSender;
import com.open.proxy.connect.HttpDecryptionReceiver;
import com.open.proxy.protocol.DataPacketTag;
import com.open.proxy.protocol.HtmlGenerator;
import log.LogDog;
import track.SpiderEnvoy;
import util.ConfigFileEnvoy;
import util.StringEnvoy;

import java.nio.channels.SocketChannel;

/**
 * 代理转发客户http请求
 */
public class TransmissionProxyClient extends AbsClient implements INetReceiver<MultiByteBuffer> {

    private byte[] mConnectData;
    private String mRealHost = null;
    private NioSender mReceptionSender;
    private HttpDecryptionReceiver mReceptionReceiver;


    /**
     * 走代理服务
     *
     * @param sender
     */
    public TransmissionProxyClient(NioSender sender, HttpDecryptionReceiver receiver, XResponse response) {
        if (sender == null || receiver == null) {
            throw new NullPointerException("sender or receiver is null !!!");
        }
        this.mReceptionSender = sender;
        this.mReceptionReceiver = receiver;
        ByteCacheStream raw = response.getRawData();
        mConnectData = raw.toByteArray();
        ConfigFileEnvoy cFileEnvoy = OPContext.getInstance().getConfigFileEnvoy();
        boolean isDebug = cFileEnvoy.getBooleanValue(IConfigKey.KEY_DEBUG_MODE);
        SpiderEnvoy.getInstance().setEnablePrint(isDebug);
        SpiderEnvoy.getInstance().startWatchKey(TransmissionProxyClient.this.toString());
    }

    public void enableLocalConnect(String host, int port) {
        setAddress(host, port);
    }

    public void enableProxyConnect(String realHost) {
        this.mRealHost = realHost;
        ConfigFileEnvoy cFileEnvoy = OPContext.getInstance().getConfigFileEnvoy();
        String remoteHost = cFileEnvoy.getValue(IConfigKey.CONFIG_REMOTE_PROXY_HOST);
        int remotePort = cFileEnvoy.getIntValue(IConfigKey.CONFIG_REMOTE_PROXY_PORT);
        setAddress(remoteHost, remotePort);
    }

    @Override
    protected void onBeReadyChannel(SocketChannel channel) {
        if (StringEnvoy.isEmpty(mRealHost)) {
            //面向真实服务端
            NioReceiver receiver = new NioReceiver();
            receiver.setDataReceiver(this);
            setReceiver(receiver);
            setSender(new NioSender());
            SpiderEnvoy.getInstance().pinKeyProbe(TransmissionProxyClient.this.toString(),
                    "onBeReadyChannel :当前是走本地代理模式");
        } else {
            //面向代理客户端
            HttpDecryptionReceiver receiver = new HttpDecryptionReceiver(true);
            //配置工作模式为客户端模式
            receiver.setResponseSender(mReceptionSender);
            setReceiver(receiver.getReceiver());
            EncryptionSender sender = new EncryptionSender(true);
            sender.setEncodeTag(DataPacketTag.PACK_PROXY_TAG);
            setSender(sender);
            SpiderEnvoy.getInstance().pinKeyProbe(TransmissionProxyClient.this.toString(),
                    "onBeReadyChannel :当前是走远程代理模式");
        }

        getSender().setChannel(getSelectionKey(), channel);
        getSender().setSenderFeedback(this);
        //绑定 ReceptionProxyClient 链接的接收者
        mReceptionReceiver.setRequestSender(getSender());

        if (StringEnvoy.isEmpty(mRealHost)) {
            //当前是https请求而且不走代理请求，则响应代理请求
            mReceptionSender.sendData(HtmlGenerator.httpsTunnelEstablished());
            SpiderEnvoy.getInstance().pinKeyProbe(TransmissionProxyClient.this.toString(), "返回CONNECT 响应");
        } else {
            getSender().sendData(mConnectData);
            mConnectData = null;
        }
    }

    @Override
    public void onReceiveFullData(MultiByteBuffer buf, Throwable e) {
        if (e != null) {
            LogDog.e("++> onReceiveException host = " + getHost() + ":" + getPort());
        }
        //对应NioReceiver
        mReceptionSender.sendData(buf);
    }


    @Override
    protected void onErrorChannel(Throwable throwable) {
        //链接失败，如果不是配置强制不走代理则尝试代理链接
        LogDog.e("==> TransmissionProxyClient connection failed,  host = " + getHost() + ":" + getPort());
        SpiderEnvoy.getInstance().pinKeyProbe(TransmissionProxyClient.this.toString(),
                "onConnectError host = " + getHost() + ":" + getPort());
    }


    @Override
    protected void onCloseChannel() {
        if (mCloseListener != null) {
            mCloseListener.onClientClose(mRealHost);
        }
        LogDog.e("==> TransmissionProxyClient connection close,  host = " + getHost() + ":" + getPort());
        String report = SpiderEnvoy.getInstance().endWatchKey(TransmissionProxyClient.this.toString());
        LogDog.saveLog(report);
    }

}
