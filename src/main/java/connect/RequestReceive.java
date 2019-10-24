package connect;

import connect.network.nio.NioClientTask;
import connect.network.nio.NioHPCClientFactory;
import connect.network.nio.NioReceive;
import log.LogDog;
import sun.nio.ch.IOStatus;
import sun.nio.ch.IOUtil;
import util.IoEnvoy;
import util.ThreadAnnotation;

import java.io.ByteArrayOutputStream;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class RequestReceive extends NioReceive {

    protected NioClientTask nioClientTask;

    private int tryCount = 3;

    public RequestReceive(NioClientTask task, String receiveMethodName) {
        super(task, receiveMethodName);
        this.nioClientTask = task;
    }

    @Override
    protected void onRead() {
        if (channel.isConnected() && channel.isOpen()) {
            try {
                byte[] data = IoEnvoy.tryRead(channel);
                if (data != null) {
                    tryCount = 3;
                    ThreadAnnotation.disposeMessage(mReceiveMethodName, mReceive, data);
                } else {
//                    LogDog.e(" ==> 收到空的数据 !!!");
                    if (tryCount == 0) {
                        NioHPCClientFactory.getFactory().removeTask(nioClientTask);
                    }
                    tryCount--;
                }
            } catch (Exception e) {
                LogDog.e(" ==> 接受数据异常 !!!" + e.getMessage() + " host = " + nioClientTask.getHost() + " obj = " + nioClientTask.toString());
                NioHPCClientFactory.getFactory().removeTask(nioClientTask);
            }
        }
    }

}
