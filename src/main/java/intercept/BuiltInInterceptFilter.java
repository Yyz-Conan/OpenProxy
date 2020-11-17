package intercept;

import intercept.joggle.IInterceptFilter;
import log.LogDog;
import util.StringEnvoy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class BuiltInInterceptFilter implements IInterceptFilter {
    private List<String> blackList;
    private List<String> whiteList;

    public BuiltInInterceptFilter() {
        blackList = new ArrayList<>();
        whiteList = new ArrayList<>();
    }

    public void init(String ipTablePath) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(ipTablePath)));
            String item;
            do {
                item = reader.readLine();
                if (StringEnvoy.isNotEmpty(item) && !item.startsWith("//") && !item.startsWith("##") && !item.startsWith("#")) {
                    String[] itemArray = item.split("!");
                    //添加黑名单
                    blackList.add(itemArray[0]);
                    for (int index = 1; index < itemArray.length; index++) {
                        //添加白名单
                        whiteList.add(itemArray[index]);
                    }
                }
            } while (item != null);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        LogDog.d("Load filter ingress configuration , Number of blacklists = " + blackList.size() + " Number of whitelists = " + whiteList.size());
    }

    public boolean isIntercept(String host) {
        if (StringEnvoy.isNotEmpty(host)) {
            for (String tmp : blackList) {
                if (host.contains(tmp)) {
                    for (String white : whiteList) {
                        if (host.contains(white)) {
                            return false;
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }
}
