package com.kni.etl.ketl.smp;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.kni.etl.EngineConstants;
import com.kni.etl.dbutils.ResourcePool;
import com.kni.etl.ketl.ETLInPort;
import com.kni.etl.ketl.ETLOutPort;
import com.kni.etl.ketl.ETLPort;
import com.kni.etl.ketl.ETLStep;
import com.kni.etl.ketl.KETLJobExecutor;
import com.kni.etl.ketl.exceptions.KETLReadException;
import com.kni.etl.ketl.exceptions.KETLThreadException;
import com.kni.etl.ketl.exceptions.KETLTransformException;
import com.kni.etl.ketl.exceptions.KETLWriteException;
import com.kni.etl.ketl.smp.ETLThreadManager.WorkerThread;
import com.kni.etl.util.ClassFromCode;
import com.kni.etl.util.XMLHelper;

/**
 * The Class ETLWorker.
 * 
 * @author nwakefield To change the template for this generated type comment go
 *         to Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and
 *         Comments
 */
public abstract class ETLWorker implements Runnable, ETLStats {

    /**
	 * The Class CodeField.
	 */
    class CodeField {

        /** The constant. */
        boolean constant = false;

        /** The datatype. */
        String datatype;

        /** The name. */
        String name;

        /** The private value. */
        boolean privateValue = false;

        /** The value. */
        String value;

        @Override
        public String toString() {
            return this.name;
        }
    }

    protected String getVersion() {
        return "Not set";
    }

    /**
	 * Debug.
	 * 
	 * @return true, if successful
	 */
    public final boolean debug() {
        return this.mDebug;
    }

    public final Collection<ETLOutPort> getOutPorts() {
        return this.mOutPorts == null ? this.hmOutports.values() : java.util.Arrays.asList(this.mOutPorts);
    }

    /** The Constant CHANNEL. */
    public static final int CHANNEL = 1;

    /** The Constant DEFAULT. */
    public static final int DEFAULT = 0;

    /** The Constant ENDOBJ. */
    public static final Object[][] ENDOBJ = new Object[0][0];

    /** The Constant LEFT. */
    public static final int LEFT = 0;

    /** The Constant PORT. */
    public static final int PORT = 2;

    /** The Constant RIGHT. */
    public static final int RIGHT = 1;

    /** The Constant STEP. */
    static final int STEP = 0;

    private static final String USE_CHECK_POINT = "USE_CHECK_POINT";

    /**
	 * Configure buffer sort.
	 * 
	 * @param srcQueue
	 *            the src queue
	 */
    protected void configureBufferSort(ManagedBlockingQueue srcQueue) {
        if (srcQueue instanceof Partitioner) return;
        Node[] sortKeys = XMLHelper.getElementsByName(this.getXMLConfig(), "IN", "BUFFERSORT", null);
        Comparator comp = null;
        if (sortKeys != null && sortKeys.length > 0) {
            Integer[] elements = new Integer[sortKeys.length];
            Boolean[] elementOrder = new Boolean[sortKeys.length];
            for (int i = 0; i < sortKeys.length; i++) {
                elements[i] = XMLHelper.getAttributeAsInt(sortKeys[i].getAttributes(), "BUFFERSORT", 0);
                elementOrder[i] = XMLHelper.getAttributeAsBoolean(sortKeys[i].getAttributes(), "BUFFERSORTORDER", true);
            }
            comp = new DefaultComparator(elements, elementOrder);
            ((ManagedBlockingQueueImpl) srcQueue).setSortComparator(comp);
        }
    }

    /**
	 * Extract port details.
	 * 
	 * @param content
	 *            the content
	 * 
	 * @return the string[]
	 * 
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    protected static final String[] extractPortDetails(String content) throws KETLThreadException {
        if (content == null) return null;
        content = content.trim();
        if (content.startsWith("\"") && content.endsWith("\"")) return null;
        String[] sources = content.split("\\.");
        if (sources == null || sources.length == 1 || sources.length > 3) throw new KETLThreadException("IN port definition invalid: \"" + content + "\"", Thread.currentThread());
        String[] res = new String[3];
        res[ETLWorker.STEP] = sources[0];
        if (sources.length == 3) {
            res[ETLWorker.CHANNEL] = sources[1];
            res[ETLWorker.PORT] = sources[2];
        } else res[ETLWorker.PORT] = sources[1];
        return res;
    }

    /**
	 * Gets the channel.
	 * 
	 * @param xmlConfig
	 *            the xml config
	 * @param type
	 *            the type
	 * 
	 * @return the channel
	 * 
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    public static final String getChannel(Element xmlConfig, int type) throws KETLThreadException {
        Node[] ports;
        if (type == ETLWorker.DEFAULT) ports = XMLHelper.getElementsByName(xmlConfig, "IN", "*", "*"); else ports = XMLHelper.getElementsByName(xmlConfig, "IN", type == ETLWorker.LEFT ? "LEFT" : "RIGHT", "TRUE");
        for (Node port : ports) {
            String content = XMLHelper.getTextContent(port);
            if (content == null) continue;
            content = content.trim();
            if (content.startsWith("\"") && content.endsWith("\"")) continue;
            String[] sources = content.split("\\.");
            if (sources == null || sources.length == 1 || sources.length > 3) throw new KETLThreadException("IN port definition invalid: \"" + content + "\"", Thread.currentThread());
            if (sources.length == 2) return "DEFAULT";
            return sources[1];
        }
        throw new KETLThreadException("Step \"" + XMLHelper.getAttributeAsString(xmlConfig.getAttributes(), "NAME", "n/a") + "\" has no in ports or ports do not have a valid source", Thread.currentThread());
    }

    /**
	 * Gets the channels.
	 * 
	 * @param config
	 *            the config
	 * 
	 * @return the channels
	 */
    public static final String[] getChannels(Element config) {
        NodeList nl = config.getElementsByTagName("OUT");
        HashSet ports = new HashSet();
        for (int i = 0; i < nl.getLength(); i++) {
            ports.add(XMLHelper.getAttributeAsString(nl.item(i).getAttributes(), "CHANNEL", "DEFAULT"));
        }
        String[] res = new String[ports.size()];
        ports.toArray(res);
        return res;
    }

    /**
	 * Gets the source.
	 * 
	 * @param xmlConfig
	 *            the xml config
	 * 
	 * @return the source
	 * 
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    public static final String[] getSource(Element xmlConfig) throws KETLThreadException {
        String left = null, right = null;
        NodeList nl = xmlConfig.getElementsByTagName("IN");
        for (int i = 0; i < nl.getLength(); i++) {
            boolean leftSource = false, rightSource = false;
            Node port = nl.item(i);
            String content = XMLHelper.getTextContent(port);
            if (ETLPort.containsConstant(content)) continue;
            if (port.hasAttributes() && XMLHelper.getAttributeAsBoolean(port.getAttributes(), "RIGHT", false)) rightSource = true; else leftSource = true;
            String[] sources = ETLWorker.extractPortDetails(content);
            if (sources == null) continue;
            if (left == null && leftSource) left = sources[ETLWorker.STEP]; else if (right == null && rightSource) right = sources[ETLWorker.STEP];
        }
        if (right == null) return new String[] { left };
        return new String[] { left, right };
    }

    /**
	 * Sets the out defaults.
	 * 
	 * @param pConfig
	 *            the new out defaults
	 * 
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    public static final void setOutDefaults(Element pConfig) throws KETLThreadException {
        NodeList nl = pConfig.getElementsByTagName("OUT");
        for (int i = 0; i < nl.getLength(); i++) {
            String content = XMLHelper.getTextContent(nl.item(i));
            if (!(content == null || content.trim().equals("*") || content.trim().length() == 0 || content.trim().startsWith(com.kni.etl.EngineConstants.VARIABLE_PARAMETER_START) && content.trim().endsWith(com.kni.etl.EngineConstants.VARIABLE_PARAMETER_END)) && (nl.item(i).hasAttributes() == false)) throw new KETLThreadException("Invalid out node found in the XML - " + XMLHelper.outputXML(nl.item(i)), Thread.currentThread());
            Element n = (Element) nl.item(i);
            if (n.hasAttribute("CHANNEL") == false) n.setAttribute("CHANNEL", "DEFAULT");
        }
    }

    /** The tune interval. */
    int tuneInterval = 200000;

    /** The core class name. */
    private String coreClassName = null;

    /** The default batch size. */
    private final int defaultBatchSize = 1000;

    /** The default queue size. */
    private final int defaultQueueSize = 5;

    /** The hm inports. */
    protected HashMap hmInports = new HashMap();

    /** The hm outports. */
    protected Map<String, ETLOutPort> hmOutports = new HashMap<String, ETLOutPort>();

    /** The batch management. */
    protected final boolean mBatchManagement = this.implementsBatchManagement();

    /** The channel class mapping. */
    HashMap mChannelClassMapping = new HashMap();

    /** The channel ports used. */
    protected HashMap mChannelPortsUsed = new HashMap();

    /** The code fields. */
    ArrayList mCodeFields = new ArrayList();

    /** The code fields lookup. */
    HashMap mCodeFieldsLookup = new HashMap();

    /** The mhm inport index. */
    private final HashMap mhmInportIndex = new HashMap();

    /** The hm outport index. */
    protected HashMap mHmOutportIndex = new HashMap();

    /** The in ports. */
    protected ETLInPort[] mInPorts;

    /** The out ports. */
    protected ETLOutPort[] mOutPorts;

    /** The source outs. */
    HashMap mSourceOuts = new HashMap();

    /** The mstr name. */
    String mstrName;

    /** The thread manager. */
    private final ETLThreadManager mThreadManager;

    /**
	 * Gets the thread manager.
	 * 
	 * @return the thread manager
	 */
    public ETLThreadManager getThreadManager() {
        return this.mThreadManager;
    }

    /** The pos. */
    private int pos = 0;

    /** The partitions. */
    protected int queueSize, batchSize, partitionID, partitions;

    public int getPartitionID() {
        return partitionID;
    }

    /** The record count. */
    private int recordCount = 0;

    /** The self tune. */
    private boolean selfTune = true;

    /** The xml config. */
    private final Node xmlConfig;

    /** The tune interval increment. */
    int tuneIntervalIncrement = 200000;

    /** The timing. */
    protected final boolean timing;

    /** The debug. */
    protected boolean mDebug;

    /** The monitor. */
    protected boolean mMonitor;

    /**
	 * The Constructor.
	 * 
	 * @param pPartitionID
	 *            TODO
	 * @param pXMLConfig
	 *            the XML config
	 * @param pPartitions
	 *            the partitions
	 * @param pThreadManager
	 *            the thread manager
	 * 
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    public ETLWorker(Node pXMLConfig, int pPartitionID, int pPartitions, ETLThreadManager pThreadManager) throws KETLThreadException {
        super();
        this.queueSize = XMLHelper.getAttributeAsInt(pXMLConfig.getAttributes(), "QUEUESIZE", this.defaultQueueSize);
        this.batchSize = XMLHelper.getAttributeAsInt(pXMLConfig.getAttributes(), "BATCHSIZE", this.defaultBatchSize);
        this.timing = XMLHelper.getAttributeAsBoolean(pXMLConfig.getAttributes(), "TIMING", true);
        this.mThreadManager = pThreadManager;
        this.partitionID = pPartitionID;
        this.mstrName = XMLHelper.getAttributeAsString(pXMLConfig.getAttributes(), "NAME", null);
        this.partitions = pPartitions;
        this.mThreadManager.addStep(this);
        this.xmlConfig = pXMLConfig;
        this.mDebug = XMLHelper.getAttributeAsBoolean(pXMLConfig.getAttributes(), "DEBUG", false);
        this.mMonitor = XMLHelper.getAttributeAsBoolean(pXMLConfig.getAttributes(), "MONITOR", false);
        this.useCheckPoint = false;
        if (this.getVersion() != null) ResourcePool.LogMessage(this, ResourcePool.INFO_MESSAGE, "Using " + this.getClass().getName() + " - " + this.getVersion().replace("$LastChangedRevision: ", "v.").replace("$", ""));
        try {
            Class cl = Class.forName("com.kni.etl.ketl.smp.ETLBatchOptimizer");
            ResourcePool.LogMessage(this, ResourcePool.DEBUG_MESSAGE, "Batch optimizer enabled");
            this.tuneInterval = XMLHelper.getAttributeAsInt(pXMLConfig.getParentNode().getAttributes(), "AUTOTUNESTART", this.tuneInterval);
            this.tuneIntervalIncrement = XMLHelper.getAttributeAsInt(pXMLConfig.getParentNode().getAttributes(), "AUTOTUNEINCREMENT", this.tuneIntervalIncrement);
            this.mBatchOptimizer = (BatchOptimizer) cl.newInstance();
        } catch (Exception e) {
        }
    }

    /**
	 * Compile.
	 * 
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    public final void compile() throws KETLThreadException {
        Class coreClass = null;
        if (!(this instanceof DefaultCore)) coreClass = this.generateCore();
        this.instantiateCore(coreClass);
    }

    /**
	 * Complete.
	 * 
	 * @return the int
	 * 
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    public int complete() throws KETLThreadException {
        return 0;
    }

    /** The total time nano. */
    protected long totalTimeNano = 0;

    /** The start time nano. */
    protected long startTimeNano = 0;

    /**
	 * Execute worker.
	 * 
	 * @throws InterruptedException
	 *             the interrupted exception
	 * @throws ClassNotFoundException
	 *             the class not found exception
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 * @throws KETLReadException
	 *             the KETL read exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws KETLTransformException
	 *             the KETL transform exception
	 * @throws KETLWriteException
	 *             the KETL write exception
	 */
    protected abstract void executeWorker() throws InterruptedException, ClassNotFoundException, KETLThreadException, KETLReadException, IOException, KETLTransformException, KETLWriteException;

    /**
	 * Generate core.
	 * 
	 * @return the class
	 * 
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    private Class generateCore() throws KETLThreadException {
        StringBuilder sb = new StringBuilder("package job." + ((ETLStep) this).getJobExecutor().getCurrentETLJob().getJobID() + ";\n");
        sb.append(this.generateCoreImports());
        sb.append(this.generateCoreHeader());
        sb.append(this.generatePortMappingCode());
        for (Object o : this.mCodeFields) {
            CodeField cons = (CodeField) o;
            if (cons.privateValue) sb.append("private ");
            if (cons.constant) sb.append("static final " + cons.datatype + " " + cons.name + " = " + cons.value + ";\n"); else sb.append(cons.datatype + " " + cons.name + ";\n");
        }
        sb.append("protected void initializeCoreFields() {");
        for (Object o : this.mCodeFields) {
            CodeField cons = (CodeField) o;
            if (cons.constant == false) sb.append(cons.name + " = " + cons.value + ";\n");
        }
        sb.append("}");
        sb.append('}');
        try {
            return ClassFromCode.getDynamicClass(((ETLStep) this).getJobExecutor().getCurrentETLJob(), sb.toString(), this.getCoreClassName(), false, false);
        } catch (Exception e) {
            throw new KETLThreadException(e, this);
        }
    }

    /**
	 * Generate core header.
	 * 
	 * @return the char sequence
	 */
    protected abstract CharSequence generateCoreHeader();

    /**
	 * Generate core imports.
	 * 
	 * @return the string
	 */
    protected String generateCoreImports() {
        return "import com.kni.etl.ketl.exceptions.*;\n" + "import com.kni.etl.ketl.smp.*;\n" + "import com.kni.etl.functions.*;\n";
    }

    /**
	 * Generate port mapping code.
	 * 
	 * @return the string
	 * 
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    protected String generatePortMappingCode() throws KETLThreadException {
        StringBuilder sb = new StringBuilder();
        if (this.mInPorts != null) {
            for (ETLInPort element : this.mInPorts) {
                if (element.isConstant()) this.getCodeField(element.getPortClass().getCanonicalName(), "new " + element.getPortClass().getCanonicalName() + "(\"" + element.getConstantValue().toString() + "\")", true, true, element.generateReference());
            }
        }
        sb.append(this.getRecordExecuteMethodHeader() + "\n");
        if (this.mOutPorts != null) for (int i = 0; i < this.mOutPorts.length; i++) {
            sb.append("try { " + this.mOutPorts[i].generateCode(i));
            sb.append(";} catch(Exception e) { if(e instanceof " + this.getDefaultExceptionClass() + ") { throw (" + this.getDefaultExceptionClass() + ")e; } else {throw new " + this.getDefaultExceptionClass() + "(\"Port " + this.mOutPorts[i].mstrName + " generated exception \" + e.toString(),e);}}\n");
        }
        sb.append(this.getRecordExecuteMethodFooter() + "\n");
        return sb.toString();
    }

    /**
	 * Gets the default exception class.
	 * 
	 * @return the default exception class
	 */
    abstract String getDefaultExceptionClass();

    /**
	 * Gets the batch manager.
	 * 
	 * @return the batch manager
	 */
    protected BatchManager getBatchManager() {
        if (this.implementsBatchManagement()) {
            return (BatchManager) this;
        }
        return null;
    }

    /**
	 * Gets the code field.
	 * 
	 * @param datatype
	 *            the datatype
	 * @param value
	 *            the value
	 * @param constant
	 *            the constant
	 * @param privateValue
	 *            the private value
	 * @param name
	 *            the name
	 * 
	 * @return the code field
	 */
    protected String getCodeField(String datatype, String value, boolean constant, boolean privateValue, String name) {
        String nm = (name == null ? datatype + constant : name);
        if (this.mCodeFieldsLookup.containsKey(nm)) return ((CodeField) this.mCodeFieldsLookup.get(nm)).name;
        CodeField cons = new CodeField();
        cons.constant = constant;
        cons.datatype = datatype;
        cons.name = (name == null ? "CONST_" + this.mCodeFieldsLookup.size() : name);
        cons.value = value;
        cons.privateValue = privateValue;
        this.mCodeFieldsLookup.put(nm, cons);
        this.mCodeFields.add(cons);
        return cons.name;
    }

    /**
	 * Gets the code generation output object.
	 * 
	 * @param pChannel
	 *            the channel
	 * 
	 * @return the code generation output object
	 */
    public final String getCodeGenerationOutputObject(String pChannel) {
        return "pOutputRecords";
    }

    /**
	 * Gets the core class name.
	 * 
	 * @return the core class name
	 */
    protected final String getCoreClassName() {
        if (this.coreClassName == null) this.coreClassName = this.mstrName;
        return this.coreClassName;
    }

    /**
	 * Gets the in port.
	 * 
	 * @param index
	 *            the index
	 * 
	 * @return the in port
	 */
    public final ETLInPort getInPort(int index) {
        for (ETLInPort element : this.mInPorts) if (element.getSourcePortIndex() == index) return element;
        return null;
    }

    /**
	 * Gets the in port.
	 * 
	 * @param arg0
	 *            the arg0
	 * 
	 * @return the in port
	 */
    public final ETLInPort getInPort(String arg0) {
        return (ETLInPort) this.hmInports.get(arg0);
    }

    /**
	 * Gets the job execution ID.
	 * 
	 * @return the job execution ID
	 */
    public abstract long getJobExecutionID();

    public abstract String getJobID();

    /**
	 * Gets the name.
	 * 
	 * @return the name
	 */
    public final String getName() {
        return this.mstrName;
    }

    /**
	 * Gets the new in port.
	 * 
	 * @param srcStep
	 *            the src step
	 * 
	 * @return the new in port
	 */
    protected ETLInPort getNewInPort(ETLStep srcStep) {
        return new ETLInPort((ETLStep) this, srcStep);
    }

    /**
	 * Gets the new out port.
	 * 
	 * @param srcStep
	 *            the src step
	 * 
	 * @return the new out port
	 */
    protected ETLOutPort getNewOutPort(ETLStep srcStep) {
        return new ETLOutPort((ETLStep) this, (ETLStep) this);
    }

    /**
	 * Gets the out channel.
	 * 
	 * @return the out channel
	 */
    protected final String getOutChannel() {
        return (String) this.mChannelClassMapping.keySet().toArray()[0];
    }

    /**
	 * Gets the out port.
	 * 
	 * @param index
	 *            the index
	 * 
	 * @return the out port
	 */
    public final ETLOutPort getOutPort(int index) {
        return this.mOutPorts[index];
    }

    /**
	 * Gets the out port.
	 * 
	 * @param arg0
	 *            the arg0
	 * 
	 * @return the out port
	 * 
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    public final ETLOutPort getOutPort(String arg0) throws KETLThreadException {
        ETLOutPort port = this.hmOutports.get(arg0);
        if (port == null) {
            Node[] nl = XMLHelper.getElementsByName(this.getXMLConfig(), "OUT", "*", "*");
            if (nl != null) {
                for (Node element : nl) {
                    String nm = XMLHelper.getAttributeAsString(element.getAttributes(), "NAME", null);
                    if (nm == null || nm.equals(arg0)) {
                        ETLOutPort newPort = this.getNewOutPort((ETLStep) this);
                        try {
                            newPort.initialize(element);
                        } catch (Exception e) {
                            throw new KETLThreadException(e, this);
                        }
                        newPort.used(false);
                        if (this.hmOutports.put(newPort.mstrName, newPort) != null) throw new KETLThreadException("Duplicate OUT port name exists, check step " + this.getName() + " port " + newPort.mstrName, this);
                    }
                }
            }
            port = this.hmOutports.get(arg0);
        }
        return port;
    }

    /**
	 * Gets the output record datatypes.
	 * 
	 * @param pChannel
	 *            the channel
	 * 
	 * @return the output record datatypes
	 * 
	 * @throws ClassNotFoundException
	 *             the class not found exception
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    protected final Class[] getOutputRecordDatatypes(String pChannel) throws ClassNotFoundException, KETLThreadException {
        Class[] result = (Class[]) this.mChannelClassMapping.get(pChannel);
        if (result == null) {
            String[] channels = ETLWorker.getChannels(this.getXMLConfig());
            for (String element : channels) {
                if (element.equals(pChannel)) {
                    Node[] nList = XMLHelper.getElementsByName(this.getXMLConfig(), "OUT", "CHANNEL", element);
                    ArrayList al = new ArrayList();
                    int portIndex = 0;
                    for (Node element0 : nList) {
                        if (this.portUsed(pChannel, ((Element) element0).getAttribute("NAME"))) {
                            ETLOutPort port = (this.getOutPort(((Element) element0).getAttribute("NAME")));
                            al.add(port.getPortClass());
                            port.setIndex(portIndex);
                            this.mHmOutportIndex.put(port, portIndex++);
                        }
                    }
                    Class[] cls = new Class[al.size()];
                    al.toArray(cls);
                    this.mChannelClassMapping.put(element, cls);
                    this.setOutputRecordDataTypes(cls, pChannel);
                    return cls;
                }
            }
        } else return result;
        throw new KETLThreadException("Invalid channel request", this);
    }

    /**
	 * Gets the queue size.
	 * 
	 * @return the queue size
	 */
    public final int getQueueSize() {
        return this.queueSize;
    }

    /**
	 * Gets the record execute method footer.
	 * 
	 * @return the record execute method footer
	 */
    protected abstract String getRecordExecuteMethodFooter();

    /**
	 * Gets the record execute method header.
	 * 
	 * @return the record execute method header
	 * 
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    protected abstract String getRecordExecuteMethodHeader() throws KETLThreadException;

    /**
	 * Gets the records processed.
	 * 
	 * @return the records processed
	 */
    protected final int getRecordsProcessed() {
        return this.recordCount;
    }

    /**
	 * Gets the used port index.
	 * 
	 * @param port
	 *            the port
	 * 
	 * @return the used port index
	 * 
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    public int getUsedPortIndex(ETLPort port) throws KETLThreadException {
        if (port instanceof ETLOutPort) return ((Integer) this.mHmOutportIndex.get(port)).intValue();
        return ((ETLInPort) port).getSourcePortIndex();
    }

    /**
	 * Gets the used ports from worker.
	 * 
	 * @param pWorker
	 *            the worker
	 * @param port
	 *            the port
	 * 
	 * @return the used ports from worker
	 * 
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    protected final void getUsedPortsFromWorker(ETLWorker pWorker, String port) throws KETLThreadException {
        ResourcePool.LogMessage(this, ResourcePool.DEBUG_MESSAGE, "Registering port usage for step " + pWorker.toString() + " by step " + this.toString());
        Node[] nl = com.kni.etl.util.XMLHelper.getElementsByName(this.getXMLConfig(), "IN", "*", "*");
        this.registerUsedPorts(pWorker, nl, "pInputRecords");
    }

    /**
	 * Gets the XML config.
	 * 
	 * @return the XML config
	 */
    public final Element getXMLConfig() {
        return (Element) this.xmlConfig;
    }

    /**
	 * Handle event code.
	 * 
	 * @param eventCode
	 *            the event code
	 * 
	 * @return the object
	 */
    public Object handleEventCode(int eventCode) {
        return null;
    }

    /**
	 * Handle exception.
	 * 
	 * @param e
	 *            the e
	 * 
	 * @return the object
	 * 
	 * @throws Exception
	 *             the exception
	 */
    public Object handleException(Exception e) throws Exception {
        throw e;
    }

    /**
	 * Handle port event code.
	 * 
	 * @param eventCode
	 *            the event code
	 * @param portIndex
	 *            the port index
	 * 
	 * @return the object
	 * 
	 * @throws Exception
	 *             the exception
	 */
    public Object handlePortEventCode(int eventCode, int portIndex) throws Exception {
        return null;
    }

    /**
	 * Handle port exception.
	 * 
	 * @param e
	 *            the e
	 * @param portIndex
	 *            the port index
	 * 
	 * @return the object
	 * 
	 * @throws Exception
	 *             the exception
	 */
    public Object handlePortException(Exception e, int portIndex) throws Exception {
        throw e;
    }

    /**
	 * Hash.
	 * 
	 * @param obj
	 *            the obj
	 * @param paths
	 *            the paths
	 * 
	 * @return the int
	 */
    protected int hash(Object[] obj, int paths) {
        if (this.pos == paths) this.pos = 0;
        return this.pos++;
    }

    /**
	 * Implements batch management.
	 * 
	 * @return true, if successful
	 */
    private final boolean implementsBatchManagement() {
        if (this instanceof BatchManager) {
            return true;
        }
        return false;
    }

    /**
	 * Initialize.
	 * 
	 * @param mkjExecutor
	 *            the mkj executor
	 * 
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    public final void initialize(KETLJobExecutor mkjExecutor) throws KETLThreadException {
        ((ETLStep) this).setJobExecutor(mkjExecutor);
        if (this.initialize(this.getXMLConfig()) != 0) throw new KETLThreadException("Core failed to initialize, see previous errors", this);
    }

    /**
	 * Initialize.
	 * 
	 * @param xmlConfig
	 *            the xml config
	 * 
	 * @return the int
	 * 
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    protected abstract int initialize(Node xmlConfig) throws KETLThreadException;

    /**
	 * Initialize all out ports.
	 * 
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    public final void initializeAllOutPorts() throws KETLThreadException {
        Node[] nl = XMLHelper.getElementsByName(this.getXMLConfig(), "OUT", "*", "*");
        if (nl != null) {
            for (Node element : nl) {
                String nm = XMLHelper.getAttributeAsString(element.getAttributes(), "NAME", null);
                if (nm == null || this.hmOutports.containsKey(nm) == false) {
                    ETLOutPort newPort = this.getNewOutPort((ETLStep) this);
                    try {
                        newPort.initialize(element);
                    } catch (Exception e) {
                        throw new KETLThreadException(e, this);
                    }
                    newPort.used(false);
                    if (this.hmOutports.put(newPort.mstrName, newPort) != null) throw new KETLThreadException("Duplicate OUT port name exists, check step " + this.getName() + " port " + newPort.mstrName, this);
                }
            }
        }
    }

    /**
	 * Initialize outports.
	 * 
	 * @param outPortNodes
	 *            the out port nodes
	 * 
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    protected void initializeOutports(ETLPort[] outPortNodes) throws KETLThreadException {
        for (ETLPort port : outPortNodes) {
            if (port.isConstant()) {
                port.instantiateConstant();
            } else if (port.containsCode()) {
                if (port.getPortClass() == null) {
                    throw new KETLThreadException("For code based transforms DATATYPE must be specified, check step " + this.getName(), this);
                }
            } else {
                ETLPort in = port.getAssociatedInPort();
                try {
                    if (port.useInheritedDataType() == false) {
                        if (in == null) throw new KETLThreadException("Specified in port for " + this.getName() + "." + port.getPortName() + " does not exist", this);
                        port.setDataTypeFromPort(in);
                    }
                } catch (Exception e) {
                    throw new KETLThreadException(e, this);
                }
            }
        }
    }

    /**
	 * Initialize queues.
	 */
    public abstract void initializeQueues();

    /**
	 * Instantiate core.
	 * 
	 * @param arg0
	 *            the arg0
	 * 
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    protected final void instantiateCore(Class arg0) throws KETLThreadException {
        try {
            DefaultCore newCore;
            if (this instanceof DefaultCore) newCore = (DefaultCore) this; else {
                newCore = (DefaultCore) arg0.newInstance();
                ((ETLCore) newCore).setOwner(this);
            }
            this.setCore(newCore);
            this.setBatchManager(this.getBatchManager());
        } catch (Exception e) {
            throw new KETLThreadException(e, this);
        }
    }

    /**
	 * Port used.
	 * 
	 * @param pChannel
	 *            the channel
	 * @param pPort
	 *            the port
	 * 
	 * @return true, if successful
	 */
    protected final boolean portUsed(String pChannel, String pPort) {
        HashSet al = (HashSet) this.mChannelPortsUsed.get(pChannel);
        return al.contains(pPort);
    }

    /**
	 * Post source connected initialize.
	 * 
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    final void postSourceConnectedInitialize() throws KETLThreadException {
        List<ETLPort> al = new ArrayList<ETLPort>();
        Node[] nl = XMLHelper.getElementsByName(this.getXMLConfig(), "OUT", "*", "*");
        if (nl != null) {
            for (Node element : nl) {
                ETLPort p = this.getOutPort(XMLHelper.getAttributeAsString(element.getAttributes(), "NAME", null));
                if (p != null) al.add(p);
            }
            this.mOutPorts = new ETLOutPort[al.size()];
            al.toArray(this.mOutPorts);
        }
        al.clear();
        nl = XMLHelper.getElementsByName(this.getXMLConfig(), "IN", "*", "*");
        if (nl != null) {
            for (Node element : nl) {
                ETLPort p = this.getInPort(XMLHelper.getAttributeAsString(element.getAttributes(), "NAME", null));
                if (p != null) al.add(p);
            }
            this.mInPorts = new ETLInPort[al.size()];
            al.toArray(this.mInPorts);
        }
        this.initializeOutports(this.mOutPorts);
    }

    /** The fan in worker used. */
    private final HashMap mFanInWorkerUsed = new HashMap();

    /**
	 * Register used ports.
	 * 
	 * @param pWorker
	 *            the worker
	 * @param nl
	 *            the nl
	 * @param objectNameInCode
	 *            the object name in code
	 * 
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    protected final void registerUsedPorts(ETLWorker pWorker, Node[] nl, String objectNameInCode) throws KETLThreadException {
        Node wildCardPort = null;
        Node wildCardOut = null;
        ETLWorker duplicateSource = (ETLWorker) this.mFanInWorkerUsed.get(pWorker.mstrName);
        if (duplicateSource == null) {
            Node[] outNodes = XMLHelper.getElementsByName(this.getXMLConfig(), "OUT", "*", "*");
            HashSet outExists = new HashSet();
            if (outNodes != null) {
                for (Node element : outNodes) {
                    String content = XMLHelper.getTextContent(element);
                    if (content != null && content.trim().equals("*")) {
                        wildCardOut = element;
                        this.getXMLConfig().removeChild(wildCardOut);
                    } else {
                        String portName = XMLHelper.getAttributeAsString(element.getAttributes(), "NAME", null);
                        if (portName == null && content != null) {
                            content = content.trim();
                            if (content.startsWith(EngineConstants.VARIABLE_PARAMETER_START) && content.endsWith(EngineConstants.VARIABLE_PARAMETER_END)) {
                                String tmp[] = EngineConstants.getParametersFromText(content);
                                if (tmp != null && tmp.length == 1) {
                                    portName = tmp[0];
                                } else portName = null;
                            }
                        }
                        if (portName != null) outExists.add(portName);
                    }
                }
            }
            HashSet srcPortsUsed = new HashSet();
            for (Node node : nl) {
                ETLOutPort srcPort = null;
                ETLInPort newPort = this.getNewInPort((ETLStep) pWorker);
                newPort.setCodeGenerationReferenceObject(objectNameInCode);
                if (ETLPort.containsConstant(XMLHelper.getTextContent(node)) == false) {
                    String[] sources = ETLWorker.extractPortDetails(XMLHelper.getTextContent(node));
                    if (sources == null) continue;
                    ResourcePool.LogMessage(this, ResourcePool.DEBUG_MESSAGE, "getUsedPortsFromWorker -> Port: " + ((Element) node).getAttribute("NAME") + " ");
                    if (sources[ETLWorker.PORT].equals("*")) {
                        if (wildCardPort != null) throw new KETLThreadException("Duplicate wild card IN port exists, check step " + this.getName() + " port XML -> " + XMLHelper.outputXML(node), this);
                        wildCardPort = node;
                        newPort = null;
                    } else {
                        srcPort = pWorker.setOutUsed(sources[ETLWorker.CHANNEL], sources[ETLWorker.PORT]);
                        srcPortsUsed.add(srcPort);
                        newPort.setSourcePort(srcPort);
                        ArrayList res = (ArrayList) this.mhmInportIndex.get(objectNameInCode);
                        if (res == null) {
                            res = new ArrayList();
                            this.mhmInportIndex.put(objectNameInCode, res);
                        }
                        res.add(newPort);
                    }
                } else {
                    ArrayList res = (ArrayList) this.mhmInportIndex.get(objectNameInCode);
                    if (res == null) {
                        res = new ArrayList();
                        this.mhmInportIndex.put(objectNameInCode, res);
                    }
                    res.add(newPort);
                }
                if (newPort != null) {
                    try {
                        newPort.initialize(node);
                        if (srcPort != null) newPort.setDataTypeFromPort(srcPort);
                    } catch (Exception e) {
                        throw new KETLThreadException(e, this);
                    }
                    if (this.hmInports.put(newPort.mstrName, newPort) != null) throw new KETLThreadException("Duplicate IN port name exists, check step " + this.getName() + " port " + newPort.mstrName, this);
                }
            }
            if (wildCardPort != null) {
                ArrayList otherPorts = new ArrayList();
                pWorker.initializeAllOutPorts();
                String[] sources = ETLWorker.extractPortDetails(XMLHelper.getTextContent(wildCardPort));
                Node parent = wildCardPort.getParentNode();
                String channel = (sources.length == 3 ? sources[ETLWorker.CHANNEL] : null);
                parent.removeChild(wildCardPort);
                NamedNodeMap nm = wildCardPort.getAttributes();
                for (ETLOutPort src : pWorker.getOutPorts()) {
                    if (channel != null && src.getChannel().equals(channel) == false) continue;
                    if (srcPortsUsed.contains(src)) continue;
                    ETLPort ePort = (ETLPort) this.hmInports.get(src.mstrName);
                    if (ePort != null && ePort.isConstant()) continue;
                    if (ePort != null) {
                        throw new KETLThreadException("IN port already exists from another source with the same name check step " + this.getName() + " port " + src.mstrName, this);
                    }
                    Element e = parent.getOwnerDocument().createElement("IN");
                    for (int i = 0; i < nm.getLength(); i++) {
                        Node n = nm.item(i);
                        if (n instanceof Attr) e.setAttribute(((Attr) n).getName(), ((Attr) n).getValue());
                    }
                    e.setAttribute("NAME", src.mstrName);
                    e.setTextContent(pWorker.mstrName + "." + src.getChannel() + "." + src.mstrName);
                    parent.appendChild(e);
                    otherPorts.add(e);
                }
                if (otherPorts.size() > 0) {
                    nl = new Node[otherPorts.size()];
                    otherPorts.toArray(nl);
                    this.registerUsedPorts(pWorker, nl, objectNameInCode);
                }
            }
            if (wildCardOut != null) {
                NamedNodeMap nm = wildCardOut.getAttributes();
                for (Object o : this.hmInports.values()) {
                    ETLInPort export = (ETLInPort) o;
                    if (outExists.contains(export.mstrName)) continue;
                    Element e = this.getXMLConfig().getOwnerDocument().createElement("OUT");
                    for (int i = 0; i < nm.getLength(); i++) {
                        Node n = nm.item(i);
                        if (n instanceof Attr && ((Attr) n).getName().equals("DATATYPE") == false) e.setAttribute(((Attr) n).getName(), ((Attr) n).getValue());
                    }
                    e.setAttribute("NAME", export.mstrName);
                    e.setTextContent(EngineConstants.VARIABLE_PARAMETER_START + export.mstrName + EngineConstants.VARIABLE_PARAMETER_END);
                    this.getXMLConfig().appendChild(e);
                }
            }
            this.mFanInWorkerUsed.put(pWorker.mstrName, pWorker);
        } else {
            for (ETLOutPort p : duplicateSource.getOutPorts()) {
                if (p.isUsed()) pWorker.setOutUsed(p.getChannel(), p.mstrName);
            }
            pWorker.initializeAllOutPorts();
        }
    }

    /**
	 * Checks if is memory low.
	 * 
	 * @param pLowMemoryThreshold
	 *            the low memory threshold
	 * 
	 * @return true, if is memory low
	 */
    protected final boolean isMemoryLow(long pLowMemoryThreshold) {
        Runtime r = Runtime.getRuntime();
        long free = (r.maxMemory() - (r.totalMemory() - r.freeMemory()));
        if (free < pLowMemoryThreshold) return true;
        return false;
    }

    /** The controlled exit. */
    private boolean controlledExit = false;

    private boolean wasPreviouslyRun = false;

    public final void run() {
        try {
            try {
                synchronized (this.mThreadManager) {
                    ResourcePool.LogMessage(this, ResourcePool.DEBUG_MESSAGE, "Alive");
                }
                if (!wasPreviouslyRun) this.executeWorker();
                this.complete();
                this.controlledExit = true;
            } catch (java.lang.Error e) {
                throw new KETLThreadException(e, this);
            }
        } catch (InterruptedException e) {
            ResourcePool.LogMessage(this, ResourcePool.INFO_MESSAGE, "Worker interrupted");
            this.controlledExit = true;
            this.interruptAllSteps();
        } catch (Throwable e) {
            this.controlledExit = true;
            this.determineIfPrimary();
            if (e.getCause() != null && e.getCause() instanceof InterruptedException) {
                ResourcePool.LogMessage(this, ResourcePool.INFO_MESSAGE, "Worker interrupted");
                this.interruptAllSteps();
            } else if (this instanceof ETLStep && this.isPrimary()) {
                ETLStep step = (ETLStep) this;
                step.getJobExecutor().getCurrentETLJob().getStatus().setException(e);
                step.getJobExecutor().getCurrentETLJob().getStatus().setErrorMessage(e.getMessage());
            }
            this.interruptAllSteps();
        } finally {
            if (this.controlledExit == false) {
                ResourcePool.LogMessage(this, ResourcePool.ERROR_MESSAGE, "Step has shutdown in a non controlled, cause is unknown and all other steps will be interrupted");
                this.interruptAllSteps();
            }
        }
    }

    private void determineIfPrimary() {
        for (WorkerThread worker : this.mThreadManager.threads) {
            if (worker.step.primaryInterruptSource == null) worker.step.primaryInterruptSource = worker.step == this;
        }
    }

    private boolean isPrimary() {
        return this.primaryInterruptSource != null && this.primaryInterruptSource;
    }

    /** The fail all. */
    private boolean mFailAll = false;

    private Boolean primaryInterruptSource;

    /**
	 * Interrupt execution.
	 * 
	 * @throws InterruptedException
	 *             the interrupted exception
	 * @throws KETLThreadException
	 * @throws
	 * @throws Exception
	 */
    protected abstract void interruptExecution() throws InterruptedException, KETLThreadException;

    /**
	 * Interrupt all steps.
	 */
    public final void interruptAllSteps() {
        this.mThreadManager.jobThreadGroup.interrupt();
        this.mFailAll = true;
    }

    /**
	 * Clean shutdown.
	 * 
	 * @return true, if successful
	 */
    public final boolean cleanShutdown() {
        return this.controlledExit;
    }

    /**
	 * Fail all.
	 * 
	 * @return true, if successful
	 */
    public final boolean failAll() {
        return this.mFailAll;
    }

    /**
	 * Self tune.
	 * 
	 * @param arg0
	 *            the arg0
	 */
    public final void selfTune(boolean arg0) {
        this.selfTune = arg0;
    }

    /**
	 * Sets the batch manager.
	 * 
	 * @param batchManager
	 *            the new batch manager
	 */
    protected abstract void setBatchManager(BatchManager batchManager);

    /**
	 * Sets the core.
	 * 
	 * @param newCore
	 *            the new core
	 */
    abstract void setCore(DefaultCore newCore);

    /**
	 * Sets the output record data types.
	 * 
	 * @param pClassArray
	 *            the class array
	 * @param pChannel
	 *            the channel
	 */
    abstract void setOutputRecordDataTypes(Class[] pClassArray, String pChannel);

    /**
	 * Sets the out used.
	 * 
	 * @param pChannel
	 *            the channel
	 * @param pPort
	 *            the port
	 * 
	 * @return the ETL out port
	 * 
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    public final ETLOutPort setOutUsed(String pChannel, String pPort) throws KETLThreadException {
        if (this.getOutPort(pPort) == null) throw new KETLThreadException("Invalid port name " + this.mstrName + "." + pPort, this);
        if (pChannel == null) {
            pChannel = ETLWorker.getChannels(this.getXMLConfig())[ETLWorker.DEFAULT];
        }
        ETLOutPort port = this.hmOutports.get(pPort);
        HashSet al = (HashSet) this.mChannelPortsUsed.get(pChannel);
        if (al == null) {
            al = new HashSet();
            this.mChannelPortsUsed.put(pChannel, al);
        }
        al.add(pPort);
        port.used(true);
        if (port.getPortClass() == null) {
            try {
                port.setDataTypeFromPort(port.getAssociatedInPort());
            } catch (ClassNotFoundException e) {
                throw new KETLThreadException(e, this);
            }
        }
        return port;
    }

    @Override
    public String toString() {
        return this.mstrName + "(" + this.partitionID + ")";
    }

    /** The batch optimizer. */
    private BatchOptimizer mBatchOptimizer = null;

    public final void updateThreadStats(int rowCount) {
        this.recordCount += rowCount;
        if (this.mBatchOptimizer != null && this.selfTune && this.recordCount > this.tuneInterval) {
            this.mBatchOptimizer.optimize(this);
        }
    }

    public final void incrementTiming(long timing) {
        this.totalTimeNano += timing;
    }

    /**
	 * Close.
	 * 
	 * @param success
	 *            the success
	 * @param jobFailed
	 *            TODO
	 */
    protected abstract void close(boolean success, boolean jobFailed);

    /**
	 * Close step.
	 * 
	 * @param success
	 *            the success
	 * @param jobSuccess
	 */
    public void closeStep(boolean success, boolean jobSuccess) {
        this.close(success, jobSuccess);
    }

    /**
	 * Success.
	 * 
	 * @return true, if successful
	 */
    public abstract boolean success();

    /** The n format. */
    private final NumberFormat nFormat = NumberFormat.getNumberInstance();

    /** The Constant nano. */
    private static final double nano = Math.pow(10, 9);

    /** The Constant nanoToMilli. */
    private static final long nanoToMilli = (long) Math.pow(10, 6);

    /**
	 * Gets the timing.
	 * 
	 * @return the timing
	 */
    public String getTiming() {
        if (this.timing == false) return "N/A";
        return "" + this.nFormat.format(this.totalTimeNano / ETLWorker.nano) + " seconds";
    }

    /**
	 * Gets the CPU timing.
	 * 
	 * @return the CPU timing
	 */
    public long getCPUTiming() {
        return this.totalTimeNano / ETLWorker.nanoToMilli;
    }

    /** The waiting for. */
    private Object mWaitingFor = null;

    private boolean useCheckPoint;

    /**
	 * Sets the waiting.
	 * 
	 * @param arg0
	 *            the new waiting
	 */
    public void setWaiting(Object arg0) {
        if (arg0 == null) this.startTimeNano = System.nanoTime();
        this.mWaitingFor = arg0;
    }

    /**
	 * Checks if is waiting.
	 * 
	 * @return true, if is waiting
	 */
    public boolean isWaiting() {
        return this.mWaitingFor == null ? false : true;
    }

    /**
	 * Waiting for.
	 * 
	 * @return the object
	 */
    public Object waitingFor() {
        return this.mWaitingFor;
    }

    /**
	 * Switch target queue.
	 * 
	 * @param currentQueue
	 *            the current queue
	 * @param newQueue
	 *            the new queue
	 */
    public abstract void switchTargetQueue(ManagedBlockingQueue currentQueue, ManagedBlockingQueue newQueue);

    public boolean isWasPreviouslyRun() {
        return wasPreviouslyRun;
    }

    public void setWasPreviouslyRun(boolean wasPreviouslyRun) {
        this.wasPreviouslyRun = wasPreviouslyRun;
    }

    public boolean isUseCheckPoint() {
        return useCheckPoint;
    }

    public void setUseCheckPoint(boolean useCheckPoint) {
        this.useCheckPoint = useCheckPoint;
    }
}
