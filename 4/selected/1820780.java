package joeq.Scheduler;

import joeq.Allocator.ObjectLayout;
import joeq.Class.PrimordialClassLoader;
import joeq.Class.jq_Class;
import joeq.Class.jq_CompiledCode;
import joeq.Class.jq_DontAlign;
import joeq.Class.jq_InstanceField;
import joeq.Class.jq_InstanceMethod;
import joeq.Class.jq_NameAndDesc;
import joeq.Class.jq_Reference;
import joeq.Class.jq_StaticMethod;
import joeq.Main.jq;
import joeq.Memory.CodeAddress;
import joeq.Memory.HeapAddress;
import joeq.Memory.StackAddress;
import joeq.Runtime.SystemInterface;
import joeq.Runtime.Unsafe;
import joeq.UTF.Utf8;
import jwutil.sync.AtomicCounter;
import jwutil.util.Assert;

/**
 * A jq_Thread corresponds to a Java (lightweight) thread.
 * 
 * @author  John Whaley <jwhaley@alum.mit.edu>
 * @version $Id: jq_Thread.java 2466 2006-06-07 23:12:58Z joewhaley $
 */
public class jq_Thread implements jq_DontAlign {

    private final jq_RegisterState registers;

    private volatile int thread_switch_enabled;

    private jq_NativeThread native_thread;

    private final Thread thread_object;

    jq_Thread next;

    private jq_CompiledCode entry_point;

    private boolean isDaemon;

    boolean isScheduler;

    private boolean hasStarted;

    private boolean isDead;

    boolean wasPreempted;

    private int priority;

    private volatile int isInterrupted;

    private final int thread_id;

    public volatile boolean is_delivering_exception;

    public static int INITIAL_STACK_SIZE = 40960;

    public static AtomicCounter thread_id_factory = new AtomicCounter(1);

    public jq_Thread(Thread t) {
        this.thread_object = t;
        this.registers = jq_RegisterState.create();
        this.thread_id = thread_id_factory.increment() << ObjectLayout.THREAD_ID_SHIFT;
        Assert._assert(this.thread_id > 0);
        Assert._assert(this.thread_id < ObjectLayout.THREAD_ID_MASK);
        this.isDead = true;
        this.priority = 5;
    }

    public Thread getJavaLangThreadObject() {
        return thread_object;
    }

    public String toString() {
        return thread_object + " (id=" + thread_id + ", sus: " + thread_switch_enabled + ")";
    }

    public jq_RegisterState getRegisterState() {
        return registers;
    }

    public jq_NativeThread getNativeThread() {
        return native_thread;
    }

    void setNativeThread(jq_NativeThread nt) {
        native_thread = nt;
    }

    public boolean isThreadSwitchEnabled() {
        return thread_switch_enabled == 0;
    }

    public void disableThreadSwitch() {
        if (!jq.RunningNative) {
            ++thread_switch_enabled;
        } else {
            ((HeapAddress) HeapAddress.addressOf(this).offset(_thread_switch_enabled.getOffset())).atomicAdd(1);
        }
    }

    public void enableThreadSwitch() {
        if (!jq.RunningNative) {
            --thread_switch_enabled;
        } else {
            ((HeapAddress) HeapAddress.addressOf(this).offset(_thread_switch_enabled.getOffset())).atomicSub(1);
        }
    }

    public void init() {
        Thread t = thread_object;
        jq_Reference z = jq_Reference.getTypeOf(t);
        jq_InstanceMethod m = z.getVirtualMethod(new jq_NameAndDesc(Utf8.get("run"), Utf8.get("()V")));
        entry_point = m.getDefaultCompiledVersion();
        StackAddress stack = SystemInterface.allocate_stack(INITIAL_STACK_SIZE);
        if (stack.isNull()) {
            throw new OutOfMemoryError("Cannot allocate thread stack of size " + INITIAL_STACK_SIZE);
        }
        this.registers.setEsp(stack);
        this.registers.setEip(entry_point.getEntrypoint());
        this.registers.setEsp((StackAddress) this.registers.getEsp().offset(-CodeAddress.size()));
        this.registers.setEsp((StackAddress) this.registers.getEsp().offset(-HeapAddress.size()));
        this.registers.getEsp().poke(HeapAddress.addressOf(t));
        this.registers.setEsp((StackAddress) this.registers.getEsp().offset(-CodeAddress.size()));
        this.registers.getEsp().poke(_destroyCurrentThread.getDefaultCompiledVersion().getEntrypoint());
    }

    public void start() {
        if (entry_point == null) {
            this.init();
        }
        if (this.hasStarted) throw new IllegalThreadStateException();
        this.isDead = false;
        this.hasStarted = true;
        jq_NativeThread.startJavaThread(this);
    }

    long sleepUntil;

    public void sleep(long millis) throws InterruptedException {
        sleepUntil = System.currentTimeMillis() + millis;
        for (; ; ) {
            if (this.isInterrupted(true)) {
                throw new InterruptedException();
            }
            yield();
            if (System.currentTimeMillis() >= sleepUntil) {
                break;
            }
        }
    }

    public void yield() {
        if (this != Unsafe.getThreadBlock()) {
            SystemInterface.debugwriteln("Yield called on " + this + " from thread " + Unsafe.getThreadBlock());
            Assert.UNREACHABLE();
        }
        this.disableThreadSwitch();
        StackAddress esp = StackAddress.getStackPointer();
        registers.setEsp((StackAddress) esp.offset(-CodeAddress.size() - HeapAddress.size()));
        registers.setEbp(StackAddress.getBasePointer());
        registers.setControlWord(0x027f);
        registers.setStatusWord(0x4000);
        registers.setTagWord(0xffff);
        this.getNativeThread().yieldCurrentThread();
    }

    public void yieldTo(jq_Thread t) {
        Assert._assert(this == Unsafe.getThreadBlock());
        this.disableThreadSwitch();
        if (t.getNativeThread() != this.getNativeThread()) {
            return;
        }
        StackAddress esp = StackAddress.getStackPointer();
        registers.setEsp((StackAddress) esp.offset(-CodeAddress.size() - HeapAddress.size() - HeapAddress.size()));
        registers.setEbp(StackAddress.getBasePointer());
        registers.setControlWord(0x027f);
        registers.setStatusWord(0x4000);
        registers.setTagWord(0xffff);
        this.getNativeThread().yieldCurrentThreadTo(t);
    }

    public void setPriority(int newPriority) {
        Assert._assert(newPriority >= 0);
        Assert._assert(newPriority <= 9);
        this.priority = newPriority;
    }

    public int getPriority() {
        return this.priority;
    }

    public void stop(Object o) {
    }

    public void suspend() {
    }

    public void resume() {
    }

    public void interrupt() {
        this.isInterrupted = 1;
    }

    public boolean isInterrupted(boolean clear) {
        boolean isInt = this.isInterrupted != 0;
        if (clear && isInt) {
            this.isInterrupted = 0;
        }
        return isInt;
    }

    public boolean isAlive() {
        return !isDead;
    }

    public boolean isDaemon() {
        return isDaemon;
    }

    public void setDaemon(boolean b) {
        isDaemon = b;
    }

    public int countStackFrames() {
        return 0;
    }

    public int getThreadId() {
        return thread_id;
    }

    public static void destroyCurrentThread() {
        jq_Thread t = Unsafe.getThreadBlock();
        t.disableThreadSwitch();
        t.isDead = true;
        jq_NativeThread.endCurrentJavaThread();
        Assert.UNREACHABLE();
    }

    public jq_Thread getNext() {
        return next;
    }

    public static final jq_Class _class;

    public static final jq_StaticMethod _destroyCurrentThread;

    public static final jq_InstanceField _thread_switch_enabled;

    public static final jq_InstanceField _isInterrupted;

    static {
        _class = (jq_Class) PrimordialClassLoader.loader.getOrCreateBSType("Ljoeq/Scheduler/jq_Thread;");
        _destroyCurrentThread = _class.getOrCreateStaticMethod("destroyCurrentThread", "()V");
        _thread_switch_enabled = _class.getOrCreateInstanceField("thread_switch_enabled", "I");
        _isInterrupted = _class.getOrCreateInstanceField("isInterrupted", "I");
    }
}
