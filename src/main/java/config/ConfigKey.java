package config;

public class ConfigKey {

    private ConfigKey() {
    }

    public static final String KEY_USER_DIR = "user.dir";
    public static final String KEY_COMMAND = "sun.java.command";
    public static final String KEY_DEBUG_MODE = "debug_mode";

    public static final String FILE_PUBLIC_KEY = "public.key";
    public static final String FILE_PRIVATE_KEY = "private.key";
    public static final String FILE_CONFIG = "config.cfg";
    public static final String FILE_INTERCEPT = "interceptFile";
    public static final String FILE_PROXY = "proxyFile";
    public static final String FILE_BG_IMAGE = "bg.jpg";

    //只在服务模式下使用
    public static final String CONFIG_LOCAL_HOST = "localHost";
    public static final String CONFIG_PROXY_LOCAL_PORT = "proxyLocalPort";
    public static final String CONFIG_UPDATE_LOCAL_PORT = "updateLocalPort";
    public static final String CONFIG_SOCKS5_LOCAL_PORT = "socks5LocalPort";


    public static final String CONFIG_ENCRYPTION_MODE = "encryptionMode";
    public static final String CONFIG_INTERCEPT = "intercept";
    public static final String CONFIG_IS_SERVER_MODE = "isServerMode";
    public static final String CONFIG_ALLOW_PROXY = "allowProxy";
    public static final String CONFIG_IMAGE = "image";

    //只在客户模式下使用
    public static final String CONFIG_ENABLE_PROXY = "enableProxy";
    public static final String CONFIG_ENABLE_SOCKS5_PROXY = "enableSocks5Proxy";
    public static final String CONFIG_REMOTE_PROXY_HOST = "remoteProxyHost";
    public static final String CONFIG_REMOTE_UPDATE_HOST = "remoteUpdateHost";
    public static final String CONFIG_REMOTE_PROXY_PORT = "remoteProxyPort";
    public static final String CONFIG_REMOTE_UPDATE_PORT = "remoteUpdatePort";
    public static final String CONFIG_REMOTE_SOCKS5_PROXY_HOST = "remoteSocks5ProxyHost";
    public static final String CONFIG_REMOTE_SOCKS5_PROXY_PORT = "remoteSocks5ProxyPort";

    //更新服务使用
    public static final String CONFIG_NEW_VERSION = "newVersion";
    public static final String CONFIG_UPDATE_FILE_PATH = "updateFilePath";

    //应用当前版本号
    public static final int currentVersion = 2;
}
