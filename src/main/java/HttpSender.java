import connect.network.nio.NioClientFactory;
import connect.network.nio.NioClientTask;
import connect.network.nio.NioSender;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public class HttpSender extends NioSender {

    private long createTime = System.currentTimeMillis();
    private final int MAX_TIME = 8000;
    private NioClientTask nioClientTask;


    public HttpSender(NioClientTask task) {
        this.nioClientTask = task;
    }

    @Override
    protected boolean onWrite(SocketChannel channel) throws IOException {
        if (cache.size() == 0) {
            if (System.currentTimeMillis() - createTime > MAX_TIME) {
                NioClientFactory.getFactory().removeTask(nioClientTask);
                return false;
            }
        }
        return super.onWrite(channel);
    }
}
