package Watermill.relational;

import java.sql.*;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import javax.swing.JOptionPane;
import org.apache.log4j.Logger;
import Watermill.interfaces.Document;
import Watermill.interfaces.Identifier;
import Watermill.interfaces.Identifiers;
import Watermill.interfaces.Value;
import Watermill.kernel.*;
import Watermill.kernel.constraints.GlobalConstraint;
import Watermill.kernel.constraints.GlobalConstraints;
import Watermill.kernel.constraints.LocalConstraint;
import Watermill.kernel.constraints.LocalConstraints;
import Watermill.kernel.pools.SAPPool;
import Watermill.util.ValueFactory;

/**
 * SqlDB is the sql implementation of Db
 * @author Julien Lafaye (julien.lafaye@cnam.fr)
 */
public abstract class SqlDb implements Document {

    private static final Logger logger = Logger.getLogger(SqlDb.class);

    /**
	 *
	 */
    private static final long serialVersionUID = 5624430574213005370L;

    public static final boolean F_CREATE = true;

    public static final boolean F_NOCREATE = false;

    protected transient Connection connection;

    public transient ResultSet pairReading;

    private transient PreparedStatement stpstmt;

    protected String host;

    protected String name;

    protected String password;

    protected String user;

    protected String defaultName;

    public SqlDb(String n, String h, String p, String u) throws WatermillException {
        host = h;
        name = n;
        password = p;
        connection = null;
        user = u;
    }

    public SqlDb() {
        connection = null;
    }

    public String getName() {
        return name;
    }

    /**
	 * Return the hostname running the database
	 */
    public String getHost() {
        return host;
    }

    public void init() throws Exception {
    }

    public Connection getConnection() throws WatermillDbConnectionException {
        try {
            if (connection == null || connection.isClosed()) {
                connection = getNewConnection();
            }
            return connection;
        } catch (Exception e) {
            e.printStackTrace();
            throw new WatermillDbConnectionException("Connection failed:" + e.getMessage());
        }
    }

    /**
	 * Get another connection to the underlying database. May be
	 * useful to perform simultaneous queries over the database
	 */
    public Connection getNewConnection() throws Exception {
        String dbtype = Constant.getParameter("dbType");
        Connection c = DriverManager.getConnection("jdbc:" + dbtype + "://" + host + "/" + name, user, password);
        DatabaseMetaData dbmd = c.getMetaData();
        logger.debug("Connection to " + dbmd.getDatabaseProductName() + " " + dbmd.getDatabaseProductVersion() + "-" + name + " successful");
        return c;
    }

    public Connection getTransactionalConnection() throws Exception {
        Connection c = DriverManager.getConnection("jdbc:" + Constant.getParameter("dbType") + "://" + host + "/" + name, user, password);
        c.setAutoCommit(false);
        return c;
    }

    /**
	 * Drop a table.
	 */
    public void dropTable(String tableName) {
        try {
            Statement s = getConnection().createStatement();
            String query = "DROP TABLE " + tableName;
            s.executeUpdate(query);
            s.close();
            logger.debug("Table " + tableName + " dropped.");
        } catch (Exception ex) {
        }
    }

    /**
	 * Create a bench product table.
	 * @param tableName Destination products table
	 */
    public void createBenchProductTable(String tableName) throws WatermillDbConnectionException {
        try {
            Statement s = getConnection().createStatement();
            String query = "CREATE TABLE " + tableName + "(" + "idp VARCHAR(50)," + "name VARCHAR(100)," + "cost int," + "stoc int," + "primary key (idp))";
            s.executeUpdate(query);
        } catch (SQLException e) {
            throw new WatermillDbConnectionException(e.getMessage());
        }
    }

    /**
	 * Create a bench sales table.
	 * @param tableName Destination sales table
	 */
    public void createBenchSalesTable(String tableName) throws WatermillDbConnectionException {
        try {
            Statement s = connection.createStatement();
            String query = "CREATE TABLE " + tableName + "(" + "idc VARCHAR(50)," + "idp VARCHAR(50)," + "primary key (idc,idp))";
            s.executeUpdate(query);
        } catch (SQLException e) {
            throw new WatermillDbConnectionException(e.getMessage());
        }
    }

    /**
	 * Protected query against the database.
	 */
    public void execute(String query) throws WatermillDbConnectionException {
        try {
            Statement stm = getConnection().createStatement();
            logger.debug(query);
            Chrono.setMode(Chrono.QUERY_MODE);
            stm.execute(query);
            Chrono.setMode(Chrono.CPU_MODE);
            stm.close();
        } catch (SQLException e) {
            Chrono.setMode(Chrono.CPU_MODE);
            throw new WatermillDbConnectionException(e.getMessage());
        }
    }

    public ResultSet executeQuery(String query, int resultSetType, int resultSetConcurrency) throws WatermillDbConnectionException {
        try {
            Statement stm = getConnection().createStatement(resultSetType, resultSetConcurrency);
            logger.debug(query);
            Chrono.setMode(Chrono.QUERY_MODE);
            if (stm.execute(query)) {
                java.sql.ResultSet rs = stm.getResultSet();
                Chrono.setMode(Chrono.CPU_MODE);
                return rs;
            } else {
                Chrono.setMode(Chrono.CPU_MODE);
                return null;
            }
        } catch (SQLException e) {
            Chrono.setMode(Chrono.CPU_MODE);
            throw new WatermillDbConnectionException(e.getMessage());
        }
    }

    /**
	 * Protected query against the database, with a result.
	 */
    public ResultSet executeQuery(String query) throws WatermillDbConnectionException {
        return executeQuery(query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    }

    /**
	 * Drop the database given in argument.
	 * DEPRECATED: track where this function is used
	 */
    protected void dropDb(String defaultDb) throws Exception {
        String oldDb = name;
        name = defaultDb;
        if (connection != null) {
            connection.close();
        }
        Statement s = getConnection().createStatement();
        logger.debug("Result of DROP db :" + s.executeUpdate("DROP DATABASE " + oldDb));
        s.close();
        connection.close();
        name = oldDb;
    }

    public void create(String dbname) throws WatermillException {
        logger.debug("Call to create");
        try {
            Statement s = getConnection().createStatement();
            String createQuery = "CREATE DATABASE " + dbname;
            logger.debug(createQuery);
            s.execute(createQuery);
            getConnection().close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new WatermillException(e.toString());
        }
    }

    public void drop(String dbname) throws WatermillException {
        try {
            Statement s = getConnection().createStatement();
            String createQuery = "DROP DATABASE " + dbname;
            logger.debug(createQuery);
            s.execute(createQuery);
            getConnection().close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new WatermillException(e.toString());
        }
    }

    /**
	 * Test whether a specific db is available or not.
	 */
    public boolean testDb(String dbname) throws WatermillException {
        throw new WatermillException("ERROR: testDb implemented only in subclasses;");
    }

    /**
	 * Delete a database
	 */
    public void deleteDb(String dbName) throws WatermillException {
        try {
            Statement s = getConnection().createStatement();
            logger.debug("Result of DROP db :" + s.executeUpdate("DROP DATABASE " + dbName));
            s.close();
        } catch (SQLException e) {
            throw new WatermillException(e.toString());
        }
    }

    /**
	 * Find a parameter value given its primary key
	 */
    public Value getValue(Identifier iid) {
        try {
            RIdentifier id = (RIdentifier) iid;
            Statement stm = getConnection().createStatement();
            RKeyValue kv = id.getKeyValue();
            RKey k = kv.getKey();
            String keyAttribute = k.getName();
            String keyTable = k.getTable();
            String keyValue = kv.getKeyRef();
            String valueAttribute = id.getName();
            Chrono.setMode(Chrono.QUERY_MODE);
            String queryString = "SELECT " + valueAttribute + " " + "FROM " + keyTable + " " + "WHERE " + keyAttribute + "='" + keyValue + "'";
            logger.debug("queryString=" + queryString);
            ResultSet rs = stm.executeQuery(queryString);
            rs.next();
            Value v = ValueFactory.createValue(rs.getString(1));
            stm.close();
            Chrono.setMode(Chrono.CPU_MODE);
            return v;
        } catch (Exception e) {
            logger.debug(e);
            return null;
        }
    }

    public void setValue(Identifier iid, Value v) {
        try {
            RIdentifier id = (RIdentifier) iid;
            Statement stm = getConnection().createStatement();
            RKeyValue kv = id.getKeyValue();
            RKey k = kv.getKey();
            String keyAttribute = k.getName();
            String keyTable = k.getTable();
            String keyValue = kv.getKeyRef();
            String valueAttribute = id.getName();
            String queryString = "UPDATE " + keyTable + " " + "SET " + valueAttribute + "=" + v + " " + "WHERE " + keyAttribute + "='" + keyValue + "'";
            logger.debug(queryString);
            Chrono.setMode(Chrono.QUERY_MODE);
            stm.execute(queryString);
            stm.close();
            Chrono.setMode(Chrono.CPU_MODE);
        } catch (Exception e) {
            logger.fatal(e);
        }
    }

    public void addValue(Identifier iid, Value v) {
        try {
            RIdentifier id = (RIdentifier) iid;
            Statement stm = getConnection().createStatement();
            RKeyValue kv = id.getKeyValue();
            RKey k = kv.getKey();
            String keyAttribute = k.getName();
            String keyTable = k.getTable();
            String keyValue = kv.getKeyRef();
            String valueAttribute = id.getName();
            String queryString = "UPDATE " + keyTable + " " + "SET " + valueAttribute + "=" + valueAttribute + "+" + v + "WHERE " + keyAttribute + "='" + keyValue + "'";
            logger.debug(queryString);
            Chrono.setMode(Chrono.QUERY_MODE);
            stm.execute(queryString);
            Chrono.setMode(Chrono.CPU_MODE);
            stm.close();
        } catch (Exception e) {
            logger.fatal(e);
        }
    }

    public Schema getSchema() {
        return null;
    }

    public Identifiers getModifiableValuesIdentifiers(LocalConstraints lcs) {
        Identifiers res = new VectorIdentifiers();
        for (Enumeration e = lcs.elements(); e.hasMoreElements(); ) {
            LocalConstraint lc = (LocalConstraint) (e.nextElement());
            res.addAll(getModifiableValuesIdentifiers(lc));
        }
        return res;
    }

    public RKey getTableKey(String tableName) {
        try {
            DatabaseMetaData dbmd = getConnection().getMetaData();
            ResultSet rs = dbmd.getPrimaryKeys(null, null, tableName);
            rs.next();
            return new RKey(tableName, rs.getString("COLUMN_NAME"));
        } catch (Exception e) {
            logger.fatal(e);
            return null;
        }
    }

    public Identifiers getModifiableValuesIdentifiers(LocalConstraint lc) {
        Identifiers id = new VectorIdentifiers();
        try {
            Statement stm = getConnection().createStatement();
            RKey key = getTableKey(lc.tableName);
            String query = "SELECT " + key.getName() + " " + "FROM " + lc.tableName;
            logger.debug(query);
            Chrono.setMode(Chrono.QUERY_MODE);
            ResultSet rs = stm.executeQuery(query);
            while (rs.next()) {
                RKeyValue kv = new RKeyValue(key, rs.getString(key.getName()));
                id.add(new RIdentifier(kv, lc.attributeName, lc.distortion));
                logger.debug("Adding local distortion " + lc.distortion + " to " + kv.toString());
            }
            stm.close();
            Chrono.setMode(Chrono.CPU_MODE);
        } catch (Exception e) {
            logger.fatal(e);
        }
        return id;
    }

    /**
	 * Get the dependency matrix from a list of global constraints
	 * @param gcs Global constraints
	 */
    public DependencyMatrix getDependency(GlobalConstraints gcs) {
        DependencyMatrix res = new DependencyMatrix();
        for (Enumeration e = gcs.elements(); e.hasMoreElements(); ) {
            GlobalConstraint gc = (GlobalConstraint) (e.nextElement());
            res.add(getDependency(gc));
        }
        return res;
    }

    /**
	 * Get a list of items identifiers involved
	 * in a global constraint.
	 * @param gc A global constraint
	 */
    protected Identifiers getDependency(GlobalConstraint gc) {
        Identifiers id = new VectorIdentifiers();
        try {
            Statement stm = getConnection().createStatement();
            logger.debug("Condition=" + gc.condition + "|");
            String query = "SELECT " + gc.keyName + " " + "FROM " + gc.tableName + " " + "WHERE " + gc.condition;
            RKey key = new RKey(gc.tableName, gc.keyName);
            Chrono.setMode(Chrono.QUERY_MODE);
            ResultSet rs = stm.executeQuery(query);
            while (rs.next()) {
                RKeyValue kv = new RKeyValue(key, rs.getString(gc.keyName));
                id.add(new RIdentifier(kv, gc.attributeName));
            }
            Chrono.setMode(Chrono.CPU_MODE);
            stm.close();
        } catch (Exception e) {
            logger.fatal(e);
        }
        return id;
    }

    /**
	 * Check a bunch of local constraints
	 * @param document The checked document
	 * @param lcs      The checked local constraints
	 */
    public boolean checkLocalConstraints(Document d, LocalConstraints lc) {
        for (Enumeration e = lc.elements(); e.hasMoreElements(); ) {
            if (!checkLocalConstraint(d, (LocalConstraint) e.nextElement())) return false;
        }
        return true;
    }

    /**
	 * Check a local constraint against a document
	 * @param document The checked document
	 * @param lc       The checked local constraint
	 */
    public boolean checkLocalConstraint(Document d, LocalConstraint lc) {
        try {
            String queryString = "SELECT " + lc.attributeName + " " + "FROM " + lc.tableName + " " + "ORDER BY " + lc.keyName;
            logger.info("CheckLocalConstraint:" + queryString);
            SqlDb d2 = (SqlDb) d;
            Statement stm1 = getConnection().createStatement();
            Statement stm2 = d2.getConnection().createStatement();
            Chrono.setMode(Chrono.QUERY_MODE);
            ResultSet rs1 = stm1.executeQuery(queryString);
            ResultSet rs2 = stm2.executeQuery(queryString);
            while (rs1.next() && rs2.next()) {
                int val1 = Integer.parseInt(rs1.getString(1));
                int val2 = Integer.parseInt(rs2.getString(1));
                if (Math.abs(val2 - val1) > lc.distortion) {
                    return false;
                }
            }
            stm1.close();
            stm2.close();
            Chrono.setMode(Chrono.CPU_MODE);
            return true;
        } catch (Exception e) {
            logger.fatal(e);
            return false;
        }
    }

    /**
	 * Check a bunch of global constraints
	 * @param document The checked document
	 * @param lc       The checked global constraints
	 */
    public boolean checkGlobalConstraints(Document d, GlobalConstraints gcs) {
        int i = 1;
        long start = System.currentTimeMillis();
        for (Enumeration e = gcs.elements(); e.hasMoreElements(); i++) {
            if (!checkGlobalConstraint(d, (GlobalConstraint) e.nextElement())) return false;
        }
        long end = System.currentTimeMillis();
        logger.info("Checking the constraints took " + (start - end));
        return true;
    }

    /**
	 * Check a global constraint
	 * @param document The checked document
	 * @param lc       The checked global constraint
	 */
    public boolean checkGlobalConstraint(Document d, GlobalConstraint gc) {
        try {
            String queryString = "SELECT sum(" + gc.attributeName + ") " + "FROM " + gc.tableName + " " + "WHERE " + gc.condition;
            SqlDb d2 = (SqlDb) d;
            Statement stm1 = getConnection().createStatement();
            Statement stm2 = d2.getConnection().createStatement();
            Chrono.setMode(Chrono.QUERY_MODE);
            ResultSet rs1 = stm1.executeQuery(queryString);
            ResultSet rs2 = stm2.executeQuery(queryString);
            rs1.next();
            rs2.next();
            int val1 = Integer.parseInt(rs1.getString(1));
            int val2 = Integer.parseInt(rs2.getString(1));
            Chrono.setMode(Chrono.CPU_MODE);
            stm1.close();
            stm2.close();
            return (Math.abs(val1 - val2) <= gc.distortion);
        } catch (Exception e) {
            logger.fatal(e);
            return false;
        }
    }

    /**
	 * Prettyprint
	 */
    public String toString() {
        return "host=" + host + " " + "name=" + name;
    }

    /**
	 * Prepare an empty table of Pairs
	 * @param name Name of the target Pairs relation
	 */
    public void createPairs(String name) {
        dropTable(name);
        try {
            Statement stm = getConnection().createStatement();
            String query = "CREATE TABLE " + name + " (" + "key1 integer," + "key2 integer," + "PRIMARY KEY (key1,key2))";
            logger.info("CREATION PAIRS:" + query);
            Chrono.setMode(Chrono.QUERY_MODE);
            stm.execute(query);
            Chrono.setMode(Chrono.CPU_MODE);
        } catch (Exception e) {
            logger.fatal(e);
        }
    }

    /**
	 * Insert a pair into a set of pairs.
	 * @param name Name of the target pairs relation
	 * @param k1   First identifier
	 * @param k2   Second identifier
	 */
    public void addIntoPairs(String name, String k1, String k2) {
        try {
            Statement s = getConnection().createStatement();
            String query = "INSERT INTO " + name + " " + "VALUES('" + k1 + "','" + k2 + "')";
            Chrono.setMode(Chrono.QUERY_MODE);
            s.execute(query);
            Chrono.setMode(Chrono.CPU_MODE);
        } catch (Exception e) {
            logger.fatal(e);
        }
    }

    /**
	 * Insert a pair into a set of pairs.
	 * @param name Name of the target pairs relation
	 * @param p    A pair to insert
	 */
    public void addIntoPairs(String name, Pair p) {
        RIdentifier r1 = (RIdentifier) p.first;
        RIdentifier r2 = (RIdentifier) p.second;
        addIntoPairs(name, r1.kv.getKeyRef(), r2.kv.getKeyRef());
    }

    public int getPairCount(String name) throws SQLException, WatermillDbConnectionException {
        Statement s = getConnection().createStatement();
        String query = "SELECT count(*) FROM " + name;
        ResultSet rs = s.executeQuery(query);
        int count = 0;
        if (rs.next()) {
            count = rs.getInt(1);
        } else {
            logger.fatal("No pairs in pairs table, should never happen");
        }
        rs.close();
        return count;
    }

    /**
	 * Prepare a resultSet listing all pairs
	 * @param name Table containing pairs
	 */
    public int resetPairReading(String name) {
        int count = 0;
        pairReading = null;
        try {
            count = getPairCount(name);
            Statement s = getConnection().createStatement();
            String query = "SELECT key1,key2 FROM " + name;
            Chrono.setMode(Chrono.QUERY_MODE);
            pairReading = s.executeQuery(query);
            Chrono.setMode(Chrono.CPU_MODE);
        } catch (Exception e) {
            logger.error(e);
        }
        return count;
    }

    /**
	 * Retrieve nextPair in the current set of pairs
	 */
    public Pair nextPair(String tableName, String keyName, String attributeName, int maxDistortion) {
        try {
            if (pairReading.next()) {
                RKey k = new RKey(tableName, keyName);
                RKeyValue kv1 = new RKeyValue(k, pairReading.getString(1));
                RKeyValue kv2 = new RKeyValue(k, pairReading.getString(2));
                return new Pair(new RIdentifier(kv1, attributeName, maxDistortion), new RIdentifier(kv2, attributeName, maxDistortion));
            } else {
                pairReading.close();
                return null;
            }
        } catch (SQLException e) {
            logger.fatal(e);
        }
        return null;
    }

    /**
	 * Compute and store pairs of tuples having same dependencies
	 * @param name Destination table to store pairs
	 * @param depMatrixName Destination table to store dependency matrix
	 * @param gcs Global constraints
	 */
    public void computePairs(String pairsListName, String depMatrixName, GlobalConstraints gcs) {
        createPairs(pairsListName);
        buildPairs(pairsListName, depMatrixName, gcs);
    }

    /**
	 * Prepare a statement to get a list of keys ordered by dependencies.
	 * Since it is huge, special statements are to be used. This function heavily
	 * depends on the underlying RDBMS and therefore should be reimplemented in subclasses.
	 */
    public Statement getKeyFlowStatement() throws Exception {
        Statement s = getConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        return s;
    }

    private String getPatternList(int arity) {
        return "pattern";
    }

    private String getPatternListSelect(int arity) {
        return "pattern";
    }

    /**
	 * Get a resultSet of identifiers ordered by dependencies. Identifiers having
	 * same dependencies are randomly ordered.
	 * THIS METHOD is rather inefficient and should be reimplented in subclasses
	 */
    protected ResultSet getKeyFlow(String name, GlobalConstraints gcs) {
        ResultSet keyFlow = null;
        try {
            String secretKey = Constant.getParameter("secretKey");
            Statement stmt = getKeyFlowStatement();
            String query = "SELECT key1," + getPatternListSelect(gcs.size()) + " " + "FROM " + name + " " + "ORDER BY " + getPatternList(gcs.size()) + "," + "md5(key1 ||  '" + secretKey + "')";
            logger.debug(query);
            keyFlow = stmt.executeQuery(query);
        } catch (Exception e) {
            logger.fatal(e);
        }
        return keyFlow;
    }

    /**
	 *  Store in a relation pairs of identifiers having same dependencies
	 */
    protected void buildPairs(String pairsListName, String depMatrixName, GlobalConstraints gcs) {
        logger.info("Using generic and inefficient method to compute pairs");
        int arity = gcs.size();
        boolean cont = true;
        int alive = 1;
        boolean[] membership1 = new boolean[arity];
        boolean[] membership2 = new boolean[arity];
        try {
            Connection c = getNewConnection();
            ResultSet keyFlow = getKeyFlow(depMatrixName, gcs);
            Chrono.setMode(Chrono.QUERY_MODE);
            PreparedStatement pstmt = c.prepareStatement("INSERT INTO " + pairsListName + " VALUES(?,?)");
            do {
                logger.debug("[p-" + (alive++) + "]");
                if (keyFlow.next()) {
                    String key1 = keyFlow.getString(1);
                    membership1 = getMembership(keyFlow, arity);
                    if (keyFlow.next()) {
                        String key2 = keyFlow.getString(1);
                        membership2 = getMembership(keyFlow, arity);
                        if (equality(membership1, membership2, arity)) {
                            pstmt.setString(1, key1);
                            pstmt.setString(2, key2);
                            pstmt.execute();
                        }
                    } else cont = false;
                } else cont = false;
            } while (cont);
            pstmt.close();
            Chrono.setMode(Chrono.CPU_MODE);
        } catch (Exception e) {
            logger.fatal(e);
        }
    }

    public void computeDependencyMatrix(String matrixName, LocalConstraint lc, GlobalConstraints gcs) {
        logger.info("SqlDb:computeDependencyMatrix");
        createDependencyMatrix(matrixName, lc, gcs);
        logger.info("SqlDb:initializeDependencyMatrix");
        initializeDependencyMatrix(matrixName, lc, gcs);
        logger.info("SqlDb:getDependencyMatrix");
        getDependencyMatrix(matrixName, lc, gcs);
        logger.info("SqlDb:computeDependencyMatrix done.");
    }

    protected void createDependencyMatrix(String matrixName, LocalConstraint lc, GlobalConstraints gcs) {
        Statement stm = null;
        String query = "";
        int arity = lc.countInvolved(gcs);
        try {
            stm = getConnection().createStatement();
            query = "CREATE TABLE " + name + " (key1 integer PRIMARY KEY,";
            for (int i = 1; i <= arity; i++) {
                query += "m" + i + " BOOLEAN DEFAULT FALSE";
                if (i < arity) query += ",";
            }
            query += ")";
            logger.debug("createDependencyMatrix:" + query);
            Chrono.setMode(Chrono.QUERY_MODE);
            stm.execute(query);
            stm.close();
            Chrono.setMode(Chrono.CPU_MODE);
        } catch (Exception e) {
            logger.fatal(e);
        }
    }

    public void initializeDependencyMatrix(String matrixName, LocalConstraint lc, GlobalConstraints gcs) {
        String keyName = lc.keyName;
        String tableName = lc.tableName;
        String query = "INSERT INTO " + matrixName + "(key1) (" + " SELECT " + keyName + " FROM " + tableName + ")";
        try {
            Statement s = getConnection().createStatement();
            Chrono.setMode(Chrono.QUERY_MODE);
            s.execute(query);
            Chrono.setMode(Chrono.CPU_MODE);
        } catch (Exception e) {
            logger.fatal(e);
        }
    }

    /**
	 * Convert a single-column ResultSet of booleans
	 * into a boolean array
	 */
    private boolean[] getMembership(ResultSet r, int arity) {
        boolean[] res = new boolean[arity];
        try {
            for (int i = 0; i < arity; i++) {
                res[i] = r.getBoolean(i + 1);
            }
        } catch (SQLException e) {
            logger.fatal(e);
        }
        return res;
    }

    /**
	 * Compare for equality two finite portions
	 * of boolean arrays.
	 */
    private boolean equality(boolean[] a1, boolean[] a2, int arity) {
        for (int i = 0; i < arity; i++) {
            if (a1[i] != a2[i]) return false;
        }
        return true;
    }

    protected void getDependencyMatrix(String matrixName, LocalConstraint lc, GlobalConstraints gcs) {
        int i = 1;
        int arity = gcs.size();
        try {
            for (GlobalConstraint gc : gcs) {
                if (lc.isInvolved(gc)) {
                    String query = "UPDATE " + name + " SET m" + i + "=true WHERE key1 IN (" + gc.rKeyString() + ")";
                    logger.info("Adding constraint " + i + " into dependency matrix ");
                    executeQuery(query);
                    i++;
                }
            }
        } catch (Exception e2) {
            logger.fatal(e2);
        }
    }

    /**
	 * Store dependencies in a relation
	 */
    protected void getDependencyMatrix(String name, GlobalConstraints gcs) {
        int i = 1;
        int arity = gcs.size();
        logger.debug("Global constraints: " + gcs.size());
        try {
            for (Enumeration e = gcs.elements(); e.hasMoreElements(); i++) {
                GlobalConstraint gc = (GlobalConstraint) (e.nextElement());
                String query = "UPDATE " + name + " SET m" + i + "=true WHERE key1 IN (" + gc.rKeyString() + ")";
                logger.info("Adding constraint " + i + " into dependency matrix ");
                executeQuery(query);
            }
        } catch (Exception e2) {
            logger.fatal(e2);
        }
    }

    /**
	 * Given a ResultSet containing a table description, build
	 * a SQL CREATE TABLE command.
	 */
    public String getCreateTableCommand(ResultSet crs, ResultSet krs) throws SQLException {
        String tableName = "";
        String attributeName = "";
        String attributeType = "";
        String attributeSize = "";
        int javaType = 0;
        int nullable = 0;
        String query = "";
        while (crs.next()) {
            tableName = crs.getString(3);
            attributeName = crs.getString(4);
            attributeType = crs.getString(6);
            attributeSize = crs.getString(7);
            javaType = crs.getInt(5);
            nullable = crs.getInt(11);
            query += attributeName + " " + attributeType;
            if (javaType == Types.VARCHAR) query += "(" + attributeSize + ")"; else if (javaType == Types.INTEGER) {
            } else {
                logger.error("Unknown Type " + attributeType);
            }
            if (nullable == DatabaseMetaData.columnNoNulls) query += " NOT NULL";
            query += ",";
        }
        String primaryKey = "";
        while (krs.next()) {
            primaryKey += krs.getString("COLUMN_NAME") + ",";
        }
        if (primaryKey.length() > 0) {
            primaryKey = primaryKey.substring(0, primaryKey.length() - 1);
            primaryKey = "(" + primaryKey + ")";
            primaryKey = ",PRIMARY KEY " + primaryKey;
        } else {
            primaryKey = "";
        }
        query = "CREATE TABLE " + tableName + "(" + query.substring(0, query.length() - 1) + primaryKey + ")";
        return query;
    }

    private boolean toDuplicate(String table) {
        return !(table.endsWith("matrix") || table.endsWith("pairlist") || table.endsWith("pairs"));
    }

    private void duplicateTable(Connection scon, Connection dcon, String table) {
        logger.debug("Duplicating table " + table);
        Statement creTab, stmt;
        ResultSet tuples, columns, keys;
        int c;
        String insert = "";
        PreparedStatement insTup;
        try {
            columns = scon.getMetaData().getColumns(null, null, table, null);
            keys = scon.getMetaData().getPrimaryKeys(null, null, table);
            creTab = dcon.createStatement();
            creTab.execute(getCreateTableCommand(columns, keys));
            stmt = scon.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            stmt.setFetchSize(50);
            tuples = stmt.executeQuery("SELECT * FROM " + table);
            c = tuples.getMetaData().getColumnCount();
            insert = "INSERT INTO " + table + " VALUES(";
            for (int j = 1; j <= c; j++) insert += "?,";
            insert = insert.substring(0, insert.length() - 1) + ")";
            logger.debug("Insert pattern " + insert);
            insTup = dcon.prepareStatement(insert);
            while (tuples.next()) {
                for (int j = 1; j <= c; j++) insTup.setObject(j, tuples.getObject(j));
                insTup.executeUpdate();
            }
            dcon.commit();
        } catch (Exception e) {
            logger.error("Unable to copy table " + table + ": " + e);
            try {
                dcon.rollback();
            } catch (SQLException e1) {
                logger.fatal(e1);
            }
        }
    }

    private void duplicateIndices(Connection scon, Connection dcon, String table) {
        try {
            String idx_name, idx_att, query;
            ResultSet idxs = scon.getMetaData().getIndexInfo(null, null, table, false, false);
            Statement stmt = dcon.createStatement();
            while (idxs.next()) {
                idx_name = idxs.getString(6);
                idx_att = idxs.getString(9);
                idx_name += "_" + idx_att + "_idx";
                logger.debug("Creating index " + idx_name);
                query = "CREATE INDEX " + idx_name + " ON " + table + "(" + idx_att + ")";
                stmt.executeUpdate(query);
                dcon.commit();
            }
        } catch (Exception e) {
            logger.error("Unable to copy indices " + e);
            try {
                dcon.rollback();
            } catch (SQLException e1) {
                logger.fatal(e1);
            }
        }
    }

    public void duplicate(String destDB, SqlDb destDb) throws WatermillException {
        Connection scon, dcon;
        scon = null;
        dcon = null;
        try {
            String insert, table;
            int c;
            ResultSet tables, tuples, columns;
            PreparedStatement insTup;
            Statement creTab, test;
            Chrono.setMode(Chrono.QUERY_MODE);
            scon = getConnection();
            dcon = destDb.getConnection();
            String[] filter = { "TABLE" };
            tables = scon.getMetaData().getTables(null, null, "%", filter);
            dcon.setAutoCommit(false);
            while (tables.next()) {
                table = tables.getString(3);
                logger.debug("Processing table " + table);
                if (toDuplicate(table)) {
                    duplicateTable(scon, dcon, table);
                    duplicateIndices(scon, dcon, table);
                }
            }
            Chrono.setMode(Chrono.CPU_MODE);
            logger.debug("Closing connections to databases");
            scon.close();
            dcon.close();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                scon.close();
                dcon.close();
            } catch (Exception f) {
                logger.fatal(f);
            }
            throw new WatermillException(e.toString());
        }
    }

    /**
	 public void executeSQL(String query) throws Exception {
	 Msg.debug("QUERY:"+query);
	 Chrono.setMode(Chrono.queryMode);
	 getConnection().createStatement().execute(query);
	 Chrono.setMode(Chrono.cpuMode);
	 }*/
    public void prepareWatermark() {
    }

    public void endWatermark() {
    }

    public Collection<String> listTables() {
        Collection<String> result = new ArrayList<String>();
        ResultSet rs = null;
        try {
            DatabaseMetaData dbm = getConnection().getMetaData();
            String types[] = { "TABLE" };
            rs = dbm.getTables(null, null, "%", types);
            while (rs.next()) {
                String str = rs.getString("TABLE_NAME");
                result.add(str);
            }
        } catch (Exception e) {
            logger.error(e);
        }
        return result;
    }

    public Collection<String> listColumns(String table) {
        Collection<String> result = new ArrayList<String>();
        ResultSet rs = null;
        try {
            DatabaseMetaData dbm = getConnection().getMetaData();
            rs = dbm.getColumns(null, null, table, null);
            while (rs.next()) {
                String str = rs.getString("COLUMN_NAME");
                result.add(str);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public Collection<String> listKeys(String table) {
        Collection<String> result = new ArrayList<String>();
        ResultSet rs = null;
        try {
            DatabaseMetaData dbm = getConnection().getMetaData();
            rs = dbm.getPrimaryKeys(null, null, table);
            while (rs.next()) {
                String str = rs.getString("COLUMN_NAME");
                result.add(str);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    abstract List<String> listDatabases();
}
