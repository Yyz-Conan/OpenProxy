package com.open.proxy.server.sync;

import com.jav.net.security.channel.base.UnusualBehaviorType;
import com.jav.net.security.channel.joggle.ISecurityPolicyProcessor;
import com.jav.net.security.guard.IpBlackListManager;

import java.util.List;

public class SecuritySyncPolicyProcessor implements ISecurityPolicyProcessor {

    /**
     * 172800000 48小时的毫秒值
     */
    private static final int TWO_DAY = 1000 * 60 * 60 * 24 * 2;

    private SecuritySyncContext mContext;

    public SecuritySyncPolicyProcessor(SecuritySyncContext context) {
        mContext = context;
    }

    @Override
    public boolean onCheckTime(long time) {
        long nowTime = System.currentTimeMillis();
        // 172800000 48小时的毫秒值
        return Math.abs(nowTime - time) < TWO_DAY;
    }

    @Override
    public boolean onCheckMachineId(String checkMid) {
        if (checkMid.equalsIgnoreCase(mContext.getMachineId())) {
            //sync 通讯使用的是服务的mid
            return true;
        }
        // 检查mid是否合法
        List<String> machineList = mContext.getMachineList();
        if (machineList == null || machineList.isEmpty()) {
            // mid列表为空默认放行所有
            return true;
        }
        for (String mid : machineList) {
            if (mid.equalsIgnoreCase(checkMid)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onCheckChannelId(String channelId) {
        return true;
    }

    @Override
    public void onUnusualBehavior(String host, UnusualBehaviorType unusualBehaviorType) {
        IpBlackListManager.getInstance().addBlackList(host);
    }
}
