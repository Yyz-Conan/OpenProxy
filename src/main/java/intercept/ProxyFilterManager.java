package intercept;

import intercept.joggle.IProxyFilter;
import util.StringEnvoy;

import java.util.ArrayList;
import java.util.List;

public class ProxyFilterManager implements IProxyFilter {

    private List<IProxyFilter> proxyFilterList;
    private static ProxyFilterManager proxyFilterManager = null;

    private ProxyFilterManager() {
        proxyFilterList = new ArrayList<>();
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

    public void addFilter(IProxyFilter filter) {
        if (!proxyFilterList.contains(filter)) {
            proxyFilterList.add(filter);
        }
    }

    public void removerFilter(IProxyFilter filter) {
        proxyFilterList.remove(filter);
    }

    @Override
    public boolean isIntercept(String host) {
        if (StringEnvoy.isNotEmpty(host)) {
            for (IProxyFilter filter : proxyFilterList) {
                if (filter.isIntercept(host)) {
                    return true;
                }
            }
        }
        return false;
    }
}
