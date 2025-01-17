package org.apache.http.conn.ssl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.RSAPrivateCrtKeySpec;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.localserver.LocalTestServer;

/**
 * Unit tests for {@link SSLSocketFactory}.
 */
public class TestSSLSocketFactory extends TestCase {

    public TestSSLSocketFactory(String testName) {
        super(testName);
    }

    public static void main(String args[]) throws Exception {
        String[] testCaseName = { TestSSLSocketFactory.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    public static Test suite() {
        return new TestSuite(TestSSLSocketFactory.class);
    }

    static class TestX509HostnameVerifier implements X509HostnameVerifier {

        private boolean fired = false;

        public boolean verify(String host, SSLSession session) {
            return true;
        }

        public void verify(String host, SSLSocket ssl) throws IOException {
            this.fired = true;
        }

        public void verify(String host, String[] cns, String[] subjectAlts) throws SSLException {
        }

        public void verify(String host, X509Certificate cert) throws SSLException {
        }

        public boolean isFired() {
            return this.fired;
        }
    }

    public void testCreateSocket() throws Exception {
        String password = "changeit";
        char[] pwd = password.toCharArray();
        RSAPrivateCrtKeySpec k;
        k = new RSAPrivateCrtKeySpec(new BigInteger(CertificatesToPlayWith.RSA_PUBLIC_MODULUS, 16), new BigInteger(CertificatesToPlayWith.RSA_PUBLIC_EXPONENT, 10), new BigInteger(CertificatesToPlayWith.RSA_PRIVATE_EXPONENT, 16), new BigInteger(CertificatesToPlayWith.RSA_PRIME1, 16), new BigInteger(CertificatesToPlayWith.RSA_PRIME2, 16), new BigInteger(CertificatesToPlayWith.RSA_EXPONENT1, 16), new BigInteger(CertificatesToPlayWith.RSA_EXPONENT2, 16), new BigInteger(CertificatesToPlayWith.RSA_COEFFICIENT, 16));
        PrivateKey pk = KeyFactory.getInstance("RSA").generatePrivate(k);
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        InputStream in1, in2, in3;
        in1 = new ByteArrayInputStream(CertificatesToPlayWith.X509_FOO);
        in2 = new ByteArrayInputStream(CertificatesToPlayWith.X509_INTERMEDIATE_CA);
        in3 = new ByteArrayInputStream(CertificatesToPlayWith.X509_ROOT_CA);
        X509Certificate[] chain = new X509Certificate[3];
        chain[0] = (X509Certificate) cf.generateCertificate(in1);
        chain[1] = (X509Certificate) cf.generateCertificate(in2);
        chain[2] = (X509Certificate) cf.generateCertificate(in3);
        ks.setKeyEntry("RSA_KEY", pk, pwd, chain);
        ks.setCertificateEntry("CERT", chain[2]);
        KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmfactory.init(ks, pwd);
        KeyManager[] keymanagers = kmfactory.getKeyManagers();
        TrustManagerFactory tmfactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmfactory.init(ks);
        TrustManager[] trustmanagers = tmfactory.getTrustManagers();
        SSLContext sslcontext = SSLContext.getInstance("TLSv1");
        sslcontext.init(keymanagers, trustmanagers, null);
        LocalTestServer server = new LocalTestServer(null, null, null, sslcontext);
        server.registerDefaultHandlers();
        server.start();
        try {
            TestX509HostnameVerifier hostnameVerifier = new TestX509HostnameVerifier();
            SSLSocketFactory socketFactory = new SSLSocketFactory(sslcontext);
            socketFactory.setHostnameVerifier(hostnameVerifier);
            Scheme https = new Scheme("https", socketFactory, 443);
            DefaultHttpClient httpclient = new DefaultHttpClient();
            httpclient.getConnectionManager().getSchemeRegistry().register(https);
            HttpHost target = new HttpHost(LocalTestServer.TEST_SERVER_ADDR.getHostName(), server.getServicePort(), "https");
            HttpGet httpget = new HttpGet("/random/100");
            HttpResponse response = httpclient.execute(target, httpget);
            assertEquals(200, response.getStatusLine().getStatusCode());
            assertTrue(hostnameVerifier.isFired());
        } finally {
            server.stop();
        }
    }
}
