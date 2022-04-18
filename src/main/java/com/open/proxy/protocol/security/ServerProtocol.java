package com.open.proxy.protocol.security;

import java.nio.ByteBuffer;
import java.util.Base64;

/**
 * 获取服务列表，可以选择最优的服务器
 *
 * @author yyz
 */
public class ServerProtocol extends ProxyProtocol {

    public ServerProtocol(String machine, byte[] data, boolean isRequest) {
        super(machine, data, isRequest);
        setEnType(EnType.BASE64.getType());
    }

    @Override
    byte cmdType() {
        return CmdType.SERVER.getType();
    }

    @Override
    public ByteBuffer toData() {
        int length = sendData().length + 41;
        ByteBuffer buffer = ByteBuffer.allocate(length);
        buffer.putLong(time());
        buffer.put(cmdType());
        buffer.put(randomCode());
        if (!isRequest()) {
            buffer.put(sendData());
        }

        byte[] data = buffer.array();
        byte[] enData = Base64.getEncoder().encode(data);
        ByteBuffer finalData = ByteBuffer.allocate(enData.length + 4);
        finalData.putShort((short) enData.length);
        finalData.put(enData);
        return finalData;
    }
}
