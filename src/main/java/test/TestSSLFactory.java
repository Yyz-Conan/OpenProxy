package test;

import connect.network.base.joggle.ISSLFactory;

import javax.net.ServerSocketFactory;
import javax.net.ssl.*;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;


public class TestSSLFactory implements ISSLFactory {

    private SSLContext sslContext = null;


    public TestSSLFactory(String protocol, InputStream certificate) {
        try {
            //指定交换数字证书的加密标准
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(certificate, "ssl-program".toCharArray());

            //TrustManager决定是否信任对方的证书
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
//            SavingTrustManager manager = new SavingTrustManager((X509TrustManager) trustManagers[0]);

            sslContext = SSLContext.getInstance(protocol);
            sslContext.init(null, new TrustManager[]{trm}, null);
        } catch (Exception var3) {
            var3.printStackTrace();
        }

    }

    @Override
    public SSLSocketFactory getSSLSocketFactory() {
        return this.sslContext.getSocketFactory();
    }

    @Override
    public ServerSocketFactory getSSLServerSocketFactory() {
        return null;
    }

    @Override
    public HostnameVerifier getHostnameVerifier() {
        return null;
    }

    private static TrustManager trm = new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }

        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }
    };

    private static class SavingTrustManager implements X509TrustManager {

        private final X509TrustManager tm;
        private X509Certificate[] chain;

        SavingTrustManager(X509TrustManager tm) {
            this.tm = tm;
        }

        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType) {
            this.chain = chain;
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) {
            try {
                this.chain = chain;
                tm.checkServerTrusted(chain, authType);
            } catch (CertificateException e) {
                e.printStackTrace();
            }
        }
    }
}
