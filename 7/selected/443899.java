package preprocessing.methods.DataReduction;

import game.utils.Exceptions.InvalidArgument;
import preprocessing.Parameters.Parameter;
import preprocessing.methods.BasePreprocessor;
import preprocessing.methods.DataReduction.KDTree.KDNode;
import preprocessing.methods.DataReduction.KDTree.KDTree;
import preprocessing.storage.PreprocessingStorage;
import weka.core.FastVector;

/**
 * Q: How does it work?
 * A: First and implemented idea - compute center point. Then compute distance from
 * center to all instraces and remove instances which are too distant.
 * <p/>
 * Q: Is it good idea?
 * A: No!!! Well as implemented down it does not work!
 * <p/>
 * Q: Better idea?
 * A: Lets try paper called "Very Fast Outlier Detection in Large Multidimensional
 * Data Sets" from A. Chaudhary, A. S. Szalay, A. W. Moore
 * <p/>
 * Q: I donnt want to read it. Tell me in simple words.
 * A: We will build KD-Tree. The space will be divided until stopping criteria is
 * matched (2 points in cell or predefined volume)
 */
public class RemoveOutlayers extends BasePreprocessor {

    private transient KDTree tree;

    private transient FastVector nonEmptyNodes;

    private transient KDNode nonEmptyNodesArray[];

    private transient double density[];

    private int minCount;

    private double minVolume;

    private double REMOVE_MULTIPLY_AVG = 4;

    public RemoveOutlayers() {
        super();
        methodName = "Outlayer remover";
        methodDescription = "Removes outlayers -- distance of sample from the centre.";
        methodTree = "Data reduction.";
        baseConfig = new RemoveOutlayersConfig();
        type = BasePreprocessor.Type.GLOBAL;
    }

    private void fetchParameters() throws NoSuchFieldException {
        Parameter p;
        try {
            p = baseConfig.getParameterObjByKey("Cut-off average boundary");
        } catch (NoSuchFieldException e) {
            logger.error("No field called \"Cut-off average boundary\" found in " + methodName + "\n", e);
            throw e;
        }
        REMOVE_MULTIPLY_AVG = (Double) p.getValue();
        try {
            p = baseConfig.getParameterObjByKey("Minimal cell volume");
        } catch (NoSuchFieldException e) {
            logger.error("No field called \"Minimal cell volume\" found in " + methodName + "\n", e);
            throw e;
        }
        minVolume = (Double) p.getValue();
        try {
            p = baseConfig.getParameterObjByKey("Minimal number of instances in cell");
        } catch (NoSuchFieldException e) {
            logger.error("No field called \"Minimal number of instances in cell\" found in " + methodName + "\n", e);
            throw e;
        }
        minCount = (Integer) p.getValue();
    }

    public boolean run() {
        try {
            fetchParameters();
        } catch (NoSuchFieldException e) {
            return false;
        }
        int[] inputAttributes = store.getInputAttributesIndices();
        for (int inputAttribute : inputAttributes) {
            if (store.getAttributeType(inputAttribute) != PreprocessingStorage.DataType.NUMERIC) {
                logger.warn(methodName + " cannot work with non-numeric attributes. Dropping method :(.");
                return true;
            }
        }
        try {
            tree = new KDTree(store, minCount, minVolume);
        } catch (InvalidArgument invalidArgument) {
            invalidArgument.printStackTrace();
            logger.error("Preprocessing storage is empty. Can not continue.");
            return false;
        }
        computeDensity();
        if (nonEmptyNodes.size() == 0) {
            return true;
        }
        nonEmptyNodesArray = new KDNode[nonEmptyNodes.size()];
        density = new double[nonEmptyNodes.size()];
        double averageDensity = 0;
        for (int i = 0; i < nonEmptyNodes.size(); i++) {
            nonEmptyNodesArray[i] = (KDNode) nonEmptyNodes.elementAt(i);
            density[i] = 1 / nonEmptyNodesArray[i].computeDensity();
            averageDensity += density[i];
        }
        averageDensity = averageDensity / nonEmptyNodes.size();
        sortDensity();
        for (int i = 0; i < density.length; i++) {
            System.out.printf("KD Leaf %d - density %f\n", i, density[i]);
            System.out.printf("Indices in leaf : ");
            FastVector f = nonEmptyNodesArray[i].getDataIndices();
            for (int j = 0; j < f.size(); j++) System.out.printf("%d ", (Integer) f.elementAt(j));
            System.out.printf("\n");
        }
        double removeAverageDensity = Double.POSITIVE_INFINITY;
        FastVector removeIndices = new FastVector();
        int numNodes2Remove = 0;
        while (removeAverageDensity > averageDensity * REMOVE_MULTIPLY_AVG) {
            if (numNodes2Remove >= density.length) {
                break;
            }
            removeAverageDensity = 0;
            for (int i = 0; i < numNodes2Remove; i++) {
                removeAverageDensity += density[density.length - 1 - i];
            }
            removeAverageDensity += density[density.length - 1 - numNodes2Remove];
            removeAverageDensity = removeAverageDensity / (numNodes2Remove + 1);
            if (removeAverageDensity > averageDensity * REMOVE_MULTIPLY_AVG) {
                numNodes2Remove++;
            }
        }
        System.out.printf("# of nodes selected for removal %d\n", numNodes2Remove);
        for (int i = 0; i < numNodes2Remove; i++) {
            System.out.printf("Removing KDNode %d\n", nonEmptyNodesArray.length - 1 - i);
            KDNode n = nonEmptyNodesArray[nonEmptyNodesArray.length - 1 - i];
            removeIndices.appendElements(n.getDataIndices());
        }
        qsortIndices(removeIndices, 0, removeIndices.size() - 1);
        for (int i = removeIndices.size() - 1; i >= 0; i--) {
            System.out.printf("Removing row %d\n", (Integer) removeIndices.elementAt(i));
            try {
                store.removeRow((Integer) removeIndices.elementAt(i));
            } catch (InvalidArgument invalidArgument) {
                logger.error(methodName + ": Instance ( " + i + " ) does not exist");
            }
        }
        return true;
    }

    public void finish() {
    }

    private void computeDensity() {
        density = new double[tree.getNumberOfLeafNodes()];
        nonEmptyNodes = new FastVector();
        int idx = 0;
        for (int i = 0; i < tree.getNumberOfLeafNodes(); i++) {
            KDNode node = tree.getLeafNode(i);
            double d = node.computeDensity();
            if (d > 0) {
                density[idx] = 1 / d;
                nonEmptyNodes.addElement(node);
                idx++;
            }
        }
    }

    private void sortDensity() {
        sort();
    }

    private void sort() {
        for (int i = 0; i < density.length; i++) {
            for (int j = density.length - 2; j >= i; j--) {
                if (density[j] > density[j + 1]) {
                    KDNode n = nonEmptyNodesArray[j];
                    nonEmptyNodesArray[j] = nonEmptyNodesArray[j + 1];
                    nonEmptyNodesArray[j + 1] = n;
                    double d = density[j];
                    density[j] = density[j + 1];
                    density[j + 1] = d;
                }
            }
        }
    }

    private void qsortIndices(FastVector f, int low, int high) {
        int l = low;
        int h = high;
        if (l > h) {
            return;
        }
        int key = (Integer) f.elementAt((h + l) / 2);
        while (l < h) {
            while ((Integer) f.elementAt(l) < key) l++;
            while ((Integer) f.elementAt(h) > key) h--;
            if (l <= h) {
                Object help = f.elementAt(l);
                f.setElementAt(f.elementAt(h), l);
                f.setElementAt(help, h);
                l++;
                h--;
            }
        }
        if (low < h) qsortIndices(f, low, h);
        if (l < high) qsortIndices(f, l, high);
    }

    public boolean isApplyOnTestingData() {
        return false;
    }
}
