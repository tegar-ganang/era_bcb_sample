package atlas.ftp.client;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.Date;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

/**
 * Simple FTP Client with the following 5 to 6 arguments:<br>
 * HOSTNAME USER PASSWD [ ACCOUNT ] command filename<br>
 * where command is PUT or PUTP or GET or GETP, P meaning Passive mode, else Active mode
 *
 * @author Frederic Bregier
 *
 */
public class AtlasFtpClient {

    /**
     * Group of Simple Ftp Commands supported
     * @author Frederic Bregier
     *
     */
    public static enum FtpCommand {

        PUT, PUTP, GET, GETP
    }

    /**
     * ErrorResult from command (System.exit value)
     * @author Frederic Bregier
     *
     */
    public static enum ErrorResult {

        OK(0), MANDATORYMISSING(10), COMMANDUNKNOWN(11), CONNECTNOTPOSSIBLE(20), CONNECTNOTCORRECT(21), LOGINNOTCORRECT(22), LOGINACCTNOTCORRECT(23), ERRORWHILECONNECT(24), MKDIR(30), CHANGEDIR(31), CHANGEFILETYPE(32), TRANSFERIOERROR(40), TRANSFERERROR(41), UNKOWNCOMMANDEXEC(50);

        public int code;

        /**
         *
         */
        private ErrorResult(int code) {
            this.code = code;
        }
    }

    private final String server;

    private final String username;

    private final String passwd;

    private final String account;

    private final FtpCommand command;

    private final String filename;

    private final FTPClient ftpClient;

    private final int port = 21;

    private boolean passiveMode = true;

    public static volatile int status = 0;

    /**
     * @param args
     */
    public static void main(String[] args) {
        if (args.length < 5 || args.length > 6) {
            System.err.println(AtlasFtpClient.class.getName() + " needs HOSTNAME USER PASSWD [ ACCOUNT ] command filename\n" + " where command is PUT or PUTP or GET or GETP, P meaning Passive mode");
            System.exit(1);
        }
        int i = 0;
        String hostname = args[i++];
        String user = args[i++];
        String passwd = args[i++];
        String acct = null;
        if (args.length == 6) {
            acct = args[i++];
        }
        String command = args[i++];
        String filename = args[i++];
        AtlasFtpClient atlasFtpClient = null;
        try {
            atlasFtpClient = new AtlasFtpClient(hostname, user, passwd, acct, command, filename);
        } catch (IllegalArgumentException e) {
            System.err.println("Bad initialization: " + e.getMessage());
            System.exit(2);
        }
        atlasFtpClient.run();
        if (status > 0) {
            System.err.println("Error during transfer: " + status);
            System.exit(status);
        }
    }

    /**
     * @param hostname
     * @param user
     * @param passwd
     * @param acct
     * @param command
     * @param filename
     * @throws IllegalArgumentException
     */
    public AtlasFtpClient(String hostname, String user, String passwd, String acct, String command, String filename) throws IllegalArgumentException {
        if (hostname == null || user == null || passwd == null || command == null || filename == null) {
            status = ErrorResult.MANDATORYMISSING.code;
            throw new IllegalArgumentException("A mandatory value is missing");
        }
        this.server = hostname;
        this.username = user;
        this.passwd = passwd;
        this.account = acct;
        String cmd = command.toUpperCase();
        this.command = FtpCommand.valueOf(cmd);
        if (this.command == null) {
            status = ErrorResult.COMMANDUNKNOWN.code;
            throw new IllegalArgumentException("Command is not correct");
        }
        this.filename = filename;
        this.ftpClient = new FTPClient();
    }

    public void run() {
        if (!connect()) {
            System.err.println("Cannot connect");
            return;
        }
        try {
            changeFileType(true);
            switch(command) {
                case GETP:
                case PUTP:
                    changeMode(true);
                    break;
                case GET:
                case PUT:
                    changeMode(false);
                    break;
                default:
                    status = ErrorResult.UNKOWNCOMMANDEXEC.code;
                    System.err.println("Command not recognized");
                    return;
            }
            switch(command) {
                case GETP:
                case GET:
                    if (!transferFile(filename, false)) {
                        System.err.println("Cannot transfer file");
                        return;
                    }
                    break;
                case PUTP:
                case PUT:
                    if (!transferFile(filename, true)) {
                        System.err.println("Cannot transfer file");
                        return;
                    }
                    break;
                default:
                    status = ErrorResult.UNKOWNCOMMANDEXEC.code;
                    System.err.println("Command not recognized");
                    return;
            }
        } finally {
            disconnect();
        }
    }

    /**
     * Try to connect to the server and goes with the authentication
     *
     * @return True if connected and authenticated, else False
     */
    public boolean connect() {
        boolean isConnected = false;
        try {
            try {
                this.ftpClient.connect(this.server, this.port);
            } catch (SocketException e) {
                status = ErrorResult.CONNECTNOTPOSSIBLE.code;
                return false;
            } catch (IOException e) {
                status = ErrorResult.CONNECTNOTPOSSIBLE.code;
                return false;
            }
            int reply = this.ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                this.disconnect();
                status = ErrorResult.CONNECTNOTCORRECT.code;
                return false;
            }
            try {
                if (this.account == null) {
                    if (!this.ftpClient.login(this.username, this.passwd)) {
                        status = ErrorResult.LOGINNOTCORRECT.code;
                        this.ftpClient.logout();
                        return false;
                    }
                } else if (!this.ftpClient.login(this.username, this.passwd, this.account)) {
                    status = ErrorResult.LOGINACCTNOTCORRECT.code;
                    this.ftpClient.logout();
                    return false;
                }
            } catch (IOException e) {
                status = ErrorResult.ERRORWHILECONNECT.code;
                try {
                    this.ftpClient.logout();
                } catch (IOException e1) {
                }
                return false;
            }
            isConnected = true;
            return true;
        } finally {
            if ((!isConnected) && this.ftpClient.isConnected()) {
                this.disconnect();
            }
        }
    }

    /**
     * Disconnect the Ftp Client
     */
    public void disconnect() {
        try {
            this.ftpClient.disconnect();
        } catch (IOException e) {
        }
    }

    /**
     * Create a new directory
     *
     * @param newDir
     * @return True if created
     */
    public boolean makeDir(String newDir) {
        try {
            return this.ftpClient.makeDirectory(newDir);
        } catch (IOException e) {
            status = ErrorResult.MKDIR.code;
            return false;
        }
    }

    /**
     * Change remote directory
     *
     * @param newDir
     * @return True if the change is OK
     */
    public boolean changeDir(String newDir) {
        try {
            return this.ftpClient.changeWorkingDirectory(newDir);
        } catch (IOException e) {
            status = ErrorResult.CHANGEDIR.code;
            return false;
        }
    }

    /**
     * Change the FileType of Transfer (Binary true, ASCII false)
     *
     * @param binaryTransfer True for Binary Transfer
     * @return True if the change is OK
     */
    public boolean changeFileType(boolean binaryTransfer) {
        try {
            if (binaryTransfer) {
                return this.ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            } else {
                return this.ftpClient.setFileType(FTP.ASCII_FILE_TYPE);
            }
        } catch (IOException e) {
            status = ErrorResult.CHANGEFILETYPE.code;
            return false;
        }
    }

    /**
     * Change to passive (true) or active (false) mode
     *
     * @param passive
     */
    public void changeMode(boolean passive) {
        this.passiveMode = passive;
        if (this.passiveMode) {
            this.ftpClient.enterLocalPassiveMode();
        } else {
            this.ftpClient.enterLocalActiveMode();
        }
    }

    /**
     * Ask to transfer a file
     *
     * @param filename
     * @param store
     * @return True if the file is correctly transfered
     */
    public boolean transferFile(String filename, boolean store) {
        boolean cstatus = false;
        InputStream input = null;
        OutputStream output = null;
        try {
            if (store) {
                input = new FileInputStream(filename);
                cstatus = this.ftpClient.storeFile(filename, input);
                input.close();
            } else {
                output = new FileOutputStream(filename);
                cstatus = this.ftpClient.retrieveFile(filename, output);
                output.flush();
                output.close();
            }
        } catch (IOException e) {
            if (input != null) try {
                input.close();
            } catch (IOException e1) {
            }
            if (output != null) try {
                output.close();
            } catch (IOException e1) {
            }
            status = ErrorResult.TRANSFERIOERROR.code;
            return false;
        }
        int istatus = this.ftpClient.getReplyCode();
        if (!cstatus) {
            status = ErrorResult.TRANSFERERROR.code;
            System.err.print((new Date()) + " error: " + this.ftpClient.getReplyString());
            System.err.println((new Date()) + " status: " + istatus + " " + FTPReply.isPositiveCompletion(istatus));
        }
        return (cstatus);
    }
}
