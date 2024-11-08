package com.kni.etl.ketl.smp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Node;
import com.kni.etl.ketl.ETLStep;
import com.kni.etl.ketl.exceptions.KETLThreadException;
import com.kni.etl.ketl.exceptions.KETLTransformException;

/**
 * The Class ETLSplit.
 * 
 * @author nwakefield To change the template for this generated type comment go to Window&gt;Preferences&gt;Java&gt;Code
 * Generation&gt;Code and Comments
 */
public abstract class ETLSplit extends ETLStep {

    /** The core. */
    private DefaultSplitCore core;

    /** The queue. */
    ManagedBlockingQueue queue[];

    /** The src queue. */
    private ManagedBlockingQueue srcQueue;

    /** The queues. */
    int queues;

    /** The channel list. */
    List channelList = new ArrayList();

    /** The channel map. */
    Map<String, Integer> channelMap = new HashMap();

    @Override
    protected CharSequence generateCoreHeader() {
        return " public class " + this.getCoreClassName() + " extends ETLSplitCore { ";
    }

    @Override
    final String getDefaultExceptionClass() {
        return KETLTransformException.class.getCanonicalName();
    }

    @Override
    protected String generateCoreImports() {
        return super.generateCoreImports() + "import com.kni.etl.ketl.smp.ETLSplitCore;\n" + "import com.kni.etl.ketl.smp.ETLSplit;\n";
    }

    /**
     * Increment error count.
     * 
     * @param e the e
     * @param objects the objects
     * @param val the val
     * 
     * @throws KETLTransformException the KETL transform exception
     */
    public final void incrementErrorCount(KETLTransformException e, Object[] objects, int val) throws KETLTransformException {
        try {
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

    /**
     * The Constructor.
     * 
     * @param pXMLConfig the XML config
     * @param pPartitionID the partition ID
     * @param pPartition the partition
     * @param pThreadManager the thread manager
     * 
     * @throws KETLThreadException the KETL thread exception
     */
    public ETLSplit(Node pXMLConfig, int pPartitionID, int pPartition, ETLThreadManager pThreadManager) throws KETLThreadException {
        super(pXMLConfig, pPartitionID, pPartition, pThreadManager);
    }

    @Override
    void setOutputRecordDataTypes(Class[] pClassArray, String pChannel) {
        this.channelMap.put(pChannel, this.channelList.size());
        this.channelList.add(pClassArray);
    }

    private int[] channelPath;

    @Override
    protected String generatePortMappingCode() throws KETLThreadException {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getRecordExecuteMethodHeader() + "\n");
        if (this.mOutPorts != null) {
            sb.append("switch(pOutPath){\n");
            for (Map.Entry<String, Integer> entry : this.channelMap.entrySet()) {
                String channel = (String) entry.getKey();
                sb.append("case " + (entry.getValue()) + ": {\n");
                for (int i = 0; i < this.mOutPorts.length; i++) {
                    if (this.mOutPorts[i].getChannel().equals(channel)) {
                        sb.append(this.mOutPorts[i].generateCode(i));
                        sb.append(";\n");
                    }
                }
                sb.append("} break;\n");
            }
            sb.append("}\n");
        }
        sb.append(this.getRecordExecuteMethodFooter() + "\n");
        return sb.toString();
    }

    @Override
    final void setCore(DefaultCore newCore) {
        this.core = (DefaultSplitCore) newCore;
    }

    /** The batch manager. */
    protected SplitBatchManager mBatchManager;

    @Override
    protected final void executeWorker() throws InterruptedException, KETLTransformException, KETLThreadException {
        this.queues = this.queue.length;
        this.mOutputRecordWidth = new int[this.queues];
        java.util.Arrays.fill(this.mOutputRecordWidth, -1);
        this.mExpectedOutputDataTypes = new Class[this.queues][];
        this.channelList.toArray(this.mExpectedOutputDataTypes);
        this.channelPath = new int[this.channelMap.size()];
        for (int i = 0; i < this.queue.length; i++) {
            this.channelPath[i] = this.channelMap.get(this.queue[i].getName());
        }
        Object[][][] res = new Object[this.queues][][];
        while (true) {
            this.interruptExecution();
            Object o;
            o = this.getSourceQueue().take();
            if (o == ETLWorker.ENDOBJ) {
                while (this.remainingRecords()) {
                    this.processData(res, new Object[this.batchSize][this.mExpectedInputDataTypes.length]);
                }
                for (int i = 0; i < this.queues; i++) {
                    this.queue[i].put(o);
                }
                break;
            }
            Object[][] data = (Object[][]) o;
            if (this.mBatchManagement) {
                data = this.mBatchManager.initializeBatch(data, data.length);
            }
            this.processData(res, data);
        }
    }

    /**
     * Remaining records.
     * 
     * @return true, if successful
     */
    protected boolean remainingRecords() {
        return false;
    }

    /**
     * Process data.
     * 
     * @param res the res
     * @param data the data
     * 
     * @throws KETLTransformException the KETL transform exception
     * @throws InterruptedException the interrupted exception
     */
    private void processData(Object[][][] res, Object[][] data) throws KETLTransformException, InterruptedException {
        if (this.timing) this.startTimeNano = System.nanoTime();
        int rows = this.splitBatch(data, data.length, res);
        if (this.timing) this.totalTimeNano += System.nanoTime() - this.startTimeNano;
        this.updateThreadStats(rows);
        for (int i = 0; i < this.queues; i++) {
            this.queue[i].put(res[this.channelPath[i]]);
        }
    }

    @Override
    protected final void setBatchManager(BatchManager batchManager) {
        this.mBatchManager = (SplitBatchManager) batchManager;
    }

    /** The expected input data types. */
    private Class[] mExpectedInputDataTypes;

    /** The expected output data types. */
    private Class[][] mExpectedOutputDataTypes;

    /** The input record width. */
    private int mInputRecordWidth = -1;

    /** The output record width. */
    private int[] mOutputRecordWidth = null;

    /**
     * Split batch.
     * 
     * @param pInputRecord the input record
     * @param length the length
     * @param pOutput the output
     * 
     * @return the int
     * 
     * @throws KETLTransformException the KETL transform exception
     */
    private int splitBatch(Object[][] pInputRecord, int length, Object[][][] pOutput) throws KETLTransformException {
        int rows = 0;
        for (int i = 0; i < this.queues; i++) pOutput[i] = new Object[length][];
        int[] resultLength = new int[this.queues];
        for (int i = 0; i < length; i++) {
            if (i == 0) this.mInputRecordWidth = this.mExpectedInputDataTypes.length;
            for (int path = 0; path < this.queues; path++) {
                if (i == 0 && this.mOutputRecordWidth[path] == -1) this.mOutputRecordWidth[path] = this.mExpectedOutputDataTypes[path].length;
                Object[] result = new Object[this.mOutputRecordWidth[path]];
                try {
                    int code = this.core.splitRecord(pInputRecord[i], this.mExpectedInputDataTypes, this.mInputRecordWidth, path, result, this.mExpectedOutputDataTypes[path], this.mOutputRecordWidth[path]);
                    switch(code) {
                        case DefaultSplitCore.SUCCESS:
                            pOutput[path][resultLength[path]++] = result;
                            break;
                        case DefaultSplitCore.SKIP_RECORD:
                            break;
                        default:
                            throw new KETLTransformException("Invalid return code, check previous error message", code);
                    }
                } catch (KETLTransformException e) {
                    this.incrementErrorCount(e, pInputRecord[i], 1);
                }
            }
        }
        for (int path = 0; path < this.queues; path++) {
            if (resultLength[path] != length) {
                Object[][] newResult = new Object[resultLength[path]][];
                System.arraycopy(pOutput[path], 0, newResult, 0, resultLength[path]);
                pOutput[path] = newResult;
            }
            rows += resultLength[path];
        }
        return rows;
    }

    @Override
    public final void initializeQueues() {
        for (ManagedBlockingQueue element : this.queue) element.registerWriter(this);
        this.getSourceQueue().registerReader(this);
    }

    /**
     * Sets the source queue.
     * 
     * @param srcQueue the src queue
     * @param worker the worker
     * 
     * @throws KETLThreadException the KETL thread exception
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
        for (int i = 0; i < this.queue.length; i++) if (this.queue[i] == currentQueue) this.queue[i] = newQueue;
    }

    /**
     * Gets the source queue.
     * 
     * @return the source queue
     */
    ManagedBlockingQueue getSourceQueue() {
        return this.srcQueue;
    }

    @Override
    protected String getRecordExecuteMethodHeader() throws KETLThreadException {
        StringBuilder sb = new StringBuilder();
        sb.append("public int splitRecord(Object[] pInputRecords," + " Class[] pInputDataTypes, int pInputRecordWidth, " + " int pOutPath, Object[] pOutputRecords, Class[] pOutputDataTypes," + " int pOutputRecordWidth) throws KETLTransformException {\n");
        return sb.toString();
    }

    @Override
    protected String getRecordExecuteMethodFooter() {
        return " return SUCCESS;}";
    }
}
