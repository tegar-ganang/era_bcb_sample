package com.continuent.tungsten.replicator.extractor.mysql;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.sql.Timestamp;
import org.apache.log4j.Logger;
import com.continuent.tungsten.replicator.conf.ReplicatorMonitor;
import com.continuent.tungsten.replicator.conf.ReplicatorRuntime;
import com.continuent.tungsten.replicator.extractor.mysql.conversion.LittleEndianConversion;

/**
 * @author <a href="mailto:seppo.jaakola@continuent.com">Seppo Jaakola</a>
 * @author <a href="mailto:stephane.giron@continuent.com">Stephane Giron</a>
 * @version 1.0
 */
public abstract class LogEvent {

    static Logger logger = Logger.getLogger(LogEvent.class);

    protected long execTime;

    protected int type;

    protected Timestamp when;

    protected int serverId;

    protected int logPos;

    protected int flags;

    protected boolean threadSpecificEvent = false;

    public LogEvent() {
        type = MysqlBinlog.START_EVENT_V3;
    }

    public LogEvent(byte[] buffer, FormatDescriptionLogEvent descriptionEvent, int eventType) throws MySQLExtractException {
        type = eventType;
        try {
            when = new Timestamp(1000 * LittleEndianConversion.convert4BytesToLong(buffer, 0));
            serverId = (int) LittleEndianConversion.convert4BytesToLong(buffer, MysqlBinlog.SERVER_ID_OFFSET);
            if (descriptionEvent.binlogVersion == 1) {
                logPos = 0;
                flags = 0;
                return;
            }
            logPos = (int) LittleEndianConversion.convert4BytesToLong(buffer, MysqlBinlog.LOG_POS_OFFSET);
            if ((descriptionEvent.binlogVersion == 3) && (buffer[MysqlBinlog.EVENT_TYPE_OFFSET] < MysqlBinlog.FORMAT_DESCRIPTION_EVENT) && (logPos > 0)) {
                logPos += LittleEndianConversion.convert4BytesToLong(buffer, MysqlBinlog.EVENT_LEN_OFFSET);
            }
            if (logger.isDebugEnabled()) logger.debug("log_pos: " + logPos);
            flags = LittleEndianConversion.convert2BytesToInt(buffer, MysqlBinlog.FLAGS_OFFSET);
            threadSpecificEvent = ((flags & MysqlBinlog.LOG_EVENT_THREAD_SPECIFIC_F) == MysqlBinlog.LOG_EVENT_THREAD_SPECIFIC_F);
            if (logger.isDebugEnabled()) logger.debug("Event is thread-specific = " + threadSpecificEvent);
            if ((buffer[MysqlBinlog.EVENT_TYPE_OFFSET] == MysqlBinlog.FORMAT_DESCRIPTION_EVENT) || (buffer[MysqlBinlog.EVENT_TYPE_OFFSET] == MysqlBinlog.ROTATE_EVENT)) {
                return;
            }
        } catch (IOException e) {
            logger.error("Cannot create log event: " + e);
            throw new MySQLExtractException("log event create failed");
        }
    }

    public long getExecTime() {
        return execTime;
    }

    public Timestamp getWhen() {
        return when;
    }

    private static LogEvent readLogEvent(boolean parseStatements, byte[] buffer, int eventLength, FormatDescriptionLogEvent descriptionEvent, boolean useBytesForString) throws MySQLExtractException {
        LogEvent event = null;
        switch(buffer[MysqlBinlog.EVENT_TYPE_OFFSET]) {
            case MysqlBinlog.QUERY_EVENT:
                event = new QueryLogEvent(buffer, eventLength, descriptionEvent, parseStatements, useBytesForString);
                break;
            case MysqlBinlog.LOAD_EVENT:
                logger.warn("Skipping unsupported LOAD_EVENT");
                break;
            case MysqlBinlog.NEW_LOAD_EVENT:
                logger.warn("Skipping unsupported NEW_LOAD_EVENT");
                break;
            case MysqlBinlog.ROTATE_EVENT:
                event = new RotateLogEvent(buffer, eventLength, descriptionEvent);
                break;
            case MysqlBinlog.SLAVE_EVENT:
                logger.warn("Skipping unsupported SLAVE_EVENT");
                break;
            case MysqlBinlog.CREATE_FILE_EVENT:
                logger.warn("Skipping unsupported CREATE_FILE_EVENT");
                break;
            case MysqlBinlog.APPEND_BLOCK_EVENT:
                logger.debug("reading APPEND_BLOCK_EVENT");
                event = new AppendBlockLogEvent(buffer, eventLength, descriptionEvent);
                break;
            case MysqlBinlog.DELETE_FILE_EVENT:
                logger.debug("reading DELETE_FILE_EVENT");
                event = new DeleteFileLogEvent(buffer, eventLength, descriptionEvent);
                break;
            case MysqlBinlog.EXEC_LOAD_EVENT:
                logger.warn("Skipping unsupported EXEC_LOAD_EVENT");
                break;
            case MysqlBinlog.START_EVENT_V3:
                logger.warn("Skipping unsupported START_EVENT_V3");
                break;
            case MysqlBinlog.STOP_EVENT:
                event = new StopLogEvent(buffer, eventLength, descriptionEvent);
                break;
            case MysqlBinlog.INTVAR_EVENT:
                logger.debug("extracting INTVAR_EVENT");
                event = new IntvarLogEvent(buffer, eventLength, descriptionEvent);
                break;
            case MysqlBinlog.XID_EVENT:
                event = new XidLogEvent(buffer, eventLength, descriptionEvent);
                break;
            case MysqlBinlog.RAND_EVENT:
                event = new RandLogEvent(buffer, eventLength, descriptionEvent);
                break;
            case MysqlBinlog.USER_VAR_EVENT:
                event = new UserVarLogEvent(buffer, eventLength, descriptionEvent);
                break;
            case MysqlBinlog.FORMAT_DESCRIPTION_EVENT:
                event = new FormatDescriptionLogEvent(buffer, eventLength, descriptionEvent);
                break;
            case MysqlBinlog.PRE_GA_WRITE_ROWS_EVENT:
                logger.warn("Skipping unsupported PRE_GA_WRITE_ROWS_EVENT");
                break;
            case MysqlBinlog.PRE_GA_UPDATE_ROWS_EVENT:
                logger.warn("Skipping unsupported PRE_GA_UPDATE_ROWS_EVENT");
                break;
            case MysqlBinlog.PRE_GA_DELETE_ROWS_EVENT:
                logger.warn("Skipping unsupported PRE_GA_DELETE_ROWS_EVENT");
                break;
            case MysqlBinlog.WRITE_ROWS_EVENT:
                logger.debug("reading WRITE_ROWS_EVENT");
                event = new WriteRowsLogEvent(buffer, eventLength, descriptionEvent, useBytesForString);
                break;
            case MysqlBinlog.UPDATE_ROWS_EVENT:
                logger.debug("reading UPDATE_ROWS_EVENT");
                event = new UpdateRowsLogEvent(buffer, eventLength, descriptionEvent, useBytesForString);
                break;
            case MysqlBinlog.DELETE_ROWS_EVENT:
                logger.debug("reading DELETE_ROWS_EVENT");
                event = new DeleteRowsLogEvent(buffer, eventLength, descriptionEvent, useBytesForString);
                break;
            case MysqlBinlog.TABLE_MAP_EVENT:
                logger.debug("reading TABLE_MAP_EVENT");
                event = new TableMapLogEvent(buffer, eventLength, descriptionEvent);
                break;
            case MysqlBinlog.BEGIN_LOAD_QUERY_EVENT:
                logger.debug("reading BEGIN_LOAD_QUERY_EVENT");
                event = new BeginLoadQueryLogEvent(buffer, eventLength, descriptionEvent);
                break;
            case MysqlBinlog.EXECUTE_LOAD_QUERY_EVENT:
                logger.debug("reading EXECUTE_LOAD_QUERY_EVENT");
                event = new ExecuteLoadQueryLogEvent(buffer, eventLength, descriptionEvent, parseStatements);
                break;
            case MysqlBinlog.INCIDENT_EVENT:
                logger.warn("Skipping unsupported INCIDENT_EVENT");
                break;
            default:
                logger.warn("Skipping unrecognized binlog event type " + buffer[MysqlBinlog.EVENT_TYPE_OFFSET]);
        }
        return event;
    }

    public static LogEvent readLogEvent(ReplicatorRuntime runtime, BinlogPosition position, FormatDescriptionLogEvent descriptionEvent, boolean parseStatements, boolean useBytesForString, boolean prefetchSchemaNameLDI) throws MySQLExtractException {
        DataInputStream dis = position.getDis();
        int eventLength = 0;
        byte[] header = new byte[descriptionEvent.commonHeaderLength];
        try {
            readDataFromBinlog(runtime, dis, header, 0, header.length, 60, ReplicatorMonitor.REAL_EXTHEAD);
            eventLength = (int) LittleEndianConversion.convert4BytesToLong(header, MysqlBinlog.EVENT_LEN_OFFSET);
            eventLength -= header.length;
            byte[] fullEvent = new byte[header.length + eventLength];
            readDataFromBinlog(runtime, dis, fullEvent, header.length, eventLength, 120, ReplicatorMonitor.REAL_EXTBODY);
            System.arraycopy(header, 0, fullEvent, 0, header.length);
            LogEvent event = readLogEvent(parseStatements, fullEvent, fullEvent.length, descriptionEvent, useBytesForString);
            if (prefetchSchemaNameLDI && event instanceof BeginLoadQueryLogEvent) {
                if (logger.isDebugEnabled()) logger.debug("Got Begin Load Query Event - Looking for corresponding Execute Event");
                BeginLoadQueryLogEvent beginLoadEvent = (BeginLoadQueryLogEvent) event;
                BinlogPosition tempPosition = position.clone();
                tempPosition.setEventID(position.getEventID() + 1);
                tempPosition.setPosition((int) position.getFis().getChannel().position());
                tempPosition.openFile();
                if (logger.isDebugEnabled()) logger.debug("Reading from " + tempPosition);
                boolean found = false;
                byte[] tmpHeader = new byte[descriptionEvent.commonHeaderLength];
                while (!found) {
                    readDataFromBinlog(runtime, tempPosition.getDis(), tmpHeader, 0, tmpHeader.length, 60, ReplicatorMonitor.REAL_EXTHEAD);
                    eventLength = (int) LittleEndianConversion.convert4BytesToLong(tmpHeader, MysqlBinlog.EVENT_LEN_OFFSET) - tmpHeader.length;
                    if (tmpHeader[MysqlBinlog.EVENT_TYPE_OFFSET] == MysqlBinlog.EXECUTE_LOAD_QUERY_EVENT) {
                        fullEvent = new byte[tmpHeader.length + eventLength];
                        readDataFromBinlog(runtime, tempPosition.getDis(), fullEvent, tmpHeader.length, eventLength, 120, ReplicatorMonitor.REAL_EXTBODY);
                        System.arraycopy(tmpHeader, 0, fullEvent, 0, tmpHeader.length);
                        LogEvent tempEvent = readLogEvent(parseStatements, fullEvent, fullEvent.length, descriptionEvent, useBytesForString);
                        if (tempEvent instanceof ExecuteLoadQueryLogEvent) {
                            ExecuteLoadQueryLogEvent execLoadQueryEvent = (ExecuteLoadQueryLogEvent) tempEvent;
                            if (execLoadQueryEvent.getFileID() == beginLoadEvent.getFileID()) {
                                if (logger.isDebugEnabled()) logger.debug("Found corresponding Execute Load Query Event - Schema is " + execLoadQueryEvent.getDefaultDb());
                                beginLoadEvent.setSchemaName(execLoadQueryEvent.getDefaultDb());
                                found = true;
                            }
                        }
                    } else {
                        long skip = 0;
                        while (skip != eventLength) {
                            skip += tempPosition.getDis().skip(eventLength - skip);
                        }
                    }
                }
                tempPosition.reset();
            }
            return event;
        } catch (EOFException e) {
            throw new MySQLExtractException("EOFException while reading " + eventLength + " bytes from binlog ", e);
        } catch (IOException e) {
            throw new MySQLExtractException("binlog read error", e);
        }
    }

    /**
     * readDataFromBinlog waits for data to be fully written in the binlog file
     * and then reads it.
     * 
     * @param runtime replicator runtime
     * @param dis Input stream from which data will be read
     * @param data Array of byte that will contain read data
     * @param offset Position in the previous array where data should be written
     * @param length Data length to be read
     * @param timeout Maximum time to wait for data to be available
     * @param monitorType Monitoring type
     * @throws IOException if an error occurs while reading from the stream
     * @throws MySQLExtractException if the timeout is reached
     */
    private static void readDataFromBinlog(ReplicatorRuntime runtime, DataInputStream dis, byte[] data, int offset, int length, int timeout, int monitorType) throws IOException, MySQLExtractException {
        long metricID = 0L;
        if (runtime.getMonitor().getDetailEnabled()) metricID = runtime.getMonitor().startRealEvent(monitorType);
        boolean alreadyLogged = false;
        int spentTime = 0;
        int timeoutInMs = timeout * 1000;
        while (length > dis.available()) {
            if (!alreadyLogged) {
                logger.warn("Trying to read more bytes (" + length + ") than available in the file... waiting for data to be available");
                alreadyLogged = true;
            }
            try {
                if (spentTime < timeoutInMs) {
                    Thread.sleep(1);
                    spentTime++;
                } else throw new MySQLExtractException("Timeout while waiting for data : spent more than " + timeoutInMs + " seconds while waiting for " + length + " bytes to be available");
            } catch (InterruptedException e) {
            }
        }
        dis.readFully(data, offset, length);
        if (runtime.getMonitor().getDetailEnabled()) runtime.getMonitor().stopRealEvent(monitorType, metricID);
    }

    public int getType() {
        return type;
    }

    protected String hexdump(byte[] buffer, int offset) {
        StringBuffer dump = new StringBuffer();
        if ((buffer.length - offset) > 0) {
            dump.append(String.format("%02x", buffer[offset]));
            for (int i = offset + 1; i < buffer.length; i++) {
                dump.append("_");
                dump.append(String.format("%02x", buffer[i]));
            }
        }
        return dump.toString();
    }

    protected String hexdump(byte[] buffer, int offset, int length) {
        StringBuffer dump = new StringBuffer();
        if (buffer.length >= offset + length) {
            dump.append(String.format("%02x", buffer[offset]));
            for (int i = offset + 1; i < offset + length; i++) {
                dump.append("_");
                dump.append(String.format("%02x", buffer[i]));
            }
        }
        return dump.toString();
    }
}
