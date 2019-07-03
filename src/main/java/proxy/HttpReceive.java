package proxy;

import connect.network.nio.NioClientFactory;
import connect.network.nio.NioClientTask;
import connect.network.nio.NioReceive;
import util.IoEnvoy;
import util.ThreadAnnotation;

import java.nio.channels.SocketChannel;

public class HttpReceive extends NioReceive {
    private final int MAX_TIME = 3000;
    private long updateTime;
    private NioClientTask nioClientTask;

    public HttpReceive(NioClientTask task, String receiveMethodName) {
        super(task, receiveMethodName);
        updateTime = System.currentTimeMillis();
        this.nioClientTask = task;
    }

    @Override
    protected void onRead(SocketChannel channel) {
        byte[] data = null;
        try {
            data = IoEnvoy.tryRead(channel);
        } catch (Exception e) {
            NioClientFactory.getFactory().removeTask(nioClientTask);
        }
        if (data != null) {
            ThreadAnnotation.disposeMessage(this.mReceiveMethodName, this.mReceive, new Object[]{data});
            updateTime = System.currentTimeMillis();
        } else {
            if (System.currentTimeMillis() - updateTime > MAX_TIME) {
                NioClientFactory.getFactory().removeTask(nioClientTask);
            }
        }
    }
}
