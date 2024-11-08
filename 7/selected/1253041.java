package preprocessing.methods.OutlierDetection;

import preprocessing.storage.PreprocessingStorage;
import preprocessing.methods.BasePreprocessor;

/**
 * Created by IntelliJ IDEA.
 * User: zamecdus
 * Date: 17.3.2010
 * Time: 9:13:06
 *
 * Abstract class, which is parent (ancestor) of all outlier detection algorithms and exists to implement general-use
 * methods such as distance functions.
 */
public abstract class OutlierDetector extends BasePreprocessor {

    protected int[] getNumericInputAttributeIndices() {
        int[] indices = store.getInputAttributesIndices();
        for (int i = 0; i < indices.length; i++) {
            if (store.getAttributeType(i) != PreprocessingStorage.DataType.NUMERIC) {
                logger.warn("Implemented outlier detection methods can not work on non-numeric game.data :(. Skipping attribute " + store.getAttributeName(i));
                int[] newIndices = new int[indices.length - 1];
                for (int j = 0; j < newIndices.length; j++) {
                    if (j < i) newIndices[j] = indices[j]; else newIndices[j] = indices[j + 1];
                }
                indices = newIndices;
                i--;
            }
        }
        return indices;
    }

    /**
     * Returns euclidean distance between two items in a given PreprocessingStorage
     *
     * @param storage Link to data storage
     * @param indices Input attribute indices (relevant columns)
     * @param item1 Row 1
     * @param item2 Row 2
     * @return Euclidean distance between the two items (rows)
     */
    public static double euclidean(PreprocessingStorage storage, int[] indices, int item1, int item2) {
        double dist = 0;
        for (int index : indices) {
            dist += Math.pow((Double) storage.getDataItem(index, item1) - (Double) storage.getDataItem(index, item2), 2);
        }
        return Math.sqrt(dist);
    }

    /**
     * Method overload for comparing elements using own PreprocessingStorage
     * @param indices Input attribute indices (relevant columns)
     * @param item1 Row 1
     * @param item2 Row 2
     * @return Euclidean distance between the two items (rows)
     */
    protected double euclidean(int[] indices, int item1, int item2) {
        return euclidean(store, indices, item1, item2);
    }
}
