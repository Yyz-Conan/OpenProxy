package com.open.proxy.protocol.security;

import com.open.proxy.cryption.joggle.IEncryptComponent;

import java.nio.ByteBuffer;

/**
 * 初始化接口，服务端校验客户端，客户端获得随机数
 *
 * @author yyz
 */
public class InitProtocol extends ProxyProtocol {

    private byte mCode;

    public InitProtocol(String machine, byte[] initData, boolean isRequest) {
        super(machine, initData, isRequest);
        setEnType(EnType.BASE64.getType());
    }

    public void setCode(byte code) {
        this.mCode = code;
    }

    @Override
    byte cmdType() {
        return CmdType.INIT.getType();
    }

    @Override
    public ByteBuffer toData(IEncryptComponent encryptComponent) {
        int length = 42;
        if (sendData() == null) {
            length += sendData().length;
        }
        ByteBuffer buffer = ByteBuffer.allocate(length);
        buffer.putLong(time());
        buffer.put(machine());
        buffer.put(cmdType());
        if (isRequest()) {
            buffer.put(enType());
        } else {
            buffer.put(mCode);
        }
        if (sendData() != null) {
            buffer.put(sendData());
        }
        byte[] data = buffer.array();
        byte[] rsaData = data;
        if (encryptComponent != null) {
            rsaData = encryptComponent.onEncrypt(data);
        }
        ByteBuffer finalData = ByteBuffer.allocate(rsaData.length + 4);
        finalData.putInt(rsaData.length);
        finalData.put(rsaData);
        return finalData;
    }

}
