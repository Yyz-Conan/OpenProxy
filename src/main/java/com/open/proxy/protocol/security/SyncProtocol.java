package com.open.proxy.protocol.security;

import java.nio.ByteBuffer;

/**
 * 服务器之间同步数据，10分钟请求一次，实现分布式
 *
 * @author yyz
 */
public class SyncProtocol extends ProxyProtocol {

    public SyncProtocol(String machine, byte[] data, boolean isRequest) {
        super(machine, data, isRequest);
        setEnType(EnType.NO_ENCODE.getType());
    }

    @Override
    byte cmdType() {
        return CmdType.SYNC.getType();
    }

    @Override
    public ByteBuffer toData() {
        if (sendData() == null) {
            return null;
        }
        int length = sendData().length + 45;
        ByteBuffer buffer = ByteBuffer.allocate(length);
        buffer.putInt(length);
        buffer.putLong(time());
        buffer.put(cmdType());
        buffer.put(machine());
        buffer.put(sendData());
        return buffer;
    }
}
