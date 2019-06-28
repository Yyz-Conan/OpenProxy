package test;

import connect.network.nio.NioClientTask;
import connect.network.nio.NioReceive;
import connect.network.nio.NioSender;
import util.LogDog;

public class HttpClient extends NioClientTask {

    public HttpClient(String host, int port) {
        super(host, port);
        NioSender sender = new NioSender();
        setSender(sender);
        NioReceive receive = new NioReceive(this, "onReceive");
        setReceive(receive);
    }

    @Override
    protected void onConnectSocketChannel(boolean isConnect) {
        LogDog.v("==##> HttpClient onConnectSocketChannel isConnect = " + isConnect);
        String data = "CONNECT offlintab.firefoxchina.cn:443 HTTP/1.1\n" +
                "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:67.0) Gecko/20100101 Firefox/67.0\n" +
                "Proxy-Connection: keep-alive\n" +
                "Connection: keep-alive\n" +
                "Host: offlintab.firefoxchina.cn:443\n" +
                "\n";
        getSender().sendData(data.getBytes());
    }

    private void onReceive(byte[] data) {
        LogDog.v("==##> HttpClient onReceive data = " + new String(data));
    }
}
