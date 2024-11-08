package com.ideo.sweetdevria.taglib.fileUpload.action;

import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.ideo.sweetdevria.servlet.IAction;
import com.ideo.sweetdevria.util.URLUtils;

/**
 * Action triggered on export excel 
 *
 */
public class FileuploadAction implements IAction {

    private static final Log LOG = LogFactory.getLog(FileuploadAction.class);

    public void execute(HttpServletRequest req, HttpServletResponse res, HttpServlet parent) throws Exception {
        String path = req.getParameter("path");
        path = URLDecoder.decode(path, "UTF-8");
        String name = req.getParameter("name");
        name = new String(name.getBytes(), "UTF-8");
        String contentType = req.getParameter("contentType");
        if (path == null) {
            NullPointerException e = new NullPointerException("The path attribute cannot be retrieved.");
            LOG.error(e);
            throw e;
        }
        URL url = new URL(path);
        InputStream inStream = null;
        try {
            inStream = URLUtils.getFileContent(url, req.getSession().getId());
            res.setContentType(contentType);
            res.addHeader("Content-Disposition", "attachment;filename=\"" + name + "\"");
            ServletOutputStream out = res.getOutputStream();
            IOUtils.copy(inStream, out);
            res.flushBuffer();
        } finally {
            if (inStream != null) {
                inStream.close();
            }
        }
    }
}
