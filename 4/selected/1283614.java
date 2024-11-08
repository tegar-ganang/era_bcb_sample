package net.sf.poormans.view.renderer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.http.HttpServletResponse;
import net.sf.poormans.tool.PathTool;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

/**
 * Rendering object to render static sources. If not found, 404 will be sent.
 * 
 * @version $Id: StaticRenderer.java 1418 2008-07-04 13:00:10Z th-schwarz $
 * @author <a href="mailto:th-schwarz@users.sourceforge.net">Thilo Schwarz</a>
 */
public class StaticRenderer {

    private static Logger logger = Logger.getLogger(StaticRenderer.class);

    /** Name of the file has to be rendered. */
    private String fileName;

    private HttpServletResponse servletResponse;

    /** Indicates if 'fileName' was found or not. */
    private boolean isNotFound = false;

    public StaticRenderer(final String requestedResource, HttpServletResponse servletResponse) {
        this.fileName = PathTool.getFSPathOfResource(requestedResource);
        this.servletResponse = servletResponse;
    }

    /**
	 * Do the rendering
	 * 
	 * @throws IOException
	 */
    public void doRender() throws IOException {
        File file = new File(fileName);
        if (!file.exists()) {
            logger.error("Static resource not found: " + fileName);
            isNotFound = true;
            return;
        }
        if (fileName.endsWith("xml") || fileName.endsWith("asp")) servletResponse.setContentType("text/xml"); else if (fileName.endsWith("css")) servletResponse.setContentType("text/css"); else if (fileName.endsWith("js")) servletResponse.setContentType("text/javascript");
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            IOUtils.copy(in, servletResponse.getOutputStream());
            logger.debug("Static resource rendered: ".concat(fileName));
        } catch (FileNotFoundException e) {
            logger.error("Static resource not found: " + fileName);
            isNotFound = true;
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    /**
	 * @return <code>true</code>, if 'filename' wasn't found, otherwise <code>false</code>.
	 */
    public boolean isNotFound() {
        return this.isNotFound;
    }
}
