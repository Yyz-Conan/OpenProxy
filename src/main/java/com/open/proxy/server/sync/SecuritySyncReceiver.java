package com.open.proxy.server.sync;

import com.jav.common.cryption.joggle.ICipherComponent;
import com.jav.net.base.MultiBuffer;
import com.jav.net.base.UdpPacket;
import com.jav.net.base.joggle.INetReceiver;
import com.jav.net.nio.NioUdpReceiver;
import com.jav.net.security.channel.joggle.ISecurityProtocolParser;
import com.jav.net.security.channel.joggle.ISecurityReceiver;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class SecuritySyncReceiver implements ISecurityReceiver {

    /**
     * 真正的数据接收者
     */
    private final NioUdpReceiver mCoreReceiver;
    /**
     * 安全协议的解析器
     */
    private ISecurityProtocolParser mProtocolParser;

    /**
     * 解密组件
     */
    private ICipherComponent mDecryptComponent;

    public SecuritySyncReceiver() {
        mCoreReceiver = new NioUdpReceiver();
        ReceiverListener listener = new ReceiverListener();
        mCoreReceiver.setDataReceiver(listener);
    }

    public NioUdpReceiver getCoreReceiver() {
        return mCoreReceiver;
    }

    private class ReceiverListener implements INetReceiver<UdpPacket> {

        @Override
        public void onReceiveFullData(UdpPacket udpPacket) {
            MultiBuffer udpData = udpPacket.getUdpData();
            byte[] encodeData = udpData.asByte();
            byte[] decodeData = mDecryptComponent.onDecrypt(encodeData);
            ByteBuffer rawData = ByteBuffer.wrap(decodeData);
            InetSocketAddress remoteAddress = (InetSocketAddress) udpPacket.getAddress();
            mProtocolParser.parserReceiverData(remoteAddress, rawData);
        }

        @Override
        public void onReceiveError(Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    @Override
    public void setProtocolParser(ISecurityProtocolParser parser) {
        mProtocolParser = parser;
    }

    @Override
    public void setDecryptComponent(ICipherComponent decryptComponent) {
        mDecryptComponent = decryptComponent;
    }

}
