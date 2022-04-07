package com.open.proxy.intercept;

/**
 * 监控代理host文件的热更新
 * @author yyz
 */
public class ProxyFileChangeWatch extends BaseFileChangeWatch {

    public ProxyFileChangeWatch(String filePath, String fileName) {
        super(filePath, fileName);
    }

    @Override
    public void onTargetChange(String targetFileName) {
        if (targetFileName.equals(mFileName)) {
            ProxyFilterManager.getInstance().loadProxyTable(getTargetFile());
        }
    }
}
