import config.AnalysisConfig;
import config.ConfigKey;
import connect.network.nio.NioClientFactory;
import connect.network.nio.NioServerFactory;
import connect.network.nio.SimpleSendTask;
import connect.server.MultipleProxyServer;
import cryption.EncryptionType;
import cryption.RSADataEnvoy;
import intercept.BuiltInInterceptFilter;
import intercept.InterceptFileChangeListener;
import intercept.InterceptFilterManager;
import intercept.WatchConfigFileTask;
import log.LogDog;
import storage.FileHelper;
import task.executor.TaskExecutorPoolManager;
import util.IoEnvoy;
import util.NetUtils;
import util.StringEnvoy;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

public class ProxyMain {

    private static final String defaultPort = "7777";
    private static final String loHost = "127.0.0.1";

    // 183.2.236.16  百度 = 14.215.177.38  czh = 58.67.203.13
    public static void main(String[] args) {
        String configFile = initEnv(ConfigKey.FILE_CONFIG);
        AnalysisConfig.getInstance().analysis(configFile);
        initInterceptFilter();
        startServer();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            NioClientFactory.destroy();
            NioServerFactory.destroy();
            SimpleSendTask.getInstance().close();
            WatchConfigFileTask.getInstance().destroy();
            TaskExecutorPoolManager.getInstance().destroyAll();
        }));
    }

    private static String initEnv(String configFile) {
        Properties properties = System.getProperties();
        String value = properties.getProperty("sun.java.command");

        String filePath = null;

        if ("ProxyMain".equals(value)) {
            //ide运行模式，则不创建文件
            URL url = ProxyMain.class.getResource(configFile);
            if (url != null) {
                filePath = url.getPath();
            }
        } else {
            String dirPath = properties.getProperty("user.dir");
            File atFile = new File(dirPath, configFile);
            if (atFile.exists()) {
                if (atFile.length() > 1024 * 1024) {
                    LogDog.e("Profile is too large > 1M !!!");
                } else {
                    filePath = atFile.getAbsolutePath();
                }
            } else {
                InputStream inputStream = ProxyMain.class.getResourceAsStream(configFile);
                try {
                    byte[] data = IoEnvoy.tryRead(inputStream);
                    FileHelper.writeFileMemMap(atFile, data, false);
                    filePath = atFile.getAbsolutePath();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return filePath;
    }

    private static void initInterceptFilter() {
        boolean intercept = AnalysisConfig.getInstance().getBooleanValue(ConfigKey.CONFIG_INTERCEPT);
        if (intercept) {
            String configInterceptFile = AnalysisConfig.getInstance().getValue(ConfigKey.FILE_INTERCEPT);
            String interceptFile = initEnv(configInterceptFile);
            //初始化地址过滤器
            BuiltInInterceptFilter proxyFilter = new BuiltInInterceptFilter();
            proxyFilter.init(interceptFile);
            InterceptFilterManager.getInstance().addFilter(proxyFilter);
            initWatch();
        }
    }


    private static void initWatch() {
        Properties properties = System.getProperties();
        String value = properties.getProperty("sun.java.command");
        String dirPath = properties.getProperty("user.dir");
        String fileName = AnalysisConfig.getInstance().getValue(ConfigKey.FILE_INTERCEPT);
        if ("ProxyMain".equals(value)) {
            //idea模式下
            dirPath = dirPath + "\\out\\production\\resources";
        }
        InterceptFileChangeListener changeListener = new InterceptFileChangeListener(dirPath, fileName);
        WatchConfigFileTask.getInstance().addWatchFile(changeListener);
    }

    private static void initRSA() {
        String publicKey = initEnv(ConfigKey.FILE_PUBLIC_KEY);
        String privateKey = initEnv(ConfigKey.FILE_PRIVATE_KEY);
        RSADataEnvoy.getInstance().init(publicKey, privateKey);
    }

    private static void startServer() {
        String host = AnalysisConfig.getInstance().getValue(ConfigKey.CONFIG_LOCAL_HOST);
        String port = AnalysisConfig.getInstance().getValue(ConfigKey.CONFIG_LOCAL_PORT);
        String encryption = AnalysisConfig.getInstance().getValue(ConfigKey.CONFIG_ENCRYPTION_MODE);

        if (StringEnvoy.isEmpty(host) || "auto".equals(host)) {
            host = NetUtils.getLocalIp("eth0");
        }
        if (StringEnvoy.isEmpty(port)) {
            port = defaultPort;
        }

        if (EncryptionType.RSA.name().equals(encryption)) {
            initRSA();
        }

        //开启代理服务
        SimpleSendTask.getInstance().open();
        MultipleProxyServer multipleProxyServer = new MultipleProxyServer();
        multipleProxyServer.setAddress(host, Integer.parseInt(port), false);
        NioServerFactory.getFactory().open();
        NioServerFactory.getFactory().addTask(multipleProxyServer);
        multipleProxyServer = new MultipleProxyServer();
        multipleProxyServer.setAddress(loHost, Integer.parseInt(port), false);
        NioServerFactory.getFactory().addTask(multipleProxyServer);
    }

}
