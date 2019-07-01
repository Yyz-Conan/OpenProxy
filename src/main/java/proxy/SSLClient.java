package proxy;

import connect.network.base.joggle.ISender;
import connect.network.tcp.TcpClientTask;
import connect.network.tcp.TcpReceive;
import connect.network.tcp.TcpSender;
import util.LogDog;

import java.net.Socket;

public class SSLClient extends TcpClientTask {
    private ISender nioClientSender;

    public SSLClient(Socket socket, ISender sender) {
        super(socket);
        setSender(new TcpSender());
        setReceive(new TcpReceive(this, "onReceiveSSL"));
        setConnectTimeout(100);
        this.nioClientSender = sender;
    }

    @Override
    protected void onConnectSocket(boolean isConnect) {
        LogDog.v("==##> SSLClient onConnectSocket isConnect = " + isConnect);
        if (isConnect) {
            String data = "GET https://wwww.baidu.com HTTP/1.1\n" +//CONNECT
                    "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:67.0) Gecko/20100101 Firefox/67.0\n" +
                    "Proxy-Connection: keep-alive\n" +
                    "Connection: keep-alive\n" +
                    "Host: wwww.baidu.com\n" +
                    "\n";
            getSender().sendData(data.getBytes());
        }
    }

    private void onReceiveSSL(byte[] data) {
        LogDog.v("==##> SSLClient onReceiveSSL data = " + new String(data));
        nioClientSender.sendData(data);
    }
}
