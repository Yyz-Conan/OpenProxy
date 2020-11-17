package intercept;

import intercept.joggle.IInterceptFilter;
import util.StringEnvoy;

import java.util.ArrayList;
import java.util.List;

public class InterceptFilterManager {

    private List<IInterceptFilter> proxyFilterList;
    private static InterceptFilterManager proxyFilterManager = null;

    private InterceptFilterManager() {
        proxyFilterList = new ArrayList<>();
    }

    public static InterceptFilterManager getInstance() {
        if (proxyFilterManager == null) {
            synchronized (InterceptFilterManager.class) {
                if (proxyFilterManager == null) {
                    proxyFilterManager = new InterceptFilterManager();
                }
            }
        }
        return proxyFilterManager;
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
