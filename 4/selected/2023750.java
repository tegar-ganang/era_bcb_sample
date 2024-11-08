package jpcsp.HLE.kernel.types;

import static jpcsp.Allegrex.Common._gp;
import static jpcsp.Allegrex.Common._k0;
import static jpcsp.Allegrex.Common._ra;
import static jpcsp.Allegrex.Common._sp;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_THREAD_ALREADY_DORMANT;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Allegrex.CpuState;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.modules.SysMemUserForUser;
import jpcsp.HLE.modules150.SysMemUserForUser.SysMemInfo;
import jpcsp.HLE.modules150.ThreadManForUser.Callback;

public class SceKernelThreadInfo extends pspAbstractMemoryMappedStructureVariableLength implements Comparator<SceKernelThreadInfo> {

    public static final int PSP_MODULE_USER = 0;

    public static final int PSP_MODULE_NO_STOP = 0x00000001;

    public static final int PSP_MODULE_SINGLE_LOAD = 0x00000002;

    public static final int PSP_MODULE_SINGLE_START = 0x00000004;

    public static final int PSP_MODULE_POPS = 0x00000200;

    public static final int PSP_MODULE_DEMO = 0x00000200;

    public static final int PSP_MODULE_GAMESHARING = 0x00000400;

    public static final int PSP_MODULE_VSH = 0x00000800;

    public static final int PSP_MODULE_KERNEL = 0x00001000;

    public static final int PSP_MODULE_USE_MEMLMD_LIB = 0x00002000;

    public static final int PSP_MODULE_USE_SEMAPHORE_LIB = 0x00004000;

    public static final int PSP_THREAD_ATTR_USER = 0x80000000;

    public static final int PSP_THREAD_ATTR_USBWLAN = 0xa0000000;

    public static final int PSP_THREAD_ATTR_VSH = 0xc0000000;

    public static final int PSP_THREAD_ATTR_KERNEL = 0x00001000;

    public static final int PSP_THREAD_ATTR_VFPU = 0x00004000;

    public static final int PSP_THREAD_ATTR_SCRATCH_SRAM = 0x00008000;

    public static final int PSP_THREAD_ATTR_NO_FILLSTACK = 0x00100000;

    public static final int PSP_THREAD_ATTR_CLEAR_STACK = 0x00200000;

    public static final int PSP_THREAD_RUNNING = 0x00000001;

    public static final int PSP_THREAD_READY = 0x00000002;

    public static final int PSP_THREAD_WAITING = 0x00000004;

    public static final int PSP_THREAD_SUSPEND = 0x00000008;

    public static final int PSP_THREAD_WAITING_SUSPEND = PSP_THREAD_WAITING | PSP_THREAD_SUSPEND;

    public static final int PSP_THREAD_STOPPED = 0x00000010;

    public static final int PSP_THREAD_KILLED = 0x00000020;

    public static final int PSP_WAIT_NONE = 0x00;

    public static final int PSP_WAIT_SLEEP = 0x01;

    public static final int PSP_WAIT_DELAY = 0x02;

    public static final int PSP_WAIT_SEMA = 0x03;

    public static final int PSP_WAIT_EVENTFLAG = 0x04;

    public static final int PSP_WAIT_MBX = 0x05;

    public static final int PSP_WAIT_VPL = 0x06;

    public static final int PSP_WAIT_FPL = 0x07;

    public static final int PSP_WAIT_MSGPIPE = 0x08;

    public static final int PSP_WAIT_THREAD_END = 0x09;

    public static final int PSP_WAIT_EVENTHANDLER = 0x0a;

    public static final int PSP_WAIT_CALLBACK_DELETE = 0x0b;

    public static final int PSP_WAIT_MUTEX = 0x0c;

    public static final int PSP_WAIT_LWMUTEX = 0x0d;

    public static final int JPCSP_FIRST_INTERNAL_WAIT_TYPE = 0x100;

    public static final int JPCSP_WAIT_IO = JPCSP_FIRST_INTERNAL_WAIT_TYPE;

    public static final int JPCSP_WAIT_UMD = JPCSP_WAIT_IO + 1;

    public static final int JPCSP_WAIT_BLOCKED = JPCSP_WAIT_UMD + 1;

    public final String name;

    public int attr;

    public int status;

    public final int entry_addr;

    private int stackAddr;

    public int stackSize;

    public int gpReg_addr;

    public final int initPriority;

    public int currentPriority;

    public int waitType;

    public int waitId;

    public int wakeupCount;

    public int exitStatus;

    public long runClocks;

    public int intrPreemptCount;

    public int threadPreemptCount;

    public int releaseCount;

    public int notifyCallback;

    public int errno;

    private SysMemInfo stackSysMemInfo;

    public final int uid;

    public int moduleid;

    public CpuState cpuContext;

    public boolean doDelete;

    public IAction doDeleteAction;

    public boolean doCallbacks;

    public final ThreadWaitInfo wait;

    public int displayLastWaitVcount;

    public long javaThreadId = -1;

    public long javaThreadCpuTimeNanos = -1;

    public static final int THREAD_CALLBACK_UMD = 0;

    public static final int THREAD_CALLBACK_IO = 1;

    public static final int THREAD_CALLBACK_MEMORYSTICK = 2;

    public static final int THREAD_CALLBACK_MEMORYSTICK_FAT = 3;

    public static final int THREAD_CALLBACK_POWER = 4;

    public static final int THREAD_CALLBACK_EXIT = 5;

    public static final int THREAD_CALLBACK_USER_DEFINED = 6;

    public static final int THREAD_CALLBACK_SIZE = 7;

    private RegisteredCallbacks[] registeredCallbacks;

    public Queue<Callback> pendingCallbacks = new LinkedList<Callback>();

    private SysMemInfo extendedStackSysMemInfo;

    public static class RegisteredCallbacks {

        private int type;

        private List<SceKernelCallbackInfo> callbacks;

        private List<SceKernelCallbackInfo> readyCallbacks;

        private int maxNumberOfCallbacks = 32;

        private boolean registerOnlyLastCallback = false;

        public RegisteredCallbacks(int type) {
            this.type = type;
            callbacks = new LinkedList<SceKernelCallbackInfo>();
            readyCallbacks = new LinkedList<SceKernelCallbackInfo>();
        }

        public boolean hasCallbacks() {
            return !callbacks.isEmpty();
        }

        public SceKernelCallbackInfo getCallbackByUid(int cbid) {
            for (SceKernelCallbackInfo callback : callbacks) {
                if (callback.uid == cbid) {
                    return callback;
                }
            }
            return null;
        }

        public boolean hasCallback(int cbid) {
            return getCallbackByUid(cbid) != null;
        }

        public boolean hasCallback(SceKernelCallbackInfo callback) {
            return callbacks.contains(callback);
        }

        public boolean addCallback(SceKernelCallbackInfo callback) {
            if (hasCallback(callback)) {
                return true;
            }
            if (getNumberOfCallbacks() >= maxNumberOfCallbacks) {
                return false;
            }
            if (registerOnlyLastCallback) {
                callbacks.clear();
            }
            callbacks.add(callback);
            return true;
        }

        public void setCallbackReady(SceKernelCallbackInfo callback) {
            if (hasCallback(callback) && !isCallbackReady(callback)) {
                readyCallbacks.add(callback);
            }
        }

        public boolean isCallbackReady(SceKernelCallbackInfo callback) {
            return readyCallbacks.contains(callback);
        }

        public SceKernelCallbackInfo removeCallback(SceKernelCallbackInfo callback) {
            if (!callbacks.remove(callback)) {
                return null;
            }
            readyCallbacks.remove(callback);
            return callback;
        }

        public SceKernelCallbackInfo getNextReadyCallback() {
            if (readyCallbacks.isEmpty()) {
                return null;
            }
            return readyCallbacks.remove(0);
        }

        public int getNumberOfCallbacks() {
            return callbacks.size();
        }

        public SceKernelCallbackInfo getCallbackByIndex(int index) {
            return callbacks.get(index);
        }

        public void setRegisterOnlyLastCallback() {
            registerOnlyLastCallback = true;
        }

        @Override
        public String toString() {
            return String.format("RegisteredCallbacks[type %d, count %d, ready %d]", type, callbacks.size(), readyCallbacks.size());
        }
    }

    public SceKernelThreadInfo(String name, int entry_addr, int initPriority, int stackSize, int attr) {
        if (stackSize < 512) {
            stackSize = 512;
        } else {
            stackSize = (stackSize + 0xFF) & ~0xFF;
        }
        this.name = name;
        this.entry_addr = entry_addr;
        this.initPriority = initPriority;
        this.stackSize = stackSize;
        this.attr = attr;
        uid = SceUidManager.getNewUid("ThreadMan-thread");
        stackSysMemInfo = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.USER_PARTITION_ID, String.format("ThreadMan-Stack-0x%x-%s", uid, name), SysMemUserForUser.PSP_SMEM_High, stackSize, 0);
        if (stackSysMemInfo == null) {
            stackAddr = 0;
        } else {
            stackAddr = stackSysMemInfo.addr;
        }
        gpReg_addr = Emulator.getProcessor().cpu.gpr[_gp];
        cpuContext = new CpuState(Emulator.getProcessor().cpu);
        wait = new ThreadWaitInfo();
        reset();
    }

    public void reset() {
        status = PSP_THREAD_STOPPED;
        int k0 = stackAddr + stackSize - 0x100;
        Memory mem = Memory.getInstance();
        if (stackAddr != 0 && stackSize > 0) {
            if ((attr & PSP_THREAD_ATTR_NO_FILLSTACK) != PSP_THREAD_ATTR_NO_FILLSTACK) {
                mem.memset(stackAddr, (byte) 0xFF, stackSize);
            }
            mem.memset(k0, (byte) 0x0, 0x100);
            mem.write32(k0 + 0xc0, stackAddr);
            mem.write32(k0 + 0xca, uid);
            mem.write32(k0 + 0xf8, 0xffffffff);
            mem.write32(k0 + 0xfc, 0xffffffff);
            mem.write32(stackAddr, uid);
        }
        currentPriority = initPriority;
        waitType = PSP_WAIT_NONE;
        waitId = 0;
        wakeupCount = 0;
        exitStatus = ERROR_KERNEL_THREAD_ALREADY_DORMANT;
        runClocks = 0;
        intrPreemptCount = 0;
        threadPreemptCount = 0;
        releaseCount = 0;
        notifyCallback = 0;
        cpuContext.pc = entry_addr;
        cpuContext.npc = entry_addr;
        cpuContext.gpr[_sp] = stackAddr + stackSize - 512;
        cpuContext.gpr[_k0] = k0;
        cpuContext.gpr[_ra] = jpcsp.HLE.modules150.ThreadManForUser.THREAD_EXIT_HANDLER_ADDRESS;
        doDelete = false;
        doCallbacks = false;
        registeredCallbacks = new RegisteredCallbacks[THREAD_CALLBACK_SIZE];
        for (int i = 0; i < registeredCallbacks.length; i++) {
            registeredCallbacks[i] = new RegisteredCallbacks(i);
        }
        registeredCallbacks[THREAD_CALLBACK_UMD].setRegisterOnlyLastCallback();
    }

    public void saveContext() {
        cpuContext = Emulator.getProcessor().cpu;
    }

    public void restoreContext() {
        cpuContext.pc = cpuContext.npc;
        Emulator.getProcessor().setCpu(cpuContext);
        RuntimeContext.update();
    }

    /** For use in the scheduler */
    @Override
    public int compare(SceKernelThreadInfo o1, SceKernelThreadInfo o2) {
        return o1.currentPriority - o2.currentPriority;
    }

    private int getPSPWaitType() {
        if (waitType >= 0x100) {
            return PSP_WAIT_EVENTFLAG;
        }
        return waitType;
    }

    @Override
    protected void write() {
        super.write();
        writeStringNZ(32, name);
        write32(attr);
        write32(status);
        write32(entry_addr);
        write32(stackAddr);
        write32(stackSize);
        write32(gpReg_addr);
        write32(initPriority);
        write32(currentPriority);
        write32(getPSPWaitType());
        write32(waitId);
        write32(wakeupCount);
        write32(exitStatus);
        write64(runClocks);
        write32(intrPreemptCount);
        write32(threadPreemptCount);
        write32(releaseCount);
    }

    public void writeRunStatus(Memory mem, int address) {
        start(mem, address);
        super.write();
        write32(status);
        write32(currentPriority);
        write32(waitType);
        write32(waitId);
        write32(wakeupCount);
        write64(runClocks);
        write32(intrPreemptCount);
        write32(threadPreemptCount);
        write32(releaseCount);
    }

    public void setSystemStack(int stackAddr, int stackSize) {
        freeStack();
        this.stackAddr = stackAddr;
        this.stackSize = stackSize;
    }

    public void freeStack() {
        if (stackSysMemInfo != null) {
            Modules.SysMemUserForUserModule.free(stackSysMemInfo);
            stackSysMemInfo = null;
            stackAddr = 0;
        }
        freeExtendedStack();
    }

    public void freeExtendedStack() {
        if (extendedStackSysMemInfo != null) {
            Modules.SysMemUserForUserModule.free(extendedStackSysMemInfo);
            extendedStackSysMemInfo = null;
        }
    }

    public int extendStack(int size) {
        extendedStackSysMemInfo = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.USER_PARTITION_ID, String.format("ThreadMan-ExtendedStack-0x%x-%s", uid, name), SysMemUserForUser.PSP_SMEM_High, size, 0);
        return extendedStackSysMemInfo.addr;
    }

    public int getStackAddr() {
        if (extendedStackSysMemInfo != null) {
            return extendedStackSysMemInfo.addr;
        }
        return stackAddr;
    }

    public static String getStatusName(int status) {
        StringBuilder s = new StringBuilder();
        if ((status & PSP_THREAD_RUNNING) == PSP_THREAD_RUNNING) {
            s.append(" | PSP_THREAD_RUNNING");
        }
        if ((status & PSP_THREAD_READY) == PSP_THREAD_READY) {
            s.append(" | PSP_THREAD_READY");
        }
        if ((status & PSP_THREAD_WAITING) == PSP_THREAD_WAITING) {
            s.append(" | PSP_THREAD_WAITING");
        }
        if ((status & PSP_THREAD_SUSPEND) == PSP_THREAD_SUSPEND) {
            s.append(" | PSP_THREAD_SUSPEND");
        }
        if ((status & PSP_THREAD_STOPPED) == PSP_THREAD_STOPPED) {
            s.append(" | PSP_THREAD_STOPPED");
        }
        if ((status & PSP_THREAD_KILLED) == PSP_THREAD_KILLED) {
            s.append(" | PSP_THREAD_KILLED");
        }
        if (s.length() > 0) {
            s.delete(0, 3);
        } else {
            s.append("UNKNOWN");
        }
        return s.toString();
    }

    public String getStatusName() {
        return getStatusName(status);
    }

    public static String getWaitName(int waitType, ThreadWaitInfo wait, int status) {
        StringBuilder s = new StringBuilder();
        switch(waitType) {
            case PSP_WAIT_NONE:
                s.append(String.format("None"));
                break;
            case PSP_WAIT_SLEEP:
                s.append(String.format("Sleep"));
                break;
            case PSP_WAIT_THREAD_END:
                s.append(String.format("ThreadEnd (0x%04X)", wait.ThreadEnd_id));
                break;
            case PSP_WAIT_EVENTFLAG:
                s.append(String.format("EventFlag (0x%04X)", wait.EventFlag_id));
                break;
            case PSP_WAIT_SEMA:
                s.append(String.format("Semaphore (0x%04X)", wait.Semaphore_id));
                break;
            case PSP_WAIT_MUTEX:
                s.append(String.format("Mutex (0x%04X)", wait.Mutex_id));
                break;
            case PSP_WAIT_LWMUTEX:
                s.append(String.format("LwMutex (0x%04X)", wait.LwMutex_id));
                break;
            case PSP_WAIT_MBX:
                s.append(String.format("Mbx (0x%04X)", wait.Mbx_id));
                break;
            case PSP_WAIT_VPL:
                s.append(String.format("Vpl (0x%04X)", wait.Vpl_id));
                break;
            case PSP_WAIT_FPL:
                s.append(String.format("Fpl (0x%04X)", wait.Fpl_id));
                break;
            case PSP_WAIT_MSGPIPE:
                s.append(String.format("MsgPipe (0x%04X)", wait.MsgPipe_id));
                break;
            case PSP_WAIT_EVENTHANDLER:
                s.append(String.format("EventHandler"));
                break;
            case PSP_WAIT_CALLBACK_DELETE:
                s.append(String.format("CallBackDelete"));
                break;
            case JPCSP_WAIT_IO:
                s.append(String.format("Io (0x%04X)", wait.Io_id));
                break;
            case JPCSP_WAIT_UMD:
                s.append(String.format("Umd (0x%02X)", wait.wantedUmdStat));
                break;
            case JPCSP_WAIT_BLOCKED:
                s.append(String.format("Blocked"));
                break;
            default:
                s.append(String.format("Unknown waitType=%d", waitType));
                break;
        }
        if ((status & PSP_THREAD_WAITING) != 0) {
            if (wait.forever) {
                s.append(" (forever)");
            } else {
                int restDelay = (int) (wait.microTimeTimeout - Emulator.getClock().microTime());
                if (restDelay < 0) {
                    restDelay = 0;
                }
                s.append(String.format(" (delay %d us, rest %d us)", wait.micros, restDelay));
            }
        }
        return s.toString();
    }

    public String getWaitName() {
        return getWaitName(waitType, wait, status);
    }

    public boolean isSuspended() {
        return (status & PSP_THREAD_SUSPEND) != 0;
    }

    public boolean isWaiting() {
        return (status & PSP_THREAD_WAITING) != 0;
    }

    public boolean isWaitingForType(int waitType) {
        if (!isWaiting() || isSuspended()) {
            return false;
        }
        return this.waitType == waitType;
    }

    public boolean isRunning() {
        return (status & PSP_THREAD_RUNNING) != 0;
    }

    public boolean isReady() {
        return (status & PSP_THREAD_READY) != 0;
    }

    public boolean isStopped() {
        return (status & PSP_THREAD_STOPPED) != 0;
    }

    public static boolean isKernelMode(int attr) {
        return (attr & PSP_THREAD_ATTR_KERNEL) != 0;
    }

    public static boolean isUserMode(int attr) {
        return (attr & PSP_THREAD_ATTR_USER) != 0;
    }

    public boolean isKernelMode() {
        return isKernelMode(attr);
    }

    public boolean isUserMode() {
        return isUserMode(attr);
    }

    public RegisteredCallbacks getRegisteredCallbacks(int type) {
        return registeredCallbacks[type];
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(name);
        s.append("(");
        s.append("Status " + getStatusName());
        s.append(", Wait " + getWaitName());
        s.append(", doCallbacks " + doCallbacks);
        s.append(")");
        return s.toString();
    }
}
