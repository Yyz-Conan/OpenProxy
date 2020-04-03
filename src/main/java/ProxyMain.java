import config.AnalysisConfig;
import connect.network.nio.NioServerFactory;
import connect.server.HttpProxyServer;
import cryption.EncryptionType;
import cryption.RSADataEnvoy;
import intercept.BuiltInProxyFilter;
import intercept.ProxyFilterManager;
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

    private static final String FILE_PUBLIC_KEY = "public.key";
    private static final String FILE_PRIVATE_KEY = "private.key";
    private static final String FILE_AT = "AddressTable.dat";
    private static final String FILE_CONFIG = "config.cfg";
    private static final String defaultPort = "7777";

    // 183.2.236.16  百度 = 14.215.177.38  czh = 58.67.203.13
    public static void main(String[] args) {
        String configFile = initEnv(FILE_CONFIG);
        AnalysisConfig.getInstance().analysis(configFile);
        initProxyFilter();
        initWatch();
        startServer();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> TaskExecutorPoolManager.getInstance().destroyAll()));
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

    private static void initProxyFilter() {
        boolean intercept = AnalysisConfig.getInstance().getBooleanValue("intercept");
        if (intercept) {
//        String interceptFile = AnalysisConfig.getInstance().getValue("interceptFile");
            String interceptFile = initEnv(FILE_AT);
            //初始化地址过滤器
            BuiltInProxyFilter proxyFilter = new BuiltInProxyFilter();
            proxyFilter.init(interceptFile);
            ProxyFilterManager.getInstance().addFilter(proxyFilter);
        }
    }

    private static void initRSA() {
        String publicKey = initEnv(FILE_PUBLIC_KEY);
        String privateKey = initEnv(FILE_PRIVATE_KEY);
        RSADataEnvoy.getInstance().init(publicKey, privateKey);
    }

    private static void startServer() {
        String host = AnalysisConfig.getInstance().getValue("localHost");
        String port = AnalysisConfig.getInstance().getValue("localPort");
        String encryption = AnalysisConfig.getInstance().getValue("encryptionMode");

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
        HttpProxyServer httpProxyServer = new HttpProxyServer();
        httpProxyServer.setAddress(host, Integer.parseInt(port), false);
        NioServerFactory.getFactory().open();
        NioServerFactory.getFactory().addTask(httpProxyServer);
        LogDog.d("==> HttpProxy Server address = " + host + ":" + port);
    }

    private static void initWatch() {
        Properties properties = System.getProperties();
        String value = properties.getProperty("sun.java.command");
        String dirPath = properties.getProperty("user.dir");
        String targetPath;
        if ("ProxyMain".equals(value)) {
            //idea模式下
            targetPath = dirPath + "\\out\\production\\resources\\" + FILE_AT;
        } else {
            targetPath = dirPath + "\\" + FILE_AT;
        }
        try {
            WatchConfigFileTask watchConfigFileTask = new WatchConfigFileTask(targetPath);
            TaskExecutorPoolManager.getInstance().runTask(watchConfigFileTask, null);
        } catch (Exception e) {
            LogDog.e("==> targetPath = " + targetPath + " " + e.getMessage());
//            e.printStackTrace();
        }
    }
}
