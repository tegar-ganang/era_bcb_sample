package com.algorithmstudy.sort;

/**
 * {@code BubbleSort} contains implementations of the bubble sort algorithm. The algorithm runs in
 * {@code O(n*n)} time. It's memory use is {@code O(n)} as it sorts in place.
 */
public class BubbleSort {

    /**
   * Sort an {@code int[]} using the bubble sort algorithm.
   * 
   * @param a
   *          The {@code int[]} to sort.
   */
    public static void bubbleSort(int[] a) {
        for (int i = a.length - 1; i > 0; i--) {
            for (int j = 0; j < i; j++) {
                if (a[j] > a[j + 1]) {
                    int tmp = a[j];
                    a[j] = a[j + 1];
                    a[j + 1] = tmp;
                }
            }
        }
    }
}
