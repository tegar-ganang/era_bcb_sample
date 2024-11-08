package annone.ui;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Set;
import org.sqlite.JDBC;
import annone.util.AnnoneException;
import annone.util.Checks;
import annone.util.Safe;
import annone.util.Text;

public class DatabaseGridData implements GridData {

    private Connection connection;

    private PreparedStatement rowCountStmt;

    private PreparedStatement insertRowStmt;

    private PreparedStatement incrementOrdinalsStmt;

    private PreparedStatement deleteRowStmt;

    private PreparedStatement decrementOrdinalsStmt;

    public DatabaseGridData() {
        try {
            connection = new JDBC().connect("jdbc:sqlite:" + File.createTempFile("annone", ".griddata").getAbsolutePath(), new Properties());
            connection.setAutoCommit(false);
            Statement stmt = this.connection.createStatement();
            stmt.addBatch("CREATE TABLE DATA (ORDINAL INTEGER NOT NULL)");
            stmt.addBatch("CREATE UNIQUE INDEX IX_DATA_ORDINAL ON DATA (ORDINAL)");
            stmt.executeBatch();
            rowCountStmt = this.connection.prepareStatement("SELECT COUNT(*) FROM DATA");
            insertRowStmt = this.connection.prepareStatement("INSERT INTO DATA (ORDINAL) VALUES (?)");
            incrementOrdinalsStmt = this.connection.prepareStatement("UPDATE DATA SET ORDINAL = ORDINAL + 1 WHERE ORDINAL >= ?");
            deleteRowStmt = this.connection.prepareStatement("DELETE FROM DATA WHERE ORDINAL = ?");
            decrementOrdinalsStmt = this.connection.prepareStatement("UPDATE DATA SET ORDINAL = ORDINAL - 1 WHERE ORDINAL >= ?");
        } catch (Exception xp) {
            throw new AnnoneException(Text.get("Can''t create database file."), xp);
        }
    }

    @Override
    public int getRowCount() {
        synchronized (this) {
            ResultSet rs = null;
            try {
                rs = rowCountStmt.executeQuery();
                int rowCount = 0;
                if (rs.next()) rowCount = rs.getInt(1);
                return rowCount;
            } catch (Throwable xp) {
                throw new AnnoneException(Text.get("Can''t get row count."), xp);
            } finally {
                Safe.close(rs);
            }
        }
    }

    @Override
    public int addRow(int row) {
        synchronized (this) {
            Checks.inRange("row", row, -1, getRowCount());
            try {
                if (row < 0) row = getRowCount();
                incrementOrdinalsStmt.setInt(1, row);
                incrementOrdinalsStmt.executeUpdate();
                insertRowStmt.setInt(1, row);
                insertRowStmt.executeUpdate();
                connection.commit();
                return row;
            } catch (Throwable xp) {
                try {
                    connection.rollback();
                } catch (Throwable xp1) {
                }
                throw new AnnoneException(Text.get("Can''t add row."), xp);
            }
        }
    }

    @Override
    public void removeRow(int row) {
        synchronized (this) {
            Checks.inRange("row", row, 0, getRowCount() - 1);
            try {
                deleteRowStmt.setInt(1, row);
                deleteRowStmt.executeUpdate();
                decrementOrdinalsStmt.setInt(1, row + 1);
                decrementOrdinalsStmt.executeUpdate();
                connection.commit();
            } catch (Throwable xp) {
                try {
                    connection.rollback();
                } catch (Throwable xp1) {
                }
                throw new AnnoneException(Text.get("Can''t add row."), xp);
            }
        }
    }

    @Override
    public Object getValue(String column, int row) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setValue(String column, int row, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void copyFrom(Set<String> columns, GridData data) {
    }

    @Override
    public void destroy() {
        Safe.close(connection);
        connection = null;
    }
}
