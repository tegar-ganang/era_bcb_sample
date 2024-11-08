package simulatorKit.simulation.enviroment;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import simulatorKit.simulation.Simulation;
import simulatorKit.simulation.util.SimRandom;

/**
 * This Class represent the Network layer abstraction in the Simulation
 * @author Igal
 */
public class Topology {

    protected static int count = 0;

    protected SimRandom randGenerator;

    /** Coficient of bandwidth multiplication of the given one from the Topology file */
    protected static final short BANDWIDTH_COFICIENT = 10;

    protected static final double RATIO_OF_SLOWEST = 0.1;

    /** List of the Node in the Graph of Topology */
    protected Node[] nodeList;

    protected Simulation simObj;

    /**
     * List of Edges in the Graph of Topology
     *@associates simulatorKit.simulation.enviroment.Edge
     * @supplierCardinality 0..*
     */
    protected Vector<TreeMap<Short, Edge>> edgeList;

    protected Topology() {
    }

    /**
     * Constractor
     * @param fileName name of the file with Topology parameters
     * @throws java.io.IOException in case of any error
     */
    public Topology(String fileName, long randomSeed, Simulation simObj) throws IOException {
        this.simObj = simObj;
        this.randGenerator = new SimRandom(randomSeed);
        ArrayList list = parseFile(fileName);
        int len = list.size();
        String line = null;
        String[] intStriVec = null;
        int graphsize = 0;
        for (int i = 0; i < len; i++) {
            line = (String) list.get(i);
            if (line.startsWith("GRAPH")) {
                line = (String) list.get(++i);
                intStriVec = line.split(" ");
                graphsize = Integer.parseInt(intStriVec[0]);
                this.nodeList = new Node[graphsize];
                this.edgeList = new Vector<TreeMap<Short, Edge>>(graphsize);
                this.simObj.getStatOutStream().println("N " + graphsize);
                continue;
            }
            if (line == "") continue;
            if (line.startsWith("VERTICES")) {
                i += graphsize;
                continue;
            }
            if (line.startsWith("EDGES")) {
                for (int k = 0; k < graphsize; k++) {
                    this.nodeList[k] = new Node();
                    this.edgeList.add(k, new TreeMap<Short, Edge>());
                }
                for (i = i + 1; i < len; i++) {
                    line = (String) list.get(i);
                    intStriVec = line.split(" ");
                    short from = Short.parseShort(intStriVec[0]);
                    short to = Short.parseShort(intStriVec[1]);
                    short latency = Short.parseShort(intStriVec[2]);
                    short bandwidth = (short) (Short.parseShort(intStriVec[3]) * (this.randGenerator.nextInt(Topology.BANDWIDTH_COFICIENT) + 1));
                    this.addEdges(from, to, bandwidth, latency);
                }
            }
        }
        list.clear();
        list = null;
        intStriVec = null;
        this.randGenerator.reset();
    }

    /**
     * @param other othe rtopology to build from 
     * @param simObj simulation refrence
     */
    public Topology(Topology other, Simulation simObj) {
        this.simObj = simObj;
        this.randGenerator = (SimRandom) other.randGenerator.clone();
        this.nodeList = other.nodeList;
        this.simObj.getStatOutStream().println("N " + other.nodeList.length);
        this.edgeList = other.edgeList;
        for (int i = 0; i < this.edgeList.size(); i++) {
            Iterator itr = this.edgeList.get(i).values().iterator();
            Edge currEdge;
            while (itr.hasNext()) {
                currEdge = (Edge) itr.next();
                if (currEdge.getToNodeIndex() < i) {
                    StringBuffer strBuff = new StringBuffer();
                    strBuff.append("E ").append(currEdge.getFromNodeIndex()).append(' ').append(currEdge.getToNodeIndex()).append(' ').append(currEdge.getLatency());
                    this.simObj.getStatOutStream().println(strBuff);
                }
            }
        }
        System.err.println("Konchil");
        this.removeAllStations();
    }

    /**
     * Parse the content of the File in to the ArrayList of Lines from this File
     * @param fileName name of the File
     * @throws java.io.IOException in any case of error
     * @return the parsed lines List
     */
    protected ArrayList parseFile(String fileName) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        String line = null;
        ArrayList<String> list = new ArrayList<String>();
        line = br.readLine();
        while (line != null) {
            list.add(line);
            line = br.readLine();
        }
        br.close();
        br = null;
        line = null;
        return list;
    }

    /**
     * Add the directed Edges between the to Nodes in the Graph og Topology
     * @param node1 first Node index
     * @param node2 second node index
     * @param bandwith the bandwidth of the edges
     * @param latency the latency on this edges
     */
    protected void addEdges(short node1, short node2, short bandwidth, short latency) {
        Edge newEdge1 = new Edge(node1, node2, bandwidth, latency);
        Edge newEdge2 = new Edge(node2, node1, bandwidth, latency);
        this.edgeList.get(node1).put(new Short(node2), newEdge1);
        this.edgeList.get(node2).put(new Short(node1), newEdge2);
        StringBuffer strBuff = new StringBuffer();
        strBuff.append("E ").append(node1).append(" ").append(node2).append(" ").append(latency);
        this.simObj.getStatisticOutStream().println(strBuff);
    }

    /** Helping method of Dijkstra algorithm , that prepare all Nodes in the Graph. */
    protected void initNodes() {
        for (int i = 0; i < this.nodeList.length; i++) {
            this.nodeList[i].initExtraProperties();
            this.nodeList[i].addExtraProperty("Known", new Boolean(false));
            this.nodeList[i].addExtraProperty("Distance", new Short(Short.MAX_VALUE));
        }
    }

    /**
     * Helping method of Dijkstra algorithm , that find and return the smallest uknown Node index.
     * @return the smallest uknown Node index
     */
    protected short getUnknownSmallestNode() {
        short ans = -1;
        short currDistance;
        short minDistance = Short.MAX_VALUE;
        for (short i = 0; i < this.nodeList.length; i++) {
            if (!((Boolean) this.nodeList[i].getExtraProperty("Known")).booleanValue()) {
                currDistance = ((Short) this.nodeList[i].getExtraProperty("Distance")).shortValue();
                if (currDistance < minDistance) {
                    minDistance = currDistance;
                    ans = i;
                }
            }
        }
        return ans;
    }

    /**
     *  Helping method of Dijkstra algorithm , that update algorithm
     * parameters of all Nodes and set the routing path to the "head" Node.
     * @param last Node index of currient Node treated by the algorithm
     * @param head Node index of the Node for wich we run the Dijkstra algorithm
     */
    protected void updateNeighbors(short last, short head) {
        Iterator itr;
        short neighbor, lastDistance, neighborDistance, edgeDistance;
        for (itr = this.edgeList.get(last).entrySet().iterator(); itr.hasNext(); ) {
            Map.Entry entry = (Map.Entry) itr.next();
            neighbor = ((Short) entry.getKey()).shortValue();
            if (((Boolean) this.nodeList[neighbor].getExtraProperty("Known")).booleanValue()) continue;
            lastDistance = ((Short) this.nodeList[last].getExtraProperty("Distance")).shortValue();
            neighborDistance = ((Short) this.nodeList[neighbor].getExtraProperty("Distance")).shortValue();
            edgeDistance = ((Edge) entry.getValue()).getLatency();
            if (neighborDistance > lastDistance + edgeDistance) {
                this.nodeList[neighbor].addExtraProperty("Distance", new Short((short) (lastDistance + edgeDistance)));
                this.nodeList[neighbor].setNextEdgeTo(new Short(head), (Edge) this.edgeList.get(neighbor).get(new Short(last)));
            }
        }
    }

    /** Helping method of Dijkstra algorithm , to remove all Extra properties added during the Dijkstra algorithm running. */
    protected void cleanNodes() {
        System.runFinalization();
        System.gc();
    }

    /**
     * This method set the routing pathes of all nodes in the Graph of Topology to the currient Node.
     * @param nodeNum the currient Node
     */
    public void setRoutePath(short nodeNum) {
        reSetRoutePath(nodeNum, false);
    }

    protected void reSetRoutePath(short nodeNum, boolean overwrite) {
        if (!overwrite && this.checkIfAlreadyRouted(nodeNum)) {
            System.err.println("Already routed");
            return;
        }
        this.initNodes();
        short last;
        this.nodeList[nodeNum].addExtraProperty("Distance", new Short((short) 0));
        count++;
        while (true) {
            last = getUnknownSmallestNode();
            if (last < 0) break;
            this.nodeList[last].addExtraProperty("Known", new Boolean(true));
            this.updateNeighbors(last, nodeNum);
        }
        this.cleanNodes();
        System.err.println(count + " " + new Date(System.currentTimeMillis()).toString());
    }

    protected boolean checkIfAlreadyRouted(short nodeNum) {
        short i;
        do {
            i = (short) Simulation.RandGenerator.nextInt(this.nodeList.length);
            if (i == nodeNum) continue;
            if (this.nodeList[i].getNextEdgeTo(new Short(nodeNum)) == null) return false; else return true;
        } while (true);
    }

    /**
     * Return the value of currient MTU (maximum transferied unit - bandwidth) of the directed edge defined by to Node indexes
     * @return the value of currient MTU (maximum transferied unit - bandwidth)
     * of the directed edge defined by to Node indexes
     * @param from Index of the from Node
     * @param to Index of the to Node
     */
    public int getCurrMTU(short from, short to) {
        if (from == to) {
            System.err.println(from + " | " + to);
            System.exit(-1);
        }
        Edge edge = (Edge) this.nodeList[from].getNextEdgeTo(new Short(to));
        int mtu = edge.getCurrBandwidth();
        from = edge.getToNodeIndex();
        while (from != to) {
            edge = (Edge) ((Node) nodeList[from]).getNextEdgeTo(new Short(to));
            if (edge.getCurrBandwidth() < mtu) mtu = edge.getCurrBandwidth();
            from = edge.getToNodeIndex();
        }
        return mtu;
    }

    /**
     * This method calculate and return the latency value of path between two Node indexes
     * @return the value of the latency value of path
     * @param from Index of the 'from Node'
     * @param to Index of the 'to Node'
     */
    public int getLatency(short from, short to) {
        Edge edge;
        int latency = 0;
        while (from != to) {
            edge = (Edge) ((Node) nodeList[from]).getNextEdgeTo(new Short(to));
            latency += edge.getLatency();
            from = edge.getToNodeIndex();
        }
        return latency;
    }

    /**
     * Return the value of default MTU (maximum transferied unit - bandwidth) of the directed edge defined by to Node indexes
     * @param from Index of the from Node
     * @param to Index of the to Node
     * @return the value of default MTU (maximum transferied unit - bandwidth) of the directed edge defined by to Node indexes
     */
    public int getMTU(short from, short to) {
        Edge edge = (Edge) this.nodeList[from].getNextEdgeTo(new Short(to));
        int mtu = edge.getBandwidth();
        from = edge.getToNodeIndex();
        while (from != to) {
            edge = (Edge) ((Node) nodeList[from]).getNextEdgeTo(new Short(to));
            if (edge.getBandwidth() < mtu) mtu = edge.getBandwidth();
            from = edge.getToNodeIndex();
        }
        return mtu;
    }

    /**
     * transmit number of parts from the "from" Node to the "to" Node.
     * @param from index of the "from" Node
     * @param to index of the "to" Node
     * @param numOfParts number of parts to tranthmit
     */
    public void transmit(short from, short to, short numOfParts) throws Exception {
        Edge edge;
        this.simObj.getStatisticOutStream().print("T " + numOfParts + " ");
        while (from != to) {
            edge = (Edge) ((Node) nodeList[from]).getNextEdgeTo(new Short(to));
            edge.transfer(numOfParts);
            this.simObj.getStatisticOutStream().print(from + " ");
            from = edge.getToNodeIndex();
        }
        this.simObj.getStatisticOutStream().println(from);
    }

    /** Reset the all Graph's Entities to the default state. */
    public void reset() {
        for (TreeMap<Short, Edge> currMap : this.edgeList) {
            for (Edge currEdge : currMap.values()) {
                currEdge.reset();
            }
        }
    }

    /**
     * Print the path from the "from" Node to the "to" Node to the "ostream". (Temp method will be removed ).
     * @param from index of the "from" node
     * @param to index of the "to" node
     */
    public void printPath(short from, short to) {
        while (from != to) {
            System.out.print(from + "->");
            Edge edge = this.nodeList[from].getNextEdgeTo(new Short(to));
            if (edge == null) {
                from = to;
                System.out.println("No path found");
            } else from = edge.getToNodeIndex();
        }
        System.out.print(to + "\n");
    }

    /**
     * Find end return the index of the Free Node ( not Station )
     * @return index of the Node or -1 of not found.
     */
    public short getFreeNode() {
        LinkedList<Short> freeNodes = new LinkedList<Short>();
        for (short i = 0; i < this.nodeList.length; i++) {
            if (!this.nodeList[i].getIsStation()) {
                freeNodes.add(new Short(i));
            }
        }
        if (freeNodes.size() == 0) return -1;
        short answer = ((Short) freeNodes.get(this.randGenerator.nextInt(freeNodes.size()))).shortValue();
        freeNodes.clear();
        freeNodes = null;
        return answer;
    }

    /**
     * Return Node with the specific index in the Topology.
     * @param index index of Node
     * @return Node with the specific index in the Topology
     */
    public Node getNode(int index) {
        return this.nodeList[index];
    }

    /**
     * Calculate and Return the number of Hops ( edges ) on the pass between two Nodes
     * @param from Index of the 'from Node'
     * @param to Index of the 'to Node'
     * @return the number of Hops
     */
    public int getNumOfHops(short from, short to) {
        int hops = 0;
        while (from != to) {
            Edge edge = (Edge) this.nodeList[from].getNextEdgeTo(new Short(to));
            from = edge.getToNodeIndex();
            hops++;
        }
        return hops;
    }

    /**
     * Clean the dynamic properties of topology
     */
    public void cleanDynamicProperites() {
        System.runFinalization();
        System.gc();
    }

    protected SimRandom getRandGenerator() {
        return randGenerator;
    }

    /**
     * Set the main random generator of Topology
     * @param randGenerator
     */
    public void setRandGenerator(SimRandom randGenerator) {
        this.randGenerator = randGenerator;
    }

    /**
     * Set the main random generator of Topology
     * @param seed
     */
    public void setRandGenerator(long seed) {
        this.randGenerator = new SimRandom(seed);
    }

    /**
     * clear all stations from topology and reset the random generator
     */
    public void removeAllStations() {
        this.reset();
        for (int i = 0; i < this.nodeList.length; i++) {
            this.nodeList[i].setIsStation(false);
        }
        this.randGenerator.reset();
    }
}
