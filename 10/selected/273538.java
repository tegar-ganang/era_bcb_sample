package ro.codemart.installer.core.utils.sql;

import ro.codemart.installer.core.InstallerException;
import ro.codemart.installer.core.operation.AbstractLongOperation;
import ro.codemart.commons.sql.ConnectionInfo;
import ro.codemart.commons.sql.SQLCommand;
import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Action that executes sequentially a sql file and commits after all the statements were executed
 *
 * @author marius.ani
 */
public class SqlBatchOperation extends AbstractLongOperation {

    public static final String BATCH_DELIMITER = "go";

    private Connection connection;

    private ConnectionInfo connectionInfo;

    private Statement sqlStatement;

    private List<String> statements = new ArrayList<String>();

    public SqlBatchOperation(ConnectionInfo connectionInfo, File batchFile) throws IOException {
        this.connectionInfo = connectionInfo;
        loadStatements(batchFile);
    }

    public SqlBatchOperation(ConnectionInfo connectionInfo, InputStream is) throws IOException {
        this.connectionInfo = connectionInfo;
        loadStatements(is);
    }

    /**
     * Reads all the SQL statements from an input stream
     *
     * @param is the input stream that contains the SQL statements
     * @throws IOException if I/O errors occur
     */
    private void loadStatements(InputStream is) throws IOException {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(is));
            String line = null;
            StringBuffer statement = new StringBuffer();
            while ((line = br.readLine()) != null) {
                if (line.trim().equalsIgnoreCase(BATCH_DELIMITER)) {
                    statements.add(statement.toString());
                    statement = new StringBuffer();
                } else if (line.trim().length() != 0) {
                    statement.append(line);
                }
            }
        } finally {
            is.close();
            if (br != null) {
                br.close();
            }
        }
    }

    /**
     * Reads the SQL statements from a file
     *
     * @param batchFile the file containing the SQL statements
     * @throws IOException if I/O errors occur
     */
    private void loadStatements(File batchFile) throws IOException {
        loadStatements(new FileInputStream(batchFile));
    }

    public void execute() throws InstallerException {
        try {
            SQLCommand sqlCommand = new SQLCommand(connectionInfo);
            connection = sqlCommand.getConnection();
            connection.setAutoCommit(false);
            sqlStatement = connection.createStatement();
            double size = (double) statements.size();
            for (String statement : statements) {
                sqlStatement.executeUpdate(statement);
                setCompletedPercentage(getCompletedPercentage() + (1 / size));
            }
            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                throw new InstallerException(InstallerException.TRANSACTION_ROLLBACK_ERROR, new Object[] { e.getMessage() }, e);
            }
            throw new InstallerException(InstallerException.SQL_EXEC_EXCEPTION, new Object[] { e.getMessage() }, e);
        } catch (ClassNotFoundException e) {
            throw new InstallerException(InstallerException.DB_DRIVER_LOAD_ERROR, e);
        } finally {
            if (connection != null) {
                try {
                    sqlStatement.close();
                    connection.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    /**
     * Return the SQL statements
     *
     * @return the SQL statements
     */
    public List<String> getStatements() {
        return statements;
    }
}
