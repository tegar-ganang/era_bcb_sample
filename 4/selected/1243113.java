package uk.co.whisperingwind.vienna;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.swing.JOptionPane;
import uk.co.whisperingwind.framework.Dialogs;
import uk.co.whisperingwind.framework.ExceptionDialog;
import uk.co.whisperingwind.framework.Model;
import uk.co.whisperingwind.framework.SwingThread;
import uk.co.whisperingwind.framework.VectorTableModel;

/**
** Model for the input and output of a query. Contains an
** AbstractTableModel, which can be used as the data model for a
** JTable. This class also executes the query. If I could work out
** how, this would also be able to tell the query view where in a
** query an error occurred.
*/
class QueryModel extends Model {

    private Connection connection;

    private String sql = "";

    private String fileName = "";

    private boolean dirty = false;

    private VectorTableModel tableModel = new VectorTableModel();

    private ConfigModel configModel = null;

    private ResultSetMetaData metaData = null;

    private ExecuteWorker executeWorker = null;

    public QueryModel(ConfigModel config) {
        super();
        configModel = config;
    }

    public void setDirty(boolean state) {
        dirty = state;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setSQL(String newSql, Object initiator) {
        sql = newSql;
        setDirty(true);
        fireEvent(initiator, "sql", sql);
    }

    public String getSQL() {
        return sql;
    }

    public void setFileName(String newName) {
        File file = new File(newName);
        String path = file.getAbsolutePath();
        fileName = path;
        fireEvent(this, "name", fileName);
    }

    public String getFileName() {
        return fileName;
    }

    public boolean openFile(String newName) {
        boolean opened = false;
        byte buffer[] = new byte[1024];
        File file = new File(newName);
        try {
            BufferedReader in = new BufferedReader(new FileReader(newName));
            String newSql = "";
            String line = in.readLine();
            while (line != null) {
                if (newSql.length() > 0) newSql += "\n";
                newSql += line;
                line = in.readLine();
            }
            in.close();
            opened = true;
            setSQL(newSql, this);
            setFileName(newName);
            setDirty(false);
        } catch (java.io.FileNotFoundException ex) {
            new ExceptionDialog(ex);
        } catch (java.io.IOException ex) {
            new ExceptionDialog(ex);
        }
        return opened;
    }

    public boolean saveFile() {
        return writeFile(fileName);
    }

    public boolean saveFileAs(String newName) {
        boolean result = true;
        File theFile = new File(newName);
        if (theFile.exists()) {
            result = false;
            int reply = JOptionPane.showConfirmDialog(null, theFile.getName() + " already exists. Overwrite it?", "File exists", JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION) result = true;
        }
        if (result) result = writeFile(newName);
        return result;
    }

    /**
    ** Returns the table model created by executing the query.
    */
    public VectorTableModel getTableModel() {
        return tableModel;
    }

    public void execute(Connection c) {
        if (executeWorker != null) executeWorker.interrupt();
        connection = c;
        executeWorker = new ExecuteWorker();
        executeWorker.start();
    }

    public void cancelExecute() {
        if (executeWorker != null) executeWorker.interrupt();
    }

    public boolean isExecuting() {
        boolean executing = false;
        if (executeWorker != null) executing = !executeWorker.isInterrupted();
        return executing;
    }

    public boolean canExport() {
        return !isExecuting() && metaData != null && tableModel.getRowCount() > 0;
    }

    public int exportResult(String fileName, String separator, String quoteString, boolean withQuotes, boolean withTitles) {
        int result = 0;
        File theFile = new File(fileName);
        if (theFile.exists()) {
            int reply = JOptionPane.showConfirmDialog(null, fileName + " already exists. Overwrite it?", "File exists", JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION) {
                if (doExport(fileName, separator, quoteString, withQuotes, withTitles)) {
                    result = tableModel.getRowCount();
                }
            }
        } else {
            if (doExport(fileName, separator, quoteString, withQuotes, withTitles)) {
                result = tableModel.getRowCount();
            }
        }
        return result;
    }

    private boolean doExport(String fileName, String separator, String quoteString, boolean withQuotes, boolean withTitles) {
        boolean exported = true;
        try {
            int columnCount = tableModel.getColumnCount();
            boolean[] quotes = new boolean[columnCount];
            if (withQuotes) {
                for (int i = 0; i < columnCount; i++) {
                    switch(metaData.getColumnType(i + 1)) {
                        case Types.CHAR:
                        case Types.LONGVARBINARY:
                        case Types.LONGVARCHAR:
                        case Types.OTHER:
                        case Types.REF:
                        case Types.STRUCT:
                        case Types.VARBINARY:
                        case Types.VARCHAR:
                            quotes[i] = true;
                            break;
                        default:
                            break;
                    }
                }
            }
            FileOutputStream os = new FileOutputStream(fileName);
            PrintStream out = new PrintStream(os);
            if (withTitles) {
                String line = "";
                for (int column = 0; column < columnCount; column++) {
                    if (column > 0) line += separator;
                    line += quoteString;
                    line += tableModel.getColumnName(column);
                    line += quoteString;
                }
                out.println(line);
            }
            int rowCount = tableModel.getRowCount();
            for (int row = 0; row < rowCount; row++) {
                String line = "";
                for (int column = 0; column < columnCount; column++) {
                    if (column > 0) line += separator;
                    if (quotes[column]) line += quoteString;
                    line += tableModel.getValueAt(row, column);
                    if (quotes[column]) line += quoteString;
                }
                out.println(line);
                Thread.yield();
            }
            out.close();
        } catch (SQLException ex) {
            exported = false;
            Dialogs.showError("SQL Error", ex.getMessage());
        } catch (FileNotFoundException ex) {
            exported = false;
            Dialogs.showError("Cannot open output file", ex.getMessage());
        }
        return exported;
    }

    private String cleanSQL() {
        String stripped = "";
        StringTokenizer tokenizer = new StringTokenizer(sql, "\n");
        while (tokenizer.hasMoreTokens()) {
            String line = tokenizer.nextToken();
            String temp = line;
            temp.trim();
            if (temp.indexOf("--") == 0) stripped = stripped + "\n"; else stripped = stripped + line + "\n";
        }
        return stripped;
    }

    private boolean writeFile(String path) {
        boolean saved = false;
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(path)));
            out.write(sql, 0, sql.length());
            out.close();
            saved = true;
            setFileName(path);
            setDirty(false);
        } catch (IOException ex) {
            new ExceptionDialog(ex);
        }
        return saved;
    }

    /**
    ** Thread to execute the query and load the results into an
    ** AbstractTableModel. Note that no one else knows about the new
    ** model until construct () has completed and finished () has
    ** run. finished () arranges for listeners to be notified in the
    ** event thread, so Swing components are updated correctly.
    */
    private class ExecuteWorker extends SwingThread {

        private VectorTableModel newTableModel = new VectorTableModel();

        public void construct() {
            int rowCount = 0;
            waitEvent(QueryModel.this, "execute", "begin");
            String resultType = "query";
            try {
                waitEvent(QueryModel.this, "status", "Executing query");
                PreparedStatement statement = connection.prepareStatement(cleanSQL());
                statement.setMaxRows(configModel.getMaxRows());
                if (statement.execute()) {
                    ResultSet resultSet = statement.getResultSet();
                    metaData = resultSet.getMetaData();
                    waitEvent(QueryModel.this, "status", "Fetching results");
                    for (int i = 0; i < metaData.getColumnCount() && !stopped; i++) {
                        Thread.yield();
                        String columnName = metaData.getColumnLabel(i + 1);
                        newTableModel.addName(columnName.toLowerCase(), metaData.getColumnDisplaySize(i + 1));
                    }
                    while (resultSet.next() && !stopped) {
                        Thread.yield();
                        Vector row = newTableModel.addRow();
                        for (int i = 0; i < metaData.getColumnCount(); i++) row.add(resultSet.getString(i + 1));
                        rowCount++;
                    }
                } else {
                    rowCount = statement.getUpdateCount();
                    resultType = "update";
                    if (rowCount > 0) waitEvent(QueryModel.this, "updated", "");
                }
                statement.close();
                if (rowCount == configModel.getMaxRows()) {
                    Dialogs.showWarning("Warning", "The number of rows returned exactly matches your\n" + "maximum rows. There are probably more rows to the\n" + "query than are displayed here.");
                }
            } catch (SQLException ex) {
                if (!stopped) {
                    Dialogs.showError("SQL Error", ex.getMessage());
                }
            } finally {
                if (stopped) {
                    waitEvent(QueryModel.this, "status", "Query interrupted");
                } else {
                    waitEvent(QueryModel.this, resultType, new Integer(rowCount).toString());
                }
            }
        }

        /**
	** Query has completed and the new table model is ready to be
	** used to populate a JTable. This runs in the event thread,
	** so I can safely change the tableModel.
	*/
        public void finished() {
            executeWorker = null;
            tableModel = newTableModel;
            fireEvent(QueryModel.this, "execute", "end");
        }
    }
}
