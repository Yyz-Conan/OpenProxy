package com.open.proxy.protocol.security;

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
        setEnType(EnType.BASE64.getType());
    }

    @Override
    byte cmdType() {
        return CmdType.DATA.getType();
    }

    @Override
    public ByteBuffer toData() {
        if (sendData() == null) {
            return null;
        }
        int length = sendData().length + 58;
        ByteBuffer buffer = ByteBuffer.allocate(length);
        buffer.putLong(time());
        buffer.put(cmdType());
        buffer.putShort(requestId());
        buffer.put(randomCode());
        buffer.put(packetOrder());
        buffer.put(sendData());

        byte[] data = buffer.array();
        byte[] enData = Base64.getEncoder().encode(data);
        ByteBuffer finalData = ByteBuffer.allocate(enData.length + 4);
        finalData.putInt(enData.length);
        finalData.put(enData);
        return finalData;
    }
}
