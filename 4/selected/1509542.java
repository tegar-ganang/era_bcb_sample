package com.rooster.action.admin.billing;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import javax.sql.rowset.CachedRowSet;
import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import sun.misc.FpUtils;
import com.rooster.process.rss.AdminDBConnect;

public class DownloadPDF extends Action {

    Logger logger = Logger.getLogger(DownloadPDF.class.getName());

    private String sAdminPropFile;

    private String sAppPath;

    public ActionForward execute(ActionMapping map, ActionForm frm, HttpServletRequest req, HttpServletResponse res) {
        logger.info("com.rooster.action.admin.billing.DownloadPDF -- Entry");
        HttpSession session = req.getSession(false);
        if ((session == null) || (session.getAttribute("UserId") == null) || (session.getAttribute("UserId").equals(new String("")))) {
            req.setAttribute("APP_ERROR", "Your Session Got Expired. Please Re-login.");
            try {
                res.sendRedirect("/loginfail.do");
            } catch (IOException e) {
            }
            return null;
        }
        String sInvoiceId = String.valueOf(req.getParameter("InvoiceId"));
        sAdminPropFile = String.valueOf(session.getAttribute("ADMIN_PROPERTIES_FILE"));
        Properties prop = new Properties();
        try {
            File file = new File(sAdminPropFile);
            InputStream is = new FileInputStream(file);
            prop.load(is);
            sAppPath = prop.getProperty("APPLICATION_ROOT_PATH");
        } catch (Exception e) {
            logger.warn(e);
        }
        try {
            String sPDFPath = getFilePath(sInvoiceId);
            String filePath = sPDFPath.substring(0, sPDFPath.lastIndexOf('/'));
            filePath = sAppPath + filePath;
            String fileName = sPDFPath.substring(sPDFPath.lastIndexOf('/') + 1);
            if (req.getServletPath().endsWith(fileName)) {
                logger.debug("File Not Found.");
                res.sendRedirect("PDFNotFoundError.do");
                return null;
            }
            fileName = URLDecoder.decode(fileName, "UTF-8");
            File file = new File(filePath, fileName);
            logger.info("FilePath TTTTTTTTTTTTTTTTTTTTTTTTTTTTT" + filePath);
            logger.info("FileName EEEEEEEEEEEEEEEEEEEEEEEEEEEEE" + fileName);
            if (!file.exists()) {
                logger.debug("File Not Found.");
                res.sendRedirect("PDFNotFoundError.do");
                return null;
            }
            String contentType = URLConnection.guessContentTypeFromName(fileName);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            BufferedInputStream input = null;
            BufferedOutputStream output = null;
            try {
                input = new BufferedInputStream(new FileInputStream(file));
                int contentLength = input.available();
                res.reset();
                res.setContentLength(contentLength);
                res.setContentType(contentType);
                res.setHeader("Content-disposition", "attachment; filename=\"" + fileName + "\"");
                output = new BufferedOutputStream(res.getOutputStream());
                while (contentLength-- > 0) {
                    output.write(input.read());
                }
                output.flush();
            } catch (IOException e) {
                logger.debug(e.getMessage());
                e.printStackTrace();
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                        logger.debug(e.getMessage());
                        e.printStackTrace();
                    }
                }
                if (output != null) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        logger.debug(e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.info("com.rooster.action.admin.billing.DownloadPDF -- Entry");
        return null;
    }

    private String getFilePath(String sInvoiceId) {
        String sFilePath = new String();
        try {
            CachedRowSet RowSet = getRows("select invoice_path from rooster_client_invoice where id ='" + sInvoiceId + "'; ");
            while (RowSet.next()) {
                sFilePath = RowSet.getString(1);
            }
        } catch (SQLException sqle) {
            logger.debug(sqle);
        }
        return sFilePath;
    }

    public CachedRowSet getRows(String sSql) {
        logger.info("com.rooster.action.admin.billing.getRows -- Entry");
        AdminDBConnect AdmindbConnect = new AdminDBConnect(sAdminPropFile);
        CachedRowSet crs = AdmindbConnect.getRows(sSql);
        return crs;
    }
}
