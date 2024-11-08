package org.continuent.myosotis.configuration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.log4j.Logger;

/**
 * Configuration file handling.
 * 
 * @author <a href="mailto:csaba.simon@continuent.com">Csaba Simon</a>
 * @author <a href="mailto:gilles.rayrat@continuent.com">Gilles Rayrat</a>
 */
public class Configuration {

    private static final Logger logger = Logger.getLogger(Configuration.class);

    public static final int MYSQL_PROTOCOL_TYPE = 0;

    public static final int POSTGRESQL_PROTOCOL_TYPE = 1;

    /**
     * Server protocol: MYSQL_PROTOCOL_TYPE or POSTGRESQL_PROTOCOL_TYPE
     */
    private int serverProtocol = MYSQL_PROTOCOL_TYPE;

    /**
     * Server version. </p> MySQL Connector/J is checking this version string.
     * Simulate a MySQL server.
     */
    private String serverVersion;

    /**
     * Server port for listening. Default: 9999.
     */
    private int serverPort;

    /**
     * Address onto which to listen for incoming connections. Null value means
     * "listen to any/all address
     */
    private String listenAddress;

    /**
     * The JDBC driver.
     */
    private Driver driver;

    /**
     * Driver for direct connections
     */
    private Driver directDriver;

    /**
     * The JDBC base URL. Default is: jdbc:sequoia://
     */
    private String jdbcDriverBaseURL;

    /**
     * The JDBC driver options. It must begin with the "?" character.
     */
    private String jdbcDriverOptions;

    /**
     * The user map.
     */
    private UserMap userMap;

    /**
     * Close the connection after was idle for the specified period (in
     * miliseconds) A value of zero is interpreted as an infinite timeout.
     */
    private int connectionCloseIdleTimeout;

    /**
     * Gives the JDBC driver a hint as to the number of rows that should be
     * fetched from the database when more rows are needed. If the value
     * specified is zero, then the hint is ignored.
     */
    private int fetchSize;

    /**
     * Format for Timestamp and Datetime output. Default is "yyyy-MM-dd
     * HH:mm:ss". See {@link java.text.SimpleDateFormat} for a full description
     */
    private String timestampFormat;

    /**
     * (MySQL only) How to mirror MySQL connector's zeroDataTimeBehavior. One
     * of:
     * <ul>
     * <li>"convertToNull"
     * <li>"round"
     * <li>"exception"
     * <li>"no" (default)
     * </ul>
     * Note that any other value will be consider as "no" See
     * {@link #getMirrorZeroDateTimeBehavior()} for behavior info.
     */
    private String mirrorZeroDateTimeBehavior;

    public static final String ZERO_DATETIME_BEHAVIOR_CONVERT_TO_NULL = "convertToNull";

    public static final String ZERO_DATETIME_BEHAVIOR_ROUND = "round";

    public static final String ZERO_DATETIME_BEHAVIOR_EXCEPTION = "exception";

    public static final String FORCE_DB_DEFAULT = "mysql";

    public static final String FORCE_DB_NONE = "none";

    /**
     * (MySQL only) Which database to connect automatically to if none is
     * specified at connection time
     */
    private String forcedDBforUnspecConnections = FORCE_DB_DEFAULT;

    /** Whether comments should be removed from input strings */
    private boolean ignoreSQLComments;

    /**
     * When true, myosotis will turn transaction related SQL (begin, commit,
     * ...) into JDBC calls instead of sending them to the cluster
     */
    private boolean manageTransactionsLocally;

    /** Configuration file name */
    private String configurationFilename;

    /** Maximum number of consecutive client connection failures */
    private int maxConsecutiveClientConnectionFailures;

    private String jdbcDirectDriverClassname;

    private String authorizedHostsFileName = null;

    /**
     * List of hosts allowed to connect to us - null means any host, not null
     * means only these ones
     */
    private List<String> authorizedHosts = null;

    /** Marker that starts a tungsten command */
    private String tungstenCommandBeginMarker = "-- TUNGSTEN:";

    /**
     * Marker that ends a tungsten command - empty means "command until the end
     * of the line"
     */
    private String tungstenCommandEndMarker = "";

    private boolean selectiveRwSplitting = false;

    private String selectiveRwSplittingMarker = "TUNGSTEN_URL_PROPERTY";

    /**
     * Flag to control streaming of result sets from server to client
     * application
     */
    private boolean passThroughMode;

    /** Read/Write with session consistency */
    private boolean useSmartScale = false;

    /** Read/Write splitting mode */
    private RWSplittingMode rwSplittingMode = RWSplittingMode.SAFE;

    /**
     * Myosotis can reconnect lost connections automatically when this flag is
     * on
     */
    private boolean autoReconnect = true;

    /**
     * Read/Write Splitting Mode:
     * <ul>
     * <li>SAFE will route only selects that don't contain "for update" string
     * <li>TEST logs a "fatal" message if a select for update is found
     * <li>DONT_CHECK_SELECT_FOR_UPDATE routes all selects to the read/only
     * connections (even if they contain "for update" which, if any, will lead
     * to data inconsistencies)
     * </ul>
     */
    public enum RWSplittingMode {

        SAFE, TEST, DONT_CHECK_SELECT_FOR_UPDATE
    }

    /**
     * Constructor. Loads the configuration file, the JDBC driver and the user
     * map file.
     * 
     * @param configurationFilename the configuration file name
     * @throws IOException if file cannot be opened
     * @throws ClassNotFoundException if the JDBC class cannot be found
     * @throws IllegalAccessException if the class or its nullary constructor is
     *             not accessible
     * @throws InstantiationException if the instantiation fails for some reason
     */
    public Configuration(String configurationFilename) throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        this.configurationFilename = configurationFilename;
        FileInputStream fis = new FileInputStream(new File(configurationFilename));
        Properties prop = new Properties();
        prop.load(fis);
        String protocol = prop.getProperty("server.protocol", "mysql").trim().toLowerCase();
        if ("mysql".equals(protocol)) {
            serverProtocol = MYSQL_PROTOCOL_TYPE;
        } else if ("postgresql".equals(protocol)) {
            serverProtocol = POSTGRESQL_PROTOCOL_TYPE;
        }
        serverVersion = prop.getProperty("server.version", "4.1.1-myosotis-0.0.0").trim();
        serverPort = Integer.parseInt(prop.getProperty("server.port", "9999").trim());
        setListenAddress(prop.getProperty("server.listen.address", null));
        jdbcDriverBaseURL = prop.getProperty("jdbc.driver.base.url", "jdbc:sequoia://").trim();
        setJdbcDriverOptions(prop.getProperty("jdbc.driver.options", "").trim());
        connectionCloseIdleTimeout = Integer.parseInt(prop.getProperty("connection.close.idle.timeout", "0").trim());
        fetchSize = Integer.parseInt(prop.getProperty("statement.fetch.size", "0").trim());
        timestampFormat = prop.getProperty("timestamp.format", "yyyy-MM-dd HH:mm:ss").trim();
        if ("".equals(timestampFormat)) timestampFormat = "yyyy-MM-dd HH:mm:ss";
        mirrorZeroDateTimeBehavior = prop.getProperty("mirrorZeroDateTimeBehavior", "no");
        forcedDBforUnspecConnections = prop.getProperty("forcedDBforUnspecConnections", FORCE_DB_DEFAULT).trim();
        ignoreSQLComments = Boolean.parseBoolean(prop.getProperty("ignoreSQLComments", "true").trim());
        manageTransactionsLocally = Boolean.parseBoolean(prop.getProperty("manageTransactionsLocally", "false").trim());
        maxConsecutiveClientConnectionFailures = Integer.parseInt(prop.getProperty("maxConsecutiveClientConnectionFailures", "10"));
        setPassThroughMode(Boolean.parseBoolean(prop.getProperty("passThroughMode", "true").trim()));
        String jdbcDriverClassname = prop.getProperty("jdbc.driver", "org.continuent.sequoia.driver.Driver").trim();
        try {
            driver = (Driver) Class.forName(jdbcDriverClassname).newInstance();
        } catch (ClassNotFoundException e) {
            logger.error("Could not find class " + jdbcDriverClassname + ". Make sure the appropriate driver is in the lib/ directory");
            throw e;
        }
        this.jdbcDirectDriverClassname = prop.getProperty("jdbc.direct-connection.driver", "").trim();
        if (!"".equals(this.jdbcDirectDriverClassname)) {
            try {
                Class.forName(jdbcDirectDriverClassname);
            } catch (ClassNotFoundException e) {
                logger.error("Could not find class " + jdbcDirectDriverClassname + ". Make sure the appropriate driver is in the lib/ directory");
                throw e;
            }
        }
        String userMapFilename = prop.getProperty("user.map.filename", "../conf/user.map").trim();
        userMap = new UserMap(userMapFilename);
        userMap.readConfig();
        authorizedHostsFileName = prop.getProperty("authorized.hosts.file", "../conf/authorized_hosts");
        BufferedReader input = null;
        try {
            input = new BufferedReader(new FileReader(authorizedHostsFileName));
            authorizedHosts = new ArrayList<String>();
            String line = null;
            while ((line = input.readLine()) != null) {
                line = line.trim();
                if (!line.startsWith("#")) authorizedHosts.add(line);
            }
        } catch (IOException e) {
            logger.warn("No or unreadable authorized hosts file " + authorizedHostsFileName + " - NOT using IP authentication");
            authorizedHosts = null;
        } finally {
            if (input != null) try {
                input.close();
            } catch (IOException ignored) {
            }
        }
        tungstenCommandBeginMarker = prop.getProperty("tungsten.command.begin.marker", tungstenCommandBeginMarker).trim();
        tungstenCommandEndMarker = prop.getProperty("tungsten.command.end.marker", tungstenCommandEndMarker).trim();
        selectiveRwSplittingMarker = prop.getProperty("selective.rwsplitting.marker", selectiveRwSplittingMarker).trim();
        selectiveRwSplitting = Boolean.valueOf(prop.getProperty("selective.rwsplitting", "false"));
        if (selectiveRwSplitting == true) {
            logger.info(String.format("Using SELECTIVE read/write splitting, using marker=%s", selectiveRwSplittingMarker));
        }
        setUseSmartScale(Boolean.valueOf(prop.getProperty("useSmartScale", "false")));
        setAutoReconnect(Boolean.valueOf(prop.getProperty("autoReconnect", "true")));
    }

    /**
     * Getter method for the protocol type.
     * 
     * @return protocol type.
     */
    public int getServerProtocol() {
        return serverProtocol;
    }

    /**
     * Getter method for the server version.
     * 
     * @return server version string.
     */
    public String getServerVersion() {
        return serverVersion;
    }

    /**
     * Getter method for the server port.
     * 
     * @return server port number.
     */
    public int getServerPort() {
        return serverPort;
    }

    /**
     * Defines an address on which to listen to for incoming connections. Empty
     * string, "any" or "*" will be treated as "listen to any/all addresses"
     * 
     * @param addr bind address, can either be a machine name or a textual
     *            representation of its IP address
     */
    public void setListenAddress(String addr) {
        if (addr != null) addr = addr.trim().toLowerCase();
        if (addr != null && addr.length() > 0 && !addr.equals("any") && !addr.equals("*")) {
            listenAddress = addr;
            if (logger.isInfoEnabled()) logger.info("Listen address set to: " + addr);
        } else listenAddress = null;
    }

    /**
     * Retrieves the configured listen address, or null if no address was
     * specified
     * 
     * @return the address to listen to for incoming connection, or null for
     *         listening to any/all address(es)
     */
    public String getListenAddress() {
        return listenAddress;
    }

    /**
     * Getter method for the JDBC driver.
     * 
     * @return JDBC driver.
     */
    public Driver getDriver() {
        return driver;
    }

    /**
     * Getter method for the direct connection JDBC driver.
     * 
     * @return JDBC driver for direct connections.
     */
    public Driver getDirectDriver() {
        return directDriver;
    }

    /**
     * Getter method for the JDBC driver base URL.
     * 
     * @return JDBC driver base URL.
     */
    public String getJdbcDriverBaseURL() {
        return jdbcDriverBaseURL;
    }

    /**
     * Getter method for the JDBC driver options.
     * 
     * @return JDBC driver options.
     */
    public String getJdbcDriverOptions() {
        return jdbcDriverOptions;
    }

    /**
     * Applies the given JDBC driver options (main connection only, ie. NOT for
     * direct connections). If the heading question mark is omitted, this
     * function will add it
     * 
     * @param jdbcDriverOptions options to pass to the driver
     */
    public void setJdbcDriverOptions(String jdbcDriverOptions) {
        if (jdbcDriverOptions == null) {
            this.jdbcDriverOptions = "";
        } else if (jdbcDriverOptions.length() > 0 && !jdbcDriverOptions.startsWith("?")) {
            this.jdbcDriverOptions = "?" + jdbcDriverOptions;
        } else {
            this.jdbcDriverOptions = jdbcDriverOptions;
        }
    }

    /**
     * Getter method for the user map.
     * 
     * @return user map.
     */
    public UserMap getUserMap() {
        return userMap;
    }

    /**
     * Getter method for connectionCloseIdleTimeout option.
     * 
     * @return Returns the connectionCloseIdleTimeout.
     */
    public int getConnectionCloseIdleTimeout() {
        return connectionCloseIdleTimeout;
    }

    /**
     * Getter method for fetchSize option.
     * 
     * @return Returns the fetchSize.
     */
    public int getFetchSize() {
        return fetchSize;
    }

    /**
     * Getter method for the timestampFormat option.
     * 
     * @return the format timestamp should use
     */
    public String getTimestampFormat() {
        return timestampFormat;
    }

    /**
     * Tells how to mirror MySQL zeroDateTimeBehavior option.
     * 
     * @return "convertToNull" if null timestamps and dates should be converted
     *         to zeros, "round" if MySQL rounded values of zero dates and
     *         timestamps should be converted to zeros, "exception" if
     *         SQLExceptions with state "S1009" (SQL_STATE_ILLEGAL_ARGUMENT)
     *         should be converted to zeros "no" or any other string for no
     *         operations on zeroDateTime
     */
    public String getMirrorZeroDateTimeBehavior() {
        return mirrorZeroDateTimeBehavior;
    }

    /**
     * Tells whether we should mirror convertToNull zero datetime behavior
     * 
     * @return true if null timestamps and dates should be converted to zeros
     */
    public boolean getMirrorZeroDateTimeConvertToNull() {
        return ZERO_DATETIME_BEHAVIOR_CONVERT_TO_NULL.equals(getMirrorZeroDateTimeBehavior());
    }

    /**
     * Tells whether we should mirror round zero datetime behavior
     * 
     * @return true if MySQL rounded values of zero dates and timestamps should
     *         be converted to zeros
     */
    public boolean getMirrorZeroDateTimeRound() {
        return ZERO_DATETIME_BEHAVIOR_ROUND.equals(getMirrorZeroDateTimeBehavior());
    }

    /**
     * Tells whether we should mirror exception zero datetime behavior
     * 
     * @return true if SQLExceptions with state "S1009"
     *         (SQL_STATE_ILLEGAL_ARGUMENT) should be converted to zeros
     */
    public boolean getMirrorZeroDateTimeException() {
        return ZERO_DATETIME_BEHAVIOR_EXCEPTION.equals(getMirrorZeroDateTimeBehavior());
    }

    /**
     * Tells whether we should mirror any zero datetime behavior
     * 
     * @return true no special action should be made for zero datetimes
     */
    public boolean getMirrorZeroDateTimeNo() {
        return (!getMirrorZeroDateTimeConvertToNull() && !getMirrorZeroDateTimeRound() && !getMirrorZeroDateTimeException());
    }

    /**
     * Whether we should force a database connection when none is specified at
     * connection time
     * 
     * @return true if a the protocol is mysql and a database name is specified
     *         in field forcedDBforUnspecConnections
     */
    public boolean getForceDBConnection() {
        if (this.serverProtocol == POSTGRESQL_PROTOCOL_TYPE) return false;
        return !FORCE_DB_NONE.equals(forcedDBforUnspecConnections);
    }

    /**
     * Which database to connect to when none is specified at connection time
     * 
     * @return the default database to connect to or "none" to disable the
     *         feature
     */
    public String getForcedDBforUnspecConnections() {
        if (this.serverProtocol == POSTGRESQL_PROTOCOL_TYPE) return FORCE_DB_NONE;
        return forcedDBforUnspecConnections;
    }

    /**
     * Whether we should remove comments from input requests
     * 
     * @return true if comments must be ignored
     */
    public boolean getIgnoreSQLComments() {
        return ignoreSQLComments;
    }

    /**
     * Whether myosotis should turn transaction related SQL (begin, commit, ...)
     * into JDBC calls instead of sending them to the cluster
     * 
     * @return true if comments must be ignored
     */
    public boolean getManageTransactionsLocally() {
        return manageTransactionsLocally;
    }

    /**
     * Sets the driver value.
     * 
     * @param driver The driver to set.
     */
    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return configurationFilename + " port: " + serverPort + " version: " + serverVersion + " protocol: " + (serverProtocol == MYSQL_PROTOCOL_TYPE ? "mysql" : "postgresql");
    }

    /**
     * Returns the jdbcDirectDriverClassname value.
     * 
     * @return Returns the jdbcDirectDriverClassname.
     */
    public String getJdbcDirectDriverClassname() {
        return jdbcDirectDriverClassname;
    }

    /**
     * Retrieves the maximum allowed number of client connection errors before
     * exiting
     * 
     * @return the max number of client connection failures
     */
    public int getMaxConsecutiveClientConnectionFailures() {
        return this.maxConsecutiveClientConnectionFailures;
    }

    /**
     * Retrieves the list of hosts allowed to connect to the server.<br>
     * Null means that all clients are allowed, non null lists clients
     * authorized in a CIDR format, any other will be refused
     * 
     * @return the list of clients authorized to connect to this server
     */
    public List<String> getAuthorizedHosts() {
        return authorizedHosts;
    }

    /**
     * Retrieves the marker that starts a tungsten command
     * 
     * @return the string starting every tungsten command
     */
    public String getTungstenCommandBeginMarker() {
        return tungstenCommandBeginMarker;
    }

    /**
     * Sets the marker that starts a tungsten command
     */
    public void setTungstenCommandBeginMarker(String tungstenCommandBeginMarker) {
        this.tungstenCommandBeginMarker = tungstenCommandBeginMarker;
    }

    /**
     * Retrieves the marker that ends a tungsten command or empty string to
     * parse commands until the end of the line
     * 
     * @return the string ending every tungsten command
     */
    public String getTungstenCommandEndMarker() {
        return tungstenCommandEndMarker;
    }

    /**
     * Sets the marker that ends a tungsten command. Empty string to parse
     * commands until the end of the line
     */
    public void setTungstenCommandEndMarker(String tungstenCommandEndMarker) {
        this.tungstenCommandEndMarker = tungstenCommandEndMarker;
    }

    public String getTungstenEmbeddedPropertyMarker() {
        return selectiveRwSplittingMarker;
    }

    public boolean isSelectiveRwSplitting() {
        return selectiveRwSplitting;
    }

    /**
     * Toggle pass-through mode on or off
     * 
     * @param on true to stream result sets from server to client application,
     *            false to use regular jdbc ResultSets
     */
    public void setPassThroughMode(boolean on) {
        passThroughMode = on;
        if (logger.isInfoEnabled()) {
            if (passThroughMode == true) {
                logger.info("Using passthrough mode to stream result sets");
            } else {
                logger.info("Using/creating JDBC result sets");
            }
        }
    }

    /**
     * Whether or not to stream result sets from server to client application
     * 
     * @return true if passThrough mode must be activated, false to use regular
     *         jdbc ResultSets
     */
    public boolean isPassThroughMode() {
        return passThroughMode;
    }

    public String getAuthorizedHostsFileName() {
        return authorizedHostsFileName;
    }

    public void setAuthorizedHostsFileName(String authorizedHostsFileName) {
        this.authorizedHostsFileName = authorizedHostsFileName;
    }

    public RWSplittingMode getRWSplittingMode() {
        return rwSplittingMode;
    }

    public void setRWSplittingMode(String modeAsString) {
        if (modeAsString.equalsIgnoreCase("test")) rwSplittingMode = RWSplittingMode.TEST; else if (modeAsString.equalsIgnoreCase("no-sfu")) rwSplittingMode = RWSplittingMode.DONT_CHECK_SELECT_FOR_UPDATE; else if (modeAsString.equalsIgnoreCase("safe")) rwSplittingMode = RWSplittingMode.SAFE; else rwSplittingMode = RWSplittingMode.SAFE;
        if (logger.isInfoEnabled()) logger.info("Setting R/W splitting mode to " + rwSplittingMode);
    }

    public void setUseSmartScale(boolean on) {
        useSmartScale = on;
        if (useSmartScale) {
            if (logger.isInfoEnabled()) logger.info("Using Smart Scale R/W splitting");
            if (!getJdbcDriverOptions().contains("RW_SESSION")) {
                logger.warn("Smart scale is enabled but 'jdbc.driver.options' does not appear to define RW_SESSION QoS!");
            }
        }
    }

    public boolean getUseSmartScale() {
        return useSmartScale;
    }

    public boolean isAutoReconnect() {
        return autoReconnect;
    }

    public void setAutoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
    }
}
