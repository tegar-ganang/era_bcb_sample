package br.com.felix.fwt.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import br.com.felix.fwt.log.LoggerFactory;

public class ImageServlet extends HttpServlet {

    private static final long serialVersionUID = 748549551350579733L;

    private static Logger logger = LoggerFactory.getLogger(ImageServlet.class);

    private static File parentFolder;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String requested = request.getRequestURI();
        String servletPath = request.getServletPath();
        int index = requested.indexOf(servletPath);
        String requestedImage = requested.substring(index + servletPath.length());
        requestedImage = URLDecoder.decode(requestedImage, "ISO-8859-1");
        if (requestedImage.startsWith("/")) {
            requestedImage = requestedImage.substring(1);
        }
        logger.info("Requested image = " + requestedImage);
        File image = new File(parentFolder, requestedImage);
        if (!image.exists()) {
            logger.error("Requested image '" + requestedImage + "' was not found.");
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } else {
            response.setContentType("image/jpeg");
            OutputStream os = response.getOutputStream();
            FileInputStream fis = new FileInputStream(image);
            int read;
            byte[] buff = new byte[1024];
            while ((read = fis.read(buff)) > 0) {
                os.write(buff, 0, read);
            }
            os.flush();
            fis.close();
        }
    }

    @Override
    public void init() throws ServletException {
        super.init();
        configure();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        configure();
    }

    private void configure() {
        synchronized (ImageServlet.class) {
            String realPath = getImagesFolderRealPath();
            parentFolder = new File(realPath);
            if (!parentFolder.exists()) {
                logger.fatal("ImageServlet is not configured properly. Please review web.xml configuration of init param 'real-path'.");
                parentFolder = null;
            } else {
                logger.info("Images folder configured to '" + parentFolder.getAbsolutePath() + "'.");
            }
        }
    }

    protected String getImagesFolderRealPath() {
        return getInitParameter("real-path");
    }
}
