package com.open.proxy.connect;

import com.jav.common.util.ConfigFileEnvoy;
import com.jav.net.nio.NioSender;
import com.open.proxy.IConfigKey;
import com.open.proxy.OPContext;
import com.open.proxy.protocol.security.InitProtocol;

public class SecuritySender extends NioSender {

    private enum Process {
        INIT, DATA, SYNC
    }

    private InitProtocol mInitProtocol;

    public SecuritySender() {
        ConfigFileEnvoy cFileEnvoy = OPContext.getInstance().getConfigFileEnvoy();
        boolean isServerMode = cFileEnvoy.getBooleanValue(IConfigKey.CONFIG_IS_SERVER_MODE);
        String machineId = cFileEnvoy.getValue(IConfigKey.CONFIG_MACHINE_ID);
        String desPassword = OPContext.getInstance().getDesPassword();
        mInitProtocol = new InitProtocol(machineId, desPassword.getBytes(), isServerMode);
    }

}
