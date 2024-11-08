package joeq.Scheduler;

import java.util.Iterator;
import java.util.Random;
import joeq.Allocator.CodeAllocator;
import joeq.Allocator.HeapAllocator;
import joeq.Allocator.RuntimeCodeAllocator;
import joeq.Allocator.SimpleAllocator;
import joeq.Class.PrimordialClassLoader;
import joeq.Class.jq_Class;
import joeq.Class.jq_DontAlign;
import joeq.Class.jq_InstanceMethod;
import joeq.Class.jq_StaticField;
import joeq.Class.jq_StaticMethod;
import joeq.Main.jq;
import joeq.Memory.CodeAddress;
import joeq.Memory.HeapAddress;
import joeq.Memory.StackAddress;
import joeq.Runtime.Debug;
import joeq.Runtime.StackCodeWalker;
import joeq.Runtime.SystemInterface;
import joeq.Runtime.Unsafe;
import jwutil.strings.Strings;
import jwutil.util.Assert;

/**
 * A jq_NativeThread corresponds to a virtual CPU in the scheduler.  There is one
 * jq_NativeThread object for each underlying (heavyweight) kernel thread.
 * The Java (lightweight) threads are multiplexed across the jq_NativeThreads.
 * 
 * @author  John Whaley <jwhaley@alum.mit.edu>
 * @author  Miho Kurano
 * @version $Id: jq_NativeThread.java 1941 2004-09-30 03:37:06Z joewhaley $
 */
public class jq_NativeThread implements jq_DontAlign {

    /** Trace flag.  When this is true, prints out debugging information about
     * what is going on in the scheduler.
     */
    public static boolean TRACE = false;

    public static boolean CHECK = false;

    public static final boolean STATISTICS = true;

    /** Data structure to represent the native thread that exists at virtual
     * machine startup.
     */
    public static final jq_NativeThread initial_native_thread = new jq_NativeThread(0);

    /**
     * Initialize the initial native thread.
     * Must be the first thing called at virtual machine startup.
     */
    public static void initInitialNativeThread() {
        initial_native_thread.pid = SystemInterface.init_thread();
        Unsafe.setThreadBlock(initial_native_thread.schedulerThread);
        initial_native_thread.thread_handle = SystemInterface.get_current_thread_handle();
        initial_native_thread.myHeapAllocator.init();
        initial_native_thread.myCodeAllocator.init();
        if (TRACE) SystemInterface.debugwriteln("Initial native thread initialized");
    }

    /** An array of all native threads. */
    public static jq_NativeThread[] native_threads;

    /** Number of Java threads that are currently active.
     *  When this equals the number of active daemon threads, it is time to shut down.
     */
    private static volatile int num_of_java_threads = 0;

    /** Number of daemon threads that are currently active.
     *  Daemon threads do not keep the VM running.
     */
    private static volatile int num_of_daemon_threads = 0;

    /** NOTE: C code relies on this field being first. */
    private int thread_handle;

    /** NOTE: C code relies on this field being second. */
    private jq_Thread currentThread;

    /** NOTE: C code relies on this field being third. */
    private int pid;

    /** counter for preempted thread */
    private int preempted_thread_counter;

    /** Queue of ready Java threads. */
    private final jq_ThreadQueue[] readyQueue;

    /** Queue of idle Java threads. */
    private final jq_ThreadQueue idleQueue;

    /** Queue of Java threads transferred from another native thread. */
    private final jq_SynchThreadQueue transferQueue;

    /** Thread-local allocators. */
    private CodeAllocator myCodeAllocator;

    private HeapAllocator myHeapAllocator;

    /** Original thread's stack pointer and base pointer.
     *  These are used for longjmp'ing back to schedulerLoop when the currently
     *  executing Java thread exits.
     */
    StackAddress original_esp, original_ebp;

    /** The index of this native thread. */
    private final int index;

    /** The Java thread that is executing while we are in the scheduler. */
    private final jq_Thread schedulerThread;

    public static final int MAX_NATIVE_THREADS = 16;

    private static boolean all_native_threads_initialized;

    private static boolean all_native_threads_started;

    /** Initialize the extra native threads.
     * 
     * @param nt initial native thread
     * @param num number of native threads to initialize
     */
    public static void initNativeThreads(jq_NativeThread nt, int num) {
        Assert._assert(num <= MAX_NATIVE_THREADS);
        native_threads = new jq_NativeThread[num];
        native_threads[0] = nt;
        for (int i = 1; i < num; ++i) {
            jq_NativeThread nt2 = native_threads[i] = new jq_NativeThread(i);
            nt2.thread_handle = SystemInterface.create_thread(_nativeThreadEntry.getDefaultCompiledVersion().getEntrypoint(), HeapAddress.addressOf(nt2));
            nt2.myHeapAllocator.init();
            nt2.myCodeAllocator.init();
            if (TRACE) SystemInterface.debugwriteln("Native thread " + i + " initialized");
        }
        all_native_threads_initialized = true;
    }

    /** Start up the extra native threads.
     */
    public static void startNativeThreads() {
        for (int i = 1; i < native_threads.length; ++i) {
            if (TRACE) SystemInterface.debugwriteln("Native thread " + i + " started");
            native_threads[i].resume();
        }
        all_native_threads_started = true;
    }

    /** Returns true iff all native threads are initialized. */
    public static boolean allNativeThreadsInitialized() {
        return all_native_threads_initialized;
    }

    /** Returns the jq_Thread that is currently running on this native thread. */
    public jq_Thread getCurrentThread() {
        return currentThread;
    }

    /** Get the native thread-local code allocator. */
    public CodeAllocator getCodeAllocator() {
        return myCodeAllocator;
    }

    /** Get the native thread-local heap allocator. */
    public HeapAllocator getHeapAllocator() {
        return myHeapAllocator;
    }

    /** Get the currently-executing Java thread. */
    public jq_Thread getCurrentJavaThread() {
        return currentThread;
    }

    public static final int NUM_OF_QUEUES = 10;

    /** Create a new jq_NativeThread (only called from initNativeThreads(),
     *  and during bootstrap initialization of initial_native_thread and
     *  break_nthread field) */
    private jq_NativeThread(int i) {
        readyQueue = new jq_ThreadQueue[NUM_OF_QUEUES];
        for (int k = 0; k < NUM_OF_QUEUES; ++k) {
            readyQueue[k] = new jq_ThreadQueue();
        }
        idleQueue = new jq_ThreadQueue();
        transferQueue = new jq_SynchThreadQueue();
        myHeapAllocator = new SimpleAllocator();
        myCodeAllocator = new RuntimeCodeAllocator();
        index = i;
        preempted_thread_counter = 0;
        Thread t = new Thread("_scheduler_" + i);
        currentThread = schedulerThread = ThreadUtils.getJQThread(t);
        if (schedulerThread != null) {
            schedulerThread.disableThreadSwitch();
            schedulerThread.setNativeThread(this);
            schedulerThread.isScheduler = true;
        } else {
            Assert._assert(!jq.IsBootstrapping && !jq.RunningNative);
        }
    }

    /** Create a new jq_NativeThread that is tied to a specific jq_Thread. */
    jq_NativeThread(jq_Thread t) {
        readyQueue = null;
        idleQueue = null;
        transferQueue = null;
        myHeapAllocator = new SimpleAllocator();
        myCodeAllocator = new RuntimeCodeAllocator();
        index = -1;
        currentThread = schedulerThread = t;
        t.setNativeThread(this);
        t.isScheduler = true;
    }

    /** Starts up/resumes this native thread. */
    public void resume() {
        SystemInterface.resume_thread(thread_handle);
    }

    /** Suspends this native thread. */
    public void suspend() {
        SystemInterface.suspend_thread(thread_handle);
    }

    /** Gets context of this native thread and puts it in r. */
    public boolean getContext(jq_RegisterState r) {
        return SystemInterface.get_thread_context(pid, r);
    }

    /** Sets context of this native thread to r. */
    public boolean setContext(jq_RegisterState r) {
        return SystemInterface.set_thread_context(pid, r);
    }

    int chosenAsLeastBusy;

    /** Returns the least-busy native thread. */
    static jq_NativeThread getLeastBusyThread() {
        int min_i = 0;
        int min = native_threads[min_i].preempted_thread_counter + native_threads[min_i].transferQueue.length();
        for (int i = 1; i < native_threads.length; ++i) {
            int v = native_threads[i].preempted_thread_counter + native_threads[i].transferQueue.length();
            if (v < min) {
                min = v;
                min_i = i;
            }
        }
        if (STATISTICS) native_threads[min_i].chosenAsLeastBusy++;
        return native_threads[min_i];
    }

    /** Put the given Java thread on the queue of the least-busy native thread. */
    public static void startJavaThread(jq_Thread t) {
        _num_of_java_threads.getAddress().atomicAdd(1);
        if (t.isDaemon()) _num_of_daemon_threads.getAddress().atomicAdd(1);
        jq_NativeThread nt = getLeastBusyThread();
        Assert._assert(t.isThreadSwitchEnabled());
        t.disableThreadSwitch();
        CodeAddress ip = t.getRegisterState().getEip();
        if (TRACE) SystemInterface.debugwriteln("Java thread " + t + " enqueued on native thread " + nt + " ip: " + ip.stringRep() + " cc: " + CodeAllocator.getCodeContaining(ip));
        jq_Thread my_t = Unsafe.getThreadBlock();
        my_t.disableThreadSwitch();
        jq_NativeThread my_nt = my_t.getNativeThread();
        Unsafe.setThreadBlock(my_nt.schedulerThread);
        nt.transferQueue.enqueue(t);
        Unsafe.setThreadBlock(my_t);
        my_t.enableThreadSwitch();
    }

    /** End the currently-executing Java thread and go back to the scheduler loop
     *  to pick up another thread. 
     */
    public static void endCurrentJavaThread() {
        jq_Thread t = Unsafe.getThreadBlock();
        if (TRACE) Debug.writeln("Ending Java thread " + t);
        Assert._assert(!t.isThreadSwitchEnabled());
        _num_of_java_threads.getAddress().atomicSub(1);
        if (TRACE) Debug.writeln("Number of Java threads now: " + num_of_java_threads);
        if (t.isDaemon()) _num_of_daemon_threads.getAddress().atomicSub(1);
        jq_NativeThread nt = t.getNativeThread();
        Unsafe.setThreadBlock(nt.schedulerThread);
        nt.currentThread = nt.schedulerThread;
        CodeAddress ip = _schedulerLoop.getDefaultCompiledVersion().getEntrypoint();
        StackAddress fp = nt.original_ebp;
        StackAddress sp = nt.original_esp;
        if (TRACE) SystemInterface.debugwriteln("Long jumping back to schedulerLoop, ip:" + ip.stringRep() + " fp: " + fp.stringRep() + " sp: " + sp.stringRep());
        HeapAddress a = (HeapAddress) sp.offset(4).peek();
        Assert._assert(a.asObject() == nt, "arg to schedulerLoop got corrupted: " + a.stringRep() + " should be " + HeapAddress.addressOf(nt).stringRep());
        Unsafe.longJump(ip, fp, sp, 0);
        Assert.UNREACHABLE();
    }

    public static boolean USE_INTERRUPTER_THREAD = false;

    public jq_InterrupterThread it;

    /** The entry point for new native threads.
     */
    public void nativeThreadEntry() {
        if (this != initial_native_thread) this.pid = SystemInterface.init_thread();
        Unsafe.setThreadBlock(this.schedulerThread);
        Assert._assert(this.currentThread == this.schedulerThread);
        if (USE_INTERRUPTER_THREAD) {
            it = new jq_InterrupterThread(this);
        } else {
            SystemInterface.set_interval_timer(SystemInterface.ITIMER_VIRTUAL, 10);
        }
        StackAddress sp = StackAddress.getStackPointer();
        StackAddress fp = StackAddress.getBasePointer();
        this.original_esp = (StackAddress) sp.offset(-CodeAddress.size() - HeapAddress.size());
        this.original_ebp = fp;
        if (TRACE) SystemInterface.debugwriteln("Started native thread: " + this + " sp: " + this.original_esp.stringRep() + " fp: " + this.original_ebp.stringRep());
        this.schedulerLoop();
        Assert.UNREACHABLE();
    }

    public void schedulerLoop() {
        HeapAddress a = (HeapAddress) this.original_esp.offset(4).peek();
        Assert._assert(a.asObject() == this);
        Assert._assert(Unsafe.getThreadBlock() == this.schedulerThread);
        for (; ; ) {
            if (this == initial_native_thread && num_of_daemon_threads == num_of_java_threads) break;
            Assert._assert(currentThread == schedulerThread);
            jq_Thread t = getNextReadyThread();
            if (t == null) {
                if (TRACE) SystemInterface.debugwriteln("Native thread " + this + " is idle!");
                SystemInterface.yield();
            } else {
                Assert._assert(!t.isThreadSwitchEnabled());
                if (TRACE) SystemInterface.debugwriteln("Native thread " + this + " scheduler loop: switching to Java thread " + t);
                currentThread = t;
                SystemInterface.set_current_context(t, t.getRegisterState());
                Assert.UNREACHABLE();
            }
        }
        dumpStatistics();
        SystemInterface.die(0);
        Assert.UNREACHABLE();
    }

    public static void dumpStatistics() {
        for (int i = 0; i < native_threads.length; ++i) {
            native_threads[i].dumpStats();
        }
    }

    public void dumpStats() {
        if (STATISTICS) {
            Debug.write("Native thread ");
            Debug.write(index);
            Debug.write(": transferred out=");
            Debug.write(transferredOut);
            Debug.write("(");
            Debug.write(failedTransferOut);
            Debug.write(" failed) in=");
            Debug.writeln(transferredIn);
            Debug.write("               : chosen as least busy=");
            Debug.writeln(chosenAsLeastBusy);
            for (int i = 0; i < readyQueueLength.length; ++i) {
                Debug.write("               : average ready queue length ");
                Debug.write(i);
                Debug.write(": ");
                System.out.println((double) readyQueueLength[i] / readyQueueN);
            }
            Debug.write("               : preempted thread length=");
            System.out.println((double) preemptedThreadsLength / readyQueueN);
        }
        if (it != null && jq_InterrupterThread.STATISTICS) {
            Debug.write("Native thread ");
            Debug.write(index);
            Debug.write(": ");
            it.dumpStatistics();
        }
    }

    /** Thread switch based on a timer or poker interrupt. */
    public void threadSwitch() {
        jq_Thread t1 = this.currentThread;
        t1.wasPreempted = true;
        if (TRACE) SystemInterface.debugwriteln("Timer interrupt in native thread: " + this + " Java thread: " + t1);
        switchThread();
        Assert.UNREACHABLE();
    }

    /** Thread switch based on explicit yield. */
    public void yieldCurrentThread() {
        jq_Thread t1 = this.currentThread;
        if (t1 == this.schedulerThread) {
            t1.enableThreadSwitch();
            Assert._assert(!t1.isThreadSwitchEnabled());
            return;
        }
        t1.wasPreempted = false;
        if (TRACE) SystemInterface.debugwriteln("Explicit yield in native thread: " + this + " Java thread: " + t1);
        switchThread();
        Assert.UNREACHABLE();
    }

    private void switchThread() {
        jq_Thread t1 = this.currentThread;
        Unsafe.setThreadBlock(this.schedulerThread);
        this.currentThread = this.schedulerThread;
        CodeAddress ip = (CodeAddress) StackAddress.getBasePointer().offset(StackAddress.size()).peek();
        if (TRACE) SystemInterface.debugwriteln("Thread switch in native thread: " + this + " Java thread: " + t1 + " ip: " + ip.stringRep() + " cc: " + CodeAllocator.getCodeContaining(ip));
        if (t1.isThreadSwitchEnabled()) {
            SystemInterface.debugwriteln("Java thread " + t1 + " has thread switching enabled on threadSwitch entry!");
            SystemInterface.die(-1);
        }
        Assert._assert(t1 != this.schedulerThread);
        jq_RegisterState state = t1.getRegisterState();
        state.setEip((CodeAddress) state.getEsp().peek());
        state.setEsp((StackAddress) state.getEsp().offset(StackAddress.size() + CodeAddress.size()));
        jq_Thread t2 = getNextReadyThread();
        transferExtraWork();
        if (t2 == null) {
            t2 = t1;
        } else {
            ip = t2.getRegisterState().getEip();
            if (TRACE) SystemInterface.debugwriteln("New ready Java thread: " + t2 + " ip: " + ip.stringRep() + " cc: " + CodeAllocator.getCodeContaining(ip));
            int priority = t1.getPriority();
            readyQueue[priority].enqueue(t1);
            if (t1.wasPreempted && !t1.isDaemon()) ++preempted_thread_counter;
            Assert._assert(!t2.isThreadSwitchEnabled());
        }
        currentThread = t2;
        SystemInterface.set_current_context(t2, t2.getRegisterState());
        Assert.UNREACHABLE();
    }

    public void yieldCurrentThreadTo(jq_Thread t) {
        jq_Thread t1 = this.currentThread;
        if (t1 == this.schedulerThread) {
            t1.enableThreadSwitch();
            Assert._assert(!t1.isThreadSwitchEnabled());
            return;
        }
        t1.wasPreempted = false;
        switchThread(t);
        Assert.UNREACHABLE();
    }

    /** Performs a thread switch to a specific thread in our local queue. */
    private void switchThread(jq_Thread t2) {
        jq_Thread t1 = this.currentThread;
        Unsafe.setThreadBlock(this.schedulerThread);
        this.currentThread = this.schedulerThread;
        CodeAddress ip = (CodeAddress) StackAddress.getBasePointer().offset(StackAddress.size()).peek();
        if (TRACE) SystemInterface.debugwriteln("Thread switch in native thread: " + this + " Java thread: " + t1 + " ip: " + ip.stringRep() + " cc: " + CodeAllocator.getCodeContaining(ip));
        if (t1.isThreadSwitchEnabled()) {
            SystemInterface.debugwriteln("Java thread " + t1 + " has thread switching enabled on threadSwitch entry!");
            SystemInterface.die(-1);
        }
        Assert._assert(t1 != this.schedulerThread);
        jq_RegisterState state = t1.getRegisterState();
        state.setEip((CodeAddress) state.getEsp().peek());
        state.setEsp((StackAddress) state.getEsp().offset(StackAddress.size() + CodeAddress.size()));
        if (t1 != t2) {
            for (int i = 0; ; ++i) {
                if (i == readyQueue.length) {
                    Assert.UNREACHABLE();
                    return;
                }
                boolean exists = readyQueue[i].remove(t2);
                if (exists) break;
            }
            if (t2.wasPreempted && !t2.isDaemon()) --preempted_thread_counter;
        }
        transferExtraWork();
        if (t1 != t2) {
            ip = t2.getRegisterState().getEip();
            if (TRACE) SystemInterface.debugwriteln("New ready Java thread: " + t2 + " ip: " + ip.stringRep() + " cc: " + CodeAllocator.getCodeContaining(ip));
            int priority = t1.getPriority();
            readyQueue[priority].enqueue(t1);
            if (t1.wasPreempted && !t1.isDaemon()) ++preempted_thread_counter;
            Assert._assert(!t2.isThreadSwitchEnabled());
        }
        currentThread = t2;
        SystemInterface.set_current_context(t2, t2.getRegisterState());
        Assert.UNREACHABLE();
    }

    public static float TRANSFER_THRESHOLD = 1.5f;

    int transferredOut;

    int failedTransferOut;

    /** Transfer extra work from our ready queue into a less-busy thread's transfer queue. */
    private void transferExtraWork() {
        jq_NativeThread that = getLeastBusyThread();
        if (this == that) return;
        if (this.preempted_thread_counter * TRANSFER_THRESHOLD > (that.preempted_thread_counter + 1)) {
            int i;
            for (i = readyQueue.length - 1; i >= 0; --i) {
                if (this.readyQueue[i].length() > that.readyQueue[i].length()) {
                    if (STATISTICS) ++transferredOut;
                    jq_Thread t2 = this.readyQueue[i].dequeue();
                    Assert._assert(!t2.isThreadSwitchEnabled());
                    if (t2.wasPreempted && !t2.isDaemon()) --preempted_thread_counter;
                    that.transferQueue.enqueue(t2);
                    break;
                }
            }
            if (STATISTICS && i < 0) ++failedTransferOut;
        }
    }

    public boolean DETERMINISTIC = false;

    /**
     * GCD of relatively_prime_value and the maximum value in
     * DISTRIBUTION should be 1.
     */
    public static final int relatively_prime_value = 37;

    public static final int[] DISTRIBUTION = { 5, 11, 18, 26, 35, 45, 56, 68, 81, 100 };

    /**
     * Keeps track of last value used, so we can compute the next value.
     */
    int distCounter;

    Random rng = new Random();

    private jq_ThreadQueue chooseNextQueue() {
        if (DETERMINISTIC) {
            distCounter += relatively_prime_value;
            int max = DISTRIBUTION[DISTRIBUTION.length - 1];
            while (distCounter >= max) {
                distCounter -= max;
            }
        } else {
            int max = DISTRIBUTION[DISTRIBUTION.length - 1];
            distCounter = rng.nextInt(max);
        }
        for (int i = 0; ; ++i) {
            if (distCounter < DISTRIBUTION[i]) {
                if (!readyQueue[i].isEmpty()) return readyQueue[i];
                int c;
                if (DETERMINISTIC) {
                    c = ((distCounter & 1) == 1) ? 1 : -1;
                } else {
                    c = rng.nextBoolean() ? 1 : -1;
                }
                for (int j = i + c; j < DISTRIBUTION.length && j >= 0; j += c) {
                    if (!readyQueue[j].isEmpty()) return readyQueue[j];
                }
                c = -c;
                for (int j = i + c; j < DISTRIBUTION.length && j >= 0; j += c) {
                    if (!readyQueue[j].isEmpty()) return readyQueue[j];
                }
                return null;
            }
        }
    }

    int readyQueueLength[] = new int[10];

    int preemptedThreadsLength;

    int readyQueueN;

    int transferredIn;

    /** Get the next ready thread from the transfer queue or the ready queue.
     *  Return null if there are no threads ready.
     */
    private jq_Thread getNextReadyThread() {
        if (!transferQueue.isEmpty()) {
            if (STATISTICS) transferredIn++;
            jq_Thread t = transferQueue.dequeue();
            t.setNativeThread(this);
            Assert._assert(!t.isThreadSwitchEnabled());
            return t;
        }
        if (STATISTICS) {
            for (int i = 0; i < readyQueue.length; ++i) {
                readyQueueLength[i] += readyQueue[i].length();
            }
            preemptedThreadsLength += this.preempted_thread_counter;
            ++readyQueueN;
            if (CHECK) verifyCount();
        }
        jq_ThreadQueue q = chooseNextQueue();
        while (q != null && !q.isEmpty()) {
            jq_Thread t = q.dequeue();
            if (t.wasPreempted && !t.isDaemon()) --preempted_thread_counter;
            if (!t.isAlive()) continue;
            Assert._assert(t.getNativeThread() == this);
            Assert._assert(!t.isThreadSwitchEnabled());
            return t;
        }
        return null;
    }

    private void verifyCount() {
        int total = 0;
        for (int i = 0; i < readyQueue.length; ++i) {
            for (Iterator j = readyQueue[i].threads(); j.hasNext(); ) {
                jq_Thread t = (jq_Thread) j.next();
                if (t.wasPreempted && !t.isDaemon()) ++total;
            }
        }
        Assert._assert(total == preempted_thread_counter);
    }

    public String toString() {
        return "NT " + index + ":" + thread_handle + "(" + Strings.hex(this) + ")";
    }

    public int getIndex() {
        return index;
    }

    public static void ctrl_break_handler() {
        Unsafe.setThreadBlock(break_jthread);
        break_nthread.thread_handle = SystemInterface.get_current_thread_handle();
        if (!has_break_occurred) {
            break_nthread.myHeapAllocator.init();
            break_nthread.myCodeAllocator.init();
            has_break_occurred = true;
        }
        SystemInterface.debugwriteln("*** BREAK! ***");
        for (int i = 0; i < native_threads.length; ++i) {
            SystemInterface.suspend_thread(native_threads[i].thread_handle);
        }
        joeq.Debugger.OnlineDebugger.debuggerEntryPoint();
        for (int i = 0; i < native_threads.length; ++i) {
            SystemInterface.resume_thread(native_threads[i].thread_handle);
        }
    }

    public static void dumpAllThreads() {
        jq_RegisterState rs = jq_RegisterState.create();
        rs.setContextFlags(jq_RegisterState.CONTEXT_CONTROL);
        for (int i = 0; i < native_threads.length; ++i) {
            SystemInterface.get_thread_context(native_threads[i].pid, rs);
            native_threads[i].dump(rs);
        }
    }

    private static boolean has_break_occurred = false;

    private static jq_NativeThread break_nthread;

    private static jq_Thread break_jthread;

    public static void initBreakThread() {
        break_nthread = new jq_NativeThread(-1);
        Thread t = new Thread("_break_");
        break_jthread = ThreadUtils.getJQThread(t);
        break_jthread.disableThreadSwitch();
        break_jthread.setNativeThread(break_nthread);
        if (TRACE) SystemInterface.debugwriteln("Break thread initialized");
    }

    public static void suspendAllThreads() {
        if (!all_native_threads_started) {
            if (TRACE) Debug.writeln("Native threads haven't started yet.");
            return;
        }
        if (TRACE) Debug.writeln("Suspending all native threads: ", native_threads.length);
        jq_Thread t = Unsafe.getThreadBlock();
        Assert._assert(!t.isThreadSwitchEnabled());
        for (int i = 0; i < native_threads.length; ++i) {
            jq_NativeThread nt = native_threads[i];
            if (nt == t.getNativeThread()) continue;
            for (; ; ) {
                if (TRACE) Debug.writeln("Attempting to suspend native thread ", i);
                nt.suspend();
                jq_Thread t2 = nt.getCurrentJavaThread();
                if (t2.isThreadSwitchEnabled()) break;
                if (TRACE) Debug.writeln("Failed, trying again.");
                nt.resume();
                SystemInterface.msleep(0);
            }
            if (TRACE) Debug.writeln("Success!");
        }
        if (TRACE) Debug.writeln("Finished suspending native threads");
    }

    public static void resumeAllThreads() {
        if (!all_native_threads_started) {
            if (TRACE) Debug.writeln("Native threads haven't started yet.");
            return;
        }
        if (TRACE) Debug.writeln("Resuming all native threads");
        jq_Thread t = Unsafe.getThreadBlock();
        Assert._assert(!t.isThreadSwitchEnabled());
        for (int i = 0; i < native_threads.length; ++i) {
            jq_NativeThread nt = native_threads[i];
            if (nt == t.getNativeThread()) continue;
            if (TRACE) Debug.writeln("Resuming native thread ", i);
            nt.resume();
        }
        if (TRACE) Debug.writeln("All native threads resumed");
    }

    public void dump(jq_RegisterState regs) {
        SystemInterface.debugwriteln(this + ": current Java thread = " + currentThread);
        StackCodeWalker.stackDump(regs.getEip(), regs.getEbp());
        for (int i = 0; i < readyQueue.length; ++i) {
            SystemInterface.debugwriteln(this + ": ready queue " + i + " = " + readyQueue[i]);
        }
        SystemInterface.debugwriteln(this + ": idle queue = " + idleQueue);
        SystemInterface.debugwriteln(this + ": transfer queue = " + transferQueue);
    }

    public jq_ThreadQueue getReadyQueue(int i) {
        return this.readyQueue[i];
    }

    public jq_ThreadQueue getIdleQueue() {
        return this.idleQueue;
    }

    public jq_ThreadQueue getTransferQueue() {
        return this.transferQueue;
    }

    public static final jq_Class _class;

    public static final jq_InstanceMethod _nativeThreadEntry;

    public static final jq_InstanceMethod _schedulerLoop;

    public static final jq_InstanceMethod _threadSwitch;

    public static final jq_StaticMethod _ctrl_break_handler;

    public static final jq_StaticField _num_of_java_threads;

    public static final jq_StaticField _num_of_daemon_threads;

    static {
        _class = (jq_Class) PrimordialClassLoader.loader.getOrCreateBSType("Ljoeq/Scheduler/jq_NativeThread;");
        _nativeThreadEntry = _class.getOrCreateInstanceMethod("nativeThreadEntry", "()V");
        _schedulerLoop = _class.getOrCreateInstanceMethod("schedulerLoop", "()V");
        _threadSwitch = _class.getOrCreateInstanceMethod("threadSwitch", "()V");
        _ctrl_break_handler = _class.getOrCreateStaticMethod("ctrl_break_handler", "()V");
        _num_of_java_threads = _class.getOrCreateStaticField("num_of_java_threads", "I");
        _num_of_daemon_threads = _class.getOrCreateStaticField("num_of_daemon_threads", "I");
    }
}
