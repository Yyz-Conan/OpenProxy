package connect.client;

import config.AnalysisConfig;
import config.ConfigKey;
import connect.UpdateDecoderProcessor;
import connect.joggle.IUpdateAffairsCallBack;
import connect.network.base.joggle.INetSender;
import connect.network.base.joggle.ISenderFeedback;
import connect.network.nio.NioClientFactory;
import connect.network.nio.NioClientTask;
import connect.network.nio.NioReceiver;
import connect.network.nio.NioSender;
import log.LogDog;
import util.StringEnvoy;
import util.TypeConversion;
import utils.DataPacketManger;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.SocketChannel;
import java.util.Enumeration;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class UpdateHandleClient extends NioClientTask implements IUpdateAffairsCallBack, ISenderFeedback {

    private int newVersion;
    private String updateFilePath;
    private int currentVersion = 0;
    private String saveFile = "update.zip";
    private String currentWorkDir;

    public UpdateHandleClient(String host, int port) {
        setAddress(host, port);
        init(false);
    }

    public UpdateHandleClient(SocketChannel channel) {
        super(channel, null);
        init(true);
    }

    private void init(boolean isServerMode) {
        Properties properties = System.getProperties();
        currentWorkDir = properties.getProperty(ConfigKey.KEY_USER_DIR) + File.separator;
        UpdateDecoderProcessor processor = new UpdateDecoderProcessor(this, isServerMode);
        NioReceiver nioReceiver = new NioReceiver();
        nioReceiver.setHookReadNetDataHandler(processor);
        setReceive(nioReceiver);
        NioSender sender = new NioSender();
        if (isServerMode) {
            newVersion = AnalysisConfig.getInstance().getIntValue(ConfigKey.CONFIG_NEW_VERSION);
            updateFilePath = AnalysisConfig.getInstance().getValue(ConfigKey.CONFIG_UPDATE_FILE_PATH);
            sender.setSenderFeedback(this);
        }
        setSender(sender);
    }


    @Override
    protected void onConnectCompleteChannel(SocketChannel channel) {
        LogDog.d("==================== connect success ============================");
        getSender().setChannel(selectionKey, channel);
        if (currentVersion > 0) {
            //send update protocol head
            getSender().sendData(DataPacketManger.PACK_PROXY_TAG);
            //send data
            byte[] versionByte = TypeConversion.intToByte(currentVersion);
            getSender().sendData(versionByte);
        }
    }


    @Override
    public void onSenderFeedBack(INetSender iNetSender, Object o, Throwable throwable) {
        if (getSender().isSendDataEmpty()) {
            //数据发送完毕断开连接
            NioClientFactory.getFactory().removeTask(this);
        }
    }

    public void checkUpdate() {
        this.currentVersion = ConfigKey.currentVersion;
    }

    @Override
    public void onServerCheckVersion(int version) {
        boolean isHasNewVersion = version < newVersion;
        //响应是否有新版本
        //send update protocol head
        getSender().sendData(DataPacketManger.PACK_PROXY_TAG);
        //send data
        byte[] countFileByte = TypeConversion.intToByte(isHasNewVersion ? 1 : 0);
        getSender().sendData(countFileByte);

        if (isHasNewVersion && StringEnvoy.isNotEmpty(updateFilePath)) {
            File updateFile = new File(updateFilePath);
            if (updateFile.exists()) {
                long fileSize = updateFile.length();
                if (fileSize > 0) {
                    byte[] fileSizeByte = TypeConversion.long2Bytes(fileSize);
                    //响应更新文件大小
                    LogDog.d("==> send update file size to client !");
                    getSender().sendData(fileSizeByte);
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
    public void onClientCheckVersion(boolean isHasNewVersion) {

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
                    getSender().sendData(byteBuffer);
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
        return currentWorkDir + saveFile;
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
                    out = new FileOutputStream(currentWorkDir + zipEntryName);
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
            NioClientFactory.getFactory().removeTask(this);
        }
    }

    @Override
    protected void onRecovery() {
        super.onRecovery();
        LogDog.d("======================  disconnect ==============================");
    }
}
