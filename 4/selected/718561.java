package asbk.pom.services;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import asbk.utils.HibernateUtil;
import com.googlecode.s2hibernate.struts2.plugin.annotations.TransactionTarget;

@SuppressWarnings("unchecked")
public abstract class AbstractService<C, I extends Serializable> extends HibernateUtil {

    private static final Log log = LogFactory.getLog(AbstractService.class);

    Class<C> entityClass;

    public Session sess() {
        return getSessionFactory().getCurrentSession();
    }

    @TransactionTarget
    protected Transaction hTransaction;

    {
        entityClass = (Class<C>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    public List<C> getAll() {
        try {
            return sess().createCriteria(entityClass).list();
        } catch (HibernateException e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    public C get(I id) {
        try {
            return (C) sess().get(entityClass, id);
        } catch (HibernateException e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    public void save(C object) {
        try {
            sess().save(object);
        } catch (HibernateException e) {
            hTransaction.rollback();
            log.error(e.getMessage());
            log.error("Be sure your Database is in read-write mode!");
            throw e;
        }
    }

    public void update(C object) {
        try {
            sess().update(object);
        } catch (HibernateException e) {
            hTransaction.rollback();
            log.error(e.getMessage());
            log.error("Be sure your Database is in read-write mode!");
            throw e;
        }
    }

    public void delete(I id) {
        try {
            C actual = get(id);
            sess().delete(actual);
        } catch (HibernateException e) {
            hTransaction.rollback();
            log.error(e.getMessage());
            log.error("Be sure your Database is in read-write mode!");
            throw e;
        }
    }
}
