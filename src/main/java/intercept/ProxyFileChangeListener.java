package intercept;

import intercept.joggle.IWatchFileChangeListener;

import java.io.File;

public class ProxyFileChangeListener implements IWatchFileChangeListener {
    private String filePath;
    private String fileName;

    public ProxyFileChangeListener(String filePath, String fileName) {
        this.filePath = filePath;
        this.fileName = fileName;
    }

    @Override
    public String getTargetFile() {
        return filePath + File.separator + fileName;
    }

    @Override
    public void onTargetChange(String targetFileName) {
        if (targetFileName.equals(fileName)) {
            ProxyFilterManager.getInstance().loadProxyTable(getTargetFile());
        }
    }
}
