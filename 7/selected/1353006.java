package algoritms;

import java.util.Arrays;
import junit.framework.TestCase;

/**
 * @author Sergiy Doroshenko
 */
public class QuickSort extends TestCase {

    private static int[] source;

    /**
     * @param args
     */
    public void testQuickSort() {
        System.out.println("start");
        int result[] = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        source = ShuffleArray.shuffle(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 });
        System.out.println(Arrays.toString(source));
        assertFalse(Arrays.equals(result, source));
        quickSort(source);
        System.out.println(Arrays.toString(source));
        assertTrue(Arrays.equals(result, source));
    }

    public void testBubbleSort() {
        System.out.println("start");
        int result[] = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        source = ShuffleArray.shuffle(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 });
        System.out.println(Arrays.toString(source));
        assertFalse(Arrays.equals(result, source));
        bubbleSort(source);
        System.out.println(Arrays.toString(source));
        assertTrue(Arrays.equals(result, source));
    }

    public void bubbleSort(final int[] s) {
        source = s;
        if (source.length < 2) return;
        boolean go = true;
        while (go) {
            go = false;
            for (int i = 0; i < source.length - 1; i++) {
                int temp = source[i];
                if (temp > source[i + 1]) {
                    source[i] = source[i + 1];
                    source[i + 1] = temp;
                    go = true;
                }
            }
        }
    }

    private static void sort(final int leftB, final int rightB) {
        if (leftB >= rightB) return;
        int pointVal = source[rightB];
        int lx = leftB;
        int rx = rightB - 1;
        while (lx <= rx) {
            while (lx <= rx && source[lx] <= pointVal) {
                lx++;
            }
            while (lx <= rx && source[rx] >= pointVal) {
                rx--;
            }
            if (lx < rx) {
                swap(lx, rx);
            }
        }
        swap(lx, rightB);
        sort(leftB, lx - 1);
        sort(lx + 1, rightB);
    }

    private static void swap(int i, int j) {
        int temp = source[i];
        source[i] = source[j];
        source[j] = temp;
    }

    /**
     * if there is more than one element in values[first]..values[last]
        Select splitVal
        Split the array so that
        values[first]..values[splitPoint � 1] <= splitVal
        values[splitPoint] = splitVal
        values[splitPoint + 1]..values[last] > splitVal
        quickSort the left half
        quickSort the right half
     * 
     * @param s
     */
    public static void quickSort(int[] s) {
        source = s;
        if (source.length < 2) return;
        sort(0, source.length - 1);
    }

    public static void main(String[] args) {
    }
}
