package mipt.rdb.util;

import java.util.Vector;
import java.io.*;
import java.sql.*;

public class SQLImporter extends SQLScript {

    public static final char importTableData = ')';

    public static final char importSchemaData = '(';

    public static final char importTableStructure = '}';

    public static final char importSchemaStructure = '{';

    public static final int SQL_FORMAT = 0;

    public static final int SHORT_FORMAT = 1;

    protected int format = -1;

    private Statement inStatement;

    private DatabaseMetaData inMetaData;

    public boolean shouldImportSchemaName = false;

    public boolean shouldQuoteNames = false;

    public static final char importSchemaName = '@';

    public static final char quoteNames = '"';

    /**
 */
    public SQLImporter(Connection outConnection, Connection inConnection) throws java.sql.SQLException {
        super(outConnection == null ? inConnection : outConnection);
        this.inMetaData = inConnection.getMetaData();
        if (false) inConnection.setReadOnly(true);
        this.inStatement = inConnection.createStatement();
    }

    /**
 * 
 * @param buffer java.lang.StringBuffer
 * @param col java.lang.String
 * @param type java.lang.String
 * @param size int
 * @param decimals int
 * @param defaultValue java.lang.String
 * @param isNullable boolean
 */
    protected void addColumnDefinition(StringBuffer buffer, String col, int type, String typeName, int size, int decimals, String defaultValue, boolean isNullable, boolean PK) {
        buffer.append(col);
        buffer.append(' ');
        switch(type) {
            case Types.VARCHAR:
                buffer.append("VARCHAR(");
                buffer.append(size);
                buffer.append(')');
                break;
            case Types.INTEGER:
                buffer.append("INT");
                break;
            case Types.DOUBLE:
            case Types.DECIMAL:
                if (size == 1) {
                    buffer.append("NUMERIC(1)");
                    break;
                } else if (size == 22) {
                    buffer.append("INT");
                    break;
                }
                buffer.append("DECIMAL(");
                buffer.append(size);
                buffer.append(',');
                buffer.append(decimals);
                buffer.append(')');
                break;
            case Types.BIT:
                buffer.append("NUMERIC(1)");
                break;
            case Types.LONGVARBINARY:
            case Types.LONGVARCHAR:
            case Types.OTHER:
                buffer.append(typeName);
                buffer.append('(');
                buffer.append(size);
                buffer.append(')');
                break;
            case Types.DATE:
            case Types.TIME:
            case Types.TIMESTAMP:
            default:
                buffer.append(typeName);
                break;
        }
        if (PK) buffer.append(" primary key");
        if (!isNullable && !PK) {
            buffer.append(" NOT NULL");
        }
        if (defaultValue != null) {
            buffer.append(" DEFAULT ");
            buffer.append(defaultValue);
        }
    }

    /**
 * 
 * @return java.lang.String
 * @param value java.lang.String
 */
    protected void addValuesFromSet(StringBuffer buffer, ResultSet set, ResultSetMetaData meta, int n) throws SQLException {
        for (int i = 1; i <= n; i++) {
            buffer.append(getFromSet(i, set, meta));
            if (i < n) buffer.append(", ");
        }
    }

    /**
 * 
 * @return java.lang.String
 * @param name java.lang.String
 * @param block java.util.Vector
 */
    private String changeSchemaOptions(String schema, Vector block) {
        if (schema == null) {
            shouldImportSchemaName = false;
            shouldQuoteNames = false;
            return schema;
        }
        if (schema.charAt(0) == importSchemaName) {
            schema = schema.substring(1);
            shouldImportSchemaName = true;
        } else shouldImportSchemaName = false;
        if (schema.charAt(0) == quoteNames) {
            schema = schema.substring(1);
            shouldQuoteNames = true;
        } else shouldQuoteNames = false;
        if (shouldImportSchemaName || shouldQuoteNames) block.set(0, schema);
        return schema;
    }

    /**
 * Executes sql command stored in Vector block, which elements are Strings or Vectors
 * @param action char - one of the class constants
 * @param block java.util.Vector
 */
    public void executeBlock(char action, Vector block) throws SQLException, IOException {
        int n = block.size();
        if (n < 2) return;
        String name = (String) block.elementAt(0);
        ResultSet set = (ResultSet) block.elementAt(1);
        w: switch(action) {
            case importTableData:
                ResultSetMetaData meta = set.getMetaData();
                int columnCount = meta.getColumnCount();
                String fullName = getFullQuotedName(name, block);
                boolean writeHeader = true;
                StringBuffer buffer;
                while (set.next()) {
                    switch(format) {
                        case SQL_FORMAT:
                            buffer = new StringBuffer("insert into " + fullName + " values (");
                            addValuesFromSet(buffer, set, meta, columnCount);
                            buffer.append(')');
                            break;
                        case SHORT_FORMAT:
                            if (writeHeader && format == SHORT_FORMAT) {
                                executeStatement(insert + fullName);
                                writeHeader = false;
                            }
                            buffer = new StringBuffer(100);
                            addValuesFromSet(buffer, set, meta, columnCount);
                            break;
                        default:
                            break w;
                    }
                    executeStatement(buffer.toString());
                }
                break;
            case importSchemaData:
                block.addElement(name);
                while (set.next()) {
                    String table = set.getString("TABLE_NAME");
                    block.setElementAt(table, 0);
                    block.setElementAt(inStatement.executeQuery("select * from " + getFullQuotedName(table, block)), 1);
                    executeBlock(importTableData, block);
                }
                break;
            case importTableStructure:
                executeCreateTable(set, (String) block.get(2), name, false);
                break;
            case importSchemaStructure:
                if (!set.next()) break;
                String table = set.getString("TABLE_NAME");
                do {
                    table = executeCreateTable(set, name, table, true);
                } while (table != null);
                break;
        }
        set.close();
    }

    /**
 * 
 * @return java.lang.String
 * @param set java.sql.ResultSet
 * @param schema java.lang.String
 * @param name java.lang.String
 * @param shouldNotNext boolean
 */
    protected String executeCreateTable(ResultSet set, String schema, String name, boolean shouldNotNext) throws SQLException, IOException {
        StringBuffer buffer = null;
        boolean quoted = name.startsWith("\"");
        String fullName = name;
        if (quoted) {
            name = name.substring(1, name.length() - 1);
            if (!shouldQuoteNames) fullName = name;
        } else {
            if (shouldQuoteNames) fullName = '"' + name + '"';
        }
        if (shouldImportSchemaName) fullName = schema + "." + fullName;
        switch(format) {
            case SQL_FORMAT:
                buffer = new StringBuffer("create table " + fullName + " (" + lineSep);
                break;
            case SHORT_FORMAT:
                executeStatement(lineSep + createTable + fullName);
                break;
            default:
                return null;
        }
        boolean wasOneColumn = false;
        String sql;
        while (shouldNotNext || set.next()) {
            shouldNotNext = false;
            if (!set.getString("TABLE_SCHEM").equals(schema)) continue;
            String table = set.getString("TABLE_NAME");
            if (!table.equals(name)) {
                if (wasOneColumn && format == SQL_FORMAT) {
                    sql = buffer.substring(0, buffer.length() - 3) + ')';
                    executeStatement(sql);
                }
                return table;
            }
            if (format == SQL_FORMAT) buffer.append('\t'); else buffer = new StringBuffer(50);
            sql = set.getString("COLUMN_NAME");
            if (shouldQuoteNames) sql = '"' + sql + '"';
            addColumnDefinition(buffer, sql, set.getShort("DATA_TYPE"), set.getString("TYPE_NAME"), set.getInt("COLUMN_SIZE"), set.getInt("DECIMAL_DIGITS"), set.getString("COLUMN_DEF"), set.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls, !wasOneColumn);
            if (format == SQL_FORMAT) {
                buffer.append(',');
                buffer.append(lineSep);
                if (lineSep.length() == 1) buffer.append(' ');
            } else {
                executeStatement(buffer.toString());
            }
            wasOneColumn = true;
        }
        if (wasOneColumn && format == SQL_FORMAT) {
            sql = buffer.substring(0, buffer.length() - 3) + ')';
            executeStatement(sql);
        }
        return null;
    }

    /**
 * Can be overriden to perform convertion from java form to other form (e.g. ODBC)
 *   to use in standard tools (conversion depends on DBMS, but as usual, it is last URL token)
 * @return java.lang.String
 * @param sourceURL java.lang.String
 */
    protected String getDatabaseURL(String javaURL) {
        int index = javaURL.lastIndexOf(':');
        if (index < 0) index = javaURL.lastIndexOf('/');
        if (index < 0) return javaURL;
        return javaURL.substring(index + 1);
    }

    /**
 * 
 * @return java.lang.String
 * @param value java.lang.String
 */
    protected String getFromSet(int i, ResultSet set, ResultSetMetaData meta) throws SQLException {
        int type = meta.getColumnType(i);
        if (type == Types.OTHER) {
            return getLOBFromSet(i, set, meta);
        }
        String value = set.getString(i);
        if (value == null) return null;
        switch(type) {
            case Types.VARCHAR:
                if (format == SHORT_FORMAT && writer != null) {
                    value = replaceChars(value, '\r', '\t');
                    value = replaceChars(value, '\n', '\t');
                }
                return '\'' + value + '\'';
            case Types.DATE:
            case Types.TIME:
            case Types.TIMESTAMP:
                int dotIndex = value.lastIndexOf('.');
                if (dotIndex >= 0) value = value.substring(0, dotIndex);
                return "TO_DATE('" + value + "', 'YYYY-MM-DD HH24:MI:SS')";
        }
        return value;
    }

    /**
 * 
 * @return java.lang.String
 * @param name java.lang.String
 */
    private String getFullQuotedName(String name, Vector block) {
        String fullName = name;
        if (name.startsWith("\"")) {
            if (!shouldQuoteNames) fullName = name.substring(1, name.length() - 1);
        } else {
            if (shouldQuoteNames) fullName = '"' + name + '"';
        }
        if (shouldImportSchemaName && block.size() > 2) {
            fullName = block.get(2) + "." + fullName;
        }
        return fullName;
    }

    /**
 * 
 */
    protected String getLOBFromSet(int i, ResultSet set, ResultSetMetaData meta) throws SQLException {
        return meta.getColumnTypeName(i).equals("CLOB") ? "empty_clob()" : "empty_blob()";
    }

    /**
 * 
 * @param block java.util.Vector
 */
    protected void importResultSet(char action, Vector block) throws IOException {
        try {
            String name = (String) block.get(0);
            switch(action) {
                case importTableData:
                    changeSchemaOptions(null, null);
                    block.addElement(inStatement.executeQuery("select * from " + name));
                    break;
                case importSchemaData:
                    name = changeSchemaOptions(name, block);
                    block.addElement(inMetaData.getTables(null, name, "%", new String[] { "TABLE" }));
                    break;
                case importTableStructure:
                    changeSchemaOptions(null, null);
                    int index = name.indexOf('.');
                    String schema;
                    if (index < 0) schema = inMetaData.getUserName(); else {
                        schema = name.substring(0, index);
                        name = name.substring(index + 1);
                    }
                    if (name.startsWith("\"") && inMetaData.getDatabaseProductName().indexOf("Lite") < 0) name = name.substring(1, name.length() - 1);
                    block.addElement(inMetaData.getColumns(null, "%", name, "%"));
                    block.addElement(schema);
                    break;
                case importSchemaStructure:
                    name = changeSchemaOptions(name, block);
                    block.addElement(inMetaData.getColumns(null, name, "%", "%"));
                    break;
            }
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
 * 
 * @param args java.lang.String[]
 */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println(" *** DDL (creates) and DML (inserts) script importer from DB ***");
            System.out.println(" You must specify name of the file with script importing data");
            System.out.println(" Fisrt rows of this file must be:");
            System.out.println(" 1) JDBC driver class for your DBMS");
            System.out.println(" 2) URL for your database instance");
            System.out.println(" 3) user in that database (with sufficient priviliges)");
            System.out.println(" 4) password of that user");
            System.out.println(" Next rows can have:");
            System.out.println("   '}' before table to create,");
            System.out.println("   '{' before schema to create tables in,");
            System.out.println("   ')' before table to insert into,");
            System.out.println("   '(' before schema to insert into tables in.");
            System.out.println(" '!' before row means that it is a comment.");
            System.out.println(" If some exception is occured, all script is rolled back.");
            System.out.println(" 2nd command line argument is name of output file;");
            System.out.println("   if its extension is *.sql, its format is standard SQL");
            System.out.println("   otherwize format is short one, understanded by SQLScript tool");
            System.out.println(" Connection information remains unchanged in the last format");
            System.out.println("   but in the first one it takes form 'connect user/password@URL'");
            System.out.println("   where URL can be formed with different rools for different DBMSs");
            System.out.println(" If file (with short format header) already exists and you specify");
            System.out.println("   3rd command line argument -db, we generate objects in the database");
            System.out.println("   (known from the file header; must differ from 1st DB) but not in file");
            System.out.println(" Note: when importing to a file of short format, line separators");
            System.out.println("    in VARCHARS will be lost; LOBs will be empty for any file");
            System.exit(0);
        }
        try {
            String[] info = new String[4];
            BufferedReader reader = new BufferedReader(new FileReader(new File(args[0])));
            Writer writer = null;
            Connection outConnection = null;
            try {
                for (int i = 0; i < info.length; i++) info[i] = reader.readLine();
                try {
                    Class.forName(info[0]);
                    Connection connection = DriverManager.getConnection(info[1], info[2], info[3]);
                    int format = args[1].toLowerCase().endsWith("sql") ? SQL_FORMAT : SHORT_FORMAT;
                    File file = new File(args[1]);
                    if (format == SHORT_FORMAT) {
                        if (file.exists() && args.length > 2 && args[2].equalsIgnoreCase("-db")) {
                            String[] outInfo = new String[info.length];
                            BufferedReader outReader = new BufferedReader(new FileReader(file));
                            for (int i = 0; i < outInfo.length; i++) outInfo[i] = reader.readLine();
                            outReader.close();
                            if (!(outInfo[1].equals(info[1]) && outInfo[2].equals(info[2]))) {
                                Class.forName(info[0]);
                                outConnection = DriverManager.getConnection(outInfo[1], outInfo[2], outInfo[3]);
                                format = SQL_FORMAT;
                            }
                        }
                    }
                    if (outConnection == null) writer = new BufferedWriter(new FileWriter(file));
                    SQLImporter script = new SQLImporter(outConnection, connection);
                    script.setFormat(format);
                    if (format == SQL_FORMAT) {
                        writer.write("connect " + info[2] + "/" + info[3] + "@" + script.getDatabaseURL(info[1]) + script.statementTerminator);
                    } else {
                        for (int i = 0; i < info.length; i++) writer.write(info[i] + lineSep);
                        writer.write(lineSep);
                    }
                    try {
                        System.out.println(script.executeScript(reader, writer) + " operations with tables has been generated during import");
                    } catch (SQLException e4) {
                        reader.close();
                        if (writer != null) writer.close(); else outConnection.close();
                        System.out.println(" Script generation error: " + e4);
                    }
                    connection.close();
                } catch (Exception e3) {
                    reader.close();
                    if (writer != null) writer.close();
                    System.out.println(" Connection error: " + e3);
                }
            } catch (IOException e2) {
                System.out.println("Error in file " + args[0]);
            }
        } catch (FileNotFoundException e1) {
            System.out.println("File " + args[0] + " not found");
        }
    }

    /**
 * Converts sql command (not recurcive like create schema) from reader to vector elements
 * Note: the first token of block must be already read and placed to Vector
 * @return java.lang.String - next token after block or null if reader has no tokens
 * @param reader java.io.BufferedReader
 * @param action char
 * @param block java.util.Vector
 */
    public String recognizeSubBlock(BufferedReader reader, char action, Vector block) throws IOException {
        importResultSet(action, block);
        if (!reader.ready()) return null;
        String current;
        do {
            current = reader.readLine();
            if (current == null || current.length() == 0) continue;
            switch(current.charAt(0)) {
                case importTableData:
                case importSchemaData:
                case importTableStructure:
                case importSchemaStructure:
                    return current;
                case comment:
                    continue;
                default:
                    continue;
            }
        } while (reader.ready());
        return null;
    }

    /**
 * 
 * @return java.lang.String
 * @param s java.lang.String
 * @param source char
 * @param dest char
 */
    private String replaceChars(String s, char source, char dest) {
        int index = 0;
        StringBuffer buf = null;
        do {
            index = s.indexOf(source, index + 1);
            if (index < 0) break;
            if (buf == null) buf = new StringBuffer(s);
            buf.setCharAt(index, dest);
        } while (true);
        return buf == null ? s : buf.toString();
    }

    /**
 * 
 * @param format int
 */
    public void setFormat(int format) {
        if (format == this.format) return;
        if (format == SHORT_FORMAT) statementTerminator = lineSep; else statementTerminator = ";" + lineSep;
        this.format = format;
    }
}
