package fido.db;

import java.util.*;
import java.sql.*;
import fido.util.FidoDataSource;

public class ProperNounTable {

    public ProperNounTable() {
    }

    private int findMaxRank(Statement stmt, String name) throws SQLException {
        ResultSet rs = null;
        try {
            String sql = "select max(SenseNumber) from ProperNouns where Noun = '" + name + "'";
            rs = stmt.executeQuery(sql);
            if (rs.next() == false) throw new SQLException("No rows returned for select max() query"); else {
                int num = rs.getInt(1);
                return num;
            }
        } finally {
            if (rs != null) rs.close();
        }
    }

    /**
	 * 
	 */
    public int add(String name, int objectId) throws FidoDatabaseException, ObjectNotFoundException {
        try {
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;
            try {
                ObjectTable ot = new ObjectTable();
                if (ot.contains(objectId) == false) throw new ObjectNotFoundException(objectId);
                conn = FidoDataSource.getConnection();
                stmt = conn.createStatement();
                int row = findMaxRank(stmt, name);
                String sql = "insert into ProperNouns (Noun, SenseNumber, ObjectId) " + "values ('" + name + "', " + (row + 1) + ", " + objectId + ")";
                stmt.executeUpdate(sql);
                return row;
            } finally {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException e) {
            throw new FidoDatabaseException(e);
        }
    }

    public void modify(String name, int row, int objectId) throws FidoDatabaseException, ObjectNotFoundException, ProperNounNotFoundException {
        try {
            Connection conn = null;
            Statement stmt = null;
            try {
                ObjectTable ot = new ObjectTable();
                if (ot.contains(objectId) == false) throw new ObjectNotFoundException(objectId);
                conn = fido.util.FidoDataSource.getConnection();
                stmt = conn.createStatement();
                int max = findMaxRank(stmt, name);
                if ((row < 1) || (row > max)) throw new IllegalArgumentException("Row number not between 1 and " + max);
                String sql = "update ProperNouns set ObjectId = " + objectId + " " + "where Noun = '" + name + "' and SenseNumber = " + row;
                int rows = stmt.executeUpdate(sql);
                if (rows == 0) throw new ProperNounNotFoundException(name);
            } finally {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException e) {
            throw new FidoDatabaseException(e);
        }
    }

    public Collection get(String name) throws FidoDatabaseException {
        try {
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;
            try {
                String sql = "select SenseNumber, ObjectId from ProperNouns " + "where Noun = '" + name + "'";
                conn = fido.util.FidoDataSource.getConnection();
                stmt = conn.createStatement();
                rs = stmt.executeQuery(sql);
                Vector objs = new Vector();
                while (rs.next() == true) {
                    ProperNoun noun = new ProperNoun(name, rs.getInt(1), rs.getInt(2));
                    objs.add(noun);
                }
                return objs;
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
	 */
    public void delete(String name, int row) throws FidoDatabaseException {
        try {
            Connection conn = null;
            Statement stmt = null;
            try {
                String sql = "delete from ProperNouns where Noun = '" + name + "' and SenseNumber = " + row;
                conn = fido.util.FidoDataSource.getConnection();
                conn.setAutoCommit(false);
                stmt = conn.createStatement();
                int max = findMaxRank(stmt, name);
                stmt.executeUpdate(sql);
                for (int i = row; i < max; ++i) {
                    stmt.executeUpdate("update ProperNouns set SenseNumber = " + i + " where SenseNumber = " + (i + 1) + " and Noun = '" + name + "'");
                }
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

    public Collection list() throws FidoDatabaseException {
        try {
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;
            try {
                String sql = "select Noun, SenseNumber, ObjectId from ProperNouns order by Noun";
                conn = fido.util.FidoDataSource.getConnection();
                stmt = conn.createStatement();
                rs = stmt.executeQuery(sql);
                Vector list = new Vector();
                while (rs.next() == true) {
                    ProperNoun noun = new ProperNoun(rs.getString(1), rs.getInt(2), rs.getInt(3));
                    list.add(noun);
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

    public int hashCode(String name, String row) throws FidoDatabaseException, ProperNounNotFoundException {
        try {
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;
            try {
                String sql = "select ObjectId from ProperNouns " + "where Noun = '" + name + "' and SenseNumber = " + row;
                conn = fido.util.FidoDataSource.getConnection();
                stmt = conn.createStatement();
                rs = stmt.executeQuery(sql);
                if (rs.next() == false) throw new ProperNounNotFoundException(name);
                Vector list = new Vector();
                list.add(name);
                list.add(row);
                list.add(rs.getString(1));
                return list.hashCode();
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
