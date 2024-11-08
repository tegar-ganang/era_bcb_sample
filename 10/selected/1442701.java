package simplejda.sqlbeans;

import java.util.*;
import java.sql.*;
import javax.sql.*;
import org.apache.log4j.*;
import simplejda.util.*;

public class TableBasedIntegerGenerator implements SQLBeanFieldGenerator {

    public static final String CLASSNAME = TableBasedIntegerGenerator.class.getName();

    private Category logger = LogController.getLogger(CLASSNAME);

    private DataSource ds = null;

    private String sequenceName = "";

    private static final String INTERAL_SEQUENCE = "sequence_internal_ref";

    private static final String SELECT_SQL = "SELECT sequence_id, value FROM sequence WHERE sequence_name=?";

    private static final String UPDATE_SQL = "UPDATE sequence SET value=? where sequence_id=?";

    private static final String INSERT_SQL = "INSERT INTO sequence (sequence_id, sequence_name, sequence_value) VALUES (?,?,?)";

    private static final String BOOTSTRAP_SQL = "INSERT INTO sequence (sequence_id, sequence_name, sequence_value) VALUES (-1,'" + INTERAL_SEQUENCE + "', 1)";

    public TableBasedIntegerGenerator(DataSource ds, String sequenceName) {
        this.ds = ds;
        this.sequenceName = sequenceName;
    }

    public Object getFieldValue() {
        try {
            return getInt(sequenceName);
        } catch (NoSuchSequenceException e) {
            if (bootStrap(sequenceName)) {
                try {
                    return getInt(sequenceName);
                } catch (NoSuchSequenceException e2) {
                    logger.error("Could not get value after successful bootstrap.  Returning 0.");
                    return new Integer(0);
                }
            } else {
                logger.error("Could not bootstrap.  Returning 0.");
                return new Integer(0);
            }
        }
    }

    private boolean bootStrap(String sequenceName) {
        return false;
    }

    private Integer getInt(String sequence) throws NoSuchSequenceException {
        Connection conn = null;
        PreparedStatement read = null;
        PreparedStatement write = null;
        boolean success = false;
        try {
            conn = ds.getConnection();
            conn.setTransactionIsolation(conn.TRANSACTION_REPEATABLE_READ);
            conn.setAutoCommit(false);
            read = conn.prepareStatement(SELECT_SQL);
            read.setString(1, sequence);
            ResultSet readRs = read.executeQuery();
            if (!readRs.next()) {
                throw new NoSuchSequenceException();
            }
            int currentSequenceId = readRs.getInt(1);
            int currentSequenceValue = readRs.getInt(2);
            Integer currentSequenceValueInteger = new Integer(currentSequenceValue);
            write = conn.prepareStatement(UPDATE_SQL);
            write.setInt(1, currentSequenceValue + 1);
            write.setInt(2, currentSequenceId);
            int rowsAffected = write.executeUpdate();
            if (rowsAffected == 1) {
                success = true;
                return currentSequenceValueInteger;
            } else {
                logger.error("Something strange has happened.  The row count was not 1, but was " + rowsAffected);
                return currentSequenceValueInteger;
            }
        } catch (SQLException sqle) {
            logger.error("Table based id generation failed : ");
            logger.error(sqle.getMessage());
            return new Integer(0);
        } finally {
            if (read != null) {
                try {
                    read.close();
                } catch (Exception e) {
                }
            }
            if (write != null) {
                try {
                    write.close();
                } catch (Exception e) {
                }
            }
            if (conn != null) {
                try {
                    if (success) {
                        conn.commit();
                    } else {
                        conn.rollback();
                    }
                    conn.close();
                } catch (Exception e) {
                }
            }
        }
    }

    private static class NoSuchSequenceException extends Exception {
    }
}
