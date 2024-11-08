package net.sf.traser.storage;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import net.sf.traser.databinding.base.CreateEvent;
import net.sf.traser.databinding.base.Item;
import net.sf.traser.databinding.base.PropertyValueResponse;
import net.sf.traser.databinding.base.PropertyValueResponse.Value;
import net.sf.traser.databinding.base.PropertyValueUpdate;
import net.sf.traser.databinding.base.PropertyValuesReport;
import net.sf.traser.databinding.management.Authorize.Property.Partner;
import net.sf.traser.numbering.BasicIdUriResolver;
import net.sf.traser.numbering.Identifier;
import net.sf.traser.numbering.Resolver;
import net.sf.traser.numbering.SchemeException;
import net.sf.traser.service.AuthorizationFault;
import net.sf.traser.service.ExistenceFault;
import net.sf.traser.service.GeneralFault;
import net.sf.traser.storage.InternalRepresenter.ValueId;
import net.sf.traser.storage.MetaDataStoragerImpl.Direction;
import net.sf.traser.utils.XmlUtils;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.util.XMLUtils;
import org.apache.commons.lang.StringUtils;
import static net.sf.traser.storage.StorageUtils.*;

/**
 * This class utilizes the apache DdlUtil package to create the necessary tables
 * in the database using indexes. It then uses the JDBC API to read / write 
 * information to / from the database. Supported databases include:
 * <ul>
 *   <li>HSQL DB</li>
 *   <li>MySQL</li>
 *   <li>Derby</li>
 * </ul>
 * 
 * @author Marcell Szathmári
 */
public class ItemDataStoragerImpl extends AbstractStorager implements ItemDataStorager {

    /** The logger to log messages. */
    private static final Logger LOG = Logger.getLogger(ItemDataStoragerImpl.class.getName());

    /** The status of the property values. Either still valid or already removed. */
    public static interface Status {

        /** Constant indicating that the property value is still valid. */
        public static final boolean VALID = false;

        /** Constant indicating that the property value was alread removed. */
        public static final boolean CLOSED = true;
    }

    ;

    /** The resolver to obtain the host of the identifier. */
    private Resolver resolver;

    /** The translator between internal and external representations. */
    private InternalRepresenter representer;

    /** The interface providing access to the item related metadata. */
    private MetaDataStoragerImpl metadata;

    @Override
    public void configure() {
        super.configure();
        resolver = manager.get(Resolver.class);
        representer = manager.get(InternalRepresenter.class);
        metadata = manager.get(MetaDataStoragerImpl.class);
    }

    public void cachePropertyValues(Item item, Date from, Date to, PropertyValueResponse... properties) throws GeneralFault {
        if (isEmbedded()) {
            synchronized (this) {
                cachePropertyValuesHelper(item, from, to, properties);
            }
        } else {
            cachePropertyValuesHelper(item, from, to, properties);
        }
    }

    public void cachePropertyValuesHelper(Item item, Date from, Date to, PropertyValueResponse... properties) throws GeneralFault {
        Connection conn = null;
        try {
            HashMap<String, Boolean> props = new HashMap<String, Boolean>();
            conn = getConnection();
            PreparedStatement stmt = null;
            try {
                stmt = conn.prepareStatement("SELECT * FROM iprops " + "WHERE id = ? AND host = ? AND prop = ? AND dir = ? " + "AND value = ?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                Integer hostIndex = representer.lookUpInternalRep(item.getResolved().getHost());
                String id = item.getResolved().getId();
                stmt.setString(1, id);
                stmt.setInt(2, hostIndex);
                stmt.setBoolean(4, Direction.ORDINARY);
                for (PropertyValueResponse pvr : properties) {
                    props.put(pvr.getPropertyName(), pvr.sizeValue() > 0);
                    int propIndex = representer.lookUpInternalRep(pvr.getPropertyName());
                    for (Value pv : pvr.getValues()) {
                        OMElement value = XMLUtils.toOM(pv.getAny());
                        long valueid = representer.getValueId(value, true);
                        stmt.setInt(3, propIndex);
                        stmt.setLong(5, valueid);
                        ResultSet rs = stmt.executeQuery();
                        if (!rs.next()) {
                            rs.moveToInsertRow();
                            rs.updateString("id", id);
                            rs.updateInt("host", hostIndex);
                            rs.updateInt("spid", representer.lookUpPartner(pv.getFromPartner()));
                            rs.updateLong("sts", pv.getFrom().getTime());
                            rs.updateInt("prop", propIndex);
                            rs.updateBoolean("dir", Direction.ORDINARY);
                            if (pv.getTo() != null) {
                                rs.updateInt("epid", representer.lookUpPartner(pv.getToPartner()));
                                rs.updateLong("ets", pv.getTo().getTime());
                                rs.updateBoolean("closed", Status.CLOSED);
                            } else {
                                rs.updateLong("ets", to.getTime());
                                rs.updateBoolean("closed", Status.VALID);
                            }
                            rs.updateLong("value", valueid);
                            rs.insertRow();
                        } else if (rs.getBoolean("closed") == Status.VALID) {
                            if (pv.getTo() != null) {
                                rs.updateInt("epid", representer.lookUpPartner(pv.getToPartner()));
                                rs.updateLong("ets", pv.getTo().getTime());
                                rs.updateBoolean("closed", Status.CLOSED);
                                rs.updateRow();
                            } else if (rs.getLong("ets") < to.getTime()) {
                                rs.updateLong("ets", to.getTime());
                                rs.updateRow();
                            }
                        }
                    }
                }
                metadata.markAsCached(item, from, to, props);
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "Cannot transform XML representation.", ex);
            } finally {
                StorageUtils.close(stmt);
            }
        } catch (SQLException ex) {
            Logger.getLogger(ItemDataStoragerImpl.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            StorageUtils.close(conn);
        }
    }

    public PropertyValuesReport getCachedValues(Item item, Date from, Date to, String partner, String... properties) throws AuthorizationFault, ExistenceFault, GeneralFault {
        PropertyValuesReport result = new PropertyValuesReport();
        result.setItem(item);
        result.setFrom(from);
        result.setTo(to);
        for (String property : properties) {
            PropertyValueResponse pvr = new PropertyValueResponse();
            pvr.setIsCached(true);
            pvr.setPropertyName(property);
            result.addProperty(pvr);
        }
        return getPropertyValues(result);
    }

    private String constructInsertValuesPart(int cnt, final String valuesTemplate) {
        StringBuilder sb = new StringBuilder(cnt * (valuesTemplate.length() + 1));
        sb.append(valuesTemplate);
        for (int i = 1; i < cnt; i++) {
            sb.append(",").append(valuesTemplate);
        }
        String valuesTemplates = sb.toString();
        return valuesTemplates;
    }

    /**
     * Creates the event in the event table and returns the assigned event id.
     * @param pid the id of the partner sending the event.
     * @param ts the timestamp contained in the event.
     * @param conn the database connection to use.
     * @return the event id of the created event.
     * @throws java.sql.SQLException if anything goes wrong.
     */
    private long createEvent(int pid, long ts, Connection conn) throws SQLException {
        PreparedStatement insertStmt = null;
        try {
            insertStmt = conn.prepareStatement("INSERT INTO eids (eid, time, pid) VALUES (DEFAULT, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            insertStmt.setLong(1, ts);
            insertStmt.setInt(2, pid);
            insertStmt.execute();
            ResultSet idRs = insertStmt.getGeneratedKeys();
            idRs.next();
            return idRs.getInt(1);
        } finally {
            StorageUtils.close(insertStmt);
        }
    }

    /**
     * Checks that a partner is authorized to read or write a property of an 
     * item. Altough the name suggests, it has nothing to do with assertions as
     * this is a runtime check. Throws authorization error if not authorized.
     * @param item the item in question.
     * @param partner the partner who wants to access a property.
     * @param properties the list of properties the partner wants to access.
     * @param right the action the partner wants to do (i.e. read or write).
     */
    private void assertAuthorized(Connection conn, String id, int host, String partner, Collection<Integer> properties, int right) throws AuthorizationFault, ExistenceFault, GeneralFault, SQLException {
        if (right == PropertyRight.NONE || partner == null) {
            assertHasProperties(conn, id, host, properties);
        } else {
            Collection<Integer> props;
            props = getAuthorizedProps(conn, id, host, partner, properties, right);
            if (properties.size() != props.size()) {
                properties.removeAll(props);
                Set<String> ps = getProperties(conn, id, host);
                LinkedList<String> unauthProps = new LinkedList<String>();
                LinkedList<String> unexistProps = new LinkedList<String>();
                for (Integer p : properties) {
                    String extRep = representer.getExternalRep(p);
                    if (ps.contains(extRep)) {
                        unauthProps.add(extRep);
                    } else {
                        unexistProps.add(extRep);
                    }
                }
                if (unexistProps.size() == 0) {
                    throw new AuthorizationFault("You are not authorized to read/write the " + StringUtils.join(unauthProps.iterator(), ", ") + " properties of item " + getIdUri(id, host));
                } else {
                    throw new ExistenceFault("Properties " + StringUtils.join(unexistProps.iterator(), ", ") + " of item " + getIdUri(id, host) + " does not exist.");
                }
            }
        }
    }

    private final Set<String> getProperties(Connection conn, String id, int host) throws GeneralFault {
        return new HashSet<String>(Arrays.asList(metadata.getProperties(conn, id, host)));
    }

    private final String getIdUri(String id, int host) {
        return id + "@{" + representer.getExternalRep(host) + "}";
    }

    private final void assertHasProperties(Connection conn, String id, int host, Collection<Integer> properties) throws ExistenceFault, GeneralFault {
        LinkedList<String> unexistProps = new LinkedList<String>();
        Set<String> ps = getProperties(conn, id, host);
        for (Integer p : properties) {
            String extRep = representer.getExternalRep(p);
            if (!ps.contains(extRep)) {
                unexistProps.add(extRep);
            }
        }
        if (unexistProps.size() > 0) {
            throw new ExistenceFault("Properties " + StringUtils.join(unexistProps.iterator(), ", ") + " of item " + getIdUri(id, host) + " does not exist.");
        }
    }

    /** Returns the iteration of properties affected by an event.
     * @param evt
     * @return
     */
    private Iterable<String> getAffectedProperties(CreateEvent evt) {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        for (CreateEvent.ItemProperty iProp : evt.getItemProperties()) {
            result.add(iProp.getName());
        }
        return result;
    }

    public void saveEvent(CreateEvent evt) throws ExistenceFault, AuthorizationFault, GeneralFault {
        if (isEmbedded()) {
            synchronized (this) {
                saveEventHelper(evt);
            }
        } else {
            saveEventHelper(evt);
        }
    }

    public void saveEventHelper(CreateEvent evt) throws ExistenceFault, AuthorizationFault, GeneralFault {
        Connection conn = null;
        Iterable<String> properties = getAffectedProperties(evt);
        boolean finished = false;
        try {
            Identifier item = resolver.resolve(evt.getItem().getItemId());
            Collection<Integer> props = representer.getInternalReps(properties);
            conn = getConnection();
            conn.setAutoCommit(false);
            long ts = evt.getTimestamp().getTime();
            int hostIndex = representer.lookUpInternalRep(item.getHost().toString());
            assertAuthorized(conn, item.getId(), hostIndex, evt.getSender(), props, PropertyRight.WRITE);
            metadata.updateTimeStamps(conn, item.getId(), hostIndex, ts, props);
            int pid = representer.lookUpPartner(evt.getSender());
            long eid = createEvent(pid, ts, conn);
            if (evt.sizeEventProperty() > 0) {
                PreparedStatement insertEventProperty = null;
                try {
                    insertEventProperty = conn.prepareStatement("INSERT INTO eprops (eid, ts, pid, prop, hashvalue, value) VALUES (?, ?, ?, ?, ?, ?)");
                    insertEventProperty.setLong(1, eid);
                    insertEventProperty.setLong(2, ts);
                    insertEventProperty.setInt(3, pid);
                    for (CreateEvent.EventProperty eProp : evt.getEventProperties()) {
                        byte[] bytes = XmlUtils.toByteArray(eProp.getAny());
                        insertEventProperty.setInt(4, representer.lookUpInternalRep(eProp.getName()));
                        insertEventProperty.setLong(5, computeHash(bytes));
                        StorageUtils.setBlob(insertEventProperty, 6, bytes);
                        insertEventProperty.execute();
                    }
                } catch (Exception ex) {
                    throw new SQLException("Cannot transform XML representation.", ex);
                } finally {
                    StorageUtils.close(insertEventProperty);
                }
            }
            int propNum = evt.sizeItemProperty();
            LinkedHashSet<Integer> _toClear = new LinkedHashSet<Integer>(propNum);
            HashMap<Integer, Set<ValueId>> _toAdd = new HashMap<Integer, Set<ValueId>>(propNum);
            HashMap<Integer, List<ValueId>> _toRemove = new HashMap<Integer, List<ValueId>>(propNum);
            List<ValueId> valueIds = new LinkedList<ValueId>();
            for (CreateEvent.ItemProperty iProp : evt.getItemProperties()) {
                int pI = representer.lookUpInternalRep(iProp.getName());
                boolean cleared = false;
                Set<ValueId> _add = null;
                List<ValueId> _remove = null;
                if (iProp.getClear() != null && iProp.getClear()) {
                    _toClear.add(pI);
                    cleared = true;
                }
                for (CreateEvent.ItemProperty.Value value : iProp.getValues()) {
                    if (value.getAction() == CreateEvent.ItemProperty.Value.Action.ADD) {
                        if (_add == null) {
                            _add = new HashSet<ValueId>(iProp.sizeValue());
                            _toAdd.put(pI, _add);
                        }
                        try {
                            byte[] bytes = XmlUtils.toByteArray(value.getAny());
                            ValueId vId = new ValueId(bytes, true);
                            _add.add(vId);
                            valueIds.add(vId);
                        } catch (Exception ex) {
                            Logger.getLogger(ItemDataStoragerImpl.class.getName()).log(Level.SEVERE, null, ex);
                            throw new SQLException("Could not intern value.", ex);
                        }
                    } else if (!cleared) {
                        if (_remove == null) {
                            _remove = new ArrayList<ValueId>(iProp.sizeValue());
                            _toRemove.put(pI, _remove);
                        }
                        try {
                            byte[] bytes = XmlUtils.toByteArray(value.getAny());
                            ValueId vId = new ValueId(bytes, false);
                            _remove.add(vId);
                            valueIds.add(vId);
                        } catch (Exception ex) {
                            Logger.getLogger(ItemDataStoragerImpl.class.getName()).log(Level.SEVERE, null, ex);
                            throw new SQLException("Could not intern value.", ex);
                        }
                    }
                }
            }
            representer.getValueIds(conn, valueIds);
            if (!_toClear.isEmpty()) {
                saveEvent_clear(conn, eid, pid, ts, item.getId(), hostIndex, _toClear);
            }
            if (!_toRemove.isEmpty()) {
                HashMap<Integer, List<Long>> toRemove = new HashMap<Integer, List<Long>>(propNum);
                for (Map.Entry<Integer, List<ValueId>> e : _toRemove.entrySet()) {
                    List<Long> _list = new ArrayList<Long>(e.getValue().size());
                    for (ValueId v : e.getValue()) {
                        _list.add(v.getId());
                    }
                    toRemove.put(e.getKey(), _list);
                }
                saveEvent_remove(conn, item.getId(), hostIndex, toRemove, eid, pid, ts);
            }
            if (!_toAdd.isEmpty()) {
                HashMap<Integer, Set<Long>> toAdd = new HashMap<Integer, Set<Long>>(propNum);
                for (Map.Entry<Integer, Set<ValueId>> e : _toAdd.entrySet()) {
                    Set<Long> _set = new HashSet<Long>(e.getValue().size());
                    for (ValueId v : e.getValue()) {
                        _set.add(v.getId());
                    }
                    toAdd.put(e.getKey(), _set);
                }
                saveEvent_add_bulk(conn, item.getId(), hostIndex, toAdd, eid, pid, ts);
            }
            finished = true;
        } catch (SchemeException ex) {
            throw new GeneralFault("Could not resolve provided identifier.", ex);
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "Database error", ex);
            throw new GeneralFault("Database error.", ex);
        } finally {
            if (finished) {
                try {
                    conn.commit();
                } catch (SQLException ex1) {
                    LOG.log(Level.SEVERE, "Could not commit changes", ex1);
                }
            } else {
                try {
                    conn.rollback();
                } catch (SQLException ex1) {
                    LOG.log(Level.SEVERE, "Could not roll back database after error", ex1);
                }
            }
            StorageUtils.close(conn);
        }
    }

    private final void saveEvent_add_bulk(Connection conn, String id, int hostIndex, Map<Integer, Set<Long>> toAdd, long eid, int pid, long ts) throws SQLException {
        HashSet<Long> values = new HashSet<Long>();
        for (Set<Long> l : toAdd.values()) {
            values.addAll(l);
        }
        Set<Integer> props = toAdd.keySet();
        try {
            saveEvent_add_bulk_check(conn, props, values, id, hostIndex, toAdd);
            saveEvent_add_bulk_insert(conn, id, hostIndex, eid, pid, ts, toAdd);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "", ex);
            throw new SQLException("Could not add item property value.", ex);
        }
    }

    private final void saveEvent_add_bulk_insert(Connection conn, String id, int hostIndex, long eid, int pid, long ts, Map<Integer, Set<Long>> toAdd) throws SQLException {
        PreparedStatement addStmt = null;
        int cnt = 0;
        for (Set<Long> s : toAdd.values()) {
            cnt += s.size();
        }
        if (cnt == 0) {
            return;
        }
        String valuesTemplates = constructInsertValuesPart(cnt, "(?, ?, ?, ?, ?, ?, ?, ?)");
        cnt = 0;
        try {
            addStmt = conn.prepareStatement("INSERT INTO iprops (id, host, seid, spid, sts, dir, prop, value) " + "VALUES " + valuesTemplates);
            for (Map.Entry<Integer, Set<Long>> entry : toAdd.entrySet()) {
                for (Long val : entry.getValue()) {
                    addStmt.setString(1 + cnt * 8, id);
                    addStmt.setInt(2 + cnt * 8, hostIndex);
                    addStmt.setLong(3 + cnt * 8, eid);
                    addStmt.setInt(4 + cnt * 8, pid);
                    addStmt.setLong(5 + cnt * 8, ts);
                    addStmt.setBoolean(6 + cnt * 8, Direction.ORDINARY);
                    addStmt.setInt(7 + cnt * 8, entry.getKey());
                    addStmt.setLong(8 + cnt * 8, val);
                    cnt++;
                }
            }
            addStmt.executeUpdate();
        } finally {
            StorageUtils.close(addStmt);
        }
    }

    private final void saveEvent_add_bulk_check(Connection conn, Set<Integer> props, HashSet<Long> values, String id, int hostIndex, Map<Integer, Set<Long>> toAdd) throws SQLException {
        PreparedStatement checkStmt = null;
        try {
            checkStmt = conn.prepareStatement("SELECT prop, value FROM iprops " + "WHERE  id = ? AND host = ? AND dir = ? AND eeid IS NULL " + "AND prop IN (" + StringUtils.join(props.iterator(), ", ") + ") " + "AND value IN (" + StringUtils.join(values.iterator(), ", ") + ")" + "ORDER BY prop, value");
            checkStmt.setString(1, id);
            checkStmt.setInt(2, hostIndex);
            checkStmt.setBoolean(3, Direction.ORDINARY);
            ResultSet rs = checkStmt.executeQuery();
            Integer lastProp = null;
            Set<Long> propValues = null;
            while (rs.next()) {
                Integer curProp = rs.getInt(1);
                if (curProp != lastProp) {
                    propValues = toAdd.get(curProp);
                    lastProp = curProp;
                }
                propValues.remove(rs.getLong(2));
            }
        } finally {
            StorageUtils.close(checkStmt);
        }
    }

    private final void saveEvent_remove(Connection conn, String id, int hostIndex, Map<Integer, List<Long>> toDelete, long eid, int pid, long ts) throws SQLException {
        PreparedStatement removeQuery = null;
        try {
            removeQuery = conn.prepareStatement("SELECT updid, value, eeid, epid, ets, closed FROM iprops " + "WHERE closed = ? AND id = ? AND host = ? AND prop = ? AND dir = ? AND value = ?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            removeQuery.setBoolean(1, Status.VALID);
            removeQuery.setString(2, id);
            removeQuery.setInt(3, hostIndex);
            for (Map.Entry<Integer, List<Long>> entry : toDelete.entrySet()) {
                removeQuery.setInt(4, entry.getKey());
                removeQuery.setBoolean(5, Direction.ORDINARY);
                for (Long val : entry.getValue()) {
                    removeQuery.setLong(6, val);
                    ResultSet rs = removeQuery.executeQuery();
                    while (rs.next()) {
                        rs.updateBoolean("closed", Status.CLOSED);
                        rs.updateLong("eeid", eid);
                        rs.updateInt("epid", pid);
                        rs.updateLong("ets", ts);
                        rs.updateRow();
                    }
                    StorageUtils.close(rs);
                }
            }
        } catch (Exception ex) {
            throw new SQLException("Could not remove item properties.", ex);
        } finally {
            StorageUtils.close(removeQuery);
        }
    }

    private final void saveEvent_clear(Connection conn, long eid, int pid, long ts, String id, int hostIndex, LinkedHashSet<Integer> toClear) throws SQLException {
        PreparedStatement updatePropRows = null;
        try {
            updatePropRows = conn.prepareStatement("UPDATE iprops SET eeid = ?, epid = ?, ets = ?, closed = ? " + "WHERE closed = ? AND id = ? AND host = ? AND dir = ? " + "AND prop IN (" + StringUtils.join(toClear.iterator(), ", ") + ")");
            updatePropRows.setLong(1, eid);
            updatePropRows.setInt(2, pid);
            updatePropRows.setLong(3, ts);
            updatePropRows.setBoolean(4, Status.CLOSED);
            updatePropRows.setBoolean(5, Status.VALID);
            updatePropRows.setString(6, id);
            updatePropRows.setInt(7, hostIndex);
            updatePropRows.setBoolean(8, Direction.ORDINARY);
            updatePropRows.execute();
        } catch (Exception ex) {
            throw new SQLException("Could not close valid property values.", ex);
        } finally {
            StorageUtils.close(updatePropRows);
        }
    }

    /**
     * Normalizes variables.
     * @param <T>
     * @param val
     * @param def
     * @return
     */
    private static <T> T norm(T val, T def) {
        return val == null ? def : val;
    }

    public PropertyValuesReport getPropertyValues(PropertyValuesReport result) throws AuthorizationFault, ExistenceFault, GeneralFault {
        if (isEmbedded()) {
            synchronized (this) {
                return getPropertyValuesHelper(result);
            }
        } else {
            return getPropertyValuesHelper(result);
        }
    }

    public PropertyValuesReport getPropertyValuesHelper(PropertyValuesReport result) throws AuthorizationFault, ExistenceFault, GeneralFault {
        Connection conn = null;
        try {
            conn = getConnection();
            int hostid = representer.lookUpInternalRep(result.getItem().getResolved().getHost());
            HashMap<Integer, PropertyValueResponse> props = new HashMap<Integer, PropertyValueResponse>();
            for (PropertyValueResponse pvr : result.getProperties()) {
                if ((norm(pvr.getCanRead(), false) && pvr.getHostedBy() == null) || norm(pvr.getIsCached(), false)) {
                    props.put(representer.lookUpInternalRep(pvr.getPropertyName()), pvr);
                }
            }
            PreparedStatement selectProperties = null;
            try {
                if (props.size() > 0) {
                    selectProperties = conn.prepareStatement("SELECT updid, prop, valueids.value, sts, spid, epid, ets, closed FROM iprops JOIN valueids " + "ON iprops.value = valueids.valueid WHERE " + "id = ? AND host = ? AND dir = ? AND " + "(ets > ? OR ets is NULL) AND sts < ? AND " + "prop IN (" + StringUtils.join(props.keySet().iterator(), ",") + ")");
                    selectProperties.setString(1, result.getItem().getResolved().getId());
                    selectProperties.setInt(2, hostid);
                    selectProperties.setBoolean(3, Direction.ORDINARY);
                    selectProperties.setLong(4, result.getFrom() == null ? 0 : result.getFrom().getTime());
                    selectProperties.setLong(5, result.getTo() == null ? Long.MAX_VALUE : result.getTo().getTime());
                    ResultSet rs = selectProperties.executeQuery();
                    while (rs.next()) {
                        Value value = new Value();
                        OMElement val = StorageUtils.getOMElement(rs, "value");
                        try {
                            value.setId(Long.toHexString(rs.getLong("updid")));
                            value.setAny(XMLUtils.toDOM(val));
                            value.setFrom(new Date(rs.getLong("sts")));
                            value.setFromPartner(representer.getExternalRep(rs.getInt("spid")));
                            long ets = rs.getLong("ets");
                            if (!rs.wasNull() && rs.getBoolean("closed") == Status.CLOSED) {
                                value.setTo(new Date(ets));
                                value.setToPartner(representer.getExternalRep(rs.getInt("epid")));
                            }
                            props.get(rs.getInt("prop")).addValue(value);
                        } catch (Exception ex) {
                            Logger.getLogger(ItemDataStoragerImpl.class.getName()).log(Level.SEVERE, "Cannot add value to response.", ex);
                        }
                    }
                }
                return result;
            } finally {
                StorageUtils.close(selectProperties);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Database error.", ex);
        } catch (XMLStreamException ex) {
            throw new RuntimeException("Could not serialize property.", ex);
        } finally {
            StorageUtils.close(conn);
        }
    }

    @Override
    public PropertyValuesReport getPropertyValues(Identifier item, Date from, Date to, Collection<String> properties, String partner) throws AuthorizationFault, ExistenceFault, GeneralFault {
        PropertyValuesReport result = metadata.getItemDataSkeleton(item, partner, properties.toArray(new String[] {}));
        manager.get(Resolver.class).resolve(result.getItem());
        if (from == null) {
            from = new Date(0);
        }
        if (to == null) {
            to = new Date();
        }
        result.setFrom(from);
        result.setTo(to);
        return getPropertyValues(result);
    }

    public Iterable<OMElement> getUniquePropertyValues(String property, String partner) {
        Connection conn = null;
        try {
            conn = getConnection();
            int prop = representer.lookUpInternalRep(property);
            PreparedStatement selectValues = null;
            PreparedStatement getValues = null;
            try {
                selectValues = conn.prepareStatement("SELECT DISTINCT id, host, value FROM iprops " + "WHERE prop = ? AND dir = ?");
                selectValues.setInt(1, prop);
                selectValues.setBoolean(2, Direction.ORDINARY);
                ResultSet rs = selectValues.executeQuery();
                LinkedList<Long> values = new LinkedList<Long>();
                while (rs.next()) {
                    if (partner == null) {
                        values.add(rs.getLong("value"));
                    } else {
                        try {
                            Identifier id = BasicIdUriResolver.getIdentifier(rs.getString(1), new URI(representer.getExternalRep(rs.getInt(2))));
                            if (metadata.getAuthorization(id, partner, Partner.Value.READ, property)) {
                                values.add(rs.getLong("value"));
                            }
                        } catch (GeneralFault ex) {
                            LOG.log(Level.WARNING, "Could not add value to list of unique values", ex);
                        } catch (URISyntaxException ex) {
                            LOG.log(Level.WARNING, "Could not add value to list of unique values", ex);
                        }
                    }
                }
                LinkedList<OMElement> result = new LinkedList<OMElement>();
                if (values.size() > 0) {
                    getValues = conn.prepareStatement("SELECT value FROM valueids WHERE valueid IN (" + StringUtils.join(values.iterator(), ",") + ")");
                    rs = getValues.executeQuery();
                    while (rs.next()) {
                        result.add(StorageUtils.getOMElement(rs, "value"));
                    }
                }
                return result;
            } finally {
                StorageUtils.close(selectValues);
                StorageUtils.close(getValues);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Database error.", ex);
        } catch (XMLStreamException ex) {
            throw new RuntimeException("Could not de-serialize property value.", ex);
        } finally {
            StorageUtils.close(conn);
        }
    }

    public Iterable<Identifier> getItemList(Iterable<PropertyValueUpdate> filters, Date start, Date end, String partner, int limit) throws GeneralFault {
        Connection conn = null;
        try {
            conn = getConnection();
            PreparedStatement selectProperties = null;
            LinkedList<Integer> props = new LinkedList<Integer>();
            int propsNum;
            boolean first = true;
            try {
                LinkedHashSet<String> resultAcc = new LinkedHashSet<String>();
                if (filters.iterator().hasNext()) {
                    selectProperties = conn.prepareStatement("SELECT id, host, value " + "FROM iprops WHERE prop = ? AND dir = ? " + "AND (ets > ? OR ets is NULL) AND sts < ? " + "AND value = ?" + (limit == 0 ? "" : " LIMIT " + limit));
                    selectProperties.setBoolean(2, Direction.ORDINARY);
                    selectProperties.setLong(3, start == null ? 0 : start.getTime());
                    selectProperties.setLong(4, end == null ? Long.MAX_VALUE : end.getTime());
                    for (PropertyValueUpdate prop : filters) {
                        int propName = representer.lookUpInternalRep(prop.getPropertyName());
                        props.add(propName);
                        selectProperties.setInt(1, propName);
                        byte[] bytes = XmlUtils.toByteArray(prop.getAny());
                        ValueId valueId = new ValueId(bytes, false);
                        long vId = representer.getValueId(conn, valueId);
                        selectProperties.setLong(5, vId);
                        ResultSet rs = selectProperties.executeQuery();
                        LinkedHashSet<String> resultAcc0 = new LinkedHashSet<String>();
                        while (rs.next()) {
                            resultAcc0.add(rs.getString("id") + "@{" + representer.getExternalRep(rs.getInt("host")) + "}");
                        }
                        if (first) {
                            resultAcc.addAll(resultAcc0);
                            first = false;
                        } else {
                            resultAcc.retainAll(resultAcc0);
                        }
                    }
                } else {
                    selectProperties = conn.prepareStatement("SELECT DISTINCT id, host FROM meta " + "WHERE type = ? AND value IS NOT NULL");
                    selectProperties.setInt(1, MetaDataStoragerImpl.META_EXISTENCE);
                    ResultSet rs = selectProperties.executeQuery();
                    while (rs.next()) {
                        resultAcc.add(rs.getString("id") + "@{" + representer.getExternalRep(rs.getInt("host")) + "}");
                    }
                }
                LinkedList<Identifier> result = new LinkedList<Identifier>();
                propsNum = props.size();
                boolean noPartner = partner == null;
                for (String iduri : resultAcc) {
                    try {
                        Identifier item = resolver.resolve(iduri);
                        boolean auth = noPartner || propsNum == getAuthorizedProps(conn, item, partner, props, PropertyRight.READ).size();
                        if (auth) {
                            result.add(item);
                        }
                    } catch (SchemeException _) {
                    }
                }
                return result;
            } catch (Exception ex) {
                throw new GeneralFault("Could not deserialize filter value", ex);
            } finally {
                StorageUtils.close(selectProperties);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Database error.", ex);
        } finally {
            StorageUtils.close(conn);
        }
    }

    /**
     * Retuns the list of
     * @param partner
     * @param props
     * @return
     */
    private Collection<Integer> getAuthorizedProps(Connection conn, Identifier item, String partner, Collection<Integer> props, int right) throws GeneralFault, SQLException {
        int hostIndex = representer.lookUpInternalRep(item.getHost().toString());
        return getAuthorizedProps(conn, item.getId(), hostIndex, partner, props, right);
    }

    /**
     * Retuns the list of
     * @param partner
     * @param props
     * @return
     */
    private Collection<Integer> getAuthorizedProps(Connection conn, String id, int hostIndex, String partner, Collection<Integer> props, int right) throws GeneralFault, SQLException {
        LinkedList<Integer> result = new LinkedList<Integer>();
        if (props.size() == 0) {
            return result;
        }
        PreparedStatement selectRights = null;
        try {
            String properties = props.size() == 1 ? "= " + props.iterator().next() : "IN (" + StringUtils.join(props.iterator(), ",") + ")";
            selectRights = conn.prepareStatement("SELECT value, prop FROM auth " + "WHERE id = ? AND host = ? AND dir = ? AND partner = ? " + "AND prop " + properties);
            selectRights.setString(1, id);
            selectRights.setInt(2, hostIndex);
            selectRights.setBoolean(3, Direction.ORDINARY);
            selectRights.setInt(4, representer.lookUpPartner(partner));
            ResultSet rs = selectRights.executeQuery();
            while (rs.next()) {
                if (PropertyRight.contains(rs.getInt(1), right, true)) {
                    result.add(rs.getInt(2));
                }
            }
            return result;
        } finally {
            StorageUtils.close(selectRights);
        }
    }
}
