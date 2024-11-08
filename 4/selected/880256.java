package org.red5.server.net.rtmps;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.NotActiveException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.ssl.SslFilter;
import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.rtmp.RTMPMinaIoHandler;
import org.red5.server.net.rtmp.codec.RTMP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles Native RTMPS protocol events fired by the MINA framework.
 * <pre>
 * var nc:NetConnection = new NetConnection();
 * nc.proxyType = "best";
 * nc.connect("rtmps:\\localhost\app");
 * </pre>
 * Originally created by: Kevin Green
 *  
 *  http://tomcat.apache.org/tomcat-6.0-doc/ssl-howto.html
 *  http://java.sun.com/j2se/1.5.0/docs/guide/security/CryptoSpec.html#AppA
 *  http://java.sun.com/j2se/1.5.0/docs/api/java/security/KeyStore.html
 *  http://tomcat.apache.org/tomcat-3.3-doc/tomcat-ssl-howto.html
 *  
 * @author Kevin Green (kevygreen@gmail.com)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class RTMPSMinaIoHandler extends RTMPMinaIoHandler {

    private static Logger log = LoggerFactory.getLogger(RTMPSMinaIoHandler.class);

    private static final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
        }

        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
        }
    } };

    /**
	 * Password for accessing the keystore.
	 */
    private char[] password;

    /**
	 * Stores the keystore file bytes.
	 */
    private byte[] keystore;

    /**
	 * The keystore type, valid options are JKS and PKCS12
	 */
    private String keyStoreType = "JKS";

    /** {@inheritDoc} */
    @Override
    public void sessionOpened(IoSession session) throws Exception {
        if (password == null || keystore == null) {
            throw new NotActiveException("Keystore or password are null");
        }
        SSLContext context = null;
        SslFilter sslFilter = null;
        RTMP rtmp = (RTMP) session.getAttribute(ProtocolState.SESSION_KEY);
        if (rtmp.getMode() != RTMP.MODE_CLIENT) {
            context = SSLContext.getInstance("TLS");
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(getKeyStore(), password);
            context.init(kmf.getKeyManagers(), null, null);
            sslFilter = new SslFilter(context);
        } else {
            context = SSLContext.getInstance("SSL");
            context.init(null, trustAllCerts, new SecureRandom());
            sslFilter = new SslFilter(context);
            sslFilter.setUseClientMode(true);
        }
        if (sslFilter != null) {
            session.getFilterChain().addFirst("sslFilter", sslFilter);
        }
        super.sessionOpened(session);
    }

    /** {@inheritDoc} */
    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        log.warn("Exception caught {}", cause.getMessage());
        if (log.isDebugEnabled()) {
            log.error("Exception detail", cause);
        }
        session.close(true);
    }

    /**
	 * Returns a KeyStore.
	 * @return KeyStore
	 * @throws IOException 
	 * @throws CertificateException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyStoreException 
	 */
    private KeyStore getKeyStore() throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException {
        KeyStore ks = KeyStore.getInstance(keyStoreType);
        ks.load(new ByteArrayInputStream(keystore), password);
        return ks;
    }

    /**
	 * Password used to access the keystore file.
	 * 
	 * @param password
	 */
    public void setKeyStorePassword(String password) {
        this.password = password.toCharArray();
    }

    /**
	 * Set keystore data from a file.
	 * 
	 * @param path contains keystore
	 */
    public void setKeystoreFile(String path) {
        FileInputStream fis = null;
        try {
            File file = new File(path);
            if (file.exists()) {
                fis = new FileInputStream(file);
                FileChannel fc = fis.getChannel();
                ByteBuffer fb = ByteBuffer.allocate(Long.valueOf(file.length()).intValue());
                fc.read(fb);
                fb.flip();
                keystore = IoBuffer.wrap(fb).array();
            } else {
                log.warn("Keystore file does not exist: {}", path);
            }
            file = null;
        } catch (Exception e) {
            log.warn("Error setting keystore data", e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
	 * Set keystore data from a file.
	 * 
	 * @param arr keystore bytes
	 */
    public void setKeystoreBytes(byte[] arr) {
        keystore = new byte[arr.length];
        System.arraycopy(arr, 0, keystore, 0, arr.length);
    }

    /**
	 * Set the key store type, JKS or PKCS12.
	 * 
	 * @param keyStoreType
	 */
    public void setKeyStoreType(String keyStoreType) {
        this.keyStoreType = keyStoreType;
    }
}
