package net.sf.webwarp.util.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.webwarp.util.types.MimeType;
import net.sf.webwarp.util.types.MimeTypeRegistry;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;
import org.springframework.web.servlet.HttpServletBean;

/**
 * Servlet that maps a URI to a folder on the filesystem. Sevlet must be mapped with a URI prefix mapping (not a URI
 * extension)!
 * <p>
 * the resouce to load is mapped to the {@link HttpServletRequest#getPathInfo()}.
 * 
 * @author mos
 */
public class FolderMappingServlet extends HttpServletBean {

    private static final long serialVersionUID = -4830424448782372006L;

    private static final Logger log = Logger.getLogger(FolderMappingServlet.class);

    public static final String MAPPED_FOLDER_KEY = "mappedFolder";

    private File mappedFolder;

    public FolderMappingServlet() {
        MimeTypeRegistry.initDefaults();
        super.addRequiredProperty(MAPPED_FOLDER_KEY);
    }

    @Override
    protected final void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (beforeServingFile(req, resp)) {
            String pathInfo = req.getPathInfo();
            Validate.notNull(pathInfo, "the path info is null -> the sevlet should be mapped with /<mapping>/*");
            String resurouce = pathInfo.substring(1);
            if (log.isDebugEnabled()) {
                log.debug("resource to expose: " + resurouce);
            }
            String extension = resurouce.substring(resurouce.lastIndexOf('.') + 1);
            MimeType mimeType = MimeTypeRegistry.getByExtension(extension);
            Validate.notNull(mimeType, "no mimetype found for extension: " + extension);
            if (log.isDebugEnabled()) {
                log.debug("the mime type to set: " + mimeType.getMimeType());
            }
            File f = new File(mappedFolder, resurouce);
            Validate.isTrue(f.exists(), "file: " + f + " does not exist");
            Validate.isTrue(f.canRead(), "can not read the file: " + f);
            if (log.isDebugEnabled()) {
                log.debug("exposing the file: " + f);
            }
            resp.setContentType(mimeType.getMimeType());
            FileInputStream fis = new FileInputStream(f);
            ServletOutputStream os = resp.getOutputStream();
            IOUtils.copy(fis, os);
            os.flush();
            IOUtils.closeQuietly(fis);
            IOUtils.closeQuietly(os);
        }
    }

    /**
     * This method may be overwritten by a subclasses do actions before serving the requested file. For example checking
     * security etc...
     * <p>
     * Dummy method which always returns true.
     * 
     * @param req
     * @param resp
     * @return proceed with handling the file.
     */
    protected boolean beforeServingFile(HttpServletRequest req, HttpServletResponse resp) {
        return true;
    }

    public void setMappedFolder(File mappedFolder) {
        this.mappedFolder = mappedFolder;
    }
}
