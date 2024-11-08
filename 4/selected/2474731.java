package ddbserver.server;

import ddbserver.common.ExceptionHandler;
import ddbserver.common.ResultType;
import ddbserver.common.SQLResult;
import ddbserver.connections.MySQLConnector;
import ddbserver.constant.Constant;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 *
 * @author qixiao
 */
public class DatabaseUtil {

    private File fileData;

    private MySQLConnector sqlConnector;

    private String siteName;

    private boolean executeSQLNoResult(String sql) {
        if (sql == null) {
            return false;
        }
        if (sqlConnector.executeSQL(sql)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean executeSQLWithResult(String sql, ResultType rt) {
        if (sql == null || rt == null) {
            return false;
        }
        Vector records = rt.getValuesVector();
        if (!sqlConnector.executeBatchInsertSlow(sql, records)) {
            return false;
        }
        return true;
    }

    public DatabaseUtil(MySQLConnector sqlCon) {
        this.sqlConnector = sqlCon;
    }

    public ResultSet getQueryResult(String sql) {
        return sqlConnector.executeQuery(sql);
    }

    public boolean ImportData() {
        if (fileData == null) {
            return false;
        }
        String line = new String();
        BufferedReader br;
        BufferedWriter bw;
        String tableName = new String();
        List<String> columns = new ArrayList<String>();
        long recordsNumber;
        String sql = new String();
        File tempDataFile;
        String filePath = new String();
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(fileData)));
            if (br.ready()) {
                if ((line = br.readLine()) != null) {
                    do {
                        tableName = siteName + "_" + getTableName(line);
                        columns = getTableColumns(line);
                        tempDataFile = new File("./Data/" + tableName + ".txt");
                        tempDataFile.createNewFile();
                        filePath = tempDataFile.getCanonicalPath();
                        sql = generateSQL(tableName, columns, filePath);
                        recordsNumber = getRecordNumber(line);
                        bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempDataFile)));
                        for (long i = 0; i < recordsNumber; i++) {
                            bw.write(br.readLine() + "\r\n");
                        }
                        bw.close();
                        if (sqlConnector != null) {
                            sqlConnector.executeSQL(sql);
                        } else {
                            return false;
                        }
                    } while ((line = br.readLine()) != null);
                }
                br.close();
            }
        } catch (Exception e) {
            ExceptionHandler.handleExcptin(e);
        }
        return true;
    }

    public String getTableName(String line) {
        String tableName = line.split("\\(")[0].trim();
        return tableName;
    }

    public int getRecordNumber(String line) {
        Integer number = new Integer(line.split("\\)")[1].trim());
        return number.intValue();
    }

    public List<String> getTableColumns(String line) {
        List<String> cols = new ArrayList<String>();
        String[] column = line.split("\\(")[1].split("\\)")[0].split(",");
        for (int i = 0; i < column.length; i++) {
            cols.add(column[i]);
        }
        return cols;
    }

    public String generateSQL(String tableName, List<String> cols, String file) {
        String query;
        String columns = new String();
        if (cols != null) {
            for (int i = 0; i < cols.size(); i++) {
                if (i != cols.size() - 1) {
                    columns = columns + cols.get(i) + ",";
                } else {
                    columns = columns + cols.get(i);
                }
            }
        }
        query = "LOAD DATA LOCAL INFILE \'" + file.replace("\\", "/") + "\' INTO TABLE " + tableName + " FIELDS TERMINATED BY \',\' ENCLOSED BY \'" + "\\'" + "\' (" + columns + ")";
        return query;
    }

    public boolean execute(SQLResult sqlResult) {
        if (sqlResult == null) {
            return false;
        }
        if (sqlResult.getSqlType().equals(Constant.LOCAL_COMMAND_NONRESULT) || sqlResult.getSqlType().equals(Constant.REMOTE_COMMAND_NONRESULT)) {
            if (executeSQLNoResult(sqlResult.getSql())) {
                return true;
            }
        } else if (sqlResult.getSqlType().equals(Constant.LOCAL_COMMAND_RESULET) || sqlResult.getSqlType().equals(Constant.REMOTE_COMMAND_RESULTE)) {
            if (executeSQLWithResult(sqlResult.getSql(), sqlResult.getResult())) {
                return true;
            }
        }
        return false;
    }
}
