package connect;

import connect.network.nio.NioClientTask;
import connect.network.nio.NioReceive;
import util.IoEnvoy;
import util.ReflectionCall;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public class RequestReceive extends NioReceive {


    public RequestReceive(NioClientTask task, String receiveMethodName) {
        super(task, receiveMethodName);
    }

    @Override
    protected void onRead(SocketChannel channel) throws IOException {
        byte[] data = IoEnvoy.tryRead(channel);
        if (data != null) {
            ReflectionCall.invoke(mReceive, mReceiveMethod, new Class[]{byte[].class}, data);
        }
    }

}
