package com.open.proxy;

public interface IConfigKey {


    String KEY_DEBUG_MODE = "debugMode";

    String FILE_PUBLIC_KEY = "public.key";
    String FILE_PRIVATE_KEY = "private.key";
    String FILE_CONFIG = "config.cfg";
    String FILE_INTERCEPT = "interceptFile";
    String FILE_PROXY = "proxyFile";
    String FILE_BG_IMAGE = "bg.jpg";

    //只在服务模式下使用
    String CONFIG_LOCAL_HOST = "localHost";
    String CONFIG_PROXY_LOCAL_PORT = "proxyLocalPort";
    String CONFIG_UPDATE_LOCAL_PORT = "updateLocalPort";
    String CONFIG_SOCKS5_LOCAL_PORT = "socks5LocalPort";


    String CONFIG_ENCRYPTION_MODE = "encryptionMode";
    String CONFIG_INTERCEPT = "intercept";
    String CONFIG_IS_SERVER_MODE = "isServerMode";
    String CONFIG_ALLOW_PROXY = "allowProxy";
    String CONFIG_IMAGE = "image";

    //只在客户模式下使用
    String CONFIG_ENABLE_IP_BLACK = "enableIPBlack";
    String CONFIG_ENABLE_PROXY = "enableProxy";
    String CONFIG_ENABLE_SOCKS5_PROXY = "enableSocks5Proxy";
    String CONFIG_REMOTE_PROXY_HOST = "remoteProxyHost";
    String CONFIG_REMOTE_UPDATE_HOST = "remoteUpdateHost";
    String CONFIG_REMOTE_PROXY_PORT = "remoteProxyPort";
    String CONFIG_REMOTE_UPDATE_PORT = "remoteUpdatePort";
    String CONFIG_REMOTE_SOCKS5_PROXY_HOST = "remoteSocks5ProxyHost";
    String CONFIG_REMOTE_SOCKS5_PROXY_PORT = "remoteSocks5ProxyPort";

    //更新服务使用
    String CONFIG_NEW_VERSION = "newVersion";
    String CONFIG_UPDATE_FILE_PATH = "updateFilePath";

    //应用当前版本号
    int currentVersion = 2;
}
