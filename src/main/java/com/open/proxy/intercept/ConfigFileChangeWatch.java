package com.open.proxy.intercept;

import com.open.proxy.OPContext;
import log.LogDog;
import util.ConfigFileEnvoy;

/**
 * 监控配置文件的热修改
 *
 * @author yyz
 */
public class ConfigFileChangeWatch extends BaseFileChangeWatch {

    public ConfigFileChangeWatch(String filePath, String fileName) {
        super(filePath, fileName);
    }


    @Override
    public void onTargetChange(String targetFileName) {
        if (targetFileName.equals(mFileName)) {
            ConfigFileEnvoy cFileEnvoy = OPContext.getInstance().getConfigFileEnvoy();
            cFileEnvoy.analysis(getTargetFile());
            LogDog.d(getTargetFile() + " the configuration file has been modified !!! ");
        }
    }
}
