package org.jdna.bmt.web.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import sagex.remote.media.MediaHandler;

public class MediaHandlerServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private transient MediaHandler mediaHandler = null;

    private transient Logger log = null;

    public MediaHandlerServlet() {
        ServicesInit.init();
        mediaHandler = new MediaHandler();
        log = Logger.getLogger(MediaHandler.class);
        log.debug("BMT Media Servlet Created");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getParameter("i") != null) {
            String img = req.getParameter("i");
            if (img == null) {
                resp.sendError(404, "Image was null");
                return;
            }
            File f = null;
            if (img.startsWith("file")) {
                try {
                    f = new File(new URI(img));
                } catch (URISyntaxException e) {
                    resp.sendError(500, e.getMessage());
                    return;
                }
            } else {
                f = new File(img);
            }
            if (f.exists()) {
                f = f.getCanonicalFile();
                if (f.getName().endsWith(".jpg") || f.getName().endsWith(".png")) {
                    resp.setContentType("image/png");
                    FileInputStream fis = null;
                    OutputStream os = resp.getOutputStream();
                    try {
                        fis = new FileInputStream(f);
                        IOUtils.copy(fis, os);
                    } finally {
                        os.flush();
                        if (fis != null) fis.close();
                    }
                }
            }
            return;
        }
        String mediaUrl = "/media" + req.getPathInfo();
        String parts[] = mediaUrl.split("/");
        mediaHandler.handleRequest(parts, req, resp);
    }

    private void proxyMediaServlet(String url, HttpServletRequest req, HttpServletResponse resp) {
        try {
            URL u = new URL("http://mediaserver:8080/sagex" + url);
            log.debug("Proxy Media: " + u.toString());
            URLConnection c = u.openConnection();
            c.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.1) Gecko/2008072820 Firefox/3.0.1");
            OutputStream os = resp.getOutputStream();
            IOUtils.copy(c.getInputStream(), os);
            os.flush();
            resp.flushBuffer();
        } catch (Throwable t) {
            log.error("Failed to get url: " + url, t);
            try {
                resp.sendError(500, t.getMessage());
            } catch (IOException e) {
            }
        }
    }
}
