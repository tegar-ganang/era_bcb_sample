package org.atlantal.utils.session;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Random;
import javax.sql.DataSource;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.transaction.file.FileResourceManager;
import org.apache.commons.transaction.file.ResourceManagerException;
import org.apache.commons.transaction.util.Log4jLogger;
import org.apache.commons.transaction.util.LoggerFacade;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * @author <a href="mailto:masurel@mably.com">Francois MASUREL</a>
 */
public class SessionInstance implements Session {

    private static final Logger LOGGER = Logger.getLogger(SessionInstance.class);

    static {
        LOGGER.setLevel(Level.DEBUG);
    }

    private static final Random R = new Random();

    private static final int TX_SHUTDOWN_MODE = FileResourceManager.SHUTDOWN_MODE_NORMAL;

    private static final long TX_TIMEOUT = 30000;

    private DataSource ds;

    private Connection conn;

    private int lock;

    private FileResourceManager fileResourceManager;

    private String txId;

    private ArrayList tempFiles;

    /**
     * Constructor
     *
     * @param conn conn
     */
    public SessionInstance(Connection conn) {
        this.conn = conn;
        this.lock = R.nextInt();
        this.fileResourceManager = null;
        this.txId = null;
        this.tempFiles = new ArrayList();
    }

    /**
     * Constructor
     *
     * @param ds ds
     * @param storeDir storeDir
     * @param tempDir tempDir
     * @throws SessionException SessionException
     */
    public SessionInstance(DataSource ds, String storeDir, String tempDir) throws SessionException {
        this.ds = ds;
        this.lock = R.nextInt();
        this.tempFiles = new ArrayList();
        if (storeDir != null) {
            LoggerFacade loggerFacade = new Log4jLogger(LOGGER);
            this.fileResourceManager = new FileResourceManager(storeDir, tempDir, false, loggerFacade, true);
        }
    }

    /**
     * @param tempFile tempFile
     */
    public void addTempFile(File tempFile) {
        this.tempFiles.add(tempFile);
    }

    /**
     * @param src src
     * @param filename filename
     * @throws IOException IOException
     */
    public void createFile(File src, String filename) throws IOException {
        try {
            FileInputStream fis = new FileInputStream(src);
            OutputStream fos = this.fileResourceManager.writeResource(this.txId, filename);
            IOUtils.copy(fis, fos);
            fos.close();
            fis.close();
        } catch (ResourceManagerException e) {
            LOGGER.error(e);
        }
    }

    /**
     * @param filename filename
     */
    public void deleteFile(String filename) {
        try {
            this.fileResourceManager.deleteResource(this.txId, filename, true);
        } catch (ResourceManagerException e) {
            LOGGER.error(e);
        }
    }

    /**
     *
     */
    public void commit() {
        this.deleteTempFiles();
        if (conn != null) {
            try {
                conn.commit();
            } catch (SQLException e) {
                LOGGER.error(e);
            }
        }
        if (this.fileResourceManager != null) {
            try {
                this.fileResourceManager.commitTransaction(this.txId);
            } catch (ResourceManagerException e) {
                LOGGER.error(e);
            }
        }
    }

    /**
     *
     */
    public void rollback() {
        this.deleteTempFiles();
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                LOGGER.error(e);
            }
        }
        if (this.fileResourceManager != null) {
            try {
                this.fileResourceManager.rollbackTransaction(this.txId);
            } catch (ResourceManagerException e) {
                LOGGER.error(e);
            }
        }
    }

    private void deleteTempFiles() {
        synchronized (this.tempFiles) {
            int tempFilesSize = this.tempFiles.size();
            for (int i = 0; i < tempFilesSize; i++) {
                try {
                    File tempFile = (File) this.tempFiles.get(i);
                    if (tempFile.exists()) {
                        tempFile.delete();
                    }
                } catch (SecurityException e) {
                    LOGGER.error(e);
                }
            }
        }
    }

    /**
     * @throws SessionException SessionException
     */
    public void start() throws SessionException {
        try {
            this.lock = R.nextInt();
            if (this.ds != null) {
                this.conn = ds.getConnection();
                this.conn.setAutoCommit(false);
            }
            if (this.fileResourceManager != null) {
                this.fileResourceManager.start();
                this.txId = fileResourceManager.generatedUniqueTxId();
                LOGGER.debug("Working Directory : " + fileResourceManager.getWorkDir());
                LOGGER.debug("Store Directory : " + fileResourceManager.getStoreDir());
                LOGGER.debug("Transaction Identifier : " + txId);
                this.fileResourceManager.startTransaction(txId);
            }
        } catch (SQLException e) {
            throw new SessionException(e);
        } catch (ResourceManagerException e) {
            throw new SessionException(e);
        }
    }

    /**
     *
     */
    public void stop() {
        if (this.ds != null) {
            DbUtils.closeQuietly(conn);
        }
        if (this.fileResourceManager != null) {
            try {
                this.fileResourceManager.stop(TX_SHUTDOWN_MODE, TX_TIMEOUT);
            } catch (ResourceManagerException e) {
                LOGGER.error(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public Connection getConnection() {
        return this.conn;
    }

    /**
     * @param connection
     *            connection
     */
    public void setConnection(Connection connection) {
        this.conn = connection;
    }

    /**
     * @return lock
     */
    public int getLock() {
        return lock;
    }

    /**
     * @param lock
     *            lock
     */
    public void setLock(int lock) {
        this.lock = lock;
    }
}
