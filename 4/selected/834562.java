package jwebapp.request;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletResponse;
import jwebapp.RequestHandler;
import jwebapp.ServerInterface;

/**
 * Provides a secure method for downloads of any file, including files that can not be 
 * accessed via a normal web request.
 *
 * <p>DownloadHandler expects a jWebApp request parameter named 'filename' that contains the name 
 * of a file that will be returned to the client via the files expected content type.  If the 
 * jWebApp request parameter is not defined then a normal request parameter should be defined.
 *
 * <p>DownloadHandler also looks for a jWebApp request parameter named 'directory'.  If directory 
 * is defined then the file defined in the request parameter can only be retreived from the 
 * download directory.  The filename defined in the request parameter should be relative to the 
 * directory.  This should always be defined!  If it is not defined, then the file parameter 
 * should not be accessable to the user, who could potentially download anything.
 *
 * <p>DownloadHandler can access files anywhere and uses ServletContext.getResource() to 
 * locate the file, so the file name can be relative to the web context. DownloadHandler allows 
 * you to protect downloads via security roles, and gives you the ability to narrow the download to a 
 * specific directory and/or file.  Therefore, DownloadHandler provides a highly secure method for 
 * file downloads.
 */
public class Download extends RequestHandler {

    private static Logger logger = Logger.getLogger(Download.class.getName());

    public String processRequest(ServerInterface serverInterface) {
        String filename = serverInterface.getRequestData().getParameter("filename");
        String directory = serverInterface.getRequestData().getParameter("directory");
        if (filename == null) filename = serverInterface.getParameter("filename");
        if (directory != null) filename = directory + File.separator + filename;
        try {
            InputStream in = serverInterface.getServletContext().getResourceAsStream(filename);
            if (in == null) {
                if (logger.isLoggable(Level.FINER)) logger.finer("filename resource '" + filename + "' was not located");
                serverInterface.getServletResponse().sendError(HttpServletResponse.SC_NOT_FOUND, "filename resource '" + filename + "' was not located");
            } else {
                serverInterface.getServletResponse().setContentType(serverInterface.getServletContext().getMimeType(filename));
                serverInterface.getServletResponse().addHeader("content-disposition", "attachment; filename=" + new File(filename).getName());
                OutputStream out = serverInterface.getServletResponse().getOutputStream();
                byte buf[] = new byte[2048];
                int numberRead = 0;
                while ((numberRead = in.read(buf)) != -1) out.write(buf, 0, numberRead);
                in.close();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.toString(), e);
            serverInterface.setErrorMessage(ServerInterface.UNDEFINED_ERROR, e.toString(), false);
        }
        return NO_FORWARDING;
    }
}
