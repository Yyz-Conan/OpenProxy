package com.open.proxy.server.sync;

import com.jav.common.log.LogDog;
import com.jav.net.security.cache.CacheExtMachineIdMater;
import com.jav.net.security.channel.base.AbsSecurityProtocolParser;
import com.jav.net.security.channel.base.ConstantCode;
import com.jav.net.security.channel.base.UnusualBehaviorType;
import com.open.proxy.server.sync.bean.SyncActivityCode;
import com.open.proxy.server.sync.bean.SyncErrorType;
import com.open.proxy.server.sync.joggle.ISyncServerEventCallBack;
import com.open.proxy.server.sync.protocol.base.SyncOperateCode;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * 同步协议解析器
 */
public class SecuritySyncProtocolParser extends AbsSecurityProtocolParser {

    private final SecuritySyncContext mContext;

    private ISyncServerEventCallBack mSyncEventCallBack;


    public SecuritySyncProtocolParser(SecuritySyncContext context) {
        mContext = context;
    }

    public void setSyncEventCallBack(ISyncServerEventCallBack callBack) {
        this.mSyncEventCallBack = callBack;
    }

    @Override
    public void parserReceiverData(InetSocketAddress remoteAddress, ByteBuffer decodeData) {
        // 解析校验时间字段
        parseCheckTime(remoteAddress, decodeData);
        // 解析cmd字段
        byte cmd = decodeData.get();
        // 解析校验machine id字段
        String machineId = parseCheckMachineId(remoteAddress, decodeData);

        if (cmd == SyncActivityCode.SYNC.getCode()) {
            if (!machineId.startsWith("S")) {
                throw new IllegalStateException(SyncErrorType.EXP_SYNC_MACHINE_ID.getErrorMsg() + " machineId : " + machineId);
            }
            byte oCode = decodeData.get();
            parserSync(remoteAddress, oCode, machineId, decodeData);
        } else {
            reportPolicyProcessor(remoteAddress, UnusualBehaviorType.EXP_ACTIVITY);
        }
    }


    /**
     * 解析sync 行为
     *
     * @param oCode
     * @param machineId
     * @param data
     */
    private void parserSync(InetSocketAddress remoteAddress, byte oCode, String machineId, ByteBuffer data) {
        byte realOperateCode = (byte) (oCode & (~ConstantCode.REP_EXCEPTION_CODE));
        if (realOperateCode == SyncOperateCode.SYNC_AVG.getCode()
                || realOperateCode == SyncOperateCode.RESPOND_SYNC_AVG.getCode()) {
            if (mSyncEventCallBack != null) {
                int proxyPort = data.getInt();
                byte loadData = data.get();
                byte status = (byte) (oCode & ConstantCode.REP_EXCEPTION_CODE);
                if (status != ConstantCode.REP_SUCCESS_CODE) {
                    return;
                }
                mSyncEventCallBack.onRespondSyncCallBack(remoteAddress, realOperateCode, proxyPort, machineId, loadData);
                LogDog.w("## receive sync avg data , server machine id : " + machineId
                        + " proxyPort : " + proxyPort + " loadData : " + loadData);
            }
        } else if (realOperateCode == SyncOperateCode.SYNC_MID.getCode()) {
            byte[] context = getContextData(data);
            if (context == null) {
                throw new RuntimeException(SyncErrorType.EXP_SYNC_DATA.getErrorMsg());
            }
            String clientMachineId = new String(context);
            //添加到 mid 列表里
            boolean ret = CacheExtMachineIdMater.getInstance().cacheMid(clientMachineId);
            LogDog.w("## cache client machine id : " + clientMachineId);
            byte status = ret ? ConstantCode.REP_SUCCESS_CODE : ConstantCode.REP_EXCEPTION_CODE;
            //响应同步mid结果
            SecurityServerSyncImage.getInstance().respondSyncMid(mContext.getMachineId(), clientMachineId, status);
            LogDog.w("## start respond sync data , server machine id : " + machineId + " status : " + ret);
        } else if (realOperateCode == SyncOperateCode.RESPOND_SYNC_MID.getCode()) {
            byte[] context = getContextData(data);
            if (context == null) {
                LogDog.e("## channel state : " + SyncErrorType.EXP_SYNC_DATA.getErrorMsg());
                throw new RuntimeException(SyncErrorType.EXP_SYNC_DATA.getErrorMsg());
            }
            String clientMachineId = new String(context);
            LogDog.w("## respond sync client machine id : " + clientMachineId);
            //通知 init 接口添加mid成功，可以切换中转服务
            if (mSyncEventCallBack != null) {
                byte status = (byte) (oCode & ConstantCode.REP_EXCEPTION_CODE);
                mSyncEventCallBack.onRespondSyncMidCallBack(status, clientMachineId);
            }
        } else {
            LogDog.e("## channel state : " + UnusualBehaviorType.EXP_OPERATE_CODE.getErrorMsg());
            throw new RuntimeException(UnusualBehaviorType.EXP_OPERATE_CODE.getErrorMsg());
        }
    }
}
