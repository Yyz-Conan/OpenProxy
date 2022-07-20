package com.open.proxy.utils;


import com.jav.net.entity.RequestMode;

public class RequestHelper {

    private RequestHelper() {
    }

    /**
     * 判断数据是否是http协议head数据
     * @param data
     * @return
     */
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
