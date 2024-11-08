package dr.evolution.tree;

import dr.evolution.distance.DistanceMatrix;

/**
 * An abstract base class for clustering algorithms from pairwise distances
 *
 * @version $Id: ClusteringTree.java,v 1.8 2005/05/24 20:25:56 rambaut Exp $
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 */
public abstract class ClusteringTree extends SimpleTree {

    /**
	 * constructor ClusteringTree
	 *
	 * @param distanceMatrix distance matrix
	 */
    public ClusteringTree(DistanceMatrix distanceMatrix, int minimumTaxa) {
        this.distanceMatrix = distanceMatrix;
        if (distanceMatrix.getTaxonCount() < minimumTaxa) {
            throw new IllegalArgumentException("less than " + minimumTaxa + " taxa in distance matrix");
        }
        init(distanceMatrix);
        while (true) {
            findNextPair();
            if (numClusters < minimumTaxa) break;
            newCluster();
        }
        finish();
    }

    protected double getDist(int a, int b) {
        return distance[alias[a]][alias[b]];
    }

    protected void init(DistanceMatrix distanceMatrix) {
        numClusters = distanceMatrix.getTaxonCount();
        clusters = new SimpleNode[numClusters];
        distance = new double[numClusters][numClusters];
        for (int i = 0; i < numClusters; i++) {
            for (int j = 0; j < numClusters; j++) {
                distance[i][j] = distanceMatrix.getElement(i, j);
            }
        }
        for (int i = 0; i < numClusters; i++) {
            clusters[i] = new SimpleNode();
            clusters[i].setTaxon(distanceMatrix.getTaxon(i));
        }
        alias = new int[numClusters];
        tipCount = new int[numClusters];
        for (int i = 0; i < numClusters; i++) {
            alias[i] = i;
            tipCount[i] = 1;
        }
    }

    protected void newCluster() {
        newCluster = new SimpleNode();
        newCluster.setHeight(newNodeHeight());
        newCluster.addChild(clusters[abi]);
        newCluster.addChild(clusters[abj]);
        clusters[abi] = newCluster;
        clusters[abj] = null;
        for (int k = 0; k < numClusters; k++) {
            if (k != besti && k != bestj) {
                int ak = alias[k];
                distance[ak][abi] = distance[abi][ak] = updatedDistance(besti, bestj, k);
                distance[ak][abj] = distance[abj][ak] = -1.0;
            }
        }
        distance[abi][abi] = 0.0;
        distance[abj][abj] = -1.0;
        for (int i = bestj; i < numClusters - 1; i++) {
            alias[i] = alias[i + 1];
        }
        tipCount[abi] += tipCount[abj];
        tipCount[abj] = 0;
        numClusters--;
    }

    protected void finish() {
        adoptNodes(newCluster);
        distance = null;
    }

    protected abstract void findNextPair();

    protected abstract double newNodeHeight();

    protected abstract double updatedDistance(int i, int j, int k);

    protected DistanceMatrix distanceMatrix;

    protected int numClusters;

    protected SimpleNode[] clusters;

    protected SimpleNode newCluster;

    protected int besti, abi;

    protected int bestj, abj;

    protected int[] tipCount;

    protected int[] alias;

    protected double[][] distance;

    protected int minimumTaxa;
}
