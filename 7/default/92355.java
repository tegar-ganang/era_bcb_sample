/**
 * A bi-directional bubble sort demonstration algorithm
 * SortAlgorithm.java, Thu Oct 27 10:32:35 1994
 *
 * @author James Gosling
 * @version 	1.6f, 31 Jan 1995
 */
class BidirectionalBubbleSortAlgorithm extends SortAlgorithm {

    void sort(int a[]) throws Exception {
        int j;
        int limit = a.length;
        int st = -1;
        while (st < limit) {
            boolean flipped = false;
            st++;
            limit--;
            for (j = st; j < limit; j++) {
                if (stopRequested) {
                    return;
                }
                if (a[j] > a[j + 1]) {
                    int T = a[j];
                    a[j] = a[j + 1];
                    a[j + 1] = T;
                    flipped = true;
                    pause(st, limit);
                }
            }
            if (!flipped) {
                return;
            }
            for (j = limit; --j >= st; ) {
                if (stopRequested) {
                    return;
                }
                if (a[j] > a[j + 1]) {
                    int T = a[j];
                    a[j] = a[j + 1];
                    a[j + 1] = T;
                    flipped = true;
                    pause(st, limit);
                }
            }
            if (!flipped) {
                return;
            }
        }
        pause(st, limit);
    }
}
