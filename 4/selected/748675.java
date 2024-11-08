package com.continuent.tungsten.jdbc.router.main;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;
import junit.framework.TestCase;
import org.apache.log4j.Logger;
import com.continuent.tungsten.commons.exec.ArgvIterator;
import com.continuent.tungsten.router.manager.RouterException;
import com.continuent.tungsten.router.manager.RouterManager;

public class TestRouterManager extends TestCase {

    /**
     * Logger
     */
    private static Logger logger = Logger.getLogger(TestRouterManager.class);

    private static final String TABLEPREFIX = "table";

    private AtomicInteger threadCount = new AtomicInteger(0);

    private static boolean localRouterHandler = false;

    private static int countInterval = 10;

    private static String userName = "tungsten";

    private static String password = "secret";

    private static Thread readers[] = null;

    private static Thread writers[] = null;

    private static boolean readOnlyWriter = false;

    private static boolean slowTest = true;

    static void printHelp() {
        println("SQL Router Test");
        println("Syntax:  [java " + TestRouterManager.class.getName() + " [options]");
        println("Options:");
        println("\t-user <text>         - user name for database connections");
        println("\t-password <text>     - password for database connections");
        println("\t-iterations <number> - number of iterations for each thread to execute");
        println("\t-threads <number>");
        println("\t-slow                - useful if you want a test that does the minimum during debugging.");
        println("Omitting a command prints router status");
    }

    /**
     * Main method to run utility.
     * 
     * @param argv optional command string
     */
    public static void main(String argv[]) {
        int iterations = 10000;
        int threadCount = 2;
        int rowCount = 10;
        countInterval = rowCount / 10;
        ArgvIterator argvIterator = new ArgvIterator(argv);
        String curArg = null;
        try {
            while (argvIterator.hasNext()) {
                curArg = argvIterator.next();
                if ("-iterations".equals(curArg)) {
                    iterations = Integer.getInteger(argvIterator.next());
                    if (iterations == 0) {
                        iterations = 100;
                    }
                } else if ("-user".equals(curArg)) {
                    userName = argvIterator.next();
                } else if ("-password".equals(curArg)) {
                    password = argvIterator.next();
                } else if ("-slow".equals(curArg)) {
                    slowTest = true;
                } else if ("-help".equals(curArg) || "-h".equals(curArg)) {
                    printHelp();
                    System.exit(0);
                } else {
                    fatal("Unrecognized global option: " + curArg);
                }
            }
        } catch (NumberFormatException e) {
            fatal("Bad numeric argument for " + curArg);
        } catch (ArrayIndexOutOfBoundsException e) {
            fatal("Missing value for " + curArg);
        }
        try {
            Class.forName("com.continuent.tungsten.router.jdbc.TSRDriver");
        } catch (Exception e) {
            println("Exception while initializing RouterManager:" + e);
            System.exit(1);
        }
        if (slowTest) {
            threadCount = 1;
            rowCount = 10;
        }
        RouterHandler handler = null;
        Thread handlerThread = null;
        if (localRouterHandler == true) {
            try {
                RouterManager.getInstance().configureRouter();
            } catch (RouterException e) {
                println("Exception while configuring RouterManager:" + e);
                return;
            }
            handler = new RouterHandler();
            handlerThread = new Thread(handler);
            handlerThread.start();
        }
        TestRouterManager test = new TestRouterManager();
        if (slowTest) {
            threadCount = 2;
            rowCount = 1;
        }
        if (threadCount < 2) {
            threadCount = 2;
        }
        readers = new Thread[threadCount / 2];
        writers = new Thread[threadCount / 2];
        for (int i = 0; i < threadCount / 2; i++) {
            if (slowTest) {
                readOnlyWriter = true;
            }
            CycleRunner writer = test.new CycleRunner(i, rowCount, iterations, test, readOnlyWriter);
            writers[i] = new Thread(writer);
            test.threadPlus();
            if (!slowTest) {
                CycleRunner reader = test.new CycleRunner(i, rowCount, iterations, test, true);
                readers[i] = new Thread(reader);
                reader.setCompanion(writer);
                test.threadPlus();
                readers[i].start();
            }
            writers[i].start();
        }
        println("[APP: WAITING FOR ALL THREADS TO EXIT]");
        test.waitForAllThreads();
        println("[APP: ALL THREADS COMPLETED. EXITING...]");
        if (localRouterHandler == true) {
            handlerThread.interrupt();
        }
        System.exit(0);
    }

    private class CycleRunner implements Runnable {

        private Logger logger = Logger.getLogger(CycleRunner.class);

        private Connection conn = null;

        private int connectionID = 0;

        private int rowCount = 0;

        private int iterations = 0;

        private boolean readOnly = false;

        private AtomicInteger ready = new AtomicInteger(0);

        private CycleRunner companion = null;

        TestRouterManager mon = null;

        public CycleRunner(int connectionID, int rowCount, int iterations, TestRouterManager mon, boolean readOnly) {
            this.connectionID = connectionID;
            this.rowCount = rowCount;
            this.iterations = iterations;
            this.mon = mon;
            this.readOnly = readOnly;
        }

        public void waitForReady() {
            synchronized (ready) {
                while (ready.get() == 0) {
                    try {
                        ready.wait();
                    } catch (InterruptedException i) {
                        return;
                    }
                }
            }
        }

        public void signalReady() {
            synchronized (ready) {
                ready.set(1);
                ready.notifyAll();
            }
        }

        public void run() {
            if (!readOnly || readOnlyWriter) {
                try {
                    setUpWriter(connectionID);
                } catch (SQLException s) {
                    mon.threadMinus();
                    return;
                }
            }
            for (int i = 0; i < iterations; i++) {
                doCycle(connectionID, rowCount);
                if (slowTest) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
            mon.threadMinus();
        }

        private void setUpWriter(int connectionId) throws SQLException {
            conn = getConnection(connectionID);
            if (conn == null) {
                throw new SQLException("Failed to get a connection for connectionID=" + connectionID);
            }
            doDropTable(conn, connectionID);
            doCreateTable(conn, connectionID);
            closeConnection(conn, connectionID);
        }

        public int doCycle(int connectionID, int rowCount) {
            int insertCount = 0;
            try {
                if (!readOnly || readOnlyWriter) {
                    conn = getConnection(connectionID);
                    signalReady();
                    for (int j = 0; j < rowCount; j++) {
                        insertCount += doInsert(conn, connectionID);
                        if (insertCount % countInterval == 0) {
                            logger.info("ID=" + connectionID + ":INSERTS=" + insertCount);
                        }
                    }
                } else {
                    if (companion != null) {
                        logger.info("ID=" + connectionID + ":WAITING FOR WRITER");
                        companion.waitForReady();
                        logger.info("ID=" + connectionID + ":WRITER READY. STARTING SELECT");
                    }
                    conn = getConnection(connectionID);
                    int selectCount = doSelect(conn, connectionID);
                    logger.info("ID=" + connectionID + ":ROWS=" + selectCount);
                }
                closeConnection(conn, connectionID);
            } catch (Exception e) {
                logger.error("Exception encountered: " + e.getLocalizedMessage());
                e.printStackTrace();
            }
            return insertCount;
        }

        public int doInsert(Connection conn, int connectionID) throws SQLException {
            Statement s = conn.createStatement();
            int count = s.executeUpdate("INSERT INTO " + getTableName(connectionID) + "(name, category)" + " VALUES" + "('snake', 'reptile')");
            return count;
        }

        public int doSelect(Connection conn, int connectionID) throws SQLException {
            int countRows = 0;
            Statement s = conn.createStatement();
            ResultSet results = s.executeQuery("SELECT * FROM " + getTableName(connectionID));
            while (results.next() && countRows < rowCount) {
                countRows++;
            }
            return rowCount;
        }

        public int doDropTable(Connection conn, int connectionID) throws SQLException {
            Statement s = conn.createStatement();
            int count = s.executeUpdate(("DROP TABLE IF EXISTS " + getTableName(connectionID)));
            return count;
        }

        public void doCreateTable(Connection conn, int connectionID) throws SQLException {
            Statement s = conn.createStatement();
            s.executeUpdate("CREATE TABLE " + getTableName(connectionID) + "(" + "id INT UNSIGNED NOT NULL AUTO_INCREMENT," + "PRIMARY KEY (id)," + "name CHAR(40), category CHAR(40))");
        }

        public Connection getConnection(int connectionID) {
            Connection conn = null;
            int retries = 0;
            while (retries++ < 5) {
                try {
                    String userName = "root";
                    String password = "davinci";
                    String url = "jdbc:t-router://default/test?qos=";
                    String qos = null;
                    if (readOnly || readOnlyWriter) {
                        qos = "RO_RELAXED";
                    } else {
                        qos = "RW_STRICT";
                    }
                    url = url + qos;
                    logger.info("[APP CONNECTIONID=" + connectionID + ":DATABASE TRY TO CONNECT] QOS=" + qos);
                    conn = DriverManager.getConnection(url, userName, password);
                    logger.info("[APP CONNECTIONID=" + connectionID + ":DATABASE CONNECTION SUCCESSFUL] QOS=" + qos);
                    return conn;
                } catch (Exception e) {
                    logger.error("Cannot connect to database server:" + e);
                    logger.info("Retrying connection in 10 seconds");
                    try {
                        Thread.sleep(10000);
                        continue;
                    } catch (Exception f) {
                        return null;
                    }
                }
            }
            return null;
        }

        /**
         * Returns the companion value.
         * 
         * @return Returns the companion.
         */
        public CycleRunner getCompanion() {
            return companion;
        }

        /**
         * Sets the companion value.
         * 
         * @param companion The companion to set.
         */
        public void setCompanion(CycleRunner companion) {
            this.companion = companion;
        }
    }

    private void threadPlus() {
        synchronized (threadCount) {
            threadCount.incrementAndGet();
            threadCount.notifyAll();
        }
    }

    private void threadMinus() {
        synchronized (threadCount) {
            threadCount.decrementAndGet();
            threadCount.notifyAll();
        }
    }

    private void waitForAllThreads() {
        int currentThreadCount = 0;
        while ((currentThreadCount = threadCount.get()) > 0) {
            try {
                synchronized (threadCount) {
                    threadCount.wait();
                    logger.info("[APP: THREADS REMAINING=" + currentThreadCount + "]");
                }
            } catch (InterruptedException e) {
                logger.error("Interrupted while waiting for threads to complete:" + e);
            }
        }
    }

    private String getTableName(int connectionID) {
        return (TABLEPREFIX + connectionID);
    }

    public void closeConnection(Connection conn, int connectionID) throws SQLException {
        conn.close();
        logger.info("[APPCONNECTIONID=" + connectionID + ":DATABASE CONNECTION END]");
    }

    private static void println(String content) {
        logger.info(content);
    }

    private static void fatal(String content) {
        logger.error(content);
        System.exit(1);
    }

    /**
     * Returns the slowTest value.
     * 
     * @return Returns the slowTest.
     */
    public static boolean isSlowTest() {
        return slowTest;
    }

    /**
     * Sets the slowTest value.
     * 
     * @param slowTest The slowTest to set.
     */
    public static void setSlowTest(boolean slowTest) {
        TestRouterManager.slowTest = slowTest;
    }

    /**
     * Returns the userName value.
     * 
     * @return Returns the userName.
     */
    public static String getUserName() {
        return userName;
    }

    /**
     * Sets the userName value.
     * 
     * @param userName The userName to set.
     */
    public static void setUserName(String userName) {
        TestRouterManager.userName = userName;
    }

    /**
     * Returns the password value.
     * 
     * @return Returns the password.
     */
    public static String getPassword() {
        return password;
    }

    /**
     * Sets the password value.
     * 
     * @param password The password to set.
     */
    public static void setPassword(String password) {
        TestRouterManager.password = password;
    }
}
