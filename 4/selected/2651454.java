package org.interlogy.servlets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA.
 * User: naryzhny
 * Date: 25.04.2007
 * Time: 12:28:02
 * To change this template use File | Settings | File Templates.
 */
public class DBFilesServlet extends HttpServlet {

    private static final Log log = LogFactory.getLog(DBFilesServlet.class);

    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        doPost(httpServletRequest, httpServletResponse);
    }

    protected void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        String pathInfo = httpServletRequest.getPathInfo();
        log.info("PathInfo: " + pathInfo);
        if (pathInfo == null || pathInfo.equals("") || pathInfo.equals("/")) {
            httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String fileName = pathInfo.charAt(0) == '/' ? pathInfo.substring(1) : pathInfo;
        log.info("FileName: " + fileName);
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = getDataSource().getConnection();
            ps = con.prepareStatement("select file, size from files where name=?");
            ps.setString(1, fileName);
            rs = ps.executeQuery();
            if (rs.next()) {
                httpServletResponse.setContentType(getServletContext().getMimeType(fileName));
                httpServletResponse.setContentLength(rs.getInt("size"));
                OutputStream os = httpServletResponse.getOutputStream();
                org.apache.commons.io.IOUtils.copy(rs.getBinaryStream("file"), os);
                os.flush();
            } else {
                httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        } finally {
            if (rs != null) try {
                rs.close();
            } catch (SQLException e) {
            }
            if (ps != null) try {
                ps.close();
            } catch (SQLException e) {
            }
            if (con != null) try {
                con.close();
            } catch (SQLException e) {
            }
        }
    }

    private DataSource getDataSource() throws ServletException {
        try {
            String dsJndi = getServletConfig().getInitParameter("ds-jndi");
            DataSource dataSource = (DataSource) new InitialContext().lookup(dsJndi);
            return dataSource;
        } catch (NamingException e) {
            throw new ServletException(e);
        }
    }
}
