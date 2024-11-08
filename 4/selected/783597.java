package com.kni.etl.ketl.smp;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Element;
import com.kni.etl.ETLJob;
import com.kni.etl.ETLJobStatus;
import com.kni.etl.ETLStatus;
import com.kni.etl.dbutils.ResourcePool;
import com.kni.etl.ketl.ETLStep;
import com.kni.etl.ketl.KETLJobExecutor;
import com.kni.etl.ketl.checkpointer.CheckPointStore;
import com.kni.etl.ketl.exceptions.KETLThreadException;
import com.kni.etl.util.XMLHelper;

/**
 * The Class ETLThreadManager.
 * 
 * @author nwakefield To change the template for this generated type comment go to Window&gt;Preferences&gt;Java&gt;Code
 *         Generation&gt;Code and Comments
 */
public class ETLThreadManager {

    /** The threads. */
    ArrayList<WorkerThread> threads = new ArrayList<WorkerThread>();

    /**
	 * The Class WorkerThread.
	 */
    class WorkerThread {

        /** The thread. */
        Thread thread;

        /** The step. */
        ETLWorker step;
    }

    /** The duplicate check. */
    private HashSet duplicateCheck = new HashSet();

    /** The mkj executor. */
    KETLJobExecutor mkjExecutor;

    /** The job thread group. */
    ThreadGroup jobThreadGroup;

    /**
	 * Instantiates a new ETL thread manager.
	 * 
	 * @param executor
	 *            the executor
	 */
    public ETLThreadManager(KETLJobExecutor executor) {
        this.mkjExecutor = executor;
        this.jobThreadGroup = new ThreadGroup(executor.getCurrentETLJob().getJobID());
    }

    /**
	 * Adds the step.
	 * 
	 * @param es
	 *            the es
	 */
    public void addStep(ETLWorker es) {
        if (this.duplicateCheck.contains(es)) return;
        this.addStep(es, es.getClass().getName());
        this.duplicateCheck.add(es);
    }

    /**
	 * Gets the step.
	 * 
	 * @param sourceStep
	 *            the source step
	 * @param name
	 *            the name
	 * @return the step
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    public synchronized ETLStats getStep(ETLWorker sourceStep, String name) throws KETLThreadException {
        for (Object o : this.threads) {
            WorkerThread wt = (WorkerThread) o;
            if (wt.step.getName().equals(name)) {
                if (wt.step.partitions == 1) return wt.step;
                if (wt.step.partitions == sourceStep.partitions && wt.step.partitionID == sourceStep.partitionID) return wt.step;
                if (wt.step.partitions != sourceStep.partitions) {
                    throw new KETLThreadException("Cannot get target step if parallism is greater than 1 and does not match source step, check steps Source: " + sourceStep.getName() + ",Target: " + wt.step.getName(), sourceStep);
                }
            }
        }
        throw new KETLThreadException("Could not find step " + name, sourceStep);
    }

    public synchronized List<ETLWorker> getFellowWorkers(String name) {
        List<ETLWorker> workers = new ArrayList<ETLWorker>();
        for (Object o : this.threads) {
            WorkerThread wt = (WorkerThread) o;
            if (wt.step.getName().equals(name)) {
                workers.add(wt.step);
            }
        }
        return workers;
    }

    /**
	 * Request queue.
	 * 
	 * @param queueSize
	 *            the queue size
	 * @return the managed blocking queue
	 */
    public ManagedBlockingQueue requestQueue(int queueSize) {
        return new ManagedBlockingQueueImpl(queueSize);
    }

    /**
	 * Adds the step.
	 * 
	 * @param es
	 *            the es
	 * @param name
	 *            the name
	 */
    private void addStep(ETLWorker es, String name) {
        WorkerThread wt = new WorkerThread();
        wt.thread = new Thread(this.jobThreadGroup, es);
        wt.step = es;
        this.threads.add(wt);
        wt.thread.setName(es.getName() + ", Type:" + name + " [" + (es.partitionID + 1) + " of " + es.partitions + "]");
    }

    public ThreadGroup getJobThreadGroup() {
        return jobThreadGroup;
    }

    /** The previous time. */
    private long startTime, previousTime;

    /** The previous writer records. */
    private int previousReaderRecords = 0, previousWriterRecords = 0;

    /** The Constant flowTypes. */
    public static final String[] flowTypes = { "FANIN", "FANOUT", "PIPELINE" };

    /** The Constant flowTypeMappings. */
    static final int[] flowTypeMappings = { ETLThreadGroup.FANIN, ETLThreadGroup.FANOUT, ETLThreadGroup.PIPELINE };

    /**
	 * Start.
	 * 
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    public void start() throws KETLThreadException {
        this.startTime = System.currentTimeMillis();
        this.previousTime = this.startTime;
        ResourcePool.LogMessage(Thread.currentThread(), ResourcePool.DEBUG_MESSAGE, "- Initializing threads");
        ResourcePool.LogMessage(Thread.currentThread(), ResourcePool.DEBUG_MESSAGE, "- Registering queues");
        for (Object o : this.threads) {
            ((WorkerThread) o).step.initializeQueues();
        }
        ResourcePool.LogMessage(Thread.currentThread(), ResourcePool.DEBUG_MESSAGE, "- Initializing core managers");
        boolean metFirstTransformer = false;
        ETLTransform lastTransformer = null;
        for (WorkerThread workerThread : this.threads) {
            try {
                ETLStep step = (ETLStep) (workerThread).step;
                if (step.isUseCheckPoint() && step instanceof ETLTransform && CheckPointStore.wasTheStepExecutedSuccessfully(step)) {
                    metFirstTransformer = true;
                    lastTransformer = (ETLTransform) step;
                }
                if ((step.isUseCheckPoint() || step instanceof ETLReader || step instanceof ETLTransform) && CheckPointStore.wasTheStepExecutedSuccessfully(step)) step.setWasPreviouslyRun(true);
                step.initialize(this.mkjExecutor);
            } catch (Throwable e) {
                if (e instanceof KETLThreadException) throw (KETLThreadException) e;
                throw new KETLThreadException(e.getMessage(), e);
            }
        }
        if (metFirstTransformer) {
            for (WorkerThread workerThread : this.threads) {
                ETLStep step = (ETLStep) workerThread.step;
                if (step.getName().equals(lastTransformer.getName())) step.setWasPreviouslyRun(false);
            }
        }
        ResourcePool.LogMessage(Thread.currentThread(), ResourcePool.DEBUG_MESSAGE, "- Compiling and instantiating cores");
        for (Object o : this.threads) {
            ((WorkerThread) o).step.compile();
        }
        ResourcePool.LogMessage(Thread.currentThread(), ResourcePool.DEBUG_MESSAGE, "- Starting threads");
        synchronized (this) {
            for (Object o : this.threads) {
                ((WorkerThread) o).thread.start();
            }
        }
        ResourcePool.LogMessage(Thread.currentThread(), ResourcePool.DEBUG_MESSAGE, "Threads initialized");
    }

    /**
	 * Monitor.
	 * 
	 * @param sleepTime
	 *            the sleep time
	 * @throws Throwable
	 *             the throwable
	 */
    public void monitor(int sleepTime) throws Throwable {
        this.monitor(sleepTime, sleepTime);
    }

    /** The detailed. */
    boolean detailed = true;

    /**
	 * Monitor.
	 * 
	 * @param sleepTime
	 *            the sleep time
	 * @param maxTime
	 *            the max time
	 * @throws Throwable
	 *             the throwable
	 */
    public void monitor(int sleepTime, int maxTime) throws Throwable {
        this.monitor(sleepTime, maxTime, null);
    }

    /**
	 * Final status.
	 * 
	 * @param jsJobStatus
	 *            the js job status
	 * @return the string
	 */
    public String finalStatus(ETLJobStatus jsJobStatus) {
        long recordWriterCount = 0, recordReaderCount = 0, recordReadErrorCount = 0, recordWriteErrorCount = 0;
        long currentTime = System.currentTimeMillis();
        for (WorkerThread o : this.threads) {
            if (o.step instanceof ETLReader) {
                recordReaderCount += o.step.getRecordsProcessed();
                recordReadErrorCount += ((ETLStep) o.step).getErrorCount();
            } else if (o.step instanceof ETLWriter) {
                recordWriterCount += o.step.getRecordsProcessed();
                recordReadErrorCount += ((ETLStep) o.step).getErrorCount();
            }
            jsJobStatus.setStats(o.step.getName(), o.step.partitions, o.step.partitionID, recordReaderCount, recordWriterCount, recordReadErrorCount, recordWriteErrorCount, o.step.getCPUTiming());
        }
        long allTimeDiff = currentTime - this.startTime;
        long prevTimeDiff = currentTime - this.previousTime;
        StringBuilder sb = new StringBuilder("Final Throughput Statistics(Records Per Second)\n");
        long recordDiff = recordReaderCount - this.previousReaderRecords;
        sb.append("\tOverall Read: " + recordReaderCount / ((allTimeDiff / 1000) + 1) + "\n");
        jsJobStatus.setStats(recordReaderCount, recordWriterCount, recordReadErrorCount, recordWriteErrorCount, allTimeDiff);
        sb.append("\tAverage Read: " + recordDiff / ((prevTimeDiff / 1000) + 1) + "\n");
        sb.append("\tTotal Records Read: " + recordReaderCount + "\n");
        recordDiff = recordWriterCount - this.previousWriterRecords;
        sb.append("\tOverall Write: " + recordWriterCount / ((allTimeDiff / 1000) + 1) + "\n");
        sb.append("\tAverage Write: " + recordDiff / ((prevTimeDiff / 1000) + 1) + "\n");
        sb.append("\tTotal Records Written: " + recordWriterCount + "\n");
        sb.append("\tThread Statistics\n\t----------------------------------------\n");
        for (Object o : this.threads) {
            ETLWorker es = ((WorkerThread) o).step;
            sb.append("\t" + ((WorkerThread) o).thread.getName() + ": " + es.getRecordsProcessed() + ", errors: " + ((ETLStep) es).getErrorCount() + ", timing: " + es.getTiming() + "\n");
        }
        jsJobStatus.setExtendedMessage("Total records read: " + recordReaderCount + ", Total records written: " + recordWriterCount);
        return sb.toString();
    }

    /**
	 * Gets the threading type.
	 * 
	 * @param config
	 *            the config
	 * @return the threading type
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    public static int getThreadingType(Element config) throws KETLThreadException {
        int res = Arrays.binarySearch(ETLThreadManager.flowTypes, XMLHelper.getAttributeAsString(config.getAttributes(), "FLOWTYPE", ETLThreadManager.flowTypes[2]));
        if (res < 0) throw new KETLThreadException("Invalid flow type, valid values are - " + Arrays.toString(ETLThreadManager.flowTypes), Thread.currentThread());
        return ETLThreadManager.flowTypeMappings[res];
    }

    /**
	 * Gets the stack trace.
	 * 
	 * @param aThrowable
	 *            the a throwable
	 * @return the stack trace
	 */
    public static String getStackTrace(Throwable aThrowable) {
        Writer result = new StringWriter();
        PrintWriter printWriter = new PrintWriter(result);
        aThrowable.printStackTrace(printWriter);
        return result.toString();
    }

    /**
	 * Close.
	 * 
	 * @param eJob
	 *            the e job
	 */
    public void close(ETLJob eJob) {
        StringBuilder sb = new StringBuilder();
        boolean errorsOccured = false;
        Throwable cause = this.mkjExecutor.getCurrentETLJob().getStatus().getException();
        if (cause != null) {
            sb.append("\n\nCause: " + cause.toString() + "\n" + ETLThreadManager.getStackTrace(cause));
            sb.append("\n\nTrace\n------\n");
        }
        for (Object o : this.threads) {
            WorkerThread wt = (WorkerThread) o;
            if (wt.step != null) {
                wt.step.closeStep(wt.step.success(), cause == null);
                if (!wt.step.success()) {
                    errorsOccured = true;
                    ArrayList a = ((ETLStep) wt.step).getLog();
                    for (int x = 0; x < a.size(); x++) {
                        Object[] tmp = (Object[]) a.get(x);
                        java.util.Date dt = (java.util.Date) tmp[1];
                        String msg = null;
                        String extMsg = "";
                        sb.append("Step - " + ((ETLStep) wt.step).toString() + "\n");
                        if (tmp[0] instanceof Exception) {
                            if (tmp[0] == cause) {
                                sb.append("\t" + x + " - see cause\n\n");
                                continue;
                            }
                            msg = ((Exception) tmp[0]).getMessage();
                            extMsg = "See trace in log";
                        } else if (tmp[0] != null) {
                            msg = tmp[0].toString();
                        }
                        if (msg != null) sb.append("\t" + x + " - [" + dt.toString() + "]" + msg.replace("\t", "\t\t") + "\n\n");
                        if (ResourcePool.getMetadata() != null) {
                            ResourcePool.getMetadata().recordJobMessage(eJob, (ETLStep) wt.step, eJob.getStatus().getErrorCode(), ResourcePool.ERROR_MESSAGE, msg, extMsg, false, dt);
                        }
                    }
                }
            }
        }
        if (ResourcePool.getMetadata() != null) {
            if (errorsOccured == true) {
                eJob.getStatus().setExtendedMessage(sb.toString());
            }
        }
    }

    /**
	 * Gets the job executor.
	 * 
	 * @return the job executor
	 */
    public KETLJobExecutor getJobExecutor() {
        return this.mkjExecutor;
    }

    /**
	 * Monitor.
	 * 
	 * @param sleepTime
	 *            the sleep time
	 * @param maxTime
	 *            the max time
	 * @param jsJobStatus
	 *            the js job status
	 * @throws Throwable
	 *             the throwable
	 */
    public void monitor(int sleepTime, int maxTime, ETLStatus jsJobStatus) throws Throwable {
        boolean state = true;
        Throwable failureException = null;
        boolean interruptAllThreads = false;
        while (state) {
            state = false;
            int recordWriterCount = 0, recordReaderCount = 0;
            boolean showStatus = false;
            long currentTime = System.currentTimeMillis();
            if (this.getJobExecutor().getCurrentETLJob().isCancelled()) {
                interruptAllThreads = true;
                ResourcePool.LogMessage(this, ResourcePool.WARNING_MESSAGE, "Cancelling job");
                this.getJobExecutor().getCurrentETLJob().cancelSuccessfull(true);
            }
            for (Object o : this.threads) {
                if (interruptAllThreads == false) {
                    if ((interruptAllThreads = ((WorkerThread) o).step.failAll())) {
                        ResourcePool.LogMessage(this, ResourcePool.ERROR_MESSAGE, "Critical job failure all steps being interrupted");
                    }
                }
                if (((WorkerThread) o).thread.isAlive()) {
                    state = true;
                    if (interruptAllThreads) ((WorkerThread) o).thread.interrupt();
                } else {
                    if (((WorkerThread) o).step.success() == false) {
                        failureException = this.mkjExecutor.getCurrentETLJob().getStatus().getException();
                        if (failureException == null) {
                            failureException = new KETLThreadException("Unknown failure, exception not received from step " + ((WorkerThread) o).step.getName(), this);
                            this.mkjExecutor.getCurrentETLJob().getStatus().setException(failureException);
                            this.mkjExecutor.getCurrentETLJob().getStatus().setErrorCode(-1);
                        }
                    }
                    if (interruptAllThreads == false && ((WorkerThread) o).step.cleanShutdown() == false) {
                        interruptAllThreads = true;
                        ResourcePool.LogMessage(this, ResourcePool.ERROR_MESSAGE, "Critical job failure all steps being interrupted");
                    }
                }
                if (currentTime - this.previousTime > 10000) {
                    showStatus = true;
                    if (((WorkerThread) o).step instanceof ETLReader) {
                        recordReaderCount += ((WorkerThread) o).step.getRecordsProcessed();
                    } else if (((WorkerThread) o).step instanceof ETLWriter) {
                        recordWriterCount += ((WorkerThread) o).step.getRecordsProcessed();
                    }
                }
            }
            if (showStatus) {
                long allTimeDiff = currentTime - this.startTime;
                long prevTimeDiff = currentTime - this.previousTime;
                if (jsJobStatus == null) {
                    StringBuilder sb = new StringBuilder("Current Throughput Statistics(Records Per Second)\n");
                    int recordDiff = recordReaderCount - this.previousReaderRecords;
                    sb.append("\tOverall Read: " + recordReaderCount / (allTimeDiff / 1000) + "\n");
                    sb.append("\tAverage Read: " + recordDiff / (prevTimeDiff / 1000) + "\n");
                    sb.append("\tTotal Records Read: " + recordReaderCount + "\n");
                    recordDiff = recordWriterCount - this.previousWriterRecords;
                    sb.append("\tOverall Write: " + recordWriterCount / (allTimeDiff / 1000) + "\n");
                    sb.append("\tAverage Write: " + recordDiff / (prevTimeDiff / 1000) + "\n");
                    sb.append("\tTotal Records Written: " + recordWriterCount + "\n");
                    sb.append("\tThread Statistics\n\t----------------------------------------\n");
                    for (Object o : this.threads) {
                        ETLWorker es = ((WorkerThread) o).step;
                        if (es.isWaiting()) sb.append("\t" + ((WorkerThread) o).thread.getName() + ": Waiting for " + es.waitingFor() + "\n"); else sb.append("\t" + ((WorkerThread) o).thread.getName() + ": " + es.getRecordsProcessed() + ", errors: " + ((ETLStep) es).getErrorCount() + (es.getTiming() == null || es.getTiming().equals("N/A") ? "" : ", timing: " + es.getTiming()) + (((WorkerThread) o).thread.isAlive() ? "" : ", Complete") + "\n");
                    }
                    if (this.mkjExecutor != null && this.mkjExecutor.getCurrentETLJob() != null) {
                        this.mkjExecutor.getCurrentETLJob().getStatus().setExtendedMessage(sb.toString());
                    }
                    ResourcePool.LogMessage(Thread.currentThread(), ResourcePool.DEBUG_MESSAGE, sb.toString());
                } else {
                    String waiting = "";
                    for (Object o : this.threads) {
                        ETLWorker es = ((WorkerThread) o).step;
                        if (es.isWaiting()) waiting = ", " + ((WorkerThread) o).thread.getName() + ": Waiting for " + es.waitingFor();
                    }
                    jsJobStatus.setExtendedMessage("Records read: " + recordReaderCount + ", Records written: " + recordWriterCount + waiting);
                }
                this.previousTime = currentTime;
                this.previousReaderRecords = recordReaderCount;
                this.previousWriterRecords = recordWriterCount;
                showStatus = false;
            }
            Thread.sleep(sleepTime);
            if (sleepTime < maxTime) sleepTime += sleepTime; else if (sleepTime > maxTime) sleepTime = maxTime;
        }
        failureException = this.mkjExecutor.getCurrentETLJob().getStatus().getException();
        if (failureException != null) throw failureException;
    }

    /**
	 * Count of step threads alive.
	 * 
	 * @param writer
	 *            the writer
	 * @return the int
	 */
    public int countOfStepThreadsAlive(ETLStep writer) {
        int cnt = 0;
        for (Object o : this.threads) {
            WorkerThread wrk = (WorkerThread) o;
            if (wrk.thread.isAlive() && wrk.step.mstrName.endsWith(writer.getName())) cnt++;
        }
        return cnt;
    }
}
