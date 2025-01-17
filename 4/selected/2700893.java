package com.ibm.JikesRVM.mm.mmtk;

import com.ibm.JikesRVM.VM;
import org.vmmagic.unboxed.*;
import org.vmmagic.pragma.*;
import com.ibm.JikesRVM.VM_Entrypoints;
import com.ibm.JikesRVM.VM_Synchronization;
import com.ibm.JikesRVM.VM_Magic;
import com.ibm.JikesRVM.VM_Scheduler;
import com.ibm.JikesRVM.VM_Thread;
import com.ibm.JikesRVM.VM_Time;
import org.mmtk.utility.Log;

/**
 * Simple, fair locks with deadlock detection.
 *
 * The implementation mimics a deli-counter and consists of two values: 
 * the ticket dispenser and the now-serving display, both initially zero.
 * Acquiring a lock involves grabbing a ticket number from the dispenser
 * using a fetchAndIncrement and waiting until the ticket number equals
 * the now-serving display.  On release, the now-serving display is
 * also fetchAndIncremented.
 * 
 * This implementation relies on there being less than 1<<32 waiters.
 *
 * $Id: Lock.java,v 1.4 2006/06/23 07:16:19 steveb-oss Exp $
 * 
 * @author Perry Cheng
 * @version $Revision: 1.4 $
 * @date $Date: 2006/06/23 07:16:19 $
 */
public class Lock extends org.mmtk.vm.Lock implements Uninterruptible {

    private static Offset dispenserFieldOffset = VM_Entrypoints.dispenserField.getOffset();

    private static Offset servingFieldOffset = VM_Entrypoints.servingField.getOffset();

    private static Offset threadFieldOffset = VM_Entrypoints.lockThreadField.getOffset();

    private static Offset startFieldOffset = VM_Entrypoints.lockStartField.getOffset();

    private static long SLOW_THRESHOLD = Long.MAX_VALUE >> 1;

    private static long TIME_OUT = Long.MAX_VALUE;

    private static final boolean REPORT_SLOW = true;

    private static int TIMEOUT_CHECK_FREQ = 1000;

    public static final int verbose = 0;

    private static int lockCount = 0;

    private String name;

    private int id;

    private int dispenser;

    private int serving;

    private VM_Thread thread;

    private long start;

    private int where = -1;

    private int[] servingHistory = new int[100];

    private int[] tidHistory = new int[100];

    private long[] startHistory = new long[100];

    private long[] endHistory = new long[100];

    public static void fullyBooted() {
        SLOW_THRESHOLD = VM_Time.millisToCycles(200);
        TIME_OUT = 10 * SLOW_THRESHOLD;
    }

    public Lock(String name) {
        this();
        this.name = name;
    }

    public Lock() {
        dispenser = serving = 0;
        id = lockCount++;
    }

    public void setName(String str) {
        name = str;
    }

    public void acquire() {
        int ticket = VM_Synchronization.fetchAndAdd(this, dispenserFieldOffset, 1);
        int retryCountdown = TIMEOUT_CHECK_FREQ;
        long localStart = 0;
        long lastSlowReport = 0;
        while (ticket != serving) {
            if (localStart == 0) lastSlowReport = localStart = VM_Time.cycles();
            if (--retryCountdown == 0) {
                retryCountdown = TIMEOUT_CHECK_FREQ;
                long now = VM_Time.cycles();
                long lastReportDuration = now - lastSlowReport;
                long waitTime = now - localStart;
                if (lastReportDuration > SLOW_THRESHOLD + VM_Time.millisToCycles(200 * (VM_Thread.getCurrentThread().getIndex() % 5))) {
                    lastSlowReport = now;
                    Log.write("GC Warning: slow/deadlock - thread ");
                    writeThreadIdToLog(VM_Thread.getCurrentThread());
                    Log.write(" with ticket ");
                    Log.write(ticket);
                    Log.write(" failed to acquire lock ");
                    Log.write(id);
                    Log.write(" (");
                    Log.write(name);
                    Log.write(") serving ");
                    Log.write(serving);
                    Log.write(" after ");
                    Log.write(VM_Time.cyclesToMillis(waitTime));
                    Log.write(" ms");
                    Log.writelnNoFlush();
                    VM_Thread t = thread;
                    if (t == null) Log.writeln("GC Warning: Locking thread unknown", false); else {
                        Log.write("GC Warning: Locking thread: ");
                        writeThreadIdToLog(t);
                        Log.write(" at position ");
                        Log.writeln(where, false);
                    }
                    Log.write("GC Warning: my start = ");
                    Log.writeln(localStart, false);
                    for (int i = (serving + 90) % 100; i != (serving % 100); i = (i + 1) % 100) {
                        if (VM.VerifyAssertions) VM._assert(i >= 0 && i < 100);
                        Log.write("GC Warning: ");
                        Log.write(i);
                        Log.write(": index ");
                        Log.write(servingHistory[i]);
                        Log.write("   tid ");
                        Log.write(tidHistory[i]);
                        Log.write("    start = ");
                        Log.write(startHistory[i]);
                        Log.write("    end = ");
                        Log.write(endHistory[i]);
                        Log.write("    start-myStart = ");
                        Log.write(VM_Time.cyclesToMillis(startHistory[i] - localStart));
                        Log.writelnNoFlush();
                    }
                    Log.flush();
                }
                if (waitTime > TIME_OUT) {
                    Log.write("GC Warning: Locked out thread: ");
                    writeThreadIdToLog(VM_Thread.getCurrentThread());
                    Log.writeln();
                    VM_Scheduler.dumpStack();
                    VM.sysFail("Deadlock or someone holding on to lock for too long");
                }
            }
        }
        if (REPORT_SLOW) {
            servingHistory[serving % 100] = serving;
            tidHistory[serving % 100] = VM_Thread.getCurrentThread().getIndex();
            startHistory[serving % 100] = VM_Time.cycles();
            setLocker(VM_Time.cycles(), VM_Thread.getCurrentThread(), -1);
        }
        if (verbose > 1) {
            Log.write("Thread ");
            writeThreadIdToLog(thread);
            Log.write(" acquired lock ");
            Log.write(id);
            Log.write(" ");
            Log.write(name);
            Log.writeln();
        }
        VM_Magic.isync();
    }

    public void check(int w) {
        if (!REPORT_SLOW) return;
        if (VM.VerifyAssertions) VM._assert(VM_Thread.getCurrentThread() == thread);
        long diff = (REPORT_SLOW) ? VM_Time.cycles() - start : 0;
        boolean show = (verbose > 1) || (diff > SLOW_THRESHOLD);
        if (show) {
            Log.write("GC Warning: Thread ");
            writeThreadIdToLog(thread);
            Log.write(" reached point ");
            Log.write(w);
            Log.write(" while holding lock ");
            Log.write(id);
            Log.write(" ");
            Log.write(name);
            Log.write(" at ");
            Log.write(VM_Time.cyclesToMillis(diff));
            Log.writeln(" ms");
        }
        where = w;
    }

    public void release() {
        long diff = (REPORT_SLOW) ? VM_Time.cycles() - start : 0;
        boolean show = (verbose > 1) || (diff > SLOW_THRESHOLD);
        if (show) {
            Log.write("GC Warning: Thread ");
            writeThreadIdToLog(thread);
            Log.write(" released lock ");
            Log.write(id);
            Log.write(" ");
            Log.write(name);
            Log.write(" after ");
            Log.write(VM_Time.cyclesToMillis(diff));
            Log.writeln(" ms");
        }
        if (REPORT_SLOW) {
            endHistory[serving % 100] = VM_Time.cycles();
            setLocker(0, null, -1);
        }
        VM_Magic.sync();
        VM_Synchronization.fetchAndAdd(this, servingFieldOffset, 1);
    }

    private final void setLocker(long start, VM_Thread thread, int w) throws InlinePragma {
        VM_Magic.setLongAtOffset(this, startFieldOffset, start);
        VM_Magic.setObjectAtOffset(this, threadFieldOffset, (Object) thread);
        where = w;
    }

    /** Write thread <code>t</code>'s identifying info via the MMTk Log class.
   * Does not use any newlines, nor does it flush.
   *
   *  This function may be called during GC; it avoids write barriers and
   *  allocation. 
   *
   *  @param t  The {@link VM_Thread} we are interested in.
   */
    private static void writeThreadIdToLog(VM_Thread t) {
        char[] buf = VM_Thread.grabDumpBuffer();
        int len = t.dump(buf);
        Log.write(buf, len);
        VM_Thread.releaseDumpBuffer();
    }
}
