package protocol;

public class DataPacketTag {

    /**
     * proxy数据的 head tag
     */
    public static final byte[] PACK_PROXY_TAG = new byte[]{'\n', 'P', 'X', '\n'};

    /**
     * update数据的 head tag
     */
    public static final byte[] PACK_UPDATE_TAG = new byte[]{'\n', 'U', 'D', '\n'};


    /**
     * socks5数据的 head tag,用于中转前代理目标链接ip和端口
     */
    public static final byte[] PACK_SOCKS5_HELLO_TAG = new byte[]{'\n', 'S', 'K', '\n'};


    /**
     * socks5数据的 head tag,用于中转数据
     */
    public static final byte[] PACK_SOCKS5_DATA_TAG = new byte[]{'\n', 'S', '5', '\n'};

}
