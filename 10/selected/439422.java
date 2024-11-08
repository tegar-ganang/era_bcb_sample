package desview.model.dao;

import desview.model.entities.Reading;
import desview.model.entities.Task;
import desview.util.persistence.HibernateUtil;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Transaction;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;

/**
 * Class DAO for reading entity.
 * @author Diones Rossetto.
 * @author Luiz Mello.
 * @since 08/04/2010.
 * @version 1.0
 */
@SuppressWarnings("rawtypes")
public class ReadingDAO implements DAO {

    /**
     * Default constructor of class.
     */
    public ReadingDAO() {
    }

    @Override
    public SessionFactory getSession() {
        return HibernateUtil.getSessionFactory();
    }

    @Override
    public void delete(Object reading) {
        Session sessao = getSession().getCurrentSession();
        Transaction transacao = sessao.beginTransaction();
        try {
            sessao.delete(reading);
            transacao.commit();
        } catch (Exception e) {
            e.printStackTrace();
            transacao.rollback();
        }
    }

    public void delete(long id) {
        Session sessao = getSession().getCurrentSession();
        Transaction transacao = sessao.beginTransaction();
        try {
            sessao.createQuery("delete Reading where id = :id").setParameter("id", new Long(id)).executeUpdate();
            transacao.commit();
        } catch (Exception e) {
            e.printStackTrace();
            transacao.rollback();
        }
    }

    @Override
    public boolean saveOrUpdate(Object reading) {
        Session sessao = getSession().getCurrentSession();
        Transaction transacao = sessao.beginTransaction();
        try {
            sessao.saveOrUpdate(reading);
            transacao.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            transacao.rollback();
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Reading> getAll() {
        Session sessao = getSession().getCurrentSession();
        Transaction transacao = sessao.beginTransaction();
        List<Reading> lista = new ArrayList<Reading>();
        try {
            lista = sessao.createCriteria(Reading.class).list();
            transacao.commit();
        } catch (Exception e) {
            e.printStackTrace();
            transacao.rollback();
        }
        return lista;
    }

    @Override
    public Object findById(Long id) {
        Session sessao = getSession().getCurrentSession();
        Transaction transacao = sessao.beginTransaction();
        Object leitura = null;
        try {
            leitura = sessao.get(Reading.class, id);
            transacao.commit();
        } catch (Exception e) {
            e.printStackTrace();
            transacao.rollback();
        }
        return leitura;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Reading> get() {
        Session sessao = getSession().getCurrentSession();
        Transaction transacao = sessao.beginTransaction();
        List<Reading> resultados = null;
        try {
            Query query = sessao.createQuery("from Reading r");
            resultados = query.list();
            transacao.commit();
        } catch (Exception e) {
            e.printStackTrace();
            transacao.rollback();
        }
        return resultados;
    }

    @Override
    public Integer count() {
        List lista = get();
        Integer i = new Integer(lista.size());
        return i.intValue();
    }

    @Override
    public void finalizeSession() {
        Session sessao = getSession().getCurrentSession();
        if (sessao.isOpen()) {
            sessao.close();
        }
    }

    public List getDistinct() {
        Session sessao = getSession().getCurrentSession();
        Transaction transacao = sessao.beginTransaction();
        List resultados = new ArrayList<Object>();
        try {
            Criteria criteria = sessao.createCriteria(Reading.class);
            criteria.setProjection(Projections.distinct(Projections.projectionList().add(Projections.property("oid")).add(Projections.property("variableName")).add(Projections.property("task"))));
            resultados = criteria.list();
            transacao.commit();
        } catch (Exception e) {
            e.printStackTrace();
            transacao.rollback();
        }
        return resultados;
    }

    @SuppressWarnings("unchecked")
    public List<Reading> getValuesToRT() {
        Session sessao = getSession().getCurrentSession();
        Transaction transacao = sessao.beginTransaction();
        StringBuilder sql = new StringBuilder();
        sql.append("select distinct task_id, oid, variable_name from reading");
        return sessao.createSQLQuery(sql.toString()).list();
    }

    @SuppressWarnings("unchecked")
    public List<Reading> getReadingByParameters(String oid, Task task) {
        Session sessao = getSession().getCurrentSession();
        Transaction transacao = sessao.beginTransaction();
        List<Reading> resultados = new ArrayList<Reading>();
        try {
            Query q = sessao.getNamedQuery("Reading.findDistinct");
            q.setParameter("oid", oid);
            q.setParameter("task", task);
            resultados = q.list();
            transacao.commit();
        } catch (RuntimeException r) {
            r.printStackTrace();
        }
        return resultados;
    }
}
