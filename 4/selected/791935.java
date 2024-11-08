package com.ibm.JikesRVM;

import com.ibm.JikesRVM.memoryManagers.vmInterface.VM_CollectorThread;
import com.ibm.JikesRVM.memoryManagers.vmInterface.MM_Interface;
import com.ibm.JikesRVM.classloader.*;
import com.ibm.JikesRVM.opt.*;
import com.ibm.JikesRVM.OSR.OSR_ObjectHolder;

/**
 * Global variables used to implement virtual machine thread scheduler.
 *    - virtual cpus
 *    - threads
 *    - queues
 *    - locks
 *
 * @author Bowen Alpern
 * @author Derek Lieber
 * PFS added calls to set HPM settings and start counting.
 */
public class VM_Scheduler implements VM_Constants, VM_Uninterruptible {

    /** Index of initial processor in which "VM.boot()" runs. */
    public static final int PRIMORDIAL_PROCESSOR_ID = 1;

    /** Index of thread in which "VM.boot()" runs */
    public static final int PRIMORDIAL_THREAD_INDEX = 1;

    /**
   * Maximum number of VM_Processor's that we can support. In SMP builds
   * the NativeDaemonProcessor takes one slot & the RVM can be run with
   * 1 to MAX_PROCESSORS-1 processors
   **/
    public static final int MAX_PROCESSORS = 13;

    /** Maximum number of VM_Thread's that we can support. */
    public static final int LOG_MAX_THREADS = 14;

    public static final int MAX_THREADS = 1 << LOG_MAX_THREADS;

    public static final int NO_CPU_AFFINITY = -1;

    public static int cpuAffinity = NO_CPU_AFFINITY;

    public static int numProcessors = 1;

    public static VM_Processor[] processors;

    public static boolean allProcessorsInitialized;

    public static boolean terminated;

    public static int nativeDPndx;

    public static int timeSlice = 10;

    public static VM_Thread[] threads;

    static int threadAllocationIndex;

    static int numActiveThreads;

    static int numDaemons;

    static VM_ProcessorLock threadCreationMutex;

    static VM_ProcessorQueue deadVPQueue;

    static VM_ProcessorQueue availableProcessorQueue;

    static VM_ProxyWakeupQueue wakeupQueue;

    static VM_ProcessorLock wakeupMutex;

    static VM_ThreadQueue debuggerQueue;

    static VM_ProcessorLock debuggerMutex;

    public static VM_ThreadQueue collectorQueue;

    public static VM_ProcessorLock collectorMutex;

    public static VM_ThreadQueue finalizerQueue;

    public static VM_ProcessorLock finalizerMutex;

    public static VM_ProcessorQueue nativeProcessorQueue;

    public static VM_ProcessorLock nativeProcessorMutex;

    public static VM_ThreadQueue attachThreadQueue;

    public static VM_ProcessorLock attachThreadMutex;

    public static VM_ProcessorLock outputMutex;

    public static VM_Lock[] locks;

    public static boolean debugRequested;

    public static VM_Address attachThreadRequested;

    static final boolean countLocks = false;

    static int globalEpoch = -1;

    static final int EPOCH_MAX = 32 * 1024;

    /**
   * Initialize boot image.
   */
    static void init() throws VM_PragmaInterruptible {
        threadCreationMutex = new VM_ProcessorLock();
        outputMutex = new VM_ProcessorLock();
        threads = new VM_Thread[MAX_THREADS];
        threadAllocationIndex = PRIMORDIAL_THREAD_INDEX;
        VM_BootRecord.the_boot_record.dumpStackAndDieOffset = VM_Entrypoints.dumpStackAndDieMethod.getOffset();
        processors = new VM_Processor[1 + PRIMORDIAL_PROCESSOR_ID];
        processors[PRIMORDIAL_PROCESSOR_ID] = new VM_Processor(PRIMORDIAL_PROCESSOR_ID, VM_Processor.RVM);
        VM_Lock.init();
    }

    static void processArg(String arg) throws VM_PragmaInterruptible {
        if (arg.startsWith("timeslice=")) {
            String tmp = arg.substring(10);
            int slice = Integer.parseInt(tmp);
            if (slice < 10 || slice > 999) VM.sysFail("Time slice outside range (10..999) " + slice);
            timeSlice = slice;
        } else if (arg.startsWith("verbose=")) {
            String tmp = arg.substring(8);
            VM_Processor.trace = Integer.parseInt(tmp);
        }
    }

    /**
   * Begin multi-threaded vm operation.
   */
    static void boot() throws VM_PragmaInterruptible {
        if (VM.VerifyAssertions) VM._assert(1 <= numProcessors && numProcessors <= MAX_PROCESSORS);
        if (VM.TraceThreads) trace("VM_Scheduler.boot", "numProcessors =", numProcessors);
        VM_Processor primordialProcessor = processors[PRIMORDIAL_PROCESSOR_ID];
        primordialProcessor.hpm.boot();
        processors = new VM_Processor[1 + numProcessors + 1];
        processors[PRIMORDIAL_PROCESSOR_ID] = primordialProcessor;
        for (int i = PRIMORDIAL_PROCESSOR_ID; ++i <= numProcessors; ) {
            processors[i] = new VM_Processor(i, VM_Processor.RVM);
            processors[i].hpm.boot();
        }
        primordialProcessor.vpStatusAddress = VM_Magic.objectAsAddress(VM_Processor.vpStatus).add(primordialProcessor.vpStatusIndex << LOG_BYTES_IN_INT);
        nativeDPndx = numProcessors + 1;
        if (VM.BuildWithNativeDaemonProcessor) {
            processors[nativeDPndx] = new VM_Processor(numProcessors + 1, VM_Processor.NATIVEDAEMON);
            if (VM.TraceThreads) trace("VM_Scheduler.boot", "created nativeDaemonProcessor with id", nativeDPndx);
        } else {
            processors[nativeDPndx] = null;
            if (VM.TraceThreads) trace("VM_Scheduler.boot", "NativeDaemonProcessor not created");
        }
        wakeupQueue = new VM_ProxyWakeupQueue(VM_EventLogger.WAKEUP_QUEUE);
        wakeupMutex = new VM_ProcessorLock();
        debuggerQueue = new VM_ThreadQueue(VM_EventLogger.DEBUGGER_QUEUE);
        debuggerMutex = new VM_ProcessorLock();
        attachThreadQueue = new VM_ThreadQueue(VM_EventLogger.ATTACHTHREAD_QUEUE);
        attachThreadMutex = new VM_ProcessorLock();
        collectorQueue = new VM_ThreadQueue(VM_EventLogger.COLLECTOR_QUEUE);
        collectorMutex = new VM_ProcessorLock();
        finalizerQueue = new VM_ThreadQueue(VM_EventLogger.FINALIZER_QUEUE);
        finalizerMutex = new VM_ProcessorLock();
        nativeProcessorQueue = new VM_ProcessorQueue(VM_EventLogger.DEAD_VP_QUEUE);
        nativeProcessorMutex = new VM_ProcessorLock();
        deadVPQueue = new VM_ProcessorQueue(VM_EventLogger.DEAD_VP_QUEUE);
        availableProcessorQueue = new VM_ProcessorQueue(VM_EventLogger.DEAD_VP_QUEUE);
        VM_CollectorThread.boot(numProcessors);
        for (int i = 0; i < numProcessors; ++i) {
            VM_Thread t = new VM_IdleThread(processors[1 + i]);
            t.start(processors[1 + i].idleQueue);
        }
        if (VM.BuildWithNativeDaemonProcessor) {
            VM_Thread t = new VM_IdleThread(processors[nativeDPndx]);
            t.start(processors[nativeDPndx].idleQueue);
            t = new VM_NativeDaemonThread(processors[nativeDPndx]);
            t.start(processors[nativeDPndx].readyQueue);
        }
        attachThreadRequested = VM_Address.zero();
        terminated = false;
        processors[PRIMORDIAL_PROCESSOR_ID].isInitialized = true;
        VM.sysCreateThreadSpecificDataKeys();
        System.loadLibrary("syswrap");
        if (VM.BuildWithNativeDaemonProcessor) VM.sysInitializeStartupLocks(numProcessors + 1); else VM.sysInitializeStartupLocks(numProcessors);
        if (cpuAffinity != NO_CPU_AFFINITY) VM.sysVirtualProcessorBind(cpuAffinity + PRIMORDIAL_PROCESSOR_ID - 1);
        for (int i = PRIMORDIAL_PROCESSOR_ID; ++i <= numProcessors; ) {
            VM_Thread target = new VM_StartupThread(MM_Interface.newStack(STACK_SIZE_NORMAL >> 2));
            if (VM.TraceThreads) trace("VM_Scheduler.boot", "starting processor id", i);
            processors[i].activeThread = target;
            processors[i].activeThreadStackLimit = target.stackLimit;
            target.registerThread();
            if (VM.BuildForPowerPC) {
                VM.sysVirtualProcessorCreate(VM_Magic.getTocPointer(), VM_Magic.objectAsAddress(processors[i]), target.contextRegisters.gprs.get(THREAD_ID_REGISTER).toAddress(), target.contextRegisters.getInnermostFramePointer());
            } else if (VM.BuildForIA32) {
                VM.sysVirtualProcessorCreate(VM_Magic.getTocPointer(), VM_Magic.objectAsAddress(processors[i]), target.contextRegisters.ip, target.contextRegisters.getInnermostFramePointer());
            }
        }
        if (VM.BuildWithNativeDaemonProcessor) {
            VM_Thread target = new VM_StartupThread(MM_Interface.newStack(STACK_SIZE_NORMAL >> LOG_BYTES_IN_ADDRESS));
            processors[nativeDPndx].activeThread = target;
            processors[nativeDPndx].activeThreadStackLimit = target.stackLimit;
            target.registerThread();
            if (VM.TraceThreads) trace("VM_Scheduler.boot", "starting native daemon processor id", nativeDPndx);
            if (VM.BuildForPowerPC) {
                VM.sysVirtualProcessorCreate(VM_Magic.getTocPointer(), VM_Magic.objectAsAddress(processors[nativeDPndx]), target.contextRegisters.gprs.get(THREAD_ID_REGISTER).toAddress(), target.contextRegisters.getInnermostFramePointer());
            } else if (VM.BuildForIA32) {
                VM.sysVirtualProcessorCreate(VM_Magic.getTocPointer(), VM_Magic.objectAsAddress(processors[nativeDPndx]), target.contextRegisters.ip, target.contextRegisters.getInnermostFramePointer());
            }
            if (VM.TraceThreads) trace("VM_Scheduler.boot", "started native daemon processor id", nativeDPndx);
        }
        VM.sysWaitForVirtualProcessorInitialization();
        allProcessorsInitialized = true;
        VM_Processor.getCurrentProcessor().enableThreadSwitching();
        if (!VM.BuildForDeterministicThreadSwitching) VM.sysVirtualProcessorEnableTimeSlicing(timeSlice);
        if (VM.BuildForEventLogging) VM_EventLogger.boot();
        VM.sysWaitForMultithreadingStart();
        OSR_ObjectHolder.boot();
        for (int i = 0; i < numProcessors; ++i) {
            VM_Thread t = VM_CollectorThread.createActiveCollectorThread(processors[1 + i]);
            t.start(processors[1 + i].readyQueue);
        }
        if (VM.BuildWithNativeDaemonProcessor) {
            VM_Thread t = VM_CollectorThread.createActiveCollectorThread(processors[nativeDPndx]);
            t.start(processors[nativeDPndx].readyQueue);
        }
        FinalizerThread tt = new FinalizerThread();
        tt.makeDaemon(true);
        tt.start();
        VM_Processor.getCurrentProcessor().stashProcessorInPthread();
    }

    /**
   * Terminate all the pthreads that belong to the VM
   * This path is used when the VM is taken down by an external pthread via 
   * the JNI call DestroyJavaVM.  All pthreads in the VM must eventually reach this 
   * method from VM_Thread.terminate() for the termination to proceed and for control 
   * to return to the pthread that calls DestroyJavaVM
   * Going by the order in processor[], the pthread for each processor will join with 
   * the next one, and the external pthread calling DestroyJavaVM will join with the
   * main pthread of the VM (see libjni.C)
   *
   * Note:  the NativeIdleThread's don't need to be terminated since they don't have
   * their own pthread;  they run on the external pthreads that had called CreateJavaVM
   * or AttachCurrentThread.
   * 
   */
    static void processorExit(int rc) {
        terminated = true;
        VM_Processor myVP = VM_Processor.getCurrentProcessor();
        VM_Processor VPtoWaitFor = null;
        for (int i = 1; i < numProcessors; i++) {
            if (processors[i] == myVP) {
                VPtoWaitFor = processors[i + 1];
                break;
            }
        }
        if (VPtoWaitFor != null) {
            VM_SysCall.call1(VM_BootRecord.the_boot_record.sysPthreadJoinIP, VPtoWaitFor.pthread_id);
        }
        VM_SysCall.call0(VM_BootRecord.the_boot_record.sysPthreadExitIP);
        if (VM.VerifyAssertions) VM._assert(VM.NOT_REACHED);
    }

    private static final boolean traceDetails = false;

    /**
   * Print out message in format "p[j] (cez#td) who: what", where:
   *    p  = processor id
   *    j  = java thread id
   *    c* = ava thread id of the owner of threadCreationMutex (if any)
   *    e* = java thread id of the owner of threadExecutionMutex (if any)
   *    z* = VM_Processor.getCurrentProcessor().threadSwitchingEnabledCount
   *         (0 means thread switching is enabled outside of the call to debug)
   *    t* = numActiveThreads
   *    d* = numDaemons
   * 
   * * parenthetical values, printed only if traceDetails = true)
   * 
   * We serialize against a mutex to avoid intermingling debug output from multiple threads.
   */
    public static void trace(String who, String what) {
        lockOutput();
        VM_Processor.getCurrentProcessor().disableThreadSwitching();
        writeDecimal(VM_Processor.getCurrentProcessorId());
        writeString("[");
        VM_Thread t = VM_Thread.getCurrentThread();
        t.dump();
        writeString("] ");
        if (traceDetails) {
            writeString("(");
            writeDecimal(numDaemons);
            writeString("/");
            writeDecimal(numActiveThreads);
            writeString(") ");
        }
        writeString(who);
        writeString(": ");
        writeString(what);
        writeString("\n");
        VM_Processor.getCurrentProcessor().enableThreadSwitching();
        unlockOutput();
    }

    /**
   * Print out message in format "p[j] (cez#td) who: what howmany", where:
   *    p  = processor id
   *    j  = java thread id
   *    c* = java thread id of the owner of threadCreationMutex (if any)
   *    e* = java thread id of the owner of threadExecutionMutex (if any)
   *    z* = VM_Processor.getCurrentProcessor().threadSwitchingEnabledCount
   *         (0 means thread switching is enabled outside of the call to debug)
   *    t* = numActiveThreads
   *    d* = numDaemons
   * 
   * * parenthetical values, printed only if traceDetails = true)
   * 
   * We serialize against a mutex to avoid intermingling debug output from multiple threads.
   */
    public static void trace(String who, String what, int howmany) {
        _trace(who, what, howmany, false);
    }

    public static void traceHex(String who, String what, int howmany) {
        _trace(who, what, howmany, true);
    }

    public static void trace(String who, String what, VM_Address addr) {
        VM_Processor.getCurrentProcessor().disableThreadSwitching();
        lockOutput();
        writeDecimal(VM_Processor.getCurrentProcessorId());
        writeString("[");
        VM_Thread.getCurrentThread().dump();
        writeString("] ");
        if (traceDetails) {
            writeString("(");
            writeDecimal(numDaemons);
            writeString("/");
            writeDecimal(numActiveThreads);
            writeString(") ");
        }
        writeString(who);
        writeString(": ");
        writeString(what);
        writeString(" ");
        writeHex(addr);
        writeString("\n");
        unlockOutput();
        VM_Processor.getCurrentProcessor().enableThreadSwitching();
    }

    private static void _trace(String who, String what, int howmany, boolean hex) {
        VM_Processor.getCurrentProcessor().disableThreadSwitching();
        lockOutput();
        writeDecimal(VM_Processor.getCurrentProcessorId());
        writeString("[");
        VM_Thread.getCurrentThread().dump();
        writeString("] ");
        if (traceDetails) {
            writeString("(");
            writeDecimal(numDaemons);
            writeString("/");
            writeDecimal(numActiveThreads);
            writeString(") ");
        }
        writeString(who);
        writeString(": ");
        writeString(what);
        writeString(" ");
        if (hex) writeHex(howmany); else writeDecimal(howmany);
        writeString("\n");
        unlockOutput();
        VM_Processor.getCurrentProcessor().enableThreadSwitching();
    }

    /**
   * Print interesting scheduler information, starting with a stack traceback.
   * Note: the system could be in a fragile state when this method
   * is called, so we try to rely on as little runtime functionality
   * as possible (eg. use no bytecodes that require VM_Runtime support).
   */
    static void traceback(String message) {
        VM_Processor.getCurrentProcessor().disableThreadSwitching();
        lockOutput();
        tracebackWithoutLock(message);
        unlockOutput();
        VM_Processor.getCurrentProcessor().enableThreadSwitching();
    }

    static void tracebackWithoutLock(String message) {
        writeString(message);
        writeString("\n");
        dumpStack(VM_Magic.getCallerFramePointer(VM_Magic.getFramePointer()));
    }

    /**
   * Dump stack of calling thread, starting at callers frame
   */
    public static void dumpStack() {
        dumpStack(VM_Magic.getFramePointer());
    }

    /**
   * Dump state of a (stopped) thread's stack.
   * @param fp address of starting frame. first frame output
   *           is the calling frame of passed frame
   */
    static void dumpStack(VM_Address fp) {
        VM_Address ip = VM_Magic.getReturnAddress(fp);
        fp = VM_Magic.getCallerFramePointer(fp);
        dumpStack(ip, fp);
    }

    /**
   * Dump state of a (stopped) thread's stack.
   * @param fp & ip for first frame to dump
   */
    public static void dumpStack(VM_Address ip, VM_Address fp) {
        writeString("\n-- Stack --\n");
        while (VM_Magic.getCallerFramePointer(fp).NE(STACKFRAME_SENTINEL_FP)) {
            if (!MM_Interface.addrInVM(ip)) {
                writeString("   <native frame>\n");
                ip = VM_Magic.getReturnAddress(fp);
                fp = VM_Magic.getCallerFramePointer(fp);
                continue;
            }
            int compiledMethodId = VM_Magic.getCompiledMethodID(fp);
            if (compiledMethodId == INVISIBLE_METHOD_ID) {
                writeString("   <invisible method>\n");
            } else {
                VM_CompiledMethod compiledMethod = VM_CompiledMethods.getCompiledMethod(compiledMethodId);
                if (compiledMethod.getCompilerType() == VM_CompiledMethod.TRAP) {
                    writeString("   <hardware trap>\n");
                } else {
                    VM_Method method = compiledMethod.getMethod();
                    VM_Offset instructionOffset = ip.diff(VM_Magic.objectAsAddress(compiledMethod.getInstructions()));
                    int lineNumber = compiledMethod.findLineNumberForInstruction(instructionOffset);
                    if (compiledMethod.getCompilerType() == VM_CompiledMethod.OPT) {
                        VM_OptCompiledMethod optInfo = (VM_OptCompiledMethod) compiledMethod;
                        VM_OptMachineCodeMap map = optInfo.getMCMap();
                        int iei = map.getInlineEncodingForMCOffset(instructionOffset);
                        if (iei >= 0) {
                            int[] inlineEncoding = map.inlineEncoding;
                            int bci = map.getBytecodeIndexForMCOffset(instructionOffset);
                            for (int j = iei; j >= 0; j = VM_OptEncodedCallSiteTree.getParent(j, inlineEncoding)) {
                                int mid = VM_OptEncodedCallSiteTree.getMethodID(j, inlineEncoding);
                                method = VM_MemberReference.getMemberRef(mid).asMethodReference().getResolvedMember();
                                lineNumber = ((VM_NormalMethod) method).getLineNumberForBCIndex(bci);
                                writeString("   ");
                                writeAtom(method.getDeclaringClass().getDescriptor());
                                writeString(" ");
                                writeAtom(method.getName());
                                writeAtom(method.getDescriptor());
                                writeString(" at line ");
                                writeDecimal(lineNumber);
                                writeString("\n");
                                if (j > 0) bci = VM_OptEncodedCallSiteTree.getByteCodeOffset(j, inlineEncoding);
                            }
                        } else {
                            writeString("   Unknown location in opt compiled method ");
                            writeAtom(method.getDeclaringClass().getDescriptor());
                            writeString(" ");
                            writeAtom(method.getName());
                            writeAtom(method.getDescriptor());
                            writeString("\n");
                        }
                        ip = VM_Magic.getReturnAddress(fp);
                        fp = VM_Magic.getCallerFramePointer(fp);
                        continue;
                    }
                    writeString("   ");
                    writeAtom(method.getDeclaringClass().getDescriptor());
                    writeString(" ");
                    writeAtom(method.getName());
                    writeAtom(method.getDescriptor());
                    writeString(" at line ");
                    writeDecimal(lineNumber);
                    writeString("\n");
                }
            }
            ip = VM_Magic.getReturnAddress(fp);
            fp = VM_Magic.getCallerFramePointer(fp);
        }
    }

    private static boolean exitInProgress = false;

    /**
   * Dump state of a (stopped) thread's stack and exit the virtual machine.
   * @param fp address of starting frame
   * Returned: doesn't return.
   * This method is called from RunBootImage.C when something goes horrifically
   * wrong with exception handling and we want to die with useful diagnostics.
   */
    public static void dumpStackAndDie(VM_Address fp) {
        if (!exitInProgress) {
            exitInProgress = true;
            dumpStack(fp);
            VM.sysExit(9999);
        } else {
            VM_SysCall.call1(VM_BootRecord.the_boot_record.sysExitIP, 9999);
        }
    }

    /**
   * Dump state of virtual machine.
   */
    public static void dumpVirtualMachine() throws VM_PragmaInterruptible {
        VM_Processor processor;
        writeString("\n-- Processors --\n");
        for (int i = 1; i <= numProcessors; ++i) {
            processor = processors[i];
            processor.dumpProcessorState();
        }
        if (VM.BuildWithNativeDaemonProcessor) {
            writeString("\n-- NativeDaemonProcessor --\n");
            processors[nativeDPndx].dumpProcessorState();
        }
        writeString("\n-- Native Processors --\n");
        for (int i = 1; i <= VM_Processor.numberNativeProcessors; i++) {
            processor = VM_Processor.nativeProcessors[i];
            if (processor == null) {
                writeString(" NULL processor for nativeProcessors entry = ");
                writeDecimal(i);
                continue;
            }
            processor.dumpProcessorState();
        }
        writeString("\n-- System Queues -- \n");
        wakeupQueue.dump();
        writeString(" wakeupQueue:");
        wakeupQueue.dump();
        writeString(" debuggerQueue:");
        debuggerQueue.dump();
        writeString(" deadVPQueue:");
        deadVPQueue.dump();
        writeString(" collectorQueue:");
        collectorQueue.dump();
        writeString(" finalizerQueue:");
        finalizerQueue.dump();
        writeString(" nativeProcessorQueue:");
        nativeProcessorQueue.dump();
        writeString("\n-- Threads --\n");
        for (int i = 1; i < threads.length; ++i) if (threads[i] != null) threads[i].dump();
        writeString("\n");
        writeString("\n-- Locks available --\n");
        for (int i = PRIMORDIAL_PROCESSOR_ID; i <= numProcessors; ++i) {
            processor = processors[i];
            int unallocated = processor.lastLockIndex - processor.nextLockIndex + 1;
            writeString(" processor ");
            writeDecimal(i);
            writeString(": ");
            writeDecimal(processor.locksAllocated);
            writeString(" locks allocated, ");
            writeDecimal(processor.locksFreed);
            writeString(" locks freed, ");
            writeDecimal(processor.freeLocks);
            writeString(" free looks, ");
            writeDecimal(unallocated);
            writeString(" unallocated slots\n");
        }
        writeString("\n");
        writeString("\n-- Locks in use --\n");
        for (int i = 0; i < locks.length; ++i) if (locks[i] != null) locks[i].dump();
        writeString("\n");
    }

    static int outputLock;

    static final void lockOutput() {
        if (VM.BuildForSingleVirtualProcessor) return;
        VM_Processor.getCurrentProcessor().disableThreadSwitching();
        do {
            int processorId = VM_Magic.prepareInt(VM_Magic.getJTOC(), VM_Entrypoints.outputLockField.getOffset());
            if (processorId == 0 && VM_Magic.attemptInt(VM_Magic.getJTOC(), VM_Entrypoints.outputLockField.getOffset(), 0, VM_Processor.getCurrentProcessorId())) {
                break;
            }
        } while (true);
        VM_Magic.isync();
    }

    static final void unlockOutput() {
        if (VM.BuildForSingleVirtualProcessor) return;
        VM_Magic.sync();
        if (true) outputLock = 0; else {
            do {
                int processorId = VM_Magic.prepareInt(VM_Magic.getJTOC(), VM_Entrypoints.outputLockField.getOffset());
                if (VM.VerifyAssertions && processorId != VM_Processor.getCurrentProcessorId()) VM.sysExit(664);
                if (VM_Magic.attemptInt(VM_Magic.getJTOC(), VM_Entrypoints.outputLockField.getOffset(), processorId, 0)) {
                    break;
                }
            } while (true);
        }
        VM_Processor.getCurrentProcessor().enableThreadSwitching();
    }

    public static void writeAtom(VM_Atom value) {
        value.sysWrite();
    }

    public static void writeString(String s) {
        VM.sysWrite(s);
    }

    public static void writeHex(int n) {
        for (int i = 0; i < 8; i++) {
            int v = n >>> 28;
            if (v < 10) {
                VM.sysWrite((char) (v + '0'));
            } else {
                VM.sysWrite((char) (v + 'a' - 10));
            }
            n <<= 4;
        }
    }

    public static void writeHex(long n) {
        int index = 18;
        while (--index > 1) {
            int digit = (int) (n & 0x000000000000000f);
            if (digit <= 9) VM.sysWrite((char) ('0' + digit)); else VM.sysWrite((char) ('a' + digit - 10));
            n >>= 4;
        }
    }

    public static void writeHex(VM_Address n) {
        writeHex(n.toLong());
        writeHex(n.toInt());
    }

    static void writeDecimal(int n) {
        if (n < 0) {
            VM.sysWrite('-');
            writeDecimalDigits(-n);
        } else if (n == 0) {
            VM.sysWrite('0');
        } else {
            writeDecimalDigits(n);
        }
    }

    private static void writeDecimalDigits(int n) {
        if (n == 0) return;
        writeDecimalDigits(n / 10);
        VM.sysWrite((char) ('0' + (n % 10)));
    }

    /**
   * how may processors to be synchronized for code patching, the last
   * one (0) will notify the blocked thread.
   */
    public static int toSyncProcessors;

    /**
   * synchronize object 
   */
    public static Object syncObj = null;
}
