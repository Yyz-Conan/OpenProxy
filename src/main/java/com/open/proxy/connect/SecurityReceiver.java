package com.open.proxy.connect;

import com.jav.common.util.IoEnvoy;
import com.jav.net.nio.NioReceiver;
import com.open.proxy.connect.joggle.ISecurityReceiverProcessListener;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * 协议格式
 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－|
 * ｜  length（4Byte） ｜    time（8Byte）    ｜    cmd_type（1Byte）  ｜   [m_id or req_id]（32Byte）  ｜
 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
 * ｜                                       data                                                     |
 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
 * SecurityReceiver 安全传输协议
 *
 * @author yyz
 */
public class SecurityReceiver extends NioReceiver {


    private enum SRState {
        INIT, DATA, NONE
    }

    private ByteBuffer mLength = ByteBuffer.allocate(4);

    private ByteBuffer mFullData = null;


    private SRState mState = SRState.INIT;

    private ISecurityReceiverProcessListener mProcessListener;

    public void setProcessListener(ISecurityReceiverProcessListener listener) {
        this.mProcessListener = listener;
    }

    @Override
    protected void onReadNetData(SocketChannel channel) throws Throwable {
        processInit(channel);
        processData(channel);
    }

    private void processInit(SocketChannel channel) throws IOException {
        if (mState == SRState.INIT) {
            int ret = IoEnvoy.readToFull(channel, mLength);
            if (ret == IoEnvoy.SUCCESS) {
                mLength.flip();
                int length = mLength.getInt();
                if (length <= 0) {
                    if (mProcessListener != null) {
                        mProcessListener.onDeny("非法数据长度!!!");
                    }
                    mState = SRState.NONE;
                    return;
                }
                mFullData = ByteBuffer.allocate(length);
                mState = SRState.DATA;
            } else if (ret == IoEnvoy.FAIL) {
                if (mProcessListener != null) {
                    mProcessListener.onError();
                }
                mState = SRState.NONE;
            }
        }
    }

    private void processData(SocketChannel channel) throws IOException {
        if (mState == SRState.DATA) {
            int ret = IoEnvoy.readToFull(channel, mFullData);
            if (ret == IoEnvoy.SUCCESS) {
                if (mProcessListener != null) {
                    mFullData.flip();
                    ByteBuffer decodeData = mProcessListener.decodeData(mFullData);
                    long time = decodeData.getLong();
                    boolean isDeny = mProcessListener.onCheckTime(time);
                    if (isDeny) {
                        mProcessListener.onDeny("时间过期或者非法时间!!!");
                        return;
                    }
                    byte[] mid = new byte[32];
                    decodeData.get(mid);
                    isDeny = mProcessListener.onCheckMid(new String(mid));
                    if (isDeny) {
                        mProcessListener.onDeny("非法机器号!!!");
                        return;
                    }
                    byte cmd = decodeData.get();
                    mProcessListener.onExecCmd(cmd, decodeData);
                }
                mFullData = null;
                mState = SRState.INIT;
            } else if (ret == IoEnvoy.FAIL) {
                if (mProcessListener != null) {
                    mProcessListener.onError();
                }
                mState = SRState.NONE;
            }
        }
    }

}
