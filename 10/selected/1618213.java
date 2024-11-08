package spamwatch.filter.links;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import spamwatch.base.DB;
import spamwatch.filter.AbstractFilterData;
import spamwatch.filter.Classification;
import spamwatch.filter.DataChangeListener;
import spamwatch.filter.DataChangeListener.PART;

@Entity
@Table(name = "LINKS_FILTER_DATA")
public class LinksFilterData extends AbstractFilterData {

    private static Log log = LogFactory.getLog(LinksFilterData.class);

    @SuppressWarnings("unused")
    @Id
    @GeneratedValue
    private long id;

    @Column(name = "MAX_ITEMS")
    private int maxItems;

    @Transient
    private Session session;

    protected static LinksFilterData loadFromDB() {
        Session session = DB.createSession();
        synchronized (session) {
            Transaction tx = session.beginTransaction();
            try {
                Criteria criteria = session.createCriteria(LinksFilterData.class);
                LinksFilterData data = (LinksFilterData) criteria.uniqueResult();
                if (data == null) {
                    data = new LinksFilterData();
                    session.save(data);
                }
                tx.commit();
                data.setSession(session);
                return data;
            } catch (HibernateException e) {
                log.warn("Exception in loadFromDB: ", e);
                tx.rollback();
                throw (e);
            }
        }
    }

    public LinksFilterData() {
        maxItems = 5000;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    @Transient
    private Query getQuery = null;

    public LinksFilterItem getItem(final String string) {
        synchronized (session) {
            Transaction tx = session.beginTransaction();
            try {
                if (getQuery == null) {
                    getQuery = session.createQuery("from LinksFilterItem where searchString=?").setCacheable(false);
                }
                getQuery.setString(0, string);
                LinksFilterItem result = (LinksFilterItem) getQuery.uniqueResult();
                tx.commit();
                return result;
            } catch (HibernateException e) {
                log.warn("Exception in getItem: ", e);
                tx.rollback();
                return null;
            }
        }
    }

    public void addToItems(Map<String, Classification> learnCache) {
        log.debug("> addToItems() entered");
        Set<String> words = learnCache.keySet();
        synchronized (session) {
            Transaction tx = session.beginTransaction();
            try {
                Query hql = session.createQuery("from LinksFilterItem where searchString=?").setCacheable(false);
                for (String word : words) {
                    Classification c = learnCache.get(word);
                    hql.setString(0, word);
                    LinksFilterItem item = (LinksFilterItem) hql.uniqueResult();
                    if (item == null) {
                        item = new LinksFilterItem(word);
                        item.setClassification(c);
                        session.save(item);
                    } else {
                        item.incUsecount();
                        if (c == Classification.UNKNOWN || c != item.getClassification()) {
                            item.setClassification(Classification.UNKNOWN);
                        }
                        session.update(item);
                    }
                }
                tx.commit();
            } catch (HibernateException e) {
                log.warn("Exception in addToItems: ", e);
                tx.rollback();
            }
            session.flush();
            session.clear();
        }
        log.trace("fireDataChange");
        fireDataChange(PART.ITEMS);
        log.debug("< addToItems() exited");
    }

    public Collection<?> getValues() {
        synchronized (session) {
            Transaction tx = session.beginTransaction();
            List<?> list;
            try {
                Criteria criteria = session.createCriteria(LinksFilterItem.class).setCacheable(false);
                list = criteria.list();
                tx.commit();
                return list;
            } catch (HibernateException e) {
                log.warn("Exception in getValues: ", e);
                tx.rollback();
                return null;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void loadFromLegacyFile(File file) throws IOException {
        synchronized (session) {
            ArrayList<LinksFilterItem> newList;
            try {
                ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
                newList = (ArrayList<LinksFilterItem>) in.readObject();
                in.close();
            } catch (ClassNotFoundException e) {
                throw new IOException(e);
            }
            Transaction tx = session.beginTransaction();
            try {
                session.createQuery("delete LinksFilterItem").setCacheable(false).executeUpdate();
                for (LinksFilterItem item : newList) {
                    session.save(item);
                }
                tx.commit();
            } catch (HibernateException e) {
                log.warn("Exception in loadFromLegacyFile: ", e);
                tx.rollback();
                throw new IOException(e);
            }
            session.flush();
            session.clear();
        }
        fireDataChange(PART.ITEMS);
    }

    public void setMaxItems(int maxItems) {
        this.maxItems = maxItems;
        fireDataChange(DataChangeListener.PART.PARAMETERS);
    }

    public int getMaxItems() {
        return maxItems;
    }

    protected void cleanMap() {
        synchronized (session) {
            List<Long> deleteList = new LinkedList<Long>();
            Transaction tx = session.beginTransaction();
            try {
                SQLQuery query = session.createSQLQuery("select id FROM LINKS_FILTER_ITEM");
                query.addScalar("id", Hibernate.LONG).setCacheable(false);
                List<?> list = query.list();
                if (list.size() > maxItems) {
                    double ratio = (maxItems * 0.75) / list.size();
                    for (Object object : list) {
                        Long l = (Long) object;
                        if (Math.random() > ratio) {
                            deleteList.add(l);
                        }
                    }
                }
                tx.commit();
            } catch (HibernateException e) {
                log.warn("Exception in cleanMap: ", e);
                tx.rollback();
            }
            if (deleteList.size() > 0) {
                long[] ids = new long[deleteList.size()];
                int index = 0;
                for (Long l : deleteList) {
                    ids[index] = l;
                    index++;
                }
                fastDeleteItemsByID(ids);
            }
        }
        fireDataChange(PART.ITEMS);
    }

    protected void fastDeleteItems(List<LinksFilterItem> deleteList) {
        if (deleteList == null || deleteList.size() == 0) {
            return;
        }
        long[] ids = new long[deleteList.size()];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = deleteList.get(i).getId();
        }
        fastDeleteItemsByID(ids);
    }

    protected void fastDeleteItemsByID(long[] ids) {
        if (ids == null || ids.length == 0) {
            return;
        }
        synchronized (session) {
            Transaction tx = session.beginTransaction();
            try {
                int currentIndex = 0;
                while (currentIndex < ids.length) {
                    int nextGroup = Math.min(ids.length - currentIndex, 1000);
                    StringBuilder sb = new StringBuilder("delete from Links_Filter_Item where id in (");
                    for (int i = 0; i < nextGroup; i++) {
                        sb.append(ids[currentIndex + i]).append(',');
                    }
                    sb.setCharAt(sb.length() - 1, ')');
                    Query query = session.createSQLQuery(sb.toString()).setCacheable(false);
                    query.executeUpdate();
                    currentIndex += nextGroup;
                }
                tx.commit();
            } catch (HibernateException e) {
                log.warn("Exception in fastDeleteItemsByID: ", e);
                tx.rollback();
            }
            session.flush();
            session.clear();
        }
        fireDataChange(PART.ITEMS);
    }

    protected void save() {
        synchronized (session) {
            Transaction tx = session.beginTransaction();
            try {
                session.update(this);
                tx.commit();
            } catch (HibernateException e) {
                log.warn("Exception in save: ", e);
                tx.rollback();
            }
        }
    }
}
