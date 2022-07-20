package com.open.proxy;

import com.jav.common.util.ConfigFileEnvoy;
import com.jav.common.util.StringEnvoy;
import com.open.proxy.cryption.RSADataEnvoy;
import com.open.proxy.utils.PasswordUtils;

import java.io.File;

public class OPContext {

    public static final String MONITOR_FILE = "monitor.page";
    private static final String KEY_USER_DIR = "user.dir";
    private static final String KEY_COMMAND = "sun.java.command";

    private static final String CURRENT_COMMAND = "com.open.proxy.ProxyMain";
    private static final String IDE_URL = "src" + File.separator + "main" + File.separator + "resources" + File.separator;

    private static class InnerClass {
        private final static OPContext sContext = new OPContext();
    }

    public static OPContext getInstance() {
        return InnerClass.sContext;
    }

    private OPContext() {
        mCFileEnvoy = new ConfigFileEnvoy();
    }

    private ConfigFileEnvoy mCFileEnvoy;
    private RSADataEnvoy mRsaDataEnvoy;

    private String currentWorkDir = null;
    private String currentCommand = null;

    private String mDesPassword;

    public ConfigFileEnvoy getConfigFileEnvoy() {
        return mCFileEnvoy;
    }

    public RSADataEnvoy getRsaDataEnvoy() {
        return mRsaDataEnvoy;
    }

    public void setRsaDataEnvoy(RSADataEnvoy mRsaDataEnvoy) {
        this.mRsaDataEnvoy = mRsaDataEnvoy;
    }

    public void init() {
        currentWorkDir = System.getProperty(KEY_USER_DIR) + File.separator;
        currentCommand = System.getProperty(KEY_COMMAND);
    }

    public String getCurrentWorkDir() {
        return currentWorkDir;
    }

    /**
     * 获取当前运行环境的路径
     *
     * @return
     */
    public String getRunEnvPath() {
        String filePath;
        if (CURRENT_COMMAND.equals(currentCommand)) {
            //ide model，not create file
            filePath = currentWorkDir + IDE_URL;
        } else {
            filePath = currentWorkDir;
        }
        return filePath;
    }

    public String getEnvFilePath(String file) {
        return getRunEnvPath() + file;
    }


    public String getDesPassword() {
        if (StringEnvoy.isEmpty(mDesPassword)) {
            mDesPassword = PasswordUtils.randomPassword(32);
        }
        return mDesPassword;
    }
}
