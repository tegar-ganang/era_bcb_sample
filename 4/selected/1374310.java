package net.sf.poormans.server;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.FileNameMap;
import java.net.URLConnection;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Servlet-based helper methods.
 *
 * @version $Id: ServletUtils.java 2122 2011-07-02 09:31:26Z th-schwarz $
 * @author <a href="mailto:th-schwarz@users.sourceforge.net">Thilo Schwarz</a>
 */
public class ServletUtils {

    private static Logger logger = Logger.getLogger(ServletUtils.class);

    private static FileNameMap contentTypes = URLConnection.getFileNameMap();

    public static void establishContentType(String fileName, HttpServletResponse resp) {
        if (StringUtils.isBlank(fileName)) return;
        String contentType = contentTypes.getContentTypeFor(fileName);
        if (StringUtils.isNotBlank(contentType)) resp.setContentType(contentType);
    }

    public static boolean writeFile(HttpServletResponse resp, File reqFile) {
        boolean retVal = false;
        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(reqFile));
            IOUtils.copy(in, resp.getOutputStream());
            logger.debug("File successful written to servlet response: " + reqFile.getAbsolutePath());
        } catch (FileNotFoundException e) {
            logger.error("Resource not found: " + reqFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error(String.format("Error while rendering [%s]: %s", reqFile.getAbsolutePath(), e.getMessage()), e);
        } finally {
            IOUtils.closeQuietly(in);
        }
        return retVal;
    }
}
