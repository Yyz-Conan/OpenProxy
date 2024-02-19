package com.open.proxy.monitor;


import com.jav.common.log.LogDog;
import com.jav.net.security.channel.base.ChannelTrafficInfo;
import com.jav.net.security.guard.SecurityChannelTraffic;
import com.jav.thread.executor.LoopTask;
import com.jav.thread.executor.LoopTaskExecutor;
import com.jav.thread.executor.TaskContainer;
import com.jav.thread.executor.TaskExecutorPoolManager;
import com.open.proxy.OpContext;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

public final class MonitorManager {

    private static final int SLEEP_TIME = 2000;

    private MonitorTask mMonitorTask;

    private MonitorManager() {
        mMonitorTask = new MonitorTask();
    }


    private class MonitorTask extends LoopTask {

        private TaskContainer mTaskContainer;

        private FileChannel mChannel;
        private ByteBuffer mBuffer;

        @Override
        protected void onInitTask() {
            String runPath = OpContext.getInstance().getRunEnvPath();
            File dir = new File(runPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File pageFile = new File(dir, OpContext.MONITOR_FILE);
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
                TaskExecutorPoolManager.getInstance().closeTask(mMonitorTask);
                return;
            }
            try {
                mChannel.truncate(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mBuffer.clear();
            List<ChannelTrafficInfo> allInfo = SecurityChannelTraffic.getInstance().getAllChannelTrafficInfo();
            if (allInfo != null) {
                for (ChannelTrafficInfo info : allInfo) {
                    if (info == null) {
                        continue;
                    }
                    String line_one = "---------------- MachineId = " + info.getMachineId() + " ----------------\n";
                    String line_two = "                           Out Traffic = " + info.getOutTrafficKB() / (SLEEP_TIME / 1000) + "kb/sec\n";
                    String line_three = "                           In Traffic = " + info.getInTrafficKB() / (SLEEP_TIME / 1000) + "kb/sec\n";
                    String line_four = "------------------------------------------------------------------------------\n";
                    mBuffer.put(line_one.getBytes());
                    mBuffer.put(line_two.getBytes());
                    mBuffer.put(line_three.getBytes());
                    mBuffer.put(line_four.getBytes());
                    info.reset();
                }
                mBuffer.flip();
                try {
                    mChannel.write(mBuffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            LoopTaskExecutor executor = mTaskContainer.getTaskExecutor();
            executor.waitTask(SLEEP_TIME);
        }

        void startTimer() {
            if (mTaskContainer == null) {
                mTaskContainer = new TaskContainer(this);
                LoopTaskExecutor executor = mTaskContainer.getTaskExecutor();
                executor.startTask();
                LogDog.i("#MM# start monitor net traffic !");
            }
        }

        void stopTimer() {
            if (mTaskContainer != null) {
                LoopTaskExecutor executor = mTaskContainer.getTaskExecutor();
                executor.stopTask();
                LogDog.i("#MM# stop monitor net traffic !");
            }
        }
    }


    private final static class InnerClass {
        private final static MonitorManager sPage = new MonitorManager();
    }

    public static MonitorManager getInstance() {
        return InnerClass.sPage;
    }

    public void start() {
        mMonitorTask.startTimer();
    }

    public void stop() {
        mMonitorTask.stopTimer();
    }


}
