package fido.db;

import java.util.*;
import java.sql.*;
import fido.util.FidoDataSource;

/**
 * 
 */
public class InstructionTable {

    public static final int COLLECTION = 1;

    public static final int DECISION = 2;

    public static final int EXECUTE_INTERNAL = 3;

    /**
	 * 
	 */
    public InstructionTable() {
    }

    private int getCurrentId(Statement stmt) throws SQLException {
        ResultSet rs = null;
        try {
            int id;
            String sql = "select currval('instructions_instructionid_seq')";
            rs = stmt.executeQuery(sql);
            if (rs.next() == false) throw new SQLException("No rows returned from select currval() query"); else return rs.getInt(1);
        } finally {
            if (rs != null) rs.close();
        }
    }

    /**
	 * 
	 */
    public int addExecuteInstruction(String exeString) throws FidoDatabaseException {
        try {
            Connection conn = null;
            Statement stmt = null;
            try {
                String sql = "insert into Instructions (Type, ExecuteString, Operator) " + "values (3, '" + exeString + "', 0)";
                conn = FidoDataSource.getConnection();
                stmt = conn.createStatement();
                stmt.executeUpdate(sql);
                return getCurrentId(stmt);
            } finally {
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
    public void modifyExecuteInstruction(int id, String exeString) throws FidoDatabaseException {
        try {
            Connection conn = null;
            Statement stmt = null;
            try {
                String sql = "update Instructions set ExecuteString = '" + exeString + "' where InstructionId = " + id;
                conn = fido.util.FidoDataSource.getConnection();
                stmt = conn.createStatement();
                stmt.executeUpdate(sql);
            } finally {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException e) {
            throw new FidoDatabaseException(e);
        }
    }

    public int addDecisionInstruction(int condition, String frameSlot, String linkName, int objectId, String attribute, int positive, int negative) throws FidoDatabaseException, ObjectNotFoundException, InstructionNotFoundException {
        try {
            Connection conn = null;
            Statement stmt = null;
            try {
                if ((condition == ConditionalOperatorTable.CONTAINS_LINK) || (condition == ConditionalOperatorTable.NOT_CONTAINS_LINK)) {
                    ObjectTable ot = new ObjectTable();
                    if (ot.contains(objectId) == false) throw new ObjectNotFoundException(objectId);
                }
                conn = fido.util.FidoDataSource.getConnection();
                conn.setAutoCommit(false);
                stmt = conn.createStatement();
                if (contains(stmt, positive) == false) throw new InstructionNotFoundException(positive);
                if (contains(stmt, negative) == false) throw new InstructionNotFoundException(negative);
                String sql = "insert into Instructions (Type, Operator, FrameSlot, LinkName, ObjectId, AttributeName) " + "values (2, " + condition + ", '" + frameSlot + "', '" + linkName + "', " + objectId + ", '" + attribute + "')";
                stmt.executeUpdate(sql);
                int id = getCurrentId(stmt);
                InstructionGroupTable groupTable = new InstructionGroupTable();
                groupTable.deleteInstruction(stmt, id);
                if (positive != -1) groupTable.addInstructionAt(stmt, id, 1, positive);
                if (negative != -1) groupTable.addInstructionAt(stmt, id, 2, negative);
                conn.commit();
                return id;
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

    public void modifyDecisionInstruction(int id, int condition, String frameSlot, String linkName, int objectId, String attribute, int positive, int negative) throws FidoDatabaseException, ObjectNotFoundException, InstructionNotFoundException {
        try {
            Connection conn = null;
            Statement stmt = null;
            try {
                if ((condition == ConditionalOperatorTable.CONTAINS_LINK) || (condition == ConditionalOperatorTable.NOT_CONTAINS_LINK)) {
                    ObjectTable ot = new ObjectTable();
                    if (ot.contains(objectId) == false) throw new ObjectNotFoundException(objectId);
                }
                conn = fido.util.FidoDataSource.getConnection();
                conn.setAutoCommit(false);
                stmt = conn.createStatement();
                if (contains(stmt, positive) == false) throw new InstructionNotFoundException(positive);
                if (contains(stmt, negative) == false) throw new InstructionNotFoundException(negative);
                String sql = "update Instructions set Operator = " + condition + ", " + "                        FrameSlot = '" + frameSlot + "', " + "                        LinkName = '" + linkName + "', " + "                        ObjectId = " + objectId + ", " + "                        AttributeName = '" + attribute + "' " + "where InstructionId = " + id;
                stmt.executeUpdate(sql);
                InstructionGroupTable groupTable = new InstructionGroupTable();
                groupTable.deleteInstruction(stmt, id);
                if (positive != -1) groupTable.addInstructionAt(stmt, id, 1, positive);
                if (negative != -1) groupTable.addInstructionAt(stmt, id, 2, negative);
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

    public int addCollectionInstruction() throws FidoDatabaseException {
        try {
            Connection conn = null;
            Statement stmt = null;
            try {
                String sql = "insert into Instructions (Type, Operator) " + "values (1, 0)";
                conn = fido.util.FidoDataSource.getConnection();
                stmt = conn.createStatement();
                stmt.executeUpdate(sql);
                return getCurrentId(stmt);
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
                String sql = "delete from Instructions where InstructionId = " + id;
                stmt.executeUpdate(sql);
                sql = "delete from InstructionGroups where InstructionId = " + id;
                stmt.executeUpdate(sql);
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

    private boolean contains(Statement stmt, int id) throws SQLException {
        ResultSet rs = null;
        try {
            String sql = "select count(1) from Instructions where InstructionId = " + id;
            rs = stmt.executeQuery(sql);
            if (rs.next() == false) throw new SQLException("No results returned from count(1) query");
            int count = rs.getInt(1);
            if (count == 0) return false;
            return true;
        } finally {
            if (rs != null) rs.close();
        }
    }

    public boolean contains(int id) throws FidoDatabaseException {
        try {
            Connection conn = null;
            Statement stmt = null;
            try {
                conn = fido.util.FidoDataSource.getConnection();
                stmt = conn.createStatement();
                return contains(stmt, id);
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
	 * Returns the GrammarLink type referrenced by the parameter <i>id</i>.
	 * @param type The type of link referrenced by <i>id</i>
	 * @exception GrammarLinkNotFoundException thrown when the <i>id</i> does
	 *            not exist in the list
	 * @return type of link
	 */
    public Instruction get(int id) throws FidoDatabaseException, InstructionNotFoundException {
        try {
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;
            try {
                String sql = "select Type, ExecuteString, FrameSlot, Operator, LinkName, " + "       ObjectId, AttributeName " + "from Instructions " + "where InstructionId = " + id;
                conn = fido.util.FidoDataSource.getConnection();
                stmt = conn.createStatement();
                rs = stmt.executeQuery(sql);
                if (rs.next() == false) throw new InstructionNotFoundException(id); else {
                    Instruction instruction = new Instruction(id, rs.getInt(1), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getString(5), rs.getInt(6), rs.getString(7));
                    return instruction;
                }
            } finally {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException e) {
            throw new FidoDatabaseException(e);
        }
    }

    /**
	 * Returns the names of all GrammarLink names in the list.  The names
	 * of the links are in alphabetical order.
	 *
	 * @return list of link names
	 */
    public Collection list() throws FidoDatabaseException {
        try {
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;
            try {
                String sql = "select a.InstructionId, a.Type, b.Description, a.ExecuteString, a.Operator, " + "       c.Description, a.FrameSlot, a.LinkName, a.ObjectId, a.AttributeName " + "from Instructions a, InstructionTypes b, ConditionalOperators c " + "where a.Type = b.Type and a.Operator = c.Operator " + "order by InstructionId";
                conn = fido.util.FidoDataSource.getConnection();
                stmt = conn.createStatement();
                rs = stmt.executeQuery(sql);
                Vector list = new Vector();
                while (rs.next() == true) {
                    int id = rs.getInt(1);
                    int type = rs.getInt(2);
                    String typeDescription = rs.getString(3);
                    String executeString = rs.getString(4);
                    if (rs.wasNull() == true) executeString = "";
                    int operator = rs.getInt(5);
                    String operatorDescription;
                    if (operator == 0) operatorDescription = ""; else operatorDescription = rs.getString(6);
                    String frameSlot = rs.getString(7);
                    if (rs.wasNull() == true) frameSlot = "";
                    String linkName = rs.getString(8);
                    if (rs.wasNull() == true) linkName = "";
                    String attributeName = rs.getString(9);
                    if (rs.wasNull() == true) attributeName = "";
                    int objectId = rs.getInt(10);
                    Instruction instruction = new Instruction(id, type, typeDescription, executeString, frameSlot, operator, operatorDescription, linkName, objectId, attributeName);
                    list.add(instruction);
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

    /**
	 * Returns the names of all Attribute names in the list.  The names
	 * of the links are in alphabetical order.
	 * @return list of link names
	 */
    public Collection listTypes() throws FidoDatabaseException {
        try {
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;
            try {
                String sql = "select Type, Description from InstructionTypes order by Type";
                conn = fido.util.FidoDataSource.getConnection();
                stmt = conn.createStatement();
                rs = stmt.executeQuery(sql);
                Vector list = new Vector();
                while (rs.next() == true) {
                    InstructionType type = new InstructionType(rs.getInt(1), rs.getString(2));
                    list.add(type);
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

    public int hashCode(String id) throws FidoDatabaseException, InstructionNotFoundException {
        return hashCode(Integer.parseInt(id));
    }

    public int hashCode(int id) throws FidoDatabaseException, InstructionNotFoundException {
        Instruction instruction = get(id);
        Vector list = new Vector();
        list.add(new Integer(instruction.getType()));
        list.add(instruction.getExecuteString());
        list.add(instruction.getFrameSlot());
        list.add(new Integer(instruction.getOperator()));
        list.add(instruction.getLinkName());
        list.add(instruction.getAttributeName());
        list.add(new Integer(instruction.getObjectId()));
        return list.hashCode();
    }

    public boolean isEmpty() throws FidoDatabaseException {
        try {
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;
            try {
                String sql = "select count(1) from Instructions";
                conn = fido.util.FidoDataSource.getConnection();
                stmt = conn.createStatement();
                rs = stmt.executeQuery(sql);
                if (rs.next() == false) throw new SQLException("No rows returned for count(1) query"); else {
                    int num = rs.getInt(1);
                    if (num == 0) return true; else return false;
                }
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
