package com.kni.etl.ketl.smp;

import java.io.IOException;
import java.util.ArrayList;
import org.w3c.dom.Node;
import com.kni.etl.dbutils.ResourcePool;
import com.kni.etl.ketl.ETLInPort;
import com.kni.etl.ketl.ETLPort;
import com.kni.etl.ketl.ETLStep;
import com.kni.etl.ketl.checkpointer.CheckPointManager;
import com.kni.etl.ketl.exceptions.KETLQAException;
import com.kni.etl.ketl.exceptions.KETLThreadException;
import com.kni.etl.ketl.exceptions.KETLWriteException;
import com.kni.etl.util.XMLHelper;

/**
 * The Class ETLWriter.
 * 
 * @author nwakefield To change the template for this generated type comment go to Window&gt;Preferences&gt;Java&gt;Code
 * Generation&gt;Code and Comments
 */
public abstract class ETLWriter extends ETLStep {

    /**
     * Increment error count.
     * 
     * @param e the e
     * @param objects the objects
     * @param val the val
     * 
     * @throws KETLWriteException the KETL write exception
     */
    public final void incrementErrorCount(KETLWriteException e, Object[] objects, int val) throws KETLWriteException {
        try {
            if (objects != null) this.logBadRecord(val, objects, e);
        } catch (IOException e1) {
            throw new KETLWriteException(e1);
        }
        try {
            super.incrementErrorCount(e, 1, val);
        } catch (Exception e1) {
            if (e1 instanceof KETLWriteException) throw (KETLWriteException) e1;
            throw new KETLWriteException(e1);
        }
    }

    /** The src queue. */
    private ManagedBlockingQueue srcQueue;

    @Override
    protected CharSequence generateCoreHeader() {
        return " public class " + this.getCoreClassName() + " extends ETLWriterCore { ";
    }

    @Override
    protected String generateCoreImports() {
        return super.generateCoreImports() + "import com.kni.etl.ketl.smp.ETLWriterCore;\n" + "import com.kni.etl.ketl.smp.ETLWriter;\n";
    }

    /**
     * Instantiates a new ETL writer.
     * 
     * @param pXMLConfig the XML config
     * @param pPartitionID the partition ID
     * @param pPartition the partition
     * @param pThreadManager the thread manager
     * 
     * @throws KETLThreadException the KETL thread exception
     */
    public ETLWriter(Node pXMLConfig, int pPartitionID, int pPartition, ETLThreadManager pThreadManager) throws KETLThreadException {
        super(pXMLConfig, pPartitionID, pPartition, pThreadManager);
    }

    /** The expected data types. */
    private Class[] mExpectedDataTypes;

    /** The record width. */
    private int mRecordWidth;

    /** The core. */
    private DefaultWriterCore core;

    /**
     * Gets the expected data types.
     * 
     * @return the expected data types
     */
    protected Class[] getExpectedDataTypes() {
        return this.mExpectedDataTypes;
    }

    @Override
    protected final void initializeOutports(ETLPort[] outPortNodes) throws KETLThreadException {
    }

    /**
     * Put next batch.
     * 
     * @param o the o
     * @param length the length
     * 
     * @return the int
     * 
     * @throws KETLWriteException the KETL write exception
     * @throws KETLQAException the KETLQA exception
     */
    protected final int putNextBatch(Object[][] o, int length) throws KETLWriteException, KETLQAException {
        int count = 0;
        for (int i = 0; i < length; i++) {
            try {
                if (this.mMonitor) ResourcePool.LogMessage(this, ResourcePool.DEBUG_MESSAGE, "[" + count + "]" + java.util.Arrays.toString(o[i]));
                int res = this.core.putNextRecord(o[i], this.mExpectedDataTypes, this.mRecordWidth);
                if (res > 0) {
                    this.recordCheck(o[i], null);
                    count += res;
                } else if (res < 0) {
                    throw new KETLWriteException("Unknown error, see previous messages");
                }
            } catch (KETLWriteException e) {
                this.recordCheck(o[i], e);
                this.incrementErrorCount(e, o[i], this.getRecordsProcessed() + i + 1);
            }
        }
        return count;
    }

    @Override
    void setOutputRecordDataTypes(Class[] pClassArray, String pChannel) {
    }

    /** The batch manager. */
    private WriterBatchManager mBatchManager;

    private CheckPointManager checkPointManager;

    @Override
    protected final void executeWorker() throws InterruptedException, KETLWriteException, KETLThreadException {
        int res;
        checkPointManager = new CheckPointManager(this);
        while (true) {
            this.interruptExecution();
            Object o;
            o = this.getSourceQueue().take();
            if (o == ETLWorker.ENDOBJ) {
                break;
            }
            Object[][] data = (Object[][]) o;
            if (this.mBatchManagement) {
                data = this.mBatchManager.initializeBatch(data, data.length);
            }
            if (this.timing) this.startTimeNano = System.nanoTime();
            res = this.putNextBatch(data, data.length);
            if (this.timing) this.totalTimeNano += System.nanoTime() - this.startTimeNano;
            if (this.mBatchManagement) {
                res = this.mBatchManager.finishBatch(data.length);
            }
            this.updateThreadStats(res);
        }
        if (this.mBatchManagement) {
            res = this.mBatchManager.finishBatch(BatchManager.LASTBATCH);
            this.updateThreadStats(res);
        }
    }

    @Override
    final String getDefaultExceptionClass() {
        return KETLWriteException.class.getCanonicalName();
    }

    @Override
    protected final void setBatchManager(BatchManager batchManager) {
        this.mBatchManager = (WriterBatchManager) batchManager;
    }

    @Override
    public final void initializeQueues() {
        ArrayList al = new ArrayList();
        Node[] nl = XMLHelper.getElementsByName(this.getXMLConfig(), "IN", "*", "*");
        if (nl != null) {
            for (Node element : nl) {
                ETLPort p = this.getInPort(XMLHelper.getAttributeAsString(element.getAttributes(), "NAME", null));
                if (p != null) al.add(p);
            }
            this.mInPorts = new ETLInPort[al.size()];
            al.toArray(this.mInPorts);
        }
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
            this.mExpectedDataTypes = worker.getOutputRecordDatatypes(ETLWorker.getChannel(this.getXMLConfig(), ETLWorker.DEFAULT));
            this.mRecordWidth = this.mExpectedDataTypes.length;
        } catch (ClassNotFoundException e) {
            throw new KETLThreadException(e, this);
        }
        this.configureBufferSort(srcQueue);
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
    protected String getRecordExecuteMethodHeader() {
        StringBuilder sb = new StringBuilder();
        sb.append("public int putNextRecord(Object[] pInputRecords, Class[] pExpectedDataTypes, int pRecordWidth) " + " throws KETLWriteException {");
        return sb.toString();
    }

    @Override
    protected String getRecordExecuteMethodFooter() {
        return " return -1;}";
    }

    @Override
    final void setCore(DefaultCore newCore) {
        this.core = (DefaultWriterCore) newCore;
    }

    @Override
    public void switchTargetQueue(ManagedBlockingQueue currentQueue, ManagedBlockingQueue newQueue) {
    }
}
