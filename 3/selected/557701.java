package org.adempierelbr.process;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import org.adempierelbr.model.MLBRNFeWebService;
import org.adempierelbr.util.AdempiereLBR;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.CLogger;

/**
 * 	ProcGenerateCert
 *	
 *  @author Mario Grigioni
 *  @version $Id: ProcGenerateCert.java, 28/01/2011 09:52 mgrigioni Exp $
 */
public class ProcGenerateCert extends SvrProcess {

    private String FilePath = "/tmp/";

    private String FileName = "cert.keystore";

    private String p_envType = "1";

    /**	Logger			*/
    private static CLogger log = CLogger.getCLogger(ProcGenerateCert.class);

    /**
	 * 	Prepare
	 */
    protected void prepare() {
        ProcessInfoParameter[] para = getParameter();
        for (int i = 0; i < para.length; i++) {
            String name = para[i].getParameterName();
            if (para[i].getParameter() == null) ; else if (name.equals("File_Directory")) {
                FilePath = para[i].getParameter().toString();
            } else if (name.equals("lbr_NFeEnv")) {
                p_envType = para[i].getParameter().toString();
            } else log.log(Level.SEVERE, "Unknown Parameter: " + name);
        }
    }

    /**
	 * 	Process
	 *	@return Info
	 *	@throws Exception
	 */
    protected String doIt() throws Exception {
        String[] passphrases = MLBRNFeWebService.getURL(p_envType);
        generateCertificate(passphrases, FilePath, FileName);
        return "Certificado gerado com sucesso em: " + FilePath + " - " + FileName;
    }

    private static void generateCertificate(String[] passphrases, String FilePath, String FileName) throws Exception {
        if (!FilePath.endsWith(AdempiereLBR.getFileSeparator())) FilePath += AdempiereLBR.getFileSeparator();
        for (String passphrase : passphrases) {
            String load = FilePath + FileName;
            File file = new File(load);
            if (!file.exists()) load = null;
            String[] conexao = passphrase.split(":");
            char[] store = ("changeit").toCharArray();
            String host = conexao[0];
            int port = (conexao.length == 1) ? 443 : Integer.parseInt(conexao[1]);
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            InputStream in = null;
            if (load != null) in = new FileInputStream(load);
            ks.load(in, store);
            if (in != null) in.close();
            SSLContext context = SSLContext.getInstance("TLS");
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);
            X509TrustManager defaultTrustManager = (X509TrustManager) tmf.getTrustManagers()[0];
            SavingTrustManager tm = new SavingTrustManager(defaultTrustManager);
            context.init(null, new TrustManager[] { tm }, null);
            SSLSocketFactory factory = context.getSocketFactory();
            System.out.println("Opening connection to " + host + ":" + port + "...");
            SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
            socket.setSoTimeout(10000);
            try {
                System.out.println("Starting SSL handshake...");
                socket.startHandshake();
                socket.close();
                System.out.println();
                System.out.println("No errors, certificate is already trusted");
            } catch (SSLException e) {
                System.out.println("Certificate chain needed");
            }
            X509Certificate[] chain = tm.chain;
            if (chain == null) {
                System.out.println("Could not obtain server certificate chain");
                log.log(Level.WARNING, "Could not obtain server certificate chain");
                return;
            }
            System.out.println();
            System.out.println("Server sent " + chain.length + " certificate(s):");
            System.out.println();
            MessageDigest sha1 = MessageDigest.getInstance("SHA1");
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            for (int i = 0; i < chain.length; i++) {
                X509Certificate cert = chain[i];
                System.out.println(" " + (i + 1) + " Subject " + cert.getSubjectDN());
                System.out.println("   Issuer  " + cert.getIssuerDN());
                sha1.update(cert.getEncoded());
                System.out.println("   sha1    " + toHexString(sha1.digest()));
                md5.update(cert.getEncoded());
                System.out.println("   md5     " + toHexString(md5.digest()));
                System.out.println();
            }
            System.out.println("Enter certificate to add to trusted keystore");
            int k = 0;
            X509Certificate cert = chain[k];
            String alias = host + "-" + (k + 1);
            ks.setCertificateEntry(alias, cert);
            OutputStream out = new FileOutputStream(FilePath + FileName);
            ks.store(out, store);
            out.close();
            System.out.println();
            System.out.println(cert);
            System.out.println();
            System.out.println("Added certificate to keystore " + FileName + " using alias '" + alias + "'");
        }
    }

    private static final char[] HEXDIGITS = "0123456789abcdef".toCharArray();

    private static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 3);
        for (int b : bytes) {
            b &= 0xff;
            sb.append(HEXDIGITS[b >> 4]);
            sb.append(HEXDIGITS[b & 15]);
            sb.append(' ');
        }
        return sb.toString();
    }

    private static class SavingTrustManager implements X509TrustManager {

        private final X509TrustManager tm;

        private X509Certificate[] chain;

        SavingTrustManager(X509TrustManager tm) {
            this.tm = tm;
        }

        public X509Certificate[] getAcceptedIssuers() {
            throw new UnsupportedOperationException();
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            throw new UnsupportedOperationException();
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            this.chain = chain;
            tm.checkServerTrusted(chain, authType);
        }
    }
}
