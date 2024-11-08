package edu.ucsd.ncmir.spl.filesystem;

import edu.sdsc.grid.io.local.LocalFile;
import edu.sdsc.grid.io.srb.SRBAccount;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class SRBAccountFactory {

    private static SRBAccount _srb_account = null;

    public static synchronized SRBAccount getAccount() throws FileNotFoundException, IOException, InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, ClassNotFoundException, IllegalBlockSizeException, BadPaddingException {
        if (SRBAccountFactory._srb_account == null) {
            String key = System.getProperty("use.ccdb");
            if (key != null) {
                ServerInfo si = ServerInfoFactory.getInstance().getServerInfo(key, "srb");
                SRBAccountFactory._srb_account = new SRBAccount(si.getHost(), si.getPort(), si.getUserName(), si.getPassword(), si.getHomeDirectory(), si.getMDASDomainName(), si.getDefaultStorageResource(), si.getZone());
            } else {
                String host = System.getProperty("srbHost");
                String s_port = System.getProperty("srbPort");
                String user = System.getProperty("srbUser");
                String proxy = System.getProperty("proxy");
                String home = System.getProperty("mdasCollectionHome");
                String domain_name = System.getProperty("mdasDomainName");
                String default_resource = System.getProperty("defaultResource");
                if ((host != null) && (s_port != null) && (user != null) && (proxy != null) && (home != null) && (domain_name != null) && (default_resource != null)) {
                    String certificate = SRBAccountFactory.getCertificate();
                    File temp = File.createTempFile("jibber", "proxy");
                    temp.deleteOnExit();
                    PrintStream stream = new PrintStream(temp);
                    for (String s : proxy.split("\\\\n")) stream.println(s);
                    stream.close();
                    temp.setReadOnly();
                    SRBAccountFactory._srb_account = new InternalSRBAccount(certificate, host, s_port, user, temp.getAbsolutePath(), home, domain_name, default_resource);
                } else SRBAccountFactory._srb_account = null;
            }
        }
        return SRBAccountFactory._srb_account;
    }

    public static URI getHomeURI() {
        URI uri = null;
        try {
            SRBAccount account = SRBAccountFactory.getAccount();
            if (account != null) uri = new URI("srb", account.getHomeDirectory(), null);
        } catch (URISyntaxException urise) {
        } catch (Exception e) {
            e.printStackTrace();
        }
        return uri;
    }

    static String _certificate = null;

    private static synchronized String getCertificate() throws IOException {
        if (SRBAccountFactory._certificate == null) {
            ClassLoader cl = SRBAccountFactory.class.getClassLoader();
            String prefix = "srbaf_" + System.getProperty("user.name") + "_" + new Date().getTime();
            SRBAccountFactory._certificate = "";
            for (Enumeration e = cl.getResources("certificates/"); e.hasMoreElements(); ) {
                JarFile jf = ((JarURLConnection) ((URL) e.nextElement()).openConnection()).getJarFile();
                for (Enumeration<JarEntry> ej = jf.entries(); ej.hasMoreElements(); ) {
                    JarEntry je = ej.nextElement();
                    if (!je.isDirectory() && je.getName().startsWith("certificates/")) {
                        InputStream in = jf.getInputStream(jf.getEntry(je.getName()));
                        String[] p = je.getName().split("/");
                        String suffix = "." + p[p.length - 1];
                        LocalFile temp = (LocalFile) LocalFile.createTempFile(prefix, suffix);
                        temp.deleteOnExit();
                        File file = temp.getFile();
                        OutputStream out = new FileOutputStream(file);
                        byte[] buffer = new byte[in.available()];
                        int bytes;
                        while ((bytes = in.read(buffer)) != -1) out.write(buffer, 0, bytes);
                        in.close();
                        out.close();
                        SRBAccountFactory._certificate += file.getPath() + ",";
                    }
                }
            }
        }
        return SRBAccountFactory._certificate;
    }
}
