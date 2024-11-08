package de.gee.erep.shared.utils.sort;

import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

/**
 * @author Matthew Gee created: 02.04.2011
 */
public class BottomUpHeapsort<T extends Comparable<T>> extends Heapsort<T> {

    @Override
    public T[] sort(T[] toSort) {
        buildMaxHeap(toSort);
        for (int i = toSort.length - 1; i > 0; i--) {
            swap(toSort, 0, i);
            bottomUpSink(toSort, toSort[0], i - 1);
        }
        return toSort;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void sort(List<T> list, Comparator<? super T> c) {
        Object[] a = list.toArray();
        sort(a, c);
        ListIterator i = list.listIterator();
        for (int j = 0; j < a.length; j++) {
            i.next();
            i.set(a[j]);
        }
    }

    @SuppressWarnings("rawtypes")
    private void sort(Object[] a, Comparator c) {
        buildMaxHeap(a, c);
        for (int i = a.length - 1; i > 0; i--) {
            swap(a, 0, i);
            bottomUpSink(a, a[0], i - 1, c);
        }
    }

    /**
	 * @param a
	 * @param object
	 * @param i
	 * @param c
	 */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void bottomUpSink(Object[] src, Object element, int r, Comparator c) {
        int j = 0;
        int m = 1;
        while (m < r) {
            if (c.compare(src[m], src[m + 1]) == -1) {
                src[j] = src[m + 1];
                j = m + 1;
            } else {
                src[j] = src[m];
                j = m;
            }
            m = j + j + 1;
        }
        if (m == r) {
            src[j] = src[r];
            j = r;
        }
        int i = ((j - 1) / 2);
        while (j > 0 && c.compare(src[i], element) == -1) {
            src[j] = src[i];
            j = i;
            i = ((j - 1) / 2);
        }
        src[j] = element;
    }

    private T[] bottomUpSink(T[] src, T element, int r) {
        int j = 0;
        int m = 1;
        while (m < r) {
            if (src[m].compareTo(src[m + 1]) == -1) {
                src[j] = src[m + 1];
                j = m + 1;
            } else {
                src[j] = src[m];
                j = m;
            }
            m = j + j + 1;
        }
        if (m == r) {
            src[j] = src[r];
            j = r;
        }
        int i = ((j - 1) / 2);
        while (j > 0 && src[i].compareTo(element) == -1) {
            src[j] = src[i];
            j = i;
            i = ((j - 1) / 2);
        }
        src[j] = element;
        return src;
    }
}
