package jgnash.engine.db4o;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import jgnash.engine.Account;
import jgnash.engine.Config;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.ExchangeRate;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.RootAccount;
import jgnash.engine.SecurityNode;
import jgnash.engine.StoredObject;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.engine.TrashObject;
import jgnash.engine.db4o.config.TBigDecimal;
import jgnash.engine.db4o.config.TBigInteger;
import jgnash.engine.recurring.DailyReminder;
import jgnash.engine.recurring.MonthlyReminder;
import jgnash.engine.recurring.OneTimeReminder;
import jgnash.engine.recurring.Reminder;
import jgnash.engine.recurring.WeeklyReminder;
import jgnash.engine.recurring.YearlyReminder;
import com.db4o.Db4o;
import com.db4o.ObjectContainer;
import com.db4o.config.Configuration;
import com.db4o.defragment.Defragment;
import com.db4o.defragment.DefragmentConfig;
import com.db4o.diagnostic.Diagnostic;
import com.db4o.diagnostic.DiagnosticListener;
import com.db4o.diagnostic.NativeQueryNotOptimized;

/**
 * db4o specific code for creating an Engine.
 * 
 * @author Craig Cavanaugh
 * 
 * $Id: Db4oEngineFactory.java,v 1.13 2007/12/07 12:59:09 ccavanaugh Exp $
 */
public class Db4oEngineFactory {

    private static Logger log = Logger.getLogger(Db4oEngineFactory.class.getName());

    public static final String FILE_EXT = ".yap";

    /** Defragment when the database is closed */
    private boolean defragOnClose = true;

    private static Map<Engine, Container> containerMap = new HashMap<Engine, Container>();

    protected static Configuration createConfig() {
        Configuration config = Db4o.newConfiguration();
        config.optimizeNativeQueries(true);
        config.callConstructors(true);
        config.diagnostic().addListener(new DiagnosticListener() {

            public void onDiagnostic(Diagnostic d) {
                if (d instanceof NativeQueryNotOptimized) {
                    log.info(((NativeQueryNotOptimized) d).problem() + "\n" + ((NativeQueryNotOptimized) d).reason() + "\n" + ((NativeQueryNotOptimized) d).solution());
                }
            }
        });
        config.objectClass(BigDecimal.class).translate(new TBigDecimal());
        config.objectClass(BigInteger.class).translate(new TBigInteger());
        config.objectClass(ExchangeRate.class).cascadeOnUpdate(true);
        config.objectClass(ExchangeRate.class).cascadeOnActivate(true);
        config.objectClass(SecurityNode.class).cascadeOnUpdate(true);
        config.objectClass(SecurityNode.class).cascadeOnActivate(true);
        config.objectClass(CurrencyNode.class).cascadeOnActivate(true);
        config.objectClass(CurrencyNode.class).cascadeOnUpdate(true);
        config.objectClass(Config.class).cascadeOnActivate(true);
        config.objectClass(Config.class).cascadeOnUpdate(true);
        config.objectClass(Account.class).cascadeOnActivate(true);
        config.objectClass(Account.class).objectField("propertyMap").cascadeOnUpdate(true);
        config.objectClass(Account.class).updateDepth(1);
        config.objectClass(RootAccount.class).updateDepth(1);
        config.objectClass(DailyReminder.class).cascadeOnActivate(true);
        config.objectClass(DailyReminder.class).cascadeOnUpdate(true);
        config.objectClass(MonthlyReminder.class).cascadeOnActivate(true);
        config.objectClass(MonthlyReminder.class).cascadeOnUpdate(true);
        config.objectClass(OneTimeReminder.class).cascadeOnActivate(true);
        config.objectClass(OneTimeReminder.class).cascadeOnUpdate(true);
        config.objectClass(WeeklyReminder.class).cascadeOnActivate(true);
        config.objectClass(WeeklyReminder.class).cascadeOnUpdate(true);
        config.objectClass(YearlyReminder.class).cascadeOnActivate(true);
        config.objectClass(YearlyReminder.class).cascadeOnUpdate(true);
        config.objectClass(Reminder.class).cascadeOnActivate(true);
        config.objectClass(Reminder.class).cascadeOnUpdate(true);
        config.objectClass(TrashObject.class).cascadeOnActivate(true);
        config.objectClass(TrashObject.class).cascadeOnUpdate(true);
        config.objectClass(InvestmentTransaction.class).cascadeOnActivate(true);
        config.objectClass(Transaction.class).cascadeOnActivate(true);
        config.objectClass(TransactionEntry.class).cascadeOnActivate(true);
        config.objectClass(StoredObject.class).indexed(true);
        config.objectClass(Account.class).indexed(true);
        config.objectClass(CurrencyNode.class).indexed(true);
        config.objectClass(Reminder.class).indexed(true);
        config.objectClass(RootAccount.class).indexed(true);
        config.objectClass(StoredObject.class).objectField("uuid").indexed(true);
        config.objectClass(StoredObject.class).objectField("markedForRemoval").indexed(true);
        config.objectClass(Account.class).objectField("accountType").indexed(true);
        config.objectClass(ExchangeRate.class).objectField("rateId").indexed(true);
        config.exceptionsOnNotStorable(true);
        config.automaticShutDown(true);
        config.messageLevel(1);
        return config;
    }

    private ObjectContainer createLocalContainer(final String fileName) {
        Configuration config = createConfig();
        File file = new File(fileName + FILE_EXT);
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
        }
        ObjectContainer container = Db4o.openFile(config, fileName + FILE_EXT);
        return container;
    }

    private ObjectContainer createClientContainer(String host, int port, String user, String password) {
        Configuration config = createConfig();
        ObjectContainer container = null;
        container = Db4o.openClient(config, host, port, user, password);
        return container;
    }

    public Engine getLocalEngine(final String fileName, final String engineName) {
        Engine engine = null;
        ObjectContainer db = createLocalContainer(fileName);
        if (db != null) {
            log.info("Created local db4o container");
            engine = new Engine(new Db4oEngineDAO(db), engineName);
            Container container = new Container(fileName, false, db);
            containerMap.put(engine, container);
        }
        return engine;
    }

    public Engine getClientEngine(final String host, final int port, final String user, final String password, final String engineName) {
        Engine engine = null;
        ObjectContainer db = createClientContainer(host, port, user, password);
        if (db != null) {
            log.info("Created client db4o container");
            engine = new Engine(new Db4oEngineDAO(db), engineName);
            Container container = new Container(null, true, db);
            containerMap.put(engine, container);
        }
        return engine;
    }

    public void closeEngine(Engine engine) {
        Container container = containerMap.get(engine);
        ObjectContainer db = container.db;
        boolean remote = container.remote;
        if (db != null) {
            if (remote) {
                db.close();
            } else {
                db.close();
                defrag(container.fileName);
            }
            containerMap.remove(engine);
        }
    }

    /** 
     * Defragment the file to claim unused space
     */
    private void defrag(String fileName) {
        if (defragOnClose) {
            try {
                DefragmentConfig config = new DefragmentConfig(fileName + FILE_EXT);
                config.forceBackupDelete(true);
                config.db4oConfig(createConfig());
                Defragment.defrag(config);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean doesDatabaseExist(String database) {
        boolean result = false;
        File f = new File(database + FILE_EXT);
        if (f.canRead()) {
            log.fine("Found database file: " + database + FILE_EXT);
            result = true;
        }
        f = null;
        return result;
    }

    public static boolean isFileLocked(String database) {
        File file = new File(database + FILE_EXT);
        try {
            FileChannel channel = new RandomAccessFile(file, "rw").getChannel();
            FileLock lock = channel.tryLock();
            if (lock != null) {
                lock.release();
                return false;
            }
        } catch (FileNotFoundException e) {
            log.fine("database file: " + database + FILE_EXT + " was not found");
        } catch (IOException e) {
            return true;
        }
        return true;
    }

    public static boolean deleteDatabase(String database) {
        boolean result = false;
        File f = new File(database + FILE_EXT);
        if (f.delete()) {
            log.info("Removed database file: " + database + FILE_EXT);
            result = true;
        }
        f = null;
        return result;
    }

    class Container {

        boolean remote;

        String fileName;

        ObjectContainer db;

        Container(String fileName, boolean remote, ObjectContainer db) {
            this.remote = remote;
            this.fileName = fileName;
            this.db = db;
        }
    }
}
