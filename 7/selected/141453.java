package gestalt.render;

/**
 * 19 Feb 1996: Fixed to avoid infinite loop discoved by Paul Haeberli.
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
 *
 * 26 Apr 2004: Added the Drawable interface to sort object lists
 *              by Dennis Paul (d3@m-d3.com)
 *
 */
public class Sort {

    public static void qSort(Drawable[] a) {
        qSort(a, 0, a.length - 1);
    }

    public static void qSort(Drawable[] a, int lo0, int hi0) {
        int lo = lo0;
        int hi = hi0;
        if (lo >= hi) {
            return;
        } else if (lo == hi - 1) {
            if (a[lo].getSortValue() > a[hi].getSortValue()) {
                Drawable T = a[lo];
                a[lo] = a[hi];
                a[hi] = T;
            }
            return;
        }
        Drawable pivot = a[(lo + hi) / 2];
        a[(lo + hi) / 2] = a[hi];
        a[hi] = pivot;
        while (lo < hi) {
            while (a[lo].getSortValue() <= pivot.getSortValue() && lo < hi) {
                lo++;
            }
            while (pivot.getSortValue() <= a[hi].getSortValue() && lo < hi) {
                hi--;
            }
            if (lo < hi) {
                Drawable T = a[lo];
                a[lo] = a[hi];
                a[hi] = T;
            }
        }
        a[hi0] = a[hi];
        a[hi] = pivot;
        qSort(a, lo0, lo - 1);
        qSort(a, hi + 1, hi0);
    }

    public static void shellSort(Drawable[] a, int theStart, int theEnd) {
        int h = 1;
        int _myListLength = theEnd - theStart;
        while ((h * 3 + 1) < _myListLength) {
            h = 3 * h + 1;
        }
        while (h > 0) {
            for (int i = h - 1; i < _myListLength; i++) {
                Drawable B = a[i + theStart];
                int j = i;
                for (j = i; (j >= h) && (a[(j - h) + theStart].getSortValue() > B.getSortValue()); j -= h) {
                    a[j + theStart] = a[(j - h) + theStart];
                }
                a[j + theStart] = B;
            }
            h = h / 3;
        }
    }

    public static void bubbleSort(Drawable[] list) {
        boolean swapped;
        do {
            swapped = false;
            for (int i = 0; i < list.length - 1; ++i) {
                if (list[i].getSortValue() > list[i + 1].getSortValue()) {
                    Drawable temp = list[i];
                    list[i] = list[i + 1];
                    list[i + 1] = temp;
                    swapped = true;
                }
            }
        } while (swapped);
    }
}
