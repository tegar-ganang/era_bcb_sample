package org.hsqldb;

import org.hsqldb.HsqlNameManager.HsqlName;
import org.hsqldb.error.Error;
import org.hsqldb.error.ErrorCode;
import org.hsqldb.index.Index;
import org.hsqldb.lib.java.JavaSystem;
import org.hsqldb.persist.HsqlDatabaseProperties;
import org.hsqldb.persist.HsqlProperties;
import org.hsqldb.result.Result;
import org.hsqldb.rights.User;
import org.hsqldb.scriptio.ScriptWriterText;

/**
 * Implementation of Statement for SQL commands.<p>
 *
 * @author Fred Toussi (fredt@users dot sourceforge.net)
 * @version 2.0.1
 * @since 1.9.0
 */
public class StatementCommand extends Statement {

    Object[] parameters;

    StatementCommand(int type, Object[] args) {
        this(type, args, null, null);
    }

    StatementCommand(int type, Object[] args, HsqlName[] readNames, HsqlName[] writeNames) {
        super(type);
        this.isTransactionStatement = true;
        this.parameters = args;
        if (readNames != null) {
            this.readTableNames = readNames;
        }
        if (writeNames != null) {
            this.writeTableNames = writeNames;
        }
        switch(type) {
            case StatementTypes.DATABASE_CHECKPOINT:
                group = StatementTypes.X_HSQLDB_DATABASE_OPERATION;
                isLogged = false;
                break;
            case StatementTypes.DATABASE_BACKUP:
            case StatementTypes.DATABASE_SCRIPT:
                group = StatementTypes.X_HSQLDB_DATABASE_OPERATION;
                isLogged = false;
                break;
            case StatementTypes.SET_DATABASE_UNIQUE_NAME:
            case StatementTypes.SET_DATABASE_FILES_WRITE_DELAY:
            case StatementTypes.SET_DATABASE_FILES_TEMP_PATH:
                this.isTransactionStatement = false;
                group = StatementTypes.X_HSQLDB_SETTING;
                break;
            case StatementTypes.SET_DATABASE_DEFAULT_INITIAL_SCHEMA:
            case StatementTypes.SET_DATABASE_DEFAULT_TABLE_TYPE:
            case StatementTypes.SET_DATABASE_FILES_CACHE_ROWS:
            case StatementTypes.SET_DATABASE_FILES_CACHE_SIZE:
            case StatementTypes.SET_DATABASE_FILES_SCALE:
            case StatementTypes.SET_DATABASE_FILES_DEFRAG:
            case StatementTypes.SET_DATABASE_FILES_EVENT_LOG:
            case StatementTypes.SET_DATABASE_FILES_LOBS_SCALE:
            case StatementTypes.SET_DATABASE_FILES_LOCK:
            case StatementTypes.SET_DATABASE_FILES_LOG:
            case StatementTypes.SET_DATABASE_FILES_LOG_SIZE:
            case StatementTypes.SET_DATABASE_FILES_NIO:
            case StatementTypes.SET_DATABASE_FILES_SCRIPT_FORMAT:
            case StatementTypes.SET_DATABASE_AUTHENTICATION:
            case StatementTypes.SET_DATABASE_PASSWORD_CHECK:
            case StatementTypes.SET_DATABASE_PROPERTY:
            case StatementTypes.SET_DATABASE_RESULT_MEMORY_ROWS:
            case StatementTypes.SET_DATABASE_SQL_REFERENTIAL_INTEGRITY:
            case StatementTypes.SET_DATABASE_SQL_STRICT:
            case StatementTypes.SET_DATABASE_TRANSACTION_CONTROL:
            case StatementTypes.SET_DATABASE_DEFAULT_ISOLATION_LEVEL:
            case StatementTypes.SET_DATABASE_GC:
            case StatementTypes.SET_DATABASE_SQL_COLLATION:
            case StatementTypes.SET_DATABASE_FILES_BACKUP_INCREMENT:
            case StatementTypes.SET_DATABASE_TEXT_SOURCE:
                group = StatementTypes.X_HSQLDB_SETTING;
                this.isTransactionStatement = true;
                break;
            case StatementTypes.SET_TABLE_CLUSTERED:
                group = StatementTypes.X_HSQLDB_SCHEMA_MANIPULATION;
                break;
            case StatementTypes.SET_TABLE_SOURCE_HEADER:
                isLogged = false;
            case StatementTypes.SET_TABLE_SOURCE:
                metaDataImpact = Statement.META_RESET_VIEWS;
                group = StatementTypes.X_HSQLDB_SCHEMA_MANIPULATION;
                this.isTransactionStatement = true;
                break;
            case StatementTypes.SET_TABLE_READONLY:
                metaDataImpact = Statement.META_RESET_VIEWS;
                group = StatementTypes.X_HSQLDB_SCHEMA_MANIPULATION;
                this.isTransactionStatement = true;
                break;
            case StatementTypes.DATABASE_SHUTDOWN:
                isLogged = false;
                group = StatementTypes.X_HSQLDB_DATABASE_OPERATION;
                this.isTransactionStatement = false;
                break;
            case StatementTypes.SET_TABLE_TYPE:
                group = StatementTypes.X_HSQLDB_SCHEMA_MANIPULATION;
                this.isTransactionStatement = true;
                break;
            case StatementTypes.SET_TABLE_INDEX:
                group = StatementTypes.X_HSQLDB_SETTING;
                this.isTransactionStatement = false;
                isLogged = false;
                break;
            case StatementTypes.SET_USER_LOCAL:
            case StatementTypes.SET_USER_INITIAL_SCHEMA:
            case StatementTypes.SET_USER_PASSWORD:
                group = StatementTypes.X_HSQLDB_SETTING;
                this.isTransactionStatement = false;
                break;
            case StatementTypes.ALTER_SESSION:
                group = StatementTypes.X_HSQLDB_SESSION;
                this.isTransactionStatement = false;
                break;
            default:
                throw Error.runtimeError(ErrorCode.U_S0500, "StatementCommand");
        }
    }

    public Result execute(Session session) {
        Result result;
        try {
            result = getResult(session);
        } catch (Throwable t) {
            result = Result.newErrorResult(t, null);
        }
        if (result.isError()) {
            result.getException().setStatementType(group, type);
            return result;
        }
        try {
            if (isLogged) {
                session.database.logger.writeOtherStatement(session, sql);
            }
        } catch (Throwable e) {
            return Result.newErrorResult(e, sql);
        }
        return result;
    }

    Result getResult(Session session) {
        if (this.isExplain) {
            return Result.newSingleColumnStringResult("OPERATION", describe(session));
        }
        switch(type) {
            case StatementTypes.DATABASE_BACKUP:
                {
                    String path = ((String) parameters[0]);
                    boolean blocking = ((Boolean) parameters[1]).booleanValue();
                    boolean script = ((Boolean) parameters[2]).booleanValue();
                    boolean compressed = ((Boolean) parameters[3]).booleanValue();
                    try {
                        session.checkAdmin();
                        if (!session.database.getType().equals(DatabaseURL.S_FILE)) {
                            return Result.newErrorResult(Error.error(ErrorCode.DATABASE_IS_NON_FILE));
                        }
                        if (session.database.isReadOnly()) {
                            return Result.newErrorResult(Error.error(ErrorCode.DATABASE_IS_MEMORY_ONLY), null);
                        }
                        if (session.database.logger.isStoredFileAccess) {
                            return Result.newErrorResult(Error.error(ErrorCode.DATABASE_IS_NON_FILE), null);
                        }
                        session.database.logger.backup(path, script, blocking, compressed);
                        return Result.updateZeroResult;
                    } catch (HsqlException e) {
                        return Result.newErrorResult(e, sql);
                    }
                }
            case StatementTypes.DATABASE_CHECKPOINT:
                {
                    boolean defrag = ((Boolean) parameters[0]).booleanValue();
                    session.database.lobManager.lock();
                    try {
                        session.checkAdmin();
                        session.checkDDLWrite();
                        session.database.logger.checkpoint(defrag);
                        return Result.updateZeroResult;
                    } catch (HsqlException e) {
                        return Result.newErrorResult(e, sql);
                    } finally {
                        session.database.lobManager.unlock();
                    }
                }
            case StatementTypes.SET_DATABASE_FILES_BACKUP_INCREMENT:
                {
                    try {
                        boolean mode = ((Boolean) parameters[0]).booleanValue();
                        session.checkAdmin();
                        session.checkDDLWrite();
                        session.database.logger.setIncrementBackup(mode);
                        return Result.updateZeroResult;
                    } catch (HsqlException e) {
                        return Result.newErrorResult(e, sql);
                    }
                }
            case StatementTypes.SET_DATABASE_FILES_CACHE_ROWS:
                {
                    try {
                        int value = ((Integer) parameters[0]).intValue();
                        session.checkAdmin();
                        session.checkDDLWrite();
                        session.database.logger.setCacheMaxRows(value);
                        return Result.updateZeroResult;
                    } catch (HsqlException e) {
                        return Result.newErrorResult(e, sql);
                    }
                }
            case StatementTypes.SET_DATABASE_FILES_CACHE_SIZE:
                {
                    try {
                        int value = ((Integer) parameters[0]).intValue();
                        session.checkAdmin();
                        session.checkDDLWrite();
                        session.database.logger.setCacheSize(value);
                        return Result.updateZeroResult;
                    } catch (HsqlException e) {
                        return Result.newErrorResult(e, sql);
                    }
                }
            case StatementTypes.SET_DATABASE_FILES_LOBS_SCALE:
                {
                    try {
                        int value = ((Integer) parameters[0]).intValue();
                        session.checkAdmin();
                        session.checkDDLWrite();
                        if (session.isProcessingScript) {
                            session.database.logger.setLobFileScaleNoCheck(value);
                        } else {
                            session.database.logger.setLobFileScale(value);
                        }
                        return Result.updateZeroResult;
                    } catch (HsqlException e) {
                        return Result.newErrorResult(e, sql);
                    }
                }
            case StatementTypes.SET_DATABASE_FILES_SCALE:
                {
                    try {
                        int value = ((Integer) parameters[0]).intValue();
                        session.checkAdmin();
                        session.checkDDLWrite();
                        if (session.isProcessingScript) {
                            session.database.logger.setCacheFileScaleNoCheck(value);
                        } else {
                            session.database.logger.setCacheFileScale(value);
                        }
                        return Result.updateZeroResult;
                    } catch (HsqlException e) {
                        return Result.newErrorResult(e, sql);
                    }
                }
            case StatementTypes.SET_DATABASE_FILES_DEFRAG:
                {
                    try {
                        int value = ((Integer) parameters[0]).intValue();
                        session.checkAdmin();
                        session.checkDDLWrite();
                        session.database.logger.setDefagLimit(value);
                        return Result.updateZeroResult;
                    } catch (HsqlException e) {
                        return Result.newErrorResult(e, sql);
                    }
                }
            case StatementTypes.SET_DATABASE_FILES_EVENT_LOG:
                {
                    try {
                        int value = ((Integer) parameters[0]).intValue();
                        session.checkAdmin();
                        session.checkDDLWrite();
                        session.database.logger.setEventLogLevel(value);
                        return Result.updateZeroResult;
                    } catch (HsqlException e) {
                        return Result.newErrorResult(e, sql);
                    }
                }
            case StatementTypes.SET_DATABASE_FILES_LOCK:
                {
                    try {
                        return Result.updateZeroResult;
                    } catch (HsqlException e) {
                        return Result.newErrorResult(e, sql);
                    }
                }
            case StatementTypes.SET_DATABASE_FILES_NIO:
                {
                    try {
                        session.checkAdmin();
                        session.checkDDLWrite();
                        Object v = parameters[0];
                        if (v instanceof Boolean) {
                            boolean value = ((Boolean) parameters[0]).booleanValue();
                            session.database.logger.setNioDataFile(value);
                        } else {
                            int value = ((Integer) parameters[0]).intValue();
                            session.database.logger.setNioMaxSize(value);
                        }
                        return Result.updateZeroResult;
                    } catch (HsqlException e) {
                        return Result.newErrorResult(e, sql);
                    }
                }
            case StatementTypes.SET_DATABASE_FILES_LOG:
                {
                    try {
                        boolean value = ((Boolean) parameters[0]).booleanValue();
                        session.checkAdmin();
                        session.checkDDLWrite();
                        session.database.logger.setLogData(value);
                        return Result.updateZeroResult;
                    } catch (HsqlException e) {
                        return Result.newErrorResult(e, sql);
                    }
                }
            case StatementTypes.SET_DATABASE_FILES_LOG_SIZE:
                {
                    try {
                        int value = ((Integer) parameters[0]).intValue();
                        session.checkAdmin();
                        session.checkDDLWrite();
                        session.database.logger.setLogSize(value);
                        return Result.updateZeroResult;
                    } catch (HsqlException e) {
                        return Result.newErrorResult(e, sql);
                    }
                }
            case StatementTypes.SET_DATABASE_FILES_TEMP_PATH:
                {
                    try {
                        String value = (String) parameters[0];
                        session.checkAdmin();
                        session.checkDDLWrite();
                        return Result.updateZeroResult;
                    } catch (HsqlException e) {
                        return Result.newErrorResult(e, sql);
                    }
                }
            case StatementTypes.SET_DATABASE_FILES_SCRIPT_FORMAT:
                {
                    try {
                        int value = ((Integer) parameters[0]).intValue();
                        session.checkAdmin();
                        session.checkDDLWrite();
                        session.database.logger.setScriptType(value);
                        return Result.updateZeroResult;
                    } catch (HsqlException e) {
                        return Result.newErrorResult(e, sql);
                    }
                }
            case StatementTypes.SET_DATABASE_FILES_WRITE_DELAY:
                {
                    try {
                        int value = ((Integer) parameters[0]).intValue();
                        session.checkAdmin();
                        session.checkDDLWrite();
                        session.database.logger.setWriteDelay(value);
                        return Result.updateZeroResult;
                    } catch (HsqlException e) {
                        return Result.newErrorResult(e, sql);
                    }
                }
            case StatementTypes.SET_DATABASE_AUTHENTICATION:
                {
                    try {
                        Routine routine = (Routine) parameters[0];
                        session.checkAdmin();
                        session.checkDDLWrite();
                        session.database.userManager.setExtAuthenticationFunction(routine);
                        return Result.updateZeroResult;
                    } catch (HsqlException e) {
                        return Result.newErrorResult(e, sql);
                    }
                }
            case StatementTypes.SET_DATABASE_PASSWORD_CHECK:
                {
                    try {
                        Routine routine = (Routine) parameters[0];
                        session.checkAdmin();
                        session.checkDDLWrite();
                        session.database.userManager.setPasswordCheckFunction(routine);
                        return Result.updateZeroResult;
                    } catch (HsqlException e) {
                        return Result.newErrorResult(e, sql);
                    }
                }
            case StatementTypes.SET_DATABASE_SQL_COLLATION:
                {
                    try {
                        String name = (String) parameters[0];
                        session.checkAdmin();
                        session.checkDDLWrite();
                        session.database.collation.setCollation(name);
                        session.database.schemaManager.setSchemaChangeTimestamp();
                        return Result.updateZeroResult;
                    } catch (HsqlException e) {
                        return Result.newErrorResult(e, sql);
                    }
                }
            case StatementTypes.SET_DATABASE_SQL_REFERENTIAL_INTEGRITY:
                {
                    boolean mode = ((Boolean) parameters[0]).booleanValue();
                    session.checkAdmin();
                    session.checkDDLWrite();
                    session.database.setReferentialIntegrity(mode);
                    return Result.updateZeroResult;
                }
            case StatementTypes.SET_DATABASE_SQL_STRICT:
                {
                    String property = (String) parameters[0];
                    boolean mode = ((Boolean) parameters[1]).booleanValue();
                    session.checkAdmin();
                    session.checkDDLWrite();
                    if (property == HsqlDatabaseProperties.sql_enforce_names) {
                        session.database.setStrictNames(mode);
                    } else if (property == HsqlDatabaseProperties.sql_enforce_size) {
                        session.database.setStrictColumnSize(mode);
                    } else if (property == HsqlDatabaseProperties.sql_enforce_types) {
                        session.database.setStrictTypes(mode);
                    } else if (property == HsqlDatabaseProperties.sql_enforce_refs) {
                        session.database.setStrictReferences(mode);
                    } else if (property == HsqlDatabaseProperties.sql_enforce_tdcd) {
                        session.database.setStrictTDCD(mode);
                    } else if (property == HsqlDatabaseProperties.sql_enforce_tdcu) {
                        session.database.setStrictTDCU(mode);
                    } else if (property == HsqlDatabaseProperties.jdbc_translate_tti_types) {
                        session.database.setTranslateTTI(mode);
                    } else if (property == HsqlDatabaseProperties.sql_concat_nulls) {
                        session.database.setConcatNulls(mode);
                    } else if (property == HsqlDatabaseProperties.sql_unique_nulls) {
                        session.database.setUniqueNulls(mode);
                    } else if (property == HsqlDatabaseProperties.sql_convert_trunc) {
                        session.database.setConvertTrunc(mode);
                    } else if (property == HsqlDatabaseProperties.sql_syntax_ora) {
                        session.database.setSyntaxOra(mode);
                    }
                    return Result.updateZeroResult;
                }
            case StatementTypes.SET_DATABASE_DEFAULT_INITIAL_SCHEMA:
                {
                    HsqlName schema = (HsqlName) parameters[0];
                    session.checkAdmin();
                    session.checkDDLWrite();
                    session.database.schemaManager.setDefaultSchemaHsqlName(schema);
                    session.database.schemaManager.setSchemaChangeTimestamp();
                    return Result.updateZeroResult;
                }
            case StatementTypes.SET_DATABASE_DEFAULT_TABLE_TYPE:
                {
                    Integer type = (Integer) parameters[0];
                    session.checkAdmin();
                    session.checkDDLWrite();
                    session.database.schemaManager.setDefaultTableType(type.intValue());
                    return Result.updateZeroResult;
                }
            case StatementTypes.SET_DATABASE_TRANSACTION_CONTROL:
                {
                    try {
                        int mode = ((Integer) parameters[0]).intValue();
                        session.checkAdmin();
                        session.database.txManager.setTransactionControl(session, mode);
                        return Result.updateZeroResult;
                    } catch (HsqlException e) {
                        return Result.newErrorResult(e, sql);
                    }
                }
            case StatementTypes.SET_DATABASE_DEFAULT_ISOLATION_LEVEL:
                {
                    try {
                        int mode = ((Integer) parameters[0]).intValue();
                        session.checkAdmin();
                        session.database.defaultIsolationLevel = mode;
                        return Result.updateZeroResult;
                    } catch (HsqlException e) {
                        return Result.newErrorResult(e, sql);
                    }
                }
            case StatementTypes.SET_DATABASE_GC:
                {
                    try {
                        int count = ((Integer) parameters[0]).intValue();
                        session.checkAdmin();
                        JavaSystem.gcFrequency = count;
                        return Result.updateZeroResult;
                    } catch (HsqlException e) {
                        return Result.newErrorResult(e, sql);
                    }
                }
            case StatementTypes.SET_DATABASE_PROPERTY:
                {
                    try {
                        String property = (String) parameters[0];
                        Object value = parameters[1];
                        session.checkAdmin();
                        session.checkDDLWrite();
                        return Result.updateZeroResult;
                    } catch (HsqlException e) {
                        return Result.newErrorResult(e, sql);
                    }
                }
            case StatementTypes.SET_DATABASE_RESULT_MEMORY_ROWS:
                {
                    int size = ((Integer) parameters[0]).intValue();
                    session.checkAdmin();
                    session.database.getProperties().setProperty(HsqlDatabaseProperties.hsqldb_result_max_memory_rows, size);
                    session.database.setResultMaxMemoryRows(size);
                    return Result.updateZeroResult;
                }
            case StatementTypes.SET_DATABASE_TEXT_SOURCE:
                {
                    try {
                        String source = (String) parameters[0];
                        HsqlProperties props = null;
                        session.checkAdmin();
                        if (source.length() > 0) {
                            props = HsqlProperties.delimitedArgPairsToProps(source, "=", ";", null);
                            if (props.getErrorKeys().length > 0) {
                                throw Error.error(ErrorCode.TEXT_TABLE_SOURCE, props.getErrorKeys()[0]);
                            }
                        }
                        session.database.logger.setDefaultTextTableProperties(source, props);
                        return Result.updateZeroResult;
                    } catch (HsqlException e) {
                        return Result.newErrorResult(e, sql);
                    }
                }
            case StatementTypes.SET_DATABASE_UNIQUE_NAME:
                {
                    try {
                        String name = (String) parameters[0];
                        session.checkAdmin();
                        session.database.setUniqueName(name);
                        return Result.updateZeroResult;
                    } catch (HsqlException e) {
                        return Result.newErrorResult(e, sql);
                    }
                }
            case StatementTypes.DATABASE_SCRIPT:
                {
                    ScriptWriterText dsw = null;
                    String name = (String) parameters[0];
                    try {
                        session.checkAdmin();
                        if (name == null) {
                            return session.database.getScript(false);
                        } else {
                            dsw = new ScriptWriterText(session.database, name, true, true, true);
                            dsw.writeAll();
                            dsw.close();
                            return Result.updateZeroResult;
                        }
                    } catch (HsqlException e) {
                        return Result.newErrorResult(e, sql);
                    }
                }
            case StatementTypes.DATABASE_SHUTDOWN:
                {
                    try {
                        int mode = ((Integer) parameters[0]).intValue();
                        session.checkAdmin();
                        session.database.close(mode);
                        return Result.updateZeroResult;
                    } catch (HsqlException e) {
                        return Result.newErrorResult(e, sql);
                    }
                }
            case StatementTypes.SET_TABLE_CLUSTERED:
                {
                    try {
                        HsqlName name = (HsqlName) parameters[0];
                        int[] colIndex = (int[]) parameters[1];
                        Table table = session.database.schemaManager.getTable(session, name.name, name.schema.name);
                        StatementSchema.checkSchemaUpdateAuthorisation(session, table.getSchemaName());
                        if (!table.isCached() && !table.isText()) {
                            throw Error.error(ErrorCode.ACCESS_IS_DENIED);
                        }
                        Index index = table.getIndexForColumns(session, colIndex);
                        if (index != null) {
                            Index[] indexes = table.getIndexList();
                            for (int i = 0; i < indexes.length; i++) {
                                indexes[i].setClustered(false);
                            }
                            index.setClustered(true);
                        }
                        return Result.updateZeroResult;
                    } catch (HsqlException e) {
                        return Result.newErrorResult(e, sql);
                    }
                }
            case StatementTypes.SET_TABLE_INDEX:
                {
                    try {
                        HsqlName name = (HsqlName) parameters[0];
                        String value = (String) parameters[1];
                        Table table = session.database.schemaManager.getTable(session, name.name, name.schema.name);
                        if (session.isProcessingScript()) {
                            table.setIndexRoots(session, value);
                        }
                        return Result.updateZeroResult;
                    } catch (HsqlException e) {
                        return Result.newErrorResult(e, sql);
                    }
                }
            case StatementTypes.SET_TABLE_READONLY:
                {
                    try {
                        HsqlName name = (HsqlName) parameters[0];
                        Table table = session.database.schemaManager.getTable(session, name.name, name.schema.name);
                        boolean mode = ((Boolean) parameters[1]).booleanValue();
                        StatementSchema.checkSchemaUpdateAuthorisation(session, table.getSchemaName());
                        table.setDataReadOnly(mode);
                        session.database.schemaManager.setSchemaChangeTimestamp();
                        return Result.updateZeroResult;
                    } catch (HsqlException e) {
                        return Result.newErrorResult(e, sql);
                    }
                }
            case StatementTypes.SET_TABLE_SOURCE:
            case StatementTypes.SET_TABLE_SOURCE_HEADER:
                {
                    try {
                        HsqlName name = (HsqlName) parameters[0];
                        Table table = session.database.schemaManager.getTable(session, name.name, name.schema.name);
                        StatementSchema.checkSchemaUpdateAuthorisation(session, table.getSchemaName());
                        if (!table.isText()) {
                            Exception e = Error.error(ErrorCode.X_S0522);
                            return Result.newErrorResult(e, sql);
                        }
                        if (parameters[1] != null) {
                            boolean mode = ((Boolean) parameters[1]).booleanValue();
                            if (mode) {
                                ((TextTable) table).connect(session);
                            } else {
                                ((TextTable) table).disconnect();
                            }
                            session.database.schemaManager.setSchemaChangeTimestamp();
                            return Result.updateZeroResult;
                        }
                        String source = (String) parameters[2];
                        boolean isDesc = ((Boolean) parameters[3]).booleanValue();
                        boolean isHeader = ((Boolean) parameters[4]).booleanValue();
                        if (isHeader) {
                            ((TextTable) table).setHeader(source);
                        } else {
                            ((TextTable) table).setDataSource(session, source, isDesc, false);
                        }
                        return Result.updateZeroResult;
                    } catch (Throwable e) {
                        if (!(e instanceof HsqlException)) {
                            e = Error.error(ErrorCode.GENERAL_IO_ERROR, e.getMessage());
                        }
                        if (session.isProcessingLog() || session.isProcessingScript()) {
                            session.addWarning((HsqlException) e);
                            session.database.logger.logWarningEvent("Problem processing SET TABLE SOURCE", e);
                            return Result.updateZeroResult;
                        } else {
                            return Result.newErrorResult(e, sql);
                        }
                    }
                }
            case StatementTypes.SET_TABLE_TYPE:
                {
                    try {
                        HsqlName name = (HsqlName) parameters[0];
                        int type = ((Integer) parameters[1]).intValue();
                        Table table = session.database.schemaManager.getUserTable(session, name.name, name.schema.name);
                        if (name.schema != SqlInvariants.LOBS_SCHEMA_HSQLNAME) {
                            StatementSchema.checkSchemaUpdateAuthorisation(session, table.getSchemaName());
                        }
                        session.setScripting(true);
                        TableWorks tw = new TableWorks(session, table);
                        tw.setTableType(session, type);
                        session.database.schemaManager.setSchemaChangeTimestamp();
                        if (name.schema == SqlInvariants.LOBS_SCHEMA_HSQLNAME) {
                            session.database.lobManager.compileStatements();
                        }
                        return Result.updateZeroResult;
                    } catch (HsqlException e) {
                        return Result.newErrorResult(e, sql);
                    }
                }
            case StatementTypes.SET_USER_LOCAL:
                {
                    User user = (User) parameters[0];
                    boolean mode = ((Boolean) parameters[1]).booleanValue();
                    session.checkAdmin();
                    session.checkDDLWrite();
                    user.isLocalOnly = mode;
                    session.database.schemaManager.setSchemaChangeTimestamp();
                    return Result.updateZeroResult;
                }
            case StatementTypes.SET_USER_INITIAL_SCHEMA:
                {
                    try {
                        User user = (User) parameters[0];
                        HsqlName schema = (HsqlName) parameters[1];
                        session.checkDDLWrite();
                        if (user == null) {
                            user = session.getUser();
                        } else {
                            session.checkAdmin();
                            session.checkDDLWrite();
                            user = session.database.userManager.get(user.getName().getNameString());
                        }
                        if (schema != null) {
                            schema = session.database.schemaManager.getSchemaHsqlName(schema.name);
                        }
                        user.setInitialSchema(schema);
                        session.database.schemaManager.setSchemaChangeTimestamp();
                        return Result.updateZeroResult;
                    } catch (HsqlException e) {
                        return Result.newErrorResult(e, sql);
                    }
                }
            case StatementTypes.SET_USER_PASSWORD:
                {
                    try {
                        User user = parameters[0] == null ? session.getUser() : (User) parameters[0];
                        String password = (String) parameters[1];
                        session.checkDDLWrite();
                        session.setScripting(true);
                        session.database.userManager.setPassword(session, user, password);
                        return Result.updateZeroResult;
                    } catch (HsqlException e) {
                        return Result.newErrorResult(e, sql);
                    }
                }
            case StatementTypes.ALTER_SESSION:
                {
                    try {
                        long sessionID = ((Number) parameters[0]).longValue();
                        int action = ((Number) parameters[1]).intValue();
                        Session targetSession = session.database.sessionManager.getSession(sessionID);
                        if (targetSession == null) {
                            throw Error.error(ErrorCode.X_2E000);
                        }
                        switch(action) {
                            case Tokens.ALL:
                                targetSession.resetSession();
                                break;
                            case Tokens.TABLE:
                                targetSession.sessionData.persistentStoreCollection.clearAllTables();
                                break;
                            case Tokens.RESULT:
                                targetSession.sessionData.closeAllNavigators();
                                break;
                            case Tokens.CLOSE:
                                targetSession.abortTransaction = true;
                                targetSession.latch.setCount(0);
                                targetSession.close();
                                break;
                            case Tokens.RELEASE:
                                targetSession.abortTransaction = true;
                                targetSession.latch.setCount(0);
                                break;
                        }
                    } catch (HsqlException e) {
                        return Result.newErrorResult(e, sql);
                    }
                    return Result.updateZeroResult;
                }
            default:
                throw Error.runtimeError(ErrorCode.U_S0500, "StatemntCommand");
        }
    }

    public boolean isAutoCommitStatement() {
        return isTransactionStatement;
    }

    public String describe(Session session) {
        return sql;
    }
}
