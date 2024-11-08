package org.jsystem.systemObjects.remoteMachine;

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
        this.ipAddress = ipAddress;
        this.machineUserName = machineUserName;
        this.machinePassword = machinePassword;
    }

    private static final int PermmisionsTimeout = 20000;

    /**
	 * The remote unit machine IP address/name.
	 */
    protected String ipAddress;

    /**
	 * The login user name for the remote unit machine.
	 */
    protected String machineUserName;

    /**
	 * The login password for the remote unit machine.
	 */
    protected String machinePassword;

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

    public String getIpAddress() {
        return ipAddress;
    }

    /**
	 * The IP address of the remote machine.
	 * 
	 * @param ipAddress
	 *            The IP address of the remote machine.
	 */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getMachineUserName() {
        return machineUserName;
    }

    /**
	 * The remote machine user name.
	 * 
	 * @param machineUserName
	 *            The remote machine user name.
	 */
    public void setMachineUserName(String machineUserName) {
        this.machineUserName = machineUserName;
    }

    public String getMachinePassword() {
        return machinePassword;
    }

    /**
	 * The remote machine password matching the supplied user name.
	 */
    public void setMachinePassword(String machinePassword) {
        this.machinePassword = machinePassword;
    }

    @Override
    public void init() throws Exception {
        super.init();
        allowPermissionsForcefully();
        setMaxTelnetConnections(ipAddress, machineUserName, machinePassword, maxConnections);
        cli = new WindowsDefaultCliConnection(ipAddress, machineUserName, machinePassword);
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
	 * Copy a list of file to the specified directory on the specified remote
	 * machine.
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
                report.startLevel("Copy files from " + getName() + " " + getIpAddress() + " ==> " + toRemoteMachine.getName() + " " + toRemoteMachine.getIpAddress());
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

    /**
	 * <b>Allow</b> access permissions to the remote machine.
	 * 
	 * @throws Exception
	 *             If there was an error while <b>allowing</b> access to the
	 *             remote machine.
	 */
    public final void allowPermissionsForcefully() throws Exception {
        setPermissions(true, true);
    }

    /**
	 * <b>Deny</b> access permissions to the remote machine.
	 * 
	 * @throws Exception
	 *             If there was an error while <b>denying</b> access to the
	 *             remote machine.
	 */
    public final void denyPermissionsForcefully() throws Exception {
        setPermissions(false, true);
    }

    /**
	 * <b>Allow</b> access permissions to the remote machine.
	 * 
	 * @throws Exception
	 *             If there was an error while <b>allowing</b> access to the
	 *             remote machine.
	 */
    public final void allowPermissions() throws Exception {
        setPermissions(true, false);
    }

    /**
	 * <b>Deny</b> access permissions to the remote machine.
	 * 
	 * @throws Exception
	 *             If there was an error while <b>denying</b> access to the
	 *             remote machine.
	 */
    public final void denyPermissions() throws Exception {
        setPermissions(false, false);
    }

    private void setPermissions(boolean allow, boolean forcefully) throws Exception {
        if (!forcefully && forcefullPermission) return;
        if ((allow && isAllowed()) || (!allow && !isAllowed())) return;
        Command command = new Command();
        String cliCommand;
        if (allow) cliCommand = "net use \\\\" + ipAddress + " /USER:" + machineUserName + " " + machinePassword + " /Yes"; else cliCommand = "net use \\\\" + ipAddress + " /DELETE /YES";
        command.setCmd(new String[] { "cmd.exe", "/C", cliCommand });
        Execute.execute(command, true);
        StringBuffer stdout = command.getStd();
        long initTime = System.currentTimeMillis();
        while (stdout.toString().length() == 0 && System.currentTimeMillis() < initTime + PermmisionsTimeout) ;
        String output = stdout.toString();
        if (!output.contains("successfully") && !output.contains("Multiple connections")) {
            report.startLevel("Set permissions failure details ->");
            report.report("Command:");
            report.report("  " + cliCommand);
            report.report("");
            report.report("Output:");
            report.report(output);
            report.stopLevel();
            throw new Exception("Could not change permissions.");
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
        return isAnalyzeSuccess(new FindText("OK\\s*\\\\\\\\" + getIpAddress() + "\\\\", true));
    }

    /**
	 * 
	 * @param rum
	 *            The remote machine to set the file for.
	 * @param file
	 *            The file to convert to access the remote machine
	 * @return The final file path to the remote machine if the file was not
	 *         already pointing to a remote machine.
	 */
    public final File getAsRemoteFile(File file) {
        if (ipAddress.equals(LocalIpAddress)) return file;
        if (file.getAbsolutePath().startsWith("//") || file.getAbsolutePath().startsWith("\\\\")) return file;
        File newFile = new File("//" + ipAddress + "/" + file.getAbsolutePath().replace(":", "$").replace("\\", "/"));
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
                try {
                    details += "Copy dir STATE: " + source.getAbsolutePath() + " ==> " + finalDestination.getAbsolutePath();
                    FileUtils.copyDirectory(source, finalDestination);
                } catch (IOException e) {
                    throw e;
                }
            } else {
                try {
                    details += "Copy file STATE: " + source.getAbsolutePath() + " ==> " + finalDestination.getAbsolutePath();
                    FileUtils.copyFile(source, finalDestination = new File(destination, source.getName()));
                } catch (IOException e) {
                    report.report("Copy file STATE: " + source.getAbsolutePath() + " ==> " + (finalDestination != null ? finalDestination.getAbsolutePath() : "null"));
                    throw e;
                }
            }
            details = details.replace("STATE", "<b>Ok</b>");
        } catch (Exception e) {
            details = details.replace("STATE", "<b>Failed</b>");
            throw e;
        } finally {
            report(details);
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
            if (!file.mkdir()) throw new Exception("Was unable to create dir: '" + file.getAbsolutePath() + "'" + ", on remote machine: " + ipAddress);
            report.report("Created dir: " + getName() + " " + file.getAbsolutePath());
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
        return ipAddress + "&" + machineUserName + ":" + machinePassword;
    }

    public void killAllMatchingProcess(String processName) throws Exception {
        CliCommand cmd = new CliCommand("taskkill.exe /f /t /im " + processName);
        if (!cli.isConnected()) cli.connect();
        executeCommand("Kill all " + processName, cmd);
    }

    public void killAllMatchingProcess(RemoteProcess process) throws Exception {
        killAllMatchingProcess(process.getExecutableName());
    }

    public void reportDebugInfo() {
    }
}
