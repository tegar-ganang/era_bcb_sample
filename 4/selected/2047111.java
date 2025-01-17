package jpcsp.HLE.kernel.managers;

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_ILLEGAL_ATTR;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_ILLEGAL_MEMSIZE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_ILLEGAL_MEMBLOCK;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_NOT_FOUND_FPOOL;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_NO_MEMORY;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_WAIT_TIMEOUT;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_WAIT_DELETE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_WAIT_STATUS_RELEASED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_WAIT_CANCELLED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_WAIT_CAN_NOT_WAIT;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_READY;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_WAIT_FPL;
import static jpcsp.HLE.modules150.SysMemUserForUser.PSP_SMEM_Low;
import static jpcsp.HLE.modules150.SysMemUserForUser.PSP_SMEM_High;
import java.util.HashMap;
import java.util.Iterator;
import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.IWaitStateChecker;
import jpcsp.HLE.kernel.types.SceKernelFplInfo;
import jpcsp.HLE.kernel.types.SceKernelFplOptParam;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.ThreadWaitInfo;
import jpcsp.HLE.modules.ThreadManForUser;
import jpcsp.util.Utilities;
import org.apache.log4j.Logger;

public class FplManager {

    protected static Logger log = Modules.getLogger("ThreadManForUser");

    private HashMap<Integer, SceKernelFplInfo> fplMap;

    private FplWaitStateChecker fplWaitStateChecker;

    private static final int PSP_FPL_ATTR_FIFO = 0;

    private static final int PSP_FPL_ATTR_PRIORITY = 0x100;

    private static final int PSP_FPL_ATTR_MASK = 0x41FF;

    private static final int PSP_FPL_ATTR_ADDR_HIGH = 0x4000;

    public void reset() {
        fplMap = new HashMap<Integer, SceKernelFplInfo>();
        fplWaitStateChecker = new FplWaitStateChecker();
    }

    private boolean removeWaitingThread(SceKernelThreadInfo thread) {
        SceKernelFplInfo fpl = fplMap.get(thread.wait.Fpl_id);
        if (fpl != null) {
            fpl.numWaitThreads--;
            if (fpl.numWaitThreads < 0) {
                log.warn("removing waiting thread " + Integer.toHexString(thread.uid) + ", fpl " + Integer.toHexString(fpl.uid) + " numWaitThreads underflowed");
                fpl.numWaitThreads = 0;
            }
            return true;
        }
        return false;
    }

    public void onThreadWaitTimeout(SceKernelThreadInfo thread) {
        if (removeWaitingThread(thread)) {
            thread.cpuContext.gpr[2] = ERROR_KERNEL_WAIT_TIMEOUT;
        } else {
            log.warn("FPL deleted while we were waiting for it! (timeout expired)");
            thread.cpuContext.gpr[2] = ERROR_KERNEL_WAIT_DELETE;
        }
    }

    public void onThreadWaitReleased(SceKernelThreadInfo thread) {
        if (removeWaitingThread(thread)) {
            thread.cpuContext.gpr[2] = ERROR_KERNEL_WAIT_STATUS_RELEASED;
        } else {
            log.warn("EventFlag deleted while we were waiting for it!");
            thread.cpuContext.gpr[2] = ERROR_KERNEL_WAIT_DELETE;
        }
    }

    public void onThreadDeleted(SceKernelThreadInfo thread) {
        if (thread.isWaitingForType(PSP_WAIT_FPL)) {
            removeWaitingThread(thread);
        }
    }

    private void onFplDeletedCancelled(int fid, int result) {
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        boolean reschedule = false;
        for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext(); ) {
            SceKernelThreadInfo thread = it.next();
            if (thread.isWaitingForType(PSP_WAIT_FPL) && thread.wait.Fpl_id == fid) {
                thread.cpuContext.gpr[2] = result;
                threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);
                reschedule = true;
            }
        }
        if (reschedule) {
            threadMan.hleRescheduleCurrentThread();
        }
    }

    private void onFplDeleted(int fid) {
        onFplDeletedCancelled(fid, ERROR_KERNEL_WAIT_DELETE);
    }

    private void onFplCancelled(int fid) {
        onFplDeletedCancelled(fid, ERROR_KERNEL_WAIT_CANCELLED);
    }

    private void onFplFree(SceKernelFplInfo info) {
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        boolean reschedule = false;
        if ((info.attr & PSP_FPL_ATTR_PRIORITY) == PSP_FPL_ATTR_FIFO) {
            for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext(); ) {
                SceKernelThreadInfo thread = it.next();
                if (thread.isWaitingForType(PSP_WAIT_FPL) && thread.wait.Fpl_id == info.uid) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("onFplFree waking thread %s", thread.toString()));
                    }
                    info.numWaitThreads--;
                    thread.cpuContext.gpr[2] = 0;
                    threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);
                    reschedule = true;
                }
            }
        } else if ((info.attr & PSP_FPL_ATTR_PRIORITY) == PSP_FPL_ATTR_PRIORITY) {
            for (Iterator<SceKernelThreadInfo> it = threadMan.iteratorByPriority(); it.hasNext(); ) {
                SceKernelThreadInfo thread = it.next();
                if (thread.isWaitingForType(PSP_WAIT_FPL) && thread.wait.Fpl_id == info.uid) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("onFplFree waking thread %s", thread.toString()));
                    }
                    info.numWaitThreads--;
                    thread.cpuContext.gpr[2] = 0;
                    threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);
                    reschedule = true;
                }
            }
        }
        if (reschedule) {
            threadMan.hleRescheduleCurrentThread();
        }
    }

    /** @return the address of the allocated block or 0 if failed. */
    private int tryAllocateFpl(SceKernelFplInfo info) {
        int block;
        int addr = 0;
        if (info.freeBlocks == 0 || (block = info.findFreeBlock()) == -1) {
            log.warn("tryAllocateFpl no free blocks (numBlocks=" + info.numBlocks + ")");
            return 0;
        }
        addr = info.allocateBlock(block);
        return addr;
    }

    public void sceKernelCreateFpl(int name_addr, int partitionid, int attr, int blocksize, int blocks, int opt_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;
        String name = Utilities.readStringZ(name_addr);
        if (log.isDebugEnabled()) {
            log.debug("sceKernelCreateFpl(name='" + name + "',partition=" + partitionid + ",attr=0x" + Integer.toHexString(attr) + ",blocksize=0x" + Integer.toHexString(blocksize) + ",blocks=" + blocks + ",opt=0x" + Integer.toHexString(opt_addr) + ")");
        }
        int memType = PSP_SMEM_Low;
        if ((attr & PSP_FPL_ATTR_ADDR_HIGH) == PSP_FPL_ATTR_ADDR_HIGH) {
            memType = PSP_SMEM_High;
        }
        int memAlign = 4;
        if (Memory.isAddressGood(opt_addr)) {
            int optsize = mem.read32(opt_addr);
            if ((optsize >= 4) && (optsize <= 8)) {
                SceKernelFplOptParam optParams = new SceKernelFplOptParam();
                optParams.read(mem, opt_addr);
                if (optParams.align > 0) {
                    memAlign = optParams.align;
                }
                if (log.isDebugEnabled()) {
                    log.debug("sceKernelCreateFpl options: struct size=" + optParams.sizeof() + ", alignment=0x" + Integer.toHexString(optParams.align));
                }
            } else {
                log.warn("sceKernelCreateFpl option at 0x" + Integer.toHexString(opt_addr) + " (size=" + optsize + ")");
            }
        }
        if ((attr & ~PSP_FPL_ATTR_MASK) != 0) {
            log.warn("sceKernelCreateFpl bad attr value 0x" + Integer.toHexString(attr));
            cpu.gpr[2] = ERROR_KERNEL_ILLEGAL_ATTR;
        } else if (blocksize == 0) {
            log.warn("sceKernelCreateFpl bad blocksize, cannot be 0");
            cpu.gpr[2] = ERROR_KERNEL_ILLEGAL_MEMSIZE;
        } else {
            SceKernelFplInfo info = SceKernelFplInfo.tryCreateFpl(name, partitionid, attr, blocksize, blocks, memType, memAlign);
            if (info != null) {
                if (log.isDebugEnabled()) {
                    log.debug("sceKernelCreateFpl '" + name + "' assigned uid " + Integer.toHexString(info.uid));
                }
                fplMap.put(info.uid, info);
                cpu.gpr[2] = info.uid;
            } else {
                cpu.gpr[2] = ERROR_KERNEL_NO_MEMORY;
            }
        }
    }

    public void sceKernelDeleteFpl(int uid) {
        CpuState cpu = Emulator.getProcessor().cpu;
        String msg = "sceKernelDeleteFpl(uid=0x" + Integer.toHexString(uid) + ")";
        SceKernelFplInfo info = fplMap.remove(uid);
        if (info == null) {
            log.warn(msg + " unknown uid");
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_FPOOL;
        } else {
            msg += " '" + info.name + "'";
            if (log.isDebugEnabled()) {
                log.debug(msg);
            }
            if (info.freeBlocks < info.numBlocks) {
                log.warn(msg + " " + (info.numBlocks - info.freeBlocks) + " unfreed blocks, continuing");
            }
            info.deleteSysMemInfo();
            cpu.gpr[2] = 0;
            onFplDeleted(uid);
        }
    }

    private void hleKernelAllocateFpl(int uid, int data_addr, int timeout_addr, boolean wait, boolean doCallbacks) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Memory.getInstance();
        if (log.isDebugEnabled()) {
            log.debug("hleKernelAllocateFpl uid=0x" + Integer.toHexString(uid) + " data_addr=0x" + Integer.toHexString(data_addr) + " timeout_addr=0x" + Integer.toHexString(timeout_addr) + " callbacks=" + doCallbacks);
        }
        SceUidManager.checkUidPurpose(uid, "ThreadMan-Fpl", true);
        SceKernelFplInfo fpl = fplMap.get(uid);
        if (fpl == null) {
            log.warn("hleKernelAllocateFpl unknown uid=0x" + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_FPOOL;
        } else {
            int addr = tryAllocateFpl(fpl);
            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
            if (addr == 0) {
                if (log.isDebugEnabled()) {
                    log.debug("hleKernelAllocateFpl - '" + fpl.name + "' fast check failed");
                }
                if (wait) {
                    fpl.numWaitThreads++;
                    SceKernelThreadInfo currentThread = threadMan.getCurrentThread();
                    currentThread.wait.Fpl_id = uid;
                    currentThread.wait.Fpl_dataAddr = data_addr;
                    threadMan.hleKernelThreadEnterWaitState(PSP_WAIT_FPL, uid, fplWaitStateChecker, timeout_addr, doCallbacks);
                } else {
                    cpu.gpr[2] = ERROR_KERNEL_WAIT_CAN_NOT_WAIT;
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("hleKernelAllocateFpl - '" + fpl.name + "' fast check succeeded");
                }
                mem.write32(data_addr, addr);
                cpu.gpr[2] = 0;
            }
        }
    }

    public void sceKernelAllocateFpl(int uid, int data_addr, int timeout_addr) {
        if (log.isDebugEnabled()) {
            log.debug("sceKernelAllocateFpl redirecting to hleKernelAllocateFpl(callbacks=false)");
        }
        hleKernelAllocateFpl(uid, data_addr, timeout_addr, true, false);
    }

    public void sceKernelAllocateFplCB(int uid, int data_addr, int timeout_addr) {
        if (log.isDebugEnabled()) {
            log.debug("sceKernelAllocateFplCB redirecting to hleKernelAllocateFpl(callbacks=true)");
        }
        hleKernelAllocateFpl(uid, data_addr, timeout_addr, true, true);
    }

    public void sceKernelTryAllocateFpl(int uid, int data_addr) {
        if (log.isDebugEnabled()) {
            log.debug("sceKernelTryAllocateFpl redirecting to hleKernelAllocateFpl");
        }
        hleKernelAllocateFpl(uid, data_addr, 0, false, false);
    }

    public void sceKernelFreeFpl(int uid, int data_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        if (log.isDebugEnabled()) {
            log.debug("sceKernelFreeFpl(uid=0x" + Integer.toHexString(uid) + ",data=0x" + Integer.toHexString(data_addr) + ")");
        }
        SceKernelFplInfo info = fplMap.get(uid);
        if (info == null) {
            log.warn("sceKernelFreeFpl unknown uid");
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_FPOOL;
        } else {
            int block = info.findBlockByAddress(data_addr);
            if (block == -1) {
                log.warn("sceKernelFreeFpl unknown block address=0x" + Integer.toHexString(data_addr));
                cpu.gpr[2] = ERROR_KERNEL_ILLEGAL_MEMBLOCK;
            } else {
                info.freeBlock(block);
                cpu.gpr[2] = 0;
                onFplFree(info);
            }
        }
    }

    public void sceKernelCancelFpl(int uid, int numWaitThreadAddr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        if (log.isDebugEnabled()) {
            log.debug("sceKernelCancelFpl(uid=0x" + Integer.toHexString(uid) + ",numWaitThreadAddr=0x" + Integer.toHexString(numWaitThreadAddr) + ")");
        }
        SceKernelFplInfo info = fplMap.get(uid);
        if (info == null) {
            log.warn("sceKernelCancelFpl unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_FPOOL;
        } else {
            Memory mem = Memory.getInstance();
            if (Memory.isAddressGood(numWaitThreadAddr)) {
                mem.write32(numWaitThreadAddr, info.numWaitThreads);
            }
            cpu.gpr[2] = 0;
            onFplCancelled(uid);
        }
    }

    public void sceKernelReferFplStatus(int uid, int info_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;
        if (log.isDebugEnabled()) {
            log.debug("sceKernelReferFplStatus(uid=0x" + Integer.toHexString(uid) + ",info_addr=0x" + Integer.toHexString(info_addr) + ")");
        }
        SceKernelFplInfo info = fplMap.get(uid);
        if (info == null) {
            log.warn("sceKernelReferFplStatus unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_FPOOL;
        } else {
            info.write(mem, info_addr);
            cpu.gpr[2] = 0;
        }
    }

    private class FplWaitStateChecker implements IWaitStateChecker {

        @Override
        public boolean continueWaitState(SceKernelThreadInfo thread, ThreadWaitInfo wait) {
            SceKernelFplInfo fpl = fplMap.get(wait.Fpl_id);
            if (fpl == null) {
                thread.cpuContext.gpr[2] = ERROR_KERNEL_NOT_FOUND_FPOOL;
                return false;
            }
            if (tryAllocateFpl(fpl) != 0) {
                fpl.numWaitThreads--;
                thread.cpuContext.gpr[2] = 0;
                return false;
            }
            return true;
        }
    }

    public static final FplManager singleton = new FplManager();

    private FplManager() {
    }
}
