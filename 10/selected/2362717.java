package net.ontopia.persistence.proxy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import net.ontopia.utils.OntopiaRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * INTERNAL: A key generator using the HIGH/LOW key generator
 * algorithm. It maintains the current counters in a counter
 * table. The key generator is able to preallocate a number of
 * identities which it hand out without having to go the database for
 * every new identity needed. It is used by the RDBMS proxy
 * implementation.
 */
public final class HighLowKeyGenerator implements KeyGeneratorIF {

    static Logger log = LoggerFactory.getLogger(HighLowKeyGenerator.class.getName());

    protected ConnectionFactoryIF connfactory;

    protected String table;

    protected String keycol;

    protected String valcol;

    protected int grabsize;

    protected String global_entry;

    protected String database;

    protected Map properties;

    protected long value;

    protected long max_value;

    public HighLowKeyGenerator(ConnectionFactoryIF connfactory, String table, String keycol, String valcol, String global_entry, int grabsize, String database, Map properties) {
        this.connfactory = connfactory;
        this.table = table;
        this.keycol = keycol;
        this.valcol = valcol;
        this.global_entry = global_entry;
        this.grabsize = grabsize;
        this.database = database;
        this.properties = properties;
        value = -1;
        max_value = -1;
    }

    public synchronized IdentityIF generateKey(Object type) {
        if (value >= max_value) return new LongIdentity(type, incrementInDatabase(type)); else return new LongIdentity(type, ++value);
    }

    /**
   * INTERNAL: Sends a request to the database to retrieve the current
   * counter value. The counter value row is then locked, the counter
   * incremented and the new value stored.
   */
    protected long incrementInDatabase(Object type) {
        long current_value;
        long new_value;
        String entry;
        if (global_entry != null) entry = global_entry; else throw new UnsupportedOperationException("Named key generators are not yet supported.");
        String lkw = (String) properties.get("net.ontopia.topicmaps.impl.rdbms.HighLowKeyGenerator.SelectSuffix");
        String sql_select;
        if (lkw == null && (database.equals("sqlserver"))) {
            sql_select = "select " + valcol + " from " + table + " with (XLOCK) where " + keycol + " = ?";
        } else {
            if (lkw == null) {
                if (database.equals("sapdb")) lkw = "with lock"; else lkw = "for update";
            }
            sql_select = "select " + valcol + " from " + table + " where " + keycol + " = ? " + lkw;
        }
        if (log.isDebugEnabled()) log.debug("KeyGenerator: retrieving: " + sql_select);
        Connection conn = null;
        try {
            conn = connfactory.requestConnection();
            PreparedStatement stm1 = conn.prepareStatement(sql_select);
            try {
                stm1.setString(1, entry);
                ResultSet rs = stm1.executeQuery();
                if (!rs.next()) throw new OntopiaRuntimeException("HIGH/LOW key generator table '" + table + "' not initialized (no rows).");
                current_value = rs.getLong(1);
                rs.close();
            } finally {
                stm1.close();
            }
            new_value = current_value + grabsize;
            String sql_update = "update " + table + " set " + valcol + " = ? where " + keycol + " = ?";
            if (log.isDebugEnabled()) log.debug("KeyGenerator: incrementing: " + sql_update);
            PreparedStatement stm2 = conn.prepareStatement(sql_update);
            try {
                stm2.setLong(1, new_value);
                stm2.setString(2, entry);
                stm2.executeUpdate();
            } finally {
                stm2.close();
            }
            conn.commit();
        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException e2) {
            }
            throw new OntopiaRuntimeException(e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    throw new OntopiaRuntimeException(e);
                }
            }
        }
        value = current_value + 1;
        max_value = new_value;
        return value;
    }
}
