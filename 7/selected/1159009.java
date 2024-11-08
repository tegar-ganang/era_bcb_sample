package au.vermilion.utils;

import java.lang.reflect.Array;

/**
 * This class allows us to use shorthand methods for keeping a dynamic list
 * while allowing us to run through the elements without having to first create
 * an iterator. The structure is 20-30% faster for inserts but about 5-10% slower
 * for removals and does not shrink. The structure is up to 10x faster for repeated
 * scans of short lists.<br>
 * <br>
 * The fastest possible iterator pattern is:<br>
 * <br>
 *    final int l = i.length;<br>
 *    final T[] d = i.data;<br>
 *    for (int x = 0; x &lt; l; x++)<br>
 *    {<br>
 *         accum += d[x];<br>
 */
public final class ExposedArrayList<T> {

    private static final float GROW_FACTOR = 1.8f;

    private static final int INITIAL_SIZE = 30;

    /**
     * The array of values stored in the list.
     */
    public T[] data = null;

    /**
     * The length of the current set, less than or equal to the array length.
     */
    public int length = -1;

    /**
     * This is the class we are storing, for when we allocate arrays.
     */
    private final Class tClass;

    /**
     * To simplify the implementation, please pass the .class of the desired type.
     */
    @SuppressWarnings("unchecked")
    public ExposedArrayList(Class t) {
        tClass = t;
        length = 0;
        data = (T[]) Array.newInstance(tClass, INITIAL_SIZE);
    }

    /**
     * Adds a new entry to the list, growing the array if necessary.
     */
    public void add(final T obj) {
        if (length >= data.length) reallocate();
        data[length] = obj;
        length++;
    }

    /**
     * Removes the entry at the specified index, shuffling the array backwards.
     */
    public void remove(final int index) {
        for (int x = index; x < length - 1; x++) {
            data[x] = data[x + 1];
        }
        data[length - 1] = null;
        length--;
    }

    /**
     * Removes the specified object, if found, shuffling the array backwards.
     */
    public void remove(final T obj) {
        for (int x = 0; x < length; x++) {
            if (data[x] == obj) {
                remove(x);
                break;
            }
        }
    }

    /**
     * Clears the list, releasing all references but maintaining the size.
     */
    public void clear() {
        for (int x = 0; x < length; x++) data[x] = null;
        length = 0;
    }

    /**
     * Doesn't really empty the list, just sets the length to zero.
     */
    public void fastclear() {
        length = 0;
    }

    /**
     * Reallocates the array if it has reached capacity.
     */
    @SuppressWarnings("unchecked")
    private void reallocate() {
        final T[] tData = (T[]) Array.newInstance(tClass, (int) (data.length * GROW_FACTOR));
        for (int x = 0; x < length; x++) tData[x] = data[x];
        data = tData;
    }

    /**
     * Copies the contents of another exposed arraylist into this one.
     */
    @SuppressWarnings("unchecked")
    public void copyFrom(ExposedArrayList list) {
        fastclear();
        if (list == null) return;
        for (int x = 0; x < list.length; x++) {
            T obj = (T) list.data[x];
            if (obj != null && obj.getClass() == tClass) add(obj);
        }
    }

    /**
     * Adds the contents of an array of T to this list.
     * @param array
     */
    public void addAll(T[] array) {
        for (int x = 0; x < array.length; x++) {
            add(array[x]);
        }
    }

    /**
     * Adds the contents of another list of T to this list.
     * @param array
     */
    public void addAll(ExposedArrayList<T> list) {
        for (int x = 0; x < list.length; x++) {
            add(list.data[x]);
        }
    }

    /**
     * Copies the contents of this list into an existing array.
     * @param array
     */
    public void toArray(T[] array) {
        for (int x = 0; x < length; x++) {
            array[x] = data[x];
        }
    }

    /**
     * Returns an array containing the contents of this list.
     * @return
     */
    @SuppressWarnings("unchecked")
    public T[] toArray() {
        T[] array = (T[]) Array.newInstance(tClass, length);
        for (int x = 0; x < length; x++) {
            array[x] = data[x];
        }
        return array;
    }
}
