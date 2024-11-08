package com.sohlman.profiler;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import com.sohlman.profiler.reporter.Reporter;

/**
 * Thread local profiler is little library for profiling measuring performance
 * of applicatons.
 * 
 * It generates tree form report where it is easy to point out system
 * bottlenecks. It collects data to ThreadLocal variable, so with this is easy
 * find out what is taking time.
 * 
 * Design is not intrusive so no additional dependencies are required.
 * 
 * Usage measuring:
 * 
 * Watch watch = ThreadLocalProfiler.start(); ThreadLocalProfiler.stop(watch,
 * getClass(), "doIt","integration time");
 * 
 * Usage: report
 * 
 * Watch[] watches = ThreadLocalProfiler.report();
 * 
 * @author Sampsa Sohlman http://sampsa.sohlman.com
 * 
 */
public class ThreadLocalProfiler {

    private static ThreadLocal<ThreadLocalProfiler> threadProfiler = new ThreadLocal<ThreadLocalProfiler>();

    private Watch root;

    private Watch current;

    private int activeWatchCounter;

    private int watchCounter;

    private static volatile boolean isDisabled = false;

    private static AtomicReference<Reporter> reporterAtomicReference = new AtomicReference<Reporter>();

    private ThreadLocalProfiler() {
        activeWatchCounter = 0;
        watchCounter = 0;
    }

    public static void setReporter(Reporter reporter) {
        reporterAtomicReference.set(reporter);
    }

    public static void setIfNotSet(Reporter reporter) {
        reporterAtomicReference.compareAndSet(null, reporter);
    }

    private static ThreadLocalProfiler createThreadLocalProfiler(ThreadLocalProfiler profiler) {
        if (!isRunning(profiler)) {
            if (isDisabled()) {
                return null;
            } else {
                profiler = new ThreadLocalProfiler();
                threadProfiler.set(profiler);
                return profiler;
            }
        } else {
            return profiler;
        }
    }

    /**
	 * Disables or enables profiler <b>Note</b> this value is common for all
	 * threads.
	 */
    public static void setDisabled(boolean value) {
        isDisabled = value;
    }

    /**
	 * Tells if ThreadLocalProfiler is disabled <b>Note</b> this value is common
	 * for all threads.
	 * 
	 * @return boolean
	 */
    public static boolean isDisabled() {
        return isDisabled || reporterAtomicReference.get() == null;
    }

    /**
	 * @param text
	 * @return
	 */
    public static Watch start() {
        ThreadLocalProfiler profiler = threadProfiler.get();
        if (isRunning(profiler)) {
            return start(profiler);
        } else {
            return start(createThreadLocalProfiler(profiler));
        }
    }

    private static Watch start(ThreadLocalProfiler profiler) {
        if (profiler != null) {
            return profiler.doStart();
        } else {
            return null;
        }
    }

    private Watch doStart() {
        Watch watch;
        if (current == null) {
            watch = new Watch(null);
            root = watch;
            activeWatchCounter = 0;
            watchCounter = 0;
        } else {
            watch = new Watch(current);
            current.addChild(watch);
        }
        activeWatchCounter++;
        watchCounter++;
        current = watch;
        return watch;
    }

    public static boolean isRunning() {
        return isRunning(threadProfiler.get());
    }

    private static boolean isRunning(ThreadLocalProfiler profiler) {
        if (profiler == null) {
            return false;
        } else if (profiler.getRoot() == null) {
            return false;
        } else {
            return profiler.current != null;
        }
    }

    private void doStop(Watch watch, String className, String methodName, String text) {
        activeWatchCounter--;
        watch.stop(className, methodName, text);
        current = watch.getParent();
    }

    Watch getRoot() {
        return root;
    }

    public static void stop(Watch watch, String text) {
        stop(watch, (String) null, (String) null, text);
    }

    public static void stop(Watch watch, Class clazz, String methodName, String text) {
        stop(watch, clazz != null ? clazz.getName() : "<no class>", methodName, text);
    }

    public static void stop(Watch watch, Class clazz, String methodName) {
        stop(watch, clazz != null ? clazz.getName() : "<no class>", methodName, null);
    }

    public static void stop(Watch watch, Class clazz, Method method) {
        stop(watch, clazz != null ? clazz.getName() : "<no class>", method != null ? method.getName() : "<no method>", null);
    }

    public static void stop(Watch watch, String className, String methodName) {
        stop(watch, className, methodName, null);
    }

    public static void stop(Watch watch, String className, String methodName, String text) {
        if (watch == null) {
            return;
        }
        ThreadLocalProfiler profiler = threadProfiler.get();
        if (profiler == null) {
            return;
        }
        profiler.doStop(watch, className, methodName, text);
        if (watch.isRoot()) {
            writeReport(profiler);
            threadProfiler.remove();
        }
    }

    private static void writeReport(ThreadLocalProfiler profiler) {
        Reporter reporter = reporterAtomicReference.get();
        if (reporter != null) {
            reporter.report(report(profiler));
        }
    }

    private static Watch[] EMTPY_REPORT = new Watch[0];

    private static Watch[] report(ThreadLocalProfiler profiler) {
        if (profiler == null) {
            return EMTPY_REPORT;
        } else {
            return profiler.doReport();
        }
    }

    private Watch[] doReport() {
        if (watchCounter == 0) {
            return EMTPY_REPORT;
        } else {
            Watch[] watches = new Watch[watchCounter];
            int counter = 0;
            doReport(this.root, watches, counter);
            return watches;
        }
    }

    private int doReport(Watch watch, Watch[] watches, int counter) {
        do {
            watches[counter] = watch;
            counter++;
            if (watch.hasChilds()) {
                counter = doReport(watch.getFirstChild(), watches, counter);
            }
            watch = watch.getNext();
        } while (watch != null);
        return counter;
    }
}
