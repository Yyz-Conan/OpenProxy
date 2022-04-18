package com.open.proxy.connect;


import com.currency.net.base.SendPacket;
import com.currency.net.entity.MultiByteBuffer;
import com.currency.net.nio.NioSender;
import com.open.proxy.cryption.DataSafeManager;
import util.TypeConversion;

import java.nio.ByteBuffer;

/**
 * 加密发送者
 */
public class EncryptionSender extends NioSender {

    private boolean mIsNeedEncryption;
    private byte[] mTag;

    public EncryptionSender(boolean isNeedDecryption) {
        mIsNeedEncryption = isNeedDecryption && DataSafeManager.getInstance().isEnable();
    }

    public void setEncodeTag(byte[] tag) {
        mTag = tag;
    }

    @Override
    public void sendData(SendPacket sendPacket) {
        Object objData = sendPacket.getSendData();
        if (mIsNeedEncryption) {
            if (objData instanceof MultiByteBuffer) {
                MultiByteBuffer buf = (MultiByteBuffer) objData;
                buf.flip();
                byte[] byteData = buf.array();
                if (byteData != null) {
                    sendEncryptData(byteData);
                }
            } else if (objData instanceof byte[]) {
                sendEncryptData((byte[]) objData);
            }
        } else {
            super.sendData(sendPacket);
        }
    }

    private void sendEncryptData(byte[] byteData) {
        byte[] encrypt = DataSafeManager.getInstance().encode(byteData);
        //send com.open.proxy.protocol head
        ByteBuffer tagData = ByteBuffer.wrap(mTag);
        super.sendData(SendPacket.getInstance(tagData));
        //send data length
        byte[] length = TypeConversion.intToByte(encrypt.length);
        ByteBuffer lengthData = ByteBuffer.wrap(length);
        super.sendData(SendPacket.getInstance(lengthData));
        //send data
        ByteBuffer encryptData = ByteBuffer.wrap(encrypt);
        super.sendData(SendPacket.getInstance(encryptData));
    }

}
