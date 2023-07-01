package com.open.proxy.intercept;


import com.jav.common.log.LogDog;
import com.jav.common.util.StringEnvoy;
import com.open.proxy.intercept.joggle.IInterceptFilter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class BuiltInInterceptFilter implements IInterceptFilter {
    private List<String> mBlackList;
    private List<String> mWhiteList;
    private final String pattern = ".+(?=)";

    public BuiltInInterceptFilter() {
        mBlackList = new ArrayList<>();
        mWhiteList = new ArrayList<>();
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
                    mBlackList.add(itemArray[0]);
                    for (int index = 1; index < itemArray.length; index++) {
                        //添加白名单
                        mWhiteList.add(pattern + itemArray[index]);
                    }
                }
            } while (item != null);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        LogDog.d("Load filter ingress configuration , Number of blacklists = " + mBlackList.size() + " Number of whitelists = " + mWhiteList.size());
    }

    public boolean isIntercept(String host) {
        if (StringEnvoy.isNotEmpty(host)) {
            for (String backRule : mBlackList) {
                if (host.contains(backRule)) {
                    LogDog.d("match back rule = " + backRule);
                    for (String whiteRule : mWhiteList) {
                        if (Pattern.matches(whiteRule, host)) {
                            LogDog.d("match white rule = " + whiteRule);
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
