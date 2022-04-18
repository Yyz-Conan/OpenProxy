package com.open.proxy.connect.http.client;


import com.currency.net.base.SendPacket;
import com.currency.net.base.joggle.INetSender;
import com.currency.net.base.joggle.ISenderFeedback;
import com.currency.net.nio.NioClientFactory;
import com.currency.net.nio.NioClientTask;
import com.currency.net.nio.NioSender;
import com.open.proxy.IConfigKey;
import com.open.proxy.OPContext;
import com.open.proxy.connect.UpdateDecoderReceiver;
import com.open.proxy.connect.joggle.IUpdateAffairsCallBack;
import com.open.proxy.connect.joggle.IUpdateConfirmCallBack;
import com.open.proxy.protocol.DataPacketTag;
import log.LogDog;
import util.ConfigFileEnvoy;
import util.StringEnvoy;
import util.TypeConversion;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.SocketChannel;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class UpdateHandleClient extends NioClientTask implements IUpdateAffairsCallBack, IUpdateConfirmCallBack, ISenderFeedback {

    private int newVersion;
    private String updateFilePath;
    private int currentVersion = 0;
    private String saveFile = "update.zip";

    public UpdateHandleClient(String host, int port) {
        setAddress(host, port);
        init(false);
    }

    public UpdateHandleClient(SocketChannel channel) {
        super(channel, null);
        init(true);
    }

    private void init(boolean isServerMode) {
        UpdateDecoderReceiver receiver = new UpdateDecoderReceiver(this, this, isServerMode);
        setReceiver(receiver);
        NioSender sender = new NioSender();
        if (isServerMode) {
            ConfigFileEnvoy cFileEnvoy = OPContext.getInstance().getConfigFileEnvoy();
            newVersion = cFileEnvoy.getIntValue(IConfigKey.CONFIG_NEW_VERSION);
            updateFilePath = cFileEnvoy.getValue(IConfigKey.CONFIG_UPDATE_FILE_PATH);
            sender.setSenderFeedback(this);
        }
        setSender(sender);
    }


    @Override
    protected void onBeReadyChannel(SocketChannel channel) {
        LogDog.d("==================== com.open.proxy.connect success ============================");
        getSender().setChannel(getSelectionKey(), channel);
        if (currentVersion > 0) {
            //send update com.open.proxy.protocol head
            getSender().sendData(SendPacket.getInstance(DataPacketTag.PACK_UPDATE_TAG));
            //send data
            byte[] versionByte = TypeConversion.intToByte(currentVersion);
            getSender().sendData(SendPacket.getInstance(versionByte));
        }
    }


    @Override
    public void onSenderFeedBack(INetSender iNetSender, Object o, Throwable throwable) {
        if (getSender().isCacheEmpty()) {
            //数据发送完毕断开连接
            NioClientFactory.getFactory().getNetTaskContainer().addUnExecTask(this);
        }
    }

    public void checkUpdate() {
        this.currentVersion = IConfigKey.currentVersion;
    }

    @Override
    public void onServerCheckVersion(int version) {
        boolean isHasNewVersion = version < newVersion;
        //响应是否有新版本
        //send update com.open.proxy.protocol head
        getSender().sendData(SendPacket.getInstance(DataPacketTag.PACK_UPDATE_TAG));
        //send data
        byte[] countFileByte = TypeConversion.intToByte(isHasNewVersion ? 1 : 0);
        getSender().sendData(SendPacket.getInstance(countFileByte));

        if (isHasNewVersion && StringEnvoy.isNotEmpty(updateFilePath)) {
            File updateFile = new File(updateFilePath);
            if (updateFile.exists()) {
                long fileSize = updateFile.length();
                if (fileSize > 0) {
                    byte[] fileSizeByte = TypeConversion.long2Bytes(fileSize);
                    //响应更新文件大小
                    LogDog.d("==> send update file size to client !");
                    getSender().sendData(SendPacket.getInstance(fileSizeByte));
                    //响应更新文件数据
                    LogDog.d("==> send update file to client !");
                    sendFileData(updateFile);
                }
            } else {
                LogDog.e("==> update file no exists !!! ");
            }
        } else {
            LogDog.e("==> not config update file or no new version !!! ");
        }
    }

    @Override
    public void onClientCheckVersion(boolean isHasNewVersion, IUpdateConfirmCallBack callBack) {
    }

    private void sendFileData(File updateFile) {
        RandomAccessFile accessFile = null;
        FileLock fileLock = null;
        try {
            accessFile = new RandomAccessFile(updateFile, "r");
            FileChannel fileChannel = accessFile.getChannel();
            fileLock = fileChannel.tryLock(0, accessFile.length(), true);
            if (fileLock != null) {
                long position = 0;
                long size = 4096;
                long fileSize = fileChannel.size();
                do {
                    MappedByteBuffer byteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, position, size);
                    getSender().sendData(SendPacket.getInstance(byteBuffer));
                    position += size;
                    if (fileSize - position < size) {
                        size = fileSize - position;
                    }
                } while (position < fileSize);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            if (fileLock != null) {
                try {
                    fileLock.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (accessFile != null) {
                try {
                    accessFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public String getSaveFile() {
        return OPContext.getInstance().getCurrentWorkDir() + saveFile;
    }


    @Override
    public void onUpdateComplete() {
        //解压文件
        File file = new File(getSaveFile());
        byte[] tmpBuf = new byte[1024];
        ZipFile zipFile = null;
        int len;
        try {
            zipFile = new ZipFile(file);
            for (Enumeration entries = zipFile.entries(); entries.hasMoreElements(); ) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                String zipEntryName = entry.getName();
                InputStream in = null;
                FileOutputStream out = null;
                try {
                    in = zipFile.getInputStream(entry);
                    out = new FileOutputStream(OPContext.getInstance().getCurrentWorkDir() + zipEntryName);
                    do {
                        len = in.read(tmpBuf);
                        if (len > 0) {
                            out.write(tmpBuf, 0, len);
                        }
                    } while (len > 0);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //数据发送完毕断开连接
            NioClientFactory.getFactory().getNetTaskContainer().addUnExecTask(this);
        }
    }

    @Override
    protected void onRecovery() {
        super.onRecovery();
        LogDog.d("======================  disconnect ==============================");
    }

    @Override
    public void onConfirm() {
        UpdateDecoderReceiver receiver = getReceiver();
        receiver.startReceiveUpdate();
    }

    @Override
    public void onCancel() {
        NioClientFactory.getFactory().getNetTaskContainer().addUnExecTask(this);
    }
}
