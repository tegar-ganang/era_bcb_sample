package org.apache.harmony.jpda.tests.jdwp.Events;

import org.apache.harmony.jpda.tests.framework.LogWriter;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;

public class MonitorContendedEnterAndEnteredDebuggee extends SyncDebuggee {

    static final String TESTED_THREAD = "BLOCKED_THREAD";

    static Object lock = new MonitorWaitMockMonitor();

    BlockedThread thread;

    public void run() {
        thread = new BlockedThread(logWriter, TESTED_THREAD);
        logWriter.println("--> Main thread : started");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        synchronized (lock) {
            synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
            logWriter.println("main thread: start tested thread");
            thread.start();
            while (!thread.getState().equals(Thread.State.valueOf("BLOCKED"))) {
                Thread.yield();
                logWriter.println("main thread: Waiting for second thread to attempt to lock a monitor");
            }
            logWriter.println("--> main thread: finish test");
        }
    }

    class BlockedThread extends Thread {

        private LogWriter logWriter;

        public BlockedThread(LogWriter writer, String name) {
            logWriter = writer;
            this.setName(name);
        }

        public void run() {
            logWriter.println("--> BlockedThread: start to run");
            synchronized (lock) {
                this.getName().trim();
                logWriter.println("--> BlockedThread: get lock");
            }
        }
    }

    public static void main(String[] args) {
        runDebuggee(MonitorContendedEnterAndEnteredDebuggee.class);
    }
}
