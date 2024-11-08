package net.sf.poormans.server;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.poormans.tool.Utils;
import net.sf.poormans.tool.compression.Zip;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Proxy servlet which refers all requests to a zip file.<br>
 * Init parameters:
 * <ul>
 * <li><b>file</b> (required): Path to the zip file</li>
 * <li><b>zipPathToSkip</b> (optional): Path to skip inside the zip file.<br>
 * Should used only if the zip file has one path which contains all the rest!</li>
 * </ul>
 * 
 * @version $Id: ZipProxyServlet.java 2119 2011-06-27 12:24:16Z th-schwarz $
 * @author <a href="mailto:th-schwarz@users.sourceforge.net">Thilo Schwarz</a>
 */
public class ZipProxyServlet extends AServlet {

    private static final Logger logger = Logger.getLogger(ZipProxyServlet.class);

    private static final long serialVersionUID = 1L;

    private String zipPathToSkip = null;

    private Map<String, ZipEntry> zipInfo;

    private ZipFile zipFile;

    @Override
    public void init(ServletConfig config) throws ServletException {
        zipPathToSkip = config.getInitParameter("zipPathToSkip");
        String fileParam = config.getInitParameter("file");
        if (StringUtils.isBlank(fileParam)) throw new IllegalArgumentException("No file parameter found!");
        File file = null;
        zipInfo = new HashMap<String, ZipEntry>();
        try {
            file = new File(config.getInitParameter("file"));
            zipFile = new ZipFile(file);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry ze = entries.nextElement();
                String entryName = ze.getName();
                if (zipPathToSkip != null) entryName = entryName.substring(zipPathToSkip.length() + 1);
                if (entryName.startsWith("/")) entryName = entryName.substring(1);
                zipInfo.put(entryName, ze);
            }
            logger.debug("Found entries in zip: " + zipInfo.size());
        } catch (IOException e) {
            throw new ServletException("Couldn't read the zip file: " + e.getMessage(), e);
        }
        logger.info(String.format("ZipProxyServlet initialzed with file [%s] and path to skip [%s]", file.getPath(), zipPathToSkip));
    }

    @Override
    protected void doRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String reqPath = req.getPathInfo();
        if (reqPath.startsWith("/")) reqPath = reqPath.substring(1);
        ZipEntry entry = zipInfo.get(reqPath);
        if (entry == null) {
            logger.debug(Utils.join("Requested path not found: [", reqPath, "]"));
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        logger.debug(Utils.join("Requested path: [", reqPath, "]"));
        ServletUtils.establishContentType(reqPath, resp);
        InputStream in = null;
        try {
            in = new BufferedInputStream(zipFile.getInputStream(entry));
            IOUtils.copy(in, resp.getOutputStream());
            logger.debug("Rendered: " + reqPath);
        } catch (FileNotFoundException e) {
            logger.error("zipped resource not found: " + reqPath);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        Zip.closeQuietly(zipFile);
    }
}
