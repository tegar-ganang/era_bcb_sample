package org.ibex.util;

import java.io.Serializable;

/** 
 *  An unsynchronized Vector implementation; same semantics as
 *  java.util.Vector. Useful for JDK1.1 platforms that don't have
 *  java.util.ArrayList.
 *
 *  May contain nulls.
 *
 *  @see java.util.Vector
 */
public final class Vec implements Serializable, Cloneable {

    private Object[] store;

    private int size = 0;

    public Vec() {
        this(10);
    }

    public Vec(int i) {
        store = new Object[i];
    }

    public Vec(int i, Object[] store) {
        size = i;
        this.store = store;
    }

    public Vec(Vec old) {
        store = new Object[old.store.length];
        System.arraycopy(old.store, 0, store, 0, old.store.length);
        this.size = old.size;
    }

    public Object clone() {
        return new Vec(this);
    }

    private void grow() {
        grow(store.length * 2);
    }

    private void grow(int newsize) {
        Object[] newstore = new Object[newsize];
        System.arraycopy(store, 0, newstore, 0, size);
        store = newstore;
    }

    public void removeAllElements() {
        for (int i = 0; i < size; i++) store[i] = null;
        size = 0;
    }

    public void toArray(Object[] o) {
        for (int i = 0; i < size; i++) o[i] = store[i];
    }

    public int indexOf(Object o) {
        for (int i = 0; i < size; i++) if (o == null ? store[i] == null : store[i].equals(o)) return i;
        return -1;
    }

    public void addElement(Object o) {
        if (size >= store.length - 1) grow();
        store[size++] = o;
    }

    public Object peek() {
        return lastElement();
    }

    public Object elementAt(int i) {
        return store[i];
    }

    public Object lastElement() {
        if (size == 0) return null;
        return store[size - 1];
    }

    public void push(Object o) {
        addElement(o);
    }

    public Object pop() {
        Object ret = lastElement();
        if (size > 0) store[--size] = null;
        return ret;
    }

    public int size() {
        return size;
    }

    public void setSize(int newSize) {
        if (newSize < 0) throw new RuntimeException("tried to set size to negative value");
        if (newSize > store.length) grow(newSize * 2);
        if (newSize < size) for (int i = newSize; i < size; i++) store[i] = null;
        size = newSize;
    }

    public Object[] copyInto(Object[] out) {
        for (int i = 0; i < size; i++) out[i] = store[i];
        return out;
    }

    public void fromArray(Object[] in) {
        setSize(in.length);
        for (int i = 0; i < size; i++) store[i] = in[i];
    }

    public void removeElementAt(int i) {
        if (i >= size || i < 0) throw new RuntimeException("tried to remove an element outside the vector's limits");
        for (int j = i; j < size - 1; j++) store[j] = store[j + 1];
        setSize(size - 1);
    }

    public void setElementAt(Object o, int i) {
        if (i >= size) setSize(i);
        store[i] = o;
    }

    public void removeElement(Object o) {
        int idx = indexOf(o);
        if (idx != -1) removeElementAt(idx);
    }

    public void insertElementAt(Object o, int at) {
        if (size == store.length) grow();
        for (int i = size; i > at; i--) store[i] = store[i - 1];
        store[at] = o;
        size++;
    }

    public interface CompareFunc {

        public int compare(Object a, Object b);
    }

    public void sort(CompareFunc c) {
        sort(this, null, c, 0, size - 1);
    }

    public static void sort(Vec a, Vec b, CompareFunc c) {
        if (b != null && a.size != b.size) throw new IllegalArgumentException("Vec a and b must be of equal size");
        sort(a, b, c, 0, a.size - 1);
    }

    private static final void sort(Vec a, Vec b, CompareFunc c, int start, int end) {
        Object tmpa, tmpb = null;
        if (start >= end) return;
        if (end - start <= 6) {
            for (int i = start + 1; i <= end; i++) {
                tmpa = a.store[i];
                if (b != null) tmpb = b.store[i];
                int j;
                for (j = i - 1; j >= start; j--) {
                    if (c.compare(a.store[j], tmpa) <= 0) break;
                    a.store[j + 1] = a.store[j];
                    if (b != null) b.store[j + 1] = b.store[j];
                }
                a.store[j + 1] = tmpa;
                if (b != null) b.store[j + 1] = tmpb;
            }
            return;
        }
        Object pivot = a.store[end];
        int lo = start - 1;
        int hi = end;
        do {
            while (c.compare(a.store[++lo], pivot) < 0) {
            }
            while ((hi > lo) && c.compare(a.store[--hi], pivot) > 0) {
            }
            swap(a, lo, hi);
            if (b != null) swap(b, lo, hi);
        } while (lo < hi);
        swap(a, lo, end);
        if (b != null) swap(b, lo, end);
        sort(a, b, c, start, lo - 1);
        sort(a, b, c, lo + 1, end);
    }

    private static final void swap(Vec vec, int a, int b) {
        if (a != b) {
            Object tmp = vec.store[a];
            vec.store[a] = vec.store[b];
            vec.store[b] = tmp;
        }
    }

    public static final void sortInts(int[] a, int start, int end) {
        int tmpa;
        if (start >= end) return;
        if (end - start <= 6) {
            for (int i = start + 1; i <= end; i++) {
                tmpa = a[i];
                int j;
                for (j = i - 1; j >= start; j--) {
                    if (a[j] <= tmpa) break;
                    a[j + 1] = a[j];
                }
                a[j + 1] = tmpa;
            }
            return;
        }
        int pivot = a[end];
        int lo = start - 1;
        int hi = end;
        do {
            while (a[++lo] < pivot) {
            }
            while ((hi > lo) && a[--hi] > pivot) {
            }
            swapInts(a, lo, hi);
        } while (lo < hi);
        swapInts(a, lo, end);
        sortInts(a, start, lo - 1);
        sortInts(a, lo + 1, end);
    }

    private static final void swapInts(int[] vec, int a, int b) {
        if (a != b) {
            int tmp = vec[a];
            vec[a] = vec[b];
            vec[b] = tmp;
        }
    }

    public static class Int {

        private int[] store;

        private int size = 0;

        public Int() {
            this(10);
        }

        public Int(int i) {
            store = new int[i];
        }

        public Int(int i, int[] store) {
            size = i;
            this.store = store;
        }

        private void grow() {
            grow(store.length * 2);
        }

        private void grow(int newsize) {
            int[] newstore = new int[newsize];
            System.arraycopy(store, 0, newstore, 0, size);
            store = newstore;
        }

        public void removeAllElements() {
            for (int i = 0; i < size; i++) store[i] = 0;
            size = 0;
        }

        public void toArray(int[] o) {
            for (int i = 0; i < size; i++) o[i] = store[i];
        }

        public int[] dump() {
            int[] o = new int[size];
            toArray(o);
            return o;
        }

        public int indexOf(int o) {
            for (int i = 0; i < size; i++) if (o == store[i]) return i;
            return -1;
        }

        public void addElement(int o) {
            if (size >= store.length - 1) grow();
            store[size++] = o;
        }

        public int peek() {
            return lastElement();
        }

        public int elementAt(int i) {
            return store[i];
        }

        public int lastElement() {
            if (size == 0) return 0;
            return store[size - 1];
        }

        public void push(int o) {
            addElement(o);
        }

        public int pop() {
            int ret = lastElement();
            if (size > 0) store[size--] = 0;
            return ret;
        }

        public int size() {
            return size;
        }

        public void setSize(int newSize) {
            if (newSize < 0) throw new RuntimeException("tried to set size to negative value");
            if (newSize > store.length) grow(newSize * 2);
            if (newSize < size) for (int i = newSize; i < size; i++) store[i] = 0;
            size = newSize;
        }

        public int[] copyInto(int[] out) {
            for (int i = 0; i < size; i++) out[i] = store[i];
            return out;
        }

        public void fromArray(int[] in) {
            setSize(in.length);
            for (int i = 0; i < size; i++) store[i] = in[i];
        }

        public void removeElementAt(int i) {
            if (i >= size || i < 0) throw new RuntimeException("tried to remove an element outside the vector's limits");
            for (int j = i; j < size - 1; j++) store[j] = store[j + 1];
            setSize(size - 1);
        }

        public void setElementAt(int o, int i) {
            if (i >= size) setSize(i);
            store[i] = o;
        }

        public void removeElement(int o) {
            int idx = indexOf(o);
            if (idx != -1) removeElementAt(idx);
        }

        public void insertElementAt(int o, int at) {
            if (size == store.length) grow();
            for (int i = size; i > at; i--) store[i] = store[i - 1];
            store[at] = o;
            size++;
        }

        public void sort() {
            sort(this, null, 0, size - 1);
        }

        public static void sort(Vec.Int a, Vec.Int b) {
            if (b != null && a.size != b.size) throw new IllegalArgumentException("Vec.Int a and b must be of equal size");
            sort(a, b, 0, a.size - 1);
        }

        private static final void sort(Vec.Int a, Vec.Int b, int start, int end) {
            int tmpa, tmpb = 0;
            if (start >= end) return;
            if (end - start <= 6) {
                for (int i = start + 1; i <= end; i++) {
                    tmpa = a.store[i];
                    if (b != null) tmpb = b.store[i];
                    int j;
                    for (j = i - 1; j >= start; j--) {
                        if ((a.store[j] - tmpa) <= 0) break;
                        a.store[j + 1] = a.store[j];
                        if (b != null) b.store[j + 1] = b.store[j];
                    }
                    a.store[j + 1] = tmpa;
                    if (b != null) b.store[j + 1] = tmpb;
                }
                return;
            }
            int pivot = a.store[end];
            int lo = start - 1;
            int hi = end;
            do {
                while ((a.store[++lo] - pivot) < 0) {
                }
                while ((hi > lo) && (a.store[--hi] - pivot) > 0) {
                }
                swap(a, lo, hi);
                if (b != null) swap(b, lo, hi);
            } while (lo < hi);
            swap(a, lo, end);
            if (b != null) swap(b, lo, end);
            sort(a, b, start, lo - 1);
            sort(a, b, lo + 1, end);
        }

        private static final void swap(Vec.Int vec, int a, int b) {
            if (a != b) {
                int tmp = vec.store[a];
                vec.store[a] = vec.store[b];
                vec.store[b] = tmp;
            }
        }

        public static final void sortInts(int[] a, int start, int end) {
            int tmpa;
            if (start >= end) return;
            if (end - start <= 6) {
                for (int i = start + 1; i <= end; i++) {
                    tmpa = a[i];
                    int j;
                    for (j = i - 1; j >= start; j--) {
                        if (a[j] <= tmpa) break;
                        a[j + 1] = a[j];
                    }
                    a[j + 1] = tmpa;
                }
                return;
            }
            int pivot = a[end];
            int lo = start - 1;
            int hi = end;
            do {
                while (a[++lo] < pivot) {
                }
                while ((hi > lo) && a[--hi] > pivot) {
                }
                swapInts(a, lo, hi);
            } while (lo < hi);
            swapInts(a, lo, end);
            sortInts(a, start, lo - 1);
            sortInts(a, lo + 1, end);
        }

        private static final void swapInts(int[] vec, int a, int b) {
            if (a != b) {
                int tmp = vec[a];
                vec[a] = vec[b];
                vec[b] = tmp;
            }
        }
    }

    public static final void sortFloats(float[] a, int start, int end) {
        float tmpa;
        if (start >= end) return;
        if (end - start <= 6) {
            for (int i = start + 1; i <= end; i++) {
                tmpa = a[i];
                int j;
                for (j = i - 1; j >= start; j--) {
                    if (a[j] <= tmpa) break;
                    a[j + 1] = a[j];
                }
                a[j + 1] = tmpa;
            }
            return;
        }
        float pivot = a[end];
        int lo = start - 1;
        int hi = end;
        do {
            while (a[++lo] < pivot) {
            }
            while ((hi > lo) && a[--hi] > pivot) {
            }
            swapFloats(a, lo, hi);
        } while (lo < hi);
        swapFloats(a, lo, end);
        sortFloats(a, start, lo - 1);
        sortFloats(a, lo + 1, end);
    }

    private static final void swapFloats(float[] vec, int a, int b) {
        if (a != b) {
            float tmp = vec[a];
            vec[a] = vec[b];
            vec[b] = tmp;
        }
    }
}
