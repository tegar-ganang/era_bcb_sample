package edu.ucsd.ncmir.spl.filesystem;

import edu.sdsc.grid.io.irods.IRODSAccount;
import edu.sdsc.grid.io.local.LocalFile;
import edu.ucsd.ncmir.spl.utilities.Base64.InputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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

public class IRODSAccountFactory {

    private static IRODSAccount _account = null;

    public static synchronized IRODSAccount getAccount() throws FileNotFoundException, IOException, InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, ClassNotFoundException, IllegalBlockSizeException, BadPaddingException {
        if (IRODSAccountFactory._account == null) {
            String key = System.getProperty("use.ccdb");
            if (key != null) {
                ServerInfo si = ServerInfoFactory.getInstance().getServerInfo(key, "irods");
                IRODSAccountFactory._account = new IRODSAccount(si.getHost(), si.getPort(), si.getUserName(), si.getPassword(), si.getHomeDirectory(), si.getZone(), si.getDefaultStorageResource());
            } else {
                String host = System.getProperty("irodsHost");
                String s_port = System.getProperty("irodsPort");
                String user = System.getProperty("irodsUser");
                String proxy = System.getProperty("proxy");
                String home = System.getProperty("mdasCollectionHome");
                String domain_name = System.getProperty("mdasDomainName");
                String default_resource = System.getProperty("defaultResource");
                if ((host != null) && (s_port != null) && (user != null) && (proxy != null) && (home != null) && (domain_name != null) && (default_resource != null)) {
                    String certificate = IRODSAccountFactory.getCertificate();
                    File temp = File.createTempFile("jibber", "proxy");
                    temp.deleteOnExit();
                    PrintStream stream = new PrintStream(temp);
                    for (String s : proxy.split("\\\\n")) stream.println(s);
                    stream.close();
                    temp.setReadOnly();
                    IRODSAccountFactory._account = new InternalIRODSAccount(certificate, host, s_port, user, temp.getAbsolutePath(), home, domain_name, default_resource);
                } else IRODSAccountFactory._account = null;
            }
        }
        return IRODSAccountFactory._account;
    }

    public static URI getHomeURI() {
        URI uri = null;
        try {
            IRODSAccount account = IRODSAccountFactory.getAccount();
            if (account != null) uri = new URI("irods", account.getHomeDirectory(), null);
        } catch (URISyntaxException urise) {
        } catch (Exception e) {
            e.printStackTrace();
        }
        return uri;
    }

    static String _certificate = null;

    private static synchronized String getCertificate() throws IOException {
        if (IRODSAccountFactory._certificate == null) {
            ClassLoader cl = IRODSAccountFactory.class.getClassLoader();
            String prefix = "irodsaf_" + System.getProperty("user.name") + "_" + new Date().getTime();
            IRODSAccountFactory._certificate = "";
            for (Enumeration e = cl.getResources("certificates/"); e.hasMoreElements(); ) {
                JarFile jf = ((JarURLConnection) ((URL) e.nextElement()).openConnection()).getJarFile();
                for (Enumeration<JarEntry> ej = jf.entries(); ej.hasMoreElements(); ) {
                    JarEntry je = ej.nextElement();
                    if (!je.isDirectory() && je.getName().startsWith("certificates/")) {
                        InputStream in = (InputStream) jf.getInputStream(jf.getEntry(je.getName()));
                        String[] p = je.getName().split("/");
                        String suffix = "." + p[p.length - 1];
                        LocalFile temp = (LocalFile) LocalFile.createTempFile(prefix, suffix);
                        temp.deleteOnExit();
                        File file = temp.getFile();
                        FileOutputStream out = new FileOutputStream(file);
                        byte[] buffer = new byte[in.available()];
                        int bytes;
                        while ((bytes = in.read(buffer)) != -1) out.write(buffer, 0, bytes);
                        in.close();
                        out.close();
                        IRODSAccountFactory._certificate += file.getPath() + ",";
                    }
                }
            }
        }
        return IRODSAccountFactory._certificate;
    }
}
