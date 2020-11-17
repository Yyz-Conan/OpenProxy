package utils;

public class DataPacketManger {

    public static final byte[] PACK_PROXY_TAG = new byte[]{'\n', 'P', 'X', '\n'};

    public static final byte[] PACK_UPDATE_TAG = new byte[]{'\n', 'U', 'D', '\n'};

//    /**
//     * 创建代理协议头
//     *
//     * @param proxyDataLength
//     * @return
//     */
//    public static byte[] packetProxy(int proxyDataLength) {
//        if (proxyDataLength == 0) {
//            return null;
//        }
//        byte[] packet = new byte[8];
//        byte[] length = TypeConversion.intToByte(proxyDataLength);
//        System.arraycopy(PACK_PROXY_TAG, 0, packet, 0, PACK_PROXY_TAG.length);
//        System.arraycopy(length, 0, packet, 4, length.length);
//        return packet;
//    }

//    /**
//     * 请求版本更新
//     *
//     * @return
//     */
//    public static byte[] packetCheckUpdate(int version) {
//        byte[] packet = new byte[8];
//        byte[] versionByte = TypeConversion.intToByte(version);
//        System.arraycopy(PACK_PROXY_TAG, 0, packet, 0, PACK_PROXY_TAG.length);
//        System.arraycopy(versionByte, 0, packet, 4, versionByte.length);
//        return packet;
//    }

//    /**
//     * 响应版本更新请求
//     *
//     * @return
//     */
//    public static byte[] packetUpdate(boolean isHasNewVersion) {
//        byte[] packet = new byte[8];
//        byte[] countFileByte = TypeConversion.intToByte(isHasNewVersion ? 1 : 0);
//        System.arraycopy(PACK_UPDATE_TAG, 0, packet, 0, PACK_UPDATE_TAG.length);
//        System.arraycopy(countFileByte, 0, packet, 4, countFileByte.length);
//        return packet;
//    }
}
