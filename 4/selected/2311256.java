package org.openconcerto.modules.finance.payment.ebics;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import org.bouncycastle.crypto.tls.TlsClient;
import org.bouncycastle.util.encoders.Base64Encoder;

public class TestEbics {

    public static void main(String[] args) throws Exception {
        Partner partner = new Partner("456");
        User user = new User("123");
        String urlhttp = "https://server-ebics.webank.fr:28103/WbkPortalFileTransfert/EbicsProtocol";
        String E001Digest = "9BF804AF2B121A5B94C82BFD8E406FFB18024D3D4BF9E";
        String X001Digest = "9BF804AF2B121A5B94C82BFD8E406FFB18024D3D4BF9E";
        String hostId = "EBIXQUAL";
        InputStream inStream = new FileInputStream("ServerEbicsValerian.cer");
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) cf.generateCertificate(inStream);
        inStream.close();
        URL url = new URL(urlhttp);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setHostnameVerifier(new HostnameVerifier() {

            @Override
            public boolean verify(String hostname, SSLSession session) {
                System.out.println(hostname);
                return true;
            }
        });
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
        connection.setRequestProperty("Accept", "text/xml");
        byte[] query = getQuery();
        int queryLength = query.length;
        connection.setRequestProperty("Content-length", String.valueOf(queryLength));
        OutputStream out = connection.getOutputStream();
        out.write(query);
        System.out.println("----------RESPONSE");
        System.out.println("Resp Code:" + connection.getResponseCode());
        System.out.println("Resp Message:" + connection.getResponseMessage());
        InputStream in = connection.getInputStream();
        FileOutputStream fOut = new FileOutputStream("out.xml");
        int ch = 0;
        while ((ch = in.read()) >= 0) {
            System.out.print((char) ch);
            fOut.write(ch);
        }
        fOut.close();
    }

    static byte[] getQuery() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BufferedReader reader = new BufferedReader(new FileReader("INI_request.xml"));
        while (reader.ready()) {
            out.write((reader.readLine() + "\n").getBytes());
        }
        reader.close();
        System.out.println("----------REQUETE");
        System.out.println(new String(out.toByteArray()));
        return out.toByteArray();
    }

    /**
     * Create an SSL context with the identity and trust stores in place
     */
    static SSLContext createSSLContext() throws Exception {
        KeyManagerFactory mgrFact = KeyManagerFactory.getInstance("SunX509");
        KeyStore serverStore = KeyStore.getInstance("JKS");
        TrustManagerFactory trustFact = TrustManagerFactory.getInstance("SunX509");
        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustFact.init(trustStore);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(mgrFact.getKeyManagers(), trustFact.getTrustManagers(), null);
        return sslContext;
    }
}
