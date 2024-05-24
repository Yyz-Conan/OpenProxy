package com.open.proxy;

import com.jav.common.cryption.joggle.EncryptionType;
import com.jav.common.log.LogDog;
import com.jav.common.util.ConfigFileEnvoy;
import com.jav.common.util.DatFileEnvoy;
import com.jav.common.util.NetUtils;
import com.jav.common.util.StringEnvoy;
import com.jav.net.security.channel.SecurityChannelBoot;
import com.jav.net.security.channel.SecurityChannelContext;
import com.jav.net.security.channel.base.ChannelEncryption;
import com.jav.net.security.guard.IpBlackListClearTimerTask;
import com.jav.thread.executor.TaskExecutorPoolManager;
import com.open.proxy.intercept.*;
import com.open.proxy.monitor.MonitorManager;
import com.open.proxy.server.http.server.MultipleProxyServer;
import com.open.proxy.server.socks5.server.Socks5Server;
import com.open.proxy.server.sync.SecuritySyncBoot;
import com.open.proxy.server.sync.SecuritySyncContext;
import com.open.proxy.server.update.UpdateServer;

import java.io.IOException;
import java.util.Map;

/**
 * 启动的类
 *
 * @author yyz
 */
public class ProxyMain {

    private static final int defaultProxyPort = 7777;
    private static final int defaultSocks5Port = 9999;
    private static final String loHost = "127.0.0.1";
    private static final String LOG_TAG = "http_proxy";

    // 测试网址
    // http://www.bifas.cn/n144925.htm
    // http://www.httpclient.cn


    public static void main(String[] args) throws IOException {
        init();
        startServer();
        shutdownHook();
    }

    private static void init() throws IOException {
        initConfig();
        initIpBack();
        initMachineIdList();
        initInterceptFilter();
    }


    private static void initConfig() throws IOException {
        OpContext.getInstance().init();
        LogDog.initLogSavePath(OpContext.getInstance().getCurrentWorkDir(), LOG_TAG);

        String configFile = OpContext.getInstance().getEnvFilePath(IConfigKey.FILE_CONFIG);
        ConfigFileEnvoy cFileEnvoy = OpContext.getInstance().getConfigFileEnvoy();
        cFileEnvoy.analysis(configFile);
    }

    private static void initMachineIdList() {
        ConfigFileEnvoy cFileEnvoy = OpContext.getInstance().getConfigFileEnvoy();
        String machineFileName = cFileEnvoy.getValue(IConfigKey.FILE_MACHINE);
        String machineFilePath = OpContext.getInstance().getEnvFilePath(machineFileName);
        DatFileEnvoy machineFileEnvoy = OpContext.getInstance().getMachineFileEnvoy();
        machineFileEnvoy.analysis(machineFilePath);
    }

    private static void initIpBack() {
        IpBlackListClearTimerTask timerTask = new IpBlackListClearTimerTask();
        String workDirPath = OpContext.getInstance().getCurrentWorkDir();
        ConfigFileEnvoy cFileEnvoy = OpContext.getInstance().getConfigFileEnvoy();
        String ipBlackPath = cFileEnvoy.getValue(IConfigKey.FILE_IP_BLACK_PATH);
        timerTask.configOutLogPath(workDirPath + ipBlackPath);
        timerTask.start();
    }

    private static void initInterceptFilter() {
        ConfigFileEnvoy cFileEnvoy = OpContext.getInstance().getConfigFileEnvoy();
        boolean isIntercept = cFileEnvoy.getBooleanValue(IConfigKey.CONFIG_INTERCEPT);
        if (isIntercept) {
            String interceptFileName = cFileEnvoy.getValue(IConfigKey.FILE_INTERCEPT);
            String interceptFilePath = OpContext.getInstance().getEnvFilePath(interceptFileName);
            // init address interceptFilter
            BuiltInInterceptFilter proxyFilter = new BuiltInInterceptFilter();
            proxyFilter.init(interceptFilePath);
            InterceptFilterManager.getInstance().addFilter(proxyFilter);
            initWatch();
        }
    }

    private static void initWatch() {
        ConfigFileEnvoy cFileEnvoy = OpContext.getInstance().getConfigFileEnvoy();
        // 添加监控黑名单配置文件的修改
        String interceptFileName = cFileEnvoy.getValue(IConfigKey.FILE_INTERCEPT);
        String envPath = OpContext.getInstance().getRunEnvPath();
        InterceptFileChangeWatch fileWatch = new InterceptFileChangeWatch(envPath, interceptFileName);
        WatchFileManager.getInstance().addWatchFile(fileWatch);
        // 添加监控cfg配置文件的修改
        ConfigFileChangeWatch cfgFileWatch = new ConfigFileChangeWatch(envPath, IConfigKey.FILE_CONFIG);
        WatchFileManager.getInstance().addWatchFile(cfgFileWatch);

        String proxyFileName = cFileEnvoy.getValue(IConfigKey.FILE_PROXY);
        ProxyFileChangeWatch proxyFileWatch = new ProxyFileChangeWatch(envPath, proxyFileName);
        WatchFileManager.getInstance().addWatchFile(proxyFileWatch);
    }

    private static void initProxyFilter() {
        ConfigFileEnvoy cFileEnvoy = OpContext.getInstance().getConfigFileEnvoy();
        String proxyFileName = cFileEnvoy.getValue(IConfigKey.FILE_PROXY);
        String proxyFilePath = OpContext.getInstance().getEnvFilePath(proxyFileName);
        // 初始化地址过滤器
        ProxyFilterManager.getInstance().loadProxyTable(proxyFilePath);
    }

    private static void startServer() throws IOException {
        ConfigFileEnvoy cFileEnvoy = OpContext.getInstance().getConfigFileEnvoy();

        SecurityChannelContext.Builder builder = new SecurityChannelContext.Builder();

        String host = cFileEnvoy.getValue(IConfigKey.CONFIG_TRANS_SERVER_HOST);
        int proxyPort = cFileEnvoy.getIntValue(IConfigKey.CONFIG_TRANS_SERVER_PORT);


        int syncPort = cFileEnvoy.getIntValue(IConfigKey.CONFIG_SYNC_SERVER_PORT);

        if (StringEnvoy.isEmpty(host) || "auto".equals(host)) {
            host = NetUtils.getLocalIp("eth0");
        }
        if (proxyPort == 0) {
            proxyPort = defaultProxyPort;
        }

        //配置http proxy 服务端
        MultipleProxyServer netProxyServer = new MultipleProxyServer();
        netProxyServer.setAddress(host, proxyPort);
        builder.addSecurityServerStarter(netProxyServer);

        //配置socks5 server
        boolean enableSocks5Proxy = cFileEnvoy.getBooleanValue(IConfigKey.CONFIG_ENABLE_SOCKS5_PROXY);
        if (enableSocks5Proxy) {
            int socks5Port = cFileEnvoy.getIntValue(IConfigKey.CONFIG_SOCKS5_SERVER_PORT);
            if (socks5Port == 0) {
                socks5Port = defaultSocks5Port;
            }
            // socks5 proxy server
            Socks5Server socks5Server = new Socks5Server();
            socks5Server.setAddress(loHost, socks5Port);
            builder.addSecurityServerStarter(socks5Server);
        }

        //配置update server
        boolean enableUpdateServer = cFileEnvoy.getBooleanValue(IConfigKey.CONFIG_ENABLE_UPDATE_SERVER);
        if (enableUpdateServer) {
            String updatePort = cFileEnvoy.getValue(IConfigKey.CONFIG_UPDATE_SERVER_PORT);
            UpdateServer updateServer = new UpdateServer();
            updateServer.setAddress(host, Integer.parseInt(updatePort));
            builder.addSecurityServerStarter(updateServer);
        }


        //配置加密
        String encryption = cFileEnvoy.getValue(IConfigKey.CONFIG_ENCRYPTION_MODE);
        String publicKeyFileName = cFileEnvoy.getValue(IConfigKey.FILE_PUBLIC_KEY);
        String privateKeyFileName = cFileEnvoy.getValue(IConfigKey.FILE_PRIVATE_KEY);
        String publicFilePath = OpContext.getInstance().getEnvFilePath(publicKeyFileName);
        String privateFilePath = OpContext.getInstance().getEnvFilePath(privateKeyFileName);

        ChannelEncryption.Builder encryptionBuilder = new ChannelEncryption.Builder();
        //init 默认使用 RSA
        encryptionBuilder.configInitEncryption(publicFilePath, privateFilePath);
        ChannelEncryption channelEncryption;
        if (EncryptionType.AES.getType().equals(encryption)) {
            channelEncryption = encryptionBuilder.builderAES(OpContext.getInstance().getAESPassword());
        } else {
            channelEncryption = encryptionBuilder.builderBase64();
        }

        String machineId = cFileEnvoy.getValue(IConfigKey.CONFIG_MACHINE_ID);
        builder.setMachineId(machineId);

        DatFileEnvoy machineFileEnvoy = OpContext.getInstance().getMachineFileEnvoy();
        builder.setMachineList(machineFileEnvoy.getDatList());

        int channelNumber = cFileEnvoy.getIntValue(IConfigKey.CONFIG_CHANNEL_NUMBER);
        builder.setChannelNumber(channelNumber);

        boolean isServerMode = cFileEnvoy.getBooleanValue(IConfigKey.CONFIG_IS_SERVER_MODE);
        boolean isEnableProxy = false;
        SecurityChannelContext channelContext;

        if (isServerMode) {
            boolean isEnableIpBlack = cFileEnvoy.getBooleanValue(IConfigKey.CONFIG_ENABLE_IP_BLACK);
            builder.setIpBlackStatus(isEnableIpBlack);

            String syncServerFileName = cFileEnvoy.getValue(IConfigKey.CONFIG_SYNC_SERVER_FILE);
            String syncServerFilePath = OpContext.getInstance().getEnvFilePath(syncServerFileName);

            ConfigFileEnvoy configFileEnvoy = new ConfigFileEnvoy();
            configFileEnvoy.analysis(syncServerFilePath);
            Map<String, String> syncServer = configFileEnvoy.getRawData();

            SecuritySyncContext.Builder syncBuilder = new SecuritySyncContext.Builder();
            syncBuilder.configSyncServer(syncServer);
            syncBuilder.configSyncServer(host, syncPort);
            syncBuilder.configProxyServer(host, proxyPort);
            syncBuilder.setMachineId(machineId);

            //启动同步服务
            SecuritySyncContext syncContext = syncBuilder.builder();
            SecuritySyncBoot.getInstance().init(syncContext);
            SecuritySyncBoot.getInstance().bootSyncServer();

            channelContext = builder.asServer(channelEncryption);
            netProxyServer.setContext(channelContext);

        } else {
            initProxyFilter();
            // 客户端模式才开启通道链接
            isEnableProxy = cFileEnvoy.getBooleanValue(IConfigKey.CONFIG_ENABLE_PROXY);
            MultipleProxyServer localProxyServer = null;
            if (isEnableProxy) {
                // init proxy channel
                String remoteHost = cFileEnvoy.getValue(IConfigKey.CONFIG_REMOTE_PROXY_HOST);
                int remotePort = cFileEnvoy.getIntValue(IConfigKey.CONFIG_REMOTE_PROXY_PORT);

                builder.configConnectSecurityServer(remoteHost, remotePort);

                //配置本地 http proxy 客户端
                localProxyServer = new MultipleProxyServer();
                localProxyServer.setAddress(loHost, proxyPort);
                builder.addSecurityServerStarter(localProxyServer);
            }
            channelContext = builder.asClient(channelEncryption);
            if (localProxyServer != null) {
                localProxyServer.setContext(channelContext);
            }
            netProxyServer.setContext(channelContext);
        }


        SecurityChannelBoot.getInstance().init(channelContext);
        SecurityChannelBoot.getInstance().startupSecurityServer();
        if (!isServerMode && isEnableProxy) {
            SecurityChannelBoot.getInstance().startConnectSecurityServer(channelNumber);
        }
        MonitorManager.getInstance().start();
    }

    private static void shutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            MonitorManager.getInstance().stop();
            SecuritySyncBoot.getInstance().release();
            WatchFileManager.getInstance().destroy();
            SecurityChannelBoot.getInstance().release();
            TaskExecutorPoolManager.getInstance().destroyAll();
            OpContext.getInstance().destroy();
        }));
    }

}
