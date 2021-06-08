package connect;


import connect.network.nio.NioSender;
import connect.network.xhttp.XMultiplexCacheManger;
import connect.network.xhttp.utils.MultiLevelBuf;
import cryption.joggle.IEncryptTransform;
import util.TypeConversion;
import utils.DataPacketManger;

import java.nio.ByteBuffer;

/**
 * 加密发送者
 */
public class EncryptionSender extends NioSender {

    private IEncryptTransform transform;

    public EncryptionSender(IEncryptTransform transform) {
        this.transform = transform;
    }

    @Override
    public void sendData(Object objData) {
        if (transform != null) {
            if (objData instanceof MultiLevelBuf) {
                MultiLevelBuf buf = (MultiLevelBuf) objData;
                buf.flip();
                byte[] byteData = buf.array();
                XMultiplexCacheManger.getInstance().lose(buf);
                sendEncryptData(byteData);
            } else if (objData instanceof byte[]) {
                sendEncryptData((byte[]) objData);
            }
        } else {
            super.sendData(objData);
        }
    }

    private void sendEncryptData(byte[] byteData) {
        byte[] encrypt = transform.onEncrypt(byteData);
        //send protocol head
        ByteBuffer tagData = ByteBuffer.wrap(DataPacketManger.PACK_PROXY_TAG);
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
