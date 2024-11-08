package it.simplerecords.base;

import it.simplerecords.annotations.ValidateExclusionOf;
import it.simplerecords.annotations.ValidateFormatOf;
import it.simplerecords.annotations.ValidateInclusionOf;
import it.simplerecords.annotations.ValidateLengthOf;
import it.simplerecords.annotations.ValidateNumericalityOf;
import it.simplerecords.annotations.ValidatePresenceOf;
import it.simplerecords.annotations.ValidateUniquenessOf;
import it.simplerecords.connection.ConnectionManager;
import it.simplerecords.constants.Messages;
import it.simplerecords.constants.TablesDependancies;
import it.simplerecords.exceptions.FieldOrMethodNotFoundException;
import it.simplerecords.exceptions.RecordException;
import it.simplerecords.exceptions.RecordValidationException;
import it.simplerecords.exceptions.RecordValidationSyntax;
import it.simplerecords.util.FieldHandler;
import it.simplerecords.util.LoggableStatement;
import it.simplerecords.util.Logger;
import it.simplerecords.util.Printable;
import it.simplerecords.util.StatementBuilder;
import it.simplerecords.util.TableNameResolver;
import it.simplerecords.util.XUIDGenerator;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

public abstract class RecordAbstract extends Printable implements Record {

    /**
	 * A logger utility. Must be switched with something more powerful
	 */
    protected Logger log = new Logger(this.getClass());

    /**
	 * If this field is true the object is frozen, i.e. no DB modifying operations can be called on it
	 */
    protected boolean frozen = false;

    protected static List<Class<? extends Record>> ownersList;

    protected Map<Class<? extends Record>, Record> ownerObjects;

    /**
	 * List of related tables, from a one to one relationship
	 */
    protected static Map<Class<? extends Record>, TablesDependancies> childList;

    /**
	 * List of records from related tables, from a one to one relationship
	 */
    protected Map<Class<? extends Record>, Record> childObjects;

    /**
	 * List of related tables, from a one to many relationship
	 */
    protected static Map<Class<? extends Record>, TablesDependancies> childrenList;

    /**
	 * List of records from related tables, from a one to many relationship
	 */
    protected Map<Class<? extends Record>, List<? extends Record>> childrenObjects;

    /**
	 * List of related tables, from a many to many relationship
	 */
    protected static Map<Class<? extends Record>, TablesDependancies> relatedList;

    /**
	 * List of records from related tables, from a many to many relationship
	 */
    protected Map<Class<? extends Record>, List<? extends Record>> relatedObjects;

    private boolean ignoreValidations;

    @Override
    public final boolean delete() throws RecordException {
        if (frozen) {
            throw new RecordException("The object is frozen.");
        }
        Connection conn = ConnectionManager.getConnection();
        LoggableStatement pStat = null;
        Class<? extends Record> actualClass = this.getClass();
        StatementBuilder builder = null;
        try {
            builder = new StatementBuilder("delete from " + TableNameResolver.getTableName(actualClass) + " where id = :id");
            Field f = FieldHandler.findField(this.getClass(), "id");
            builder.set("id", FieldHandler.getValue(f, this));
            pStat = builder.getPreparedStatement(conn);
            log.log(pStat.getQueryString());
            int i = pStat.executeUpdate();
            return i == 1;
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                throw new RecordException("Error executing rollback");
            }
            throw new RecordException(e);
        } finally {
            try {
                if (pStat != null) {
                    pStat.close();
                }
                conn.commit();
                conn.close();
            } catch (SQLException e) {
                throw new RecordException("Error closing connection");
            }
        }
    }

    @Override
    public final boolean exists() throws RecordException {
        Connection conn = ConnectionManager.getConnection();
        LoggableStatement pStat = null;
        Class<? extends Record> actualClass = this.getClass();
        String tableName = TableNameResolver.getTableName(actualClass);
        String sql = "select * from " + tableName + " where id = :id";
        StatementBuilder builder = new StatementBuilder(sql);
        try {
            Field f = FieldHandler.findField(this.getClass(), "id");
            Object idValue = FieldHandler.getValue(f, this);
            if ((Integer) idValue == 0) {
                return false;
            }
            builder.set("id", idValue);
        } catch (Exception e) {
            throw new RecordException(e);
        }
        try {
            pStat = builder.getPreparedStatement(conn);
            log.log(pStat.getQueryString());
            ResultSet rs = pStat.executeQuery();
            boolean exists = rs.next();
            return exists;
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                throw new RecordException("Error executing rollback");
            }
            throw new RecordException(e);
        } finally {
            try {
                if (pStat != null) {
                    pStat.close();
                }
                conn.commit();
                conn.close();
            } catch (SQLException e) {
                throw new RecordException("Error closing connection");
            }
        }
    }

    @Override
    public final Record getClone() throws RecordException {
        Class<? extends Record> actualClass = this.getClass();
        Record b = null;
        try {
            b = actualClass.cast(actualClass.newInstance());
            Field[] fields = actualClass.getDeclaredFields();
            for (Field f : fields) {
                f.set(b, FieldHandler.getValue(f, this));
            }
        } catch (Exception e) {
            throw new RecordException(e);
        }
        return b;
    }

    @Override
    public final boolean save() throws RecordException, RecordValidationException, RecordValidationSyntax {
        if (frozen) {
            throw new RecordException("The object is frozen.");
        }
        boolean toReturn = false;
        Class<? extends Record> actualClass = this.getClass();
        HashMap<String, Integer> columns = getColumns(TableNameResolver.getTableName(actualClass));
        Connection conn = ConnectionManager.getConnection();
        LoggableStatement pStat = null;
        try {
            if (exists()) {
                doValidations(true);
                StatementBuilder builder = new StatementBuilder("update " + TableNameResolver.getTableName(actualClass) + " set");
                String updates = "";
                for (String key : columns.keySet()) {
                    if (!key.equals("id")) {
                        Field f = null;
                        try {
                            f = FieldHandler.findField(actualClass, key);
                        } catch (FieldOrMethodNotFoundException e) {
                            throw new RecordException("Database column name >" + key + "< not found in class " + actualClass.getCanonicalName());
                        }
                        updates += key + " = :" + key + ", ";
                        builder.set(key, FieldHandler.getValue(f, this));
                    }
                }
                builder.append(updates.substring(0, updates.length() - 2));
                builder.append("where id = :id");
                builder.set(":id", FieldHandler.getValue(FieldHandler.findField(actualClass, "id"), this));
                pStat = builder.getPreparedStatement(conn);
                log.log(pStat.getQueryString());
                int i = pStat.executeUpdate();
                toReturn = i == 1;
            } else {
                doValidations(false);
                StatementBuilder builder = new StatementBuilder("insert into " + TableNameResolver.getTableName(actualClass) + " ");
                String names = "";
                String values = "";
                for (String key : columns.keySet()) {
                    Field f = null;
                    try {
                        f = FieldHandler.findField(actualClass, key);
                    } catch (FieldOrMethodNotFoundException e) {
                        throw new RecordException("Database column name >" + key + "< not found in class " + actualClass.getCanonicalName());
                    }
                    if (key.equals("id") && (Integer) FieldHandler.getValue(f, this) == 0) {
                        continue;
                    }
                    names += key + ", ";
                    values += ":" + key + ", ";
                    builder.set(key, f.get(this));
                }
                names = names.substring(0, names.length() - 2);
                values = values.substring(0, values.length() - 2);
                builder.append("(" + names + ")");
                builder.append("values");
                builder.append("(" + values + ")");
                pStat = builder.getPreparedStatement(conn);
                log.log(pStat.getQueryString());
                int i = pStat.executeUpdate();
                toReturn = i == 1;
            }
            if (childList != null) {
                if (childObjects == null) {
                    childObjects = new HashMap<Class<? extends Record>, Record>();
                }
                for (Class<? extends Record> c : childList.keySet()) {
                    if (childObjects.get(c) != null) {
                        childObjects.get(c).save();
                    }
                }
            }
            if (childrenList != null) {
                if (childrenObjects == null) {
                    childrenObjects = new HashMap<Class<? extends Record>, List<? extends Record>>();
                }
                for (Class<? extends Record> c : childrenList.keySet()) {
                    if (childrenObjects.get(c) != null) {
                        for (Record r : childrenObjects.get(c)) {
                            r.save();
                        }
                    }
                }
            }
            if (relatedList != null) {
                if (childrenObjects == null) {
                    childrenObjects = new HashMap<Class<? extends Record>, List<? extends Record>>();
                }
                for (Class<? extends Record> c : relatedList.keySet()) {
                    if (childrenObjects.get(c) != null) {
                        for (Record r : childrenObjects.get(c)) {
                            r.save();
                        }
                    }
                }
            }
            return toReturn;
        } catch (Exception e) {
            if (e instanceof RecordValidationException) {
                throw (RecordValidationException) e;
            }
            if (e instanceof RecordValidationSyntax) {
                throw (RecordValidationSyntax) e;
            }
            try {
                conn.rollback();
            } catch (SQLException e1) {
                throw new RecordException("Error executing rollback");
            }
            throw new RecordException(e);
        } finally {
            try {
                if (pStat != null) {
                    pStat.close();
                }
                conn.commit();
                conn.close();
            } catch (SQLException e) {
                throw new RecordException("Error closing connection");
            }
        }
    }

    @Override
    public final boolean update() throws RecordException {
        if (frozen) {
            throw new RecordException("The object is frozen.");
        }
        boolean toReturn = false;
        Class<? extends Record> actualClass = this.getClass();
        HashMap<String, Integer> columns = getColumns(TableNameResolver.getTableName(actualClass));
        LoggableStatement pStat = null;
        Connection conn = ConnectionManager.getConnection();
        try {
            if (exists()) {
                StatementBuilder builder = new StatementBuilder("update " + TableNameResolver.getTableName(actualClass) + " set");
                String updates = "";
                for (String key : columns.keySet()) {
                    if (!key.equals("id")) {
                        updates += key + " = :" + key + ", ";
                        Field f = FieldHandler.findField(actualClass, key);
                        builder.set(key, FieldHandler.getValue(f, this));
                    }
                }
                builder.append(updates.substring(0, updates.length() - 2));
                builder.append("where id = :id");
                builder.set(":id", FieldHandler.getValue(FieldHandler.findField(actualClass, "id"), this));
                pStat = builder.getPreparedStatement(conn);
                log.log(pStat.getQueryString());
                int i = pStat.executeUpdate();
                toReturn = i == 1;
            } else {
                throw new RecordException(Messages.SR_UPDATE_IMPOSSIBLE);
            }
            if (childList != null) {
                if (childObjects == null) {
                    childObjects = new HashMap<Class<? extends Record>, Record>();
                }
                for (Class<? extends Record> c : childList.keySet()) {
                    childObjects.get(c).update();
                }
            }
            if (childrenList != null) {
                if (childrenObjects == null) {
                    childrenObjects = new HashMap<Class<? extends Record>, List<? extends Record>>();
                }
                for (Class<? extends Record> c : childrenList.keySet()) {
                    for (Record r : childrenObjects.get(c)) {
                        r.update();
                    }
                }
            }
            if (relatedList != null) {
                if (childrenObjects == null) {
                    childrenObjects = new HashMap<Class<? extends Record>, List<? extends Record>>();
                }
                for (Class<? extends Record> c : relatedList.keySet()) {
                    for (Record r : childrenObjects.get(c)) {
                        r.update();
                    }
                }
            }
            return toReturn;
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                throw new RecordException("Error executing rollback");
            }
            throw new RecordException(e);
        } finally {
            try {
                if (pStat != null) {
                    pStat.close();
                }
                conn.commit();
                conn.close();
            } catch (SQLException e) {
                throw new RecordException("Error closing connection");
            }
        }
    }

    /**
	 * Freezes the object. Any further DB methods called on the object (apart from find and exist that do not change the DB status) will
	 * throw an exception. Warning, this method is irreversible
	 */
    public final void freeze() {
        frozen = true;
    }

    /**
	 * Gets column names and informations from a DatabaseMetaData object.
	 * 
	 * @param tableName
	 *           The name of the table
	 * @return An HashMap<String, Integer> containing the column name and the column type.
	 * @throws RecordException
	 *            Any SqlException raised in the method
	 */
    public static HashMap<String, Integer> getColumns(String tableName) throws RecordException {
        Connection conn = ConnectionManager.getConnection();
        DatabaseMetaData dbMeta = null;
        HashMap<String, Integer> columns = null;
        try {
            dbMeta = conn.getMetaData();
            ResultSet columnsRs = dbMeta.getColumns(null, null, tableName, null);
            columns = new HashMap<String, Integer>();
            while (columnsRs.next()) {
                columns.put(columnsRs.getString(4), columnsRs.getInt(5));
            }
        } catch (Exception e) {
            throw new RecordException("Error analizing table " + tableName, e);
        } finally {
            try {
                conn.commit();
                conn.close();
            } catch (SQLException e) {
                throw new RecordException("Error closing connection");
            }
        }
        return columns;
    }

    public static String getFieldName(Class<? extends Record> c) throws RecordException {
        String tableName = "";
        tableName = c.getSimpleName();
        char name[] = tableName.toCharArray();
        tableName = "";
        int i = 0;
        for (Character ch : name) {
            if (i > 0 && (int) ch >= 65 && (int) ch <= 90) {
                tableName += "_" + Character.toLowerCase(ch);
            } else {
                tableName += Character.toLowerCase(ch);
            }
            ++i;
        }
        return tableName;
    }

    /**
	 * Sets the value of a filed
	 * 
	 * @param b
	 *           The actual object containing the field
	 * @param attribute
	 *           The name of the field
	 * @param value
	 *           The value to bve stored in the field
	 * @throws RecordException
	 *            On any Exception raised inside the method
	 * @throws FieldOrMethodNotFoundException
	 */
    public static void setValue(Record b, String attribute, Object value) throws RecordException, FieldOrMethodNotFoundException {
        if (value == null) {
            return;
        }
        Field f = FieldHandler.findField(b.getClass(), attribute);
        FieldHandler.setValue(f, b, value);
    }

    /**
	 * Method to add parsed query conditions to a StatementBuilder
	 * 
	 * @param builder
	 *           The statement buider to use
	 * @param attributes
	 *           The list of conditions to add
	 * @param start
	 *           whether to start with a WHERE statement or an AND statement (to add other conditions before the passed ones)
	 */
    public static void addConditions(StatementBuilder builder, JSONObject attributes, boolean start) throws RecordException {
        if (start && attributes.has("conditions")) {
            builder.append("where");
        }
        if (attributes.has("conditions")) {
            try {
                JSONArray values = attributes.getJSONArray("conditions");
                if (values.length() > 0) {
                    String sql = values.getString(0);
                    for (int i = 1; i < values.length(); ++i) {
                        int pos = sql.indexOf('?');
                        if (pos == -1) {
                            throw new RecordException("No corresponding ? for parameter number " + (i - 1));
                        }
                        String key = ":" + XUIDGenerator.getUniqueID();
                        sql = sql.substring(0, pos) + key + sql.substring(pos + 1);
                        String raw = values.getString(i);
                        Object value = null;
                        try {
                            value = Integer.parseInt(raw);
                        } catch (NumberFormatException e) {
                        }
                        if (value == null) {
                            try {
                                value = Double.parseDouble(raw);
                            } catch (NumberFormatException e) {
                            }
                        }
                        if (value == null) {
                            value = raw;
                        }
                        builder.set(key, value);
                    }
                    builder.append(sql);
                }
            } catch (JSONException e) {
                try {
                    String condition = attributes.getString("conditions");
                    builder.append(condition);
                } catch (JSONException e1) {
                    throw new RecordException("Error parsing 'conditions' attribute.", e1);
                }
            }
        }
        if (attributes.has("group")) {
            try {
                JSONArray values = attributes.getJSONArray("group");
                if (values.length() > 0) {
                    builder.append("group by");
                    for (int i = 0; i < values.length(); ++i) {
                        builder.append(values.getString(i));
                        if (i != values.length() - 1) {
                            builder.append(", ");
                        }
                    }
                }
            } catch (JSONException e) {
                try {
                    String group = attributes.getString("group");
                    builder.append("group by " + group);
                } catch (JSONException e1) {
                    throw new RecordException("Error parsing 'group' attribute.", e1);
                }
            }
        }
        if (attributes.has("order")) {
            try {
                JSONArray values = attributes.getJSONArray("order");
                if (values.length() > 0) {
                    builder.append("order by");
                    for (int i = 0; i < values.length(); ++i) {
                        builder.append(values.getString(i));
                        if (i != values.length() - 1) {
                            builder.append(", ");
                        }
                    }
                }
            } catch (JSONException e) {
                try {
                    String order = attributes.getString("order");
                    if (order.length() > 0) {
                        builder.append(" order by " + order);
                    }
                } catch (JSONException e1) {
                    throw new RecordException("Error parsing 'order' attribute.", e1);
                }
            }
        }
        if (attributes.has("limit")) {
            try {
                builder.append("limit " + attributes.getInt("limit"));
            } catch (JSONException e) {
                throw new RecordException("Error parsing 'limit' attribute.", e);
            }
        }
        if (attributes.has("offset")) {
            try {
                builder.append("offset " + attributes.getInt("offset"));
            } catch (JSONException e) {
                throw new RecordException("Error parsing 'offset' attribute.", e);
            }
        }
    }

    public static void belongsTo(Class<? extends Record> record) {
        if (ownersList == null) {
            ownersList = new ArrayList<Class<? extends Record>>();
        }
        ownersList.add(record);
    }

    @SuppressWarnings("unchecked")
    private void getOwnerPrivate(Class<? extends Record> c) throws RecordException {
        try {
            Record r = c.newInstance();
            String foreignKey = "fk_" + getFieldName(c);
            int fk = 0;
            try {
                fk = (Integer) FieldHandler.getValue(FieldHandler.findField(this.getClass(), foreignKey), this);
            } catch (FieldOrMethodNotFoundException e) {
                throw new RecordException("Database column name >" + foreignKey + "< not found in class " + this.getClass().getCanonicalName());
            }
            if (r instanceof RecordBase) {
                r = ((RecordBase) r).find(fk).get(0);
            } else if (r instanceof StaticRecordBase) {
                try {
                    Method m = r.getClass().getSuperclass().getDeclaredMethod("find", Class.class, Integer[].class);
                    Object tmp = m.invoke(null, r.getClass(), new Integer[] { fk });
                    r = (Record) ((List<Record>) tmp).get(0);
                } catch (SecurityException e) {
                    throw new RecordException(e);
                } catch (NoSuchMethodException e) {
                    throw new RecordException(e);
                } catch (IllegalArgumentException e) {
                    throw new RecordException(e);
                } catch (InvocationTargetException e) {
                    throw new RecordException(e);
                }
            }
            if (ownerObjects == null) {
                ownerObjects = new HashMap<Class<? extends Record>, Record>();
            }
            ownerObjects.put(c, r);
        } catch (InstantiationException e) {
            throw new RecordException(e);
        } catch (IllegalAccessException e) {
            throw new RecordException(e);
        }
    }

    @Override
    public final Record getOwner(Class<? extends Record> record, boolean forceReload) throws RecordException, FieldOrMethodNotFoundException {
        if (ownersList == null || !ownersList.contains(record)) {
            throw new RecordException("Table " + TableNameResolver.getTableName(this.getClass()) + " does not belong to " + TableNameResolver.getTableName(record));
        }
        if (ownerObjects == null) {
            ownerObjects = new HashMap<Class<? extends Record>, Record>();
        }
        if (forceReload || ownerObjects.get(record) == null) {
            getOwnerPrivate(record);
            try {
                String fieldname = "fk_" + getFieldName(record);
                Field f = null;
                try {
                    f = FieldHandler.findField(record, "id");
                } catch (FieldOrMethodNotFoundException e) {
                    throw new RecordException("Database column name >" + fieldname + "< not found in class " + record.getCanonicalName());
                }
                Record r = ownerObjects.get(record);
                int val = (Integer) FieldHandler.getValue(f, r);
                setValue(this, fieldname, val);
            } catch (IllegalArgumentException e) {
                throw new RecordException(e);
            }
        }
        return ownerObjects.get(record);
    }

    public final List<? extends Record> getOwners(Class<? extends Record> record, boolean forceReload) throws RecordException {
        if (!relatedList.containsKey(record)) {
            throw new RecordException("Table " + TableNameResolver.getTableName(this.getClass()) + " is not in a many to many relationship with " + TableNameResolver.getTableName(record));
        }
        if (relatedObjects == null) {
            relatedObjects = new HashMap<Class<? extends Record>, List<? extends Record>>();
        }
        if (forceReload || relatedObjects.get(record) == null) {
            List<Integer> ids = new Vector<Integer>();
            String fkThis = "fk_" + TableNameResolver.getTableName(this.getClass());
            String fkOther = "fk_" + TableNameResolver.getTableName(record);
            String relationTableName = TableNameResolver.getRelationshipTableName(this.getClass(), record);
            Connection conn = ConnectionManager.getConnection();
            LoggableStatement pStat = null;
            ResultSet rs = null;
            try {
                StatementBuilder builder = new StatementBuilder("select " + fkOther + " from " + relationTableName + " where " + fkThis + "=:id");
                Field idThis = null;
                try {
                    idThis = FieldHandler.findField(this.getClass(), "id");
                } catch (FieldOrMethodNotFoundException e) {
                    throw new RecordException("Database column name >id< not found in class " + this.getClass().getCanonicalName());
                }
                builder.set(":id", FieldHandler.getValue(idThis, this));
                pStat = builder.getPreparedStatement(conn);
                log.log(pStat.getQueryString());
                rs = pStat.executeQuery();
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
                rs.close();
                pStat.close();
                if (ids.isEmpty()) {
                    relatedObjects.put(record, null);
                } else {
                    List<? extends Record> owners = null;
                    Record r = record.newInstance();
                    if (r instanceof RecordBase) {
                        owners = ((RecordBase) r).find(ids.toArray(new Integer[] {}));
                    } else {
                        try {
                            Method m = r.getClass().getSuperclass().getDeclaredMethod("find", Class.class, Integer[].class);
                            Object tmp = m.invoke(null, r.getClass(), ids.toArray(new Integer[] {}));
                            owners = (List<Record>) tmp;
                        } catch (Exception e) {
                            throw new RecordException(e);
                        }
                    }
                    relatedObjects.put(record, owners);
                }
            } catch (Exception e) {
                throw new RecordException("Unable to get record owners.", e);
            } finally {
                try {
                    if (rs != null) {
                        rs.close();
                    }
                    if (pStat != null) {
                        pStat.close();
                    }
                    conn.commit();
                    conn.close();
                } catch (SQLException e) {
                    throw new RecordException("Error closing the connection", e);
                }
            }
        }
        return relatedObjects.get(record);
    }

    @Override
    public final Record getOwner(Class<? extends Record> record) throws RecordException, FieldOrMethodNotFoundException {
        return getOwner(record, false);
    }

    @Override
    public final void setOwner(Record owner) throws RecordException, RecordValidationException, RecordValidationSyntax, FieldOrMethodNotFoundException {
        if (ownersList == null || !ownersList.contains(owner.getClass())) {
            throw new RecordException("Table " + TableNameResolver.getTableName(this.getClass()) + " does not belong to " + TableNameResolver.getTableName(owner.getClass()));
        }
        owner.doValidations(false);
        if (ownerObjects == null) {
            ownerObjects = new HashMap<Class<? extends Record>, Record>();
        }
        ownerObjects.put(owner.getClass(), owner);
        try {
            setValue(this, "fk_" + getFieldName(owner.getClass()), FieldHandler.getValue(FieldHandler.findField(owner.getClass(), "id"), this));
        } catch (IllegalArgumentException e) {
            throw new RecordException(e);
        } catch (FieldOrMethodNotFoundException e) {
            throw new RecordException("Database column name >id< not found in class " + owner.getClass().getCanonicalName());
        }
    }

    @Override
    public final void setOwner(Class<? extends Record> record, int id) throws RecordException, RecordValidationException, RecordValidationSyntax, FieldOrMethodNotFoundException {
        try {
            Record r = record.newInstance();
            if (r instanceof RecordBase) {
                r = ((RecordBase) r).find(id).get(0);
            } else if (r instanceof StaticRecordBase) {
                Method m;
                try {
                    m = r.getClass().getSuperclass().getDeclaredMethod("find", Class.class, Integer[].class);
                    Object tmp = m.invoke(null, r.getClass(), new Object[] { id });
                    r = (Record) tmp;
                } catch (SecurityException e) {
                    throw new RecordException(e);
                } catch (NoSuchMethodException e) {
                    throw new RecordException(e);
                } catch (IllegalArgumentException e) {
                    throw new RecordException(e);
                } catch (InvocationTargetException e) {
                    throw new RecordException(e);
                }
            }
            setOwner(r);
        } catch (InstantiationException e) {
            throw new RecordException(e);
        } catch (IllegalAccessException e) {
            throw new RecordException(e);
        }
    }

    public static final void hasOne(Class<? extends Record> record, TablesDependancies dependacy) {
        if (childList == null) {
            childList = new HashMap<Class<? extends Record>, TablesDependancies>();
        }
        childList.put(record, dependacy);
    }

    public static final void hasOne(Class<? extends Record> record) {
        hasOne(record, TablesDependancies.destroy);
    }

    @SuppressWarnings("unchecked")
    private void getChildPrivate(Class<? extends Record> c) throws RecordException {
        try {
            Record r = c.newInstance();
            int id = 0;
            try {
                id = (Integer) FieldHandler.getValue(FieldHandler.findField(this.getClass(), "id"), this);
            } catch (FieldOrMethodNotFoundException e) {
                throw new RecordException("Database column name >id< not found in class " + this.getClass().getCanonicalName());
            }
            String fkField = "fk_" + getFieldName(this.getClass());
            if (r instanceof RecordBase) {
                r = ((RecordBase) r).find(fkField + " = ?", id).get(0);
            } else if (r instanceof StaticRecordBase) {
                Method m;
                try {
                    m = r.getClass().getSuperclass().getDeclaredMethod("find", Class.class, String.class, Object[].class);
                    Object tmp = m.invoke(null, r.getClass(), fkField + " = ?", new Object[] { id });
                    r = (Record) ((List<Record>) tmp).get(0);
                } catch (Exception e) {
                    throw new RecordException(e);
                }
            }
            if (childObjects == null) {
                childObjects = new HashMap<Class<? extends Record>, Record>();
            }
            childObjects.put(c, r);
        } catch (InstantiationException e) {
            throw new RecordException(e);
        } catch (IllegalAccessException e) {
            throw new RecordException(e);
        }
    }

    @Override
    public final Record getChild(Class<? extends Record> record, boolean forceReload) throws RecordException, FieldOrMethodNotFoundException {
        if (childList == null || !childList.containsKey(record)) {
            throw new RecordException("Table " + TableNameResolver.getTableName(this.getClass()) + " does not belong to " + TableNameResolver.getTableName(record));
        }
        if (childObjects == null) {
            childObjects = new HashMap<Class<? extends Record>, Record>();
        }
        if (forceReload || childObjects.get(record) == null) {
            if (childObjects.get(record) != null) {
                try {
                    setValue(childObjects.get(record), "fk_" + getFieldName(this.getClass()), null);
                    childObjects.get(record).update();
                } catch (IllegalArgumentException e) {
                    throw new RecordException(e);
                } catch (RecordException e) {
                    if (!e.getMessage().equals(Messages.SR_UPDATE_IMPOSSIBLE)) {
                        throw e;
                    }
                }
            }
            getChildPrivate(record);
        }
        return childObjects.get(record);
    }

    @Override
    public final Record getChild(Class<? extends Record> record) throws RecordException, FieldOrMethodNotFoundException {
        return getChild(record, false);
    }

    @Override
    public final void setChild(Record child) throws RecordException, RecordValidationException, RecordValidationSyntax, FieldOrMethodNotFoundException {
        if (childList == null || childList.get(child.getClass()) == null) {
            throw new RecordException("Table " + TableNameResolver.getTableName(this.getClass()) + " does not belong to " + TableNameResolver.getTableName(child.getClass()));
        }
        child.doValidations(false);
        try {
            if (childObjects == null) {
                childObjects = new HashMap<Class<? extends Record>, Record>();
            }
            if (childObjects.get(child.getClass()) != null) {
                setValue(childObjects.get(child.getClass()), "fk_" + getFieldName(this.getClass()), null);
                try {
                    childObjects.get(child.getClass()).update();
                } catch (RecordException e) {
                    if (!e.getMessage().equals(Messages.SR_UPDATE_IMPOSSIBLE)) {
                        throw e;
                    }
                }
            }
            setValue(child, "fk_" + getFieldName(this.getClass()), FieldHandler.getValue(FieldHandler.findField(this.getClass(), "id"), this));
        } catch (IllegalArgumentException e) {
            throw new RecordException(e);
        } catch (FieldOrMethodNotFoundException e) {
            throw new RecordException("Database column name >id< not found in class " + this.getClass().getCanonicalName());
        }
        childObjects.put(child.getClass(), child);
    }

    @Override
    public final void setChild(Class<? extends Record> record, int id) throws RecordException, RecordValidationException, RecordValidationSyntax, FieldOrMethodNotFoundException {
        try {
            Record r = record.newInstance();
            if (r instanceof RecordBase) {
                r = ((RecordBase) r).find(id).get(0);
            } else if (r instanceof StaticRecordBase) {
                Method m;
                try {
                    m = r.getClass().getSuperclass().getDeclaredMethod("find", Class.class, Integer[].class);
                    Object tmp = m.invoke(null, r.getClass(), new Object[] { id });
                    r = (Record) tmp;
                } catch (SecurityException e) {
                    throw new RecordException(e);
                } catch (NoSuchMethodException e) {
                    throw new RecordException(e);
                } catch (IllegalArgumentException e) {
                    throw new RecordException(e);
                } catch (InvocationTargetException e) {
                    throw new RecordException(e);
                }
            }
            setChild(r);
        } catch (InstantiationException e) {
            throw new RecordException(e);
        } catch (IllegalAccessException e) {
            throw new RecordException(e);
        }
    }

    public static final void hasMany(Class<? extends Record> record, TablesDependancies dependacy) {
        if (childrenList == null) {
            childrenList = new HashMap<Class<? extends Record>, TablesDependancies>();
        }
        childrenList.put(record, dependacy);
    }

    public static final void hasMany(Class<? extends Record> record) {
        hasMany(record, TablesDependancies.destroy);
    }

    @SuppressWarnings("unchecked")
    private void getChildrenPrivate(Class<? extends Record> c) throws RecordException {
        if (childrenList != null && childrenList.containsKey(c)) {
            try {
                Record r = c.newInstance();
                List<? extends Record> l = null;
                int id = 0;
                try {
                    id = (Integer) FieldHandler.getValue(FieldHandler.findField(this.getClass(), "id"), this);
                } catch (FieldOrMethodNotFoundException e) {
                    throw new RecordException("Database column name >id< not found in class " + this.getClass().getCanonicalName());
                }
                String fkField = "fk_" + getFieldName(this.getClass());
                if (r instanceof RecordBase) {
                    l = ((RecordBase) r).find(fkField + " = ?", id);
                } else if (r instanceof StaticRecordBase) {
                    Method m;
                    try {
                        m = r.getClass().getSuperclass().getDeclaredMethod("find", Class.class, String.class, Object[].class);
                        Object tmp = m.invoke(null, r.getClass(), fkField + " = ?", new Object[] { id });
                        l = (List<Record>) tmp;
                    } catch (Exception e) {
                        throw new RecordException(e);
                    }
                }
                if (childrenObjects == null) {
                    childrenObjects = new HashMap<Class<? extends Record>, List<? extends Record>>();
                }
                childrenObjects.put(c, l);
            } catch (InstantiationException e) {
                throw new RecordException(e);
            } catch (IllegalAccessException e) {
                throw new RecordException(e);
            }
        } else {
            String relationTableName = TableNameResolver.getRelationshipTableName(this.getClass(), c);
            Vector<Integer> ids = new Vector<Integer>();
            Connection conn = ConnectionManager.getConnection();
            LoggableStatement pStat = null;
            ResultSet rs = null;
            StatementBuilder builder = new StatementBuilder("select fk_" + TableNameResolver.getTableName(c) + " from");
            builder.append(relationTableName);
            Object idThis = null;
            try {
                idThis = FieldHandler.getValue(FieldHandler.findField(this.getClass(), "id"), this);
            } catch (Exception e) {
                throw new RecordException(e);
            }
            builder.append("where fk_" + TableNameResolver.getTableName(this.getClass()) + " = :idThis");
            builder.set(":idThis", idThis);
            pStat = builder.getPreparedStatement(conn);
            log.log(pStat.getQueryString());
            try {
                rs = pStat.executeQuery();
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
            } catch (SQLException e) {
                throw new RecordException(e);
            }
            if (!ids.isEmpty()) {
                try {
                    Record r = c.newInstance();
                    List records = null;
                    if (r instanceof RecordBase) {
                        records = ((RecordBase) r).find(ids.toArray(new Integer[0]));
                    } else if (r instanceof StaticRecordBase) {
                        Method m;
                        try {
                            m = r.getClass().getSuperclass().getDeclaredMethod("find", Class.class, Integer[].class);
                            Object tmp = m.invoke(null, r.getClass(), ids.toArray(new Integer[0]));
                            records = (List<Record>) tmp;
                        } catch (Exception e) {
                            throw new RecordException(e);
                        }
                    }
                    childrenObjects.put(c, records);
                } catch (Exception e) {
                    throw new RecordException(e);
                }
            } else {
                childrenObjects.put(c, null);
            }
        }
    }

    @Override
    public final List<? extends Record> getChildren(Class<? extends Record> record, boolean forceReload) throws RecordException {
        boolean found = false;
        if (childrenList != null && childrenList.containsKey(record)) {
            found = true;
        }
        if (!found && relatedList != null && relatedList.containsKey(record)) {
            found = true;
        }
        if (!found) {
            throw new RecordException("Table " + TableNameResolver.getTableName(this.getClass()) + " has no " + TableNameResolver.getTableName(record) + " children.");
        }
        if (childrenObjects == null) {
            childrenObjects = new HashMap<Class<? extends Record>, List<? extends Record>>();
        }
        if (forceReload || childrenObjects.isEmpty()) {
            if (childrenList != null && childrenList.containsKey(record) && childrenObjects.get(record) != null) {
                for (Record r : childrenObjects.get(record)) {
                    try {
                        setValue(r, "fk_" + getFieldName(this.getClass()), FieldHandler.getValue(FieldHandler.findField(this.getClass(), "id"), this));
                        try {
                            r.update();
                        } catch (RecordException e) {
                            if (!e.getMessage().equals(Messages.SR_UPDATE_IMPOSSIBLE)) {
                                throw e;
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        throw new RecordException(e);
                    } catch (FieldOrMethodNotFoundException e) {
                        throw new RecordException("Database column name >id< not found in class " + this.getClass().getCanonicalName());
                    }
                }
            }
            getChildrenPrivate(record);
        }
        return childrenObjects.get(record);
    }

    @Override
    public final List<? extends Record> getChildren(Class<? extends Record> record) throws RecordException {
        return getChildren(record, false);
    }

    @Override
    public final void removeChild(Record child) throws RecordException, FieldOrMethodNotFoundException {
        if (childrenList == null || !childrenList.containsKey(child.getClass())) {
            throw new RecordException("Table " + TableNameResolver.getTableName(this.getClass()) + " has no " + TableNameResolver.getTableName(child.getClass()) + " children.");
        }
        if (childrenObjects == null) {
            childrenObjects = new HashMap<Class<? extends Record>, List<? extends Record>>();
        }
        List<? extends Record> children = childrenObjects.get(child.getClass());
        children.remove(child);
        setValue(child, "fk_" + getFieldName(this.getClass()), null);
        try {
            child.update();
        } catch (RecordException e) {
            if (!e.getMessage().equals(Messages.SR_UPDATE_IMPOSSIBLE)) {
                throw e;
            }
        }
    }

    @Override
    public final void removeChild(Class<? extends Record> record, int id) throws RecordException, FieldOrMethodNotFoundException {
        try {
            Record r = record.newInstance();
            if (r instanceof RecordBase) {
                r = ((RecordBase) r).find(id).get(0);
            } else if (r instanceof StaticRecordBase) {
                Method m;
                try {
                    m = r.getClass().getSuperclass().getDeclaredMethod("find", Class.class, Integer[].class);
                    Object tmp = m.invoke(null, r.getClass(), new Object[] { id });
                    r = (Record) tmp;
                } catch (SecurityException e) {
                    throw new RecordException(e);
                } catch (NoSuchMethodException e) {
                    throw new RecordException(e);
                } catch (IllegalArgumentException e) {
                    throw new RecordException(e);
                } catch (InvocationTargetException e) {
                    throw new RecordException(e);
                }
            }
            removeChild(r);
        } catch (InstantiationException e) {
            throw new RecordException(e);
        } catch (IllegalAccessException e) {
            throw new RecordException(e);
        }
    }

    private void addRelatedChild(Record a, Record b) throws RecordException {
        String linkTableName = TableNameResolver.getRelationshipTableName(a.getClass(), b.getClass());
        Connection conn = ConnectionManager.getConnection();
        LoggableStatement pStat = null;
        ResultSet rs = null;
        String foreignA = "fk_" + TableNameResolver.getTableName(a.getClass());
        String foreignB = "fk_" + TableNameResolver.getTableName(b.getClass());
        Object fkA = null;
        Object fkB = null;
        try {
            fkA = FieldHandler.getValue(FieldHandler.findField(a.getClass(), "id"), a);
            fkB = FieldHandler.getValue(FieldHandler.findField(b.getClass(), "id"), b);
        } catch (Exception e) {
            throw new RecordException(e);
        }
        StatementBuilder builder = new StatementBuilder("select * from " + linkTableName);
        builder.append("where " + foreignA + "=:" + foreignA + " and " + foreignB + "=:" + foreignB);
        builder.set(":" + foreignA, fkA);
        builder.set(":" + foreignB, fkB);
        pStat = builder.getPreparedStatement(conn);
        log.log(pStat.getQueryString());
        try {
            rs = pStat.executeQuery();
            if (rs.next()) {
                conn.close();
                return;
            }
        } catch (SQLException e) {
            throw new RecordException(e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pStat != null) {
                    pStat.close();
                }
            } catch (SQLException e) {
                throw new RecordException("Error closing connection");
            }
        }
        builder = new StatementBuilder("insert into");
        builder.append(linkTableName);
        builder.append("(" + foreignA + ", " + foreignB + ")");
        builder.append("values (:fkA, :fkB)");
        builder.set(":fkA", fkA);
        builder.set(":fkB", fkB);
        pStat = builder.getPreparedStatement(conn);
        log.log(pStat.getQueryString());
        try {
            pStat.executeUpdate();
        } catch (SQLException e) {
            throw new RecordException("Error adding a child", e);
        } finally {
            try {
                if (pStat != null) {
                    pStat.close();
                }
                conn.commit();
                conn.close();
            } catch (SQLException e) {
                throw new RecordException("Error closing connection");
            }
        }
    }

    @Override
    public final void setChildren(Class<? extends Record> record, List<Record> children) throws RecordException, RecordValidationException, RecordValidationSyntax, FieldOrMethodNotFoundException {
        boolean found = false;
        if (childrenList != null && childrenList.containsKey(record)) {
            found = true;
        }
        if (!found && relatedList != null && relatedList.containsKey(record)) {
            found = true;
        }
        if (!found) {
            throw new RecordException("Table " + TableNameResolver.getTableName(this.getClass()) + " has no " + TableNameResolver.getTableName(record) + " children.");
        }
        for (Record r : children) {
            r.doValidations(false);
        }
        if (childrenObjects == null) {
            childrenObjects = new HashMap<Class<? extends Record>, List<? extends Record>>();
        }
        List<? extends Record> l = childrenObjects.get(record.getClass());
        if (childrenList != null && childrenList.containsKey(record)) {
            try {
                for (Record r : l) {
                    setValue(r, "fk_" + getFieldName(this.getClass()), null);
                    try {
                        r.update();
                    } catch (RecordException e) {
                        if (!e.getMessage().equals(Messages.SR_UPDATE_IMPOSSIBLE)) {
                            throw e;
                        }
                    }
                }
                for (Record r : children) {
                    setValue(r, "fk_" + getFieldName(this.getClass()), FieldHandler.getValue(FieldHandler.findField(this.getClass(), "id"), this));
                }
            } catch (Exception e) {
                throw new RecordException(e);
            }
        } else {
            for (Record r : children) {
                addRelatedChild(this, r);
            }
        }
        childrenObjects.put(record, children);
    }

    @Override
    public final void setChildren(Class<? extends Record> record, Integer... ids) throws RecordException, RecordValidationException, RecordValidationSyntax, FieldOrMethodNotFoundException {
        try {
            List<Record> l = new ArrayList<Record>();
            for (int id : ids) {
                Record r = record.newInstance();
                if (r instanceof RecordBase) {
                    r = ((RecordBase) r).find(id).get(0);
                    l.add(r);
                } else if (r instanceof StaticRecordBase) {
                    Method m;
                    try {
                        m = r.getClass().getSuperclass().getDeclaredMethod("find", Class.class, Integer[].class);
                        Object tmp = m.invoke(null, r.getClass(), new Object[] { id });
                        r = (Record) tmp;
                        l.add(r);
                    } catch (SecurityException e) {
                        throw new RecordException(e);
                    } catch (NoSuchMethodException e) {
                        throw new RecordException(e);
                    } catch (IllegalArgumentException e) {
                        throw new RecordException(e);
                    } catch (InvocationTargetException e) {
                        throw new RecordException(e);
                    }
                }
            }
            setChildren(record, l);
        } catch (InstantiationException e) {
            throw new RecordException(e);
        } catch (IllegalAccessException e) {
            throw new RecordException(e);
        }
    }

    @Override
    public final void appendChild(Record child) throws RecordException, RecordValidationException, RecordValidationSyntax, FieldOrMethodNotFoundException {
        child.doValidations(false);
        boolean found = false;
        if (childrenList != null && childrenList.containsKey(child.getClass())) {
            found = true;
        }
        if (!found && relatedList != null && relatedList.containsKey(child.getClass())) {
            found = true;
        }
        if (!found) {
            throw new RecordException("Table " + TableNameResolver.getTableName(this.getClass()) + " has no " + TableNameResolver.getTableName(child.getClass()) + " children.");
        }
        if (childrenList != null && childrenList.containsKey(child.getClass())) {
            try {
                setValue(child, "fk_" + getFieldName(this.getClass()), FieldHandler.getValue(FieldHandler.findField(this.getClass(), "id"), this));
            } catch (IllegalArgumentException e) {
                throw new RecordException(e);
            } catch (FieldOrMethodNotFoundException e) {
                throw new RecordException("Database column name >id< not found in class " + this.getClass().getCanonicalName());
            }
            if (childrenObjects == null) {
                childrenObjects = new HashMap<Class<? extends Record>, List<? extends Record>>();
            }
            List l = childrenObjects.get(child.getClass());
            l.add(child);
        } else {
            addRelatedChild(this, child);
            if (relatedObjects == null) {
                relatedObjects = new HashMap<Class<? extends Record>, List<? extends Record>>();
            }
            List l = relatedObjects.get(child.getClass());
            if (l == null) {
                l = new ArrayList();
            }
            l.add(child);
        }
    }

    @Override
    public final void appendChild(Class<? extends Record> record, int id) throws RecordException, RecordValidationException, RecordValidationSyntax, FieldOrMethodNotFoundException {
        try {
            Record r = record.newInstance();
            if (r instanceof RecordBase) {
                r = ((RecordBase) r).find(id).get(0);
            } else if (r instanceof StaticRecordBase) {
                Method m;
                try {
                    m = r.getClass().getSuperclass().getDeclaredMethod("find", Class.class, Integer[].class);
                    Object tmp = m.invoke(null, r.getClass(), new Object[] { id });
                    r = (Record) tmp;
                } catch (SecurityException e) {
                    throw new RecordException(e);
                } catch (NoSuchMethodException e) {
                    throw new RecordException(e);
                } catch (IllegalArgumentException e) {
                    throw new RecordException(e);
                } catch (InvocationTargetException e) {
                    throw new RecordException(e);
                }
            }
            appendChild(r);
        } catch (InstantiationException e) {
            throw new RecordException(e);
        } catch (IllegalAccessException e) {
            throw new RecordException(e);
        }
    }

    public static final void hasAndBelongsToMany(Class<? extends Record> record) throws RecordException {
        hasAndBelongsToMany(record, TablesDependancies.destroy);
    }

    public static final void hasAndBelongsToMany(Class<? extends Record> record, TablesDependancies tablesDependancies) throws RecordException {
        if (relatedList == null) {
            relatedList = new HashMap<Class<? extends Record>, TablesDependancies>();
        }
        relatedList.put(record, tablesDependancies);
    }

    @Override
    public void getAllDependancies() throws RecordException {
        if (ownersList != null) {
            for (Class<? extends Record> c : ownersList) {
                getOwnerPrivate(c);
            }
        }
        if (childList != null) {
            for (Class<? extends Record> c : childList.keySet()) {
                getChildPrivate(c);
            }
        }
        if (childrenList != null) {
            for (Class<? extends Record> c : childrenList.keySet()) {
                getChildrenPrivate(c);
            }
        }
    }

    @Override
    public void refresh() throws RecordException {
        refresh(false);
    }

    @Override
    public void refresh(boolean cascade) throws RecordException {
        Connection conn = ConnectionManager.getConnection();
        LoggableStatement pStat = null;
        Class<? extends Record> actualClass = this.getClass();
        String tableName = TableNameResolver.getTableName(actualClass);
        String sql = "select * from " + tableName + " where id = :id";
        StatementBuilder builder = new StatementBuilder(sql);
        try {
            Field f = FieldHandler.findField(this.getClass(), "id");
            builder.set("id", FieldHandler.getValue(f, this));
        } catch (Exception e) {
            throw new RecordException(e);
        }
        try {
            pStat = builder.getPreparedStatement(conn);
            log.log(pStat.getQueryString());
            ResultSet rs = pStat.executeQuery();
            ResultSetMetaData rsMeta = rs.getMetaData();
            if (rs.next()) {
                for (int i = 1; i <= rsMeta.getColumnCount(); ++i) {
                    setValue(this, rsMeta.getColumnLabel(i), rs.getObject(i));
                }
            } else {
                Field f = FieldHandler.findField(this.getClass(), "id");
                throw new RecordException("Cannot find element with ID=" + FieldHandler.getValue(f, this) + " in the database");
            }
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                throw new RecordException("Error executing rollback");
            }
            throw new RecordException(e);
        } finally {
            try {
                if (pStat != null) {
                    pStat.close();
                }
                conn.commit();
                conn.close();
            } catch (SQLException e) {
                throw new RecordException("Error closing connection");
            }
        }
        if (cascade) {
            if (childObjects != null) {
                for (Class<? extends Record> key : childObjects.keySet()) {
                    if (childObjects.get(key) != null) {
                        childObjects.get(key).refresh(cascade);
                    }
                }
            }
            if (childrenObjects != null) {
                for (Class<? extends Record> key : childrenObjects.keySet()) {
                    if (childrenObjects.get(key) != null) {
                        for (Record r : childrenObjects.get(key)) {
                            r.refresh(cascade);
                        }
                    }
                }
            }
            if (relatedObjects != null) {
                for (Class<? extends Record> key : relatedObjects.keySet()) {
                    if (relatedObjects.get(key) != null) {
                        for (Record r : relatedObjects.get(key)) {
                            r.refresh(cascade);
                        }
                    }
                }
            }
        }
    }

    @Override
    public int updateAttribute(String attribute, Object newValue) throws RecordException, RecordValidationException, RecordValidationSyntax, FieldOrMethodNotFoundException {
        Connection conn = ConnectionManager.getConnection();
        LoggableStatement pStat = null;
        Class<? extends Record> actualClass = this.getClass();
        String tableName = TableNameResolver.getTableName(actualClass);
        Field id = null;
        Field toChange = null;
        try {
            id = FieldHandler.findField(this.getClass(), "id");
            toChange = FieldHandler.findField(this.getClass(), attribute);
            toChange.set(this, newValue);
        } catch (Exception e) {
            throw new RecordException(e);
        }
        doValidation(toChange, true);
        String sql = "update " + tableName + " set " + attribute + " = :" + attribute + " where id = :id";
        StatementBuilder builder = new StatementBuilder(sql);
        try {
            builder.set(":id", FieldHandler.getValue(id, this));
            builder.set(":" + attribute, FieldHandler.getValue(toChange, this));
            pStat = builder.getPreparedStatement(conn);
            log.log(pStat.getQueryString());
            int res = pStat.executeUpdate();
            return res;
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                throw new RecordException("Error executing rollback");
            }
            throw new RecordException(e);
        } finally {
            try {
                if (pStat != null) {
                    pStat.close();
                }
                conn.commit();
                conn.close();
            } catch (SQLException e) {
                throw new RecordException("Error closing connection");
            }
        }
    }

    private RecordValidationException generateValidationException(String defaultMessage, String message, String fieldName, Object value, Annotation annotation) {
        RecordValidationException ex = null;
        if (message.equals("")) {
            ex = new RecordValidationException(defaultMessage);
        } else {
            ex = new RecordValidationException(message);
        }
        ex.setFieldName(fieldName);
        ex.setFieldValue(value);
        ex.setAnnotation(annotation);
        return ex;
    }

    @Override
    public final void doValidations(boolean update) throws RecordValidationException, RecordValidationSyntax, RecordException, FieldOrMethodNotFoundException {
        if (ignoreValidations) {
            return;
        }
        Class<? extends Record> actualClass = this.getClass();
        Field fields[] = actualClass.getDeclaredFields();
        for (Field field : fields) {
            doValidation(field, update);
        }
    }

    public final void doValidation(Field field, boolean update) throws RecordValidationException, RecordValidationSyntax, RecordException, FieldOrMethodNotFoundException {
        if (ignoreValidations) {
            return;
        }
        Class<? extends Record> actualClass = this.getClass();
        Object value = FieldHandler.getValue(field, this);
        if (field.isAnnotationPresent(ValidatePresenceOf.class) && value == null) {
            if (value == null) {
                ValidatePresenceOf annotation = (ValidatePresenceOf) field.getAnnotation(ValidatePresenceOf.class);
                throw generateValidationException("The field " + field.getName() + " is mandatory", annotation.message(), field.getName(), value, annotation);
            }
        }
        if (field.isAnnotationPresent(ValidateNumericalityOf.class)) {
            ValidateNumericalityOf annotation = (ValidateNumericalityOf) field.getAnnotation(ValidateNumericalityOf.class);
            if (value == null && !annotation.allowNull()) {
                throw generateValidationException("The field " + field.getName() + " is not numerical", annotation.message(), field.getName(), value, annotation);
            }
            if (value != null) {
                try {
                    String type = annotation.type();
                    if (type.equalsIgnoreCase("integer")) {
                        Integer.parseInt(value.toString());
                    } else if (type.equalsIgnoreCase("float")) {
                        Float.parseFloat(value.toString());
                    } else if (type.equalsIgnoreCase("double")) {
                        Double.parseDouble(value.toString());
                    } else if (type.equalsIgnoreCase("bigDecimal")) {
                        new BigDecimal(value.toString());
                    } else {
                        throw new RecordValidationSyntax("Type " + annotation.type() + " is invalid in ValidateNumericalityOf");
                    }
                } catch (NumberFormatException e) {
                    throw generateValidationException("The field " + field.getName() + " is not numerical (" + annotation.type() + ")", annotation.message(), field.getName(), value, annotation);
                }
            }
        }
        if (field.isAnnotationPresent(ValidateExclusionOf.class)) {
            ValidateExclusionOf annotation = (ValidateExclusionOf) field.getAnnotation(ValidateExclusionOf.class);
            if (value == null && !annotation.allowNull()) {
                throw generateValidationException("Value not permitted for field " + field.getName(), annotation.message(), field.getName(), value, annotation);
            }
            if (value != null) {
                String list = annotation.list();
                StringTokenizer st = new StringTokenizer(list);
                while (st.hasMoreTokens()) {
                    if (value.equals(st.nextToken())) {
                        throw generateValidationException("Value not permitted for field " + field.getName(), annotation.message(), field.getName(), value, annotation);
                    }
                }
            }
        }
        if (field.isAnnotationPresent(ValidateInclusionOf.class)) {
            ValidateInclusionOf annotation = (ValidateInclusionOf) field.getAnnotation(ValidateInclusionOf.class);
            if (value == null && !annotation.allowNull()) {
                throw generateValidationException("Value not permitted for field " + field.getName(), annotation.message(), field.getName(), value, annotation);
            }
            if (value != null) {
                String list = annotation.list();
                StringTokenizer st = new StringTokenizer(list);
                boolean found = false;
                while (st.hasMoreTokens()) {
                    if (value.equals(st.nextToken())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw generateValidationException("Value not permitted for field " + field.getName(), annotation.message(), field.getName(), value, annotation);
                }
            }
        }
        if (field.isAnnotationPresent(ValidateLengthOf.class)) {
            ValidateLengthOf annotation = (ValidateLengthOf) field.getAnnotation(ValidateLengthOf.class);
            int min = annotation.min();
            int max = annotation.max();
            if (min > max) {
                throw new RecordValidationSyntax("min value greater than max value for field " + field.getName());
            }
            if (value == null && min > 0) {
                throw generateValidationException("field " + field.getName() + " is too short (0)", annotation.minMessage(), field.getName(), value, annotation);
            }
            String s = (String) value;
            if (s.length() < min) {
                throw generateValidationException("field " + field.getName() + " is too short (" + s.length() + ")", annotation.minMessage(), field.getName(), value, annotation);
            }
            if (s.length() > max) {
                throw generateValidationException("field " + field.getName() + " is too long (" + s.length() + ")", annotation.maxMessage(), field.getName(), value, annotation);
            }
        }
        if (field.isAnnotationPresent(ValidateFormatOf.class)) {
            ValidateFormatOf annotation = field.getAnnotation(ValidateFormatOf.class);
            if (value == null && !annotation.allowNull()) {
                throw generateValidationException("Format of field " + field.getName() + " is invalid.", annotation.message(), field.getName(), value, annotation);
            }
            if (value != null) {
                Pattern p = Pattern.compile(annotation.regexp());
                Matcher m = p.matcher(value.toString());
                if (!m.matches()) {
                    throw generateValidationException("Format of field " + field.getName() + " is invalid.", annotation.message(), field.getName(), value, annotation);
                }
            }
        }
        if (field.isAnnotationPresent(ValidateUniquenessOf.class)) {
            ValidateUniquenessOf annotation = (ValidateUniquenessOf) field.getAnnotation(ValidateUniquenessOf.class);
            if (value == null && !annotation.allowNull()) {
                throw generateValidationException("Field " + field.getName() + " is not unique", annotation.message(), field.getName(), value, annotation);
            }
            if (value != null) {
                String tableName = TableNameResolver.getTableName(actualClass);
                StatementBuilder builder = new StatementBuilder("select id from " + tableName);
                builder.append("where " + field.getName() + " = :" + field.getName());
                builder.set(":" + field.getName(), value);
                Connection conn = ConnectionManager.getConnection();
                LoggableStatement pStat = builder.getPreparedStatement(conn);
                log.log(pStat.getQueryString());
                RecordValidationException ex = null;
                try {
                    ResultSet rs = pStat.executeQuery();
                    int id = 0;
                    int i = 0;
                    while (rs.next()) {
                        i++;
                        id = rs.getInt(1);
                    }
                    if (i > 1) {
                        ex = generateValidationException("Field " + field.getName() + " is not unique", annotation.message(), field.getName(), value, annotation);
                    } else if (i == 1 && !update) {
                        ex = generateValidationException("Field " + field.getName() + " is not unique", annotation.message(), field.getName(), value, annotation);
                    } else if (i == 1 && update) {
                        Field idField = null;
                        try {
                            idField = this.getClass().getDeclaredField("id");
                            idField.setAccessible(true);
                        } catch (Exception e) {
                            throw new RecordException("Class " + actualClass.getCanonicalName() + " has no field id");
                        }
                        try {
                            if (id != idField.getInt(this)) {
                                ex = generateValidationException("Field " + field.getName() + " is not unique", annotation.message(), field.getName(), value, annotation);
                            }
                        } catch (Exception e) {
                        }
                    }
                    if (ex != null) {
                        throw ex;
                    }
                } catch (SQLException e) {
                    throw new RecordException("Error calculating uniqueness of field " + field.getName(), e);
                } finally {
                    try {
                        if (pStat != null) {
                            pStat.close();
                        }
                        conn.close();
                    } catch (SQLException e) {
                        throw new RecordException("Error closing connection");
                    }
                }
            }
        }
    }

    @Override
    public void setIgnoreValidations(boolean ignore) {
        ignoreValidations = ignore;
    }
}
