package org.dasein.persist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import org.dasein.util.Jiterator;
import org.dasein.persist.jdbc.Counter;
import org.dasein.persist.jdbc.Creator;
import org.dasein.persist.jdbc.Deleter;
import org.dasein.persist.jdbc.Loader;
import org.dasein.persist.jdbc.Updater;
import org.dasein.persist.jdbc.AutomatedSql.Operator;
import org.dasein.persist.jdbc.AutomatedSql.TranslationMethod;
import org.dasein.util.CacheLoader;
import org.dasein.util.CachedItem;
import org.dasein.util.CacheManagementException;
import org.dasein.util.JitCollection;
import org.dasein.util.JiteratorFilter;

/**
 * A relational cache that will periodically dump its cache.  This is useful for managed objects that are created and then
 * either never accessed again or very very rarely accessed again (Valtira's session tracking system would be an ideal
 * candidate).
 *
 * @author morgan
 *
 * @param <T>
 */
public final class RelationalReleaseCache<T extends CachedItem> extends PersistentCache<T> {

    public static final Logger logger = Logger.getLogger(RelationalReleaseCache.class);

    public static class OrderedColumn {

        public String column;

        public boolean descending = false;
    }

    private String readDataSource = null;

    private TranslationMethod translationMethod = TranslationMethod.NONE;

    private String writeDataSource = null;

    public RelationalReleaseCache() {
        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    while (true) {
                        sleep(60 * 60 * 1000);
                        getCache().releaseAll();
                    }
                } catch (InterruptedException e) {
                }
            }
        };
        t.setDaemon(false);
        t.setName("DASEIN RELATIONAL CACHE FLUSHER");
        t.start();
    }

    /**
     * Constructs a new persistent factory for objects of the specified class with
     * the named unique identifier attributes.
     * @param cls the class of objects managed by this factory
     * @param keys a list of unique identifiers for instances of the specified class
     */
    protected void init(Class<T> cls, Key... keys) {
        readDataSource = Execution.getDataSourceName(cls.getName(), true);
        writeDataSource = Execution.getDataSourceName(cls.getName(), false);
        if (readDataSource == null) {
            readDataSource = writeDataSource;
        }
        if (writeDataSource == null) {
            writeDataSource = readDataSource;
        }
    }

    private Counter getCounter(SearchTerm[] whereTerms) {
        final SearchTerm[] terms = whereTerms;
        final RelationalReleaseCache<T> self = this;
        Counter counter = new Counter() {

            public void init() {
                setTarget(self.getEntityClassName());
                if (terms != null && terms.length > 0) {
                    ArrayList<Criterion> criteria = new ArrayList<Criterion>();
                    for (SearchTerm term : terms) {
                        criteria.add(new Criterion(term.getColumn(), term.getOperator()));
                    }
                    setCriteria(criteria.toArray(new Criterion[criteria.size()]));
                }
            }

            public boolean isReadOnly() {
                return true;
            }
        };
        return counter;
    }

    private Creator getCreator() {
        final RelationalReleaseCache<T> self = this;
        Creator creator = new Creator() {

            public void init() {
                setTarget(self.getEntityClassName());
                switch(translationMethod) {
                    case CUSTOM:
                        setCustomTranslating();
                        break;
                    case STANDARD:
                        setTranslating(true);
                        break;
                    case NONE:
                        setTranslating(false);
                        break;
                }
            }

            public boolean isReadOnly() {
                return false;
            }
        };
        return creator;
    }

    private Deleter getDeleter(SearchTerm... terms) {
        final SearchTerm[] killTerms = terms;
        final RelationalReleaseCache<T> self = this;
        Deleter deleter = new Deleter() {

            public void init() {
                setTarget(self.getEntityClassName());
                if (killTerms != null && killTerms.length > 0) {
                    ArrayList<Criterion> criteria = new ArrayList<Criterion>();
                    for (SearchTerm term : killTerms) {
                        criteria.add(new Criterion(term.getJoinEntity(), term.getColumn(), term.getOperator()));
                    }
                    setCriteria(criteria.toArray(new Criterion[criteria.size()]));
                } else {
                    setCriteria(self.getPrimaryKey().getFields());
                }
                switch(translationMethod) {
                    case CUSTOM:
                        setCustomTranslating();
                        break;
                    case STANDARD:
                        setTranslating(true);
                        break;
                    case NONE:
                        setTranslating(false);
                        break;
                }
            }

            public boolean isReadOnly() {
                return false;
            }
        };
        return deleter;
    }

    private Loader getLoader(SearchTerm[] whereTerms, OrderedColumn[] orderBy) {
        final SearchTerm[] terms = whereTerms;
        final OrderedColumn[] order = orderBy;
        final RelationalReleaseCache<T> self = this;
        Loader loader = new Loader() {

            public void init() {
                setTarget(self.getEntityClassName());
                setEntityJoins(getJoins());
                if (terms != null && terms.length > 0) {
                    ArrayList<Criterion> criteria = new ArrayList<Criterion>();
                    for (SearchTerm term : terms) {
                        criteria.add(new Criterion(term.getJoinEntity(), term.getColumn(), term.getOperator()));
                    }
                    setCriteria(criteria.toArray(new Criterion[criteria.size()]));
                }
                if (order != null && order.length > 0) {
                    ArrayList<String> cols = new ArrayList<String>();
                    boolean desc = order[0].descending;
                    for (OrderedColumn col : order) {
                        cols.add(col.column);
                    }
                    setOrder(desc, cols.toArray(new String[cols.size()]));
                }
                switch(translationMethod) {
                    case CUSTOM:
                        setCustomTranslating();
                        break;
                    case STANDARD:
                        setTranslating(true);
                        break;
                    case NONE:
                        setTranslating(false);
                        break;
                }
            }

            public boolean isReadOnly() {
                return true;
            }
        };
        return loader;
    }

    private Updater getUpdater() {
        final RelationalReleaseCache<T> self = this;
        Updater updater = new Updater() {

            public void init() {
                setTarget(self.getEntityClassName());
                setCriteria(self.getPrimaryKey().getFields());
                switch(translationMethod) {
                    case CUSTOM:
                        setCustomTranslating();
                        break;
                    case STANDARD:
                        setTranslating(true);
                        break;
                    case NONE:
                        setTranslating(false);
                        break;
                }
            }

            public boolean isReadOnly() {
                return false;
            }
        };
        return updater;
    }

    /**
     * Counts the total number of objects governed by this factory in the database.
     * @return the number of objects in the database
     * @throws PersistenceException an error occurred counting the elements in the database
     */
    @Override
    public long count() throws PersistenceException {
        logger.debug("enter - count()");
        try {
            Transaction xaction = Transaction.getInstance(true);
            Counter counter = getCounter(null);
            try {
                Map<String, Object> results;
                long count;
                results = xaction.execute(counter, new HashMap<String, Object>(), readDataSource);
                count = ((Number) results.get("count")).longValue();
                xaction.commit();
                return count;
            } finally {
                xaction.rollback();
            }
        } finally {
            logger.debug("exit - count()");
        }
    }

    @Override
    public long count(SearchTerm... terms) throws PersistenceException {
        logger.debug("enter - count(SearchTerm...)");
        try {
            Transaction xaction = Transaction.getInstance(true);
            Counter counter = getCounter(terms);
            try {
                Map<String, Object> params = toParams(terms);
                Map<String, Object> results;
                long count;
                results = xaction.execute(counter, params, readDataSource);
                count = ((Number) results.get("count")).longValue();
                xaction.commit();
                return count;
            } finally {
                xaction.rollback();
            }
        } finally {
            logger.debug("exit - count(SearchTerm...)");
        }
    }

    /**
     * Creates the specified object with the data provided in the specified state under
     * the governance of the specified transaction.
     * @param xaction the transaction governing this event
     * @param state the new state for the new object
     * @throws PersistenceException an error occurred talking to the data store, or
     * creates are not supported
     */
    @Override
    public T create(Transaction xaction, Map<String, Object> state) throws PersistenceException {
        state.put("--key--", getPrimaryKey().getFields()[0]);
        xaction.execute(getCreator(), state, writeDataSource);
        return getCache().find(state);
    }

    @Override
    public Collection<T> find(SearchTerm[] terms, JiteratorFilter<T> filter, Boolean orderDesc, String... orderFields) throws PersistenceException {
        logger.debug("enter - find(SearchTerm[], JiteratorFilter, Boolean, String)");
        try {
            OrderedColumn[] order;
            if (orderFields == null) {
                order = new OrderedColumn[0];
            } else {
                int i = 0;
                order = new OrderedColumn[orderFields.length];
                for (String field : orderFields) {
                    order[i] = new OrderedColumn();
                    order[i].column = field;
                    order[i].descending = (orderDesc != null && orderDesc);
                    i++;
                }
            }
            return this.load(getLoader(terms, order), filter, toParams(terms));
        } finally {
            logger.debug("exit - find(SearchTerm[], JiteratorFilter, Boolean, String...)");
        }
    }

    /**
     * Retrieves the object uniquely identified by the value for the specified ID field.
     * @param primaryKeyValue the ID field identifying the object
     * @return the object matching the query criterion
     * @throws PersistenceException an error occurred talking to the data store
     */
    @Override
    public T get(Object primaryKeyValue) throws PersistenceException {
        try {
            CacheLoader<T> loader;
            loader = new CacheLoader<T>() {

                public T load(Object... args) {
                    SearchTerm[] terms = new SearchTerm[1];
                    Collection<T> list;
                    terms[0] = new SearchTerm((String) args[0], Operator.EQUALS, args[1]);
                    try {
                        list = RelationalReleaseCache.this.load(getLoader(terms, null), null, toParams(terms));
                    } catch (PersistenceException e) {
                        try {
                            try {
                                Thread.sleep(1000L);
                            } catch (InterruptedException ignore) {
                            }
                            list = RelationalReleaseCache.this.load(getLoader(terms, null), null, toParams(terms));
                        } catch (Throwable forgetIt) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                    }
                    if (list.isEmpty()) {
                        return null;
                    }
                    return list.iterator().next();
                }
            };
            logger.debug("Executing cache find...");
            try {
                return getCache().find(getPrimaryKeyField(), primaryKeyValue, loader, getPrimaryKeyField(), primaryKeyValue);
            } catch (CacheManagementException e) {
                throw new PersistenceException(e);
            } catch (RuntimeException e) {
                Throwable t = e.getCause();
                if (t != null && t instanceof PersistenceException) {
                    throw (PersistenceException) t;
                }
                if (logger.isDebugEnabled()) {
                    e.printStackTrace();
                }
                throw new PersistenceException(e);
            } finally {
                logger.debug("Executed.");
            }
        } finally {
            logger.debug("exit - get(String,Object)");
        }
    }

    /**
     * Loads all elements of this class from the data store. Use this method only when you know
     * exactly what you are doing. Otherwise, you will pull a lot of data.
     * @return all objects from the database
     * @throws PersistenceException an error occurred executing the query
     */
    @Override
    public Collection<T> list() throws PersistenceException {
        logger.debug("enter - list()");
        try {
            return find(null, null, false);
        } finally {
            logger.debug("exit - list()");
        }
    }

    private Map<String, Object> toParams(SearchTerm... searchTerms) {
        HashMap<String, Object> params = new HashMap<String, Object>();
        if (searchTerms != null) {
            for (SearchTerm term : searchTerms) {
                params.put(term.getColumn(), term.getValue());
            }
        }
        return params;
    }

    @SuppressWarnings("unchecked")
    private Collection<T> load(Loader loader, JiteratorFilter<T> filter, Map<String, Object> params) throws PersistenceException {
        logger.debug("enter - load(Class,SearchTerm...)");
        try {
            Transaction xaction = Transaction.getInstance(true);
            final Jiterator<T> it = new Jiterator<T>(filter);
            params.put("--key--", getPrimaryKey().getFields()[0]);
            try {
                final Map<String, Object> results;
                results = xaction.execute(loader, params, readDataSource);
                xaction.commit();
                Thread t = new Thread() {

                    public void run() {
                        try {
                            for (Map<String, Object> map : (Collection<Map<String, Object>>) results.get(Loader.LISTING)) {
                                it.push(getCache().find(map));
                            }
                            it.complete();
                        } catch (Exception e) {
                            it.setLoadException(e);
                        } catch (Throwable t) {
                            it.setLoadException(new RuntimeException(t));
                        }
                    }
                };
                t.setDaemon(true);
                t.setName("Loader");
                t.start();
                return new JitCollection<T>(it, getEntityClassName());
            } catch (PersistenceException e) {
                it.setLoadException(e);
                throw e;
            } catch (RuntimeException e) {
                it.setLoadException(e);
                throw e;
            } catch (Throwable t) {
                RuntimeException e = new RuntimeException(t);
                it.setLoadException(e);
                throw e;
            } finally {
                xaction.rollback();
            }
        } finally {
            logger.debug("exit - load(Class,Map)");
        }
    }

    /**
     * Removes the specified item from the system permanently.
     * @param xaction the transaction under which this event is occurring
     * @param item the item to be removed
     * @throws PersistenceException an error occurred talking to the data store or
     * removal of these objects is prohibited
     */
    @Override
    public void remove(Transaction xaction, T item) throws PersistenceException {
        xaction.execute(getDeleter(), getCache().getKeys(item), writeDataSource);
        getCache().release(item);
    }

    @Override
    public void remove(Transaction xaction, SearchTerm... terms) throws PersistenceException {
        xaction.execute(getDeleter(), toParams(terms), writeDataSource);
    }

    public String toString() {
        return getCache().toString();
    }

    /**
     * Updates the specified object with the data provided in the specified state under
     * the governance of the specified transaction.
     * @param xaction the transaction governing this event
     * @param item the item to be updated
     * @param state the new state for the updated object
     * @throws PersistenceException an error occurred talking to the data store, or
     * updates are not supported
     */
    @Override
    public void update(Transaction xaction, T item, Map<String, Object> state) throws PersistenceException {
        state.put("--key--", getPrimaryKey().getFields()[0]);
        xaction.execute(getUpdater(), state, writeDataSource);
    }
}
