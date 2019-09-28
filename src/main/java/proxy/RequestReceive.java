package proxy;

import connect.network.nio.NioClientTask;
import connect.network.nio.NioHPCClientFactory;
import connect.network.nio.NioReceive;
import util.IoEnvoy;
import util.ThreadAnnotation;

import java.nio.channels.SocketChannel;

public class RequestReceive extends NioReceive {
    private NioClientTask nioClientTask;

    public RequestReceive(NioClientTask task, String receiveMethodName) {
        super(task, receiveMethodName);
        this.nioClientTask = task;
    }

    @Override
    protected void onRead(SocketChannel channel) {
        try {
            byte[] data = IoEnvoy.tryRead(channel);
            if (data != null) {
                ThreadAnnotation.disposeMessage(this.mReceiveMethodName, this.mReceive, new Object[]{data});
            } else {
                NioHPCClientFactory.getFactory().removeTask(nioClientTask);
            }
        } catch (Exception e) {
            NioHPCClientFactory.getFactory().removeTask(nioClientTask);
        }
    }
}
