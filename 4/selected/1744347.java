package edu.rabbit;

import java.io.File;
import java.util.Set;
import edu.rabbit.kernel.IDbHandle;
import edu.rabbit.kernel.DbUtility;
import edu.rabbit.kernel.btree.Btree;
import edu.rabbit.kernel.btree.BtreeFlags;
import edu.rabbit.kernel.btree.IBtree;
import edu.rabbit.kernel.db.DbHandle;
import edu.rabbit.kernel.fs.FileOpenPermission;
import edu.rabbit.kernel.fs.FileType;
import edu.rabbit.kernel.fs.IFile;
import edu.rabbit.kernel.mutex.IMutex;
import edu.rabbit.kernel.pager.IPager;
import edu.rabbit.kernel.schema.Schema;
import edu.rabbit.kernel.table.Options;
import edu.rabbit.kernel.table.PragmasHandler;
import edu.rabbit.kernel.table.Table;
import edu.rabbit.schema.IIndexDef;
import edu.rabbit.schema.ISchema;
import edu.rabbit.schema.ITableDef;
import edu.rabbit.schema.IVirtualTableDef;
import edu.rabbit.table.DefaultBusyHandler;
import edu.rabbit.table.IBusyHandler;
import edu.rabbit.table.IOptions;
import edu.rabbit.table.ITable;
import edu.rabbit.table.ITransaction;
import edu.rabbit.table.RunnableWithLock;

/**
 * <p>
 * Connection to database. This class currently is main entry point in Rabbit
 * API.
 * </p>
 * 
 * <p>
 * It allows to perform next tasks:
 * 
 * <ul>
 * <li>Open existed and create new SQLite database.</li>
 * <li>Get and modify database's schema.</li>
 * <li>Control transactions.</li>
 * <li>Read, search and modify data in database.</li>
 * <li>Get and set database's options.</li>
 * </ul>
 * 
 * </p>
 * 
 * @author Yuanyan<yanyan.cao@gmail.com>
 * 
 * 
 */
public class Database {

    /**
     * File name for in memory database.
     */
    public static final File IN_MEMORY = new File(IPager.MEMORY_DB);

    private static final String TRANSACTION_ALREADY_STARTED = "Transaction already started";

    private static final Set<BtreeFlags> READ_FLAGS = DbUtility.of(BtreeFlags.READONLY);

    private static final Set<FileOpenPermission> READ_PERMISSIONS = DbUtility.of(FileOpenPermission.READONLY);

    private static final Set<BtreeFlags> WRITE_FLAGS = DbUtility.of(BtreeFlags.READWRITE, BtreeFlags.CREATE);

    private static final Set<FileOpenPermission> WRITE_PREMISSIONS = DbUtility.of(FileOpenPermission.READWRITE, FileOpenPermission.CREATE);

    private boolean writable;

    private IDbHandle dbHandle;

    private IBtree btree;

    private boolean transaction;

    private TransactionMode transactionMode;

    private boolean open = false;

    private File file;

    /**
     * <p>
     * Creates connection to database but not open it. Doesn't open database
     * file until not called method {@link #open()}.
     * </p>
     * 
     * <p>
     * File could be null or have special value {@link #IN_MEMORY}. If file is
     * null then will be created temporary file which will be deleted at close.
     * If file is {@link #IN_MEMORY} then file doesn't created and instead
     * database will placed in memory. If regular file is specified but doesn't
     * exist then it will be tried to created.
     * </p>
     * 
     * @param file
     *            path to data base. Could be null or {@link #IN_MEMORY}.
     * @param writable
     *            if true then will allow data modification.
     */
    public Database(final File file, final boolean writable) {
        this.writable = writable;
        this.file = file;
    }

    /**
     * <p>
     * Opens connection to database. It does not create any locking on database.
     * First lock will be created when be called any method which requires real
     * access to options or schema.
     * </p>
     * 
     * @throws DbException
     *             if any trouble with access to file or database format.
     */
    public synchronized void open() throws DbException {
        if (!open) {
            dbHandle = new DbHandle();
            dbHandle.setBusyHandler(new DefaultBusyHandler());
            btree = new Btree();
            final Set<BtreeFlags> flags = (writable ? WRITE_FLAGS : READ_FLAGS);
            final Set<FileOpenPermission> permissions = (writable ? WRITE_PREMISSIONS : READ_PERMISSIONS);
            final FileType type = (file != null ? FileType.MAIN_DB : FileType.TEMP_DB);
            btree.open(file, dbHandle, flags, type, permissions);
            IFile file = btree.getPager().getFile();
            if (file != null) {
                Set<FileOpenPermission> realPermissions = btree.getPager().getFile().getPermissions();
                writable = realPermissions.contains(FileOpenPermission.READWRITE);
            }
            open = true;
        } else {
            throw new DbException(DbErrorCode.MISUSE, "Database is open already");
        }
    }

    /**
     * <p>
     * Opens connection to data base. It does not create any locking on
     * database. First lock will be created when be called any method which
     * requires real access to options or schema.
     * <p>
     * 
     * <p>
     * File could be null or have special value {@link #IN_MEMORY}. If file is
     * null then will be created temporary file which will be deleted at close.
     * If file is {@link #IN_MEMORY} then file doesn't created and instead
     * database will placed in memory. If regular file is specified but doesn't
     * exist then it will be tried to created.
     * </p>
     * 
     * @param file
     *            path to data base. Could be null or {@link #IN_MEMORY}.
     * @param write
     *            open for writing if true.
     * @throws DbException
     *             if any trouble with access to file or database format.
     */
    public static Database open(File file, boolean write) throws DbException {
        final Database db = new Database(file, write);
        db.open();
        return db;
    }

    /**
     * Checks is database open.
     * 
     * @return true if database is open.
     */
    public boolean isOpen() {
        return open;
    }

    private void checkOpen() throws DbException {
        if (!isOpen()) throw new DbException(DbErrorCode.MISUSE, "Database closed");
    }

    /**
     * Close connection to database. It is safe to call this method if database
     * connections is closed already.
     * 
     * @throws DbException
     *             it is possible to get exception if there is actvie
     *             transaction and rollback did not success.
     */
    public void close() throws DbException {
        if (open) {
            runWithLock(new RunnableWithLock() {

                public Object runWithLock(Database db) throws DbException {
                    if (btree != null) {
                        btree.close();
                        btree = null;
                        open = false;
                    }
                    return null;
                }
            });
            if (!open) {
                dbHandle = null;
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (open) {
                close();
            }
        } finally {
            super.finalize();
        }
    }

    /**
     * Reads database schema and options.
     * 
     * @throws DbException
     */
    private void readSchema() throws DbException {
        runWithLock(new RunnableWithLock() {

            public Object runWithLock(Database db) throws DbException {
                btree.enter();
                try {
                    dbHandle.setOptions(new Options(btree, dbHandle));
                    btree.setSchema(new Schema(dbHandle, btree));
                } finally {
                    btree.leave();
                }
                return null;
            }
        });
    }

    /**
     * Set cache size (in count of pages).
     * 
     * @param cacheSize
     *            the count of pages which can hold cache.
     * @throws DbException
     */
    public void setCacheSize(final int cacheSize) throws DbException {
        checkOpen();
        runWithLock(new RunnableWithLock() {

            public Object runWithLock(Database db) throws DbException {
                btree.setCacheSize(cacheSize);
                return null;
            }
        });
    }

    /**
     * Get cache size (in count of pages).
     * 
     * @return the count of pages which can hold cache.
     * @throws DbException
     */
    public int getCacheSize() throws DbException {
        checkOpen();
        refreshSchema();
        return (Integer) runWithLock(new RunnableWithLock() {

            public Object runWithLock(Database db) throws DbException {
                return btree.getCacheSize();
            }
        });
    }

    /**
     * Do some actions with locking database's internal threads synchronization
     * mutex. It is related only with synchronization of access to one
     * connection from multiple threads. It is not related with transactions and
     * locks of database file. For concurrent access to database from threads or
     * processes use transactions.
     * 
     * @param op operation to run
     * @return result of the {@link RunnableWithLock#runWithLock(Database)} call.
     *  
     * @throws DbException in case operation fails to run.
     */
    public Object runWithLock(RunnableWithLock op) throws DbException {
        checkOpen();
        dbHandle.getMutex().enter();
        try {
            return op.runWithLock(this);
        } finally {
            dbHandle.getMutex().leave();
        }
    }

    public File getFile() {
        return this.file;
    }

    /**
     * Check write access to data base.
     * 
     * @return true if modification is allowed
     */
    public boolean isWritable() throws DbException {
        checkOpen();
        return writable;
    }

    /**
     * Get database schema.
     * 
     * @return database schema.
     */
    public ISchema getSchema() throws DbException {
        return getSchemaInternal();
    }

    private Schema getSchemaInternal() throws DbException {
        checkOpen();
        refreshSchema();
        return btree.getSchema();
    }

    /**
     * Open table.
     * 
     * @param tableName name of the table to open.
     * @return opened table
     */
    public ITable getTable(final String tableName) throws DbException {
        checkOpen();
        refreshSchema();
        return (Table) runWithLock(new RunnableWithLock() {

            public Object runWithLock(Database db) throws DbException {
                return new Table(db, btree, tableName, writable);
            }
        });
    }

    /**
     * Run modifications in write transaction.
     * 
     * @param op transaction to run.
     * @return result of the {@link ITransaction#run(Database)} call.
     */
    public Object runWriteTransaction(ITransaction op) throws DbException {
        checkOpen();
        if (writable) {
            return runTransaction(op, TransactionMode.WRITE);
        } else {
            throw new DbException(DbErrorCode.MISUSE, "Can't start write transaction on read-only database");
        }
    }

    /**
     * Run read-only transaction.
     * 
     * @param op transaction to run.
     * @return result of the {@link ITransaction#run(Database)} call.
     */
    public Object runReadTransaction(ITransaction op) throws DbException {
        checkOpen();
        return runTransaction(op, TransactionMode.READ_ONLY);
    }

    /**
     * Run transaction.
     * 
     * @param op
     *            transaction's body (closure).
     * @param mode
     *            transaction's mode.
     * @return result of the {@link ITransaction#run(Database)} call.
     */
    public Object runTransaction(final ITransaction op, final TransactionMode mode) throws DbException {
        checkOpen();
        return runWithLock(new RunnableWithLock() {

            public Object runWithLock(Database db) throws DbException {
                if (transaction) {
                    if (mode != transactionMode && transactionMode == TransactionMode.READ_ONLY) {
                        throw new DbException(DbErrorCode.MISUSE, TRANSACTION_ALREADY_STARTED);
                    } else {
                        return op.run(Database.this);
                    }
                } else {
                    beginTransaction(mode);
                    boolean success = false;
                    try {
                        final Object result = op.run(Database.this);
                        btree.commit();
                        success = true;
                        return result;
                    } finally {
                        if (!success) {
                            btree.rollback();
                        }
                        transaction = false;
                        transactionMode = null;
                    }
                }
            }
        });
    }

    /**
     * Begin transaction.
     * 
     * @param mode
     *            transaction's mode.
     * @throws DbException
     */
    public void beginTransaction(final TransactionMode mode) throws DbException {
        checkOpen();
        runWithLock(new RunnableWithLock() {

            public Object runWithLock(Database db) throws DbException {
                refreshSchema();
                if (transaction) {
                    throw new DbException(DbErrorCode.MISUSE, TRANSACTION_ALREADY_STARTED);
                } else {
                    btree.beginTrans(mode);
                    transaction = true;
                    transactionMode = mode;
                    return null;
                }
            }
        });
    }

    /**
     * Commit transaction.
     * 
     * @throws DbException
     */
    public void commit() throws DbException {
        checkOpen();
        runWithLock(new RunnableWithLock() {

            public Object runWithLock(Database db) throws DbException {
                if (transaction) {
                    btree.closeAllCursors();
                    btree.commit();
                    transaction = false;
                    transactionMode = null;
                } else {
                    throw new DbException(DbErrorCode.MISUSE, "Transaction wasn't started");
                }
                return null;
            }
        });
    }

    /**
     * Rollback transaction.
     * 
     * @throws DbException
     */
    public void rollback() throws DbException {
        checkOpen();
        runWithLock(new RunnableWithLock() {

            public Object runWithLock(Database db) throws DbException {
                btree.closeAllCursors();
                btree.rollback();
                transaction = false;
                transactionMode = null;
                return null;
            }
        });
    }

    /**
     * Get database options.
     * 
     * @return options of this database
     * @throws DbException
     */
    public IOptions getOptions() throws DbException {
        checkOpen();
        if (null == btree.getSchema()) {
            readSchema();
        }
        return dbHandle.getOptions();
    }

    /**
     * Executes pragma statement. If statement queries pragma value then pragma
     * value will be returned.
     */
    public Object pragma(final String sql) throws DbException {
        checkOpen();
        refreshSchema();
        return runWithLock(new RunnableWithLock() {

            public Object runWithLock(Database db) throws DbException {
                return new PragmasHandler(getOptions()).pragma(sql);
            }
        });
    }

    /**
     * Create table from SQL clause.
     * 
     * @param sql
     *            CREATE TABLE ... sentence.
     * @return definition of create table.
     */
    public ITableDef createTable(final String sql) throws DbException {
        checkOpen();
        return (ITableDef) runWriteTransaction(new ITransaction() {

            public Object run(Database db) throws DbException {
                return getSchemaInternal().createTable(sql);
            }
        });
    }

    /**
     * Create index from SQL clause.
     * 
     * @param sql
     *            CREATE INDEX ... sentence.
     * @return definition of created index.
     */
    public IIndexDef createIndex(final String sql) throws DbException {
        checkOpen();
        return (IIndexDef) runWriteTransaction(new ITransaction() {

            public Object run(Database db) throws DbException {
                return getSchemaInternal().createIndex(sql);
            }
        });
    }

    /**
     * Drop table.
     * 
     * @param tableName name of table to drop.
     */
    public void dropTable(final String tableName) throws DbException {
        checkOpen();
        runWriteTransaction(new ITransaction() {

            public Object run(Database db) throws DbException {
                getSchemaInternal().dropTable(tableName);
                return null;
            }
        });
    }

    /**
     * Drop index.
     * 
     * @param indexName name of the index to drop.
     */
    public void dropIndex(final String indexName) throws DbException {
        checkOpen();
        runWriteTransaction(new ITransaction() {

            public Object run(Database db) throws DbException {
                getSchemaInternal().dropIndex(indexName);
                return null;
            }
        });
    }

    /**
     * Alters table.
     * 
     * @param sql
     *            ALTER TABLE ... sentence.
     * @return altered table schema definition.
     */
    public ITableDef alterTable(final String sql) throws DbException {
        checkOpen();
        return (ITableDef) runWriteTransaction(new ITransaction() {

            public Object run(Database db) throws DbException {
                return getSchemaInternal().alterTable(sql);
            }
        });
    }

    /**
     * Create virtual table from SQL clause.
     * 
     * @param sql
     *            CREATE VIRTUAL TABLE ... sentence.
     * @return definition of create virtual table.
     */
    public IVirtualTableDef createVirtualTable(final String sql) throws DbException {
        checkOpen();
        return (IVirtualTableDef) runWriteTransaction(new ITransaction() {

            public Object run(Database db) throws DbException {
                return getSchemaInternal().createVirtualTable(sql, 0);
            }
        });
    }

    /**
     * Get busy handler.
     * 
     * @return the busy handler.
     */
    public IBusyHandler getBusyHandler() {
        return dbHandle.getBusyHandler();
    }

    /**
     * Set busy handler. Busy handler treats situation when database is locked
     * by other process or thread.
     * 
     * @param iBusyHandler
     *            the busy handler.
     */
    public void setBusyHandler(IBusyHandler iBusyHandler) {
        dbHandle.setBusyHandler(iBusyHandler);
    }

    /**
     * Refresh database schema.
     * 
     * @throws DbException
     */
    public void refreshSchema() throws DbException {
        if (null == btree.getSchema() || !getOptions().verifySchemaVersion(false)) {
            readSchema();
        }
    }

    /**
     * Return true if a transaction is active.
     * 
     * @return true if there is active running transaction in this database now.
     */
    public boolean isInTransaction() {
        return transaction;
    }

    public TransactionMode getTransactionMode() {
        return transactionMode;
    }

    /**
     * Get threading synchronization mutex.
     * 
     * @return Mutex semaphore instance used to synchronize access to this database from multiple threads.
     */
    public IMutex getMutex() {
        return dbHandle.getMutex();
    }
}
