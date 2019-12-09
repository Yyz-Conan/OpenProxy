package process;

import connect.RequestReceive;
import connect.network.nio.NioClientTask;
import connect.network.nio.NioHPCClientFactory;
import log.LogDog;
import util.IoEnvoy;
import util.ReflectionCall;

public abstract class DecryptionReceive extends RequestReceive {

    public DecryptionReceive(NioClientTask task, String receiveMethodName) {
        super(task, receiveMethodName);
    }

    @Override
    protected void onRead() {
        if (channel.isConnected() && channel.isOpen()) {
            try {
                byte[] data = IoEnvoy.tryRead(channel);
                if (data != null) {
                    tryCount = 3;
                    data = onDecrypt(data);
                    ReflectionCall.invoke(mReceive, mReceiveMethod, new Class[]{byte[].class}, data);
                } else {
//                    LogDog.e(" ==> 收到空的数据 !!!");
                    if (tryCount == 0) {
                        NioHPCClientFactory.getFactory().removeTask(nioClientTask);
                    }
                    tryCount--;
                }
            } catch (Exception e) {
                LogDog.e(" ==> 接受数据异常 !!! " + e.getMessage() + " host = " + nioClientTask.getHost() + " obj = " + nioClientTask.toString());
                NioHPCClientFactory.getFactory().removeTask(nioClientTask);
            }
        }
    }

    protected abstract byte[] onDecrypt(byte[] src);
}
