package org.archive.settings.file;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.archive.settings.CheckpointRecovery;
import org.archive.settings.RecoverAction;
import org.archive.state.Immutable;
import org.archive.state.Initializable;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.Module;
import org.archive.state.StateProvider;
import org.archive.util.CachedBdbMap;
import org.archive.util.FileUtils;
import org.archive.util.bdbje.EnhancedEnvironment;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.je.CheckpointConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DatabaseNotFoundException;
import com.sleepycat.je.DbInternal;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.SecondaryConfig;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.SecondaryKeyCreator;
import com.sleepycat.je.dbi.EnvironmentImpl;
import com.sleepycat.je.utilint.DbLsn;

public class BdbModule implements Module, Initializable, Checkpointable, Serializable, Closeable {

    private static final Logger LOGGER = Logger.getLogger(BdbModule.class.getName());

    private static class DatabasePlusConfig implements Serializable {

        private static final long serialVersionUID = 1L;

        public transient Database database;

        public String name;

        public String primaryName;

        public BdbConfig config;
    }

    /**
     * Configuration object for databases.  Needed because 
     * {@link DatabaseConfig} is not serializable.  Also it prevents invalid
     * configurations.  (All databases opened through this module must be
     * deferred-write, because otherwise they can't sync(), and you can't
     * run a checkpoint without doing sync() first.)
     * 
     * @author pjack
     *
     */
    public static class BdbConfig implements Serializable {

        private static final long serialVersionUID = 1L;

        boolean allowCreate;

        boolean sortedDuplicates;

        boolean transactional;

        public BdbConfig() {
        }

        public boolean isAllowCreate() {
            return allowCreate;
        }

        public void setAllowCreate(boolean allowCreate) {
            this.allowCreate = allowCreate;
        }

        public boolean getSortedDuplicates() {
            return sortedDuplicates;
        }

        public void setSortedDuplicates(boolean sortedDuplicates) {
            this.sortedDuplicates = sortedDuplicates;
        }

        public DatabaseConfig toDatabaseConfig() {
            DatabaseConfig result = new DatabaseConfig();
            result.setDeferredWrite(true);
            result.setTransactional(transactional);
            result.setAllowCreate(allowCreate);
            result.setSortedDuplicates(sortedDuplicates);
            return result;
        }

        public boolean isTransactional() {
            return transactional;
        }

        public void setTransactional(boolean transactional) {
            this.transactional = transactional;
        }
    }

    public static class SecondaryBdbConfig extends BdbConfig {

        private static final long serialVersionUID = 1L;

        private SecondaryKeyCreator keyCreator;

        public SecondaryBdbConfig() {
        }

        public SecondaryKeyCreator getKeyCreator() {
            return keyCreator;
        }

        public void setKeyCreator(SecondaryKeyCreator keyCreator) {
            this.keyCreator = keyCreator;
        }

        public SecondaryConfig toSecondaryConfig() {
            SecondaryConfig result = new SecondaryConfig();
            result.setDeferredWrite(true);
            result.setTransactional(transactional);
            result.setAllowCreate(allowCreate);
            result.setSortedDuplicates(sortedDuplicates);
            result.setKeyCreator(keyCreator);
            return result;
        }
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Immutable
    public static final Key<String> DIR = Key.make("state");

    @Immutable
    public static final Key<Integer> BDB_CACHE_PERCENT = Key.make(60);

    @Immutable
    public static final Key<Boolean> CHECKPOINT_COPY_BDBJE_LOGS = Key.make(true);

    static {
        KeyManager.addKeys(BdbModule.class);
    }

    private boolean checkpointCopy;

    private String path;

    private int cachePercent;

    private transient EnhancedEnvironment bdbEnvironment;

    private transient Database classCatalogDB;

    private transient StoredClassCatalog classCatalog;

    private Map<String, CachedBdbMap> bigMaps = new ConcurrentHashMap<String, CachedBdbMap>();

    private Map<String, DatabasePlusConfig> databases = new ConcurrentHashMap<String, DatabasePlusConfig>();

    private transient Thread shutdownHook;

    public BdbModule() {
    }

    public void initialTasks(StateProvider provider) {
        checkpointCopy = provider.get(this, CHECKPOINT_COPY_BDBJE_LOGS);
        cachePercent = provider.get(this, BDB_CACHE_PERCENT);
        path = provider.get(this, DIR);
        try {
            setUp(path, cachePercent, true);
        } catch (DatabaseException e) {
            throw new IllegalStateException(e);
        }
        shutdownHook = new BdbShutdownHook(this);
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    private void setUp(String path, int cachePercent, boolean create) throws DatabaseException {
        EnvironmentConfig config = new EnvironmentConfig();
        config.setAllowCreate(create);
        config.setLockTimeout(5000000);
        config.setCachePercent(cachePercent);
        config.setSharedCache(true);
        File f = new File(path);
        f.mkdirs();
        this.bdbEnvironment = new EnhancedEnvironment(f, config);
        this.classCatalog = this.bdbEnvironment.getClassCatalog();
    }

    public void closeDatabase(Database db) {
        try {
            closeDatabase(db.getDatabaseName());
        } catch (DatabaseException e) {
            LOGGER.log(Level.SEVERE, "Error getting db name", e);
        }
    }

    public void closeDatabase(String name) {
        DatabasePlusConfig dpc = databases.remove(name);
        if (dpc == null) {
            throw new IllegalStateException("No such database: " + name);
        }
        Database db = dpc.database;
        try {
            db.sync();
            db.close();
        } catch (DatabaseException e) {
            LOGGER.log(Level.SEVERE, "Error closing db " + name, e);
        }
    }

    public Database openDatabase(String name, BdbConfig config, boolean recycle) throws DatabaseException {
        if (databases.containsKey(name)) {
            throw new IllegalStateException("Database already exists: " + name);
        }
        if (!recycle) {
            try {
                bdbEnvironment.truncateDatabase(null, name, false);
            } catch (DatabaseNotFoundException e) {
            }
        }
        DatabasePlusConfig dpc = new DatabasePlusConfig();
        dpc.database = bdbEnvironment.openDatabase(null, name, config.toDatabaseConfig());
        dpc.name = name;
        dpc.config = config;
        databases.put(name, dpc);
        return dpc.database;
    }

    public SecondaryDatabase openSecondaryDatabase(String name, Database db, SecondaryBdbConfig config) throws DatabaseException {
        if (databases.containsKey(name)) {
            throw new IllegalStateException("Database already exists: " + name);
        }
        SecondaryDatabase result = bdbEnvironment.openSecondaryDatabase(null, name, db, config.toSecondaryConfig());
        DatabasePlusConfig dpc = new DatabasePlusConfig();
        dpc.database = result;
        dpc.name = name;
        dpc.primaryName = db.getDatabaseName();
        dpc.config = config;
        databases.put(name, dpc);
        return result;
    }

    public StoredClassCatalog getClassCatalog() {
        return classCatalog;
    }

    public <K, V> Map<K, V> getBigMap(String dbName, boolean recycle, Class<? super K> key, Class<? super V> value) throws DatabaseException {
        @SuppressWarnings("unchecked") CachedBdbMap<K, V> r = bigMaps.get(dbName);
        if (r != null) {
            return r;
        }
        if (!recycle) {
            try {
                bdbEnvironment.truncateDatabase(null, dbName, false);
            } catch (DatabaseNotFoundException e) {
            }
        }
        r = new CachedBdbMap<K, V>(dbName);
        r.initialize(bdbEnvironment, key, value, classCatalog);
        bigMaps.put(dbName, r);
        return r;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (in instanceof CheckpointRecovery) {
            CheckpointRecovery cr = (CheckpointRecovery) in;
            path = cr.translatePath(path);
            cr.setState(this, DIR, path);
        }
        try {
            setUp(path, this.cachePercent, false);
            for (CachedBdbMap map : bigMaps.values()) {
                map.initialize(this.bdbEnvironment, map.getKeyClass(), map.getValueClass(), this.classCatalog);
            }
            for (DatabasePlusConfig dpc : databases.values()) {
                if (!(dpc.config instanceof SecondaryBdbConfig)) {
                    dpc.database = bdbEnvironment.openDatabase(null, dpc.name, dpc.config.toDatabaseConfig());
                }
            }
            for (DatabasePlusConfig dpc : databases.values()) {
                if (dpc.config instanceof SecondaryBdbConfig) {
                    SecondaryBdbConfig conf = (SecondaryBdbConfig) dpc.config;
                    Database primary = databases.get(dpc.primaryName).database;
                    dpc.database = bdbEnvironment.openSecondaryDatabase(null, dpc.name, primary, conf.toSecondaryConfig());
                }
            }
        } catch (DatabaseException e) {
            IOException io = new IOException();
            io.initCause(e);
            throw io;
        }
        this.shutdownHook = new BdbShutdownHook(this);
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    public void checkpoint(File dir, List<RecoverAction> actions) throws IOException {
        if (checkpointCopy) {
            actions.add(new BdbRecover(path));
        }
        for (Map.Entry<String, CachedBdbMap> me : bigMaps.entrySet()) {
            me.getValue().sync();
        }
        EnvironmentConfig envConfig;
        try {
            for (DatabasePlusConfig dbc : databases.values()) {
                dbc.database.sync();
            }
            envConfig = bdbEnvironment.getConfig();
        } catch (DatabaseException e) {
            IOException io = new IOException();
            io.initCause(e);
            throw io;
        }
        final List bkgrdThreads = Arrays.asList(new String[] { "je.env.runCheckpointer", "je.env.runCleaner", "je.env.runINCompressor" });
        try {
            setBdbjeBkgrdThreads(envConfig, bkgrdThreads, "false");
            CheckpointConfig chkptConfig = new CheckpointConfig();
            chkptConfig.setForce(true);
            chkptConfig.setMinimizeRecoveryTime(true);
            bdbEnvironment.checkpoint(chkptConfig);
            LOGGER.fine("Finished bdb checkpoint.");
            EnvironmentImpl envImpl = DbInternal.envGetEnvironmentImpl(bdbEnvironment);
            long firstFileInNextSet = DbLsn.getFileNumber(envImpl.forceLogFileFlip());
            final String lastBdbCheckpointLog = getBdbLogFileName(firstFileInNextSet - 1);
            processBdbLogs(dir, lastBdbCheckpointLog);
            LOGGER.fine("Finished processing bdb log files.");
        } catch (DatabaseException e) {
            IOException io = new IOException();
            io.initCause(e);
            throw io;
        } finally {
            setBdbjeBkgrdThreads(envConfig, bkgrdThreads, "true");
        }
    }

    private void processBdbLogs(final File checkpointDir, final String lastBdbCheckpointLog) throws IOException {
        File bdbDir = getBdbSubDirectory(checkpointDir);
        if (!bdbDir.exists()) {
            bdbDir.mkdir();
        }
        PrintWriter pw = new PrintWriter(new FileOutputStream(new File(checkpointDir, "bdbje-logs-manifest.txt")));
        try {
            boolean pastLastLogFile = false;
            Set<String> srcFilenames = null;
            do {
                FilenameFilter filter = new FilenameFilter() {

                    public boolean accept(File dir, String name) {
                        return name != null && name.toLowerCase().endsWith(".jdb");
                    }
                };
                srcFilenames = new HashSet<String>(Arrays.asList(new File(path).list(filter)));
                List tgtFilenames = Arrays.asList(bdbDir.list(filter));
                if (tgtFilenames != null && tgtFilenames.size() > 0) {
                    srcFilenames.removeAll(tgtFilenames);
                }
                if (srcFilenames.size() > 0) {
                    srcFilenames = new TreeSet<String>(srcFilenames);
                    int count = 0;
                    for (final Iterator i = srcFilenames.iterator(); i.hasNext() && !pastLastLogFile; ) {
                        String name = (String) i.next();
                        if (this.checkpointCopy) {
                            FileUtils.copyFiles(new File(path, name), new File(bdbDir, name));
                        }
                        pw.println(name);
                        if (name.equals(lastBdbCheckpointLog)) {
                            pastLastLogFile = true;
                        }
                        count++;
                    }
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine("Copied " + count);
                    }
                }
            } while (!pastLastLogFile && srcFilenames != null && srcFilenames.size() > 0);
        } finally {
            pw.close();
        }
    }

    private void setBdbjeBkgrdThreads(final EnvironmentConfig config, final List threads, final String setting) {
        for (final Iterator i = threads.iterator(); i.hasNext(); ) {
            config.setConfigParam((String) i.next(), setting);
        }
    }

    private String getBdbLogFileName(final long index) {
        String lastBdbLogFileHex = Long.toHexString(index);
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < (8 - lastBdbLogFileHex.length()); i++) {
            buffer.append('0');
        }
        buffer.append(lastBdbLogFileHex);
        buffer.append(".jdb");
        return buffer.toString();
    }

    public void close() {
        close2();
        Runtime.getRuntime().removeShutdownHook(shutdownHook);
    }

    void close2() {
        if (classCatalog == null) {
            return;
        }
        for (Map.Entry<String, CachedBdbMap> me : bigMaps.entrySet()) try {
            me.getValue().close();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error closing bigMap " + me.getKey(), e);
        }
        List<String> dbNames = new ArrayList<String>(databases.keySet());
        for (String dbName : dbNames) try {
            closeDatabase(dbName);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error closing db " + dbName, e);
        }
        try {
            this.bdbEnvironment.sync();
            this.bdbEnvironment.close();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error closing environment.", e);
        }
    }

    private static File getBdbSubDirectory(File checkpointDir) {
        return new File(checkpointDir, "bdbje-logs");
    }

    public Database getDatabase(String name) {
        DatabasePlusConfig dpc = databases.get(name);
        if (dpc == null) {
            return null;
        }
        return dpc.database;
    }

    private static class BdbRecover implements RecoverAction {

        private static final long serialVersionUID = 1L;

        private String path;

        public BdbRecover(String path) {
            this.path = path;
        }

        public void recoverFrom(File checkpointDir, CheckpointRecovery recovery) throws Exception {
            File bdbDir = getBdbSubDirectory(checkpointDir);
            path = recovery.translatePath(path);
            FileUtils.copyFiles(bdbDir, new File(path));
        }
    }

    private static class BdbShutdownHook extends Thread {

        private final BdbModule bdb;

        public BdbShutdownHook(BdbModule bdb) {
            this.bdb = bdb;
        }

        public void run() {
            this.bdb.close2();
        }
    }
}
