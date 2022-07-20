package com.open.proxy.utils;


import com.jav.thread.executor.LoopTask;
import com.jav.thread.executor.LoopTaskExecutor;
import com.jav.thread.executor.TaskContainer;
import com.open.proxy.safety.IPBlackListManager;

import java.util.Calendar;

/**
 * ip黑名单定时任务，凌晨执行
 */
public class IPBlackListClearTimerTask extends LoopTask {

    private TaskContainer mContainer;


    @Override
    protected void onRunLoopTask() {
        LoopTaskExecutor executor = mContainer.getTaskExecutor();
        executor.waitTask(getMilliSecondsNextEarlyMorning());
        if (executor.isLoopState()) {
            //到凌晨开始清楚ip黑名单
            IPBlackListManager.getInstance().clear();
        }
    }

    public void start() {
        if (mContainer == null) {
            mContainer = new TaskContainer(this);
            LoopTaskExecutor executor = mContainer.getTaskExecutor();
            executor.startTask();
        }
    }

    public void stop() {
        if (mContainer != null) {
            LoopTaskExecutor executor = mContainer.getTaskExecutor();
            executor.stopTask();
        }
    }

    /**
     * 获取当前时间到凌晨12点的毫秒
     *
     * @return
     */
    public Long getMilliSecondsNextEarlyMorning() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return (cal.getTimeInMillis() - System.currentTimeMillis());
    }
}
