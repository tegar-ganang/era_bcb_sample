package meconsea.webcoll.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

public class FileComm {

    private static final Logger LOG = Logger.getLogger(FileComm.class);

    /**
	 * Writes a String to a file creating the file if it does not exist using
	 * the default encoding for the VM.
	 * 
	 * @param pFile
	 * @param pData
	 * @return write successful return true otherwrise false
	 */
    public static boolean writeStringToFile(File pFile, String pData) {
        boolean flag = false;
        try {
            FileUtils.writeStringToFile(pFile, pData);
            flag = true;
        } catch (Exception e) {
            LOG.error("将数据写入到" + pFile.getName() + "出现异常�?", e);
        }
        return flag;
    }

    /**
	 * Writes a String to a file creating the file if it does not exist
	 * 
	 * @param pFile
	 * @param pData
	 * @param pEncoding
	 * @return write successful return true otherwrise false
	 */
    public static boolean writeStringToFile(File pFile, String pData, String pEncoding) {
        boolean flag = false;
        try {
            FileUtils.writeStringToFile(pFile, pData, pEncoding);
            flag = true;
        } catch (Exception e) {
            LOG.error("根据编码" + pEncoding + "将数据写入到" + pFile.getName() + "出现异常�?", e);
        }
        return flag;
    }

    /**
	 * Writes a String to a file creating the file by filePath.
	 * 
	 * @param pFilePath
	 * @param pData
	 * @param pEncoding
	 * @return write successful return true otherwrise false
	 */
    public static boolean writeStringToFile(String pFilePath, String pData, String pEncoding) {
        boolean flag = false;
        try {
            File file = new File(pFilePath);
            flag = writeStringToFile(file, pData, pEncoding);
        } catch (Exception e) {
            LOG.error("将数据写入到" + pFilePath + "出现异常�?", e);
        }
        return flag;
    }

    public static boolean writeLinesToFile(String pFilePath, List lines, String lineEndSpec) {
        boolean flag = false;
        try {
            File file = new File(pFilePath);
            FileUtils.writeLines(file, lines, lineEndSpec);
            flag = true;
        } catch (Exception e) {
            LOG.error("将Lines写到" + pFilePath + "出现异常�?", e);
        }
        return flag;
    }

    /**
	 * Writes a String to a file creating the file by filePath.
	 * 
	 * @param pFilePath
	 * @param pData
	 * @param pEncoding
	 * @return write successful return true otherwrise false
	 */
    public static boolean writeStringToFile(String pFilePath, String pData) {
        boolean flag = false;
        try {
            File file = new File(pFilePath);
            flag = writeStringToFile(file, pData);
        } catch (Exception e) {
            LOG.error("将数据写入到" + pFilePath + "出现异常�?", e);
        }
        return flag;
    }

    /**
	 * write bytes from an inputream to a file.
	 * 
	 * @param pIs
	 * @param filePath
	 * @param pAppend
	 * @return write successful return true otherwrise false
	 */
    public static boolean writeFileByBinary(InputStream pIs, String pFilePath, boolean pAppend) {
        boolean flag = false;
        try {
            flag = writeFileByBinary(pIs, new File(pFilePath), pAppend);
        } catch (Exception e) {
            LOG.error("将字节流写入�?" + pFilePath + "出现异常�?", e);
        }
        return flag;
    }

    /**
	 * write bytes from an inputream to a file.
	 * 
	 * @param pIs
	 * @param filePath
	 * @param pAppend
	 * @return write successful return true otherwrise false
	 */
    public static boolean writeFileByBinary(InputStream pIs, File pFile, boolean pAppend) {
        boolean flag = false;
        try {
            FileOutputStream fos = new FileOutputStream(pFile, pAppend);
            IOUtils.copy(pIs, fos);
            fos.flush();
            fos.close();
            pIs.close();
            flag = true;
        } catch (Exception e) {
            LOG.error("将字节流写入�?" + pFile.getName() + "出现异常�?", e);
        }
        return flag;
    }

    /**
	 * write chars from a reader to a file.
	 * 
	 * @param pIs
	 * @param filePath
	 * @param pAppend
	 * @return write successful return true otherwrise false
	 */
    public static boolean writeFileByChars(Reader pReader, String pFilePath, boolean pAppend) {
        boolean flag = false;
        try {
            flag = writeFileByChars(pReader, new File(pFilePath), pAppend);
        } catch (Exception e) {
            LOG.error("将字符流写入�?" + pFilePath + "出现异常�?", e);
        }
        return flag;
    }

    /**
	 * write chars from a reader to a file.
	 * 
	 * @param pIs
	 * @param filePath
	 * @param pAppend
	 * @return write successful return true otherwrise false
	 */
    public static boolean writeFileByChars(Reader pReader, File pFile, boolean pAppend) {
        boolean flag = false;
        try {
            FileWriter fw = new FileWriter(pFile, pAppend);
            IOUtils.copy(pReader, fw);
            fw.flush();
            fw.close();
            pReader.close();
            flag = true;
        } catch (Exception e) {
            LOG.error("将字符流写入�?" + pFile.getName() + "出现异常�?", e);
        }
        return flag;
    }

    /**
	 * write bytes from an inputstream to a file of chars.
	 * 
	 * @param pIs
	 * @param pFile
	 * @param pAppend
	 * @return
	 */
    public static boolean writeFileB2C(InputStream pIs, File pFile, boolean pAppend) {
        boolean flag = false;
        try {
            FileWriter fw = new FileWriter(pFile, pAppend);
            IOUtils.copy(pIs, fw);
            fw.flush();
            fw.close();
            pIs.close();
            flag = true;
        } catch (Exception e) {
            LOG.error("将字节流写入�?" + pFile.getName() + "出现异常�?", e);
        }
        return flag;
    }

    /**
	 * write bytes from an inputstream to a file of chars.
	 * 
	 * @param pIs
	 * @param pFilePath
	 * @param pAppend
	 * @return
	 */
    public static boolean writeFileB2C(InputStream pIs, String pFilePath, boolean pAppend) {
        boolean flag = false;
        try {
            flag = writeFileB2C(pIs, new File(pFilePath), pAppend);
        } catch (Exception e) {
            LOG.error("将字节流写入�?" + pFilePath + "出现异常�?", e);
        }
        return flag;
    }

    /**
	 * get the contents of a file as a list of Strings, one entry per line.
	 * 
	 * @param pFilePath
	 * @return
	 */
    public static List readLines(String pFilePath) {
        List list = null;
        try {
            FileReader fr = new FileReader(pFilePath);
            list = IOUtils.readLines(fr);
        } catch (FileNotFoundException e) {
            LOG.error(pFilePath + "没有找到�?");
        } catch (IOException e) {
            LOG.error("读取" + pFilePath + "出现异常.", e);
        }
        return list;
    }

    public static String readStrData(String pFilePath) {
        String res = "";
        try {
            FileReader fr = new FileReader(pFilePath);
            res = IOUtils.toString(fr);
        } catch (Exception e) {
            LOG.error("读取文件内容出现异常", e);
        }
        return res;
    }

    public static String readInputStrData(String pFilePath, String encoding) {
        String res = "";
        try {
            if (encoding == null || "".equals(encoding)) {
                encoding = "UTF-8";
            }
            FileInputStream input = new FileInputStream(pFilePath);
            res = new String(IOUtils.toCharArray(input, encoding));
        } catch (Exception e) {
            LOG.error("读取文件内容出现异常", e);
        }
        return res;
    }

    public static boolean appendStr2File(String pFilePath, String content, boolean pAppend) {
        try {
            FileWriter fw = new FileWriter(pFilePath, pAppend);
            RandomAccessFile rf = new RandomAccessFile(pFilePath, "rw");
            if (rf.length() == 0) {
                fw.write(content);
            } else {
                fw.write(IOUtils.LINE_SEPARATOR_UNIX + content);
            }
            rf.close();
            fw.close();
            return true;
        } catch (Exception e) {
            LOG.error("追加文件出错" + e.getMessage());
        }
        return false;
    }

    /**
	 * create mutilDirectory 
	 * @param parentDir
	 * @param childrenDir '/' begin
	 * @return
	 */
    public static boolean createMutilDir(String parentDir, String childrenDir) {
        File pDir = new File(parentDir);
        if (pDir.exists() && pDir.isDirectory()) {
            File subDir = new File(parentDir + childrenDir);
            if (!subDir.exists()) {
                return subDir.mkdirs();
            }
            return true;
        } else {
            return false;
        }
    }
}
