package ch.olsen.products.util.database.otsdb.test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import junit.framework.Assert;
import junit.framework.TestCase;
import ch.olsen.products.util.Application;
import ch.olsen.products.util.database.Pair;
import ch.olsen.products.util.database.Request;
import ch.olsen.products.util.database.RequestException;
import ch.olsen.products.util.database.Request.RequestExceptionConnection;
import ch.olsen.products.util.database.Request.RequestExceptionNoDriver;
import ch.olsen.products.util.database.otsdb.Otsdb;
import ch.olsen.products.util.database.otsdb.OtsdbInterface;
import ch.olsen.products.util.database.otsdb.OtsdbPool;
import ch.olsen.products.util.database.otsdb.Otsdb.OtsdbLogin;
import ch.olsen.products.util.database.otsdb.OtsdbPool.OtsdbUnit;

/**
 * This is a test class to make sure this class does what it should
 * And possibly to bench mark it too
 */
public class TestOtsdb extends TestCase {

    static OtsdbLogin login = new OtsdbLogin();

    /**
	 * just adds a method to get the current worker
	 * for the test client to track which worker it is using
	 */
    static class OtsdbInsider extends Otsdb {

        OtsdbUnit getCurrentWorker() {
            if (worker == null) worker = getWorker();
            return worker;
        }
    }

    /**
	 * Task to replace the "heart" of a worker; give a worker a task
	 */
    public abstract static class Task {

        WorkerAbstr worker = null;

        String table = "A";

        int inserts = 1000;

        public Task(String table, int inserts) {
            this.table = table;
            this.inserts = inserts;
        }

        public void setWorker(WorkerAbstr worker) {
            this.worker = worker;
        }

        public abstract void run();

        public abstract void accident(int i);

        public abstract Task clone();
    }

    /**
	 * Task to insert lots of records
	 */
    public static class TaskInsertSequence extends Task {

        public TaskInsertSequence(String table, int inserts) {
            super(table, inserts);
        }

        public void accident(int i) {
        }

        public void run() {
            if (worker == null) fail("worker is not allocated");
            for (int i = 0; i < inserts; ++i) {
                accident(i);
                String insert = "INSERT INTO " + table + " (name) VALUES ('" + worker.name + "-" + String.valueOf(i) + "')";
                try {
                    worker.db.executeUpdate(insert);
                } catch (RequestException e) {
                    fail("caught the exception: " + e.getMessage() + "\n" + e.getCause());
                }
                if (i % (inserts / 10) == 0) worker.log(i + " inserts");
            }
            try {
                worker.disconnect();
            } catch (RequestExceptionConnection e) {
                worker.log("problem while disconnecting");
                e.printStackTrace();
            }
        }

        @Override
        public Task clone() {
            return new TaskInsertSequence(table, inserts);
        }
    }

    /**
	 * Abstract class to share as much as possible with the pooled and regular
	 * requests
	 */
    public abstract static class WorkerAbstr extends Thread {

        String name = "A";

        OtsdbLogin login;

        OtsdbInterface db = null;

        Task task = null;

        public WorkerAbstr(String name, OtsdbLogin login, Task task) {
            this.name = name;
            this.task = task;
            task.setWorker(this);
        }

        public void run() {
            task.run();
        }

        public abstract void disconnect() throws RequestExceptionConnection;

        public synchronized void log(String message) {
            System.out.println(threadInfo() + " : " + message);
        }

        protected abstract String threadInfo();
    }

    /**
	 * our basic worker, makes 1000 inserts
	 * adds a method "accident" to introduce extra behaviors
	 */
    static class PooledWorker extends WorkerAbstr {

        OtsdbInsider otsdb = null;

        public PooledWorker(String name, OtsdbLogin login, Task task) {
            super(name, login, task);
            otsdb = new OtsdbInsider();
            db = otsdb;
            otsdb.setLogin(login);
        }

        protected synchronized String threadInfo() {
            return "Thread " + name + " using worker " + otsdb.getCurrentWorker().getRank() + " / " + otsdb.getCurrentWorker().getPoolSize();
        }

        @Override
        public void disconnect() throws RequestExceptionConnection {
            db.disconnect();
        }
    }

    /**
	 * regular (not pooled) version of our worker
	 */
    static class RegularWorker extends WorkerAbstr {

        public static OtsdbPool pool = null;

        OtsdbUnit otsdb = null;

        public RegularWorker(String name, OtsdbLogin login, Task task) {
            super(name, login, task);
            if (pool == null) pool = new OtsdbPool(login);
            otsdb = pool.new OtsdbUnit();
            db = otsdb;
        }

        protected synchronized String threadInfo() {
            return "Thread " + name + " using worker " + otsdb.getRank() + " / " + otsdb.getPoolSize();
        }

        @Override
        public void disconnect() throws RequestExceptionConnection {
            otsdb.getConnectInfo().trials = otsdb.getConnectInfo().maxRetry;
            db.disconnect();
        }
    }

    static class LockingTaskInsertSequence extends TaskInsertSequence {

        boolean lock = false;

        int lockStart = 200;

        int lockEnd = 500;

        OtsdbInterface otsdb = null;

        public LockingTaskInsertSequence(String table, int inserts, int lockStart, int lockEnd) {
            super(table, inserts);
            this.lockStart = lockStart;
            this.lockEnd = lockEnd;
        }

        public void setOtsdbWorker(WorkerAbstr worker) {
            this.worker = worker;
        }

        public LockingTaskInsertSequence clone() {
            return new LockingTaskInsertSequence(table, inserts, lockStart, lockEnd);
        }

        public void accident(int i) {
            if (lock) {
                if (i > lockEnd && worker.db.isLocked()) {
                    try {
                        worker.log("releasing the lock");
                        worker.db.unlock();
                        worker.log("released the lock");
                    } catch (RequestException e) {
                        worker.log("unlock exception: " + e.getMessage());
                        fail("unlock exception: " + e.getMessage());
                    }
                }
            } else if (i > lockStart) {
                lock = true;
                String[] read = {};
                String[] write = { table };
                try {
                    worker.log("locking");
                    worker.db.lock(read, write);
                    worker.log("locked");
                } catch (RequestException e) {
                    worker.log("unlock exception: " + e.getMessage());
                    fail("unlock exception: " + e.getMessage());
                }
            }
        }
    }

    protected void setUp() throws Exception {
        login.server = "localhost";
        login.database = "test_otsdb";
        login.user = "tester_otsdb";
        login.pwd = "tester";
        Request request = new Request();
        request.connect(login.server, login.database, login.user, login.pwd);
        String cleanTestTable = "drop table if exists A";
        request.executeUpdate(cleanTestTable);
        cleanTestTable = "drop table if exists B";
        request.executeUpdate(cleanTestTable);
        String tableBodies = "(" + "id int(10) unsigned NOT NULL auto_increment, " + "name varchar(50) NOT NULL default '', " + "PRIMARY KEY  (id))";
        String createTable = "create table A " + tableBodies;
        request.executeUpdate(createTable);
        createTable = "create table B " + tableBodies;
        request.executeUpdate(createTable);
        request.disconnect();
        Application.getLogger(OtsdbPool.class.getCanonicalName()).setDebug();
    }

    public void testLogin() {
        System.out.println("testLogin started");
        OtsdbLogin newLogin = new OtsdbLogin();
        newLogin.copy(login);
        Assert.assertTrue(newLogin.equals(login));
        newLogin.properties = new Properties();
        newLogin.properties.setProperty("socketTimeout", "5000");
        Assert.assertTrue(!newLogin.equals(login));
        Otsdb dbA = new Otsdb();
        dbA.setLogin(login);
        String insert = "INSERT INTO A (name) VALUES ('yxc'), ('vbn')";
        try {
            dbA.executeUpdate(insert);
        } catch (RequestException e) {
            e.printStackTrace();
            fail("failed testAccess");
        }
        Otsdb dbB = new Otsdb();
        dbB.setLogin(newLogin);
        insert = "INSERT INTO A (name) VALUES ('asd'), ('fgh')";
        try {
            dbB.executeUpdate(insert);
        } catch (RequestException e) {
            e.printStackTrace();
            fail("failed testAccess");
        }
        System.out.println("testLogin finished");
    }

    /**
	 * just to make sure we can read the database fine
	 */
    public void testAccess() {
        System.out.println("testAccess started");
        Otsdb dbA = new Otsdb();
        dbA.setLogin(login);
        String insert = "INSERT INTO A (name) VALUES ('qwe'), ('rtz')";
        try {
            dbA.executeUpdate(insert);
        } catch (RequestException e) {
            e.printStackTrace();
            fail("failed testAccess");
        }
        System.out.println("testAccess finished");
    }

    /**
	 * just to make sure we can access the database with 2 workers concurrently
	 */
    public void testConcurrentAccess() {
        System.out.println("testConcurrentAccess started");
        Task task = new TaskInsertSequence("A", 500);
        PooledWorker x = new PooledWorker("X", login, task);
        PooledWorker y = new PooledWorker("Y", login, task);
        x.start();
        y.start();
        try {
            x.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            y.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("testConcurrentAccess finished");
    }

    /**
	 * what we see here are two workers sharing a connection to the same table
	 * the first one locks the table,
	 * then the second one gets a second connection but still can't write to the
	 * table until the first one release the lock
	 */
    public void testConcurrentSingleLockAccess() {
        System.out.println("testConcurrentSingleLockAccess started");
        Task lockTask = new LockingTaskInsertSequence("A", 10000, 2000, 5000);
        PooledWorker x = new PooledWorker("X", login, lockTask);
        Task task = new TaskInsertSequence("A", 10000);
        PooledWorker y = new PooledWorker("Y", login, task);
        x.start();
        y.start();
        try {
            x.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            y.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("testConcurrentSingleLockAccess finished");
    }

    /**
	 * Here we have two workers sharing a connection but writing to two
	 * different tables. The first one locks, the second goes on writing his
	 * table but through a second connection.
	 */
    public void testConcurrentSingleLockAccess2ndTable() {
        System.out.println("testConcurrentSingleLockAccess2ndTable started");
        Task lockTask = new LockingTaskInsertSequence("A", 10000, 2000, 5000);
        PooledWorker x = new PooledWorker("X", login, lockTask);
        Task task = new TaskInsertSequence("B", 10000);
        PooledWorker y = new PooledWorker("Y", login, task);
        x.start();
        y.start();
        try {
            x.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            y.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("testConcurrentSingleLockAccess2ndTable finished");
    }

    /**
	 * what if we have two lockers
	 * the first locks the second is on hold, the first release the lock, the
	 * second locks, the first is on hold until the 2nd unlocks.
	 */
    public void testTwoConcurrentLocks() {
        System.out.println("testTwoConcurrentLocks started");
        Task lockTask = new LockingTaskInsertSequence("A", 10000, 2000, 5000);
        PooledWorker x = new PooledWorker("X", login, lockTask);
        Task lockTask2 = new LockingTaskInsertSequence("A", 10000, 2000, 5000);
        PooledWorker y = new PooledWorker("Y", login, lockTask2);
        x.start();
        y.start();
        try {
            x.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            y.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("testTwoConcurrentLocks finished");
    }

    /**
	 * what if we have two lockers
	 * the first locks the second is on hold, the first release the lock, the
	 * second locks, the first is on hold until the 2nd unlocks.
	 */
    public void testTwoConcurrentLocks2() {
        System.out.println("testTwoConcurrentLocks2 started");
        Task lockTask = new LockingTaskInsertSequence("A", 10000, 0, 5000);
        PooledWorker x = new PooledWorker("X", login, lockTask);
        Task lockTask2 = new LockingTaskInsertSequence("A", 10000, 0, 5000);
        PooledWorker y = new PooledWorker("Y", login, lockTask2);
        x.start();
        y.start();
        try {
            x.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            y.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("testTwoConcurrentLocks2 finished");
    }

    /**
	 * what if we have six lockers
	 * they lock straight away on two different tables.
	 * just to see how many connections we get.
	 */
    public void testSixConcurrentLocks() {
        System.out.println("testSixConcurrentLocks started");
        LockingTaskInsertSequence taskA = new LockingTaskInsertSequence("A", 10000, 0, 5000);
        PooledWorker u = new PooledWorker("U", login, taskA);
        PooledWorker v = new PooledWorker("V", login, taskA.clone());
        PooledWorker w = new PooledWorker("W", login, taskA.clone());
        LockingTaskInsertSequence taskB = new LockingTaskInsertSequence("B", 10000, 0, 5000);
        PooledWorker x = new PooledWorker("X", login, taskB);
        PooledWorker y = new PooledWorker("Y", login, taskB.clone());
        PooledWorker z = new PooledWorker("Z", login, taskB.clone());
        List<PooledWorker> ws = new LinkedList<PooledWorker>();
        ws.add(u);
        ws.add(v);
        ws.add(w);
        ws.add(x);
        ws.add(y);
        ws.add(z);
        for (PooledWorker worker : ws) worker.start();
        for (PooledWorker worker : ws) try {
            worker.join();
        } catch (InterruptedException e) {
        }
        System.out.println("testSixConcurrentLocks finished");
    }

    enum TaskType {

        SIMPLE, LOCKABLE
    }

    ;

    public static Task getTask(TaskType type, String table, int inserts, int lockStart, int lockEnd) {
        if (type.equals(TaskType.SIMPLE)) {
            return new TaskInsertSequence(table, inserts);
        } else {
            return new LockingTaskInsertSequence(table, inserts, lockStart, lockEnd);
        }
    }

    enum WorkerType {

        REGULAR, POOLED
    }

    ;

    public static WorkerAbstr getWorker(WorkerType workerType, String name, OtsdbLogin login, Task task) {
        if (workerType.equals(WorkerType.POOLED)) return new PooledWorker(name, login, task); else return new RegularWorker(name, login, task);
    }

    /**
	 * with much more workers
	 */
    public void testManyWorkers() {
        String results = "";
        System.out.println("testManyWorkers started");
        for (WorkerType workerType : WorkerType.values()) {
            boolean[] booleans = { true, false };
            for (boolean locker : booleans) {
                String presentation = "****** testManyWorkers " + workerType.toString() + " " + (locker ? "with" : "without") + " lock";
                int c = 10;
                Task taskA = null;
                Task taskB = null;
                if (locker) {
                    taskB = new LockingTaskInsertSequence("B", 10 * c, 2 * c, 5 * c);
                    taskA = new LockingTaskInsertSequence("A", 10 * c, 2 * c, 5 * c);
                } else {
                    taskB = new TaskInsertSequence("B", 10 * c);
                    taskA = new TaskInsertSequence("A", 10 * c);
                }
                RegularWorker.pool = null;
                try {
                    setUp();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                System.out.println(presentation + " started");
                long start = System.currentTimeMillis();
                WorkerAbstr x = getWorker(workerType, "X", login, taskB);
                x.start();
                List<Thread> ys = new LinkedList<Thread>();
                for (int i = 0; i < 10; i++) {
                    WorkerAbstr y = getWorker(workerType, "Y" + i, login, taskA.clone());
                    y.start();
                    ys.add(y);
                }
                for (Thread y : ys) {
                    try {
                        y.join();
                    } catch (InterruptedException e) {
                    }
                }
                try {
                    x.join();
                } catch (InterruptedException e) {
                }
                long end = System.currentTimeMillis();
                results += presentation + " finished in " + (end - start) + " ms \n";
                System.out.println(presentation + " finished in " + (end - start) + " ms");
            }
            System.out.println(results);
            System.out.println("testManyWorkers finished");
        }
    }

    public static class TradingModel extends Task {

        public static int models = 10;

        int trades = 10;

        public int modelId = 0;

        public TradingModel(String table, int modelId, int inserts) {
            super(table, inserts);
            this.modelId = modelId;
            trades = inserts;
        }

        public void run() {
            for (int i = 0; i < trades; i++) {
                try {
                    Thread.sleep(Math.round(Math.random() * 10));
                } catch (InterruptedException e) {
                }
                try {
                    String[] read = { "Account", "Model" };
                    String[] write = { "Trade" };
                    worker.log("locking");
                    worker.db.lock(read, write);
                    worker.log("locked");
                    double gearing = Math.random();
                    double modelAmount = 0;
                    String selectModel = "select amount from Model " + "where modelId = " + modelId;
                    Pair<Statement, ResultSet> result = worker.db.executeQuery(selectModel);
                    while (result.second.next()) {
                        modelAmount = result.second.getDouble("amount");
                    }
                    worker.db.close(result);
                    String insert = "insert into Trade (accountId, modelId, amount, price) values ";
                    String selectAccounts = "select accountId, amount from Account " + "where modelId = " + modelId;
                    result = worker.db.executeQuery(selectAccounts);
                    boolean first = true;
                    boolean traded = false;
                    while (result.second.next()) {
                        traded = true;
                        if (first) first = false; else insert += ",";
                        String accountId = result.second.getString("accountId");
                        double accountAmount = result.second.getDouble("amount");
                        insert += "(" + accountId + ", " + modelId + ", " + accountAmount / modelAmount * gearing + ", 0)";
                    }
                    if (traded) worker.db.executeUpdate(insert);
                } catch (RequestException e) {
                    worker.log(e.getMessage());
                    fail(e.getMessage());
                    e.printStackTrace();
                } catch (SQLException e) {
                    worker.log(e.getMessage());
                    fail(e.getMessage());
                    e.printStackTrace();
                } finally {
                    try {
                        worker.db.unlock();
                    } catch (RequestException e) {
                    }
                }
            }
            try {
                worker.disconnect();
            } catch (RequestExceptionConnection e) {
                e.printStackTrace();
            }
        }

        @Override
        public void accident(int i) {
        }

        @Override
        public Task clone() {
            return new TaskInsertSequence(table, inserts);
        }
    }

    public static class Trader extends Task {

        public static int accounts = 20;

        public static int trades = 0;

        int account = 1;

        private String name;

        public Trader(String table, int inserts) {
            super(table, inserts);
            account = inserts;
            name = table;
        }

        @Override
        public void accident(int i) {
        }

        @Override
        public Task clone() {
            return new TaskInsertSequence(table, inserts);
        }

        @Override
        public void run() {
            int trials = 0;
            try {
                while (true) {
                    String[] read = { "Account", "Model" };
                    String[] write = { "Trade", "History" };
                    worker.log("locking");
                    worker.db.lock(read, write);
                    worker.log("locked");
                    trials++;
                    String queryTrades = "select tradeId, accountName , modelName , Trade.amount from Account, Trade, Model where accountName ='" + name + "' and Trade.price =0 and Trade.accountId=Account.accountId and Trade.modelId=Model.modelId";
                    Pair<Statement, ResultSet> result = worker.db.executeQuery(queryTrades);
                    while (result.second.next()) {
                        trials = 0;
                        String tradeId = result.second.getString("tradeId");
                        String accountName = result.second.getString("accountName");
                        String modelName = result.second.getString("modelName");
                        String amount = result.second.getString("amount");
                        double price = (1.0 + Math.random());
                        String update = "update Trade set price = " + price + " where tradeId = " + tradeId;
                        worker.log(accountName + ": " + modelName + " traded " + amount + " at: " + price);
                        worker.db.executeUpdate(update);
                        String flush = "insert into History select * from Trade where tradeId = " + tradeId;
                        worker.db.executeUpdate(flush);
                        String delete = "delete from Trade where tradeId = " + tradeId;
                        worker.db.executeUpdate(delete);
                        trades++;
                    }
                    worker.db.unlock();
                    if (trials > 100) return;
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                    }
                }
            } catch (RequestException e) {
                worker.log(e.getMessage());
                fail(e.getMessage());
                e.printStackTrace();
            } catch (SQLException e) {
                worker.log(e.getMessage());
                fail(e.getMessage());
                e.printStackTrace();
            } finally {
                try {
                    worker.db.unlock();
                } catch (RequestException e) {
                    e.printStackTrace();
                }
                try {
                    worker.db.disconnect();
                } catch (RequestExceptionConnection e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setUp2() throws RequestException {
        Request request = new Request();
        request.connect(login.server, login.database, login.user, login.pwd);
        String[] tables = { "Account", "Model", "Trade", "History" };
        for (String table : tables) {
            String cleanTestTable = "drop table if exists " + table;
            request.executeUpdate(cleanTestTable);
        }
        String[] tableBodies = { "create table Account (" + "`accountId` int(10) unsigned NOT NULL auto_increment, " + "`accountName` varchar(50) NOT NULL default '', " + "`amount` double(16,4) unsigned default '0.0000', " + "`modelId` int(10) unsigned, " + "PRIMARY KEY  (`accountId`))", "create table Model ( " + "`modelId` int(10) unsigned NOT NULL auto_increment, " + "`modelName` varchar(50) NOT NULL default '', " + "`amount` double(16,4) unsigned default '0.0000'," + " PRIMARY KEY  (`modelId`))", "create table Trade ( " + "`tradeId` int(10) unsigned NOT NULL auto_increment, " + "`accountId` varchar(50) NOT NULL default '', " + "`modelId` int(10) unsigned, " + "`amount` double(16,4) unsigned default '0.0000', " + "`price` double(16,4) default NULL, " + "PRIMARY KEY  (`tradeId`))", "create table History ( " + "`tradeId` int(10) unsigned NOT NULL auto_increment, " + "`accountId` varchar(50) NOT NULL default '', " + "`modelId` int(10) unsigned, " + "`amount` double(16,4) unsigned default '0.0000', " + "`price` double(16,4) default NULL, " + "PRIMARY KEY  (`tradeId`))" };
        for (String body : tableBodies) {
            request.executeUpdate(body);
        }
        int nbAccounts = Trader.accounts;
        int nbModels = TradingModel.models;
        String accounts = "insert into Account (accountName,amount,modelId) values ";
        for (int i = 1; i <= nbAccounts; i++) {
            String sql = accounts + "('A" + String.valueOf(i) + "'," + String.valueOf(Math.random() * 1000000) + "," + String.valueOf(Math.ceil(Math.random() * nbModels)) + ")";
            request.executeUpdate(sql);
        }
        String models = "insert into Model (modelName, amount) values ";
        for (int i = 1; i <= nbModels; i++) {
            String sql = models + "('M" + String.valueOf(i) + "'," + String.valueOf(Math.random() * 1000000) + ")";
            request.executeUpdate(sql);
        }
        request.disconnect();
    }

    public void testReal() {
        System.out.println("testReal started");
        try {
            setUp2();
            String results = "";
            for (WorkerType workerType : WorkerType.values()) {
                Trader.trades = 0;
                Request request = new Request();
                request.connect(login.server, login.database, login.user, login.pwd);
                String cleanTestTable = "delete from Trade";
                request.executeUpdate(cleanTestTable);
                request.disconnect();
                String presentation = "****** testReal " + workerType.toString();
                System.out.println(presentation + " started");
                long start = System.currentTimeMillis();
                int trades = 1000;
                List<Thread> threads = new LinkedList<Thread>();
                for (int i = 1; i <= TradingModel.models; i++) {
                    TradingModel modelTask = new TradingModel("table", i, trades);
                    WorkerAbstr model = getWorker(workerType, "TM" + i, login, modelTask);
                    model.start();
                    threads.add(model);
                }
                for (int i = 1; i <= Trader.accounts; i++) {
                    Trader traderTask = new Trader("A" + i, i);
                    WorkerAbstr trade = getWorker(workerType, "trader" + i, login, traderTask);
                    trade.start();
                    threads.add(trade);
                }
                for (Thread thread : threads) {
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                    }
                }
                long end = System.currentTimeMillis();
                results += presentation + " traded " + Trader.trades + " trades in " + (end - start) + " ms \n";
                System.out.println(presentation + " finished in " + (end - start) + " ms");
            }
            System.out.println(results);
            System.out.println("testManyWorkers finished");
        } catch (RequestExceptionConnection e) {
            fail("error!");
            e.printStackTrace();
        } catch (RequestExceptionNoDriver e) {
            fail("error!");
            e.printStackTrace();
        } catch (RequestException e) {
            fail("error!");
            e.printStackTrace();
        }
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
    }
}
