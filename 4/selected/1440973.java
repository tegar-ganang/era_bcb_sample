package oss.net.pstream.jca;

import java.io.InputStream;
import java.io.OutputStream;
import java.sql.CallableStatement;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpMethodBase;
import oss.net.pstream.PortletStream;
import oss.net.pstream.PortletStreamException;
import oss.net.pstream.PortletStreamParameters;

/**
 * @author mgregory
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class StreamerConnection implements java.sql.Connection {

    private PortletStreamParameters _params;

    private HttpClient _client;

    private StreamerManagedConnection _mcon;

    private PortletStream _ps;

    private boolean _closed = false;

    private static final boolean _debug = false;

    private String _protocol;

    public StreamerConnection(StreamerManagedConnection Mcon, PortletStreamParameters params, String Host, int Port, String Protocol) throws Exception {
        _params = params;
        _client = Mcon.getClient();
        _client.getHostConfiguration().setHost(Host, Port, Protocol);
        _protocol = Protocol;
        _mcon = Mcon;
        _ps = new PortletStream(params.debug());
    }

    public HttpClient getClient() {
        _closed = false;
        return _client;
    }

    public PortletStreamParameters getParams() {
        _closed = false;
        return _params;
    }

    /**
	 * @see java.sql.Connection#close()
	 */
    public void close() throws SQLException {
        _closed = true;
        _params.endSession();
        if (_debug) System.out.println(this.toString() + " closing");
        _mcon.sendClose(this);
    }

    /**
	 * @see java.sql.Connection#clearWarnings()
	 */
    public void clearWarnings() throws SQLException {
    }

    /**
	 * @see java.sql.Connection#commit()
	 */
    public void commit() throws SQLException {
    }

    /**
	 * @see java.sql.Connection#createStatement()
	 */
    public Statement createStatement() throws SQLException {
        return null;
    }

    /**
	 * @see java.sql.Connection#createStatement(int, int, int)
	 */
    public Statement createStatement(int arg0, int arg1, int arg2) throws SQLException {
        return null;
    }

    /**
	 * @see java.sql.Connection#createStatement(int, int)
	 */
    public Statement createStatement(int arg0, int arg1) throws SQLException {
        return null;
    }

    /**
	 * @see java.sql.Connection#getAutoCommit()
	 */
    public boolean getAutoCommit() throws SQLException {
        return false;
    }

    /**
	 * @see java.sql.Connection#getCatalog()
	 */
    public String getCatalog() throws SQLException {
        return null;
    }

    /**
	 * @see java.sql.Connection#getHoldability()
	 */
    public int getHoldability() throws SQLException {
        return 0;
    }

    /**
	 * @see java.sql.Connection#getMetaData()
	 */
    public DatabaseMetaData getMetaData() throws SQLException {
        return null;
    }

    /**
	 * @see java.sql.Connection#getTransactionIsolation()
	 */
    public int getTransactionIsolation() throws SQLException {
        return 0;
    }

    /**
	 * @see java.sql.Connection#getTypeMap()
	 */
    public Map getTypeMap() throws SQLException {
        return null;
    }

    /**
	 * @see java.sql.Connection#getWarnings()
	 */
    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    /**
	 * @see java.sql.Connection#isClosed()
	 */
    public boolean isClosed() throws SQLException {
        return _closed;
    }

    /**
	 * @see java.sql.Connection#isReadOnly()
	 */
    public boolean isReadOnly() throws SQLException {
        return false;
    }

    /**
	 * @see java.sql.Connection#nativeSQL(String)
	 */
    public String nativeSQL(String arg0) throws SQLException {
        return null;
    }

    /**
	 * @see java.sql.Connection#prepareCall(String, int, int, int)
	 */
    public CallableStatement prepareCall(String arg0, int arg1, int arg2, int arg3) throws SQLException {
        return null;
    }

    /**
	 * @see java.sql.Connection#prepareCall(String, int, int)
	 */
    public CallableStatement prepareCall(String arg0, int arg1, int arg2) throws SQLException {
        return null;
    }

    /**
	 * @see java.sql.Connection#prepareCall(String)
	 */
    public CallableStatement prepareCall(String arg0) throws SQLException {
        return null;
    }

    /**
	 * @see java.sql.Connection#prepareStatement(String, int, int, int)
	 */
    public PreparedStatement prepareStatement(String arg0, int arg1, int arg2, int arg3) throws SQLException {
        return null;
    }

    /**
	 * @see java.sql.Connection#prepareStatement(String, int, int)
	 */
    public PreparedStatement prepareStatement(String arg0, int arg1, int arg2) throws SQLException {
        return null;
    }

    /**
	 * @see java.sql.Connection#prepareStatement(String, int)
	 */
    public PreparedStatement prepareStatement(String arg0, int arg1) throws SQLException {
        return null;
    }

    /**
	 * @see java.sql.Connection#prepareStatement(String, int[])
	 */
    public PreparedStatement prepareStatement(String arg0, int[] arg1) throws SQLException {
        return null;
    }

    /**
	 * @see java.sql.Connection#prepareStatement(String, String[])
	 */
    public PreparedStatement prepareStatement(String arg0, String[] arg1) throws SQLException {
        return null;
    }

    /**
	 * @see java.sql.Connection#prepareStatement(String)
	 */
    public PreparedStatement prepareStatement(String arg0) throws SQLException {
        return null;
    }

    /**
	 * @see java.sql.Connection#releaseSavepoint(Savepoint)
	 */
    public void releaseSavepoint(Savepoint arg0) throws SQLException {
    }

    /**
	 * @see java.sql.Connection#rollback()
	 */
    public void rollback() throws SQLException {
    }

    /**
	 * @see java.sql.Connection#rollback(Savepoint)
	 */
    public void rollback(Savepoint arg0) throws SQLException {
    }

    /**
	 * @see java.sql.Connection#setAutoCommit(boolean)
	 */
    public void setAutoCommit(boolean arg0) throws SQLException {
    }

    /**
	 * @see java.sql.Connection#setCatalog(String)
	 */
    public void setCatalog(String arg0) throws SQLException {
    }

    /**
	 * @see java.sql.Connection#setHoldability(int)
	 */
    public void setHoldability(int arg0) throws SQLException {
    }

    /**
	 * @see java.sql.Connection#setReadOnly(boolean)
	 */
    public void setReadOnly(boolean arg0) throws SQLException {
    }

    /**
	 * @see java.sql.Connection#setSavepoint()
	 */
    public Savepoint setSavepoint() throws SQLException {
        return null;
    }

    /**
	 * @see java.sql.Connection#setSavepoint(String)
	 */
    public Savepoint setSavepoint(String arg0) throws SQLException {
        return null;
    }

    /**
	 * @see java.sql.Connection#setTransactionIsolation(int)
	 */
    public void setTransactionIsolation(int arg0) throws SQLException {
    }

    /**
	 * @see java.sql.Connection#setTypeMap(Map)
	 */
    public void setTypeMap(Map arg0) throws SQLException {
    }

    public void setManager(StreamerManagedConnection mcon) {
        _mcon = mcon;
    }

    public StreamerManagedConnection getManager() {
        return _mcon;
    }

    /**
	 * Returns the host from the HostConfiguration
	 **/
    public String getHost() {
        return _client.getHost();
    }

    /**
	 * Returns the port from the HostConfiguration
	 **/
    public int getPort() {
        return _client.getPort();
    }

    /**
	 * Returns the protocol from the HostConfiguration
	 **/
    public String getProtocol() {
        return _protocol;
    }

    public void execute(HttpServletRequest request, HttpServletResponse response, HttpMethodBase method) throws Exception {
        if (_debug) System.out.println(this.toString() + " : executing.");
        _ps.execute(_client, method, _params, request, response);
        if (_debug) System.out.println(this.toString() + " : returning data...");
        try {
            byte[] buffer = new byte[8000];
            OutputStream os = response.getOutputStream();
            InputStream is = _params.getResponseBodyInputStream();
            int bytesread = 0;
            while ((bytesread = is.read(buffer)) > 0) {
                os.write(buffer, 0, bytesread);
            }
        } catch (PortletStreamException err) {
        }
    }

    public String execute(HttpServletRequest request, HttpMethodBase method) throws Exception {
        _ps.execute(_client, method, _params, request);
        String result = null;
        return _params.getContentBuffer();
    }
}
