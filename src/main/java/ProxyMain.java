import config.AnalysisConfig;
import connect.HttpProxyServer;
import connect.network.nio.NioServerFactory;
import intercept.BuiltInProxyFilter;
import intercept.ProxyFilterManager;
import intercept.WatchConfigFIleTask;
import log.LogDog;
import storage.FileHelper;
import task.executor.TaskExecutorPoolManager;
import util.IoEnvoy;
import util.NetUtils;
import util.StringEnvoy;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

public class ProxyMain {

    private static final String FILE_AT = "AddressTable.dat";
    private static final String FILE_CONFIG = "config.cfg";
    private static final String defaultPort = "7777";


    // 183.2.236.16  百度 = 14.215.177.38  czh = 58.67.203.13
    public static void main(String[] args) {
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
            URL url = ProxyMain.class.getClassLoader().getResource(configFile);
            filePath = url.getPath();
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
        String addressTableFile = initEnv(FILE_AT);
        //初始化地址过滤器
        BuiltInProxyFilter proxyFilter = new BuiltInProxyFilter();
        proxyFilter.init(addressTableFile);
        ProxyFilterManager.getInstance().addFilter(proxyFilter);
    }

    private static void startServer() {
        String configFile = initEnv(FILE_CONFIG);
        String host = null;
        String port = null;
        Map<String, String> configMap = AnalysisConfig.analysis(configFile);
        if (configMap != null) {
            host = configMap.get("host");
            port = configMap.get("port");
        }
        if (StringEnvoy.isEmpty(host) || "auto".equals(host)) {
            host = NetUtils.getLocalIp("eth2");
        }
        if (StringEnvoy.isEmpty(port)) {
            port = defaultPort;
        }
        //开启代理服务
        HttpProxyServer httpProxyServer = new HttpProxyServer();
        httpProxyServer.setAddress(host, Integer.parseInt(port));
        NioServerFactory.getFactory().open();
        NioServerFactory.getFactory().addTask(httpProxyServer);
        LogDog.d("==> HttpProxy Server address = " + host + ":" + port);
    }

    private static void initWatch() {
        Properties properties = System.getProperties();
        String dirPath = properties.getProperty("user.dir");
        WatchConfigFIleTask watchConfigFIleTask = new WatchConfigFIleTask(dirPath);
        TaskExecutorPoolManager.getInstance().runTask(watchConfigFIleTask, null);
    }
}
