package uqdsd.infosec.analysis;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import uqdsd.infosec.GlobalProperties;
import uqdsd.infosec.model.DataFlow;
import uqdsd.infosec.model.ProgressListener;
import uqdsd.infosec.model.faultset.FaultMode;
import uqdsd.infosec.model.faultset.FaultModeSet;
import uqdsd.infosec.model.visitors.LiftingVisitor;
import uqdsd.infosec.model.visitors.XMLVisitor;
import uqdsd.infosec.tasks.KillableThread;

/**
 * @author InfoSec Project (c) 2008 UQ Performs the directed path analysis using an adjacency
 *         matrix
 */
public class DepthFirstPathsAnalysis extends KillableThread {

    private LiftingVisitor lifting;

    private boolean preprogrammed = false;

    private ProgressListener progress = null;

    private Set<String> redSource = new HashSet<String>();

    private Set<String> blackSink = new HashSet<String>();

    private DefaultMutableTreeNode[] pathTrees;

    private ArcTree[] arcTrees;

    private DefaultMutableTreeNode[] portTrees;

    private int[] resultTreeSizes;

    private int pofLimit;

    private double probabilityLowerBound = 0.0;

    private int numPathsFound;

    private int maxPathLength;

    private boolean maxPathLengthReachedFlag = false;

    private String progressNotePrefix;

    @SuppressWarnings("unchecked")
    private HashMap<String, Vector> flowAnalysis = new HashMap<String, Vector>();

    private static boolean debug = false;

    private boolean findOnePathOnly = false;

    private boolean followArcDupes = true;

    private void initVariables(int pofL, int maxPathLength) {
        pofLimit = pofL;
        pathTrees = new DefaultMutableTreeNode[pofL + 1];
        arcTrees = new ArcTree[pofL + 1];
        portTrees = new DefaultMutableTreeNode[pofL + 1];
        resultTreeSizes = new int[pofL + 1];
        debug = GlobalProperties.getInteger("Search.DebugLevel") >= 2;
        followArcDupes = GlobalProperties.getCondition("Search.FollowArcLabelDupes");
        this.maxPathLength = maxPathLength;
    }

    public DepthFirstPathsAnalysis(LiftingVisitor lifting, String theRed, String theBlack, int pofLimit) {
        super();
        initVariables(pofLimit, GlobalProperties.getInteger("Search.MaxPathLength"));
        preprogrammed = true;
        redSource.add(theRed);
        blackSink.add(theBlack);
        this.lifting = lifting;
    }

    public DepthFirstPathsAnalysis(LiftingVisitor lifting, String theRed, String theBlack) {
        super();
        initVariables(GlobalProperties.getInteger("Search.POFLimit"), GlobalProperties.getInteger("Search.MaxPathLength"));
        preprogrammed = true;
        redSource.add(theRed);
        blackSink.add(theBlack);
        this.lifting = lifting;
    }

    public DepthFirstPathsAnalysis(LiftingVisitor lifting) {
        super();
        initVariables(GlobalProperties.getInteger("Search.POFLimit"), GlobalProperties.getInteger("Search.MaxPathLength"));
        this.lifting = lifting;
    }

    public DepthFirstPathsAnalysis(LiftingVisitor lifting, int pofLimit) {
        super();
        initVariables(pofLimit, GlobalProperties.getInteger("Search.MaxPathLength"));
        this.lifting = lifting;
    }

    public boolean maxPathLengthReached() {
        return maxPathLengthReachedFlag;
    }

    public double getProbabilityLowerBound() {
        return probabilityLowerBound;
    }

    public void setProbabilityLowerBound(double lowestMinimumProbability) {
        if (lowestMinimumProbability >= 0.0 && lowestMinimumProbability <= 1.0) {
            this.probabilityLowerBound = lowestMinimumProbability;
        }
    }

    public int getNumPathsFound() {
        return numPathsFound;
    }

    public int getPOFLimit() {
        return pofLimit;
    }

    public void beginAnalysis(ProgressListener progress) {
        this.progress = progress;
        start();
    }

    @Override
    protected synchronized void finish() {
        super.finish();
        if (Integer.parseInt(GlobalProperties.getProperty("Search.DebugLevel")) >= 1) {
            try {
                PrintWriter pw = new PrintWriter(System.out);
                writeReport(pw);
                pw.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        beginFindAll();
        finish();
    }

    private Set<String> getAllBlackSinks() {
        return preprogrammed ? blackSink : lifting.getAllBlackSinks();
    }

    private Set<String> getAllRedSources() {
        return preprogrammed ? redSource : lifting.getAllRedSources();
    }

    @SuppressWarnings("unchecked")
    public Map getFlowAnalysisResults() {
        return flowAnalysis;
    }

    public void beginFindOne() {
        findOnePathOnly = true;
        begin();
    }

    public void beginFindOne(int maxPathLength) {
        this.maxPathLength = maxPathLength;
        beginFindOne();
    }

    public void beginFindAll() {
        findOnePathOnly = false;
        begin();
    }

    private void begin() {
        if (!getAllBlackSinks().isEmpty()) {
            int numberDone = 0;
            numPathsFound = 0;
            for (String redSource : getAllRedSources()) {
                progressNotePrefix = "Searching from " + redSource;
                if (progress != null) progress.note(progressNotePrefix);
                ArrayList<FaultModeSet> faultSets = new ArrayList<FaultModeSet>();
                faultSets.add(new FaultModeSet());
                ArrayList<String> destination = new ArrayList<String>();
                TreeSet<String> destination_set = new TreeSet<String>();
                destination.add(redSource);
                destination_set.add(redSource);
                LinkedList<Map<String, FaultModeSet>> componentBindings = new LinkedList<Map<String, FaultModeSet>>();
                componentBindings.add(new TreeMap<String, FaultModeSet>());
                exploreAdjacency(faultSets, destination, destination_set, new ArrayList<String>(), componentBindings, false, null, numberDone++, 0);
                if (findOnePathOnly && numPathsFound > 0) {
                    return;
                }
            }
        }
        for (int ii = 0; ii <= pofLimit; ++ii) {
            if (pathTrees[ii] != null) {
                pathTrees[ii].setUserObject(resultTreeSizes[ii] + " path" + (resultTreeSizes[ii] != 1 ? "s" : "") + " found.");
            }
            if (portTrees[ii] != null) {
                portTrees[ii].setUserObject("Port Tree");
            }
        }
    }

    public DefaultMutableTreeNode getPortTree(int pof) {
        return portTrees[pof];
    }

    public DefaultMutableTreeNode getPathTree(int pof) {
        return pathTrees[pof];
    }

    public ArcTree getArcTree(int pof) {
        return arcTrees[pof];
    }

    private void exploreAdjacency(List<FaultModeSet> faultSets, List<String> destination, Set<String> destination_set, List<String> arcs, List<Map<String, FaultModeSet>> componentBindings, boolean collatedbailout, String arcbailout, int numberDone, int pof) {
        if (isInterrupted()) {
            finish();
            return;
        }
        if (destination.size() > maxPathLength) {
            maxPathLengthReachedFlag = true;
            return;
        }
        String lastDestination = destination.get(destination.size() - 1).toString();
        int columnCount = lifting.getAdjacencyMatrix().getNumberOfColumns();
        boolean isSink = true;
        boolean viableReason = false;
        for (int c = 0; c < columnCount; c++) {
            String proposedStep = lifting.getAdjacencyMatrix().getColumnName(c);
            if (debug) {
                System.out.println(destination + "--?-->" + proposedStep);
            }
            if (destination_set.contains(proposedStep)) {
                continue;
            }
            FaultModeSet fss = lifting.getAdjacencyMatrix().getValueAt(lifting.getAdjacencyMatrix().getRowIndex(lastDestination), c);
            if (fss.isEmpty()) {
                continue;
            }
            Map<String, FaultModeSet> newComponentBindings = componentBindings.get(componentBindings.size() - 1);
            String association = "", name = "", description = "", instance = "";
            for (FaultMode faultMode : fss.getAllFaultModes()) {
                association = faultMode.getAssociation();
                name = faultMode.getAbbreviation();
                description = faultMode.getDescription();
                instance = faultMode.getAssociatedInstance();
                break;
            }
            String componentassociation = (instance == null ? "" : instance) + "::" + association;
            boolean isAnArc = association.startsWith(LiftingVisitor.CONNECTIONON);
            boolean isCollated = false;
            if (!isAnArc) {
                if (!lifting.getComponentDefinition(association).get(0).getComponentModel().isTransitive()) {
                    if (collatedbailout) {
                        viableReason = true;
                        if (debug) {
                            System.out.println(proposedStep + " not considered (collated)");
                        }
                        continue;
                    }
                    isCollated = true;
                }
                if (componentBindings.get(componentBindings.size() - 1).containsKey(componentassociation)) {
                    fss = componentBindings.get(componentBindings.size() - 1).get(componentassociation).intersect(fss);
                }
                if (fss.isEmpty()) {
                    viableReason = true;
                    if (debug) {
                        System.out.println(proposedStep + " not considered (incompatible modes)");
                        System.out.println(componentBindings);
                    }
                    continue;
                }
                newComponentBindings = new TreeMap<String, FaultModeSet>(componentBindings.get(componentBindings.size() - 1));
                if (newComponentBindings.containsKey(componentassociation)) {
                    newComponentBindings.remove(componentassociation);
                }
                newComponentBindings.put(componentassociation, fss);
                pof = 0;
                double max_path_prob = 0.0;
                for (FaultModeSet fs : newComponentBindings.values()) {
                    boolean normalFound = false;
                    double max_prob_fault_set = 0.0;
                    for (FaultMode fm : fs.getAllFaultModes()) {
                        if (fm.isNormal()) {
                            normalFound = true;
                        }
                        if (fm.getProbability() > max_prob_fault_set) {
                            max_prob_fault_set = fm.getProbability();
                        }
                    }
                    max_path_prob *= max_prob_fault_set;
                    if (!normalFound) {
                        if (++pof > pofLimit) {
                            break;
                        }
                    } else {
                        max_path_prob = 1.0;
                        if (max_path_prob < probabilityLowerBound) {
                            break;
                        }
                    }
                }
                if (pof > pofLimit) {
                    viableReason = true;
                    if (debug) {
                        System.out.println(proposedStep + " not considered (too many faults)");
                    }
                    continue;
                }
                if (max_path_prob < probabilityLowerBound) {
                    viableReason = true;
                    if (debug) {
                        System.out.print(proposedStep + " not considered (lowest min probability reached) ");
                        System.out.println(max_path_prob + " < " + probabilityLowerBound);
                    }
                    continue;
                }
            } else {
                if (!followArcDupes && arcbailout != null && name.equals(arcbailout)) {
                    viableReason = true;
                    if (debug) {
                        System.out.println(proposedStep + " not considered (arc duplication)");
                    }
                    continue;
                }
                int idx = description.indexOf(LiftingVisitor.INDEXCODE);
                if (idx >= 0) {
                    int lookupCode = Integer.parseInt(description.substring(idx + LiftingVisitor.INDEXCODE.length()));
                    if (lifting.lookupArc(lookupCode).getChannelInformation() == DataFlow.NO_DATA) {
                        viableReason = true;
                        if (debug) {
                            System.out.println(proposedStep + " not considered (not a data channel)");
                        }
                        continue;
                    }
                }
            }
            if (getAllRedSources().contains(proposedStep)) {
                viableReason = true;
                if (debug) {
                    System.out.println(proposedStep + " not considered (red source encountered)");
                }
                continue;
            }
            if (getAllBlackSinks().contains(proposedStep)) {
                Iterator<FaultModeSet> fsItr = faultSets.iterator();
                Iterator<String> destItr = destination.iterator();
                Iterator<Map<String, FaultModeSet>> bindingsItr = componentBindings.iterator();
                if (pathTrees[pof] == null) {
                    pathTrees[pof] = new DefaultMutableTreeNode();
                    portTrees[pof] = new DefaultMutableTreeNode();
                    resultTreeSizes[pof] = 0;
                }
                DefaultMutableTreeNode displayPointer = pathTrees[pof];
                DefaultMutableTreeNode portPointer = portTrees[pof];
                while (fsItr.hasNext()) {
                    FaultModeSet fsnext = fsItr.next();
                    String dest = destItr.next();
                    Map<String, FaultModeSet> bindings = bindingsItr.next();
                    Enumeration<?> displayChildren = displayPointer.children();
                    Enumeration<?> portChildren = portPointer.children();
                    DefaultMutableTreeNode displayFuturePointer = null;
                    DefaultMutableTreeNode portFuturePointer = null;
                    StepDescription stepDesc = new StepDescription(fsnext, dest);
                    stepDesc.setBindings(bindings);
                    while (displayChildren.hasMoreElements()) {
                        DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) displayChildren.nextElement();
                        if (dmtn.getUserObject().equals(stepDesc)) {
                            displayFuturePointer = dmtn;
                            break;
                        }
                    }
                    while (portChildren.hasMoreElements()) {
                        DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) portChildren.nextElement();
                        if (dmtn.getUserObject().equals(dest)) {
                            portFuturePointer = dmtn;
                            break;
                        }
                    }
                    if (displayFuturePointer == null) {
                        displayFuturePointer = new DefaultMutableTreeNode();
                        displayFuturePointer.setUserObject(stepDesc);
                        displayPointer.add(displayFuturePointer);
                    }
                    if (portFuturePointer == null) {
                        portFuturePointer = new DefaultMutableTreeNode();
                        portFuturePointer.setUserObject(dest);
                        portPointer.add(portFuturePointer);
                    }
                    displayPointer = displayFuturePointer;
                    portPointer = portFuturePointer;
                }
                StepDescription finalStep = new StepDescription(fss, proposedStep);
                finalStep.setBindings(newComponentBindings);
                displayPointer.add(new DefaultMutableTreeNode(finalStep));
                portPointer.add(new DefaultMutableTreeNode(proposedStep));
                resultTreeSizes[pof]++;
                ++numPathsFound;
                if (progress != null) progress.note(progressNotePrefix + " - " + numPathsFound + " paths found.");
                if (debug) {
                    System.out.println(" PATH FOUND -> " + proposedStep);
                }
                if (arcTrees[pof] == null) {
                    arcTrees[pof] = new ArcTree(null, "root");
                }
                arcTrees[pof].addPath(arcs);
                if (findOnePathOnly) {
                    return;
                }
                continue;
            }
            List<String> newDestination = new ArrayList<String>(destination);
            Set<String> newDestination_set = new TreeSet<String>(destination_set);
            List<FaultModeSet> newFaultSets = new ArrayList<FaultModeSet>(faultSets);
            List<String> newArcs = new ArrayList<String>(arcs);
            newDestination.add(proposedStep);
            newDestination_set.add(proposedStep);
            newFaultSets.add(fss);
            List<Map<String, FaultModeSet>> newComponentBindingsList = new LinkedList<Map<String, FaultModeSet>>(componentBindings);
            newComponentBindingsList.add(newComponentBindings);
            if (isAnArc) {
                newArcs.add(name + " from " + lastDestination + description);
            }
            isSink = false;
            exploreAdjacency(newFaultSets, newDestination, newDestination_set, newArcs, newComponentBindingsList, isCollated, isAnArc ? name : null, -1, pof);
        }
        if (debug && isSink && !viableReason) {
            System.out.println(destination);
            System.out.println("** SINK **");
            System.out.println(componentBindings);
        }
    }

    public void writeReport(Writer target) throws IOException {
        Document doc = new Document();
        Element root_element = new Element("AllPathsAnalysisResults");
        doc.setRootElement(root_element);
        for (int i = 0; i < pofLimit + 1; i++) {
            if (pathTrees[i] != null) {
                Element tree_element = new Element("Tree");
                root_element.addContent(tree_element);
                tree_element.setAttribute("pof", "" + i);
                Enumeration<?> e = pathTrees[i].children();
                while (e.hasMoreElements()) {
                    writeTree(tree_element, (DefaultMutableTreeNode) e.nextElement(), target);
                }
            }
        }
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        outputter.output(doc, target);
    }

    private void writeTree(Element parent_element, DefaultMutableTreeNode tree, Writer target) throws IOException {
        Element child_element = ((StepDescription) tree.getUserObject()).toXML();
        parent_element.addContent(child_element);
        Enumeration<?> e = tree.children();
        if (e.hasMoreElements()) {
            while (e.hasMoreElements()) {
                writeTree(child_element, (DefaultMutableTreeNode) e.nextElement(), target);
            }
        }
    }

    public class StepDescription {

        private String destination = "";

        private FaultModeSet arc = null;

        private Map<String, FaultModeSet> bindings = null;

        public StepDescription(FaultModeSet arc, String destination) {
            this.arc = arc;
            this.destination = destination;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof StepDescription) {
                StepDescription sd = (StepDescription) obj;
                if (sd.destination.equals(destination)) {
                    if (arc != null) {
                        return sd.arc.equals(arc);
                    } else {
                        return sd.arc == null;
                    }
                }
            }
            return false;
        }

        @Override
        public String toString() {
            if (arc.isEmpty()) {
                return "Starting at source " + destination;
            } else {
                return arc.toString() + " to reach " + destination;
            }
        }

        public Element toXML() {
            Element toReturn = new Element("Reach");
            toReturn.setAttribute("port", destination);
            if (!arc.isEmpty()) {
                toReturn.addContent(XMLVisitor.toXML(arc));
            }
            return toReturn;
        }

        public FaultModeSet getArc() {
            return arc;
        }

        public void setArc(FaultModeSet arc) {
            this.arc = arc;
        }

        public String getDestination() {
            return destination;
        }

        public void setDestination(String destination) {
            this.destination = destination;
        }

        public Map<String, FaultModeSet> getBindings() {
            return bindings;
        }

        public void setBindings(Map<String, FaultModeSet> bindings) {
            this.bindings = bindings;
        }
    }

    public class ArcTree implements TreeNode {

        private Vector<ArcTree> children = new Vector<ArcTree>();

        private ArcTree parent = null;

        private String arcName;

        private int lookup;

        public ArcTree(ArcTree parent, String arcName) {
            this.parent = parent;
            int indexLocate = arcName.indexOf(LiftingVisitor.INDEXCODE);
            if (indexLocate < 0) {
                lookup = -1;
                this.arcName = arcName;
            } else {
                this.arcName = arcName.substring(0, indexLocate);
                lookup = Integer.parseInt(arcName.substring(indexLocate + 5));
            }
        }

        public int getLookup() {
            return lookup;
        }

        public boolean isHashedCutset(int[] radix, int[] cutset, int size) {
            for (int ii = 0; ii < size; ++ii) {
                if (radix[cutset[ii]] == lookup) {
                    return true;
                }
            }
            Enumeration<ArcTree> ii = children();
            while (ii.hasMoreElements()) {
                ArcTree child = ii.nextElement();
                if (!child.isHashedCutset(radix, cutset, size)) {
                    return false;
                }
            }
            return !isLeaf();
        }

        public void addChild(ArcTree child) {
            children.add(child);
        }

        public String getName() {
            return arcName;
        }

        @Override
        public String toString() {
            return getName();
        }

        public void addPath(List<String> path) {
            if (path.isEmpty()) {
                return;
            }
            String first = path.get(0);
            int childIndex = children.indexOf(first);
            ArcTree child = null;
            if (childIndex < 0) {
                child = new ArcTree(this, first);
                addChild(child);
            } else {
                child = children.get(childIndex);
            }
            path.remove(0);
            child.addPath(path);
        }

        public Enumeration<ArcTree> children() {
            return children.elements();
        }

        public boolean getAllowsChildren() {
            return true;
        }

        public TreeNode getChildAt(int childIndex) {
            return children.get(childIndex);
        }

        public int getChildCount() {
            return children.size();
        }

        public int getIndex(TreeNode node) {
            return children.indexOf(node);
        }

        public TreeNode getParent() {
            return parent;
        }

        public boolean isLeaf() {
            return children.isEmpty();
        }
    }
}
