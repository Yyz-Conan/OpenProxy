package com.open.proxy.server.sync;

import com.jav.net.security.util.SystemStatusTool;
import com.jav.thread.executor.LoopTask;
import com.jav.thread.executor.TaskExecutorPoolManager;

import java.nio.channels.SocketChannel;

/**
 * 同步服务的接待服务端
 *
 * @author yyz
 */
public class SecuritySyncServerReception extends SecuritySyncClient {

    private TimerSync mTimerSync;

    protected SecuritySyncServerReception(SecuritySyncContext context) {
        super(context);
    }

    protected SecuritySyncServerReception(SecuritySyncContext context, SocketChannel channel) {
        super(context, channel);
    }

    @Override
    protected void onBeReadyChannel(SocketChannel channel) {
        super.onBeReadyChannel(channel);
        SecuritySyncSender syncSender = mSyncMeter.getSender();
        SecurityServerSyncImage.getInstance().init(syncSender);

        if (getHost() != null) {
            mTimerSync = new TimerSync();
            mTimerSync.startTimer();
        }
    }

    @Override
    protected void onCloseChannel() {
        super.onCloseChannel();
        if (mTimerSync != null) {
            mTimerSync.stopTimer();
        }
    }

    private class TimerSync extends LoopTask {

        private static final int DELAY_TIME = 5 * 60 * 1000;

        @Override
        protected void onRunLoopTask() {
            SecuritySyncSender syncSender = mSyncMeter.getSender();
            long loadCount = SecuritySyncBoot.getInstance().getLocalServerLoadCount();
            byte loadAvg = SystemStatusTool.getSystemAvgLoad(loadCount);
            syncSender.requestSyncAvg(mContext.getMachineId(), mContext.getSyncPort(), loadAvg);

            try {
                Thread.sleep(DELAY_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void startTimer() {
            TaskExecutorPoolManager.getInstance().runTask(this);
        }

        public void stopTimer() {
            TaskExecutorPoolManager.getInstance().closeTask(this);
        }
    }
}
