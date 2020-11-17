package intercept;

import intercept.joggle.IWatchFileChangeListener;
import log.LogDog;
import task.executor.BaseLoopTask;
import task.executor.TaskExecutorPoolManager;
import task.executor.joggle.ITaskContainer;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class WatchConfigFileTask extends BaseLoopTask {
    private WatchService watchService;
    private List<IWatchFileChangeListener> listenerList;

    private static WatchConfigFileTask watchConfigFileTask;
    private ITaskContainer container;


    private WatchConfigFileTask() throws IllegalStateException {
        listenerList = new ArrayList<>();
        try {
            watchService = FileSystems.getDefault().newWatchService();
            container = TaskExecutorPoolManager.getInstance().runTask(this, null);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static WatchConfigFileTask getInstance() throws IllegalStateException {
        if (watchConfigFileTask == null) {
            synchronized (WatchConfigFileTask.class) {
                if (watchConfigFileTask == null) {
                    watchConfigFileTask = new WatchConfigFileTask();
                }
            }
        }
        return watchConfigFileTask;
    }

    public void destroy() {
        if (container != null) {
            TaskExecutorPoolManager.getInstance().destroy(container);
            watchConfigFileTask = null;
            container = null;
        }
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            watchService = null;
        }
    }

    public void addWatchFile(IWatchFileChangeListener listener) {
        if (watchService != null) {
            listenerList.add(listener);
            String targetPath = listener.getTargetFile();
            addListener(targetPath);
        }
    }

    private void addListener(String targetPath) {
        try {
            File file = new File(targetPath);
            Path path = Paths.get(file.getParent());
            path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            LogDog.d("==> monitor " + targetPath + " profile successfully !!!");
        } catch (Exception e) {
            LogDog.e("==> monitor " + targetPath + " profile failed  !!!" + e.getMessage());
        }
    }

    @Override
    protected void onRunLoopTask() {
        WatchKey key = null;
        try {
            key = watchService.take();
        } catch (Throwable e) {
        }
        if (key != null && key.isValid()) {
            for (WatchEvent<?> event : key.pollEvents()) {
                if (event.count() == 1) {
                    //文件内容发送改变
                    Path path = (Path) event.context();
                    for (IWatchFileChangeListener listener : listenerList) {
                        listener.onTargetChange(path.toString());
                    }
                }
                key.reset();
            }
        }
    }
}
