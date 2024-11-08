package gr.demokritos.iit.conceptualIndex.documentModel;

import gr.demokritos.iit.jinsect.structs.EdgeCachedLocator;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import gr.demokritos.iit.jinsect.events.NotificationListener;
import gr.demokritos.iit.jinsect.structs.UniqueVertexGraph;
import gr.demokritos.iit.jinsect.structs.UniqueVertexHugeGraph;
import gr.demokritos.iit.jinsect.utils;
import salvo.jesus.graph.*;

/** Represents a graph of symbols of different character lengths. Each symbol is connected
 *to any symbols of smaller size that compose it. Symbols of higher size (also called rank)
 *are considered to be offspring of lower rank symbols that compose the former.
 *
 * @author ggianna
 */
public class SymbolicGraph extends UniqueVertexHugeGraph implements gr.demokritos.iit.jinsect.events.Notifier {

    /** The maximum size (in characters) of the n-grams appearing in this graph.
     */
    int MaxNGramSize;

    /** The minimum size (in characters) of the n-grams appearing in this graph.
     */
    int MinNGramSize;

    /**A listener used to notify of progress during long processes. Defaults to <code>null</code>.
     */
    private NotificationListener Listener = null;

    /**The datastring corresponding to the loaded data
     */
    protected String DataString;

    /**The alphabet of symbols (characters) contained in the data string
     */
    protected Set Alphabet = null;

    /** Creates a new instance of SymbolicGraph given a range of n-gram ranks (lengths).  
     *@param iMinNGramSize The minimum n-gram size within this graph.
     *@param iMaxNGramSize The maximum n-gram size within this graph.
     */
    public SymbolicGraph(int iMinNGramSize, int iMaxNGramSize) {
        super(16);
        MaxNGramSize = iMaxNGramSize;
        MinNGramSize = iMinNGramSize;
    }

    /** Calculates the path between two vertices in this graph.  
     *@param vFrom The starting vertex.
     *@param vTo The destination vertex.
     *@return A list of edges indicating the path between the starting and destination vertex.
     *If there is no path between the two vertices the returned list will be <code>null</code>.
     */
    public List getPathBetween(Vertex vFrom, Vertex vTo) {
        return getPathBetween(vFrom, vTo, new HashMap());
    }

    /** Calculates the path between two vertices in this graph, given a list 
     * of already visited vertices, that should not be traversed again.
     *@param vFrom The starting vertex.
     *@param vTo The destination vertex.
     *@param mAlreadyVisited A map of already visited vertices, where every visited vertex
     *is mapped to a value of 1.
     *@return A list of edges indicating the path between the starting and destination vertex.
     *If there is no path between the two vertices the returned list will be <code>null</code>.
     */
    public List getPathBetween(Vertex vFrom, Vertex vTo, Map mAlreadyVisited) {
        ArrayList l = new ArrayList();
        Edge e = utils.locateDirectedEdgeInGraph(this, vFrom, vTo);
        if (e != null) {
            l.add(e);
            return l;
        }
        if ((vFrom = locateVertex(vFrom)) == null) return null;
        if ((vTo = locateVertex(vTo)) == null) return null;
        if (mAlreadyVisited.get(vFrom) != null) return null;
        List lEdges = this.getAdjacentVertices(vFrom);
        java.util.Iterator iIter = lEdges.iterator();
        String sTailLbl = vTo.getLabel();
        while (iIter.hasNext()) {
            Vertex vCurrent = (Vertex) iIter.next();
            e = gr.demokritos.iit.jinsect.utils.locateDirectedEdgeInGraph(this, vFrom, vCurrent);
            if (e == null) continue;
            if (vCurrent.getLabel().compareTo(sTailLbl) == 0) {
                l.add(e);
                return l;
            } else {
                mAlreadyVisited.put(vFrom, 1);
                List lChild = getPathBetween(vCurrent, vTo, mAlreadyVisited);
                if (lChild != null) {
                    l.add(gr.demokritos.iit.jinsect.utils.locateDirectedEdgeInGraph(this, vFrom, vCurrent));
                    l.addAll(lChild);
                    return l;
                }
                mAlreadyVisited.remove(vTo);
            }
        }
        return null;
    }

    /** Calculates the minimum distance path (i.e. undirected path) between two vertices in this graph.  
     *@param vFrom The starting vertex.
     *@param vTo The destination vertex.
     *@return A list of edges indicating the path between the starting and destination vertex.
     *If there is no path between the two vertices the returned list will be <code>null</code>.
     */
    public List getShortestLinkBetween(Vertex vFrom, Vertex vTo) {
        return getShortestLinkBetween(vFrom, vTo, new HashMap());
    }

    /** Calculates the shortest link (i.e. undirected path between two vertices in this graph, given 
     * a list of already visited vertices, that should not be traversed again.
     *@param vFrom The starting vertex.
     *@param vTo The destination vertex.
     *@param mAlreadyVisited A map of already visited vertices, where every visited vertex
     *is mapped to a value of 1.
     *@return A list of edges indicating the path between the starting and destination vertex.
     *If there is no path between the two vertices the returned list will be <code>null</code>.
     */
    public List getShortestLinkBetween(Vertex vFrom, Vertex vTo, Map mAlreadyVisited) {
        ArrayList l = new ArrayList();
        Edge e = gr.demokritos.iit.jinsect.utils.locateEdgeInGraph(this, vFrom, vTo);
        if (e != null) {
            l.add(e);
            return l;
        }
        if ((vFrom = gr.demokritos.iit.jinsect.utils.locateVertexInGraph(this, vFrom)) == null) return null;
        if ((vTo = gr.demokritos.iit.jinsect.utils.locateVertexInGraph(this, vTo)) == null) return null;
        if (mAlreadyVisited.get(vFrom) != null) return null;
        List lEdges = this.getAdjacentVertices(vFrom);
        java.util.Iterator iIter = lEdges.iterator();
        String sTailLbl = vTo.getLabel();
        ArrayList lCandidatePaths = new ArrayList();
        while (iIter.hasNext()) {
            Vertex vCurrent = (Vertex) iIter.next();
            e = gr.demokritos.iit.jinsect.utils.locateEdgeInGraph(this, vFrom, vCurrent);
            if (e == null) continue;
            if (vCurrent.getLabel().compareTo(sTailLbl) == 0) {
                l.add(e);
                lCandidatePaths.add(l);
            } else {
                mAlreadyVisited.put(vFrom, 1);
                List lChild = getLinkBetween(vCurrent, vTo, mAlreadyVisited);
                if (lChild != null) {
                    l.add(gr.demokritos.iit.jinsect.utils.locateEdgeInGraph(this, vFrom, vCurrent));
                    l.addAll(lChild);
                    lCandidatePaths.add(l);
                }
                mAlreadyVisited.remove(vTo);
            }
        }
        if (lCandidatePaths.size() > 0) {
            List lRes = null;
            double dMinPathLen = Double.POSITIVE_INFINITY;
            Iterator iCurPath = lCandidatePaths.iterator();
            while (iCurPath.hasNext()) {
                List lCurPath = (List) iCurPath.next();
                double dCurPathLen = getPathLength(lCurPath);
                if (dMinPathLen > dCurPathLen) {
                    dMinPathLen = dCurPathLen;
                    lRes = lCurPath;
                }
            }
            return lRes;
        }
        return null;
    }

    /** Calculates the link (i.e. undirected path) between two vertices in this graph.  
     *@param vFrom The starting vertex.
     *@param vTo The destination vertex.
     *@return A list of edges indicating the path between the starting and destination vertex.
     *If there is no path between the two vertices the returned list will be <code>null</code>.
     */
    public List getLinkBetween(Vertex vFrom, Vertex vTo) {
        return getLinkBetween(vFrom, vTo, new HashMap());
    }

    /** Calculates the link (i.e. undirected path) between two vertices in this graph, given a list 
     * of already visited vertices, that should not be traversed again.
     *@param vFrom The starting vertex.
     *@param vTo The destination vertex.
     *@param mAlreadyVisited A map of already visited vertices, where every visited vertex
     *is mapped to a value of 1.
     *@return A list of edges indicating the path between the starting and destination vertex.
     *If there is no path between the two vertices the returned list will be <code>null</code>.
     */
    public List getLinkBetween(Vertex vFrom, Vertex vTo, Map mAlreadyVisited) {
        ArrayList l = new ArrayList();
        Edge e = gr.demokritos.iit.jinsect.utils.locateEdgeInGraph(this, vFrom, vTo);
        if (e != null) {
            l.add(e);
            return l;
        }
        if ((vFrom = gr.demokritos.iit.jinsect.utils.locateVertexInGraph(this, vFrom)) == null) return null;
        if ((vTo = gr.demokritos.iit.jinsect.utils.locateVertexInGraph(this, vTo)) == null) return null;
        if (mAlreadyVisited.get(vFrom) != null) return null;
        List lEdges = this.getAdjacentVertices(vFrom);
        java.util.Iterator iIter = lEdges.iterator();
        String sTailLbl = vTo.getLabel();
        while (iIter.hasNext()) {
            Vertex vCurrent = (Vertex) iIter.next();
            e = gr.demokritos.iit.jinsect.utils.locateEdgeInGraph(this, vFrom, vCurrent);
            if (e == null) continue;
            if (vCurrent.getLabel().compareTo(sTailLbl) == 0) {
                l.add(e);
                return l;
            } else {
                mAlreadyVisited.put(vFrom, 1);
                List lChild = getLinkBetween(vCurrent, vTo, mAlreadyVisited);
                if (lChild != null) {
                    l.add(gr.demokritos.iit.jinsect.utils.locateEdgeInGraph(this, vFrom, vCurrent));
                    l.addAll(lChild);
                    return l;
                }
                mAlreadyVisited.remove(vTo);
            }
        }
        return null;
    }

    /** Calculates the total path weight over a graph path.
     *@param lPath The list of edges indicating the path to follow.
     *@return The total weight of the path, as a sum of individual edge weights.
     */
    public double getPathWeight(List lPath) {
        double dRes = 0.0;
        Iterator iIter = lPath.iterator();
        while (iIter.hasNext()) {
            WeightedEdge e = (WeightedEdge) iIter.next();
            dRes += e.getWeight();
        }
        return dRes;
    }

    /** Calculates the total distance over a graph path, only taking into account edge count.
     *@param lPath The list of edges indicating the path to follow.
     *@return The total size of the path, as a sum of individual edge weights.
     */
    public final double getPathLength(List lPath) {
        return lPath.size();
    }

    /** Returns a vertex of higher rank (that is to say length), that is composed by at least 
     * the vertices appearing in a given list.
     *@param lNodes The list of vertices (or nodes) that should be ancestors of the higher rank
     *vertex.
     *@return The common offspring of the vertices in the given list. If no such offspring vertex
     *is found, then <code>null</code> is returned.
     */
    public Vertex getCommonSubnode(List lNodes) {
        if (lNodes.size() == 0) return null;
        if (lNodes.size() == 1) return (Vertex) lNodes.get(0);
        ArrayList lTempNodes = new ArrayList();
        lTempNodes.addAll(lNodes);
        lNodes = utils.bubbleSortVerticesByStringLength(lNodes);
        ListIterator iIter = lTempNodes.listIterator();
        Vertex vBase = (Vertex) iIter.next();
        if ((vBase = gr.demokritos.iit.jinsect.utils.locateVertexInGraph(this, vBase)) == null) return null;
        iIter.remove();
        List lChildren = this.getAdjacentVertices(vBase);
        ListIterator liNeighbours = lChildren.listIterator();
        boolean bAllOK = true;
        while (liNeighbours.hasNext()) {
            Vertex vChild = (Vertex) liNeighbours.next();
            if (gr.demokritos.iit.jinsect.utils.locateDirectedEdgeInGraph(this, vBase, vChild) != null) {
                ListIterator iOtherParents = lNodes.listIterator();
                while (iOtherParents.hasNext()) {
                    if (getPathBetween((Vertex) iOtherParents.next(), vChild) == null) {
                        bAllOK = false;
                        break;
                    }
                }
                if (bAllOK) return vChild;
            }
        }
        liNeighbours = lChildren.listIterator();
        while (liNeighbours.hasNext()) {
            Vertex vChild = (Vertex) liNeighbours.next();
            if (gr.demokritos.iit.jinsect.utils.locateDirectedEdgeInGraph(this, vBase, vChild) != null) {
                List lNewList = new ArrayList();
                lNewList.add(vChild);
                lNewList.addAll(lNodes);
                Vertex vRes = getCommonSubnode(lNewList);
                if (vRes != null) return vRes;
            }
        }
        return null;
    }

    /** Augments this graph based on a given file.
     *@param sFilename The filename of the file to load.
     */
    public void loadFromFile(String sFilename) {
        try {
            ByteArrayOutputStream bsOut = new ByteArrayOutputStream();
            FileInputStream fiIn = new FileInputStream(sFilename);
            int iData = 0;
            while ((iData = fiIn.read()) > -1) bsOut.write(iData);
            String sDataString = bsOut.toString();
            setDataString(sDataString);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            setDataString("");
        }
    }

    /** Augments this graph given a string. For all n-gram ranks the n-grams are computed
     * and vertices are created mapping lower rank vertices to higher rank vertices. 
     *@param sDataString The string used to extract n-grams.
     */
    public void setDataString(String sDataString) {
        if (DataString != null) DataString = DataString + sDataString; else DataString = sDataString;
        Alphabet = null;
        int iLen = sDataString.length();
        int iStart = 0, iNGramSize = 0;
        EdgeCachedLocator ecl = new EdgeCachedLocator(100);
        HashMap hCache = new HashMap();
        for (iNGramSize = MinNGramSize; iNGramSize <= MaxNGramSize; iNGramSize++) {
            String sCurNGram = "";
            String sCurNGramChild = "";
            if (iNGramSize > sDataString.length()) break;
            for (iStart = 0; iStart < sDataString.length(); iStart++) {
                if (iStart + iNGramSize <= sDataString.length()) sCurNGram = sDataString.substring(iStart, iStart + iNGramSize); else continue;
                if (hCache.containsKey(sCurNGram)) continue; else hCache.put(sCurNGram, 1);
                int iOccurenceCnt = 1;
                if (iStart + iNGramSize + 1 < sDataString.length()) sCurNGramChild = sDataString.substring(iStart, iStart + iNGramSize + 1);
                WeightedEdge we = null;
                if ((we = (WeightedEdge) ecl.locateEdgeInGraph(this, new VertexImpl(sCurNGram), new VertexImpl(sCurNGramChild))) == null) try {
                    this.addEdge(new VertexImpl(sCurNGram), new VertexImpl(sCurNGramChild), 1.0);
                } catch (Exception e) {
                    System.out.println("Cannot add edge:(" + sCurNGram + "," + sCurNGramChild + "");
                    e.printStackTrace();
                    continue;
                } else {
                    we.setWeight(we.getWeight() + 1.0);
                }
                int iCurPos = iStart + 1;
                while ((iCurPos = sDataString.indexOf(sCurNGram, ++iCurPos)) > -1) {
                    if (iCurPos + iNGramSize + 1 < sDataString.length()) sCurNGramChild = sDataString.substring(iCurPos, iCurPos + iNGramSize + 1);
                    if ((we = (WeightedEdge) ecl.locateEdgeInGraph(this, new VertexImpl(sCurNGram), new VertexImpl(sCurNGramChild))) == null) try {
                        this.addEdge(new VertexImpl(sCurNGram), new VertexImpl(sCurNGramChild), 1.0);
                    } catch (Exception e) {
                        System.out.println("Cannot add edge:(" + sCurNGram + "," + sCurNGramChild + "");
                        e.printStackTrace();
                        continue;
                    } else {
                        we.setWeight(we.getWeight() + 1.0);
                    }
                }
            }
            if (Listener != null) {
                Listener.Notify(this, iNGramSize - MinNGramSize / 100);
            }
        }
    }

    /** Returns the data string corresponding to the SymbolicGraph.
     *@return The data string of the graph.
     */
    public String getDataString() {
        return DataString;
    }

    /** Returns the set of characters contained in the data string of the graph.
     *@return A {@link Set} containing the unique characters in the data string.
     */
    public Set getAlphabet() {
        Set sRes = new TreeSet();
        if (Alphabet == null) {
            for (int iCnt = 0; iCnt < DataString.length(); iCnt++) {
                sRes.add(DataString.charAt(iCnt));
            }
        } else sRes.addAll(Alphabet);
        return sRes;
    }

    /** Adds an edge to the graph, if the former does not already exist in the graph. Otherwise,
     *it increases the weight of the edge by one.
     *@param vHead The head vertex of the edge.
     *@param vTail The tail vertex of the edge.
     */
    public Edge addEdge(Vertex vHead, Vertex vTail) throws Exception {
        WeightedEdge e;
        if ((e = (WeightedEdge) utils.locateDirectedEdgeInGraph(this, vHead, vTail)) == null) {
            e = (WeightedEdge) super.addEdge(vHead, vTail);
            e.setWeight(1.0);
        } else {
            e.setWeight(e.getWeight() + 1.0);
        }
        return e;
    }

    public void setNotificationListener(NotificationListener nlListener) {
        Listener = nlListener;
    }

    public void removeNotificationListener() {
        Listener = null;
    }

    public NotificationListener getNotificationListener() {
        return Listener;
    }

    /** Helper function. Checks the functionality of the class.
     */
    public static void main(String[] sArgs) {
        if (sArgs.length > 0) {
            SymbolicGraph sg = new SymbolicGraph(1, 7);
            sg.setDataString(sArgs[0]);
            System.out.println(utils.graphToDot(sg, true));
        } else try {
            SymbolicGraph sg = new SymbolicGraph(1, 1);
            sg.addEdge(new VertexImpl("a"), new VertexImpl("b"));
            sg.addEdge(new VertexImpl("b"), new VertexImpl("c"));
            sg.addEdge(new VertexImpl("b"), new VertexImpl("d"));
            sg.addEdge(new VertexImpl("e"), new VertexImpl("d"));
            sg.addEdge(new VertexImpl("f"), new VertexImpl("e"));
            sg.addEdge(new VertexImpl("f"), new VertexImpl("g"));
            sg.addEdge(new VertexImpl("f"), new VertexImpl("h"));
            sg.addEdge(new VertexImpl("c"), new VertexImpl("h"));
            for (int iCnt = 0; iCnt < 100; iCnt++) System.err.println(sg.getShortestLinkBetween(new VertexImpl("a"), new VertexImpl("h")));
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }
}
