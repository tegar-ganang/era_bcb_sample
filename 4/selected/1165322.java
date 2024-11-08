package net.sourceforge.processdash.tool.bridge.impl;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.tool.bridge.client.BridgedWorkingDirectory;
import net.sourceforge.processdash.tool.bridge.client.WorkingDirectory;
import net.sourceforge.processdash.ui.lib.ExceptionDialog;
import net.sourceforge.processdash.ui.lib.SwingWorker;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.RobustFileOutputStream;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.lock.FileConcurrencyLock;
import net.sourceforge.processdash.util.lock.LockFailureException;

/**
 * This class assists an individual with the task of migrating their personal
 * data (stored on their local hard drive) into the Enterprise Server.
 */
public class DatasetAutoMigrator {

    public static final String ENABLED_SYSPROP = DatasetAutoMigrator.class.getName() + ".enabled";

    public static final String FORCE_MIGRATE = "force";

    public interface DialogParentSource {

        Component getDialogParent();
    }

    /**
     * Possibly migrate a preexisting dataset into the bridged dataset that
     * makes up the current working directory.
     * 
     * When a user first opens their personal dataset from the PDES, and that
     * personal dataset is empty, a special system property is set.  This
     * class checks for that system property and potentially initiates an
     * operation to import their preexisting historical data into the server.
     * 
     * This method could potentially call System.exit() if the user asks not
     * to continue.
     * 
     * @return true if data was migrated, false otherwise.
     */
    public static boolean maybeRun(WorkingDirectory workingDir, DialogParentSource dps) {
        if (!StringUtils.hasValue(System.getProperty(ENABLED_SYSPROP))) return false;
        if (Settings.getVal(ALREADY_MIGRATED_SETTING) != null) return false;
        if (!(workingDir instanceof BridgedWorkingDirectory)) return false;
        if (Settings.isReadOnly()) return false;
        try {
            workingDir.assertWriteLock();
        } catch (Exception e) {
            return false;
        }
        File sourceDir = getDirectoryToMigrate();
        if (sourceDir == null) return false;
        return new DatasetAutoMigrator((BridgedWorkingDirectory) workingDir, sourceDir, dps).run();
    }

    private static File getDirectoryToMigrate() {
        Preferences prefs = Preferences.userRoot().node(USER_VALUES_PREFS_NODE);
        String userDataDirName = prefs.get(DATA_PATH, null);
        if (!StringUtils.hasValue(userDataDirName)) return null;
        File userDataDir = new File(userDataDirName);
        if (!userDataDir.isDirectory()) return null;
        for (String filename : EXPECTED_FILES) if (new File(userDataDir, filename).isFile() == false) return null;
        File teamServerFile = new File(userDataDir, TeamServerPointerFile.FILE_NAME);
        if (teamServerFile.exists()) return null;
        return userDataDir;
    }

    private BridgedWorkingDirectory workingDir;

    private File sourceDir;

    private DialogParentSource dps;

    private FileConcurrencyLock sourceDirLock;

    private ResourceBundle resources;

    public DatasetAutoMigrator(BridgedWorkingDirectory workingDir, File sourceDir, DialogParentSource dps) {
        this.workingDir = workingDir;
        this.sourceDir = sourceDir;
        this.dps = dps;
        this.sourceDirLock = new FileConcurrencyLock(new File(sourceDir, DashboardInstanceStrategy.LOCK_FILE_NAME));
        this.resources = ResourceBundle.getBundle("Templates.resources.ProcessDashboard");
    }

    private boolean run() {
        if (promptUserToConfirmMigration()) {
            new MigrateDataWorker().start();
            return true;
        } else {
            if (promptUserToContinueAndCreateDataset() == false) exitApplication();
            return false;
        }
    }

    private class MigrateDataWorker extends SwingWorker {

        JDialog progressDialog;

        @Override
        public void start() {
            buildProgressDialog();
            super.start();
            progressDialog.setVisible(true);
        }

        private void buildProgressDialog() {
            progressDialog = new JDialog((Frame) null, getRes("Migrate.Progress.Title"), true);
            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);
            JPanel p = new JPanel(new BorderLayout(10, 10));
            p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            p.add(new JLabel(getRes("Migrate.Progress.Message")), BorderLayout.NORTH);
            p.add(progressBar, BorderLayout.CENTER);
            progressDialog.getContentPane().add(p);
            progressDialog.pack();
            progressDialog.setLocationRelativeTo(null);
        }

        @Override
        public Object construct() {
            migrateData();
            return null;
        }

        @Override
        public void finished() {
            progressDialog.dispose();
        }
    }

    private void migrateData() {
        try {
            ensureDirectoryIsNotInUse();
            copyDataFiles();
            updateSettings();
            flushWorkingData();
            finalizeOldDataDirectory();
        } catch (IOException ioe) {
            workingDir.clearSyncTimestamp();
            showMigrationExceptionDialog(ioe);
            exitApplication();
        }
    }

    private void ensureDirectoryIsNotInUse() {
        while (true) {
            try {
                sourceDirLock.acquireLock("Dataset Auto Migrator");
                return;
            } catch (LockFailureException lfe) {
                if (promptUserToCloseRunningApplication() == false) exitApplication();
            }
        }
    }

    private void copyDataFiles() throws IOException {
        File destDir = workingDir.getDirectory();
        List<String> files = FileUtils.listRecursively(sourceDir, DashboardInstanceStrategy.INSTANCE.getFilenameFilter());
        for (String filename : files) {
            File srcFile = new File(sourceDir, filename);
            File destFile = new File(destDir, filename);
            RobustFileOutputStream out = new RobustFileOutputStream(destFile);
            FileUtils.copyFile(srcFile, out);
            out.close();
        }
    }

    private void updateSettings() {
        InternalSettings.maybeReload();
        InternalSettings.set(ALREADY_MIGRATED_SETTING, Long.toString(System.currentTimeMillis()));
    }

    private void flushWorkingData() {
        try {
            workingDir.setOfflineLockEnabled(true);
        } catch (Exception e) {
        }
        try {
            workingDir.flushData();
        } catch (Exception e) {
        }
    }

    private void finalizeOldDataDirectory() {
        writeTeamServerPointerFile();
        writeMarkerFile();
        makeFilesReadOnly();
        sourceDirLock.releaseLock(true);
    }

    private void writeTeamServerPointerFile() {
        try {
            TeamServerPointerFile f = new TeamServerPointerFile(sourceDir);
            f.addServerEntry(workingDir.getDescription());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void writeMarkerFile() {
        try {
            File marker = new File(sourceDir, MARKER_FILE);
            PrintWriter out = new PrintWriter(new FileWriter(marker));
            out.println("This directory is obsolete.  The data it contains");
            out.println("has been moved to the server at");
            out.println(workingDir.getDescription());
            out.println();
            out.println("The contents of this directory have been left intact");
            out.println("while affected users transition to the new directory.");
            out.println("After all users have transitioned, this directory");
            out.println("can be safely deleted.");
            out.flush();
            out.close();
        } catch (IOException e) {
        }
    }

    protected void makeFilesReadOnly() {
        for (File f : sourceDir.listFiles()) f.setReadOnly();
    }

    private boolean promptUserToConfirmMigration() {
        if (FORCE_MIGRATE.equals(System.getProperty(ENABLED_SYSPROP))) return true;
        String title = getRes("Migrate.Confirm.Title");
        String messageFmt = getRes("Migrate.Confirm.Message_FMT");
        String[] message = MessageFormat.format(messageFmt, sourceDir.getPath()).split("\n");
        String yesOption = getRes("Migrate.Options.Yes_Recommended");
        String noOption = getRes("Migrate.Options.No");
        String[] options = new String[] { yesOption, noOption };
        int userChoice = JOptionPane.showOptionDialog(dp(), message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, yesOption);
        return (userChoice == 0);
    }

    private boolean promptUserToContinueAndCreateDataset() {
        String title = getRes("Migrate.Create.Title");
        String[] message = getRes("Migrate.Create.Message").split("\n");
        String yesOption = getRes("Migrate.Options.Yes");
        String noOption = getRes("Migrate.Options.No_Recommended");
        String[] options = new String[] { yesOption, noOption };
        int userChoice = JOptionPane.showOptionDialog(dp(), message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, noOption);
        return (userChoice == 0);
    }

    private boolean promptUserToCloseRunningApplication() {
        String title = getRes("Migrate.Close.Title");
        String[] message = getRes("Migrate.Close.Message").split("\n");
        int userChoice = JOptionPane.showConfirmDialog(dp(), message, title, JOptionPane.OK_CANCEL_OPTION);
        return (userChoice == JOptionPane.OK_OPTION);
    }

    private void showMigrationExceptionDialog(IOException ioe) {
        String title = getRes("Migrate.Error.Title");
        String[] message = getRes("Migrate.Error.Message").split("\n");
        ExceptionDialog.show(dp(), title, message, ioe);
    }

    private Component dp() {
        if (dps != null) return dps.getDialogParent(); else return null;
    }

    private String getRes(String key) {
        return resources.getString(key);
    }

    private static void exitApplication() {
        System.exit(0);
    }

    private static final String USER_VALUES_PREFS_NODE = "/net/sourceforge/processdash/installer";

    private static final String DATA_PATH = "DATA_PATH";

    private static final String[] EXPECTED_FILES = { "global.dat", "0.dat", "timelog.xml" };

    private static final String MARKER_FILE = "00-This-Directory-is-Obsolete.txt";

    private static final String ALREADY_MIGRATED_SETTING = "autoMigrated.date";
}
