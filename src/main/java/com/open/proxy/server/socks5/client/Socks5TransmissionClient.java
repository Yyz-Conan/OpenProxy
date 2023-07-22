package com.open.proxy.server.socks5.client;


import com.jav.net.base.joggle.INetReceiver;
import com.jav.net.component.joggle.ICacheComponent;
import com.jav.net.entity.MultiByteBuffer;
import com.jav.net.nio.NioReceiver;
import com.jav.net.nio.NioSender;
import com.open.proxy.server.BindClientTask;
import com.open.proxy.server.joggle.ISocks5ProcessListener;
import com.open.proxy.server.socks5.DecryptionReceiver;
import com.open.proxy.server.socks5.EncryptionSender;
import com.open.proxy.server.socks5.Socks5DecryptionReceiver;
import com.open.proxy.protocol.DataPacketTag;
import com.open.proxy.protocol.HtmlGenerator;

import java.nio.channels.SocketChannel;

/**
 * 连接代理服务端,作用于中转数据
 */
public class Socks5TransmissionClient extends BindClientTask implements INetReceiver<MultiByteBuffer> {

    private ISocks5ProcessListener mListener;
    private String mRealHost;
    private int mRealPort;

    /**
     * 不走代理（客户端模式或者服务端模式）,连接真实目标服务,数据不经过加密（非socks5格式数据）
     *
     * @param listener
     */
    public Socks5TransmissionClient(ISocks5ProcessListener listener) {
        this.mListener = listener;
        init();
    }

    /**
     * 当前开启代理，而且是客户端模式,连接远程代理服务,数据经过加密（非socks5格式数据）
     *
     * @param listener
     * @param realHost
     * @param realPort
     */
    public Socks5TransmissionClient(ISocks5ProcessListener listener, String realHost, int realPort) {
        this.mListener = listener;
        this.mRealHost = realHost;
        this.mRealPort = realPort;
        initCryption();
        sendRealTargetInfo(realHost, realPort);
        //        enableProxy();
    }

    private void enableProxy() {
        byte[] data = HtmlGenerator.httpsTunnelEstablished();
        mListener.onDownStreamData(new MultiByteBuffer(data));
    }

    public String getRealHost() {
        return mRealHost == null ? getHost() : mRealHost;
    }

    public int getRealPort() {
        return mRealPort <= 0 ? getPort() : mRealPort;
    }

    private void init() {
        NioReceiver receiver = new NioReceiver();
        receiver.setDataReceiver(this);
        setReceiver(receiver);
        NioSender sender = new NioSender();
        setSender(sender);
    }

    private void sendRealTargetInfo(String realHost, int realPort) {
        byte[] targetInfo = createTargetInfoProtocol(realHost, realPort);
        EncryptionSender sender = getSender();
        ICacheComponent component = sender.getCacheComponent();
        component.addLastData(new MultiByteBuffer(targetInfo));
        //        sender.sendData(new MultiByteBuffer(targetInfo));
        // 发送完hello数据,切换tag(PACK_SOCKS5_DATA_TAG)用于中转数据
        sender.setEncodeTag(DataPacketTag.PACK_SOCKS5_DATA_TAG);
    }

    private void initCryption() {
        Socks5DecryptionReceiver receiver = new Socks5DecryptionReceiver(mListener);
        receiver.setForwardModel();
        DecryptionReceiver decryptionReceiver = receiver.getReceiver();
        decryptionReceiver.setDecodeTag(DataPacketTag.PACK_SOCKS5_DATA_TAG);
        setReceiver(decryptionReceiver);
        EncryptionSender sender = new EncryptionSender(true);
        // 当前是远程服务端使用,第一个hello数据是 PACK_SOCKS5_HELLO_TAG 类型
        sender.setEncodeTag(DataPacketTag.PACK_SOCKS5_HELLO_TAG);
        setSender(sender);
    }

    private byte[] createTargetInfoProtocol(String host, int port) {
        int length = host.length();
        byte[] head = new byte[3 + length];
        int index = 0;
        head[index] = (byte) length;
        index++;
        byte[] hostByte = host.getBytes();
        System.arraycopy(hostByte, 0, head, 1, length);
        index += length;

        head[index] = (byte) (((port & 0XFF00) >> 8));
        head[index + 1] = (byte) (port & 0XFF);
        return head;
    }


    @Override
    protected void onBeReadyChannel(SocketChannel channel) {
        getSender().setChannel(getSelectionKey(), channel);
    }


    @Override
    public void onReceiveFullData(MultiByteBuffer buffer) {
        mListener.onDownStreamData(buffer);
    }

    @Override
    public void onReceiveError(Throwable throwable) {

    }
}
