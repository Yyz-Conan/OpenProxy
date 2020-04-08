package intercept;

import intercept.joggle.IWatchFileChangeListener;
import log.LogDog;
import task.executor.BaseLoopTask;
import task.executor.TaskExecutorPoolManager;
import task.executor.joggle.ITaskContainer;

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
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            watchService = null;
        }
        if (container != null) {
            TaskExecutorPoolManager.getInstance().destroy(container);
            watchConfigFileTask = null;
            container = null;
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
        Path path = Paths.get(targetPath);
        try {
            path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            LogDog.d("==> monitor " + targetPath + " profile successfully !!!");
        } catch (IOException e) {
            LogDog.d("==> monitor " + targetPath + " profile failed  !!!" + e.getMessage());
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
                //文件内容发送改变
                Path path = (Path) event.context();
                for (IWatchFileChangeListener listener : listenerList) {
                    listener.onTargetChange(path.toString());
                }
                key.reset();
            }
        }
    }
}
