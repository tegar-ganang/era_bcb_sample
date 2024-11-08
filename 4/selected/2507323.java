package ti.plato.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import ti.mcore.Environment;
import ti.mcore.u.io.ZipUtil;
import ti.mcore.u.log.PlatoLogger;
import ti.plato.application.ApplicationActionBarAdvisor;
import ti.plato.application.PlatoPlugin;
import ti.plato.constants.Constants;
import ti.plato.ui.u.PluginUtil;
import ti.plato.ui.views.manager.util.PersistenceTool;

public class WorkspaceManagement {

    private static final PlatoLogger LOGGER = PlatoLogger.getLogger(WorkspaceManagement.class);

    /** Extension for zipped file names */
    private static final String ZIP_EXT = ".pto";

    private static final String DEFAULT_NAME = "Untitled";

    private static final String PROP_EXIT_CODE = "eclipse.exitcode";

    private static final String PROP_EXIT_DATA = "eclipse.exitdata";

    private static final String PROP_VM = "eclipse.vm";

    private static final String PROP_VMARGS = "eclipse.vmargs";

    private static final String NEW_LINE = "\n";

    private static final String PROP_COMMANDS = "eclipse.commands";

    private static final String CMD_VMARGS = "-vmargs";

    private static final String METADATA_FOLDER = ".metadata";

    private static final String VERSION_FILENAME = "version.ini";

    private static final String WORKSPACE_VERSION_KEY = "org.eclipse.core.runtime";

    private String currentWorkspaceFileNameShort = null;

    private String currentWorkspaceFileNameFull = null;

    private String currentWorkspaceDirectory = null;

    private String lastFileDialogPath = null;

    private boolean projectModified = false;

    private static final String SEP = File.separator;

    private static WorkspaceManagement workspaceManagement = new WorkspaceManagement();

    private boolean autoSaveWorkSpace = false;

    public static WorkspaceManagement getDefault() {
        return workspaceManagement;
    }

    private WorkspaceManagement() {
    }

    public void setAutoSave(boolean autoSave) {
        autoSaveWorkSpace = autoSave;
    }

    public void exit() {
        setAutoSave(true);
        ApplicationActionBarAdvisor.getDefault().close();
    }

    public void markForTrashing() {
        if (currentWorkspaceDirectory == null) return;
        String id = currentWorkspaceDirectory + SEP + "trash.txt";
        File idFile = new File(id);
        try {
            idFile.createNewFile();
        } catch (IOException e) {
            LOGGER.logError(e);
        }
    }

    public void deleteResidualWorkspaceDirectories() {
        long t = System.currentTimeMillis();
        String workspaceDir = Environment.getEnvironment().getUserDirectory().getPath() + SEP + "Workspace";
        File workspaceFile = new File(workspaceDir);
        String[] workspaceList = workspaceFile.list();
        if (workspaceList == null) return;
        int workspaceListLength = workspaceList.length;
        if (workspaceListLength == 0) return;
        for (int workspaceListIndex = 0; workspaceListIndex < workspaceListLength; workspaceListIndex++) {
            if (!(new File(workspaceDir + SEP + workspaceList[workspaceListIndex] + SEP + "trash.txt").exists())) continue;
            deleteDirectory(workspaceDir + SEP + workspaceList[workspaceListIndex]);
            File newFile = new File(workspaceDir + SEP + workspaceList[workspaceListIndex]);
            newFile.delete();
        }
        t = System.currentTimeMillis() - t;
        LOGGER.dbg("deleting trash directories: " + t + "ms");
    }

    private void deleteDirectory(String dataDirectory) {
        File dataContent = new File(dataDirectory);
        String[] filelist = dataContent.list();
        int dataContentLength = filelist.length;
        for (int dataContentIndex = 0; dataContentIndex < dataContentLength; dataContentIndex++) {
            File newFile = new File(dataDirectory + SEP + filelist[dataContentIndex]);
            if (newFile.isDirectory()) deleteDirectory(newFile.getPath());
            if (!newFile.delete()) {
                LOGGER.dbg(newFile.getPath());
            }
        }
    }

    public void setProjectModified() {
        if (projectModified != true) {
            projectModified = true;
            updateTitleBar(null, null);
        }
    }

    public String getCurrentWorkspaceDirectory() {
        return currentWorkspaceDirectory;
    }

    public void setCurrentWorkspaceDirectory(String value) {
        currentWorkspaceDirectory = value;
        File currentWorkspaceDirectoryFile = new File(currentWorkspaceDirectory);
        currentWorkspaceDirectoryFile.mkdirs();
        String id = currentWorkspaceDirectory + SEP + "id.txt";
        File idFile = new File(id);
        try {
            PlatoPlugin.logInfo("idFile=" + idFile + "(" + idFile.exists() + ")", null);
            idFile.createNewFile();
        } catch (IOException e) {
            LOGGER.logError(e);
        }
        InitAll.getDefault().setCurrentWorkspaceDirectory(value);
    }

    public boolean doYouWantToSave(Shell shell) {
        if (!projectModified) return true;
        if (autoSaveWorkSpace) {
            return (saveFileWorkspace(shell, true) != null);
        }
        MessageBox mbox = new MessageBox(shell, SWT.YES | SWT.NO | SWT.CANCEL | SWT.ICON_QUESTION | SWT.APPLICATION_MODAL);
        mbox.setText("Question");
        mbox.setMessage("Do you want to save your workspace?");
        int mresult = mbox.open();
        if (mresult == SWT.NO) return true;
        if (mresult == SWT.CANCEL) return false;
        return (saveFileWorkspace(shell, true) != null);
    }

    private boolean titleBarLiveFeed = true;

    private String titleBarLogFile = null;

    public void updateTitleBar(final Boolean titleBarLiveFeed, final String titleBarLogFile) {
        if (Display.getCurrent() == null) {
            Display.getDefault().asyncExec(new Runnable() {

                public void run() {
                    updateTitleBarImpl(titleBarLiveFeed, titleBarLogFile);
                }
            });
        } else {
            updateTitleBarImpl(titleBarLiveFeed, titleBarLogFile);
        }
    }

    private void updateTitleBarImpl(Boolean titleBarLiveFeed, String titleBarLogFile) {
        LOGGER.dbg("updateTitleBarImpl(" + titleBarLiveFeed + ", " + titleBarLogFile + ")");
        if (titleBarLiveFeed != null) {
            this.titleBarLiveFeed = titleBarLiveFeed;
        }
        if (titleBarLogFile != null) {
            File file = new File(titleBarLogFile);
            this.titleBarLogFile = file.getName();
        }
        if (currentWorkspaceFileNameShort == null) {
            String vmargs = System.getProperty("plato.open.path");
            if (vmargs != null) {
                currentWorkspaceFileNameFull = vmargs;
                currentWorkspaceFileNameShort = vmargs.substring(vmargs.lastIndexOf(SEP) + 1, vmargs.length());
            }
        }
        String displayText = "";
        String currentVersion = PlatoPlugin.getVersion();
        String titleBar = Constants.pluginTitle.replace("<version>", currentVersion);
        if (currentWorkspaceFileNameShort == null) displayText += titleBar + " - " + DEFAULT_NAME + ZIP_EXT; else displayText += titleBar + " - " + currentWorkspaceFileNameShort;
        if (this.titleBarLiveFeed) displayText += " - " + "Live Feed"; else displayText += " - " + this.titleBarLogFile;
        if (projectModified) displayText += " (*)";
        LOGGER.dbg("displayText=" + displayText);
        IWorkbenchWindow ww = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        Shell s = (ww == null) ? null : ww.getShell();
        if (s == null) {
            s = PluginUtil.getShell();
        }
        s.setText(displayText);
    }

    public String openFileWorkspace(String fileNameFull) {
        String newDirectoryWorkspace = createDirectoryWorkspace();
        boolean unzipResult = ZipUtil.unzip(fileNameFull, newDirectoryWorkspace);
        LOGGER.dbg("--- using workspace: " + fileNameFull + " (" + unzipResult + ")");
        if (!unzipResult) {
            Display display = Display.getDefault();
            Shell shell = new Shell(display, SWT.ON_TOP);
            MessageBox mbox = new MessageBox(shell, SWT.OK | SWT.ICON_WARNING | SWT.APPLICATION_MODAL);
            mbox.setText("Warning");
            mbox.setMessage("The default workspace is corrupted, PLATO will repair it using default settings.\n" + fileNameFull);
            mbox.open();
            shell.dispose();
            newDirectoryWorkspace = createDirectoryWorkspace();
        }
        File logFile = new File(newDirectoryWorkspace + SEP + ".metadata" + SEP + ".log");
        if (logFile.exists()) logFile.delete();
        return newDirectoryWorkspace;
    }

    public String openFileWorkspace(Shell shell) {
        FileDialog dialog = new FileDialog(PluginUtil.getShell(), SWT.OPEN | SWT.SYSTEM_MODAL);
        dialog.setText("Open Workspace File...");
        dialog.setFilterPath(".");
        dialog.setFilterExtensions(new String[] { "*" + ZIP_EXT });
        dialog.setFilterNames(new String[] { "PLATO Workspace Files (*" + ZIP_EXT + ")" });
        if (lastFileDialogPath == null) dialog.setFilterPath(System.getProperty("user.home") + SEP + "Desktop"); else dialog.setFilterPath(lastFileDialogPath);
        String name = dialog.open();
        if (name == null || name.compareTo("") == 0) {
            return null;
        }
        lastFileDialogPath = dialog.getFilterPath();
        return openFileWorkspace(name);
    }

    /**
	 * Check if the current PLATO can understand the workspace at the 
	 * specified path.  Note that the path is a workspace directory,
	 * or a .pto file...
	 * 
	 * @param path
	 * @return
	 */
    public boolean isUpdateRequired(String path) {
        String remoteVersion = readWorkspaceVersion(path);
        if (remoteVersion == null) return true;
        String currentVersion = PlatoPlugin.getVersion();
        String[] currentVersionArray = currentVersion.split(SEP + ".");
        if (currentVersionArray.length != 3) return false;
        int currentVersionMilestone = Integer.parseInt(currentVersionArray[0]);
        int currentVersionRevision = Integer.parseInt(currentVersionArray[1]);
        int currentVersionPatch = Integer.parseInt(currentVersionArray[2]);
        String[] remoteVersionArray = remoteVersion.split(SEP + ".");
        if (remoteVersionArray.length != 3) return false;
        int remoteVersionMilestone = Integer.parseInt(remoteVersionArray[0]);
        int remoteVersionRevision = Integer.parseInt(remoteVersionArray[1]);
        int remoteVersionPatch = Integer.parseInt(remoteVersionArray[2]);
        boolean isUpdateRequired = false;
        if (currentVersionMilestone < remoteVersionMilestone) isUpdateRequired = true; else if (currentVersionMilestone == remoteVersionMilestone) {
            if (currentVersionRevision < remoteVersionRevision) isUpdateRequired = true; else if (currentVersionRevision == remoteVersionRevision) {
                if (currentVersionPatch < remoteVersionPatch) isUpdateRequired = true;
            }
        }
        return isUpdateRequired;
    }

    public void saveFileWorkspaceDefault() {
        String currentVersion = PlatoPlugin.getVersion();
        String hostDir = Constants.defaultPtoSubdir + currentVersion + "/";
        (new File(hostDir)).mkdir();
        String path = hostDir;
        if (developerCalling()) path += Constants.defaultPtoNameDev; else path += Constants.defaultPtoName;
        saveFileWorkspace(path);
    }

    public void saveFileWorkspace(String fullPath) {
        LOGGER.dbg("saveFileWorkspace(" + fullPath + ")");
        if (currentWorkspaceDirectory == null) return;
        LOGGER.dbg("going to persist");
        try {
            ResourcesPlugin.getWorkspace().save(true, null);
        } catch (CoreException e) {
            LOGGER.logError(e);
        }
        PersistenceTool.persistViews();
        PersistenceTool.persistPerspectives();
        LOGGER.dbg("done persisting");
        Location instanceLoc = Platform.getInstanceLocation();
        instanceLoc.release();
        try {
            ZipUtil.zip(fullPath, currentWorkspaceDirectory, ZipUtil.STORE_PATH_FROM_ZIP_ROOT);
            LOGGER.dbg("done saveFileWorkspace(" + fullPath + ")");
        } catch (Exception e) {
            LOGGER.logError(e);
        }
        try {
            instanceLoc.lock();
        } catch (IOException e) {
            LOGGER.logError(e);
        }
    }

    public void setWorkspaceFileName(String name) {
        currentWorkspaceFileNameFull = name;
        currentWorkspaceFileNameShort = new File(currentWorkspaceFileNameFull).getName();
    }

    public String saveFileWorkspace(Shell shell, boolean forceSave) {
        if (!forceSave || currentWorkspaceFileNameShort == null) {
            FileDialog dialog = new FileDialog(PluginUtil.getShell(), SWT.SAVE | SWT.SYSTEM_MODAL);
            dialog.setText("Save As...");
            dialog.setFilterExtensions(new String[] { "*" + ZIP_EXT });
            dialog.setFilterNames(new String[] { "PLATO Workspace Files (*" + ZIP_EXT + ")" });
            if (currentWorkspaceFileNameShort == null) dialog.setFileName(DEFAULT_NAME + ZIP_EXT); else dialog.setFileName(currentWorkspaceFileNameShort);
            if (lastFileDialogPath == null) dialog.setFilterPath(System.getProperty("user.home") + SEP + "Desktop"); else dialog.setFilterPath(lastFileDialogPath);
            final String name = dialog.open();
            if (name == null || name.compareTo("") == 0) {
                return null;
            }
            File file = new File(name);
            if (file.exists()) {
                MessageBox mbox = new MessageBox(shell, SWT.OK | SWT.CANCEL | SWT.ICON_WARNING | SWT.APPLICATION_MODAL);
                mbox.setText("Warning");
                mbox.setMessage("This file already exists. Do you want to overwrite it?");
                if (mbox.open() == SWT.CANCEL) return null;
            }
            currentWorkspaceFileNameShort = dialog.getFileName();
            updateTitleBar(null, null);
            lastFileDialogPath = dialog.getFilterPath();
            currentWorkspaceFileNameFull = name;
        }
        saveFileWorkspace(currentWorkspaceFileNameFull);
        projectModified = false;
        updateTitleBar(null, null);
        return currentWorkspaceFileNameFull;
    }

    public String createDirectoryWorkspace() {
        String workspaceDir = Environment.getEnvironment().getUserDirectory().getPath() + SEP + "Workspace";
        Date date = new Date();
        String strDate = date.toString();
        strDate = strDate.replace(":", "");
        workspaceDir = workspaceDir + SEP + strDate;
        File workspaceFile = new File(workspaceDir);
        workspaceFile.mkdirs();
        String id = workspaceDir + SEP + "id.txt";
        File idFile = new File(id);
        try {
            idFile.createNewFile();
        } catch (IOException e) {
            LOGGER.logError(e);
        }
        return workspaceDir;
    }

    public void handlePreviousSessionsLogFiles(boolean previousSession) {
        String workspaceDir = Environment.getEnvironment().getUserDirectory().getPath() + SEP + "Workspace";
        if (developerCalling() && currentWorkspaceDirectory != null) {
            String workspaceDir2 = currentWorkspaceDirectory.substring(0, currentWorkspaceDirectory.lastIndexOf(SEP));
            if (workspaceDir2.compareTo(workspaceDir) != 0) {
                workspaceDir = workspaceDir2;
            }
        }
        File workspaceFile = new File(workspaceDir);
        String[] workspaceList = workspaceFile.list();
        if (workspaceList == null) return;
        int workspaceListLength = workspaceList.length;
        if (workspaceListLength == 0) return;
        ArrayList logList = new ArrayList();
        ArrayList logList2 = new ArrayList();
        for (int workspaceListIndex = 0; workspaceListIndex < workspaceListLength; workspaceListIndex++) {
            if (!(new File(workspaceDir + SEP + workspaceList[workspaceListIndex] + SEP + "id.txt").exists())) continue;
            String logPath = workspaceDir + SEP + workspaceList[workspaceListIndex] + SEP + ".metadata" + SEP + ".log";
            if (!(new File(logPath).exists())) continue;
            String remoteVersion = WorkspaceManagement.readWorkspaceVersion(workspaceDir + SEP + workspaceList[workspaceListIndex]);
            if (remoteVersion == null) continue;
            logList.add(workspaceList[workspaceListIndex]);
            Date date = new Date();
            String strDate = date.toString();
            strDate = strDate.replace(":", "");
            logList2.add("v" + remoteVersion + "_" + System.getProperty("user.name") + "_" + strDate);
        }
        int logListLength = logList.size();
        if (logListLength == 0) return;
        String lastDestination = null;
        boolean lastException = false;
        for (int logListIndex = 0; logListIndex < logListLength; logListIndex++) {
            String source = workspaceDir + SEP + (String) logList.get(logListIndex) + SEP + ".metadata" + SEP + ".log";
            String destination = workspaceDir + SEP + (String) logList2.get(logListIndex) + ".log";
            String testContent = readContent(source);
            if (testContent != null && (testContent.contains("xception"))) {
                lastException = true;
            } else {
                lastException = false;
            }
            lastDestination = destination;
            File sourceFile = new File(source);
            File destinationFile = new File(destination);
            try {
                copyFile(sourceFile, destinationFile);
            } catch (Exception e) {
                LOGGER.logError(e);
            }
            sourceFile.delete();
        }
        File pFileConnected = new File("\\\\sandshare\\db\\plato");
        if (!pFileConnected.exists()) {
            if (lastException && developerCalling()) {
                String message = "";
                if (previousSession) message = "Previous sessions of PLATO have generated error log files.\nWould you like to see them?"; else message = "PLATO has generated error log files.\nWould you like to see them?";
                boolean yes = Environment.getEnvironment().showQuestionMessage(message);
                if (!yes) return;
                PluginUtil.openInSystemEditor(lastDestination);
            }
            return;
        }
        File pFile = null;
        if (developerCalling()) pFile = new File("\\\\sandshare\\db\\plato\\Log\\Developer"); else pFile = new File("\\\\sandshare\\db\\plato\\Log\\User");
        if (!pFile.exists()) pFile.mkdirs();
        if (!pFile.exists()) return;
        String lastSource = null;
        for (int logListIndex = 0; logListIndex < logListLength; logListIndex++) {
            String source = workspaceDir + SEP + (String) logList2.get(logListIndex) + ".log";
            lastSource = source;
            String destination = pFile.getPath() + SEP + (String) logList2.get(logListIndex) + ".log";
            File sourceFile = new File(source);
            File destinationFile = new File(destination);
            try {
                copyFile(sourceFile, destinationFile);
            } catch (Exception e) {
                LOGGER.logError(e);
            }
        }
        if (lastException && developerCalling()) {
            String message = "";
            if (previousSession) message = "Previous sessions of PLATO have generated error log files.\nWould you like to see them?"; else message = "PLATO has generated error log files.\nWould you like to see them?";
            boolean yes = Environment.getEnvironment().showQuestionMessage(message);
            if (!yes) return;
            PluginUtil.openInSystemEditor(lastSource);
            return;
        }
    }

    public void copyFile(File in, File out) throws Exception {
        FileInputStream fis = new FileInputStream(in);
        FileOutputStream fos = new FileOutputStream(out);
        byte[] buf = new byte[1024];
        int i = 0;
        while ((i = fis.read(buf)) != -1) {
            fos.write(buf, 0, i);
        }
        fis.close();
        fos.close();
    }

    private static String readContent(String pathFile) {
        FileInputStream iniStream = null;
        try {
            iniStream = new FileInputStream(pathFile);
        } catch (FileNotFoundException e) {
            Environment.getEnvironment().unhandledException(e);
            return null;
        }
        int iniLength = 0;
        try {
            iniLength = iniStream.available();
        } catch (IOException e) {
            Environment.getEnvironment().unhandledException(e);
        }
        if (iniLength == 0) return null;
        byte buffer[] = new byte[iniLength];
        try {
            iniStream.read(buffer);
        } catch (IOException e) {
            Environment.getEnvironment().unhandledException(e);
            return null;
        }
        String iniContent = new String(buffer);
        try {
            iniStream.close();
        } catch (IOException e) {
            Environment.getEnvironment().unhandledException(e);
        }
        return iniContent;
    }

    /**
	 * The version file is stored in the metadata area of the workspace. This
	 * method returns an URL to the file or null if the directory or file does
	 * not exist (and the create parameter is false).
	 * 
	 * @param create
	 *            If the directory and file does not exist this parameter
	 *            controls whether it will be created.
	 * @return An url to the file or null if the version file does not exist or
	 *         could not be created.
	 */
    private static File getVersionFile(String workspacePath, boolean create) {
        try {
            File metaDir = new File(workspacePath, METADATA_FOLDER);
            if (!metaDir.exists() && (!create || !metaDir.mkdir())) return null;
            File versionFile = new File(metaDir, VERSION_FILENAME);
            if (!versionFile.exists() && (!create || !versionFile.createNewFile())) return null;
            return versionFile;
        } catch (IOException e) {
            return null;
        }
    }

    /**
	 * Look at the argument URL for the workspace's version information. Return
	 * that version if found and null otherwise.   Note that the workspace
	 * path could be an unzipped workspace (ie. a directory) or a workspace
	 * file (*.pto).
	 */
    public static String readWorkspaceVersion(String workspacePath) {
        ZipFile zipFile = null;
        try {
            InputStream is = null;
            File workspaceFile = new File(workspacePath);
            if (!workspaceFile.exists()) return null;
            if (workspaceFile.isDirectory()) {
                File versionFile = getVersionFile(workspacePath, false);
                if ((versionFile != null) && versionFile.exists()) {
                    is = new FileInputStream(versionFile);
                }
            } else {
                zipFile = new ZipFile(workspaceFile);
                ZipEntry zipEntry = zipFile.getEntry(METADATA_FOLDER + "/" + VERSION_FILENAME);
                if (zipEntry == null) zipEntry = zipFile.getEntry(METADATA_FOLDER + "\\" + VERSION_FILENAME);
                if (zipEntry != null) is = zipFile.getInputStream(zipEntry);
            }
            if (is == null) return null;
            Properties props = new Properties();
            try {
                props.load(is);
            } finally {
                is.close();
            }
            return props.getProperty(WORKSPACE_VERSION_KEY);
        } catch (IOException e) {
            return null;
        } finally {
            try {
                if (zipFile != null) zipFile.close();
            } catch (IOException e) {
                Environment.getEnvironment().unhandledException(e);
            }
        }
    }

    /**
	 * Write the version of the metadata into a known file overwriting any
	 * existing file contents. Writing the version file isn't really crucial,
	 * so the function is silent about failure
	 */
    public static void writeWorkspaceVersion(String workspacePath) {
        File versionFile = getVersionFile(workspacePath, true);
        if (versionFile == null) return;
        OutputStream output = null;
        try {
            String versionLine = WORKSPACE_VERSION_KEY + '=' + PlatoPlugin.getVersion();
            output = new FileOutputStream(versionFile);
            output.write(versionLine.getBytes("UTF-8"));
        } catch (IOException e) {
        } finally {
            try {
                if (output != null) output.close();
            } catch (IOException e) {
            }
        }
    }

    /**
	 * Check workspace compatibility between two PLATO version strings.
	 * Conceptually, this performs the following operation:
	 * <pre>
	 *      currentVersionStr >= minimumVersionStr
	 * </pre>
	 * Note that there is some similar logic in the launcher, in Form1.cs,
	 * so if this changes you probably want to go update that code too.
	 * 
	 * @return <code>true</code> if the <code>currentVersionStr</code> 
	 *   version of PLATO is sufficient to open a workspace file created 
	 *   by the <code>minimumVersionStr</code> version of PLATO
	 */
    public static boolean checkVersion(String currentVersionStr, String minimumVersionStr) {
        if (currentVersionStr.equals(minimumVersionStr)) return true;
        int[] currentVersion = parseVersion(currentVersionStr);
        int[] minimumVersion = parseVersion(minimumVersionStr);
        if (currentVersion == null) currentVersion = parseVersion(currentVersionStr.substring(0, currentVersionStr.lastIndexOf('.')));
        if ((currentVersion == null) || (minimumVersion == null)) return false;
        for (int i = 0; i < 3; i++) {
            if (currentVersion[i] == -1 || minimumVersion[i] == -1) continue; else if (currentVersion[i] > minimumVersion[i]) return true; else if (currentVersion[i] < minimumVersion[i]) return false;
        }
        return true;
    }

    private static int[] parseVersion(String version) {
        int[] detectedVersion = new int[3];
        String[] matcher = version.split("\\.");
        if (matcher.length != 3) return null;
        try {
            for (int i = 0; i < 3; i++) {
                detectedVersion[i] = Integer.parseInt(matcher[i]);
            }
        } catch (Throwable t) {
            Environment.getEnvironment().unhandledException(t);
            return null;
        }
        return detectedVersion;
    }

    private String edsFile = null;

    public void setEds(String edsPath) {
        edsFile = edsPath;
    }

    public String getEds() {
        return edsFile;
    }

    private boolean developerCalling() {
        return PluginUtil.developerCalling();
    }

    public void restartPLATO(String path) {
        String command_line = buildCommandLine(path);
        if (command_line == null) return;
        System.setProperty(PROP_EXIT_CODE, Integer.toString(24));
        System.setProperty(PROP_EXIT_DATA, command_line);
        Shell shell = new Shell(PlatformUI.getWorkbench().getDisplay(), SWT.ON_TOP);
        MessageBox mbox = new MessageBox(shell, SWT.OK | SWT.CANCEL | SWT.ICON_INFORMATION | SWT.APPLICATION_MODAL);
        mbox.setText("Information");
        mbox.setMessage("PLATO will restart with your workspace.");
        int mresult = mbox.open();
        if (mresult == SWT.CANCEL) return;
        shell.dispose();
        PlatformUI.getWorkbench().restart();
    }

    /**
	 * Create and return a string with command line options for eclipse.exe that
	 * will launch a new workbench that is the same as the currently running
	 * one, but using the argument directory as its workspace.
	 * 
	 * @param workspace
	 *            the directory to use as the new workspace
	 * @return a string of command line options or null on error
	 */
    private String buildCommandLine(String workspace) {
        if (PluginUtil.developerCalling()) {
            ti.mcore.Environment.getEnvironment().showErrorMessage("This functionality can only be used from the installed version of PLATO.");
            return null;
        }
        StringBuffer result = new StringBuffer(512);
        String property = System.getProperty(PROP_VM);
        result.append(property);
        result.append(NEW_LINE);
        if (workspace != null) {
            result.append("-Dplato.workspace=" + workspace);
            result.append(NEW_LINE);
        } else {
            result.append(NEW_LINE);
            workspace = createDirectoryWorkspace();
        }
        String vmargs = System.getProperty(PROP_VMARGS);
        if (vmargs != null) result.append(vmargs);
        property = System.getProperty(PROP_COMMANDS);
        if (property != null) {
            result.append(property);
        }
        if (vmargs != null) {
            result.append(CMD_VMARGS);
            result.append(NEW_LINE);
            result.append(vmargs);
        }
        return result.toString();
    }
}
