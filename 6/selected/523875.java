package logParse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Date;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import tools.Constants;

public class FTPConnection {

    private static int connectTimeout = 0;

    /**
	 * 读取配置文件，得到设置信息
	 */
    static {
        String timeStr = LogicProcess.readProperties(Constants.PROPERTY_FILE_PATH, "FtpConnectTimeout");
        connectTimeout = Integer.valueOf(timeStr);
    }

    /**
	 * 登录FTP服务器下载文件至本地
	 * @param url 服务器地址
	 * @param username 用户名
	 * @param password 口令
	 * @param remotePath 远程文件路径
	 * @param statrFileName 作判定条件的文件名
	 * @param localPath 本地下载路径
	 * @return 若连接下载失败，返回false；若成功，返回true。
	 * @throws FileNotFoundException 
	 */
    public static boolean downFile(String url, String username, String password, String remotePath, Date DBLastestDate, String localPath) {
        File dFile = new File(localPath);
        if (!dFile.exists()) {
            dFile.mkdir();
        }
        boolean success = false;
        FTPClient ftp = new FTPClient();
        ftp.setConnectTimeout(connectTimeout);
        System.out.println("FTP begin!!");
        try {
            int reply;
            ftp.connect(url);
            ftp.login(username, password);
            reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                return success;
            }
            ftp.changeWorkingDirectory(remotePath);
            String[] filesName = ftp.listNames();
            if (DBLastestDate == null) {
                System.out.println(" 初次下载，全部下载 ");
                for (String string : filesName) {
                    if (!string.matches("[0-9]{12}")) {
                        continue;
                    }
                    File localFile = new File(localPath + "/" + string);
                    OutputStream is = new FileOutputStream(localFile);
                    ftp.retrieveFile(string, is);
                    is.close();
                }
            } else {
                System.out.println(" 加一下载 ");
                Date date = DBLastestDate;
                long ldate = date.getTime();
                Date nowDate = new Date();
                String nowDateStr = Constants.DatetoString(nowDate, Constants.Time_template_LONG);
                String fileName;
                do {
                    ldate += 60 * 1000;
                    Date converterDate = new Date(ldate);
                    fileName = Constants.DatetoString(converterDate, Constants.Time_template_LONG);
                    File localFile = new File(localPath + "/" + fileName);
                    OutputStream is = new FileOutputStream(localFile);
                    if (!ftp.retrieveFile(fileName, is)) {
                        localFile.delete();
                    }
                    is.close();
                } while (fileName.compareTo(nowDateStr) < 0);
            }
            ftp.logout();
            success = true;
        } catch (IOException e) {
            System.out.println("FTP timeout return");
            e.printStackTrace();
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException ioe) {
                }
            }
        }
        return success;
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
    }
}
