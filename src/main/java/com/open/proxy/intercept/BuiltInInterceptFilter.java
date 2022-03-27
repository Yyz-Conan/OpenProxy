package com.open.proxy.intercept;

import com.open.proxy.intercept.joggle.IInterceptFilter;
import log.LogDog;
import util.StringEnvoy;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class BuiltInInterceptFilter implements IInterceptFilter {
    private List<String> blackList;
    private List<String> whiteList;
    private final String pattern = ".+(?=)";

    public BuiltInInterceptFilter() {
        blackList = new ArrayList<>();
        whiteList = new ArrayList<>();
    }

    public void init(String ipTablePath) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(ipTablePath));
            String item;
            do {
                item = reader.readLine();
                if (StringEnvoy.isNotEmpty(item) && !item.startsWith("//") && !item.startsWith("##") && !item.startsWith("#")) {
                    String[] itemArray = item.split("!");
                    //添加黑名单
                    blackList.add(pattern + itemArray[0]);
                    for (int index = 1; index < itemArray.length; index++) {
                        //添加白名单
                        whiteList.add(pattern + itemArray[index]);
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
            for (String backRule : blackList) {
                if (Pattern.matches(backRule, host)) {
                    for (String whiteRule : whiteList) {
                        if (Pattern.matches(whiteRule, host)) {
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
