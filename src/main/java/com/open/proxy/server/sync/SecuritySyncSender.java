package com.open.proxy.server.sync;

import com.jav.net.base.AbsNetSender;
import com.jav.net.base.MultiBuffer;
import com.jav.net.base.UdpPacket;
import com.jav.net.security.channel.SecuritySender;
import com.open.proxy.server.sync.protocol.SyncProtocol;
import com.open.proxy.server.sync.protocol.base.SyncOperateCode;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * sync服务数据发送者
 *
 * @author yyz
 */
public class SecuritySyncSender extends SecuritySender {


    public SecuritySyncSender(AbsNetSender sender) {
        super(sender);
    }

    /**
     * 响应sync avg 请求，
     *
     * @param machineId 本服务的machine id （注意不是请求端的machine id）
     * @param proxyPort 当前中转服务的端口号
     * @param loadAvg   当前服务的负载值
     */
    protected void respondSyncAvg(SocketAddress target, String machineId, int proxyPort, byte loadAvg) {
        SyncProtocol syncProtocol = new SyncProtocol(machineId, proxyPort, loadAvg);
        syncProtocol.setOperateCode(SyncOperateCode.RESPOND_SYNC_AVG.getCode());
        ByteBuffer encodeData = syncProtocol.toData(mEncryptComponent);
        MultiBuffer udpData = new MultiBuffer(encodeData);
        UdpPacket udpPacket = new UdpPacket(target, udpData);
        mCoreSender.sendData(udpPacket);
    }

    /**
     * 请求 sync avg
     *
     * @param target    目标地址
     * @param machineId 本服务的machine id
     * @param proxyPort 当前中转服务的端口号
     * @param loadAvg   当前服务的负载值
     */
    protected void requestSyncAvg(SocketAddress target, String machineId, int proxyPort, byte loadAvg) {
        SyncProtocol syncProtocol = new SyncProtocol(machineId, proxyPort, loadAvg);
        syncProtocol.setOperateCode(SyncOperateCode.SYNC_AVG.getCode());
        ByteBuffer encodeData = syncProtocol.toData(mEncryptComponent);
        MultiBuffer udpData = new MultiBuffer(encodeData);
        UdpPacket udpPacket = new UdpPacket(target, udpData);
        mCoreSender.sendData(udpPacket);
    }


    /**
     * 响应 sync data 请求
     *
     * @param serverMachineId 本服务的machine id （注意不是请求端的machine id）
     * @param clientMachineId 客户端的machine id
     * @param status
     */
    protected void respondSyncMid(String serverMachineId, String clientMachineId, byte status) {
        SyncProtocol syncProtocol = new SyncProtocol(serverMachineId);
        byte operateCode = (byte) (status | SyncOperateCode.RESPOND_SYNC_MID.getCode());
        syncProtocol.setOperateCode(operateCode);
        syncProtocol.setSendData(clientMachineId.getBytes());
        ByteBuffer encodeData = syncProtocol.toData(mEncryptComponent);
        mCoreSender.sendData(new MultiBuffer(encodeData));
    }

    /**
     * 请求 sync data
     *
     * @param serverMachineId 本服务的machine id
     * @param clientMachineId 客户端的machine id
     */
    protected void requestSyncMid(String serverMachineId, String clientMachineId) {
        SyncProtocol syncProtocol = new SyncProtocol(serverMachineId);
        syncProtocol.setOperateCode(SyncOperateCode.SYNC_MID.getCode());
        syncProtocol.setSendData(clientMachineId.getBytes());
        ByteBuffer encodeData = syncProtocol.toData(mEncryptComponent);
        mCoreSender.sendData(new MultiBuffer(encodeData));
    }

}
