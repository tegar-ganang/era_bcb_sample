package be.ac.fundp.infonet.econf.util;

import com.oroinc.net.ftp.*;
import java.io.*;

public class FTPUtils {

    /**
     * Logging object.
     */
    private static org.apache.log4j.Category m_logCat = org.apache.log4j.Category.getInstance(FTPUtils.class.getName());

    /**
     * Uploads the specified file to the specified FTP server.
     * @param in
     * The file to upload
     * @param out
     * The file name in the ftp server. If null, it will be the same as the file to upload
     * @param host
     * The host of the FTP Server
     * @param port
     * The FTP port number
     * @param path
     * The path in the FTP server where the file has to be uploaded
     * @param login
     * The user's login in the FTP server
     * @param password
     * The user's password
     * @param renameIfExist
     * Specify whether the desitnation file should be renamed or not if it already exists.
     * @exception IOException if an error occurs while uploading the file
     */
    public static void uploadFile(File in, String out, String host, int port, String path, String login, String password, boolean renameIfExist) throws IOException {
        FTPClient ftp = null;
        try {
            m_logCat.info("Uploading " + in + " to " + host + ":" + port + " at " + path);
            ftp = new FTPClient();
            int reply;
            ftp.connect(host, port);
            m_logCat.info("Connected to " + host + "... Trying to authenticate");
            reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                m_logCat.error("FTP server " + host + " refused connection.");
                throw new IOException("Cannot connect to the FTP Server: connection refused.");
            }
            if (!ftp.login(login, password)) {
                ftp.logout();
                throw new IOException("Cannot connect to the FTP Server: login / password is invalid!");
            }
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
            if (!ftp.changeWorkingDirectory(path)) {
                m_logCat.warn("Remote working directory: " + path + "does not exist on the FTP Server ...");
                m_logCat.info("Trying to create remote directory: " + path);
                if (!ftp.makeDirectory(path)) {
                    m_logCat.error("Failed to create remote directory: " + path);
                    throw new IOException("Failed to store " + in + " in the remote directory: " + path);
                }
                if (!ftp.changeWorkingDirectory(path)) {
                    m_logCat.error("Failed to change directory. Unexpected error");
                    throw new IOException("Failed to change to remote directory : " + path);
                }
            }
            if (out == null) {
                out = in.getName();
                if (out.startsWith("/")) {
                    out = out.substring(1);
                }
            }
            if (renameIfExist) {
                String[] files = ftp.listNames();
                String f = in + out;
                for (int i = 0; i < files.length; i++) {
                    if (files[i].equals(out)) {
                        m_logCat.debug("Found existing file on the server: " + out);
                        boolean rename_ok = false;
                        String bak = "_bak";
                        int j = 0;
                        String newExt = null;
                        while (!rename_ok) {
                            if (j == 0) newExt = bak; else newExt = bak + j;
                            if (ftp.rename(out, out + newExt)) {
                                m_logCat.info(out + " renamed to " + out + newExt);
                                rename_ok = true;
                            } else {
                                m_logCat.warn("Renaming to " + out + newExt + " has failed!, trying again ...");
                                j++;
                            }
                        }
                        break;
                    }
                }
            }
            InputStream input = new FileInputStream(in);
            m_logCat.info("Starting transfert of " + in);
            ftp.storeFile(out, input);
            m_logCat.info(in + " uploaded successfully");
            input.close();
            ftp.logout();
        } catch (FTPConnectionClosedException e) {
            m_logCat.error("Server closed connection.", e);
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException f) {
                }
            }
        }
    }

    /**
     * Uploads the specified file to the specified FTP server. If the file already exists it is overwritten
     * @param in
     * The file to upload
     * @param out
     * The file name in the ftp server. If null, it will be the same as the file to upload
     * @param host
     * The host of the FTP Server
     * @param port
     * The FTP port number
     * @param path
     * The path in the FTP server where the file has to be uploaded
     * @param login
     * The user's login in the FTP server
     * @param password
     * The user's password
     * @exception IOException if an error occurs while uploading the file
     */
    public static void uploadFile(File in, String out, String host, int port, String path, String login, String password) throws IOException {
        uploadFile(in, out, host, port, path, login, password, false);
    }
}
