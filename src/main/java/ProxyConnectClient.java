import connect.network.nio.NioClientFactory;
import connect.network.nio.NioClientTask;
import connect.network.nio.NioReceive;
import connect.network.nio.NioSender;
import util.LogDog;

import java.net.InetAddress;

public class ProxyConnectClient extends NioClientTask {

    private NioSender target;

    public ProxyConnectClient(byte[] data, NioSender target) {
        if (data == null || target == null) {
            throw new NullPointerException("data or target is null !!!");
        }
        this.target = target;
        String proxyData = new String(data);

        String[] args = proxyData.split("\r\n");
        if (args == null || args.length <= 1) {
            return;
        }
        String[] tmp = args[1].split(":");
        if (tmp == null || tmp.length == 0) {
            return;
        }
        LogDog.d("==> ProxyConnectClient request address = " + args[1]);
        String host = tmp[1].trim();
        int port = tmp.length == 2 ? 80 : Integer.parseInt(tmp[2]);
        try {
            InetAddress address = InetAddress.getByName(host);
            host = address.getHostAddress();
        } catch (Exception e) {
            e.printStackTrace();
        }
        setAddress(host, port);

        NioSender sender = new HttpSender(this);
        sender.sendData(data);
        setSender(sender);
        setReceive(new NioReceive(this, "onHttpSubmitCallBack"));
    }

    private void onHttpSubmitCallBack(byte[] data) {
        if (data.length == 0) {
            NioClientFactory.getFactory().removeTask(this);
            return;
        }
        String html = new String(data);
        LogDog.d("==> onHttpSubmitCallBack = " + html.substring(0, 20));
        target.sendData(data);
    }
}
