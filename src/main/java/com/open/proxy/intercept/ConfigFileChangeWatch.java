package com.open.proxy.intercept;


import com.jav.common.log.LogDog;
import com.jav.common.util.ConfigFileEnvoy;
import com.open.proxy.OpContext;

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
        if (targetFileName.startsWith(mFileName)) {
            ConfigFileEnvoy cFileEnvoy = OpContext.getInstance().getConfigFileEnvoy();
            cFileEnvoy.analysis(getTargetFile());
            LogDog.d(getTargetFile() + " the configuration file has been modified !!! ");
        }
    }
}
