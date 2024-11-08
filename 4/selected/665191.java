package com.ibm.realtime.flexotask.scheduling.streaming;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import at.dms.kjc.backendSupport.BackEndFactory;
import at.dms.kjc.backendSupport.BackEndScaffold;
import at.dms.kjc.backendSupport.Channel;
import at.dms.kjc.backendSupport.CodeStoreHelper;
import at.dms.kjc.backendSupport.ComputeCodeStore;
import at.dms.kjc.backendSupport.ComputeNode;
import at.dms.kjc.backendSupport.ComputeNodesI;
import at.dms.kjc.backendSupport.FilterInfo;
import at.dms.kjc.slicegraph.Edge;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.IDSliceRemoval;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.OutputSliceNode;
import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.slicegraph.SliceNode;

/** 
 * This back end factory class is used to create the chains of tasks for each
 * compute node used to execute the graph. 
 */
class StreamBackEndFactory extends BackEndFactory {

    /** Debugging flag */
    private static final boolean debug = true;

    /** The compute nodes */
    private StreamComputeNodes nodes;

    /** The BackEndScaffold */
    private BackEndScaffold scaffolding;

    /** A map from compute node to list of FSNs */
    private final LinkedHashMap<ComputeNode, List<FilterSliceNode>> steadyComputeNodeChains = new LinkedHashMap<ComputeNode, List<FilterSliceNode>>();

    /** A list of FSNs; the init phase we always run with a single thread */
    private final HashMap<String, List<FilterSliceNode>> taskNameToChain = new HashMap();

    /**
	 * Constructor.
	 * @param numOfNodes the number of compute nodes.
	 */
    public StreamBackEndFactory(int numOfNodes) {
        this(new StreamComputeNodes(numOfNodes));
    }

    /**
	 * Constructor.
	 * @param nodes the compute nodes.
	 */
    public StreamBackEndFactory(StreamComputeNodes nodes) {
        if (nodes == null) {
            nodes = new StreamComputeNodes(1);
        }
        this.nodes = nodes;
    }

    public BackEndScaffold getBackEndMain() {
        if (scaffolding == null) {
            scaffolding = new BackEndScaffold();
        }
        return scaffolding;
    }

    public Channel getChannel(Edge e) {
        return null;
    }

    public Channel getChannel(SliceNode src, SliceNode dst) {
        return null;
    }

    public CodeStoreHelper getCodeStoreHelper(SliceNode node) {
        return null;
    }

    public ComputeCodeStore getComputeCodeStore(ComputeNode arg0) {
        return null;
    }

    public ComputeNode getComputeNode(Object arg0) {
        if (debug) System.err.println("getComputeNode " + arg0);
        return nodes.getNthComputeNode(((Integer) arg0).intValue());
    }

    public ComputeNodesI getComputeNodes() {
        return nodes;
    }

    public void processFilterSliceNode(FilterSliceNode filter, at.dms.kjc.slicegraph.SchedulingPhase whichPhase, ComputeNodesI computeNodes) {
        if (filter.getFilter().getName().startsWith("Identity__")) {
            IDSliceRemoval.doit(filter.getParent());
            return;
        }
        if ((debug) && (whichPhase == at.dms.kjc.slicegraph.SchedulingPhase.STEADY)) System.err.println("processFilterSliceNode: " + filter.getFilter().getName() + " -- Multiplicity: " + FilterInfo.getFilterInfo(filter).steadyMult + " CN: " + "" + getLayout().getComputeNode(filter));
        if (whichPhase == at.dms.kjc.slicegraph.SchedulingPhase.STEADY) {
            if (!steadyComputeNodeChains.containsKey(getLayout().getComputeNode(filter))) {
                steadyComputeNodeChains.put(getLayout().getComputeNode(filter), new LinkedList<FilterSliceNode>());
            }
            LinkedList list = (LinkedList) steadyComputeNodeChains.get(getLayout().getComputeNode(filter));
            taskNameToChain.put(StreamScheduler.getTaskNameFromFSN(filter), list);
            list.addLast(filter);
        }
    }

    public void processFilterSlices(Slice slice, at.dms.kjc.slicegraph.SchedulingPhase whichPhase, ComputeNodesI computeNodes) {
    }

    public void processInputSliceNode(InputSliceNode input, at.dms.kjc.slicegraph.SchedulingPhase whichPhase, ComputeNodesI computeNodes) {
    }

    public void processOutputSliceNode(OutputSliceNode output, at.dms.kjc.slicegraph.SchedulingPhase whichPhase, ComputeNodesI computeNodes) {
    }

    /**
	 * Returns the map mapping from name to a chain of tasks.
	 * @return the map mapping from name to a chain of tasks.
	 */
    Map<String, List<FilterSliceNode>> getNameToChainMap() {
        return taskNameToChain;
    }

    /**
	 * Returns a list of chains, where each chain is a collection of tasks
	 * to be executed on a single compute node for the steady phase. 
	 * @return a list of chains, where each chain is a collection of tasks
	 * to be executed on a single compute node for the steady phase.
	 */
    List<List<FilterSliceNode>> getSteadyComputeNodeChains() {
        return new LinkedList(steadyComputeNodeChains.values());
    }
}
