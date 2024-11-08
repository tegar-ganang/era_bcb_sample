package org.xmlsh.commands.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.List;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.xmlsh.core.Options;
import org.xmlsh.core.Options.OptionValue;
import org.xmlsh.core.XCommand;
import org.xmlsh.core.XValue;
import org.xmlsh.sh.shell.SerializeOpts;
import org.xmlsh.util.Base64Coder;
import org.xmlsh.util.StringPair;
import org.xmlsh.util.Util;

public class http extends XCommand {

    private static Logger mLogger = LogManager.getLogger(http.class);

    @Override
    public int run(List<XValue> args) throws Exception {
        Options opts = new Options("get:,put:,post:,head:,options:,delete:,connectTimeout:,contentType:,readTimeout:,+useCaches,+followRedirects,user:,password:,H=add-header:+,disableTrust:,keystore:,keypass:,sslproto:");
        opts.parse(args);
        SerializeOpts serializeOpts = getSerializeOpts();
        String method = "GET";
        boolean doInput = true;
        boolean doOutput = false;
        String surl = null;
        if (opts.hasOpt("get")) {
            method = "GET";
            surl = opts.getOptString("get", null);
        } else if (opts.hasOpt("put")) {
            method = "PUT";
            doInput = true;
            doOutput = true;
            surl = opts.getOptString("put", null);
        } else if (opts.hasOpt("post")) {
            method = "POST";
            doOutput = true;
            surl = opts.getOptString("post", null);
        } else if (opts.hasOpt("head")) {
            method = "HEAD";
            surl = opts.getOptString("head", null);
        } else if (opts.hasOpt("options")) {
            surl = opts.getOptString("options", null);
            method = "OPTIONS";
        } else if (opts.hasOpt("delete")) {
            surl = opts.getOptString("delete", null);
            method = "DELETE";
        } else if (opts.hasOpt("trace")) {
            method = "TRACE";
            surl = opts.getOptString("trace", null);
        } else surl = opts.getRemainingArgs().get(0).toString();
        if (surl == null) {
            usage();
            return 1;
        }
        int ret = 0;
        URL url = new URL(surl);
        URLConnection conn = url.openConnection();
        if (conn instanceof HttpURLConnection) {
            HttpURLConnection http = (HttpURLConnection) conn;
            setOptions(http, opts);
            http.setRequestMethod(method);
            OptionValue headers = opts.getOpt("H");
            if (headers != null) {
                for (XValue v : headers.getValues()) {
                    StringPair pair = new StringPair(v.toString(), '=');
                    http.addRequestProperty(pair.getLeft(), pair.getRight());
                }
            }
            http.setDoInput(doInput);
            http.setDoOutput(doOutput);
            if (doOutput) {
                conn.connect();
                OutputStream out = http.getOutputStream();
                Util.copyStream(getStdin().asInputStream(serializeOpts), out);
                out.close();
            }
            ret = http.getResponseCode();
            if (ret == 200) ret = 0;
        }
        if (doInput) {
            InputStream in = conn.getInputStream();
            Util.copyStream(in, getStdout().asOutputStream(serializeOpts));
            in.close();
        }
        return ret;
    }

    private void setOptions(HttpURLConnection http, Options opts) throws KeyManagementException, NoSuchAlgorithmException, UnrecoverableKeyException, CertificateException, FileNotFoundException, KeyStoreException, IOException {
        if (opts.hasOpt("connectTimeout")) http.setConnectTimeout((int) (opts.getOptDouble("connectTimeout", 0) * 1000.));
        if (opts.hasOpt("readTimeout")) http.setReadTimeout((int) (opts.getOptDouble("readTimeout", 0) * 1000.));
        if (opts.hasOpt("useCaches")) http.setUseCaches(opts.getOpt("useCaches").getFlag());
        if (opts.hasOpt("followRedirects")) http.setInstanceFollowRedirects(opts.getOpt("followRedirects").getFlag());
        if (opts.hasOpt("contentType")) http.setRequestProperty("Content-Type", opts.getOptString("contentType", "text/xml"));
        String disableTrustProto = opts.getOptString("disableTrust", null);
        String keyStore = opts.getOptString("keystore", null);
        String keyPass = opts.getOptString("keypass", null);
        String sslProto = opts.getOptString("sslProto", "SSLv3");
        if (disableTrustProto != null && http instanceof HttpsURLConnection) disableTrust((HttpsURLConnection) http, disableTrustProto); else if (keyStore != null) setClient((HttpsURLConnection) http, keyStore, keyPass, sslProto);
        String user = opts.getOptString("user", null);
        String pass = opts.getOptString("password", null);
        if (user != null && pass != null) {
            String up = user + ":" + pass;
            String credentials_encoding = "US-ASCII";
            String encoding = new String(Base64Coder.encode(up.getBytes(credentials_encoding)));
            http.setRequestProperty("Authorization", "Basic " + encoding);
        }
    }

    private void setClient(HttpsURLConnection http, String keyStoreName, String keyPass, String sslProto) throws NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, KeyStoreException, UnrecoverableKeyException, KeyManagementException {
        char[] keystorepass = keyPass.toCharArray();
        File keystoreFile = mShell.getFile(keyStoreName);
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream(keystoreFile), keystorepass);
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, keystorepass);
        SSLContext sc = SSLContext.getInstance(sslProto);
        sc.init(keyManagerFactory.getKeyManagers(), null, null);
        http.setSSLSocketFactory(sc.getSocketFactory());
    }

    private void disableTrust(HttpsURLConnection http, String protocol) throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            }
        } };
        SSLContext sc = SSLContext.getInstance(protocol);
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        http.setSSLSocketFactory(sc.getSocketFactory());
        http.setHostnameVerifier(new HostnameVerifier() {

            public boolean verify(String string, SSLSession ssls) {
                return true;
            }
        });
    }
}
