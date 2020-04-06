package intercept;

import log.LogDog;
import storage.FileHelper;
import util.StringEnvoy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ProxyFilterManager {

    private static ProxyFilterManager proxyFilterManager;

    private String tableFile = null;
    boolean isWindowsOs = false;
    private List<String> proxyList;

    private ProxyFilterManager() {
        proxyList = new ArrayList<>();
    }

    public static ProxyFilterManager getInstance() {
        if (proxyFilterManager == null) {
            proxyFilterManager = new ProxyFilterManager();
        }
        return proxyFilterManager;
    }

    public void loadProxyTable(String tableFile) {
        this.tableFile = tableFile;
        byte[] data = FileHelper.readFileMemMap(tableFile);
        if (data == null) {
            LogDog.e("read proxy table file error or file is empty !!! path = " + tableFile);
            return;
        }
        String content = new String(data);
        initImpl(content);
    }

    private void initImpl(String content) {
        Properties props = System.getProperties();
        String os = props.getProperty("os.name").toLowerCase();
        String[] array;
        if (os.contains("windows")) {
            isWindowsOs = true;
            array = content.split("\r\n");
        } else {
            array = content.split("\n");
        }

        for (String item : array) {
            if (!item.startsWith("//") && !item.startsWith("##") && !item.startsWith("#")) {
                proxyList.add(item.replace("\r", ""));
            }
        }
        LogDog.d("Load proxy table , host number of proxyList = " + proxyList.size());
    }

    /**
     * 添加要代理的域名
     *
     * @param host
     */
    public void addProxyHost(String host) {
        boolean isHas = proxyList.contains(host);
        if (!isHas) {
            proxyList.add(host);
            Properties props = System.getProperties();
            String os = props.getProperty("os.name").toLowerCase();
            String data;
            if (os.contains("windows")) {
                data = host + "\r\n";
            } else {
                data = host + "\n";
            }
            FileHelper.writeFileMemMap(new File(tableFile), data.getBytes(), true);
        }
    }


    public boolean isNeedProxy(String host) {
        if (StringEnvoy.isNotEmpty(host)) {
            for (String tmp : proxyList) {
                if (host.contains(tmp)) {
                    return true;
                }
            }
        }
        return false;
    }

}
