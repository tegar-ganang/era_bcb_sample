package ispyb.client.results.image;

import ispyb.client.util.ClientLogger;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

/**
 * Class with utils to manage the files
 * 
 * @author ricardo.leal@esrf.fr
 * @version 0.1
 */
public class FileUtil {

    /**
     *  
     */
    public FileUtil() {
    }

    /**
     * Gunzip a local file
     * 
     * @param sourceFileName
     * @return
     */
    public static byte[] readBytes(String sourceFileName) {
        ByteArrayOutputStream outBuffer = null;
        FileInputStream inFile = null;
        BufferedInputStream bufInputStream = null;
        try {
            outBuffer = new ByteArrayOutputStream();
            inFile = new FileInputStream(sourceFileName);
            bufInputStream = new BufferedInputStream(inFile);
            byte[] tmpBuffer = new byte[8 * 1024];
            int n = 0;
            while ((n = bufInputStream.read(tmpBuffer)) >= 0) outBuffer.write(tmpBuffer, 0, n);
        } catch (FileNotFoundException fnf) {
            ClientLogger.getInstance().error("[readBytes] File not found :" + fnf.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inFile != null) {
                try {
                    inFile.close();
                } catch (IOException ioex) {
                }
            }
            if (outBuffer != null) {
                try {
                    outBuffer.close();
                } catch (IOException ioex) {
                }
            }
            if (bufInputStream != null) {
                try {
                    bufInputStream.close();
                } catch (IOException ioex) {
                }
            }
        }
        return outBuffer.toByteArray();
    }

    public static String fileToString(String sourceFileName) {
        BufferedReader inFile = null;
        String output = new String();
        try {
            inFile = new BufferedReader(new FileReader(sourceFileName));
            String s = new String();
            while ((s = inFile.readLine()) != null) output += s + "\n";
            inFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inFile != null) {
                try {
                    inFile.close();
                } catch (IOException ioex) {
                    output = "nofile";
                    return output;
                }
            }
        }
        return output;
    }

    /**
     * downloadFile
     * @param fullFilePath
     * @param mimeType
     * @param response
     */
    public static void DownloadFile(String fullFilePath, String mimeType, String attachmentFilename, HttpServletResponse response) {
        try {
            byte[] imageBytes = FileUtil.readBytes(fullFilePath);
            response.setContentLength(imageBytes.length);
            ServletOutputStream out = response.getOutputStream();
            response.setHeader("Pragma", "public");
            response.setHeader("Cache-Control", "max-age=0");
            response.setContentType(mimeType);
            response.setHeader("Content-Disposition", "attachment; filename=" + attachmentFilename);
            out.write(imageBytes);
            out.flush();
            out.close();
        } catch (FileNotFoundException fnf) {
            ClientLogger.getInstance().debug("[DownloadFile] File not found: " + fullFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
