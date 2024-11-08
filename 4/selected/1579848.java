package mipt.rdb.util;

import java.util.Vector;
import java.io.*;
import java.sql.*;

public class DMLScript extends SQLScript {

    public static final char update = '*';

    public static final char delete = '^';

    /**
 * DMLScript constructor comment.
 * @param connection java.sql.Connection
 * @exception java.sql.SQLException The exception description.
 */
    public DMLScript(java.sql.Connection connection) throws java.sql.SQLException {
        super(connection);
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
            case delete:
                name = "delete from " + name;
                if (n > 0) {
                    StringBuffer buffer = new StringBuffer(name);
                    buffer.append(" where ");
                    buffer.append((String) block.elementAt(1));
                    if (n > 1) {
                        for (int i = 2; i <= n; i++) {
                            String str = (String) block.elementAt(i);
                            if (str.equalsIgnoreCase("AND") || str.equalsIgnoreCase("OR")) {
                                buffer.append(' ' + str + ' ' + block.elementAt(++i));
                            } else {
                                buffer.append(" or " + str);
                            }
                        }
                    }
                    name = buffer.toString();
                }
                executeStatement(name);
                break;
            case insert:
                name = "insert into " + name + " values (";
                for (int i = 1; i <= n; i++) {
                    StringBuffer buffer = new StringBuffer(name);
                    String list = (String) block.elementAt(i);
                    if (!isCommaDelimited(list)) {
                        String[] values = getListItems(list, " \t", false);
                        buffer.append(values[0]);
                        for (int j = 1; j < values.length; j++) {
                            buffer.append(", " + quoteValue(values[j]));
                        }
                    } else {
                        buffer.append(list);
                    }
                    buffer.append(')');
                    executeStatement(buffer.toString());
                }
                break;
            case update:
                name = "update " + name + " set ";
                String delims = ", \t";
                String[] columns = getListItems((String) block.elementAt(1), delims, true);
                for (int i = 2; i <= n; i++) {
                    String list = (String) block.elementAt(i);
                    String[] values = getListItems(list, delims, true);
                    values[1] = quoteValue(values[1]);
                    executeStatement(name + columns[1] + " = " + values[1] + " where " + columns[0] + " = " + values[0]);
                }
                break;
        }
    }

    /**
 //* Does not use this if values contains ',' as delimiter: I an support it but with loss if ',' in quoted items
 * @return java.lang.String
 * @param row java.lang.String
 */
    protected String[] getListItems(String values, String delims, boolean maximum2items) {
        java.util.StringTokenizer tok = new java.util.StringTokenizer(values, delims);
        Vector buffer = new Vector();
        boolean unclosedQuote = false;
        while (tok.hasMoreTokens()) {
            int i = buffer.size();
            String token;
            if (i == 1 && maximum2items && delims.indexOf(',') >= 0) token = tok.nextToken(" \t"); else token = tok.nextToken();
            if (unclosedQuote || (maximum2items && i == 2)) {
                if (token.endsWith("'")) unclosedQuote = false;
                i--;
                buffer.set(i, buffer.get(i) + " " + token);
            } else {
                if (token.charAt(0) == '\'') unclosedQuote = true;
                buffer.add(token);
            }
        }
        String[] items = new String[buffer.size()];
        buffer.copyInto(items);
        return items;
    }

    /**
 * 
 * @return boolean
 * @param string java.lang.String
 */
    protected static final boolean isCommaDelimited(String string) {
        boolean result = false;
        do {
            int comma = string.indexOf(',');
            if (comma < 0) return result;
            result = true;
            int firstQuote = string.indexOf('\'');
            if (firstQuote >= 0) {
                int secondQuote = string.indexOf('\'', firstQuote + 1);
                if (firstQuote < comma && comma < secondQuote) {
                    string = string.substring(secondQuote + 1);
                    result = false;
                } else {
                    return result;
                }
            } else {
                return result;
            }
        } while (true);
    }

    /**
 * 
 * @param args java.lang.String[]
 */
    public static void main(String[] args) {
        if (args.length <= 0) {
            System.out.println(" *** DML script generator and executor ***");
            System.out.println(" You must specify name of the file with SQL script data");
            System.out.println(" Fisrt rows of this file must be:");
            System.out.println(" 1) JDBC driver class for your DBMS");
            System.out.println(" 2) URL for your database instance");
            System.out.println(" 3) user in that database (with sufficient priviliges)");
            System.out.println(" 4) password of that user");
            System.out.println(" Next rows can have:");
            System.out.println("   '&' before table to insert into,");
            System.out.println("   '^' before table delete from,");
            System.out.println("   '*' before table update.");
            System.out.println(" Other rows contain parameters of these actions:");
            System.out.println("   for & action each parameter is a list of values,");
            System.out.println("   for * action -//- pare of values with 1st PK (will be in where clause),");
            System.out.println("   for ^ (not obligatory) -//- part of where clause or AND or OR");
            System.out.println("    (by depault parts are united with OR)");
            System.out.println(" Note: despite SQLScript, 1) list (pare) of values can be separated");
            System.out.println("   by space or tab, not only comma and 2) string values can be not quoted,");
            System.out.println("   but in this (2) case only 2 values allowed (even in insert)");
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
                for (int i = 0; i < info.length; i++) info[i] = reader.readLine();
                try {
                    Class.forName(info[0]);
                    Connection connection = DriverManager.getConnection(info[1], info[2], info[3]);
                    SQLScript script = new DMLScript(connection);
                    if (args.length > 1) {
                        writer = new BufferedWriter(new FileWriter(args[1]));
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
 * 
 * @return java.lang.String
 * @param value java.lang.String
 */
    protected String quoteValue(String value) {
        if (!(value.charAt(0) == '\'')) return '\'' + value + '\'';
        return value;
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
                case delete:
                case insert:
                case update:
                    return current;
                default:
                    block.addElement(current);
            }
        } while (reader.ready());
        return null;
    }
}
