package cn.sh.fang.chenance.data.dao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.Persistence;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

public abstract class BaseService {

    private static final String PROP_DB_FILEPATH = "db.filepath";

    private static final String CHENANCE_PROPERTIES = "chenance.properties";

    static Logger LOG = Logger.getLogger(BaseService.class);

    static EntityManagerFactory factory;

    protected static EntityManager em;

    static EntityTransaction t;

    public static String filepath = null;

    public static String jdbcUrl;

    public static String driverClass = "org.sqlite.JDBC";

    static Properties props = new Properties();

    static boolean needMigrate = false;

    static Connection conn;

    static {
        InputStream is;
        try {
            is = new FileInputStream(new File(CHENANCE_PROPERTIES));
            props.load(is);
            filepath = props.getProperty(PROP_DB_FILEPATH);
        } catch (FileNotFoundException e) {
            LOG.info("chenance.properties not found");
        } catch (IOException e) {
            LOG.error("Read chenance.properties failed", e);
            System.exit(0);
        } finally {
            if (filepath == null) {
                filepath = System.getProperty("user.home") + File.separator + "chenance.db";
            }
        }
        LOG.info("Open db from: " + filepath);
        jdbcUrl = "jdbc:sqlite:" + filepath;
    }

    public BaseService() {
    }

    public static void init() throws SQLException {
        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException e) {
        }
        if (new File(filepath).exists() == false) {
            LOG.warn("data file not exists, start creating table ...");
            conn = DriverManager.getConnection(jdbcUrl);
            createTable();
        } else {
            conn = DriverManager.getConnection(jdbcUrl);
        }
        if (needMigrate) {
            LOG.warn("merging old data ...");
            migrateData();
            new File(System.getProperty("user.home") + "/chenance/").renameTo(new File(System.getProperty("user.home") + "/chenance.bak/"));
            LOG.warn("migration finished!");
        }
        String oldVer = getLocalDataVersion();
        String newVer = getCurrentDataVersion();
        if (Float.valueOf(oldVer) < Float.valueOf(newVer)) {
            try {
                FileUtils.copyFile(new File(filepath), new File(filepath + ".bak"));
            } catch (IOException e) {
                LOG.fatal("Backuping database failed", e);
                throw new SQLException("Backuping database failed", e);
            }
            updateData(oldVer, newVer);
        }
        try {
            conn.close();
        } catch (SQLException e) {
            LOG.warn(e);
        }
        HashMap<String, String> props = new HashMap<String, String>();
        props.put("hibernate.connection.driver_class", driverClass);
        props.put("hibernate.connection.url", jdbcUrl);
        props.put("hibernate.dialect", "org.hibernate.dialect.SQLiteDialect");
        factory = Persistence.createEntityManagerFactory("chenance-data", props);
        if (em == null) {
            openSession();
        }
    }

    private static void openSession() {
        LOG.debug("creating em");
        em = factory.createEntityManager();
        em.setFlushMode(FlushModeType.AUTO);
        t = em.getTransaction();
        t.begin();
    }

    private static void migrateData() throws SQLException {
        String csv;
        try {
            Statement stmt = conn.createStatement();
            csv = readAsString(new FileInputStream(System.getProperty("user.home") + "/chenance/account.csv"), "UTF-8");
            String[] lines = csv.split("\n");
            LOG.debug(lines);
            stmt.execute("delete from t_account");
            for (String s : lines) {
                if (s == lines[0]) {
                    continue;
                }
                LOG.debug(s);
                String sql = "insert into t_account values(";
                String[] datas = s.split(",");
                String[] datas2 = new String[datas.length + 1];
                for (int i = 0; i < datas.length; i++) {
                    datas2[i <= 10 ? i : i + 1] = datas[i];
                }
                datas = datas2;
                datas[11] = "31";
                for (int i = 0; i < datas.length; i++) {
                    if (datas[i].equals("")) {
                        sql += "null,";
                    } else if (i == 12 || i == 13) {
                        sql += String.valueOf(new SimpleDateFormat("\"yyyy-MM-dd HH:mm:ss.SSS\"").parse(datas[i]).getTime());
                        sql += ",";
                    } else if (i == 15) {
                        sql += datas[i].equals("\"FALSE\"") ? "0" : "1";
                    } else if (i == 0 || (i >= 7 && i <= 10)) {
                        sql += datas[i].replaceAll("\"", "") + ",";
                    } else {
                        sql += datas[i] + ",";
                    }
                }
                sql += ")";
                LOG.debug(sql);
                stmt.execute(sql);
            }
            stmt.execute("update sqlite_sequence set seq = (select max(id) from t_account) where name = 't_account'");
            csv = readAsString(new FileInputStream(System.getProperty("user.home") + "/chenance/category.csv"), "UTF-8");
            lines = csv.split("\n");
            LOG.debug(lines);
            stmt.execute("delete from t_category");
            for (String s : lines) {
                if (s == lines[0]) {
                    continue;
                }
                LOG.debug(s);
                String sql = "insert into t_category values(";
                String[] datas = s.split(",");
                for (int i = 0; i < datas.length; i++) {
                    if (datas[i].equals("")) {
                        sql += "null,";
                    } else if (i == 5 || i == 6) {
                        sql += String.valueOf(new SimpleDateFormat("\"yyyy-MM-dd HH:mm:ss.SSS\"").parse(datas[i]).getTime());
                        sql += ",";
                    } else if (i == 8) {
                        sql += datas[i].equals("\"FALSE\"") ? "0" : "1";
                    } else if (i == 0 || i == 1 || i == 4 || i == 5) {
                        sql += datas[i].replaceAll("\"", "") + ",";
                    } else {
                        sql += datas[i] + ",";
                    }
                }
                sql += ")";
                LOG.debug(sql);
                stmt.execute(sql);
            }
            stmt.execute("update sqlite_sequence set seq = (select max(id) from t_category) where name = 't_category'");
            csv = readAsString(new FileInputStream(System.getProperty("user.home") + "/chenance/transaction.csv"), "UTF-8");
            lines = csv.split("\n");
            LOG.debug(lines);
            stmt.execute("delete from t_transaction");
            for (String s : lines) {
                if (s == lines[0]) {
                    continue;
                }
                LOG.debug(s);
                String sql = "insert into t_transaction values(";
                String[] datas = s.split(",");
                for (int i = 0; i < datas.length; i++) {
                    if (datas[i].equals("")) {
                        sql += "null,";
                    } else if (i == 2 || i == 11 || i == 12) {
                        sql += String.valueOf(new SimpleDateFormat("\"yyyy-MM-dd HH:mm:ss.S\"").parse(datas[i]).getTime());
                        sql += ",";
                    } else if (i == 14) {
                        sql += datas[i].equals("\"FALSE\"") ? "0" : "1";
                    } else if (i == 8) {
                        sql += datas[i].equals("\"FALSE\"") ? "0," : "1,";
                    } else if (i >= 0 && i <= 10) {
                        sql += datas[i].replaceAll("\"", "") + ",";
                    } else {
                        sql += datas[i] + ",";
                    }
                }
                sql += ")";
                LOG.debug(sql);
                stmt.execute(sql);
            }
            stmt.execute("update sqlite_sequence set seq = (select max(id) from t_transaction) where name = 't_transaction'");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private static String getCurrentDataVersion() {
        InputStream as = BaseService.class.getResourceAsStream("/META-INF/VERSION");
        if (as == null) {
            LOG.debug("impl-ver: 0");
            return "0";
        } else {
            BufferedReader r = new BufferedReader(new InputStreamReader(as));
            String ver = null;
            try {
                ver = r.readLine();
            } catch (IOException e) {
                LOG.error(e);
            }
            LOG.debug("impl-ver:" + ver);
            return ver;
        }
    }

    private static String getLocalDataVersion() throws SQLException {
        String sql = "select key, value from t_setting where key = 'chenance.data.version'";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        if (rs.next()) {
            return rs.getString(2);
        } else {
            return "99999";
        }
    }

    static String[] vers = { "1.0", "1.1", "1.2", "1.3", "1.4" };

    private static void updateData(String oldVer, String newVer) throws SQLException {
        LOG.warn("Updating database from version " + oldVer + " to " + newVer);
        ArrayList<String> a = new ArrayList<String>();
        org.apache.commons.collections.CollectionUtils.addAll(a, vers);
        int i = a.indexOf(oldVer);
        for (int j = i; j < vers.length - 1; j++) {
            execUpdateSql(vers[j], vers[j + 1]);
        }
        try {
            conn.commit();
        } catch (SQLException e) {
            LOG.error("commit update failed", e);
        }
        updateVersion(newVer);
        LOG.warn("Updating database finished");
    }

    protected static void execUpdateSql(String from, String to) throws SQLException {
        LOG.warn("Updating database from version " + from + " to " + to);
        try {
            InputStream is = BaseService.class.getResourceAsStream("/sql/upgrades/" + from + "-" + to + ".sql");
            if (is == null) {
                LOG.info("no database changes from " + from + " to " + to);
                return;
            }
            String sql = null;
            sql = readAsString(is, "Shift-JIS");
            LOG.debug(sql);
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(sql);
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    private static void updateVersion(String ver) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("update t_setting set value = '" + ver + "' where key = 'chenance.data.version'");
        LOG.warn("Updated to " + ver);
    }

    public static void createTable() throws SQLException {
        Statement stmt = conn.createStatement();
        try {
            String sql;
            sql = readAsString(BaseService.class.getResourceAsStream("/sql/db.sql"), "Shift-JIS");
            String[] sqls = sql.split(";");
            for (String s : sqls) {
                if (s == sqls[sqls.length - 1]) {
                    break;
                }
                LOG.debug(s);
                stmt.execute(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                stmt.close();
            } catch (SQLException e) {
                LOG.warn(e);
            }
        }
    }

    public static String readAsString(InputStream is, String encoding) throws IOException {
        StringBuffer buf = new StringBuffer();
        InputStreamReader in = new InputStreamReader(is, encoding);
        BufferedReader r = new BufferedReader(in);
        try {
            for (String s = r.readLine(); s != null; s = r.readLine()) {
                buf.append(s).append('\n');
            }
            return buf.toString();
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                in.close();
            } catch (Exception e) {
            }
        }
    }

    public static void shutdown() {
        closeSession();
        factory.close();
        factory = null;
    }

    private static void closeSession() {
        t.commit();
        em.close();
        t = null;
        em = null;
    }

    public static void flushSession() {
        closeSession();
        openSession();
    }

    public static void moveTo(String newdir) throws IOException {
        newdir = newdir + File.separator + "chenance.db";
        BaseService.shutdown();
        File f = null;
        try {
            f = new File(BaseService.filepath);
            FileUtils.copyFile(f, new File(newdir));
        } catch (IOException e) {
            throw new IOException("Move .db file failed", e);
        }
        try {
            jdbcUrl = "jdbc:sqlite:" + newdir;
            BaseService.init();
        } catch (SQLException e) {
            throw new IOException("Reopen .db file failed", e);
        }
        try {
            FileUtils.moveFile(f, new File(BaseService.filepath + "." + new Date().getTime() + ".bak"));
        } catch (IOException e) {
            throw new IOException("Backup .db file failed", e);
        }
        BaseService.filepath = newdir;
        props.setProperty(PROP_DB_FILEPATH, newdir);
        try {
            props.store(new FileOutputStream(CHENANCE_PROPERTIES), null);
        } catch (FileNotFoundException e) {
            throw new IOException("Cannot save chenance.properties", e);
        } catch (IOException e) {
            throw new IOException("Cannot save chenance.properties", e);
        }
    }
}
