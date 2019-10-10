package connect;

import connect.network.nio.NioClientTask;
import connect.network.nio.NioHPCClientFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 连接池
 */
public class ConnectPool {
    private Map<String, NioClientTask> connectMap;

    public ConnectPool() {
        connectMap = new HashMap<>();
    }

    public NioClientTask contains(String host) {
        NioClientTask target = null;
        synchronized (this) {
            Set<String> keys = connectMap.keySet();
            for (String key : keys) {
                if (host.contains(key)) {
                    target = connectMap.get(key);
                    break;
                }
            }
        }
        return target;
    }

    public NioClientTask get(String host) {
        synchronized (this) {
            return connectMap.get(host);
        }
    }

    public void put(String host, NioClientTask clientTask) {
        synchronized (this) {
            connectMap.put(host, clientTask);
        }
    }

    public void remove(String host) {
        synchronized (this) {
            connectMap.remove(host);
        }
    }

    public void clear() {
        synchronized (this) {
            connectMap.clear();
        }
    }

    public void destroy() {
        synchronized (this) {
            Collection<NioClientTask> clientTasks = connectMap.values();
            for (NioClientTask task : clientTasks) {
                NioHPCClientFactory.getFactory().removeTask(task);
            }
            clear();
        }
    }
}
