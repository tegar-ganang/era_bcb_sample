package algorithms;

import SortingFramework.*;

/**
 * Implementation of Bubble Sort
 * @author A. Mittag 2009-06-22
 */
public class BubbleSort extends SortingAlgorithm {

    /**
     * Constructor, just sets description
     */
    public BubbleSort() {
        this.description = "Bubble Sort. Maximum recommended array length n=50.000";
    }

    /**
     * The BubbleSort Algorithm
     * @param array
     */
    @Override
    public void sortArray(int[] array) {
        boolean sorted = false;
        while (sorted == false) {
            sorted = true;
            for (int i = 0; i <= array.length - 2; i++) {
                if (array[i] > array[i + 1]) {
                    int temp = array[i];
                    array[i] = array[i + 1];
                    array[i + 1] = temp;
                    sorted = false;
                }
            }
        }
    }
}
