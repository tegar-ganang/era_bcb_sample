package edu.jas.gb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.Logger;
import edu.jas.poly.ExpVector;
import edu.jas.poly.GenPolynomial;
import edu.jas.structure.RingElem;
import edu.jas.util.ChannelFactory;
import edu.jas.util.DistHashTable;
import edu.jas.util.DistHashTableServer;
import edu.jas.util.SocketChannel;
import edu.jas.util.TaggedSocketChannel;
import edu.jas.util.Terminator;
import edu.jas.util.ThreadPool;

/**
 * Groebner Base distributed hybrid algorithm. Implements a
 * distributed memory with multi-core CPUs parallel version of
 * Groebner bases. Using pairlist class, distributed multi-threaded
 * tasks do reduction, one communication channel per remote node.
 * @param <C> coefficient type
 * @author Heinz Kredel
 */
public class GroebnerBaseDistributedHybrid<C extends RingElem<C>> extends GroebnerBaseAbstract<C> {

    public static final Logger logger = Logger.getLogger(GroebnerBaseDistributedHybrid.class);

    public final boolean debug = logger.isDebugEnabled();

    /**
     * Number of threads to use.
     */
    protected final int threads;

    /**
     * Default number of threads.
     */
    protected static final int DEFAULT_THREADS = 2;

    /**
     * Number of threads per node to use.
     */
    protected final int threadsPerNode;

    /**
     * Default number of threads per compute node.
     */
    protected static final int DEFAULT_THREADS_PER_NODE = 1;

    protected final ThreadPool pool;

    /**
     * Default server port.
     */
    protected static final int DEFAULT_PORT = 4711;

    /**
     * Server port to use.
     */
    protected final int port;

    /**
     * Message tag for pairs.
     */
    public static final Integer pairTag = new Integer(1);

    /**
     * Message tag for results.
     */
    public static final Integer resultTag = new Integer(2);

    /**
     * Message tag for acknowledgments.
     */
    public static final Integer ackTag = new Integer(3);

    /**
     * Constructor.
     */
    public GroebnerBaseDistributedHybrid() {
        this(DEFAULT_THREADS, DEFAULT_PORT);
    }

    /**
     * Constructor.
     * @param threads number of threads to use.
     */
    public GroebnerBaseDistributedHybrid(int threads) {
        this(threads, new ThreadPool(threads), DEFAULT_PORT);
    }

    /**
     * Constructor.
     * @param threads number of threads to use.
     * @param port server port to use.
     */
    public GroebnerBaseDistributedHybrid(int threads, int port) {
        this(threads, new ThreadPool(threads), port);
    }

    /**
     * Constructor.
     * @param threads number of threads to use.
     * @param threadsPerNode threads per node to use.
     * @param port server port to use.
     */
    public GroebnerBaseDistributedHybrid(int threads, int threadsPerNode, int port) {
        this(threads, threadsPerNode, new ThreadPool(threads), port);
    }

    /**
     * Constructor.
     * @param threads number of threads to use.
     * @param pool ThreadPool to use.
     * @param port server port to use.
     */
    public GroebnerBaseDistributedHybrid(int threads, ThreadPool pool, int port) {
        this(threads, DEFAULT_THREADS_PER_NODE, pool, port);
    }

    /**
     * Constructor.
     * @param threads number of threads to use.
     * @param threadsPerNode threads per node to use.
     * @param pl pair selection strategy
     * @param port server port to use.
     */
    public GroebnerBaseDistributedHybrid(int threads, int threadsPerNode, PairList<C> pl, int port) {
        this(threads, threadsPerNode, new ThreadPool(threads), pl, port);
    }

    /**
     * Constructor.
     * @param threads number of threads to use.
     * @param threadsPerNode threads per node to use.
     * @param port server port to use.
     */
    public GroebnerBaseDistributedHybrid(int threads, int threadsPerNode, ThreadPool pool, int port) {
        this(threads, threadsPerNode, pool, new OrderedPairlist<C>(), port);
    }

    /**
     * Constructor.
     * @param threads number of threads to use.
     * @param threadsPerNode threads per node to use.
     * @param pool ThreadPool to use.
     * @param pl pair selection strategy
     * @param port server port to use.
     */
    public GroebnerBaseDistributedHybrid(int threads, int threadsPerNode, ThreadPool pool, PairList<C> pl, int port) {
        super(new ReductionPar<C>(), pl);
        if (threads < 1) {
            threads = 1;
        }
        this.threads = threads;
        this.threadsPerNode = threadsPerNode;
        this.pool = pool;
        this.port = port;
    }

    /**
     * Cleanup and terminate.
     */
    public void terminate() {
        if (pool == null) {
            return;
        }
        pool.terminate();
    }

    /**
     * Distributed hybrid Groebner base. 
     * @param modv number of module variables.
     * @param F polynomial list.
     * @return GB(F) a Groebner base of F or null, if a IOException occurs.
     */
    public List<GenPolynomial<C>> GB(int modv, List<GenPolynomial<C>> F) {
        long t = System.currentTimeMillis();
        final int DL_PORT = port + 100;
        ChannelFactory cf = new ChannelFactory(port);
        cf.init();
        DistHashTableServer<Integer> dls = new DistHashTableServer<Integer>(DL_PORT);
        dls.init();
        logger.debug("dist-list server running");
        GenPolynomial<C> p;
        List<GenPolynomial<C>> G = new ArrayList<GenPolynomial<C>>();
        PairList<C> pairlist = null;
        boolean oneInGB = false;
        int l = F.size();
        int unused;
        ListIterator<GenPolynomial<C>> it = F.listIterator();
        while (it.hasNext()) {
            p = it.next();
            if (p.length() > 0) {
                p = p.monic();
                if (p.isONE()) {
                    oneInGB = true;
                    G.clear();
                    G.add(p);
                }
                if (!oneInGB) {
                    G.add(p);
                }
                if (pairlist == null) {
                    pairlist = strategy.create(modv, p.ring);
                    if (!p.ring.coFac.isField()) {
                        throw new IllegalArgumentException("coefficients not from a field");
                    }
                }
                if (p.isONE()) {
                    unused = pairlist.putOne();
                } else {
                    unused = pairlist.put(p);
                }
            } else {
                l--;
            }
        }
        if (l <= 1) {
        }
        logger.info("pairlist " + pairlist);
        logger.debug("looking for clients");
        DistHashTable<Integer, GenPolynomial<C>> theList = new DistHashTable<Integer, GenPolynomial<C>>("localhost", DL_PORT);
        List<GenPolynomial<C>> al = pairlist.getList();
        for (int i = 0; i < al.size(); i++) {
            GenPolynomial<C> nn = theList.put(new Integer(i), al.get(i));
            if (nn != null) {
                logger.info("double polynomials " + i + ", nn = " + nn + ", al(i) = " + al.get(i));
            }
        }
        Terminator finner = new Terminator(threads * threadsPerNode);
        HybridReducerServer<C> R;
        logger.info("using pool = " + pool);
        for (int i = 0; i < threads; i++) {
            R = new HybridReducerServer<C>(threadsPerNode, finner, cf, theList, pairlist);
            pool.addJob(R);
        }
        logger.info("main loop waiting " + finner);
        finner.waitDone();
        int ps = theList.size();
        logger.info("#distributed list = " + ps);
        G = pairlist.getList();
        if (ps != G.size()) {
            logger.info("#distributed list = " + theList.size() + " #pairlist list = " + G.size());
        }
        for (GenPolynomial<C> q : theList.getValueList()) {
            if (q != null && !q.isZERO()) {
                logger.debug("final q = " + q.leadingExpVector());
            }
        }
        logger.debug("distributed list end");
        long time = System.currentTimeMillis();
        List<GenPolynomial<C>> Gp;
        Gp = minimalGB(G);
        time = System.currentTimeMillis() - time;
        logger.info("parallel gbmi time = " + time);
        G = Gp;
        logger.debug("server cf.terminate()");
        cf.terminate();
        logger.info("server not pool.terminate() " + pool);
        logger.info("server theList.terminate() " + theList.size());
        theList.terminate();
        logger.info("server dls.terminate() " + dls);
        dls.terminate();
        t = System.currentTimeMillis() - t;
        logger.info("server GB end, time = " + t + ", " + pairlist.toString());
        return G;
    }

    /**
     * GB distributed client.
     * @param host the server runs on.
     * @throws IOException
     */
    public void clientPart(String host) throws IOException {
        ChannelFactory cf = new ChannelFactory(port + 10);
        cf.init();
        SocketChannel channel = cf.getChannel(host, port);
        TaggedSocketChannel pairChannel = new TaggedSocketChannel(channel);
        pairChannel.init();
        if (debug) {
            logger.info("clientPart pairChannel   = " + pairChannel);
        }
        final int DL_PORT = port + 100;
        DistHashTable<Integer, GenPolynomial<C>> theList = new DistHashTable<Integer, GenPolynomial<C>>(host, DL_PORT);
        ThreadPool pool = new ThreadPool(threadsPerNode);
        logger.info("client using pool = " + pool);
        for (int i = 0; i < threadsPerNode; i++) {
            HybridReducerClient<C> Rr = new HybridReducerClient<C>(threadsPerNode, pairChannel, i, theList);
            pool.addJob(Rr);
        }
        if (debug) {
            logger.info("clients submitted");
        }
        pool.terminate();
        logger.info("client pool.terminate()");
        pairChannel.close();
        logger.info("client pairChannel.close()");
        theList.terminate();
        cf.terminate();
        logger.info("client cf.terminate()");
        channel.close();
        logger.info("client channel.close()");
        return;
    }

    /**
     * Minimal ordered groebner basis.
     * @param Fp a Groebner base.
     * @return a reduced Groebner base of Fp.
     */
    @Override
    public List<GenPolynomial<C>> minimalGB(List<GenPolynomial<C>> Fp) {
        GenPolynomial<C> a;
        ArrayList<GenPolynomial<C>> G;
        G = new ArrayList<GenPolynomial<C>>(Fp.size());
        ListIterator<GenPolynomial<C>> it = Fp.listIterator();
        while (it.hasNext()) {
            a = it.next();
            if (a.length() != 0) {
                G.add(a);
            }
        }
        if (G.size() <= 1) {
            return G;
        }
        ExpVector e;
        ExpVector f;
        GenPolynomial<C> p;
        ArrayList<GenPolynomial<C>> F;
        F = new ArrayList<GenPolynomial<C>>(G.size());
        boolean mt;
        while (G.size() > 0) {
            a = G.remove(0);
            e = a.leadingExpVector();
            it = G.listIterator();
            mt = false;
            while (it.hasNext() && !mt) {
                p = it.next();
                f = p.leadingExpVector();
                mt = e.multipleOf(f);
            }
            it = F.listIterator();
            while (it.hasNext() && !mt) {
                p = it.next();
                f = p.leadingExpVector();
                mt = e.multipleOf(f);
            }
            if (!mt) {
                F.add(a);
            } else {
            }
        }
        G = F;
        if (G.size() <= 1) {
            return G;
        }
        Collections.reverse(G);
        MiReducerServer<C>[] mirs = (MiReducerServer<C>[]) new MiReducerServer[G.size()];
        int i = 0;
        F = new ArrayList<GenPolynomial<C>>(G.size());
        while (G.size() > 0) {
            a = G.remove(0);
            List<GenPolynomial<C>> R = new ArrayList<GenPolynomial<C>>(G.size() + F.size());
            R.addAll(G);
            R.addAll(F);
            mirs[i] = new MiReducerServer<C>(R, a);
            pool.addJob(mirs[i]);
            i++;
            F.add(a);
        }
        G = F;
        F = new ArrayList<GenPolynomial<C>>(G.size());
        for (i = 0; i < mirs.length; i++) {
            a = mirs[i].getNF();
            F.add(a);
        }
        return F;
    }
}

/**
 * Distributed server reducing worker proxy threads.
 * @param <C> coefficient type
 */
class HybridReducerServer<C extends RingElem<C>> implements Runnable {

    public static final Logger logger = Logger.getLogger(HybridReducerServer.class);

    public final boolean debug = logger.isDebugEnabled();

    private final Terminator finner;

    private final ChannelFactory cf;

    private TaggedSocketChannel pairChannel;

    private final DistHashTable<Integer, GenPolynomial<C>> theList;

    private final PairList<C> pairlist;

    private final int threadsPerNode;

    /**
     * Message tag for pairs.
     */
    public final Integer pairTag = GroebnerBaseDistributedHybrid.pairTag;

    /**
     * Message tag for results.
     */
    public final Integer resultTag = GroebnerBaseDistributedHybrid.resultTag;

    /**
     * Message tag for acknowledgments.
     */
    public final Integer ackTag = GroebnerBaseDistributedHybrid.ackTag;

    /**
     * Constructor.
     * @param tpn number of threads per node
     * @param fin terminator
     * @param cf channel factory
     * @param dl distributed hash table
     * @param L ordered pair list
     */
    HybridReducerServer(int tpn, Terminator fin, ChannelFactory cf, DistHashTable<Integer, GenPolynomial<C>> dl, PairList<C> L) {
        threadsPerNode = tpn;
        finner = fin;
        this.cf = cf;
        theList = dl;
        pairlist = L;
    }

    public void run() {
        logger.info("reducer server running with " + cf);
        SocketChannel channel = null;
        try {
            channel = cf.getChannel();
            pairChannel = new TaggedSocketChannel(channel);
            pairChannel.init();
        } catch (InterruptedException e) {
            logger.debug("get pair channel interrupted");
            e.printStackTrace();
            return;
        }
        if (debug) {
            logger.info("pairChannel   = " + pairChannel);
        }
        finner.initIdle(threadsPerNode);
        AtomicInteger active = new AtomicInteger(0);
        HybridReducerReceiver<C> receiver = new HybridReducerReceiver<C>(threadsPerNode, finner, active, pairChannel, theList, pairlist);
        receiver.start();
        Pair<C> pair;
        boolean set = false;
        boolean goon = true;
        int polIndex = -1;
        int red = 0;
        int sleeps = 0;
        while (goon) {
            logger.debug("receive request");
            Object req = null;
            try {
                req = pairChannel.receive(pairTag);
            } catch (InterruptedException e) {
                goon = false;
                e.printStackTrace();
            } catch (IOException e) {
                goon = false;
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                goon = false;
                e.printStackTrace();
            }
            logger.info("received request, req = " + req);
            if (req == null) {
                goon = false;
                break;
            }
            if (!(req instanceof GBTransportMessReq)) {
                goon = false;
                break;
            }
            logger.info("find pair");
            while (!pairlist.hasNext()) {
                if (!finner.hasJobs() && !pairlist.hasNext()) {
                    goon = false;
                    break;
                }
                try {
                    sleeps++;
                    logger.info("waiting for reducers, remaining = " + finner.getJobs());
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    goon = false;
                    break;
                }
            }
            if (!pairlist.hasNext() && !finner.hasJobs()) {
                logger.info("termination detection: no pairs and no jobs left");
                goon = false;
                break;
            }
            finner.notIdle();
            pair = pairlist.removeNext();
            if (debug) {
                logger.info("active count = " + active.get());
                logger.info("send pair = " + pair);
            }
            GBTransportMess msg = null;
            if (pair != null) {
                msg = new GBTransportMessPairIndex(pair);
            } else {
                msg = new GBTransportMess();
            }
            try {
                red++;
                pairChannel.send(pairTag, msg);
                int a = active.getAndIncrement();
            } catch (IOException e) {
                e.printStackTrace();
                goon = false;
                break;
            }
        }
        logger.info("terminated, send " + red + " reduction pairs");
        logger.debug("send end");
        try {
            for (int i = 0; i < threadsPerNode; i++) {
                pairChannel.send(pairTag, new GBTransportMessEnd());
            }
            pairChannel.send(resultTag, new GBTransportMessEnd());
        } catch (IOException e) {
            if (logger.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
        receiver.terminate();
        int d = active.get();
        logger.info("remaining active tasks = " + d);
        pairChannel.close();
        logger.info("redServ pairChannel.close()");
        finner.release();
        channel.close();
        logger.info("redServ channel.close()");
    }
}

/**
 * Distributed server receiving worker thread.
 * @param <C> coefficient type
 */
class HybridReducerReceiver<C extends RingElem<C>> extends Thread {

    public static final Logger logger = Logger.getLogger(HybridReducerReceiver.class);

    public final boolean debug = logger.isDebugEnabled();

    private final DistHashTable<Integer, GenPolynomial<C>> theList;

    private final PairList<C> pairlist;

    private final TaggedSocketChannel pairChannel;

    private final Terminator finner;

    private final int threadsPerNode;

    private final AtomicInteger active;

    private volatile boolean goon;

    /**
     * Message tag for pairs.
     */
    public final Integer pairTag = GroebnerBaseDistributedHybrid.pairTag;

    /**
     * Message tag for results.
     */
    public final Integer resultTag = GroebnerBaseDistributedHybrid.resultTag;

    /**
     * Message tag for acknowledgments.
     */
    public final Integer ackTag = GroebnerBaseDistributedHybrid.ackTag;

    /**
     * Constructor.
     * @param tpn number of threads per node
     * @param fin terminator
     * @param a active remote tasks count
     * @param pc tagged socket channel
     * @param dl distributed hash table
     * @param L ordered pair list
     */
    HybridReducerReceiver(int tpn, Terminator fin, AtomicInteger a, TaggedSocketChannel pc, DistHashTable<Integer, GenPolynomial<C>> dl, PairList<C> L) {
        active = a;
        threadsPerNode = tpn;
        finner = fin;
        pairChannel = pc;
        theList = dl;
        pairlist = L;
        goon = true;
    }

    /**
     * Work loop.
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        GenPolynomial<C> H = null;
        int red = 0;
        int polIndex = -1;
        while (goon) {
            logger.debug("receive result");
            Object rh = null;
            try {
                rh = pairChannel.receive(resultTag);
                int i = active.getAndDecrement();
            } catch (InterruptedException e) {
                goon = false;
                break;
            } catch (IOException e) {
                e.printStackTrace();
                goon = false;
                finner.initIdle(1);
                break;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                goon = false;
                finner.initIdle(1);
                break;
            }
            logger.info("received H polynomial");
            if (rh == null) {
                if (this.isInterrupted()) {
                    goon = false;
                    finner.initIdle(1);
                    break;
                }
            } else if (rh instanceof GBTransportMessEnd) {
                logger.info("received GBTransportMessEnd");
                goon = false;
                break;
            } else if (rh instanceof GBTransportMessPoly) {
                red++;
                GBTransportMessPoly<C> mpi = (GBTransportMessPoly<C>) rh;
                H = mpi.pol;
                if (H != null) {
                    if (debug) {
                        logger.info("H = " + H.leadingExpVector());
                    }
                    if (!H.isZERO()) {
                        if (H.isONE()) {
                            polIndex = pairlist.putOne();
                            GenPolynomial<C> nn = theList.put(new Integer(polIndex), H);
                            if (nn != null) {
                                logger.info("double polynomials nn = " + nn + ", H = " + H);
                            }
                        } else {
                            polIndex = pairlist.put(H);
                            GenPolynomial<C> nn = theList.put(new Integer(polIndex), H);
                            if (nn != null) {
                                logger.info("double polynomials nn = " + nn + ", H = " + H);
                            }
                        }
                    }
                }
            }
            finner.initIdle(1);
            try {
                pairChannel.send(ackTag, new GBTransportMess());
                logger.debug("send acknowledgement");
            } catch (IOException e) {
                e.printStackTrace();
                goon = false;
                break;
            }
        }
        goon = false;
        logger.info("terminated, received " + red + " reductions");
    }

    /**
     * Terminate.
     */
    public void terminate() {
        goon = false;
        this.interrupt();
        try {
            this.join();
        } catch (InterruptedException e) {
        }
        logger.debug("HybridReducerReceiver terminated");
    }
}

/**
 * Distributed clients reducing worker threads.
 */
class HybridReducerClient<C extends RingElem<C>> implements Runnable {

    private static final Logger logger = Logger.getLogger(HybridReducerClient.class);

    public final boolean debug = logger.isDebugEnabled();

    private final TaggedSocketChannel pairChannel;

    private final DistHashTable<Integer, GenPolynomial<C>> theList;

    private final ReductionPar<C> red;

    private final int threadsPerNode;

    /**
     * Message tag for pairs.
     */
    public final Integer pairTag = GroebnerBaseDistributedHybrid.pairTag;

    /**
     * Message tag for results.
     */
    public final Integer resultTag = GroebnerBaseDistributedHybrid.resultTag;

    /**
     * Message tag for acknowledgments.
     */
    public final Integer ackTag = GroebnerBaseDistributedHybrid.ackTag;

    /**
     * Constructor.
     * @param tpn number of threads per node
     * @param tc tagged socket channel
     * @param tid thread identification
     * @param dl distributed hash table
     */
    HybridReducerClient(int tpn, TaggedSocketChannel tc, Integer tid, DistHashTable<Integer, GenPolynomial<C>> dl) {
        this.threadsPerNode = tpn;
        pairChannel = tc;
        theList = dl;
        red = new ReductionPar<C>();
    }

    public void run() {
        if (debug) {
            logger.info("pairChannel   = " + pairChannel + " reducer client running");
        }
        Pair<C> pair = null;
        GenPolynomial<C> pi;
        GenPolynomial<C> pj;
        GenPolynomial<C> S;
        GenPolynomial<C> H = null;
        boolean goon = true;
        boolean doEnd = false;
        int reduction = 0;
        Integer pix;
        Integer pjx;
        while (goon) {
            Object req = new GBTransportMessReq();
            logger.info("send request = " + req);
            try {
                pairChannel.send(pairTag, req);
            } catch (IOException e) {
                goon = false;
                if (logger.isDebugEnabled()) {
                    e.printStackTrace();
                }
                logger.info("receive pair, exception ");
                break;
            }
            logger.debug("receive pair, goon = " + goon);
            doEnd = false;
            Object pp = null;
            try {
                pp = pairChannel.receive(pairTag);
            } catch (InterruptedException e) {
                goon = false;
                e.printStackTrace();
            } catch (IOException e) {
                goon = false;
                if (logger.isDebugEnabled()) {
                    e.printStackTrace();
                }
                break;
            } catch (ClassNotFoundException e) {
                goon = false;
                e.printStackTrace();
            }
            if (debug) {
                logger.info("received pair = " + pp);
            }
            H = null;
            if (pp == null) {
                continue;
            }
            if (pp instanceof GBTransportMessEnd) {
                goon = false;
                doEnd = true;
                continue;
            }
            if (pp instanceof GBTransportMessPair || pp instanceof GBTransportMessPairIndex) {
                pi = pj = null;
                if (pp instanceof GBTransportMessPair) {
                    pair = ((GBTransportMessPair<C>) pp).pair;
                    if (pair != null) {
                        pi = pair.pi;
                        pj = pair.pj;
                    }
                }
                if (pp instanceof GBTransportMessPairIndex) {
                    pix = ((GBTransportMessPairIndex) pp).i;
                    pjx = ((GBTransportMessPairIndex) pp).j;
                    pi = (GenPolynomial<C>) theList.getWait(pix);
                    pj = (GenPolynomial<C>) theList.getWait(pjx);
                }
                if (pi != null && pj != null) {
                    S = red.SPolynomial(pi, pj);
                    if (S.isZERO()) {
                    } else {
                        if (logger.isDebugEnabled()) {
                            logger.debug("ht(S) = " + S.leadingExpVector());
                        }
                        H = red.normalform(theList, S);
                        reduction++;
                        if (H.isZERO()) {
                        } else {
                            H = H.monic();
                            if (logger.isInfoEnabled()) {
                                logger.info("ht(H) = " + H.leadingExpVector());
                            }
                        }
                    }
                }
            }
            if (pp instanceof GBTransportMess) {
                logger.debug("null pair results in null H poly");
            }
            if (logger.isDebugEnabled()) {
                logger.debug("#distributed list = " + theList.size());
                logger.debug("send H polynomial = " + H);
            }
            try {
                pairChannel.send(resultTag, new GBTransportMessPoly<C>(H));
                doEnd = true;
            } catch (IOException e) {
                goon = false;
                e.printStackTrace();
            }
            logger.info("done send poly message of " + pp);
            try {
                pp = pairChannel.receive(ackTag);
            } catch (InterruptedException e) {
                goon = false;
                e.printStackTrace();
            } catch (IOException e) {
                goon = false;
                if (logger.isDebugEnabled()) {
                    e.printStackTrace();
                }
                break;
            } catch (ClassNotFoundException e) {
                goon = false;
                e.printStackTrace();
            }
            if (!(pp instanceof GBTransportMess)) {
                logger.error("invalid acknowledgement " + pp);
            }
            logger.info("received acknowledgment " + pp);
        }
        logger.info("terminated, done " + reduction + " reductions");
        if (!doEnd) {
            try {
                pairChannel.send(resultTag, new GBTransportMessEnd());
            } catch (IOException e) {
            }
            logger.info("terminated, send done");
        }
    }
}
