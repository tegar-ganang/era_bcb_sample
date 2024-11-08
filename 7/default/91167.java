/**
 * A bubble sort demonstration algorithm
 * SortAlgorithm.java, Thu Oct 27 10:32:35 1994
 *
 * @author James Gosling
 * @version 	1.6, 31 Jan 1995
 *
 * Modified 23 Jun 1995 by Jason Harrison@cs.ubc.ca:
 *   Algorithm completes early when no items have been swapped in the 
 *   last pass.
 */
class BubbleSort2Algorithm extends SortAlgorithm {

    void sort(int a[]) throws Exception {
        for (int i = a.length; --i >= 0; ) {
            boolean flipped = false;
            for (int j = 0; j < i; j++) {
                if (stopRequested) {
                    return;
                }
                if (a[j] > a[j + 1]) {
                    int T = a[j];
                    a[j] = a[j + 1];
                    a[j + 1] = T;
                    flipped = true;
                }
                pause(i, j);
            }
            if (!flipped) {
                return;
            }
        }
    }
}
