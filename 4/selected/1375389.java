package ch.olsen.products.util.database.otsdb;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import ch.olsen.products.util.Application;
import ch.olsen.products.util.ConnectionConfig;
import ch.olsen.products.util.FastMode;
import ch.olsen.products.util.database.Pair;
import ch.olsen.products.util.database.Request;
import ch.olsen.products.util.database.RequestException;
import ch.olsen.products.util.database.RequestException.Reason;
import ch.olsen.products.util.database.otsdb.Otsdb.OtsdbLogin;
import ch.olsen.products.util.logging.Logger;

/**
 * pool of connections to the database
 * This is not a classical pool; we don't want each of the threads to have its
 * own db connection; that would make far too many connections.
 * We just make sure that if one connection is locked, then we provide another
 * one (either the first non locked connection or a new one if none).
 *
 * TODO : add a worker inactivity detection for locked processes to cleanout
 *        a possible bugger
 */
public class OtsdbPool {

    static final Logger log = Application.getLogger(OtsdbPool.class.getCanonicalName());

    private OtsdbLogin login = null;

    private OtsdbUnit unlockedWorker = null;

    private List<OtsdbUnit> lockedWorkers = new LinkedList<OtsdbUnit>();

    private List<OtsdbUnit> unusedWorkers = new LinkedList<OtsdbUnit>();

    private ConnectionConfig connectConfig = new ConnectionConfig();

    private int workerCount = 0;

    public OtsdbPool(OtsdbLogin login) {
        this.login = login;
    }

    public OtsdbLogin getLogin() {
        return login;
    }

    public void setLogin(OtsdbLogin login) {
        this.login = login;
    }

    public void setConnectionInfo(ConnectionConfig connectionConfig) {
        this.connectConfig = connectionConfig;
    }

    /**
	 * get the default unlocked connection
	 * @return database connection
	 */
    public synchronized OtsdbUnit getOtsdb() {
        if (unlockedWorker == null) {
            unlockedWorker = new OtsdbUnit();
            unlockedWorker.connection.maxRetry = connectConfig.maxRetry;
            unlockedWorker.connection.wait = connectConfig.wait;
        }
        return unlockedWorker;
    }

    /**
	 * get a dedicated worker for putting a lock
	 * @return a dedicated worker
	 */
    public OtsdbUnit getLockWorker() {
        OtsdbUnit worker = null;
        synchronized (unusedWorkers) {
            if (!unusedWorkers.isEmpty()) worker = unusedWorkers.remove(0);
        }
        if (worker == null) {
            worker = new OtsdbUnit();
            worker.connection.maxRetry = connectConfig.maxRetry;
            worker.connection.wait = connectConfig.wait;
        }
        synchronized (lockedWorkers) {
            lockedWorkers.add(worker);
        }
        return worker;
    }

    /**
	 * this is the wrapper of the Request class that also handles locking info
	 */
    public class OtsdbUnit extends Request implements OtsdbInterface {

        ConnectionConfig connection = new ConnectionConfig();

        private boolean locked = false;

        private int rank = 0;

        public class OtsdbConnectionException extends RequestException {

            private static final long serialVersionUID = 1L;

            OtsdbConnectionException(String message) {
                super(message);
            }
        }

        private RequestException checkConnection(RequestException e) throws RequestException {
            if (e.getReason().equals(Reason.NOCONNECTION)) connection.disconnected();
            return e;
        }

        public OtsdbUnit() {
            rank = ++workerCount;
            if (!FastMode.fastmode) log.debug("created a new worker #" + rank);
        }

        public synchronized void lock(String read[], String write[]) throws RequestException {
            if (locked) {
                throw new RequestException("failed to lock because tables already locked", Reason.LOCKED);
            }
            String sql = "LOCK TABLES ";
            String separator = ", ";
            if (read != null) for (String table : read) {
                sql += table + " READ" + separator;
            }
            if (write != null) for (String table : write) {
                sql += table + " WRITE" + separator;
            }
            sql = sql.substring(0, sql.length() - separator.length());
            try {
                executeUpdate(sql);
                locked = true;
            } catch (RequestException e) {
                log.error("didn't manage to lock: " + sql);
                throw e;
            }
        }

        /**
		 * @deprecated we used to use one ConnectionConfig per pool (a pool is
		 * per server/database/user) to speed up disconnection detection,
		 * but it is dangerous because we then declare closed a connection that
		 * might be still in use (like for reading a request's result)
		 * @param connectConfig
		 */
        public void setConnectionInfo(ConnectionConfig connectConfig) {
            connection = connectConfig;
        }

        public synchronized void unlock() throws RequestException {
            if (locked) {
                executeUpdate("UNLOCK TABLES");
                locked = false;
                lockedWorkers.remove(this);
                synchronized (unusedWorkers) {
                    unusedWorkers.add(this);
                }
            }
        }

        public final boolean isLocked() {
            return locked;
        }

        public Pair<Statement, ResultSet> executeQuery(String sql) throws RequestException {
            connect();
            try {
                return super.executeQuery(sql);
            } catch (RequestException e) {
                throw checkConnection(e);
            }
        }

        public long executeUpdate(String sql) throws RequestException {
            connect();
            try {
                return super.executeUpdate(sql);
            } catch (RequestException e) {
                throw checkConnection(e);
            }
        }

        public synchronized void connect() throws OtsdbConnectionException {
            while (!connection.isConnected() && connection.retry()) {
                try {
                    super.connect(login.server, login.database, login.user, login.pwd, login.properties);
                    connection.connected();
                    log.info("connected to " + login.database + "@" + login.server);
                    return;
                } catch (RequestExceptionNoDriver e) {
                    log.mail("fatal db connection problem - no driver! " + e);
                    System.exit(1);
                } catch (RequestExceptionConnection e) {
                    connection.disconnected();
                    log.warn("attempt " + connection.trials + "/" + connection.maxRetry + " to connect to the db, retrying, e: " + e);
                    connection.holdon();
                }
            }
            if (!connection.isConnected()) {
                connection.reset();
                throw new OtsdbConnectionException("fatal db connection problem: check if db server is running");
            }
        }

        @Override
        public synchronized void disconnect() throws RequestExceptionConnection {
            if (connection.isConnected() && (connection.isFailed() || locked)) {
                super.disconnect();
                locked = false;
            }
            connection.disconnected();
            connection.reset();
        }

        public final ConnectionConfig getConnectInfo() {
            return connection;
        }

        public final int getRank() {
            return rank;
        }

        public final int getPoolSize() {
            return workerCount;
        }
    }
}
