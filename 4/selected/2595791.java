package ch.olsen.products.util.database.otsdb.test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import junit.framework.Assert;
import junit.framework.TestCase;
import ch.olsen.products.util.database.Pair;
import ch.olsen.products.util.database.RequestException;
import ch.olsen.products.util.database.RequestException.Reason;
import ch.olsen.products.util.database.otsdb.Otsdb;

public class TestSimpleOtsdb extends TestCase {

    static final String table1 = "testOtsdbJava1";

    static final String table2 = "testOtsdbJava2";

    static final String create(String table) {
        return "CREATE TABLE `" + table + "` (" + " `testId` int(2) unsigned NOT NULL auto_increment," + " `testName` varchar(20) NOT NULL default '', " + " PRIMARY KEY  (`testId`), " + " UNIQUE KEY `testId` (`testId`) " + ") ";
    }

    static final String insert(String table) {
        return "INSERT INTO " + table + " (testName) " + "VALUES ('test1'), ('test2')";
    }

    static final String select(String table) {
        return "SELECT * FROM " + table;
    }

    static final String drop(String table) {
        return "DROP TABLE " + table;
    }

    /**
	 * simple single access
	 * @param otsdbCfg
	 */
    public static void testOtsdb(String otsdbCfg) {
        Otsdb db = new Otsdb();
        db.loadConfig(otsdbCfg);
        try {
            db.executeUpdate(create(table1));
        } catch (RequestException e) {
            Assert.fail("at test1 create: " + e);
        }
        try {
            db.executeUpdate(create(table1));
        } catch (RequestException e) {
            if (!e.getReason().equals(Reason.TABLEEXIST)) {
                Assert.fail("at test1, 2nd create: " + e);
            }
        }
        try {
            long i = db.executeUpdate(insert(table1));
            Assert.assertEquals(i, 1);
        } catch (RequestException e) {
            Assert.fail("at test1 insert: " + e);
            return;
        }
        try {
            Pair<Statement, ResultSet> result = db.executeQuery(select(table1));
            if (result.second.next()) {
                int id = result.second.getInt("testId");
                Assert.assertEquals(id, 1);
                String name = result.second.getString("testName");
                Assert.assertEquals(name, "test");
            }
        } catch (RequestException e) {
            Assert.fail("at test1 request: " + e);
            return;
        } catch (SQLException e) {
            Assert.fail("at test1 request: " + e);
            return;
        }
        try {
            db.executeUpdate(drop(table1));
        } catch (RequestException e) {
            Assert.fail("at test1 drop: " + e);
            return;
        }
    }

    public static void testOtsdbLock(String otsdbCfg) {
        final Otsdb db1 = new Otsdb();
        db1.loadConfig(otsdbCfg);
        Otsdb db2 = new Otsdb();
        try {
            db1.executeUpdate(create(table1));
            db1.executeUpdate(insert(table1));
            Pair<Statement, ResultSet> result1 = db1.executeQuery(select(table1));
            while (result1.second.next()) {
                Pair<Statement, ResultSet> result2 = db2.executeQuery(select(table1));
                if (result2.second.next()) {
                    int id = result2.second.getInt("testId");
                    Assert.assertEquals(id, 1);
                    String name = result2.second.getString("testName");
                    Assert.assertEquals(name, "test1");
                }
                int id = result1.second.getInt("testId");
                String name = result1.second.getString("testName");
                System.out.println("read: " + id + ", " + name);
            }
        } catch (RequestException e) {
            Assert.fail("at create: " + e);
        } catch (SQLException e) {
            Assert.fail("at request: " + e);
            return;
        }
        String[] read = {};
        String[] write = { table1 };
        try {
            db1.lock(read, write);
        } catch (RequestException e) {
            Assert.fail("at create: " + e);
        }
        try {
            db1.lock(read, write);
        } catch (RequestException e) {
            if (!e.getReason().equals(Reason.LOCKED)) {
                Assert.fail("at create: " + e);
            }
        }
        new Thread() {

            public void run() {
                System.out.println("db1 is waiting: ");
                try {
                    for (int i = 0; i < 20; i++) {
                        sleep(100);
                        System.out.print(".");
                    }
                    System.out.println();
                    System.out.println("db1 release the lock");
                    db1.unlock();
                } catch (Exception e) {
                }
            }
        }.start();
        try {
            System.out.println("db2 request the lock");
            db2.lock(read, write);
            System.out.println("db2 got the lock");
        } catch (RequestException e) {
            Assert.fail("at second connection's lock: " + e);
        }
        try {
            db1.executeUpdate(create(table2));
            db1.executeUpdate(insert(table2));
            Pair<Statement, ResultSet> result1 = db1.executeQuery(select(table2));
            if (result1.second.next()) {
                int id = result1.second.getInt("testId");
                Assert.assertEquals(id, 1);
                String name = result1.second.getString("testName");
                Assert.assertEquals(name, "test1");
            }
        } catch (RequestException e) {
            Assert.fail("at create: " + e);
        } catch (SQLException e) {
            Assert.fail("at request: " + e);
        }
        try {
            String[] read2 = {};
            String[] write2 = { table2 };
            db1.lock(read2, write2);
            db1.executeUpdate(insert(table2));
            Pair<Statement, ResultSet> result1 = db1.executeQuery(select(table2));
            if (result1.second.next()) {
                int id = result1.second.getInt("testId");
                Assert.assertEquals(id, 1);
                String name = result1.second.getString("testName");
                Assert.assertEquals(name, "test1");
            }
            db1.unlock();
        } catch (RequestException e) {
            Assert.fail("at create: " + e);
        } catch (SQLException e) {
            Assert.fail("at request: " + e);
        }
        Pair<Statement, ResultSet> result2;
        try {
            result2 = db2.executeQuery(select(table2));
            if (result2.second.next()) {
                int id = result2.second.getInt("testId");
                Assert.assertEquals(id, 1);
                String name = result2.second.getString("testName");
                Assert.assertEquals(name, "test1");
            }
        } catch (RequestException e) {
            if (!e.getReason().equals(Reason.LOCKED)) {
                Assert.fail("at create: " + e);
            }
        } catch (SQLException e) {
            Assert.fail("at request: " + e);
        }
        try {
            db2.executeUpdate(drop(table1));
            db2.unlock();
            db2.executeUpdate(drop(table2));
        } catch (RequestException e) {
            Assert.fail("at create: " + e);
        }
    }

    public static void testOtsdbLock2(String otsdbCfg) {
        final Otsdb db1 = new Otsdb();
        db1.loadConfig(otsdbCfg);
        Otsdb db2 = new Otsdb();
        try {
            db1.executeUpdate(create(table1));
            db1.executeUpdate(insert(table1));
            db1.executeUpdate(create(table2));
            db1.executeUpdate(insert(table2));
            String[] read1 = {};
            String[] write1 = { table1 };
            db1.lock(read1, write1);
            Pair<Statement, ResultSet> result1 = db2.executeQuery(select(table2));
            if (result1.second.next()) {
                int id = result1.second.getInt("testId");
                Assert.assertEquals(id, 1);
                String name = result1.second.getString("testName");
                Assert.assertEquals(name, "test1");
            }
            db1.executeUpdate(drop(table1));
            db1.executeUpdate(drop(table2));
        } catch (RequestException e) {
            Assert.fail("at create: " + e);
        } catch (SQLException e) {
            Assert.fail("at request: " + e);
        }
    }

    public static void testOtsdbLock3(String otsdbCfg) {
        final Otsdb db1 = new Otsdb();
        db1.loadConfig(otsdbCfg);
        try {
            db1.executeUpdate(create(table1));
            db1.executeUpdate(insert(table1));
            db1.executeUpdate(create(table2));
            db1.executeUpdate(insert(table2));
            Pair<Statement, ResultSet> result1 = db1.executeQuery(select(table2));
            boolean read = false;
            while (result1.second.next()) {
                if (read) {
                    String[] read1 = {};
                    String[] write1 = { table1 };
                    db1.lock(read1, write1);
                }
                int id = result1.second.getInt("testId");
                String name = result1.second.getString("testName");
                System.out.println("read: " + id + ", " + name);
                read = true;
            }
            db1.unlock();
            db1.executeUpdate(drop(table1));
            db1.executeUpdate(drop(table2));
        } catch (RequestException e) {
            Assert.fail("at create: " + e);
        } catch (SQLException e) {
            Assert.fail("at request: " + e);
        }
    }

    public static void main(String[] args) {
        String dbCfg = "";
        for (int i = 0; i < args.length; i++) {
            if (args[i].compareTo("-db") == 0) {
                if (++i < args.length) {
                    dbCfg = args[i];
                }
            }
        }
        if (dbCfg.length() == 0) {
            Assert.fail("no config file provided");
        }
        testOtsdbLock(dbCfg);
        testOtsdbLock2(dbCfg);
        testOtsdbLock3(dbCfg);
    }
}
