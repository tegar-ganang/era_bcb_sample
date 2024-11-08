package org.kwantu.modelbrowser;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.net.URLDecoder;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kwantu.m2.KwantuFaultException;

/**
 *Sometimes an image is stored in a path outside the web container and this 
 * make it impossible for a client to access that image directly by relative 
 * URI. This class creates a Servlet which loads the image from a path outside
 * the web container and then streams the image to the HttpServletResponse. 
 */
public class KwantuImageServlet extends HttpServlet {

    private static final Log LOG = LogFactory.getLog(KwantuImageServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String imageFilePath = System.getProperty("java.io.tmpdir");
        String imageFileName = req.getParameter("file");
        if (imageFileName == null) {
            LOG.info("ImageFileName not supplied to request ");
            return;
        }
        imageFileName = URLDecoder.decode(imageFileName, "UTF-8");
        File imageFile = new File(imageFilePath, imageFileName);
        if (!imageFile.exists()) {
            LOG.info("ImageFile does not exist");
            return;
        }
        String contentType = URLConnection.guessContentTypeFromName(imageFileName);
        if (contentType == null || !contentType.startsWith("image")) {
            LOG.info("ContentType not an image");
            return;
        }
        BufferedInputStream input = null;
        BufferedOutputStream output = null;
        try {
            input = new BufferedInputStream(new FileInputStream(imageFile));
            int contentLength = input.available();
            res.reset();
            res.setContentLength(contentLength);
            res.setContentType(contentType);
            res.setHeader("Content-disposition", "inline; filename=\"" + imageFileName + "\"");
            output = new BufferedOutputStream(res.getOutputStream());
            while (contentLength-- > 0) {
                output.write(input.read());
            }
            output.flush();
        } catch (IOException e) {
            throw new KwantuFaultException("Problem writing to response ", e);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    LOG.error("Closing InputStream ", e);
                }
            }
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    LOG.error("Closing OutputStream ", e);
                }
            }
        }
    }
}
