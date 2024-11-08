package com.quantanetwork.esm.port;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Query;

public final class QuantaEntityManager implements Serializable {

    public static final String CLASS_LOADER[] = { "eclipselink.classloader" };

    private static final long serialVersionUID = 1L;

    private String persistUnitName = "";

    private EntityManagerFactory entityManagerFactory = null;

    private ClassLoader classLoader = null;

    private Map<String, Object> connPropMap = null;

    public QuantaEntityManager() {
    }

    public QuantaEntityManager(ClassLoader classLoader, String persistUnitName) {
        this.classLoader = classLoader;
        this.persistUnitName = persistUnitName;
    }

    public QuantaEntityManager(String persistUnitName) {
        this.persistUnitName = persistUnitName;
    }

    public void createTables() {
        EntityManager em = getEntityManager();
        if (em != null) {
            em.close();
        }
    }

    public void close() {
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
    }

    /**
	 * 获取指定对象类型和主键值的实体实例
	 * @param entityClass 实体对象类型
	 * @param primaryKey 该实体类型对应的主键值
	 * @return 指定实体类型的一个实例，当查询条件不成立时，返回一个空对象
	 */
    public <T> T find(Class<T> entityClass, Object primaryKey) {
        return find(null, entityClass, primaryKey);
    }

    /**
	 * 获取指定对象类型和主键值的实体实例
	 * @param em 指定的EntityManager对象
	 * @param entityClass 实体对象类型
	 * @param primaryKey 该实体类型对应的主键值
	 * @return 指定实体类型的一个实例，当查询条件不成立时，返回一个空对象
	 */
    public <T> T find(EntityManager em, Class<T> entityClass, Object primaryKey) {
        T rtnVal = null;
        boolean doCloseEntityManager = false;
        EntityManager emTemp = em;
        if (emTemp == null) {
            emTemp = getEntityManager();
            doCloseEntityManager = true;
        }
        if (emTemp != null) {
            rtnVal = emTemp.find(entityClass, primaryKey);
            if (doCloseEntityManager) {
                emTemp.close();
            }
        }
        return rtnVal;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public Map<String, Object> getConnPropMap() {
        return connPropMap;
    }

    /**
	 * 获取一个EntityManager实例，对于QuantaEntityManager实例所提供的方法不够用时，
	 * 可以通过调用该方法获取到相应的实例之后，进行扩展调用
	 * @return JPA规范中的EntityManager实例
	 */
    public EntityManager getEntityManager() {
        EntityManager rtnVal = null;
        if (entityManagerFactory == null) {
            ClassLoader oClassLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader != null) {
                Thread.currentThread().setContextClassLoader(classLoader);
            }
            try {
                entityManagerFactory = Persistence.createEntityManagerFactory(persistUnitName, connPropMap);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if (oClassLoader != null) {
                Thread.currentThread().setContextClassLoader(oClassLoader);
            }
        }
        if (entityManagerFactory != null) {
            rtnVal = entityManagerFactory.createEntityManager();
        }
        return rtnVal;
    }

    public EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }

    public String getPersistUnitName() {
        return persistUnitName;
    }

    public <T> Collection<T> merge(Collection<T> objs) throws RuntimeException {
        return merge(null, objs);
    }

    public <T> Collection<T> merge(EntityManager em, Collection<T> objs) throws RuntimeException {
        Collection<T> rtnVal = new ArrayList<T>();
        boolean doCloseEntityManager = false;
        EntityManager emTemp = em;
        if (emTemp == null) {
            emTemp = getEntityManager();
            doCloseEntityManager = true;
        }
        if (emTemp != null) {
            EntityTransaction et = emTemp.getTransaction();
            try {
                if (et.isActive() == false) {
                    et.begin();
                }
                for (T t : objs) {
                    T tt = emTemp.merge(t);
                    rtnVal.add(tt);
                }
                if (et.isActive()) {
                    et.commit();
                }
            } catch (Exception exception) {
                if (et.isActive()) {
                    et.rollback();
                }
                if (doCloseEntityManager) {
                    emTemp.close();
                }
                throw new RuntimeException(exception);
            }
            if (doCloseEntityManager) {
                emTemp.close();
            }
        }
        return rtnVal;
    }

    public <T> T merge(EntityManager em, T obj) throws RuntimeException {
        T rtnVal = null;
        boolean doCloseEntityManager = false;
        EntityManager emTemp = em;
        if (emTemp == null) {
            emTemp = getEntityManager();
            doCloseEntityManager = true;
        }
        if (emTemp != null) {
            EntityTransaction et = emTemp.getTransaction();
            try {
                if (et.isActive() == false) {
                    et.begin();
                }
                rtnVal = emTemp.merge(obj);
                if (et.isActive()) {
                    et.commit();
                }
            } catch (Exception exception) {
                if (et.isActive()) {
                    et.rollback();
                }
                if (doCloseEntityManager) {
                    emTemp.close();
                }
                throw new RuntimeException(exception);
            }
            if (doCloseEntityManager) {
                emTemp.close();
            }
        }
        return rtnVal;
    }

    public <T> T merge(T obj) throws RuntimeException {
        return merge(null, obj);
    }

    public <T> void persist(Collection<T> objs) throws RuntimeException {
        EntityManager em = getEntityManager();
        if (em != null) {
            EntityTransaction et = em.getTransaction();
            try {
                if (et.isActive() == false) {
                    et.begin();
                }
                for (T t : objs) {
                    em.persist(t);
                }
                if (et.isActive()) {
                    et.commit();
                }
            } catch (Exception exception) {
                if (et.isActive()) {
                    et.rollback();
                }
                em.close();
                throw new RuntimeException(exception);
            }
            em.close();
        }
    }

    public <T> void persist(T obj) throws RuntimeException {
        EntityManager em = getEntityManager();
        if (em != null) {
            EntityTransaction et = em.getTransaction();
            try {
                if (et.isActive() == false) {
                    et.begin();
                }
                em.persist(obj);
                if (et.isActive()) {
                    et.commit();
                }
            } catch (Exception exception) {
                if (et.isActive()) {
                    et.rollback();
                }
                em.close();
                throw new RuntimeException(exception);
            }
            em.close();
        }
    }

    /**
	 * 查询操作：SELECT en FROM EntityClass en[ where en.property = :mKey1 and en.property = :mKey2[ and ...]]
	 * @param entityClass 指定返回的List实例中的元素的实体对象类型
	 * @param firstResult 指定的记录起始位置
	 * @param maxResultSize 指定本次查询的结果返回的最大记录数
	 * @param strQuery 形如查询操作中的语句
	 * @param mapVal 以查询操作语句中:mKeyn的mKeyn为Key，以条件值为Value的Map实例，如果不带条件查询，该参数为空
	 * @return 返回一个指定对象类型的List<T>实例
	 * @throws RuntimeException
	 */
    public <T> List<T> query(Class<T> entityClass, int firstResult, int maxResultSize, String strQuery, Map<String, Object> mapVal) throws RuntimeException {
        return query(null, entityClass, firstResult, maxResultSize, strQuery, mapVal);
    }

    /**
	 * 查询操作：SELECT en FROM EntityClass en[ where en.property = ?1 and en.property = ?2[ and ...]]
	 * @param entityClass 指定返回的List实例中的元素的实体对象类型
	 * @param firstResult 指定本次查询的结果起始记录数
	 * @param maxResultSize 指定本次查询的结果返回的最大记录数
	 * @param strQuery 形如查询操作中的语句
	 * @param objs 对应于查询操作语句中?n的条件值的数组，如果没有条件值，该参数缺省
	 * @return 返回一个不指定对象类型的List<?>实例
	 * @throws RuntimeException
	 */
    public <T> List<T> query(Class<T> entityClass, int firstResult, int maxResultSize, String strQuery, Object... objs) throws RuntimeException {
        return query(null, entityClass, firstResult, maxResultSize, strQuery, objs);
    }

    /**
	 * 查询操作：SELECT en FROM EntityClass en[ where en.property = :mKey1 and en.property = :mKey2[ and ...]]
	 * @param entityClass 指定返回的List实例中的元素的实体对象类型
	 * @param maxResultSize 指定本次查询的结果返回的最大记录数
	 * @param strQuery 形如查询操作中的语句
	 * @param mapVal 以查询操作语句中:mKeyn的mKeyn为Key，以条件值为Value的Map实例，如果不带条件查询，该参数为空
	 * @return 返回一个指定对象类型的List<T>实例
	 * @throws RuntimeException
	 */
    public <T> List<T> query(Class<T> entityClass, int maxResultSize, String strQuery, Map<String, Object> mapVal) throws RuntimeException {
        return query(null, entityClass, 0, maxResultSize, strQuery, mapVal);
    }

    /**
	 * 查询操作：SELECT en FROM EntityClass en[ where en.property = ?1 and en.property = ?2[ and ...]]
	 * @param entityClass 指定返回的List实例中的元素的实体对象类型
	 * @param maxResultSize 指定本次查询的结果返回的最大记录数
	 * @param strQuery 形如查询操作中的语句
	 * @param objs 对应于查询操作语句中?n的条件值的数组，如果没有条件值，该参数缺省
	 * @return 返回一个不指定对象类型的List<?>实例
	 * @throws RuntimeException
	 */
    public <T> List<T> query(Class<T> entityClass, int maxResultSize, String strQuery, Object... objs) throws RuntimeException {
        return query(null, entityClass, 0, maxResultSize, strQuery, objs);
    }

    /**
	 * 查询操作：SELECT en FROM EntityClass en[ where en.property = :mKey1 and en.property = :mKey2[ and ...]]
	 * @param entityClass 指定返回的List实例中的元素的实体对象类型
	 * @param strQuery 形如查询操作中的语句
	 * @param mapVal 以查询操作语句中:mKeyn的mKeyn为Key，以条件值为Value的Map实例，如果不带条件查询，该参数为空
	 * @return 返回一个指定对象类型的List<T>实例
	 * @throws RuntimeException
	 */
    public <T> List<T> query(Class<T> entityClass, String strQuery, Map<String, Object> mapVal) throws RuntimeException {
        return query(null, entityClass, 0, strQuery, mapVal);
    }

    /**
	 * 查询操作：SELECT en FROM EntityClass en[ where en.property = ?1 and en.property = ?2[ and ...]]
	 * @param entityClass 指定返回的List实例中的元素的实体对象类型
	 * @param strQuery 形如查询操作中的语句
	 * @param objs 对应于查询操作语句中?n的条件值的数组，如果没有条件值，该参数缺省
	 * @return 返回一个不指定对象类型的List<?>实例
	 * @throws RuntimeException
	 */
    public <T> List<T> query(Class<T> entityClass, String strQuery, Object... objs) throws RuntimeException {
        return query(null, entityClass, 0, strQuery, objs);
    }

    /**
	 * 查询操作：SELECT en FROM EntityClass en[ where en.property = :mKey1 and en.property = :mKey2[ and ...]]
	 * @param em 指定的EntityManager对象
	 * @param entityClass 指定返回的List实例中的元素的实体对象类型
	 * @param firstResult 指定的记录起始位置
	 * @param maxResultSize 指定本次查询的结果返回的最大记录数
	 * @param strQuery 形如查询操作中的语句
	 * @param mapVal 以查询操作语句中:mKeyn的mKeyn为Key，以条件值为Value的Map实例，如果不带条件查询，该参数为空
	 * @return 返回一个指定对象类型的List<T>实例
	 * @throws RuntimeException
	 */
    public <T> List<T> query(EntityManager em, Class<T> entityClass, int firstResult, int maxResultSize, String strQuery, Map<String, Object> mapVal) throws RuntimeException {
        List<T> rtnVal = new ArrayList<T>();
        List<?> valTemp = null;
        EntityManager emTemp = em;
        boolean doCloseEntityManager = false;
        if (emTemp == null) {
            emTemp = getEntityManager();
            doCloseEntityManager = true;
        }
        if (emTemp != null) {
            try {
                Query query = emTemp.createQuery(strQuery);
                if (mapVal != null) {
                    for (String mKey : mapVal.keySet()) {
                        Object mObj = mapVal.get(mKey);
                        query.setParameter(mKey, mObj);
                    }
                }
                if (query != null) {
                    if (firstResult > 0) {
                        query.setFirstResult(firstResult);
                    }
                    if (maxResultSize > 0) {
                        query.setMaxResults(maxResultSize);
                    }
                    valTemp = query.getResultList();
                }
            } catch (Exception exception) {
                if (doCloseEntityManager) {
                    emTemp.close();
                }
                throw new RuntimeException(exception);
            }
            if (doCloseEntityManager) {
                emTemp.close();
            }
        }
        if (valTemp != null) {
            for (Object vObj : valTemp) {
                try {
                    T tObj = entityClass.cast(vObj);
                    if (tObj != null) {
                        rtnVal.add(tObj);
                    }
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            }
        }
        return rtnVal;
    }

    /**
	 * 查询操作：SELECT en FROM EntityClass en[ where en.property = ?1 and en.property = ?2[ and ...]]
	 * @param em 指定的EntityManager对象
	 * @param entityClass 指定返回的List实例中的元素的实体对象类型
	 * @param firstResult 指定本次查询的结果起始记录数
	 * @param maxResultSize 指定本次查询的结果返回的最大记录数
	 * @param strQuery 形如查询操作中的语句
	 * @param objs 对应于查询操作语句中?n的条件值的数组，如果没有条件值，该参数缺省
	 * @return 返回一个不指定对象类型的List<?>实例
	 * @throws RuntimeException
	 */
    public <T> List<T> query(EntityManager em, Class<T> entityClass, int firstResult, int maxResultSize, String strQuery, Object... objs) throws RuntimeException {
        List<T> rtnVal = new ArrayList<T>();
        List<?> valTemp = null;
        boolean doCloseEntityManager = false;
        EntityManager emTemp = em;
        if (emTemp == null) {
            emTemp = getEntityManager();
            doCloseEntityManager = true;
        }
        if (emTemp != null) {
            try {
                Query query = emTemp.createQuery(strQuery);
                if (objs != null) {
                    int index = 1;
                    for (Object obj : objs) {
                        query.setParameter(index++, obj);
                    }
                }
                if (query != null) {
                    if (firstResult > 0) {
                        query.setFirstResult(firstResult);
                    }
                    if (maxResultSize > 0) {
                        query.setMaxResults(maxResultSize);
                    }
                    valTemp = query.getResultList();
                }
            } catch (Exception exception) {
                if (doCloseEntityManager) {
                    emTemp.close();
                }
                throw new RuntimeException(exception);
            }
            if (doCloseEntityManager) {
                emTemp.close();
            }
        }
        if (valTemp != null) {
            for (Object vObj : valTemp) {
                try {
                    T tObj = entityClass.cast(vObj);
                    if (tObj != null) {
                        rtnVal.add(tObj);
                    }
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            }
        }
        return rtnVal;
    }

    /**
	 * 查询操作：SELECT en FROM EntityClass en[ where en.property = :mKey1 and en.property = :mKey2[ and ...]]
	 * @param em 指定的EntityManager对象
	 * @param entityClass 指定返回的List实例中的元素的实体对象类型
	 * @param maxResultSize 指定本次查询的结果返回的最大记录数
	 * @param strQuery 形如查询操作中的语句
	 * @param mapVal 以查询操作语句中:mKeyn的mKeyn为Key，以条件值为Value的Map实例，如果不带条件查询，该参数为空
	 * @return 返回一个指定对象类型的List<T>实例
	 * @throws RuntimeException
	 */
    public <T> List<T> query(EntityManager em, Class<T> entityClass, int maxResultSize, String strQuery, Map<String, Object> mapVal) throws RuntimeException {
        return query(em, entityClass, 0, maxResultSize, strQuery, mapVal);
    }

    /**
	 * 查询操作：SELECT en FROM EntityClass en[ where en.property = ?1 and en.property = ?2[ and ...]]
	 * @param em 指定的EntityManager对象
	 * @param entityClass 指定返回的List实例中的元素的实体对象类型
	 * @param maxResultSize 指定本次查询的结果返回的最大记录数
	 * @param strQuery 形如查询操作中的语句
	 * @param objs 对应于查询操作语句中?n的条件值的数组，如果没有条件值，该参数缺省
	 * @return 返回一个不指定对象类型的List<?>实例
	 * @throws RuntimeException
	 */
    public <T> List<T> query(EntityManager em, Class<T> entityClass, int maxResultSize, String strQuery, Object... objs) throws RuntimeException {
        return query(em, entityClass, 0, maxResultSize, strQuery, objs);
    }

    /**
	 * 查询操作：SELECT en FROM EntityClass en[ where en.property = :mKey1 and en.property = :mKey2[ and ...]]
	 * @param em 指定的EntityManager对象
	 * @param entityClass 指定返回的List实例中的元素的实体对象类型
	 * @param strQuery 形如查询操作中的语句
	 * @param mapVal 以查询操作语句中:mKeyn的mKeyn为Key，以条件值为Value的Map实例，如果不带条件查询，该参数为空
	 * @return 返回一个指定对象类型的List<T>实例
	 * @throws RuntimeException
	 */
    public <T> List<T> query(EntityManager em, Class<T> entityClass, String strQuery, Map<String, Object> mapVal) throws RuntimeException {
        return query(em, entityClass, 0, strQuery, mapVal);
    }

    /**
	 * 查询操作：SELECT en FROM EntityClass en[ where en.property = ?1 and en.property = ?2[ and ...]]
	 * @param em 指定的EntityManager对象
	 * @param entityClass 指定返回的List实例中的元素的实体对象类型
	 * @param strQuery 形如查询操作中的语句
	 * @param objs 对应于查询操作语句中?n的条件值的数组，如果没有条件值，该参数缺省
	 * @return 返回一个不指定对象类型的List<?>实例
	 * @throws RuntimeException
	 */
    public <T> List<T> query(EntityManager em, Class<T> entityClass, String strQuery, Object... objs) throws RuntimeException {
        return query(em, entityClass, 0, strQuery, objs);
    }

    public <T> void remove(Collection<T> objs) throws RuntimeException {
        EntityManager em = getEntityManager();
        if (em != null) {
            EntityTransaction et = em.getTransaction();
            try {
                if (et.isActive() == false) {
                    et.begin();
                }
                for (T t : objs) {
                    em.remove(em.merge(t));
                }
                if (et.isActive()) {
                    et.commit();
                }
            } catch (Exception exception) {
                if (et.isActive()) {
                    et.rollback();
                }
                em.close();
                throw new RuntimeException(exception);
            }
            em.close();
        }
    }

    public <T> void remove(EntityManager em, Collection<T> objs) throws RuntimeException {
        EntityManager emTemp = em;
        boolean doCloseEntityManager = false;
        if (emTemp == null) {
            emTemp = getEntityManager();
            doCloseEntityManager = true;
        }
        if (emTemp != null) {
            EntityTransaction et = emTemp.getTransaction();
            try {
                if (et.isActive() == false) {
                    et.begin();
                }
                for (T t : objs) {
                    emTemp.remove(t);
                }
                if (et.isActive()) {
                    et.commit();
                }
            } catch (Exception exception) {
                if (et.isActive()) {
                    et.rollback();
                }
                if (doCloseEntityManager) {
                    emTemp.close();
                }
                throw new RuntimeException(exception);
            }
            if (doCloseEntityManager) {
                emTemp.close();
            }
        }
    }

    public <T> void remove(T obj) throws RuntimeException {
        EntityManager em = getEntityManager();
        if (em != null) {
            EntityTransaction et = em.getTransaction();
            try {
                if (et.isActive() == false) {
                    et.begin();
                }
                em.remove(em.merge(obj));
                if (et.isActive()) {
                    et.commit();
                }
            } catch (Exception exception) {
                if (et.isActive()) {
                    et.rollback();
                }
                em.close();
                throw new RuntimeException(exception);
            }
            em.close();
        }
    }

    public <T> void remove(EntityManager em, T obj) throws RuntimeException {
        EntityManager emTemp = em;
        boolean doCloseEntityManager = false;
        if (emTemp == null) {
            emTemp = getEntityManager();
            doCloseEntityManager = true;
        }
        if (emTemp != null) {
            EntityTransaction et = emTemp.getTransaction();
            try {
                if (et.isActive() == false) {
                    et.begin();
                }
                emTemp.remove(obj);
                if (et.isActive()) {
                    et.commit();
                }
            } catch (Exception exception) {
                if (et.isActive()) {
                    et.rollback();
                }
                if (doCloseEntityManager) {
                    emTemp.close();
                }
                throw new RuntimeException(exception);
            }
            if (doCloseEntityManager) {
                emTemp.close();
            }
        }
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public void setConnPropMap(Map<String, Object> connMap) {
        this.connPropMap = connMap;
    }

    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    public void setPersistUnitName(String persistUnitName) {
        this.persistUnitName = persistUnitName;
    }

    /**
	 * 修改操作：UPDATE EntityClass en set en.property = :mKey1[,en.property = :mKey2...][ where en.property = :mKey3[ and...]]<br>
	 * 删除操作: DELETE FROM EntityClass en[ where en.property = :mKey1[ and...]]
	 * @param strUpdate 形如修改操作或者删除操作中的语句
	 * @param mapVal 以修改/删除操作语句中:mKeyn的mKeyn为Key，以条件值为Value的Map实例，如果不带条件查询，该参数为空
	 * @return 返回本次操作所影响的数据实体个数
	 * @throws java.lang.RuntimeException
	 */
    public int update(String strUpdate, Map<String, Object> mapVal) throws RuntimeException {
        int rtnVal = 0;
        EntityManager em = getEntityManager();
        if (em != null) {
            EntityTransaction et = em.getTransaction();
            try {
                Query query = em.createQuery(strUpdate);
                if (mapVal != null) {
                    for (String mKey : mapVal.keySet()) {
                        Object mObj = mapVal.get(mKey);
                        query.setParameter(mKey, mObj);
                    }
                }
                if (query != null) {
                    if (et.isActive() == false) {
                        et.begin();
                    }
                    rtnVal = query.executeUpdate();
                    if (et.isActive()) {
                        et.commit();
                    }
                }
            } catch (Exception exception) {
                if (et.isActive()) {
                    et.rollback();
                }
                em.close();
                throw new RuntimeException(exception);
            }
            em.close();
        }
        return rtnVal;
    }

    /**
	 * 修改操作：UPDATE EntityClass en set en.property = ?1[,en.property = ?2...][ where en.property = ?3[ and...]]<br>
	 * 删除操作: DELETE FROM EntityClass en[ where en.property = ?1[ and...]]
	 * @param strUpdate 形如修改操作或者删除操作中的语句
	 * @param objs 对应于修改/删除操作语句中?n的条件值的数组，如果没有条件值，该参数缺省
	 * @return 返回本次操作所影响的数据实体个数
	 * @throws java.lang.RuntimeException
	 */
    public int update(String strUpdate, Object... objs) throws RuntimeException {
        int rtnVal = 0;
        EntityManager em = getEntityManager();
        if (em != null) {
            EntityTransaction et = em.getTransaction();
            try {
                Query query = em.createQuery(strUpdate);
                if (objs != null) {
                    int index = 1;
                    for (Object obj : objs) {
                        query.setParameter(index++, obj);
                    }
                }
                if (query != null) {
                    if (et.isActive() == false) {
                        et.begin();
                    }
                    rtnVal = query.executeUpdate();
                    if (et.isActive()) {
                        et.commit();
                    }
                }
            } catch (Exception exception) {
                if (et.isActive()) {
                    et.rollback();
                }
                em.close();
                throw new RuntimeException(exception);
            }
            em.close();
        }
        return rtnVal;
    }
}
