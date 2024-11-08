package lopdsoft_uploader;

import java.io.FileInputStream;
import java.io.IOException;
import org.apache.commons.net.ftp.FTPClient;

/**
 *
 * @author luis
 */
public class FtpUploader {

    String server;

    Integer port;

    String username;

    String password;

    String filename;

    String path;

    FTPClient ftpClient;

    public FtpUploader() {
        this.server = "";
        this.port = 21;
        this.username = "";
        this.password = "";
        this.filename = "";
        this.path = "";
        this.ftpClient = new FTPClient();
    }

    public void setServerData(String server, Integer port) {
        this.server = server;
        this.port = port;
    }

    public void setLoginData(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public void setFileData(String filename, String path) {
        this.filename = filename;
        this.path = path;
    }

    private boolean connect() {
        try {
            this.ftpClient.connect(this.server, this.port);
            this.ftpClient.login(this.username, this.password);
            return true;
        } catch (IOException iOException) {
            return false;
        }
    }

    private boolean disconnect() {
        try {
            this.ftpClient.disconnect();
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    public boolean uploadFile() {
        try {
            FileInputStream inStream = new FileInputStream(this.filename);
            if (this.connect()) {
                if (this.checkPath()) {
                    this.ftpClient.changeWorkingDirectory(this.path);
                    if (this.ftpClient.storeFile(this.filename, inStream)) return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkPath() {
        try {
            if (this.ftpClient.changeWorkingDirectory(this.path)) return true;
            if (this.ftpClient.makeDirectory(this.path)) return true;
            return false;
        } catch (IOException iOException) {
            return false;
        }
    }

    public static void main(String[] args) {
        System.out.println("Building FTPClient...");
        FtpUploader fu = new FtpUploader();
        fu.setServerData("85.112.5.134", 21);
        fu.setLoginData("luis", "9874292etd56");
        fu.setFileData("fileAux.txt", "./uploadedFile/code/");
        System.out.println("Uploading file...");
        fu.uploadFile();
        System.out.println("File Uploaded.");
    }
}
