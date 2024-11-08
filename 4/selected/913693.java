package org.python.core;

/**
 * 
 * @deprecated Java 1 support is deprecated -- remove.
 *
 */
class ThreadStateMapping {

    private static boolean checkedJava2 = false;

    public static ThreadStateMapping makeMapping() {
        if (!checkedJava2) {
            checkedJava2 = true;
            String version = System.getProperty("java.version");
            if (version.compareTo("1.2") >= 0) {
                try {
                    Class c = Class.forName("org.python.core.ThreadStateMapping2");
                    return (ThreadStateMapping) c.newInstance();
                } catch (Throwable t) {
                }
            }
        }
        return new ThreadStateMapping();
    }

    private static java.util.Hashtable threads;

    private static ThreadState cachedThreadState;

    private static int additionCounter = 0;

    private static final int MAX_ADDITIONS = 25;

    public ThreadState getThreadState(PySystemState newSystemState) {
        Thread t = Thread.currentThread();
        ThreadState ts = cachedThreadState;
        if (ts != null && ts.thread == t) {
            return ts;
        }
        if (threads == null) {
            threads = new java.util.Hashtable();
        }
        ts = (ThreadState) threads.get(t);
        if (ts == null) {
            if (newSystemState == null) {
                Py.writeDebug("threadstate", "no current system state");
                newSystemState = Py.defaultSystemState;
            }
            ts = new ThreadState(t, newSystemState);
            threads.put(t, ts);
            additionCounter++;
            if (additionCounter > MAX_ADDITIONS) {
                cleanupThreadTable();
                additionCounter = 0;
            }
        }
        cachedThreadState = ts;
        return ts;
    }

    /**
     * Enumerates through the thread table looking for dead thread references
     * and removes them. Called internally by getThreadState(PySystemState).
     */
    private void cleanupThreadTable() {
        for (java.util.Enumeration e = threads.keys(); e.hasMoreElements(); ) {
            try {
                Object key = e.nextElement();
                ThreadState tempThreadState = (ThreadState) threads.get(key);
                if ((tempThreadState != null) && (tempThreadState.thread != null) && !tempThreadState.thread.isAlive()) {
                    threads.remove(key);
                }
            } catch (ClassCastException exc) {
            }
        }
    }
}
