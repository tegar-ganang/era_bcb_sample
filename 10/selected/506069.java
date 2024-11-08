package casdadm.core;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

/**
 *
 * @author David Leite
 */
public class GenericJPADAO<E> implements DAO<E> {

    Class<E> clazz;

    EntityManager emAttach;

    EntityTransaction txAttach = null;

    public GenericJPADAO(Class<E> clazz) {
        this.clazz = clazz;
    }

    public EntityManager getEntityManager() {
        return EntityManagerSingleton.getEntityManager();
    }

    public E attach(E obj) {
        emAttach = getEntityManager();
        txAttach = emAttach.getTransaction();
        txAttach.begin();
        return emAttach.merge(obj);
    }

    public void commitAndDettach() {
        txAttach.commit();
        emAttach.close();
    }

    public void insert(E obj) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            obj = em.merge(obj);
            tx.commit();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    public void persist(E obj) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(obj);
            tx.commit();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    public void insertListOfObject(List<E> objList) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            for (E object : objList) {
                em.merge(object);
            }
            tx.commit();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    public void insertVariableObjects(E... objetos) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            for (E object : objetos) {
                em.merge(object);
            }
            tx.commit();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    public void delete(E obj) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            obj = em.merge(obj);
            em.remove(obj);
            tx.commit();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    public void deleteAll() {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Query q = em.createQuery("DELETE FROM " + clazz.getSimpleName());
            tx.commit();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    public List<E> getList() {
        return findAll();
    }

    public E findById(Integer id) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            return em.find(clazz, id);
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    public List<E> findByFields(E obj) {
        Field[] fields = obj.getClass().getDeclaredFields();
        Method[] methods = obj.getClass().getDeclaredMethods();
        List parameters = new ArrayList();
        List<E> objects = new ArrayList<E>();
        String query = "select distinct o from " + obj.getClass().getSimpleName() + " o where ";
        String fieldName;
        Object parameter;
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            for (Method method : methods) {
                if (method.getName().startsWith("get")) {
                    for (Field field : fields) {
                        if (!field.getName().startsWith("transient")) {
                            fieldName = field.getName();
                            if (method.getName().toLowerCase().equals("get" + fieldName.toLowerCase())) {
                                Class fieldClass = field.getType();
                                if (fieldClass == List.class) {
                                    break;
                                }
                                parameter = method.invoke(obj);
                                if (parameter != null) {
                                    query += "o." + fieldName + " = ? and ";
                                    parameters.add(parameter);
                                }
                                break;
                            }
                        }
                    }
                }
            }
            if (query.endsWith(" where ")) {
                query = query.replaceAll(" where ", "");
            } else {
                query = query.substring(0, query.lastIndexOf(" and "));
            }
            Query q = em.createQuery(query);
            Integer i = 1;
            for (Object parametro : parameters) {
                q.setParameter(i++, parametro);
            }
            tx.commit();
            objects = q.getResultList();
        } catch (IllegalAccessException ex) {
            Logger.getLogger(GenericJPADAO.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(GenericJPADAO.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(GenericJPADAO.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
        return objects;
    }

    public List<E> findAll() {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        List<E> list = new ArrayList<E>();
        try {
            tx.begin();
            String query = "select o from " + clazz.getSimpleName() + " o";
            Query q = em.createQuery(query);
            list = q.getResultList();
            tx.commit();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
        return list;
    }

    public List<E> getObjectFromQuery(String query, Object... parametros) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        int position = 1;
        List<E> array = new ArrayList<E>();
        try {
            tx.begin();
            Query q = em.createQuery(query);
            for (Object parametro : parametros) {
                q.setParameter(position++, parametro);
            }
            tx.commit();
            array = q.getResultList();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
        return array;
    }

    public List<E> getObjectFromNamedQuery(String namedQuery, Object... parametros) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        int position = 1;
        List<E> array = new ArrayList<E>();
        try {
            tx.begin();
            Query q = em.createNamedQuery(namedQuery);
            for (Object parametro : parametros) {
                q.setParameter(position++, parametro);
            }
            tx.commit();
            array = q.getResultList();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
        return array;
    }

    public List<E> getObjectFromNamedQueryLimitedResult(int i, String namedQuery, Object... parametros) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        int position = 1;
        List<E> array = new ArrayList<E>();
        try {
            tx.begin();
            Query q = em.createNamedQuery(namedQuery);
            q.setMaxResults(i);
            for (Object parametro : parametros) {
                q.setParameter(position++, parametro);
            }
            tx.commit();
            array = q.getResultList();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
        return array;
    }

    public E update(E obj) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        E e = null;
        try {
            tx.begin();
            e = em.merge(obj);
            tx.commit();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
        return e;
    }

    public Integer executeQuery(String query, Object... parametros) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        int num = 0, position = 1;
        try {
            tx.begin();
            Query q = em.createQuery(query);
            for (Object parametro : parametros) {
                q.setParameter(position++, parametro);
            }
            num = q.executeUpdate();
            tx.commit();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
            return num;
        }
    }

    public E findById(Object id) {
        E ret = null;
        EntityManager em = getEntityManager();
        String query = "select o from " + clazz.getSimpleName() + " o where id = ?";
        List<E> lista = (List<E>) em.createQuery(query).setParameter(1, id).getResultList();
        if (lista.size() > 0) {
            ret = lista.get(0);
        }
        return ret;
    }
}
