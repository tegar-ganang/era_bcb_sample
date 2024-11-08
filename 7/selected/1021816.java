package com.onehao.middle.l4;

public class MyBubbleSortTest {

    public static void exchange(int[] a, int i) {
        int temp;
        temp = a[i];
        a[i] = a[i + 1];
        a[i + 1] = temp;
    }

    public static void main(String[] args) {
        int[] a = new int[] { 5, 4, 7, 9, 3 };
        int length = a.length;
        for (int j = 0; j < length - 1; j++) {
            for (int i = 0; i < length - j - 1; i++) {
                if (a[i] > a[i + 1]) {
                    exchange(a, i);
                }
            }
        }
        for (int i = 0; i < length; i++) {
            System.out.println(a[i]);
        }
    }
}
