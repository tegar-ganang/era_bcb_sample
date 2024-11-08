package algoritms;

import java.util.Arrays;

public class BubbleSort {

    /**
     * @param args
     */
    public static void main(String[] args) {
        int[] intArray = shuffle(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 });
        System.out.println(Arrays.toString(intArray));
        System.out.println(Arrays.toString(bubbleSort(intArray)));
        intArray = shuffle(intArray);
        System.out.println(Arrays.toString(intArray));
        System.out.println(Arrays.toString(bubbleSort2(intArray)));
    }

    public static int[] shuffle(int[] source) {
        int l = source.length;
        int[] target = new int[l];
        int k;
        for (int i = 0; i < target.length; i++) {
            k = (int) (Math.random() * l--);
            target[i] = source[k];
            source[k] = source[l];
        }
        return target;
    }

    public static int[] bubbleSort(int[] source) {
        if (source != null && source.length > 0) {
            boolean flag = true;
            while (flag) {
                flag = false;
                for (int i = 0; i < source.length - 1; i++) {
                    if (source[i] > source[i + 1]) {
                        int temp = source[i];
                        source[i] = source[i + 1];
                        source[i + 1] = temp;
                        flag = true;
                    }
                }
            }
        }
        return source;
    }

    public static int[] bubbleSort2(int[] source) {
        if (null != source && source.length > 0) {
            boolean flag = false;
            while (!flag) {
                for (int i = 0; i < source.length - 1; i++) {
                    if (source[i] > source[i + 1]) {
                        int temp = source[i];
                        source[i] = source[i + 1];
                        source[i + 1] = temp;
                        break;
                    } else if (i == source.length - 2) {
                        flag = true;
                    }
                }
            }
        }
        return source;
    }
}
