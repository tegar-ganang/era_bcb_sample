package org.jnorm;

public class TestToPrimitive {

    public void test2() {
        Integer[] arr1 = { 21, 32, 44, 55 };
        int[] arr2 = new int[arr1.length];
        for (int i = 0; i < arr1.length; i++) {
            arr2[i] = arr1[i];
        }
    }

    public void test3() {
        Integer[] arr1 = { 21, 32, 44, 55 };
        int[] arr2 = new int[arr1.length];
        for (int i = 0; i < arr1.length; i++) {
            arr2[i] = arr1[i].intValue();
        }
    }

    public void test4() {
        Long[] arr1 = { 21l, 32l, 44l, 55l };
        long[] arr2 = new long[arr1.length];
        for (int i = 0; i < arr1.length; i++) {
            arr2[i] = arr1[i];
        }
    }

    public void test5() {
        Double[] arr1 = { 21d, 32d, 44d, 55d };
        double[] arr2 = new double[arr1.length];
        for (int i = 0; i < arr1.length; i++) {
            arr2[i] = arr1[i];
        }
    }

    public void test6() {
        Float[] arr1 = { 21f, 32f, 44f, 55f };
        float[] arr2 = new float[arr1.length];
        for (int i = 0; i < arr1.length; i++) {
            arr2[i] = arr1[i];
        }
    }

    public void test7() {
        Short[] arr1 = { 21, 32, 44, 55 };
        short[] arr2 = new short[arr1.length];
        for (int i = 0; i < arr1.length; i++) {
            arr2[i] = arr1[i];
        }
    }

    public void test2_1() {
        Integer[] arr1 = { 21, 32, 44, 55 };
        int[] arr2 = new int[arr1.length];
        for (int i = 0; i < arr1.length; i++) {
            arr2[i] = arr1[i + 1];
        }
    }

    public void test11() {
        Integer[] arr1 = { 21, 32, 44, 55 };
        int[] arr2 = new int[arr1.length];
        for (int i = 0; i < arr1.length; i++) {
            if (arr1 != null) {
                arr2[i] = arr1[i];
            } else {
                arr2[i] = 100;
            }
        }
    }

    public void test12() {
        Integer[] arr1 = { 21, 32, 44, 55 };
        int[] arr2 = new int[arr1.length];
        for (int i = 0; i < arr1.length; i++) {
            if (arr1 == null) {
                arr2[i] = 100;
            } else {
                arr2[i] = arr1[i];
            }
        }
    }
}
