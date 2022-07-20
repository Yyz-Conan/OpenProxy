package com.open.proxy.connect.joggle;

import java.nio.ByteBuffer;

public interface ISecurityReceiverProcessListener {

    ByteBuffer decodeData(ByteBuffer encodeData);

    boolean onCheckTime(long time);

    boolean onCheckMid(String mid);

    void onExecCmd(byte cmd, ByteBuffer data);

    void onDeny(String msg);

    void onError();
}
