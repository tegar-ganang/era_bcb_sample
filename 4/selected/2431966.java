package jpcsp.HLE.kernel.managers;

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_EVENT_FLAG_ILLEGAL_WAIT_PATTERN;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_EVENT_FLAG_NO_MULTI_PERM;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_EVENT_FLAG_POLL_FAILED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_ILLEGAL_MODE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_NOT_FOUND_EVENT_FLAG;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_WAIT_CANCELLED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_WAIT_CAN_NOT_WAIT;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_WAIT_DELETE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_WAIT_STATUS_RELEASED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_WAIT_TIMEOUT;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_READY;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_WAIT_EVENTFLAG;
import static jpcsp.util.Utilities.readStringZ;
import java.util.HashMap;
import java.util.Iterator;
import jpcsp.Memory;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.IWaitStateChecker;
import jpcsp.HLE.kernel.types.SceKernelEventFlagInfo;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.ThreadWaitInfo;
import jpcsp.HLE.modules.ThreadManForUser;
import org.apache.log4j.Logger;

public class EventFlagManager {

    protected static Logger log = Modules.getLogger("ThreadManForUser");

    private static HashMap<Integer, SceKernelEventFlagInfo> eventMap;

    private EventFlagWaitStateChecker eventFlagWaitStateChecker;

    protected static final int PSP_EVENT_WAITSINGLE = 0;

    protected static final int PSP_EVENT_WAITMULTIPLE = 0x200;

    protected static final int PSP_EVENT_WAITANDOR_MASK = 0x01;

    protected static final int PSP_EVENT_WAITAND = 0x00;

    protected static final int PSP_EVENT_WAITOR = 0x01;

    protected static final int PSP_EVENT_WAITCLEARALL = 0x10;

    protected static final int PSP_EVENT_WAITCLEAR = 0x20;

    public void reset() {
        eventMap = new HashMap<Integer, SceKernelEventFlagInfo>();
        eventFlagWaitStateChecker = new EventFlagWaitStateChecker();
    }

    /** Don't call this unless thread.waitType == PSP_WAIT_EVENTFLAG
     * @return true if the thread was waiting on a valid event flag */
    private boolean removeWaitingThread(SceKernelThreadInfo thread) {
        SceKernelEventFlagInfo event = eventMap.get(thread.wait.EventFlag_id);
        if (event != null) {
            event.numWaitThreads--;
            if (event.numWaitThreads < 0) {
                log.warn("removing waiting thread " + Integer.toHexString(thread.uid) + ", event " + Integer.toHexString(event.uid) + " numWaitThreads underflowed");
                event.numWaitThreads = 0;
            }
            return true;
        }
        return false;
    }

    /** Don't call this unless thread.wait.waitingOnEventFlag == true */
    public void onThreadWaitTimeout(SceKernelThreadInfo thread) {
        if (removeWaitingThread(thread)) {
            thread.cpuContext.gpr[2] = ERROR_KERNEL_WAIT_TIMEOUT;
        } else {
            log.warn("EventFlag deleted while we were waiting for it! (timeout expired)");
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
        if (thread.isWaitingForType(PSP_WAIT_EVENTFLAG)) {
            removeWaitingThread(thread);
        }
    }

    private void onEventFlagDeletedCancelled(int evid, int result) {
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        boolean reschedule = false;
        for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext(); ) {
            SceKernelThreadInfo thread = it.next();
            if (thread.isWaitingForType(PSP_WAIT_EVENTFLAG) && thread.wait.EventFlag_id == evid) {
                thread.cpuContext.gpr[2] = result;
                threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);
                reschedule = true;
            }
        }
        if (reschedule) {
            threadMan.hleRescheduleCurrentThread();
        }
    }

    private void onEventFlagDeleted(int evid) {
        onEventFlagDeletedCancelled(evid, ERROR_KERNEL_WAIT_DELETE);
    }

    private void onEventFlagCancelled(int evid) {
        onEventFlagDeletedCancelled(evid, ERROR_KERNEL_WAIT_CANCELLED);
    }

    private void onEventFlagModified(SceKernelEventFlagInfo event) {
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        boolean reschedule = false;
        for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext(); ) {
            SceKernelThreadInfo thread = it.next();
            if (thread.isWaitingForType(PSP_WAIT_EVENTFLAG) && thread.wait.EventFlag_id == event.uid) {
                if (checkEventFlag(event, thread.wait.EventFlag_bits, thread.wait.EventFlag_wait, thread.wait.EventFlag_outBits_addr)) {
                    if (log.isDebugEnabled()) {
                        log.debug("onEventFlagModified waking thread 0x" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "'");
                    }
                    event.numWaitThreads--;
                    thread.cpuContext.gpr[2] = 0;
                    threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);
                    reschedule = true;
                    if (event.currentPattern == 0) {
                        break;
                    }
                }
            }
        }
        if (reschedule) {
            threadMan.hleRescheduleCurrentThread();
        }
    }

    private boolean checkEventFlag(SceKernelEventFlagInfo event, int bits, int wait, int outBits_addr) {
        boolean matched = false;
        if (((wait & PSP_EVENT_WAITANDOR_MASK) == PSP_EVENT_WAITAND) && ((event.currentPattern & bits) == bits)) {
            matched = true;
        } else if (((wait & PSP_EVENT_WAITANDOR_MASK) == PSP_EVENT_WAITOR) && ((event.currentPattern & bits) != 0)) {
            matched = true;
        }
        if (matched) {
            Memory mem = Memory.getInstance();
            if (Memory.isAddressGood(outBits_addr)) {
                mem.write32(outBits_addr, event.currentPattern);
            }
            if ((wait & PSP_EVENT_WAITCLEARALL) == PSP_EVENT_WAITCLEARALL) {
                event.currentPattern = 0;
            }
            if ((wait & PSP_EVENT_WAITCLEAR) == PSP_EVENT_WAITCLEAR) {
                event.currentPattern &= ~bits;
            }
        }
        return matched;
    }

    public int sceKernelCreateEventFlag(int name_addr, int attr, int initPattern, int option) {
        String name = readStringZ(name_addr);
        if (log.isDebugEnabled()) {
            log.debug("sceKernelCreateEventFlag(name='" + name + "', attr=0x" + Integer.toHexString(attr) + ", initPattern=0x" + Integer.toHexString(initPattern) + ", option=0x" + Integer.toHexString(option) + ")");
        }
        SceKernelEventFlagInfo event = new SceKernelEventFlagInfo(name, attr, initPattern, initPattern);
        eventMap.put(event.uid, event);
        return event.uid;
    }

    public int sceKernelDeleteEventFlag(int uid) {
        if (log.isDebugEnabled()) {
            log.debug("sceKernelDeleteEventFlag uid=0x" + Integer.toHexString(uid) + ")");
        }
        SceUidManager.checkUidPurpose(uid, "ThreadMan-eventflag", true);
        SceKernelEventFlagInfo event = eventMap.remove(uid);
        if (event == null) {
            log.warn("sceKernelDeleteEventFlag unknown uid");
            return ERROR_KERNEL_NOT_FOUND_EVENT_FLAG;
        }
        if (event.numWaitThreads > 0) {
            log.warn("sceKernelDeleteEventFlag numWaitThreads " + event.numWaitThreads);
        }
        onEventFlagDeleted(uid);
        return 0;
    }

    public int sceKernelSetEventFlag(int uid, int bitsToSet) {
        if (log.isDebugEnabled()) {
            log.debug("sceKernelSetEventFlag uid=0x" + Integer.toHexString(uid) + " bitsToSet=0x" + Integer.toHexString(bitsToSet));
        }
        SceUidManager.checkUidPurpose(uid, "ThreadMan-eventflag", true);
        SceKernelEventFlagInfo event = eventMap.get(uid);
        if (event == null) {
            log.warn("sceKernelSetEventFlag unknown uid=0x" + Integer.toHexString(uid));
            return ERROR_KERNEL_NOT_FOUND_EVENT_FLAG;
        }
        event.currentPattern |= bitsToSet;
        onEventFlagModified(event);
        return 0;
    }

    public int sceKernelClearEventFlag(int uid, int bitsToKeep) {
        if (log.isDebugEnabled()) {
            log.debug("sceKernelClearEventFlag uid=0x" + Integer.toHexString(uid) + " bitsToKeep=0x" + Integer.toHexString(bitsToKeep));
        }
        SceUidManager.checkUidPurpose(uid, "ThreadMan-eventflag", true);
        SceKernelEventFlagInfo event = eventMap.get(uid);
        if (event == null) {
            log.warn("sceKernelClearEventFlag unknown uid=0x" + Integer.toHexString(uid));
            return ERROR_KERNEL_NOT_FOUND_EVENT_FLAG;
        }
        event.currentPattern &= bitsToKeep;
        return 0;
    }

    public int hleKernelWaitEventFlag(int uid, int bits, int wait, int outBits_addr, int timeout_addr, boolean doCallbacks) {
        if (log.isDebugEnabled()) {
            log.debug("hleKernelWaitEventFlag uid=0x" + Integer.toHexString(uid) + " bits=0x" + Integer.toHexString(bits) + " wait=0x" + Integer.toHexString(wait) + " outBits=0x" + Integer.toHexString(outBits_addr) + " timeout=0x" + Integer.toHexString(timeout_addr) + " callbacks=" + doCallbacks);
        }
        if ((wait & ~(PSP_EVENT_WAITOR | PSP_EVENT_WAITCLEAR | PSP_EVENT_WAITCLEARALL)) != 0 || (wait & (PSP_EVENT_WAITCLEAR | PSP_EVENT_WAITCLEARALL)) == (PSP_EVENT_WAITCLEAR | PSP_EVENT_WAITCLEARALL)) {
            return ERROR_KERNEL_ILLEGAL_MODE;
        }
        if (bits == 0) {
            return ERROR_KERNEL_EVENT_FLAG_ILLEGAL_WAIT_PATTERN;
        }
        if (!Modules.ThreadManForUserModule.isDispatchThreadEnabled()) {
            if (log.isDebugEnabled()) {
                log.debug("hleKernelWaitEventFlag called when dispatch thread disabled");
            }
            return ERROR_KERNEL_WAIT_CAN_NOT_WAIT;
        }
        SceUidManager.checkUidPurpose(uid, "ThreadMan-eventflag", true);
        SceKernelEventFlagInfo event = eventMap.get(uid);
        if (event == null) {
            log.warn("hleKernelWaitEventFlag unknown uid=0x" + Integer.toHexString(uid));
            return ERROR_KERNEL_NOT_FOUND_EVENT_FLAG;
        }
        if (event.numWaitThreads >= 1 && (event.attr & PSP_EVENT_WAITMULTIPLE) != PSP_EVENT_WAITMULTIPLE) {
            log.warn("hleKernelWaitEventFlag already another thread waiting on it");
            return ERROR_KERNEL_EVENT_FLAG_NO_MULTI_PERM;
        }
        if (!checkEventFlag(event, bits, wait, outBits_addr)) {
            if (log.isDebugEnabled()) {
                log.debug("hleKernelWaitEventFlag - '" + event.name + "' fast check failed");
            }
            event.numWaitThreads++;
            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
            SceKernelThreadInfo currentThread = threadMan.getCurrentThread();
            currentThread.wait.EventFlag_id = uid;
            currentThread.wait.EventFlag_bits = bits;
            currentThread.wait.EventFlag_wait = wait;
            currentThread.wait.EventFlag_outBits_addr = outBits_addr;
            threadMan.hleKernelThreadEnterWaitState(PSP_WAIT_EVENTFLAG, uid, eventFlagWaitStateChecker, timeout_addr, doCallbacks);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("hleKernelWaitEventFlag - '" + event.name + "' fast check succeeded");
            }
        }
        return 0;
    }

    public int sceKernelWaitEventFlag(int uid, int bits, int wait, int outBits_addr, int timeout_addr) {
        if (log.isDebugEnabled()) {
            log.debug("sceKernelWaitEventFlag redirecting to hleKernelWaitEventFlag(callbacks=false)");
        }
        return hleKernelWaitEventFlag(uid, bits, wait, outBits_addr, timeout_addr, false);
    }

    public int sceKernelWaitEventFlagCB(int uid, int bits, int wait, int outBits_addr, int timeout_addr) {
        if (log.isDebugEnabled()) {
            log.debug("sceKernelWaitEventFlagCB redirecting to hleKernelWaitEventFlag(callbacks=true)");
        }
        return hleKernelWaitEventFlag(uid, bits, wait, outBits_addr, timeout_addr, true);
    }

    public int sceKernelPollEventFlag(int uid, int bits, int wait, int outBits_addr) {
        if (log.isDebugEnabled()) {
            log.debug("sceKernelPollEventFlag uid=0x" + Integer.toHexString(uid) + " bits=0x" + Integer.toHexString(bits) + " wait=0x" + Integer.toHexString(wait) + " outBits=0x" + Integer.toHexString(outBits_addr));
        }
        SceUidManager.checkUidPurpose(uid, "ThreadMan-eventflag", true);
        SceKernelEventFlagInfo event = eventMap.get(uid);
        if (event == null) {
            log.warn("sceKernelPollEventFlag unknown uid=0x" + Integer.toHexString(uid));
            return ERROR_KERNEL_NOT_FOUND_EVENT_FLAG;
        }
        if (bits == 0) {
            return ERROR_KERNEL_EVENT_FLAG_ILLEGAL_WAIT_PATTERN;
        }
        if (!checkEventFlag(event, bits, wait, outBits_addr)) {
            if (Memory.isAddressGood(outBits_addr)) {
                Memory.getInstance().write32(outBits_addr, event.currentPattern);
            }
            return ERROR_KERNEL_EVENT_FLAG_POLL_FAILED;
        }
        return 0;
    }

    public int sceKernelCancelEventFlag(int uid, int newPattern, int numWaitThreadAddr) {
        if (log.isDebugEnabled()) {
            log.debug("sceKernelCancelEventFlag uid=0x" + Integer.toHexString(uid) + " newPattern=0x" + Integer.toHexString(newPattern) + " numWaitThreadAddr=0x" + Integer.toHexString(numWaitThreadAddr));
        }
        SceUidManager.checkUidPurpose(uid, "ThreadMan-eventflag", true);
        SceKernelEventFlagInfo event = eventMap.get(uid);
        if (event == null) {
            log.warn("sceKernelCancelEventFlag unknown uid=0x" + Integer.toHexString(uid));
            return ERROR_KERNEL_NOT_FOUND_EVENT_FLAG;
        }
        Memory mem = Memory.getInstance();
        if (Memory.isAddressGood(numWaitThreadAddr)) {
            mem.write32(numWaitThreadAddr, event.numWaitThreads);
        }
        event.currentPattern = newPattern;
        event.numWaitThreads = 0;
        onEventFlagCancelled(uid);
        return 0;
    }

    public int sceKernelReferEventFlagStatus(int uid, int addr) {
        if (log.isDebugEnabled()) {
            log.debug("sceKernelReferEventFlagStatus uid=0x" + Integer.toHexString(uid) + " addr=0x" + Integer.toHexString(addr));
        }
        SceUidManager.checkUidPurpose(uid, "ThreadMan-eventflag", true);
        SceKernelEventFlagInfo event = eventMap.get(uid);
        if (event == null) {
            log.warn("sceKernelReferEventFlagStatus unknown uid=0x" + Integer.toHexString(uid));
            return ERROR_KERNEL_NOT_FOUND_EVENT_FLAG;
        }
        event.write(Memory.getInstance(), addr);
        return 0;
    }

    private class EventFlagWaitStateChecker implements IWaitStateChecker {

        @Override
        public boolean continueWaitState(SceKernelThreadInfo thread, ThreadWaitInfo wait) {
            SceKernelEventFlagInfo event = eventMap.get(wait.EventFlag_id);
            if (event == null) {
                thread.cpuContext.gpr[2] = ERROR_KERNEL_NOT_FOUND_EVENT_FLAG;
                return false;
            }
            if (checkEventFlag(event, wait.EventFlag_bits, wait.EventFlag_wait, wait.EventFlag_outBits_addr)) {
                event.numWaitThreads--;
                thread.cpuContext.gpr[2] = 0;
                return false;
            }
            return true;
        }
    }

    public static final EventFlagManager singleton = new EventFlagManager();

    private EventFlagManager() {
    }
}
