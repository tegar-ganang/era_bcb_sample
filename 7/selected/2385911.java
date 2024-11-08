package org.tigr.microarray.mev.cluster.algorithm.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import org.tigr.microarray.mev.cluster.Cluster;
import org.tigr.microarray.mev.cluster.Node;
import org.tigr.microarray.mev.cluster.NodeList;
import org.tigr.microarray.mev.cluster.NodeValue;
import org.tigr.microarray.mev.cluster.NodeValueList;
import org.tigr.microarray.mev.cluster.algorithm.AbortException;
import org.tigr.microarray.mev.cluster.algorithm.AbstractAlgorithm;
import org.tigr.microarray.mev.cluster.algorithm.AlgorithmData;
import org.tigr.microarray.mev.cluster.algorithm.AlgorithmEvent;
import org.tigr.microarray.mev.cluster.algorithm.AlgorithmException;
import org.tigr.microarray.mev.cluster.algorithm.AlgorithmParameters;
import org.tigr.util.FloatMatrix;
import org.tigr.util.QSort;

public class KMC extends AbstractAlgorithm {

    private boolean stop = false;

    private int function;

    private float factor;

    private boolean absolute;

    private int number_of_genes;

    private int number_of_samples;

    private FloatMatrix expMatrix;

    private boolean calculateMeans;

    private int iterations;

    private boolean converged;

    private boolean kmcGenes;

    private int validN;

    private int hcl_function;

    private boolean hcl_absolute;

    int[] clusterConvergence;

    public KMC() {
    }

    public AlgorithmData execute(AlgorithmData data) throws AlgorithmException {
        AlgorithmParameters map = data.getParams();
        function = map.getInt("distance-function", EUCLIDEAN);
        factor = map.getFloat("distance-factor", 1.0f);
        absolute = map.getBoolean("distance-absolute", false);
        calculateMeans = map.getBoolean("calculate-means", true);
        kmcGenes = map.getBoolean("kmc-cluster-genes", true);
        hcl_function = map.getInt("hcl-distance-function", EUCLIDEAN);
        hcl_absolute = map.getBoolean("hcl-distance-absolute", false);
        int number_of_iterations = map.getInt("number-of-iterations", 0);
        int number_of_clusters = map.getInt("number-of-clusters", 0);
        boolean hierarchical_tree = map.getBoolean("hierarchical-tree", false);
        int method_linkage = map.getInt("method-linkage", 0);
        boolean calculate_genes = map.getBoolean("calculate-genes", false);
        boolean calculate_experiments = map.getBoolean("calculate-experiments", false);
        this.expMatrix = data.getMatrix("experiment");
        number_of_genes = this.expMatrix.getRowDimension();
        number_of_samples = this.expMatrix.getColumnDimension();
        this.clusterConvergence = new int[number_of_clusters];
        KMCluster[] clusters;
        FloatMatrix means = null;
        FloatMatrix medians = null;
        FloatMatrix variances = null;
        if (calculateMeans) {
            clusters = calculate(number_of_genes, number_of_clusters, number_of_iterations);
            means = getMeans(clusters);
            variances = getVariances(clusters, means);
        } else {
            clusters = calculateMedians(number_of_genes, number_of_clusters, number_of_iterations);
            medians = getMedians(clusters);
            variances = getVariances(clusters, medians);
        }
        float[] tempConv = new float[clusterConvergence.length];
        for (int i = 0; i < clusterConvergence.length; i++) {
            tempConv[i] = (float) clusterConvergence[i];
        }
        QSort qsort = new QSort(tempConv);
        tempConv = qsort.getSorted();
        int[] sortedClusterIndices = qsort.getOrigIndx();
        int temp;
        for (int i = 0; i < number_of_clusters - 1; i++) {
            for (int j = 0; j < number_of_clusters - 1 - i; j++) {
                if (tempConv[j] == tempConv[j + 1]) {
                    if (clusters[sortedClusterIndices[j]].size() < clusters[sortedClusterIndices[j + 1]].size()) {
                        temp = sortedClusterIndices[j];
                        sortedClusterIndices[j] = sortedClusterIndices[j + 1];
                        sortedClusterIndices[j + 1] = temp;
                    }
                }
            }
        }
        KMCluster[] newClusterOrder = new KMCluster[clusters.length];
        FloatMatrix newMeansMedsOrder = new FloatMatrix(clusters.length, number_of_samples);
        FloatMatrix newVariancesOrder = new FloatMatrix(clusters.length, number_of_samples);
        for (int i = 0; i < clusters.length; i++) {
            newClusterOrder[i] = clusters[sortedClusterIndices[i]];
            newVariancesOrder.A[i] = variances.A[sortedClusterIndices[i]];
            if (calculateMeans) newMeansMedsOrder.A[i] = means.A[sortedClusterIndices[i]]; else newMeansMedsOrder.A[i] = medians.A[sortedClusterIndices[i]];
            clusterConvergence[i] = (int) (tempConv[i]);
        }
        clusters = newClusterOrder;
        variances = newVariancesOrder;
        if (calculateMeans) means = newMeansMedsOrder; else medians = newMeansMedsOrder;
        AlgorithmEvent event = null;
        if (hierarchical_tree) {
            event = new AlgorithmEvent(this, AlgorithmEvent.SET_UNITS, clusters.length, "Calculate Hierarchical Trees");
            fireValueChanged(event);
            event.setIntValue(0);
            event.setId(AlgorithmEvent.PROGRESS_VALUE);
            fireValueChanged(event);
        }
        Cluster result_cluster = new Cluster();
        NodeList nodeList = result_cluster.getNodeList();
        int[] features;
        for (int i = 0; i < clusters.length; i++) {
            if (stop) {
                throw new AbortException();
            }
            features = convert2int(clusters[i]);
            Node node = new Node(features);
            nodeList.addNode(node);
            if (hierarchical_tree) {
                node.setValues(calculateHierarchicalTree(features, method_linkage, calculate_genes, calculate_experiments));
                event.setIntValue(i + 1);
                fireValueChanged(event);
            }
        }
        AlgorithmData result = new AlgorithmData();
        result.addCluster("cluster", result_cluster);
        if (calculateMeans) result.addMatrix("clusters_means", means); else result.addMatrix("clusters_means", medians);
        result.addMatrix("clusters_variances", variances);
        result.addParam("iterations", String.valueOf(getIterations()));
        result.addParam("converged", String.valueOf(getConverged()));
        result.addIntArray("convergence-iterations", clusterConvergence);
        return result;
    }

    private NodeValueList calculateHierarchicalTree(int[] features, int method, boolean genes, boolean experiments) throws AlgorithmException {
        NodeValueList nodeList = new NodeValueList();
        AlgorithmData data = new AlgorithmData();
        FloatMatrix experiment;
        if (kmcGenes) experiment = getSubExperiment(this.expMatrix, features); else experiment = getSubExperimentReducedCols(this.expMatrix, features);
        data.addMatrix("experiment", experiment);
        data.addParam("hcl-distance-function", String.valueOf(this.hcl_function));
        data.addParam("hcl-distance-absolute", String.valueOf(this.hcl_absolute));
        data.addParam("method-linkage", String.valueOf(method));
        HCL hcl = new HCL();
        AlgorithmData result;
        if (genes) {
            data.addParam("calculate-genes", String.valueOf(true));
            result = hcl.execute(data);
            validate(result);
            addNodeValues(nodeList, result);
        }
        if (experiments) {
            data.addParam("calculate-genes", String.valueOf(false));
            result = hcl.execute(data);
            validate(result);
            addNodeValues(nodeList, result);
        }
        return nodeList;
    }

    private void addNodeValues(NodeValueList target_list, AlgorithmData source_result) {
        target_list.addNodeValue(new NodeValue("child-1-array", source_result.getIntArray("child-1-array")));
        target_list.addNodeValue(new NodeValue("child-2-array", source_result.getIntArray("child-2-array")));
        target_list.addNodeValue(new NodeValue("node-order", source_result.getIntArray("node-order")));
        target_list.addNodeValue(new NodeValue("height", source_result.getMatrix("height").getRowPackedCopy()));
    }

    private FloatMatrix getSubExperiment(FloatMatrix experiment, int[] features) {
        FloatMatrix subExperiment = new FloatMatrix(features.length, experiment.getColumnDimension());
        for (int i = 0; i < features.length; i++) {
            subExperiment.A[i] = experiment.A[features[i]];
        }
        return subExperiment;
    }

    /**
     *  Creates a matrix with reduced columns (samples) as during experiment clustering
     */
    private FloatMatrix getSubExperimentReducedCols(FloatMatrix experiment, int[] features) {
        FloatMatrix copyMatrix = experiment.copy();
        FloatMatrix subExperiment = new FloatMatrix(features.length, copyMatrix.getColumnDimension());
        for (int i = 0; i < features.length; i++) {
            subExperiment.A[i] = copyMatrix.A[features[i]];
        }
        subExperiment = subExperiment.transpose();
        return subExperiment;
    }

    /**
     * Checking the result of hcl algorithm calculation.
     * @throws AlgorithmException, if the result is incorrect.
     */
    private void validate(AlgorithmData result) throws AlgorithmException {
        if (result.getIntArray("child-1-array") == null) {
            throw new AlgorithmException("parameter 'child-1-array' is null");
        }
        if (result.getIntArray("child-2-array") == null) {
            throw new AlgorithmException("parameter 'child-2-array' is null");
        }
        if (result.getIntArray("node-order") == null) {
            throw new AlgorithmException("parameter 'node-order' is null");
        }
        if (result.getMatrix("height") == null) {
            throw new AlgorithmException("parameter 'height' is null");
        }
    }

    private int[] convert2int(ArrayList source) {
        int[] int_matrix = new int[source.size()];
        for (int i = 0; i < int_matrix.length; i++) {
            int_matrix[i] = (int) ((Float) source.get(i)).floatValue();
        }
        return int_matrix;
    }

    private KMCluster[] calculate(int number_of_genes, int number_of_clusters, int number_of_iterations) throws AlgorithmException {
        int current;
        int i;
        int address = 0;
        int counter = 0;
        float dissim[] = new float[number_of_clusters];
        int[] location = new int[number_of_genes];
        Float[] elements = new Float[number_of_genes];
        KMCluster[] clusters = new KMCluster[number_of_clusters];
        for (i = 0; i < clusters.length; i++) {
            clusters[i] = new KMCluster();
        }
        int clusterIndex = 0;
        Random random = new Random();
        for (i = 0; i < number_of_genes; i++) {
            clusterIndex = (int) Math.floor(random.nextFloat() * number_of_clusters);
            clusterIndex = Math.min(clusterIndex, number_of_clusters - 1);
            elements[i] = new Float(i);
            location[i] = clusterIndex;
            clusters[clusterIndex].add(elements[i]);
        }
        AlgorithmEvent event = new AlgorithmEvent(this, AlgorithmEvent.SET_UNITS, 200);
        fireValueChanged(event);
        int currentProgress = 0;
        int oldCurrentProgress = 0;
        double Factor = 200 / (double) (number_of_genes * number_of_iterations);
        for (i = 0; i < number_of_clusters; i++) {
            clusters[i].calculateMean();
        }
        current = 0;
        int iterations = 0;
        boolean converged = false;
        int reallocations = 0;
        while (counter != number_of_genes * number_of_iterations) {
            if (stop) {
                throw new AbortException();
            }
            currentProgress = (int) (counter * Factor);
            if (currentProgress > oldCurrentProgress) {
                event.setId(AlgorithmEvent.PROGRESS_VALUE);
                event.setIntValue(currentProgress);
                fireValueChanged(event);
                oldCurrentProgress = currentProgress;
            }
            for (i = 0; i < number_of_clusters; i++) {
                dissim[i] = ExperimentUtil.geneDistance(expMatrix, clusters[i].getMean(), current, 0, function, factor, absolute);
            }
            address = findNearest(dissim);
            if (address != location[current]) {
                reallocations++;
                clusters[location[current]].updateMeanForLoosingCluster((int) (elements[current].intValue()));
                clusters[location[current]].remove(elements[current]);
                clusters[address].updateMeanForWinningCluster(elements[current].intValue());
                clusters[address].add(elements[current]);
                location[current] = address;
                this.clusterConvergence[location[current]] = iterations + 1;
                this.clusterConvergence[address] = iterations + 1;
            }
            current++;
            if (current == number_of_genes) {
                current = 0;
            }
            counter++;
            if (counter % number_of_genes == 0) {
                iterations++;
                if (reallocations == 0) {
                    converged = true;
                    break;
                } else {
                    event.setId(AlgorithmEvent.MONITOR_VALUE);
                    event.setIntValue(reallocations);
                    fireValueChanged(event);
                    reallocations = 0;
                }
            }
        }
        event.setId(AlgorithmEvent.MONITOR_VALUE);
        event.setIntValue(-1);
        fireValueChanged(event);
        setIterations(iterations);
        setConverged(converged);
        return clusters;
    }

    /**
     *  Code added to allow running of K-medians
     */
    private KMCluster[] calculateMedians(int number_of_genes, int number_of_clusters, int number_of_iterations) throws AlgorithmException {
        int current;
        int i;
        int address = 0;
        int counter = 0;
        float dissim[] = new float[number_of_clusters];
        int[] location = new int[number_of_genes];
        Float[] elements = new Float[number_of_genes];
        KMCluster[] clusters = new KMCluster[number_of_clusters];
        for (i = 0; i < clusters.length; i++) {
            clusters[i] = new KMCluster();
        }
        int clusterIndex = 0;
        Random random = new Random();
        for (i = 0; i < number_of_genes; i++) {
            clusterIndex = (int) Math.floor(random.nextFloat() * number_of_clusters);
            clusterIndex = Math.min(clusterIndex, number_of_clusters - 1);
            elements[i] = new Float(i);
            location[i] = clusterIndex;
            clusters[clusterIndex].add(elements[i]);
        }
        AlgorithmEvent event = new AlgorithmEvent(this, AlgorithmEvent.SET_UNITS, 200);
        fireValueChanged(event);
        int currentProgress = 0;
        int oldCurrentProgress = 0;
        double Factor = 200 / (double) (number_of_genes * number_of_iterations);
        for (i = 0; i < number_of_clusters; i++) {
            clusters[i].calculateMedian();
        }
        current = 0;
        int iterations = 0;
        boolean converged = false;
        int reallocations = 0;
        while (counter != number_of_genes * number_of_iterations) {
            if (stop) {
                throw new AbortException();
            }
            currentProgress = (int) (counter * Factor);
            if (currentProgress > oldCurrentProgress) {
                event.setId(AlgorithmEvent.PROGRESS_VALUE);
                event.setIntValue(currentProgress);
                fireValueChanged(event);
                oldCurrentProgress = currentProgress;
            }
            for (i = 0; i < number_of_clusters; i++) {
                dissim[i] = ExperimentUtil.geneDistance(expMatrix, clusters[i].getMedian(), current, 0, function, factor, absolute);
            }
            address = findNearest(dissim);
            if (address != location[current]) {
                reallocations++;
                clusters[location[current]].remove(elements[current]);
                clusters[address].add(elements[current]);
                clusters[location[current]].calculateMedian();
                clusters[address].calculateMedian();
                location[current] = address;
            }
            current++;
            if (current == number_of_genes) {
                current = 0;
            }
            counter++;
            if (counter % number_of_genes == 0) {
                iterations++;
                if (reallocations == 0) {
                    converged = true;
                    break;
                } else {
                    event.setId(AlgorithmEvent.MONITOR_VALUE);
                    event.setIntValue(reallocations);
                    fireValueChanged(event);
                    reallocations = 0;
                }
            }
        }
        event.setId(AlgorithmEvent.MONITOR_VALUE);
        event.setIntValue(-1);
        fireValueChanged(event);
        setIterations(iterations);
        setConverged(converged);
        return clusters;
    }

    private void setIterations(int iterations) {
        this.iterations = iterations;
    }

    private int getIterations() {
        return iterations;
    }

    private void setConverged(boolean converged) {
        this.converged = converged;
    }

    private boolean getConverged() {
        return converged;
    }

    private int findNearest(float x[]) {
        int address = 0;
        float smallest = x[0];
        for (int i = 1; i < x.length; i++) {
            if (x[i] < smallest) {
                smallest = x[i];
                address = i;
            }
        }
        return address;
    }

    public void abort() {
        stop = true;
    }

    private class KMCluster extends ArrayList {

        private FloatMatrix mean = new FloatMatrix(1, number_of_samples);

        private FloatMatrix median = new FloatMatrix(1, number_of_samples);

        private float[] sums = new float[number_of_samples];

        private int[] validNList = new int[number_of_samples];

        public KMCluster() {
        }

        public void calculateMean() {
            float currentMean;
            int n = size();
            float value;
            for (int i = 0; i < number_of_samples; i++) {
                currentMean = 0f;
                validN = 0;
                for (int j = 0; j < n; j++) {
                    value = expMatrix.get(((Float) get(j)).intValue(), i);
                    if (!Float.isNaN(value)) {
                        currentMean += value;
                        validN++;
                    }
                }
                sums[i] = currentMean;
                validNList[i] = validN;
                mean.set(0, i, currentMean / (float) validN);
            }
        }

        public void updateMeanForLoosingCluster(int index) {
            float[] currValues = expMatrix.A[index];
            for (int i = 0; i < number_of_samples; i++) {
                if (!Float.isNaN(currValues[i])) {
                    validNList[i]--;
                    sums[i] -= currValues[i];
                    mean.set(0, i, sums[i] / (float) validNList[i]);
                }
            }
        }

        public void updateMeanForWinningCluster(int index) {
            float[] currValues = expMatrix.A[index];
            for (int i = 0; i < number_of_samples; i++) {
                if (!Float.isNaN(currValues[i])) {
                    validNList[i]++;
                    sums[i] += currValues[i];
                    mean.set(0, i, sums[i] / (float) validNList[i]);
                }
            }
        }

        public FloatMatrix getMean() {
            return mean;
        }

        private void calculateMedian() {
            float currentMedian;
            float[] values;
            int numberOfValidValues;
            for (int i = 0; i < number_of_samples; i++) {
                values = new float[size()];
                numberOfValidValues = getValues(values, i);
                if (numberOfValidValues == 0) break;
                median.set(0, i, computeMedian(values));
            }
        }

        private float computeMedian(float[] array) {
            int numberOfItems;
            float returnValue;
            Arrays.sort(array);
            numberOfItems = array.length;
            if (numberOfItems % 2 == 1) {
                returnValue = array[numberOfItems / 2];
            } else {
                returnValue = array[numberOfItems / 2 - 1];
                returnValue += array[numberOfItems / 2];
                returnValue /= 2.0;
            }
            return returnValue;
        }

        /**
         *  strips values to use to calculate median
         */
        private int getValues(float[] values, int sampleIndex) {
            float currVal;
            int currIndex = 0;
            int numberOfValues = 0;
            for (int i = 0; i < values.length; i++) {
                currVal = expMatrix.get(((Float) get(i)).intValue(), sampleIndex);
                if (!Float.isNaN(currVal)) {
                    values[currIndex] = currVal;
                    numberOfValues++;
                    currIndex++;
                }
            }
            return numberOfValues;
        }

        public float getElementMedian(float[] array, int start, int end, int medNum) {
            if (start == end) return array[start];
            int part = randomPartition(array, start, end);
            int k = part - start + 1;
            if (medNum <= k) return getElementMedian(array, start, part, medNum); else return getElementMedian(array, part + 1, end, medNum - k);
        }

        private int randomPartition(float[] array, int start, int end) {
            int i = (int) ((end - start + 1) * Math.random()) + start;
            swap(array, i, start);
            return partition(array, start, end);
        }

        private void swap(float[] array, int i, int j) {
            float temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }

        public int partition(float[] array, int start, int end) {
            float x = array[start];
            int i = start - 1;
            int j = end + 1;
            int currExpIndex = 0;
            while (true) {
                do {
                    j--;
                } while (array[j] > x);
                do {
                    i++;
                } while (array[i] < x);
                if (i < j) swap(array, i, j); else {
                    return j;
                }
            }
        }

        public FloatMatrix getMedian() {
            return median;
        }
    }

    private FloatMatrix getMeans(KMCluster[] clusters) {
        FloatMatrix means = new FloatMatrix(clusters.length, number_of_samples);
        FloatMatrix mean;
        for (int i = 0; i < clusters.length; i++) {
            mean = clusters[i].getMean();
            means.A[i] = mean.A[0];
        }
        return means;
    }

    private FloatMatrix getMedians(KMCluster[] clusters) {
        FloatMatrix medians = new FloatMatrix(clusters.length, number_of_samples);
        FloatMatrix median;
        for (int i = 0; i < clusters.length; i++) {
            median = clusters[i].getMedian();
            medians.A[i] = median.A[0];
        }
        return medians;
    }

    private FloatMatrix getVariances(KMCluster[] clusters, FloatMatrix means) {
        final int rows = means.getRowDimension();
        final int columns = means.getColumnDimension();
        FloatMatrix variances = new FloatMatrix(rows, columns);
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                variances.set(row, column, getSampleVariance(clusters[row], column, means.get(row, column)));
            }
        }
        return variances;
    }

    private float getSampleNormalizedSum(KMCluster cluster, int column, float mean) {
        final int size = cluster.size();
        float sum = 0f;
        float value;
        validN = 0;
        for (int i = 0; i < size; i++) {
            value = expMatrix.get(((Float) cluster.get(i)).intValue(), column);
            if (!Float.isNaN(value)) {
                sum += Math.pow(value - mean, 2);
                validN++;
            }
        }
        return sum;
    }

    private float getSampleVariance(KMCluster cluster, int column, float mean) {
        if (validN > 1) return (float) Math.sqrt(getSampleNormalizedSum(cluster, column, mean) / (float) (validN - 1)); else return 0f;
    }
}
