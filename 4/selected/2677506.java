package org.jdna.minecraft.tools;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.log4j.Logger;

public class ModInstallerDialog extends JDialog {

    private static final Logger log = Logger.getLogger(ModInstallerDialog.class);

    private static int MAX_TASKS = 6;

    private final JPanel contentPanel = new JPanel();

    private Map<String, Boolean> copied = new HashMap<String, Boolean>();

    /**
	 * Launch the application.
	 */
    public static void main(String[] args) {
        try {
            ModInstallerDialog dialog = new ModInstallerDialog();
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setVisible(true);
            List<File> files = new ArrayList<File>();
            files.add(new File("/home/sean/MINECRAFT_MODS/ModLoader.zip"));
            files.add(new File("/home/sean/MINECRAFT_MODS/Arrows Mod.zip"));
            files.add(new File("/home/sean/MINECRAFT_MODS/Planes 1.7.3 v15.zip"));
            dialog.performUpdate(files, new IStatusHandler() {

                @Override
                public void installed(File f) {
                    System.out.println("installed file: " + f);
                }

                @Override
                public void fail(File f, Throwable t) {
                    System.out.println("Failed: " + f);
                    t.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    Preferences prefs = Application.getInstance().getPreferences();

    JList list = new JList(new DefaultListModel());

    JButton btnOk = new JButton("Cancel");

    JLabel lblNewLabel = new JLabel("");

    JProgressBar progressBar = new JProgressBar();

    private final JScrollPane scrollPane = new JScrollPane();

    /**
	 * Create the dialog.
	 */
    public ModInstallerDialog() {
        setTitle("Installing Mods...");
        setBounds(100, 100, 640, 480);
        getContentPane().setLayout(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        getContentPane().add(contentPanel, BorderLayout.CENTER);
        GridBagLayout gbl_contentPanel = new GridBagLayout();
        gbl_contentPanel.columnWidths = new int[] { 0, 0 };
        gbl_contentPanel.rowHeights = new int[] { 0, 0, 0, 0, 0 };
        gbl_contentPanel.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
        gbl_contentPanel.rowWeights = new double[] { 0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE };
        contentPanel.setLayout(gbl_contentPanel);
        GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
        gbc_lblNewLabel.fill = GridBagConstraints.HORIZONTAL;
        gbc_lblNewLabel.insets = new Insets(0, 0, 5, 0);
        gbc_lblNewLabel.gridx = 0;
        gbc_lblNewLabel.gridy = 0;
        contentPanel.add(lblNewLabel, gbc_lblNewLabel);
        GridBagConstraints gbc_progressBar = new GridBagConstraints();
        gbc_progressBar.fill = GridBagConstraints.HORIZONTAL;
        gbc_progressBar.insets = new Insets(0, 0, 5, 0);
        gbc_progressBar.gridx = 0;
        gbc_progressBar.gridy = 1;
        contentPanel.add(progressBar, gbc_progressBar);
        GridBagConstraints gbc_scrollPane = new GridBagConstraints();
        gbc_scrollPane.fill = GridBagConstraints.BOTH;
        gbc_scrollPane.insets = new Insets(0, 0, 5, 0);
        gbc_scrollPane.gridx = 0;
        gbc_scrollPane.gridy = 2;
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        contentPanel.add(scrollPane, gbc_scrollPane);
        scrollPane.setViewportView(list);
        list.setAutoscrolls(true);
        list.setCellRenderer(new MyCellRenderer());
        GridBagConstraints gbc_btnOk = new GridBagConstraints();
        gbc_btnOk.gridx = 0;
        gbc_btnOk.gridy = 3;
        contentPanel.add(btnOk, gbc_btnOk);
        btnOk.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
    }

    public void performUpdate(final List<File> files, final IStatusHandler handler) {
        SwingWorker t = new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                performUpdate2(files, handler);
                return null;
            }
        };
        t.execute();
    }

    public void performUpdate2(List<File> files, IStatusHandler handler) {
        progressBar.setIndeterminate(false);
        progressBar.setMinimum(0);
        progressBar.setIndeterminate(true);
        lblNewLabel.setText("Installing files... Do not close the window.");
        try {
            int progress = 1;
            if (prefs.isCreateBackupsEnabled()) {
                if (prefs.isCreateFullBackupsEnabled()) {
                    setProgress(progress++, "Creating full backup...");
                    Application.getInstance().getBackupManager().fullBackup();
                } else {
                    setProgress(progress++, "Creating backup of minecraft.jar...");
                    Application.getInstance().getBackupManager().backupJarOnly();
                }
            }
            setProgress(progress++, "Preparing mods...");
            File dir = prefs.getMinecraftDirAsFile();
            if (!prefs.isValidMinecraftDir(dir)) {
                dir = Application.getInstance().chooseMinecraftDir();
                if (dir == null) {
                    close();
                    return;
                }
                prefs.setMinecraftDir(dir.getAbsolutePath());
            }
            if (!prefs.isValidMinecraftDir(dir)) {
                throw new Exception("You must choose a valid minecraft installation directory");
            }
            setMessage("Extracting minecraft.jar...");
            log.info("extracting...");
            File tmpArea = prefs.getTmpArea();
            File mineDir = File.createTempFile("minecraft-", "-jar", tmpArea);
            if (mineDir.exists()) mineDir.delete();
            mineDir.mkdirs();
            if (!mineDir.exists() || !mineDir.isDirectory()) {
                throw new Exception("Can't create directory: " + mineDir);
            }
            Util.unzip(prefs.getMinecraftJar(), mineDir);
            File meta = new File(mineDir, "META-INF");
            if (meta.exists()) {
                setMessage("deleting META-INF");
                FileUtils.deleteDirectory(meta);
            }
            for (File f : files) {
                try {
                    if (!f.exists()) {
                        throw new Exception("File Not Found: " + f);
                    }
                    setMessage("");
                    setProgress(progress++, "Extracting " + f.getName());
                    updateMinecraft(f, mineDir);
                    handler.installed(f);
                } catch (Throwable t) {
                    handler.fail(f, t);
                }
            }
            setProgress(progress++, "Creating new minecraft.jar");
            String name = BackupManager.createBackupName("minecraft.jar");
            File newjar = new File(prefs.getTmpArea(), name);
            Util.zip(mineDir, newjar);
            setProgress(progress++, "Installing new minecraft.jar");
            FileUtils.copyFile(newjar, prefs.getMinecraftJar());
            File mockMineDir = new File(prefs.getTmpArea(), "mockminecraft");
            if (mockMineDir.exists()) {
                setProgress(progress++, "Copying Resources...");
                FileUtils.copyDirectory(mockMineDir, prefs.getMinecraftDirAsFile(), new TrackingFileFilter(mockMineDir, true), true);
            }
            setProgress(MAX_TASKS, "<html><b><font color='green'>Done</font></b></html>");
            lblNewLabel.setText("<html><b><font color='green'>Installing complete.  You can now close this window.</font></b></html>");
        } catch (Exception e) {
            lblNewLabel.setText("<html><b><font color=red>Installation Failed.  You should restore your Minecraft.jar</font></b></html>");
            log.error("Failed to install mods", e);
            Application.getInstance().error("Error", "Install Failed: " + e.getMessage() + "; See log for details.");
        } finally {
            btnOk.setText("Close");
            try {
                FileUtils.deleteDirectory(prefs.getTmpArea());
            } catch (IOException e) {
                setMessage("<html><font color='red'>Unable to clean out tmp area: " + prefs.getTmpArea() + "; You should manually delete this location.</font></html>");
                log.warn("Could not clean up the tmp area: " + prefs.getTmpArea(), e);
            }
            progressBar.setValue(0);
            progressBar.setIndeterminate(false);
        }
    }

    private class TrackingFileFilter implements FileFilter {

        private boolean verbose;

        private File base;

        public TrackingFileFilter(File baseDir, boolean verbose) {
            this.verbose = verbose;
            this.base = baseDir;
        }

        @Override
        public boolean accept(File pathname) {
            if (pathname.isFile()) {
                if (copied.containsKey(pathname.getAbsolutePath())) {
                    setMessage("Collision: " + pathname);
                }
            }
            if (pathname.isDirectory()) {
                return true;
            }
            copied.put(pathname.getAbsolutePath(), Boolean.TRUE);
            progressBar.setValue(progressBar.getValue() + 1);
            if (verbose) {
                setMessage("<html>Copied: <i>" + Util.relativePath(base, pathname) + "</i></html>");
            }
            return true;
        }
    }

    private void updateMinecraft(File f, File mineCraftJarDir) throws IOException {
        File zipDir = new File(prefs.getTmpArea(), f.getName());
        Util.unzip(f, zipDir);
        File mockMineDir = new File(prefs.getTmpArea(), "mockminecraft");
        FileUtils.forceMkdir(mockMineDir);
        File mockRes = new File(mockMineDir, "resources");
        FileUtils.forceMkdir(mockRes);
        if (hasClassFiles(zipDir)) {
            FileUtils.copyDirectory(zipDir, mineCraftJarDir, new TrackingFileFilter(prefs.getTmpArea(), true), true);
            return;
        }
        File files[] = zipDir.listFiles();
        if (files == null || files.length == 0) {
            setMessage(f.getName() + " contained no files");
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                if (file.getName().startsWith("__")) continue;
                if (file.getName().toLowerCase().contains("resource")) {
                    FileUtils.copyDirectory(file, mockRes, new TrackingFileFilter(prefs.getTmpArea(), true), true);
                    continue;
                }
                File dir = findClassesDir(file);
                if (dir != null) {
                    FileUtils.copyDirectory(dir, mineCraftJarDir, new TrackingFileFilter(prefs.getTmpArea(), true), true);
                    continue;
                }
                setMessage("Unknown Directory " + file + " (ignored)");
            } else {
                if (!file.getName().toLowerCase().contains("readme")) {
                    setMessage("Copied: " + file.getName());
                    FileUtils.copyFileToDirectory(file, mockMineDir);
                    continue;
                }
            }
        }
    }

    private File findClassesDir(File dir) {
        Collection<File> files = FileUtils.listFiles(dir, new SuffixFileFilter(".class"), TrueFileFilter.TRUE);
        if (files != null && files.size() > 0) {
            return files.iterator().next().getParentFile();
        }
        return null;
    }

    private boolean hasClassFiles(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().endsWith(".class")) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void close() {
        setVisible(false);
    }

    private void setMessage(final String msg) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                ((DefaultListModel) list.getModel()).addElement(msg);
                list.ensureIndexIsVisible(list.getModel().getSize() - 1);
            }
        });
    }

    public void setProgress(int progress) {
        progressBar.setValue(progress);
    }

    public void setProgress(int progress, String msg) {
        setProgress(progress);
        setMessage(msg);
    }

    public static class MyCellRenderer extends JLabel implements ListCellRenderer {

        public MyCellRenderer() {
            setOpaque(true);
        }

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            setText(value.toString());
            return this;
        }
    }
}
