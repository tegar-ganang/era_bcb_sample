package de.jassda.jabyba.backend.runtime;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import de.jassda.jabyba.backend.util.ByteUtil;
import de.jassda.jabyba.backend.util.PacketFactory;
import de.jassda.jabyba.backend.util.Constants.Logging;
import de.jassda.jabyba.jdwp.JDWP;
import de.jassda.jabyba.jdwp.Packet;
import de.jassda.jabyba.jdwp.delegate.ThreadReference;
import de.jassda.jabyba.jdwp.delegate.VirtualMachine;

/**
 * A RuntimeThread encapsulated a usual {@link java.lang.Thread} but adds some
 * additional information to it. Besides that this class manages all instances of 
 * RuntimeThread as a decentralised database. <br />
 * This classes offers some methods to suspend and resume RuntimeThreads. This needs 
 * further explanations. A JVM allows a debugger to suspend and resume any thread. This 
 * has to be simulated here as well. Contrary to a VM the JaByba has no direct control of the
 * debuggee's threads. In order to implement simular suspend/resume behaviour RuntimeThreads
 * have to check if they are suspend before they contact the debugger. If the debugger set a 
 * suspend-request for a thread it will wait now. Note the delay between requesting a suspend
 * and the actual suspend. It's <b>not</b> garantueed that a thread which is marked as suspended 
 * will ever suspend. It is however garantueed that a suspended thread won't contact the 
 * debugger.
 * 
 * @author <a href="mailto:johannes.rieken@informatik.uni-oldenburg.de">riejo</a> */
public class RuntimeThread extends Thread implements VirtualMachine.Suspend, VirtualMachine.Resume, VirtualMachine.AllThreads, ThreadReference.Suspend, ThreadReference.Resume {

    private static Hashtable<RuntimeThread, Integer> suspendCount = new Hashtable<RuntimeThread, Integer>();

    private static int _newThreadSuspend = 0;

    private Runnable nestedRunnable;

    private int id;

    private Object mutex;

    /**
	 * A new instance of a RuntimeThread. For each RuntimeThread a Stack is created and 
	 * it is added to a internal registry.
	 * @param target The actual thread/runnable object  */
    protected RuntimeThread(Runnable target) {
        super(target);
        StackFrame.createStackFrame(this);
        id = RuntimeRegistry.getInstance().newObject(this);
        mutex = new Object();
        initDelegates();
        nestedRunnable = target;
        suspendCount.put(this, _newThreadSuspend);
    }

    private void initDelegates() {
        VirtualMachine.Delegate.AllThreads.allThreads = this;
        VirtualMachine.Delegate.Suspend.suspend = this;
        VirtualMachine.Delegate.Resume.resume = this;
        ThreadReference.Delegate.Suspend.suspend = this;
        ThreadReference.Delegate.Resume.resume = this;
    }

    /**
	 * @return The identifier of this RuntimeThread.	 */
    public int getIdentifier() {
        return id;
    }

    /**
	 * TODO use mutex for suspend, resume
	 * @return An object can be used for explizit synchronization.	 */
    public Object getMutex() {
        return mutex;
    }

    public boolean equals(Object obj) {
        return obj instanceof RuntimeThread && ((RuntimeThread) obj).id == id;
    }

    /**
	 * The only way to get a instance of RuntimeThread is this method. It garantuees that
	 * each nested {@link java.lang.Thread} is encapsulated by only one RuntimeThread.
	 * @param target The nested Thread/Runnable.
	 * @return The instance which encapsulates the target.	 */
    public static RuntimeThread newInstance(Runnable target) {
        for (RuntimeThread thread : suspendCount.keySet()) {
            if (thread.equals(target) || thread.nestedRunnable.equals(target)) {
                Logging.logRuntime.info("Return RuntimeThread [id=" + thread.getIdentifier() + "] for target: " + target);
                return thread;
            }
        }
        RuntimeThread t = new RuntimeThread(target);
        Logging.logRuntime.info("New RuntimeThread [id=" + t.getIdentifier() + "] for target:" + target);
        return t;
    }

    /**
	 * Checks if the is a suspend request for the passed thread. If so the calling thread has to wait
	 * until the thread recieves a resume. 
	 * @param thread Thread which is supposed to suspend.
	 * @param loc Only for debugging purpose.	 */
    public static void waitForResume(RuntimeThread thread, String src) {
        Logging.logRuntime.info("RuntimeThread [id=" + thread.getIdentifier() + "] waits to resume. waitForResume called by [" + src + "]");
        synchronized (thread) {
            while (isSuspended(thread)) {
                try {
                    thread.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        Logging.logRuntime.info("RuntimeThread [id=" + thread.getIdentifier() + "] is resumed now");
    }

    public static void waitForResume(RuntimeThread thread) {
        waitForResume(thread, "not specified");
    }

    /**
	 * Sets a suspend request for the passed thread.
	 * <br /> <b>Note:</b> The suspend can only be applied
	 * if the method {@link #waitForResume(RuntimeThread, String)} is called on the same thread.
	 * @param thread The thread which has to suspend.
	 * @param loc The debugging purpose.
	 * @return The number of suspend requests for this thread.	 */
    public static int suspend(RuntimeThread thread) {
        int n = suspendCount.get(thread) + 1;
        suspendCount.put(thread, n);
        Logging.logRuntime.info("Runtime Thread [id=" + thread.getIdentifier() + "] has suspend count of " + n);
        return n;
    }

    /**
	 * Suspend all thread.
	 * @see #suspend(RuntimeThread, String)
	 * */
    public static void suspendAll() {
        for (RuntimeThread thread : suspendCount.keySet()) {
            suspend(thread);
        }
        _newThreadSuspend += 1;
    }

    /**
	 * Resumes the passed thread. This means that the number of suspend request is 
	 * decreased by one. The minium is zero. If zero is reached the thread will the resumed / 
	 * won't be suspended on his next call of {@link #waitForResume(RuntimeThread, String)}.
	 * @param thread The thread which suspend count will be decreased.
	 * @return The number of suspend requests.	 */
    public static int resume(RuntimeThread thread) {
        int n = suspendCount.get(thread) - 1;
        n = n < 0 ? 0 : n;
        suspendCount.put(thread, n);
        synchronized (thread) {
            thread.notifyAll();
        }
        Logging.logRuntime.info("Runtime Thread [id=" + thread.getIdentifier() + "] has suspend count of " + n);
        return n;
    }

    /**
	 * Resumes all threads.
	 * @see #resume(RuntimeThread, String);	 */
    public static void resumeAll() {
        for (RuntimeThread thread : suspendCount.keySet()) {
            resume(thread);
        }
        _newThreadSuspend = _newThreadSuspend > 0 ? _newThreadSuspend - 1 : _newThreadSuspend;
    }

    /**
	 * This is different. Contrary to {@link #resumeAll(String)} this method
	 * resets the suspend counter of all thread to zero.	 */
    public static void resumeAllTotally() {
        for (RuntimeThread thread : suspendCount.keySet()) {
            suspendCount.put(thread, 0);
        }
        _newThreadSuspend = 0;
    }

    /**
	 * Forces all threads to stop. The deprecated method {@link Thread#stop()}
	 * is used to stop the threads. This is supposed to be done when the backend
	 * is reseted.	 */
    @SuppressWarnings("deprecation")
    public static void forceStop() {
        for (RuntimeThread thread : suspendCount.keySet()) {
            thread.stop();
            suspendCount.remove(thread);
        }
    }

    /**
	 * @param thread
	 * @return <code>true</code> iff the passed thread has suspend count higher than 0.	 */
    public static boolean isSuspended(RuntimeThread thread) {
        return suspendCount.get(thread) > 0;
    }

    /**
	 * @param thread
	 * @return Returns the number of suspend requests for the passed thread.	 */
    public static int getSuspendCount(RuntimeThread thread) {
        return suspendCount.get(thread);
    }

    /**
	 * @return The number of threads.	 */
    public static int getThreadCount() {
        return suspendCount.size();
    }

    /**
	 * @return All RuntimeThread in an array.	 */
    public static RuntimeThread[] allRuntimeThreads() {
        RuntimeThread[] t = new RuntimeThread[suspendCount.size()];
        return suspendCount.keySet().toArray(t);
    }

    /**
	 * Returns all threads currently running in Jabyba. Only {@linkplain RuntimeThread}
	 * objects -threads of the debuggee process- are considered as Threads. 
	 */
    public Packet handleVirtualMachine$AllThreads(Packet cmd) {
        cmd = PacketFactory.changeToReply(cmd);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(buffer);
        try {
            out.write(ByteUtil.writeInteger(getThreadCount()));
            for (RuntimeThread thread : allRuntimeThreads()) {
                out.write(ByteUtil.writeInteger(thread.getIdentifier()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        cmd.data = buffer.toByteArray();
        return cmd;
    }

    /**
	 * Suspends all runtime threads and replies with a default packet.
	 * @see #suspendAll() 
	 * @see de.jassda.jabyba.jdwp.delegate.VirtualMachine.Suspend#handleVirtualMachine$Suspend(de.jassda.jabyba.jdwp.Packet, de.jassda.jabyba.jdwp.PacketWriter)
	 * @param cmd
	 * @param pWriter
	 */
    public Packet handleVirtualMachine$Suspend(Packet cmd) {
        RuntimeThread.suspendAll();
        cmd = PacketFactory.changeToReply(cmd);
        return cmd;
    }

    /**
	 * Resumes all runtime thread and replies with a default packet.
	 * @see #resumeAll() 
	 * @see de.jassda.jabyba.jdwp.delegate.VirtualMachine.Resume#handleVirtualMachine$Resume(de.jassda.jabyba.jdwp.Packet, de.jassda.jabyba.jdwp.PacketWriter)
	 * @param cmd
	 * @param pWriter
	 */
    public Packet handleVirtualMachine$Resume(Packet cmd) {
        RuntimeThread.resumeAll();
        cmd = PacketFactory.changeToReply(cmd);
        return cmd;
    }

    /**
	 * Suspends a single thread which id is contained in the packet.
	 * 
	 * @see #suspend(RuntimeThread)
	 */
    public Packet handleThreadReference$Suspend(Packet cmd) {
        RuntimeRegistry registry = RuntimeRegistry.getInstance();
        int threadID = ByteUtil.readObjectID(cmd.data, 0);
        Object thread = registry.getObject(threadID);
        if (thread == null || !(thread instanceof RuntimeThread)) {
            cmd = PacketFactory.changeToReply(cmd);
            cmd.errorCode = JDWP.Error.INVALID_OBJECT;
            return cmd;
        }
        RuntimeThread.suspend((RuntimeThread) thread);
        return PacketFactory.changeToReply(cmd);
    }

    /**
	 * Resumes a single thread which id is contained in the packet.
	 * 
	 * @see #resume(RuntimeThread)
	 */
    public Packet handleThreadReference$Resume(Packet cmd) {
        RuntimeRegistry registry = RuntimeRegistry.getInstance();
        int threadID = ByteUtil.readObjectID(cmd.data, 0);
        Object thread = registry.getObject(threadID);
        if (thread == null || !(thread instanceof RuntimeThread)) {
            cmd = PacketFactory.changeToReply(cmd);
            cmd.errorCode = JDWP.Error.INVALID_OBJECT;
            return cmd;
        }
        RuntimeThread.resume((RuntimeThread) thread);
        return PacketFactory.changeToReply(cmd);
    }
}
