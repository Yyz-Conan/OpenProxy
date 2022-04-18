package com.open.proxy.protocol.security;

import com.open.proxy.cryption.RSADataEnvoy;

import javax.crypto.NoSuchPaddingException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;

/**
 * 初始化接口，服务端校验客户端，客户端获得随机数
 *
 * @author yyz
 */
public class InitProtocol extends ProxyProtocol {

    private RSADataEnvoy mRSADataEnvoy;

    public InitProtocol(String machine, byte[] initData, boolean isRequest) {
        super(machine, initData, isRequest);
        setEnType(EnType.BASE64.getType());
        mRSADataEnvoy = new RSADataEnvoy();
    }

    public void initRsa(String pubicKeyPath, String privateKeyPath) throws NoSuchPaddingException, NoSuchAlgorithmException {
        mRSADataEnvoy.init(pubicKeyPath, privateKeyPath);
    }

    @Override
    byte cmdType() {
        return CmdType.INIT.getType();
    }

    @Override
    public ByteBuffer toData() {
        int length = isRequest() ? 42 : 41;
        if (sendData() == null) {
            length += sendData().length;
        }
        ByteBuffer buffer = ByteBuffer.allocate(length);
        buffer.putLong(time());
        buffer.put(cmdType());
        buffer.put(machine());
        if (isRequest()) {
            buffer.put(enType());
        }
        if (sendData() != null) {
            buffer.put(sendData());
        }
        byte[] data = buffer.array();
        byte[] rsaData = mRSADataEnvoy.superCipher(data, true, true);
        ByteBuffer finalData = ByteBuffer.allocate(rsaData.length + 4);
        finalData.putInt(rsaData.length);
        finalData.put(rsaData);
        return finalData;
    }

}
