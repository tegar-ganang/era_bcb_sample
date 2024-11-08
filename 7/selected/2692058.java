package pocu.data_structs;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

/**
 * An array list. Adding to the end of the list and indexed queries happen in
 * (O)1 time. If the capacity is exceeded a new list of double size and the old
 * list will be copied over.
 */
public class AList implements List {

    private static final int DEFAULT_INIT_SIZE = 32;

    private int tail = 0;

    private Object[] array;

    /** Create a new list using the default capacity. */
    public AList() {
        this(DEFAULT_INIT_SIZE);
    }

    /** Create a new list using the given capacity. */
    public AList(int size) {
        array = new Object[size];
    }

    /** Create and return a synchronized version of this list using the default capacity. */
    public static AList getSynchronized() {
        return new AListSynchronized();
    }

    /** Create and return a synchronized version of this list using the given capacity. */
    public static AList getSynchronized(int size) {
        return new AListSynchronized(size);
    }

    /**
     * Add 'val' to the beginning of this list, O(n). Iterable objects will NOT
     * be descended into: all objects are treated as singular objects. To add
     * the contents of an Iterable object see the addAll() suite of methods. The
     * array will be grown as needed.
     */
    public void addFirst(Object val) {
        insertBefore(0, val);
    }

    /**
     * Add 'val' to the end of this list, O(1). Iterable objects will NOT be
     * descended into: all objects are treated as singular objects. To add the
     * contents of an Iterable object see the addAll() suite of methods. The
     * array will be grown as needed.
     */
    public void addLast(Object val) {
        add(val);
    }

    /** Remove and return the first element in this list, O(n). */
    public Object removeFirst() {
        return remove(0);
    }

    /** Remove and return the last element in this list, O(1). */
    public Object removeLast() {
        return remove(tail - 1);
    }

    /** Return the first element in this list without removing it, O(1). */
    public Object getFirst() {
        return get(0);
    }

    /** Return the last element in this list without removing it, O(1). */
    public Object getLast() {
        return get(tail - 1);
    }

    /**
     * Add 'val' to the end of this list, O(1). The index used will be
     * returned. Iterable objects will NOT be descended into: all objects are
     * treated as singular objects. To add the contents of an Iterable object
     * see the addAll() suite of methods. The array will be grown as needed.
     */
    public int add(Object val) {
        if (tail >= array.length) growArray();
        array[tail] = val;
        return tail++;
    }

    /**
     * Add all elements from 'i' to the end of the list in iteration order, O(n)
     * where n is the length of 'i'. The array will be grown as needed.
     */
    public void addAll(Iterable i) {
        if (i == null) return;
        Iter iter = i.iter();
        while (iter.hasNext()) add(iter.next());
    }

    /**
     * Add all elements from 'arr' to the end of the list in order, O(n) where
     * n is the length of 'arr'. The array will be grown as needed.
     */
    public void addAll(Object[] arr) {
        if (arr == null) return;
        for (int i = 0; i < arr.length; i++) add(arr[i]);
    }

    /**
     * Set 'val' into 'index', O(1). If the index is larger than the size of the
     * array the array will be grown as needed to accommodate it. Any element
     * already existing in 'index' will be lost.
     */
    public void set(int index, Object val) {
        if (index < 0) throw new IndexOutOfBoundsException(Integer.toString(index));
        while (index >= array.length) growArray();
        array[index] = val;
        if (index >= tail) tail = index + 1;
    }

    /** Return the element from 'index' without removing it, O(1). */
    public Object get(int index) {
        if (index < 0) throw new IndexOutOfBoundsException(Integer.toString(index));
        if (index >= tail) return null;
        return array[index];
    }

    /**
     * Insert 'val' before 'index', pushing the element at 'index' and all
     * remaining elements up, O(n). The array will be grown as needed.
     */
    public void insertBefore(int index, Object val) {
        validateBounds(index);
        if (tail >= array.length) growArray();
        for (int i = tail; i > index; i--) array[i] = array[i - 1];
        array[index] = val;
        tail++;
    }

    /**
     * Insert 'val' after 'index', pushing the element after 'index' and all
     * remaining elements up, O(n). The array will be grown as needed.
     */
    public void insertAfter(int index, Object val) {
        validateBounds(index);
        if (tail >= array.length) growArray();
        for (int i = tail; i > index; i--) array[i] = array[i - 1];
        array[index + 1] = val;
        tail++;
    }

    /**
     * Remove and return element at 'index', shifting all following elements
     * back, O(n).
     */
    public Object remove(int index) {
        validateBounds(index);
        Object val = array[index];
        tail--;
        for (int i = index; i < tail; i++) array[i] = array[i + 1];
        return val;
    }

    /** Return true if this list contains 'o', O(n). */
    public boolean contains(Object o) {
        return Common.contains(this, o);
    }

    public AList copy() {
        AList list = new AList(array.length);
        list.addAll(this);
        return list;
    }

    public Object[] toArray() {
        return Common.toArray(this);
    }

    public void fillArray(Object[] array) {
        Common.fillArray(this, array);
    }

    public int size() {
        return tail;
    }

    public void clear() {
        tail = 0;
    }

    public void sort() {
        Arrays.sort(array, 0, tail, new StdComparator());
    }

    public void sort(Comparator comparator) {
        Arrays.sort(array, 0, tail, comparator);
    }

    public void reverse() {
        int b = 0, e = tail - 1;
        while (b < e) {
            Object o = array[b];
            array[b] = array[e];
            array[e] = o;
            b++;
            e--;
        }
    }

    public Iter iter() {
        return new AListIter();
    }

    public Iterator iterator() {
        return (Iterator) iter();
    }

    public String toString() {
        return "AList: size=" + size();
    }

    public String dump() {
        return Common.dump(this);
    }

    private void validateBounds(int index) {
        if (index < 0 || index >= tail) throw new IndexOutOfBoundsException(Integer.toString(index));
    }

    private static final int GROWTH_STEP_SIZE = 524288;

    private void growArray() {
        int size = array.length;
        if (size <= 0) size = 1; else if (size < GROWTH_STEP_SIZE) size *= 2; else size += GROWTH_STEP_SIZE;
        Object[] newArr = new Object[size];
        for (int i = 0; i < array.length; i++) newArr[i] = array[i];
        array = newArr;
    }

    private class AListIter implements Iter {

        private int i = -1;

        public boolean hasNext() {
            return i + 1 < tail;
        }

        public Object next() {
            if (i + 1 >= tail) throw new IllegalStateException();
            return array[++i];
        }

        public void set(Object val) {
            if (i == -1) throw new IllegalStateException();
            array[i] = val;
        }

        public void remove() {
            if (i == -1) throw new IllegalStateException();
            AList.this.remove(i--);
        }

        public void insertBefore(Object val) {
            if (i == -1) throw new IllegalStateException();
            AList.this.insertBefore(i++, val);
        }

        public void insertAfter(Object val) {
            if (i == -1) throw new IllegalStateException();
            AList.this.insertAfter(i, val);
        }
    }
}

class AListSynchronized extends AList {

    public AListSynchronized() {
        super();
    }

    public AListSynchronized(int size) {
        super(size);
    }

    public void addFirst(Object val) {
        synchronized (this) {
            super.addFirst(val);
        }
    }

    public void addLast(Object val) {
        synchronized (this) {
            super.addLast(val);
        }
    }

    public Object removeFirst() {
        synchronized (this) {
            return super.removeFirst();
        }
    }

    public Object removeLast() {
        synchronized (this) {
            return super.removeLast();
        }
    }

    public Object getFirst() {
        synchronized (this) {
            return super.getFirst();
        }
    }

    public Object getLast() {
        synchronized (this) {
            return super.getLast();
        }
    }

    public int add(Object val) {
        synchronized (this) {
            return super.add(val);
        }
    }

    public void addAll(Iterable i) {
        synchronized (this) {
            super.addAll(i);
        }
    }

    public void addAll(Object[] arr) {
        synchronized (this) {
            super.addAll(arr);
        }
    }

    public void set(int index, Object val) {
        synchronized (this) {
            super.set(index, val);
        }
    }

    public Object get(int index) {
        synchronized (this) {
            return super.get(index);
        }
    }

    public void insertBefore(int index, Object val) {
        synchronized (this) {
            super.insertBefore(index, val);
        }
    }

    public void insertAfter(int index, Object val) {
        synchronized (this) {
            super.insertAfter(index, val);
        }
    }

    public Object remove(int index) {
        synchronized (this) {
            return super.remove(index);
        }
    }

    public AList copy() {
        synchronized (this) {
            return super.copy();
        }
    }

    public Object[] toArray() {
        synchronized (this) {
            return super.toArray();
        }
    }

    public void fillArray(Object[] array) {
        synchronized (this) {
            super.fillArray(array);
        }
    }

    public int size() {
        synchronized (this) {
            return super.size();
        }
    }

    public void clear() {
        synchronized (this) {
            super.clear();
        }
    }

    public Iter iter() {
        return new SynchronizedIter(super.iter());
    }

    private class SynchronizedIter implements Iter {

        private Iter iter;

        public SynchronizedIter(Iter iter) {
            assert iter != null;
            this.iter = iter;
        }

        public boolean hasNext() {
            synchronized (AListSynchronized.this) {
                return iter.hasNext();
            }
        }

        public Object next() {
            synchronized (AListSynchronized.this) {
                return iter.next();
            }
        }

        public void set(Object val) {
            synchronized (AListSynchronized.this) {
                iter.set(val);
            }
        }

        public void remove() {
            synchronized (AListSynchronized.this) {
                iter.remove();
            }
        }

        public void insertBefore(Object val) {
            synchronized (AListSynchronized.this) {
                iter.insertBefore(val);
            }
        }

        public void insertAfter(Object val) {
            synchronized (AListSynchronized.this) {
                iter.insertAfter(val);
            }
        }
    }
}
