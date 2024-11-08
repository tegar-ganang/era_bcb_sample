package iwork.manager.installer;

import iwork.manager.core.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;
import java.io.*;
import java.net.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.SwingUtilities;

/**
* Installs the contents of the given zip file into the given directory in a
 * separate thread. You can poll the progress information and display it in
 * a progress bar oder progress window.
 *
 * @author Ulf Ochsenfahrt (ulfjack@stanford.edu)
 */
public class InstallerThread extends Thread {

    private static final Pattern prefixPattern = Pattern.compile("([^/]+/).*");

    private static class Pair {

        public String a, b;

        public Pair(String a, String b) {
            this.a = a;
            this.b = b;
        }
    }

    HashSet prefixSet = new HashSet();

    List installList = new ArrayList();

    File directory;

    ZipFile zipf;

    List listeners = new ArrayList();

    int current = 0;

    int maximum = 0;

    String currentFile = "";

    /**
        * Gets the number of files that have already been processed.
     */
    public int getProgress() {
        return current;
    }

    /**
        * Gets the number of files that have to be processed.
     */
    public int getMaximum() {
        return maximum;
    }

    /**
        * Gets the name of the currently processed file.
     */
    public String getCurrentFile() {
        return currentFile;
    }

    /**
        * The given ActionListener will be notified upon completion of the
     * installation. Passing null is a no-op.
     * The callback will happen in the SwingThread and can therefore trigger
     * changes in the user interface!
     */
    public void addActionListener(ActionListener l) {
        if (l != null) listeners.add(l);
    }

    private void fireEvent(ActionEvent event) {
        Iterator it = listeners.iterator();
        while (it.hasNext()) {
            ActionListener el = (ActionListener) it.next();
            el.actionPerformed(event);
        }
    }

    private void streamTo(InputStream in, File targetFile) throws IOException {
        File f = targetFile;
        File parentDir = f.getParentFile();
        if (parentDir != null) parentDir.mkdirs();
        OutputStream out = new FileOutputStream(f);
        byte[] buffer = new byte[1024];
        int amount;
        while ((amount = in.read(buffer)) > 0) out.write(buffer, 0, amount);
        out.close();
    }

    private String convertName(String name, String prefix) {
        name = name.substring(prefix.length());
        return name;
    }

    /**
        * Installs all files in the given zip-file that match the given prefix into
     * the given directory after removing the prefix from the name.
     * Creates directories as necessary.
     *
     * @param zipf Zip-File
     * @param path destination directory
     * @param prefix the prefix to be stripped, i.e. "tomcat/"
     * @return success
     */
    private boolean installTo(ZipFile zipf, File path, String prefix) throws Exception {
        path.mkdir();
        Enumeration mEnum = zipf.entries();
        while (mEnum.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) mEnum.nextElement();
            String entryname = entry.getName();
            if (entryname.startsWith(prefix)) {
                String name = convertName(entryname, prefix);
                File targetFile = new File(path, name);
                this.currentFile = entryname;
                if (entry.isDirectory()) {
                    System.out.println("Creating directory: " + name);
                    targetFile.mkdir();
                } else {
                    System.out.println("Installing file: " + name);
                    streamTo(zipf.getInputStream(entry), targetFile);
                }
            }
            current++;
            if (Thread.interrupted()) {
                return false;
            }
        }
        return true;
    }

    /**
        * Creates the package directory for the module to be installed.
     *
     * @param moduleName name of the module to be installed
     */
    private File createTarget(String moduleName) {
        File target = new File(this.directory, moduleName);
        System.out.println("Setting install path: " + target.getPath());
        return target;
    }

    /**
        * Gets an array of all module names of modules that will be installed by
     * this <code>InstallerThread</code>.
     */
    public String[] getModules() throws Exception {
        Enumeration mEnum = zipf.entries();
        if (!mEnum.hasMoreElements()) throw new Exception("Zip-File has no entries!");
        HashSet prefixSet = new HashSet();
        while (mEnum.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) mEnum.nextElement();
            Matcher m = prefixPattern.matcher(entry.getName());
            if (m.matches()) prefixSet.add(m.group(1)); else prefixSet.add("");
        }
        if (prefixSet.contains("")) {
            prefixSet = new HashSet();
            prefixSet.add("");
        }
        List result = new ArrayList();
        installList = new ArrayList();
        Iterator it = prefixSet.iterator();
        while (it.hasNext()) {
            String prefix = (String) it.next();
            ZipEntry entry = zipf.getEntry(prefix + Controller.CONFIG_FILE_NAME);
            if (entry == null) throw new Exception("No configuration file found in \"" + prefix + "\"!");
            LineNumberReader in = new LineNumberReader(new InputStreamReader(zipf.getInputStream(entry)));
            ConfigReader cr = new ConfigReader(".", Controller.CONFIG_FILE_NAME, in);
            in.close();
            String moduleName = cr.getInstallSetting("INSTALL_DIR", "");
            if ("".equals(moduleName)) throw new Exception("No installation directory found in \"" + Controller.CONFIG_FILE_NAME + "\"!");
            installList.add(new Pair(prefix, moduleName));
            result.add(moduleName);
        }
        return (String[]) result.toArray(new String[0]);
    }

    public void checkModulesInstalled() throws Exception {
        Iterator it = installList.iterator();
        while (it.hasNext()) {
            String moduleName = ((Pair) it.next()).b;
            File ftest = createTarget(moduleName);
            if (ftest.exists()) throw new Exception("Module \"" + moduleName + "\" is already installed!");
        }
    }

    private boolean installZipFile() throws Exception {
        maximum = zipf.size();
        if (prefixSet.size() == 0) getModules();
        checkModulesInstalled();
        Iterator it = installList.iterator();
        while (it.hasNext()) {
            Pair pair = (Pair) it.next();
            String prefix = pair.a;
            String moduleName = pair.b;
            boolean success = installTo(zipf, createTarget(moduleName), prefix);
            if (!success) {
                return false;
            }
        }
        return true;
    }

    public void run() {
        boolean success = false;
        try {
            success = installZipFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (success) {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    fireEvent(null);
                }
            });
        }
    }

    /**
        * Create an InstallerThread for the given file. It will attempt to install it
     * into <code>directory</code>.
     *
     * @param directory absolute or relative path of directory
     *		into which the package's directory will be copied; for example:
     *		<tt>..\Software</tt>
     *		or <tt>C:\iROS\Software</tt>
     * @param f a zip file containing the package
     */
    public InstallerThread(File directory, File f) throws Exception {
        this.directory = directory;
        this.zipf = new ZipFile(f);
    }
}
