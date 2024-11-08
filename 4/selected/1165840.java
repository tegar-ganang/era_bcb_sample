package org.esk.dablog.web.servlets;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;
import org.esk.dablog.ApplicationConstants;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;

/**
 * This class
 * User: jc
 * Date: 24.11.2006
 * Time: 23:39:44
 * $Id:$
 */
public class ImageServlet extends HttpServlet {

    private static final String PATH_TO_IMAGES_PARAMETER = "pathToImages";

    private String pathToImages;

    public void init(ServletConfig servletConfig) throws ServletException {
        this.pathToImages = servletConfig.getInitParameter(PATH_TO_IMAGES_PARAMETER);
        super.init(servletConfig);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String[] path = StringUtils.split(request.getRequestURI(), "/");
        String file = path[path.length - 1];
        File f = new File(pathToImages + "/" + file);
        response.setContentType(getServletContext().getMimeType(f.getName()));
        FileInputStream fis = new FileInputStream(f);
        IOUtils.copy(fis, response.getOutputStream());
        fis.close();
    }

    public void setPathToImages(String pathToImages) {
        this.pathToImages = pathToImages;
    }
}
