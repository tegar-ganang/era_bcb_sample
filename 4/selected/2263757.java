package com.kni.etl.ketl.smp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.LinkedBlockingQueue;
import org.w3c.dom.Node;
import com.kni.etl.dbutils.ResourcePool;
import com.kni.etl.ketl.ETLInPort;
import com.kni.etl.ketl.ETLStep;
import com.kni.etl.ketl.checkpointer.CheckPointStore;
import com.kni.etl.ketl.exceptions.KETLQAException;
import com.kni.etl.ketl.exceptions.KETLThreadException;
import com.kni.etl.ketl.exceptions.KETLTransformException;
import com.kni.etl.util.ExternalSort;
import com.kni.etl.util.XMLHelper;
import com.kni.etl.util.aggregator.Aggregator;
import com.kni.etl.util.aggregator.Direct;

/**
 * The Class ETLTransform.
 * 
 * @author nwakefield To change the template for this generated type comment go
 *         to Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and
 *         Comments
 */
public abstract class ETLTransform extends ETLStep {

    /** The aggregate out. */
    private final ArrayList aggregateOut = new ArrayList();

    /** The aggregates. */
    private Aggregator[] aggregates;

    /** The core. */
    private DefaultTransformCore core = null;

    /** alternate core that processes the batch differently */
    private BatchTransformCore batchCore = null;

    /** The sort data. */
    private boolean mAggregate = false, mSortData = false;

    /** The batch manager. */
    private TransformBatchManager mBatchManager;

    /** The default comparator. */
    private Comparator mDefaultComparator = null;

    /** The expected output data types. */
    private Class[] mExpectedInputDataTypes, mExpectedOutputDataTypes;

    /** The external sort. */
    private ExternalSort mExternalSort;

    /** The input record width. */
    private int mInputRecordWidth = -1;

    /** The output record width. */
    private int mOutputRecordWidth = -1;

    /** The post sort batch size. */
    int postSortBatchSize;

    /** The previous. */
    private Object[] previous = null;

    /** The queue. */
    ManagedBlockingQueue queue;

    /** The src queue. */
    private ManagedBlockingQueue srcQueue;

    private ManagedBlockingQueue interimQueue;

    /**
	 * The Constructor.
	 * 
	 * @param pXMLConfig
	 *            the XML config
	 * @param pPartitionID
	 *            the partition ID
	 * @param pPartition
	 *            the partition
	 * @param pThreadManager
	 *            the thread manager
	 * 
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    public ETLTransform(Node pXMLConfig, int pPartitionID, int pPartition, ETLThreadManager pThreadManager) throws KETLThreadException {
        super(pXMLConfig, pPartitionID, pPartition, pThreadManager);
        try {
            this.mCheckpointStore = new CheckPointStore(this);
        } catch (IOException e) {
            throw new KETLThreadException("Unable to create check point store", e, this);
        }
    }

    private CheckPointStore mCheckpointStore;

    /**
	 * Aggregate batch.
	 * 
	 * @param res
	 *            the res
	 * @param length
	 *            the length
	 * 
	 * @throws InterruptedException
	 *             the interrupted exception
	 */
    private void aggregateBatch(Object[][] res, int length) throws InterruptedException {
        Comparator cmp = this.mDefaultComparator;
        if (res == null && (this.aggregateOut.size() > 0 || this.previous != null)) {
            Object[] last = new Object[this.mOutputRecordWidth];
            for (int pos = 0; pos < this.mOutputRecordWidth; pos++) {
                last[pos] = this.aggregates[pos].getValue();
            }
            this.aggregateOut.add(last);
            this.sendAggregateBatch();
            return;
        }
        if (this.timing) this.startTimeNano = System.nanoTime();
        for (int i = 0; i < length; i++) {
            Object[] current = res[i];
            if (this.previous != null && cmp.compare(current, this.previous) != 0) {
                Object[] result = new Object[this.mOutputRecordWidth];
                for (int pos = 0; pos < this.mOutputRecordWidth; pos++) {
                    result[pos] = this.aggregates[pos].getValue();
                }
                this.aggregateOut.add(result);
                if (this.aggregateOut.size() >= length) {
                    if (this.timing) this.totalTimeNano += System.nanoTime() - this.startTimeNano;
                    this.sendAggregateBatch();
                    if (this.timing) this.startTimeNano = System.nanoTime();
                }
            }
            for (int pos = 0; pos < this.mOutputRecordWidth; pos++) {
                this.aggregates[pos].add(current[pos]);
            }
            this.previous = current;
        }
        if (this.timing) this.totalTimeNano += System.nanoTime() - this.startTimeNano;
    }

    @Override
    protected final void executeWorker() throws ClassNotFoundException, KETLThreadException, InterruptedException, IOException, KETLTransformException {
        if (this instanceof AggregatingTransform) {
            this.mAggregate = true;
            this.aggregates = ((AggregatingTransform) this).getAggregates();
            this.mDefaultComparator = this.getAggregateComparator();
        }
        LinkedBlockingQueue<Object> queue;
        if (this.timing) this.startTimeNano = System.nanoTime();
        queue = mCheckpointStore.processCheckPoint(this, this.getSourceQueue());
        if (this.timing) this.totalTimeNano += System.nanoTime() - this.startTimeNano;
        if (this.mSortData) {
            this.sortData(queue);
            this.transformFromSort();
        } else this.transformFromQueue(queue);
    }

    @Override
    protected CharSequence generateCoreHeader() {
        return " public class " + this.getCoreClassName() + " extends ETLTransformCore { ";
    }

    @Override
    protected String generateCoreImports() {
        return super.generateCoreImports() + "import com.kni.etl.ketl.smp.ETLTransformCore;\nimport java.util.Calendar;\n" + "import com.kni.etl.ketl.smp.ETLTransform;\n";
    }

    /**
	 * Gets the aggregate comparator.
	 * 
	 * @return the aggregate comparator
	 */
    private Comparator getAggregateComparator() {
        ArrayList res = new ArrayList();
        for (int i = 0; i < this.aggregates.length; i++) if (this.aggregates[i] instanceof Direct) {
            res.add(i);
        }
        Integer[] elements = new Integer[res.size()];
        res.toArray(elements);
        return new DefaultComparator(elements);
    }

    private int maxSortSize = 5000;

    private int maxMergeSize = 128;

    private int maxReadBufferSize = 4096;

    /**
	 * Gets the max sort size.
	 * 
	 * @return the max sort size
	 */
    private int getMaxSortSize() {
        return this.maxSortSize;
    }

    /**
	 * Gets the merge size.
	 * 
	 * @return the merge size
	 */
    private int getMergeSize() {
        return this.maxMergeSize;
    }

    /**
	 * Gets the read buffer size.
	 * 
	 * @return the read buffer size
	 */
    private int getReadBufferSize() {
        return this.maxReadBufferSize;
    }

    @Override
    protected String getRecordExecuteMethodFooter() {
        return " return SUCCESS;}";
    }

    @Override
    protected String getRecordExecuteMethodHeader() throws KETLThreadException {
        StringBuilder sb = new StringBuilder();
        sb.append("public int transformRecord(Object[] pInputRecords, " + "Class[] pInputDataTypes, int pInputRecordWidth, Object[] pOutputRecords" + " , Class[] pOutputDataTypes, int pOutputRecordWidth) throws KETLTransformException {");
        return sb.toString();
    }

    /**
	 * Gets the sort comparator.
	 * 
	 * @return the sort comparator
	 */
    protected Comparator getSortComparator() {
        ArrayList cols = new ArrayList();
        ArrayList order = new ArrayList();
        for (int i = 0; i < this.mInPorts.length; i++) switch(this.mInPorts[i].getSort()) {
            case ETLInPort.ASC:
                cols.add(i);
                order.add(true);
                break;
            case ETLInPort.DESC:
                cols.add(i);
                order.add(false);
                break;
        }
        Integer[] elements = new Integer[cols.size()];
        Boolean[] elementOrder = new Boolean[order.size()];
        cols.toArray(elements);
        order.toArray(elementOrder);
        return new DefaultComparator(elements, elementOrder);
    }

    /**
	 * Gets the source queue.
	 * 
	 * @return the source queue
	 */
    private ManagedBlockingQueue getSourceQueue() {
        return this.srcQueue;
    }

    private int totalReadBufferSize = 524288;

    /**
	 * Gets the total read buffer size.
	 * 
	 * @return the total read buffer size
	 */
    private int getTotalReadBufferSize() {
        return this.totalReadBufferSize;
    }

    private int writeBufferSize = 4096;

    /**
	 * Gets the write buffer size.
	 * 
	 * @return the write buffer size
	 */
    private int getWriteBufferSize() {
        return this.writeBufferSize;
    }

    /**
	 * Increment error count.
	 * 
	 * @param e
	 *            the e
	 * @param objects
	 *            the objects
	 * @param val
	 *            the val
	 * 
	 * @throws KETLTransformException
	 *             the KETL transform exception
	 */
    public final void incrementErrorCount(KETLTransformException e, Object[] objects, int val) throws KETLTransformException {
        try {
            if (e.getCause() instanceof InterruptedException) throw e;
            if (objects != null) this.logBadRecord(val, objects, e);
        } catch (IOException e1) {
            throw new KETLTransformException(e1);
        }
        try {
            super.incrementErrorCount(e, 1, val);
        } catch (Exception e1) {
            if (e1 instanceof KETLTransformException) throw (KETLTransformException) e1;
            throw new KETLTransformException(e1);
        }
    }

    @Override
    protected int initialize(Node xmlConfig) throws KETLThreadException {
        int res = super.initialize(xmlConfig);
        if (res != 0) return res;
        this.mSortData = XMLHelper.getAttributeAsBoolean(xmlConfig.getAttributes(), "SORT", false);
        this.maxMergeSize = XMLHelper.getAttributeAsInt(xmlConfig.getAttributes(), "SORTMERGESIZE", this.maxMergeSize);
        this.maxSortSize = XMLHelper.getAttributeAsInt(xmlConfig.getAttributes(), "SORTSIZE", this.maxSortSize);
        this.maxReadBufferSize = XMLHelper.getAttributeAsInt(xmlConfig.getAttributes(), "SORTREADBUFFER", this.maxReadBufferSize);
        this.totalReadBufferSize = XMLHelper.getAttributeAsInt(xmlConfig.getAttributes(), "SORTTOTALREADBUFFER", this.totalReadBufferSize);
        this.writeBufferSize = XMLHelper.getAttributeAsInt(xmlConfig.getAttributes(), "SORTWRITEBUFFER", this.writeBufferSize);
        return 0;
    }

    @Override
    public final void initializeQueues() {
        this.queue.registerWriter(this);
        this.getSourceQueue().registerReader(this);
    }

    /**
	 * Repeat record possible.
	 * 
	 * @return true, if successful
	 */
    protected boolean repeatRecordPossible() {
        return false;
    }

    /**
	 * Send aggregate batch.
	 * 
	 * @throws InterruptedException
	 *             the interrupted exception
	 */
    private void sendAggregateBatch() throws InterruptedException {
        Object[][] out = new Object[this.aggregateOut.size()][];
        this.aggregateOut.toArray(out);
        this.queue.put(out);
        this.aggregateOut.clear();
    }

    @Override
    protected final void setBatchManager(BatchManager batchManager) {
        this.mBatchManager = (TransformBatchManager) batchManager;
    }

    @Override
    final void setCore(DefaultCore newCore) {
        if (newCore instanceof BatchTransformCore) this.batchCore = (BatchTransformCore) newCore; else this.core = (DefaultTransformCore) newCore;
    }

    @Override
    void setOutputRecordDataTypes(Class[] pClassArray, String pChannel) {
        this.mExpectedOutputDataTypes = pClassArray;
        this.mOutputRecordWidth = this.mExpectedOutputDataTypes.length;
    }

    /**
	 * Sets the source queue.
	 * 
	 * @param srcQueue
	 *            the src queue
	 * @param worker
	 *            the worker
	 * 
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    void setSourceQueue(ManagedBlockingQueue srcQueue, ETLWorker worker) throws KETLThreadException {
        this.getUsedPortsFromWorker(worker, ETLWorker.getChannel(this.getXMLConfig(), ETLWorker.DEFAULT));
        this.srcQueue = srcQueue;
        try {
            this.mExpectedInputDataTypes = worker.getOutputRecordDatatypes(ETLWorker.getChannel(this.getXMLConfig(), ETLWorker.DEFAULT));
            this.mInputRecordWidth = this.mExpectedInputDataTypes.length;
        } catch (ClassNotFoundException e) {
            throw new KETLThreadException(e, this);
        }
        this.configureBufferSort(srcQueue);
    }

    @Override
    public void switchTargetQueue(ManagedBlockingQueue currentQueue, ManagedBlockingQueue newQueue) {
        this.queue = newQueue;
    }

    /**
	 * Sort data.
	 * 
	 * @throws InterruptedException
	 *             the interrupted exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws ClassNotFoundException
	 *             the class not found exception
	 * @throws KETLThreadException
	 */
    private void sortData(LinkedBlockingQueue queue) throws InterruptedException, IOException, ClassNotFoundException, KETLThreadException {
        this.postSortBatchSize = this.batchSize;
        this.mExternalSort = new ExternalSort(this.getSortComparator(), this.getMaxSortSize(), this.getMergeSize(), this.getTotalReadBufferSize(), this.getReadBufferSize(), this.getWriteBufferSize());
        ResourcePool.LogMessage(this, ResourcePool.INFO_MESSAGE, "Starting sort");
        while (true) {
            this.interruptExecution();
            Object o;
            o = queue.take();
            if (o == ETLWorker.ENDOBJ) {
                break;
            }
            if (this.timing) this.startTimeNano = System.nanoTime();
            Object[][] res = (Object[][]) o;
            this.postSortBatchSize = (res.length + this.postSortBatchSize) / 2;
            for (int i = res.length - 1; i > -1; i--) this.mExternalSort.add(res[i]);
            if (this.timing) this.totalTimeNano += System.nanoTime() - this.startTimeNano;
        }
        this.mExternalSort.commit();
        ResourcePool.LogMessage(this, ResourcePool.INFO_MESSAGE, "Sort complete, sort rate " + this.mExternalSort.sortRate() + " rec/s");
    }

    /** The count. */
    private int count = 0;

    /**
	 * Transform batch.
	 * 
	 * @param res
	 *            the res
	 * @param length
	 *            the length
	 * 
	 * @return the object[][]
	 * 
	 * @throws KETLTransformException
	 *             the KETL transform exception
	 * @throws KETLQAException
	 *             the KETLQA exception
	 */
    final Object[][] transformBatch(Object[][] res, int length) throws KETLTransformException, KETLQAException {
        int resultLength = 0;
        int outputArraySize = length;
        boolean newDataArray = true;
        Object[][] data = res;
        for (int i = 0; i < length; i++) {
            Object[] result = new Object[this.mOutputRecordWidth];
            if (this.mMonitor) ResourcePool.LogMessage(this, ResourcePool.DEBUG_MESSAGE, "[" + (this.count) + "][IN][" + i + "]" + java.util.Arrays.toString(res[i]));
            int code;
            try {
                code = this.core.transformRecord(res[i], this.mExpectedInputDataTypes, this.mInputRecordWidth, result, this.mExpectedOutputDataTypes, this.mOutputRecordWidth);
            } catch (KETLTransformException e) {
                this.recordCheck(result, e);
                this.incrementErrorCount(e, res[i], 1);
                code = DefaultTransformCore.SKIP_RECORD;
            }
            switch(code) {
                case DefaultTransformCore.SUCCESS:
                    if (this.mMonitor) ResourcePool.LogMessage(this, ResourcePool.DEBUG_MESSAGE, "[" + (this.count++) + "][OUT][" + i + "]" + java.util.Arrays.toString(result));
                    this.recordCheck(result, null);
                    if (resultLength == outputArraySize) {
                        outputArraySize = outputArraySize + ((outputArraySize + 2) / 2);
                        Object[][] newResult = new Object[outputArraySize][];
                        System.arraycopy(data, 0, newResult, 0, resultLength);
                        data = newResult;
                    }
                    data[resultLength++] = result;
                    break;
                case DefaultTransformCore.SKIP_RECORD:
                    break;
                case DefaultTransformCore.REPEAT_RECORD:
                    if (resultLength == outputArraySize) {
                        outputArraySize = outputArraySize + ((outputArraySize + 2) / 2);
                        Object[][] newResult = new Object[outputArraySize][];
                        System.arraycopy(data, 0, newResult, 0, resultLength);
                        data = newResult;
                    }
                    if (newDataArray) {
                        Object[][] tmp = new Object[length][];
                        System.arraycopy(data, 0, tmp, 0, resultLength);
                        data = tmp;
                        newDataArray = false;
                    }
                    data[resultLength++] = result;
                    i--;
                    break;
                default:
                    throw new KETLTransformException("Invalid return code, check previous error message", code);
            }
        }
        if (resultLength != length) {
            Object[][] newResult = new Object[resultLength][];
            System.arraycopy(data, 0, newResult, 0, resultLength);
            data = newResult;
        }
        return data;
    }

    /**
	 * Transform from queue.
	 * 
	 * @throws InterruptedException
	 *             the interrupted exception
	 * @throws KETLTransformException
	 *             the KETL transform exception
	 * @throws KETLThreadException
	 */
    private final void transformFromQueue(LinkedBlockingQueue sourceQueue) throws InterruptedException, KETLTransformException, KETLThreadException {
        if (this.batchCore != null) {
            this.batchCore.iterableTransform(sourceQueue, this.queue);
        } else {
            while (true) {
                this.interruptExecution();
                Object o;
                o = sourceQueue.take();
                if (o == ETLWorker.ENDOBJ) {
                    if (this.mAggregate) {
                        this.aggregateBatch(null, -1);
                    }
                    if (this.mBatchManagement) {
                        this.mBatchManager.finishBatch(null, BatchManager.LASTBATCH);
                    }
                    this.queue.put(o);
                    break;
                }
                Object[][] res = (Object[][]) o;
                if (this.mBatchManagement) {
                    res = this.mBatchManager.initializeBatch(res, res.length);
                }
                if (this.timing) this.startTimeNano = System.nanoTime();
                res = this.transformBatch(res, res.length);
                if (this.timing) this.totalTimeNano += System.nanoTime() - this.startTimeNano;
                if (this.mBatchManagement) {
                    res = this.mBatchManager.finishBatch(res, res.length);
                }
                if (this.mAggregate) {
                    this.aggregateBatch(res, res.length);
                } else this.queue.put(res);
                this.updateThreadStats(res.length);
            }
        }
    }

    /**
	 * Transform from sort.
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws ClassNotFoundException
	 *             the class not found exception
	 * @throws InterruptedException
	 *             the interrupted exception
	 * @throws KETLTransformException
	 *             the KETL transform exception
	 * @throws KETLThreadException
	 */
    private void transformFromSort() throws IOException, ClassNotFoundException, InterruptedException, KETLTransformException, KETLThreadException {
        boolean readData = true;
        while (true) {
            this.interruptExecution();
            Object o = null;
            if (this.timing) this.startTimeNano = System.nanoTime();
            Object[][] data = readData ? new Object[this.postSortBatchSize][] : null;
            if (readData) {
                for (int i = 0; i < this.postSortBatchSize; i++) {
                    o = this.mExternalSort.getNext();
                    if (o == null) {
                        readData = false;
                        Object[][] tmp = new Object[i][];
                        System.arraycopy(data, 0, tmp, 0, i);
                        data = tmp;
                        this.postSortBatchSize = i;
                        break;
                    }
                    data[i] = (Object[]) o;
                }
            }
            if (this.timing) this.totalTimeNano += System.nanoTime() - this.startTimeNano;
            if (data == null) {
                if (this.mAggregate) {
                    this.aggregateBatch(null, -1);
                }
                this.queue.put(ETLWorker.ENDOBJ);
                break;
            }
            Object[][] res;
            if (this.timing) this.startTimeNano = System.nanoTime();
            res = this.transformBatch(data, this.postSortBatchSize);
            if (this.timing) this.totalTimeNano += System.nanoTime() - this.startTimeNano;
            if (this.mAggregate) {
                this.aggregateBatch(res, res.length);
            } else this.queue.put(res);
            this.updateThreadStats(res.length);
        }
    }

    @Override
    final String getDefaultExceptionClass() {
        return KETLTransformException.class.getCanonicalName();
    }

    /**
	 * Gets the expected input data types.
	 * 
	 * @return the expected input data types
	 */
    protected final Class[] getExpectedInputDataTypes() {
        return this.mExpectedInputDataTypes;
    }
}
