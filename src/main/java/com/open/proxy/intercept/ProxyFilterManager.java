package com.open.proxy.intercept;


import com.jav.common.log.LogDog;
import com.jav.common.storage.FileHelper;
import com.jav.common.util.StringEnvoy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProxyFilterManager {

    private String tableFile = null;
    private volatile List<String> proxyList;
    private volatile List<String> localList;

    private static class InnerClass {
        private final static ProxyFilterManager sManager = new ProxyFilterManager();
    }

    private ProxyFilterManager() {
        proxyList = new ArrayList<>();
        localList = new ArrayList<>();
    }

    public static ProxyFilterManager getInstance() {
        return InnerClass.sManager;
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
        synchronized (this) {
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
        synchronized (this) {
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
        String[] hostArray = host.split("\\.");
        synchronized (this) {
            for (String tmp : proxyList) {
                String[] tmpArray = tmp.split("\\.");
                boolean isMatch = compareDomainName(tmpArray, hostArray);
                if (isMatch) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean compareDomainName(String[] source, String[] target) {
        if (source.length > target.length) {
            return false;
        }
        int match = 0;
        int diff = target.length - source.length;
        for (int index = source.length - 1; index >= 0; index--) {
            if (source[index].equals(target[index + diff])) {
                match++;
            }
        }
        return match == source.length;
    }

}
