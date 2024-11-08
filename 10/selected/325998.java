package br.furb.jpa.persistence;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Query;
import br.furb.util.Utils;

/**
 * @author usuario
 * 
 * @param <TEntity>
 */
@SuppressWarnings("unchecked")
public abstract class GenericPersistence<TEntity> implements PersistenceOperations<TEntity> {

    private Class<TEntity> entityClass;

    private String puName;

    private GenericPersistenceException createGenericPersistenceException(Exception ex) {
        GenericPersistenceException pEx = new GenericPersistenceException(ex.getMessage());
        if (ex.getMessage().indexOf(PersistenceErrorTypes.TRANSACTION_NOT_ACTIVE.toString()) >= 0) pEx.setErrorType(PersistenceErrorTypes.TRANSACTION_NOT_ACTIVE); else if (ex.getMessage().indexOf(PersistenceErrorTypes.CANNOT_OPEN_CONNECTION.toString()) >= 0) pEx.setErrorType(PersistenceErrorTypes.CANNOT_OPEN_CONNECTION); else if (ex.getMessage().indexOf(PersistenceErrorTypes.ENTITY_NOT_FOUND.toString()) >= 0) pEx.setErrorType(PersistenceErrorTypes.ENTITY_NOT_FOUND); else if (ex.getMessage().indexOf(PersistenceErrorTypes.UNABLE_TO_CONFIGURE_EMF.toString()) >= 0) pEx.setErrorType(PersistenceErrorTypes.UNABLE_TO_CONFIGURE_EMF); else if (ex.getMessage().indexOf(PersistenceErrorTypes.UNABLE_TO_BUILD_EMF.toString()) >= 0) pEx.setErrorType(PersistenceErrorTypes.UNABLE_TO_BUILD_EMF); else if (ex.getMessage().indexOf(PersistenceErrorTypes.DETACHED_ENTITY.toString()) >= 0) pEx.setErrorType(PersistenceErrorTypes.DETACHED_ENTITY); else if (ex.getMessage().indexOf(PersistenceErrorTypes.NO_PERSISTENCE_PROVIDER.toString()) >= 0) pEx.setErrorType(PersistenceErrorTypes.NO_PERSISTENCE_PROVIDER);
        return pEx;
    }

    private void setNullEntityId(TEntity entity) {
        try {
            Field[] fields = entity.getClass().getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(Id.class)) {
                    field.setAccessible(true);
                    field.set(entity, null);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private int getEntityId(TEntity entity) {
        try {
            Field[] fields = entity.getClass().getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(Id.class)) {
                    String getMethodStr = Utils.concatString("get", Utils.concatString(field.getName().substring(0, 1).toUpperCase(), field.getName().substring(1, field.getName().length())));
                    Method getMethod = null;
                    getMethod = entity.getClass().getMethod(getMethodStr);
                    Integer returnValue = (Integer) getMethod.invoke(entity, (Object[]) null);
                    if (returnValue != null) return returnValue.intValue(); else return -1;
                }
            }
        } catch (Exception ex) {
            return -1;
        }
        return -1;
    }

    /**
	 * @param puName
	 */
    public GenericPersistence(String puName) {
        this.entityClass = (Class<TEntity>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        this.puName = puName.toLowerCase();
    }

    @Override
    public int executeBulkOperation(String bulkQuery, Object[] parameters) throws GenericPersistenceException {
        EntityManager em = null;
        try {
            em = EMFactory.getEntityManagerFactory(this.puName).createEntityManager();
            em.getTransaction().begin();
            Query query = em.createQuery(bulkQuery);
            if (parameters != null) for (int count = 0; count < parameters.length; count++) query.setParameter((count + 1), parameters[count]);
            int rows = query.executeUpdate();
            em.getTransaction().commit();
            return rows;
        } catch (Exception ex) {
            if (em != null) if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw (GenericPersistenceException) this.createGenericPersistenceException(ex).initCause(ex);
        } finally {
            if (em != null) em.close();
        }
    }

    @Override
    public TEntity update(TEntity entity) throws GenericPersistenceException {
        EntityManager em = null;
        try {
            em = EMFactory.getEntityManagerFactory(this.puName).createEntityManager();
            em.getTransaction().begin();
            entity = em.merge(entity);
            em.getTransaction().commit();
            return entity;
        } catch (Exception ex) {
            if (em != null) if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw (GenericPersistenceException) this.createGenericPersistenceException(ex).initCause(ex);
        } finally {
            if (em != null) em.close();
        }
    }

    @Override
    public void save(TEntity entity) throws GenericPersistenceException {
        EntityManager em = null;
        try {
            em = EMFactory.getEntityManagerFactory(this.puName).createEntityManager();
            em.getTransaction().begin();
            this.setNullEntityId(entity);
            em.persist(entity);
            em.getTransaction().commit();
        } catch (Exception ex) {
            if (em != null) if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw (GenericPersistenceException) this.createGenericPersistenceException(ex).initCause(ex);
        } finally {
            if (em != null) em.close();
        }
    }

    @Override
    public void delete(TEntity entity) throws GenericPersistenceException {
        EntityManager em = null;
        try {
            em = EMFactory.getEntityManagerFactory(this.puName).createEntityManager();
            em.getTransaction().begin();
            entity = (TEntity) em.find(entity.getClass(), this.getEntityId(entity));
            if (entity == null) throw new Exception(PersistenceErrorTypes.ENTITY_NOT_FOUND.toString());
            em.remove(entity);
            em.getTransaction().commit();
        } catch (Exception ex) {
            if (em != null) if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw (GenericPersistenceException) this.createGenericPersistenceException(ex).initCause(ex);
        } finally {
            if (em != null) em.close();
        }
    }

    @Override
    public TEntity findByPK(int pkValue) throws GenericPersistenceException {
        EntityManager em = null;
        try {
            em = EMFactory.getEntityManagerFactory(this.puName).createEntityManager();
            TEntity entity = (TEntity) em.find(this.entityClass, pkValue);
            return entity;
        } catch (Exception ex) {
            throw (GenericPersistenceException) this.createGenericPersistenceException(ex).initCause(ex);
        }
    }

    @Override
    public ArrayList<TEntity> executeNativeQuery(String sql, Object[] parameters) throws GenericPersistenceException {
        EntityManager em = null;
        ArrayList<TEntity> returnList = new ArrayList<TEntity>();
        try {
            em = EMFactory.getEntityManagerFactory(this.puName).createEntityManager();
            em.getTransaction().begin();
            Query query = em.createNativeQuery(sql);
            if (parameters != null) for (int count = 0; count < parameters.length; count++) query.setParameter((count + 1), parameters[count]);
            returnList = (ArrayList<TEntity>) query.getResultList();
            em.getTransaction().commit();
        } catch (Exception ex) {
            returnList.clear();
            if (em != null) if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw (GenericPersistenceException) this.createGenericPersistenceException(ex).initCause(ex);
        }
        return returnList;
    }

    @Override
    public ArrayList<TEntity> executeNamedQuery(String namedQuery, Object[] parameters) throws GenericPersistenceException {
        EntityManager em = null;
        ArrayList<TEntity> returnList = new ArrayList<TEntity>();
        try {
            em = EMFactory.getEntityManagerFactory(this.puName).createEntityManager();
            em.getTransaction().begin();
            Query query = em.createNamedQuery(namedQuery);
            if (parameters != null) for (int count = 0; count < parameters.length; count++) query.setParameter((count + 1), parameters[count]);
            returnList = (ArrayList<TEntity>) query.getResultList();
            em.getTransaction().commit();
        } catch (Exception ex) {
            returnList.clear();
            if (em != null) if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw (GenericPersistenceException) this.createGenericPersistenceException(ex).initCause(ex);
        }
        return returnList;
    }

    @Override
    public ArrayList<TEntity> executeQuery(String jpql, Object[] parameters) throws GenericPersistenceException {
        EntityManager em = null;
        ArrayList<TEntity> returnList = new ArrayList<TEntity>();
        try {
            em = EMFactory.getEntityManagerFactory(this.puName).createEntityManager();
            em.getTransaction().begin();
            Query query = em.createQuery(jpql);
            if (parameters != null) for (int count = 0; count < parameters.length; count++) query.setParameter((count + 1), parameters[count]);
            returnList = (ArrayList<TEntity>) query.getResultList();
            em.getTransaction().commit();
        } catch (Exception ex) {
            returnList.clear();
            if (em != null) if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw (GenericPersistenceException) this.createGenericPersistenceException(ex).initCause(ex);
        }
        return returnList;
    }

    @Override
    public int executeNativeCommand(String sql, Object[] parameters) throws GenericPersistenceException {
        EntityManager em = null;
        try {
            em = EMFactory.getEntityManagerFactory(this.puName).createEntityManager();
            em.getTransaction().begin();
            Query query = em.createNativeQuery(sql);
            if (parameters != null) for (int count = 0; count < parameters.length; count++) query.setParameter((count + 1), parameters[count]);
            int inserted = query.executeUpdate();
            em.getTransaction().commit();
            return inserted;
        } catch (Exception ex) {
            if (em != null) if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw (GenericPersistenceException) this.createGenericPersistenceException(ex).initCause(ex);
        }
    }

    /**
	 * Fecha a conex�o com a base dados
	 */
    public void close() {
        EMFactory.closeEntityManagerFactory(puName);
    }

    /**
	 * @return O nome da base de dados
	 */
    public String getPuName() {
        return puName;
    }

    /**
	 * @param puName - O nome da base de dados
	 */
    public void setPuName(String puName) {
        this.puName = puName;
    }
}
