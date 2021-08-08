import config.AnalysisConfig;
import config.ConfigKey;
import connect.http.server.MultipleProxyServer;
import connect.http.server.UpdateServer;
import connect.network.nio.NioClientFactory;
import connect.network.nio.NioServerFactory;
import connect.network.xhttp.XMultiplexCacheManger;
import connect.socks5.server.Socks5Server;
import cryption.DataSafeManager;
import cryption.EncryptionType;
import cryption.RSADataEnvoy;
import intercept.BuiltInInterceptFilter;
import intercept.InterceptFileChangeListener;
import intercept.InterceptFilterManager;
import intercept.WatchFileManager;
import log.LogDog;
import task.executor.TaskExecutorPoolManager;
import util.NetUtils;
import util.StringEnvoy;

import java.io.File;

public class ProxyMain {

    private static final int defaultProxyPort = 7777;
    private static final int defaultSocks5Port = 9999;
    private static final String loHost = "127.0.0.1";

    private static final String CURRENT_COMMAND = "ProxyMain";

    private static String IDE_URL = "src" + File.separator + "main" + File.separator + "resources" + File.separator;

    private static String currentWorkDir = null;
    private static String currentCommand = null;

    public static void main(String[] args) {
        currentWorkDir = System.getProperty(ConfigKey.KEY_USER_DIR) + File.separator;
        currentCommand = System.getProperty(ConfigKey.KEY_COMMAND);
        String configFile = initEnv(ConfigKey.FILE_CONFIG);
        AnalysisConfig.getInstance().analysis(configFile);
        initInterceptFilter();
        startServer();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            NioClientFactory.destroy();
            NioServerFactory.destroy();
            WatchFileManager.getInstance().destroy();
            TaskExecutorPoolManager.getInstance().destroyAll();
            XMultiplexCacheManger.destroy();
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
        boolean intercept = AnalysisConfig.getInstance().getBooleanValue(ConfigKey.CONFIG_INTERCEPT);
        if (intercept) {
            String configInterceptFile = AnalysisConfig.getInstance().getValue(ConfigKey.FILE_INTERCEPT);
            String interceptFile = initEnv(configInterceptFile);
            //init address interceptFilter
            BuiltInInterceptFilter proxyFilter = new BuiltInInterceptFilter();
            proxyFilter.init(interceptFile);
            InterceptFilterManager.getInstance().addFilter(proxyFilter);
            initWatch();
        }
    }


    private static void initWatch() {
        String interceptFileName = AnalysisConfig.getInstance().getValue(ConfigKey.FILE_INTERCEPT);
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
        String host = AnalysisConfig.getInstance().getValue(ConfigKey.CONFIG_LOCAL_HOST);
        int proxyPort = AnalysisConfig.getInstance().getIntValue(ConfigKey.CONFIG_PROXY_LOCAL_PORT);
        String updatePort = AnalysisConfig.getInstance().getValue(ConfigKey.CONFIG_UPDATE_LOCAL_PORT);
        int socks5Port = AnalysisConfig.getInstance().getIntValue(ConfigKey.CONFIG_SOCKS5_LOCAL_PORT);
        String encryption = AnalysisConfig.getInstance().getValue(ConfigKey.CONFIG_ENCRYPTION_MODE);

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
        NioServerFactory.getFactory().addTask(multipleProxyServer);
        multipleProxyServer = new MultipleProxyServer();
        multipleProxyServer.setAddress(loHost, proxyPort);
        NioServerFactory.getFactory().addTask(multipleProxyServer);
        //open update file server
        UpdateServer updateServer = new UpdateServer();
        updateServer.setAddress(host, Integer.parseInt(updatePort));
        NioServerFactory.getFactory().addTask(updateServer);
        updateServer = new UpdateServer();
        updateServer.setAddress(loHost, Integer.parseInt(updatePort));
        NioServerFactory.getFactory().addTask(updateServer);
        //open connect.socks5 proxy server
        Socks5Server socks5Server = new Socks5Server();
        socks5Server.setAddress(host, socks5Port);
        NioServerFactory.getFactory().addTask(socks5Server);
        socks5Server = new Socks5Server();
        socks5Server.setAddress(loHost, socks5Port);
        NioServerFactory.getFactory().addTask(socks5Server);
    }

}
