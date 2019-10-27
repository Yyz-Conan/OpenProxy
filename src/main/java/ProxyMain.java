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

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

public class ProxyMain {

    private static final String AT_FILE_NAME = "AddressTable.dat";


    // 183.2.236.16  百度 = 14.215.177.38  czh = 58.67.203.13
    public static void main(String[] args) {
        startServer();
    }


    public static void startServer() {
//        test();
        Properties properties = System.getProperties();
        String value = properties.getProperty("sun.java.command");

        String filePath = null;

        if ("ProxyMain".equals(value)) {
            //ide运行模式，则不创建文件
            URL url = ProxyMain.class.getClassLoader().getResource(AT_FILE_NAME);
            filePath = url.getPath();

        } else {
            String dirPath = properties.getProperty("user.dir");
            File atFile = new File(dirPath, AT_FILE_NAME);
            if (atFile.exists()) {
                if (atFile.length() > 1024 * 1024 * 10) {
                    LogDog.e("Profile is too large > 10M !!!");
                } else {
                    filePath = atFile.getAbsolutePath();
                }
            } else {
                InputStream inputStream = ProxyMain.class.getResourceAsStream(AT_FILE_NAME);
                try {
                    byte[] data = IoEnvoy.tryRead(inputStream);
                    FileHelper.writeFileMemMap(atFile, data, false);
                    filePath = atFile.getAbsolutePath();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        //初始化地址过滤器
        BuiltInProxyFilter proxyFilter = new BuiltInProxyFilter();
        proxyFilter.init(filePath);
        ProxyFilterManager.getInstance().addFilter(proxyFilter);

        //开启代理服务
        HttpProxyServer httpProxyServer = new HttpProxyServer();
        String host = NetUtils.getLocalIp("eth2");
        int defaultPort = 7777;
        httpProxyServer.setAddress(host, defaultPort);
        NioServerFactory.getFactory().open();
        NioServerFactory.getFactory().addTask(httpProxyServer);
        LogDog.d("==> HttpProxy Server address = " + host + ":" + defaultPort);

        WatchConfigFIleTask watchConfigFIleTask = new WatchConfigFIleTask(filePath);
        TaskExecutorPoolManager.getInstance().runTask(watchConfigFIleTask, null);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> TaskExecutorPoolManager.getInstance().destroyAll()));
    }
}
