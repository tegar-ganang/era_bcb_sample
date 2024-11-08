package it.simplerecords.base;

import it.simplerecords.connection.ConnectionManager;
import it.simplerecords.exceptions.RecordException;
import it.simplerecords.util.LoggableStatement;
import it.simplerecords.util.StatementBuilder;
import it.simplerecords.util.TableNameResolver;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

public class RecordBase extends RecordAbstract {

    /**
	 * Deletes the current object representation from the DB or a list of rows by IDs
	 * 
	 * @param attributes
	 *           The list of attributes to refine the query (group by, where...)
	 * @param ids
	 *           The list of IDs to delete. If none is given, all the table is deleted
	 * @return True if the row was successfully deleted
	 * @throws RecordException
	 *            On any Exception raised inside the method
	 */
    public final int deleteAll(String attributes, Integer... ids) throws RecordException {
        Connection conn = ConnectionManager.getConnection();
        LoggableStatement pStat = null;
        Class<? extends Record> actualClass = this.getClass();
        StatementBuilder builder = null;
        if (!attributes.startsWith("{")) {
            attributes = "{" + attributes;
        }
        if (!attributes.endsWith("}")) {
            attributes += "}";
        }
        try {
            JSONObject parsed = new JSONObject(attributes);
            if (ids.length == 0) {
                builder = new StatementBuilder("delete from " + TableNameResolver.getTableName(actualClass));
            } else {
                builder = new StatementBuilder("delete from " + TableNameResolver.getTableName(actualClass) + " where id in (");
                for (int i = 0; i < ids.length; ++i) {
                    builder.append(":id" + i);
                    if (i != ids.length - 1) {
                        builder.append(", ");
                    }
                    builder.set(":id" + i, ids[i]);
                }
                builder.append(")");
            }
            addConditions(builder, parsed, ids.length == 0);
            pStat = builder.getPreparedStatement(conn);
            log.log(pStat.getQueryString());
            int i = pStat.executeUpdate();
            return i;
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
	 * Deletes the current object representation from the DB or a list of rows by IDs
	 * 
	 * @param ids
	 *           The list of IDs to delete. If none is given, all the table is deleted
	 * @return True if the row was successfully deleted
	 * @throws RecordException
	 *            On any Exception raised inside the method
	 */
    public final int deleteAll(Integer... ids) throws RecordException {
        return deleteAll("", ids);
    }

    /**
	 * Deletes all the rows, thus emptying the table.
	 * 
	 * @throws RecordException
	 *            On any Exception raised inside the method
	 */
    public final void deleteAll() throws RecordException {
        Connection conn = ConnectionManager.getConnection();
        LoggableStatement pStat = null;
        Class<? extends Record> actualClass = this.getClass();
        try {
            StatementBuilder builder = new StatementBuilder("delete from " + TableNameResolver.getTableName(actualClass));
            pStat = builder.getPreparedStatement(conn);
            log.log(pStat.getQueryString());
            pStat.executeUpdate();
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

    public final boolean exists(Integer id) throws RecordException {
        Connection conn = ConnectionManager.getConnection();
        LoggableStatement pStat = null;
        Class<? extends RecordBase> actualClass = this.getClass();
        String tableName = TableNameResolver.getTableName(actualClass);
        String sql = "select * from " + tableName + " where id = :id";
        StatementBuilder builder = new StatementBuilder(sql);
        builder.set("id", id);
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

    public List<Record> findBySQL(String sql) throws RecordException {
        List<Record> records = null;
        LoggableStatement pStat = null;
        Connection conn = ConnectionManager.getConnection();
        Class<? extends RecordBase> actualClass = this.getClass();
        try {
            pStat = new LoggableStatement(conn, sql);
            log.log(pStat.getQueryString());
            ResultSet rs = pStat.executeQuery();
            records = new ArrayList<Record>();
            ResultSetMetaData rsMeta = rs.getMetaData();
            while (rs.next()) {
                RecordBase b = actualClass.cast(actualClass.newInstance());
                for (int i = 1; i <= rsMeta.getColumnCount(); ++i) {
                    setValue(b, rsMeta.getColumnLabel(i), rs.getObject(i));
                }
                records.add(b);
            }
            rs.close();
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                throw new RecordException("Error executing rollback");
            }
            throw new RecordException("Error getting table data", e);
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
        return records;
    }

    public final List<? extends Record> find(String attributes, Integer... ids) throws RecordException {
        List<Record> records = null;
        String sql = null;
        LoggableStatement pStat = null;
        Connection conn = ConnectionManager.getConnection();
        Class<? extends RecordBase> actualClass = this.getClass();
        String tableName = TableNameResolver.getTableName(actualClass);
        JSONObject parsed = null;
        if (!attributes.startsWith("{")) {
            attributes = "{" + attributes;
        }
        if (!attributes.endsWith("}")) {
            attributes += "}";
        }
        try {
            parsed = new JSONObject(attributes);
        } catch (JSONException e) {
            throw new RecordException("Error parsing attributes", e);
        }
        if (parsed.has("select")) {
            try {
                JSONArray values = parsed.getJSONArray("select");
                if (values.length() > 0) {
                    sql = "select ";
                    for (int i = 0; i < values.length(); ++i) {
                        sql += values.getString(i);
                        if (i != values.length() - 1) {
                            sql += ", ";
                        }
                    }
                    sql += " from " + tableName + " ";
                }
            } catch (JSONException e) {
                try {
                    String select = parsed.getString("select");
                    sql = "select " + select;
                    sql += " from " + tableName + " ";
                } catch (JSONException e1) {
                    throw new RecordException("Error parsing 'select' attribute.", e1);
                }
            }
        } else {
            sql = "select * from " + tableName + " ";
        }
        StatementBuilder builder = new StatementBuilder(sql);
        if (ids.length == 1) {
            builder.append("where id = :id");
            builder.set("id", ids[0]);
        } else if (ids.length > 1) {
            String sqlList = "";
            for (Integer i : ids) {
                sqlList += ":id" + i + ", ";
                builder.set(":id" + i, i);
            }
            sqlList = sqlList.substring(0, sqlList.length() - 2);
            builder.append("where id in (" + sqlList + ")");
        }
        addConditions(builder, parsed, ids.length == 0);
        try {
            records = new ArrayList<Record>();
            pStat = builder.getPreparedStatement(conn);
            log.log(pStat.getQueryString());
            ResultSet rs = pStat.executeQuery();
            ResultSetMetaData rsMeta = rs.getMetaData();
            if (!parsed.has("method") || parsed.getString("method").equalsIgnoreCase("all")) {
                while (rs.next()) {
                    RecordBase b = actualClass.cast(actualClass.newInstance());
                    for (int i = 1; i <= rsMeta.getColumnCount(); ++i) {
                        setValue(b, rsMeta.getColumnLabel(i), rs.getObject(i));
                    }
                    records.add(b);
                }
            } else if (parsed.getString("method").equalsIgnoreCase("first")) {
                if (rs.first()) {
                    RecordBase b = actualClass.cast(actualClass.newInstance());
                    for (int i = 1; i <= rsMeta.getColumnCount(); ++i) {
                        setValue(b, rsMeta.getColumnLabel(i), rs.getObject(i));
                    }
                    records.add(b);
                }
            } else if (parsed.getString("method").equalsIgnoreCase("last")) {
                if (rs.last()) {
                    RecordBase b = actualClass.cast(actualClass.newInstance());
                    for (int i = 1; i <= rsMeta.getColumnCount(); ++i) {
                        setValue(b, rsMeta.getColumnLabel(i), rs.getObject(i));
                    }
                    records.add(b);
                }
            }
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                throw new RecordException("Error executing rollback");
            }
            throw new RecordException("Error getting table data", e);
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
        this.freeze();
        return records;
    }

    public List<? extends Record> find(Integer... ids) throws RecordException {
        return find("{}", ids);
    }

    /**
	 * Method to retreive the first element of a search in a table
	 * 
	 * @param attributes
	 *           The optional attributes for the search
	 * @param ids
	 *           The optional list of id to search for
	 * @return The first Record returned by the search (null if none is found)
	 * @throws RecordException
	 *            If an error occurs
	 */
    public Record first(String attributes, Integer... ids) throws RecordException {
        try {
            if (!attributes.startsWith("{")) {
                attributes = "{" + attributes;
            }
            if (!attributes.endsWith("}")) {
                attributes += "}";
            }
            JSONObject parsed = new JSONObject(attributes);
            parsed.put("method", "first");
            List<? extends Record> l = find(parsed.toString(), ids);
            if (l.isEmpty()) {
                return null;
            } else {
                return l.get(0);
            }
        } catch (JSONException e) {
            throw new RecordException("Error parsing parameters", e);
        }
    }

    /**
	 * Method to retreive the first element of a search in a table
	 * 
	 * @param ids
	 *           The optional list of id to search for
	 * @return The first Record returned by the search (null if none is found)
	 * @throws RecordException
	 *            If an error occurs
	 */
    public Record last(Integer... ids) throws RecordException {
        return last("{method : last}", ids);
    }

    /**
	 * Method to retreive the last element of a search in a table
	 * 
	 * @param attributes
	 *           The optional attributes for the search
	 * @param ids
	 *           The optional list of id to search for
	 * @return The last Record returned by the search (null if none is found)
	 * @throws RecordException
	 *            If an error occurs
	 */
    public Record last(String attributes, Integer... ids) throws RecordException {
        try {
            if (!attributes.startsWith("{")) {
                attributes = "{" + attributes;
            }
            if (!attributes.endsWith("}")) {
                attributes += "}";
            }
            JSONObject parsed = new JSONObject(attributes);
            parsed.put("method", "last");
            List<? extends Record> l = find(parsed.toString(), ids);
            if (l.isEmpty()) {
                return null;
            } else {
                return l.get(0);
            }
        } catch (JSONException e) {
            throw new RecordException("Error parsing parameters", e);
        }
    }

    /**
	 * Method to retreive the last element of a search in a table
	 * 
	 * @param ids
	 *           The optional list of id to search for
	 * @return The last Record returned by the search (null if none is found)
	 * @throws RecordException
	 *            If an error occurs
	 */
    public Record first(Integer... ids) throws RecordException {
        List<? extends Record> l = find("{method : first}", ids);
        return l.get(0);
    }

    public int updateBySql(String sql) throws RecordException {
        LoggableStatement pStat = null;
        Connection conn = ConnectionManager.getConnection();
        try {
            pStat = new LoggableStatement(conn, sql);
            log.log(pStat.getQueryString());
            int res = pStat.executeUpdate();
            return res;
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                throw new RecordException("Error executing rollback");
            }
            throw new RecordException("Error getting table data", e);
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

    public final Record create() {
        return null;
    }
}
