package com.gwtaf.fileio.server;

import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.gwtaf.core.shared.model.ValidateException;
import com.gwtaf.core.shared.util.InvalidSessionException;
import com.gwtaf.core.shared.util.StringUtil;

public abstract class FileDownloadServlet extends HttpServlet {

    private static final long serialVersionUID = -2638224318753451420L;

    private final Log log = LogFactory.getLog(FileDownloadServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (this.log.isDebugEnabled()) this.log.debug("processing download request");
        try {
            IFile item = getData(req);
            if (item != null) {
                if (this.log.isDebugEnabled()) this.log.debug("sending file '" + item.getName() + "' to browser");
                if (StringUtil.isValid(item.getContentType())) resp.setContentType(item.getContentType());
                resp.setHeader("Content-Disposition", makeContentDisposition(item.getName()));
                if (isDisableCache()) {
                    resp.setHeader("Cache-Control", "no-store");
                    resp.setHeader("Pragma", "no-cache");
                    resp.setDateHeader("Expires", 0);
                }
                ServletOutputStream out = resp.getOutputStream();
                if (item instanceof IFileStream) {
                    IFileStream stream = (IFileStream) item;
                    stream.write(out);
                } else {
                    InputStream is = item.getInputStream();
                    if (is != null) {
                        int read;
                        byte[] buff = new byte[1024];
                        while ((read = is.read(buff)) != -1) {
                            out.write(buff, 0, read);
                        }
                    } else {
                        byte[] data = item.getData();
                        if (data != null) out.write(data); else {
                            resp.sendError(HttpServletResponse.SC_NO_CONTENT, "Data not available");
                            return;
                        }
                    }
                }
                out.close();
                if (this.log.isDebugEnabled()) this.log.debug("sent file done.");
            } else resp.sendError(HttpServletResponse.SC_NO_CONTENT, "Data not available");
        } catch (InvalidSessionException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
            this.log.error("download failed", e);
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            this.log.error("download failed", e);
        }
    }

    private String makeContentDisposition(String file) {
        StringBuilder buff = new StringBuilder();
        if (isAttachment()) buff.append("attachment"); else buff.append("inline");
        buff.append(";");
        if (StringUtil.isValid(file)) buff.append("filename=\"").append(file).append("\"");
        return buff.toString();
    }

    protected boolean isAttachment() {
        return true;
    }

    protected boolean isDisableCache() {
        return false;
    }

    protected abstract IFile getData(HttpServletRequest req) throws ValidateException, InvalidSessionException;
}
