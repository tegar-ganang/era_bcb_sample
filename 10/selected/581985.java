package com.rapidlogix.monitor.dao;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.type.Type;
import com.rapidlogix.monitor.config.ConfigManager;
import com.rapidlogix.monitor.config.ScopeConfig;
import com.rapidlogix.monitor.config.ThresholdLevelConfig;
import com.rapidlogix.monitor.config.ChartConfig;
import com.rapidlogix.monitor.model.RlxTransaction;
import com.rapidlogix.monitor.model.RlxVariable;
import com.rapidlogix.monitor.model.VariableIntervals;
import com.rapidlogix.monitor.stats.ThresholdManager;
import com.rapidlogix.monitor.util.AppUtil;

public class StorageDao extends Dao {

    public void cleanup(Date variableCleanupDate, Date transactionCleanupDate) throws Exception {
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            String variableCleanupDateStr = sdf.format(variableCleanupDate);
            String transactionCleanupDateStr = sdf.format(transactionCleanupDate);
            Query q = session.createQuery("delete from RlxTransaction where timestamp < '" + transactionCleanupDateStr + "'");
            q.executeUpdate();
            q = session.createQuery("delete from RlxOperation where timestamp < '" + transactionCleanupDateStr + "'");
            q.executeUpdate();
            q = session.createSQLQuery("delete from transaction_param where transaction_ref_id not in (select distinct t.ref_id from transaction t)");
            q.executeUpdate();
            q = session.createSQLQuery("delete from operation_param where operation_ref_id not in (select distinct o.ref_id from operation o)");
            q.executeUpdate();
            q = session.createQuery("delete from RlxVariableSnapshot where timestamp < '" + variableCleanupDateStr + "'");
            q.executeUpdate();
            q = session.createQuery("delete from RlxVariable v where v.id not in (select distinct vs.variableId from RlxVariableSnapshot vs)");
            q.executeUpdate();
            tx.commit();
        } catch (Exception ex) {
            if (tx != null) {
                tx.rollback();
            }
            throw ex;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public void createStorageSchema(String type) throws Exception {
        Session session = null;
        Transaction tx = null;
        try {
            String schema = "/storage-" + type + ".sql";
            session = sessionFactory.openSession();
            SQLQuery sqlQuery = session.createSQLQuery(AppUtil.readClasspathFile(schema));
            sqlQuery.executeUpdate();
        } catch (Exception ex) {
            if (tx != null) {
                tx.rollback();
            }
            throw ex;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public void saveList(ArrayList list) throws Exception {
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            int i = 0;
            for (Object obj : list) {
                session.save(obj);
                if (++i % 250 == 0) {
                    session.flush();
                    session.clear();
                }
            }
            tx.commit();
        } catch (Exception ex) {
            if (tx != null) {
                tx.rollback();
            }
            throw ex;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public List getTransactions(int count) throws Exception {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query query = session.createQuery("from RlxTransaction r order by r.timestamp desc");
            query.setMaxResults(count);
            List list = query.list();
            Hibernate.initialize(list);
            return list;
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public List getTransactions(String host, String scope, Date startDate, Date endDate, int limit) throws Exception {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            StringBuffer queryBuf = new StringBuffer();
            queryBuf.append("from RlxTransaction r where ");
            queryBuf.append("r.host = ?");
            if (scope != null) {
                queryBuf.append("and r.scope = ?");
            }
            if (startDate != null && endDate != null) {
                queryBuf.append(" and r.timestamp >= ? and r.timestamp <= ?");
            }
            queryBuf.append(" order by r.timestamp desc");
            Query query = session.createQuery(queryBuf.toString());
            int i = 0;
            query.setString(i++, host);
            if (scope != null) {
                query.setString(i++, scope);
            }
            if (startDate != null && endDate != null) {
                query.setTimestamp(i++, startDate);
                query.setTimestamp(i++, endDate);
            }
            query.setMaxResults(limit);
            List list = query.list();
            Hibernate.initialize(list);
            return list;
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public List getTransactions(String host, String scope, int count) throws Exception {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query query = session.createQuery("from RlxTransaction r where r.host = ? and r.scope = ? order by r.timestamp desc");
            query.setString(0, host);
            query.setString(1, scope);
            query.setMaxResults(count);
            List list = query.list();
            Hibernate.initialize(list);
            return list;
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public RlxTransaction getTransaction(long refId) throws Exception {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query query = session.createQuery("from RlxTransaction where ref_id=?");
            query.setLong(0, refId);
            query.list();
            List list = query.list();
            if (list != null && !list.isEmpty()) {
                Hibernate.initialize(list);
                return (RlxTransaction) list.get(0);
            } else {
                return null;
            }
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public List getOperations(long transactionRefId) throws Exception {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query query = session.createQuery("from RlxOperation where transactionRefId = ? order by refId");
            query.setLong(0, transactionRefId);
            List list = query.list();
            Hibernate.initialize(list);
            return list;
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public List getTransactionParams(long transactionRefId) throws Exception {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query query = session.createQuery("from RlxTransactionParam where transactionRefId = ? order by refId");
            query.setLong(0, transactionRefId);
            List list = query.list();
            Hibernate.initialize(list);
            return list;
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public List getOperationParams(long operationRefId) throws Exception {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query query = session.createQuery("from RlxOperationParam where operationRefId = ? order by refId");
            query.setLong(0, operationRefId);
            List list = query.list();
            Hibernate.initialize(list);
            return list;
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public long getVariableId(String name, String host, String scope, short unit, short interval, int order) throws Exception {
        Session session = null;
        Transaction tx = null;
        try {
            RlxVariable variable = null;
            session = sessionFactory.openSession();
            Query query = session.createQuery("from RlxVariable v where v.name = ? and host = ?" + (scope != null ? " and v.scope = ?" : ""));
            query.setString(0, name);
            query.setString(1, host);
            if (scope != null) {
                query.setString(2, scope);
            }
            List list = query.list();
            if (list != null && !list.isEmpty()) {
                variable = (RlxVariable) list.get(0);
            } else {
                variable = new RlxVariable();
                variable.setName(name);
                variable.setHost(host);
                variable.setScope(scope);
                variable.setUnit(unit);
                variable.setInterval(interval);
                variable.setOrder(order);
                tx = session.beginTransaction();
                session.save(variable);
                tx.commit();
            }
            return variable.getId();
        } catch (Exception ex) {
            if (tx != null) {
                tx.rollback();
            }
            throw ex;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public RlxVariable getVariable(String host, String scope, String name) throws Exception {
        Session session = null;
        try {
            RlxVariable variable = null;
            session = sessionFactory.openSession();
            Query query = session.createQuery("from RlxVariable v where v.name = ? and v.host = ?" + (scope != null ? " and v.scope = ?" : ""));
            query.setString(0, name);
            query.setString(1, host);
            if (scope != null) {
                query.setString(2, scope);
            }
            List list = query.list();
            if (list != null && !list.isEmpty()) {
                return (RlxVariable) list.get(0);
            } else {
                return null;
            }
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public void cleanupVariableSnapshotLast(String host) throws Exception {
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            Query q = session.createQuery("delete from RlxVariableSnapshotLast vsl where vsl.variableId in (select distinct v.id from RlxVariable v where v.host = ?)");
            q.setString(0, host);
            q.executeUpdate();
            tx.commit();
        } catch (Exception ex) {
            if (tx != null) {
                tx.rollback();
            }
            throw ex;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public RlxVariable getVariable(long id) throws Exception {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            RlxVariable variable = (RlxVariable) session.load(RlxVariable.class, id);
            Hibernate.initialize(variable);
            return variable;
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public List getVariableNames() throws Exception {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query query = session.createQuery("select distinct v.name from RlxVariable v ");
            List list = query.list();
            Hibernate.initialize(list);
            return list;
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public List getHosts() throws Exception {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query query = session.createQuery("select distinct v.host from RlxVariable v order by v.host");
            List list = query.list();
            return list;
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public List getAllVariables() throws Exception {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query query = session.createQuery("from RlxVariable v");
            List list = query.list();
            Hibernate.initialize(list);
            return list;
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public List getVariableList(String host, String scopePattern, String name) throws Exception {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query query = session.createQuery("from RlxVariable v where v.host = ? and v.scope like ? and v.name = ? order by v.scope, v.name");
            query.setString(0, host);
            query.setString(1, scopePattern);
            query.setString(2, name);
            List list = query.list();
            Hibernate.initialize(list);
            return list;
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public List getScopeVariables(String host, String scopePattern, Date startDate, Date endDate, int limit) throws Exception {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query query = session.createQuery("from RlxVariable v where v.host = ? and v.scope like ? and (select count(*) from RlxVariableSnapshot vs where vs.timestamp >= ? and where vs.timestamp <= ?) > 0 order by v.scope,v.order");
            query.setString(0, host);
            query.setString(1, scopePattern);
            query.setMaxResults(limit);
            List list = query.list();
            Hibernate.initialize(list);
            return list;
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public List getVariableSnapshots(long variableId, Date startDate, Date endDate) throws Exception {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query query = session.createQuery("from RlxVariableSnapshot vs where vs.variableId = ? and vs.timestamp >= ? and vs.timestamp <= ? order by vs.timestamp desc");
            query.setLong(0, variableId);
            query.setTimestamp(1, startDate);
            query.setTimestamp(2, endDate);
            List list = query.list();
            Hibernate.initialize(list);
            return list;
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public List getVariableSnapshotMap(String scopePattern, Date startDate, Date endDate) throws Exception {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query query = session.createQuery("from RlxVariable v, RlxVariableSnapshot vs where v.id = vs.variableId and v.scope like ? and vs.timestamp >= ? and vs.timestamp <= ?");
            query.setString(0, scopePattern);
            query.setTimestamp(1, startDate);
            query.setTimestamp(2, endDate);
            List list = query.list();
            Hibernate.initialize(list);
            return list;
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public Double getAggregatedValue(long variableId, Date startDate, String function) throws Exception {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query query = session.createQuery("select " + function + "(value) from RlxVariableSnapshot vs where vs.variableId = ? and vs.timestamp > ?");
            query.setLong(0, variableId);
            query.setTimestamp(1, startDate);
            List list = query.list();
            Object obj = list.get(0);
            if (obj == null) {
                return null;
            } else {
                return getDouble(obj);
            }
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public Double getLastValue(long variableId) throws Exception {
        HashMap<Long, Object> lastValueMap = new HashMap<Long, Object>();
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query query = null;
            query = session.createSQLQuery("select vs.value " + "from variable v " + "join variable_snapshot_last vs on v.id = vs.variable_id " + "where v.id = ? " + "and vs.snapshot_timestamp > ? ");
            query.setLong(0, variableId);
            query.setTimestamp(1, new Date(System.currentTimeMillis() - 2 * 60 * 1000));
            List list = query.list();
            if (!list.isEmpty()) {
                Object obj = list.get(0);
                if (obj != null) {
                    return getDouble(obj);
                }
            }
            return null;
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public void getLastValues(String host, String scopePattern, Map<Long, Double> lastValueMap) throws Exception {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query query = null;
            query = session.createSQLQuery("select v.id,vs.value " + "from variable v " + "join variable_snapshot_last vs on v.id = vs.variable_id " + "where v.host = ? " + "and v.scope like ? " + "and vs.snapshot_timestamp > ? ");
            query.setString(0, host);
            query.setString(1, scopePattern);
            query.setTimestamp(2, new Date(System.currentTimeMillis() - 2 * 60 * 1000));
            List list = query.list();
            for (Object item : list) {
                Object[] itemArr = (Object[]) item;
                lastValueMap.put(getLong(itemArr[0]), getDouble(itemArr[1]));
            }
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public HashMap<Long, Long> calculateThresholds(Date startDate) throws Exception {
        HashMap<Long, Long> thresholdMap = new HashMap<Long, Long>();
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query query = null;
            query = session.createSQLQuery("select vs.variable_id,count(*),avg(value),stddev_pop(value) from variable_snapshot vs where vs.snapshot_timestamp > ? group by vs.variable_id");
            query.setTimestamp(0, startDate);
            List list = query.list();
            for (Object item : list) {
                Object[] itemArr = (Object[]) item;
                long count = getLong(itemArr[1]);
                if (count >= 2) {
                    long avg = getLong(itemArr[2]);
                    long std = getLong(itemArr[3]);
                    thresholdMap.put(getLong(itemArr[0]), ThresholdManager.deriveThreshold(avg, std));
                }
            }
            return thresholdMap;
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    private long getLong(Object obj) throws Exception {
        return (long) getDouble(obj);
    }

    private double getDouble(Object obj) throws Exception {
        if (obj instanceof Long) {
            return ((Long) obj).doubleValue();
        } else if (obj instanceof Integer) {
            return ((Integer) obj).doubleValue();
        } else if (obj instanceof Double) {
            return ((Double) obj).doubleValue();
        } else if (obj instanceof Float) {
            return ((Double) obj).doubleValue();
        } else if (obj instanceof BigInteger) {
            return ((BigInteger) obj).doubleValue();
        } else if (obj instanceof BigDecimal) {
            return ((BigDecimal) obj).doubleValue();
        }
        throw new Exception("cannot get numeric value");
    }

    public List findTransaction(String operationName, int limit) throws Exception {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query query = session.createQuery("select distinct t from RlxTransaction t, RlxOperation o where t.refId = o.transactionRefId and o.name = ? order by t.timestamp desc");
            query.setString(0, operationName);
            query.setMaxResults(limit);
            List list = query.list();
            Hibernate.initialize(list);
            return list;
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        System.out.println(sdf.format(new Date(System.currentTimeMillis())));
    }

    public void test() throws Exception {
        try {
            ArrayList<RlxTransaction> transactionList = new ArrayList<RlxTransaction>();
            for (int i = 1; i < 1000; i++) {
                RlxTransaction transaction = new RlxTransaction();
                transaction.setName("test" + i);
                transaction.setExecutionTime(111);
                transaction.setTimestamp(new Date(System.currentTimeMillis()));
                transactionList.add(transaction);
            }
            long start = System.currentTimeMillis();
            saveList(transactionList);
            System.out.print(System.currentTimeMillis() - start);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
