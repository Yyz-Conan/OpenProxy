package com.open.proxy.monitor;


import com.jav.thread.executor.LoopTask;
import com.jav.thread.executor.TaskExecutorPoolManager;
import com.open.proxy.OPContext;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public final class MonitorManager {

    private static final int SLEEP_TIME = 2000;

    private CoreTask mCoreTask;

    private MonitorManager() {
        mCoreTask = new CoreTask();
    }

    private int mConnectCount = 0;
    private int mNetUp = 1;
    private int mNetDown = 1;

    private class CoreTask extends LoopTask {

        private FileChannel mChannel;
        private ByteBuffer mBuffer;

        @Override
        protected void onInitTask() {
            String runPath = OPContext.getInstance().getRunEnvPath();
            File dir = new File(runPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File pageFile = new File(dir, OPContext.MONITOR_FILE);
            if (!pageFile.exists()) {
                try {
                    pageFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                mChannel = new RandomAccessFile(pageFile, "rw").getChannel();
                mBuffer = ByteBuffer.allocate(4096);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onRunLoopTask() {
            if (mChannel == null) {
                TaskExecutorPoolManager.getInstance().closeTask(mCoreTask);
                return;
            }
            try {
                mChannel.truncate(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mBuffer.clear();
            try {
                String countStr = String.format("             当前链接数量%d         \n", mConnectCount);
                String speedStr = String.format("     上行速度 %d kb/s    下行速度 %d kb/s      \n", getSpeed(mNetUp), getSpeed(mNetDown));
                mBuffer.put("------------------------------------------\n".getBytes());
                mBuffer.put(countStr.getBytes());
                mBuffer.put(speedStr.getBytes());
                mBuffer.put("------------------------------------------\n".getBytes());
                mBuffer.flip();
                mChannel.write(mBuffer);
            } catch (Exception e) {
                e.printStackTrace();
            }
            mConnectCount++;
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private int getSpeed(int value) {
        if (value <= 0) {
            return 0;
        }
        return value / 2 / 1024;
    }

    private final static class InnerClass {
        private final static MonitorManager sPage = new MonitorManager();
    }

    public static MonitorManager getInstance() {
        return InnerClass.sPage;
    }

    public void start() {
        TaskExecutorPoolManager.getInstance().runTask(mCoreTask);
    }

    public void stop() {
        TaskExecutorPoolManager.getInstance().closeTask(mCoreTask);
    }

    public void updateConnectCount(int count) {
        mConnectCount = count;
    }

    public void updateSpeed(int netUp, int netDown) {
        mNetUp = netUp;
        mNetDown = netDown;
    }
}
