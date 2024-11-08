package org.gomba;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

/**
 * Write a LOB to a JDBC data store. the SQL in the <code>query</code>
 * init-param should be a SELECT that selects a LOB field. Currently LOBs are
 * overwritten but not cleared, so previous data in the LOB that exceeds the
 * length of the new data will not be overwritten. This servlet inherits the
 * init-params of {@link org.gomba.AbstractServlet}, plus:
 * <dl>
 * <dt>http-method</dt>
 * <dd>The value can be POST or PUT. (Required)</dd>
 * <dt>column</dt>
 * <dd>The result set column containing the BLOB or CLOB value. This init-param
 * is required only if the result set contains more than one column. (Optional)
 * </dd>
 * <dt>update-query</dt>
 * <dd>An SQL statement that writes the BLOB or CLOB to the db. The statement
 * must contain one parameter of BLOB or CLOB type using the
 * <code>${blob.myColumn}</code> syntax. This is required only if the JDBC
 * driver does not support LOB in-place modification. (Optional)</dd>
 * </dl>
 * 
 * Note about HTTP method usage. The POST method is normally used for creation
 * (INSERT in SQL) operations. The PUT method is normally used for update
 * (UPDATE in SQL) operations.
 * 
 * @author Flavio Tordini
 * @version $Id: LOBUpdateServlet.java,v 1.8 2005/07/21 09:12:22 flaviotordini Exp $
 */
public class LOBUpdateServlet extends SingleQueryServlet {

    /**
     * The result set column name to render. May be null.
     */
    private String columnName;

    /**
     * <code>true</code> if this servlet supports the POST HTTP method.
     */
    private boolean supportPost;

    /**
     * <code>true</code> if this servlet supports the PUT HTTP method.
     */
    private boolean supportPut;

    /**
     * The parsed update query definition, if any.
     */
    private QueryDefinition updateQueryDefinition;

    /**
     * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String updateQuery = config.getInitParameter("update-query");
        try {
            if (updateQuery == null && checkLocatorsUpdateCopy(getDataSource())) {
                throw new ServletException("The database does not allow in-place LOB modification. " + "Updates made to a LOB are made on a copy and not directly to the LOB. " + "The 'update-query' init-param must be specified.");
            }
        } catch (SQLException sqle) {
            throw new ServletException("Error checking if locators update copies.", sqle);
        }
        if (updateQuery != null) {
            try {
                this.updateQueryDefinition = new QueryDefinition(updateQuery);
            } catch (Exception e) {
                throw new ServletException("Error parsing update query definition.", e);
            }
        }
        this.columnName = config.getInitParameter("column");
        String httpMethod = config.getInitParameter("http-method");
        if (httpMethod == null) {
            throw new ServletException("Missing init-param: http-method");
        }
        if (httpMethod.equals("POST")) {
            this.supportPost = true;
        } else if (httpMethod.equals("PUT")) {
            this.supportPut = true;
        } else {
            throw new ServletException("Unsupported HTTP method: " + httpMethod);
        }
    }

    /**
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
     *           javax.servlet.http.HttpServletResponse)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (this.supportPost) {
            processRequest(request, response, false);
        } else {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }

    /**
     * @see javax.servlet.http.HttpServlet#doPut(javax.servlet.http.HttpServletRequest,
     *           javax.servlet.http.HttpServletResponse)
     */
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (this.supportPut) {
            processRequest(request, response, false);
        } else {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }

    /**
     * Get a reference to the LOB object and write the content of the request
     * body to it.
     * 
     * @see org.gomba.AbstractServlet#doInput(java.sql.ResultSet,
     *           javax.servlet.http.HttpServletRequest, org.gomba.ParameterResolver)
     */
    protected void doInput(ResultSet resultSet, HttpServletRequest request, ParameterResolver parameterResolver, Connection connection) throws Exception {
        if (resultSet == null) {
            throw new Exception("Resultset is null.");
        }
        ResultSetMetaData rsmd = resultSet.getMetaData();
        if (rsmd.getColumnCount() != 1 && this.columnName == null) {
            throw new Exception("The resultset contains more than one column. " + "You must set the 'column' init-param.");
        }
        final int columnIndex;
        if (this.columnName != null) {
            columnIndex = DatumServlet.getColumnIndex(rsmd, this.columnName);
        } else {
            columnIndex = 1;
        }
        final int columnType = rsmd.getColumnType(columnIndex);
        switch(columnType) {
            case Types.BLOB:
            case Types.LONGVARBINARY:
                if (this.updateQueryDefinition == null) {
                    Blob blob = resultSet.getBlob(columnIndex);
                    if (blob == null) {
                        throw new Exception("BLOB value is null.");
                    }
                    writeBytes(request.getInputStream(), blob.setBinaryStream(1));
                } else {
                    Query updateQuery = new Query(this.updateQueryDefinition, parameterResolver);
                    Blob blob = getBlob(updateQuery.getStatementParameters());
                    if (blob == null) {
                        throw new Exception("Unable to get reference to a BLOB. " + "Maybe the BLOB field is null on the db " + "or the wrong column name has been specified.");
                    }
                    writeBytes(request.getInputStream(), blob.setBinaryStream(1));
                    updateQuery.execute(connection);
                }
                break;
            case Types.CLOB:
            case Types.LONGVARCHAR:
                if (this.updateQueryDefinition == null) {
                    Clob clob = resultSet.getClob(columnIndex);
                    if (clob == null) {
                        throw new Exception("CLOB value is null.");
                    }
                    writeCharacters(request.getReader(), clob.setCharacterStream(1));
                } else {
                    Query updateQuery = new Query(this.updateQueryDefinition, parameterResolver);
                    Clob clob = getClob(updateQuery.getStatementParameters());
                    if (clob == null) {
                        throw new Exception("Unable to get reference to a CLOB. " + "Maybe the CLOB field is null on the db " + "or the wrong column name has been specified.");
                    }
                    writeCharacters(request.getReader(), clob.setCharacterStream(1));
                    updateQuery.execute(connection);
                }
                break;
            default:
                throw new ServletException("Invalid SQL data type: " + columnType);
        }
    }

    /**
     * Write characters from a Reader to a Writer.
     */
    private static void writeCharacters(Reader reader, Writer writer) throws IOException {
        try {
            char[] buffer = new char[4096];
            int length;
            while ((length = reader.read(buffer)) >= 0) {
                writer.write(buffer, 0, length);
            }
        } finally {
            writer.close();
            writer.close();
        }
    }

    /**
     * Write bytes from an InputStream to an OutputStream.
     */
    private static void writeBytes(InputStream is, OutputStream os) throws IOException {
        try {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = is.read(buffer)) >= 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            is.close();
            os.close();
        }
    }

    /**
     * Check if the JDBC driver supports in-place modification of LOB locators.
     */
    private static boolean checkLocatorsUpdateCopy(DataSource dataSource) throws SQLException {
        Connection connection = dataSource.getConnection();
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            return metaData.locatorsUpdateCopy();
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    /**
     * Iterate thorugh the statement parameters and find a Blob. TODO make sure
     * there is only one Blob
     */
    private static Blob getBlob(List parameters) throws Exception {
        for (Iterator i = parameters.iterator(); i.hasNext(); ) {
            Object parameter = i.next();
            if (parameter instanceof Blob) {
                return (Blob) parameter;
            }
        }
        return null;
    }

    /**
     * Iterate thorugh the statement parameters and find a Clob. TODO make sure
     * there is only one Clob
     */
    private static Clob getClob(List parameters) throws Exception {
        for (Iterator i = parameters.iterator(); i.hasNext(); ) {
            Object parameter = i.next();
            if (parameter instanceof Clob) {
                return (Clob) parameter;
            }
        }
        return null;
    }
}
