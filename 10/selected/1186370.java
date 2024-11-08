package fido.db;

import java.util.*;
import java.sql.*;
import fido.util.FidoDataSource;

/**
 * 
 */
public class WebServiceTable {

    /**
	 * 
	 */
    public WebServiceTable() {
    }

    /**
	 * 
	 * 
	 *
	 *
	 */
    public int add(WebService ws) throws FidoDatabaseException {
        try {
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;
            try {
                String sql = "insert into WebServices (MethodName, ServiceURI) " + "values ('" + ws.getMethodName() + "', '" + ws.getServiceURI() + "')";
                conn = fido.util.FidoDataSource.getConnection();
                conn.setAutoCommit(false);
                stmt = conn.createStatement();
                stmt.executeUpdate(sql);
                int id;
                sql = "select currval('webservices_webserviceid_seq')";
                rs = stmt.executeQuery(sql);
                if (rs.next() == false) throw new SQLException("No rows returned from select currval() query"); else id = rs.getInt(1);
                PreparedStatement pstmt = conn.prepareStatement("insert into WebServiceParams " + "(WebServiceId, Position, ParameterName, Type) " + "values (?, ?, ?, ?)");
                pstmt.setInt(1, id);
                pstmt.setInt(2, 0);
                pstmt.setString(3, null);
                pstmt.setInt(4, ws.getReturnType());
                pstmt.executeUpdate();
                for (Iterator it = ws.parametersIterator(); it.hasNext(); ) {
                    WebServiceParameter param = (WebServiceParameter) it.next();
                    pstmt.setInt(2, param.getPosition());
                    pstmt.setString(3, param.getName());
                    pstmt.setInt(4, param.getType());
                    pstmt.executeUpdate();
                }
                conn.commit();
                return id;
            } catch (SQLException e) {
                if (conn != null) conn.rollback();
                throw e;
            } finally {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new FidoDatabaseException(e);
        }
    }

    /**
	 * 
	 * 
	 *
	 *
	 */
    public WebService get(int id) throws FidoDatabaseException, WebServiceNotFoundException {
        try {
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;
            try {
                String sql = "select a.MethodName, a.ServiceURI, b.Position, b.ParameterName, " + "       b.Type, c.Description " + "from WebServices a, WebServiceParams b, WebServiceParamTypes c " + "where a.WebServiceId = " + id + " AND " + "      a.WebServiceId = b.WebServiceId AND " + "      b.Type = c.Type " + "order by b.Position";
                conn = fido.util.FidoDataSource.getConnection();
                stmt = conn.createStatement();
                rs = stmt.executeQuery(sql);
                boolean first = true;
                WebService ws = null;
                while (rs.next() == true) {
                    if (first == true) {
                        ws = new WebService(id, rs.getString(1), rs.getString(2));
                        first = false;
                    }
                    int position = rs.getInt(3);
                    if (position == 0) {
                        ws.setReturn(rs.getInt(5), rs.getString(6));
                    } else {
                        WebServiceParameter param = new WebServiceParameter(position, rs.getString(4), rs.getInt(5), rs.getString(6));
                        ws.addParameter(param);
                    }
                }
                if (first == true) throw new WebServiceNotFoundException(id); else return ws;
            } finally {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException e) {
            throw new FidoDatabaseException(e);
        }
    }

    /**
	 * 
	 * 
	 *
	 *
	 */
    public void delete(int id) throws FidoDatabaseException {
        try {
            Connection conn = null;
            Statement stmt = null;
            try {
                conn = fido.util.FidoDataSource.getConnection();
                conn.setAutoCommit(false);
                stmt = conn.createStatement();
                stmt.executeUpdate("delete from WebServices where WebServiceId = " + id);
                stmt.executeUpdate("delete from WebServiceParams where WebServiceId = " + id);
                conn.commit();
            } catch (SQLException e) {
                if (conn != null) conn.rollback();
                throw e;
            } finally {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException e) {
            throw new FidoDatabaseException(e);
        }
    }

    public Vector list() throws FidoDatabaseException {
        try {
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;
            try {
                String sql = "select a.WebServiceId, a.MethodName, a.ServiceURI, b.Position, " + "       b.ParameterName, b.Type, c.Description " + "from WebServices a, WebServiceParams b, WebServiceParamTypes c " + "where a.WebServiceId = b.WebServiceId AND " + "      b.Type = c.Type " + "order by a.WebServiceId, b.Position";
                conn = fido.util.FidoDataSource.getConnection();
                stmt = conn.createStatement();
                rs = stmt.executeQuery(sql);
                Vector list = new Vector();
                WebService ws = null;
                while (rs.next() == true) {
                    int id = rs.getInt(1);
                    if ((ws == null) || (ws.getWebServiceId() != id)) {
                        ws = new WebService(id, rs.getString(2), rs.getString(3));
                        list.add(ws);
                    }
                    int position = rs.getInt(4);
                    if (position == 0) {
                        ws.setReturn(rs.getInt(5), rs.getString(6));
                    } else {
                        WebServiceParameter param = new WebServiceParameter(position, rs.getString(5), rs.getInt(6), rs.getString(7));
                        ws.addParameter(param);
                    }
                }
                return list;
            } finally {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException e) {
            throw new FidoDatabaseException(e);
        }
    }
}
