package com.yahoo.zookeeper.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.log4j.Logger;
import com.yahoo.jute.BinaryInputArchive;
import com.yahoo.jute.BinaryOutputArchive;
import com.yahoo.jute.InputArchive;
import com.yahoo.jute.Record;
import com.yahoo.zookeeper.ZooDefs.OpCode;
import com.yahoo.zookeeper.data.ACL;
import com.yahoo.zookeeper.data.Id;
import com.yahoo.zookeeper.data.Stat;
import com.yahoo.zookeeper.proto.RequestHeader;
import com.yahoo.zookeeper.server.SessionTracker.SessionExpirer;
import com.yahoo.zookeeper.server.quorum.Leader;
import com.yahoo.zookeeper.server.quorum.QuorumPacket;
import com.yahoo.zookeeper.server.quorum.Leader.Proposal;
import com.yahoo.zookeeper.txn.CreateSessionTxn;
import com.yahoo.zookeeper.txn.CreateTxn;
import com.yahoo.zookeeper.txn.DeleteTxn;
import com.yahoo.zookeeper.txn.ErrorTxn;
import com.yahoo.zookeeper.txn.SetACLTxn;
import com.yahoo.zookeeper.txn.SetDataTxn;
import com.yahoo.zookeeper.txn.TxnHeader;

/**
 * This class implements a simple standalone ZooKeeperServer. It sets up the
 * following chain of RequestProcessors to process requests:
 * PrepRequestProcessor -> SyncRequestProcessor -> FinalRequestProcessor
 */
public class ZooKeeperServer implements SessionExpirer, ServerStats.Provider {

    private static final Logger LOG = Logger.getLogger(ZooKeeperServer.class);

    /**
     * Create an instance of Zookeeper server
     */
    public interface Factory {

        public ZooKeeperServer createServer() throws IOException;

        public NIOServerCnxn.Factory createConnectionFactory() throws IOException;
    }

    /**
     * The server delegates loading of the tree to an instance of the interface
     */
    public interface DataTreeBuilder {

        public DataTree build();
    }

    public static class BasicDataTreeBuilder implements DataTreeBuilder {

        public DataTree build() {
            return new DataTree();
        }
    }

    private static final int DEFAULT_TICK_TIME = 3000;

    protected int tickTime = DEFAULT_TICK_TIME;

    public static final int commitLogCount = 500;

    public int commitLogBuffer = 700;

    public LinkedList<Proposal> committedLog = new LinkedList<Proposal>();

    public long minCommittedLog, maxCommittedLog;

    private DataTreeBuilder treeBuilder;

    public DataTree dataTree;

    protected SessionTracker sessionTracker;

    /**
     * directory for storing the snapshot
     */
    File dataDir;

    /**
     * directoy for storing the log tnxns
     */
    File dataLogDir;

    protected ConcurrentHashMap<Long, Integer> sessionsWithTimeouts;

    protected long hzxid = 0;

    public static final Exception ok = new Exception("No prob");

    protected RequestProcessor firstProcessor;

    LinkedBlockingQueue<Long> sessionsToDie = new LinkedBlockingQueue<Long>();

    protected boolean running;

    /**
     * This is the secret that we use to generate passwords, for the moment it
     * is more of a sanity check.
     */
    private final long superSecret = 0XB3415C00L;

    int requestsInProcess;

    List<ChangeRecord> outstandingChanges = new ArrayList<ChangeRecord>();

    private NIOServerCnxn.Factory serverCnxnFactory;

    public static void main(String[] args) {
        ServerConfig.parse(args);
        runStandalone(new Factory() {

            public NIOServerCnxn.Factory createConnectionFactory() throws IOException {
                return new NIOServerCnxn.Factory(ServerConfig.getClientPort());
            }

            public ZooKeeperServer createServer() throws IOException {
                return new ZooKeeperServer(new BasicDataTreeBuilder());
            }
        });
    }

    public static void runStandalone(Factory factory) {
        try {
            ServerStats.registerAsConcrete();
            ZooKeeperServer zk = factory.createServer();
            zk.startup();
            NIOServerCnxn.Factory t = factory.createConnectionFactory();
            t.setZooKeeperServer(zk);
            t.join();
            if (zk.isRunning()) zk.shutdown();
        } catch (Exception e) {
            LOG.fatal("Unexpected exception", e);
        }
        System.exit(0);
    }

    void removeCnxn(ServerCnxn cnxn) {
        dataTree.removeCnxn(cnxn);
    }

    /**
     * Creates a ZooKeeperServer instance. It sets everything up, but doesn't
     * actually start listening for clients until run() is invoked.
     *
     * @param dataDir
     *            the directory to put the data
     * @throws IOException
     */
    public ZooKeeperServer(File dataDir, File dataLogDir, int tickTime, DataTreeBuilder treeBuilder) throws IOException {
        this.treeBuilder = treeBuilder;
        this.dataDir = dataDir;
        this.dataLogDir = dataLogDir;
        this.tickTime = tickTime;
        if (!dataDir.isDirectory()) {
            throw new IOException("data directory does not exist");
        }
        ServerStats.getInstance().setStatsProvider(this);
    }

    /**
     * This constructor is for backward comaptibility with the existing unit
     * test code.
     */
    public ZooKeeperServer(File dataDir, File dataLogDir, int tickTime) throws IOException {
        this.treeBuilder = new BasicDataTreeBuilder();
        this.dataDir = dataDir;
        this.dataLogDir = dataLogDir;
        this.tickTime = tickTime;
        if (!dataDir.isDirectory()) {
            throw new IOException("data directory does not exist");
        }
        ServerStats.getInstance().setStatsProvider(this);
    }

    /**
     * Default constructor, relies on the config for its agrument values
     *
     * @throws IOException
     */
    public ZooKeeperServer(DataTreeBuilder treeBuilder) throws IOException {
        this(new File(ServerConfig.getDataDir()), new File(ServerConfig.getDataLogDir()), DEFAULT_TICK_TIME, treeBuilder);
    }

    public static long getZxidFromName(String name, String prefix) {
        long zxid = -1;
        String nameParts[] = name.split("\\.");
        if (nameParts.length == 2 && nameParts[0].equals(prefix)) {
            try {
                zxid = Long.parseLong(nameParts[1], 16);
            } catch (NumberFormatException e) {
            }
        }
        return zxid;
    }

    public static long isValidSnapshot(File f) throws IOException {
        long zxid = getZxidFromName(f.getName(), "snapshot");
        if (zxid == -1) return -1;
        RandomAccessFile raf = new RandomAccessFile(f, "r");
        try {
            raf.seek(raf.length() - 5);
            byte bytes[] = new byte[5];
            raf.read(bytes);
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            int len = bb.getInt();
            byte b = bb.get();
            if (len != 1 || b != '/') {
                LOG.warn("Invalid snapshot " + f + " len = " + len + " byte = " + (b & 0xff));
                return -1;
            }
        } finally {
            raf.close();
        }
        return zxid;
    }

    /**
     * Compare file file names of form "prefix.version". Sort order result
     * returned in order of version.
     */
    private static class DataDirFileComparator implements Comparator<File> {

        private String prefix;

        private boolean ascending;

        public DataDirFileComparator(String prefix, boolean ascending) {
            this.prefix = prefix;
            this.ascending = ascending;
        }

        public int compare(File o1, File o2) {
            long z1 = getZxidFromName(o1.getName(), prefix);
            long z2 = getZxidFromName(o2.getName(), prefix);
            int result = z1 < z2 ? -1 : (z1 > z2 ? 1 : 0);
            return ascending ? result : -result;
        }
    }

    /**
     * Sort the list of files. Recency as determined by the version component
     * of the file name.
     *
     * @param files array of files
     * @param prefix files not matching this prefix are assumed to have a
     * version = -1)
     * @param ascending true sorted in ascending order, false results in
     * descending order
     * @return sorted input files
     */
    static List<File> sortDataDir(File[] files, String prefix, boolean ascending) {
        List<File> filelist = Arrays.asList(files);
        Collections.sort(filelist, new DataDirFileComparator(prefix, ascending));
        return filelist;
    }

    /**
     * Find the log file that starts at, or just before, the snapshot. Return
     * this and all subsequent logs. Results are ordered by zxid of file,
     * ascending order.
     *
     * @param logDirList array of files
     * @param snapshotZxid return files at, or before this zxid
     * @return
     */
    static File[] getLogFiles(File[] logDirList, long snapshotZxid) {
        List<File> files = sortDataDir(logDirList, "log", true);
        long logZxid = 0;
        for (File f : files) {
            long fzxid = getZxidFromName(f.getName(), "log");
            if (fzxid > snapshotZxid) {
                continue;
            }
            if (fzxid > logZxid) {
                logZxid = fzxid;
            }
        }
        List<File> v = new ArrayList<File>(5);
        for (File f : files) {
            long fzxid = getZxidFromName(f.getName(), "log");
            if (fzxid < logZxid) {
                continue;
            }
            v.add(f);
        }
        return v.toArray(new File[0]);
    }

    /**
     *  Restore sessions and data
     */
    private void loadSnapshotAndLogs() throws IOException {
        long zxid = -1;
        List<File> files = sortDataDir(dataDir.listFiles(), "snapshot", false);
        for (File f : files) {
            zxid = isValidSnapshot(f);
            if (zxid == -1) {
                LOG.warn("Skipping " + f);
                continue;
            }
            LOG.warn("Processing snapshot: " + f);
            InputStream snapIS = new BufferedInputStream(new FileInputStream(f));
            loadData(BinaryInputArchive.getArchive(snapIS));
            snapIS.close();
            dataTree.lastProcessedZxid = zxid;
            File[] logfiles = getLogFiles(dataLogDir.listFiles(), zxid);
            for (File logfile : logfiles) {
                LOG.warn("Processing log file: " + logfile);
                InputStream logIS = new BufferedInputStream(new FileInputStream(logfile));
                zxid = playLog(BinaryInputArchive.getArchive(logIS));
                logIS.close();
            }
            hzxid = zxid;
            break;
        }
        if (zxid == -1) {
            sessionsWithTimeouts = new ConcurrentHashMap<Long, Integer>();
            dataTree = treeBuilder.build();
        }
    }

    public void loadData() throws IOException, InterruptedException {
        loadSnapshotAndLogs();
        LinkedList<Long> deadSessions = new LinkedList<Long>();
        for (long session : dataTree.getSessions()) {
            if (sessionsWithTimeouts.get(session) == null) {
                deadSessions.add(session);
            }
        }
        dataTree.initialized = true;
        for (long session : deadSessions) {
            killSession(session);
        }
        snapshot();
    }

    public void loadData(InputArchive ia) throws IOException {
        sessionsWithTimeouts = new ConcurrentHashMap<Long, Integer>();
        dataTree = treeBuilder.build();
        int count = ia.readInt("count");
        while (count > 0) {
            long id = ia.readLong("id");
            int to = ia.readInt("timeout");
            sessionsWithTimeouts.put(id, to);
            ZooTrace.logTraceMessage(LOG, ZooTrace.SESSION_TRACE_MASK, "loadData --- session in archive: " + id + " with timeout: " + to);
            count--;
        }
        dataTree.deserialize(ia, "tree");
    }

    public long playLog(InputArchive logStream) throws IOException {
        long highestZxid = 0;
        try {
            while (true) {
                byte[] bytes = logStream.readBuffer("txnEntry");
                if (bytes.length == 0) {
                    throw new EOFException();
                }
                InputArchive ia = BinaryInputArchive.getArchive(new ByteArrayInputStream(bytes));
                TxnHeader hdr = new TxnHeader();
                Record txn = deserializeTxn(ia, hdr);
                if (logStream.readByte("EOR") != 'B') {
                    LOG.error("Last transaction was partial.");
                    throw new EOFException();
                }
                if (hdr.getZxid() <= highestZxid && highestZxid != 0) {
                    LOG.error(highestZxid + "(higestZxid) >= " + hdr.getZxid() + "(next log) for type " + hdr.getType());
                } else {
                    highestZxid = hdr.getZxid();
                }
                switch(hdr.getType()) {
                    case OpCode.createSession:
                        sessionsWithTimeouts.put(hdr.getClientId(), ((CreateSessionTxn) txn).getTimeOut());
                        ZooTrace.logTraceMessage(LOG, ZooTrace.SESSION_TRACE_MASK, "playLog --- create session in log: " + Long.toHexString(hdr.getClientId()) + " with timeout: " + ((CreateSessionTxn) txn).getTimeOut());
                        dataTree.processTxn(hdr, txn);
                        break;
                    case OpCode.closeSession:
                        sessionsWithTimeouts.remove(hdr.getClientId());
                        ZooTrace.logTraceMessage(LOG, ZooTrace.SESSION_TRACE_MASK, "playLog --- close session in log: " + Long.toHexString(hdr.getClientId()));
                        dataTree.processTxn(hdr, txn);
                        break;
                    default:
                        dataTree.processTxn(hdr, txn);
                }
                Request r = new Request(null, 0, hdr.getCxid(), hdr.getType(), null, null);
                r.txn = txn;
                r.hdr = hdr;
                r.zxid = hdr.getZxid();
                addCommittedProposal(r);
            }
        } catch (EOFException e) {
        }
        return highestZxid;
    }

    /**
     * maintains a list of last 500 or so committed requests. This is used for
     * fast follower synchronization.
     *
     * @param request
     *            committed request
     */
    public void addCommittedProposal(Request request) {
        synchronized (committedLog) {
            if (committedLog.size() > commitLogCount) {
                committedLog.removeFirst();
                minCommittedLog = committedLog.getFirst().packet.getZxid();
            }
            if (committedLog.size() == 0) {
                minCommittedLog = request.zxid;
                maxCommittedLog = request.zxid;
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BinaryOutputArchive boa = BinaryOutputArchive.getArchive(baos);
            try {
                request.hdr.serialize(boa, "hdr");
                if (request.txn != null) {
                    request.txn.serialize(boa, "txn");
                }
                baos.close();
            } catch (IOException e) {
                LOG.error("FIXMSG", e);
            }
            QuorumPacket pp = new QuorumPacket(Leader.PROPOSAL, request.zxid, baos.toByteArray(), null);
            Proposal p = new Proposal();
            p.packet = pp;
            p.request = request;
            committedLog.add(p);
            maxCommittedLog = p.packet.getZxid();
        }
    }

    public static Record deserializeTxn(InputArchive ia, TxnHeader hdr) throws IOException {
        hdr.deserialize(ia, "hdr");
        Record txn = null;
        switch(hdr.getType()) {
            case OpCode.createSession:
                txn = new CreateSessionTxn();
                break;
            case OpCode.closeSession:
                return null;
            case OpCode.create:
                txn = new CreateTxn();
                break;
            case OpCode.delete:
                txn = new DeleteTxn();
                break;
            case OpCode.setData:
                txn = new SetDataTxn();
                break;
            case OpCode.setACL:
                txn = new SetACLTxn();
                break;
            case OpCode.error:
                txn = new ErrorTxn();
                break;
        }
        if (txn != null) {
            txn.deserialize(ia, "txn");
        }
        return txn;
    }

    public void truncateLog(long finalZxid) throws IOException {
        long highestZxid = 0;
        for (File f : dataDir.listFiles()) {
            long zxid = isValidSnapshot(f);
            if (zxid == -1) {
                LOG.warn("Skipping " + f);
                continue;
            }
            if (zxid > highestZxid) {
                highestZxid = zxid;
            }
        }
        File[] files = getLogFiles(dataLogDir.listFiles(), highestZxid);
        boolean truncated = false;
        for (File f : files) {
            FileInputStream fin = new FileInputStream(f);
            InputArchive ia = BinaryInputArchive.getArchive(fin);
            FileChannel fchan = fin.getChannel();
            try {
                while (true) {
                    byte[] bytes = ia.readBuffer("txtEntry");
                    if (bytes.length == 0) {
                        throw new EOFException();
                    }
                    InputArchive iab = BinaryInputArchive.getArchive(new ByteArrayInputStream(bytes));
                    TxnHeader hdr = new TxnHeader();
                    deserializeTxn(iab, hdr);
                    if (ia.readByte("EOF") != 'B') {
                        throw new EOFException();
                    }
                    if (hdr.getZxid() == finalZxid) {
                        long pos = fchan.position();
                        fin.close();
                        FileOutputStream fout = new FileOutputStream(f);
                        FileChannel fchanOut = fout.getChannel();
                        fchanOut.truncate(pos);
                        truncated = true;
                        break;
                    }
                }
            } catch (EOFException eof) {
            }
            if (truncated == true) {
                break;
            }
        }
        if (truncated == false) {
            LOG.error("Not able to truncate the log " + Long.toHexString(finalZxid));
            System.exit(13);
        }
    }

    public void snapshot(BinaryOutputArchive oa) throws IOException, InterruptedException {
        HashMap<Long, Integer> sessSnap = new HashMap<Long, Integer>(sessionsWithTimeouts);
        oa.writeInt(sessSnap.size(), "count");
        for (Entry<Long, Integer> entry : sessSnap.entrySet()) {
            oa.writeLong(entry.getKey().longValue(), "id");
            oa.writeInt(entry.getValue().intValue(), "timeout");
        }
        dataTree.serialize(oa, "tree");
    }

    public void snapshot() throws InterruptedException {
        long lastZxid = dataTree.lastProcessedZxid;
        ZooTrace.logTraceMessage(LOG, ZooTrace.getTextTraceLevel(), "Snapshotting: " + Long.toHexString(lastZxid));
        try {
            File f = new File(dataDir, "snapshot." + Long.toHexString(lastZxid));
            OutputStream sessOS = new BufferedOutputStream(new FileOutputStream(f));
            BinaryOutputArchive oa = BinaryOutputArchive.getArchive(sessOS);
            snapshot(oa);
            sessOS.flush();
            sessOS.close();
            ZooTrace.logTraceMessage(LOG, ZooTrace.getTextTraceLevel(), "Snapshotting finished: " + Long.toHexString(lastZxid));
        } catch (IOException e) {
            LOG.error("Severe error, exiting", e);
            System.exit(10);
        }
    }

    /**
     * This should be called from a synchronized block on this!
     */
    public long getZxid() {
        return hzxid;
    }

    synchronized long getNextZxid() {
        return ++hzxid;
    }

    long getTime() {
        return System.currentTimeMillis();
    }

    static String getLogName(long zxid) {
        return "log." + Long.toHexString(zxid);
    }

    public void closeSession(long sessionId) throws InterruptedException {
        ZooTrace.logTraceMessage(LOG, ZooTrace.SESSION_TRACE_MASK, "ZooKeeperServer --- Session to be closed: " + Long.toHexString(sessionId));
        submitRequest(null, sessionId, OpCode.closeSession, 0, null, null);
    }

    protected void killSession(long sessionId) {
        dataTree.killSession(sessionId);
        ZooTrace.logTraceMessage(LOG, ZooTrace.SESSION_TRACE_MASK, "ZooKeeperServer --- killSession: " + Long.toHexString(sessionId));
        if (sessionTracker != null) {
            sessionTracker.removeSession(sessionId);
        }
    }

    public void expire(long sessionId) {
        try {
            ZooTrace.logTraceMessage(LOG, ZooTrace.SESSION_TRACE_MASK, "ZooKeeperServer --- Session to expire: " + Long.toHexString(sessionId));
            closeSession(sessionId);
        } catch (Exception e) {
            LOG.error("FIXMSG", e);
        }
    }

    void touch(ServerCnxn cnxn) throws IOException {
        if (cnxn == null) {
            return;
        }
        long id = cnxn.getSessionId();
        int to = cnxn.getSessionTimeout();
        if (!sessionTracker.touchSession(id, to)) {
            throw new IOException("Missing session " + Long.toHexString(id));
        }
    }

    public void startup() throws IOException, InterruptedException {
        if (dataTree == null) {
            loadData();
        }
        createSessionTracker();
        setupRequestProcessors();
        running = true;
        synchronized (this) {
            notifyAll();
        }
    }

    protected void setupRequestProcessors() {
        RequestProcessor finalProcessor = new FinalRequestProcessor(this);
        RequestProcessor syncProcessor = new SyncRequestProcessor(this, finalProcessor);
        firstProcessor = new PrepRequestProcessor(this, syncProcessor);
    }

    protected void createSessionTracker() {
        sessionTracker = new SessionTrackerImpl(this, sessionsWithTimeouts, tickTime, 1);
    }

    public boolean isRunning() {
        return running;
    }

    public void shutdown() {
        this.running = false;
        if (sessionTracker != null) {
            sessionTracker.shutdown();
        }
        if (firstProcessor != null) {
            firstProcessor.shutdown();
        }
        if (dataTree != null) {
            dataTree.clear();
        }
    }

    public synchronized void incInProcess() {
        requestsInProcess++;
    }

    public synchronized void decInProcess() {
        requestsInProcess--;
    }

    public int getInProcess() {
        return requestsInProcess;
    }

    /**
     * This structure is used to facilitate information sharing between PrepRP
     * and FinalRP.
     */
    static class ChangeRecord {

        ChangeRecord(long zxid, String path, Stat stat, int childCount, List<ACL> acl) {
            this.zxid = zxid;
            this.path = path;
            this.stat = stat;
            this.childCount = childCount;
            this.acl = acl;
        }

        long zxid;

        String path;

        Stat stat;

        int childCount;

        List<ACL> acl;

        @SuppressWarnings("unchecked")
        ChangeRecord duplicate(long zxid) {
            Stat stat = new Stat();
            if (this.stat != null) {
                DataTree.copyStat(this.stat, stat);
            }
            return new ChangeRecord(zxid, path, stat, childCount, acl == null ? new ArrayList<ACL>() : new ArrayList(acl));
        }
    }

    byte[] generatePasswd(long id) {
        Random r = new Random(id ^ superSecret);
        byte p[] = new byte[16];
        r.nextBytes(p);
        return p;
    }

    protected boolean checkPasswd(long sessionId, byte[] passwd) {
        return sessionId != 0 && Arrays.equals(passwd, generatePasswd(sessionId));
    }

    long createSession(ServerCnxn cnxn, byte passwd[], int timeout) throws InterruptedException {
        long sessionId = sessionTracker.createSession(timeout);
        Random r = new Random(sessionId ^ superSecret);
        r.nextBytes(passwd);
        ByteBuffer to = ByteBuffer.allocate(4);
        to.putInt(timeout);
        cnxn.setSessionId(sessionId);
        submitRequest(cnxn, sessionId, OpCode.createSession, 0, to, null);
        return sessionId;
    }

    protected void revalidateSession(ServerCnxn cnxn, long sessionId, int sessionTimeout) throws IOException, InterruptedException {
        boolean rc = sessionTracker.touchSession(sessionId, sessionTimeout);
        ZooTrace.logTraceMessage(LOG, ZooTrace.SESSION_TRACE_MASK, "Session " + Long.toHexString(sessionId) + " is valid: " + rc);
        cnxn.finishSessionInit(rc);
    }

    public void reopenSession(ServerCnxn cnxn, long sessionId, byte[] passwd, int sessionTimeout) throws IOException, InterruptedException {
        if (!checkPasswd(sessionId, passwd)) {
            cnxn.finishSessionInit(false);
        } else {
            revalidateSession(cnxn, sessionId, sessionTimeout);
        }
    }

    public void closeSession(ServerCnxn cnxn, RequestHeader requestHeader) throws InterruptedException {
        closeSession(cnxn.getSessionId());
    }

    public long getServerId() {
        return 0;
    }

    /**
     * @param cnxn
     * @param sessionId
     * @param xid
     * @param bb
     */
    public void submitRequest(ServerCnxn cnxn, long sessionId, int type, int xid, ByteBuffer bb, List<Id> authInfo) {
        if (firstProcessor == null) {
            synchronized (this) {
                try {
                    while (!running) {
                        wait(1000);
                    }
                } catch (InterruptedException e) {
                    LOG.error("FIXMSG", e);
                }
                if (firstProcessor == null) {
                    throw new RuntimeException("Not started");
                }
            }
        }
        try {
            touch(cnxn);
            Request si = new Request(cnxn, sessionId, xid, type, bb, authInfo);
            boolean validpacket = Request.isValid(type);
            if (validpacket) {
                firstProcessor.processRequest(si);
                if (cnxn != null) {
                    incInProcess();
                }
            } else {
                LOG.warn("Dropping packet at server of type " + type);
            }
        } catch (IOException e) {
            LOG.error("FIXMSG", e);
        }
    }

    public static void byteBuffer2Record(ByteBuffer bb, Record record) throws IOException {
        BinaryInputArchive ia;
        ia = BinaryInputArchive.getArchive(new ByteBufferInputStream(bb));
        record.deserialize(ia, "request");
    }

    public static int getSnapCount() {
        String sc = System.getProperty("zookeeper.snapCount");
        try {
            return Integer.parseInt(sc);
        } catch (Exception e) {
            return 10000;
        }
    }

    public int getGlobalOutstandingLimit() {
        String sc = System.getProperty("zookeeper.globalOutstandingLimit");
        int limit;
        try {
            limit = Integer.parseInt(sc);
        } catch (Exception e) {
            limit = 1000;
        }
        return limit;
    }

    public void setServerCnxnFactory(NIOServerCnxn.Factory factory) {
        serverCnxnFactory = factory;
    }

    public NIOServerCnxn.Factory getServerCnxnFactory() {
        return serverCnxnFactory;
    }

    public long getLastProcessedZxid() {
        return dataTree.lastProcessedZxid;
    }

    public long getOutstandingRequests() {
        return getInProcess();
    }
}
