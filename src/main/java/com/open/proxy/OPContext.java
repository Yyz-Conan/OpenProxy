package com.open.proxy;

import util.ConfigFileEnvoy;

public class OPContext {

    private static class InnerClass {
        private final static OPContext sContext = new OPContext();
    }

    public static OPContext getInstance() {
        return InnerClass.sContext;
    }

    private OPContext() {
        mCFileEnvoy = new ConfigFileEnvoy();
    }

    private ConfigFileEnvoy mCFileEnvoy;

    public ConfigFileEnvoy getConfigFileEnvoy() {
        return mCFileEnvoy;
    }
}
