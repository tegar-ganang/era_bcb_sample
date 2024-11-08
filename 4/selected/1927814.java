package net.assimilator.watch;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.export.Exporter;
import net.jini.jeri.BasicJeriExporter;
import net.jini.jeri.BasicILFactory;
import net.jini.jeri.tcp.TcpServerEndpoint;
import net.jini.security.TrustVerifier;
import net.jini.security.proxytrust.ServerProxyTrust;
import com.sun.jini.config.Config;
import com.sun.jini.proxy.BasicProxyTrustVerifier;
import net.assimilator.config.ExporterConfig;
import net.assimilator.core.ThresholdValues;

/**
 * The WatchDataSourceImpl provides support for the WatchDataSource interface
 * <p>
 * The WatchDataSourceImpl supports the following configuration entries; where
 * each configuration entry name is associated with the component name <span
 * style="font-family: monospace;">net.assimilator.watch </span> <br>
 * <br>
 * <ul>
 * <li><span style="font-weight: bold; font-family: courier
 * new,courier,monospace;">watchDataSourceExporter </span> <br
 * style="font-family: courier new,courier,monospace;"> <table cellpadding="2"
 * cellspacing="2" border="0" style="text-align: left; width: 100%;"> <tbody>
 * <tr><td style="vertical-align: top; text-align: right; font-weight:
 * bold;">Type: <br>
 * </td>
 * <td style="vertical-align: top;">Exporter</td>
 * </tr>
 * <tr><td style="vertical-align: top; text-align: right; font-weight:
 * bold;">Default: <br>
 * </td>
 * <td style="vertical-align: top;">A new <code>BasicJeriExporter</code> with
 * <ul>
 * <li>a <code>TcpServerEndpoint</code> created on a random port,</li>
 * <li>a <code>BasicILFactory</code>,</li>
 * <li>distributed garbage collection turned off,</li>
 * <li>keep alive on.</li>
 * </ul>
 * <code></code></td>
 * </tr>
 * <tr><td style="vertical-align: top; text-align: right; font-weight:
 * bold;">Description: <br>
 * </td>
 * <td style="vertical-align: top;">The Exporter used to export the
 * WatchDataSourceImpl server. A new exporter is obtained every time a
 * WatchDataSourceImpl needs to export itself.</td>
 * </tr>
 * </tbody> </table></li>
 * </ul>
 * 
 * <ul>
 * <li><span style="font-weight: bold; font-family: courier
 * new,courier,monospace;">collectionSize</span> <br
 * style="font-family: courier new,courier,monospace;"> <table cellpadding="2"
 * cellspacing="2" border="0" style="text-align: left; width: 100%;"> <tbody>
 * <tr><td style="vertical-align: top; text-align: right; font-weight:
 * bold;">Type: <br>
 * </td>
 * <td style="vertical-align: top;">Exporter</td>
 * </tr>
 * <tr><td style="vertical-align: top; text-align: right; font-weight:
 * bold;">Default: <br>
 * </td>
 * <td style="vertical-align: top;">1000</td>
 * </tr>
 * <tr><td style="vertical-align: top; text-align: right; font-weight:
 * bold;">Description: <br>
 * </td>
 * <td style="vertical-align: top;">The size of the WatchDataSource history
 * collection.</td>
 * </tr>
 * </tbody> </table></li>
 * </ul>
 */
public class WatchDataSourceImpl implements WatchDataSource, ServerProxyTrust {

    private List queue = Collections.synchronizedList(new LinkedList());

    /** The writer Thread */
    private transient Thread writerThread;

    private int offset = 0;

    /** defines the default history size */
    public static final int DEFAULT_COLLECTION_SIZE = 1000;

    /** defines the default history max size */
    public static final int MAX_COLLECTION_SIZE = 10000;

    /** The current history maximum size */
    private int max = DEFAULT_COLLECTION_SIZE;

    /** the history */
    private ArrayList history;

    /** Holds value of property id. */
    private String id = null;

    /** Holds value of property archivable. */
    private Archivable archivable;

    /** The class name used to view the WatchDataSource */
    private String viewClass;

    /** Holds value of property ThresholdVales. */
    private ThresholdValues thresholdValues = new ThresholdValues();

    /** Configuration for the WatchDataSource */
    private Configuration config;

    /** The Exporter for the WatchDataSource */
    private Exporter exporter;

    /** Object supporting remote semantics required for an WatchDataSource */
    private WatchDataSource proxy;

    /** Flag to indicate whether the WatchDataSource is exported */
    private boolean exported = false;

    /** Component for accessing configuration and getting a Logger */
    private static final String COMPONENT = "net.assimilator.watch";

    /** A suitable Logger */
    private static Logger logger = Logger.getLogger(COMPONENT);

    /**
     * Constructs WatchDataSourceImpl object.
     * 
     * @param id The ID of the WatchDataSource
     * @param archivable the archivable
     * @param config Configuration object for use
     */
    public WatchDataSourceImpl(String id, Archivable archivable, Configuration config) {
        setID(id);
        if (archivable != null) setArchivable(archivable); else setArchivable(new NullArchive());
        this.config = config;
        int collectionSize = DEFAULT_COLLECTION_SIZE;
        try {
            collectionSize = Config.getIntEntry(config, COMPONENT, "collectionSize", DEFAULT_COLLECTION_SIZE, 1, MAX_COLLECTION_SIZE);
        } catch (ConfigurationException e) {
            if (logger.isLoggable(Level.FINEST)) logger.log(Level.FINEST, "Getting WatchDataSource collection size", e);
            collectionSize = DEFAULT_COLLECTION_SIZE;
        }
        if (logger.isLoggable(Level.FINEST)) logger.finest("Watch [" + id + "] history collection size=" + collectionSize);
        max = collectionSize;
        history = new ArrayList(collectionSize);
        startWriter();
    }

    /**
     * Export the WatchDataSourceImpl using a configured Exporter, defaulting to
     * BasicJeriExporter
     */
    public WatchDataSource export() throws RemoteException {
        if (exported && proxy != null) return (proxy);
        final Exporter defaultExporter = new BasicJeriExporter(TcpServerEndpoint.getInstance(0), new BasicILFactory(), false, true);
        if (config != null) {
            try {
                exporter = ExporterConfig.getExporter(config, COMPONENT, "watchDataSourceExporter", defaultExporter);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Getting watchDataSourceExporter", e);
            }
        }
        if (exporter == null) exporter = defaultExporter;
        proxy = (WatchDataSource) exporter.export(this);
        exported = true;
        return (proxy);
    }

    /**
     * Unexport the WatchDataSourceImpl on an anonymous port
     * 
     * @param force If true, unexports the WatchDataSourceImpl even if there
     * are pending or in-progress calls; if false, only unexports the
     * WatchDataSourceImpl if there are no pending or in-progress calls
     */
    public void unexport(boolean force) {
        if (!exported) return;
        try {
            exporter.unexport(force);
            exported = false;
            proxy = null;
        } catch (IllegalStateException e) {
        }
    }

    /**
     * Get the WatchDataSource proxy
     */
    public WatchDataSource getProxy() {
        return (proxy);
    }

    /**
     * @see net.assimilator.watch.WatchDataSource#getID
     */
    public String getID() {
        return (id);
    }

    /**
     * Setter for property id.
     * 
     * @param id New value of property id.
     */
    public void setID(String id) {
        if (id == null) throw new NullPointerException("id is null");
        this.id = id;
    }

    /**
     * @see net.assimilator.watch.WatchDataSource#getOffset
     */
    public synchronized int getOffset() {
        return (offset);
    }

    /**
     * @see net.assimilator.watch.WatchDataSource#setSize
     */
    public synchronized void setSize(int size) {
        if (size < this.max) {
            max = size;
            if (history.size() > size) {
                trimHistory((history.size() - size) - 1);
                history.trimToSize();
            }
        } else history.ensureCapacity(size);
        this.max = size;
    }

    /**
     * @see net.assimilator.watch.WatchDataSource#clear
     */
    public synchronized void clear() {
        history.clear();
    }

    /**
     * Trims the history. Always starting at the beginning, with an index of 0
     * 
     * @param range The number or records to trim
     */
    private synchronized void trimHistory(int range) {
        if (range == 1) {
            history.remove(0);
        } else {
            List subList = history.subList(0, range);
            subList.clear();
            subList = null;
        }
        offset += range;
    }

    /**
     * @see net.assimilator.watch.WatchDataSource#getSize
     */
    public synchronized int getSize() {
        return (max);
    }

    /**
     * @see net.assimilator.watch.WatchDataSource#getCurrentSize
     */
    public int getCurrentSize() {
        return (history.size());
    }

    /**
     * @see net.assimilator.watch.WatchDataSource#addCalculable
     */
    public void addCalculable(Calculable calculable) {
        if (calculable == null) throw new NullPointerException("calculable is null");
        synchronized (queue) {
            queue.add(calculable);
            queue.notifyAll();
        }
    }

    private synchronized void addToHistory(Calculable calculable) {
        if (history.size() == max) trimHistory(1);
        if (history.size() > max) trimHistory((history.size() - max) - 1);
        history.add(calculable);
    }

    /**
     * Starts the writer thread that actually writes to the WatchDataSource
     */
    private void startWriter() {
        if (writerThread != null) return;
        Writer writer = new Writer();
        writer.setName(id + ":writer");
        writer.setDaemon(true);
        writer.start();
    }

    /**
     * Getter for property archivable.
     * 
     * @return Value of property archivable.
     */
    public Archivable getArchivable() {
        return (archivable);
    }

    /**
     * Setter for property archivable.
     * 
     * @param archivable New value of property archivable.
     */
    public void setArchivable(Archivable archivable) {
        if (archivable == null) archivable = new NullArchive();
        this.archivable = archivable;
    }

    /**
     * The thread that writes to the WatchDataSource
     */
    class Writer extends Thread {

        public void run() {
            writerThread = Thread.currentThread();
            while (!writerThread.isInterrupted()) {
                if (queue.isEmpty()) {
                    try {
                        synchronized (queue) {
                            queue.wait(1000 * 30);
                        }
                    } catch (InterruptedException ex) {
                        return;
                    }
                } else {
                    Calculable calc = (Calculable) queue.remove(0);
                    addToHistory(calc);
                    if (archivable != null) archivable.archive(calc);
                }
            }
        }
    }

    /**
     * @see net.assimilator.watch.WatchDataSource#getCalculable
     */
    public synchronized Calculable[] getCalculable() {
        return (Calculable[]) history.toArray(new Calculable[history.size()]);
    }

    /**
     * @see net.assimilator.watch.WatchDataSource#getCalculable
     */
    public Calculable[] getCalculable(String id) {
        return (getCalculable(id, 0, history.size()));
    }

    /**
     * @see net.assimilator.watch.WatchDataSource#getCalculable
     */
    public synchronized Calculable[] getCalculable(int offset, int length) {
        offset = offset - this.offset;
        offset = (offset < 0 ? 0 : offset);
        length = Math.min(length, history.size() - offset);
        return ((Calculable[]) history.subList(offset, offset + length).toArray(new Calculable[length]));
    }

    /**
     * @see net.assimilator.watch.WatchDataSource#getCalculable
     */
    public synchronized Calculable[] getCalculable(String id, int offset, int length) {
        if (id == null) throw new NullPointerException("id is null");
        offset = offset - this.offset;
        offset = (offset < 0 ? 0 : offset);
        length = Math.min(length, history.size() - offset);
        List list = history.subList(offset, offset + length);
        ArrayList result = new ArrayList(length);
        Iterator iter = list.iterator();
        while (iter.hasNext()) {
            Calculable calc = (Calculable) iter.next();
            if (calc.getId().equals(id)) result.add(calc);
        }
        return (Calculable[]) result.toArray(new Calculable[result.size()]);
    }

    /**
     * @see net.assimilator.watch.WatchDataSource#getLastCalculable
     */
    public synchronized Calculable getLastCalculable() {
        try {
            return (Calculable) history.get(history.size() - 1);
        } catch (IndexOutOfBoundsException ex) {
            return (null);
        }
    }

    /**
     * @see net.assimilator.watch.WatchDataSource#getLastCalculable
     */
    public synchronized Calculable getLastCalculable(String id) {
        if (id == null) throw new NullPointerException("id is null");
        for (int i = history.size() - 1; i >= 0; i--) {
            Calculable calc = (Calculable) history.get(i);
            if (calc.getId().equals(id)) return (calc);
        }
        return (null);
    }

    /**
     * Make sure the archival file is closed before garbage collection
     */
    protected void finalize() {
        if (archivable != null) archivable.close();
    }

    /**
     * @see net.assimilator.watch.WatchDataSource#getThresholdValues
     */
    public ThresholdValues getThresholdValues() {
        return (thresholdValues);
    }

    /**
     * Set the ThresholdValues
     */
    public void setThresholdValues(ThresholdValues tValues) {
        if (tValues != null) thresholdValues = tValues;
    }

    /**
     * @see net.assimilator.watch.WatchDataSource#close
     */
    public void close() {
        if (writerThread != null && writerThread.isAlive()) writerThread.interrupt();
        if (archivable != null) {
            archivable.close();
            archivable = null;
        }
        unexport(true);
    }

    /**
     * @see net.assimilator.watch.WatchDataSource#setView
     */
    public void setView(String viewClass) {
        this.viewClass = viewClass;
    }

    /**
     * @see net.assimilator.watch.WatchDataSource#getView
     */
    public String getView() {
        return (viewClass);
    }

    /**
     * Returns a <code>TrustVerifier</code> which can be used to verify that a
     * given proxy to this WatchDataSource can be trusted
     */
    public TrustVerifier getProxyVerifier() {
        if (logger.isLoggable(Level.FINEST)) logger.entering(this.getClass().getName(), "getProxyVerifier");
        return (new BasicProxyTrustVerifier(proxy));
    }
}
