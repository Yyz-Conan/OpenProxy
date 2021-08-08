package connect;


import connect.network.nio.NioSender;
import connect.network.xhttp.XMultiplexCacheManger;
import connect.network.xhttp.utils.MultiLevelBuf;
import cryption.DataSafeManager;
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
    public void sendData(Object objData) {
        if (mIsNeedEncryption) {
            if (objData instanceof MultiLevelBuf) {
                MultiLevelBuf buf = (MultiLevelBuf) objData;
                buf.flip();
                byte[] byteData = buf.array();
                XMultiplexCacheManger.getInstance().lose(buf);
                if (byteData != null) {
                    sendEncryptData(byteData);
                }
            } else if (objData instanceof byte[]) {
                sendEncryptData((byte[]) objData);
            }
        } else {
            super.sendData(objData);
        }
    }

    private void sendEncryptData(byte[] byteData) {
        byte[] encrypt = DataSafeManager.getInstance().encode(byteData);
        //send protocol head
        ByteBuffer tagData = ByteBuffer.wrap(mTag);
        super.sendData(tagData);
        //send data length
        byte[] length = TypeConversion.intToByte(encrypt.length);
        ByteBuffer lengthData = ByteBuffer.wrap(length);
        super.sendData(lengthData);
        //send data
        ByteBuffer encryptData = ByteBuffer.wrap(encrypt);
        super.sendData(encryptData);
    }

}
