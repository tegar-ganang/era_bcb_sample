package org.nestframework.action.defaults;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.regex.Pattern;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nestframework.action.DownloadItem;
import org.nestframework.action.IActionHandler;
import org.nestframework.action.Redirect;
import org.nestframework.annotation.Intercept;
import org.nestframework.core.ExecuteContext;
import org.nestframework.core.Stage;
import org.nestframework.utils.NestUtil;

/**
 * Default forward handler.
 * <p>
 * If forward start with http:// or ftp:// etc. then is a redirection.
 * If forward start with "!" then is a redirection.
 * </p>
 * 
 * @author audin
 */
@Intercept({ Stage.HANDLE_VIEW })
public class DefaultActionForwarder implements IActionHandler {

    /**
	 * Logger for this class
	 */
    private static final Log log = LogFactory.getLog(DefaultActionForwarder.class);

    public boolean process(ExecuteContext context) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("process(ExecuteContext) - start");
        }
        Object rs = context.getForward();
        String forward = null;
        boolean redirect = false;
        boolean isLocal = true;
        long streamLen = 0;
        long startPos = 0;
        String filename = null;
        if (rs instanceof String) {
            forward = (String) context.getForward();
        } else if (rs instanceof Redirect) {
            Redirect r = (Redirect) context.getForward();
            forward = r.getTarget();
            redirect = true;
            isLocal = r.isLocal();
        } else if (rs instanceof DownloadItem) {
            DownloadItem dt = (DownloadItem) rs;
            if (dt.getFile() != null) {
                rs = dt.getFile();
            } else if (dt.getInputStream() != null) {
                rs = dt.getInputStream();
            } else if (dt.getData() != null) {
                rs = dt.getData();
            }
            streamLen = dt.getContentLength();
            if (dt.getContentType() != null) {
                context.getResponse().setContentType("application/octet-stream");
            }
            filename = dt.getFilename();
        }
        InputStream is = null;
        if (rs instanceof InputStream) {
            is = (InputStream) rs;
        } else if (rs instanceof File) {
            File file = (File) rs;
            streamLen = file.length();
            is = new FileInputStream(file);
            if (filename == null) {
                filename = NestUtil.getFilename(file.getName());
            }
        } else if (rs instanceof byte[]) {
            byte[] data = (byte[]) context.getForward();
            is = new ByteArrayInputStream(data);
            streamLen = data.length;
        }
        if (is != null) {
            String ct = context.getResponse().getContentType();
            if (ct == null) {
                context.getResponse().setContentType("application/octet-stream");
            }
            if (filename != null) {
                String ua = context.getRequest().getHeader("User-Agent");
                if (ua != null && ua.indexOf("MSIE") != -1) {
                    filename = URLEncoder.encode(filename, "UTF-8");
                } else {
                    filename = new String(filename.getBytes(), "ISO8859-1");
                }
                context.getResponse().setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            }
            ServletOutputStream os = context.getResponse().getOutputStream();
            if (streamLen != 0 && context.getRequest().getHeader("Range") != null) {
                context.getResponse().setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                startPos = Long.parseLong(context.getRequest().getHeader("Range").toLowerCase().replaceAll("bytes=", "").replaceAll("-", ""));
                if (startPos != 0) {
                    String contentRange = new StringBuffer("bytes ").append(new Long(startPos).toString()).append("-").append(new Long(streamLen - 1).toString()).append("/").append(new Long(streamLen).toString()).toString();
                    context.getResponse().setHeader("Content-Range", contentRange);
                    is.skip(startPos);
                }
            }
            if (streamLen != 0) {
                context.getResponse().setHeader("Accept-Ranges", "bytes");
                context.getResponse().setHeader("Content-Length", new Long(streamLen - startPos).toString());
            }
            byte[] buf = new byte[1024];
            int readLen = 0;
            try {
                while ((readLen = is.read(buf)) != -1) {
                    os.write(buf, 0, readLen);
                }
                os.flush();
                os.close();
            } catch (Exception e) {
            } finally {
                is.close();
            }
            return true;
        }
        if (forward == null) {
            if (log.isDebugEnabled()) {
                log.debug("process(ExecuteContext) - end");
            }
            return false;
        } else if (!redirect && Pattern.matches("^[a-z]+:\\/\\/.*", forward.toLowerCase())) {
            redirect = true;
            isLocal = false;
        } else if (forward.startsWith("!")) {
            redirect = true;
            isLocal = true;
            forward = forward.substring(1);
        }
        if (redirect) {
            if (isLocal && forward.startsWith("/")) {
                forward = context.getRequest().getContextPath() + forward;
            }
            context.getResponse().sendRedirect(forward);
        } else {
            context.getRequest().getRequestDispatcher(forward).forward(context.getRequest(), context.getResponse());
        }
        if (log.isDebugEnabled()) {
            log.debug("process(ExecuteContext) - end");
        }
        return false;
    }
}
