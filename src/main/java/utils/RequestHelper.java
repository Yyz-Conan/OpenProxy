package utils;

import connect.network.base.RequestMode;

public class RequestHelper {

    private RequestHelper() {
    }

    public static boolean isRequest(byte[] data) {
        if (data == null || data.length < 7) {
            return false;
        }
        String method = new String(data, 0, 7);
        for (RequestMode mode : RequestMode.values()) {
            if (method.contains(mode.getMode())) {
                return true;
            }
        }
        return false;
    }
}
