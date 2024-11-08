package xlion.maildisk.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.log4j.Logger;

public class FileUtil {

    /**
	 * Logger for this class
	 */
    private static final Logger logger = Logger.getLogger(FileUtil.class);

    public static void copyFile(String oldPath, String newPath) throws IOException {
        int bytesum = 0;
        int byteread = 0;
        File oldfile = new File(oldPath);
        if (oldfile.exists()) {
            InputStream inStream = new FileInputStream(oldPath);
            FileOutputStream fs = new FileOutputStream(newPath);
            byte[] buffer = new byte[1444];
            while ((byteread = inStream.read(buffer)) != -1) {
                bytesum += byteread;
                fs.write(buffer, 0, byteread);
            }
            inStream.close();
        }
    }

    public static boolean isFileEqual(String fileName1, String fileName2) {
        File file1 = new File(fileName1);
        if (file1.exists()) {
            File file2 = new File(fileName2);
            if (file2.exists()) {
                try {
                    FileInputStream stream1 = new FileInputStream(fileName1);
                    FileInputStream stream2 = new FileInputStream(fileName2);
                    byte b1[] = new byte[100000];
                    byte b2[] = new byte[100000];
                    int lenB1 = 0;
                    int lenB2 = 0;
                    while ((lenB1 = stream1.read(b1)) != -1) {
                        lenB2 = stream2.read(b2);
                        if (lenB1 != lenB2) {
                            return false;
                        }
                        for (int i = 0; i < lenB1; i++) {
                            if (b1[i] != b2[i]) {
                                return false;
                            }
                        }
                    }
                } catch (IOException e) {
                    logger.warn("isFileEqual(String, String) - exception ignored", e);
                    return false;
                }
                return true;
            } else {
                if (logger.isInfoEnabled()) {
                    logger.info("file not exist) - fileName2=" + fileName2);
                }
                return false;
            }
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("file not exist) - fileName1=" + fileName1);
            }
            return false;
        }
    }
}
