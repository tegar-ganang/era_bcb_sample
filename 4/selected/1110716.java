package org.jdeluxe.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import org.eclipse.core.resources.IFile;
import org.jdeluxe.log.JdxLog;

/**
 * The Class FileUtils.
 */
public class FileUtils {

    /**
	 * Copy a file from sopurce to destination.
	 * 
	 * @param sFileSrc source path
	 * @param sFileDst destination path
	 * 
	 * @return true if successful
	 */
    public static boolean fileCopy(String sFileSrc, String sFileDst) {
        boolean ok = true;
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            File fSrc = new File(sFileSrc);
            int len = 32768;
            byte[] buff = new byte[(int) Math.min(len, fSrc.length())];
            fis = new FileInputStream(fSrc);
            boolean append = false;
            fos = new FileOutputStream(sFileDst, append);
            while (0 < (len = fis.read(buff))) fos.write(buff, 0, len);
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
            ok = false;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JdxLog.logError(ex);
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JdxLog.logError(ex);
                }
            }
        }
        return ok;
    }

    /**
	 * Cops inputstream to a file.
	 * 
	 * @param is the inputstream
	 * @param sFileDst the file destination
	 * 
	 * @return true is successful
	 */
    public static boolean fileCopy(InputStream is, String sFileDst) {
        boolean ok = true;
        FileOutputStream fos = null;
        try {
            int len = 32768;
            byte[] buff = new byte[len];
            boolean append = false;
            fos = new FileOutputStream(sFileDst, append);
            while (0 < (len = is.read(buff))) fos.write(buff, 0, len);
            fos.flush();
        } catch (IOException e) {
            JdxLog.logError(e);
            ok = false;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JdxLog.logError(ex);
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JdxLog.logError(ex);
                }
            }
        }
        return ok;
    }

    /**
	 * Read an IFile to a String.
	 * 
	 * @param file the input file
	 * 
	 * @return String containing the file content
	 */
    public static String readFile(IFile file) {
        if (!file.exists()) return "";
        InputStream stream = null;
        try {
            stream = file.getContents();
            Reader reader = new BufferedReader(new InputStreamReader(stream));
            StringBuffer result = new StringBuffer(2048);
            char[] buf = new char[2048];
            while (true) {
                int count = reader.read(buf);
                if (count < 0) break;
                result.append(buf, 0, count);
            }
            return result.toString();
        } catch (Exception e) {
            JdxLog.logError(e);
            return null;
        } finally {
            try {
                if (stream != null) stream.close();
            } catch (IOException ex) {
                ex.printStackTrace();
                JdxLog.logError(ex);
                return "";
            }
        }
    }
}
