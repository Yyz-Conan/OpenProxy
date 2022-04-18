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
import com.open.proxy.intercept.*;
import log.LogDog;
import task.executor.TaskExecutorPoolManager;
import util.ConfigFileEnvoy;
import util.NetUtils;
import util.StringEnvoy;

public class ProxyMain {

    private static final int defaultProxyPort = 7777;
    private static final int defaultSocks5Port = 9999;
    private static final String loHost = "127.0.0.1";
    private static final String LOG_TAG = "http_proxy";


    public static void main(String[] args) {
        init();
        startServer();
        shutdownHook();
    }

    private static void init() {
        OPContext.getInstance().init();
        LogDog.initLogSavePath(OPContext.getInstance().getCurrentWorkDir(), LOG_TAG);
        initConfig();
        initDataEncode();
        initInterceptFilter();
    }

    private static void initConfig() {
        String configFile = OPContext.getInstance().getEnvFilePath(IConfigKey.FILE_CONFIG);
        ConfigFileEnvoy cFileEnvoy = OPContext.getInstance().getConfigFileEnvoy();
        cFileEnvoy.analysis(configFile);
    }

    private static void initInterceptFilter() {
        ConfigFileEnvoy cFileEnvoy = OPContext.getInstance().getConfigFileEnvoy();
        boolean intercept = cFileEnvoy.getBooleanValue(IConfigKey.CONFIG_INTERCEPT);
        if (intercept) {
            String configInterceptFile = cFileEnvoy.getValue(IConfigKey.FILE_INTERCEPT);
            String interceptFile = OPContext.getInstance().getEnvFilePath(configInterceptFile);
            //init address interceptFilter
            BuiltInInterceptFilter proxyFilter = new BuiltInInterceptFilter();
            proxyFilter.init(interceptFile);
            InterceptFilterManager.getInstance().addFilter(proxyFilter);
            initWatch();
        }
    }


    private static void initWatch() {
        ConfigFileEnvoy cFileEnvoy = OPContext.getInstance().getConfigFileEnvoy();
        //添加监控黑名单配置文件的修改
        String interceptFileName = cFileEnvoy.getValue(IConfigKey.FILE_INTERCEPT);
        String envPath = OPContext.getInstance().getRunEnvPath();
        InterceptFileChangeWatch fileWatch = new InterceptFileChangeWatch(envPath, interceptFileName);
        WatchFileManager.getInstance().addWatchFile(fileWatch);
        //添加监控cfg配置文件的修改
        ConfigFileChangeWatch cfgFileWatch = new ConfigFileChangeWatch(envPath, IConfigKey.FILE_CONFIG);
        WatchFileManager.getInstance().addWatchFile(cfgFileWatch);
    }

    private static void initDataEncode() {
        ConfigFileEnvoy cFileEnvoy = OPContext.getInstance().getConfigFileEnvoy();
        String encryption = cFileEnvoy.getValue(IConfigKey.CONFIG_ENCRYPTION_MODE);
        if (EncryptionType.RSA.name().equals(encryption)) {
            initRSA();
        }
        DataSafeManager.getInstance().init();
    }

    private static void initRSA() {
        String publicKey = OPContext.getInstance().getEnvFilePath(IConfigKey.FILE_PUBLIC_KEY);
        String privateKey = OPContext.getInstance().getEnvFilePath(IConfigKey.FILE_PRIVATE_KEY);
        try {
            RSADataEnvoy rsaDataEnvoy = new RSADataEnvoy();
            rsaDataEnvoy.init(publicKey, privateKey);
            OPContext.getInstance().setRsaDataEnvoy(rsaDataEnvoy);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void startServer() {
        ConfigFileEnvoy cFileEnvoy = OPContext.getInstance().getConfigFileEnvoy();
        String host = cFileEnvoy.getValue(IConfigKey.CONFIG_LOCAL_HOST);
        int proxyPort = cFileEnvoy.getIntValue(IConfigKey.CONFIG_PROXY_LOCAL_PORT);
        String updatePort = cFileEnvoy.getValue(IConfigKey.CONFIG_UPDATE_LOCAL_PORT);
        int socks5Port = cFileEnvoy.getIntValue(IConfigKey.CONFIG_SOCKS5_LOCAL_PORT);

        if (StringEnvoy.isEmpty(host) || "auto".equals(host)) {
            host = NetUtils.getLocalIp("eth0");
        }
        if (proxyPort == 0) {
            proxyPort = defaultProxyPort;
        }

        if (socks5Port == 0) {
            proxyPort = defaultSocks5Port;
        }

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

    private static void shutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            NioClientFactory.destroy();
            NioServerFactory.destroy();
            WatchFileManager.getInstance().destroy();
            TaskExecutorPoolManager.getInstance().destroyAll();
            XMultiplexCacheManger.getInstance().destroy();
        }));
    }

}
