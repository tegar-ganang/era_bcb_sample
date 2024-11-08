package kr.or.common.util;

import java.io.*;
import java.net.URLEncoder;
import java.util.*;
import javax.servlet.http.HttpServletResponse;
import kr.or.javacafe.member.controller.MemberController;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;

public class FileUpDownUtil {

    private static Logger logger = LoggerFactory.getLogger(FileUpDownUtil.class);

    public static int fileSize(String strFilePath, String strFileName) {
        String SEPARATOR = System.getProperty("file.separator");
        File f = new File(strFilePath + SEPARATOR + strFileName);
        if (f.length() > 0) {
            return (int) f.length();
        } else {
            return 0;
        }
    }

    public static int fileCopy(String strSourceFilePath, String strDestinationFilePath, String strFileName) throws IOException {
        String SEPARATOR = System.getProperty("file.separator");
        File dir = new File(strSourceFilePath);
        if (!dir.exists()) dir.mkdirs();
        File realDir = new File(strDestinationFilePath);
        if (!realDir.exists()) realDir.mkdirs();
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(new File(strSourceFilePath + SEPARATOR + strFileName));
            fos = new FileOutputStream(new File(strDestinationFilePath + SEPARATOR + strFileName));
            IOUtils.copy(fis, fos);
        } catch (Exception ex) {
            return -1;
        } finally {
            try {
                fos.close();
                fis.close();
            } catch (Exception ex2) {
            }
        }
        return 0;
    }

    public static int fileUpload(long lngFileSize, InputStream inputStream, String strFilePath, String strFileName) throws IOException {
        String SEPARATOR = System.getProperty("file.separator");
        if (lngFileSize > (10 * 1024 * 1024)) {
            return -1;
        }
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            File dir = new File(strFilePath);
            if (!dir.exists()) dir.mkdirs();
            is = inputStream;
            fos = new FileOutputStream(new File(strFilePath + SEPARATOR + strFileName));
            IOUtils.copy(is, fos);
        } catch (Exception ex) {
            return -2;
        } finally {
            try {
                fos.close();
                is.close();
            } catch (Exception ex2) {
            }
        }
        return 0;
    }

    public static int fileDownload(HttpServletResponse response, String strFilePath, String strFileName, String strFileContentType, String strRealFileName) throws IOException {
        String SEPARATOR = System.getProperty("file.separator");
        File objRealFile = new File(strFilePath + SEPARATOR + strFileName);
        if (objRealFile.length() > 0) {
            response.setContentType(strFileContentType);
            response.setContentLength((int) objRealFile.length());
            response.setHeader("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode(strRealFileName, "UTF-8") + "\"");
            FileCopyUtils.copy(new FileInputStream(objRealFile), response.getOutputStream());
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Image File Not Found!!!!!");
            }
            return -1;
        }
        return 0;
    }

    public static int imageDownload(HttpServletResponse response, String strFilePath, String strFileName) throws IOException {
        String[] arrTemp = strFileName.split("[.]");
        if (arrTemp.length < 2) {
            return -1;
        }
        String strFileContentType = "";
        String strType = arrTemp[arrTemp.length - 1].toLowerCase();
        if (strType.equals("gif")) {
            strFileContentType = "image/gif";
        } else if (strType.equals("jpe") || strType.equals("jpeg") || strType.equals("jpg")) {
            strFileContentType = "image/jpeg";
        } else if (strType.equals("png")) {
            strFileContentType = "image/png";
        }
        if (strFileContentType.equals("")) {
            return -1;
        }
        return fileDownload(response, strFilePath, strFileName, strFileContentType, strFileName);
    }
}
