package com.open.proxy.connect.http;

import com.jav.net.xhttp.entity.XReceiverMode;
import com.open.proxy.connect.joggle.IDecryptionDataListener;
import com.open.proxy.connect.joggle.ISecurityReceiverProcessListener;
import com.open.proxy.cryption.AESDataEnvoy;
import com.open.proxy.cryption.AESEncrypt;
import com.open.proxy.cryption.DataSafeManager;
import com.open.proxy.cryption.joggle.EncryptionType;
import com.open.proxy.cryption.joggle.IDecryptComponent;
import com.open.proxy.cryption.joggle.IEncryptComponent;

import java.nio.ByteBuffer;

public class SecurityReceiverProcess implements ISecurityReceiverProcessListener {

    /**
     * cmd_type 0x1 初始化 0x2 传输数据 0x3 同步数据
     */
    private enum CmdType {

        INIT((byte) 1), TRANS((byte) 2), SYNC((byte) 3);

        private byte mCmd;

        CmdType(byte cmd) {
            mCmd = cmd;
        }

        public byte getCmd() {
            return mCmd;
        }
    }

    private enum InitStatus {

        NORMAL((byte) 1), SERVER_IP((byte) 2);

        private byte mCode;

        InitStatus(byte code) {
            mCode = code;
        }

        public byte getCode() {
            return mCode;
        }

        public static InitStatus getInstance(byte code) {
            if (NORMAL.getCode() == code) {
                return NORMAL;
            } else if (SERVER_IP.getCode() == code) {
                return SERVER_IP;
            }
            return null;
        }
    }

    private XReceiverMode mMode;

    private DataSafeManager mDataSafeManager;

    private IDecryptionDataListener mDecryptionDataListener;

    public SecurityReceiverProcess() {
        mDataSafeManager = new DataSafeManager();
        mDataSafeManager.init(EncryptionType.RSA);
    }

    public void setReceiverMode(XReceiverMode mode) {
        this.mMode = mode;
    }

    public void setDecryptionDataListener(IDecryptionDataListener listener) {
        this.mDecryptionDataListener = listener;
    }

    @Override
    public ByteBuffer decodeData(ByteBuffer encodeData) {
        //第一次用rsa解密
        byte[] data = encodeData.array();
        if (mDataSafeManager.isInit()) {
            byte[] decodeData = mDataSafeManager.decode(data);
            return ByteBuffer.wrap(decodeData);
        }
        return encodeData;
    }

    @Override
    public boolean onCheckTime(long time) {
        long nowTime = System.currentTimeMillis();
        if (nowTime > time) {
            long diff = nowTime - time;
            //172800000 48小时的毫秒值
            return diff > 172800000;
        }
        return true;
    }

    @Override
    public boolean onCheckMid(String mid) {
        return false;
    }

    @Override
    public void onExecCmd(byte cmd, ByteBuffer data) {
        if (cmd == CmdType.INIT.getCmd()) {
            if (mMode == XReceiverMode.REQUEST) {
                //当前是处理客户端请求数据
                byte enType = data.get();
                //根据客户端定义初始化加解密
                EncryptionType encryption = EncryptionType.getInstance(enType);
                mDataSafeManager.init(encryption);
                if (encryption == EncryptionType.AES) {
                    //获取AES对称密钥
                    byte[] aesKey = new byte[data.limit() - data.position()];
                    data.get(aesKey);

                    IDecryptComponent decryptComponent = mDataSafeManager.getDecrypt();
                    AESDataEnvoy decryptEnvoy = decryptComponent.getEncrypt();
                    decryptEnvoy.initKey(aesKey);

                    IEncryptComponent encryptComponent = mDataSafeManager.getEncrypt();
                    AESDataEnvoy encryptEnvoy = encryptComponent.getEncrypt();
                    encryptEnvoy.initKey(aesKey);
                }
            } else {
                //当前处理服务端响应数据
                byte resCode = data.get();
                InitStatus status = InitStatus.getInstance(resCode);
                if (InitStatus.SERVER_IP == status) {
                    //断开当前服务链接，连接返回的服务器ip

                }
            }
        } else if (cmd == CmdType.TRANS.getCmd()) {
            byte pctCount = data.get();
            //pctCount 作用于udp 传输数据
            byte[] context = new byte[data.limit() - data.position()];
            data.get(context);
            if (mDecryptionDataListener != null) {
                mDecryptionDataListener.onDecryption(context);
            }
        }
    }


    @Override
    public void onDeny(String msg) {

    }

    @Override
    public void onError() {

    }
}
