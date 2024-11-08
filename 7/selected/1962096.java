package edu.java.homework.hw03;

import java.util.Arrays;

public class SortAlgorithms {

    /**
     * @param args
     */
    public static void main(String[] args) {
        int[] arr = { 1, 37, 3, 9, 5, 0, 8, 9, 28, 11, -1 };
        System.out.println(Arrays.toString(arr));
        System.out.println("Straight selection sort: ");
        selectSort(arr);
        System.out.println(Arrays.toString(arr));
        System.out.println("Bubble sort: ");
        bubbleSort(arr);
        System.out.println(Arrays.toString(arr));
        System.out.println("Shake sort: ");
        shakeSort(arr);
        System.out.println(Arrays.toString(arr));
        System.out.println("Shell sort: ");
        shellSort(arr);
        System.out.println(Arrays.toString(arr));
        System.out.println("Quick sort: ");
        quickSort(arr, 0, arr.length - 1);
        System.out.println(Arrays.toString(arr));
        System.out.println("Merge sort: ");
        mergeSort(arr, 0, arr.length - 1);
        System.out.println(Arrays.toString(arr));
    }

    public static void selectSort(int[] a) {
        if (a == null) {
            throw new IllegalArgumentException("Null-pointed array");
        }
        for (int i = 0; i <= a.length - 2; i++) {
            int min = a[i];
            int index = i;
            for (int j = i; j <= a.length - 1; j++) {
                if (a[j] < min) {
                    min = a[j];
                    index = j;
                }
            }
            a[index] = a[i];
            a[i] = min;
        }
    }

    public static void bubbleSort(int[] a) {
        if (a == null) {
            throw new IllegalArgumentException("Null-pointed array");
        }
        int right = a.length - 1;
        int k = 0;
        while (right > 0) {
            k = 0;
            for (int i = 0; i <= right - 1; i++) {
                if (a[i] > a[i + 1]) {
                    k = i;
                    int temp = a[i];
                    a[i] = a[i + 1];
                    a[i + 1] = temp;
                }
            }
            right = k;
        }
    }

    public static void shakeSort(int[] a) {
        if (a == null) {
            throw new IllegalArgumentException("Null-pointed array");
        }
        int k = 0;
        int left = 0;
        int right = a.length - 1;
        while (right - left > 0) {
            k = 0;
            for (int i = 0; i <= right - 1; i++) {
                if (a[i] > a[i + 1]) {
                    k = i;
                    int temp = a[i];
                    a[i] = a[i + 1];
                    a[i + 1] = temp;
                }
            }
            right = k;
            k = a.length - 1;
            for (int i = left; i <= right - 1; i++) {
                if (a[i] > a[i + 1]) {
                    k = i;
                    int temp = a[i];
                    a[i] = a[i + 1];
                    a[i + 1] = temp;
                }
            }
            left = k;
        }
    }

    public static void insertSort(int[] a) {
        if (a == null) {
            throw new IllegalArgumentException("Null-pointed array");
        }
        int x = 0;
        for (int i = 0; i < a.length; i++) {
            x = a[i];
            int j = i - 1;
            while (j >= 0 && x < a[j]) {
                a[j + 1] = a[j];
                j--;
            }
            a[j + 1] = x;
        }
    }

    public static void shellSort(int[] a) {
        int h = 0;
        int i = 0;
        int j = 0;
        while (2 * (3 * h + 1) <= a.length) {
            h = 3 * h + 1;
        }
        while (h > 0) {
            for (i = h; i <= a.length - 1; i++) {
                int x = a[i];
                for (j = i - h; j >= 0; j = j - h) {
                    if (x < a[j]) {
                        a[j + h] = a[j];
                    } else {
                        break;
                    }
                }
            }
            h = h / 3;
        }
    }

    public static void quickSort(int[] a, int left, int right) {
        int i = left;
        int j = right;
        int x = a[(right + left) / 2];
        int temp = 0;
        do {
            while (a[i] < x) {
                i++;
            }
            while (x < a[j]) {
                j--;
            }
            if (i < j) {
                temp = a[i];
                a[i] = a[j];
                a[j] = temp;
                i++;
                j--;
            } else if (i == j) {
                i++;
                j--;
            } else {
                break;
            }
        } while (i <= j);
        if (j > left) {
            quickSort(a, left, j);
        }
        if (i < right) {
            quickSort(a, i, right);
        }
    }

    public static void mergeSort(int[] a, int left, int right) {
        if (left >= right) {
            return;
        }
        int mid = (left + right) / 2;
        mergeSort(a, left, mid);
        mergeSort(a, mid + 1, right);
        int[] b = new int[a.length];
        int i = 0;
        int j = 0;
        for (i = mid + 1; i > left; i--) {
            b[i - 1] = a[i - 1];
        }
        for (j = mid; j <= right - 1; j++) {
            b[right + mid - j] = a[j + 1];
        }
        for (int k = left; k <= right; k++) {
            if (b[i] < b[j]) {
                a[k] = b[i];
                i++;
            } else if (b[i] > b[j]) {
                a[k] = b[j];
                j--;
            }
        }
    }
}
