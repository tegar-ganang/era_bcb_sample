package ethan;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

public class FtpDownload {

    private static int fileCount = 1;

    /**
	 * @param args
	 * @throws IOException
	 * @throws SocketException
	 */
    public static void main(String[] args) {
        int tryTimes = 0;
        while (!processDownload(args) && tryTimes++ < 5) {
            System.out.println("Upload Faild, try again");
        }
    }

    private static boolean processDownload(String[] args) {
        if (args.length != 5) {
            System.out.println("Please specify the right parameter");
            System.exit(1);
        }
        String ftpHost = args[0];
        String ftpUsername = args[1];
        String ftpPassword = args[2];
        String remoteDir = args[3];
        String localDir = args[4];
        FTPClient ftp = null;
        try {
            ftp = getFtpClient(ftpHost, ftpUsername, ftpPassword);
            if (ftp == null) {
                return false;
            }
            System.out.println("begin to download");
            downloadFolder(ftp, localDir, remoteDir);
            System.out.println("finish to download");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (ftp != null) {
                try {
                    ftp.logout();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                if (ftp.isConnected()) {
                    try {
                        ftp.disconnect();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return true;
    }

    private static void downloadFolder(FTPClient ftp, String localFolder, String remoteFolder) {
        System.out.println("download Folder " + remoteFolder);
        try {
            File localFolderFile = new File(localFolder);
            if (!localFolderFile.exists()) {
                localFolderFile.mkdirs();
                System.out.println("make Folder " + remoteFolder);
            }
            FTPFile[] files = ftp.listFiles(remoteFolder);
            for (FTPFile ftpFile : files) {
                if (ftpFile.isFile()) {
                    downloadFile(ftp, localFolder + "/" + ftpFile.getName(), remoteFolder + "/" + ftpFile.getName());
                } else if (ftpFile.isDirectory() && !ftpFile.getName().equals(".") && !ftpFile.getName().equals("..")) {
                    downloadFolder(ftp, localFolder + "/" + ftpFile.getName(), remoteFolder + "/" + ftpFile.getName());
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static boolean downloadFile(FTPClient ftp, String localFile, String remoteFile) {
        System.out.println("download file " + remoteFile + " --- file count:" + fileCount++);
        OutputStream output = null;
        try {
            output = new FileOutputStream(localFile);
            boolean isSuccess = ftp.retrieveFile(remoteFile, output);
            return isSuccess;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static FTPClient getFtpClient(String ftpHost, String ftpUsername, String ftpPassword) throws SocketException, IOException {
        FTPClient ftp = new FTPClient();
        ftp.connect(ftpHost);
        int reply = ftp.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftp.disconnect();
            return null;
        }
        if (!ftp.login(ftpUsername, ftpPassword)) {
            return null;
        }
        ftp.setFileType(FTP.BINARY_FILE_TYPE);
        ftp.enterLocalPassiveMode();
        return ftp;
    }
}
