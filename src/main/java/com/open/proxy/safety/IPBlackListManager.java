package com.open.proxy.safety;

import java.util.concurrent.LinkedBlockingDeque;

/**
 * 管理ip黑名单
 */
public class IPBlackListManager {

    private static final class InnerClass {
        public static final IPBlackListManager sManager = new IPBlackListManager();
    }

    public static IPBlackListManager getInstance() {
        return InnerClass.sManager;
    }

    private LinkedBlockingDeque ipCache = new LinkedBlockingDeque();

    private IPBlackListManager() {

    }

    public void add(String ip) {
        ipCache.add(ip);
    }

    public void remove(String ip) {
        ipCache.remove(ip);
    }

    public boolean isContains(String ip) {
        return ipCache.contains(ip);
    }

    public void clear() {
        ipCache.clear();
    }
}
