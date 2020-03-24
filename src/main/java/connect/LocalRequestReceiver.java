package connect;

import connect.network.base.joggle.INetReceiver;
import connect.network.base.joggle.INetSender;
import connect.network.xhttp.XHttpReceiver;
import util.IoEnvoy;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public class LocalRequestReceiver extends XHttpReceiver {

    private INetSender remoteSender;
    private boolean isTLS = false;
    private boolean isFirst = true;

    public LocalRequestReceiver(INetReceiver receiver) {
        super(receiver);
    }

    public void setRemoteSender(INetSender remoteSender) {
        this.remoteSender = remoteSender;
    }

    public void setTLS(boolean TLS) {
        isTLS = TLS;
    }

    @Override
    protected void onRead(SocketChannel channel) throws Exception {
        Exception exception = null;
        try {
            if (isFirst || !isTLS) {
                readHttpFullData(channel);
            } else {
                readFullData(channel);
            }
        } catch (Exception var7) {
//            var7.printStackTrace();
            exception = var7;
            throw var7;
        } finally {
            if (isFirst || !isTLS) {
                notifyReceiver(response, exception);
                response.reset();
                isFirst = false;
            }
        }
    }

    private void readFullData(SocketChannel channel) throws IOException {
        byte[] data = IoEnvoy.tryRead(channel);
        remoteSender.sendData(data);
//        LogDog.d("==> readTlsFullData sendData  = " + new String(data) + this);
    }
}
