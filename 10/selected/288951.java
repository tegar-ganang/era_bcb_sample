package net.sf.traser.storage;

import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.traser.common.Security;
import net.sf.traser.databinding.base.GetPropertyValues;
import net.sf.traser.databinding.base.Item;
import net.sf.traser.databinding.base.PropertyValueResponse;
import net.sf.traser.databinding.base.PropertyValuesReport;
import net.sf.traser.databinding.management.Authorize;
import net.sf.traser.databinding.management.Authorize.Property.Partner;
import net.sf.traser.databinding.management.Authorize.Property.Partner.Value;
import net.sf.traser.databinding.management.Manage.Property;
import net.sf.traser.facade.DatabindingConverter;
import net.sf.traser.numbering.Identifier;
import net.sf.traser.numbering.Resolver;
import net.sf.traser.numbering.SchemeException;
import net.sf.traser.service.ExistenceFault;
import net.sf.traser.service.GeneralFault;
import org.apache.commons.lang.StringUtils;
import org.apache.ws.security.WSSecurityException;

/**
 *
 * @author Marcell Szathmári
 */
public class MetaDataStoragerImpl extends AbstractStorager implements MetaDataStorager {

    /** The code of the existence flag in the metadata table. */
    public static final int META_EXISTENCE = 0;

    /** The separator character used to store the list of declared properties for an item. */
    private static final String PROPERTY_SEPARATOR = "'";

    /** The separator character used to delimit values associated to properties. */
    private static final String VALUE_SEPARATOR = ":";

    /** The code of the property rows that tell what properties are supported for an item. */
    private static final int META_PROPERTY = 1;

    /** The code of the property rows that tell what is the last modification date of a property of an item. */
    private static final int META_MODIFIED = 2;

    /** The code of the duplication rows that tell what the duplication status of an item is. */
    private static final int META_DUPLICATION = 3;

    /** The code of the duplication rows that tell who augments the property. */
    private static final int META_AUGMENTER = 4;

    /** The code of the duplication rows that tell who augments the property. */
    private static final int META_DUPLICATOR = 5;

    /**
     * @param item
     * @param ts
     * @param ps
     * @throws net.sf.traser.service.GeneralFault
     */
    final void updateTimeStamps(Connection conn, String id, int host, long ts, Collection<Integer> ps) throws GeneralFault {
        if (ps.size() == 0) {
            return;
        }
        String propFilter = StringUtils.join(ps.iterator(), ", ");
        try {
            Set<Integer> props = checkTimestamps(conn, ps, propFilter, host, id, ts);
            if (!props.isEmpty()) {
                createTimestamps(conn, host, id, props);
            }
            updateTimestamps(conn, propFilter, ts, host, id);
        } catch (SQLException ex) {
            Logger.getLogger(MetaDataStoragerImpl.class.getName()).log(Level.SEVERE, null, ex);
            throw new GeneralFault("Could not determine if update is out-of-order.");
        }
    }

    private final void updateTimestamps(Connection conn, String propFilter, long ts, int host, String id) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE meta SET value = ? " + "WHERE meta.host = ? " + "AND meta.type = ? " + "AND meta.id = ? " + "AND meta.prop IN (" + propFilter + ")");
            stmt.setString(1, String.valueOf(ts));
            stmt.setInt(2, host);
            stmt.setInt(3, META_MODIFIED);
            stmt.setString(4, id);
            stmt.executeUpdate();
        } finally {
            StorageUtils.close(stmt);
        }
    }

    private final Set<Integer> checkTimestamps(Connection conn, Collection<Integer> ps, String propFilter, int host, String id, long ts) throws SQLException, NumberFormatException, GeneralFault {
        Set<Integer> props = new HashSet<Integer>(ps);
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("SELECT * FROM meta " + "WHERE meta.host = ? " + "AND meta.type = ? " + "AND meta.id = ? " + "AND meta.prop IN (" + propFilter + ")", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            stmt.setInt(1, host);
            stmt.setInt(2, META_MODIFIED);
            stmt.setString(3, id);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                if (Long.parseLong(rs.getString("value")) > ts) {
                    throw new GeneralFault("Out-of-order udpates are not supported!");
                }
                props.remove(rs.getInt("prop"));
            }
        } finally {
            StorageUtils.close(stmt);
        }
        return props;
    }

    public boolean isCached(GetPropertyValues gpv) {
        if (gpv.sizeProperty() == 0) {
            return true;
        }
        Date from = null, till = null;
        boolean onlyNeedsOneValidPeriod = (gpv.ifAt() && gpv.getTime() == null) || (gpv.ifDuring() && gpv.getDuring().getTill() == null) || (!gpv.ifAt() && !gpv.ifDuring());
        if (gpv.ifAt()) {
            from = till = gpv.getTime();
        } else if (gpv.ifDuring()) {
            from = gpv.getDuring().getFrom();
            till = gpv.getDuring().getTill();
        }
        Connection conn = null;
        Iterable<Integer> props = representer.getInternalReps(gpv.getProperties());
        try {
            conn = getConnection();
            PreparedStatement stmt = null;
            try {
                stmt = conn.prepareStatement("SELECT COUNT(*) FROM cachedperiods WHERE " + "id = ? AND host = ? AND " + "prop IN (" + StringUtils.join(props.iterator(), ",") + ") AND " + "starttime < ? AND endtime > ? AND hasvalues >= ?");
                stmt.setString(1, gpv.getItem().getResolved().getId());
                Integer hostIndex = representer.lookUpInternalRep(gpv.getItem().getResolved().getHost());
                stmt.setInt(2, hostIndex);
                stmt.setLong(3, onlyNeedsOneValidPeriod ? Long.MAX_VALUE : from.getTime());
                stmt.setLong(4, onlyNeedsOneValidPeriod ? 0 : till.getTime());
                stmt.setInt(5, onlyNeedsOneValidPeriod ? 1 : 0);
                ResultSet rs = stmt.executeQuery();
                return rs.next() && rs.getInt(1) > 0;
            } finally {
                StorageUtils.close(stmt);
            }
        } catch (SQLException ex) {
            Logger.getLogger(MetaDataStoragerImpl.class.getName()).log(Level.SEVERE, "Cannot determine cache status, assuming not cached.", ex);
            return false;
        } finally {
            StorageUtils.close(conn);
        }
    }

    /**
     * Marks the specified time period as cached for the given properties of the item.
     * @param item the item to mark the properties of.
     * @param from the start of the time frame.
     * @param to the end of the time frame.
     * @param properties the properties to save the status of with an associated flag that tells if the returned answer 
     * being cached contained values for that property.
     */
    public void markAsCached(Item item, Date from, Date to, Map<String, Boolean> properties) {
        if (isEmbedded()) {
            synchronized (this) {
                markAsCachedHelper(item, from, to, properties);
            }
        } else {
            markAsCachedHelper(item, from, to, properties);
        }
    }

    public void markAsCachedHelper(Item item, Date from, Date to, Map<String, Boolean> properties) {
        if (properties.size() == 0) {
            return;
        }
        Connection conn = null;
        Iterable<Integer> props = representer.getInternalReps(properties.keySet());
        Integer hostIndex = representer.lookUpInternalRep(item.getResolved().getHost());
        HashMap<Integer, long[]> periods = new HashMap<Integer, long[]>();
        for (Map.Entry<String, Boolean> e : properties.entrySet()) {
            periods.put(representer.lookUpInternalRep(e.getKey()), new long[] { from.getTime(), to.getTime(), e.getValue() ? 1 : 0 });
        }
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            conn.setSavepoint();
            PreparedStatement stmt = null;
            try {
                stmt = conn.prepareStatement("SELECT MIN(starttime), MAX(endtime), MAX(hasvalues) FROM cachedperiods WHERE " + "id = ? AND host = ? AND prop = ? AND " + "starttime <= ? AND endtime >= ?");
                stmt.setString(1, item.getResolved().getId());
                stmt.setInt(2, hostIndex);
                stmt.setLong(4, to.getTime());
                stmt.setLong(5, from.getTime());
                for (Map.Entry<Integer, long[]> e1 : periods.entrySet()) {
                    stmt.setInt(3, e1.getKey());
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        e1.getValue()[0] = Math.min(rs.getLong(1), e1.getValue()[0]);
                        e1.getValue()[1] = Math.max(rs.getLong(2), e1.getValue()[1]);
                        e1.getValue()[2] = Math.max(rs.getInt(3), e1.getValue()[2]);
                    }
                    StorageUtils.close(rs);
                }
                StorageUtils.close(stmt);
                stmt = conn.prepareStatement("DELETE FROM cachedperiods WHERE " + "id = ? AND host = ? AND " + "starttime <= ? AND endtime >= ? AND " + "prop IN (" + StringUtils.join(props.iterator(), ",") + ")");
                stmt.setString(1, item.getResolved().getId());
                stmt.setInt(2, hostIndex);
                stmt.setLong(3, to.getTime());
                stmt.setLong(4, from.getTime());
                stmt.executeUpdate();
                StorageUtils.close(stmt);
                stmt = conn.prepareStatement("INSERT INTO cachedperiods (id, host, prop, starttime, endtime, hasvalues) VALUES (?, ?, ?, ?, ?, ?)");
                stmt.setString(1, item.getResolved().getId());
                stmt.setInt(2, hostIndex);
                for (Map.Entry<Integer, long[]> e2 : periods.entrySet()) {
                    stmt.setInt(3, e2.getKey());
                    stmt.setLong(4, e2.getValue()[0]);
                    stmt.setLong(5, e2.getValue()[1]);
                    stmt.setInt(6, (int) e2.getValue()[2]);
                    stmt.executeUpdate();
                }
            } finally {
                StorageUtils.close(stmt);
            }
            conn.commit();
        } catch (SQLException ex) {
            Logger.getLogger(MetaDataStoragerImpl.class.getName()).log(Level.SEVERE, "Cannot update cachedperiods table.", ex);
            try {
                conn.rollback();
            } catch (SQLException ex1) {
                Logger.getLogger(MetaDataStoragerImpl.class.getName()).log(Level.SEVERE, "Could not roll back database, please consult system administrator.", ex1);
            }
        } finally {
            StorageUtils.close(conn);
        }
    }

    private void createTimestamps(Connection conn, int host, String id, Set<Integer> props) throws SQLException, GeneralFault {
        PreparedStatement stmt = null;
        try {
            String[] values = new String[props.size()];
            int i = 0;
            for (Integer p : props) {
                values[i] = "('', " + host + ", '" + id + "', " + META_MODIFIED + ", " + p + ")";
                i++;
            }
            stmt = conn.prepareStatement("INSERT INTO meta (value, host, id, type, prop) VALUES " + StringUtils.join(values, ','));
            if (stmt.executeUpdate() != values.length) {
                throw new GeneralFault("Could not timestamp property update.");
            }
        } finally {
            StorageUtils.close(stmt);
        }
    }

    /** The constants used to determine if a property is a directly referenced one or is indirectly named in an update. */
    public static interface Direction {

        /** Constant telling that the property is a normal property. */
        public static final boolean ORDINARY = false;

        /** Constant telling that the property is an inverse property. */
        public static final boolean INVERSE = true;
    }

    /** The resolver to extract identifiers from messages. */
    private Resolver resolver;

    /** The translator between internal and external representations. */
    private InternalRepresenter representer;

    @Override
    public void configure() {
        super.configure();
        resolver = manager.get(Resolver.class);
        representer = manager.get(InternalRepresenter.class);
    }

    public Existence itemExists(Identifier item) {
        String existence = getMetaValue(item, META_EXISTENCE);
        return existence == null ? Existence.NOTEXISTING : Existence.valueOf(existence);
    }

    public Existence itemExists(Item item) {
        String existence = getMetaValue(item, META_EXISTENCE);
        return existence == null ? Existence.NOTEXISTING : Existence.valueOf(existence);
    }

    public void createItem(Identifier item) {
        setMetaValue(item, META_EXISTENCE, Existence.LOCAL.name());
    }

    public void createItem(Item item) {
        setMetaValue(item, META_EXISTENCE, Existence.LOCAL.name());
    }

    /**
     * Gets the value associated with the item and type in the meta table.
     * @param item the item the value is associated with.
     * @param type the type under what the value is stored.
     * @return the value stored in the database.
     */
    String getMetaValue(Identifier item, int type) {
        return getMetaValue(item.getId(), item.getHost().toString(), null, type);
    }

    /**
     * Gets the value associated with the item and type in the meta table.
     * @param item the item the value is associated with.
     * @param type the type under what the value is stored.
     * @return the value stored in the database.
     */
    String getMetaValue(Connection conn, Identifier item, int type) throws GeneralFault {
        try {
            return getMetaValue(conn, item.getId(), item.getHost().toString(), null, type);
        } catch (SQLException ex) {
            throw new GeneralFault("Could not get meta-data.", ex);
        }
    }

    /**
     * Gets the value associated with the item and type in the meta table.
     * @param item the item the value is associated with.
     * @param type the type under what the value is stored.
     * @return the value stored in the database.
     */
    String getMetaValue(Identifier item, String prop, int type) {
        return getMetaValue(item.getId(), item.getHost().toString(), representer.lookUpInternalRep(prop), type);
    }

    /**
     * Gets the value associated with the item and type in the meta table.
     * @param item the item the value is associated with.
     * @param type the type under what the value is stored.
     * @return the value stored in the database.
     */
    String getMetaValue(Identifier item, Integer prop, int type) {
        return getMetaValue(item.getId(), item.getHost().toString(), prop, type);
    }

    /**
     * Gets the value associated with the item and type in the meta table.
     * @param item the item the value is associated with.
     * @param type the type under what the value is stored.
     * @return the value stored in the database.
     */
    String getMetaValue(Item item, int type) {
        return getMetaValue(item.getResolved().getId(), item.getResolved().getHost(), null, type);
    }

    /**
     * Gets the value associated with the item and type in the meta table.
     * @param item the item the value is associated with.
     * @param type the type under what the value is stored.
     * @return the value stored in the database.
     */
    String getMetaValue(Item item, String prop, int type) {
        return getMetaValue(item.getResolved().getId(), item.getResolved().getHost(), representer.lookUpInternalRep(prop), type);
    }

    /**
     * Gets the value associated with the item and type in the meta table.
     * @param item the item the value is associated with.
     * @param type the type under what the value is stored.
     * @return the value stored in the database.
     */
    String getMetaValue(Item item, Integer prop, int type) {
        return getMetaValue(item.getResolved().getId(), item.getResolved().getHost(), prop, type);
    }

    /**
     * Gets the value associated with the item and type in the meta table.
     * @param item the item the value is associated with.
     * @param type the type under what the value is stored.
     * @return the value stored in the database.
     */
    String getMetaValue(String id, String host, Integer prop, int type) {
        Connection conn = null;
        try {
            return getMetaValue(conn = getConnection(), id, host, prop, type);
        } catch (SQLException ex) {
            Logger.getLogger(MetaDataStoragerImpl.class.getName()).log(Level.SEVERE, "", ex);
            throw new RuntimeException("Database error, could not determine the existence of the item. Please try again or contact system administrator.");
        } finally {
            StorageUtils.close(conn);
        }
    }

    /**
     * Gets the value associated with the item and type in the meta table.
     * @param item the item the value is associated with.
     * @param type the type under what the value is stored.
     * @return the value stored in the database.
     */
    String getMetaValue(Connection conn, String id, String host, Integer prop, int type) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("SELECT meta.value FROM meta " + "WHERE meta.host = ? " + "AND meta.type = ? " + "AND meta.id = ? " + "AND meta.prop " + (prop == null ? "IS NULL" : "= ?"));
            Integer hostIndex = representer.lookUpInternalRep(host);
            stmt.setInt(1, hostIndex);
            stmt.setInt(2, type);
            stmt.setString(3, id);
            if (prop != null) {
                stmt.setInt(4, prop);
            }
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getString(1) : null;
        } finally {
            StorageUtils.close(stmt);
        }
    }

    /**
     * Gets the value associated with the item and type in the meta table.
     * @param item the item the value is associated with.
     * @param type the type under what the value is stored.
     * @return the value stored in the database.
     */
    String getMetaValue(Connection conn, String id, int host, Integer prop, int type) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("SELECT meta.value FROM meta " + "WHERE meta.host = ? " + "AND meta.type = ? " + "AND meta.id = ? " + "AND meta.prop " + (prop == null ? "IS NULL" : "= ?"));
            stmt.setInt(1, host);
            stmt.setInt(2, type);
            stmt.setString(3, id);
            if (prop != null) {
                stmt.setInt(4, prop);
            }
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getString(1) : null;
        } finally {
            StorageUtils.close(stmt);
        }
    }

    /**
     * Stores the value associated with the item and type into the meta table.
     * If there is no entry yet it inserts one, if there is, it updates it.
     * @param item the item the value is associated with.
     * @param type the type under what the value is stored.
     * @param value the type under what the value is stored.
     */
    void setMetaValue(Identifier item, int type, String value) {
        setMetaValue(item.getId(), item.getHost().toString(), null, type, value);
    }

    /**
     * Stores the value associated with the item and type into the meta table.
     * If there is no entry yet it inserts one, if there is, it updates it.
     * @param item the item the value is associated with.
     * @param type the type under what the value is stored.
     * @param value the type under what the value is stored.
     */
    void setMetaValue(Identifier item, String prop, int type, String value) {
        setMetaValue(item.getId(), item.getHost().toString(), representer.lookUpInternalRep(prop), type, value);
    }

    /**
     * Stores the value associated with the item and type into the meta table.
     * If there is no entry yet it inserts one, if there is, it updates it.
     * @param item the item the value is associated with.
     * @param type the type under what the value is stored.
     * @param value the type under what the value is stored.
     */
    void setMetaValue(Identifier item, Integer prop, int type, String value) {
        setMetaValue(item.getId(), item.getHost().toString(), prop, type, value);
    }

    /**
     * Stores the value associated with the item and type into the meta table.
     * If there is no entry yet it inserts one, if there is, it updates it.
     * @param item the item the value is associated with.
     * @param type the type under what the value is stored.
     * @param value the type under what the value is stored.
     */
    void setMetaValue(Item item, int type, String value) {
        setMetaValue(item.getResolved().getId(), item.getResolved().getHost(), null, type, value);
    }

    /**
     * Stores the value associated with the item and type into the meta table.
     * If there is no entry yet it inserts one, if there is, it updates it.
     * @param item the item the value is associated with.
     * @param type the type under what the value is stored.
     * @param value the type under what the value is stored.
     */
    void setMetaValue(Item item, String prop, int type, String value) {
        setMetaValue(item.getResolved().getId(), item.getResolved().getHost(), representer.lookUpInternalRep(prop), type, value);
    }

    /**
     * Stores the value associated with the item and type into the meta table.
     * If there is no entry yet it inserts one, if there is, it updates it.
     * @param item the item the value is associated with.
     * @param type the type under what the value is stored.
     * @param value the type under what the value is stored.
     */
    void setMetaValue(Item item, Integer prop, int type, String value) {
        setMetaValue(item.getResolved().getId(), item.getResolved().getHost(), prop, type, value);
    }

    /**
     * Stores the value associated with the item and type into the meta table.
     * If there is no entry yet it inserts one, if there is, it updates it.
     * @param item the item the value is associated with.
     * @param type the type under what the value is stored.
     * @param value the type under what the value is stored.
     */
    void setMetaValue(String id, String host, Integer prop, int type, String value) {
        Logger.getLogger(MetaDataStoragerImpl.class.getName()).fine("Updating meta-value of '" + id + "@" + host + "' item's '" + prop + "' property to '" + value + "'");
        Connection conn = null;
        try {
            setMetaValue(conn = getConnection(), id, host, prop, type, value);
        } catch (SQLException ex) {
            throw new RuntimeException("Database error, could not determine the existence of the item. Please try again or contact system administrator.", ex);
        } finally {
            StorageUtils.close(conn);
        }
    }

    /**
     * Stores the value associated with the item and type into the meta table.
     * If there is no entry yet it inserts one, if there is, it updates it.
     * @param item the item the value is associated with.
     * @param type the type under what the value is stored.
     * @param value the type under what the value is stored.
     */
    void setMetaValue(Connection conn, String id, String host, Integer prop, int type, String value) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("SELECT * FROM meta " + "WHERE meta.host = ? " + "AND meta.type = ? " + "AND meta.id = ? " + "AND meta.prop " + (prop == null ? "IS NULL" : "= ?"), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            Integer hostIndex = representer.lookUpInternalRep(host);
            stmt.setInt(1, hostIndex);
            stmt.setInt(2, type);
            stmt.setString(3, id);
            if (prop != null) {
                stmt.setInt(4, prop);
            }
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                if (value != null) {
                    rs.updateString("value", value);
                    rs.updateRow();
                } else {
                    rs.deleteRow();
                }
            } else {
                rs.moveToInsertRow();
                rs.updateInt("host", hostIndex);
                rs.updateInt("type", type);
                rs.updateString("id", id);
                rs.updateString("value", value);
                if (prop != null) {
                    rs.updateInt("prop", prop);
                }
                rs.insertRow();
            }
        } finally {
            StorageUtils.close(stmt);
        }
    }

    /** Returns true if the item has a property like the named one.
     * @param item
     * @param property
     * @return
     */
    public boolean hasProperty(Identifier item, String property) {
        String value = getMetaValue(item, META_PROPERTY);
        return value != null && value.contains(PROPERTY_SEPARATOR + property + PROPERTY_SEPARATOR);
    }

    final String[] getProperties(Identifier item) {
        return getMetaValue(item, META_PROPERTY).split(PROPERTY_SEPARATOR);
    }

    final String[] getProperties(Connection conn, String id, int host) throws GeneralFault {
        try {
            String meta = getMetaValue(conn, id, host, null, META_PROPERTY);
            return meta == null ? new String[0] : meta.split(PROPERTY_SEPARATOR);
        } catch (SQLException ex) {
            throw new GeneralFault("Could not get meta-data from database.", ex);
        }
    }

    public void createProperty(Identifier item, String property, String partner) throws GeneralFault {
        createProperties(item, partner, property);
    }

    private final void createPropertyAuth(Authorize auth, String property, String partner) {
        Authorize.Property p = new Authorize.Property();
        auth.addProperty(p);
        p.setName(property);
        Partner pa;
        p.addPartner(pa = new Partner());
        pa.setString(partner);
        pa.setAction(Partner.Action.GRANT);
        pa.setValue(Partner.Value.READ);
        p.addPartner(pa = new Partner());
        pa.setString(partner);
        pa.setAction(Partner.Action.GRANT);
        pa.setValue(Partner.Value.WRITE);
        p.addPartner(pa = new Partner());
        pa.setString(partner);
        pa.setAction(Partner.Action.GRANT);
        pa.setValue(Partner.Value.AUTH);
    }

    public void createProperties(Identifier item, String partner, String... properties) throws GeneralFault {
        String value = getMetaValue(item, META_PROPERTY);
        value = value == null ? "" : value;
        Authorize auth = partner == null ? null : new Authorize();
        Set<String> ps = new HashSet<String>(Arrays.asList(value.split(PROPERTY_SEPARATOR)));
        boolean modified = false;
        for (String p : properties) {
            if (!ps.contains(p)) {
                ps.add(p);
                if (auth != null) {
                    createPropertyAuth(auth, p, partner);
                }
                modified = true;
            }
        }
        if (modified) {
            ps.remove("");
            setMetaValue(item, META_PROPERTY, PROPERTY_SEPARATOR + StringUtils.join(ps.iterator(), PROPERTY_SEPARATOR) + PROPERTY_SEPARATOR);
            if (auth != null) {
                Item i = new Item();
                i.setItemId(item.toString());
                auth.setItem(i);
                authorize(auth);
            }
        }
    }

    public void createProperty(Item item, String property, String partner) throws GeneralFault {
        try {
            createProperty(resolver.resolve(item.getItemId()), property, partner);
        } catch (SchemeException ex) {
            throw new GeneralFault("Could not parse item identifier.", ex);
        }
    }

    public Collection<String> getPropertyKeyList(Identifier item) {
        LinkedList<String> result = new LinkedList<String>();
        String properties = getMetaValue(item, META_PROPERTY);
        if (properties != null) {
            for (String prop : properties.split(PROPERTY_SEPARATOR)) {
                if (!"".equals(prop)) {
                    result.add(prop);
                }
            }
        }
        return result;
    }

    private boolean canAuth(int partner, String id, int host, int property) throws GeneralFault {
        Connection conn = null;
        try {
            return canAuth(conn = getConnection(), partner, id, host, property);
        } catch (SQLException ex) {
            throw new GeneralFault("Could not check authorization of sending partner.", ex);
        } finally {
            StorageUtils.close(conn);
        }
    }

    private boolean canAuth(Connection conn, int partner, String id, int host, int property) throws GeneralFault, SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("SELECT value FROM auth " + "WHERE id = ? AND host = ? AND prop = ? AND dir = ? AND partner = ?");
            stmt.setString(1, id);
            stmt.setInt(2, host);
            stmt.setInt(3, property);
            stmt.setBoolean(4, Direction.ORDINARY);
            stmt.setInt(5, partner);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && PropertyRight.contains(rs.getInt("value"), PropertyRight.convert(Value.AUTH), true);
        } catch (SQLException ex) {
            throw new GeneralFault("Could not check authorization of sending partner.", ex);
        } finally {
            StorageUtils.close(stmt);
        }
    }

    /**
     * Implements the authorize method of the Storager interface.
     *
     * Note that it currently cannot handle revocation.
     * Note also that rights are not hierarchial.
     *
     * @param auth the authorization message received.
     * @throws GeneralFault
     */
    @Override
    public void authorize(Authorize auth) throws GeneralFault {
        Identifier item = manager.get(DatabindingConverter.class).getIdentifier(auth.getItem());
        if (item == null) {
            Logger.getLogger(MetaDataStoragerImpl.class.getName()).warning("Could not edit authorization of null item.");
            return;
        }
        String senderPartner = auth.getSender();
        Integer sender = senderPartner == null ? null : representer.lookUpPartner(senderPartner);
        int hostIndex = representer.lookUpInternalRep(item.getHost().toString());
        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            PreparedStatement selectRights = null;
            try {
                selectRights = conn.prepareStatement("SELECT * FROM auth " + "WHERE id = ? AND host = ? AND prop = ? AND dir = ?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                selectRights.setString(1, item.getId());
                selectRights.setInt(2, hostIndex);
                selectRights.setBoolean(4, Direction.ORDINARY);
                for (Authorize.Property prop : auth.getProperties()) {
                    int propIndex = representer.lookUpInternalRep(prop.getName());
                    if (sender != null && !canAuth(sender, item.getId(), hostIndex, propIndex)) {
                        continue;
                    }
                    selectRights.setInt(3, propIndex);
                    ResultSet rs = selectRights.executeQuery();
                    HashMap<Integer, List<Partner>> rules = new HashMap<Integer, List<Partner>>();
                    for (Partner part : prop.getPartners()) {
                        int partner = representer.lookUpPartner(part.getString());
                        List<Partner> list = rules.get(partner);
                        if (list == null) {
                            list = new LinkedList<Partner>();
                            rules.put(partner, list);
                        }
                        list.add(part);
                    }
                    while (rs.next()) {
                        int partner = rs.getInt("partner");
                        List<Partner> list = rules.get(partner);
                        if (list != null) {
                            rs.updateInt("value", getUpdatedRights(rs.getInt("value"), list));
                            rs.updateRow();
                            rules.remove(partner);
                        }
                    }
                    for (Map.Entry<Integer, List<Partner>> entry : rules.entrySet()) {
                        rs.moveToInsertRow();
                        rs.updateString("id", item.getId());
                        rs.updateInt("host", hostIndex);
                        rs.updateInt("prop", propIndex);
                        rs.updateBoolean("dir", Direction.ORDINARY);
                        rs.updateInt("partner", entry.getKey());
                        rs.updateInt("value", getUpdatedRights(PropertyRight.NONE, entry.getValue()));
                        rs.insertRow();
                    }
                    conn.commit();
                }
            } finally {
                StorageUtils.close(selectRights);
            }
        } catch (SQLException ex) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
            }
            throw new RuntimeException("Database error.", ex);
        } finally {
            StorageUtils.close(conn);
        }
    }

    /**
     * Calculates the new authorization rights of a partner to an item based on
     * a list of GRANT and REVOKE statements.
     * @param current the current authorization right.
     * @param list the list of GRANT and REVOKE statements.
     * @return the calculated new authorization right.
     */
    private int getUpdatedRights(int current, List<Partner> list) throws GeneralFault {
        for (Partner part : list) {
            int right = PropertyRight.convert(part.getValue());
            boolean action = part.getAction() == Partner.Action.GRANT;
            if (!PropertyRight.contains(current, right, action)) {
                current = action ? right | current : ~right & current;
            }
        }
        return current;
    }

    public Authorize getAuthorization(Identifier item, String... properties) throws GeneralFault {
        String su = null;
        try {
            Security sec = manager.get(Security.class);
            if (sec != null) {
                X509Certificate[] suCerts = sec.getCryptoProvider().getCertificates(sec.getSenderUser());
                su = suCerts[0].getSubjectDN().getName();
            }
        } catch (WSSecurityException ex) {
            Logger.getLogger(MetaDataStoragerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return getAuthorization(su, item, properties);
    }

    private Authorize getAuthorization(String superuser, Identifier item, String... properties) throws GeneralFault {
        Authorize result = new Authorize();
        Item i = new Item();
        i.setItemId(item.toString());
        result.setItem(i);
        if (properties.length == 0) {
            properties = this.getPropertyKeyList(item).toArray(properties);
        }
        Partner.Value[] values = new Value[] { Partner.Value.READ, Partner.Value.WRITE, Partner.Value.AUTH };
        int suId = superuser == null ? 0 : representer.lookUpPartner(superuser);
        Connection conn = null;
        try {
            conn = getConnection();
            PreparedStatement stmt = null;
            try {
                stmt = conn.prepareStatement("SELECT * FROM auth " + "WHERE id = ? AND prop = ? AND dir = ?");
                stmt.setString(1, item.getId());
                stmt.setBoolean(3, Direction.ORDINARY);
                for (String property : properties) {
                    Authorize.Property p = new Authorize.Property();
                    p.setName(property);
                    stmt.setInt(2, representer.lookUpInternalRep(property));
                    ResultSet rs = stmt.executeQuery();
                    boolean hadSu = false;
                    while (rs.next()) {
                        int pId = rs.getInt("partner");
                        if (pId != suId) {
                            String pDN = representer.getExternalRep(pId);
                            int val = rs.getInt("value");
                            for (Value value : values) {
                                if (PropertyRight.contains(val, PropertyRight.convert(value), true)) {
                                    Partner partner = new Partner();
                                    partner.setAction(Partner.Action.GRANT);
                                    partner.setString(pDN);
                                    partner.setValue(value);
                                    p.addPartner(partner);
                                }
                            }
                        }
                    }
                    if (suId > 0) {
                        for (Value value : values) {
                            Partner partner = new Partner();
                            partner.setAction(Partner.Action.GRANT);
                            partner.setString(superuser);
                            partner.setValue(value);
                            partner.setOwner(true);
                            p.addPartner(partner);
                        }
                    }
                    result.addProperty(p);
                    StorageUtils.close(rs);
                }
                return result;
            } catch (SQLException ex) {
                throw new GeneralFault("Could not retrieve authorization settings.", ex);
            } finally {
                StorageUtils.close(stmt);
            }
        } catch (SQLException ex) {
            throw new GeneralFault("Could not retrieve authorization settings.", ex);
        } finally {
            StorageUtils.close(conn);
        }
    }

    public boolean getAuthorization(Identifier item, String partner, Value value, String... properties) throws GeneralFault {
        if (partner == null) {
            return true;
        }
        int needed = PropertyRight.convert(value);
        GetAuthorizationHelper helper = new GetAuthorizationHelper(item, partner);
        try {
            for (String property : properties) {
                int auth = helper.getAuthorization(property);
                if (!PropertyRight.contains(auth, needed, true)) {
                    return false;
                }
            }
            return true;
        } finally {
            helper.finish();
        }
    }

    private class GetAuthorizationHelper {

        private Connection conn;

        private PreparedStatement selectRights;

        private boolean superUser = false;

        private GetAuthorizationHelper(Identifier item, String partner) throws GeneralFault {
            if (partner == null) {
                superUser = true;
                return;
            }
            try {
                conn = getConnection();
                int hostIndex = representer.lookUpInternalRep(item.getHost().toString());
                try {
                    selectRights = conn.prepareStatement("SELECT value FROM auth " + "WHERE id = ? AND host = ? AND dir = ? AND partner = ? " + "AND prop = ?");
                    selectRights.setString(1, item.getId());
                    selectRights.setInt(2, hostIndex);
                    selectRights.setBoolean(3, Direction.ORDINARY);
                    selectRights.setInt(4, representer.lookUpPartner(partner));
                } catch (SQLException ex) {
                    StorageUtils.close(conn);
                    throw new GeneralFault("Database error.", ex);
                }
            } catch (SQLException ex) {
                throw new GeneralFault("Database error.", ex);
            }
        }

        private int getAuthorization(String property) {
            if (superUser) {
                return SUPERUSER;
            }
            int prop = representer.lookUpInternalRep(property);
            try {
                selectRights.setInt(5, prop);
                ResultSet rs = selectRights.executeQuery();
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    return PropertyRight.NONE;
                }
            } catch (SQLException ex) {
                throw new RuntimeException("Database error.", ex);
            }
        }

        private void finish() {
            StorageUtils.close(selectRights);
            StorageUtils.close(conn);
        }
    }

    /** Rights of the local user. */
    private static final int SUPERUSER = PropertyRight.READ + PropertyRight.WRITE + PropertyRight.AUTH;

    public PropertyValuesReport getItemDataSkeleton(Identifier item, String partner, String... properties) throws GeneralFault, ExistenceFault {
        Existence existence = itemExists(item);
        if (existence == Existence.NOTEXISTING) {
            throw new ExistenceFault("The item '" + item.toString() + "' does not exist.");
        }
        if (properties.length == 0) {
            properties = getPropertyKeyList(item).toArray(properties);
        }
        PropertyValuesReport result = new PropertyValuesReport();
        result.setItem(manager.get(DatabindingConverter.class).getItem(item));
        GetAuthorizationHelper helper = new GetAuthorizationHelper(item, partner);
        try {
            for (String property : properties) {
                PropertyValueResponse pvr = new PropertyValueResponse();
                pvr.setPropertyName(property);
                resolver.resolve(result.getItem());
                if (existence == Existence.LOCAL && getPropertyDuplicationStatus(result.getItem(), property) == Property.Level.AUGMENT) {
                    pvr.setHostedBy(getMetaValue(result.getItem(), property, META_AUGMENTER).split(VALUE_SEPARATOR, 2)[1]);
                } else {
                    int value = helper.getAuthorization(property);
                    pvr.setCanRead(PropertyRight.contains(value, PropertyRight.READ, true));
                    pvr.setCanWrite(PropertyRight.contains(value, PropertyRight.WRITE, true));
                    pvr.setCanAuth(PropertyRight.contains(value, PropertyRight.AUTH, true));
                }
                result.addProperty(pvr);
            }
            return result;
        } finally {
            helper.finish();
        }
    }

    public Property.Level getPropertyDuplicationStatus(Item item, String property) {
        String rule = getMetaValue(item, property, META_DUPLICATION);
        return rule == null ? Property.Level.NONE : Property.Level.valueOf(rule);
    }

    public void setPropertyDuplicationStatus(Item item, String property, Property.Level level) {
        switch(level) {
            case NONE:
                setMetaValue(item, property, META_DUPLICATION, null);
                break;
            default:
                setMetaValue(item, property, META_DUPLICATION, level.name());
                switch(level) {
                    case DUPLICATE:
                        break;
                    default:
                        if (itemExists(item) == Existence.NOTEXISTING) {
                            setMetaValue(item, META_EXISTENCE, Existence.AUGMENTED.name());
                        }
                        try {
                            createProperty(item, property, null);
                        } catch (GeneralFault ex) {
                            Logger.getLogger(MetaDataStoragerImpl.class.getName()).log(Level.SEVERE, null, ex);
                        }
                }
        }
    }

    /** Map to translate the levels to meta types. */
    EnumMap<Property.Level, Integer> propertyDuplicationHelper = new EnumMap<Property.Level, Integer>(Property.Level.class);

    {
        propertyDuplicationHelper.put(Property.Level.AUGMENT, META_AUGMENTER);
        propertyDuplicationHelper.put(Property.Level.DUPLICATE, META_DUPLICATOR);
    }

    public Set<String> getPropertyDuplicationPeers(Item item, String property, Property.Level level) {
        String meta;
        Integer type = propertyDuplicationHelper.get(level);
        return getPropertyDuplicationPeerHelper(item, property, type).keySet();
    }

    private static enum PeerDir {

        ADD, REMOVE
    }

    private Map<String, String> getPropertyDuplicationPeerHelper(Item item, String property, Integer type) {
        Map<String, String> peers = new LinkedHashMap<String, String>();
        String meta = getMetaValue(item, property, type);
        if (meta != null) {
            for (String p : meta.split(PROPERTY_SEPARATOR)) {
                String ps[] = p.split(VALUE_SEPARATOR, 2);
                peers.put(ps[1], p);
            }
        }
        return peers;
    }

    private void setPropertyDuplicationPeerHelper(Item item, String property, String peer, Property.Level level, PeerDir direction) {
        Integer type = propertyDuplicationHelper.get(level);
        Map<String, String> peers = getPropertyDuplicationPeerHelper(item, property, type);
        if (peers.containsKey(peer) ^ (direction == PeerDir.ADD)) {
            if (direction == PeerDir.ADD) {
                peers.put(peer, "0" + VALUE_SEPARATOR + peer);
            } else {
                peers.remove(peer);
            }
            setMetaValue(item, property, type, StringUtils.join(peers.values().iterator(), PROPERTY_SEPARATOR));
        }
    }

    public void setPropertyDuplicationPeer(Item item, String property, String peer, Property.Level level) {
        switch(level) {
            case NONE:
                setPropertyDuplicationPeerHelper(item, property, peer, Property.Level.AUGMENT, PeerDir.REMOVE);
                setPropertyDuplicationPeerHelper(item, property, peer, Property.Level.DUPLICATE, PeerDir.REMOVE);
                break;
            case DUPLICATE:
                setPropertyDuplicationPeerHelper(item, property, peer, Property.Level.AUGMENT, PeerDir.REMOVE);
                setPropertyDuplicationPeerHelper(item, property, peer, Property.Level.DUPLICATE, PeerDir.ADD);
                break;
            case AUGMENT:
                setPropertyDuplicationPeerHelper(item, property, peer, Property.Level.DUPLICATE, PeerDir.REMOVE);
                setPropertyDuplicationPeerHelper(item, property, peer, Property.Level.AUGMENT, PeerDir.ADD);
                break;
            case OVERRIDE:
            default:
                throw new UnsupportedOperationException("Peers cannot be specified for this kind of level.");
        }
    }

    public Date getPropertyDuplicationLastUpdateOfPeer(Item item, String property, String peer) {
        Map<String, String> peers = getPropertyDuplicationPeerHelper(item, property, META_DUPLICATOR);
        if (peers.containsKey(peer)) {
            return new Date(Long.valueOf(peers.get(peer).split(VALUE_SEPARATOR, 2)[0]));
        } else {
            return null;
        }
    }

    public void setPropertyDuplicationLastUpdateOfPeer(Item item, String property, String peer, Date date) {
        Integer type = META_DUPLICATOR;
        Map<String, String> peers = getPropertyDuplicationPeerHelper(item, property, type);
        if (peers.containsKey(peer)) {
            peers.put(peer, date.getTime() + ":" + peer);
            setMetaValue(item, property, type, StringUtils.join(peers.values().iterator(), PROPERTY_SEPARATOR));
        }
    }
}
