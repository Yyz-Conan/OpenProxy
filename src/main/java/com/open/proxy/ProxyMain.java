package com.open.proxy;

import com.jav.common.log.LogDog;
import com.jav.common.util.ConfigFileEnvoy;
import com.jav.common.util.DatFileEnvoy;
import com.jav.common.util.NetUtils;
import com.jav.common.util.StringEnvoy;
import com.jav.net.nio.NioClientFactory;
import com.jav.net.nio.NioServerFactory;
import com.jav.net.security.channel.SecurityChannelContext;
import com.jav.net.security.channel.SecurityChannelManager;
import com.jav.net.security.guard.IpBlackListClearTimerTask;
import com.jav.thread.executor.TaskExecutorPoolManager;
import com.open.proxy.connect.http.server.MultipleProxyServer;
import com.open.proxy.intercept.*;

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

    public static void main(String[] args) {
        init();
        startServer();
        shutdownHook();
    }

    private static void init() {
        initConfig();
        initIpBack();
        initMachineIdList();
        initInterceptFilter();
    }


    private static void initConfig() {
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
    }

    private static void initProxyFilter() {
        ConfigFileEnvoy cFileEnvoy = OpContext.getInstance().getConfigFileEnvoy();
        String proxyFileName = cFileEnvoy.getValue(IConfigKey.FILE_PROXY);
        String proxyFilePath = OpContext.getInstance().getEnvFilePath(proxyFileName);
        // 初始化地址过滤器
        ProxyFilterManager.getInstance().loadProxyTable(proxyFilePath);
    }

    private static void startServer() {
        ConfigFileEnvoy cFileEnvoy = OpContext.getInstance().getConfigFileEnvoy();
        String host = cFileEnvoy.getValue(IConfigKey.CONFIG_TRANS_SERVER_HOST);
        int proxyPort = cFileEnvoy.getIntValue(IConfigKey.CONFIG_TRANS_SERVER_PORT);
        String updatePort = cFileEnvoy.getValue(IConfigKey.CONFIG_UPDATE_SERVER_PORT);
        int socks5Port = cFileEnvoy.getIntValue(IConfigKey.CONFIG_SOCKS5_SERVER_PORT);
        int syncPort = cFileEnvoy.getIntValue(IConfigKey.CONFIG_SYNC_SERVER_PORT);

        if (StringEnvoy.isEmpty(host) || "auto".equals(host)) {
            host = NetUtils.getLocalIp("eth0");
        }
        if (proxyPort == 0) {
            proxyPort = defaultProxyPort;
        }

        if (socks5Port == 0) {
            socks5Port = defaultSocks5Port;
        }

        // open proxy server
        MultipleProxyServer netProxyServer = new MultipleProxyServer();
        netProxyServer.setAddress(host, proxyPort);


        // // open update file server
        // UpdateServer updateServer = new UpdateServer();
        // updateServer.setAddress(host, Integer.parseInt(updatePort));
        // NioServerFactory.getFactory().getNetTaskComponent().addExecTask(updateServer);
        // updateServer = new UpdateServer();
        // updateServer.setAddress(loHost, Integer.parseInt(updatePort));
        // NioServerFactory.getFactory().getNetTaskComponent().addExecTask(updateServer);
        //
        // // open com.open.proxy.connect.socks5 proxy server
        // Socks5Server socks5Server = new Socks5Server();
        // socks5Server.setAddress(host, socks5Port);
        // NioServerFactory.getFactory().getNetTaskComponent().addExecTask(socks5Server);
        // socks5Server = new Socks5Server();
        // socks5Server.setAddress(loHost, socks5Port);
        // NioServerFactory.getFactory().getNetTaskComponent().addExecTask(socks5Server);

        SecurityChannelContext.Builder builder = new SecurityChannelContext.Builder();

        boolean isServerMode = cFileEnvoy.getBooleanValue(IConfigKey.CONFIG_IS_SERVER_MODE);
        String encryption = cFileEnvoy.getValue(IConfigKey.CONFIG_ENCRYPTION_MODE);
        builder.setEncryption(encryption, OpContext.getInstance().getDesPassword());
        String machineId = cFileEnvoy.getValue(IConfigKey.CONFIG_MACHINE_ID);
        builder.setMachineId(machineId);
        String publicKeyFileName = cFileEnvoy.getValue(IConfigKey.FILE_PUBLIC_KEY);
        String privateKeyFileName = cFileEnvoy.getValue(IConfigKey.FILE_PRIVATE_KEY);
        String publicFilePath = OpContext.getInstance().getEnvFilePath(publicKeyFileName);
        String privateFilePath = OpContext.getInstance().getEnvFilePath(privateKeyFileName);
        builder.setInitRsaKeyFile(publicFilePath, privateFilePath);
        DatFileEnvoy machineFileEnvoy = OpContext.getInstance().getMachineFileEnvoy();
        builder.setMachineList(machineFileEnvoy.getDatList());

        builder.setServerMode(isServerMode);
        builder.configBootSecurityServer(netProxyServer);

        int channelNumber = cFileEnvoy.getIntValue(IConfigKey.CONFIG_CHANNEL_NUMBER);
        builder.setChannelNumber(channelNumber);


        if (isServerMode) {
            boolean isEnableIpBlack = cFileEnvoy.getBooleanValue(IConfigKey.CONFIG_ENABLE_IP_BLACK);
            builder.setIpBlackStatus(isEnableIpBlack);

            String syncServerFileName = cFileEnvoy.getValue(IConfigKey.CONFIG_SYNC_SERVER_FILE);
            String syncServerFilePath = OpContext.getInstance().getEnvFilePath(syncServerFileName);

            ConfigFileEnvoy configFileEnvoy = new ConfigFileEnvoy();
            configFileEnvoy.analysis(syncServerFilePath);
            Map<String, String> syncServer = configFileEnvoy.getRawData();
            builder.setSyncServer(syncServer);
            builder.setLocalSyncInfo(host, proxyPort);

            // SecuritySyncService netSyncService = new SecuritySyncService();
            // netSyncService.setAddress(host, syncPort);
            // builder.configBootSecurityServer(netSyncService);
        } else {
            initProxyFilter();
            // 客户端模式才开启通道链接
            boolean isEnableProxy = cFileEnvoy.getBooleanValue(IConfigKey.CONFIG_ENABLE_PROXY);
            if (isEnableProxy) {
                // init proxy channel
                String remoteHost = cFileEnvoy.getValue(IConfigKey.CONFIG_REMOTE_PROXY_HOST);
                int remotePort = cFileEnvoy.getIntValue(IConfigKey.CONFIG_REMOTE_PROXY_PORT);

                builder.configConnectSecurityServer(remoteHost, remotePort);

                MultipleProxyServer localProxyServer = new MultipleProxyServer();
                localProxyServer.setAddress(loHost, proxyPort);
                builder.configBootSecurityServer(localProxyServer);
            }
        }
        SecurityChannelContext context = builder.builder();
        SecurityChannelManager.getInstance().init(context);
    }

    private static void shutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            NioClientFactory.destroy();
            NioServerFactory.destroy();
            WatchFileManager.getInstance().destroy();
            SecurityChannelManager.getInstance().release();
            TaskExecutorPoolManager.getInstance().destroyAll();
            SecurityChannelManager.getInstance().release();
            OpContext.getInstance().destroy();
        }));
    }

}
