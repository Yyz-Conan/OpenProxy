package connect;

import connect.network.nio.NioSender;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

public class CacheNioSender extends NioSender {

    private LinkedList<byte[]> list = new LinkedList();

    @Override
    public void setChannel(SocketChannel channel) {
        synchronized (list) {
            super.setChannel(channel);
            sendCacheData();
        }
    }

    private void sendCacheData() {
        if (!list.isEmpty()) {
            for (byte[] data : list) {
                try {
                    super.sendDataImp(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            list.clear();
        }
    }

    @Override
    protected void sendDataImp(byte[] data) throws IOException {
        if (data != null) {
            synchronized (list) {
                if (channel == null) {
                    list.add(data);
                } else {
                    super.sendDataImp(data);
                }
            }
        }
    }
}
