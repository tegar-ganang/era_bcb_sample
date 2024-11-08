package com.rooster.action.admin.onboard;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import com.rooster.client.Util.ClientUtil;
import com.rooster.constants.PropertyFileConst;

public class DownloadDocument extends Action {

    Logger log = Logger.getLogger(DownloadDocument.class.getName());

    public ActionForward execute(ActionMapping map, ActionForm frm, HttpServletRequest req, HttpServletResponse res) {
        HttpSession session = req.getSession(false);
        if ((session == null) || (session.getAttribute("UserId") == null) || (session.getAttribute("UserId").equals(new String("")))) {
            req.setAttribute("APP_ERROR", "Your Session Got Expired. Please Re-login.");
            try {
                res.sendRedirect("/loginfail.do");
            } catch (IOException e) {
            }
            return null;
        }
        String sDocId = String.valueOf(req.getParameter("doc_id"));
        String sAppPath = String.valueOf(session.getAttribute(PropertyFileConst.APPLICATION_ROOT_PATH));
        try {
            String sResumePath = getFilePath(req, sDocId);
            sResumePath = sAppPath + sResumePath;
            log.info("sResumePath : " + sResumePath);
            String filePath = sResumePath.substring(0, sResumePath.lastIndexOf('/'));
            log.info("filePath : " + filePath);
            String fileName = sResumePath.substring(sResumePath.lastIndexOf('/') + 1);
            log.info("fileName: " + fileName);
            if (req.getServletPath().endsWith(fileName)) {
                log.debug("File Not Found.");
                res.sendRedirect("/FileNotFoundError.do");
                return null;
            }
            fileName = URLDecoder.decode(fileName, "UTF-8");
            File file = new File(filePath, fileName);
            if (!file.exists()) {
                log.debug("File Not Found.");
                res.sendRedirect("/FileNotFoundError.do");
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
                log.debug(e.getMessage());
                e.printStackTrace();
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                        log.debug(e.getMessage());
                        e.printStackTrace();
                    }
                }
                if (output != null) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        log.debug(e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getFilePath(HttpServletRequest request, String sDocId) {
        String sFilePath = new String();
        DataSource dbSrc = null;
        Connection con = null;
        Statement stmnt = null;
        ResultSet rs = null;
        try {
            String sSql = "select doc_path from rooster_onboard_docs where id=" + sDocId + ";";
            log.info("Qry: " + sSql);
            dbSrc = getDataSource(request);
            con = dbSrc.getConnection();
            stmnt = con.createStatement();
            rs = stmnt.executeQuery(sSql);
            while (rs.next()) {
                sFilePath = rs.getString(1);
            }
        } catch (SQLException sqle) {
            log.debug(sqle);
        } finally {
            try {
                if (stmnt != null) {
                    stmnt.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException sqle) {
                log.debug(sqle);
            }
        }
        return sFilePath;
    }
}
