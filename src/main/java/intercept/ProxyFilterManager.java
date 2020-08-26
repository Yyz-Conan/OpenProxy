package intercept;

import log.LogDog;
import storage.FileHelper;
import util.StringEnvoy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProxyFilterManager {

    private static ProxyFilterManager proxyFilterManager;

    private String tableFile = null;
    private volatile List<String> proxyList;
    private volatile List<String> localList;

    private ProxyFilterManager() {
        proxyList = new ArrayList<>();
        localList = new ArrayList<>();
    }

    public static ProxyFilterManager getInstance() {
        if (proxyFilterManager == null) {
            synchronized (ProxyFilterManager.class) {
                if (proxyFilterManager == null) {
                    proxyFilterManager = new ProxyFilterManager();
                }
            }
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
        String[] array = content.split("\n");
        proxyList.clear();
        for (String item : array) {
            if (!item.startsWith("//") && !item.startsWith("##") && !item.startsWith("#")) {
                String host = item.replace("\r", "");
                if (host.startsWith("~")) {
                    localList.add(host.substring(1));
                } else {
                    proxyList.add(host);
                }
            }
        }
        LogDog.d("Load proxy table , proxy host number  = " + proxyList.size() + " local host number = " + localList.size());
    }

    /**
     * 添加要代理的域名
     *
     * @param host
     */
    public void addProxyHost(String host) {
        if (StringEnvoy.isEmpty(host)) {
            return;
        }
        synchronized (proxyFilterManager) {
            host = host.replace("www.", "");
            boolean isHas = proxyList.contains(host);
            boolean localHas = localList.contains(host);
            if (!isHas && !localHas) {
                proxyList.add(host);
                String data = host + "\n";
                FileHelper.writeFileMemMap(new File(tableFile), data.getBytes(), true);
            }
        }
    }

    public boolean isNoProxy(String host) {
        if (StringEnvoy.isEmpty(host)) {
            return false;
        }
        synchronized (proxyFilterManager) {
            for (String tmp : localList) {
                if (host.contains(tmp)) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean isNeedProxy(String host) {
        if (StringEnvoy.isEmpty(host)) {
            return false;
        }
        synchronized (proxyFilterManager) {
            for (String tmp : proxyList) {
                if (host.contains(tmp)) {
                    return true;
                }
            }
            return false;
        }
    }

}
