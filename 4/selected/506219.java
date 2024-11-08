package com.continuent.tungsten.replicator.extractor.mysql;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import org.apache.log4j.Logger;
import com.continuent.tungsten.commons.commands.FileCommands;
import com.continuent.tungsten.replicator.ReplicatorException;
import com.continuent.tungsten.replicator.SQLQuery.MySQLQuery;
import com.continuent.tungsten.replicator.conf.FailurePolicy;
import com.continuent.tungsten.replicator.conf.ReplicatorRuntime;
import com.continuent.tungsten.replicator.database.Database;
import com.continuent.tungsten.replicator.database.DatabaseFactory;
import com.continuent.tungsten.replicator.dbms.DBMSData;
import com.continuent.tungsten.replicator.dbms.LoadDataFileDelete;
import com.continuent.tungsten.replicator.dbms.LoadDataFileFragment;
import com.continuent.tungsten.replicator.dbms.LoadDataFileQuery;
import com.continuent.tungsten.replicator.dbms.RowChangeData;
import com.continuent.tungsten.replicator.dbms.RowIdData;
import com.continuent.tungsten.replicator.dbms.StatementData;
import com.continuent.tungsten.replicator.event.DBMSEmptyEvent;
import com.continuent.tungsten.replicator.event.DBMSEvent;
import com.continuent.tungsten.replicator.event.ReplOption;
import com.continuent.tungsten.replicator.extractor.ExtractorException;
import com.continuent.tungsten.replicator.extractor.RawExtractor;
import com.continuent.tungsten.replicator.extractor.mysql.conversion.LittleEndianConversion;
import com.continuent.tungsten.replicator.plugin.PluginContext;

/**
 * This class defines a MySQLExtractor
 * 
 * @author <a href="mailto:seppo.jaakola@continuent.com">Seppo Jaakola</a>
 * @version 1.0
 */
public class MySQLExtractor implements RawExtractor {

    private static Logger logger = Logger.getLogger(MySQLExtractor.class);

    private ReplicatorRuntime runtime = null;

    private String host = "localhost";

    private int port = 3306;

    private String user = "root";

    private String password = "";

    private boolean strictVersionChecking = true;

    private boolean parseStatements = true;

    private String binlogFilePattern = "mysql-bin";

    private String binlogDir = "/var/log/mysql";

    private boolean useRelayLogs = false;

    private long relayLogWaitTimeout = 0;

    private int relayLogRetention = 3;

    private String relayLogDir = null;

    private String url;

    private static long binlogPositionMaxLength = 10;

    BinlogPosition binlogPosition = null;

    private static long INDEX_CHECK_INTERVAL = 60000;

    private HashMap<Long, TableMapLogEvent> tableEvents = new HashMap<Long, TableMapLogEvent>();

    private int transactionFragSize = 0;

    private boolean fragmentedTransaction = false;

    private RelayLogTask relayLogTask = null;

    private Thread relayLogThread = null;

    private boolean useBytesForStrings = false;

    private boolean prefetchSchemaNameLDI = false;

    private HashMap<Integer, String> loadDataSchemas;

    private String jdbcHeader;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getBinlogFilePattern() {
        return binlogFilePattern;
    }

    public void setBinlogFilePattern(String binlogFilePattern) {
        this.binlogFilePattern = binlogFilePattern;
    }

    public String getBinlogDir() {
        return binlogDir;
    }

    public void setBinlogDir(String binlogDir) {
        this.binlogDir = binlogDir;
    }

    public boolean isStrictVersionChecking() {
        return strictVersionChecking;
    }

    public void setStrictVersionChecking(boolean strictVersionChecking) {
        this.strictVersionChecking = strictVersionChecking;
    }

    public boolean isParseStatements() {
        return parseStatements;
    }

    public void setParseStatements(boolean parseStatements) {
        this.parseStatements = parseStatements;
    }

    public boolean isUsingBytesForString() {
        return useBytesForStrings;
    }

    public void setUsingBytesForString(boolean useBytes) {
        this.useBytesForStrings = useBytes;
    }

    public boolean isUseRelayLogs() {
        return useRelayLogs;
    }

    public void setUseRelayLogs(boolean useRelayDir) {
        this.useRelayLogs = useRelayDir;
    }

    public long getRelayLogWaitTimeout() {
        return relayLogWaitTimeout;
    }

    public void setRelayLogWaitTimeout(long relayLogWaitTimeout) {
        this.relayLogWaitTimeout = relayLogWaitTimeout;
    }

    public int getRelayLogRetention() {
        return relayLogRetention;
    }

    public void setRelayLogRetention(int relayLogRetention) {
        this.relayLogRetention = relayLogRetention;
    }

    public String getRelayLogDir() {
        return relayLogDir;
    }

    public void setRelayLogDir(String relayLogDir) {
        this.relayLogDir = relayLogDir;
    }

    public int getTransactionFragSize() {
        return transactionFragSize;
    }

    public void setTransactionFragSize(int transactionFragSize) {
        this.transactionFragSize = transactionFragSize;
    }

    public String getJdbcHeader() {
        return jdbcHeader;
    }

    public void setJdbcHeader(String jdbcHeader) {
        this.jdbcHeader = jdbcHeader;
    }

    private void check_header(BinlogPosition position) throws MySQLExtractException {
        byte header[] = new byte[MysqlBinlog.BIN_LOG_HEADER_SIZE];
        byte buf[] = new byte[MysqlBinlog.PROBE_HEADER_LEN];
        long tmp_pos = 0;
        FormatDescriptionLogEvent description_event = new FormatDescriptionLogEvent(3);
        try {
            if (position.getFis().read(header) != header.length) {
                logger.error("Failed reading header;  Probably an empty file");
                throw new MySQLExtractException("could not read binlog header");
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        if (!java.util.Arrays.equals(header, MysqlBinlog.BINLOG_MAGIC)) {
            logger.error("File is not a binary log file");
            throw new MySQLExtractException("binlog file header mismatch");
        }
        for (; ; ) {
            try {
                tmp_pos = position.getFis().getChannel().position();
                position.getFis().mark(2048);
                position.getDis().readFully(buf);
                logger.debug("buf[4]=" + buf[4]);
                long start_position = 0;
                if (buf[4] == MysqlBinlog.START_EVENT_V3) {
                    if (LittleEndianConversion.convert4BytesToLong(buf, MysqlBinlog.EVENT_LEN_OFFSET) < (MysqlBinlog.LOG_EVENT_MINIMAL_HEADER_LEN + MysqlBinlog.START_V3_HEADER_LEN)) {
                        description_event = new FormatDescriptionLogEvent(1);
                    }
                    break;
                } else if (tmp_pos >= start_position) break; else if (buf[4] == MysqlBinlog.FORMAT_DESCRIPTION_EVENT) {
                    FormatDescriptionLogEvent new_description_event;
                    position.getFis().reset();
                    new_description_event = (FormatDescriptionLogEvent) LogEvent.readLogEvent(runtime, position, description_event, parseStatements, useBytesForStrings, prefetchSchemaNameLDI);
                    if (new_description_event == null) {
                        logger.error("Could not read a Format_description_log_event event " + "at offset " + tmp_pos + "this could be a log format error or read error");
                        throw new MySQLExtractException("binlog format error");
                    }
                    description_event = new_description_event;
                    logger.debug("Setting description_event");
                } else if (buf[4] == MysqlBinlog.ROTATE_EVENT) {
                    LogEvent ev;
                    position.getFis().reset();
                    ev = LogEvent.readLogEvent(runtime, position, description_event, parseStatements, useBytesForStrings, prefetchSchemaNameLDI);
                    if (ev == null) {
                        logger.error("Could not read a Rotate_log_event event " + "at offset " + tmp_pos + " this could be a log format error or " + "read error");
                        throw new MySQLExtractException("binlog format error");
                    }
                } else {
                    break;
                }
            } catch (EOFException e) {
                break;
            } catch (IOException e) {
                logger.error("Could not read entry at offset " + tmp_pos + " : Error in log format or read error");
                throw new MySQLExtractException("binlog read error" + e);
            }
        }
    }

    private LogEvent processFile(BinlogPosition position) throws ExtractorException, InterruptedException {
        try {
            if (position.getFis() == null) {
                position.openFile();
            }
            logger.debug("extracting from pos, file: " + position.getFileName() + " pos: " + position.getPosition());
            if (position.getFis() != null) {
                long indexCheckStart = System.currentTimeMillis();
                while (position.getDis().available() == 0) {
                    if (System.currentTimeMillis() - indexCheckStart > INDEX_CHECK_INTERVAL) {
                        BinlogIndex bi = new BinlogIndex(binlogDir, binlogFilePattern, true);
                        File nextBinlog = bi.nextBinlog(position.getFileName());
                        if (nextBinlog != null) {
                            logger.warn("Current log file appears to be missing log-rotate event: " + position.getFileName());
                            logger.info("Auto-generating log-rotate event for next binlog file: " + nextBinlog.getName());
                            return new RotateLogEvent(nextBinlog.getName());
                        }
                        assertRelayLogsEnabled();
                        indexCheckStart = System.currentTimeMillis();
                    }
                    Thread.sleep(10);
                }
                FormatDescriptionLogEvent description_event = new FormatDescriptionLogEvent(4);
                if (position.getPosition() == 0) {
                    check_header(position);
                    position.setPosition(0);
                    position.openFile();
                    byte[] buf = new byte[MysqlBinlog.BIN_LOG_HEADER_SIZE];
                    position.getDis().readFully(buf);
                }
                LogEvent event = LogEvent.readLogEvent(runtime, position, description_event, parseStatements, useBytesForStrings, prefetchSchemaNameLDI);
                position.setEventID(position.getEventID() + 1);
                position.setPosition((int) position.getFis().getChannel().position());
                return event;
            } else {
                logger.error("binlog file channel not open");
                throw new MySQLExtractException("binlog file channel not open");
            }
        } catch (IOException e) {
            logger.error("binlog file read error");
            throw new MySQLExtractException("binlog file read error");
        }
    }

    private static String getDBMSEventId(BinlogPosition binlogPosition, long sessionId) {
        String fileName = binlogPosition.getFileName();
        String binlogNumber = fileName.substring(fileName.lastIndexOf('.') + 1);
        String position = getPositionAsString(binlogPosition.getPosition());
        return String.format("%s:%s;%d", binlogNumber, position, sessionId);
    }

    private static String getPositionAsString(long position) {
        return String.format("%0" + (binlogPositionMaxLength + 1) + "d", new Long(position));
    }

    private BinlogPosition positionBinlog(BinlogPosition position, boolean flush) throws ExtractorException {
        Database conn = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            conn = DatabaseFactory.createDatabase(url, user, password);
            conn.connect();
            st = conn.createStatement();
            if (flush) {
                logger.debug("Flushing logs");
                st.executeUpdate("FLUSH LOGS");
            }
            logger.debug("Seeking head position in binlog");
            rs = st.executeQuery("SHOW MASTER STATUS");
            if (!rs.next()) throw new ExtractorException("Error getting master status; is the MySQL binlog enabled?");
            String binlogFile = rs.getString(1);
            long binlogOffset;
            if (position == null || position.getFileName().equals(binlogFile)) {
                binlogOffset = rs.getLong(2);
            } else {
                binlogOffset = 0;
            }
            if (useRelayLogs) {
                startRelayLogs(binlogFile, binlogOffset);
            }
            logger.info("Starting from position: " + binlogFile + ":" + binlogOffset);
            return new BinlogPosition(binlogOffset, binlogFile, binlogDir, binlogFilePattern);
        } catch (SQLException e) {
            logger.info("url: " + url + " user: " + user + " password: ********");
            throw new ExtractorException(e);
        } finally {
            cleanUpDatabaseResources(conn, st, rs);
        }
    }

    private DBMSEvent extractEvent(BinlogPosition position) throws ExtractorException, InterruptedException {
        boolean inTransaction = fragmentedTransaction;
        fragmentedTransaction = false;
        boolean autocommitMode = true;
        boolean doFileFragment = false;
        Timestamp startTime = null;
        long sessionId = 0;
        ArrayList<DBMSData> dataArray = new ArrayList<DBMSData>();
        boolean foundRowsLogEvent = false;
        LinkedList<ReplOption> savedOptions = new LinkedList<ReplOption>();
        try {
            String defaultDb = null;
            RowChangeData rowChangeData = null;
            long fragSize = 0;
            while (true) {
                BinlogPosition previousPosition = position.clone();
                DBMSEvent dbmsEvent = null;
                LogEvent logEvent = processFile(position);
                if (logEvent == null) {
                    logger.debug("Unknown binlog field, skipping");
                    continue;
                }
                if (startTime == null) startTime = logEvent.getWhen();
                if (logEvent instanceof RowsLogEvent) {
                    fragSize += ((RowsLogEvent) logEvent).getEventSize();
                }
                if (logger.isDebugEnabled()) logger.debug("Current fragment size=" + fragSize);
                boolean doCommit = false;
                if (logEvent.getClass() == QueryLogEvent.class) {
                    QueryLogEvent event = (QueryLogEvent) logEvent;
                    String queryString = event.getQuery();
                    StatementData statement = new StatementData(queryString);
                    if (logger.isDebugEnabled()) logger.debug("Query extracted: " + queryString);
                    MySQLQuery query = null;
                    if (!useBytesForStrings) query = new MySQLQuery(queryString); else {
                        query = new MySQLQuery(new String(event.getQueryAsBytes(), 0, Math.min(event.getQueryAsBytes().length, 200)));
                    }
                    doCommit = !inTransaction || query.doesCommit();
                    if (query.startsTransaction()) {
                        inTransaction = true;
                        doCommit = false;
                        savedOptions.add(new ReplOption("autocommit", event.getAutocommitFlag()));
                        savedOptions.add(new ReplOption("sql_auto_is_null", event.getAutoIsNullFlag()));
                        savedOptions.add(new ReplOption("foreign_key_checks", event.getForeignKeyChecksFlag()));
                        savedOptions.add(new ReplOption("unique_checks", event.getUniqueChecksFlag()));
                        savedOptions.add(new ReplOption("sql_mode", event.getSqlMode()));
                        savedOptions.add(new ReplOption("character_set_client", String.valueOf(event.getClientCharsetId())));
                        savedOptions.add(new ReplOption("collation_connection", String.valueOf(event.getClientCollationId())));
                        savedOptions.add(new ReplOption("collation_server", String.valueOf(event.getServerCollationId())));
                        if (event.getAutoIncrementIncrement() >= 0) savedOptions.add(new ReplOption("auto_increment_increment", String.valueOf(event.getAutoIncrementIncrement())));
                        if (event.getAutoIncrementOffset() >= 0) savedOptions.add(new ReplOption("auto_increment_offset", String.valueOf(event.getAutoIncrementOffset())));
                        continue;
                    }
                    if (query.getQuery().toUpperCase().startsWith("COMMIT")) {
                        doCommit = true;
                        inTransaction = !autocommitMode;
                    } else {
                        boolean isCreateOrDropDB = query.createDatabase();
                        boolean prependUseDb = !(query.doesCommit() && isCreateOrDropDB);
                        if (defaultDb == null) {
                            sessionId = event.getSessionId();
                            if (prependUseDb) {
                                defaultDb = event.getDefaultDb();
                                statement.setDefaultSchema(defaultDb);
                            }
                        } else {
                            assert (sessionId == event.getSessionId());
                            String newDb = event.getDefaultDb();
                            if (!defaultDb.equals(newDb) && prependUseDb) {
                                defaultDb = newDb;
                                statement.setDefaultSchema(newDb);
                            }
                        }
                        if (isCreateOrDropDB) statement.addOption(StatementData.CREATE_OR_DROP_DB, "");
                        statement.setTimestamp(event.getWhen().getTime());
                        if (!useBytesForStrings) {
                            statement.setQuery(queryString);
                            fragSize += queryString.length();
                        } else {
                            byte[] bytes = event.getQueryAsBytes();
                            statement.setQuery(bytes);
                            fragSize += bytes.length;
                        }
                        statement.addOption("autocommit", event.getAutocommitFlag());
                        statement.addOption("sql_auto_is_null", event.getAutoIsNullFlag());
                        statement.addOption("foreign_key_checks", event.getForeignKeyChecksFlag());
                        statement.addOption("unique_checks", event.getUniqueChecksFlag());
                        if (event.getAutoIncrementIncrement() >= 0) statement.addOption("auto_increment_increment", String.valueOf(event.getAutoIncrementIncrement()));
                        if (event.getAutoIncrementOffset() >= 0) statement.addOption("auto_increment_offset", String.valueOf(event.getAutoIncrementOffset()));
                        statement.addOption("sql_mode", event.getSqlMode());
                        statement.addOption("character_set_client", String.valueOf(event.getClientCharsetId()));
                        statement.addOption("collation_connection", String.valueOf(event.getClientCollationId()));
                        statement.addOption("collation_server", String.valueOf(event.getServerCollationId()));
                        statement.setErrorCode(event.getErrorCode());
                        dataArray.add(statement);
                    }
                } else if (logEvent.getClass() == UserVarLogEvent.class) {
                    logger.debug("USER_VAR_EVENT detected: " + ((UserVarLogEvent) logEvent).getQuery());
                    dataArray.add(new StatementData(((UserVarLogEvent) logEvent).getQuery()));
                } else if (logEvent.getClass() == RandLogEvent.class) {
                    logger.debug("RAND_EVENT detected: " + ((RandLogEvent) logEvent).getQuery());
                    dataArray.add(new StatementData(((RandLogEvent) logEvent).getQuery()));
                } else if (logEvent.getClass() == IntvarLogEvent.class) {
                    logger.debug("INTVAR_EVENT detected, value: " + ((IntvarLogEvent) logEvent).getValue());
                    dataArray.add(new RowIdData(((IntvarLogEvent) logEvent).getValue()));
                } else if (logEvent.getClass() == XidLogEvent.class) {
                    logger.debug("Commit extracted: " + ((XidLogEvent) logEvent).getXid());
                    if (!dataArray.isEmpty()) {
                        doCommit = true;
                    }
                    if (rowChangeData != null) {
                        doCommit = true;
                    }
                    inTransaction = !autocommitMode;
                    if (!doCommit) {
                        logger.debug("Clearing Table Map events");
                        tableEvents.clear();
                        tableEvents = new HashMap<Long, TableMapLogEvent>();
                        return new DBMSEmptyEvent(getDBMSEventId(position, sessionId));
                    }
                } else if (logEvent.getClass() == StopLogEvent.class) {
                    logger.debug("Stop event extracted: ");
                    String stopEventId = getDBMSEventId(previousPosition, sessionId);
                    logger.info("Skipping over server stop event in log: " + stopEventId);
                } else if (logEvent.getClass() == RotateLogEvent.class) {
                    logger.debug("got Rotate log event");
                    position.reset();
                    position.setFileName(((RotateLogEvent) logEvent).getNewBinlogFilename());
                    position.openFile();
                    if (useRelayLogs) purgeRelayLogs(false);
                } else if (logEvent.getClass() == TableMapLogEvent.class) {
                    logger.debug("got table map event");
                    TableMapLogEvent tableEvent = (TableMapLogEvent) logEvent;
                    tableEvents.put(tableEvent.getTableId(), tableEvent);
                } else if (logEvent instanceof RowsLogEvent) {
                    if (logger.isDebugEnabled()) logger.debug("got rows log event - event size = " + ((RowsLogEvent) logEvent).getEventSize());
                    rowChangeData = new RowChangeData();
                    RowsLogEvent rowsEvent = (RowsLogEvent) logEvent;
                    TableMapLogEvent tableEvent = tableEvents.get(rowsEvent.getTableId());
                    rowsEvent.processExtractedEvent(rowChangeData, tableEvent);
                    dataArray.add(rowChangeData);
                    foundRowsLogEvent = true;
                } else if (logEvent instanceof BeginLoadQueryLogEvent) {
                    BeginLoadQueryLogEvent event = (BeginLoadQueryLogEvent) logEvent;
                    if (prefetchSchemaNameLDI) {
                        if (loadDataSchemas == null) loadDataSchemas = new HashMap<Integer, String>();
                        loadDataSchemas.put(Integer.valueOf(event.getFileID()), event.getSchemaName());
                    }
                    dataArray.add(new LoadDataFileFragment(event.getFileID(), event.getData(), event.getSchemaName()));
                    doFileFragment = true;
                } else if (logEvent instanceof AppendBlockLogEvent) {
                    AppendBlockLogEvent event = (AppendBlockLogEvent) logEvent;
                    String schema = null;
                    if (prefetchSchemaNameLDI && loadDataSchemas != null) schema = loadDataSchemas.get(Integer.valueOf(event.getFileID()));
                    dataArray.add(new LoadDataFileFragment(event.getFileID(), event.getData(), schema));
                    doFileFragment = true;
                } else if (logEvent instanceof ExecuteLoadQueryLogEvent) {
                    ExecuteLoadQueryLogEvent event = (ExecuteLoadQueryLogEvent) logEvent;
                    if (loadDataSchemas != null) loadDataSchemas.remove(Integer.valueOf(event.getFileID()));
                    String queryString = event.getQuery();
                    LoadDataFileQuery statement = new LoadDataFileQuery(queryString, event.getWhen().getTime(), event.getDefaultDb(), event.getFileID(), event.getStartPos(), event.getEndPos());
                    statement.addOption("autocommit", event.getAutocommitFlag());
                    statement.addOption("sql_auto_is_null", event.getAutoIsNullFlag());
                    statement.addOption("foreign_key_checks", event.getForeignKeyChecksFlag());
                    statement.addOption("unique_checks", event.getUniqueChecksFlag());
                    statement.addOption("sql_mode", event.getSqlMode());
                    statement.addOption("character_set_client", String.valueOf(event.getClientCharsetId()));
                    statement.addOption("collation_connection", String.valueOf(event.getClientCollationId()));
                    statement.addOption("collation_server", String.valueOf(event.getServerCollationId()));
                    if (logger.isDebugEnabled()) {
                        logger.debug("statement.getOptions() = " + statement.getOptions());
                    }
                    statement.setErrorCode(event.getErrorCode());
                    dataArray.add(statement);
                    doFileFragment = true;
                } else if (logEvent instanceof DeleteFileLogEvent) {
                    LoadDataFileDelete delete = new LoadDataFileDelete(((DeleteFileLogEvent) logEvent).getFileID());
                    dataArray.add(delete);
                } else {
                    logger.debug("got binlog event: " + logEvent);
                }
                if (doCommit) {
                    logger.debug("Performing commit processing in extractor");
                    runtime.getMonitor().incrementEvents(dataArray.size());
                    String eventId = getDBMSEventId(position, sessionId);
                    dbmsEvent = new DBMSEvent(eventId, dataArray, startTime);
                    if (foundRowsLogEvent) dbmsEvent.setOptions(savedOptions);
                    logger.debug("Clearing Table Map events");
                    tableEvents.clear();
                    savedOptions.clear();
                } else if (transactionFragSize > 0 && fragSize > transactionFragSize) {
                    if (logger.isDebugEnabled()) logger.debug("Fragmenting -- fragment size = " + fragSize);
                    runtime.getMonitor().incrementEvents(dataArray.size());
                    String eventId = getDBMSEventId(position, sessionId);
                    dbmsEvent = new DBMSEvent(eventId, dataArray, false, startTime);
                    if (foundRowsLogEvent) dbmsEvent.setOptions(savedOptions);
                    this.fragmentedTransaction = true;
                } else if (doFileFragment) {
                    doFileFragment = false;
                    runtime.getMonitor().incrementEvents(dataArray.size());
                    String eventId = getDBMSEventId(position, sessionId);
                    dbmsEvent = new DBMSEvent(eventId, dataArray, false, startTime);
                    if (foundRowsLogEvent) dbmsEvent.setOptions(savedOptions);
                }
                if (dbmsEvent != null) return dbmsEvent;
            }
        } catch (ExtractorException e) {
            logger.error("Failed to extract from " + position, e);
            if (runtime.getExtractorFailurePolicy() == FailurePolicy.STOP) throw e;
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected failure while extracting event " + position, e);
            if (runtime.getExtractorFailurePolicy() == FailurePolicy.STOP) throw new ExtractorException("Unexpected failure while extracting event " + position, e);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.continuent.tungsten.replicator.extractor.RawExtractor#extract()
     */
    public synchronized DBMSEvent extract() throws InterruptedException, ExtractorException {
        assertRelayLogsEnabled();
        return extractEvent(binlogPosition);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.continuent.tungsten.replicator.extractor.RawExtractor#extract(java.lang.String)
     */
    public DBMSEvent extract(String id) throws InterruptedException, ExtractorException {
        setLastEventId(id);
        return extract();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.continuent.tungsten.replicator.extractor.RawExtractor#setLastEventId(java.lang.String)
     */
    public void setLastEventId(String eventId) throws ExtractorException {
        if (eventId != null) {
            int colonIndex = eventId.indexOf(':');
            int semicolonIndex = eventId.indexOf(";");
            String binlogFileIndex = eventId.substring(0, colonIndex);
            int binlogOffset;
            if (semicolonIndex != -1) {
                binlogOffset = Integer.valueOf(eventId.substring(colonIndex + 1, semicolonIndex));
            } else {
                binlogOffset = Integer.valueOf(eventId.substring(colonIndex + 1));
            }
            String binlogFile;
            if (binlogFileIndex.startsWith(binlogFilePattern)) binlogFile = binlogFileIndex; else binlogFile = binlogFilePattern + "." + binlogFileIndex;
            if (useRelayLogs) startRelayLogs(binlogFile, binlogOffset);
            binlogPosition = new BinlogPosition(binlogOffset, binlogFile, binlogDir, binlogFilePattern);
        } else {
            binlogPosition = positionBinlog(null, true);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.continuent.tungsten.replicator.plugin.ReplicatorPlugin#configure(com.continuent.tungsten.replicator.plugin.PluginContext)
     */
    public void configure(PluginContext context) throws ReplicatorException {
        runtime = (ReplicatorRuntime) context;
        StringBuffer sb = new StringBuffer();
        if (jdbcHeader == null) sb.append("jdbc:mysql://"); else sb.append(jdbcHeader);
        sb.append(host);
        sb.append(":");
        sb.append(port);
        sb.append("/");
        url = sb.toString();
        if (this.useRelayLogs) {
            if (relayLogDir == null) {
                throw new ReplicatorException("Relay logging is enabled but relay log directory is not set");
            }
            File relayLogs = new File(relayLogDir);
            if (!relayLogs.canWrite()) {
                throw new ReplicatorException("Relay log directory does not exist or is not writable: " + relayLogs.getAbsolutePath());
            }
            logger.info("Using relay log directory as source of binlogs: " + relayLogDir);
            binlogDir = relayLogDir;
        }
    }

    /**
     * If strictVersionChecking is enabled we ensure this database is a
     * supported version. {@inheritDoc}
     * 
     * @see com.continuent.tungsten.replicator.plugin.ReplicatorPlugin#prepare(com.continuent.tungsten.replicator.plugin.PluginContext)
     */
    public void prepare(PluginContext context) throws ReplicatorException {
        if (!strictVersionChecking) {
            logger.warn("MySQL start-up checks are disabled; binlog " + "extraction may fail for unsupported versions " + "or if InnoDB is not present");
            return;
        }
        Database conn = null;
        try {
            conn = DatabaseFactory.createDatabase(url, user, password);
            conn.connect();
            String version = getDatabaseVersion(conn);
            if (version != null && version.startsWith("5")) {
                logger.info("Binlog extraction is supported for this MySQL version: " + version);
            } else {
                logger.warn("Binlog extraction is not certified for this server version: " + version);
                logger.warn("You may experience replication failures due to binlog incompatibilities");
            }
            getMaxBinlogSize(conn);
            checkInnoDBSupport(conn);
        } catch (SQLException e) {
            String message = "Unable to connect to MySQL server while preparing extractor; is server available?";
            logger.error(message);
            logger.info("url: " + url + " user: " + user + " password: *********");
            throw new ExtractorException(message, e);
        } finally {
            cleanUpDatabaseResources(conn, null, null);
        }
    }

    private String getDatabaseVersion(Database conn) throws ExtractorException {
        String version = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            st = conn.createStatement();
            rs = st.executeQuery("SELECT @@VERSION");
            if (rs.next()) {
                version = rs.getString(1);
            }
        } catch (SQLException e) {
            String message = "Unable to connect to MySQL server to check version; is server available?";
            logger.error(message);
            logger.info("url: " + url + " user: " + user + " password: *********");
            throw new ExtractorException(message, e);
        } finally {
            cleanUpDatabaseResources(null, st, rs);
        }
        return version;
    }

    private void getMaxBinlogSize(Database conn) throws ExtractorException {
        Statement st = null;
        ResultSet rs = null;
        try {
            st = conn.createStatement();
            rs = st.executeQuery("show variables like 'max_binlog_size'");
            if (rs.next()) {
                binlogPositionMaxLength = rs.getString(1).length();
            }
        } catch (SQLException e) {
            String message = "Unable to connect to MySQL server to get max_binlog_size setting; is server available?";
            logger.error(message);
            logger.info("url: " + url + " user: " + user + " password: *********");
            throw new ExtractorException(message, e);
        } finally {
            cleanUpDatabaseResources(null, st, rs);
        }
    }

    private void checkInnoDBSupport(Database conn) throws ExtractorException {
        Statement st = null;
        ResultSet rs = null;
        try {
            st = conn.createStatement();
            rs = st.executeQuery("show variables like 'have_innodb'");
            if (!rs.next() || rs.getString(2).compareToIgnoreCase("disabled") == 0) {
                logger.warn("Warning! InnoDB support does not seem to be activated (check mysql have_innodb variable)");
            }
        } catch (SQLException e) {
            String message = "Unable to connect to MySQL server to check have_innodb setting; is server available?";
            logger.error(message);
            logger.info("url: " + url + " user: " + user + " password: *********");
            throw new ExtractorException(message, e);
        } finally {
            cleanUpDatabaseResources(null, st, rs);
        }
    }

    /** Starts relay logs. */
    private synchronized void startRelayLogs(String fileName, long offset) throws ExtractorException {
        if (!useRelayLogs) return;
        String startPosition = fileName + ":" + offset;
        stopRelayLogs();
        RelayLogClient relayClient = new RelayLogClient();
        relayClient.setUrl(url);
        relayClient.setLogin(user);
        relayClient.setPassword(password);
        relayClient.setBinlogDir(binlogDir);
        relayClient.setBinlog(fileName);
        relayClient.setBinlogPrefix(binlogFilePattern);
        relayClient.connect();
        relayLogTask = new RelayLogTask(relayClient);
        relayLogThread = new Thread(relayLogTask, "Relay Client " + host + ":" + port);
        relayLogThread.start();
        logger.info("Waiting for relay log position to catch up to extraction position: " + startPosition);
        long startTime = System.currentTimeMillis();
        long maxEndTime;
        if (relayLogWaitTimeout > 0) maxEndTime = startTime + (relayLogWaitTimeout * 1000); else maxEndTime = Long.MAX_VALUE;
        int loopCount = 0;
        while (System.currentTimeMillis() < maxEndTime) {
            RelayLogPosition position = relayLogTask.getPosition();
            if (position.hasReached(fileName, offset)) break;
            if (relayLogTask.isFinished()) throw new ExtractorException("Relay log task failed while waiting for start position: " + startPosition);
            if (loopCount % 10 == 0) logger.info("Current relay log position: " + position.toString());
            loopCount++;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new ExtractorException("Unexpected interruption while positioning binlog");
            }
        }
        if (System.currentTimeMillis() >= maxEndTime) {
            throw new ExtractorException("Timed out waiting for relay log to reach extraction position: " + fileName + ":" + offset);
        }
    }

    /**
     * Purge old relay logs that have aged out past the number of retained
     * files.
     */
    private void purgeRelayLogs(boolean wait) {
        if (relayLogRetention > 1) {
            logger.info("Checking for old relay log files...");
            File logDir = new File(binlogDir);
            File[] filesToPurge = FileCommands.filesOverRetentionAndInactive(logDir, binlogFilePattern, relayLogRetention + 1, this.binlogPosition.getFileName());
            FileCommands.deleteFiles(filesToPurge, wait);
        }
    }

    /** Starts relay logs and ensures they are running. */
    private synchronized void assertRelayLogsEnabled() throws ExtractorException, InterruptedException {
        if (useRelayLogs) {
            if (relayLogTask == null) {
                startRelayLogs(binlogPosition.getFileName(), binlogPosition.getPosition());
            } else if (relayLogTask.isFinished()) throw new ExtractorException("Relay log task has unexpectedly terminated; logs may not be accessible");
        }
    }

    /** Stops relay log operation. */
    private synchronized void stopRelayLogs() {
        if (relayLogTask == null || relayLogTask.isFinished()) return;
        logger.info("Cancelling relay log thread");
        relayLogTask.cancel();
        relayLogThread.interrupt();
        try {
            relayLogThread.join(10000);
        } catch (InterruptedException e) {
            logger.warn("Interrupted while waiting for relay log task to complete");
        }
        if (relayLogTask.isFinished()) {
            relayLogTask = null;
            relayLogThread = null;
        } else logger.warn("Unable to cancel relay log thread");
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.continuent.tungsten.replicator.plugin.ReplicatorPlugin#release(com.continuent.tungsten.replicator.plugin.PluginContext)
     */
    public void release(PluginContext context) throws ReplicatorException {
        stopRelayLogs();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.continuent.tungsten.replicator.extractor.RawExtractor#getCurrentResourceEventId()
     */
    public String getCurrentResourceEventId() throws ExtractorException, InterruptedException {
        Database conn = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            conn = DatabaseFactory.createDatabase(url, user, password);
            conn.connect();
            st = conn.createStatement();
            logger.debug("Seeking head position in binlog");
            rs = st.executeQuery("SHOW MASTER STATUS");
            if (!rs.next()) throw new ExtractorException("Error getting master status; is the MySQL binlog enabled?");
            String binlogFile = rs.getString(1);
            long binlogOffset = rs.getLong(2);
            String eventId = binlogFile.substring(binlogFile.lastIndexOf('.') + 1) + ":" + getPositionAsString(binlogOffset);
            return eventId;
        } catch (SQLException e) {
            logger.info("url: " + url + " user: " + user + " password: ********");
            throw new ExtractorException("Unable to run SHOW MASTER STATUS to find log position", e);
        } finally {
            cleanUpDatabaseResources(conn, st, rs);
        }
    }

    private void cleanUpDatabaseResources(Database conn, Statement st, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException ignore) {
            }
        }
        if (st != null) {
            try {
                st.close();
            } catch (SQLException ignore) {
            }
        }
        if (conn != null) conn.close();
    }

    public void setPrefetchSchemaNameLDI(boolean prefetchSchemaNameLDI) {
        this.prefetchSchemaNameLDI = prefetchSchemaNameLDI;
    }
}
