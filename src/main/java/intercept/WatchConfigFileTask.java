package intercept;

import log.LogDog;
import task.executor.BaseLoopTask;
import util.StringEnvoy;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

public class WatchConfigFileTask extends BaseLoopTask {
    private String targetPath;
    private String targetFile;
    private WatchService watchService;

    public WatchConfigFileTask(String targetPath) {
        if (StringEnvoy.isEmpty(targetPath)) {
            throw new NullPointerException("targetPath is null");
        }
        File file = new File(targetPath);
        if (!file.exists()) {
            throw new IllegalStateException("targetPath file is null");
        }
        this.targetPath = file.getParent();
        targetFile = file.getName();
    }

    @Override
    protected void onInitTask() {
        Path path = Paths.get(targetPath);
        try {
            watchService = FileSystems.getDefault().newWatchService();
            path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            LogDog.d("==> monitor " + targetPath + "\\" + targetFile + " profile task started successfully !!!");
        } catch (IOException e) {
            LogDog.d("==> monitor " + targetPath + "\\" + targetFile + " profile task failed to start !!!" + e.getMessage());
        }
    }

    @Override
    protected void onRunLoopTask() {
        WatchKey key = null;
        try {
            key = watchService.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (key != null && key.isValid()) {
            for (WatchEvent<?> event : key.pollEvents()) {
                //文件内容发送改变
                Path path = (Path) event.context();
                if (path.toString().equals(targetFile)) {
                    LogDog.d(targetFile + "配置文件发生修改 !!! ");
                    //清除所有的过滤器
                    InterceptFilterManager.getInstance().clear();
                    //创建新的过滤器
                    BuiltInInterceptFilter proxyFilter = new BuiltInInterceptFilter();
                    proxyFilter.init(targetPath + File.separator + targetFile);
                    //添加新的过滤器
                    InterceptFilterManager.getInstance().addFilter(proxyFilter);
                }
                key.reset();
            }
        }
    }

    @Override
    protected void onDestroyTask() {
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
