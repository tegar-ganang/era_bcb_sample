package org.vizzini.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

/**
 * Provides a self-extractor for jar (and zip) files.
 *
 * @author   Z.S. Jin
 * @author   John D. Mitchell
 * @author   Jeffrey M. Thompson
 * @version  v0.4
 * @since    v0.1
 */
public class JarSelfExtractor extends JFrame {

    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /** Manifest filepath. */
    static String MANIFEST = "META-INF/MANIFEST.MF";

    /** Class name. */
    private String _myClassName;

    /**
     * Construct this object.
     *
     * @since  v0.1
     */
    protected JarSelfExtractor() {
    }

    /**
     * Application method.
     *
     * @param  args  Application arguments.
     *
     * @since  v0.1
     */
    public static void main(String[] args) {
        JarSelfExtractor extractor = new JarSelfExtractor();
        String jarFileName = extractor.getJarFileName();
        extractor.extract(jarFileName);
        System.exit(0);
    }

    /**
     * Extract the contents of the given jar file.
     *
     * @param  jarFile  Jar file.
     *
     * @since  v0.1
     */
    public void extract(String jarFile) {
        File currentArchive = new File(jarFile);
        File outputDir = getOutputDirectory(currentArchive);
        if (outputDir != null) {
            byte[] buf = new byte[1024];
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd hh:mma", Locale.getDefault());
            ProgressMonitor pm = null;
            boolean overwrite = false;
            ZipFile zf = null;
            FileOutputStream out = null;
            InputStream in = null;
            try {
                zf = new ZipFile(currentArchive);
                int size = zf.size();
                int extracted = 0;
                pm = new ProgressMonitor(getParent(), "Extracting files...", "starting", 0, size - 4);
                pm.setMillisToDecideToPopup(0);
                pm.setMillisToPopup(0);
                Enumeration<? extends ZipEntry> entries = zf.entries();
                for (int i = 0; i < size; i++) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.isDirectory()) {
                        continue;
                    }
                    String pathname = entry.getName();
                    if (_myClassName.equals(pathname) || MANIFEST.equals(pathname.toUpperCase())) {
                        continue;
                    }
                    extracted++;
                    pm.setProgress(i);
                    pm.setNote(pathname);
                    if (pm.isCanceled()) {
                        return;
                    }
                    in = zf.getInputStream(entry);
                    File outFile = new File(outputDir, pathname);
                    Date archiveTime = new Date(entry.getTime());
                    if (overwrite == false) {
                        if (outFile.exists()) {
                            Object[] options = { "Yes", "Yes To All", "No" };
                            Date existTime = new Date(outFile.lastModified());
                            Long archiveLen = new Long(entry.getSize());
                            String msg = "File name conflict: " + "There is already a file with " + "that name on the disk!\n" + "\nFile name: " + outFile.getName() + "\nExisting file: " + formatter.format(existTime) + ",  " + outFile.length() + "Bytes" + "\nFile in archive:" + formatter.format(archiveTime) + ",  " + archiveLen + "Bytes" + "\n\nWould you like to overwrite the file?";
                            int result = JOptionPane.showOptionDialog(JarSelfExtractor.this, msg, "Warning", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
                            if (result == 2) {
                                continue;
                            } else if (result == 1) {
                                overwrite = true;
                            }
                        }
                    }
                    File parent = new File(outFile.getParent());
                    if (!parent.exists()) {
                        parent.mkdirs();
                    }
                    out = new FileOutputStream(outFile);
                    while (true) {
                        int nRead = in.read(buf, 0, buf.length);
                        if (nRead <= 0) {
                            break;
                        }
                        out.write(buf, 0, nRead);
                    }
                    out.close();
                    outFile.setLastModified(archiveTime.getTime());
                }
                pm.close();
                zf.close();
                getToolkit().beep();
                JOptionPane.showMessageDialog(JarSelfExtractor.this, "Extracted " + extracted + " file" + ((extracted > 1) ? "s" : "") + " from the\n" + jarFile + "\narchive into the\n" + outputDir.getPath() + "\ndirectory.", "Zip Self Extractor", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (zf != null) {
                        zf.close();
                    }
                    if (out != null) {
                        out.close();
                    }
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * @return  the name of the jar file this is in.
     *
     * @since   v0.1
     */
    protected String getJarFileName() {
        String className = this.getClass().getName();
        className = className.replaceAll("[.]", "/");
        _myClassName = className + ".class";
        URL urlJar = ClassLoader.getSystemResource(_myClassName);
        String urlStr = urlJar.toString();
        int from = "jar:file:".length();
        int to = urlStr.indexOf("!/");
        String answer = urlStr.substring(from, to);
        return answer;
    }

    /**
     * @param   currentArchive  Current archive.
     *
     * @return  the output directory.
     *
     * @since   v0.1
     */
    protected File getOutputDirectory(File currentArchive) {
        System.out.println("user.dir = " + System.getProperty("user.dir"));
        JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
        fc.setDialogType(JFileChooser.OPEN_DIALOG);
        fc.setDialogTitle("Select destination directory for extracting " + currentArchive.getName());
        fc.setMultiSelectionEnabled(false);
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        File answer = null;
        if (fc.showDialog(JarSelfExtractor.this, "Select") == JFileChooser.APPROVE_OPTION) {
            answer = fc.getSelectedFile();
        }
        return answer;
    }
}
