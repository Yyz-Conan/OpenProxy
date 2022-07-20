package com.open.proxy.protocol.security;

import com.open.proxy.cryption.joggle.IEncryptComponent;

import java.nio.ByteBuffer;
import java.util.Base64;

/**
 * 传输数据
 *
 * @author yyz
 */
public class DataProtocol extends ProxyProtocol {

    public DataProtocol(String machine, byte[] data, boolean isRequest) {
        super(machine, data, isRequest);
    }

    @Override
    byte cmdType() {
        return CmdType.DATA.getType();
    }

    @Override
    public ByteBuffer toData(IEncryptComponent encryptComponent) {
        if (sendData() == null) {
            return null;
        }
        int length = sendData().length + 42;
        ByteBuffer buffer = ByteBuffer.allocate(length);
        buffer.putLong(time());
        buffer.put(requestId());
        buffer.put(cmdType());
        buffer.put(packetOrder());
        buffer.put(sendData());

        byte[] data = buffer.array();
        byte[] enData = data;
        if (encryptComponent != null) {
            enData = encryptComponent.onEncrypt(data);
        }
        ByteBuffer finalData = ByteBuffer.allocate(enData.length + 4);
        finalData.putInt(enData.length);
        finalData.put(enData);
        return finalData;
    }
}
