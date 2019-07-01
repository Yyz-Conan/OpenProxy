import connect.network.nio.NioServerFactory;
import proxy.HttpProxyServer;
import util.LogDog;
import util.NetUtils;

public class ProxyMain {


    // 183.2.236.16  百度 = 14.215.177.38  czh = 58.67.203.13
    public static void main(String[] args) throws Exception {
        HttpProxyServer httpProxyServer = new HttpProxyServer();
        String host = NetUtils.getLocalIp("eth2");
        LogDog.d("==> proxy.HttpProxyServer host = " + host);
        httpProxyServer.setAddress(host, 7777);
        NioServerFactory.getFactory().open();
        NioServerFactory.getFactory().addTask(httpProxyServer);


//        HttpClient httpClient = new HttpClient("www.baidu.com", 443);
//        String keyPath = HttpProxyServer.class.getClassLoader().getResource("cacerts").getPath();
//        String password = "changeit";
//        NioSSLFactory sslFactory = new NioSSLFactory("TLS", "SunX509", "JKS", keyPath, password);
//        NioClientFactory.getFactory().setSSlFactory(sslFactory);
//        NioClientFactory.getFactory().open();
//        NioClientFactory.getFactory().addTask(httpClient);


//        String host = null;
//        int port = -1;
//        String path = null;
////        for (int i = 0; i < args.length; i++)
////            System.out.println(args[i]);
//
////        if (args.length < 3) {
////            System.out.println("USAGE: java SSLSocketClientWithClientAuth host port requestedfilepath");
////            System.exit(-1);
////        }
//
//        try {
////            host = args[0];
////            port = Integer.parseInt(args[1]);
////            path = args[2];
//
////            host = "github.com";
////            host = "blog.csdn.net";
//            host = "www.baidu.com";
//            port = 443;
//            path = "/";
//        } catch (IllegalArgumentException e) {
//            System.out.println("USAGE: java SSLSocketClientWithClientAuth host port requestedfilepath");
//            System.exit(-1);
//        }
//
//        try {
//
//            /*
//             * Set up a key manager for client authentication
//             * if asked by the server.  Use the implementation's
//             * default TrustStore and secureRandom routines.
//             */
//            SSLSocketFactory factory = null;
//            try {
//                SSLContext ctx;
//                KeyManagerFactory kmf;
//                KeyStore ks;
//                char[] passphrase = "changeit".toCharArray();
//
//                ctx = SSLContext.getInstance("TLS");
//                kmf = KeyManagerFactory.getInstance("SunX509");
//                ks = KeyStore.getInstance("JKS");
//
//                String keyPath = HttpProxyServer.class.getClassLoader().getResource("cacerts").getPath();
//                ks.load(new FileInputStream(keyPath), passphrase);
//
//                kmf.init(ks, passphrase);
//                ctx.init(kmf.getKeyManagers(), null, null);
//
//                factory = ctx.getSocketFactory();
//            } catch (Exception e) {
//                throw new IOException(e.getMessage());
//            }
//
//            SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
//            socket.setUseClientMode(true);
//            SocketChannel channel = socket.getChannel();
//
//            /*
//             * send http request
//             *
//             * See SSLSocketClient.java for more information about why
//             * there is a forced handshake here when using PrintWriters.
//             */
//            System.out.println("start https request:" + host + " " + port + " " + path);
//            socket.startHandshake();
//
//            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
//            out.println("GET " + "https://wwww.baidu.com" + " HTTP/1.1");
//            out.println("Host: " + host);
////            out.println("Proxy-Connection: Keep-Alive");
////            out.println("Proxy-Authorization: Basic *");
//            out.println();
//            out.flush();
//
//            /*
//             * Make sure there were no surprises
//             */
//            if (out.checkError())
//                System.out.println(
//                        "SSLSocketClient: java.io.PrintWriter error");
//
//            /* read response */
//            BufferedReader in = new BufferedReader(
//                    new InputStreamReader(
//                            socket.getInputStream()));
//
//            String inputLine;
//
//            //最好没有空行会阻塞在这里
//            while ((inputLine = in.readLine()) != null)
//                System.out.println(inputLine);
//
//            in.close();
//            out.close();
//            socket.close();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }
}
