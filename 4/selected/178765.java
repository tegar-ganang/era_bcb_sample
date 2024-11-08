package org.jruby.extension;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import javax.swing.JOptionPane;

/**
 *
 * @author nathan
 */
public class Installer {

    public static String JAR_FILE = "jruby.jar";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        File log = new File(tmpDir, "Installer.log");
        try {
            OutputStream os = new FileOutputStream(log, true);
            PrintWriter pw = new PrintWriter(os, true);
            try {
                pw.println("--------------------------------------------------");
                pw.println("Running installation @ " + new java.util.Date());
                pw.println(" --- arg listing ---");
                for (String arg : args) {
                    pw.println(arg);
                }
                System.getProperties().list(pw);
                File dir = getWritableExtensionDirectory(pw);
                pw.println("Target directory: " + dir.getAbsolutePath());
                InputStream in = Installer.class.getResourceAsStream("/" + JAR_FILE);
                OutputStream out = new FileOutputStream(new File(dir, JAR_FILE));
                int bytesRead;
                byte[] buffer = new byte[1024];
                while ((bytesRead = in.read(buffer)) != -1) out.write(buffer, 0, bytesRead);
            } catch (Throwable thrown) {
                JOptionPane.showMessageDialog(null, thrown.toString());
                thrown.printStackTrace(pw);
            } finally {
                pw.println("--------------------------------------------------");
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static File getWritableExtensionDirectory(PrintWriter pw) {
        String[] extDirs = System.getProperty("java.ext.dirs").split(File.pathSeparator);
        for (String extDir : extDirs) {
            File dir = new File(extDir);
            pw.println("Testing permission to write to directory '" + dir.getAbsolutePath() + "'");
            if (dir.canWrite()) return dir;
            pw.println("No permission to write to '" + dir.getAbsolutePath() + "'");
        }
        String deployExt = System.getProperty("deployment.user.extdir");
        pw.println("Testing '" + deployExt + "'");
        if (deployExt != null) {
            File dir = new File(deployExt);
            pw.println("Found '" + dir.getAbsolutePath() + "' ... but can it be written?");
            if (dir.canWrite()) return dir;
            pw.println("No permission to write to '" + dir.getAbsolutePath() + "'");
        }
        File dir = new File(System.getProperty("user.home"), ".java/deployment/ext");
        pw.println("Testing permission to write to directory '" + dir.getAbsolutePath() + "'");
        if (dir.exists() && dir.canWrite()) return dir;
        pw.println("No permission to write to '" + dir.getAbsolutePath() + "' (does it exist? " + dir.exists() + ")");
        throw new SecurityException("Unable to locate a suitable extensions directory with sufficient operating system permission to write.");
    }
}
