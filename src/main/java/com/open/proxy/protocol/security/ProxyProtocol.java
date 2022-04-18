package com.open.proxy.protocol.security;

import java.nio.ByteBuffer;
import java.security.MessageDigest;

public abstract class ProxyProtocol {

    /**
     * 作用类型
     */
    protected enum CmdType {

        /**
         * 定义加密
         */
        INIT((byte) 1),
        /**
         * 传输数据
         */
        DATA((byte) 2),
        /**
         * 获取服务
         */
        SERVER((byte) 3),
        /**
         * 同步服务
         */
        SYNC((byte) 4);

        private byte mType;

        CmdType(byte type) {
            mType = type;
        }

        public byte getType() {
            return mType;
        }
    }

    /**
     * 编码类型
     */
    protected enum EnType {

        NO_ENCODE((byte) 0), BASE64((byte) 1), DES((byte) 2), OTHER((byte) 3);

        private byte mType;

        EnType(byte type) {
            mType = type;
        }

        public byte getType() {
            return mType;
        }
    }


    /**
     * 机器码（32Byte）
     */
    private byte[] mByteMachine;

    /**
     * 发送的数据
     */
    private byte[] mSendData;

    /**
     * 配置数据用途的类型（1Byte）
     */
    private byte mCmdType;

    /**
     * 数据编码类型，配置数据加密类型（1Byte）
     */
    private byte mEnType;

    /**
     * 随机code，服务端返回(32Byte)
     */
    private byte[] randomCode;

    /**
     * 请求id，区分http请求的链路
     */
    private short requestId;

    /**
     * 用于udp记录包的顺序,最大255个包，超出服务器会重置
     */
    private byte packetOrder;

    /**
     * 当前的数据是否是客户端发起
     */
    private boolean mIsRequest;

    public ProxyProtocol(String machine, byte[] data, boolean isRequest) {
        this.mByteMachine = md5_32(machine);
        this.mIsRequest = isRequest;
        this.mCmdType = cmdType();
        updateSendData(data);
    }

    protected long time() {
        return System.currentTimeMillis();
    }

    abstract byte cmdType();

    protected byte enType() {
        return mEnType;
    }

    protected byte[] machine() {
        return mByteMachine;
    }

    protected byte[] randomCode() {
        return randomCode;
    }

    protected short requestId() {
        return requestId;
    }

    protected byte packetOrder() {
        return packetOrder;
    }

    protected byte[] sendData() {
        return mSendData;
    }

    protected boolean isRequest() {
        return mIsRequest;
    }

    public void setCmdType(byte mCmdType) {
        this.mCmdType = mCmdType;
    }

    public void setEnType(byte mEnType) {
        this.mEnType = mEnType;
    }

    public void setRequestId(short requestId) {
        this.requestId = requestId;
    }

    public void setRandomCode(byte[] randomCode) {
        this.randomCode = randomCode;
    }

    public void setPacketOrder(byte packetOrder) {
        this.packetOrder = packetOrder;
    }

    public void updateSendData(byte[] sendData) {
        this.mSendData = sendData;
    }

    public ByteBuffer toData() {
        return null;
    }

    private byte[] md5_32(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.reset();
            digest.update(text.getBytes());
            return digest.digest();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
