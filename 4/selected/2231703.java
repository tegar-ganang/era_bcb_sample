package mipt.rdb.util;

import java.util.Vector;
import java.io.*;
import java.sql.*;

public class SQLScript {

    private Connection connection;

    private Statement statement;

    protected Writer writer;

    public static final char comment = '!';

    public static final char createSchema = '@';

    public static final char createTable = '#';

    public static final char createTrigger = '$';

    public static final char dropSchema = '<';

    public static final char dropTable = '>';

    public static final char insert = '&';

    protected static String lineSep = System.getProperty("line.separator");

    protected String statementTerminator = ";";

    protected String callableStatementTerminator = lineSep + "/";

    protected String commentStartsWith = "-- ";

    private String explicitCommitStatement = null;

    private String passwordClause = "identified by";

    private String schemaUserRole = null;

    private String createSchemaClause = "create schema ";

    private boolean createForeignKeyInAlterTable = false;

    private boolean generateForeignKeyNameWhenAlterTable = false;

    private boolean alterTableForSelfReferencesOnly = false;

    private String dropTableCascadeClause = " cascade";

    protected String triggerLanguage = null;

    private Vector procedures = null;

    /**
 * 
 * @param conn java.sql.Connection
 */
    public SQLScript(Connection connection) throws SQLException {
        this.connection = connection;
        this.statement = connection.createStatement();
        initOptions(connection.getMetaData());
    }

    /**
 * @return String[] - nulls or constraints if they exist and needed to add after create
 * @param buffer java.lang.StringBuffer
 * @param block java.util.Vector
 * @param schemaName java.lang.String
 */
    protected final String[] addCreateTableClause(StringBuffer buffer, Vector block, String schemaName, boolean inSchemaStatement) {
        int n = block.size() - 1;
        String name = (String) block.elementAt(0);
        String fullName = schemaName == null ? name : schemaName + "." + name;
        buffer.append("create table " + (inSchemaStatement ? name : fullName) + " (");
        String param, constraints[] = null;
        int j, FKindex = 1;
        for (int i = 1; i <= n; i++) {
            param = (String) block.elementAt(i);
            if (param.charAt(0) == createTrigger) {
                if (triggerLanguage == null) continue;
                if (constraints == null) {
                    constraints = new String[n + 1];
                    constraints[0] = fullName;
                }
                j = param.indexOf(':');
                String tableToDelete = param.substring(1, j);
                param = param.substring(j + 1);
                j = param.indexOf('=');
                String PK = param.substring(0, j), FK = param.substring(j + 1);
                j = tableToDelete.indexOf('.');
                if (fullName.charAt(j + 1) == '\"') j++;
                param = PK.charAt(0) == '\"' ? PK.substring(1, PK.length() - 1) : PK;
                String procedureName = tableToDelete.substring(0, j + 1) + "del" + param + tableToDelete.substring(j + 1);
                for (j = 0; j < procedures.size(); j++) if (procedures.elementAt(j).equals(procedureName)) {
                    param = null;
                    break;
                }
                if (param != null) {
                    procedures.addElement(procedureName);
                    for (j = 1; j < i; j++) if (constraints[j] == null) break;
                    if ("PL/SQL".equals(triggerLanguage)) {
                        constraints[j] = createTrigger + "create procedure " + procedureName + "(PK int) as begin delete from " + tableToDelete + " where " + PK + "=PK; end;";
                    }
                }
                j = fullName.indexOf('.');
                if (fullName.charAt(j + 1) == '"') j++;
                param = FK.charAt(0) == '\"' ? FK.substring(1, FK.length() - 1) : PK;
                String triggerName = fullName.substring(0, j + 1) + param + fullName.substring(j + 1);
                if ("PL/SQL".equals(triggerLanguage)) {
                    constraints[i] = createTrigger + "create trigger " + triggerName + " after delete on " + fullName + " for each row begin " + procedureName + "(:old." + FK + "); end;";
                }
                continue;
            }
            if (createForeignKeyInAlterTable && param.startsWith("foreign")) {
                if ((!alterTableForSelfReferencesOnly) || param.lastIndexOf(fullName) >= 0) {
                    if (constraints == null) {
                        constraints = new String[n + 1];
                        constraints[0] = fullName;
                    }
                    if (generateForeignKeyNameWhenAlterTable) {
                        j = fullName.indexOf('.');
                        constraints[i] = j < 0 ? name : name.substring(j + 1);
                        if (constraints[i].charAt(0) == '\"') constraints[i] = constraints[i].substring(1, constraints[i].length() - 1);
                        j = param.indexOf('(');
                        constraints[i] = param.substring(0, j) + constraints[i] + "FK" + String.valueOf(FKindex++) + param.substring(j);
                    } else {
                        constraints[i] = param;
                    }
                    continue;
                }
            }
            appendToList(buffer, param, i == n);
        }
        n = buffer.length() - 2;
        if (buffer.charAt(n) == ',') {
            buffer.setCharAt(n, ')');
            buffer.deleteCharAt(n + 1);
        }
        return constraints;
    }

    /**
 * 
 * @return java.lang.StringBuffer
 * @param params java.lang.String[] - including table name!
 */
    protected final String alterTableClause(String[] params) {
        int n = params.length - 1;
        StringBuffer buffer = new StringBuffer("alter table " + params[0] + " add (");
        for (int i = 1; i <= n; i++) if (params[i] != null) {
            appendToList(buffer, params[i], i == n);
        }
        return buffer.toString();
    }

    /**
 * 
 */
    private void appendToList(StringBuffer buffer, String listElement, boolean end) {
        buffer.append(listElement);
        buffer.append(end ? ")" : ", ");
    }

    /**
 * Executes sql command stored in Vector block, which elements are Strings or Vectors
 * @param action char - one of the class constants
 * @param block java.util.Vector
 */
    public void executeBlock(char action, Vector block) throws SQLException, IOException {
        int n = block.size() - 1;
        if (n < 0) return;
        String name = (String) block.elementAt(0);
        switch(action) {
            case createSchema:
                Vector subBlock;
                if (schemaUserRole == null) {
                    if (createSchemaClause != null) {
                        StringBuffer buffer = new StringBuffer(createSchemaClause + name + " ");
                        String constraints[], allConstraints[][] = null;
                        for (int i = 1; i <= n; i++) {
                            subBlock = (Vector) block.elementAt(i);
                            constraints = addCreateTableClause(buffer, subBlock, name, true);
                            if (constraints != null) {
                                if (allConstraints == null) allConstraints = new String[n][];
                                allConstraints[i - 1] = constraints;
                            }
                            if (i < n) buffer.append(" ");
                        }
                        executeStatement(buffer.toString());
                        if (allConstraints != null) {
                            for (int i = 0; i < n; i++) {
                                constraints = allConstraints[i];
                                if (constraints != null) {
                                    constraints = executeCalls(constraints);
                                    if (constraints != null) executeStatement(alterTableClause(constraints));
                                }
                            }
                        }
                    }
                } else {
                    executeStatement("create user " + name + " " + passwordClause + " " + name);
                    if (!schemaUserRole.equals("")) executeStatement("grant " + schemaUserRole + " to " + name);
                }
                if ((schemaUserRole != null) || (createSchemaClause == null)) {
                    for (int i = 1; i <= n; i++) {
                        subBlock = (Vector) block.elementAt(i);
                        subBlock.setElementAt(name + '.' + subBlock.elementAt(0), 0);
                        executeBlock(createTable, subBlock);
                    }
                }
                break;
            case createTable:
                StringBuffer buffer = new StringBuffer();
                String constraints[] = addCreateTableClause(buffer, block, null, false);
                executeStatement(buffer.toString());
                if (constraints != null) {
                    constraints = executeCalls(constraints);
                    if (constraints != null) executeStatement(alterTableClause(constraints));
                }
                break;
            case insert:
                name = "insert into " + name + " values (";
                for (int i = 1; i <= n; i++) {
                    executeStatement(name + ((String) block.elementAt(i)) + ")");
                }
                break;
            case dropSchema:
                name = (schemaUserRole == null ? "schema " : "user ") + name;
                executeStatement("drop " + name + " cascade");
                break;
            case dropTable:
                executeStatement("drop table " + name + dropTableCascadeClause);
                break;
        }
    }

    /**
 * Recognizes block from reader and executes it
 * @return java.lang.String - see recognizeBlock()
 * @param reader java.io.BufferedReader
 * @param action char
 * @param block java.util.Vector
 */
    public String executeBlock(BufferedReader reader, char action, Vector block) throws IOException, SQLException {
        String result = recognizeBlock(reader, action, block);
        executeBlock(action, block);
        return result;
    }

    /**
 * 
 * @return int
 * @param writer java.io.Writer
 * @param sql java.lang.String
 */
    public int executeCall(String sql) throws SQLException, IOException {
        if (writer == null) {
            return connection.prepareCall(sql).executeUpdate();
        } else {
            writer.write(sql + callableStatementTerminator);
            writer.flush();
            return 0;
        }
    }

    /**
 * Creates procedures and triggers form constraints
 * @return java.lang.String[]
 * @param writer java.io.Writer
 * @param constraints java.lang.String[]
 */
    protected String[] executeCalls(String[] constraints) throws SQLException, IOException {
        Vector buffer = new Vector();
        for (int j = 0; j < constraints.length; j++) {
            if (constraints[j] == null) continue;
            if (constraints[j].charAt(0) == createTrigger) executeCall(constraints[j].substring(1)); else buffer.addElement(constraints[j]);
        }
        if (buffer.size() <= 1) return null;
        String[] restConstraints = new String[buffer.size()];
        buffer.copyInto(restConstraints);
        return restConstraints;
    }

    /**
 * Forms SQL commands from lines of the given reader and executes them
 * @return int - number of executed commands
 * @param reader java.io.BufferedReader
 * @param writer java.io.Writer - null to save in DB, !null to save in scriptWriter
 */
    public int executeScript(BufferedReader reader, Writer writer) throws SQLException, IOException {
        setWriter(writer);
        if (!reader.ready()) return 0;
        if (triggerLanguage != null) procedures = new Vector();
        boolean wasAutoCommit = false;
        if (writer == null) {
            wasAutoCommit = connection.getAutoCommit();
            if (wasAutoCommit) connection.setAutoCommit(false); else connection.commit();
            if (explicitCommitStatement != null) {
                statement.executeUpdate(explicitCommitStatement);
            }
        }
        String current;
        Vector block = new Vector();
        current = reader.readLine();
        int i = 0;
        try {
            do {
                if ((current.length() == 0) || (current.charAt(0) == comment)) {
                    if ((writer != null) && (current.length() > 0) && (commentStartsWith != null)) {
                        writer.write(commentStartsWith + current.substring(1) + statementTerminator);
                    }
                    current = reader.readLine();
                    continue;
                }
                block.addElement(current.substring(1));
                current = executeBlock(reader, current.charAt(0), block);
                i++;
                block.removeAllElements();
            } while (reader.ready());
            if ((current != null) && (current.length() != 0) && (current.charAt(0) != comment)) {
                block.addElement(current.substring(1));
                executeBlock(reader, current.charAt(0), block);
                i++;
            }
        } catch (Exception e) {
            if (writer == null) {
                connection.rollback();
                if (wasAutoCommit) connection.setAutoCommit(true);
            }
            System.out.println(current);
            if (e instanceof SQLException) throw (SQLException) e; else if (e instanceof IOException) throw (IOException) e; else {
                e.printStackTrace();
                throw new SQLException("Undefined exception");
            }
        }
        connection.commit();
        if (wasAutoCommit) connection.setAutoCommit(true);
        return i;
    }

    /**
 * 
 * @return int
 * @param writer java.io.Writer
 * @param sql java.lang.String
 */
    public int executeStatement(String sql) throws SQLException, IOException {
        if (writer == null) {
            return statement.executeUpdate(sql);
        } else {
            writer.write(sql + statementTerminator);
            writer.flush();
            return 0;
        }
    }

    /**
 * 
 */
    protected void initOptions(DatabaseMetaData meta) throws SQLException {
        String dbms = meta.getDatabaseProductName();
        if (dbms.indexOf("Oracle") >= 0) {
            explicitCommitStatement = null;
            commentStartsWith = "-- ";
            passwordClause = "identified by";
            if (dbms.indexOf("Lite") >= 0) {
                schemaUserRole = null;
                createSchemaClause = "create schema ";
                createForeignKeyInAlterTable = true;
                generateForeignKeyNameWhenAlterTable = false;
                alterTableForSelfReferencesOnly = false;
                dropTableCascadeClause = " cascade";
                triggerLanguage = null;
            } else {
                schemaUserRole = "RESOURCE";
                createSchemaClause = null;
                createForeignKeyInAlterTable = false;
                dropTableCascadeClause = " cascade constraints";
                triggerLanguage = "PL/SQL";
            }
        } else if (dbms.indexOf("Cache") >= 0) {
            explicitCommitStatement = "set transaction %COMMITMODE EXPLICIT";
            commentStartsWith = null;
            passwordClause = "identify by";
            schemaUserRole = null;
            createSchemaClause = null;
            createForeignKeyInAlterTable = true;
            generateForeignKeyNameWhenAlterTable = true;
            alterTableForSelfReferencesOnly = true;
            dropTableCascadeClause = " cascade";
            triggerLanguage = null;
        }
        statementTerminator += lineSep;
        callableStatementTerminator += lineSep;
    }

    /**
 * 
 * @param args java.lang.String[]
 */
    public static void main(String[] args) {
        if (args.length <= 0) {
            System.out.println(" *** SQL script generator and executor ***");
            System.out.println(" You must specify name of the file with SQL script data");
            System.out.println(" Fisrt rows of this file must be:");
            System.out.println(" 1) JDBC driver class for your DBMS");
            System.out.println(" 2) URL for your database instance");
            System.out.println(" 3) user in that database (with administrator priviliges)");
            System.out.println(" 4) password of that user");
            System.out.println(" Next rows can have: '@' before schema to create,");
            System.out.println("   '#' before table to create, '&' before table to insert,");
            System.out.println("   '$' before trigger (inverse 'FK on delete cascade') to create,");
            System.out.println("   '>' before table to drop, '<' before schema to drop.");
            System.out.println(" Other rows contain parameters of these actions:");
            System.out.println("   for & action each parameter is a list of values,");
            System.out.println("   for @ -//- is # acrion, for # -//- is column/constraint ");
            System.out.println("    definition or $ action. $ syntax to delete from table:");
            System.out.println("    fullNameOfTable:itsColInWhereClause=matchingColOfThisTable");
            System.out.println(" '!' before row means that it is a comment.");
            System.out.println(" If some exception is occured, all script is rolled back.");
            System.out.println(" If you specify 2nd command line argument - file name too -");
            System.out.println("   connection will be established but all statements will");
            System.out.println("   be saved in that output file and not transmitted to DB");
            System.out.println(" If you specify 3nd command line argument - connect_string -");
            System.out.println("   connect information will be added to output file");
            System.out.println("   in the form 'connect user/password@connect_string'");
            System.exit(0);
        }
        try {
            String[] info = new String[4];
            BufferedReader reader = new BufferedReader(new FileReader(new File(args[0])));
            Writer writer = null;
            try {
                for (int i = 0; i < 4; i++) info[i] = reader.readLine();
                try {
                    Class.forName(info[0]);
                    Connection connection = DriverManager.getConnection(info[1], info[2], info[3]);
                    SQLScript script = new SQLScript(connection);
                    if (args.length > 1) {
                        writer = new FileWriter(args[1]);
                        if (args.length > 2) writer.write("connect " + info[2] + "/" + info[3] + "@" + args[2] + script.statementTerminator);
                    }
                    try {
                        System.out.println(script.executeScript(reader, writer) + " updates has been performed during script execution");
                    } catch (SQLException e4) {
                        reader.close();
                        if (writer != null) writer.close();
                        System.out.println(" Script execution error: " + e4);
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
 * Converts sql command from reader to vector elements
 * Note: the first token of block must be already read and placed to Vector
 * @return java.lang.String - next token after block or null if reader has no tokens
 * @param reader java.io.BufferedReader
 * @param action char - can`t be comment
 * @param block java.util.Vector
 */
    public String recognizeBlock(BufferedReader reader, char action, Vector block) throws IOException {
        if (action == createSchema) {
            if (!reader.ready()) return null;
            String current = reader.readLine();
            Vector subBlock;
            if ((current.length() == 0) || (current.charAt(0) == comment)) return recognizeBlock(reader, action, block);
            do {
                subBlock = new Vector();
                block.addElement(subBlock);
                subBlock.addElement(current.substring(1));
                current = recognizeSubBlock(reader, current.charAt(0), subBlock);
                if ((current == null) || (current.charAt(0) != createTable)) return current;
            } while (true);
        } else {
            return recognizeSubBlock(reader, action, block);
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
        if (!reader.ready()) return null;
        String current;
        do {
            current = reader.readLine();
            if (current == null || current.length() == 0) continue;
            switch(current.charAt(0)) {
                case comment:
                    continue;
                case createSchema:
                case createTable:
                case insert:
                case dropSchema:
                case dropTable:
                    return current;
                default:
                    block.addElement(current);
            }
        } while (reader.ready());
        return null;
    }

    /**
 * 
 * @param writer java.io.Writer
 */
    public void setWriter(Writer writer) {
        this.writer = writer;
    }
}
