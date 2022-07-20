package com.open.proxy.intercept;


import com.jav.common.log.LogDog;

public class InterceptFileChangeWatch extends BaseFileChangeWatch {

    public InterceptFileChangeWatch(String filePath, String fileName) {
        super(filePath, fileName);
    }

    @Override
    public void onTargetChange(String targetFileName) {
        if (targetFileName.equals(mFileName)) {
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
