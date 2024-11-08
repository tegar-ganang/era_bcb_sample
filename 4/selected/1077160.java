package de.grogra.graph.impl;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StreamCorruptedException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;
import java.util.Set;
import java.util.concurrent.Executor;
import de.grogra.graph.ArrayPath;
import de.grogra.graph.Attribute;
import de.grogra.graph.AttributeAccessor;
import de.grogra.graph.AttributeChangeListener;
import de.grogra.graph.BooleanMap;
import de.grogra.graph.ChangeBoundaryListener;
import de.grogra.graph.EdgeChangeListener;
import de.grogra.graph.EdgePattern;
import de.grogra.graph.EdgePatternImpl;
import de.grogra.graph.EventSupport;
import de.grogra.graph.Graph;
import de.grogra.graph.GraphState;
import de.grogra.graph.Instantiator;
import de.grogra.graph.ObjectAttribute;
import de.grogra.graph.ObjectMap;
import de.grogra.graph.ParentAttribute;
import de.grogra.graph.SpecialEdgeDescriptor;
import de.grogra.graph.Visitor;
import de.grogra.persistence.FatalPersistenceException;
import de.grogra.persistence.ManageableType;
import de.grogra.persistence.PersistenceCapable;
import de.grogra.persistence.PersistenceConnection;
import de.grogra.persistence.PersistenceInput;
import de.grogra.persistence.PersistenceInputStream;
import de.grogra.persistence.PersistenceManager;
import de.grogra.persistence.PersistenceOutput;
import de.grogra.persistence.PersistenceOutputStream;
import de.grogra.persistence.ResolvableReference;
import de.grogra.persistence.SOReferenceImpl;
import de.grogra.persistence.ServerConnection;
import de.grogra.persistence.Shareable;
import de.grogra.persistence.SharedObjectProvider;
import de.grogra.persistence.Transaction;
import de.grogra.persistence.TransactionApplier;
import de.grogra.reflect.Reflection;
import de.grogra.reflect.Type;
import de.grogra.reflect.TypeDecorator;
import de.grogra.reflect.XClass;
import de.grogra.util.Described;
import de.grogra.util.I18NBundle;
import de.grogra.util.Lock;
import de.grogra.util.LockProtectedRunnable;
import de.grogra.util.StringMap;
import de.grogra.util.ThreadContext;
import de.grogra.util.Utils;
import de.grogra.xl.util.IntList;
import de.grogra.xl.util.LongToIntHashMap;
import de.grogra.xl.util.ObjectList;
import de.grogra.xl.util.XBitSet;
import de.grogra.xl.util.XHashMap;

public class GraphManager extends PersistenceManager implements Graph, SharedObjectProvider {

    public static final I18NBundle I18N = I18NBundle.getInstance(GraphManager.class);

    public static final String META_GRAPH = "MetaGraph";

    public static final GraphManager STATIC = new GraphManager(new ServerConnection(null), null, false, false);

    public static final GraphState STATIC_STATE = STATIC.createStaticState(null);

    static final int GC_BITMARK_HANDLE = 1;

    private static final int INVALID_BITMARK_HANDLE = 3;

    private final StringMap roots = new StringMap();

    private final PropertyChangeSupport soListeners = new PropertyChangeSupport(this);

    private final IdentityHashMap stateMap = new IdentityHashMap();

    private GraphState mainState;

    private Executor sync;

    final EventSupport support;

    private boolean makePersistentTransitive = true;

    /**
	 * Stores a reference to the rate vector.
	 * When integration starts, a reference to the rate vector is stored here,
	 * so that PropertyRuntime.GraphProperty can access the rate vector.
	 * @see de.grogra.rgg.model.PropertyRuntime.GraphProperty#defRateAssign()
	 */
    public double[] rate;

    /**
	 * A mapping of each node ID (participating in integration) to an index into
	 * the rate vector is stored here. It is assumed, that all properties of a
	 * node participating in integration are stored in the rate array in a
	 * continuous block, where the start index of the block is the number stored
	 * in this mapping.
	 */
    public final LongToIntHashMap baseMap = new LongToIntHashMap();

    public GraphManager(PersistenceConnection cx, String key, boolean logLifeCycle, boolean checkLock) {
        super(cx, key, checkLock);
        if (allocateBitMark(false) != GC_BITMARK_HANDLE) {
            throw new AssertionError();
        }
        currentGCMark = false;
        rootExtent = new Extent(makeLock, Node.$TYPE, null);
        typeExtents.put(getHashKey(Node.$TYPE), rootExtent);
        support = new EventSupport(createObjectMap());
    }

    public void initMainState(Executor sync) {
        this.sync = sync;
        mainState = new State(this, ThreadContext.current(), false);
    }

    public GraphState getMainState() {
        return mainState;
    }

    public Map getStateMap() {
        return stateMap;
    }

    public void addChangeBoundaryListener(ChangeBoundaryListener l) {
        support.addChangeBoundaryListener(l);
    }

    public void removeChangeBoundaryListener(ChangeBoundaryListener l) {
        support.removeChangeBoundaryListener(l);
    }

    public void addAttributeChangeListener(AttributeChangeListener l) {
        support.addAttributeChangeListener(l);
    }

    public void addEdgeChangeListener(EdgeChangeListener l) {
        support.addEdgeChangeListener(l);
    }

    public void removeAttributeChangeListener(AttributeChangeListener l) {
        support.removeAttributeChangeListener(l);
    }

    public void removeEdgeChangeListener(EdgeChangeListener l) {
        support.removeEdgeChangeListener(l);
    }

    public void addAttributeChangeListener(Object object, boolean asNode, AttributeChangeListener l) {
        support.addAttributeChangeListener(object, asNode, l);
    }

    public void removeAttributeChangeListener(Object object, boolean asNode, AttributeChangeListener l) {
        support.removeAttributeChangeListener(object, asNode, l);
    }

    public void addEdgeChangeListener(Object object, boolean asNode, EdgeChangeListener l) {
        support.addEdgeChangeListener(object, asNode, l);
    }

    public void removeEdgeChangeListener(Object object, boolean asNode, EdgeChangeListener l) {
        support.removeEdgeChangeListener(object, asNode, l);
    }

    public void setRoot(String key, Node root) {
        roots.put(key, root);
        synchronized (makeLock) {
            makePersistentImpl(root, -1L, null);
        }
    }

    /**
	 * Returns the root node of the {@link Graph#MAIN_GRAPH} of this graph.
	 * 
	 * @return root node of main graph
	 */
    public Node getRoot() {
        return (Node) getRoot(MAIN_GRAPH);
    }

    public Object getRoot(String key) {
        return roots.get(key);
    }

    public String[] getRootKeys() {
        return roots.getKeys();
    }

    public void logDataOnPersistenceChange(boolean value) {
        logDataOnPersistenceChange = value;
    }

    public void suppressLogging(boolean value) {
        loggingSuppressed = value;
    }

    @Override
    protected TransactionApplier createXAApplier() {
        return new GraphXAApplier();
    }

    @Override
    protected Transaction createTransaction(Thread thread) {
        return new GraphTransaction(this, thread);
    }

    @Override
    protected boolean isAllowedThread(boolean write) {
        return !write || getMainState().getContext().isCurrent();
    }

    @Override
    protected void executeInAllowedThread(Runnable r) {
        sync.execute(r);
    }

    @Override
    protected void invokeRun(LockProtectedRunnable task, boolean sameThread, Lock lock) {
        Transaction t = getTransaction(false);
        int n = -1;
        if ((t != null) && lock.isWriteLock()) {
            n = t.getActiveCount();
            t.begin(false);
        }
        Throwable ex = null;
        try {
            task.run(sameThread, lock);
        } catch (Throwable e) {
            ex = e;
        }
        if (n >= 0) {
            while (t.getActiveCount() > n) {
                if (ex != null) {
                    t.rollback();
                } else {
                    t.commit();
                }
            }
        }
        Utils.rethrow(ex);
    }

    @Override
    protected void enterWriteLock() {
        getActiveTransaction();
    }

    @Override
    protected void leaveWriteLock() {
        getTransaction(true).close();
    }

    @Override
    protected void prepareCompletion(Transaction t, boolean commit) {
        if (commit && t.hasModified()) {
            ObjectList<Node> current = firstStack;
            ObjectList<Node> next = secondStack;
            synchronized (makeLock) {
                boolean nextGCMark = !currentGCMark;
                current.clear();
                next.clear();
                for (int i = 0; i < roots.size(); i++) {
                    Node n = (Node) roots.getValueAt(i);
                    next.add(n);
                }
                while (!next.isEmpty()) {
                    ObjectList<Node> tmp = next;
                    next = current;
                    current = tmp;
                    for (int i = current.size - 1; i >= 0; i--) {
                        if (current.get(i).setGCMark(nextGCMark) == nextGCMark) {
                            current.elements[i] = null;
                        }
                    }
                    while (!current.isEmpty()) {
                        Node n = current.pop();
                        if (n != null) {
                            addDirectlyReachable(n, next, t);
                        }
                    }
                }
                currentGCMark = nextGCMark;
                for (int i = hashBuckets.length - 1; i >= 0; i--) {
                    for (Node n = hashBuckets[i]; n != null; n = n.hashBucketNext) {
                        if (n.getGCMark() != currentGCMark) {
                            if (logDataOnPersistenceChange) {
                                t.logReadData(n, true);
                            }
                            next.add(n);
                        }
                    }
                }
                while (!next.isEmpty()) {
                    makeTransient(next.pop(), t);
                }
            }
        }
        super.prepareCompletion(t, commit);
    }

    @Override
    protected void transactionApplied(Transaction.Data xa, boolean rollback, Transaction t) {
        super.transactionApplied(xa, rollback, t);
        finishMakePersistent(((GraphTransaction) t).madePersistent, 0);
        synchronized (makeLock) {
            ObjectList<Node> n = ((GraphTransaction) t).extentIndexChanged;
            while (!n.isEmpty()) {
                Node m = n.pop();
                getExtentUnsync(m.getNType()).reenqueue(m);
            }
            n = ((GraphTransaction) t).madeTransient;
            while (!n.isEmpty()) {
                removeFromExtent(n.pop());
            }
        }
        ((GraphTransaction) t).madeTransient.clear();
        ((GraphTransaction) t).madePersistent.clear();
        ((GraphTransaction) t).extentIndexChanged.clear();
    }

    @Override
    protected void completeTransaction(Transaction t, boolean commit) {
        super.completeTransaction(t, commit);
        ((GraphTransaction) t).madeTransient.clear();
        ((GraphTransaction) t).madePersistent.clear();
        ((GraphTransaction) t).extentIndexChanged.clear();
    }

    private Node[] hashBuckets = new Node[64];

    private int extentSize = 0;

    private int resizeThreshold = 40;

    private boolean logDataOnPersistenceChange = true;

    boolean loggingSuppressed = false;

    private boolean currentGCMark;

    final XHashMap<String, Node> nodeForName = new XHashMap<String, Node>();

    private final HashMap<Object, Extent> typeExtents = new HashMap<Object, Extent>();

    private final Extent rootExtent;

    private ObjectList<Node> firstStack = new ObjectList<Node>(64);

    private ObjectList<Node> secondStack = new ObjectList<Node>(64);

    private final HashMap<String, Object> props = new HashMap<String, Object>();

    public void setProperty(String key, Object value) {
        synchronized (props) {
            props.put(key, value);
        }
    }

    public Object getProperty(String key) {
        synchronized (props) {
            return props.get(key);
        }
    }

    private static int hashCode(long id) {
        int h = (int) id ^ (int) (id >>> 32);
        h += ~(h << 9);
        h ^= (h >>> 14);
        h += (h << 4);
        return h ^ (h >>> 10);
    }

    Node getNodeOrPlaceholder(long id) {
        Node e = hashBuckets[hashCode(id) & (hashBuckets.length - 1)];
        while (e != null) {
            if (e.id == id) {
                return e;
            }
            e = e.hashBucketNext;
        }
        return null;
    }

    void removeFromExtent(Node n) {
        int i = hashCode(n.id) & (hashBuckets.length - 1);
        Node e = hashBuckets[i], prev = null;
        while (e != null) {
            if (e == n) {
                extentSize--;
                if (prev != null) {
                    prev.hashBucketNext = e.hashBucketNext;
                } else {
                    hashBuckets[i] = e.hashBucketNext;
                }
                n.setGraphManager(null, -2);
                if (n.getName() != null) {
                    synchronized (nodeForName) {
                        nodeForName.remove(n.getName(), n);
                    }
                }
                getExtentUnsync(n.getNType()).remove(n);
                return;
            }
            prev = e;
            e = e.hashBucketNext;
        }
        throw new FatalPersistenceException("Illegal node " + n);
    }

    @Override
    public long prepareId(PersistenceCapable node) {
        long id = node.getId();
        if (id < 0) {
            id = nextId();
            ((Node) node).setGraphManager(null, id);
        }
        return id;
    }

    public void setMakePersistentTransitive(boolean value) {
        makePersistentTransitive = value;
    }

    @Override
    protected void makePersistentImpl(PersistenceCapable o, long id, Transaction t) {
        if (o.getPersistenceManager() != null) {
            if (o.getPersistenceManager() != this) {
                throw new FatalPersistenceException("Different PM");
            }
            return;
        }
        boolean useId = id >= 0;
        ObjectList<Node> current = firstStack;
        ObjectList<Node> next = secondStack;
        ObjectList<Node> madePersistent = (t != null) ? ((GraphTransaction) t).madePersistent : null;
        current.clear();
        next.clear();
        next.add((Node) o);
        int madePersistentStart = (madePersistent != null) ? madePersistent.size : -1;
        while (!next.isEmpty()) {
            ObjectList<Node> tmp = next;
            next = current;
            current = tmp;
            for (int i = current.size() - 1; i >= 0; i--) {
                Node n = current.get(i);
                if (n.manager == this) {
                    current.elements[i] = null;
                    continue;
                }
                if (useId) {
                    useId = false;
                    idUsed(id);
                } else if (n.id >= 0) {
                    idUsed(id = n.id);
                } else {
                    id = nextId();
                }
                int index = hashCode(id) & (hashBuckets.length - 1);
                Node e = hashBuckets[index];
                n.hashBucketNext = e;
                while (e != null) {
                    if (e.id == id) {
                        throw new FatalPersistenceException("Cannot make " + n + " persistent: There already exists the object " + e + " with id " + id);
                    }
                    e = e.hashBucketNext;
                }
                hashBuckets[index] = n;
                n.setGraphManager(this, id);
                if (++extentSize > resizeThreshold) {
                    Node[] hb = new Node[hashBuckets.length << 1];
                    int mask = hb.length - 1;
                    for (int j = hashBuckets.length - 1; j >= 0; j--) {
                        e = hashBuckets[j];
                        if (e != null) {
                            hashBuckets[j] = null;
                            do {
                                Node x = e.hashBucketNext;
                                int k = hashCode(e.id) & mask;
                                e.hashBucketNext = hb[k];
                                hb[k] = e;
                                e = x;
                            } while (e != null);
                        }
                    }
                    hashBuckets = hb;
                    resizeThreshold = (int) (hb.length * 0.7f);
                }
                n.setGCMark(currentGCMark);
                if (t != null) {
                    madePersistent.add(n);
                } else {
                    current.push(n);
                    finishMakePersistent(current, current.size - 1);
                    current.pop();
                }
                if (Transaction.isNotApplying(t)) {
                    t.logMakePersistent(n);
                }
            }
            if (!makePersistentTransitive) {
                current.clear();
            }
            while (!current.isEmpty()) {
                Node m = current.pop();
                if (m != null) {
                    addDirectlyReachable(m, next, null);
                }
            }
        }
        if (logDataOnPersistenceChange && Transaction.isNotApplying(t)) {
            for (int i = madePersistentStart; i < madePersistent.size; i++) {
                t.logReadData(madePersistent.get(i), false);
            }
        }
    }

    void finishMakePersistent(ObjectList<Node> nodes, int start) {
        synchronized (nodeForName) {
            for (int i = nodes.size - 1; i >= start; i--) {
                Node n = nodes.get(i);
                if (n.getName() != null) {
                    nodeForName.add(n.getName(), n);
                }
            }
        }
        synchronized (makeLock) {
            for (int i = nodes.size - 1; i >= start; i--) {
                Node n = nodes.get(i);
                getExtentUnsync(n.getNType()).add(n);
            }
        }
    }

    public Extent getExtent(Type type) {
        synchronized (makeLock) {
            return getExtentUnsync(type);
        }
    }

    public Extent getExtent(Class type) {
        synchronized (makeLock) {
            Extent e = typeExtents.get(type);
            if (e == null) {
                if (!Node.class.isAssignableFrom(type)) {
                    throw new IllegalArgumentException("No Node subtype: " + type.getName());
                }
                Type t = Reflection.getType(type);
                e = new Extent(makeLock, t, getExtentUnsync(t.getSupertype()));
                typeExtents.put(type, e);
            }
            return e;
        }
    }

    public void rebuildExtents() {
        synchronized (makeLock) {
            getExtentUnsync(Node.$TYPE).clear();
            for (int i = hashBuckets.length - 1; i >= 0; i--) {
                for (Node n = hashBuckets[i]; n != null; n = n.hashBucketNext) {
                    getExtentUnsync(n.getNType()).add(n);
                }
            }
        }
    }

    private static Object getHashKey(Type type) {
        type = TypeDecorator.undecorate(type);
        return (type instanceof XClass) ? type : type.getImplementationClass();
    }

    private Extent getExtentUnsync(Type type) {
        Extent e = typeExtents.get(getHashKey(type));
        if (e == null) {
            if (!Reflection.isSuperclassOrSame(Node.$TYPE, type)) {
                throw new IllegalArgumentException("No Node subtype: " + type.getBinaryName());
            }
            e = new Extent(makeLock, type, getExtentUnsync(type.getSupertype()));
            typeExtents.put(getHashKey(type), e);
        }
        return e;
    }

    private void addDirectlyReachable(Node n, ObjectList<Node> next, Transaction xa) {
        Edge toReplace = null;
        boolean optimize = true;
        for (Edge e = n.getFirstEdge(); e != null; e = e.getNext(n)) {
            if (e.edgeBits != 0) {
                PersistenceManager pm;
                Node m = e.getNeighbor(n);
                if (xa != null) {
                    if (e == n) {
                        toReplace = null;
                        optimize = false;
                    } else if (optimize && (toReplace == null) && e.isSource(m)) {
                        toReplace = e;
                    }
                    if (m.getGCMark() == currentGCMark) {
                        next.add(m);
                    }
                } else if ((pm = m.manager) == null) {
                    next.add(m);
                } else if (pm != this) {
                    throw new FatalPersistenceException("A conflicting persistence manager " + "was found in the graph.");
                }
            }
        }
        if (toReplace != null) {
            Node s = toReplace.getSource();
            int b = toReplace.edgeBits;
            toReplace.remove(xa, false);
            Edge e = s.getOrCreateEdgeTo(n);
            assert e == n;
            e.addEdgeBits(b, xa, false);
        }
        ManageableType.Field[] f = n.getManageableType().getFCOFields();
        for (int j = f.length - 1; j >= 0; j--) {
            addNodes(f[j].getObject(n), next, xa != null);
        }
    }

    private void addNodes(Object o, ObjectList<Node> next, boolean checkGC) {
        if (o instanceof Node) {
            Node m = (Node) o;
            PersistenceManager pm;
            if (checkGC) {
                if (m.getGCMark() == currentGCMark) {
                    next.add(m);
                }
            } else if ((pm = m.manager) == null) {
                next.add(m);
            } else if (pm != this) {
                throw new FatalPersistenceException("A conflicting persistence manager " + "was found in the graph.");
            }
        } else if (o instanceof Object[]) {
            Object[] a = (Object[]) o;
            for (int i = a.length - 1; i >= 0; i--) {
                addNodes(a[i], next, checkGC);
            }
        } else if (o instanceof List) {
            List l = (List) o;
            if (o instanceof RandomAccess) {
                for (int i = l.size() - 1; i >= 0; i--) {
                    addNodes(l.get(i), next, checkGC);
                }
            } else {
                for (Iterator i = l.iterator(); i.hasNext(); ) {
                    addNodes(i.next(), next, checkGC);
                }
            }
        }
    }

    @Override
    protected void makeTransientImpl(PersistenceCapable o, Transaction t) {
        Node n = (Node) o;
        if (getNodeOrPlaceholder(n.id) != n) {
            throw new FatalPersistenceException(n + " is not registered in the graph. " + getNodeOrPlaceholder(n.id));
        }
        if (Transaction.isNotApplying(t)) {
            t.logMakeTransient(o);
        }
        if (t != null) {
            ((GraphTransaction) t).madeTransient.add(n);
            n.bits |= Node.DELETED;
        } else {
            removeFromExtent(n);
        }
    }

    @Override
    public Node getObject(long id) {
        Node n = getNodeOrPlaceholder(id);
        return (n instanceof PlaceholderNode) ? null : n;
    }

    private final Object markLock = new Object();

    private XBitSet objectMarks = new XBitSet();

    private int bitMarks = 0;

    private final IntList objBitMarks = new IntList();

    private final IntList objBitMarksUsed = new IntList();

    private ObjectList disposeBits = new ObjectList(), disposeObjects = new ObjectList();

    @Override
    public final int allocateBitMark(boolean resetOnDispose) {
        synchronized (markLock) {
            int m = Utils.indexOfOne(~bitMarks);
            if (m >= 0) {
                bitMarks |= 1 << m;
                disposeBits.set(m, resetOnDispose ? new ObjectList() : null);
                return 1 << m;
            }
            int oi;
            findObjectMark: {
                int[] a = objBitMarksUsed.elements;
                for (oi = objBitMarksUsed.size - 1; oi >= 0; oi--) {
                    if (a[oi] != 255) {
                        break findObjectMark;
                    }
                }
                oi = objBitMarksUsed.size;
                objBitMarks.add(allocateObjectMark(false));
                objBitMarksUsed.add(0);
            }
            m = Utils.indexOfOne(~objBitMarksUsed.elements[oi]);
            objBitMarksUsed.elements[oi] |= 1 << m;
            m = (objBitMarks.elements[oi] << 3) + m;
            disposeBits.set(m + 32, resetOnDispose ? new ObjectList() : null);
            return -m;
        }
    }

    @Override
    public final void disposeBitMark(int handle, boolean resetAll) {
        disposeBitMark(handle, resetAll, true);
    }

    public final void disposeBitMark(int handle, boolean resetAll, boolean onlyNodes) {
        int index;
        boolean obj = false;
        switch(handle) {
            case 1 << 0:
                index = 0;
                break;
            case 1 << 1:
                index = 1;
                break;
            case 1 << 2:
                index = 2;
                break;
            case 1 << 3:
                index = 3;
                break;
            case 1 << 4:
                index = 4;
                break;
            case 1 << 5:
                index = 5;
                break;
            case 1 << 6:
                index = 6;
                break;
            case 1 << 7:
                index = 7;
                break;
            case 1 << 8:
                index = 8;
                break;
            case 1 << 9:
                index = 9;
                break;
            case 1 << 10:
                index = 10;
                break;
            case 1 << 11:
                index = 11;
                break;
            case 1 << 12:
                index = 12;
                break;
            case 1 << 13:
                index = 13;
                break;
            case 1 << 14:
                index = 14;
                break;
            case 1 << 15:
                index = 15;
                break;
            case 1 << 16:
                index = 16;
                break;
            case 1 << 17:
                index = 17;
                break;
            case 1 << 18:
                index = 18;
                break;
            case 1 << 19:
                index = 19;
                break;
            case 1 << 20:
                index = 20;
                break;
            case 1 << 21:
                index = 21;
                break;
            case 1 << 22:
                index = 22;
                break;
            case 1 << 23:
                index = 23;
                break;
            case 1 << 24:
                index = 24;
                break;
            case 1 << 25:
                index = 25;
                break;
            case 1 << 26:
                index = 26;
                break;
            case 1 << 27:
                index = 27;
                break;
            case 1 << 28:
                index = 28;
                break;
            case 1 << 29:
                index = 29;
                break;
            case 1 << 30:
                index = 30;
                break;
            case 1 << 31:
                index = 31;
                break;
            default:
                obj = true;
                index = 32 - handle;
                break;
        }
        synchronized (markLock) {
            Object o;
            if (((o = disposeBits.get(index)) != null) || resetAll) {
                if (resetAll || (o == this)) {
                    for (int i = hashBuckets.length - 1; i >= 0; i--) {
                        for (Node n = hashBuckets[i]; n != null; n = n.hashBucketNext) {
                            n.setBitMark(handle, false);
                            if (!onlyNodes) {
                                for (Edge e = n.getFirstEdge(); e != null; e = e.getNext(n)) {
                                    e.setBitMark(handle, false);
                                }
                            }
                        }
                    }
                } else {
                    Object[] a = ((ObjectList) o).elements;
                    for (int i = ((ObjectList) o).size - 1; i >= 0; i--) {
                        ((Edge) a[i]).setBitMark(handle, false);
                    }
                }
                disposeBits.set(index, null);
            }
            if (obj) {
                index -= 32;
                int om = index >> 3;
                int[] a = objBitMarks.elements;
                for (int i = objBitMarks.size - 1; i >= 0; i--) {
                    if (a[i] == om) {
                        if ((objBitMarksUsed.elements[i] &= ~(1 << (index & 7))) == 0) {
                            disposeObjectMark(om, false);
                            objBitMarks.removeAt(i);
                            objBitMarksUsed.removeAt(i);
                        }
                        return;
                    }
                }
                throw new AssertionError();
            } else {
                bitMarks &= ~handle;
            }
        }
    }

    @Override
    public final int allocateObjectMark(boolean resetOnDispose) {
        synchronized (markLock) {
            int m = objectMarks.nextClearBit(0);
            if (m < 0) {
                m = objectMarks.size();
            }
            objectMarks.set(m);
            disposeObjects.set(m, resetOnDispose ? new ObjectList() : null);
            return m;
        }
    }

    @Override
    public final void disposeObjectMark(int handle, boolean resetAll) {
        disposeObjectMark(handle, resetAll, true);
    }

    public final void disposeObjectMark(int handle, boolean resetAll, boolean onlyNodes) {
        synchronized (markLock) {
            Object o;
            if (((o = disposeObjects.get(handle)) != null) || resetAll) {
                if (resetAll || (o == this)) {
                    for (int i = hashBuckets.length - 1; i >= 0; i--) {
                        for (Node n = hashBuckets[i]; n != null; n = n.hashBucketNext) {
                            n.setObjectMark(handle, null);
                            if (!onlyNodes) {
                                for (Edge e = n.getFirstEdge(); e != null; e = e.getNext(n)) {
                                    e.setObjectMark(handle, null);
                                }
                            }
                        }
                    }
                } else {
                    Object[] a = ((ObjectList) o).elements;
                    for (int i = ((ObjectList) o).size - 1; i >= 0; i--) {
                        ((Edge) a[i]).setObjectMark(handle, null);
                    }
                }
                disposeObjects.set(handle, null);
            }
            objectMarks.set(handle, false);
        }
    }

    final void bitMarkSet(Edge object, int handle) {
        int index;
        switch(handle) {
            case 1 << 0:
                index = 0;
                break;
            case 1 << 1:
                index = 1;
                break;
            case 1 << 2:
                index = 2;
                break;
            case 1 << 3:
                index = 3;
                break;
            case 1 << 4:
                index = 4;
                break;
            case 1 << 5:
                index = 5;
                break;
            case 1 << 6:
                index = 6;
                break;
            case 1 << 7:
                index = 7;
                break;
            case 1 << 8:
                index = 8;
                break;
            case 1 << 9:
                index = 9;
                break;
            case 1 << 10:
                index = 10;
                break;
            case 1 << 11:
                index = 11;
                break;
            case 1 << 12:
                index = 12;
                break;
            case 1 << 13:
                index = 13;
                break;
            case 1 << 14:
                index = 14;
                break;
            case 1 << 15:
                index = 15;
                break;
            case 1 << 16:
                index = 16;
                break;
            case 1 << 17:
                index = 17;
                break;
            case 1 << 18:
                index = 18;
                break;
            case 1 << 19:
                index = 19;
                break;
            case 1 << 20:
                index = 20;
                break;
            case 1 << 21:
                index = 21;
                break;
            case 1 << 22:
                index = 22;
                break;
            case 1 << 23:
                index = 23;
                break;
            case 1 << 24:
                index = 24;
                break;
            case 1 << 25:
                index = 25;
                break;
            case 1 << 26:
                index = 26;
                break;
            case 1 << 27:
                index = 27;
                break;
            case 1 << 28:
                index = 28;
                break;
            case 1 << 29:
                index = 29;
                break;
            case 1 << 30:
                index = 30;
                break;
            case 1 << 31:
                index = 31;
                break;
            default:
                index = 32 - handle;
                break;
        }
        Object o = disposeBits.get(index);
        if ((o != null) && (o != this)) {
            if (((ObjectList) o).size << 4 > extentSize) {
                disposeBits.set(index, this);
            } else {
                ((ObjectList) o).add(object);
            }
        }
    }

    final void objectMarkSet(Edge object, int handle) {
        Object o = disposeObjects.get(handle);
        if ((o != null) && (o != this)) {
            if (((ObjectList) o).size << 4 > extentSize) {
                disposeObjects.set(handle, this);
            } else {
                ((ObjectList) o).add(object);
            }
        }
    }

    public BooleanMap createBooleanMap() {
        final int h = allocateBitMark(false);
        final int he = allocateBitMark(false);
        return new BooleanMap() {

            @Override
            public boolean putBoolean(Object object, boolean asNode, boolean value) {
                return ((Edge) object).setBitMark((asNode || (object instanceof EdgeImpl)) ? h : he, value);
            }

            @Override
            public boolean getBoolean(Object object, boolean asNode) {
                return ((Edge) object).getBitMark((asNode || (object instanceof EdgeImpl)) ? h : he);
            }

            public void dispose() {
                disposeBitMark(h, true, false);
                disposeBitMark(he, true, true);
            }
        };
    }

    public <V> ObjectMap<V> createObjectMap() {
        final int h = allocateObjectMark(false);
        final int he = allocateObjectMark(false);
        return new ObjectMap<V>() {

            @Override
            public V putObject(Object object, boolean asNode, V value) {
                return (V) ((Edge) object).setObjectMark((asNode || (object instanceof EdgeImpl)) ? h : he, value);
            }

            @Override
            public V getObject(Object object, boolean asNode) {
                return (V) ((Edge) object).getObjectMark((asNode || (object instanceof EdgeImpl)) ? h : he);
            }

            public void dispose() {
                disposeObjectMark(h, true, false);
                disposeObjectMark(he, true, true);
            }
        };
    }

    @Override
    public void writeExtent(PersistenceOutputStream out) throws IOException {
        writeExtent(new GraphOutputStream(out));
    }

    public void writeExtent(final GraphOutput out) throws IOException {
        class Task implements LockProtectedRunnable {

            IOException ex;

            public void run(boolean sameThread, Lock lock) {
                try {
                    writeExtent0(out);
                } catch (IOException e) {
                    ex = e;
                }
            }
        }
        Task t = new Task();
        Utils.executeForcedlyAndUninterruptibly(this, t, false);
        if (t.ex != null) {
            throw t.ex;
        }
    }

    void writeExtent0(GraphOutput out) throws IOException {
        int mark = allocateBitMark(false);
        try {
            out.beginExtent(this, roots.size());
            for (int i = 0; i < roots.size(); i++) {
                out.beginRoot(roots.getKeyAt(i));
                writeExtent((Node) roots.getValueAt(i), null, mark, out, null);
                out.endRoot(roots.getKeyAt(i));
            }
            out.endExtent();
        } finally {
            disposeBitMark(mark, true);
        }
    }

    private static void writeExtent(Node n, Node parent, int mark, GraphOutput out, Edge e) throws IOException {
        boolean visited = n.setBitMark(mark, true);
        if (visited && (e == null)) {
            return;
        }
        out.beginNode(n, e);
        if (!visited) {
            for (e = n.getFirstEdge(); e != null; e = e.getNext(n)) {
                Node m;
                if ((m = e.getTarget()) != n) {
                    writeExtent(m, n, mark, out, e);
                } else if (parent != (m = e.getSource())) {
                    writeExtent(m, n, mark, out, null);
                }
            }
        }
        out.endNode(n);
    }

    @Override
    public void readExtent(PersistenceInputStream in) throws IOException {
        setMakePersistentTransitive(false);
        in.beginExtent(this);
        int n = in.readInt();
        for (int i = 0; i < n; i++) {
            in.checkInt(GraphOutputStream.IO_ROOT);
            String name = in.readUTF();
            in.check(GraphOutputStream.IO_NODE_BEGIN);
            setRoot(name, readNode(in));
        }
        in.endExtent();
        setMakePersistentTransitive(true);
    }

    private static Node readNode(PersistenceInputStream in) throws IOException {
        Node n = (Node) in.readPersistentObject();
        while (true) {
            switch(in.readUnsignedByte()) {
                case GraphOutputStream.IO_EDGE:
                    int edges = in.readInt();
                    n.getOrCreateEdgeTo(readNode(in)).addEdgeBits(edges, null);
                    break;
                case GraphOutputStream.IO_NODE_BEGIN:
                    readNode(in);
                    break;
                case GraphOutputStream.IO_NODE_END:
                    return n;
                default:
                    throw new StreamCorruptedException();
            }
        }
    }

    void sharedObjectModified(PropertyChangeEvent e) {
        soListeners.firePropertyChange(e);
    }

    public void addSharedObjectListener(PropertyChangeListener l) {
        soListeners.addPropertyChangeListener(l);
    }

    public void removeSharedObjectListener(PropertyChangeListener l) {
        soListeners.removePropertyChangeListener(l);
    }

    private static boolean accept0(Node v, ArrayPath path, Visitor cb, int handle) {
        Object o = null;
        Object p = null;
        Edge e = null;
        boolean gotoReturn = false;
        boolean b = true;
        ObjectList stack = path.stack;
        int stackStart = stack.size;
        try {
            loop: while (true) {
                traverseEdges: if (gotoReturn || ((o = cb.visitEnter(path, true)) != Visitor.STOP)) {
                    if (!gotoReturn) {
                        Instantiator i;
                        if ((i = v.getInstantiator()) != null) {
                            Object ie;
                            b = true;
                            if ((ie = cb.visitInstanceEnter()) != Visitor.STOP) {
                                try {
                                    cb.getGraphState().beginInstancing(v, path.getObjectId(-1));
                                    b = i.instantiate(path, cb);
                                } finally {
                                    cb.getGraphState().endInstancing();
                                }
                            }
                            if (!(b & cb.visitInstanceLeave(ie))) {
                                break traverseEdges;
                            }
                        }
                        e = v.getFirstEdge();
                    }
                    for (; e != null; e = e.getNext(v)) {
                        if (!gotoReturn) {
                            path.pushEdgeSet(e, -1, false);
                            Node n = e.getNeighbor(v);
                            path.pushNode(n, (n.manager != null) ? n.id : n.hashCode());
                            b = true;
                            if ((p = cb.visitEnter(path, false)) != Visitor.STOP) {
                                if ((handle == INVALID_BITMARK_HANDLE) || !n.setBitMark(handle, true)) {
                                    stack.push(o).push(p).push(v).push(e);
                                    v = n;
                                    continue loop;
                                }
                            }
                        }
                        gotoReturn = false;
                        b &= cb.visitLeave(p, path, false);
                        path.popNode();
                        path.popEdgeSet();
                        if (!b) {
                            break traverseEdges;
                        }
                    }
                }
                b = cb.visitLeave(o, path, true);
                if (stack.size == stackStart) {
                    return b;
                }
                e = (Edge) stack.pop();
                v = (Node) stack.pop();
                p = stack.pop();
                o = stack.pop();
                gotoReturn = true;
            }
        } finally {
            path.stack.setSize(stackStart);
        }
    }

    public final void accept(Object start, Visitor v, ArrayPath placeInPath) {
        Node root = (Node) ((start != null) ? start : roots.get(MAIN_GRAPH));
        if (root != null) {
            int h = allocateBitMark(false);
            try {
                root.setBitMark(h, true);
                if (placeInPath == null) {
                    placeInPath = new ArrayPath(this);
                } else {
                    placeInPath.clear(this);
                }
                placeInPath.pushNode(root, root.id);
                accept0(root, placeInPath, v, h);
                placeInPath.popNode();
            } finally {
                disposeBitMark(h, true);
            }
        }
    }

    public static void acceptGraph(Node root, Visitor v, ArrayPath pathToUse) {
        if (root != null) {
            pathToUse.pushNode(root, (root.manager != null) ? root.id : root.hashCode());
            accept0(root, pathToUse, v, INVALID_BITMARK_HANDLE);
            pathToUse.popNode();
        }
    }

    public GraphState createStaticState(ThreadContext tc) {
        return new State(this, tc, true);
    }

    public ObjectAttribute getParentAttribute() {
        return ParentAttribute.TREE;
    }

    public EdgePattern getTreePattern() {
        return EdgePatternImpl.TREE;
    }

    public String getProviderName() {
        return META_GRAPH;
    }

    private final SOReferenceImpl ref = new SOReferenceImpl();

    public ResolvableReference readReference(PersistenceInput in) throws java.io.IOException {
        final long sid = in.readPersistentObjectId();
        final SharedObjectNode o = (SharedObjectNode) getObject(sid);
        if ((o != null) && (o.getSharedObject() != null)) {
            ref.object = o.getSharedObject();
            return ref;
        }
        return new ResolvableReference() {

            private SharedObjectNode n = o;

            @Override
            public Shareable resolve() {
                if (n == null) {
                    n = (SharedObjectNode) getObject(sid);
                }
                return (n != null) ? n.getSharedObject() : null;
            }

            @Override
            public boolean isResolvable() {
                return resolve() != null;
            }
        };
    }

    public void writeObject(Shareable object, PersistenceOutput out) throws java.io.IOException {
        object.getProvider().writeObject(object, out);
    }

    public void addMetaNode(Node node, Transaction t) {
        ((Node) getRoot(META_GRAPH)).addEdgeBitsTo(node, BRANCH_EDGE, t);
    }

    public void removeMetaNode(Node node, Transaction t) {
        ((Node) getRoot(META_GRAPH)).removeEdgeBitsTo(node, -1, t);
    }

    public int getLifeCycleState(Object object, boolean asNode) {
        if (!(object instanceof Edge)) {
            return TRANSIENT;
        }
        Edge e = (Edge) object;
        if (e.getGraph() != this) {
            return TRANSIENT;
        }
        Node n;
        if (asNode) {
            if (!(e instanceof Node)) {
                return TRANSIENT;
            }
            n = (Node) e;
        } else {
            n = e.getSource();
            if (n == null) {
                return PERSISTENT_DELETED;
            }
            if ((n.bits & Node.DELETED) != 0) {
                return PERSISTENT_DELETED;
            }
            n = e.getTarget();
            if (n == null) {
                return PERSISTENT_DELETED;
            }
        }
        return ((n.bits & Node.DELETED) != 0) ? PERSISTENT_DELETED : PERSISTENT;
    }

    public String getName(Object object, boolean asNode) {
        return asNode ? ((Node) object).getName() : ((Edge) object).getSource().getId() + " -> " + ((Edge) object).getTarget().getId();
    }

    public long getId(Object node) {
        return ((Node) node).id;
    }

    public Object getNodeForId(long id) {
        return getObject(id);
    }

    public Object getObjectForName(boolean node, String name) {
        return node ? getNodeForName(name) : null;
    }

    public Node getNodeForName(String name) {
        synchronized (nodeForName) {
            return nodeForName.get(name);
        }
    }

    public Object getDescription(Object object, boolean asNode, String type) {
        if (Utils.isStringDescription(type)) {
            if (asNode) {
                String n = ((Node) object).getName();
                return (n != null) ? n : ((Node) object).getNType().getSimpleName() + '.' + ((Node) object).getId();
            } else {
                StringBuffer b = new StringBuffer();
                Edge e = (Edge) object;
                boolean s = Described.SHORT_DESCRIPTION.equals(type);
                if (s) {
                    b.append(e.getSource().getId()).append(' ');
                }
                e.getEdgeKeys(b, false, true);
                if (s) {
                    b.append(' ').append(e.getTarget().getId());
                }
                return b.toString();
            }
        } else if (Described.ICON.equals(type)) {
            return null;
        } else {
            return null;
        }
    }

    public SpecialEdgeDescriptor[] getSpecialEdgeDescriptors(Object node, boolean asSource) {
        return ((Node) node).getNType().getSpecialEdgeDescriptors(asSource);
    }

    public int getSymbol(Object object, boolean asNode) {
        return asNode ? ((Node) object).getSymbol() : RHOMBUS_SYMBOL;
    }

    public int getColor(Object object, boolean asNode) {
        return asNode ? ((Node) object).getSymbolColor() : 0x00ffffa0;
    }

    public Attribute[] getAttributes(Object object, boolean asNode) {
        if (asNode) {
            return ((Node) object).getAttributes();
        }
        Edge e = (Edge) object;
        int bits = e.getEdgeBits();
        if ((bits & SPECIAL_EDGE_MASK) == 0) {
            return Attribute.ATTRIBUTE_0;
        }
        return (((bits & SPECIAL_EDGE_OF_SOURCE_BIT) != 0) ? e.getSource() : e.getTarget()).getEdgeAttributes(e);
    }

    public Attribute[] getDependent(Object object, boolean asNode, Attribute a) {
        return asNode ? ((Node) object).getNType().dependencies.getDependent(a) : a.toArray();
    }

    public AttributeAccessor getAccessor(Object object, boolean asNode, Attribute attribute) {
        if (asNode) {
            return ((Node) object).getAccessor(attribute);
        }
        Edge e = (Edge) object;
        int bits = e.getEdgeBits();
        if ((bits & SPECIAL_EDGE_MASK) == 0) {
            return null;
        }
        return (((bits & SPECIAL_EDGE_OF_SOURCE_BIT) != 0) ? e.getSource() : e.getTarget()).getEdgeAttributeAccessor(attribute);
    }

    public Object getFirstEdge(Object node) {
        return ((Node) node).getFirstEdge();
    }

    public Instantiator getInstantiator(Object node) {
        return ((Node) node).getInstantiator();
    }

    public int getEdgeBits(Object edge) {
        return ((Edge) edge).getEdgeBits();
    }

    public Object getSourceNode(Object edge) {
        return ((Edge) edge).getSource();
    }

    public Object getTargetNode(Object edge) {
        return ((Edge) edge).getTarget();
    }

    public Object getNextEdge(Object edge, Object parentNode) {
        return ((Edge) edge).getNext((Node) parentNode);
    }

    public void dumpNodeStatistics(PrintWriter out) {
        out.println("Node Statistics for " + this);
        rootExtent.dumpStatistics(out, rootExtent.totalSize(), 1);
    }

    public void dumpNodeStatistics(OutputStream out) {
        PrintWriter pw = new PrintWriter(out);
        dumpNodeStatistics(pw);
        pw.flush();
    }

    public String toXLString(boolean qualified) {
        StringBuffer buf = new StringBuffer();
        appendXLTo(buf, qualified);
        return buf.toString();
    }

    public void appendXLTo(StringBuffer buf, boolean qualified) {
        HashMap<Node, String> visited = new HashMap<Node, String>();
        ObjectList<Node> toVisit = new ObjectList<Node>();
        append(buf, qualified, getRoot(), null, false, visited, toVisit);
        while (!toVisit.isEmpty()) {
            Node n = toVisit.pop();
            if (visited.get(n) == null) {
                buf.append(", ");
                append(buf, qualified, n, null, false, visited, toVisit);
            }
        }
    }

    private static void append(StringBuffer buf, boolean qualified, Node node, Edge edge, boolean branch, HashMap<Node, String> visited, ObjectList<Node> toVisit) {
        boolean closeBranch = false;
        if (edge != null) {
            int bits = edge.getEdgeBits();
            switch(bits) {
                case Graph.BRANCH_EDGE:
                    if (!branch) {
                        buf.append(" [");
                        closeBranch = true;
                    }
                    break;
                case Graph.SUCCESSOR_EDGE:
                    if (branch) {
                        buf.append("> ");
                    } else {
                        buf.append(' ');
                    }
                    break;
                default:
                    if (!branch) {
                        buf.append(' ');
                    }
                    buf.append("-0x").append(Integer.toHexString(bits)).append("-> ");
                    break;
            }
        }
        String s = visited.get(node);
        if (s != null) {
            buf.append(s);
        } else {
            int inCount = 0;
            int outCount = 0;
            for (Edge e = node.getFirstEdge(); e != null; e = e.getNext(node)) {
                if (e.isSource(node)) {
                    outCount++;
                } else if (e != edge) {
                    inCount++;
                }
            }
            if (inCount == 0) {
                s = "";
            } else {
                s = "n" + visited.size();
                buf.append(s).append(':');
            }
            visited.put(node, s);
            buf.append(qualified ? node.getNType().getName() : node.getNType().getSimpleName());
            for (int i = 0; i < 2; i++) {
                for (Edge e = node.getFirstEdge(); e != null; e = e.getNext(node)) {
                    if ((i == 0) == (e.getEdgeBits() == Graph.BRANCH_EDGE)) {
                        if (e.isSource(node)) {
                            if (--outCount > 0) {
                                buf.append(" [");
                            }
                            append(buf, qualified, e.getTarget(), e, outCount > 0, visited, toVisit);
                            if (outCount > 0) {
                                buf.append(']');
                            }
                        } else if ((e != edge) && (visited.get(e.getSource()) == null)) {
                            toVisit.add(e.getSource());
                        }
                    }
                }
            }
        }
        if (closeBranch) {
            buf.append(']');
        }
    }
}
