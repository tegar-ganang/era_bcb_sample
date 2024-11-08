package com.dukesoftware.utils.common;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import com.google.common.base.Function;

public class ArrayUtils {

    public static final int getIndex(Object[] array, Object value) {
        for (int i = 0; i < array.length; i++) {
            Object elem = array[i];
            if (elem != null && elem.equals(value)) {
                return i;
            }
        }
        return -1;
    }

    public static final int getIndex(int[] array, int value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == value) {
                return i;
            }
        }
        return -1;
    }

    public static final String appendAsStringArray(Object[] array) {
        StringBuffer sb = new StringBuffer();
        for (Object part : array) {
            sb.append(part);
        }
        return sb.toString();
    }

    public static final String appendAsStringArray(int[] array) {
        StringBuffer sb = new StringBuffer();
        for (int part : array) {
            sb.append(part);
        }
        return sb.toString();
    }

    public static void rotateArray(Object[] array) {
        Object top = array[0];
        for (int i = 0, len = array.length - 1; i < len; i++) {
            array[i] = array[i + 1];
        }
        array[array.length - 1] = top;
    }

    public static void rotateArraySubtractNull(Object[] array) {
        Object top = array[0];
        int storePos = 0;
        for (int i = 0, len = array.length - 1; i < len; i++) {
            Object move = array[i + 1];
            if (move != null) {
                array[storePos] = move;
                storePos++;
            }
        }
        array[storePos] = top;
        for (int i = storePos + 1; i < array.length; i++) {
            array[i] = null;
        }
    }

    public static final void initArrayZero(int[] pos) {
        for (int i = 0; i < pos.length; i++) pos[i] = 0;
    }

    public static final void initArray(int[] a, int value) {
        for (int i = 0; i < a.length; i++) a[i] = value;
    }

    @Deprecated
    public static final void exchange(double[] x1, double[] x2, int i, int j) {
        double t = x1[i];
        x1[i] = x1[j];
        x1[j] = t;
        t = x2[i];
        x2[i] = x2[j];
        x2[j] = t;
    }

    public static final void exchange(double[] x, int i, int j) {
        double t = x[i];
        x[i] = x[j];
        x[j] = t;
    }

    public static final void exchange(Object[] x, int i, int j) {
        Object tmp = x[i];
        x[i] = x[j];
        x[j] = tmp;
    }

    public static final boolean equals(double[] a, double[] b) {
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) return false;
        }
        return true;
    }

    public static final double[] copy(double[] source) {
        double[] ans = new double[source.length];
        System.arraycopy(source, 0, ans, 0, source.length);
        return ans;
    }

    public static final <T> void populateToCollection(T[] array, Collection<T> collection) {
        for (T elem : array) {
            collection.add(elem);
        }
    }

    public static final <T extends Comparable<T>> T[] sortAndPopulateToArray(ConcurrentHashMap<String, T> paramMap, T[] array) {
        List<T> list = new ArrayList<T>();
        list.addAll(paramMap.values());
        Collections.sort(list);
        return list.toArray(array);
    }

    public static final <T> String appendAllString(Collection<T> collection, String delimiter) {
        StringBuffer result = new StringBuffer();
        for (T s : collection) {
            result.append(delimiter).append(s.toString());
        }
        return result.toString().replaceFirst(delimiter, "");
    }

    public static final <T> String appendAsString(T[] array, String delimiter) {
        if (array.length == 0) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        for (T t : array) {
            sb.append(delimiter).append(t);
        }
        return sb.substring(delimiter.length(), sb.length());
    }

    public static final <T> String appendAsString(int[] array, String delimiter) {
        if (array.length == 0) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        for (int t : array) {
            sb.append(delimiter).append(t);
        }
        return sb.substring(delimiter.length(), sb.length());
    }

    public static final int countNull(Object[] array) {
        int count = 0;
        for (Object o : array) {
            if (o == null) {
                count++;
            }
        }
        return count;
    }

    public static final <T> int searchIndex(T[] array, int start, T elem) {
        for (int i = start, len = array.length, end = len + start; i < end; i++) {
            int j = i % len;
            if (array[j] == elem) {
                return j;
            }
        }
        return -1;
    }

    public static final <T> int searchIndex(T[] array, T elem) {
        for (int i = 0, len = array.length; i < len; i++) {
            if (array[i] == elem) {
                return i;
            }
        }
        return -1;
    }

    public static final void setAllNull(Object[] array) {
        for (int i = 0, len = array.length; i < len; i++) {
            array[i] = null;
        }
    }

    public static final int firstNotNullIndex(Object[] array) {
        for (int i = 0, len = array.length; i < len; i++) {
            if (array[i] != null) {
                return i;
            }
        }
        return -1;
    }

    public static final int lastNotNullIndex(Object[] array) {
        int last = -1;
        for (int i = 0, len = array.length; i < len; i++) {
            if (array[i] != null) {
                last = i;
            }
        }
        return last;
    }

    public static final boolean inRange(Object[] array, int index) {
        return index < array.length && index >= 0;
    }

    public static <T> boolean doesNotContain(List<T[]> rows, T[] row) {
        for (int i = 0, size = rows.size(); i < size; ++i) {
            if (EqualUtil.areEqual(rows.get(i), row)) {
                return false;
            }
        }
        return true;
    }

    public static <T> List<T[]> removeDuplicates(List<T[]> rows) {
        List<T[]> retRows = new ArrayList<T[]>();
        for (int i = 0; i < rows.size(); ++i) {
            T[] row = rows.get(i);
            if (doesNotContain(retRows, row)) {
                retRows.add(row);
            }
        }
        return retRows;
    }

    public static final void shuffle(Object[] src, int times) {
        Random rand = new Random(0);
        for (int i = 0, max = src.length; i < times; i++) {
            int index1 = rand.nextInt(max);
            int index2 = rand.nextInt(max);
            Object temp = src[index1];
            src[index1] = src[index2];
            src[index2] = temp;
        }
    }

    public static void main(String[] args) {
        int[] a = new int[1000000];
        for (int i = 0; i < a.length; i++) {
            a[i] = i;
        }
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            findBinary(a, i);
        }
        System.out.println(System.currentTimeMillis() - start);
        start = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            findLinear(a, i);
        }
        System.out.println(System.currentTimeMillis() - start);
    }

    public static final int findBinary(int[] src, int key) {
        int high = src.length - 1;
        if (src[0] <= key && src[high] >= key) {
            int low = 0;
            for (int mid = (low + high) >> 1, v = src[mid]; low <= high; v = src[mid], mid = (low + high) >> 1) {
                if (v == key) {
                    return mid;
                } else if (v < key) {
                    low = mid + 1;
                } else {
                    high = mid - 1;
                }
            }
        }
        return -1;
    }

    public static final int findLinear(int[] src, int key) {
        if (src[0] <= key && src[src.length - 1] >= key) {
            for (int i = 0; i < src.length; i++) {
                if (src[i] == key) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static final double[] clone(double[] src) {
        double[] clone = new double[src.length];
        System.arraycopy(src, 0, clone, 0, src.length);
        return clone;
    }

    public static final boolean areAllTrue(boolean[] array) {
        for (int i = 0, n = array.length; i < n; i++) {
            if (!array[i]) {
                return false;
            }
        }
        return true;
    }

    public static final <F, T> T[] transform(F[] src, Function<F, T> f, Class<T> clazz) {
        T[] dest = (T[]) Array.newInstance(clazz, src.length);
        for (int i = 0; i < src.length; i++) {
            dest[i] = f.apply(src[i]);
        }
        return dest;
    }

    public static final int[] createIntArrayFilledByIndex(int length) {
        int[] array = new int[length];
        for (int i = 0; i < length; i++) {
            array[i] = i;
        }
        return array;
    }

    public static final int[] convertDoubleArrayToIntArray(double[] a) {
        int[] ans = new int[a.length];
        for (int i = 0; i < a.length; i++) ans[i] = (int) a[i];
        return ans;
    }

    public static final double[] convertIntArrayToDoubleArray(int[] a) {
        double[] ans = new double[a.length];
        for (int i = 0; i < a.length; i++) ans[i] = a[i];
        return ans;
    }

    public static final void fillAllZero(double[] data) {
        for (int i = 0; i < data.length; i++) data[i] = 0d;
    }

    public static double calcAsVectorAbs(double[] a) {
        double sum = 0;
        for (double val : a) sum += val * val;
        return sum;
    }
}
