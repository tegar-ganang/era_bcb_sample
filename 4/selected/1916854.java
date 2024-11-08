package dengues.system;

/**
 * Qiang.Zhang.Adolf@gmail.com class global comment. Detailled comment <br/>
 * 
 * $Id: Dengues.epf 1 2006-09-29 17:06:40Z qiang.zhang $
 * 
 */
public class ConnectivityUtils {

    private static java.util.Properties pProperties = new java.util.Properties();

    private boolean EOF = false;

    private int ln = 0;

    private boolean BATCH = true;

    private final String EKW = new String("go");

    private java.sql.Connection cConn;

    private java.sql.Statement sStatement;

    /**
     * Qiang.Zhang.Adolf@gmail.com Comment method "testConnection".
     * 
     * @return
     */
    public static java.lang.String testConnection(String driver, String url, String username, String pwd, String rootpath) {
        String resultMessage = "";
        if (rootpath != null && !"".equals(rootpath)) {
            System.setProperty("derby.system.home", rootpath);
        }
        try {
            Class.forName(driver);
            java.sql.Connection connection = java.sql.DriverManager.getConnection(url, username, pwd);
            connection.close();
            resultMessage = "connect successfully!";
        } catch (Exception ex) {
            resultMessage = ex.getMessage();
        }
        return resultMessage;
    }

    /**
     * Qiang.Zhang.Adolf@gmail.com Comment method "main".
     * 
     * @param arg
     */
    public static void main(String[] arg) {
        for (int i = 0; i < arg.length; i++) {
            String p = arg[i];
            if (p.equals("-?")) {
                printHelp();
                System.exit(0);
            }
        }
        ConnectivityUtils tool = new ConnectivityUtils();
        tool.execute(arg);
        System.exit(0);
    }

    /**
     * Qiang.Zhang.Adolf@gmail.com Comment method "execute".
     * 
     * @param arg
     */
    public void execute(String[] arg) {
        for (int i = 0; i < arg.length; i++) {
            String p = arg[i];
            if (p.charAt(0) == '-') {
                String p1 = arg[i + 1];
                if (p1.charAt(0) == '-') {
                    p1 = "";
                } else {
                    i++;
                }
                pProperties.put(p.substring(1), p1);
            }
        }
        java.io.BufferedReader in = null;
        java.util.Properties p = pProperties;
        String driver = p.getProperty("driver");
        String url = p.getProperty("url");
        String database = p.getProperty("database");
        String user = p.getProperty("user");
        String password = p.getProperty("password");
        String script = p.getProperty("script");
        String rootpath = p.getProperty("rootpath");
        String schema = p.getProperty("schema");
        if (rootpath != null && !"".equals(rootpath)) {
            System.setProperty("derby.system.home", rootpath);
        }
        boolean log = p.getProperty("log", "true").equalsIgnoreCase("true");
        try {
            BATCH = p.getProperty("batch", "true").equalsIgnoreCase("true");
            if (log) {
                trace("driver   = " + driver);
                trace("url      = " + url);
                trace("database = " + database);
                trace("user     = " + user);
                trace("password = " + password);
                trace("script   = " + script);
                trace("log      = " + log);
                trace("batch    = " + BATCH);
                trace("rootpath    = " + rootpath);
                trace("schema    = " + schema);
            }
            Class.forName(driver).newInstance();
            cConn = java.sql.DriverManager.getConnection(url, user, password);
            in = new java.io.BufferedReader(new java.io.FileReader(script));
        } catch (Exception e) {
            System.out.println("Run Script.init error: " + e.getMessage());
            e.printStackTrace();
        }
        try {
            sStatement = cConn.createStatement();
            String sql;
            sql = fileToString(in);
            java.sql.ResultSet results = sStatement.getResultSet();
            int updateCount = sStatement.getUpdateCount();
            if (updateCount == -1) {
                trace(toString(results));
            } else {
                trace("update count " + ln);
            }
        } catch (java.sql.SQLException e) {
            System.out.println("SQL Error at line " + ln + ": " + e);
        }
        try {
            cConn.close();
            in.close();
        } catch (Exception ce) {
        }
    }

    /**
     * Translate ResultSet to String representation
     * 
     * @param r
     */
    private String toString(java.sql.ResultSet r) {
        try {
            if (r == null) {
                return "No Result";
            }
            java.sql.ResultSetMetaData m = r.getMetaData();
            int col = m.getColumnCount();
            StringBuffer strbuf = new StringBuffer();
            for (int i = 1; i <= col; i++) {
                strbuf = strbuf.append(m.getColumnLabel(i) + "\t");
            }
            strbuf = strbuf.append("\n");
            while (r.next()) {
                for (int i = 1; i <= col; i++) {
                    strbuf = strbuf.append(r.getString(i) + "\t");
                    if (r.wasNull()) {
                        strbuf = strbuf.append("(null)\t");
                    }
                }
                strbuf = strbuf.append("\n");
            }
            return strbuf.toString();
        } catch (java.sql.SQLException e) {
            return null;
        }
    }

    /**
     * Read file and convert it to string.
     */
    private String fileToString(java.io.BufferedReader in) {
        if (EOF) {
            return null;
        }
        EOF = true;
        StringBuffer a = new StringBuffer();
        try {
            String line;
            while ((line = in.readLine()) != null) {
                ln = ln + 1;
                if (BATCH) {
                    if (line.startsWith("print ")) {
                        trace("\n" + line.substring(5));
                        continue;
                    }
                    if (line.equalsIgnoreCase(EKW)) {
                        EOF = false;
                        break;
                    }
                }
                if (line != null && line.trim().length() > 1) {
                    trace("SQL (" + ln + ") : " + line);
                    sStatement.execute(line);
                }
                a.append(line);
                a.append('\n');
            }
            a.append('\n');
            return a.toString();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Method declaration
     * 
     * 
     * @param s
     */
    private void trace(String s) {
        if (s != null && !"".equals(s)) {
            System.out.println(s);
        }
    }

    /**
     * Method declaration
     * 
     */
    private static void printHelp() {
        System.out.println("Usage: java Run Script [-options]\n" + "where options include:\n" + "    -driver <classname>     name of the driver class\n" + "    -url <name>             first part of the jdbc url\n" + "    -database <name>        second part of the jdbc url\n" + "    -user <name>            username used for connection\n" + "    -password <name>        password for this user\n" + "    -log <true/false>       write log to system out\n" + "    -batch <true/false>     allow go/print pseudo statements\n" + "    -script <script file>   reads from script file\n");
    }
}
