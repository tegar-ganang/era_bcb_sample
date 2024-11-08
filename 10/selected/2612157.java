package org.storrow.jdbc4d.util;

import java.sql.*;
import java.io.*;

public class Console {

    public static String URL = Console.class.getName() + ".url";

    public static String USERNAME = Console.class.getName() + ".username";

    public static String PASSWORD = Console.class.getName() + ".password";

    public static String FILE = Console.class.getName() + ".file";

    public static String DRIVER = Console.class.getName() + ".driver";

    public static String LOG = Console.class.getName() + ".log";

    public static String PROMPT = "jdbc4d> ";

    private Connection conn = null;

    private ResultSet results = null;

    public Console(String[] args) {
        if (System.getProperty(DRIVER) == null) System.setProperty(DRIVER, "org.storrow.jdbc4d.jdbc.FourDDriver");
        if (System.getProperty(LOG) == null) System.setProperty(LOG, "4dsql.log");
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-d")) System.setProperty(URL, args[++i]); else if (args[i].equals("-u")) System.setProperty(USERNAME, args[++i]); else if (args[i].equals("-p")) System.setProperty(PASSWORD, args[++i]); else if (args[i].equals("-f")) System.setProperty(FILE, args[++i]); else if (args[i].equals("-c")) System.setProperty(DRIVER, args[++i]); else if (args[i].equals("-l")) System.setProperty(LOG, args[++i]);
        }
    }

    private Connection getConnection() throws SQLException {
        try {
            Class.forName("" + System.getProperty(DRIVER));
        } catch (Throwable e) {
            throw new ExceptionInInitializerError(e);
        }
        if (conn != null) if (!conn.isClosed()) return conn;
        if (System.getProperty(URL) == null) System.setProperty(URL, input("Enter the database URL: "));
        if (System.getProperty(URL).indexOf(":") == -1) System.setProperty(URL, "jdbc:4d:storrow:@" + System.getProperty(URL));
        if (System.getProperty(USERNAME) == null) System.setProperty(USERNAME, input("Username: "));
        if (System.getProperty(PASSWORD) == null) System.setProperty(PASSWORD, input("Password: "));
        conn = DriverManager.getConnection(("" + System.getProperty(URL)), ("" + System.getProperty(USERNAME)), ("" + System.getProperty(PASSWORD)));
        return conn;
    }

    public void init() {
        PrintStream logStream;
        Object log = System.getProperty(LOG);
        if (log == null || "stderr".equals(log)) logStream = System.err; else {
            try {
                logStream = new PrintStream(new FileOutputStream(log.toString(), true));
            } catch (IOException ioe) {
                reportException(ioe);
                logStream = System.err;
            }
        }
        DriverManager.setLogStream(logStream);
        try {
            getConnection();
            println("connected");
        } catch (Exception e) {
            reportException(e);
        }
        try {
            runFile(new File("" + System.getProperty(FILE)));
        } catch (IOException ioe) {
            reportException(ioe);
        }
    }

    protected void start() {
        init();
        do {
        } while (readCommand());
        println("Exiting...");
        System.exit(0);
    }

    protected void print(String str) {
        System.out.print(str);
    }

    protected void println(String str) {
        System.out.println(str);
    }

    protected void runFile(File f) throws IOException {
        if (f.isFile()) {
            BufferedReader reader = new BufferedReader(new FileReader(f));
            String cmd = null;
            while ((cmd = reader.readLine()) != null) {
                if (cmd.trim().startsWith("#")) {
                    println(cmd);
                    continue;
                }
                if (cmd.length() != 0) println("executing: " + cmd);
                try {
                    if (!executeCommand(cmd)) System.exit(0);
                } catch (Throwable e) {
                    reportException(e);
                    System.exit(1);
                }
            }
        }
    }

    protected void reportException(Throwable e) {
        e.printStackTrace();
    }

    protected String cmdLineInput(String prompt) {
        return input(prompt);
    }

    protected String input(String prompt) {
        print(prompt);
        System.out.flush();
        try {
            String cmd = new BufferedReader(new InputStreamReader(System.in)).readLine();
            return cmd;
        } catch (IOException e) {
            return null;
        }
    }

    public boolean readCommand() {
        try {
            String cmd = cmdLineInput(PROMPT);
            if (cmd == null) return false;
            executeCommand(cmd);
        } catch (Exception e) {
            reportException(e);
        }
        return true;
    }

    public boolean executeCommand(String cmd) throws Exception {
        if (cmd == null) return false;
        if (cmd.length() == 0) return true;
        if (cmd.toLowerCase().startsWith("assert")) {
            if (cmd.toLowerCase().startsWith("assertrecords")) {
                int checkLen = Integer.parseInt(cmd.substring("assertrecords".length() + 1));
                results.beforeFirst();
                int count = 0;
                while (results.next()) count++;
                if (count != checkLen) throw new Error("Assertion failed: " + checkLen + " != " + count); else println("Assertion passed: " + checkLen + " == " + count);
            } else {
                println("UNKNWON ASSERTION TYPE: " + cmd);
            }
        } else if (cmd.toLowerCase().startsWith("desc tables")) {
            printResultSet(getConnection().getMetaData().getTables(null, null, null, null), 0);
        } else if (cmd.toLowerCase().equals("transaction")) {
            getConnection().setAutoCommit(false);
            println("Begin transaction");
        } else if (cmd.toLowerCase().equals("reconnect")) {
            print("reconnecting...");
            conn = null;
            getConnection();
            println("done");
        } else if (cmd.toLowerCase().equals("commit")) {
            getConnection().commit();
            println("commit complete");
        } else if (cmd.toLowerCase().equals("rollback")) {
            getConnection().rollback();
            println("rollback complete");
        } else if (cmd.toLowerCase().equals("exit")) {
            println("exiting");
            return false;
        } else if (cmd.toLowerCase().startsWith("select")) {
            long t = System.currentTimeMillis();
            results = getConnection().createStatement().executeQuery(cmd);
            printResultSet(results, (System.currentTimeMillis() - t));
            return true;
        } else {
            int count = getConnection().createStatement().executeUpdate(cmd);
            println(count + " rows affected");
        }
        return true;
    }

    protected void printResultSet(ResultSet results, long duration) throws SQLException {
        int i = 0;
        if (results == null) {
            println("No results");
            return;
        }
        println("");
        ResultSetMetaData meta = results.getMetaData();
        for (int colIndex = 1; colIndex <= meta.getColumnCount(); colIndex++) {
            String col = meta.getColumnLabel(colIndex);
            while (col.length() < meta.getColumnDisplaySize(colIndex)) col += " ";
            if (col.length() > meta.getColumnDisplaySize(colIndex)) col = col.substring(0, meta.getColumnDisplaySize(colIndex) - 3) + "...";
            print(col);
        }
        println("");
        while (results != null && results.next()) {
            i++;
            for (int colIndex = 1; colIndex <= meta.getColumnCount(); colIndex++) {
                Object rob = results.getObject(colIndex);
                String val = "";
                if (rob == null) val = "NULL"; else val = rob.toString();
                while (val.length() < meta.getColumnDisplaySize(colIndex)) val += " ";
                if (val.length() > meta.getColumnDisplaySize(colIndex)) val = val.substring(0, meta.getColumnDisplaySize(colIndex) - 3) + "...";
                print(val);
            }
            println("");
        }
        println("");
        println(i + " results in " + ((float) duration / 1000) + " seconds");
    }

    public static void main(String[] args) {
        new Console(args).start();
    }
}
