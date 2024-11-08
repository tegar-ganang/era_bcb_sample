package com.bitgate.util.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.zip.GZIPOutputStream;
import org.w3c.dom.Node;
import com.bitgate.server.Server;
import com.bitgate.util.archive.Archive;
import com.bitgate.util.debug.Debug;
import com.bitgate.util.node.NodeUtil;
import com.bitgate.util.scheduler.SchedulerInterface;

/**
 * This is rudimentary Database Pooling code; the actual code you should be using for pooling is in nuklees.util.db.  This
 * code performs a pool of local connections to only one database specified; it does not connect to mulitple separate
 * databases.  That's what the nuklees.util.db classes are for.
 *
 * @author Kenji Hollis &lt;kenji@nuklees.com&gt;
 * @version $Id: //depot/nuklees/util/db/Pool.java#65 $
 */
public class Pool {

    private ArrayList<Connection> poolThreads;

    private QueryCache queryCache;

    private String databasePoolType, databaseUser, databasePass, databaseHost, databaseName, databasePoolName;

    private int databasePort, databaseHits, poolStartSupply, databaseMax, databaseResizeTime, databaseResizeHits;

    private int currentPoolThread;

    private int default1timeout, default2timeout;

    private int rotateTimeout, rotateDays;

    private boolean rotateCompress;

    private String rotateDest, rotateArchive;

    /**
     * This is a class that watches the size of database connection pools internally, and shrinks or grows their connections
     * to a given database, based on the activity they see.
     */
    class PoolSizeWatcher implements SchedulerInterface {

        public PoolSizeWatcher() {
        }

        public int getScheduleRate() {
            if (databaseResizeTime != 0) {
                return databaseResizeTime / 10;
            }
            return 0;
        }

        public void handle() {
            String deleteStyle = "all";
            if (databaseResizeTime == 0) {
                return;
            }
            String decreaseString = NodeUtil.walkNodeTree(Server.getConfig(), "//configuration/object[@type='engine.tunable']/property[@type='engine.pooldbdecrease']/@value");
            if (decreaseString != null) {
                deleteStyle = decreaseString;
            }
            if (databaseHits >= databaseResizeHits) {
                for (int i = 0; i < (databaseHits / databaseResizeHits); i++) {
                    increasePoolSize();
                }
            }
            if (databaseHits < databaseResizeHits) {
                if (deleteStyle.equalsIgnoreCase("all")) {
                    decreasePoolToMinimum();
                } else {
                    decreasePoolSize();
                }
            }
            databaseHits = 0;
        }

        public String identString() {
            return "Pool Size Watcher for Database";
        }
    }

    /**
     * This class rotates through pools as they experience heavy or light usage.  It rotates the data from memory to the
     * filesystem, and loads those queries back into memory as they get used by the system.  This is used for database
     * query caching, which helps to speed up the system's use of database connections, and memory.
     */
    class PoolRotator implements SchedulerInterface {

        public PoolRotator() {
        }

        public int getScheduleRate() {
            if (rotateTimeout != 0) {
                return rotateTimeout / 10;
            }
            return 0;
        }

        public void handle() {
            FileChannel srcChannel, destChannel;
            String destOutFile = databaseName + ".script." + System.currentTimeMillis();
            String destOutFileCompressed = databaseName + ".script." + System.currentTimeMillis() + ".gz";
            if (rotateDest != null) {
                (new File(rotateDest)).mkdirs();
                if (destOutFile.indexOf("/") != -1) {
                    destOutFile = rotateDest + "/" + destOutFile.substring(destOutFile.lastIndexOf("/") + 1);
                }
                if (destOutFileCompressed.indexOf("/") != -1) {
                    destOutFileCompressed = rotateDest + "/" + destOutFileCompressed.substring(destOutFileCompressed.lastIndexOf("/") + 1);
                }
            }
            if (rotateCompress) {
                try {
                    GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(destOutFileCompressed));
                    FileInputStream in = new FileInputStream(databaseName + ".script");
                    byte buf[] = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    in.close();
                    out.finish();
                    out.close();
                    buf = null;
                    in = null;
                    out = null;
                    Debug.debug("Rotated database file '" + databaseName + ".script' to '" + destOutFileCompressed + "'");
                } catch (Exception e) {
                    Debug.debug("Unable to rotate database file '" + databaseName + ".script': " + e);
                }
            } else {
                try {
                    srcChannel = new FileInputStream(databaseName + ".script").getChannel();
                } catch (IOException e) {
                    Debug.debug("Unable to read file '" + databaseName + ".script' for database rotation.");
                    return;
                }
                try {
                    destChannel = new FileOutputStream(destOutFile).getChannel();
                } catch (IOException e) {
                    Debug.debug("Unable to rotate file to '" + destOutFile + "': " + e.getMessage());
                    return;
                }
                try {
                    destChannel.transferFrom(srcChannel, 0, srcChannel.size());
                    srcChannel.close();
                    destChannel.close();
                    srcChannel = null;
                    destChannel = null;
                } catch (IOException e) {
                    Debug.debug("Unable to copy data for file rotation: " + e.getMessage());
                    return;
                }
                Debug.debug("Rotated database file '" + databaseName + ".script' to '" + destOutFile + "'");
            }
            if (rotateDest != null) {
                long comparisonTime = rotateDays * (60 * 60 * 24 * 1000);
                long currentTime = System.currentTimeMillis();
                File fileList[] = (new File(rotateDest)).listFiles();
                DateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
                java.util.Date date = new java.util.Date(currentTime);
                String archiveFile = format1.format(date).toString() + ".zip";
                if (rotateArchive != null) {
                    archiveFile = rotateArchive + "/" + archiveFile;
                    (new File(rotateArchive)).mkdirs();
                }
                Archive archive = new Archive(archiveFile);
                for (int i = 0; i < fileList.length; i++) {
                    String currentFilename = fileList[i].getName();
                    long timeDifference = (currentTime - fileList[i].lastModified());
                    if ((rotateCompress && currentFilename.endsWith(".gz")) || (!rotateCompress && currentFilename.indexOf(".script.") != -1)) {
                        if (rotateDest != null) {
                            currentFilename = rotateDest + "/" + currentFilename;
                        }
                        if (timeDifference > comparisonTime) {
                            archive.addFile(fileList[i].getName(), currentFilename);
                            fileList[i].delete();
                        }
                    }
                }
                archive = null;
                fileList = null;
                format1 = null;
                date = null;
            }
        }

        public String identString() {
            return "Pool Rotator for '" + databaseName + "'";
        }
    }

    /**
     * Connects to the database, starting a specific number of pools.
     *
     * @param startPoolNum Starting pool number, based on the properties file.
     */
    public Pool(int startPoolNum) {
        databaseHits = 0;
        currentPoolThread = 0;
        default1timeout = 0;
        default2timeout = 0;
        rotateTimeout = 0;
        setPool(startPoolNum, null);
        queryCache = new QueryCache(this);
        Server.getScheduler().register("Pool Size Watcher for '" + databaseName + "'", new PoolSizeWatcher());
    }

    /**
     * Connects to the database, starting a specific number of pools.
     *
     * @param startPoolNum Starting pool number, based on the properties file.
     * @param node The current configuration node.
     */
    public Pool(int startPoolNum, Node node) {
        databaseHits = 0;
        currentPoolThread = 0;
        default1timeout = 0;
        default2timeout = 0;
        rotateTimeout = 0;
        setPool(startPoolNum, node);
        queryCache = new QueryCache(this);
        String poolName = NodeUtil.walkNodeTree(node, "property[@type='engine.name']/@value");
        Server.getScheduler().register("Pool Size Watcher for '" + databaseName + "' (" + poolName + ")", new PoolSizeWatcher());
    }

    /**
     * Connects to the database, starting a specific number of pools.
     *
     * @param startName The starting name of the property to base the lookups from.
     */
    public Pool(String startName) {
        databaseHits = 0;
        currentPoolThread = 0;
        default1timeout = 0;
        default2timeout = 0;
        rotateTimeout = 0;
        queryCache = new QueryCache(this);
        setPool(startName, null);
        Server.getScheduler().register("Pool Size Watcher for '" + databaseName + "'", new PoolSizeWatcher());
    }

    /**
     * Connects to the database, starting a specific number of pools, with known information.
     *
     * @param pool The pool name.
     * @param type The type of database.
     * @param user The username to connect with.
     * @param pass The password to connect with.
     * @param database The database to connect to.
     * @param host The host to connect to.
     * @param port The port to connect to.
     * @param start The number of start entries.
     */
    public Pool(String pool, String type, String user, String pass, String database, String host, int port, int start) {
        databaseHits = 0;
        currentPoolThread = 0;
        default1timeout = 0;
        default2timeout = 0;
        rotateTimeout = 0;
        setPool(pool, type, user, pass, database, host, port, start);
        queryCache = new QueryCache(this);
        Server.getScheduler().register("Pool Size Watcher for '" + pool + "'", new PoolSizeWatcher());
    }

    /**
     * Connects to the database, starting a specific number of pools.
     *
     * @param startName The starting name of the property to base the lookups from.
     * @param node The current configuration node.
     */
    public Pool(String startName, Node node) {
        databaseHits = 0;
        currentPoolThread = 0;
        default1timeout = 0;
        default2timeout = 0;
        rotateTimeout = 0;
        setPool(startName, node);
        String poolName = NodeUtil.walkNodeTree(node, "property[@type='engine.name']/@value");
        Server.getScheduler().register("Pool Size Watcher for '" + databaseName + "' (" + poolName + ")", new PoolSizeWatcher());
    }

    private void setPool(int startNum, Node node) {
        String poolname = null;
        if (node != null) {
            poolname = NodeUtil.walkNodeTree(node, "property[@type='engine.name']/@value");
        } else {
            poolname = NodeUtil.walkNodeTree(Server.getConfig(), "//configuration/object[@type='engine.dbpool'][" + startNum + "]/property[@type='engine.name']/@value");
        }
        setPool(poolname, node);
    }

    private void setPool(String property, Node node) {
        databasePoolName = property;
        Node configNode = null;
        if (node == null) {
            configNode = NodeUtil.selectNode(Server.getConfig(), "//configuration/object[@type='engine.dbpool']/property[@type='engine.name'][@value='" + property + "']/..");
        } else {
            configNode = node;
        }
        String stringStart = NodeUtil.walkNodeTree(configNode, "property[@type='engine.start']/@value");
        if (stringStart == null) {
            poolStartSupply = 5;
        } else {
            poolStartSupply = Integer.parseInt(stringStart);
        }
        databasePoolType = NodeUtil.walkNodeTree(configNode, "property[@type='engine.type']/@value");
        databaseUser = NodeUtil.walkNodeTree(configNode, "property[@type='engine.username']/@value");
        databasePass = NodeUtil.walkNodeTree(configNode, "property[@type='engine.password']/@value");
        databaseHost = NodeUtil.walkNodeTree(configNode, "property[@type='engine.host']/@value");
        if (databasePoolType == null || databasePoolType.equals("")) {
            Debug.warn("No database pool type specified for pool name '" + property + "'");
            return;
        }
        String stringPort = NodeUtil.walkNodeTree(configNode, "property[@type='engine.port']/@value");
        if (stringPort == null) {
            poolThreads = null;
            return;
        }
        databasePort = Integer.parseInt(stringPort);
        databaseName = NodeUtil.walkNodeTree(configNode, "property[@type='engine.database']/@value");
        String stringMax = NodeUtil.walkNodeTree(configNode, "property[@type='engine.max']/@value");
        if (stringMax == null) {
            databaseMax = (poolStartSupply * 2);
        } else {
            databaseMax = Integer.parseInt(stringMax);
        }
        String resizeTime = NodeUtil.walkNodeTree(configNode, "property[@type='engine.resizetime']/@value");
        String resizeHits = NodeUtil.walkNodeTree(configNode, "property[@type='engine.resizehits']/@value");
        if (resizeTime != null) {
            databaseResizeTime = Integer.parseInt(resizeTime);
        }
        if (resizeHits != null) {
            databaseResizeHits = Integer.parseInt(resizeHits);
        }
        PoolList.getDefault().add(databasePoolName);
    }

    private void setPool(String pool, String type, String user, String pass, String database, String host, int port, int start) {
        databasePoolName = pool;
        poolStartSupply = start;
        databasePoolType = type;
        databaseUser = user;
        databasePass = pass;
        databaseHost = host;
        databasePort = port;
        databaseName = database;
        databaseMax = (poolStartSupply * 2);
        databaseResizeTime = 0;
        databaseResizeHits = 0;
        PoolList.getDefault().add(databasePoolName);
    }

    /**
     * This function is used to set the Database Pool name; it is particularly used for Informix, and no where else.
     *
     * @param poolName The pool name to assign to this connection.
     */
    public void setPoolName(String poolName) {
        databasePoolName = poolName;
        PoolList.getDefault().add(databasePoolName);
    }

    /**
     * This function allows access to the query cache.
     *
     * @return QueryCache object.
     */
    public QueryCache getQueryCache() {
        return queryCache;
    }

    /**
     * This sets the first movement timeout for the SQL element.
     *
     * @param timeout The timeout to set.
     */
    public void setTimeout1(int timeout) {
        default1timeout = timeout;
    }

    /**
     * This sets the second movement timeout for the SQL element.
     *
     * @param timeout The timeout to set.
     */
    public void setTimeout2(int timeout) {
        default2timeout = timeout;
    }

    /**
     * This retrieves the first movement timeout.
     *
     * @return <code>int</code> containing the timeout value.
     */
    public int getTimeout1() {
        return default1timeout;
    }

    /**
     * This retrieves the second movement timeout.
     *
     * @return <code>int</code> containing the timeout value.
     */
    public int getTimeout2() {
        return default2timeout;
    }

    /**
     * This sets the rotate timeout on the log files (only valid if the file is internal, or the database somehow supports it.)
     *
     * @param timeout The timeout to set.
     */
    public void setRotateLogs(int timeout) {
        rotateTimeout = timeout;
        if (!databasePoolType.equalsIgnoreCase("hsql")) {
            return;
        }
        Debug.debug("Setting database '" + databaseName + "' type '" + databasePoolType + "' to '" + timeout + "'");
        Server.getScheduler().register("Pool Rotator for '" + databaseName + "'", new PoolRotator());
    }

    /**
     * Sets the maximum number of days the rotation is to take place for.
     *
     * @param days The number of days to rotate for.
     */
    public void setRotateDays(int days) {
        rotateDays = days;
    }

    /**
     * Indicates to the system whether or not to compress the output files that the system creates.
     *
     * @param compress <code>true</code> to compress, <code>false</code> otherwise.
     */
    public void setRotateCompress(boolean compress) {
        rotateCompress = compress;
    }

    /**
     * Sets the destination directory for the rotation files to be written to.
     *
     * @param dest The destination directory to write to.
     */
    public void setRotateDest(String dest) {
        rotateDest = dest;
    }

    /**
     * Sets the rotate archival directory.
     *
     * @param archive The archival directory name.
     */
    public void setRotateArchive(String archive) {
        rotateArchive = archive;
    }

    /**
     * This function starts the pool.
     */
    public void startPool() {
        String databaseConnectString;
        if (PoolTable.getDefault().getFailure(databasePoolName)) {
            return;
        }
        Debug.banner("--- Starting " + poolStartSupply + " database connection(s) for '" + databasePoolName + "' [Type='" + databasePoolType + "' Host='" + databaseHost + ":" + databasePort + "']");
        if (poolStartSupply == 0) {
            Debug.warn("Start supply for '" + databasePoolName + "' set to 0 database entries: You should probably set " + "this at least to 1 or higher.");
        }
        poolThreads = new ArrayList<Connection>();
        databaseConnectString = DBDriver.createConnectionString(databasePoolName, databasePoolType, databaseHost, databasePort, databaseUser, databasePass, databaseName);
        if (databaseConnectString == null) {
            Debug.crit("Unsupported database type '" + databasePoolType + "', or other error.");
            Debug.crit("Could not create a valid JDBC connection string based on your current settings!");
            poolThreads = null;
            PoolTable.getDefault().addFailure(databasePoolName);
            return;
        }
        String driverClass = DBDriver.driverClassName(databasePoolType);
        if (driverClass == null) {
            Debug.crit("Unsupported database type '" + databasePoolType + "'.  Please refer to documentation.");
            Debug.crit("Could not create a valid JDBC connection string based on your current settings!");
            poolThreads = null;
            PoolTable.getDefault().addFailure(databasePoolName);
            return;
        }
        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException e) {
            Debug.crit("Database drivers for database type '" + databasePoolType + "' unavailable: " + e.getMessage());
            PoolTable.getDefault().addFailure(databasePoolName);
            return;
        }
        int successfulPool = 0;
        if (poolStartSupply == 0) {
            successfulPool++;
        }
        for (int i = 0; i < poolStartSupply; i++) {
            Connection conn = null;
            boolean canAdd = true;
            try {
                DriverManager.registerDriver(DriverManager.getDriver(databaseConnectString));
                conn = DriverManager.getConnection(databaseConnectString, databaseUser, databasePass);
            } catch (SQLException e) {
                Debug.crit("SQL Connection Error: Unable to start pool for driver='" + databasePoolType + "' with connect='" + databaseConnectString + "' user='" + databaseUser + "' errorcode='" + e.getErrorCode() + "': " + e.getMessage());
                canAdd = false;
                PoolTable.getDefault().addFailure(databasePoolName);
            } catch (Exception e) {
                Debug.crit("Unable to start pool for driver '" + databasePoolType + "' with connect='" + databaseConnectString + "' user='" + databaseUser + "': Refer to logs for details: " + e);
                canAdd = false;
                PoolTable.getDefault().addFailure(databasePoolName);
            }
            if (conn != null) {
                try {
                    if (conn.isClosed()) {
                        Debug.crit("--- Connection to database pool '" + databasePoolName + "' failed: Closed connection.");
                    }
                } catch (SQLException e) {
                    Debug.crit("--- Connection to database pool '" + databasePoolName + "' failed: errorcode='" + e.getErrorCode() + "': Refer to logs for details.");
                }
            }
            if (canAdd) {
                poolThreads.add(conn);
                successfulPool++;
            }
        }
    }

    /**
     * This function shuts down all pooled database connections.
     */
    public void shutdown() {
        if (poolThreads == null) {
            return;
        }
        int poolThreadsSize = poolThreads.size();
        for (int i = 0; i < poolThreadsSize; i++) {
            Connection conn = (Connection) poolThreads.get(i);
            try {
                conn.close();
            } catch (SQLException ex) {
            }
        }
        Debug.debug("Pool: Pool connections shutdown for pool '" + databasePoolName + "'");
    }

    /**
     * This function increases a pool to an already existing pool.  This will only increase the pool size by one.
     */
    public void increasePoolSize() {
        if (poolThreads.size() >= databaseMax) {
            return;
        }
        String databaseConnectString;
        databaseConnectString = DBDriver.createConnectionString(databasePoolName, databasePoolType, databaseHost, databasePort, databaseUser, databasePass, databaseName);
        if (databaseConnectString == null) {
            Debug.crit("Unsupported database type '" + databasePoolType + "', or other error.");
            Debug.crit("Could not create a valid JDBC connection string based on your current settings!");
            poolThreads = null;
            return;
        }
        Connection conn = null;
        boolean canAdd = true;
        try {
            DriverManager.registerDriver(DriverManager.getDriver(databaseConnectString));
            conn = DriverManager.getConnection(databaseConnectString, databaseUser, databasePass);
        } catch (SQLException e) {
            Debug.crit("Unable to start pool for driver: errorcode='" + e.getErrorCode() + "': " + e.getMessage());
            canAdd = false;
        } catch (Exception e) {
            Debug.crit("Unable to start pool for driver: " + e.getMessage());
            canAdd = false;
        }
        if (canAdd) {
            poolThreads.add(conn);
        }
    }

    /**
     * This code attempts to re-establish a connection with a database.  It does not always work necessarily, as to test if
     * a class has a connection, you actually have to perform a command.  This code is here to re-establish connections with
     * SQL servers that actually correctly disconnect when no activity has happened in a specific amount of time.
     *
     * @param position The position which has failed, and must be reestablished to the pool.
     */
    public void reestablishConnection(int position) {
        String databaseConnectString;
        databaseConnectString = DBDriver.createConnectionString(databasePoolName, databasePoolType, databaseHost, databasePort, databaseUser, databasePass, databaseName);
        if (databaseConnectString == null) {
            Debug.crit("Unsupported database type '" + databasePoolType + "', or other error.");
            Debug.crit("Could not create a valid JDBC connection string based on your current settings!");
            poolThreads = null;
            return;
        }
        Connection conn = null;
        boolean canAdd = true;
        for (int tries = 0; tries < 5; tries++) {
            try {
                DriverManager.registerDriver(DriverManager.getDriver(databaseConnectString));
                conn = DriverManager.getConnection(databaseConnectString, databaseUser, databasePass);
            } catch (SQLException e) {
                Debug.crit("Unable to start pool for driver: errorcode='" + e.getErrorCode() + "': " + e.getMessage());
                canAdd = false;
            } catch (Exception e) {
                Debug.crit("Unable to start pool for driver: " + e.getMessage());
                canAdd = false;
            }
            if (conn != null) {
                try {
                    if (conn.isClosed()) {
                        Debug.warn("Pool: Cannot reestablish connection to database pool '" + databasePoolName + "': Closed connection.");
                        canAdd = true;
                    }
                } catch (SQLException e) {
                    canAdd = true;
                }
            }
            try {
                if (conn != null && !conn.isClosed()) {
                    Debug.debug("Pool: Reestablishment of connection to '" + databasePoolName + "' successful.");
                    break;
                }
            } catch (SQLException e) {
            }
        }
        try {
            if (conn == null || (conn != null && conn.isClosed())) {
                Debug.crit("Unable to reestablish database connection for pool '" + databasePoolName + "'");
            }
        } catch (SQLException e) {
            Debug.debug("Unable to determine if connection is closed: errorcode='" + e.getErrorCode() + "': " + e.getMessage());
            return;
        }
        if (canAdd) {
            poolThreads.set(position, conn);
        }
    }

    /**
     * This function decreases the number of available database thread pools to the minimum start number.
     */
    public void decreasePoolToMinimum() {
        if (poolThreads == null) {
            return;
        }
        if (poolThreads.size() <= poolStartSupply) {
            return;
        }
        synchronized (poolThreads) {
            int poolThreadsSize = 0;
            while ((poolThreadsSize = poolThreads.size()) > poolStartSupply) {
                Connection conn = (Connection) poolThreads.remove(poolThreadsSize - 1);
                try {
                    conn.close();
                } catch (SQLException ex) {
                }
                conn = null;
            }
        }
    }

    /**
     * This function decreases the number of available database thread pools to the minimum start number.
     */
    public void decreasePoolSize() {
        if (poolThreads == null) {
            return;
        }
        if (poolThreads.size() <= poolStartSupply) {
            return;
        }
        synchronized (poolThreads) {
            Connection conn = (Connection) poolThreads.remove(poolThreads.size() - 1);
            try {
                conn.close();
            } catch (SQLException ex) {
            }
            conn = null;
        }
    }

    /**
     * This function returns the next available SQL database connection.
     *
     * @return Connection class containing the next available connection.
     */
    public Connection getNextConnection() {
        if (poolThreads == null) {
            return null;
        }
        if (poolThreads.size() == 0) {
            increasePoolSize();
        }
        databaseHits++;
        currentPoolThread++;
        if (currentPoolThread >= poolThreads.size()) {
            currentPoolThread = 0;
        }
        if (poolThreads.size() == 0 || poolThreads.get(currentPoolThread) == null) {
            currentPoolThread = 0;
            increasePoolSize();
        }
        if (poolThreads.size() == 0) {
            return null;
        }
        Connection thrConn = (Connection) poolThreads.get(currentPoolThread);
        try {
            if (thrConn.isClosed()) {
                reestablishConnection(currentPoolThread);
            }
        } catch (Exception e) {
        }
        return (Connection) poolThreads.get(currentPoolThread);
    }

    /**
     * This function closes all open SQL connections.
     */
    public void closeAllConnections() {
        if (poolThreads == null) {
            return;
        }
        if (poolThreads.size() <= poolStartSupply) {
            return;
        }
        int numCloses = 0;
        synchronized (poolThreads) {
            while (poolThreads.size() > 0) {
                Connection conn = (Connection) poolThreads.remove(poolThreads.size() - 1);
                try {
                    conn.close();
                    numCloses++;
                } catch (SQLException ex) {
                }
                conn = null;
            }
        }
        if (numCloses > 0) {
            Debug.inform("Pool closed " + numCloses + " connection(s).");
        }
    }

    /**
     * Retrieves the name of the current pool.
     *
     * @return <code>String</code> containing the pool name.
     */
    public String getPoolName() {
        return databasePoolName;
    }

    /**
     * Returns the currently active number of connections.
     *
     * @return <code>int</code> containing the number of active connections.
     */
    public int getNumActiveConnections() {
        return poolThreads.size();
    }

    /**
     * Returns the current type of pool.
     *
     * @return <code>String</code> containing the current pool type.
     */
    public String getPoolType() {
        return databasePoolType;
    }

    /**
     * Returns the username connected to the pool.
     *
     * @return <code>String</code> containing the pool username.
     */
    public String getPoolUser() {
        return databaseUser;
    }

    /**
     * Returns the password of the connected pool.
     *
     * @return <code>String</code> containing the password for the connected pool.
     */
    public String getPoolPass() {
        return databasePass;
    }

    /**
     * Returns the hostname that the pool is connected to.
     *
     * @return <code>String</code> of the host.
     */
    public String getPoolHost() {
        return databaseHost;
    }

    /**
     * Returns the name of the database name this pool is connected to.
     *
     * @return <code>String</code> containing the database name.
     */
    public String getPoolDatabaseName() {
        return databaseName;
    }

    /**
     * Returns the port to which the pool database connection is connected to.
     *
     * @return <code>int</code> containing the pool port number.
     */
    public int getPoolPort() {
        return databasePort;
    }

    /**
     * Returns the number of hits the pool has received.
     *
     * @return <code>int</code> containing the number of current hits.
     */
    public int getPoolHits() {
        return databaseHits;
    }

    /**
     * Returns the start supply amount for this pool.
     *
     * @return <code>int</code> containing the number of starting connections.
     */
    public int getPoolStartSupply() {
        return poolStartSupply;
    }

    /**
     * Returns the number of maximum connections to a database this pool can grow to.
     *
     * @return <code>int</code> containing the number of maximum connections allowed.
     */
    public int getPoolMax() {
        return databaseMax;
    }

    /**
     * Returns the time in seconds that this system will resize when a certain number of <code>Hits</code> are retrieved.
     *
     * @return <code>int</code> containing the resize time in seconds.
     */
    public int getPoolResizeTime() {
        return databaseResizeTime;
    }

    /**
     * Returns the number of hits the pool must receive before it increases in size automatically.
     *
     * @return <code>int</code> containing the number of hits.
     */
    public int getPoolResizeHits() {
        return databaseResizeHits;
    }
}
