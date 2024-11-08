import java.util.Arrays;

public class BubbleSort {

    public static int[] bubbleSort(int... a) {
        boolean swapped;
        do {
            swapped = false;
            for (int i = 0; i < a.length - 1; i++) {
                if (a[i] > a[i + 1]) {
                    int tmp = a[i];
                    a[i] = a[i + 1];
                    a[i + 1] = tmp;
                    swapped = true;
                }
            }
        } while (swapped);
        return a;
    }

    public static int[] bubbleSortOtimizado(int... a) {
        boolean swapped;
        int n = a.length - 2;
        do {
            swapped = false;
            for (int i = 0; i <= n; i++) {
                if (a[i] > a[i + 1]) {
                    int tmp = a[i];
                    a[i] = a[i + 1];
                    a[i + 1] = tmp;
                    swapped = true;
                }
            }
            n = n - 1;
        } while (swapped);
        return a;
    }

    public static void main(String[] args) {
        int[] got = bubbleSort(7, 5, 0, 3, 8, 2, 9, 4, 1, 6);
        int[] expected = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        if (!Arrays.equals(got, expected)) {
            throw new AssertionError("expected " + Arrays.toString(expected) + " but got " + Arrays.toString(got));
        }
        got = bubbleSortOtimizado(7, 5, 0, 3, 8, 2, 9, 4, 1, 6);
        if (!Arrays.equals(got, expected)) {
            throw new AssertionError("expected " + Arrays.toString(expected) + " but got " + Arrays.toString(got));
        }
    }
}
