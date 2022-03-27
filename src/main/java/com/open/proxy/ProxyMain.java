package com.open.proxy;

import com.currency.net.nio.NioClientFactory;
import com.currency.net.nio.NioServerFactory;
import com.currency.net.xhttp.XMultiplexCacheManger;
import com.open.proxy.connect.http.server.MultipleProxyServer;
import com.open.proxy.connect.http.server.UpdateServer;
import com.open.proxy.connect.socks5.server.Socks5Server;
import com.open.proxy.cryption.DataSafeManager;
import com.open.proxy.cryption.EncryptionType;
import com.open.proxy.cryption.RSADataEnvoy;
import com.open.proxy.intercept.BuiltInInterceptFilter;
import com.open.proxy.intercept.InterceptFileChangeListener;
import com.open.proxy.intercept.InterceptFilterManager;
import com.open.proxy.intercept.WatchFileManager;
import log.LogDog;
import task.executor.TaskExecutorPoolManager;
import util.ConfigFileEnvoy;
import util.NetUtils;
import util.StringEnvoy;

import java.io.File;

public class ProxyMain {

    private static final int defaultProxyPort = 7777;
    private static final int defaultSocks5Port = 9999;
    private static final String loHost = "127.0.0.1";

    private static final String CURRENT_COMMAND = "com.open.proxy.ProxyMain";

    private static String IDE_URL = "src" + File.separator + "main" + File.separator + "resources" + File.separator;

    private static String currentWorkDir = null;
    private static String currentCommand = null;

    public static void main(String[] args) {
        currentWorkDir = System.getProperty(ConfigKey.KEY_USER_DIR) + File.separator;
        currentCommand = System.getProperty(ConfigKey.KEY_COMMAND);
        String configFile = initEnv(ConfigKey.FILE_CONFIG);
        ConfigFileEnvoy cFileEnvoy = OPContext.getInstance().getConfigFileEnvoy();
        cFileEnvoy.analysis(configFile);
        initInterceptFilter();
        startServer();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            NioClientFactory.destroy();
            NioServerFactory.destroy();
            WatchFileManager.getInstance().destroy();
            TaskExecutorPoolManager.getInstance().destroyAll();
            XMultiplexCacheManger.getInstance().destroy();
        }));
        LogDog.initLogSavePath(currentWorkDir, "http_proxy");
    }

    private static String initEnv(String configFile) {
        String filePath;
        if (CURRENT_COMMAND.equals(currentCommand)) {
            //ide model，not create file
            filePath = currentWorkDir + IDE_URL + configFile;
        } else {
            filePath = currentWorkDir + configFile;
        }
        return filePath;
    }

    private static void initInterceptFilter() {
        ConfigFileEnvoy cFileEnvoy = OPContext.getInstance().getConfigFileEnvoy();
        boolean intercept = cFileEnvoy.getBooleanValue(ConfigKey.CONFIG_INTERCEPT);
        if (intercept) {
            String configInterceptFile = cFileEnvoy.getValue(ConfigKey.FILE_INTERCEPT);
            String interceptFile = initEnv(configInterceptFile);
            //init address interceptFilter
            BuiltInInterceptFilter proxyFilter = new BuiltInInterceptFilter();
            proxyFilter.init(interceptFile);
            InterceptFilterManager.getInstance().addFilter(proxyFilter);
            initWatch();
        }
    }


    private static void initWatch() {
        ConfigFileEnvoy cFileEnvoy = OPContext.getInstance().getConfigFileEnvoy();
        String interceptFileName = cFileEnvoy.getValue(ConfigKey.FILE_INTERCEPT);
        String interceptPath = initEnv(interceptFileName);
        InterceptFileChangeListener changeListener = new InterceptFileChangeListener(interceptPath, interceptFileName);
        WatchFileManager.getInstance().addWatchFile(changeListener);
    }

    private static void initRSA() {
        String publicKey = initEnv(ConfigKey.FILE_PUBLIC_KEY);
        String privateKey = initEnv(ConfigKey.FILE_PRIVATE_KEY);
        RSADataEnvoy.getInstance().init(publicKey, privateKey);
    }

    private static void startServer() {
        ConfigFileEnvoy cFileEnvoy = OPContext.getInstance().getConfigFileEnvoy();
        String host = cFileEnvoy.getValue(ConfigKey.CONFIG_LOCAL_HOST);
        int proxyPort = cFileEnvoy.getIntValue(ConfigKey.CONFIG_PROXY_LOCAL_PORT);
        String updatePort = cFileEnvoy.getValue(ConfigKey.CONFIG_UPDATE_LOCAL_PORT);
        int socks5Port = cFileEnvoy.getIntValue(ConfigKey.CONFIG_SOCKS5_LOCAL_PORT);
        String encryption = cFileEnvoy.getValue(ConfigKey.CONFIG_ENCRYPTION_MODE);

        if (StringEnvoy.isEmpty(host) || "auto".equals(host)) {
            host = NetUtils.getLocalIp("eth0");
        }
        if (proxyPort == 0) {
            proxyPort = defaultProxyPort;
        }

        if (socks5Port == 0) {
            proxyPort = defaultSocks5Port;
        }

        if (EncryptionType.RSA.name().equals(encryption)) {
            initRSA();
        }
        DataSafeManager.getInstance().init();

        //open proxy server
        MultipleProxyServer multipleProxyServer = new MultipleProxyServer();
        multipleProxyServer.setAddress(host, proxyPort);
        NioServerFactory.getFactory().open();
        NioServerFactory.getFactory().getNetTaskContainer().addExecTask(multipleProxyServer);
        multipleProxyServer = new MultipleProxyServer();
        multipleProxyServer.setAddress(loHost, proxyPort);
        NioServerFactory.getFactory().getNetTaskContainer().addExecTask(multipleProxyServer);
        //open update file server
        UpdateServer updateServer = new UpdateServer();
        updateServer.setAddress(host, Integer.parseInt(updatePort));
        NioServerFactory.getFactory().getNetTaskContainer().addExecTask(updateServer);
        updateServer = new UpdateServer();
        updateServer.setAddress(loHost, Integer.parseInt(updatePort));
        NioServerFactory.getFactory().getNetTaskContainer().addExecTask(updateServer);
        //open com.open.proxy.connect.socks5 proxy server
        Socks5Server socks5Server = new Socks5Server();
        socks5Server.setAddress(host, socks5Port);
        NioServerFactory.getFactory().getNetTaskContainer().addExecTask(socks5Server);
        socks5Server = new Socks5Server();
        socks5Server.setAddress(loHost, socks5Port);
        NioServerFactory.getFactory().getNetTaskContainer().addExecTask(socks5Server);
    }

}
