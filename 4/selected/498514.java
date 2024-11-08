package org.tripcom.tsadapter.sparql;

import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import net.jini.core.entry.Entry;
import net.jini.space.JavaSpace;
import org.apache.log4j.Logger;
import org.tripcom.integration.entry.AccessType;
import org.tripcom.integration.entry.DataResultExternal;
import org.tripcom.integration.entry.RdMetaMEntry;
import org.tripcom.integration.entry.RdResultDMEntry;
import org.tripcom.integration.entry.RdTSAdapterEntry;
import org.tripcom.integration.entry.ReadType;
import org.tripcom.integration.entry.TSAdapterEntry;
import org.tripcom.integration.entry.TripleEntry;
import org.tripcom.integration.entry.UnsubscribeRequest;
import org.tripcom.tsadapter.EntryListener;
import org.tripcom.tsadapter.TSAdapter;
import com.ontotext.ordi.exception.ORDIException;

/**
 * This class is responsible to queue and execute the blocking read, subscribed
 * and unsubscribe requests.
 * 
 * @author vassil
 * 
 */
public class Processor implements Runnable, EntryListener {

    public static final int MAX_PARALLEL_OPERATION = 100;

    private static Logger logger = Logger.getLogger(Processor.class);

    private static Set<GraphTask> waitTask = new HashSet<GraphTask>();

    private static BlockingQueue<GraphTask> tasks = new ArrayBlockingQueue<GraphTask>(MAX_PARALLEL_OPERATION);

    public static synchronized void addTask(GraphTask t) {
        if (t == null) {
            throw new IllegalArgumentException();
        }
        while (tasks.remainingCapacity() == 0) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ie) {
                logger.info("Maximum number of queued tasks is reached!");
            }
        }
        tasks.add(t);
    }

    public void run() {
        while (true) {
            try {
                GraphTask t = tasks.poll(100, TimeUnit.MILLISECONDS);
                if (t == null) {
                    removeExpired();
                    continue;
                }
                t.evaluate();
                if (t.result.size() > 0) {
                    logger.info(String.format("Write results for read %d!", t.getOperationId()));
                    writeToSpace(t);
                }
                if (t.result.size() == 0 || t.isSubscribe()) {
                    synchronized (waitTask) {
                        waitTask.add(t);
                    }
                }
            } catch (ORDIException oe) {
                logger.error(oe);
            } catch (InterruptedException ie) {
            }
        }
    }

    public void process(Entry entry) {
        if (entry instanceof UnsubscribeRequest) {
            UnsubscribeRequest u = (UnsubscribeRequest) entry;
            synchronized (waitTask) {
                for (GraphTask t : waitTask) {
                    if (t.getOperationId() == u.getOperationID()) {
                        waitTask.remove(t);
                        logger.info(String.format("Removed subscription of operation id %d!", u.operationID));
                        return;
                    }
                }
            }
            logger.info(String.format("Failed to remove subscription for operation id %d!", u.operationID));
        } else if (entry instanceof TSAdapterEntry) {
            if (((TSAdapterEntry) entry).data.size() == 0) {
                return;
            }
            logger.info("New data is available restart all waiting" + " blocking operations!");
            synchronized (waitTask) {
                tasks.addAll(waitTask);
                waitTask.clear();
            }
        }
    }

    private void writeToSpace(GraphTask t) {
        Entry entry = t.entry;
        if (entry instanceof RdTSAdapterEntry) {
            RdTSAdapterEntry e = (RdTSAdapterEntry) entry;
            JavaSpace space = TSAdapter.getAdapter().getSpace();
            AccessType atype = t.isDeleting() ? AccessType.Delete : AccessType.Read;
            Entry[] results = null;
            if (e.kind == ReadType.NOTIFY) {
                results = new Entry[1];
                Set<Set<TripleEntry>> data = new HashSet<Set<TripleEntry>>();
                data.add(t.result);
                results[0] = new DataResultExternal(e.operationID, data, new Boolean(true));
            } else {
                results = new Entry[2];
                results[0] = new RdMetaMEntry(t.result, e.space, new TreeSet<java.net.URI>(), e.timeout != null ? e.timeout.getTimeout() : 0, atype, e.clientInfo, e.operationID, e.transactionID);
                results[1] = new RdResultDMEntry(t.result, e.space, new TreeSet<URI>(), e.operationID, e.transactionID, true);
            }
            try {
                for (int i = 0; i < results.length; i++) {
                    space.write(results[i], null, Long.MAX_VALUE);
                }
            } catch (Exception ex) {
                logger.error("Could not write to space read results!", ex);
            }
        }
    }

    private void removeExpired() {
        GraphTask t = null;
        synchronized (waitTask) {
            for (Iterator<GraphTask> i = waitTask.iterator(); i.hasNext(); ) {
                t = i.next();
                if (t.isExpired()) {
                    writeToSpace(t);
                    i.remove();
                }
            }
        }
    }
}
