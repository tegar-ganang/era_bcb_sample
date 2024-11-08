package joeq.Runtime;

import joeq.Allocator.DefaultHeapAllocator;
import joeq.Allocator.ObjectLayout;
import joeq.Class.PrimordialClassLoader;
import joeq.Class.jq_Class;
import joeq.Class.jq_InstanceField;
import joeq.Class.jq_StaticMethod;
import joeq.Memory.HeapAddress;
import joeq.Scheduler.jq_Thread;
import jwutil.strings.Strings;
import jwutil.util.Assert;

public class Monitor {

    public static boolean TRACE = false;

    private Monitor() {
        Assert.UNREACHABLE("Monitor objects must be constructed specially.");
    }

    int atomic_count = 0;

    jq_Thread monitor_owner;

    int entry_count = 0;

    int semaphore;

    /** Returns the depth of the lock on the given object. */
    public static int getLockEntryCount(Object k) {
        int lockword = HeapAddress.addressOf(k).offset(ObjectLayout.STATUS_WORD_OFFSET).peek4();
        if (lockword < 0) {
            Monitor m = getMonitor(lockword);
            if (TRACE) SystemInterface.debugwriteln("Getting fat lock entry count: " + m.entry_count);
            return m.entry_count;
        }
        int c = ((lockword & ObjectLayout.LOCK_COUNT_MASK) >> ObjectLayout.LOCK_COUNT_SHIFT);
        if ((lockword & ObjectLayout.THREAD_ID_MASK) != 0) ++c;
        if (TRACE) SystemInterface.debugwriteln("Getting thin lock entry count, lockword=" + Strings.hex8(lockword) + ", count=" + c);
        return c;
    }

    /** Monitorenter runtime routine.
     *  Checks for thin lock usage, otherwise falls back to inflated locks.
     */
    public static void monitorenter(Object k) {
        jq_Thread t = Unsafe.getThreadBlock();
        int tid = t.getThreadId();
        HeapAddress status_address = (HeapAddress) HeapAddress.addressOf(k).offset(ObjectLayout.STATUS_WORD_OFFSET);
        int status_flags = status_address.peek4() & ObjectLayout.STATUS_FLAGS_MASK;
        int newlockword = status_flags | tid;
        int oldlockword = status_address.atomicCas4(status_flags, newlockword);
        if (Unsafe.isEQ()) {
            return;
        }
        int counter = oldlockword ^ newlockword;
        if (counter >= ObjectLayout.LOCK_COUNT_MASK) {
            int entrycount;
            if (counter == ObjectLayout.LOCK_COUNT_MASK) {
                if (TRACE) SystemInterface.debugwriteln("Thin lock counter overflow, inflating lock...");
                entrycount = (ObjectLayout.LOCK_COUNT_MASK >> ObjectLayout.LOCK_COUNT_SHIFT) + 2;
                Monitor m = allocateInflatedLock();
                m.monitor_owner = t;
                m.entry_count = entrycount;
                newlockword = HeapAddress.addressOf(m).to32BitValue() | ObjectLayout.LOCK_EXPANDED | status_flags;
                Assert._assert(HeapAddress.addressOf(k).offset(ObjectLayout.STATUS_WORD_OFFSET).peek4() == oldlockword);
                HeapAddress.addressOf(k).offset(ObjectLayout.STATUS_WORD_OFFSET).poke4(newlockword);
            } else {
                if (TRACE) SystemInterface.debugwriteln(t + " tid " + Strings.hex(tid) + ": Lock contention with tid " + Strings.hex(oldlockword & ObjectLayout.THREAD_ID_MASK) + ", inflating...");
                entrycount = 1;
                Monitor m = allocateInflatedLock();
                m.monitor_owner = t;
                m.entry_count = entrycount;
                installInflatedLock(k, m);
            }
        } else if (counter < 0) {
            Monitor m = getMonitor(oldlockword);
            m.lock(t);
        } else {
            HeapAddress.addressOf(k).offset(ObjectLayout.STATUS_WORD_OFFSET).poke4(oldlockword + ObjectLayout.LOCK_COUNT_INC);
        }
    }

    /** Monitorexit runtime routine.
     *  Checks for thin lock usage, otherwise falls back to inflated locks.
     */
    public static void monitorexit(Object k) {
        jq_Thread t = Unsafe.getThreadBlock();
        int tid = t.getThreadId();
        HeapAddress status_address = (HeapAddress) HeapAddress.addressOf(k).offset(ObjectLayout.STATUS_WORD_OFFSET);
        int oldlockword = status_address.peek4();
        int counter = oldlockword ^ tid;
        if (counter < 0) {
            Monitor m = getMonitor(oldlockword);
            m.unlock(t);
        } else if (counter <= ObjectLayout.STATUS_FLAGS_MASK) {
            status_address.atomicAnd(ObjectLayout.STATUS_FLAGS_MASK);
        } else if (counter <= (ObjectLayout.LOCK_COUNT_MASK | ObjectLayout.STATUS_FLAGS_MASK)) {
            status_address.atomicSub(ObjectLayout.LOCK_COUNT_INC);
        } else {
            SystemInterface.debugwriteln("Thin lock not owned by us (" + Strings.hex8(tid) + ")! lockword=" + Strings.hex8(oldlockword));
            throw new IllegalMonitorStateException();
        }
    }

    /** Get the Monitor object associated with this lockword. */
    public static Monitor getMonitor(int lockword) {
        int word = lockword & (~ObjectLayout.LOCK_EXPANDED & ~ObjectLayout.STATUS_FLAGS_MASK);
        HeapAddress a = HeapAddress.address32(word);
        return (Monitor) a.asObject();
    }

    public static Monitor allocateInflatedLock() {
        return (Monitor) DefaultHeapAllocator.allocateObjectAlign8(_class.getInstanceSize(), _class.getVTable());
    }

    public void free() {
    }

    /** Installs an inflated lock on the given object.
     *  Uses a spin-loop to wait until the object is unlocked or inflated.
     */
    public static void installInflatedLock(Object k, Monitor m) {
        Assert._assert(m.monitor_owner == Unsafe.getThreadBlock());
        Assert._assert(m.entry_count >= 1);
        for (; ; ) {
            HeapAddress status_address = (HeapAddress) HeapAddress.addressOf(k).offset(ObjectLayout.STATUS_WORD_OFFSET);
            int oldlockword = status_address.peek4();
            if (oldlockword < 0) {
                Assert._assert(m.entry_count == 1);
                m.free();
                Monitor m2 = getMonitor(oldlockword);
                if (TRACE) SystemInterface.debugwriteln("Inflated by another thread! lockword=" + Strings.hex8(oldlockword) + " lock=" + m2);
                Assert._assert(m != m2);
                m2.lock(Unsafe.getThreadBlock());
                return;
            }
            int status_flags = oldlockword & ObjectLayout.STATUS_FLAGS_MASK;
            HeapAddress m_addr = HeapAddress.addressOf(m);
            if ((m_addr.to32BitValue() & ObjectLayout.STATUS_FLAGS_MASK) != 0 || (m_addr.to32BitValue() & ObjectLayout.LOCK_EXPANDED) != 0) {
                Assert.UNREACHABLE("Monitor object has address " + m_addr.stringRep());
            }
            int newlockword = m_addr.to32BitValue() | ObjectLayout.LOCK_EXPANDED | status_flags;
            status_address.atomicCas4(status_flags, newlockword);
            if (Unsafe.isEQ()) {
                if (TRACE) SystemInterface.debugwriteln("Thread " + m.monitor_owner.getThreadId() + " obtained inflated lock! new lockword=" + Strings.hex8(newlockword));
                return;
            } else {
                if (TRACE) SystemInterface.debugwriteln("Thread " + m.monitor_owner.getThreadId() + " failed to obtain inflated lock, lockword was " + Strings.hex8(oldlockword));
            }
            Thread.yield();
        }
    }

    /** Lock this monitor with the given thread block.
     */
    public void lock(jq_Thread t) {
        jq_Thread m_t = this.monitor_owner;
        if (m_t == t) {
            Assert._assert(this.atomic_count >= 0);
            Assert._assert(this.entry_count > 0);
            ++this.entry_count;
            if (TRACE) SystemInterface.debugwriteln("We (" + t + ") own lock " + this + ", incrementing entry count: " + this.entry_count);
            return;
        }
        if (TRACE) SystemInterface.debugwriteln("We (" + t + ") are attempting to obtain lock " + this);
        HeapAddress ac_loc = (HeapAddress) HeapAddress.addressOf(this).offset(_atomic_count.getOffset());
        ac_loc.atomicAdd(1);
        if (!Unsafe.isEQ()) {
            if (TRACE) SystemInterface.debugwriteln("Lock " + this + " cannot be obtained (owned by " + m_t + ", or there are other waiters); waiting on semaphore (" + this.atomic_count + " waiters)");
            this.waitOnSemaphore();
            if (TRACE) SystemInterface.debugwriteln("We (" + t + ") finished waiting on " + this);
        } else {
            if (TRACE) SystemInterface.debugwriteln(this + " is unlocked, we (" + t + ") obtain it.");
        }
        Assert._assert(this.monitor_owner == null);
        Assert._assert(this.entry_count == 0);
        Assert._assert(this.atomic_count >= 0);
        if (TRACE) SystemInterface.debugwriteln("We (" + t + ") obtained lock " + this);
        this.monitor_owner = t;
        this.entry_count = 1;
    }

    /** Unlock this monitor with the given thread block.
     */
    public void unlock(jq_Thread t) {
        jq_Thread m_t = this.monitor_owner;
        if (m_t != t) {
            SystemInterface.debugwriteln("We (" + t + ") tried to unlock lock " + this + " owned by " + m_t);
            throw new IllegalMonitorStateException();
        }
        if (--this.entry_count > 0) {
            if (TRACE) SystemInterface.debugwriteln("Decrementing lock " + this + " entry count " + this.entry_count);
            return;
        }
        if (TRACE) SystemInterface.debugwriteln("We (" + t + ") are unlocking lock " + this + ", current waiters=" + this.atomic_count);
        this.monitor_owner = null;
        HeapAddress ac_loc = (HeapAddress) HeapAddress.addressOf(this).offset(_atomic_count.getOffset());
        ac_loc.atomicSub(1);
        if (Unsafe.isGE()) {
            if (TRACE) SystemInterface.debugwriteln((this.atomic_count + 1) + " threads are waiting on released lock " + this + ", releasing semaphore.");
            this.releaseSemaphore();
        } else {
            if (TRACE) SystemInterface.debugwriteln("No threads are waiting on released lock " + this + ".");
        }
    }

    /** Create a semaphore if there isn't one already, and wait on it.
     */
    public void waitOnSemaphore() {
        if (this.semaphore == 0) {
            this.semaphore = SystemInterface.init_semaphore();
        }
        for (; ; ) {
            int rc = SystemInterface.wait_for_single_object(this.semaphore, 10);
            if (rc == SystemInterface.WAIT_TIMEOUT) {
                Thread.yield();
                continue;
            } else if (rc == 0) {
                return;
            } else {
                SystemInterface.debugwriteln("Bad return value from WaitForSingleObject: " + rc);
            }
        }
    }

    /** Create a semaphore if there isn't one already, and release it.
     */
    public void releaseSemaphore() {
        if (this.semaphore == 0) {
            this.semaphore = SystemInterface.init_semaphore();
        }
        SystemInterface.release_semaphore(this.semaphore, 1);
    }

    public static final jq_Class _class;

    public static final jq_StaticMethod _monitorenter;

    public static final jq_StaticMethod _monitorexit;

    public static final jq_InstanceField _atomic_count;

    static {
        _class = (jq_Class) PrimordialClassLoader.loader.getOrCreateBSType("Ljoeq/Runtime/Monitor;");
        _monitorenter = _class.getOrCreateStaticMethod("monitorenter", "(Ljava/lang/Object;)V");
        _monitorexit = _class.getOrCreateStaticMethod("monitorexit", "(Ljava/lang/Object;)V");
        _atomic_count = _class.getOrCreateInstanceField("atomic_count", "I");
    }
}
