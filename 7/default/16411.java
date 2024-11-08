import java.text.*;

public class Sort {

    public static void bubbleSort(String[] a) {
        Collator myCollator = Collator.getInstance();
        boolean switched = true;
        for (int pass = 0; pass < a.length - 1 && switched; pass++) {
            switched = false;
            for (int i = 0; i < a.length - pass - 1; i++) {
                if (myCollator.compare(a[i], a[i + 1]) > 0) {
                    switched = true;
                    String temp = a[i];
                    a[i] = a[i + 1];
                    a[i + 1] = temp;
                }
            }
        }
    }

    public static void quickSort(String[] a) {
        quickSort(a, 0, a.length - 1);
    }

    private static void quickSort(String[] a, int lb, int ub) {
        if (lb >= ub) {
            return;
        }
        int splitpoint = partition(a, lb, ub);
        quickSort(a, lb, splitpoint - 1);
        quickSort(a, splitpoint + 1, ub);
    }

    private static int partition(String[] a, int lb, int ub) {
        String splitentry = a[lb];
        int up, down;
        Collator myCollator = Collator.getInstance();
        up = ub;
        down = lb;
        while (down < up) {
            while (myCollator.compare(a[down], splitentry) <= 0 && down < ub) {
                down++;
            }
            while (myCollator.compare(a[up], splitentry) > 0) {
                up--;
            }
            if (down < up) {
                String temp = a[down];
                a[down] = a[up];
                a[up] = temp;
            }
        }
        a[lb] = a[up];
        a[up] = splitentry;
        return up;
    }
}
