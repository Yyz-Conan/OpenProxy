package com.open.proxy.connect.socks5;


import com.jav.common.util.IoEnvoy;
import com.jav.common.util.TypeConversion;
import com.jav.net.base.SocketChannelCloseException;
import com.jav.net.nio.NioReceiver;
import com.open.proxy.connect.joggle.DecryptionStatus;
import com.open.proxy.connect.joggle.IDecryptionDataListener;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

public class DecryptionReceiver extends NioReceiver {

    private boolean mIsNeedDecryption;
    private ByteBuffer packetHead;
    private ByteBuffer packetData;
    private DecryptionStatus decryptionStatus;
    private IDecryptionDataListener mListener;
    private byte[] mTag;


    public DecryptionReceiver(boolean isNeedDecryption) {
        mIsNeedDecryption = isNeedDecryption;
        if (isNeedDecryption) {
            packetHead = ByteBuffer.allocate(4);
            decryptionStatus = DecryptionStatus.TAG;
        }
    }

    public void setDecodeTag(byte[] tag) {
        mTag = tag;
    }

    public void setOnDecryptionDataListener(IDecryptionDataListener listener) {
        this.mListener = listener;
    }

    @Override
    protected void onReadNetData(SocketChannel channel) throws Throwable {
        if (mIsNeedDecryption) {
            if (decryptionStatus == DecryptionStatus.TAG) {
                packetHead.clear();
                int ret = IoEnvoy.readToFull(channel, packetHead);
                if (ret == IoEnvoy.SUCCESS) {
                    byte[] tag = packetHead.array();
                    boolean isSame = Arrays.equals(tag, mTag);
                    if (isSame) {
                        decryptionStatus = DecryptionStatus.SIZE;
                    } else {
                        throw new SocketChannelCloseException();
                    }
                } else if (ret == IoEnvoy.FAIL) {
                    throw new SocketChannelCloseException();
                }
            }
            if (decryptionStatus == DecryptionStatus.SIZE) {
                packetHead.clear();
                int ret = IoEnvoy.readToFull(channel, packetHead);
                if (ret == IoEnvoy.SUCCESS) {
                    int packetSize = TypeConversion.byteToInt(packetHead.array(), 0);
                    if (packetSize > 0) {
                        packetData = ByteBuffer.allocate(packetSize);
                        decryptionStatus = DecryptionStatus.DATA;
                    } else {
                        throw new SocketChannelCloseException();
                    }
                } else if (ret == IoEnvoy.FAIL) {
                    throw new SocketChannelCloseException();
                }
            }
            if (decryptionStatus == DecryptionStatus.DATA) {
                int ret = IoEnvoy.readToFull(channel, packetData);
                if (ret == IoEnvoy.SUCCESS) {
//                    byte[] decrypt = DataSafeManager.getInstance().decode(packetData.array());
//                    if (mListener != null) {
//                        mListener.onDecryption(decrypt);
//                    }
                    packetData = null;
                    decryptionStatus = DecryptionStatus.TAG;
                } else if (ret == IoEnvoy.FAIL) {
                    throw new SocketChannelCloseException();
                }
            }
        } else {
            super.onReadNetData(channel);
        }
    }
}
