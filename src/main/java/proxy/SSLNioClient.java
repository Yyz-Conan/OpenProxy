package proxy;

import connect.network.base.joggle.ISender;
import connect.network.nio.NioClientTask;
import connect.network.nio.NioReceive;
import connect.network.nio.NioSender;
import util.LogDog;

import java.nio.channels.SocketChannel;


public class SSLNioClient extends NioClientTask {
    private ISender localSender;

    public SSLNioClient(SocketChannel remoteChannel, ISender localSender) {
        super(remoteChannel);
        if (localSender == null || remoteChannel == null) {
            throw new NullPointerException("remoteChannel and localSender is can not be null !!!");
        }
        this.localSender = localSender;
        setSender(new NioSender());
        setReceive(new NioReceive(this, "onReceiveSSLNio"));
    }

    private void onReceiveSSLNio(byte[] data) {
        String html = new String(data);
        LogDog.v("==##> SSLNioClient onReceiveSSLNio data = " + html);
        localSender.sendData(data);
    }
}
