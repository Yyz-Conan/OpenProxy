import connect.network.nio.NioServerFactory;
import sun.security.ssl.SSLSocketImpl;
import util.IoUtils;
import util.LogDog;
import util.NetUtils;

import javax.net.ssl.*;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

public class Proxy {

    private static TrustManager trm = new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }

        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }
    };

    // 183.2.236.16  百度 = 14.215.177.38  czh = 58.67.203.13
    public static void main(String[] args) {

        try {
//            SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket("183.2.236.16", 443);
//            socket.setUseClientMode(true);
//            socket.setEnableSessionCreation(false);
//            socket.setNeedClientAuth(false);
//            socket.setWantClientAuth(false);
////            socket.startHandshake();
//            socket.setSoTimeout(1000);
////            socket.getOutputStream().write("hello".getBytes());
//            byte[] tmp = IoUtils.tryRead(socket.getInputStream());
//            if (tmp != null) {
//                LogDog.d("==> " + new String(tmp));
//            }

//            SSLContext sc = SSLContext.getInstance("SSL");
//            sc.init(null, new TrustManager[]{trm}, null);
//            SSLSocketFactory factory = sc.getSocketFactory();
//            SSLSocket socket = (SSLSocket) factory.createSocket("183.2.236.16", 443);
//            socket.setUseClientMode(true);
//            socket.setSoTimeout(3000);
//            socket.startHandshake();
//            SSLSession session = socket.getSession();
//            Certificate[] serverCertificates = session.getPeerCertificates();
//            for (int i = 0; i < serverCertificates.length; i++) {
//                System.out.print("-----BEGIN CERTIFICATE-----\n");
//                System.out.print(new sun.misc.BASE64Encoder().encode(serverCertificates[i].getEncoded()));
//                System.out.print("\n-----END CERTIFICATE-----\n");
//            }
//            socket.getOutputStream().write("hello".getBytes());
//            byte[] czhTmp = IoUtils.tryRead(socket.getInputStream());
//            if (czhTmp != null) {
//                LogDog.d("==> " + new String(czhTmp));
//            }
//            socket.close();


            InputStream inputStream = Proxy.class.getClassLoader().getResourceAsStream("ssl_ks");
            TestSSLFactory sslFactory = new TestSSLFactory("SSL", inputStream);//TLS
            SSLSocketFactory sslSocketFactory = sslFactory.getSSLSocketFactory();
//            SSLSocketImpl sslSocketImpl = (SSLSocketImpl) sslSocketFactory.createSocket(socket, "14.215.177.38", 443, true);
//            SSLSocketImpl sslSocketImpl = (SSLSocketImpl) SSLSocketFactory.getDefault().createSocket("14.215.177.38", 443);
            SSLSocketImpl sslSocketImpl = (SSLSocketImpl) sslSocketFactory.createSocket("61.140.13.228", 443);
            sslSocketImpl.setSoTimeout(3000);
            sslSocketImpl.setUseClientMode(true);
            sslSocketImpl.addHandshakeCompletedListener(handshakeCompletedEvent -> {
                SSLSocket sslSocket = handshakeCompletedEvent.getSocket();
                try {
                    sslSocket.getOutputStream().write("hello".getBytes());
                    byte[] data = IoUtils.tryRead(sslSocket.getInputStream());
                    if (data != null) {
                        LogDog.d("==> " + new String(data));
                    }
                    sslSocket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            sslSocketImpl.startHandshake();
            SSLSession session = sslSocketImpl.getSession();
            Certificate[] serverCertificates = session.getPeerCertificates();
            for (int i = 0; i < serverCertificates.length; i++) {
                System.out.print("-----BEGIN CERTIFICATE-----\n");
                System.out.print(new sun.misc.BASE64Encoder().encode(serverCertificates[i].getEncoded()));
                System.out.print("\n-----END CERTIFICATE-----\n");
            }
//            sslSocketImpl.getOutputStream().write("hello".getBytes());
//            byte[] data = IoUtils.tryRead(sslSocketImpl.getInputStream());
//            if (data != null) {
//                LogDog.d("==> " + new String(data));
//            }
//            sslSocketImpl.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

//        https://blog.csdn.net/wangyangzhizhou/article/details/50377638


        HttpProxyServer httpProxyServer = new HttpProxyServer();
        String host = NetUtils.getLocalIp("wlan");
        LogDog.d("==> HttpProxyServer host = " + host);
        httpProxyServer.setAddress(host, 7777);
        NioServerFactory.getFactory().open();
        NioServerFactory.getFactory().addTask(httpProxyServer);
    }
}
