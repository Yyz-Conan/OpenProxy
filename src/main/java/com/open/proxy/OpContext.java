package com.open.proxy;

import com.jav.common.util.ConfigFileEnvoy;
import com.jav.common.util.DatFileEnvoy;
import com.jav.common.util.StringEnvoy;
import com.jav.net.base.joggle.INetFactory;
import com.jav.net.nio.NioClientFactory;
import com.jav.net.nio.NioClientTask;
import com.open.proxy.utils.PasswordUtils;

import java.io.File;
import java.io.IOException;

/**
 * Open Proxy 环境配置信息
 *
 * @author yyz
 */
public class OpContext {

    public static final String MONITOR_FILE = "channelTraffic.console";
    private static final String KEY_USER_DIR = "user.dir";
    private static final String KEY_COMMAND = "sun.java.command";

    private static final String BOOT_CLASS = "com.open.proxy.ProxyMain";
    private static final String CHEETAH_CLASS = "CheetahMain";
    private static final String TEST_SERVER_CLASS = "TestRemoteServer";
    private static final String TEST_CLIENT_CLASS = "TestLocalClient";
    private static final String NORMAL_IDE_URL = "src" + File.separator + "main" + File.separator + "resources" + File.separator;
    private static final String TEST_SERVER_IDE_URL = "src" + File.separator + "test" + File.separator + "resources" + File.separator + "server" + File.separator;
    private static final String TEST_CLIENT_IDE_URL = "src" + File.separator + "test" + File.separator + "resources" + File.separator + "client" + File.separator;

    private static final int WORK_COUNT = 4;

    private static class InnerClass {
        private final static OpContext sContext = new OpContext();
    }

    public static OpContext getInstance() {
        return InnerClass.sContext;
    }

    private ConfigFileEnvoy mCFileEnvoy;
    private DatFileEnvoy mMachineFileEnvoy;
    private INetFactory<NioClientTask> mBClientFactory;

    private String mCurrentWorkDir = null;
    private String mCurrentCommand = null;

    private String mAESPassword;

    private OpContext() {
        mCFileEnvoy = new ConfigFileEnvoy();
        mMachineFileEnvoy = new DatFileEnvoy();
        mBClientFactory = new NioClientFactory(WORK_COUNT);
    }

    public INetFactory<NioClientTask> getBClientFactory() {
        return mBClientFactory;
    }

    public ConfigFileEnvoy getConfigFileEnvoy() {
        return mCFileEnvoy;
    }

    public DatFileEnvoy getMachineFileEnvoy() {
        return mMachineFileEnvoy;
    }

    public void init() throws IOException {
        mCurrentWorkDir = System.getProperty(KEY_USER_DIR) + File.separator;
        mCurrentCommand = System.getProperty(KEY_COMMAND);
        mBClientFactory.open();
    }

    public String getCurrentWorkDir() {
        return mCurrentWorkDir;
    }

    /**
     * 获取当前运行环境的路径
     *
     * @return
     */
    public String getRunEnvPath() {
        String filePath;
        if (BOOT_CLASS.equals(mCurrentCommand)) {
            // ide model，not create file
            filePath = mCurrentWorkDir + NORMAL_IDE_URL;
        } else if (CHEETAH_CLASS.equals(mCurrentCommand)) {
            // ide model，not create file
            filePath = mCurrentWorkDir + NORMAL_IDE_URL;
        } else if (TEST_SERVER_CLASS.equals(mCurrentCommand)) {
            filePath = mCurrentWorkDir + TEST_SERVER_IDE_URL;
        } else if (TEST_CLIENT_CLASS.equals(mCurrentCommand)) {
            filePath = mCurrentWorkDir + TEST_CLIENT_IDE_URL;
        } else {
            filePath = mCurrentWorkDir;
        }
        return filePath;
    }

    public String getEnvFilePath(String file) {
        return getRunEnvPath() + file;
    }


    public byte[] getAESPassword() {
        if (StringEnvoy.isEmpty(mAESPassword)) {
            mAESPassword = PasswordUtils.randomPassword(32);
        }
        return mAESPassword.getBytes();
    }

    public void destroy() {
        mBClientFactory.close();
    }
}
