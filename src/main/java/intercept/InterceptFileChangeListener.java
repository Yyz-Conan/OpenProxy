package intercept;

import intercept.joggle.IWatchFileChangeListener;
import log.LogDog;

public class InterceptFileChangeListener implements IWatchFileChangeListener {
    private String filePath;
    private String fileName;

    public InterceptFileChangeListener(String filePath, String fileName) {
        this.filePath = filePath;
        this.fileName = fileName;
    }

    @Override
    public String getTargetFile() {
        return filePath;
    }

    @Override
    public void onTargetChange(String targetFileName) {
        if (targetFileName.equals(fileName)) {
            //清除所有的过滤器
            InterceptFilterManager.getInstance().clear();
            //创建新的过滤器
            BuiltInInterceptFilter proxyFilter = new BuiltInInterceptFilter();
            proxyFilter.init(getTargetFile());
            //添加新的过滤器
            InterceptFilterManager.getInstance().addFilter(proxyFilter);
            LogDog.d(getTargetFile() + " the configuration file has been modified !!! ");
        }
    }
}
