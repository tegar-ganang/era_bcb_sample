package jsynoptic.installer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import jsynoptic.installer.resources.InstallerResources;
import simtools.ui.CustomizedLocale;

/**
 * @author nicolas
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
class PackagesPanel extends InstallerPanel {

    /**
	 * A looger to dump error or warning messages in a soket, an output stream, a file...
	 */
    static Logger _logger = simtools.util.LogConfigurator.getLogger(PackagesPanel.class.getName());

    protected ArrayList boxes;

    protected int numPackages;

    protected File location;

    protected JTextField tfLoc;

    protected JButton btnLoc;

    protected JEditorPane tipPane;

    protected JScrollPane tipScroll;

    protected JProgressBar progress;

    protected boolean installFinished;

    protected Thread installThread;

    protected String os;

    public PackagesPanel(Installer installer) {
        super(installer);
        setLayout(new BorderLayout());
        boxes = new ArrayList();
        numPackages = Long.decode(Installer.resources.getString("numPackages")).intValue();
        if (File.separatorChar == '/') os = "Unix"; else if (File.separatorChar == '\\') os = "Windows"; else os = "Other";
        JPanel packSection = new JPanel(new BorderLayout());
        JPanel boxPanel = new JPanel(new GridLayout(numPackages, 1));
        JPanel scrollableBoxPanel = new JPanel(new BorderLayout());
        for (int i = 1; i <= numPackages; ++i) {
            int defaultState = Long.decode(Installer.resources.getString("packageDefaultState" + i)).intValue();
            String name;
            try {
                name = Installer.resources.getString("packageName" + i + os);
            } catch (MissingResourceException e1) {
                name = Installer.resources.getString("packageName" + i);
            }
            JCheckBox cb = new JCheckBox(name, defaultState >= 1);
            cb.addMouseListener(new TipListener("packageTip" + i));
            if (defaultState == 2) cb.setEnabled(false);
            boxes.add(cb);
            cb.setAlignmentX(Component.LEFT_ALIGNMENT);
            boxPanel.add(cb);
        }
        scrollableBoxPanel.add(boxPanel, BorderLayout.NORTH);
        packSection.add(new JScrollPane(scrollableBoxPanel), BorderLayout.WEST);
        tipPane = new JEditorPane();
        tipPane.setEditable(false);
        tipPane.setAutoscrolls(false);
        tipPane.setContentType("text/html");
        tipPane.setText(Installer.resources.getString("pointPackageForDescription"));
        packSection.add(tipScroll = new JScrollPane(tipPane), BorderLayout.CENTER);
        packSection.add(progress = new JProgressBar(), BorderLayout.SOUTH);
        add(packSection, BorderLayout.CENTER);
        Box hbox = Box.createHorizontalBox();
        hbox.add(new JLabel(Installer.resources.getString("location")));
        location = new File(Installer.resources.getString("defaultLocation" + os));
        hbox.add(tfLoc = new JTextField());
        updateLoc();
        Dimension d = tfLoc.getPreferredSize();
        d.width = getWidth();
        tfLoc.setMaximumSize(d);
        hbox.add(btnLoc = new JButton(Installer.resources.getString("chooseLocation")));
        add(hbox, BorderLayout.SOUTH);
        btnLoc.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser(location);
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int res = fc.showOpenDialog(PackagesPanel.this.installer);
                if (res == JFileChooser.APPROVE_OPTION) {
                    location = fc.getSelectedFile();
                    if (location == null) location = fc.getCurrentDirectory();
                    updateLoc();
                }
            }
        });
        installFinished = false;
        installThread = null;
    }

    public boolean process() {
        boolean run = false;
        synchronized (this) {
            if (installFinished) run = true; else {
                if (installThread != null) return false;
                installThread = new Thread() {

                    public void run() {
                        install();
                    }
                };
            }
        }
        if (run) {
            runJSynoptic();
            return true;
        }
        installThread.start();
        return false;
    }

    protected void runJSynoptic() {
        new Thread() {

            public void run() {
                installer.writeProperties();
                location.getAbsolutePath();
                File jsynoptic;
                if (os.equals("Unix")) jsynoptic = new File(location, "jsynoptic.sh"); else if (os.equals("Windows")) jsynoptic = new File(location, "jsynoptic.exe"); else jsynoptic = null;
                if ((jsynoptic != null) && jsynoptic.exists()) {
                    try {
                        Runtime.getRuntime().exec(jsynoptic.getAbsolutePath());
                        System.exit(0);
                    } catch (IOException e1) {
                    }
                }
                ResourceBundle runRes = ResourceBundle.getBundle("jsynoptic.ui.resources.RunResources", CustomizedLocale.get());
                File jsynJar = new File(location, "jsynoptic-" + runRes.getString("productVersion") + ".jar");
                if (!jsynJar.exists()) {
                    JOptionPane.showMessageDialog(installer, Installer.resources.getString("cantFindJSynoptic"), Installer.resources.getString("errorInInstall"), JOptionPane.ERROR_MESSAGE);
                    return;
                }
                installer.dispose();
                URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
                Method method;
                try {
                    method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
                    method.setAccessible(true);
                    method.invoke(sysloader, new Object[] { jsynJar.toURL() });
                    method = Class.forName("jsynoptic.ui.Run").getDeclaredMethod("main", new Class[] { String[].class });
                    method.invoke(null, new Object[] { new String[] {} });
                } catch (Exception e) {
                    System.err.println(Installer.resources.getString("installErrorIs") + e.getLocalizedMessage());
                    System.err.println(Installer.resources.getString("errorInInstall"));
                    System.exit(1);
                }
            }
        }.start();
    }

    public void install() {
        location = new File(tfLoc.getText());
        if (!location.exists()) {
            location.mkdirs();
        }
        if (!location.isDirectory()) location = new File(location.getParentFile(), "jsynoptic");
        tfLoc.setText(location.getAbsolutePath());
        boolean overwriteAll = false;
        byte[] data = new byte[4096];
        installer.previous.setEnabled(false);
        installer.next.setEnabled(false);
        tfLoc.setEnabled(false);
        btnLoc.setEnabled(false);
        try {
            for (int i = 1; i <= numPackages; ++i) {
                JCheckBox cb = (JCheckBox) boxes.get(i - 1);
                if (!cb.isSelected()) continue;
                cb.setBackground(Color.red);
                String zipName;
                try {
                    if (installer.jre == null) {
                        try {
                            zipName = Installer.resources.getString("packageZip" + i + os + "NoJRE");
                        } catch (MissingResourceException e) {
                            zipName = Installer.resources.getString("packageZip" + i + os);
                        }
                    } else {
                        zipName = Installer.resources.getString("packageZip" + i + os);
                    }
                } catch (MissingResourceException e1) {
                    zipName = Installer.resources.getString("packageZip" + i);
                }
                InputStream is = InstallerResources.class.getResourceAsStream(zipName);
                if (is == null) throw new IOException();
                ZipInputStream zip = new ZipInputStream(is);
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    File f = new File(location, entry.getName());
                    if (entry.isDirectory()) {
                        f.mkdirs();
                        continue;
                    }
                    if (f.exists() && (!overwriteAll)) {
                        Object[] options = new Object[] { Installer.resources.getString("yes"), Installer.resources.getString("yesAll"), Installer.resources.getString("no"), Installer.resources.getString("cancel") };
                        int res = JOptionPane.showOptionDialog(installer, Installer.resources.getString("overwriteFile") + f.getAbsolutePath(), Installer.resources.getString("overwriteTitle"), JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                        if (res == 2) continue;
                        if ((res == JOptionPane.CLOSED_OPTION) || (res == 3)) break;
                        if (res == 1) overwriteAll = true;
                    }
                    long size = entry.getSize();
                    if (size != -1) {
                        progress.setIndeterminate(false);
                        progress.setMinimum(0);
                        progress.setMaximum((int) size);
                        progress.setValue(0);
                    } else progress.setIndeterminate(true);
                    progress.setString(f.getAbsolutePath());
                    progress.setStringPainted(true);
                    FileOutputStream fos = new FileOutputStream(f);
                    int nread;
                    while ((nread = zip.read(data)) != -1) {
                        fos.write(data, 0, nread);
                        try {
                            SwingUtilities.invokeAndWait(new BarUpdater(nread));
                        } catch (InterruptedException e1) {
                        } catch (InvocationTargetException e1) {
                        }
                    }
                    fos.flush();
                    fos.close();
                }
                cb.setBackground(Color.green);
            }
            if (installer.zip != null) {
                ZipFile zip = new ZipFile(installer.zip);
                ZipEntry entry = zip.getEntry("jre");
                if (entry == null) throw new IOException("Internal Error!");
                Enumeration enumEntries = zip.entries();
                while (enumEntries.hasMoreElements()) {
                    entry = (ZipEntry) enumEntries.nextElement();
                    if (!entry.getName().startsWith("jre")) continue;
                    File f = new File(location, entry.getName());
                    if (entry.isDirectory()) {
                        f.mkdirs();
                        continue;
                    }
                    if (f.exists() && (!overwriteAll)) {
                        Object[] options = new Object[] { Installer.resources.getString("yes"), Installer.resources.getString("yesAll"), Installer.resources.getString("no"), Installer.resources.getString("cancel") };
                        int res = JOptionPane.showOptionDialog(installer, Installer.resources.getString("overwriteFile") + f.getAbsolutePath(), Installer.resources.getString("overwriteTitle"), JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                        if (res == 2) continue;
                        if ((res == JOptionPane.CLOSED_OPTION) || (res == 3)) break;
                        if (res == 1) overwriteAll = true;
                    }
                    long size = entry.getSize();
                    if (size != -1) {
                        progress.setIndeterminate(false);
                        progress.setMinimum(0);
                        progress.setMaximum((int) size);
                        progress.setValue(0);
                    } else progress.setIndeterminate(true);
                    progress.setString(Installer.resources.getString("installingJRE") + " : " + f.getAbsolutePath());
                    progress.setStringPainted(true);
                    FileOutputStream fos = new FileOutputStream(f);
                    InputStream zipStream = zip.getInputStream(entry);
                    int nread;
                    while ((nread = zipStream.read(data)) != -1) {
                        fos.write(data, 0, nread);
                        try {
                            SwingUtilities.invokeAndWait(new BarUpdater(nread));
                        } catch (InterruptedException e1) {
                        } catch (InvocationTargetException e1) {
                        }
                    }
                    fos.flush();
                    fos.close();
                }
                zip.close();
            } else if (installer.jre != null) {
                progress.setMinimum(0);
                progress.setMaximum(1);
                progress.setValue(0);
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {

                        public void run() {
                            progress.setIndeterminate(true);
                            progress.setString(Installer.resources.getString("installingJRE"));
                        }
                    });
                } catch (InterruptedException e1) {
                } catch (InvocationTargetException e1) {
                }
                copy(installer.jre, location);
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {

                        public void run() {
                            progress.setIndeterminate(false);
                            progress.setValue(1);
                        }
                    });
                } catch (InterruptedException e1) {
                } catch (InvocationTargetException e1) {
                }
            }
            if (os.equals("Unix")) {
                File chmod = new File("/usr/bin/chmod");
                if (!chmod.exists()) chmod = new File("/bin/chmod");
                if (!chmod.exists()) chmod = new File("/usr/local/bin/chmod");
                if (!chmod.exists()) chmod = new File("/sbin/chmod");
                if (!chmod.exists()) chmod = new File("/usr/sbin/chmod");
                if (!chmod.exists()) chmod = new File("/usr/local/sbin/chmod");
                if (chmod.exists()) {
                    String cmd = chmod.getAbsolutePath() + " +x ";
                    File jsynoptic = new File(location, "jsynoptic.sh");
                    if (jsynoptic.exists()) Runtime.getRuntime().exec(cmd + jsynoptic.getAbsolutePath());
                    File jreBinDir = new File(new File(location, "jre"), "bin");
                    if (jreBinDir.exists()) {
                        File[] binaries = jreBinDir.listFiles();
                        for (int i = 0; i < binaries.length; ++i) {
                            Runtime.getRuntime().exec(cmd + binaries[i].getAbsolutePath());
                        }
                    }
                    Runtime.getRuntime().exec(cmd + "jsynoptic.sh");
                }
            }
        } catch (IOException e) {
            installer.previous.setEnabled(true);
            installer.next.setEnabled(true);
            tfLoc.setEnabled(true);
            btnLoc.setEnabled(true);
            JOptionPane.showMessageDialog(installer, Installer.resources.getString("installErrorIs") + e.getLocalizedMessage(), Installer.resources.getString("errorInInstall"), JOptionPane.ERROR_MESSAGE);
            synchronized (this) {
                installFinished = false;
                installThread = null;
            }
            return;
        }
        synchronized (this) {
            installFinished = true;
            progress.setString(Installer.resources.getString("installComplete"));
            installer.next.setText(Installer.resources.getString("run"));
            installer.cancel.setText(Installer.resources.getString("dontRun"));
            ActionListener[] listeners = installer.cancel.getActionListeners();
            for (int i = 0; i < listeners.length; ++i) installer.cancel.removeActionListener(listeners[i]);
            installer.cancel.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    installer.writeProperties();
                    System.exit(0);
                }
            });
            installer.next.setEnabled(true);
        }
    }

    /**
	 * Recursive copy of file1 to file2, including subdirectories
	 * @param src A file or directory to copy recursively
	 * @param dest The name of the destination file if src is actually a file. In case dest is a directory, src will be copied in this directory with the same name.
	 * @throws IOException if an error occurs, and in particular if src or dest refers to a non existent file or directory 
	 */
    protected void copy(File src, File dest) throws IOException {
        if (src.isDirectory() && dest.isFile()) throw new IOException("Cannot copy a directory to a file");
        if (src.isDirectory()) {
            File newDir = new File(dest, src.getName());
            if (!newDir.mkdirs()) throw new IOException("Cannot create a new Directory");
            File[] entries = src.listFiles();
            for (int i = 0; i < entries.length; ++i) copy(entries[i], newDir);
            return;
        }
        if (dest.isDirectory()) {
            File newFile = new File(dest, src.getName());
            newFile.createNewFile();
            copy(src, newFile);
            return;
        }
        try {
            if (src.length() == 0) {
                dest.createNewFile();
                return;
            }
            FileChannel fc = new FileInputStream(src).getChannel();
            FileChannel dstChannel = new FileOutputStream(dest).getChannel();
            long transfered = 0;
            long totalLength = src.length();
            while (transfered < totalLength) {
                long num = fc.transferTo(transfered, totalLength - transfered, dstChannel);
                if (num == 0) throw new IOException("Error while copying");
                transfered += num;
            }
            dstChannel.close();
            fc.close();
        } catch (IOException e) {
            if (os.equals("Unix")) {
                _logger.fine("Trying to use cp to copy file...");
                File cp = new File("/usr/bin/cp");
                if (!cp.exists()) cp = new File("/bin/cp");
                if (!cp.exists()) cp = new File("/usr/local/bin/cp");
                if (!cp.exists()) cp = new File("/sbin/cp");
                if (!cp.exists()) cp = new File("/usr/sbin/cp");
                if (!cp.exists()) cp = new File("/usr/local/sbin/cp");
                if (cp.exists()) {
                    Process cpProcess = Runtime.getRuntime().exec(cp.getAbsolutePath() + " '" + src.getAbsolutePath() + "' '" + dest.getAbsolutePath() + "'");
                    int errCode;
                    try {
                        errCode = cpProcess.waitFor();
                    } catch (java.lang.InterruptedException ie) {
                        throw e;
                    }
                    return;
                }
            }
            throw e;
        }
    }

    protected class BarUpdater implements Runnable {

        int delta;

        public BarUpdater(int delta) {
            this.delta = delta;
        }

        public void run() {
            progress.setValue(progress.getValue() + delta);
        }
    }

    protected void updateLoc() {
        try {
            tfLoc.setText(location.getCanonicalPath());
        } catch (IOException e) {
            tfLoc.setText(location.getAbsolutePath());
        }
    }

    public void update() {
        synchronized (this) {
            if (installFinished) {
                installer.next.setText(Installer.resources.getString("run"));
                installer.cancel.setText(Installer.resources.getString("dontRun"));
                installer.previous.setEnabled(false);
            } else {
                installer.next.setText(Installer.resources.getString("install"));
                installer.cancel.setText(Installer.resources.getString("cancel"));
            }
        }
        setBorder(BorderFactory.createTitledBorder(Installer.resources.getString("selectPackages")));
        for (int i = 1; i <= numPackages; ++i) {
            JCheckBox cb = (JCheckBox) boxes.get(i - 1);
            String name;
            try {
                name = Installer.resources.getString("packageName" + i + os);
            } catch (MissingResourceException e1) {
                name = Installer.resources.getString("packageName" + i);
            }
            cb.setText(name);
        }
        tipPane.setText(Installer.resources.getString("pointPackageForDescription"));
        progress.setMinimum(0);
        progress.setMaximum(1);
        progress.setValue(0);
        progress.setStringPainted(false);
    }

    protected class TipListener extends MouseAdapter {

        protected String key;

        public TipListener(String resourceKey) {
            key = resourceKey;
        }

        public void mouseEntered(MouseEvent e) {
            String text;
            try {
                text = Installer.resources.getString(key + os);
            } catch (MissingResourceException e1) {
                text = Installer.resources.getString(key);
            }
            tipPane.setText(text);
            JScrollBar sb = tipScroll.getVerticalScrollBar();
            if (sb != null) sb.setValue(sb.getMinimum());
        }
    }
}
