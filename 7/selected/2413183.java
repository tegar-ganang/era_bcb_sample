package UniKrak.Pathfind;

import UniKrak.Graph.Edge;
import UniKrak.Graph.Graph;
import UniKrak.Graph.Node;
import java.util.*;

public class Pathfinder {

    public int msgLevel = 1;

    int[] queue = new int[1200];

    int readPtr = 0, writePtr = 0;

    int algorithm = 4;

    float accumDist;

    public void reset() {
        accumDist = 0.0f;
        readPtr = 0;
        writePtr = 0;
    }

    public void useAlgorithm(String function) {
        String realFctName;
        function = function.toLowerCase();
        if (function.equals("breadthfirst") || function.equals("breadth first")) {
            algorithm = 1;
        } else if (function.equals("depthfirst") || function.equals("depth first")) {
            algorithm = 2;
        } else if (function.equals("astar") || function.equals("a star") || function.equals("a*") || function.equals("a *")) {
            algorithm = 3;
        } else if (function.equals("dijkstra")) {
            algorithm = 4;
        } else {
            if (msgLevel >= 1) System.out.println("useAlgorithm: Supplied function name (" + function + ") is not recognised.");
            return;
        }
    }

    public int[] findSimpleRoute(Graph g, int start, int end) {
        int[] path;
        int k;
        boolean ok1 = false, ok2 = false;
        if (start >= 0 && start < g.nodeList.length) {
            ok1 = true;
        }
        if (end >= 0 && end < g.nodeList.length) {
            ok2 = true;
        }
        if (!ok1) System.out.println("Node 1 (index=" + start + ") is not in the specified graph.");
        if (!ok2) System.out.println("Node 2 (index=" + end + ") is not in the specified graph.");
        if (!g.freshlyReset) g.reset();
        if (ok1 && ok2) {
            switch(algorithm) {
                case 1:
                    path = BreadthFirst(g, start, end);
                    break;
                case 2:
                    path = DepthFirst(g, start, end);
                    break;
                case 3:
                    path = AStar(g, start, end);
                    break;
                case 4:
                    path = Dijkstra(g, start, end);
                    break;
                default:
                    System.out.println("wrapper: Specified function unknown (id=fctNum, must be between 1 and 4).");
                    return null;
            }
            return path;
        }
        return null;
    }

    private void enqueue(int index) {
        queue[writePtr] = index;
        writePtr++;
        if (writePtr >= queue.length) writePtr = 0;
    }

    private int dequeue() {
        int i;
        i = readPtr;
        readPtr++;
        if (readPtr >= queue.length) readPtr = 0;
        return queue[i];
    }

    private int[] BreadthFirst(Graph g, int v1, int v2) {
        int node;
        int i, j;
        int child;
        float tempDist;
        int[] resultArray;
        if (!g.freshlyReset) {
            g.reset();
            reset();
        }
        if (msgLevel >= 2) System.out.println("\nFinding path with \"Breadth First\", from node " + v1 + " to node " + v2);
        g.freshlyReset = false;
        enqueue(v1);
        while (readPtr != writePtr) {
            node = dequeue();
            for (i = 0; i < g.nodeList[node].edges.length; i++) {
                child = g.getOtherNode(node, g.nodeList[node].edges[i]);
                if (child == -1) continue;
                tempDist = g.nodeList[node].accumDist + g.edgeList[g.nodeList[node].edges[i]].cost;
                if (!g.nodeList[child].visited || (g.nodeList[child].accumDist > tempDist)) {
                    g.nodeList[child].bfParent = node;
                    g.nodeList[child].bfDepth = g.nodeList[node].bfDepth + 1;
                    g.nodeList[child].visited = true;
                    g.nodeList[child].accumDist = tempDist;
                    enqueue(child);
                }
            }
        }
        if (g.nodeList[v2].visited) {
            accumDist = g.nodeList[v2].accumDist;
            resultArray = new int[g.nodeList[v2].bfDepth + 1];
            node = v2;
            for (j = 0; j < resultArray.length; j++) {
                resultArray[resultArray.length - j - 1] = node;
                node = g.nodeList[node].bfParent;
            }
        } else {
            if (msgLevel >= 1) System.out.println("No connection between start node and end node");
            return null;
        }
        if (msgLevel >= 2) printResult(resultArray, accumDist);
        return resultArray;
    }

    private int[] sortNodesToGoal(Graph g, int parent, int goal) {
        float a, b;
        int tempI, tempR;
        float tempF;
        int len = g.nodeList[parent].edges.length;
        int[] nodes = new int[len];
        float[] dists = new float[len];
        int[] ref = new int[len];
        if (len == 0) return null;
        if (len == 1) {
            ref[0] = 0;
            return ref;
        }
        for (int i = 0; i < len; i++) {
            nodes[i] = g.getOtherNode(parent, g.nodeList[parent].edges[i]);
            a = g.nodeList[nodes[i]].x - g.nodeList[goal].x;
            b = g.nodeList[nodes[i]].y - g.nodeList[goal].y;
            dists[i] = (float) Math.sqrt(a * a + b * b);
            ref[i] = i;
        }
        for (int i = len - 1; i >= 0; i--) {
            for (int j = 0; j < i; j++) {
                if (dists[j] > dists[j + 1]) {
                    tempI = nodes[j];
                    nodes[j] = nodes[j + 1];
                    nodes[j + 1] = tempI;
                    tempF = dists[j];
                    dists[j] = dists[j + 1];
                    dists[j + 1] = tempF;
                    tempR = ref[j];
                    ref[j] = ref[j + 1];
                    ref[j + 1] = tempR;
                }
            }
        }
        return ref;
    }

    private int[] ascheckNode(Graph g, int node, int fromStart) {
        int i;
        int[] resultArray = null;
        int[] sortedIndex;
        if (g.nodeList[node].visited) return null;
        g.nodeList[node].visited = true;
        if (node == goal) {
            resultArray = new int[fromStart + 1];
            resultArray[fromStart] = node;
            return resultArray;
        } else {
            sortedIndex = sortNodesToGoal(g, node, goal);
            if (sortedIndex == null) return null;
            for (i = 0; i < sortedIndex.length; i++) {
                if ((resultArray = ascheckNode(g, g.getOtherNode(node, g.nodeList[node].edges[sortedIndex[i]]), fromStart + 1)) != null) {
                    resultArray[fromStart] = node;
                    accumDist += g.edgeList[g.nodeList[node].edges[sortedIndex[i]]].cost;
                    return resultArray;
                }
            }
        }
        return null;
    }

    private int[] AStar(Graph g, int v1, int v2) {
        int[] resultArray;
        if (!g.freshlyReset) {
            g.reset();
            reset();
        }
        if (msgLevel >= 2) System.out.println("\nFinding path with \"A*\", from node " + v1 + " to node " + v2);
        g.freshlyReset = false;
        goal = v2;
        if ((resultArray = ascheckNode(g, v1, 0)) != null) {
            if (msgLevel >= 2) printResult(resultArray, accumDist);
            return resultArray;
        } else {
            if (msgLevel >= 1) System.out.println("No connection between start node and end node");
            return null;
        }
    }

    private int goal;

    private int[] dfcheckNode(Graph g, int node, int fromStart) {
        int i;
        int[] resultArray = null;
        if (g.nodeList[node].visited) return null;
        g.nodeList[node].visited = true;
        if (node == goal) {
            resultArray = new int[fromStart + 1];
            resultArray[fromStart] = node;
            return resultArray;
        } else {
            for (i = 0; i < g.nodeList[node].edges.length; i++) {
                if ((resultArray = dfcheckNode(g, g.getOtherNode(node, g.nodeList[node].edges[i]), fromStart + 1)) != null) {
                    resultArray[fromStart] = node;
                    accumDist += g.edgeList[g.nodeList[node].edges[i]].cost;
                    return resultArray;
                }
            }
        }
        return null;
    }

    private int[] DepthFirst(Graph g, int v1, int v2) {
        int[] resultArray;
        if (!g.freshlyReset) {
            g.reset();
            reset();
        }
        if (msgLevel >= 2) System.out.println("\nFinding path with \"Depth First\", from node " + v1 + " to node " + v2);
        g.freshlyReset = false;
        goal = v2;
        if ((resultArray = dfcheckNode(g, v1, 0)) != null) {
            if (msgLevel >= 2) printResult(resultArray, accumDist);
            return resultArray;
        } else {
            if (msgLevel >= 1) System.out.println("No connection between start node and end node");
            return null;
        }
    }

    private int[] Dijkstra(Graph g, int v1, int v2) {
        if (v1 >= g.nodeList.length || v2 >= g.nodeList.length) return null;
        int numVisited = 0;
        float[] dists = new float[g.nodeList.length];
        int[] resultArray = null;
        int node;
        for (int i = 0; i < g.nodeList.length; i++) {
            dists[i] = Float.POSITIVE_INFINITY;
            g.nodeList[i].visited = false;
            g.nodeList[i].bfDepth = 0;
        }
        dists[v1] = 0;
        while (numVisited < g.nodeList.length) {
            float smallest = Float.POSITIVE_INFINITY;
            int b = 0;
            for (int i = 0; i < g.nodeList.length; i++) {
                if (!g.nodeList[i].visited && dists[i] < smallest) {
                    smallest = dists[i];
                    b = i;
                }
            }
            if (smallest == Float.POSITIVE_INFINITY) {
                break;
            }
            g.nodeList[b].visited = true;
            numVisited++;
            if (b == v2) break;
            for (int i = 0; i < g.nodeList[b].edges.length; i++) {
                int edge = g.nodeList[b].edges[i];
                node = g.getOtherNode(b, edge);
                if (dists[b] + g.edgeList[edge].cost < dists[node]) {
                    dists[node] = dists[b] + g.edgeList[edge].cost;
                    g.nodeList[node].bfParent = b;
                    g.nodeList[node].bfDepth = g.nodeList[b].bfDepth + 1;
                }
            }
        }
        if (g.nodeList[v2].visited) {
            accumDist = g.nodeList[v2].accumDist;
            resultArray = new int[g.nodeList[v2].bfDepth + 1];
            node = v2;
            for (int j = 0; j < resultArray.length; j++) {
                resultArray[resultArray.length - j - 1] = node;
                node = g.nodeList[node].bfParent;
            }
        }
        return resultArray;
    }

    public int[][] findSequenceRoute(Graph g, int[] waypoints) {
        int[][] output;
        int[] recv;
        output = new int[waypoints.length - 1][];
        for (int i = 0; i < waypoints.length - 1; i++) {
            recv = findSimpleRoute(g, waypoints[i], waypoints[i + 1]);
            output[i] = recv;
        }
        return output;
    }

    private void printResult(int[] results, float totalDist) {
        int i;
        System.out.print("Nodes in path: " + results.length + "\nPath: [");
        for (i = 0; i < results.length; i++) System.out.print(" " + results[i]);
        System.out.print(" ]\nTotal cost: " + totalDist + "\n");
    }

    private int cantTouchThis;

    private Graph SimplifyTS(Graph input, int[] interest) {
        Graph output = new Graph();
        int i, j, k = 0;
        int[] tmpPath;
        output.nodeList = new Node[interest.length];
        output.edgeList = new Edge[interest.length * (interest.length - 1) / 2];
        for (i = 0; i < output.nodeList.length; i++) {
            output.nodeList[i] = new Node(interest[i], input.nodeList[interest[i]].x, input.nodeList[interest[i]].y, 0);
            output.nodeList[i].edges = new int[interest.length - 1];
        }
        for (i = 0; i < output.nodeList.length; i++) {
            for (j = i + 1; j < output.nodeList.length; j++) {
                output.nodeList[i].edges[j - 1] = k;
                output.nodeList[j].edges[i] = k;
                input.reset();
                tmpPath = findSimpleRoute(input, interest[i], interest[j]);
                output.edgeList[k] = new Edge(i, j, accumDist);
                output.edgeList[k].nodesToRepresent = tmpPath;
                if (i == 0 && j == interest.length - 1) cantTouchThis = k;
                k++;
            }
        }
        return output;
    }

    private int[][] ComplexifyTS(Graph input, Graph output, int[] nodesInInput) {
        int[][] route = new int[nodesInInput.length - 1][];
        int[] partNodes;
        boolean reverse;
        Edge e;
        for (int i = 0; i < nodesInInput.length - 1; i++) {
            partNodes = null;
            reverse = false;
            for (int j = 0; j < input.nodeList[nodesInInput[i]].edges.length; j++) {
                if (input.getOtherNode(nodesInInput[i], input.nodeList[nodesInInput[i]].edges[j]) == nodesInInput[i + 1]) {
                    e = input.edgeList[input.nodeList[nodesInInput[i]].edges[j]];
                    partNodes = e.nodesToRepresent;
                    if (nodesInInput[i] == e.v2) reverse = true;
                    break;
                }
            }
            if (partNodes == null) {
                System.out.println("Path cannot be complexified; it defines a pair of nodes that are not connected");
                return null;
            }
            route[i] = new int[partNodes.length];
            for (int j = 0; j < partNodes.length; j++) {
                if (reverse) route[i][j] = partNodes[partNodes.length - j - 1]; else route[i][j] = partNodes[j];
            }
        }
        return route;
    }

    private int[] from, to;

    int[] route = null;

    private Graph tgGraph;

    private void randomPath(Random r) {
        int i, j, i0, j0, k;
        for (i = 0; i < tgGraph.nodeList.length; i++) to[i] = -1;
        for (i0 = i = 0; i < tgGraph.nodeList.length - 1; i++) {
            j = (int) (r.nextLong() % (tgGraph.nodeList.length - i));
            to[i0] = 0;
            for (j0 = k = 0; k < j; k++) {
                j0++;
                while (to[j0] != -1) j0++;
            }
            while (to[j0] != -1) j0++;
            to[i0] = j0;
            from[j0] = i0;
            i0 = j0;
        }
        to[i0] = 0;
        from[0] = i0;
    }

    private boolean improve() {
        int i, j, h;
        double d1, d2;
        double H[] = new double[tgGraph.nodeList.length];
        for (i = 0; i < tgGraph.nodeList.length; i++) H[i] = -tgGraph.getDist(from[i], i) - tgGraph.getDist(i, to[i]) + tgGraph.getDist(from[i], to[i]);
        for (i = 0; i < tgGraph.nodeList.length; i++) {
            d1 = -tgGraph.getDist(i, to[i]);
            j = to[to[i]];
            while (j != i) {
                d2 = H[j] + tgGraph.getDist(i, j) + tgGraph.getDist(j, to[i]) + d1;
                if (d2 < -1e-5) {
                    h = from[j];
                    to[h] = to[j];
                    from[to[j]] = h;
                    h = to[i];
                    to[i] = j;
                    to[j] = h;
                    from[h] = j;
                    from[j] = i;
                    return true;
                }
                j = to[j];
            }
        }
        return false;
    }

    private boolean improvecross() {
        int i, j, h, h1, hj;
        double d1, d2, d;
        for (i = 0; i < tgGraph.nodeList.length; i++) {
            d1 = -tgGraph.getDist(i, to[i]);
            j = to[to[i]];
            d2 = 0;
            d = 0;
            while (to[j] != i) {
                d += tgGraph.getDist(j, from[j]) - tgGraph.getDist(from[j], j);
                d2 = d1 + tgGraph.getDist(i, j) + d + tgGraph.getDist(to[i], to[j]) - tgGraph.getDist(j, to[j]);
                if (d2 < -1e-5) {
                    h = to[i];
                    h1 = to[j];
                    to[i] = j;
                    to[h] = h1;
                    from[h1] = h;
                    hj = i;
                    while (j != h) {
                        h1 = from[j];
                        to[j] = h1;
                        from[j] = hj;
                        hj = j;
                        j = h1;
                    }
                    from[j] = hj;
                    return true;
                }
                j = to[j];
            }
        }
        return false;
    }

    private void localoptimize() {
        do {
            while (improve()) ;
        } while (improvecross());
    }

    private int[] travelingSalesman(Graph g) {
        Random R = new Random();
        float currentBestLength = 1e20f, tempLength;
        int[] routeTemp = new int[g.nodeList.length];
        int count = 0, k;
        tgGraph = g;
        route = new int[g.nodeList.length];
        if (g.nodeList.length < 3) {
            route[0] = 0;
            if (g.nodeList.length == 2) route[1] = 1;
        } else if (g.nodeList.length == 3) {
            route[0] = 0;
            route[1] = 1;
            route[2] = 2;
        } else {
            to = new int[g.nodeList.length];
            from = new int[g.nodeList.length];
            while (count < 30) {
                randomPath(R);
                localoptimize();
                k = 0;
                for (int i = 0; i < g.nodeList.length; i++) {
                    routeTemp[i] = k;
                    k = to[k];
                }
                tempLength = tgGraph.getPathLength(routeTemp);
                if (tempLength < currentBestLength - 1e-10f) {
                    currentBestLength = tempLength;
                    route = routeTemp;
                    count = 0;
                }
                count++;
            }
        }
        return route;
    }

    public int[][] findComplexRoute(Graph g, int[] waypoints) {
        Graph nw = SimplifyTS(g, waypoints);
        int[] sRoute = travelingSalesman(nw);
        int[][] cRoute = ComplexifyTS(nw, g, sRoute);
        return cRoute;
    }
}
