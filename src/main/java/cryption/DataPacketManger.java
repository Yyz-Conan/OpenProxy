package cryption;

import java.util.ArrayList;
import java.util.List;

public class DataPacketManger {

    private static final byte[] PACK_TAG = new byte[]{'\n', 'o', '1', '\n'};

    public static byte[] packet(byte[] src) {
        if (src == null || src.length == 0) {
            return null;
        }
        byte[] packet = new byte[4 + src.length];
        System.arraycopy(src, 0, packet, 0, src.length);
        System.arraycopy(PACK_TAG, 0, packet, src.length, 4);
        return packet;
    }

    public static byte[][] unpack(byte[] src, int length) {
        if (length > 5 && PACK_TAG[3] == src[length - 1] && PACK_TAG[2] == src[length - 2]
                && PACK_TAG[1] == src[length - 3] && PACK_TAG[0] == src[length - 4]) {
            int off = 0;
            List<Integer> list = new ArrayList<>();
            do {
                int index = findTag(src, off, length);
                if (index > 0) {
                    //找到结束标志
                    list.add(index);
                    if (src.length <= index + 4) {
                        break;
                    }
                    off = index + 1;
                } else {
                    break;
                }
            } while (true);
            byte[][] result = null;
            if (!list.isEmpty()) {
                off = 0;
                result = new byte[list.size()][];
                for (int index = 0; index < list.size(); index++) {
                    byte[] unpack = new byte[list.get(index) - off];
                    System.arraycopy(src, off, unpack, 0, unpack.length);
                    result[index] = unpack;
                    off += unpack.length + 4;
                }
            }
            return result;
        }
        return null;
    }

    private static int findTag(byte[] data, int off, int length) {
        for (int index = off; index < length; index++) {
            if (data[index] == PACK_TAG[0]) {
                if (data[index + 1] == PACK_TAG[1] && data[index + 2] == PACK_TAG[2] && data[index + 3] == PACK_TAG[3]) {
                    return index;
                }
            }
        }
        return -1;
    }
}
