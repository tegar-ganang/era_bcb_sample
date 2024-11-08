package com.bureauveritas.photolibrary;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;

/**
 * @author Nicolas Richeton / Smile
 */
public class FileServlet extends HttpServlet {

    protected String LIBRARY_PATH = null;

    /**
     * @see javax.servlet.http.HttpServlet#void
     *      (javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getParameter("path");
        String name = req.getParameter("name");
        if (!PathUtils.isValid(path + "/" + name)) {
            return;
        }
        File jpegFile = new File(LIBRARY_PATH + path + "/" + name);
        byte[] result = null;
        FileInputStream fis = new FileInputStream(jpegFile);
        try {
            output(resp, fis, jpegFile.length(), jpegFile.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        fis.close();
    }

    private void output(HttpServletResponse resp, InputStream is, long length, String fileName) throws Exception {
        resp.reset();
        String mimeType = "image/jpeg";
        resp.setContentType(mimeType);
        resp.setContentLength((int) length);
        resp.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");
        resp.setHeader("Cache-Control", "must-revalidate");
        ServletOutputStream sout = resp.getOutputStream();
        IOUtils.copy(is, sout);
        sout.flush();
        resp.flushBuffer();
    }

    public void init() throws ServletException {
        InputStream is = ThumbnailServlet.class.getResourceAsStream("/path.properties");
        Properties p = new Properties();
        try {
            p.load(is);
            LIBRARY_PATH = p.getProperty("path");
        } catch (IOException e) {
            throw new ServletException();
        }
    }
}
