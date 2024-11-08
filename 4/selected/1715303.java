package pubweb.worker.re;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import padmig.DontMigrate;
import padmig.PadMig;
import padmig.Migratable;
import padmig.MigrationException;
import padmig.Migratory;
import padmig.Undock;
import padrmi.ObjectInputStreamWithLoader;
import padrmi.Server;
import padrmi.URLClassLoaderFactory;
import padrmi.exception.PpException;
import pubweb.AbortedException;
import pubweb.IntegrityException;
import pubweb.InternalException;
import pubweb.JobProcess;
import pubweb.MigrationInProgressException;
import pubweb.NotConnectedException;
import pubweb.ResetInProgressException;
import pubweb.bsp.BSP;
import pubweb.bsp.BSPMigratable;
import pubweb.bsp.BSPMigratableProgram;
import pubweb.bsp.BSPProgram;
import pubweb.bsp.Message;
import pubweb.service.Worker2Supernode;
import pubweb.service.Worker2Worker;
import pubweb.util.SuperstepMessageListContainer;

public class RuntimeEnvironment implements BSP, BSPMigratable, Migratable, Runnable, Serializable {

    public static final int TREE_DEGREE = 4;

    public static final long READY_WAIT_INTERVAL = 100L;

    public static final long SYNC_WAIT_INTERVAL = 100L;

    public static final long INIT_SYNC_WAIT_INTERVAL = 5000L;

    public static final long EXITING_WAIT_INTERVAL = 60000L;

    public static final long BACKUP_WAIT_INTERVAL = 1000L;

    public static final long MIGRATION_IN_PROGRESS_WAIT_INTERVAL = 5000L;

    public static final int STATE_NONE = 1;

    public static final int STATE_INITIALIZED = 2;

    public static final int STATE_READY = 3;

    public static final int STATE_MIGRATING = 4;

    public static final int MIGRATION_TYPE_NONE = 1;

    public static final int MIGRATION_TYPE_INITIAL = 2;

    public static final int MIGRATION_TYPE_ORDINARY = 3;

    public static final int MIGRATION_TYPE_BACKUP = 4;

    private volatile boolean backupCopyWritten;

    private volatile boolean syncWaitingForPredecessor;

    private volatile boolean returnedFromSavingBackupMigration;

    private volatile boolean[] syncWaitingForSuccessors;

    private volatile boolean outOfSyncAfterRestore = false;

    private volatile int state;

    private volatile int migrationType;

    protected transient Worker2Supernode supernode;

    protected boolean debug;

    protected boolean isMaster;

    protected boolean migratable;

    protected int pid;

    protected int predecessor;

    protected int superstep;

    protected int[] successors;

    protected List<Message>[] outgoingMsgs, pastOutgoingMsgs;

    protected JobProcess[] procs;

    protected ProcessRestorer restorer;

    protected ReServices mainThread;

    protected URL codebase;

    private long msgSeqNo;

    private Map<String, Worker2Worker> peers;

    private Set<Message> incomingMsgs;

    private Message[] receivedMsgs;

    private RePrivilegedExceptionActionWrapper privilegedExceptionActionWrapper;

    private ReRawDataThread rawDataThread;

    private ReStdErrThread stdErrThread;

    private ReStdOutThread stdOutThread;

    private String mainClass;

    private byte[] progArgs;

    public RuntimeEnvironment() throws Exception {
        supernode = (Worker2Supernode) Server.getDefaultServer().getProxyFactory().createProxy(new URL(System.getProperty("pubweb.supernode.url") + "/" + Worker2Supernode.class.getSimpleName()), Worker2Supernode.class);
        state = STATE_NONE;
    }

    public RuntimeEnvironment(ReServices mainThread, JobProcess[] processes, int pid, boolean debug, URL codebase, String mainClass, byte[] progArgs) throws MalformedURLException {
        this.mainThread = mainThread;
        this.procs = processes;
        this.pid = pid;
        this.debug = debug;
        migratable = false;
        migrationType = MIGRATION_TYPE_NONE;
        this.codebase = codebase;
        this.mainClass = mainClass;
        this.progArgs = progArgs;
        incomingMsgs = new TreeSet<Message>();
        peers = new HashMap<String, Worker2Worker>();
        privilegedExceptionActionWrapper = new RePrivilegedExceptionActionWrapper(this);
        restorer = new ProcessRestorer(this);
        supernode = (Worker2Supernode) Server.getDefaultServer().getProxyFactory().createProxy(new URL(System.getProperty("pubweb.supernode.url") + "/" + Worker2Supernode.class.getSimpleName()), Worker2Supernode.class);
        state = STATE_INITIALIZED;
    }

    public void run() {
        while (state != STATE_READY) {
            try {
                Thread.sleep(READY_WAIT_INTERVAL);
            } catch (InterruptedException e) {
            }
        }
        if (migratable) {
            mainThread.jobDiedOnError(new IntegrityException("migratable jobs cannot be started / resumed via the run() method"));
            return;
        }
        BSPProgram program;
        Serializable deserializedProgArgs = null;
        try {
            ClassLoader loader = URLClassLoaderFactory.getURLClassLoader(codebase);
            Class<?> prog = loader.loadClass(mainClass);
            program = (BSPProgram) prog.newInstance();
            if (progArgs != null) {
                ByteArrayInputStream bis = new ByteArrayInputStream(progArgs);
                ObjectInputStream ois = new ObjectInputStreamWithLoader(bis, loader);
                deserializedProgArgs = (Serializable) ois.readObject();
                ois.close();
            }
        } catch (Exception e) {
            if (debug) {
                System.err.println("error loading the program from " + codebase.toExternalForm() + ":");
                e.printStackTrace();
            }
            mainThread.jobDiedOnError(new Exception("error loading the program from " + codebase.toExternalForm(), e));
            return;
        } catch (Error e) {
            if (debug) {
                System.err.println("error loading the program from " + codebase.toExternalForm() + ":");
                e.printStackTrace();
            }
            mainThread.jobDiedOnError(new Exception("error loading the program from " + codebase.toExternalForm(), e));
            return;
        }
        try {
            ensureAllProcessesAreRunning();
        } catch (IntegrityException e) {
            if (debug) {
                System.err.println("error waiting for other PIDs during init:");
                e.printStackTrace();
            }
            mainThread.jobDiedOnError(new Exception("error waiting for other PIDs during init", e));
            return;
        }
        try {
            program.bspMain(this, deserializedProgArgs);
            mainThread.superstepCompleted(superstep, true);
        } catch (AbortedException e) {
            if (debug) {
                System.err.println("caught AbortedException -- terminating now:");
                e.printStackTrace();
            }
            mainThread.jobAborted(e);
            return;
        } catch (Exception e) {
            if (debug) {
                System.err.println("error executing the program:");
                e.printStackTrace();
            }
            mainThread.jobDiedOnError(new Exception("error executing the program", e));
            return;
        } catch (Error e) {
            if (debug) {
                System.err.println("error executing the program:");
                e.printStackTrace();
            }
            mainThread.jobDiedOnError(new Exception("error executing the program", e));
            return;
        }
        syncImpl();
        joinThreads();
        mainThread.processFinished();
    }

    private synchronized void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(procs);
        out.writeInt(pid);
        out.writeBoolean(debug);
        out.writeBoolean(migratable);
        out.writeInt(migrationType);
        out.writeObject(codebase);
        out.writeObject(mainClass);
        out.writeObject(progArgs);
        out.writeBoolean(isMaster);
        out.writeInt(predecessor);
        out.writeObject(successors);
        out.writeInt(superstep);
        out.writeBoolean(backupCopyWritten);
        out.writeObject(receivedMsgs);
        out.writeObject(outgoingMsgs);
        out.writeObject(pastOutgoingMsgs);
        out.writeLong(msgSeqNo);
        if (migrationType == MIGRATION_TYPE_ORDINARY) {
            out.writeObject(incomingMsgs);
            out.writeObject(syncWaitingForSuccessors);
            out.writeBoolean(syncWaitingForPredecessor);
        }
    }

    private synchronized void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        procs = (JobProcess[]) in.readObject();
        pid = in.readInt();
        debug = in.readBoolean();
        migratable = in.readBoolean();
        migrationType = in.readInt();
        codebase = (URL) in.readObject();
        mainClass = (String) in.readObject();
        progArgs = (byte[]) in.readObject();
        isMaster = in.readBoolean();
        predecessor = in.readInt();
        successors = (int[]) in.readObject();
        superstep = in.readInt();
        backupCopyWritten = in.readBoolean();
        receivedMsgs = (Message[]) in.readObject();
        @SuppressWarnings("unchecked") List<Message>[] tmp = (List[]) in.readObject();
        outgoingMsgs = tmp;
        @SuppressWarnings("unchecked") List<Message>[] tmp2 = (List[]) in.readObject();
        pastOutgoingMsgs = tmp2;
        msgSeqNo = in.readLong();
        if (migrationType == MIGRATION_TYPE_ORDINARY) {
            @SuppressWarnings("unchecked") Set<Message> tmp3 = (Set<Message>) in.readObject();
            incomingMsgs = tmp3;
            syncWaitingForSuccessors = (boolean[]) in.readObject();
            syncWaitingForPredecessor = in.readBoolean();
        }
        supernode = (Worker2Supernode) Server.getDefaultServer().getProxyFactory().createProxy(new URL(System.getProperty("pubweb.supernode.url") + "/" + Worker2Supernode.class.getSimpleName()), Worker2Supernode.class);
        state = STATE_INITIALIZED;
    }

    public void printStdOut(String line) {
        stdOutThread.print(line);
    }

    public void printStdErr(String line) {
        stdErrThread.print(line);
    }

    public void writeRawData(Serializable data) {
        rawDataThread.write(data);
    }

    public void flush() {
        stdOutThread.flush();
        stdErrThread.flush();
        rawDataThread.flush();
    }

    public void abort(Throwable cause) throws AbortedException {
        throw new AbortedException(cause);
    }

    public void sync() {
        if (migratable) throw new UnsupportedOperationException("the sync() method is not applicable for migratable programs - please use syncMig() instead");
        try {
            syncedWrapperForSuperstepEnd();
        } catch (PrivilegedActionException e) {
            Exception ne = e.getException();
            try {
                synchronized (privilegedExceptionActionWrapper) {
                    privilegedExceptionActionWrapper.setNextActionMainThreadErrorNotify(new Exception("could not send status message in sync()", ne));
                    AccessController.doPrivileged(privilegedExceptionActionWrapper);
                }
            } catch (PrivilegedActionException ex) {
                System.err.println("FATAL: could not send status message in RuntimeEnvironment.sync():");
                ne.printStackTrace();
                System.err.println("FATAL: could not inform main thread about this fact in RuntimeEnvironment.sync():");
                ex.getException().printStackTrace();
                System.exit(1);
            }
        }
        syncImpl();
    }

    private void syncImpl() {
        for (int i = 0; i < procs.length; i++) {
            if (outgoingMsgs[i].size() > 0) {
                boolean secondTry = false;
                while (true) {
                    try {
                        synchronized (privilegedExceptionActionWrapper) {
                            try {
                                privilegedExceptionActionWrapper.setNextActionSendMessages(i);
                                AccessController.doPrivileged(privilegedExceptionActionWrapper);
                            } catch (PrivilegedActionException pae) {
                                updateGuid(procs[i]);
                                privilegedExceptionActionWrapper.setNextActionUpdatePeer(i);
                                AccessController.doPrivileged(privilegedExceptionActionWrapper);
                                privilegedExceptionActionWrapper.setNextActionSendMessages(i);
                                AccessController.doPrivileged(privilegedExceptionActionWrapper);
                            }
                        }
                        break;
                    } catch (Exception e) {
                        if (e instanceof PrivilegedActionException) e = ((PrivilegedActionException) e).getException();
                        if (migratable) {
                            if (e instanceof MigrationInProgressException) {
                                if (debug) {
                                    System.out.println("temporary failure sending messages to pid " + i + ": migration in progress");
                                }
                                try {
                                    Thread.sleep(MIGRATION_IN_PROGRESS_WAIT_INTERVAL);
                                } catch (InterruptedException ie) {
                                }
                            } else {
                                if (secondTry) {
                                    try {
                                        synchronized (privilegedExceptionActionWrapper) {
                                            privilegedExceptionActionWrapper.setNextActionMainThreadErrorNotify(new Exception("restoring PID " + i + " failed while sending messages in syncImpl()", e));
                                            AccessController.doPrivileged(privilegedExceptionActionWrapper);
                                        }
                                        break;
                                    } catch (PrivilegedActionException ex) {
                                        System.err.println("FATAL: restoring PID " + predecessor + " failed while sending messages in RuntimeEnvironment.syncImpl():");
                                        e.printStackTrace();
                                        System.err.println("FATAL: could not inform main thread about this fact in RuntimeEnvironment.syncImpl():");
                                        ex.getException().printStackTrace();
                                        System.exit(1);
                                    }
                                } else {
                                    System.err.println("sending messages to process " + i + " failed -- assuming process is dead");
                                    if (debug) {
                                        e.printStackTrace();
                                    }
                                    restorer.addDeadProcess(i);
                                    restorer.waitUntilAllDeadProcessesRestored();
                                    secondTry = true;
                                }
                            }
                        } else {
                            try {
                                synchronized (privilegedExceptionActionWrapper) {
                                    privilegedExceptionActionWrapper.setNextActionMainThreadErrorNotify(new Exception("could not deliver messages in syncImpl()", e));
                                    AccessController.doPrivileged(privilegedExceptionActionWrapper);
                                }
                            } catch (PrivilegedActionException ex) {
                                System.err.println("FATAL: could not deliver messages in RuntimeEnvironment.syncImpl():");
                                e.printStackTrace();
                                System.err.println("FATAL: could not inform main thread about this fact in RuntimeEnvironment.syncImpl():");
                                ex.getException().printStackTrace();
                                System.exit(1);
                            }
                        }
                    }
                }
            }
        }
        boolean dontWaitForChildrenSyncUp = false;
        if (outOfSyncAfterRestore) {
            int correctSuperstep = -1;
            for (int i = 0; i < procs.length; i++) {
                boolean secondTry = false;
                while (true) {
                    try {
                        SuperstepMessageListContainer smlc;
                        synchronized (privilegedExceptionActionWrapper) {
                            try {
                                privilegedExceptionActionWrapper.setNextActionPollMessages(i);
                                smlc = ((SuperstepMessageListContainer) AccessController.doPrivileged(privilegedExceptionActionWrapper));
                            } catch (PrivilegedActionException pae) {
                                updateGuid(procs[i]);
                                privilegedExceptionActionWrapper.setNextActionUpdatePeer(i);
                                AccessController.doPrivileged(privilegedExceptionActionWrapper);
                                privilegedExceptionActionWrapper.setNextActionPollMessages(i);
                                smlc = ((SuperstepMessageListContainer) AccessController.doPrivileged(privilegedExceptionActionWrapper));
                            }
                        }
                        if (smlc.superstep > correctSuperstep) {
                            correctSuperstep = smlc.superstep;
                        }
                        synchronized (incomingMsgs) {
                            incomingMsgs.addAll(smlc.messageList);
                        }
                        break;
                    } catch (Exception e) {
                        if (e instanceof PrivilegedActionException) e = ((PrivilegedActionException) e).getException();
                        if (e instanceof MigrationInProgressException) {
                            if (debug) {
                                System.out.println("temporary failure polling messages from pid " + i + ": migration in progress");
                            }
                            try {
                                Thread.sleep(MIGRATION_IN_PROGRESS_WAIT_INTERVAL);
                            } catch (InterruptedException ie) {
                            }
                        } else {
                            if (secondTry) {
                                try {
                                    synchronized (privilegedExceptionActionWrapper) {
                                        privilegedExceptionActionWrapper.setNextActionMainThreadErrorNotify(new Exception("restoring PID " + i + " failed during polling messages in syncImpl()", e));
                                        AccessController.doPrivileged(privilegedExceptionActionWrapper);
                                    }
                                    break;
                                } catch (PrivilegedActionException ex) {
                                    System.err.println("FATAL: restoring PID " + i + " failed during polling messages in RuntimeEnvironment.syncImpl():");
                                    e.printStackTrace();
                                    System.err.println("FATAL: could not inform main thread about this fact in RuntimeEnvironment.syncImpl():");
                                    ex.getException().printStackTrace();
                                    System.exit(1);
                                }
                            } else {
                                System.err.println("polling messages from process " + i + " failed -- assuming process is dead");
                                if (debug) {
                                    e.printStackTrace();
                                }
                                restorer.addDeadProcess(i);
                                restorer.waitUntilAllDeadProcessesRestored();
                                secondTry = true;
                            }
                        }
                    }
                }
            }
            if (correctSuperstep > superstep) {
                dontWaitForChildrenSyncUp = true;
            } else if (correctSuperstep == superstep) {
                outOfSyncAfterRestore = false;
            } else {
                try {
                    synchronized (privilegedExceptionActionWrapper) {
                        privilegedExceptionActionWrapper.setNextActionMainThreadErrorNotify(new IntegrityException("restored process ahead of execution after restore in syncImpl()"));
                        AccessController.doPrivileged(privilegedExceptionActionWrapper);
                    }
                } catch (PrivilegedActionException ex) {
                    System.err.println("FATAL: restored process ahead of execution after restore in RuntimeEnvironment.syncImpl()");
                    System.err.println("FATAL: could not inform main thread about this fact in RuntimeEnvironment.syncImpl():");
                    ex.getException().printStackTrace();
                    System.exit(1);
                }
            }
        }
        if (!dontWaitForChildrenSyncUp) {
            for (int k = 0; true; k++) {
                boolean ready = true;
                for (int i = 0; i < syncWaitingForSuccessors.length; i++) {
                    if (syncWaitingForSuccessors[i]) {
                        ready = false;
                        break;
                    }
                }
                if (ready) {
                    break;
                }
                if (k % 300 == 100) {
                    for (int i = 0; i < syncWaitingForSuccessors.length; i++) {
                        if (syncWaitingForSuccessors[i]) {
                            try {
                                synchronized (privilegedExceptionActionWrapper) {
                                    try {
                                        privilegedExceptionActionWrapper.setNextActionPingProcess(successors[i]);
                                        AccessController.doPrivileged(privilegedExceptionActionWrapper);
                                    } catch (PrivilegedActionException pae) {
                                        updateGuid(procs[successors[i]]);
                                        privilegedExceptionActionWrapper.setNextActionUpdatePeer(successors[i]);
                                        AccessController.doPrivileged(privilegedExceptionActionWrapper);
                                        privilegedExceptionActionWrapper.setNextActionPingProcess(successors[i]);
                                        AccessController.doPrivileged(privilegedExceptionActionWrapper);
                                    }
                                }
                            } catch (Exception e) {
                                if (e instanceof PrivilegedActionException) e = ((PrivilegedActionException) e).getException();
                                System.err.println("pinging process " + successors[i] + " failed while waiting for sync up -- assuming process is dead");
                                if (debug) {
                                    e.printStackTrace();
                                }
                                restorer.addDeadProcess(successors[i]);
                                restorer.waitUntilAllDeadProcessesRestored();
                            }
                        }
                    }
                }
                try {
                    Thread.sleep(SYNC_WAIT_INTERVAL);
                } catch (InterruptedException e) {
                }
            }
        }
        if (!isMaster) {
            syncWaitingForPredecessor = true;
            boolean dontWaitForRootSyncDown = false;
            boolean secondTry = false;
            while (true) {
                try {
                    synchronized (privilegedExceptionActionWrapper) {
                        try {
                            privilegedExceptionActionWrapper.setNextActionSyncUp();
                            dontWaitForRootSyncDown = !((Boolean) AccessController.doPrivileged(privilegedExceptionActionWrapper)).booleanValue();
                        } catch (PrivilegedActionException pae) {
                            updateGuid(procs[predecessor]);
                            privilegedExceptionActionWrapper.setNextActionUpdatePeer(predecessor);
                            AccessController.doPrivileged(privilegedExceptionActionWrapper);
                            privilegedExceptionActionWrapper.setNextActionSyncUp();
                            dontWaitForRootSyncDown = !((Boolean) AccessController.doPrivileged(privilegedExceptionActionWrapper)).booleanValue();
                        }
                    }
                    break;
                } catch (Exception e) {
                    if (e instanceof PrivilegedActionException) e = ((PrivilegedActionException) e).getException();
                    if (migratable) {
                        if (e instanceof MigrationInProgressException) {
                            if (debug) {
                                System.out.println("temporary failure during sync up with pid " + predecessor + ": migration in progress");
                            }
                            try {
                                Thread.sleep(MIGRATION_IN_PROGRESS_WAIT_INTERVAL);
                            } catch (InterruptedException ie) {
                            }
                        } else {
                            if (secondTry) {
                                try {
                                    synchronized (privilegedExceptionActionWrapper) {
                                        privilegedExceptionActionWrapper.setNextActionMainThreadErrorNotify(new Exception("restoring PID " + predecessor + " failed during sync up in syncImpl()", e));
                                        AccessController.doPrivileged(privilegedExceptionActionWrapper);
                                    }
                                    break;
                                } catch (PrivilegedActionException ex) {
                                    System.err.println("FATAL: restoring PID " + predecessor + " failed during sync up in RuntimeEnvironment.syncImpl():");
                                    e.printStackTrace();
                                    System.err.println("FATAL: could not inform main thread about this fact in RuntimeEnvironment.syncImpl():");
                                    ex.getException().printStackTrace();
                                    System.exit(1);
                                }
                            } else {
                                System.err.println("sync up with process " + predecessor + " failed -- assuming process is dead");
                                if (debug) {
                                    e.printStackTrace();
                                }
                                restorer.addDeadProcess(predecessor);
                                restorer.waitUntilAllDeadProcessesRestored();
                                secondTry = true;
                            }
                        }
                    } else {
                        try {
                            synchronized (privilegedExceptionActionWrapper) {
                                privilegedExceptionActionWrapper.setNextActionMainThreadErrorNotify(new Exception("could not contact predecessor in syncImpl()", e));
                                AccessController.doPrivileged(privilegedExceptionActionWrapper);
                            }
                            break;
                        } catch (PrivilegedActionException ex) {
                            System.err.println("FATAL: could not contact predecessor in RuntimeEnvironment.syncImpl():");
                            e.printStackTrace();
                            System.err.println("FATAL: could not inform main thread about this fact in RuntimeEnvironment.syncImpl():");
                            ex.getException().printStackTrace();
                            System.exit(1);
                        }
                    }
                }
            }
            if (!dontWaitForRootSyncDown) {
                for (int k = 0; true; k++) {
                    if (!syncWaitingForPredecessor) break;
                    if (k % 300 == 100) {
                        try {
                            synchronized (privilegedExceptionActionWrapper) {
                                try {
                                    privilegedExceptionActionWrapper.setNextActionPingProcess(predecessor);
                                    AccessController.doPrivileged(privilegedExceptionActionWrapper);
                                } catch (PrivilegedActionException pae) {
                                    updateGuid(procs[predecessor]);
                                    privilegedExceptionActionWrapper.setNextActionUpdatePeer(predecessor);
                                    AccessController.doPrivileged(privilegedExceptionActionWrapper);
                                    privilegedExceptionActionWrapper.setNextActionPingProcess(predecessor);
                                    AccessController.doPrivileged(privilegedExceptionActionWrapper);
                                }
                            }
                        } catch (Exception e) {
                            if (e instanceof PrivilegedActionException) e = ((PrivilegedActionException) e).getException();
                            System.err.println("pinging process " + predecessor + " failed while waiting for sync down -- assuming process is dead");
                            if (debug) {
                                e.printStackTrace();
                            }
                            restorer.addDeadProcess(predecessor);
                            restorer.waitUntilAllDeadProcessesRestored();
                        }
                    }
                    try {
                        Thread.sleep(SYNC_WAIT_INTERVAL);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
        if (!dontWaitForChildrenSyncUp) {
            for (int i = 0; i < syncWaitingForSuccessors.length; i++) syncWaitingForSuccessors[i] = true;
        }
        for (int i = 0; i < successors.length; i++) {
            boolean secondTry = false;
            while (true) {
                try {
                    synchronized (privilegedExceptionActionWrapper) {
                        try {
                            privilegedExceptionActionWrapper.setNextActionSyncDown(i);
                            AccessController.doPrivileged(privilegedExceptionActionWrapper);
                        } catch (PrivilegedActionException any) {
                            updateGuid(procs[successors[i]]);
                            privilegedExceptionActionWrapper.setNextActionUpdatePeer(successors[i]);
                            AccessController.doPrivileged(privilegedExceptionActionWrapper);
                            privilegedExceptionActionWrapper.setNextActionSyncDown(i);
                            AccessController.doPrivileged(privilegedExceptionActionWrapper);
                        }
                    }
                    break;
                } catch (Exception e) {
                    if (e instanceof PrivilegedActionException) e = ((PrivilegedActionException) e).getException();
                    if (migratable) {
                        if (e instanceof MigrationInProgressException) {
                            if (debug) {
                                System.out.println("temporary failure during sync down with pid " + successors[i] + ": migration in progress");
                            }
                            try {
                                Thread.sleep(MIGRATION_IN_PROGRESS_WAIT_INTERVAL);
                            } catch (InterruptedException ie) {
                            }
                        } else {
                            if (secondTry) {
                                try {
                                    synchronized (privilegedExceptionActionWrapper) {
                                        privilegedExceptionActionWrapper.setNextActionMainThreadErrorNotify(new Exception("restoring PID " + successors[i] + " failed during sync down in syncImpl()", e));
                                        AccessController.doPrivileged(privilegedExceptionActionWrapper);
                                    }
                                    break;
                                } catch (PrivilegedActionException ex) {
                                    System.err.println("FATAL: restoring PID " + successors[i] + " failed during sync down in RuntimeEnvironment.syncImpl():");
                                    e.printStackTrace();
                                    System.err.println("FATAL: could not inform main thread about this fact in RuntimeEnvironment.syncImpl():");
                                    ex.getException().printStackTrace();
                                    System.exit(1);
                                }
                            } else {
                                System.err.println("sync down with process " + successors[i] + " failed -- assuming process is dead");
                                if (debug) {
                                    e.printStackTrace();
                                }
                                restorer.addDeadProcess(successors[i]);
                                restorer.waitUntilAllDeadProcessesRestored();
                                secondTry = true;
                            }
                        }
                    } else {
                        try {
                            synchronized (privilegedExceptionActionWrapper) {
                                privilegedExceptionActionWrapper.setNextActionMainThreadErrorNotify(new Exception("could not contact successor in syncImpl()", e));
                                AccessController.doPrivileged(privilegedExceptionActionWrapper);
                            }
                            break;
                        } catch (PrivilegedActionException ex) {
                            System.err.println("FATAL: could not contact successor in RuntimeEnvironment.syncImpl():");
                            e.printStackTrace();
                            System.err.println("FATAL: could not inform main thread about this fact in RuntimeEnvironment.syncImpl():");
                            ex.getException().printStackTrace();
                            System.exit(1);
                        }
                    }
                }
            }
        }
        boolean integrityViolated = false;
        synchronized (incomingMsgs) {
            List<Message> tmpList = new LinkedList<Message>();
            Iterator<Message> it = incomingMsgs.iterator();
            while (it.hasNext()) {
                Message msg = it.next();
                if (msg.getSuperstep() == superstep) {
                    tmpList.add(msg);
                    it.remove();
                } else if (msg.getSuperstep() < superstep || msg.getSuperstep() > superstep + 1) {
                    integrityViolated = true;
                }
            }
            receivedMsgs = tmpList.toArray(new Message[tmpList.size()]);
            Arrays.sort(receivedMsgs);
        }
        if (integrityViolated) {
            try {
                synchronized (privilegedExceptionActionWrapper) {
                    privilegedExceptionActionWrapper.setNextActionMainThreadErrorNotify(new IntegrityException("found message from another superstep in syncImpl()"));
                    AccessController.doPrivileged(privilegedExceptionActionWrapper);
                }
            } catch (PrivilegedActionException e) {
                System.err.println("FATAL: found message from another superstep in RuntimeEnvironment.syncImpl()");
                System.err.println("FATAL: could not inform main thread about this fact in RuntimeEnvironment.syncImpl():");
                e.getException().printStackTrace();
                System.exit(1);
            }
        }
        synchronized (outgoingMsgs) {
            for (int i = 0; i < outgoingMsgs.length; i++) {
                pastOutgoingMsgs[i] = outgoingMsgs[i];
                outgoingMsgs[i] = new LinkedList<Message>();
            }
            superstep += 1;
        }
    }

    public void send(int to, Serializable msg) throws IntegrityException {
        if (to < 0 || to >= procs.length) throw new IntegrityException("destination out of pid range");
        outgoingMsgs[to].add(new Message(superstep, pid, to, msgSeqNo++, msg, codebase));
    }

    public void send(int pidLow, int pidHigh, Serializable msg) throws IntegrityException {
        if (pidHigh < pidLow || pidLow < 0 || pidHigh >= procs.length) throw new IntegrityException("destination pid range invalid");
        for (int i = pidLow; i <= pidHigh; i++) {
            outgoingMsgs[i].add(new Message(superstep, pid, i, msgSeqNo++, msg, codebase));
        }
    }

    public void send(int[] pids, Serializable msg) throws IntegrityException {
        for (int i = 0; i <= pids.length; i++) {
            if (pids[i] < 0 || pids[i] >= procs.length) throw new IntegrityException("destination " + pids[i] + " out of pid range");
        }
        for (int i = 0; i <= pids.length; i++) {
            outgoingMsgs[pids[i]].add(new Message(superstep, pid, pids[i], msgSeqNo++, msg, codebase));
        }
    }

    public int getNumberOfMessages() {
        return receivedMsgs.length;
    }

    public Message getMessage(int index) throws IntegrityException {
        if (index < 0 || index >= receivedMsgs.length) throw new IntegrityException("index out of range");
        return receivedMsgs[index];
    }

    public Message[] getAllMessages() {
        return receivedMsgs;
    }

    public Message findMessage(int src, int index) throws IntegrityException {
        if (src < 0 || src >= procs.length) throw new IntegrityException("source pid out of pid range");
        int pos;
        for (pos = 0; pos < receivedMsgs.length; pos++) {
            if (receivedMsgs[pos].getSource() >= src) break;
        }
        if (pos == receivedMsgs.length || receivedMsgs[pos].getSource() > src) return null;
        if (index < 0) throw new IntegrityException("index out of range");
        if (pos + index >= procs.length || receivedMsgs[pos + index].getSource() > src) return null;
        return receivedMsgs[pos + index];
    }

    public Message[] findAllMessages(int src) throws IntegrityException {
        if (src < 0 || src >= procs.length) throw new IntegrityException("source pid out of pid range");
        int pos;
        for (pos = 0; pos < receivedMsgs.length; pos++) {
            if (receivedMsgs[pos].getSource() >= src) break;
        }
        if (pos == receivedMsgs.length || receivedMsgs[pos].getSource() > src) return new Message[0];
        int end;
        for (end = pos + 1; end < receivedMsgs.length; end++) {
            if (receivedMsgs[end].getSource() > src) break;
        }
        Message[] msgs = new Message[end - pos];
        System.arraycopy(receivedMsgs, pos, msgs, 0, msgs.length);
        return msgs;
    }

    public int getNumberOfProcessors() {
        return procs.length;
    }

    public int getProcessId() {
        return pid;
    }

    public long getTime() {
        return System.currentTimeMillis();
    }

    public String getHostname() {
        try {
            synchronized (privilegedExceptionActionWrapper) {
                privilegedExceptionActionWrapper.setNextActionGetHostname();
                return (String) AccessController.doPrivileged(privilegedExceptionActionWrapper);
            }
        } catch (PrivilegedActionException e) {
            if (debug) {
                System.out.println("error determining hostname in getHostname()");
                e.getException().printStackTrace();
            }
            return "[unknown]";
        }
    }

    public InputStream getResourceAsStream(String name) throws IOException, MalformedURLException {
        try {
            synchronized (privilegedExceptionActionWrapper) {
                privilegedExceptionActionWrapper.setNextActionGetResourceAsStream(name);
                return (InputStream) AccessController.doPrivileged(privilegedExceptionActionWrapper);
            }
        } catch (PrivilegedActionException e) {
            Exception ne = e.getException();
            if (ne instanceof IOException) {
                throw (IOException) ne;
            }
            if (ne instanceof MalformedURLException) {
                throw (MalformedURLException) ne;
            }
            throw new RuntimeException("unexpected error retrieving resource as stream", ne);
        }
    }

    @Migratory
    public void syncMig() {
        boolean beingKilled = false;
        try {
            syncedWrapperForSuperstepEnd();
        } catch (PrivilegedActionException e) {
            Exception ne = e.getException();
            if (ne instanceof ResetInProgressException) {
                restorer.cancel();
                beingKilled = true;
            } else {
                try {
                    syncedWrapperForMainThreadErrorNotify(new IntegrityException("could not send status message in syncMig()", ne));
                } catch (PrivilegedActionException ex) {
                    System.err.println("FATAL: could not send status message in RuntimeEnvironment.syncMig():");
                    ne.printStackTrace();
                    System.err.println("FATAL: could not inform main thread about this fact in RuntimeEnvironment.syncMig():");
                    ex.getException().printStackTrace();
                    System.exit(1);
                }
            }
        }
        if (beingKilled) {
            while (true) {
                try {
                    Thread.sleep(EXITING_WAIT_INTERVAL);
                } catch (InterruptedException ie) {
                }
            }
        }
        mayMigrateImpl();
        backupCopyWritten = false;
        syncImpl();
        createBackupCopy();
        mayMigrateImpl();
    }

    @Migratory
    public boolean mayMigrate() {
        return mayMigrateImpl();
    }

    @Migratory
    private boolean mayMigrateImpl() {
        try {
            String dest = syncedWrapperForCheckIfMigationExpected();
            if (dest == null) return false;
            updateGuid(procs[pid]);
            if (dest.equals(procs[pid].getWorker())) throw new IntegrityException("supernode suggested migration to ourselves");
            String processServerID = syncedWrapperForInitRemoteNodeForProcess(dest);
            syncedEnterMigratingState();
            if (debug) System.out.println("pid " + pid + ": migrating to guid " + dest);
            migrationType = MIGRATION_TYPE_ORDINARY;
            PadMig.migrate(Server.exchangeGUID(supernode.getWorkerUrl(dest), processServerID));
            return true;
        } catch (Exception e) {
            if (e instanceof PrivilegedActionException) e = ((PrivilegedActionException) e).getException();
            System.err.println("migration failed");
            if (debug) e.printStackTrace();
            try {
                syncedWrapperForMigrationFailed();
            } catch (PrivilegedActionException any) {
            }
            return false;
        }
    }

    @Migratory
    private void createBackupCopy() {
        try {
            returnedFromSavingBackupMigration = true;
            migrationType = MIGRATION_TYPE_BACKUP;
            PadMig.checkpoint(new File(mainThread.getBackupDir(), procs[0].getJob().getId() + "-" + pid + "-" + superstep + ".obj"));
            syncedWrapperForSigningBackup(procs[0].getJob().getId() + "-" + pid + "-" + superstep + ".obj");
            if (returnedFromSavingBackupMigration) {
                syncedWrapperForCopyingBackup(procs[0].getJob().getId() + "-" + pid + "-" + superstep + ".obj");
                syncedWrapperForDeletingOldBackups();
            }
        } catch (Exception e) {
            if (e instanceof PrivilegedActionException) e = ((PrivilegedActionException) e).getException();
            System.err.println("error creating backup copy");
            if (debug) e.printStackTrace();
        }
        backupCopyWritten = true;
    }

    public synchronized void sendMessages(List<Message> msgs, int superstep) throws IntegrityException, MigrationInProgressException {
        if (state == STATE_MIGRATING) {
            throw new MigrationInProgressException("process migrating -- try again later at migration destination");
        } else if (state != STATE_READY) {
            throw new IntegrityException("not expecting sendMessages() in state " + state);
        }
        if (superstep == this.superstep || superstep == this.superstep + 1) {
            synchronized (incomingMsgs) {
                incomingMsgs.addAll(msgs);
            }
        } else if (superstep == this.superstep - 1) {
            if (debug) {
                System.out.println("ignoring incoming messages from previous superstep (" + superstep + ") from PID " + (msgs.isEmpty() ? "[unknown]" : msgs.get(0).getSource()));
            }
        } else {
            throw new IntegrityException("not expecting sendMessages() from superstep " + superstep + " in superstep " + this.superstep);
        }
    }

    public synchronized SuperstepMessageListContainer pollMessages(int srcPid, int superstep) throws IntegrityException, MigrationInProgressException {
        if (state == STATE_MIGRATING) {
            throw new MigrationInProgressException("process migrating -- try again later at migration destination");
        } else if (state != STATE_READY) {
            throw new IntegrityException("not expecting pollMessages() in state " + state);
        }
        synchronized (outgoingMsgs) {
            if (superstep == this.superstep) {
                return new SuperstepMessageListContainer(this.superstep, outgoingMsgs[srcPid]);
            } else if (superstep == this.superstep - 1 && superstep >= 0) {
                return new SuperstepMessageListContainer(this.superstep, pastOutgoingMsgs[srcPid]);
            } else {
                throw new IntegrityException("not expecting pollMessages() from superstep " + superstep + " in superstep " + this.superstep);
            }
        }
    }

    public synchronized boolean syncUp(int srcPid, int superstep) throws IntegrityException, MigrationInProgressException {
        if (state == STATE_MIGRATING) {
            throw new MigrationInProgressException("process migrating -- try again later at migration destination");
        } else if (state != STATE_READY) {
            throw new IntegrityException("not expecting syncUp() in state " + state);
        }
        if (srcPid < pid * TREE_DEGREE + 1 || srcPid > pid * TREE_DEGREE + TREE_DEGREE) throw new IntegrityException("not expecting bspSyncUp() from this PID");
        if (superstep == this.superstep || superstep == this.superstep + 1) {
            syncWaitingForSuccessors[srcPid - (pid * TREE_DEGREE + 1)] = false;
            return true;
        } else if (superstep == this.superstep - 1) {
            return false;
        } else {
            throw new IntegrityException("not expecting syncUp() from superstep " + superstep + " in superstep " + this.superstep);
        }
    }

    public synchronized void syncDown(int srcPid, int superstep) throws IntegrityException, MigrationInProgressException {
        if (state == STATE_MIGRATING) {
            throw new MigrationInProgressException("process migrating -- try again later at migration destination");
        } else if (state != STATE_READY) {
            throw new IntegrityException("not expecting syncDown() in state " + state);
        }
        if (srcPid != predecessor) throw new IntegrityException("not expecting bspSyncDown() from this PID");
        if (superstep == this.superstep) {
            syncWaitingForPredecessor = false;
        } else if (superstep == this.superstep - 1) {
            if (debug) {
                System.out.println("ignoring sync-down command from previous superstep (" + superstep + ") from PID " + srcPid);
            }
        } else {
            throw new IntegrityException("not expecting syncUp() from superstep " + superstep + " in superstep " + this.superstep);
        }
    }

    public synchronized void migrationCompleted() throws IntegrityException {
        if (state != STATE_MIGRATING) throw new IntegrityException("this process is not migrating");
        joinThreads();
        if (debug) System.out.println("pid " + pid + ": migration completed");
        mainThread.processMigrated();
    }

    public void init() {
        if (state != STATE_INITIALIZED) {
            mainThread.jobDiedOnError(new IntegrityException("the no-args constructor must not be used with non-migratable programs"));
            return;
        }
        if (pid < 0 || pid >= procs.length) {
            mainThread.jobDiedOnError(new IntegrityException("invalid PID"));
            return;
        }
        isMaster = (pid == 0);
        if (!isMaster) predecessor = (pid - 1) / TREE_DEGREE;
        if (procs.length - 1 < pid * TREE_DEGREE + 1) successors = new int[0]; else if (procs.length - 1 >= pid * TREE_DEGREE + TREE_DEGREE) {
            successors = new int[TREE_DEGREE];
            for (int i = 0; i < successors.length; i++) successors[i] = pid * TREE_DEGREE + 1 + i;
        } else {
            successors = new int[(procs.length - 1) - (pid * TREE_DEGREE)];
            for (int i = 0; i < successors.length; i++) successors[i] = pid * TREE_DEGREE + 1 + i;
        }
        superstep = 0;
        msgSeqNo = 0;
        @SuppressWarnings("unchecked") List<Message>[] tmp = new List[procs.length];
        outgoingMsgs = tmp;
        for (int i = 0; i < procs.length; i++) outgoingMsgs[i] = new LinkedList<Message>();
        @SuppressWarnings("unchecked") List<Message>[] tmp2 = new List[procs.length];
        pastOutgoingMsgs = tmp2;
        for (int i = 0; i < procs.length; i++) pastOutgoingMsgs[i] = null;
        receivedMsgs = new Message[0];
        backupCopyWritten = false;
        syncWaitingForPredecessor = false;
        syncWaitingForSuccessors = new boolean[successors.length];
        for (int i = 0; i < syncWaitingForSuccessors.length; i++) syncWaitingForSuccessors[i] = true;
        stdOutThread = new ReStdOutThread(this);
        stdOutThread.setPriority(Thread.NORM_PRIORITY);
        stdOutThread.start();
        stdErrThread = new ReStdErrThread(this);
        stdErrThread.setPriority(Thread.NORM_PRIORITY);
        stdErrThread.start();
        rawDataThread = new ReRawDataThread(this);
        rawDataThread.setPriority(Thread.NORM_PRIORITY);
        rawDataThread.start();
        state = STATE_READY;
    }

    public Serializable migratableMain(Serializable[] args) {
        URL initialDestination = (URL) args[0];
        procs = (JobProcess[]) args[1];
        pid = ((Integer) args[2]).intValue();
        debug = ((Boolean) args[3]).booleanValue();
        migratable = true;
        migrationType = MIGRATION_TYPE_NONE;
        codebase = (URL) args[4];
        mainClass = (String) args[5];
        progArgs = (byte[]) args[6];
        try {
            if (pid < 0 || pid >= procs.length) throw new IntegrityException("invalid PID");
            isMaster = (pid == 0);
            if (!isMaster) predecessor = (pid - 1) / TREE_DEGREE;
            if (procs.length - 1 < pid * TREE_DEGREE + 1) successors = new int[0]; else if (procs.length - 1 >= pid * TREE_DEGREE + TREE_DEGREE) {
                successors = new int[TREE_DEGREE];
                for (int i = 0; i < successors.length; i++) successors[i] = pid * TREE_DEGREE + 1 + i;
            } else {
                successors = new int[(procs.length - 1) - (pid * TREE_DEGREE)];
                for (int i = 0; i < successors.length; i++) successors[i] = pid * TREE_DEGREE + 1 + i;
            }
            superstep = 0;
            msgSeqNo = 0;
            @SuppressWarnings("unchecked") List<Message>[] tmp = new List[procs.length];
            outgoingMsgs = tmp;
            for (int i = 0; i < procs.length; i++) outgoingMsgs[i] = new LinkedList<Message>();
            @SuppressWarnings("unchecked") List<Message>[] tmp2 = new List[procs.length];
            pastOutgoingMsgs = tmp2;
            for (int i = 0; i < procs.length; i++) pastOutgoingMsgs[i] = null;
            receivedMsgs = new Message[0];
            backupCopyWritten = false;
            execMigratableProgram(initialDestination);
        } catch (Throwable e) {
            return e;
        }
        return null;
    }

    public void restore(ReServices mainThread) throws MalformedURLException {
        this.mainThread = mainThread;
        peers = new HashMap<String, Worker2Worker>();
        privilegedExceptionActionWrapper = new RePrivilegedExceptionActionWrapper(this);
        restorer = new ProcessRestorer(this);
        if (migrationType != MIGRATION_TYPE_ORDINARY) {
            incomingMsgs = new TreeSet<Message>();
            syncWaitingForPredecessor = false;
            syncWaitingForSuccessors = new boolean[successors.length];
            for (int i = 0; i < syncWaitingForSuccessors.length; i++) syncWaitingForSuccessors[i] = true;
        }
        stdOutThread = new ReStdOutThread(this);
        stdOutThread.setPriority(Thread.NORM_PRIORITY);
        stdOutThread.start();
        stdErrThread = new ReStdErrThread(this);
        stdErrThread.setPriority(Thread.NORM_PRIORITY);
        stdErrThread.start();
        rawDataThread = new ReRawDataThread(this);
        rawDataThread.setPriority(Thread.NORM_PRIORITY);
        rawDataThread.start();
        if (migrationType == MIGRATION_TYPE_BACKUP) {
            returnedFromSavingBackupMigration = false;
            outOfSyncAfterRestore = true;
        }
        state = STATE_READY;
    }

    public int getMigrationType() {
        return migrationType;
    }

    protected void waitUntilBackupCopyWritten() {
        while (!backupCopyWritten) {
            try {
                Thread.sleep(BACKUP_WAIT_INTERVAL);
            } catch (InterruptedException ie) {
            }
        }
    }

    protected void joinThreads() {
        stdOutThread.die();
        stdErrThread.die();
        rawDataThread.die();
        while (true) {
            try {
                stdOutThread.join();
                break;
            } catch (InterruptedException ie) {
            }
        }
        while (true) {
            try {
                stdErrThread.join();
                break;
            } catch (InterruptedException ie) {
            }
        }
        while (true) {
            try {
                rawDataThread.join();
                break;
            } catch (InterruptedException ie) {
            }
        }
    }

    protected Worker2Worker getOrResolvePeer(String guid) throws IntegrityException, MalformedURLException, PpException {
        if (guid == null) {
            throw new IntegrityException("guid is null");
        }
        synchronized (peers) {
            if (peers.containsKey(guid)) {
                return peers.get(guid);
            } else {
                URL url = new URL(supernode.getWorkerUrl(guid) + "/" + Worker2Worker.class.getSimpleName());
                Worker2Worker peer = (Worker2Worker) Server.getDefaultServer().getProxyFactory().createProxy(url, Worker2Worker.class);
                peers.put(guid, peer);
                return peer;
            }
        }
    }

    protected Worker2Worker resolveOrUpdatePeer(String guid) throws IntegrityException, MalformedURLException, PpException {
        if (guid == null) {
            throw new IntegrityException("guid is null");
        }
        synchronized (peers) {
            URL url = new URL(supernode.getWorkerUrl(guid) + "/" + Worker2Worker.class.getSimpleName());
            Worker2Worker peer = (Worker2Worker) Server.getDefaultServer().getProxyFactory().createProxy(url, Worker2Worker.class);
            peers.put(guid, peer);
            return peer;
        }
    }

    protected void copyBackup(String name) throws ClassNotFoundException, IntegrityException, InternalException, IOException, MalformedURLException, NotConnectedException, PpException {
        String dstGUID = null, myGUID;
        updateGuid(procs[pid]);
        myGUID = procs[pid].getWorker();
        int x = (pid + 1) % procs.length;
        while (true) {
            updateGuid(procs[x]);
            if (!procs[x].getWorker().equals(myGUID)) {
                dstGUID = procs[x].getWorker();
                break;
            }
            x = (x + 1) % procs.length;
            if (x == pid) break;
        }
        if (dstGUID != null) {
            Worker2Worker peer = resolveOrUpdatePeer(dstGUID);
            peer.storeBackupCopy(procs[0].getJob(), name, new URL(System.getProperty("pubweb.worker.url")));
        }
    }

    protected void updateGuid(JobProcess process) throws PpException, IntegrityException, InternalException {
        if (process == null) {
            throw new IntegrityException("process is null");
        }
        if (supernode == null) {
            throw new IntegrityException("supernode is null");
        }
        String guid = supernode.locateProcess(process.getJob(), process.getPid());
        if (guid == null) {
            mainThread.jobDiedOnError(new IntegrityException("no worker assigned to process after update (this should only happen if the scheduler has no workers left)"));
        }
        process.setWorker(guid);
    }

    private void ensureAllProcessesAreRunning() throws IntegrityException {
        while (true) {
            boolean ready = true;
            for (int i = 0; i < syncWaitingForSuccessors.length; i++) {
                if (syncWaitingForSuccessors[i]) {
                    ready = false;
                    break;
                }
            }
            if (ready) break;
            try {
                Thread.sleep(SYNC_WAIT_INTERVAL);
            } catch (InterruptedException e) {
            }
        }
        if (!isMaster) {
            syncWaitingForPredecessor = true;
            while (true) {
                try {
                    try {
                        Worker2Worker pred = getOrResolvePeer(procs[predecessor].getWorker());
                        pred.syncUp(procs[predecessor], pid, superstep);
                        break;
                    } catch (Exception e) {
                        updateGuid(procs[predecessor]);
                        resolveOrUpdatePeer(procs[predecessor].getWorker());
                    }
                } catch (Exception any) {
                    if (debug) {
                        System.err.println("could not contact predecessor during initialization (needs not be an error)");
                        any.printStackTrace();
                    }
                }
                try {
                    Thread.sleep(INIT_SYNC_WAIT_INTERVAL);
                } catch (InterruptedException e) {
                }
            }
            while (true) {
                if (!syncWaitingForPredecessor) break;
                try {
                    Thread.sleep(SYNC_WAIT_INTERVAL);
                } catch (InterruptedException e) {
                }
            }
        }
        for (int i = 0; i < syncWaitingForSuccessors.length; i++) syncWaitingForSuccessors[i] = true;
        for (int i = 0; i < successors.length; i++) {
            try {
                Worker2Worker succ;
                try {
                    succ = getOrResolvePeer(procs[successors[i]].getWorker());
                    succ.syncDown(procs[successors[i]], pid, superstep);
                } catch (Exception ex) {
                    updateGuid(procs[successors[i]]);
                    succ = resolveOrUpdatePeer(procs[successors[i]].getWorker());
                    succ.syncDown(procs[successors[i]], pid, superstep);
                }
            } catch (Exception e) {
                throw new IntegrityException("could not contact successor during initialization", e);
            }
        }
    }

    @Undock
    private void execMigratableProgram(URL initialDestination) throws MigrationException {
        migrationType = MIGRATION_TYPE_INITIAL;
        PadMig.migrate(initialDestination);
        while (state != STATE_READY) {
            try {
                Thread.sleep(READY_WAIT_INTERVAL);
            } catch (InterruptedException e) {
            }
        }
        BSPMigratableProgram program;
        Serializable deserializedProgArgs = null;
        try {
            @DontMigrate ClassLoader loader = URLClassLoaderFactory.getURLClassLoader(codebase);
            @DontMigrate Class<?> prog = loader.loadClass(mainClass);
            program = (BSPMigratableProgram) prog.newInstance();
            if (progArgs != null) {
                @DontMigrate ByteArrayInputStream bis = new ByteArrayInputStream(progArgs);
                @DontMigrate ObjectInputStream ois = new ObjectInputStreamWithLoader(bis, loader);
                deserializedProgArgs = (Serializable) ois.readObject();
                ois.close();
            }
        } catch (Exception e) {
            if (debug) {
                System.err.println("error loading the program from " + codebase.toExternalForm() + ":");
                e.printStackTrace();
            }
            mainThread.jobDiedOnError(new Exception("error loading the program from " + codebase.toExternalForm(), e));
            return;
        } catch (Error e) {
            if (debug) {
                System.err.println("error loading the program from " + codebase.toExternalForm() + ":");
                e.printStackTrace();
            }
            mainThread.jobDiedOnError(new Exception("error loading the program from " + codebase.toExternalForm(), e));
            return;
        }
        try {
            ensureAllProcessesAreRunning();
        } catch (IntegrityException e) {
            if (debug) {
                System.err.println("error waiting for other PIDs during init:");
                e.printStackTrace();
            }
            mainThread.jobDiedOnError(new Exception("error waiting for other PIDs during init", e));
            return;
        }
        try {
            createBackupCopy();
            program.bspMain(this, deserializedProgArgs);
            mainThread.superstepCompleted(superstep, true);
        } catch (AbortedException e) {
            if (debug) {
                System.err.println("caught AbortedException -- terminating now:");
                e.printStackTrace();
            }
            mainThread.jobAborted(e);
            return;
        } catch (Exception e) {
            if (debug) {
                System.err.println("error executing the program:");
                e.printStackTrace();
            }
            mainThread.jobDiedOnError(new Exception("error executing the program", e));
            return;
        } catch (Error e) {
            if (debug) {
                System.err.println("error executing the program:");
                e.printStackTrace();
            }
            mainThread.jobDiedOnError(new Exception("error executing the program", e));
            return;
        }
        syncImpl();
        joinThreads();
        mainThread.processFinished();
    }

    private String syncedWrapperForInitRemoteNodeForProcess(String dest) throws PrivilegedActionException {
        synchronized (privilegedExceptionActionWrapper) {
            privilegedExceptionActionWrapper.setNextActionInitRemoteNodeForProcess(dest);
            return (String) AccessController.doPrivileged(privilegedExceptionActionWrapper);
        }
    }

    private void syncedWrapperForMigrationFailed() throws PrivilegedActionException {
        synchronized (privilegedExceptionActionWrapper) {
            privilegedExceptionActionWrapper.setNextActionMigrationFailed();
            AccessController.doPrivileged(privilegedExceptionActionWrapper);
        }
    }

    private void syncedWrapperForMainThreadErrorNotify(Throwable cause) throws PrivilegedActionException {
        synchronized (privilegedExceptionActionWrapper) {
            privilegedExceptionActionWrapper.setNextActionMainThreadErrorNotify(cause);
            AccessController.doPrivileged(privilegedExceptionActionWrapper);
        }
    }

    private void syncedWrapperForCopyingBackup(String name) throws PrivilegedActionException {
        synchronized (privilegedExceptionActionWrapper) {
            privilegedExceptionActionWrapper.setNextActionCopyBackup(name);
            AccessController.doPrivileged(privilegedExceptionActionWrapper);
        }
    }

    private void syncedWrapperForSigningBackup(String name) throws PrivilegedActionException {
        synchronized (privilegedExceptionActionWrapper) {
            privilegedExceptionActionWrapper.setNextActionSignBackup(name);
            AccessController.doPrivileged(privilegedExceptionActionWrapper);
        }
    }

    private void syncedWrapperForDeletingOldBackups() throws PrivilegedActionException {
        synchronized (privilegedExceptionActionWrapper) {
            privilegedExceptionActionWrapper.setNextActionDeleteOldBackups();
            AccessController.doPrivileged(privilegedExceptionActionWrapper);
        }
    }

    private void syncedWrapperForSuperstepEnd() throws PrivilegedActionException {
        synchronized (privilegedExceptionActionWrapper) {
            privilegedExceptionActionWrapper.setNextActionMainThreadSuperstepEnd();
            AccessController.doPrivileged(privilegedExceptionActionWrapper);
        }
    }

    private String syncedWrapperForCheckIfMigationExpected() throws PrivilegedActionException {
        synchronized (privilegedExceptionActionWrapper) {
            privilegedExceptionActionWrapper.setNextActionMainThreadCheckIfMigationExpected();
            return (String) AccessController.doPrivileged(privilegedExceptionActionWrapper);
        }
    }

    private synchronized void syncedEnterMigratingState() {
        state = STATE_MIGRATING;
    }
}
