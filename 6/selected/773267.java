package br.org.databasetools.core.protocol;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import br.org.databasetools.core.exception.FTPException;

public class FileTransferProtocol {

    public static final String BINARY_MODE = "BINARY";

    public static final String ASCII_MODE = "ASCII";

    private String host;

    private String username;

    private String password;

    private String directory;

    private String mode;

    private FTPClient ftp;

    private boolean isConnected = false;

    public FileTransferProtocol() throws FTPException {
    }

    public void disconnect() throws FTPException {
        try {
            ftp.disconnect();
        } catch (IOException ex) {
            throw new FTPException(ex);
        }
    }

    public void connect() throws FTPException {
        try {
            ftp = new FTPClient();
            ftp.connect(host);
            if (FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                ftp.login(this.username, this.password);
            } else {
                ftp.disconnect();
                throw new FTPException("Não foi possivel se conectar no servidor FTP");
            }
            isConnected = true;
        } catch (Exception ex) {
            throw new FTPException(ex);
        }
    }

    public void changeDir(String directory) throws FTPException {
        try {
            if (directory != null) {
                if (ftp.changeWorkingDirectory(directory) == false) {
                    throw new FTPException("Não foi possível acessar diretorio " + this.directory + " no servidor de FTP: " + this.host);
                }
            }
        } catch (Exception ex) {
            throw new FTPException(ex);
        }
    }

    public String[] list(String pathname) throws FTPException {
        try {
            FTPFile[] files = ftp.listFiles();
            String[] result = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                result[i] = files[i].getName();
            }
            return result;
        } catch (Exception ex) {
            throw new FTPException(ex);
        }
    }

    public void receiveFile(String fileName, String store) throws FTPException {
        try {
            if (isConnected == false) throw new FTPException("Primeiro chame o metodo connect()");
            FileOutputStream fos = new FileOutputStream(new File(store + "/" + fileName));
            ftp.retrieveFile(fileName, fos);
            fos.flush();
            fos.close();
        } catch (Exception ex) {
            throw new FTPException(ex);
        }
    }

    public void deleteFile(String fileName) throws FTPException {
        try {
            if (isConnected == false) throw new FTPException("Primeiro chame o metodo connect()");
            ftp.deleteFile(fileName);
        } catch (Exception ex) {
            throw new FTPException(ex);
        }
    }

    public void renameFile(String fileNameSource, String fileNameTarget) throws FTPException {
        try {
            if (isConnected == false) throw new FTPException("Primeiro chame o metodo connect()");
            ftp.rename(fileNameSource, fileNameTarget);
        } catch (Exception ex) {
            throw new FTPException(ex);
        }
    }

    public void sendFile(String fileNameAndPath, String mode) throws FTPException {
        try {
            if (isConnected == false) throw new FTPException("Primeiro chame o metodo connect()");
            InputStream is = new FileInputStream(fileNameAndPath);
            int idx = fileNameAndPath.lastIndexOf(File.separator);
            if (idx < 0) idx = 0; else idx++;
            String fileName = fileNameAndPath.substring(idx, fileNameAndPath.length());
            if (mode.equals(BINARY_MODE)) ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
            if (mode.equals(ASCII_MODE)) ftp.setFileType(FTPClient.ASCII_FILE_TYPE);
            ftp.storeFile(fileName, is);
            is.close();
        } catch (Exception ex) {
            throw new FTPException(ex);
        }
    }

    public void send(String fileNameAndPath) throws FTPException {
        if (isConnected == false) throw new FTPException("Primeiro chame o metodo connect()");
        if (getMode() != null) {
            sendFile(fileNameAndPath, getMode());
        } else {
            sendFile(fileNameAndPath, BINARY_MODE);
        }
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) throws FTPException {
        this.mode = mode;
    }
}
