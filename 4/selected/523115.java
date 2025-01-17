package org.jumpmind.symmetric.load.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jumpmind.symmetric.common.ErrorConstants;
import org.jumpmind.symmetric.common.ParameterConstants;
import org.jumpmind.symmetric.common.csv.CsvConstants;
import org.jumpmind.symmetric.db.IDbDialect;
import org.jumpmind.symmetric.load.DataLoaderContext;
import org.jumpmind.symmetric.load.DataLoaderStatistics;
import org.jumpmind.symmetric.load.IColumnFilter;
import org.jumpmind.symmetric.load.IDataLoader;
import org.jumpmind.symmetric.load.IDataLoaderContext;
import org.jumpmind.symmetric.load.IDataLoaderFilter;
import org.jumpmind.symmetric.load.IDataLoaderStatistics;
import org.jumpmind.symmetric.load.TableTemplate;
import org.jumpmind.symmetric.model.Node;
import org.jumpmind.symmetric.model.Trigger;
import org.jumpmind.symmetric.service.IConfigurationService;
import org.jumpmind.symmetric.service.INodeService;
import org.jumpmind.symmetric.service.IParameterService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import com.csvreader.CsvReader;

public class CsvLoader implements IDataLoader {

    static final Log logger = LogFactory.getLog(CsvLoader.class);

    protected JdbcTemplate jdbcTemplate;

    protected IDbDialect dbDialect;

    protected IParameterService parameterService;

    protected IConfigurationService configurationService;

    protected INodeService nodeService;

    protected CsvReader csvReader;

    protected DataLoaderContext context;

    protected DataLoaderStatistics stats;

    protected List<IDataLoaderFilter> filters;

    protected Map<String, IColumnFilter> columnFilters;

    public void open(BufferedReader reader) throws IOException {
        csvReader = new CsvReader(reader);
        csvReader.setEscapeMode(CsvReader.ESCAPE_MODE_BACKSLASH);
        csvReader.setSafetySwitch(false);
        context = new DataLoaderContext();
        stats = new DataLoaderStatistics();
    }

    public void open(BufferedReader reader, List<IDataLoaderFilter> filters, Map<String, IColumnFilter> columnFilters) throws IOException {
        open(reader);
        this.filters = filters;
        this.columnFilters = columnFilters;
    }

    public boolean hasNext() throws IOException {
        while (csvReader.readRecord()) {
            String[] tokens = csvReader.getValues();
            if (tokens[0].equals(CsvConstants.BATCH)) {
                context.setBatchId(new Long(tokens[1]));
                stats = new DataLoaderStatistics();
                return true;
            } else if (tokens[0].equals(CsvConstants.NODEID)) {
                context.setNodeId(tokens[1]);
            } else if (tokens[0].equals(CsvConstants.VERSION)) {
                context.setVersion(tokens[1] + "." + tokens[2] + "." + tokens[3]);
            } else if (isMetaTokenParsed(tokens)) {
                continue;
            } else {
                throw new RuntimeException("Unexpected token '" + tokens[0] + "' while parsing for next batch");
            }
        }
        return false;
    }

    public void skip() throws IOException {
        context.setSkipping(true);
        load();
    }

    public boolean load() throws IOException {
        try {
            long rowsBeforeCommit = parameterService.getLong(ParameterConstants.DATA_LOADER_MAX_ROWS_BEFORE_COMMIT);
            long rowsProcessed = 0;
            prepareTableForDataLoad();
            while (csvReader.readRecord()) {
                String[] tokens = csvReader.getValues();
                stats.incrementLineCount();
                if (tokens != null && tokens.length > 0 && tokens[0] != null) {
                    stats.incrementByteCount(csvReader.getRawRecord().length());
                    if (tokens[0].equals(CsvConstants.INSERT)) {
                        if (context.getTableTemplate() == null) {
                            throw new IllegalStateException(ErrorConstants.METADATA_MISSING);
                        } else if (!context.getTableTemplate().isIgnoreThisTable() && !context.isSkipping()) {
                            insert(tokens);
                            rowsProcessed++;
                        }
                    } else if (tokens[0].equals(CsvConstants.UPDATE)) {
                        if (context.getTableTemplate() == null) {
                            throw new IllegalStateException(ErrorConstants.METADATA_MISSING);
                        } else if (!context.getTableTemplate().isIgnoreThisTable() && !context.isSkipping()) {
                            update(tokens);
                            rowsProcessed++;
                        }
                    } else if (tokens[0].equals(CsvConstants.DELETE)) {
                        if (context.getTableTemplate() == null) {
                            throw new IllegalStateException(ErrorConstants.METADATA_MISSING);
                        } else if (!context.getTableTemplate().isIgnoreThisTable() && !context.isSkipping()) {
                            delete(tokens);
                            rowsProcessed++;
                        }
                    } else if (tokens[0].equals(CsvConstants.OLD)) {
                        context.setOldData((String[]) ArrayUtils.subarray(tokens, 1, tokens.length));
                    } else if (isMetaTokenParsed(tokens)) {
                        continue;
                    } else if (tokens[0].equals(CsvConstants.COMMIT)) {
                        rowsProcessed = 0;
                        break;
                    } else if (tokens[0].equals(CsvConstants.SQL)) {
                        if ((context.getTableTemplate() == null || !context.getTableTemplate().isIgnoreThisTable()) && !context.isSkipping()) {
                            runSql(tokens[1]);
                            rowsProcessed++;
                        }
                    } else if (tokens[0].equals(CsvConstants.CREATE)) {
                        if (!context.isSkipping()) {
                            runDdl(tokens[1]);
                            rowsProcessed++;
                        }
                    } else {
                        logger.warn("Unexpected token '" + tokens[0] + "' on line " + stats.getLineCount() + " of batch " + context.getBatchId());
                    }
                }
                if (rowsProcessed > rowsBeforeCommit && rowsBeforeCommit > 0) {
                    return false;
                }
            }
            return true;
        } finally {
            cleanupAfterDataLoad();
        }
    }

    protected boolean isMetaTokenParsed(String[] tokens) {
        boolean isMetaTokenParsed = true;
        if (tokens[0].equals(CsvConstants.TABLE)) {
            setTable(tokens[1]);
        } else if (tokens[0].equals(CsvConstants.KEYS)) {
            context.setKeyNames((String[]) ArrayUtils.subarray(tokens, 1, tokens.length));
        } else if (tokens[0].equals(CsvConstants.COLUMNS)) {
            context.setColumnNames((String[]) ArrayUtils.subarray(tokens, 1, tokens.length));
        } else if (tokens[0].equals(CsvConstants.BINARY)) {
            context.setBinaryEncodingType(tokens[1]);
        } else if (tokens[0].equals(CsvConstants.CHANNEL)) {
            context.setChannelId(tokens[1]);
        } else {
            isMetaTokenParsed = false;
        }
        return isMetaTokenParsed;
    }

    protected void setTable(String tableName) {
        cleanupAfterDataLoad();
        context.setTableName(tableName);
        if (context.getTableTemplate() == null) {
            String schema = null;
            String catalog = null;
            if (parameterService.is(ParameterConstants.DATA_LOADER_LOOKUP_TARGET_SCHEMA)) {
                Node sourceNode = nodeService.findNode(context.getNodeId());
                Node targetNode = nodeService.findIdentity();
                if (sourceNode != null) {
                    Trigger trigger = null;
                    if (targetNode == null) {
                        trigger = configurationService.getTriggerFor(tableName, sourceNode.getNodeGroupId());
                    } else {
                        trigger = configurationService.getTriggerForTarget(tableName, sourceNode.getNodeGroupId(), targetNode.getNodeGroupId(), context.getChannelId());
                        if (trigger != null && !StringUtils.isBlank(trigger.getTargetTableName())) {
                            tableName = trigger.getTargetTableName();
                        }
                        if (trigger != null && !StringUtils.isBlank(trigger.getTargetSchemaName())) {
                            schema = trigger.getTargetSchemaName();
                        }
                        if (trigger != null && !StringUtils.isBlank(trigger.getTargetCatalogName())) {
                            catalog = trigger.getTargetCatalogName();
                        }
                    }
                }
            }
            context.setTableTemplate(new TableTemplate(jdbcTemplate, dbDialect, tableName, this.columnFilters != null ? this.columnFilters.get(tableName) : null, parameterService.is(ParameterConstants.DATA_LOADER_NO_KEYS_IN_UPDATE), schema, catalog));
        }
        prepareTableForDataLoad();
    }

    protected void prepareTableForDataLoad() {
        if (context != null && context.getTableTemplate() != null) {
            dbDialect.prepareTableForDataLoad(context.getTableTemplate().getTable());
        }
    }

    protected void cleanupAfterDataLoad() {
        if (context != null && context.getTableTemplate() != null) {
            dbDialect.cleanupAfterDataLoad(context.getTableTemplate().getTable());
        }
    }

    protected int insert(String[] tokens) {
        stats.incrementStatementCount();
        String[] columnValues = parseColumns(tokens, 1);
        int rows = 0;
        boolean continueToLoad = true;
        if (filters != null) {
            stats.startTimer();
            for (IDataLoaderFilter filter : filters) {
                continueToLoad &= filter.filterInsert(context, columnValues);
            }
            stats.incrementFilterMillis(stats.endTimer());
        }
        if (continueToLoad) {
            boolean enableFallbackUpdate = parameterService.is(ParameterConstants.DATA_LOADER_ENABLE_FALLBACK_UPDATE);
            Object savepoint = null;
            try {
                stats.startTimer();
                if (enableFallbackUpdate) {
                    savepoint = dbDialect.createSavepointForFallback();
                }
                rows = context.getTableTemplate().insert(context, columnValues);
                dbDialect.releaseSavepoint(savepoint);
            } catch (DataIntegrityViolationException e) {
                if (enableFallbackUpdate) {
                    dbDialect.rollbackToSavepoint(savepoint);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Unable to insert into " + context.getTableName() + ", updating instead: " + ArrayUtils.toString(tokens));
                    }
                    String keyValues[] = parseKeys(tokens, 1);
                    stats.incrementFallbackUpdateCount();
                    rows = context.getTableTemplate().update(context, columnValues, keyValues);
                    if (rows == 0) {
                        throw new RuntimeException("Unable to update " + context.getTableName() + ": " + ArrayUtils.toString(tokens), e);
                    }
                } else {
                    throw e;
                }
            } finally {
                stats.incrementDatabaseMillis(stats.endTimer());
            }
        }
        return rows;
    }

    protected int update(String[] tokens) {
        stats.incrementStatementCount();
        String columnValues[] = parseColumns(tokens, 1);
        String keyValues[] = parseKeys(tokens, 1 + columnValues.length);
        int rows = 0;
        boolean continueToLoad = true;
        if (filters != null) {
            stats.startTimer();
            for (IDataLoaderFilter filter : filters) {
                continueToLoad &= filter.filterUpdate(context, columnValues, keyValues);
            }
            stats.incrementFilterMillis(stats.endTimer());
        }
        if (continueToLoad) {
            boolean enableFallbackInsert = parameterService.is(ParameterConstants.DATA_LOADER_ENABLE_FALLBACK_INSERT);
            stats.startTimer();
            rows = context.getTableTemplate().update(context, columnValues, keyValues);
            if (rows == 0) {
                if (enableFallbackInsert) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Unable to update " + context.getTableName() + ", inserting instead: " + ArrayUtils.toString(tokens));
                    }
                    stats.incrementFallbackInsertCount();
                    rows = context.getTableTemplate().insert(context, columnValues);
                } else {
                    stats.incrementDatabaseMillis(stats.endTimer());
                    throw new RuntimeException("Unable to update " + context.getTableName() + ": " + ArrayUtils.toString(tokens));
                }
            } else if (rows > 1) {
                logger.warn("Too many rows (" + rows + ") updated for " + context.getTableName() + ": " + ArrayUtils.toString(tokens));
            }
            stats.incrementDatabaseMillis(stats.endTimer());
        }
        return rows;
    }

    protected int delete(String[] tokens) {
        stats.incrementStatementCount();
        String keyValues[] = parseKeys(tokens, 1);
        int rows = 0;
        boolean continueToLoad = true;
        if (filters != null) {
            stats.startTimer();
            for (IDataLoaderFilter filter : filters) {
                continueToLoad &= filter.filterDelete(context, keyValues);
            }
            stats.incrementFilterMillis(stats.endTimer());
        }
        if (continueToLoad) {
            boolean allowMissingDelete = parameterService.is(ParameterConstants.DATA_LOADER_ALLOW_MISSING_DELETE);
            stats.startTimer();
            rows = context.getTableTemplate().delete(context, keyValues);
            stats.incrementDatabaseMillis(stats.endTimer());
            if (rows == 0) {
                if (allowMissingDelete) {
                    logger.warn("Delete of " + context.getTableName() + " affected no rows: " + ArrayUtils.toString(tokens));
                    stats.incrementMissingDeleteCount();
                } else {
                    throw new RuntimeException("Delete of " + context.getTableName() + " affected no rows: " + ArrayUtils.toString(tokens));
                }
            }
        }
        return rows;
    }

    protected void runSql(String sql) {
        stats.incrementStatementCount();
        if (logger.isDebugEnabled()) {
            logger.debug("Running SQL: " + sql);
        }
        jdbcTemplate.execute(sql);
        if (context.getTableTemplate() != null) {
            context.getTableTemplate().resetMetaData(false);
        }
    }

    protected void runDdl(String xml) {
        stats.incrementStatementCount();
        if (logger.isDebugEnabled()) {
            logger.debug("Running DDL: " + xml);
        }
        dbDialect.createTables(xml);
        if (context.getTableTemplate() != null) {
            context.getTableTemplate().resetMetaData(false);
        }
    }

    protected String[] parseKeys(String[] tokens, int startIndex) {
        if (context.getTableTemplate().getKeyNames() == null) {
            throw new RuntimeException("Key names were not specified for table " + context.getTableTemplate().getTableName());
        }
        int keyLength = context.getTableTemplate().getKeyNames().length;
        return parseValues("key", tokens, startIndex, startIndex + keyLength);
    }

    protected String[] parseColumns(String[] tokens, int startIndex) {
        if (context.getTableTemplate().getColumnNames() == null) {
            throw new RuntimeException("Column names were not specified for table " + context.getTableTemplate().getTableName());
        }
        int columnLength = context.getTableTemplate().getColumnNames().length;
        return parseValues("column", tokens, startIndex, startIndex + columnLength);
    }

    protected String[] parseValues(String name, String[] tokens, int startIndex, int endIndex) {
        if (tokens.length < endIndex) {
            throw new RuntimeException("Expected to have " + (endIndex - startIndex) + " " + name + " values for " + context.getTableTemplate().getTableName() + ": " + ArrayUtils.toString(tokens));
        }
        return (String[]) ArrayUtils.subarray(tokens, startIndex, endIndex);
    }

    public IDataLoader clone() {
        CsvLoader dataLoader = new CsvLoader();
        dataLoader.setJdbcTemplate(jdbcTemplate);
        dataLoader.setDbDialect(dbDialect);
        dataLoader.setParameterService(parameterService);
        dataLoader.setConfigurationService(configurationService);
        dataLoader.setNodeService(nodeService);
        return dataLoader;
    }

    public void close() {
        if (csvReader != null) {
            csvReader.close();
        }
    }

    public IDataLoaderContext getContext() {
        return context;
    }

    public IDataLoaderStatistics getStatistics() {
        return stats;
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setDbDialect(IDbDialect dbDialect) {
        this.dbDialect = dbDialect;
    }

    public void setParameterService(IParameterService parameterService) {
        this.parameterService = parameterService;
    }

    public void setConfigurationService(IConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public void setNodeService(INodeService nodeService) {
        this.nodeService = nodeService;
    }
}
