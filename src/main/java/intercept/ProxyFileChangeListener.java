package intercept;

import intercept.joggle.IWatchFileChangeListener;

public class ProxyFileChangeListener implements IWatchFileChangeListener {
    private String targetFile;

    public ProxyFileChangeListener(String targetFile) {
        this.targetFile = targetFile;
    }

    @Override
    public String getTargetFile() {
        return targetFile;
    }

    @Override
    public void onTargetChange(String file) {
        if (file.equals(targetFile)) {
            ProxyFilterManager.getInstance().loadProxyTable(file);
        }
    }
}
