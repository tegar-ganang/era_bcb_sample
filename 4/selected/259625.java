package com.incendiaryblue.file;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletOutputStream;
import java.io.*;

/**
 * Download manager servelet to force download of files through user browser
 */
public class FileDownloadManager extends FileUploadManager {

    public void processResponse(HttpServletRequest request, HttpServletResponse resp, String fileName, String filePath, String selectedPath) throws IOException {
        File file = new File(filePath + selectedPath + fileName);
        InputStream inS = new FileInputStream(file);
        resp.setContentType("application/x-download");
        resp.setContentLength((int) file.length());
        resp.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        ServletOutputStream o = resp.getOutputStream();
        BufferedInputStream buf = new BufferedInputStream(inS);
        byte[] data = new byte[2048];
        int readed = buf.read(data);
        while (readed != -1) {
            o.write(data, 0, readed);
            readed = buf.read(data);
        }
        o.flush();
        o.close();
        buf.close();
    }
}
