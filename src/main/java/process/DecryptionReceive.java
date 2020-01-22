package process;

import connect.RequestReceive;
import connect.network.nio.NioClientTask;
import util.IoEnvoy;
import util.ReflectionCall;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public abstract class DecryptionReceive extends RequestReceive {

    public DecryptionReceive(NioClientTask task, String receiveMethodName) {
        super(task, receiveMethodName);
    }

    @Override
    protected void onRead(SocketChannel channel) throws IOException {
        byte[] data = IoEnvoy.tryRead(channel);
        if (data != null) {
            data = onDecrypt(data);
            ReflectionCall.invoke(mReceive, mReceiveMethod, new Class[]{byte[].class}, data);
        }
    }

    protected abstract byte[] onDecrypt(byte[] src);
}
