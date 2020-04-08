package intercept;

import intercept.joggle.IWatchFileChangeListener;
import log.LogDog;

public class InterceptFileChangeListener implements IWatchFileChangeListener {
    private String targetFile;

    public InterceptFileChangeListener(String targetFile) {
        this.targetFile = targetFile;
    }

    @Override
    public String getTargetFile() {
        return targetFile;
    }

    @Override
    public void onTargetChange(String file) {
        if (file.equals(targetFile)) {
            //清除所有的过滤器
            InterceptFilterManager.getInstance().clear();
            //创建新的过滤器
            BuiltInInterceptFilter proxyFilter = new BuiltInInterceptFilter();
            proxyFilter.init(targetFile);
            //添加新的过滤器
            InterceptFilterManager.getInstance().addFilter(proxyFilter);
            LogDog.d(targetFile + "配置文件发生修改 !!! ");
        }
    }
}
