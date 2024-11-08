package com.topq.remotemachine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import jsystem.extensions.analyzers.text.FindText;
import jsystem.framework.report.Reporter;
import jsystem.framework.system.SystemObjectImpl;
import jsystem.utils.FileUtils;
import jsystem.utils.StringUtils;
import jsystem.utils.exec.Command;
import jsystem.utils.exec.Execute;
import com.aqua.sysobj.conn.CliCommand;
import com.aqua.sysobj.conn.CliConnectionImpl;
import com.aqua.sysobj.conn.WindowsDefaultCliConnection;

public class RemoteMachine extends SystemObjectImpl {

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_H-mm-ss");

    private static String LocalIpAddress;

    static {
        try {
            LocalIpAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
        }
    }

    public RemoteMachine() {
    }

    public RemoteMachine(String ipAddress, String machineUserName, String machinePassword) {
        this.hots = ipAddress;
        this.user = machineUserName;
        this.password = machinePassword;
        setName(this.getClass().getSimpleName());
    }

    private static final int PermmisionsTimeout = 20000;

    /**
	 * The remote unit machine IP address/name.
	 */
    protected String hots;

    /**
	 * The login user name for the remote unit machine.
	 */
    protected String user;

    /**
	 * The login password for the remote unit machine.
	 */
    protected String password;

    /**
	 * The maximum allowed connection to the remote machine.
	 */
    protected int maxConnections = 10;

    /**
	 * The temp folder to use on the remote machine.
	 */
    protected String tempFolder = "c:/temp";

    /**
	 * A cli connection to perform remote actions on the remote unit machine.
	 */
    protected CliConnectionImpl cli;

    private boolean forcefullPermission = false;

    public String getHost() {
        return hots;
    }

    /**
	 * The IP address of the remote machine.
	 * 
	 * @param host
	 *            The IP address of the remote machine.
	 */
    public void setHost(String host) {
        this.hots = host;
    }

    public String getUser() {
        return user;
    }

    /**
	 * The remote machine user name.
	 * 
	 * @param user
	 *            The remote machine user name.
	 */
    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    /**
	 * The remote machine password matching the supplied user name.
	 */
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public void init() throws Exception {
        try {
            super.init();
            allowPermissionsForcefully();
            setMaxTelnetConnections(hots, user, password, maxConnections);
            cli = new WindowsDefaultCliConnection(hots, user, password);
        } catch (Exception e) {
            analyzeException(e);
            throw e;
        }
    }

    @Override
    public void close() {
        try {
            denyPermissionsForcefully();
        } catch (Exception e) {
        }
        if (cli.isConnected()) cli.disconnect();
        super.close();
    }

    public final String executeCommand(String title, CliCommand command) throws Exception {
        if (!cli.isConnected()) cli.connect();
        cli.handleCliCommand(title, command);
        return command.getResult();
    }

    public final void clearTempFolder() throws Exception {
        report.startLevel("Clearing temp folder at: " + getTempFolder().getAbsolutePath());
        boolean printStatuses = isPrintStatuses();
        setPrintStatuses(false);
        deleteFiles("Delete Temp", getFilesListFor(getTempFolder()));
        setPrintStatuses(printStatuses);
        report.stopLevel();
    }

    /**
	 * Tests if the specified file exists on the remote machine
	 * 
	 * @param file
	 *            The file to test.
	 * @throws Exception
	 *             If something is wrong.
	 */
    public final boolean fileExists(String label, File file) throws Exception {
        if (file == null) {
            report.report(label + " - No file was specified");
            return false;
        }
        return getAsRemoteFile(file).exists();
    }

    /**
	 * Delete the specified file from the remote machine
	 * 
	 * @param file
	 *            The file to delete.
	 * @throws Exception
	 *             If something is wrong.
	 */
    public final void deleteFile(String label, File file) throws Exception {
        if (file == null) {
            report.report(label + " - No file was specified/found");
            return;
        }
        actualDeleteFile(label, file);
    }

    /**
	 * Delete the specified files from the remote machine.
	 * 
	 * @param files
	 *            The files to delete from the remote machine.
	 * @throws Exception
	 *             If something is wrong.
	 */
    public final void deleteFiles(String label, File[] files) throws Exception {
        if (files == null) {
            report.report(label + " - No files were specified/found");
            return;
        }
        for (File file : files) actualDeleteFile(label, file);
    }

    /**
	 * Performs the actual deletion of the file on the remote machine.
	 * 
	 * @param label
	 * 
	 * @param fileToDelete
	 *            The file to delete on the remote machine.
	 */
    private boolean actualDeleteFile(String label, File fileToDelete) {
        if (fileToDelete == null) {
            report.report(label + " - File was null, Should not happened");
            return false;
        }
        File rootFile = getAsRemoteFile(fileToDelete);
        boolean deleted = false;
        if (rootFile.isDirectory()) {
            File[] files = rootFile.listFiles();
            for (File file : files) actualDeleteFile(label, file);
        }
        deleted = rootFile.delete();
        if (!deleted) report.report(label + " - Failed to delete file: " + rootFile.getAbsolutePath()); else report(label + " - File deleted: " + rootFile.getAbsolutePath());
        return deleted;
    }

    /**
	 * Unzip file on the remote machine
	 * 
	 * @param from
	 *            The file to extract.
	 * @param to
	 *            The folder to extract to.
	 */
    public void unzip(File from, File to) {
    }

    /**
	 * Writes the supplied text into a file on the remote machine.
	 * 
	 * @param fileData
	 *            The data to save.
	 * @param fileToWriteTo
	 *            The file to save the data into.
	 * @throws Exception
	 *             If anything goes wrong!
	 */
    public void writeTextIntoRemoteFile(String title, String fileData, File fileToWriteTo) throws Exception {
        fileToWriteTo = getAsRemoteFile(fileToWriteTo);
        report.report(title, fileData.replace("\r\n", "<br>").replace("\n", "<br>"), true);
        fileToWriteTo.createNewFile();
        FileWriter fw = new FileWriter(fileToWriteTo);
        fw.write(fileData);
        fw.flush();
        fw.close();
    }

    /**
	 * Read the entire text from the supplied file.
	 * 
	 * @param fileToReadFrom
	 *            The file to read the data from.
	 * @throws Exception
	 *             If anything goes wrong!
	 */
    public String readTextFromRemoteFile(String title, File fileToReadFrom) throws Exception {
        try {
            fileToReadFrom = getAsRemoteFile(fileToReadFrom);
            FileReader fr = new FileReader(fileToReadFrom);
            BufferedReader br = new BufferedReader(fr);
            String line = "";
            String fileData = "";
            while ((line = br.readLine()) != null) {
                fileData += line + "\n";
            }
            fr.close();
            report.report(title, fileData.replace("\r\n", "<br>").replace("\n", "<br>"), true);
            return fileData;
        } catch (Exception e) {
            report.report("Failed to read from: " + fileToReadFrom.getAbsolutePath(), false);
            throw e;
        } finally {
        }
    }

    /**
	 * Copy a list of file to the specified directory on the specified remote machine.
	 * 
	 * @param sources
	 *            The files to copy, Must be only files!!
	 * @param toRemoteMachine
	 *            The remote machine to copy the files to.
	 * @param destinationDir
	 *            The directory to copy the files to.
	 * @return The UNC path to the destination directory.
	 * @throws Exception
	 *             If anything goes wrong.
	 */
    public File[] copyFilesTo(File[] sources, RemoteMachine toRemoteMachine, File destinationDir) throws Exception {
        if (sources == null) throw new NullPointerException("Sources files array is null");
        if (toRemoteMachine == null) throw new NullPointerException("RemoteUnitMachine object is null");
        if (destinationDir == null) throw new NullPointerException("Destination directory is null");
        File[] uncFiles = new File[sources.length];
        Vector<File> copiedFiles = new Vector<File>();
        for (int i = 0; i < sources.length; i++) {
            uncFiles[i] = getAsRemoteFile(sources[i]);
        }
        destinationDir = toRemoteMachine.getAsRemoteFile(destinationDir);
        toRemoteMachine.makeDirs(destinationDir);
        for (int i = 0; i < uncFiles.length; i++) {
            try {
                report.startLevel("Copy files from" + getIpForDisplay() + "==>" + toRemoteMachine.getIpForDisplay());
                copyFile(uncFiles[i], destinationDir);
            } catch (Exception e) {
                throw e;
            } finally {
                report.stopLevel();
            }
            copiedFiles.add(new File(destinationDir, uncFiles[i].getName()));
        }
        return copiedFiles.toArray(new File[copiedFiles.size()]);
    }

    public File[] copyFilesToLocal(File[] sources, File destinationDir) throws Exception {
        if (sources == null) throw new NullPointerException("Sources files array is null");
        if (destinationDir == null) throw new NullPointerException("Destination directory is null");
        File[] uncFiles = new File[sources.length];
        Vector<File> copiedFiles = new Vector<File>();
        for (int i = 0; i < sources.length; i++) {
            uncFiles[i] = getAsRemoteFile(sources[i]);
        }
        for (int i = 0; i < uncFiles.length; i++) {
            try {
                startLevel("Copy files from" + getIpForDisplay() + "==> JSystem machine " + destinationDir.getPath());
                copyFile(uncFiles[i], destinationDir);
            } catch (Exception e) {
                throw e;
            } finally {
                stopLevel();
            }
            copiedFiles.add(new File(destinationDir, uncFiles[i].getName()));
        }
        return copiedFiles.toArray(new File[copiedFiles.size()]);
    }

    /**
	 * <b>Allow</b> access permissions to the remote machine.
	 * 
	 * @throws Exception
	 *             If there was an error while <b>allowing</b> access to the remote machine.
	 */
    public final void allowPermissionsForcefully() throws Exception {
        setPermissions(true, true);
    }

    /**
	 * <b>Deny</b> access permissions to the remote machine.
	 * 
	 * @throws Exception
	 *             If there was an error while <b>denying</b> access to the remote machine.
	 */
    public final void denyPermissionsForcefully() throws Exception {
        setPermissions(false, true);
    }

    /**
	 * <b>Allow</b> access permissions to the remote machine.
	 * 
	 * @throws Exception
	 *             If there was an error while <b>allowing</b> access to the remote machine.
	 */
    public final void allowPermissions() throws Exception {
        setPermissions(true, false);
    }

    /**
	 * <b>Deny</b> access permissions to the remote machine.
	 * 
	 * @throws Exception
	 *             If there was an error while <b>denying</b> access to the remote machine.
	 */
    public final void denyPermissions() throws Exception {
        setPermissions(false, false);
    }

    private void setPermissions(boolean allow, boolean forcefully) throws Exception {
        if (!forcefully && forcefullPermission) return;
        if ((allow && isAllowed()) || (!allow && !isAllowed())) return;
        Command command = new Command();
        String cliCommand;
        if (allow) cliCommand = "net use \\\\" + hots + " /USER:" + user + " " + password + " /Yes"; else cliCommand = "net use \\\\" + hots + " /DELETE /YES";
        command.setCmd(new String[] { "cmd.exe", "/C", cliCommand });
        Execute.execute(command, true);
        StringBuffer stdout = command.getStd();
        long initTime = System.currentTimeMillis();
        while (stdout.toString().length() == 0 && System.currentTimeMillis() < initTime + PermmisionsTimeout) ;
        String output = stdout.toString();
        if (!output.contains("successfully") && !output.contains("Multiple connections")) {
            throw new Exception(output);
        }
        if (forcefully && allow) {
            this.forcefullPermission = true;
        } else {
            this.forcefullPermission = false;
        }
    }

    private boolean isAllowed() throws Exception {
        Command command = new Command();
        command.setCmd(new String[] { "cmd.exe", "/C", "net use" });
        Execute.execute(command, true);
        setTestAgainstObject(command.getStdout().toString());
        return isAnalyzeSuccess(new FindText("OK\\s*\\\\\\\\" + getHost() + "\\\\", true));
    }

    /**
	 * 
	 * @param rum
	 *            The remote machine to set the file for.
	 * @param file
	 *            The file to convert to access the remote machine
	 * @return The final file path to the remote machine if the file was not already pointing to a remote machine.
	 */
    public final File getAsRemoteFile(File file) {
        if (hots.equals(LocalIpAddress)) return file;
        if (file.getAbsolutePath().startsWith("//") || file.getAbsolutePath().startsWith("\\\\")) return file;
        File newFile = new File("//" + hots + "/" + file.getAbsolutePath().replace(":", "$").replace("\\", "/"));
        return newFile;
    }

    /**
	 * @param source
	 *            The source file.
	 * @param destination
	 *            The destination directory.
	 * @throws IOException
	 *             if cannot copy the files to the remote location.
	 */
    private void copyFile(File source, File destination) throws Exception {
        File finalDestination = new File(destination, source.getName());
        String details = "";
        try {
            if (source.isDirectory()) {
                details += "Copy dir STATE: " + source.getAbsolutePath() + " ==> " + finalDestination.getAbsolutePath();
                FileUtils.copyDirectory(source, finalDestination);
            } else {
                details += "Copy file STATE: " + source.getAbsolutePath() + " ==> " + finalDestination.getAbsolutePath();
                FileUtils.copyFile(source, finalDestination = new File(destination, source.getName()));
            }
            details = details.replace("STATE", "<b>Ok</b>");
        } catch (Exception e) {
            details = details.replace("STATE", "<b>Failed</b>");
            throw e;
        } finally {
            report(details, details.contains("<b>Ok</b>") ? Reporter.PASS : Reporter.FAIL);
        }
    }

    /**
	 * Get the list of files contained in the remote dir.
	 * 
	 * @param dir
	 *            The remote directory to get its files.
	 * @return The list of the files in the remote directory.
	 * @throws Exception
	 *             If anything went wrong.
	 */
    protected File[] getFilesListFor(File dir) throws Exception {
        File[] files = getAsRemoteFile(dir).listFiles();
        return files;
    }

    public void makeDirs(File dirFile) throws Exception {
        dirFile = getAsRemoteFile(dirFile);
        File[] parents = getNotExisitingParentDirs(dirFile);
        for (File file : parents) {
            if (!file.mkdir()) throw new Exception("Was unable to create dir: '" + file.getAbsolutePath() + "'" + ", on remote machine: " + hots);
            report.report("Created dir:" + getIpForDisplay() + file.getAbsolutePath());
        }
    }

    private File[] getNotExisitingParentDirs(File dirFile) {
        Vector<File> parents = new Vector<File>();
        File dir = dirFile;
        while (!dir.exists()) {
            parents.insertElementAt(dir, 0);
            dir = dir.getParentFile();
        }
        return parents.toArray(new File[parents.size()]);
    }

    public File getAsLocalFile(File remoteFile) {
        String newName = remoteFile.getAbsolutePath().replace("" + File.separatorChar, "/");
        if (!newName.startsWith("//")) return remoteFile;
        newName = newName.substring(newName.indexOf("/", 3) + 1);
        newName = newName.replace("$", ":");
        return new File(newName);
    }

    public File[] unzipFileOnRemoteMachine(String label, File zipFile, File toFolder, boolean deleteZip) throws Exception {
        makeDirs(toFolder);
        CliCommand command = new CliCommand();
        command.setCommand(getPathToZip() + " x -y -o\"" + toFolder.getAbsolutePath() + "\" \"" + getAsLocalFile(zipFile).getAbsolutePath() + "\"");
        command.setTimeout(2 * 60 * 1000);
        cli.handleCliCommand(label, command);
        if (deleteZip) deleteFile(label + " - Delete Zip File", zipFile);
        File[] extractedFiles = getAsRemoteFile(toFolder).listFiles();
        return extractedFiles;
    }

    public File zipFileOnRemoteMachine(String dirToZip) throws Exception {
        return zipFileOnRemoteMachine(dirToZip, dirToZip);
    }

    public File zipFileOnRemoteMachine(String dirToZip, String zipFileName) throws Exception {
        CliCommand command = new CliCommand();
        if (dirToZip.lastIndexOf(File.pathSeparator) == dirToZip.length() - 1) {
            dirToZip = dirToZip.substring(0, dirToZip.length() - 2);
        }
        command.setCommand(getPathToZip() + " a " + zipFileName + ".zip " + dirToZip);
        command.setTimeout(2 * 60 * 1000);
        command.setSilent(true);
        cli.handleCliCommand("compressing dir " + dirToZip, command);
        return new File(zipFileName + ".zip");
    }

    public void setMaxTelnetConnections(String ip, String user, String password, int connections) throws Exception {
        Command cmd = new Command();
        cmd.setCmd(new String[] { "cmd.exe", "/C", "Tlntadmn " + ip + " -u " + user + " -p " + password + " config maxconn = " + connections });
        Execute.execute(cmd, true);
    }

    private static Date tempDate = new Date();

    public String getCurrentMilliesAsDate() {
        tempDate.setTime(System.currentTimeMillis());
        return sdf.format(tempDate);
    }

    public File getTempFolder() {
        return new File(tempFolder);
    }

    public Reporter getReport() {
        return report;
    }

    public CliConnectionImpl getCLI() {
        return cli;
    }

    public void launchCommand(String descrption, CliCommand command) throws Exception {
        try {
            if (!cli.isConnected()) cli.connect();
            cli.handleCliCommand(descrption, command);
        } catch (Exception e) {
            throw new Exception("Failed to launch command: " + descrption);
        }
    }

    public String getTasklist() throws Exception {
        CliCommand command = new CliCommand("tasklist");
        executeCommand("Get Tasklist", command);
        return command.getResult();
    }

    @Override
    public String toString() {
        return hots + "&" + user + ":" + password;
    }

    public void killAllMatchingProcess(String processName) throws Exception {
        CliCommand cmd = new CliCommand("taskkill.exe /f /t /im " + processName);
        if (!cli.isConnected()) cli.connect();
        executeCommand("Kill all " + processName, cmd);
    }

    public void killAllMatchingProcess(RemoteProcess process) throws Exception {
        killAllMatchingProcess(process.getExecutableName());
    }

    /**
	 * Analyze exception thrown by the remote machine.
	 */
    public void analyzeException(Exception e) throws Exception {
        if (!myExpection(e)) {
            return;
        }
        switch(RmExceptionType.string2enum(e.getMessage())) {
            case MACHINE_NOT_FOUND:
                reportException(e, "Machine" + getIpForDisplay() + "is unreachable");
                break;
            case BAD_USER_NAME_OR_PASSWORD:
                reportException(e, getIpForDisplay() + "Unknown user name or password - " + getUser() + ", " + getPassword());
                break;
        }
    }

    /**
	 * Test whether the Exception was thrown by the current remote machine or by other remote machine.
	 */
    public boolean myExpection(Exception e) {
        for (StackTraceElement ste : e.getStackTrace()) {
            if (ste.getClassName().equals(getClass().getName())) {
                return true;
            }
        }
        return false;
    }

    protected String getIpForDisplay() {
        return " " + getName() + " (" + getHost() + ") ";
    }

    /**
	 * Report exception in separate level.
	 * 
	 * @param e
	 *            Exception that triggered the report, use as level body.
	 * @param t
	 * @throws IOException
	 */
    protected void reportException(Exception e, String t) throws IOException {
        report.startLevel(t.replace("\r\n", " "));
        report.report(StringUtils.getStackTrace(e), Reporter.FAIL);
        report.stopLevel();
    }

    public void reportDebugInfo(Object object) throws Exception {
    }

    public String pathToZip;

    public String getPathToZip() {
        return pathToZip;
    }

    public void setPathToZip(String pathToZip) {
        this.pathToZip = pathToZip;
    }
}
