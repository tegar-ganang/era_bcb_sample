package net.sourceforge.processdash;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import net.sourceforge.processdash.tool.bridge.client.WorkingDirectory;
import net.sourceforge.processdash.tool.export.mgr.ExternalResourceManager;
import net.sourceforge.processdash.ui.ConsoleWindow;
import net.sourceforge.processdash.util.DashboardBackupFactory;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.ProfTimer;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.TempFileFactory;
import net.sourceforge.processdash.util.ThreadThrottler;

/** Backup data and other files automatically.
 *
 * We want to back up data files (*.dat), defect logs (*.def), the time log
 * (time.log), the state file (state), user settings (pspdash.ini), and
 * the error log (log.txt).
 *
 * Do this each time the dashboard starts or shuts down.
 * Also do it at periodically, as configured by the user.
 */
public class FileBackupManager {

    public static final int STARTUP = 0;

    public static final int RUNNING = 1;

    public static final int SHUTDOWN = 2;

    public static final String BACKUP_TIMES_SETTING = "backup.timesOfDay";

    private WorkingDirectory workingDirectory;

    private OutputStream logFile = null;

    private static final String LOG_FILE_NAME = "log.txt";

    private static final Logger logger = Logger.getLogger(FileBackupManager.class.getName());

    public FileBackupManager(WorkingDirectory workingDirectory) {
        this.workingDirectory = workingDirectory;
        DashboardBackupFactory.setMaxHistLogSize(Settings.getInt("logging.maxHistLogSize", 500000));
        DashboardBackupFactory.setKeepBackupsNumDays(Settings.getInt("backup.keepBackupsNumDays", 42));
    }

    public void maybeRun(int when, String who) {
        if (Settings.isReadOnly()) return;
        if (loggingEnabled() && when == SHUTDOWN) stopLogging();
        if (Settings.getBool("backup.enabled", true)) {
            ProfTimer pt = new ProfTimer(FileBackupManager.class, "FileBackupManager.run");
            try {
                runImpl(when, who, false);
            } catch (Throwable t) {
            }
            pt.click("Finished backup");
        }
        if (loggingEnabled() && when == STARTUP) startLogging(workingDirectory.getDirectory());
    }

    public File run() {
        ProfTimer pt = new ProfTimer(FileBackupManager.class, "FileBackupManager.run");
        File result = null;
        try {
            result = runImpl(RUNNING, null, true);
        } catch (Throwable t) {
            printError(t);
        }
        pt.click("Finished backup");
        return result;
    }

    private synchronized File runImpl(int when, String who, boolean externalCopyDesired) {
        boolean needExternalCopy = externalCopyDesired;
        String[] extraBackupLocations = getExtraBackupLocations();
        if (StringUtils.hasValue(who) && extraBackupLocations != null) needExternalCopy = true;
        File result = null;
        try {
            URL backupURL = workingDirectory.doBackup(WHEN_STR[when]);
            if (needExternalCopy) result = createExternalizedBackupFile(backupURL);
        } catch (IOException ioe) {
            printError(ioe);
            return null;
        }
        if (result != null && who != null && extraBackupLocations != null) {
            makeExtraBackupCopies(result, who, extraBackupLocations);
        }
        return result;
    }

    private static final String[] WHEN_STR = { "startup", "checkpoint", "shutdown" };

    private File createExternalizedBackupFile(URL backup) throws IOException {
        ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(backup.openStream()));
        File result = TempFileFactory.get().createTempFile("pdash-backup", ".zip");
        result.deleteOnExit();
        ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(result)));
        ExternalResourceManager extMgr = ExternalResourceManager.getInstance();
        ZipEntry e;
        while ((e = zipIn.getNextEntry()) != null) {
            String filename = e.getName();
            if (extMgr.isArchivedItem(filename)) continue;
            ZipEntry eOut = new ZipEntry(filename);
            eOut.setTime(e.getTime());
            zipOut.putNextEntry(eOut);
            FileUtils.copyFile(zipIn, zipOut);
            zipOut.closeEntry();
        }
        zipIn.close();
        extMgr.addExternalResourcesToBackup(zipOut);
        zipOut.finish();
        zipOut.close();
        return result;
    }

    private void stopLogging() {
        if (logFile != null) try {
            ConsoleWindow.getInstalledConsole().setCopyOutputStream(null);
            logFile.flush();
            logFile.close();
        } catch (IOException ioe) {
            printError(ioe);
        }
    }

    private void startLogging(File dataDir) {
        try {
            File out = new File(dataDir, LOG_FILE_NAME);
            logFile = new FileOutputStream(out);
            ConsoleWindow.getInstalledConsole().setCopyOutputStream(logFile);
            System.out.println("Process Dashboard - logging started at " + new Date());
            System.out.println(System.getProperty("java.vendor") + " JRE " + System.getProperty("java.version") + "; " + System.getProperty("os.name"));
            System.out.println("Using " + workingDirectory);
        } catch (IOException ioe) {
            printError(ioe);
        }
    }

    private static boolean loggingEnabled() {
        return Settings.getBool("logging.enabled", true);
    }

    private static String[] getExtraBackupLocations() {
        String extraBackupDirs = InternalSettings.getExtendableVal("backup.extraDirectories", ";");
        if (!StringUtils.hasValue(extraBackupDirs)) return null; else return extraBackupDirs.replace('/', File.separatorChar).split(";");
    }

    private static void makeExtraBackupCopies(File backupFile, String who, String[] locations) {
        if (backupFile == null || who == null || who.length() == 0 || locations == null || locations.length == 0) return;
        String filename = "backup-" + FileUtils.makeSafe(who) + ".zip";
        Set<File> copies = new HashSet<File>();
        for (int i = 0; i < locations.length; i++) {
            ThreadThrottler.tick();
            File copy = null;
            File oneLocation = new File(locations[i]);
            if (oneLocation.isDirectory()) copy = new File(oneLocation, filename); else if (oneLocation.getParentFile().isDirectory()) {
                String oneName = oneLocation.getName();
                if (!oneName.toLowerCase().endsWith(".zip")) oneName = oneName + ".zip";
                if (oneName.indexOf("%date") != -1) {
                    SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
                    String now = fmt.format(new Date());
                    oneName = StringUtils.findAndReplace(oneName, "%date", now);
                }
                copy = new File(oneLocation.getParentFile(), oneName);
            }
            if (copy == null || copies.contains(copy)) continue;
            try {
                FileUtils.copyFile(backupFile, copy);
                copies.add(copy);
            } catch (Exception e) {
                System.err.println("Warning: unable to make extra backup to '" + copy + "'");
            }
        }
    }

    public static boolean inBackupSet(File dir, String name) {
        return DashboardBackupFactory.DASH_FILE_FILTER.accept(dir, name);
    }

    private static void printError(Throwable t) {
        printError("Unexpected error in FileBackupManager", t);
    }

    private static void printError(String msg, Throwable t) {
        System.err.println(msg);
        t.printStackTrace();
    }

    public static class BGTask implements Runnable {

        private DashboardContext context;

        public void setDashboardContext(DashboardContext context) {
            this.context = context;
        }

        public void run() {
            try {
                ProcessDashboard dash = (ProcessDashboard) context;
                String qualifier = ProcessDashboard.getBackupQualifier(context.getData());
                dash.fileBackupManager.maybeRun(FileBackupManager.RUNNING, qualifier);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Encountered exception when performing auto backup", e);
            }
        }
    }
}
