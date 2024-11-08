/**
 * 19 Feb 1996: Fixed to avoid infinite loop discoved by Paul Haberli.
 *              Misbehaviour expressed when the pivot element was not unique.
 *              -Jason Harrison
 *
 * 21 Jun 1996: Modified code based on comments from Paul Haeberli, and
 *              Peter Schweizer (Peter.Schweizer@mni.fh-giessen.de).  
 *              Used Daeron Meyer's (daeron@geom.umn.edu) code for the
 *              new pivoting code. - Jason Harrison
 *
 * 09 Jan 1998: Another set of bug fixes by Thomas Everth (everth@wave.co.nz)
 *              and John Brzustowski (jbrzusto@gpu.srv.ualberta.ca).
 */
class QubbleSortAlgorithm extends SortAlgorithm {

    void bsort(int a[], int lo, int hi) throws Exception {
        for (int j = hi; j > lo; j--) {
            for (int i = lo; i < j; i++) {
                if (a[i] > a[i + 1]) {
                    int T = a[i];
                    a[i] = a[i + 1];
                    a[i + 1] = T;
                    pause();
                }
            }
        }
    }

    void sort(int a[], int lo0, int hi0) throws Exception {
        int lo = lo0;
        int hi = hi0;
        pause(lo, hi);
        if ((hi - lo) <= 6) {
            bsort(a, lo, hi);
            return;
        }
        int pivot = a[(lo + hi) / 2];
        a[(lo + hi) / 2] = a[hi];
        a[hi] = pivot;
        while (lo < hi) {
            while (a[lo] <= pivot && lo < hi) {
                lo++;
            }
            while (pivot <= a[hi] && lo < hi) {
                hi--;
            }
            if (lo < hi) {
                int T = a[lo];
                a[lo] = a[hi];
                a[hi] = T;
                pause();
            }
        }
        a[hi0] = a[hi];
        a[hi] = pivot;
        sort(a, lo0, lo - 1);
        sort(a, hi + 1, hi0);
    }

    void sort(int a[]) throws Exception {
        sort(a, 0, a.length - 1);
    }
}
