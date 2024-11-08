package org.opennms.netmgt.importer.operations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Category;
import org.opennms.core.utils.ThreadCategory;
import org.opennms.netmgt.dao.OnmsDao;
import org.opennms.netmgt.eventd.EventIpcManager;
import org.opennms.netmgt.xml.event.Event;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * This nodes job is to tracks nodes that need to be deleted, added, or changed
 * @author david
 *
 */
public class ImportOperationsManager {

    private List<ImportOperation> m_inserts = new LinkedList<ImportOperation>();

    private List<ImportOperation> m_updates = new LinkedList<ImportOperation>();

    private Map<String, Integer> m_foreignIdToNodeMap;

    private ImportOperationFactory m_operationFactory;

    private ImportStatistics m_stats = new DefaultImportStatistics();

    private EventIpcManager m_eventMgr;

    private int m_scanThreads = 50;

    private int m_writeThreads = 4;

    private String m_foreignSource;

    public ImportOperationsManager(Map<String, Integer> foreignIdToNodeMap, ImportOperationFactory operationFactory) {
        m_foreignIdToNodeMap = new HashMap<String, Integer>(foreignIdToNodeMap);
        m_operationFactory = operationFactory;
    }

    public SaveOrUpdateOperation foundNode(String foreignId, String nodeLabel, String building, String city) {
        if (nodeExists(foreignId)) {
            return updateNode(foreignId, nodeLabel, building, city);
        } else {
            return insertNode(foreignId, nodeLabel, building, city);
        }
    }

    private boolean nodeExists(String foreignId) {
        return m_foreignIdToNodeMap.containsKey(foreignId);
    }

    private SaveOrUpdateOperation insertNode(String foreignId, String nodeLabel, String building, String city) {
        InsertOperation insertOperation = m_operationFactory.createInsertOperation(getForeignSource(), foreignId, nodeLabel, building, city);
        m_inserts.add(insertOperation);
        return insertOperation;
    }

    private SaveOrUpdateOperation updateNode(String foreignId, String nodeLabel, String building, String city) {
        Integer nodeId = processForeignId(foreignId);
        UpdateOperation updateOperation = m_operationFactory.createUpdateOperation(nodeId, getForeignSource(), foreignId, nodeLabel, building, city);
        m_updates.add(updateOperation);
        return updateOperation;
    }

    /**
     * Return NodeId and remove it from the Map so we know which nodes have been operated on thereby
     * tracking nodes to be deleted.
     * @param foreignId
     * @return a nodeId
     */
    private Integer processForeignId(String foreignId) {
        return m_foreignIdToNodeMap.remove(foreignId);
    }

    public int getOperationCount() {
        return m_inserts.size() + m_updates.size() + m_foreignIdToNodeMap.size();
    }

    public int getInsertCount() {
        return m_inserts.size();
    }

    public int getUpdateCount() {
        return m_updates.size();
    }

    public int getDeleteCount() {
        return m_foreignIdToNodeMap.size();
    }

    class DeleteIterator implements Iterator<ImportOperation> {

        private Iterator<Entry<String, Integer>> m_foreignIdIterator = m_foreignIdToNodeMap.entrySet().iterator();

        public boolean hasNext() {
            return m_foreignIdIterator.hasNext();
        }

        public ImportOperation next() {
            Entry<String, Integer> entry = m_foreignIdIterator.next();
            Integer nodeId = entry.getValue();
            String foreignId = entry.getKey();
            return m_operationFactory.createDeleteOperation(nodeId, m_foreignSource, foreignId);
        }

        public void remove() {
            m_foreignIdIterator.remove();
        }
    }

    class OperationIterator implements Iterator<ImportOperation> {

        Iterator<Iterator<ImportOperation>> m_iterIter;

        Iterator<ImportOperation> m_currentIter;

        OperationIterator() {
            List<Iterator<ImportOperation>> iters = new ArrayList<Iterator<ImportOperation>>(3);
            iters.add(new DeleteIterator());
            iters.add(m_updates.iterator());
            iters.add(m_inserts.iterator());
            m_iterIter = iters.iterator();
        }

        public boolean hasNext() {
            while ((m_currentIter == null || !m_currentIter.hasNext()) && m_iterIter.hasNext()) {
                m_currentIter = m_iterIter.next();
                m_iterIter.remove();
            }
            return (m_currentIter == null ? false : m_currentIter.hasNext());
        }

        public ImportOperation next() {
            return m_currentIter.next();
        }

        public void remove() {
            m_currentIter.remove();
        }
    }

    public void shutdownAndWaitForCompletion(ExecutorService executorService, String msg) {
        executorService.shutdown();
        try {
            while (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
            }
        } catch (InterruptedException e) {
            log().error(msg, e);
        }
    }

    public void persistOperations(TransactionTemplate template, OnmsDao<?, ?> dao) {
        m_stats.beginProcessingOps();
        m_stats.setDeleteCount(getDeleteCount());
        m_stats.setInsertCount(getInsertCount());
        m_stats.setUpdateCount(getUpdateCount());
        ExecutorService dbPool = Executors.newFixedThreadPool(m_writeThreads);
        preprocessOperations(template, dao, new OperationIterator(), dbPool);
        shutdownAndWaitForCompletion(dbPool, "persister interrupted!");
        m_stats.finishProcessingOps();
    }

    private void preprocessOperations(final TransactionTemplate template, final OnmsDao<?, ?> dao, OperationIterator iterator, final ExecutorService dbPool) {
        m_stats.beginPreprocessingOps();
        ExecutorService threadPool = Executors.newFixedThreadPool(m_scanThreads);
        for (Iterator<ImportOperation> it = iterator; it.hasNext(); ) {
            final ImportOperation oper = it.next();
            Runnable r = new Runnable() {

                public void run() {
                    preprocessOperation(oper, template, dao, dbPool);
                }
            };
            threadPool.execute(r);
        }
        shutdownAndWaitForCompletion(threadPool, "preprocessor interrupted!");
        m_stats.finishPreprocessingOps();
    }

    protected void preprocessOperation(final ImportOperation oper, final TransactionTemplate template, final OnmsDao<?, ?> dao, final ExecutorService dbPool) {
        m_stats.beginPreprocessing(oper);
        log().info("Preprocess: " + oper);
        oper.gatherAdditionalData();
        Runnable r = new Runnable() {

            public void run() {
                persistOperation(oper, template, dao);
            }
        };
        dbPool.execute(r);
        m_stats.finishPreprocessing(oper);
    }

    protected void persistOperation(final ImportOperation oper, TransactionTemplate template, final OnmsDao<?, ?> dao) {
        m_stats.beginPersisting(oper);
        log().info("Persist: " + oper);
        List<Event> events = persistToDatabase(oper, template);
        m_stats.finishPersisting(oper);
        if (m_eventMgr != null && events != null) {
            m_stats.beginSendingEvents(oper, events);
            log().info("Send Events: " + oper);
            for (Iterator<Event> eventIt = events.iterator(); eventIt.hasNext(); ) {
                Event event = eventIt.next();
                m_eventMgr.sendNow(event);
            }
            m_stats.finishSendingEvents(oper, events);
        }
        log().info("Clear cache: " + oper);
        dao.clear();
    }

    /**
     * Persist the import operation changes to the database.
     *  
     * @param oper changes to persist
     * @param template transaction template in which to perform the persist operation
     * @return list of events
	 */
    @SuppressWarnings("unchecked")
    private List<Event> persistToDatabase(final ImportOperation oper, TransactionTemplate template) {
        List<Event> events = (List<Event>) template.execute(new TransactionCallback() {

            public Object doInTransaction(TransactionStatus status) {
                List<Event> result = oper.persist();
                return result;
            }
        });
        return events;
    }

    private Category log() {
        return ThreadCategory.getInstance(getClass());
    }

    public void setScanThreads(int scanThreads) {
        m_scanThreads = scanThreads;
    }

    public void setWriteThreads(int writeThreads) {
        m_writeThreads = writeThreads;
    }

    public EventIpcManager getEventMgr() {
        return m_eventMgr;
    }

    public void setEventMgr(EventIpcManager eventMgr) {
        m_eventMgr = eventMgr;
    }

    public ImportStatistics getStats() {
        return m_stats;
    }

    public void setStats(ImportStatistics stats) {
        m_stats = stats;
    }

    public void setForeignSource(String foreignSource) {
        m_foreignSource = foreignSource;
    }

    public String getForeignSource() {
        return m_foreignSource;
    }
}
