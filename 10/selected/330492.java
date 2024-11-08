package spamwatch.filter.learning;

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
import spamwatch.filter.DataChangeListener.PART;
import spamwatch.util.cache.Cache;
import spamwatch.util.cache.CacheReorganisationListener;

@Entity
@Table(name = "LEARNING_FILTER_DATA")
public class LearningFilterData extends AbstractFilterData {

    private static Log log = LogFactory.getLog(LearningFilterData.class);

    @SuppressWarnings("unused")
    @Id
    @GeneratedValue
    private long id;

    @Column(name = "MAX_WORD_LENGTH")
    private int maxWordLength;

    @Column(name = "MAX_ITEMS")
    private int maxItems;

    @Transient
    private int maxHamOccurences;

    @Transient
    private int maxSpamOccurences;

    @Transient
    private Session session;

    @Transient
    private Cache<String, LearningFilterItem> cache;

    @Transient
    private Query getQuery = null;

    protected static LearningFilterData loadFromDB() {
        Session session = DB.createSession();
        synchronized (session) {
            Transaction tx = session.beginTransaction();
            try {
                Criteria criteria = session.createCriteria(LearningFilterData.class);
                LearningFilterData data = (LearningFilterData) criteria.uniqueResult();
                if (data == null) {
                    data = new LearningFilterData();
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

    public LearningFilterData() {
        maxHamOccurences = -1;
        maxSpamOccurences = -1;
        maxWordLength = 20;
        maxItems = 20000;
    }

    public void setSession(Session session) {
        synchronized (session) {
            this.session = session;
            cache = new Cache<String, LearningFilterItem>(getMaxItems() / 10, getMaxItems() / 20, Cache.Strategy.REMOVE_OLDEST_LAST_USED, -1);
            cache.addCacheReorganisationListener(new CacheReorganisationListener<String, LearningFilterItem>() {

                @Override
                public boolean cacheRemoveRequest(Cache<String, LearningFilterItem> cache, String key, Reason reason) {
                    return true;
                }

                @Override
                public void reorganisationFinished(Cache<String, LearningFilterItem> cache, Reason reason) {
                    if (log.isDebugEnabled()) {
                        log.debug("Cache reorganized. Reason: " + reason);
                        log.debug("Cache statistics:\n" + cache.getStatistics());
                    }
                }
            });
            getQuery = session.createQuery("from LearningFilterItem where searchString=?").setCacheable(false);
            log.trace("Start cache prefill");
            Query prefillQuery = session.createQuery("from LearningFilterItem item order by (item.spamOccurrences + item.hamOccurrences)").setCacheable(false);
            log.trace("Query for max " + (getMaxItems() / 20) + " items");
            prefillQuery.setMaxResults(getMaxItems() / 20);
            List<?> list = prefillQuery.list();
            log.trace("Got " + list.size() + " items");
            for (Object object : list) {
                LearningFilterItem item = (LearningFilterItem) object;
                cache.put(item.getSearchString(), item);
            }
            log.trace("End cache prefill");
        }
    }

    public int getMaxHamOccurences() {
        if (maxHamOccurences < 0) {
            synchronized (session) {
                Integer result = (Integer) session.createSQLQuery("SELECT MAX(HAM_OCCURRENCES) FROM LEARNING_FILTER_ITEM").setCacheable(false).uniqueResult();
                if (result == null) {
                    maxHamOccurences = 0;
                } else {
                    maxHamOccurences = result.intValue();
                }
            }
        }
        return maxHamOccurences;
    }

    public int getMaxSpamOccurences() {
        if (maxSpamOccurences < 0) {
            synchronized (session) {
                Transaction tx = session.beginTransaction();
                try {
                    Integer result = (Integer) session.createSQLQuery("SELECT MAX(SPAM_OCCURRENCES) FROM LEARNING_FILTER_ITEM").setCacheable(false).uniqueResult();
                    if (result == null) {
                        maxSpamOccurences = 0;
                    } else {
                        maxSpamOccurences = result.intValue();
                    }
                    tx.commit();
                } catch (HibernateException e) {
                    log.warn("Exception in getMaxSpamOccurences: ", e);
                    tx.rollback();
                    maxSpamOccurences = 0;
                }
            }
        }
        return maxSpamOccurences;
    }

    public LearningFilterItem getItem(String string) {
        synchronized (session) {
            Object cacheEntry = cache.getWithNullAllowed(string);
            if (cacheEntry != Cache.CACHE_MISS) {
                if (log.isDebugEnabled()) log.trace("found in cache: " + string);
                return (LearningFilterItem) cacheEntry;
            } else {
                if (log.isDebugEnabled()) log.trace("get from DB: " + string);
                Transaction tx = session.beginTransaction();
                try {
                    getQuery.setString(0, string);
                    LearningFilterItem item = (LearningFilterItem) getQuery.uniqueResult();
                    cache.put(string, item);
                    tx.commit();
                    return item;
                } catch (HibernateException e) {
                    log.warn("Exception in getItem: ", e);
                    tx.rollback();
                    return null;
                }
            }
        }
    }

    private LearningFilterItem getItemNoTransaction(String string) {
        synchronized (session) {
            Object cacheEntry = cache.getWithNullAllowed(string);
            if (cacheEntry != Cache.CACHE_MISS) {
                if (log.isDebugEnabled()) log.trace("found in cache: " + string);
                return (LearningFilterItem) cacheEntry;
            } else {
                if (log.isDebugEnabled()) log.trace("get from DB: " + string);
                try {
                    getQuery.setString(0, string);
                    LearningFilterItem item = (LearningFilterItem) getQuery.uniqueResult();
                    cache.put(string, item);
                    return item;
                } catch (HibernateException e) {
                    log.warn("Exception in getItemNoTransaction: ", e);
                    return null;
                }
            }
        }
    }

    public void addToItems(Map<String, int[]> learnCache) {
        log.debug("> addToItems() entered");
        Set<String> words = learnCache.keySet();
        synchronized (session) {
            Transaction tx = session.beginTransaction();
            try {
                for (String word : words) {
                    if (word.length() > getMaxWordLength()) {
                        continue;
                    }
                    LearningFilterItem item = getItemNoTransaction(word);
                    if (item == null) {
                        item = new LearningFilterItem(word);
                    }
                    int[] values = learnCache.get(word);
                    item.incHamOccurrences(values[0]);
                    item.incSpamOccurrences(values[1]);
                    session.saveOrUpdate(item);
                }
                tx.commit();
            } catch (HibernateException e) {
                log.warn("Exception in addToItems: ", e);
                tx.rollback();
            }
            session.flush();
            session.clear();
            maxHamOccurences = -1;
            maxSpamOccurences = -1;
            cache.resetCache();
        }
        log.trace("fireDataChange");
        fireDataChange(PART.ITEMS);
        log.debug("< addToItems() exited");
    }

    protected void cleanMap() {
        synchronized (session) {
            Transaction tx = session.beginTransaction();
            try {
                session.createQuery("delete LearningFilterItem where length(searchString)>" + maxWordLength).setCacheable(false).executeUpdate();
                tx.commit();
            } catch (HibernateException e) {
                log.warn("Exception in cleanMap: ", e);
                tx.rollback();
            }
            List<Long> deleteList = new LinkedList<Long>();
            tx = session.beginTransaction();
            try {
                SQLQuery query = session.createSQLQuery("select id FROM LEARNING_FILTER_ITEM");
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

    protected void fastDeleteItems(ArrayList<LearningFilterItem> deleteList) {
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
        log.debug("fastDeleteItemsByID entered");
        if (ids == null || ids.length == 0) {
            log.debug("fastDeleteItemsByID exited (list empty)");
            return;
        }
        synchronized (session) {
            log.debug("start delete " + ids.length + " items");
            Transaction tx = session.beginTransaction();
            try {
                int currentIndex = 0;
                while (currentIndex < ids.length) {
                    int nextGroup = Math.min(ids.length - currentIndex, 1000);
                    StringBuilder sb = new StringBuilder("delete from Learning_Filter_Item where id in (");
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
                log.warn("Exception in fastDeleteItems: ", e);
                tx.rollback();
            }
            log.debug("delete finished");
            maxHamOccurences = -1;
            maxSpamOccurences = -1;
            session.flush();
            session.clear();
            cache.resetCache();
        }
        fireDataChange(PART.ITEMS);
        log.debug("fastDeleteItemsByID exited");
    }

    public void setMaxWordLength(int number) {
        this.maxWordLength = number;
        fireDataChange(PART.PARAMETERS);
    }

    public int getMaxWordLength() {
        return maxWordLength;
    }

    public void setMaxItems(int maxItems) {
        this.maxItems = maxItems;
        fireDataChange(PART.PARAMETERS);
    }

    public int getMaxItems() {
        return maxItems;
    }

    @SuppressWarnings("unchecked")
    public void loadFromLegacyFile(File file) throws IOException {
        synchronized (session) {
            ArrayList<LearningFilterItem> newList;
            try {
                ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
                newList = (ArrayList<LearningFilterItem>) in.readObject();
                in.close();
            } catch (ClassNotFoundException e) {
                throw new IOException(e);
            }
            Transaction tx = session.beginTransaction();
            try {
                session.createQuery("delete LearningFilterItem").setCacheable(false).executeUpdate();
                cache.resetCache();
                for (LearningFilterItem item : newList) {
                    session.save(item);
                }
                maxHamOccurences = -1;
                maxSpamOccurences = -1;
                tx.commit();
            } catch (Exception e) {
                log.warn("Exception in loadFromLagacyFile: ", e);
                tx.rollback();
                throw new IOException(e);
            }
            session.flush();
            session.clear();
        }
        fireDataChange(PART.ITEMS);
    }

    public Collection<?> getValues() {
        synchronized (session) {
            Transaction tx = session.beginTransaction();
            Criteria criteria = session.createCriteria(LearningFilterItem.class).setCacheable(false);
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
