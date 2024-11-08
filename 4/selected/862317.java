package pt.gotham.gardenia.dbforms.servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbforms.config.DbFormsConfig;
import org.dbforms.config.DbFormsConfigRegistry;
import org.dbforms.config.Field;
import org.dbforms.config.FieldTypes;
import org.dbforms.config.Table;
import org.dbforms.util.SqlUtil;
import org.dbforms.util.StringUtil;
import org.dbforms.util.Util;
import org.dbforms.util.FileHolder;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * #fixme - add appropriate exception-handling..
 * 
 * @author joe peer
 */
public class FileServlet extends HttpServlet {

    private static Log logCat = LogFactory.getLog(FileServlet.class.getName());

    /**
	 * Process the HTTP Get request
	 * 
	 * @param request
	 *            Description of the Parameter
	 * @param response
	 *            Description of the Parameter
	 * 
	 * @exception ServletException
	 *                Description of the Exception
	 * @exception IOException
	 *                Description of the Exception
	 */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        DbFormsConfig config = null;
        try {
            config = DbFormsConfigRegistry.instance().lookup();
        } catch (Exception e) {
            logCat.error(e);
            throw new ServletException(e);
        }
        try {
            String tf = request.getParameter("tf");
            String keyValuesStr = request.getParameter("keyval");
            if (!Util.isNull(keyValuesStr) && !("null".equals(keyValuesStr))) {
                int tableId = Integer.parseInt(StringUtil.getEmbeddedString(tf, 0, '_'));
                Table table = config.getTable(tableId);
                int fieldId = Integer.parseInt(StringUtil.getEmbeddedString(tf, 1, '_'));
                Field field = table.getField(fieldId);
                StringBuffer queryBuf = new StringBuffer();
                String dbConnectionName = request.getParameter("invname_" + tableId);
                Connection con = config.getConnection(dbConnectionName);
                String nameField = request.getParameter("nf");
                InputStream is = null;
                String fileName = null;
                queryBuf.append("SELECT ");
                queryBuf.append(field.getName());
                if (nameField != null) {
                    queryBuf.append(", ");
                    queryBuf.append(nameField);
                }
                queryBuf.append(" FROM ");
                queryBuf.append(table.getName());
                queryBuf.append(" WHERE ");
                queryBuf.append(table.getWhereClauseForKeyFields());
                logCat.info("::doGet - query is [" + queryBuf + "]");
                PreparedStatement ps = con.prepareStatement(queryBuf.toString());
                table.populateWhereClauseWithKeyFields(keyValuesStr, ps, 1);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    if (field.getType() == FieldTypes.DISKBLOB) {
                        fileName = rs.getString(1);
                        String directory = field.getDirectory();
                        try {
                            directory = Util.replaceRealPath(directory, DbFormsConfigRegistry.instance().lookup().getRealPath());
                        } catch (Exception ex) {
                            logCat.error("::doGet - error replacing REALPATH on diskblob directory" + ex);
                        }
                        is = SqlUtil.readDiskBlob(fileName, directory, request.getParameter("defaultValue"));
                    } else if (field.getType() == FieldTypes.BLOB) {
                        if (nameField != null) {
                            fileName = rs.getString(2);
                            is = SqlUtil.readDbFieldBlob(rs);
                        } else {
                            FileHolder fh = SqlUtil.readFileHolderBlob(rs);
                            is = fh.getInputStreamFromBuffer();
                            fileName = fh.getFileName();
                        }
                    }
                } else {
                    logCat.info("::doGet - we have got no result using query " + queryBuf);
                }
                if (is != null) {
                    writeToClient(request, response, fileName, is);
                }
                SqlUtil.closeConnection(con);
            }
        } catch (SQLException sqle) {
            logCat.error("::doGet - SQL exception", sqle);
        }
    }

    /**
	 * Process the HTTP Post request
	 * 
	 * @param request
	 *            Description of the Parameter
	 * @param response
	 *            Description of the Parameter
	 * 
	 * @exception ServletException
	 *                Description of the Exception
	 * @exception IOException
	 *                Description of the Exception
	 */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    /**
	 * Write the content of the input file to the client.
	 * 
	 * @param request
	 *            DOCUMENT ME!
	 * @param response
	 *            Description of the Parameter
	 * @param fileName
	 *            Description of the Parameter
	 * @param is
	 *            Description of the Parameter
	 * 
	 * @exception IOException
	 *                Description of the Exception
	 */
    private void writeToClient(HttpServletRequest request, HttpServletResponse response, String fileName, InputStream is) throws IOException {
        String contentType = request.getSession().getServletContext().getMimeType(fileName);
        logCat.info("::writeToClient- writing to client:" + fileName + " ct=" + contentType);
        if (!Util.isNull(contentType)) {
            response.setContentType(contentType);
        }
        response.setHeader("Cache-control", "private");
        response.setHeader("Content-Disposition", "attachment; fileName=\"" + fileName + "\"");
        ServletOutputStream out = response.getOutputStream();
        byte[] b = new byte[1024];
        int read;
        while ((read = is.read(b)) != -1) out.write(b, 0, read);
        out.close();
    }
}
