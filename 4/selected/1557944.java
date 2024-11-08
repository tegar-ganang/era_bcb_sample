package fi.arcusys.qnet.common.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import fi.arcusys.commons.hibernate.DefaultSessionFactory;
import fi.arcusys.commons.hibernate.dao.StaticDAO;
import fi.arcusys.qnet.common.dao.ResourceFileStorage;
import fi.arcusys.qnet.common.dao.ResourceFileStorageFactory;
import fi.arcusys.qnet.common.model.ResourceFile;
import fi.arcusys.qnet.common.model.ResourceFileWithData;

/**
 * Servlet implementation class for Servlet: ResourceFileDownloadServlet
 *
 */
public class ResourceFileDownloadServlet extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(ResourceFileDownloadServlet.class);

    public ResourceFileDownloadServlet() {
        super();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String ids = request.getParameter("id");
        if (log.isDebugEnabled()) {
            log.debug("doGet; id=" + ids);
        }
        if (null == ids || 0 == ids.length()) {
            log.error("Required request parameter 'id' is missing");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Required request parameter 'id' is missing");
        } else {
            Long id = Long.valueOf(ids);
            Session hs = DefaultSessionFactory.getInstance().getSessionFactory().openSession();
            Transaction tx = hs.beginTransaction();
            try {
                ResourceFile rf = StaticDAO.get(ResourceFile.class, id, hs);
                Hibernate.initialize(rf);
                if (null == rf) {
                    log.error("No such ResourceFile found: " + id);
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                } else {
                    sendResourceFile(request, response, rf);
                }
            } finally {
                if (null != tx) {
                    tx.commit();
                    tx = null;
                }
                hs.close();
            }
        }
    }

    private InputStream getResourceFileInputStream(ResourceFile rf) throws IOException {
        InputStream in;
        if (rf instanceof ResourceFileWithData) {
            ResourceFileWithData rfwd = (ResourceFileWithData) rf;
            log.debug("Data is in dataBlob");
            if (null == rfwd.getDataBlob()) {
                throw new IOException("dataBlob is null");
            }
            try {
                in = rfwd.getDataBlob().getBinaryStream();
            } catch (SQLException ex) {
                log.error("Catched SQLException while opening dataBlob", ex);
                throw new IOException(ex.getMessage());
            }
        } else {
            log.debug("External Data; using ResourceFileStorage");
            ResourceFileStorage rfs = ResourceFileStorageFactory.getInstance().openResourceFileStorage();
            in = rfs.getDataAsStream(rf);
        }
        return in;
    }

    private void sendResourceFile(HttpServletRequest req, HttpServletResponse resp, ResourceFile rf) throws ServletException, IOException {
        if (log.isDebugEnabled()) {
            log.debug("Opening data of ResourceFile: " + rf);
        }
        InputStream in = new BufferedInputStream(getResourceFileInputStream(rf));
        try {
            if (log.isDebugEnabled()) {
                log.debug("Sending data of ResourceFile: " + rf);
            }
            resp.setContentType(rf.getContentType());
            resp.setHeader("Content-Disposition", "attachment; filename=" + rf.getName());
            resp.setContentLength(null != rf.getSize() ? rf.getSize().intValue() : 0);
            OutputStream out = new BufferedOutputStream(resp.getOutputStream());
            final byte[] READ_BUF = new byte[65536];
            int read = 0;
            do {
                read = in.read(READ_BUF);
                if (read > 0) {
                    out.write(READ_BUF, 0, read);
                }
            } while (read > 0);
            out.flush();
            out.close();
        } finally {
            try {
                in.close();
            } catch (Exception ex) {
            }
        }
    }
}
