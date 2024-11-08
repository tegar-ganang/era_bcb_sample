package edu.upmc.opi.caBIG.caTIES.security;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.log4j.Logger;

public class CertificateCopier {

    private static Logger logger = Logger.getLogger(CertificateCopier.class);

    private static String[] certificateNames = new String[] { "spirit-cert.0" };

    public static void main(String[] args) {
        CertificateCopier.initCertificates();
    }

    public static boolean initCertificates() {
        for (int i = 0; i < certificateNames.length; i++) {
            String path = System.getProperty("user.home") + File.separator + ".globus" + File.separator + "certificates" + File.separator + certificateNames[i];
            File file = new File(path);
            if (!file.exists()) {
                logger.info(certificateNames[i] + " certificate not found. Copying it to user.home");
                try {
                    InputStream in = CertificateCopier.class.getClassLoader().getResourceAsStream("certificates/" + certificateNames[i]);
                    if (in == null) {
                        logger.error("Could not find certificate file to be copied in the classpath");
                        return false;
                    }
                    path = System.getProperty("user.home");
                    file = new File(path);
                    if (!file.exists()) {
                        logger.error("Could not find user.home directory:" + path);
                        return false;
                    }
                    path += File.separator + ".globus";
                    file = new File(path);
                    if (!file.exists()) file.mkdir();
                    path += File.separator + "certificates";
                    file = new File(path);
                    if (!file.exists()) file.mkdir();
                    path += File.separator + certificateNames[i];
                    file = new File(path);
                    file.createNewFile();
                    copyInputStream(in, new BufferedOutputStream(new FileOutputStream(file)));
                } catch (IOException e) {
                    e.printStackTrace();
                    logger.error("Failed to copy certificate to user.home");
                    return false;
                }
            }
        }
        return true;
    }

    public static final void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[512];
        int len;
        while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
        in.close();
        out.close();
    }
}
