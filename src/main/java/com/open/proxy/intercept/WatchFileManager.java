package com.open.proxy.intercept;


import com.jav.common.log.LogDog;
import com.jav.thread.executor.LoopTask;
import com.jav.thread.executor.TaskContainer;
import com.open.proxy.intercept.joggle.IFileChangeWatch;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class WatchFileManager extends LoopTask {
    private WatchService mWatchService;
    private List<IFileChangeWatch> mListenerList;

    private static WatchFileManager sWatchConfigFileTask;
    private TaskContainer mContainer;

    private static class InnerClass {
        private final static WatchFileManager sManager = new WatchFileManager();
    }

    private WatchFileManager() throws IllegalStateException {
        mListenerList = new ArrayList<>();
        try {
            mWatchService = FileSystems.getDefault().newWatchService();
            mContainer = new TaskContainer(this);
            mContainer.getTaskExecutor().startTask();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static WatchFileManager getInstance() throws IllegalStateException {
        return InnerClass.sManager;
    }

    public void destroy() {
        if (mContainer != null) {
            mContainer.getTaskExecutor().stopTask();
            sWatchConfigFileTask = null;
            mContainer = null;
        }
        if (mWatchService != null) {
            try {
                mWatchService.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mWatchService = null;
        }
    }

    public void addWatchFile(IFileChangeWatch listener) {
        if (mWatchService != null) {
            mListenerList.add(listener);
            String targetPath = listener.getTargetFile();
            addListener(targetPath);
        }
    }

    private void addListener(String targetPath) {
        try {
            File file = new File(targetPath);
            Path path = Paths.get(file.getParent());
            path.register(mWatchService, StandardWatchEventKinds.ENTRY_MODIFY);
            LogDog.d("==> monitor " + targetPath + " profile successfully !!!");
        } catch (Exception e) {
            LogDog.e("==> monitor " + targetPath + " profile failed  !!!" + e.getMessage());
        }
    }

    @Override
    protected void onRunLoopTask() {
        WatchKey key = null;
        try {
            key = mWatchService.take();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        if (key != null && key.isValid()) {
            for (WatchEvent<?> event : key.pollEvents()) {
                if (event.count() == 1) {
                    //文件内容发送改变
                    Path path = (Path) event.context();
                    for (IFileChangeWatch listener : mListenerList) {
                        listener.onTargetChange(path.toString());
                    }
                }
                key.reset();
            }
        }
    }
}
