package com.open.proxy.connect.socks5;


import com.jav.common.cryption.DataSafeManager;
import com.jav.common.cryption.joggle.EncryptionType;
import com.jav.common.util.TypeConversion;
import com.jav.net.entity.MultiByteBuffer;
import com.jav.net.nio.NioSender;

import java.nio.ByteBuffer;

/**
 * 加密发送者
 */
public class EncryptionSender extends NioSender {

    private boolean mIsNeedEncryption;

    private DataSafeManager mDataSafeManager;
    private byte[] mTag;

    public EncryptionSender(boolean isNeedDecryption) {
        mIsNeedEncryption = isNeedDecryption;
        mDataSafeManager =  new DataSafeManager();
        mDataSafeManager.init(EncryptionType.RSA);
    }

    public void setEncodeTag(byte[] tag) {
        mTag = tag;
    }

    @Override
    public void sendData(MultiByteBuffer buffer) {
        if (mIsNeedEncryption) {
//            buffer.flip();
            byte[] byteData = buffer.array();
            if (byteData != null) {
                sendEncryptData(byteData);
            }
        } else {
            super.sendData(buffer);
        }
    }

    private void sendEncryptData(byte[] byteData) {
        byte[] encrypt = mDataSafeManager.encode(byteData);
        //send com.open.proxy.protocol head
        ByteBuffer tagData = ByteBuffer.wrap(mTag);
        super.sendData(new MultiByteBuffer(tagData));
        //send data length
        byte[] length = TypeConversion.intToByte(encrypt.length);
        ByteBuffer lengthData = ByteBuffer.wrap(length);
        super.sendData(new MultiByteBuffer(lengthData));
        //send data
        ByteBuffer encryptData = ByteBuffer.wrap(encrypt);
        super.sendData(new MultiByteBuffer(encryptData));
    }

}
