package connect.socks5;

import connect.DecryptionReceiver;
import connect.joggle.IDecryptionDataListener;
import connect.joggle.ISocks5ProcessListener;
import connect.joggle.Socks5ProcessStatus;
import protocol.DataPacketTag;

import java.security.InvalidParameterException;

/**
 * socks5加密接收者
 */
public class Socks5DecryptionReceiver implements IDecryptionDataListener {

    private ISocks5ProcessListener mListener;
    private DecryptionReceiver mDecryptionReceiver;

    private Socks5ProcessStatus mStatus = Socks5ProcessStatus.COMMAND;
    private boolean mServerMode = false;

    public Socks5DecryptionReceiver(ISocks5ProcessListener listener) {
        if (listener == null) {
            throw new InvalidParameterException("listener is null !!!");
        }
        this.mListener = listener;
        mDecryptionReceiver = new DecryptionReceiver(true);
        mDecryptionReceiver.setOnDecryptionDataListener(this);
    }

    public DecryptionReceiver getReceiver() {
        return mDecryptionReceiver;
    }

    public void setForwardModel() {
        mStatus = Socks5ProcessStatus.FORWARD;
    }

    public void setServerMode() {
        this.mServerMode = true;
    }

    @Override
    public void onDecryption(byte[] decrypt) {
        if (mStatus == Socks5ProcessStatus.COMMAND) {
            int index = 0;
            int hostLength = decrypt[index];
            index++;
            byte[] hostByte = new byte[hostLength];
            System.arraycopy(decrypt, index, hostByte, 0, hostLength);
            index = hostLength + 1;
            int targetPort = ((decrypt[index] & 0XFF) << 8) | (decrypt[index + 1] & 0XFF);
//            int targetPort = ((decrypt[index + 1] & 0XFF) << 8) | (decrypt[index] & 0XFF);
            //回调开启连接真实目标服务
            mListener.onBeginProxy(new String(hostByte), targetPort);
            //切换数据tag
            mDecryptionReceiver.setDecodeTag(DataPacketTag.PACK_SOCKS5_DATA_TAG);
            //改变状态为中转状态
            mStatus = Socks5ProcessStatus.FORWARD;
        } else if (mStatus == Socks5ProcessStatus.FORWARD) {
            //中转状态直接回调数据
            if (mServerMode) {
                mListener.onUpstreamData(decrypt);
            } else {
                mListener.onDownStreamData(decrypt);
            }
        }
    }
}
