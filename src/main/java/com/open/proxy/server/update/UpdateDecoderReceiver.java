package com.open.proxy.server.update;


import com.jav.common.log.LogDog;
import com.jav.common.util.IoEnvoy;
import com.jav.common.util.TypeConversion;
import com.jav.net.base.SocketChannelCloseException;
import com.jav.net.nio.NioReceiver;
import com.open.proxy.server.joggle.IUpdateAffairsCallBack;
import com.open.proxy.server.joggle.IUpdateConfirmCallBack;
import com.open.proxy.protocol.DataPacketTag;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;

public class UpdateDecoderReceiver extends NioReceiver {

    private ReceiverStatus receiverStatus = ReceiverStatus.TAG;
    private UpdateStatus updateStatus = UpdateStatus.VERSION;

    private ByteBuffer fourBuf;
    private ByteBuffer eightBuf;
    private ByteBuffer fileBuffer;
    private long fileSize;
    private boolean isServerMode;

    private RandomAccessFile updateAccessFile;
    private IUpdateAffairsCallBack affairsCallBack;
    private IUpdateConfirmCallBack confirmCallBack;

    private boolean isCall = false;

    public UpdateDecoderReceiver(IUpdateAffairsCallBack affairsCallBack, IUpdateConfirmCallBack confirmCallBack, boolean isServerMode) {
        fourBuf = ByteBuffer.allocate(4);
        eightBuf = ByteBuffer.allocate(8);
        fileBuffer = ByteBuffer.allocateDirect(4096);
        this.affairsCallBack = affairsCallBack;
        this.confirmCallBack = confirmCallBack;
        this.isServerMode = isServerMode;
    }

    public enum ReceiverStatus {
        TAG, DATA
    }

    public enum UpdateStatus {
        VERSION, FILE_DATA_SIZE, FILE_DATA
    }

    @Override
    public void onReadNetData(SocketChannel channel) throws Throwable {
        if (receiverStatus == ReceiverStatus.TAG) {
            fourBuf.clear();
            int ret = IoEnvoy.readToFull(channel, fourBuf);
            if (ret == IoEnvoy.SUCCESS) {
                byte[] tag = fourBuf.array();
                //判断tag是否一致
                boolean isSame = byteEquals(tag, DataPacketTag.PACK_UPDATE_TAG);
                if (isSame) {
                    receiverStatus = ReceiverStatus.DATA;
                } else {
                    //不一致断开链接
                    throw new SocketChannelCloseException();
                }
            } else if (ret == IoEnvoy.FAIL) {
                throw new SocketChannelCloseException();
            }
        }
        if (receiverStatus == ReceiverStatus.DATA) {
            analysis(channel);
        }
    }

    private boolean byteEquals(byte[] a, byte[] b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.length != b.length) {
            return false;
        }
        for (int index = a.length - 1; index <= 0; index--) {
            if (a[index] != b[index]) {
                return false;
            }
        }
        return true;
    }


    private void analysis(SocketChannel channel) throws Throwable {
        if (updateStatus == UpdateStatus.VERSION && isServerMode) {
            //当前服务端模式，解析版本号
            fourBuf.clear();
            int ret = IoEnvoy.readToFull(channel, fourBuf);
            if (ret == IoEnvoy.SUCCESS) {
                LogDog.d("==> analysis client version !");
                byte[] versionByte = fourBuf.array();
                int version = TypeConversion.byteToInt(versionByte, 0);
                LogDog.d("==> client version = " + version);
                if (affairsCallBack != null) {
                    //回调版本号
                    affairsCallBack.onServerCheckVersion(version);
                }
                receiverStatus = ReceiverStatus.TAG;
            } else if (ret == IoEnvoy.FAIL) {
                throw new SocketChannelCloseException();
            }
        }
        if (updateStatus == UpdateStatus.VERSION && !isServerMode) {
            if (isCall) {
                return;
            }
            isCall = true;
            //当前是客户端模式
            fourBuf.clear();
            int ret = IoEnvoy.readToFull(channel, fourBuf);
            if (ret == IoEnvoy.SUCCESS) {
                LogDog.d("==> analysis server version !");
                byte[] isHasNewVersionByte = fourBuf.array();
                boolean isHasNewVersion = TypeConversion.byteToInt(isHasNewVersionByte, 0) == 1;
                if (affairsCallBack != null) {
                    affairsCallBack.onClientCheckVersion(isHasNewVersion, confirmCallBack);
                }
            } else if (ret == IoEnvoy.FAIL) {
                throw new SocketChannelCloseException();
            }
        }
        if (updateStatus == UpdateStatus.FILE_DATA_SIZE) {
            fourBuf.clear();
            int ret = IoEnvoy.readToFull(channel, eightBuf);
            if (ret == IoEnvoy.SUCCESS) {
                LogDog.d("==> analysis update file size !");
                //解析文件的大小
                byte[] data = eightBuf.array();
                fileSize = TypeConversion.bytes2Long(data);
                updateStatus = UpdateStatus.FILE_DATA;
                LogDog.d("==> update file size = " + fileSize);
            } else if (ret == IoEnvoy.FAIL) {
                throw new SocketChannelCloseException();
            }
        }
        if (updateStatus == UpdateStatus.FILE_DATA) {
            //保存文件
            if (updateAccessFile == null) {
                if (affairsCallBack != null) {
                    String saveFile = affairsCallBack.getSaveFile();
                    updateAccessFile = new RandomAccessFile(saveFile, "rw");
                }
            }
            if (updateAccessFile != null) {
                LogDog.d("==> save update file !");
                do {
                    fileBuffer.clear();
                    int ret = channel.read(fileBuffer);
                    if (ret > 0) {
                        fileSize -= ret;
                        fileBuffer.flip();
                        FileChannel fileChannel = updateAccessFile.getChannel();
                        //追写
                        fileChannel.position(fileChannel.size());
                        fileChannel.write(fileBuffer);
                    }
                } while (fileSize > 0 || fileBuffer.position() == fileBuffer.capacity());
                if (fileSize == 0) {
                    //接收数据完成
                    updateAccessFile.close();
                    if (affairsCallBack != null) {
                        affairsCallBack.onUpdateComplete();
                    }
                }
            }
        }
    }

    public void startReceiveUpdate() {
        isCall = false;
        updateStatus = UpdateStatus.FILE_DATA_SIZE;
    }

}
