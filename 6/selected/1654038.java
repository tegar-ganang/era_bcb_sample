package ethan;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.SocketException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

public class UploadBackup2 {

    private static PrintStream out;

    private static boolean isSuccess = false;

    /**
	 * @param args
	 * @throws IOException
	 * @throws SocketException
	 */
    public static void main(String[] args) {
        int tryTimes = 0;
        while (!processUpload(args) && tryTimes++ < 5) {
            System.out.println("Upload Faild, try again");
        }
        out.flush();
        out.println("end the backup at " + new Date());
        out.println("-----------------------------------------------------------------------------------------------");
        if (out != null) {
            out.close();
        }
    }

    private static boolean processUpload(String[] args) {
        if (args.length != 6) {
            System.out.println("Please specify the right parameter");
            System.out.println("Current args number:" + args.length);
            System.exit(1);
        }
        String ftpHost = args[0];
        String ftpUsername = args[1];
        String ftpPassword = args[2];
        String remoteDir = args[3];
        String localDir = args[4];
        try {
            out = new PrintStream(new FileOutputStream(args[5], true));
            System.setOut(out);
            System.setErr(out);
        } catch (FileNotFoundException e2) {
            e2.printStackTrace();
            out = System.out;
        }
        String fileName = "oso_" + new Date().getTime() + ".sql";
        try {
            String cmd = "mysqldump -uroot -pwelcome2u onsiteoffice > " + localDir + "/" + fileName;
            out.println(cmd);
            Process p = Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c", cmd });
            InputStream is = p.getInputStream();
            int data;
            while ((data = is.read()) != -1) {
                out.print((char) data);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        out.println("Begin backup at " + new Date());
        FTPClient ftp = null;
        try {
            ftp = getFtpClient(ftpHost, ftpUsername, ftpPassword);
            if (ftp == null) {
                return false;
            }
            if (new File(localDir + "/" + fileName).exists()) {
                uploadFile(ftp, localDir + "/" + fileName, remoteDir + "/" + fileName);
            }
            isSuccess = true;
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

    private static FTPClient getFtpClient(String ftpHost, String ftpUsername, String ftpPassword) throws SocketException, IOException {
        FTPClient ftp = new FTPClient();
        ftp.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
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

    private static boolean uploadFile(FTPClient ftp, String localFile, String remoteFile) throws IOException {
        InputStream input = new FileInputStream(localFile);
        boolean isSuccess = ftp.storeFile(remoteFile, input);
        input.close();
        return isSuccess;
    }
}
