package spamwatch.filter.address;

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
import spamwatch.filter.DataChangeListener.PART;
import spamwatch.filter.fromto.FromToFilterItem;

@Entity
@Table(name = "ADDRESS_FILTER_DATA")
public class AddressFilterData extends AbstractFilterData {

    private static Log log = LogFactory.getLog(AddressFilterData.class);

    @SuppressWarnings("unused")
    @Id
    @GeneratedValue
    private long id;

    @Column(name = "MAX_ITEMS")
    private int maxItems;

    @Transient
    private Session session;

    public static AddressFilterData loadFromDB() {
        Session session = DB.createSession();
        synchronized (session) {
            Transaction tx = session.beginTransaction();
            AddressFilterData data;
            try {
                Criteria criteria = session.createCriteria(AddressFilterData.class);
                data = (AddressFilterData) criteria.uniqueResult();
                if (data == null) {
                    data = new AddressFilterData();
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

    public AddressFilterData() {
        maxItems = 2500;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    @Transient
    private Query getQuery = null;

    public AddressFilterItem getItem(String string) {
        synchronized (session) {
            Transaction tx = session.beginTransaction();
            try {
                if (getQuery == null) {
                    getQuery = session.createQuery("from AddressFilterItem where searchString=?").setCacheable(false);
                }
                getQuery.setString(0, string);
                AddressFilterItem result = (AddressFilterItem) getQuery.uniqueResult();
                tx.commit();
                return result;
            } catch (HibernateException e) {
                log.warn("Exception in getItem: ", e);
                tx.rollback();
                return null;
            }
        }
    }

    public void addToItems(Map<String, Classification[]> learnCache) {
        log.debug("> addToItems() entered");
        Set<String> emails = learnCache.keySet();
        synchronized (session) {
            Transaction tx = session.beginTransaction();
            try {
                Query hql = session.createQuery("from AddressFilterItem where searchString=?").setCacheable(false);
                for (String email : emails) {
                    Classification[] values = learnCache.get(email);
                    hql.setString(0, email);
                    AddressFilterItem item = (AddressFilterItem) hql.uniqueResult();
                    if (item == null) {
                        item = new AddressFilterItem(email);
                        if (values[0] != null) {
                            item.setClassificationFrom(values[0]);
                        }
                        if (values[1] != null) {
                            item.setClassificationTo(values[1]);
                        }
                    } else {
                        if (values[0] != null && values[0] != item.getClassificationFrom()) {
                            item.setClassificationFrom(Classification.UNKNOWN);
                        }
                        if (values[1] != null && values[1] != item.getClassificationTo()) {
                            item.setClassificationTo(Classification.UNKNOWN);
                        }
                    }
                    item.incUsecount();
                    session.saveOrUpdate(item);
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

    @SuppressWarnings("unchecked")
    public void loadFromLegacyFile(File file) throws IOException {
        synchronized (session) {
            Transaction tx = session.beginTransaction();
            try {
                ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
                ArrayList<FromToFilterItem> newList = (ArrayList<FromToFilterItem>) in.readObject();
                in.close();
                session.createQuery("delete AddressFilterItem").setCacheable(false).executeUpdate();
                for (FromToFilterItem newItem : newList) {
                    AddressFilterItem item = new AddressFilterItem();
                    item.setClassificationFrom(newItem.getClassificationFrom());
                    item.setClassificationTo(newItem.getClassificationTo());
                    item.setSearchString(newItem.getSearchString());
                    session.save(item);
                }
                tx.commit();
            } catch (ClassNotFoundException e) {
                log.warn("Exception in loadFromLegacyFile: ", e);
                tx.rollback();
                throw new IOException(e);
            }
            session.flush();
            session.clear();
        }
        fireDataChange(PART.ITEMS);
    }

    public int getMaxItems() {
        return maxItems;
    }

    public void setMaxItems(int maxItems) {
        this.maxItems = maxItems;
        fireDataChange(PART.PARAMETERS);
    }

    protected void cleanMap() {
        synchronized (session) {
            List<Long> deleteList = new LinkedList<Long>();
            Transaction tx = session.beginTransaction();
            try {
                SQLQuery query = session.createSQLQuery("select id FROM ADDRESS_FILTER_ITEM");
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

    protected void fastDeleteItems(ArrayList<AddressFilterItem> deleteList) {
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
                    StringBuilder sb = new StringBuilder("delete from ADDRESS_FILTER_ITEM where id in (");
                    for (int i = 0; i < nextGroup; i++) {
                        sb.append(ids[currentIndex + i]).append(',');
                    }
                    sb.setCharAt(sb.length() - 1, ')');
                    Query query = session.createSQLQuery(sb.toString()).setCacheable(false);
                    query.executeUpdate();
                    currentIndex += nextGroup;
                }
                tx.commit();
                session.flush();
                session.clear();
            } catch (HibernateException e) {
                log.warn("Exception in fastDeleteItems: ", e);
                tx.rollback();
            }
        }
        fireDataChange(PART.ITEMS);
    }

    public Collection<?> getValues() {
        synchronized (session) {
            Transaction tx = session.beginTransaction();
            Criteria criteria = session.createCriteria(AddressFilterItem.class).setCacheable(false);
            List<?> list = criteria.list();
            tx.commit();
            return list;
        }
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
