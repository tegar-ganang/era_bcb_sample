package fido.db;

import java.util.*;
import java.sql.*;
import fido.util.FidoDataSource;

public class InstructionGroupTable {

    public InstructionGroupTable() {
    }

    public void addInstructionAt(Statement stmt, int id, int row, int groupId) throws SQLException {
        String sql = "insert into InstructionGroups (InstructionId, Rank, GroupInstruction) " + "values (" + id + ", " + row + ", " + groupId + ");";
        stmt.executeUpdate(sql);
    }

    public void deleteInstruction(Statement stmt, int id) throws SQLException {
        String sql = "delete from InstructionGroups where InstructionId = " + id;
        stmt.executeUpdate(sql);
    }

    private int getNextRank(Statement stmt, int id) throws SQLException {
        ResultSet rs = null;
        try {
            String sql = "select max(rank) from InstructionGroups " + "where InstructionId = " + id;
            rs = stmt.executeQuery(sql);
            if (rs.next() == false) throw new SQLException("No rows returned from max() query"); else {
                int max = rs.getInt(1);
                return (max + 1);
            }
        } finally {
            if (rs != null) rs.close();
        }
    }

    public void addGroupInstruction(int id, int groupId) throws FidoDatabaseException, InstructionNotFoundException {
        try {
            Connection conn = null;
            Statement stmt = null;
            try {
                InstructionTable it = new InstructionTable();
                if (it.contains(groupId) == false) throw new InstructionNotFoundException(groupId);
                conn = FidoDataSource.getConnection();
                stmt = conn.createStatement();
                int max = getNextRank(stmt, id);
                String sql = "insert into InstructionGroups (InstructionId, Rank, GroupInstruction) " + "values (" + id + ", " + max + ", " + groupId + ")";
                stmt.executeUpdate(sql);
            } finally {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException e) {
            throw new FidoDatabaseException(e);
        }
    }

    private void bumpAllRowsUp(Statement stmt, int id, int row) throws SQLException {
        ResultSet rs = null;
        try {
            String sql = "select max(Rank) from InstructionGroups " + "where InstructionId = " + id + " and Rank >= " + row;
            rs = stmt.executeQuery(sql);
            if (rs.next() == true) {
                int num = rs.getInt(1);
                for (int i = row; i < num; ++i) {
                    stmt.executeUpdate("update InstructionGroups set Rank = " + i + " where InstructionId = " + id + "   and Rank = " + (i + 1));
                }
            }
        } finally {
            if (rs != null) rs.close();
        }
    }

    public void deleteGroupInstruction(int id, int rank) throws FidoDatabaseException, InstructionNotFoundException {
        try {
            Connection conn = null;
            Statement stmt = null;
            try {
                conn = fido.util.FidoDataSource.getConnection();
                conn.setAutoCommit(false);
                stmt = conn.createStatement();
                String sql = "delete from InstructionGroups " + "where InstructionId = " + id + " and Rank = " + rank;
                stmt.executeUpdate(sql);
                bumpAllRowsUp(stmt, id, rank);
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

    private int findMaxRank(Statement stmt, int id) throws SQLException {
        ResultSet rs = null;
        try {
            String sql = "select max(Rank) from InstructionGroups where InstructionId = '" + id + "'";
            rs = stmt.executeQuery(sql);
            if (rs.next() == false) throw new SQLException("No rows returned for select max() query"); else {
                int num = rs.getInt(1);
                return num;
            }
        } finally {
            if (rs != null) rs.close();
        }
    }

    public void moveRowUp(int id, int row) throws FidoDatabaseException {
        try {
            Connection conn = null;
            Statement stmt = null;
            try {
                conn = fido.util.FidoDataSource.getConnection();
                conn.setAutoCommit(false);
                stmt = conn.createStatement();
                int max = findMaxRank(stmt, id);
                if ((row < 2) || (row > max)) throw new IllegalArgumentException("Row number not between 2 and " + max);
                stmt.executeUpdate("update InstructionGroups set Rank = -1 where InstructionId = '" + id + "' and Rank = " + row);
                stmt.executeUpdate("update InstructionGroups set Rank = " + row + " where InstructionId = '" + id + "' and Rank = " + (row - 1));
                stmt.executeUpdate("update InstructionGroups set Rank = " + (row - 1) + " where InstructionId = '" + id + "' and Rank = -1");
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

    /**
	 * Moves the row specified by <i>row</i> down in priority.  The String <i>row</i>
	 * must be a number.
	 * @exception SQLException Input / Output error saving WordClassifications object
	 *            to database
	 */
    public void moveRowDown(int id, int row) throws FidoDatabaseException {
        try {
            Connection conn = null;
            Statement stmt = null;
            try {
                conn = fido.util.FidoDataSource.getConnection();
                conn.setAutoCommit(false);
                stmt = conn.createStatement();
                int max = findMaxRank(stmt, id);
                if ((row < 1) || (row > (max - 1))) throw new IllegalArgumentException("Row number not between 1 and " + (max - 1));
                stmt.executeUpdate("update InstructionGroups set Rank = -1 where InstructionId = '" + id + "' and Rank = " + row);
                stmt.executeUpdate("update InstructionGroups set Rank = " + row + " where InstructionId = '" + id + "' and Rank = " + (row + 1));
                stmt.executeUpdate("update InstructionGroups set Rank = " + (row + 1) + " where InstructionId = '" + id + "' and Rank = -1");
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

    public Collection listGroupInstructions(int id) throws FidoDatabaseException {
        try {
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;
            try {
                String sql = "select Rank, GroupInstruction from InstructionGroups " + "where InstructionId = " + id + " order by Rank";
                conn = fido.util.FidoDataSource.getConnection();
                stmt = conn.createStatement();
                rs = stmt.executeQuery(sql);
                Vector list = new Vector();
                while (rs.next() == true) {
                    CollectionInstruction instr = new CollectionInstruction(rs.getInt(1), rs.getInt(2));
                    list.add(instr);
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
