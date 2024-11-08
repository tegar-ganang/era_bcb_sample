package org.apache.jackrabbit.core.persistence.db;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.persistence.PMContext;
import org.apache.jackrabbit.core.persistence.util.Serializer;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor;
import org.springframework.jdbc.support.nativejdbc.SimpleNativeJdbcExtractor;

@SuppressWarnings("unchecked")
public class DataSourcePersistenceManager extends SimpleDbPersistenceManager {

    private static Log LOG = LogFactory.getLog(DataSourcePersistenceManager.class);

    public static DataSource dataSource;

    public static String schema;

    public DataSourcePersistenceManager() {
        initialized = false;
    }

    public Connection getConnection() throws ClassNotFoundException, SQLException {
        super.setSchema(schema);
        Connection connection = dataSource.getConnection();
        LOG.info("Get a connection of " + schema + ", " + connection);
        NativeJdbcExtractor nativeExtractor = new SimpleNativeJdbcExtractor();
        return nativeExtractor.getNativeConnection(connection);
    }

    protected void closeConnection(Connection connection) throws Exception {
        LOG.info("Close a connection " + connection);
        super.closeConnection(connection);
    }

    private Class blobClass;

    private Integer DURATION_SESSION_CONSTANT;

    private Integer MODE_READWRITE_CONSTANT;

    public void init(PMContext context) throws Exception {
        super.init(context);
        if (!schema.equalsIgnoreCase("oracle")) return;
        if (!externalBLOBs) blobStore = new OracleBLOBStore();
        blobClass = con.getClass().getClassLoader().loadClass("oracle.sql.BLOB");
        DURATION_SESSION_CONSTANT = new Integer(blobClass.getField("DURATION_SESSION").getInt(null));
        MODE_READWRITE_CONSTANT = new Integer(blobClass.getField("MODE_READWRITE").getInt(null));
    }

    public synchronized void store(NodeState state) throws ItemStateException {
        if (!schema.equalsIgnoreCase("oracle")) {
            super.store(state);
            return;
        }
        if (!initialized) throw new IllegalStateException("not initialized");
        boolean update = state.getStatus() != ItemState.STATUS_NEW;
        String sql = (update) ? nodeStateUpdateSQL : nodeStateInsertSQL;
        Blob blob = null;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(INITIAL_BUFFER_SIZE);
            Serializer.serialize(state, out);
            blob = createTemporaryBlob(new ByteArrayInputStream(out.toByteArray()));
            executeStmt(sql, new Object[] { blob, state.getNodeId().toString() });
        } catch (Exception e) {
            String msg = "failed to write node state: " + state.getId();
            LOG.error(msg, e);
            throw new ItemStateException(msg, e);
        } finally {
            if (blob != null) {
                try {
                    freeTemporaryBlob(blob);
                } catch (Exception ignore) {
                    LOG.error(null, ignore);
                }
            }
        }
    }

    public synchronized void store(PropertyState state) throws ItemStateException {
        if (!schema.equalsIgnoreCase("oracle")) {
            super.store(state);
            return;
        }
        if (!initialized) throw new IllegalStateException("not initialized");
        boolean update = state.getStatus() != ItemState.STATUS_NEW;
        String sql = (update) ? propertyStateUpdateSQL : propertyStateInsertSQL;
        Blob blob = null;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(INITIAL_BUFFER_SIZE);
            Serializer.serialize(state, out, blobStore);
            blob = createTemporaryBlob(new ByteArrayInputStream(out.toByteArray()));
            executeStmt(sql, new Object[] { blob, state.getPropertyId().toString() });
        } catch (Exception e) {
            String msg = "failed to write property state: " + state.getId();
            LOG.error(msg, e);
            throw new ItemStateException(msg, e);
        } finally {
            if (blob != null) {
                try {
                    freeTemporaryBlob(blob);
                } catch (Exception ignore) {
                    LOG.error(null, ignore);
                }
            }
        }
    }

    public synchronized void store(NodeReferences refs) throws ItemStateException {
        if (!schema.equalsIgnoreCase("oracle")) {
            super.store(refs);
            return;
        }
        if (!initialized) throw new IllegalStateException("not initialized");
        boolean update = exists(refs.getId());
        String sql = (update) ? nodeReferenceUpdateSQL : nodeReferenceInsertSQL;
        Blob blob = null;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(INITIAL_BUFFER_SIZE);
            Serializer.serialize(refs, out);
            blob = createTemporaryBlob(new ByteArrayInputStream(out.toByteArray()));
            executeStmt(sql, new Object[] { blob, refs.getId().toString() });
        } catch (Exception e) {
            String msg = "failed to write node references: " + refs.getId();
            LOG.error(msg, e);
            throw new ItemStateException(msg, e);
        } finally {
            if (blob != null) {
                try {
                    freeTemporaryBlob(blob);
                } catch (Exception ignore) {
                    LOG.error(null, ignore);
                }
            }
        }
    }

    protected Blob createTemporaryBlob(InputStream in) throws Exception {
        Method createTemporary = blobClass.getMethod("createTemporary", new Class[] { Connection.class, Boolean.TYPE, Integer.TYPE });
        Object blob = createTemporary.invoke(null, new Object[] { con, Boolean.FALSE, DURATION_SESSION_CONSTANT });
        Method open = blobClass.getMethod("open", new Class[] { Integer.TYPE });
        open.invoke(blob, new Object[] { MODE_READWRITE_CONSTANT });
        Method getBinaryOutputStream = blobClass.getMethod("getBinaryOutputStream", new Class[0]);
        OutputStream out = (OutputStream) getBinaryOutputStream.invoke(blob, null);
        try {
            int read;
            byte[] buf = new byte[8192];
            while ((read = in.read(buf, 0, buf.length)) > -1) {
                out.write(buf, 0, read);
            }
        } finally {
            try {
                out.flush();
            } catch (IOException ioe) {
                LOG.error(null, ioe);
            }
            out.close();
        }
        Method close = blobClass.getMethod("close", new Class[0]);
        close.invoke(blob, null);
        return (Blob) blob;
    }

    protected void freeTemporaryBlob(Object blob) throws Exception {
        Method freeTemporary = blobClass.getMethod("freeTemporary", new Class[0]);
        freeTemporary.invoke(blob, null);
    }

    class OracleBLOBStore extends DbBLOBStore {

        public synchronized void put(String blobId, InputStream in, long size) throws Exception {
            Statement stmt = executeStmt(blobSelectExistSQL, new Object[] { blobId });
            ResultSet rs = stmt.getResultSet();
            boolean exists = rs.next();
            closeResultSet(rs);
            Blob blob = null;
            try {
                String sql = (exists) ? blobUpdateSQL : blobInsertSQL;
                blob = createTemporaryBlob(in);
                executeStmt(sql, new Object[] { blob, blobId });
            } finally {
                if (blob != null) {
                    try {
                        freeTemporaryBlob(blob);
                    } catch (Exception ignore) {
                    }
                }
            }
        }
    }
}
