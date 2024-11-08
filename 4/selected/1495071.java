package gnu.classpath.tools.jarsigner;

import gnu.classpath.Configuration;
import gnu.classpath.SystemProperties;
import gnu.java.util.jar.JarUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.logging.Logger;

/**
 * The JAR signing handler of the <code>gjarsigner</code> tool.
 */
public class JarSigner {

    private static final Logger log = Logger.getLogger(JarSigner.class.getName());

    /** The owner tool of this handler. */
    private Main main;

    JarSigner(Main main) {
        super();
        this.main = main;
    }

    void start() throws Exception {
        if (Configuration.DEBUG) log.entering(this.getClass().getName(), "start");
        JarFile jarFile = new JarFile(main.getJarFileName());
        SFHelper sfHelper = new SFHelper(jarFile);
        sfHelper.startSigning();
        for (Enumeration e = jarFile.entries(); e.hasMoreElements(); ) {
            JarEntry je = (JarEntry) e.nextElement();
            String jeName = je.getName();
            if (jeName.equals(JarFile.MANIFEST_NAME) || jeName.endsWith(File.separator)) continue;
            sfHelper.updateEntry(je);
            if (main.isVerbose()) System.out.println(Messages.getString("JarSigner.1") + jeName);
        }
        sfHelper.finishSigning(main.isSectionsOnly());
        if (main.isVerbose()) System.out.println(Messages.getString("JarSigner.2") + JarFile.MANIFEST_NAME);
        File signedJarFile = File.createTempFile("gcp-", ".jar");
        FileOutputStream fos = new FileOutputStream(signedJarFile);
        JarOutputStream outSignedJarFile = new JarOutputStream(fos, sfHelper.getManifest());
        for (Enumeration e = jarFile.entries(); e.hasMoreElements(); ) {
            JarEntry je = (JarEntry) e.nextElement();
            String jeName = je.getName();
            if (jeName.equals(JarFile.MANIFEST_NAME) || jeName.endsWith(File.separator)) continue;
            log.finest("Processing " + jeName);
            JarEntry newEntry = new JarEntry(jeName);
            newEntry.setTime(je.getTime());
            outSignedJarFile.putNextEntry(newEntry);
            InputStream jeis = jarFile.getInputStream(je);
            copyFromTo(jeis, outSignedJarFile);
        }
        String signaturesFileName = main.getSigFileName();
        String sfFileName = JarUtils.META_INF + signaturesFileName + JarUtils.SF_SUFFIX;
        if (Configuration.DEBUG) log.fine("Processing " + sfFileName);
        JarEntry sfEntry = new JarEntry(sfFileName);
        sfEntry.setTime(System.currentTimeMillis());
        outSignedJarFile.putNextEntry(sfEntry);
        sfHelper.writeSF(outSignedJarFile);
        if (Configuration.DEBUG) log.fine("Created .SF file");
        if (main.isVerbose()) System.out.println(Messages.getString("JarSigner.8") + sfFileName);
        String dsaFileName = JarUtils.META_INF + signaturesFileName + JarUtils.DSA_SUFFIX;
        if (Configuration.DEBUG) log.fine("Processing " + dsaFileName);
        JarEntry dsaEntry = new JarEntry(dsaFileName);
        dsaEntry.setTime(System.currentTimeMillis());
        outSignedJarFile.putNextEntry(dsaEntry);
        sfHelper.writeDSA(outSignedJarFile, main.getSignerPrivateKey(), main.getSignerCertificateChain(), main.isInternalSF());
        if (Configuration.DEBUG) log.fine("Created .DSA file");
        if (main.isVerbose()) System.out.println(Messages.getString("JarSigner.8") + dsaFileName);
        outSignedJarFile.close();
        fos.close();
        signedJarFile.renameTo(new File(main.getSignedJarFileName()));
        if (Configuration.DEBUG) log.fine("Renamed signed JAR file");
        if (main.isVerbose()) System.out.println(SystemProperties.getProperty("line.separator") + Messages.getString("JarSigner.14"));
        if (Configuration.DEBUG) log.exiting(this.getClass().getName(), "start");
    }

    private void copyFromTo(InputStream in, JarOutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) != -1) if (n > 0) out.write(buffer, 0, n);
    }
}
