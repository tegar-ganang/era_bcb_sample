package repast.simphony.visualization.cgd;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.visualization.cgd.graph.CGDEdge;
import repast.simphony.visualization.cgd.graph.CGDGraph;
import repast.simphony.visualization.cgd.graph.CGDNode;

public class CGDProcessor {

    CGDGraph graph = new CGDGraph();

    Network rootGraph;

    HashMap<Network, CGDGraph> translation = new HashMap<Network, CGDGraph>();

    HashMap<Object, Object> objectData = new HashMap<Object, Object>();

    HashMap<Object, Integer> indexNumber = new HashMap<Object, Integer>();

    int rootNumbNodes = 0;

    public HashMap processGraph(Network rGraph) {
        HashMap result = new HashMap();
        createCGDGraph(rGraph);
        graph.compute();
        transferFromCGDGraphToOrig();
        return result;
    }

    private void createCGDGraph(Network rGraph) {
        ArrayList nodes = new ArrayList();
        HashMap edges = new HashMap();
        Iterator itN = rGraph.getNodes().iterator();
        Iterator itE = rGraph.getEdges().iterator();
        int eIndex = 0;
        int maxEIndex = 0;
        int nIndex = 0;
        int maxNIndex = 0;
        int n = 0;
        while (itN.hasNext()) {
            Object fNode = itN.next();
            CGDNode node = new CGDNode();
            node.setIdentifier(rGraph.getName());
            node.setIndex(n);
            nodes.add(node);
            indexNumber.put(fNode, n);
            objectData.put(fNode, node);
            n++;
        }
        rootNumbNodes = nodes.size();
        graph.setNodes(nodes);
        graph.setMaxNIndex(maxNIndex);
        while (itE.hasNext()) {
            RepastEdge fEdge = (RepastEdge) itE.next();
            Object fSource = fEdge.getSource();
            ;
            int s = indexNumber.get(fSource);
            CGDNode source = (CGDNode) graph.getNode(s);
            Object fTarget = fEdge.getTarget();
            int t = indexNumber.get(fTarget);
            CGDNode target = (CGDNode) graph.getNode(t);
            CGDEdge edge = new CGDEdge(source, target);
            edge.setIdentifier(rGraph.getName());
            eIndex = s;
            edge.setIndex(eIndex);
            if (maxEIndex < eIndex) maxEIndex = eIndex;
            edges.put(new Point(s, t), edge);
            n++;
        }
        graph.setEdges(edges);
        translation.put(rGraph, graph);
    }

    public HashMap getFinalEdges() {
        HashMap results = new HashMap();
        return results;
    }

    public HashMap getFinalNodes() {
        HashMap results = new HashMap();
        return results;
    }

    private boolean transferFromCGDGraphToOrig() {
        boolean result = false;
        ArrayList nodes = graph.getNodes();
        HashMap edges = graph.getEdges();
        if (nodes == null) return result;
        if (nodes.size() != rootNumbNodes) {
            System.out.println("nodes.size=" + nodes.size() + " and origNumbNodes=" + rootNumbNodes);
            return result;
        }
        for (int i = 0; i < nodes.size(); i++) {
            CGDNode cgdNode = (CGDNode) nodes.get(i);
            int nIndex = cgdNode.getIndex();
            Object node = null;
            Iterator ic = objectData.entrySet().iterator();
            while (ic.hasNext()) {
                Map.Entry e = (Map.Entry) ic.next();
                CGDNode o = (CGDNode) e.getValue();
                if (o.equals(cgdNode)) {
                    node = e.getKey();
                }
            }
        }
        result = true;
        return result;
    }

    public HashMap getObjectData() {
        return objectData;
    }
}
