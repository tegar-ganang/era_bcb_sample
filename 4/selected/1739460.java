package mecca.util;

import java.io.FileInputStream;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author Shaiful Nizam Tajul
 * @version 1.01
 */
public class Download extends HttpServlet {

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doPost(req, res);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        ServletOutputStream out = res.getOutputStream();
        HttpSession session = req.getSession();
        Logger log = new Logger("infusion.util.Download");
        String dir = req.getParameter("filepath");
        String name = req.getParameter("filename");
        String sourceFilePathName = dir + "/" + name;
        log.setMessage("dir = " + dir);
        log.setMessage("name = " + name);
        res.setContentType("application/x-msdownload");
        res.setHeader("Content-Disposition", "attachment;filename=" + getFileName(sourceFilePathName));
        int readBytes = 0;
        int totalRead = 0;
        int blockSize = 65000;
        FileInputStream fileIn = null;
        try {
            java.io.File file = new java.io.File(sourceFilePathName);
            fileIn = new FileInputStream(file);
            long fileLen = file.length();
            res.setContentLength((int) fileLen);
            byte b[] = new byte[blockSize];
            while ((long) totalRead < fileLen) {
                readBytes = fileIn.read(b, 0, blockSize);
                totalRead += readBytes;
                out.write(b, 0, readBytes);
            }
        } catch (java.io.IOException ex) {
        } finally {
            if (fileIn != null) fileIn.close();
        }
    }

    private String getFileName(String filePathName) {
        String token = new String();
        String value = new String();
        int pos = 0;
        int i = 0;
        int start = 0;
        int end = 0;
        pos = filePathName.lastIndexOf('/');
        if (pos != -1) return filePathName.substring(pos + 1, filePathName.length());
        pos = filePathName.lastIndexOf('\\');
        if (pos != -1) return filePathName.substring(pos + 1, filePathName.length()); else return filePathName;
    }
}
