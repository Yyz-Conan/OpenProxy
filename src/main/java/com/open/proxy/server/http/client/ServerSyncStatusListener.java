package com.open.proxy.server.http.client;

import com.jav.common.log.LogDog;
import com.jav.net.security.channel.SecurityServerChannelImage;
import com.jav.net.security.channel.base.ConstantCode;
import com.jav.net.security.protocol.base.InitResult;
import com.open.proxy.server.sync.SecurityServerSyncImage;
import com.open.proxy.server.sync.bean.SecuritySyncPayloadData;
import com.open.proxy.server.sync.joggle.IServerSyncStatusListener;

public class ServerSyncStatusListener implements IServerSyncStatusListener {

    private String mMachineId;
    private SecuritySyncPayloadData mLowLoadServer;
    private SecurityServerChannelImage mServerChannelImage;

    public ServerSyncStatusListener(String machineId, SecuritySyncPayloadData lowLoadServer, SecurityServerChannelImage image) {
        mMachineId = machineId;
        mLowLoadServer = lowLoadServer;
        mServerChannelImage = image;
    }

    @Override
    public String getMachineId() {
        return mMachineId;
    }

    @Override
    public void onSyncMinState(byte status) {
        if (status == ConstantCode.REP_SUCCESS_CODE) {
            String data = mLowLoadServer.getHost() + ":" + mLowLoadServer.getPort();
            mServerChannelImage.respondInitData(InitResult.SERVER_IP.getCode(), data.getBytes());
            LogDog.i("respond client low service address = " + data);
        } else {
            mServerChannelImage.respondInitData(InitResult.ERROR.getCode(), null);
            LogDog.w("sync server has error, respond sync error result!");
        }
        SecurityServerSyncImage.getInstance().removerListener(this);
    }

}
