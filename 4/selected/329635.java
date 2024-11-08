package ostf.test.client.ftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import ostf.data.ArrayUtil;
import ostf.test.client.ActionResult;
import ostf.test.client.TestAction;
import ostf.test.client.TestActionException;
import ostf.test.data.expression.ExpressionException;
import ostf.test.thread.TestThread;
import ostf.test.unit.TestUnit;

public class FtpAction implements TestAction {

    public static final String[] VALID_FOLDER_METHOD = new String[] { "create", "delete", "cd", "list" };

    public static final String[] VALID_FILE_METHOD = new String[] { "download", "upload", "delete", "rename" };

    protected FtpActionConfig config = null;

    protected FTPClient ftpClient = null;

    protected String server = null;

    protected int port = 21;

    protected String method = null;

    protected String folder = null;

    protected String localFile = null;

    protected String remoteFile = null;

    protected String command = null;

    public FtpAction(FtpActionConfig config, TestThread thread) throws TestActionException {
        this.config = config;
        setupFtpClient(thread);
        try {
            setupCommand(thread);
        } catch (ExpressionException e) {
            throw new TestActionException("Error to setup ftp command for action " + config.getName(), e);
        }
    }

    protected void setupFtpClient(TestThread thread) throws TestActionException {
        Object client = thread.getTestProperty(TestUnit.TEST_CLIENT);
        if (client == null) throw new TestActionException("No TestClient is defined for action " + config.getName());
        if (!(client instanceof FtpTestClient)) throw new TestActionException("FtpTestClient is needed for action " + config.getName() + ". But we got " + client.getClass().getName());
        ftpClient = ((FtpTestClient) client).getFtpClient();
        server = ((FtpTestClient) client).getHostName();
        port = ((FtpTestClient) client).getPort();
    }

    protected void setupCommand(TestThread thread) throws TestActionException, ExpressionException {
        command = config.getCommand();
        if (!COMMAND_LOGIN.equalsIgnoreCase(command)) {
            if (!ftpClient.isConnected()) {
                thread.breakCurrentComponent();
                throw new TestActionException("Ftp connection is closed in config " + config.getName());
            }
        }
        if (COMMAND_LOGIN.equalsIgnoreCase(command) || COMMAND_LOGOUT.equalsIgnoreCase(command)) return;
        if (command.endsWith("Folder")) {
            method = config.getCommand().substring(0, config.getCommand().indexOf("Folder"));
            if (ArrayUtil.stringInArray(method, VALID_FOLDER_METHOD, true)) {
                if (config.getFolder() != null) folder = (String) thread.getActualData(config.getFolder());
                if ("create".equalsIgnoreCase(method) || "delete".equalsIgnoreCase(method)) config.checkNullParam("Folder", folder, thread);
                return;
            }
        }
        if (command.endsWith("File")) {
            method = config.getCommand().substring(0, config.getCommand().indexOf("File"));
            if (ArrayUtil.stringInArray(method, VALID_FILE_METHOD, true)) {
                if ("upload".equalsIgnoreCase(method)) {
                    localFile = (String) thread.getActualData(config.getLocalFile());
                    config.checkNullParam("localFile", localFile, thread);
                    if (config.getRemoteFile() != null) {
                        remoteFile = (String) thread.getActualData(config.getRemoteFile());
                        config.checkNullParam("remoteFile", remoteFile, thread);
                    }
                } else if ("download".equalsIgnoreCase(method)) {
                    remoteFile = (String) thread.getActualData(config.getRemoteFile());
                    config.checkNullParam("remoteFile", remoteFile, thread);
                    if (config.getLocalFile() != null) {
                        localFile = (String) thread.getActualData(config.getLocalFile());
                        config.checkNullParam("localFile", localFile, thread);
                    }
                } else if ("rename".equalsIgnoreCase(method)) {
                    localFile = (String) thread.getActualData(config.getLocalFile());
                    config.checkNullParam("oldFile", localFile, thread);
                    remoteFile = (String) thread.getActualData(config.getRemoteFile());
                    config.checkNullParam("newFile", remoteFile, thread);
                } else {
                    localFile = (String) thread.getActualData(config.getLocalFile());
                    config.checkNullParam("localFile", localFile, thread);
                }
                return;
            }
        }
        command = (String) thread.getActualData(config.getCommand());
        config.checkNullParam("command", command, thread);
    }

    protected ActionResult connectAndLogin(TestThread thread) {
        closeFtpClient(thread);
        ActionResult result = new ActionResult();
        result.setRequestString("login to ftp server " + server + ":" + port);
        result.start();
        try {
            ftpClient.connect(server, port);
            int reply = ftpClient.getReplyCode();
            if (FTPReply.isPositiveCompletion(reply)) {
                configFtpClient(thread);
                boolean status = ftpClient.login(thread.getCurrentUser().getUserLogin(), thread.getCurrentUser().getUserPassword());
                if (!status) {
                    result.setSuccess(false);
                    result.setException(new TestActionException(ftpClient.getReplyCode() + " : " + ftpClient.getReplyString()));
                } else {
                    result.setSuccess(true);
                }
            } else {
                result.setSuccess(false);
                result.setException(new TestActionException("501 : Could not connect, " + result.getResponseMessage()));
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setException(new TestActionException(config, e));
        }
        if (!result.isSuccess()) closeFtpClient(thread);
        result.end();
        result.setResponseCode(String.valueOf(ftpClient.getReplyCode()));
        String message = ftpClient.getReplyString();
        result.setResponseMessage(message);
        result.setResponseLength(message != null ? message.getBytes().length : 0);
        return result;
    }

    protected ActionResult logoutAndDisc(TestThread thread, ActionResult result) {
        result.setRequestString("logout from ftp server " + server + ":" + port);
        try {
            if (ftpClient.logout()) {
                result.setSuccess(true);
            } else {
                result.setSuccess(false);
                result.setException(new TestActionException(ftpClient.getReplyCode() + " : " + ftpClient.getReplyString()));
            }
        } catch (IOException e) {
            result.setSuccess(false);
            result.setException(new TestActionException(config, e));
        } finally {
            closeFtpClient(thread);
        }
        result.end();
        result.setResponseCode(String.valueOf(ftpClient.getReplyCode()));
        String message = ftpClient.getReplyString();
        result.setResponseMessage(message);
        result.setResponseLength(message != null ? message.getBytes().length : 0);
        return result;
    }

    protected boolean uploadFile(TestThread thread, ActionResult result) {
        result.setRequestString("upload file " + localFile);
        InputStream input = null;
        ftpClient.enterLocalPassiveMode();
        File infile = new File(localFile);
        boolean status = false;
        try {
            input = new FileInputStream(infile);
            if (remoteFile != null) status = ftpClient.storeFile(remoteFile, input); else status = ftpClient.storeFile(infile.getName(), input);
        } catch (Exception e) {
            result.setException(new TestActionException(config, e));
        } finally {
            IOUtils.closeQuietly(input);
        }
        return status;
    }

    protected boolean downloadFile(TestThread thread, ActionResult result) {
        result.setRequestString("download file " + remoteFile);
        InputStream input = null;
        OutputStream output = null;
        OutputStream target = null;
        boolean status = false;
        ftpClient.enterLocalPassiveMode();
        try {
            if (localFile != null) {
                File lcFile = new File(localFile);
                if (lcFile.exists() && lcFile.isDirectory()) output = new FileOutputStream(new File(lcFile, remoteFile)); else output = new FileOutputStream(lcFile);
                target = output;
            } else {
                target = new FileOutputStream(remoteFile);
            }
            input = ftpClient.retrieveFileStream(remoteFile);
            long bytes = IOUtils.copy(input, target);
            status = bytes > 0;
            if (status) {
                result.setResponseLength(bytes);
            }
        } catch (Exception e) {
            result.setException(new TestActionException(config, e));
        } finally {
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(output);
        }
        return status;
    }

    protected ActionResult runCommand(TestThread thread, ActionResult result) {
        result.setRequestString("do comand '" + command + "'");
        try {
            int reply = ftpClient.sendCommand(command);
            if (FTPReply.isPositiveCompletion(reply)) {
                result.setSuccess(true);
            } else {
                result.setSuccess(false);
                result.setException(new TestActionException(ftpClient.getReplyCode() + " : " + ftpClient.getReplyString()));
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setException(new TestActionException(config, e));
        }
        result.end();
        result.setResponseCode(String.valueOf(ftpClient.getReplyCode()));
        String message = ftpClient.getReplyString();
        result.setResponseMessage(message);
        result.setResponseLength(message != null ? message.getBytes().length : 0);
        return result;
    }

    protected FtpActionResult handleFolder(TestThread thread, FtpActionResult result) {
        result.setRequestString(method + " folder " + (folder != null ? folder : ""));
        boolean status = false;
        try {
            if ("create".equalsIgnoreCase(method)) {
                status = ftpClient.makeDirectory(folder);
            } else if ("delete".equalsIgnoreCase(method)) {
                status = ftpClient.removeDirectory(folder);
            } else if ("cd".equalsIgnoreCase(method)) {
                if (folder == null) {
                    result.setRequestString("cd parent folder");
                    status = ftpClient.changeToParentDirectory();
                } else status = ftpClient.changeWorkingDirectory(folder);
            } else if ("list".equalsIgnoreCase(method)) {
                FTPFile[] files = null;
                if (folder == null) files = ftpClient.listFiles(); else files = ftpClient.listFiles(folder);
                String[] names = new String[files.length];
                int begin = 0;
                int end = files.length - 1;
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isDirectory()) names[begin++] = files[i].getName(); else if (files[i].isFile()) names[end--] = files[i].getName();
                }
                String[] fileNames = new String[files.length - 1 - end];
                String[] folderNames = new String[begin];
                System.arraycopy(names, 0, folderNames, 0, begin);
                System.arraycopy(names, end + 1, fileNames, 0, fileNames.length);
                result.setSubFolders(folderNames);
                result.setFiles(fileNames);
                status = true;
            }
        } catch (IOException e) {
            result.setException(new TestActionException(config, e));
        }
        result.end();
        result.setSuccess(status);
        if (!status && result.getException() == null) result.setException(new TestActionException(ftpClient.getReplyCode() + " : " + ftpClient.getReplyString()));
        result.setResponseCode(String.valueOf(ftpClient.getReplyCode()));
        String message = ftpClient.getReplyString();
        result.setResponseMessage(message);
        result.setResponseLength(message != null ? message.getBytes().length : 0);
        return result;
    }

    protected FtpActionResult handleFile(TestThread thread, FtpActionResult result) {
        boolean status = false;
        try {
            if ("upload".equalsIgnoreCase(method)) {
                status = uploadFile(thread, result);
            } else if ("download".equalsIgnoreCase(method)) {
                status = downloadFile(thread, result);
            } else if ("delete".equalsIgnoreCase(method)) {
                result.setRequestString("delete " + localFile);
                status = ftpClient.deleteFile(localFile);
            } else if ("rename".equalsIgnoreCase(method)) {
                result.setRequestString("rename " + localFile + " to " + remoteFile);
                status = ftpClient.rename(localFile, remoteFile);
            }
        } catch (IOException e) {
            result.setException(new TestActionException(config, e));
        }
        result.end();
        result.setSuccess(status);
        if (!status && result.getException() == null) result.setException(new TestActionException(ftpClient.getReplyCode() + " : " + ftpClient.getReplyString()));
        result.setResponseCode(String.valueOf(ftpClient.getReplyCode()));
        String message = ftpClient.getReplyString();
        result.setResponseMessage(message);
        if (result.getResponseLength() <= 0) result.setResponseLength(message != null ? message.getBytes().length : 0);
        return result;
    }

    public ActionResult doAction(TestThread thread) {
        if (COMMAND_LOGIN.equalsIgnoreCase(config.getCommand())) {
            return connectAndLogin(thread);
        }
        FtpActionResult result = new FtpActionResult();
        result.start();
        configFtpClient(thread);
        if (COMMAND_LOGOUT.equalsIgnoreCase(config.getCommand())) {
            return logoutAndDisc(thread, result);
        } else if (method != null && config.getCommand().endsWith("Folder")) {
            return handleFolder(thread, result);
        } else if (method != null && config.getCommand().endsWith("File")) {
            return handleFile(thread, result);
        } else return runCommand(thread, result);
    }

    protected void configFtpClient(TestThread thread) {
        try {
            HashMap<String, String> ftpParams = config.getFtpParams(thread);
            if (ftpParams.containsKey(FtpActionConfig.FTP_SOCKET_TIMEOUT)) ftpClient.setSoTimeout(Integer.parseInt(ftpParams.get(FtpActionConfig.FTP_SOCKET_TIMEOUT)));
            if (ftpParams.containsKey(FtpActionConfig.FTP_DATA_TIMEOUT)) ftpClient.setDataTimeout(Integer.parseInt(ftpParams.get(FtpActionConfig.FTP_DATA_TIMEOUT)));
            if (ftpParams.containsKey(FtpActionConfig.FTP_SOCKET_LINGER)) ftpClient.setSoLinger(true, Integer.parseInt(ftpParams.get(FtpActionConfig.FTP_SOCKET_LINGER)));
            if (ftpParams.containsKey(FtpActionConfig.FTP_TCP_NODELAY)) ftpClient.setTcpNoDelay(Boolean.parseBoolean(ftpParams.get(FtpActionConfig.FTP_TCP_NODELAY)));
            if (ftpParams.containsKey(FtpActionConfig.FTP_BINARY_FILE)) {
                if ("true".equalsIgnoreCase(ftpParams.get(FtpActionConfig.FTP_BINARY_FILE))) ftpClient.setFileType(FTP.BINARY_FILE_TYPE); else ftpClient.setFileType(FTP.ASCII_FILE_TYPE);
            }
        } catch (NumberFormatException e) {
            thread.getLogger().warn("Exception in ftp action " + config.getName(), e);
        } catch (Exception e) {
            thread.getLogger().warn("Exception in ftp action " + config.getName(), e);
        }
    }

    protected void closeFtpClient(TestThread thread) {
        if (ftpClient != null && ftpClient.isConnected()) try {
            ftpClient.disconnect();
        } catch (IOException e) {
            thread.getLogger().warn("Error to disconnect ftp client", e);
        }
    }
}
