package vqwiki.actions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.logging.Level;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import vqwiki.Environment;

/**
 * Delivers an attachment to the browser. Sends as a binary stream along with a content-type
 * determined by the mime.types file in the classpath.
 *
 * @author garethc
 * @since 25/10/2002 14:18:32
 */
public class ViewAttachmentAction extends AbstractWikiAction {

    private static final long serialVersionUID = 6355877196467304959L;

    /**
     * {@inheritDoc}
     */
    public void doAction(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String attachmentName = request.getParameter("attachment");
        String virtualWiki = getVirtualWiki(request);
        File uploadPath = getEnvironment().uploadPath(virtualWiki, attachmentName);
        response.reset();
        response.setHeader("Content-Disposition", getEnvironment().getStringSetting(Environment.PROPERTY_ATTACHMENT_TYPE) + ";filename=" + attachmentName + ";");
        int dotIndex = attachmentName.indexOf('.');
        if (dotIndex >= 0 && dotIndex < attachmentName.length() - 1) {
            String extension = attachmentName.substring(attachmentName.lastIndexOf('.') + 1);
            logger.fine("Extension: " + extension);
            String mimetype = (String) getMimeByExtension().get(extension.toLowerCase());
            logger.fine("MIME: " + mimetype);
            if (mimetype != null) {
                logger.fine("Setting content type to: " + mimetype);
                response.setContentType(mimetype);
            }
        }
        FileInputStream in = null;
        ServletOutputStream out = null;
        try {
            in = new FileInputStream(uploadPath);
            out = response.getOutputStream();
            IOUtils.copy(in, out);
            out.flush();
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
    }

    /**
     *
     */
    protected HashMap getMimeByExtension() throws Exception {
        HashMap map = new HashMap();
        InputStream resourceAsStream = getClass().getResourceAsStream("/mime.types");
        if (resourceAsStream == null) {
            logger.log(Level.WARNING, "couldn't find the MIME types file mime.types");
            return map;
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(resourceAsStream));
        while (true) {
            String line = in.readLine();
            if (line == null) {
                break;
            }
            if (!line.startsWith("#") && !line.trim().equals("")) {
                StringTokenizer tokens = new StringTokenizer(line);
                if (tokens.hasMoreTokens()) {
                    String type = tokens.nextToken();
                    while (tokens.hasMoreTokens()) {
                        map.put(tokens.nextToken(), type);
                    }
                }
            }
        }
        return map;
    }
}
