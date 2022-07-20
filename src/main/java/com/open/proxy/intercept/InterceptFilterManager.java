package com.open.proxy.intercept;


import com.jav.common.util.StringEnvoy;
import com.open.proxy.intercept.joggle.IInterceptFilter;

import java.util.ArrayList;
import java.util.List;

public class InterceptFilterManager {

    private List<IInterceptFilter> proxyFilterList;

    private static class InnerClass {
        private final static InterceptFilterManager sManager = new InterceptFilterManager();
    }

    private InterceptFilterManager() {
        proxyFilterList = new ArrayList<>();
    }

    public static InterceptFilterManager getInstance() {
        return InnerClass.sManager;
    }

    public void addFilter(IInterceptFilter filter) {
        if (!proxyFilterList.contains(filter)) {
            proxyFilterList.add(filter);
        }
    }

    public void removerFilter(IInterceptFilter filter) {
        proxyFilterList.remove(filter);
    }

    public void clear() {
        proxyFilterList.clear();
    }

    public boolean isIntercept(String host) {
        if (StringEnvoy.isNotEmpty(host)) {
            for (IInterceptFilter filter : proxyFilterList) {
                if (filter.isIntercept(host)) {
                    return true;
                }
            }
        }
        return false;
    }
}
