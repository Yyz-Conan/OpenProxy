package com.open.proxy;

/**
 * 配置key
 *
 * @author yyz
 */
public interface IConfigKey {

    /**
     * 配置文件名
     */
    String FILE_CONFIG = "config.cfg";

    //---------------------------开关配置-------------------------------------------------

    String KEY_DEBUG_MODE = "debugMode";

    String CONFIG_ALLOW_PROXY = "allowProxy";
    String CONFIG_ENABLE_PROXY = "enableProxy";
    String CONFIG_INTERCEPT = "enableIntercept";
    String CONFIG_IS_SERVER_MODE = "isServerMode";
    String CONFIG_ENCRYPTION_MODE = "encryptionMode";
    String CONFIG_ENABLE_SOCKS5_PROXY = "enableSocks5Proxy";

    //----------------------------文件配置------------------------------------------------


    String FILE_PROXY = "proxyFile";
    String FILE_MACHINE = "machineFile";
    String FILE_INTERCEPT = "interceptFile";
    String FILE_PUBLIC_KEY = "rsaPublicFile";
    String FILE_PRIVATE_KEY = "rsaPrivateFile";
    String FILE_IP_BLACK_PATH = "ipBlackPath";

    //----------------------------服务配置------------------------------------------------

    /**
     * 配置服务
     */
    String CONFIG_SYNC_SERVER_PORT = "syncServerPort";
    String CONFIG_SYNC_SERVER_FILE = "syncServerFile";
    String CONFIG_TRANS_SERVER_HOST = "transServerHost";
    String CONFIG_TRANS_SERVER_PORT = "transServerPort";
    String CONFIG_UPDATE_SERVER_PORT = "updateServerPort";
    String CONFIG_SOCKS5_SERVER_PORT = "socks5ServerPort";

    /**
     * 开关配置
     */
    String CONFIG_ENABLE_IP_BLACK = "enableIPBlack";
    /**
     * 更新服务使用
     */
    String CONFIG_NEW_VERSION = "newVersion";
    String CONFIG_UPDATE_FILE_PATH = "updateFilePath";


    //----------------------------连接配置------------------------------------------------


    /**
     * 只在客户模式下使用
     */
    String CONFIG_CHANNEL_NUMBER = "channelNumber";
    String CONFIG_REMOTE_PROXY_HOST = "remoteProxyHost";
    String CONFIG_REMOTE_PROXY_PORT = "remoteProxyPort";
    String CONFIG_REMOTE_UPDATE_HOST = "remoteUpdateHost";
    String CONFIG_REMOTE_UPDATE_PORT = "remoteUpdatePort";
    String CONFIG_REMOTE_SOCKS5_PROXY_HOST = "remoteSocks5ProxyHost";
    String CONFIG_REMOTE_SOCKS5_PROXY_PORT = "remoteSocks5ProxyPort";


    //----------------------------信息配置------------------------------------------------


    /**
     * 本机机器id
     */
    String CONFIG_MACHINE_ID = "machineId";

    /**
     * 应用当前版本号
     */
    int currentVersion = 2;
}
