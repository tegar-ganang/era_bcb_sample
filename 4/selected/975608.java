package org.mmtk.plan;

import org.mmtk.utility.Log;
import org.mmtk.utility.heap.HeapGrowthManager;
import org.mmtk.utility.options.Options;
import org.mmtk.vm.Monitor;
import org.mmtk.vm.VM;
import org.vmmagic.pragma.*;

@Uninterruptible
public class ControllerCollectorContext extends CollectorContext {

    /** The lock to use to manage collection */
    private Monitor lock;

    /** The set of worker threads to use */
    private ParallelCollectorGroup workers;

    /** Flag used to control the 'race to request' */
    private boolean requestFlag;

    /** The current request index */
    private int requestCount;

    /** The request index that was last completed */
    private int lastRequestCount = -1;

    /** Is there concurrent collection activity */
    private boolean concurrentCollection = false;

    /**
   * Create a controller context.
   *
   * @param workers The worker group to use for collection.
   */
    public ControllerCollectorContext(ParallelCollectorGroup workers) {
        this.workers = workers;
    }

    @Override
    @Interruptible
    public void initCollector(int id) {
        super.initCollector(id);
        lock = VM.newHeavyCondLock("CollectorControlLock");
    }

    /**
   * Main execution loop.
   */
    @Unpreemptible
    public void run() {
        while (true) {
            if (Options.verbose.getValue() >= 5) Log.writeln("[STWController: Waiting for request...]");
            waitForRequest();
            if (Options.verbose.getValue() >= 5) Log.writeln("[STWController: Request recieved.]");
            long startTime = VM.statistics.nanoTime();
            if (concurrentCollection) {
                if (Options.verbose.getValue() >= 5) Log.writeln("[STWController: Stopping concurrent collectors...]");
                Plan.concurrentWorkers.abortCycle();
                Plan.concurrentWorkers.waitForCycle();
                Phase.clearConcurrentPhase();
                concurrentCollection = false;
            }
            if (Options.verbose.getValue() >= 5) Log.writeln("[STWController: Stopping the world...]");
            VM.collection.stopAllMutators();
            boolean userTriggeredCollection = Plan.isUserTriggeredCollection();
            boolean internalTriggeredCollection = Plan.isInternalTriggeredCollection();
            clearRequest();
            if (Options.verbose.getValue() >= 5) Log.writeln("[STWController: Triggering worker threads...]");
            workers.triggerCycle();
            workers.waitForCycle();
            if (Options.verbose.getValue() >= 5) Log.writeln("[STWController: Worker threads complete!]");
            long elapsedTime = VM.statistics.nanoTime() - startTime;
            HeapGrowthManager.recordGCTime(VM.statistics.nanosToMillis(elapsedTime));
            if (VM.activePlan.global().lastCollectionFullHeap() && !internalTriggeredCollection) {
                if (Options.variableSizeHeap.getValue() && !userTriggeredCollection) {
                    if (Options.verbose.getValue() >= 5) Log.writeln("[STWController: Considering heap size.]");
                    HeapGrowthManager.considerHeapSize();
                }
                HeapGrowthManager.reset();
            }
            Plan.resetCollectionTrigger();
            if (Options.verbose.getValue() >= 5) Log.writeln("[STWController: Resuming mutators...]");
            VM.collection.resumeAllMutators();
            if (concurrentCollection) {
                if (Options.verbose.getValue() >= 5) Log.writeln("[STWController: Triggering concurrent collectors...]");
                Plan.concurrentWorkers.triggerCycle();
            }
        }
    }

    /**
   * Request that concurrent collection is performed after this stop-the-world increment.
   */
    public void requestConcurrentCollection() {
        concurrentCollection = true;
    }

    /**
   * Request a collection.
   */
    public void request() {
        if (requestFlag) {
            return;
        }
        lock.lock();
        if (!requestFlag) {
            requestFlag = true;
            requestCount++;
            lock.broadcast();
        }
        lock.unlock();
    }

    /**
   * Clear the collection request, making future requests incur an
   * additional collection cycle.
   */
    private void clearRequest() {
        lock.lock();
        requestFlag = false;
        lock.unlock();
    }

    /**
   * Wait until a request is received.
   */
    private void waitForRequest() {
        lock.lock();
        lastRequestCount++;
        while (lastRequestCount == requestCount) {
            lock.await();
        }
        lock.unlock();
    }
}
