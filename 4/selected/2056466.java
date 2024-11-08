package com.ibm.JikesRVM;

import com.ibm.JikesRVM.memoryManagers.vmInterface.MM_Interface;
import com.ibm.JikesRVM.memoryManagers.JMTk.Plan;

/**
 * Multiplex execution of large number of VM_Threads on small 
 * number of o/s kernel threads.
 *
 * @author Bowen Alpern 
 * @author Derek Lieber
 * @modified Peter F. Sweeney (added HPM support)
 */
public final class VM_Processor extends Plan implements VM_Uninterruptible, VM_Constants {

    static final int RVM = 0;

    static final int NATIVE = 1;

    static final int NATIVEDAEMON = 2;

    public static int trace = 0;

    private static final boolean debug_native = false;

    /**
   * For builds where thread switching is deterministic rather than timer driven
   * Initialized in constructor
   */
    public int deterministicThreadSwitchCount;

    public static int numberNativeProcessors = 0;

    public static VM_Synchronizer nativeProcessorCountLock = new VM_Synchronizer();

    public static VM_Processor[] nativeProcessors = new VM_Processor[100];

    public static int numberAttachedProcessors = 0;

    public static VM_Processor[] attachedProcessors = new VM_Processor[100];

    public VM_HardwarePerformanceMonitor hpm;

    /**
   * Create data object to be associated with an o/s kernel thread 
   * (aka "virtual cpu" or "pthread").
   * @param id id that will be returned by getCurrentProcessorId() for 
   * this processor.
   */
    VM_Processor(int id, int processorType) {
        if (VM.runningVM) jtoc = VM_Magic.getJTOC();
        this.id = id;
        this.transferMutex = new VM_ProcessorLock();
        this.transferQueue = new VM_GlobalThreadQueue(VM_EventLogger.TRANSFER_QUEUE, this.transferMutex);
        this.readyQueue = new VM_ThreadQueue(VM_EventLogger.READY_QUEUE);
        this.ioQueue = new VM_ThreadIOQueue(VM_EventLogger.IO_QUEUE);
        this.processWaitQueue = new VM_ThreadProcessWaitQueue(VM_EventLogger.PROCESS_WAIT_QUEUE);
        this.processWaitQueueLock = new VM_ProcessorLock();
        this.idleQueue = new VM_ThreadQueue(VM_EventLogger.IDLE_QUEUE);
        this.lastLockIndex = -1;
        this.isInSelect = false;
        this.processorMode = processorType;
        lastVPStatusIndex = (lastVPStatusIndex + VP_STATUS_STRIDE) % VP_STATUS_SIZE;
        this.vpStatusIndex = lastVPStatusIndex;
        this.vpStatusAddress = VM_Magic.objectAsAddress(vpStatus).add(this.vpStatusIndex << LOG_BYTES_IN_INT);
        if (VM.VerifyAssertions) VM._assert(vpStatus[this.vpStatusIndex] == UNASSIGNED_VP_STATUS);
        vpStatus[this.vpStatusIndex] = IN_JAVA;
        if (VM.BuildForDeterministicThreadSwitching) {
            this.deterministicThreadSwitchCount = VM.deterministicThreadSwitchInterval;
        }
        MM_Interface.setupProcessor(this);
        hpm = new VM_HardwarePerformanceMonitor(id);
    }

    /**
   * Is it ok to switch to a new VM_Thread in this processor?
   */
    boolean threadSwitchingEnabled() throws VM_PragmaInline {
        return threadSwitchingEnabledCount == 1;
    }

    /**
   * Enable thread switching in this processor.
   */
    public void enableThreadSwitching() {
        ++threadSwitchingEnabledCount;
        if (VM.VerifyAssertions) VM._assert(threadSwitchingEnabledCount <= 1);
        if (VM.VerifyAssertions && MM_Interface.gcInProgress()) VM._assert(threadSwitchingEnabledCount < 1 || getCurrentProcessorId() == 0);
        if (threadSwitchingEnabled() && threadSwitchPending) {
            threadSwitchRequested = -1;
            threadSwitchPending = false;
        }
    }

    /**
   * Disable thread switching in this processor.
   */
    public void disableThreadSwitching() throws VM_PragmaInline {
        --threadSwitchingEnabledCount;
    }

    /**
   * Get processor that's being used to run the current java thread.
   */
    public static VM_Processor getCurrentProcessor() throws VM_PragmaInline {
        return VM_ProcessorLocalState.getCurrentProcessor();
    }

    /**
   * Get id of processor that's being used to run the current java thread.
   */
    public static int getCurrentProcessorId() throws VM_PragmaInline {
        return getCurrentProcessor().id;
    }

    /**
   * Become next "ready" thread.
   * Note: This method is ONLY intended for use by VM_Thread.
   * @param timerTick   timer interrupted if true
   */
    void dispatch(boolean timerTick) {
        if (VM.VerifyAssertions) VM._assert(lockCount == 0);
        if (VM.BuildForEventLogging && VM.EventLoggingEnabled) VM_EventLogger.logDispatchEvent();
        VM_Thread newThread = getRunnableThread();
        while (newThread.suspendPending) {
            newThread.suspendLock.lock();
            newThread.suspended = true;
            newThread.suspendLock.unlock();
            newThread = getRunnableThread();
        }
        previousThread = activeThread;
        activeThread = newThread;
        if (!previousThread.isDaemon && idleProcessor != null && !readyQueue.isEmpty() && getCurrentProcessor().processorMode != NATIVEDAEMON) {
            VM_Thread t = readyQueue.dequeue();
            if (trace > 0) VM_Scheduler.trace("VM_Processor", "dispatch: offload ", t.getIndex());
            scheduleThread(t);
        }
        if (VM.EnableCPUMonitoring) {
            double now = VM_Time.now();
            if (previousThread.cpuStartTime != -1) {
                previousThread.cpuTotalTime += now - previousThread.cpuStartTime;
            }
            previousThread.cpuStartTime = 0;
            newThread.cpuStartTime = now;
        }
        if (VM.BuildForHPM && VM_HardwarePerformanceMonitors.hpm_safe && !VM_HardwarePerformanceMonitors.hpm_thread_group) {
            hpm.updateHPMcounters(previousThread, newThread, timerTick);
        }
        threadId = newThread.getLockingId();
        activeThreadStackLimit = newThread.stackLimit;
        VM_Magic.threadSwitch(previousThread, newThread.contextRegisters);
    }

    /**
   * Find a thread that can be run by this processor and remove it 
   * from its queue.
   */
    private VM_Thread getRunnableThread() throws VM_PragmaInline {
        int loopcheck = 0;
        for (int i = transferQueue.length(); 0 < i; i--) {
            transferMutex.lock();
            VM_Thread t = transferQueue.dequeue();
            transferMutex.unlock();
            if (t.isGCThread) {
                if (trace > 1) VM_Scheduler.trace("VM_Processor", "getRunnableThread: collector thread", t.getIndex());
                return t;
            } else if (t.beingDispatched && t != VM_Thread.getCurrentThread()) {
                if (trace > 1) VM_Scheduler.trace("VM_Processor", "getRunnableThread: stack in use", t.getIndex());
                transferMutex.lock();
                transferQueue.enqueue(t);
                transferMutex.unlock();
                if (processorMode == NATIVE) {
                    i++;
                    if (loopcheck++ >= 1000000) break;
                    if (VM.VerifyAssertions) VM._assert(t.isNativeIdleThread, "VM_Processor.getRunnableThread() assert t.isNativeIdleThread");
                }
            } else {
                if (trace > 1) VM_Scheduler.trace("VM_Processor", "getRunnableThread: transfer to readyQueue", t.getIndex());
                readyQueue.enqueue(t);
            }
        }
        if ((epoch % VM_Scheduler.numProcessors) + 1 == id) {
            if (ioQueue.isReady()) {
                VM_Thread t = ioQueue.dequeue();
                if (trace > 1) VM_Scheduler.trace("VM_Processor", "getRunnableThread: ioQueue (early)", t.getIndex());
                if (VM.VerifyAssertions) VM._assert(t.beingDispatched == false || t == VM_Thread.getCurrentThread());
                return t;
            }
        }
        if ((epoch % NUM_TICKS_BETWEEN_WAIT_POLL) == id) {
            VM_Thread result = null;
            processWaitQueueLock.lock();
            if (processWaitQueue.isReady()) {
                VM_Thread t = processWaitQueue.dequeue();
                if (VM.VerifyAssertions) VM._assert(t.beingDispatched == false || t == VM_Thread.getCurrentThread());
                result = t;
            }
            processWaitQueueLock.unlock();
            if (result != null) return result;
        }
        if (!readyQueue.isEmpty()) {
            VM_Thread t = readyQueue.dequeue();
            if (trace > 1) VM_Scheduler.trace("VM_Processor", "getRunnableThread: readyQueue", t.getIndex());
            if (VM.VerifyAssertions) VM._assert(t.beingDispatched == false || t == VM_Thread.getCurrentThread());
            return t;
        }
        if (ioQueue.isReady()) {
            VM_Thread t = ioQueue.dequeue();
            if (trace > 1) VM_Scheduler.trace("VM_Processor", "getRunnableThread: ioQueue", t.getIndex());
            if (VM.VerifyAssertions) VM._assert(t.beingDispatched == false || t == VM_Thread.getCurrentThread());
            return t;
        }
        if (!idleQueue.isEmpty()) {
            VM_Thread t = idleQueue.dequeue();
            if (trace > 1) VM_Scheduler.trace("VM_Processor", "getRunnableThread: idleQueue", t.getIndex());
            if (VM.VerifyAssertions) VM._assert(t.beingDispatched == false || t == VM_Thread.getCurrentThread());
            return t;
        }
        VM._assert(VM.NOT_REACHED);
        return null;
    }

    /**
   * Add a thread to this processor's transfer queue.
   */
    private void transferThread(VM_Thread t) {
        if (this != getCurrentProcessor() || t.isGCThread || (t.beingDispatched && t != VM_Thread.getCurrentThread())) {
            transferMutex.lock();
            transferQueue.enqueue(t);
            transferMutex.unlock();
        } else if (t.isIdleThread) {
            idleQueue.enqueue(t);
        } else {
            readyQueue.enqueue(t);
        }
    }

    /**
   * non-null --> a processor that has no work to do
   */
    static VM_Processor idleProcessor;

    /**
   * Put thread onto most lightly loaded virtual processor.
   */
    public void scheduleThread(VM_Thread t) {
        if (t.processorAffinity != null) {
            if (trace > 0) {
                VM_Scheduler.trace("VM_Processor.scheduleThread", "outgoing to specific processor:", t.getIndex());
            }
            t.processorAffinity.transferThread(t);
            return;
        }
        if (t == VM_Thread.getCurrentThread() && readyQueue.isEmpty() && transferQueue.isEmpty()) {
            if (trace > 0) VM_Scheduler.trace("VM_Processor.scheduleThread", "staying on same processor:", t.getIndex());
            getCurrentProcessor().transferThread(t);
            return;
        }
        VM_Processor idle = idleProcessor;
        if (idle != null) {
            idleProcessor = null;
            if (trace > 0) VM_Scheduler.trace("VM_Processor.scheduleThread", "outgoing to idle processor:", t.getIndex());
            idle.transferThread(t);
            return;
        }
        if (trace > 0) VM_Scheduler.trace("VM_Processor.scheduleThread", "outgoing to round-robin processor:", t.getIndex());
        chooseNextProcessor(t).transferThread(t);
    }

    /**
   * Cycle (round robin) through the available processors.
   */
    private VM_Processor chooseNextProcessor(VM_Thread t) {
        t.chosenProcessorId = (t.chosenProcessorId % VM_Scheduler.numProcessors) + 1;
        return VM_Scheduler.processors[t.chosenProcessorId];
    }

    public static final int UNASSIGNED_VP_STATUS = 0;

    public static final int IN_JAVA = 1;

    public static final int IN_NATIVE = 2;

    public static final int BLOCKED_IN_NATIVE = 3;

    public static final int IN_SIGWAIT = 4;

    public static final int BLOCKED_IN_SIGWAIT = 5;

    static int generateNativeProcessorId() throws VM_PragmaInterruptible {
        int r;
        synchronized (nativeProcessorCountLock) {
            r = ++numberNativeProcessors;
        }
        return -r;
    }

    /**
   *  For JNI createJVM and attachCurrentThread: create a VM_Processor 
   * structure to represent a pthread that has been created externally 
   * It will have:
   *   -the normal idle queue with a VM_NativeIdleThread
   * It will not have:
   *   -a newly created pthread as in the normal case
   * The reference to this VM_Processor will be held in two places:
   *   -as nativeAffinity in the Java thread created by the JNI call
   *   -as an entry in the attachedProcessors array for use by GC
   *
   */
    static VM_Processor createNativeProcessorForExistingOSThread(VM_Thread withThisThread) throws VM_PragmaInterruptible {
        VM_Processor newProcessor = new VM_Processor(generateNativeProcessorId(), NATIVE);
        VM_Thread t = new VM_NativeIdleThread(newProcessor, true);
        t.start(newProcessor.idleQueue);
        newProcessor.activeThread = withThisThread;
        newProcessor.activeThreadStackLimit = withThisThread.stackLimit;
        newProcessor.isInitialized = true;
        if (registerAttachedProcessor(newProcessor) != 0) return newProcessor; else return null;
    }

    static int registerAttachedProcessor(VM_Processor newProcessor) throws VM_PragmaInterruptible {
        if (numberAttachedProcessors == 100) {
            return 0;
        }
        for (int i = 1; i < attachedProcessors.length; i++) {
            if (attachedProcessors[i] == null) {
                attachedProcessors[i] = newProcessor;
                numberAttachedProcessors++;
                return i;
            }
        }
        return 0;
    }

    static int unregisterAttachedProcessor(VM_Processor pr) throws VM_PragmaInterruptible {
        for (int i = 1; i < attachedProcessors.length; i++) {
            if (attachedProcessors[i] != pr) {
                attachedProcessors[i] = null;
                numberAttachedProcessors--;
                return i;
            }
        }
        return 0;
    }

    static VM_Processor createNativeProcessor() throws VM_PragmaInterruptible {
        VM.sysWrite("VM_Processor createNativeProcessor NOT YET IMPLEMENTED for IA32\n");
        VM.sysExit(666);
        return null;
        VM_Processor newProcessor = new VM_Processor(0, NATIVE);
        VM.disableGC();
        synchronized (nativeProcessorCountLock) {
            int processId = generateNativeProcessorId();
            newProcessor.id = processId;
            nativeProcessors[-processId] = newProcessor;
            if (debug_native) {
                VM_Scheduler.trace("VM_Processor", "created native processor", processId);
            }
        }
        VM.enableGC();
        VM_Thread t = new VM_NativeIdleThread(newProcessor);
        t.start(newProcessor.transferQueue);
        VM_Thread target = new VM_StartupThread(MM_Interface.newStack(STACK_SIZE_NORMAL >> LOG_BYTES_IN_ADDRESS));
        VM.disableGC();
        VM.sysInitializeStartupLocks(1);
        newProcessor.activeThread = target;
        newProcessor.activeThreadStackLimit = target.stackLimit;
        target.registerThread();
        VM.sysVirtualProcessorCreate(VM_Magic.getTocPointer(), VM_Magic.objectAsAddress(newProcessor), target.contextRegisters.gprs.get(VM.THREAD_ID_REGISTER).toAddress(), target.contextRegisters.getInnermostFramePointer());
        while (!newProcessor.isInitialized) VM.sysVirtualProcessorYield();
        VM.enableGC();
        if (debug_native) {
            VM_Scheduler.trace("VM_Processor", "started native processor", newProcessor.id);
            VM_Scheduler.trace("VM_Processor", "native processor pthread_id", newProcessor.pthread_id);
        }
        return newProcessor;
    }

    /**
   * Called during thread startup to stash the ID of the
   * {@link VM_Processor} in its pthread's thread-specific storage,
   * which will allow us to access the VM_Processor from
   * arbitrary native code.  This is enabled IFF we are intercepting
   * blocking system calls.
   */
    void stashProcessorInPthread() {
        VM_SysCall.call1(VM_BootRecord.the_boot_record.sysStashVmProcessorIdInPthreadIP, this.id);
    }

    /**
   * sets the VP status to BLOCKED_IN_NATIVE if it is currently IN_NATIVE (ie C)
   * returns true if BLOCKED_IN_NATIVE
   */
    public boolean lockInCIfInC() {
        int newState, oldState;
        boolean result = true;
        do {
            oldState = VM_Magic.prepareInt(VM_Magic.addressAsObject(vpStatusAddress), 0);
            if (VM.VerifyAssertions) VM._assert(oldState != BLOCKED_IN_NATIVE);
            if (oldState != IN_NATIVE) {
                if (VM.VerifyAssertions) VM._assert((oldState == IN_JAVA) || (oldState == IN_SIGWAIT));
                result = false;
                break;
            }
            newState = BLOCKED_IN_NATIVE;
        } while (!(VM_Magic.attemptInt(VM_Magic.addressAsObject(vpStatusAddress), 0, oldState, newState)));
        return result;
    }

    public boolean blockInWaitIfInWait() {
        int oldState;
        boolean result = true;
        do {
            oldState = VM_Magic.prepareInt(VM_Magic.addressAsObject(vpStatusAddress), 0);
            if (VM.VerifyAssertions) VM._assert(oldState != BLOCKED_IN_SIGWAIT);
            if (oldState != IN_SIGWAIT) {
                if (VM.VerifyAssertions) VM._assert(oldState == IN_JAVA);
                result = false;
                break;
            }
        } while (!(VM_Magic.attemptInt(VM_Magic.addressAsObject(vpStatusAddress), 0, oldState, BLOCKED_IN_SIGWAIT)));
        return result;
    }

    /**
   * Is it time for this processor's currently running VM_Thread 
   * to call its "threadSwitch" method?
   * A value of: 
   *    -1 means yes
   *     0 means no
   * This word is set by a timer interrupt every 10 milliseconds and
   * interrogated by every compiled method, typically in the method's prologue.
   */
    public int threadSwitchRequested;

    /**
   * thread currently running on this processor
   */
    public VM_Thread activeThread;

    /**
   * cached activeThread.stackLimit;
   * removes 1 load from stackoverflow sequence.
   */
    public VM_Address activeThreadStackLimit;

    /**
   * Base pointer of JTOC (VM_Statics.slots)
   */
    Object jtoc;

    /**
   * Thread id of VM_Thread currently executing on the processor
   */
    public int threadId;

    /**
   * FP for current frame
   */
    VM_Address framePointer;

    /**
   * "hidden parameter" for interface invocation thru the IMT
   */
    int hiddenSignatureId;

    /**
   * "hidden parameter" from ArrayIndexOutOfBounds trap to C trap handler
   */
    int arrayIndexTrapParam;

    public final Plan mmPlan = new Plan();

    public int large_live;

    public int small_live;

    public long totalBytesAllocated;

    public long totalObjectsAllocated;

    public long synchronizedObjectsAllocated;

    /**
   * Identity of this processor.
   * Note: 1. VM_Scheduler.processors[id] == this processor
   *      2. id must be non-zero because it is used in 
   *      VM_ProcessorLock ownership tests
   */
    public int id;

    /**
   * Has this processor's pthread initialization completed yet?
   * A value of:
   *   false means "cpu is still executing C code (on a C stack)"
   *   true  means "cpu is now executing vm code (on a vm stack)"
   */
    boolean isInitialized;

    /**
   * Should this processor dispatch a new VM_Thread when 
   * "threadSwitch" is called?
   * Also used to decide if it's safe to call yield() when 
   * contending for a lock.
   * A value of:
   *    1 means "yes" (switching enabled)
   * <= 0 means "no"  (switching disabled)
   */
    int threadSwitchingEnabledCount;

    /**
   * Was "threadSwitch" called while this processor had 
   * thread switching disabled?
   */
    boolean threadSwitchPending;

    /**
   * thread previously running on this processor
   */
    public VM_Thread previousThread;

    /**
   * threads to be added to ready queue
   */
    public VM_GlobalThreadQueue transferQueue;

    public VM_ProcessorLock transferMutex;

    /**
   * threads waiting for a timeslice in which to run
   */
    VM_ThreadQueue readyQueue;

    /**
   * Threads waiting for a subprocess to exit.
   */
    VM_ThreadProcessWaitQueue processWaitQueue;

    /**
   * Lock protecting a process wait queue.
   * This is needed because a thread may need to switch
   * to a different <code>VM_Processor</code> in order to perform
   * a waitpid.  (This is because of Linux's weird pthread model,
   * in which pthreads are essentially processes.)
   */
    VM_ProcessorLock processWaitQueueLock;

    /**
   * threads waiting for i/o
   */
    public VM_ThreadIOQueue ioQueue;

    /**
   * thread to run when nothing else to do
   */
    public VM_ThreadQueue idleQueue;

    /**
   * number of processor locks currently held (for._assertion checking)
   */
    public int lockCount;

    /**
   * Is the processor doing a select with a wait option
   * A value of:
   *   false means "processor is not executing a select"
   *   true  means "processor is  executing a select with a wait option"
   */
    boolean isInSelect;

    /**
   * This thread's successor on a VM_ProcessorQueue.
   */
    public VM_Processor next;

    int processorMode;

    public static final int VP_STATUS_SIZE = 8000;

    public static final int VP_STATUS_STRIDE = 101;

    public static int lastVPStatusIndex = 0;

    public static int[] vpStatus = new int[VP_STATUS_SIZE];

    static int epoch = 0;

    /**
   * Number of timer ticks between checks of the process wait
   * queue.  Assuming a tick frequency of 10 milliseconds, we will
   * check about twice per second.  Waiting for processes
   * to die is almost certainly not going to be on a performance-critical
   * code path, and we don't want to add unnecessary overhead to
   * the thread scheduler.
   */
    public static final int NUM_TICKS_BETWEEN_WAIT_POLL = 50;

    /**
   * index of this processor's status word in vpStatus array
   */
    public int vpStatusIndex;

    /**
   * address of this processors status word in vpStatus array
   */
    public VM_Address vpStatusAddress;

    /**
   * pthread_id (AIX's) for use by signal to wakeup
   * sleeping idle thread (signal accomplished by pthread_kill call)
   * 
   * CRA, Maria
   */
    public int pthread_id;

    int firstLockIndex;

    int lastLockIndex;

    int nextLockIndex;

    VM_Lock freeLock;

    int freeLocks;

    int locksAllocated;

    int locksFreed;

    VM_ProcessorLock awaitingProcessorLock;

    VM_Processor contenderLink;

    private double scratchSeconds;

    private double scratchNanoseconds;

    public void dumpProcessorState() throws VM_PragmaInterruptible {
        VM_Scheduler.writeString("Processor ");
        VM_Scheduler.writeDecimal(id);
        if (this == VM_Processor.getCurrentProcessor()) VM_Scheduler.writeString(" (me)");
        VM_Scheduler.writeString(" running thread");
        if (activeThread != null) activeThread.dump(); else VM_Scheduler.writeString(" NULL Active Thread");
        VM_Scheduler.writeString("\n");
        VM_Scheduler.writeString(" system thread id ");
        VM_Scheduler.writeDecimal(pthread_id);
        VM_Scheduler.writeString("\n");
        VM_Scheduler.writeString(" transferQueue:");
        if (transferQueue != null) transferQueue.dump();
        VM_Scheduler.writeString(" readyQueue:");
        if (readyQueue != null) readyQueue.dump();
        VM_Scheduler.writeString(" ioQueue:");
        if (ioQueue != null) ioQueue.dump();
        VM_Scheduler.writeString(" processWaitQueue:");
        if (processWaitQueue != null) processWaitQueue.dump();
        VM_Scheduler.writeString(" idleQueue:");
        if (idleQueue != null) idleQueue.dump();
        if (processorMode == RVM) VM_Scheduler.writeString(" mode: RVM\n"); else if (processorMode == NATIVE) VM_Scheduler.writeString(" mode: NATIVE\n"); else if (processorMode == NATIVEDAEMON) VM_Scheduler.writeString(" mode: NATIVEDAEMON\n");
        VM_Scheduler.writeString(" status: ");
        int status = vpStatus[vpStatusIndex];
        if (status == IN_NATIVE) VM_Scheduler.writeString("IN_NATIVE\n");
        if (status == IN_JAVA) VM_Scheduler.writeString("IN_JAVA\n");
        if (status == BLOCKED_IN_NATIVE) VM_Scheduler.writeString("BLOCKED_IN_NATIVE\n");
        if (status == IN_SIGWAIT) VM_Scheduler.writeString("IN_SIGWAIT\n");
        if (status == BLOCKED_IN_SIGWAIT) VM_Scheduler.writeString("BLOCKED_IN_SIGWAIT\n");
        VM_Scheduler.writeString(" threadSwitchRequested: ");
        VM_Scheduler.writeDecimal(threadSwitchRequested);
        VM_Scheduler.writeString("\n");
    }

    /**
   * flag indicating this processor need synchronization.
   */
    public boolean needsSync = false;
}
