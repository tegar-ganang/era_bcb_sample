package net.jadoth.collections;

import java.util.Comparator;
import net.jadoth.collections.sorting.Sortable;
import net.jadoth.math.FastRandom;

/**
 * @author Thomas M�nz
 *
 */
public abstract class JaSort {

    public static final Comparator<Boolean> compareBoolean = new Comparator<Boolean>() {

        @Override
        public int compare(final Boolean b1, final Boolean b2) {
            if (b1 == null) {
                return b2 == null ? 0 : -1;
            } else if (b2 == null) {
                return 1;
            }
            return !b1.booleanValue() && b2.booleanValue() ? -1 : !b1.booleanValue() && !b2.booleanValue() ? 1 : 0;
        }
    };

    public static final Comparator<Byte> compareByte = new Comparator<Byte>() {

        @Override
        public int compare(final Byte b1, final Byte b2) {
            if (b1 == null) {
                return b2 == null ? 0 : -1;
            } else if (b2 == null) {
                return 1;
            }
            return b1.byteValue() < b2.byteValue() ? -1 : b1.byteValue() > b2.byteValue() ? 1 : 0;
        }
    };

    public static final Comparator<Short> compareShort = new Comparator<Short>() {

        @Override
        public int compare(final Short s1, final Short s2) {
            if (s1 == null) {
                return s2 == null ? 0 : -1;
            } else if (s2 == null) {
                return 1;
            }
            return s1.shortValue() < s2.shortValue() ? -1 : s1.shortValue() > s2.shortValue() ? 1 : 0;
        }
    };

    public static final Comparator<Integer> compareInteger = new Comparator<Integer>() {

        @Override
        public int compare(final Integer i1, final Integer i2) {
            if (i1 == null) {
                return i2 == null ? 0 : -1;
            } else if (i2 == null) {
                return 1;
            }
            return i1.intValue() < i2.intValue() ? -1 : i1.intValue() > i2.intValue() ? 1 : 0;
        }
    };

    public static final Comparator<Long> compareLong = new Comparator<Long>() {

        @Override
        public int compare(final Long l1, final Long l2) {
            if (l1 == null) {
                return l2 == null ? 0 : -1;
            } else if (l2 == null) {
                return 1;
            }
            return l1.longValue() < l2.longValue() ? -1 : l1.longValue() > l2.longValue() ? 1 : 0;
        }
    };

    public static final Comparator<Float> compareFloat = new Comparator<Float>() {

        @Override
        public int compare(final Float f1, final Float f2) {
            if (f1 == null) {
                return f2 == null ? 0 : -1;
            } else if (f2 == null) {
                return 1;
            }
            return f1.floatValue() < f2.floatValue() ? -1 : f1.floatValue() > f2.floatValue() ? 1 : 0;
        }
    };

    public static final Comparator<Double> compareDouble = new Comparator<Double>() {

        @Override
        public int compare(final Double d1, final Double d2) {
            if (d1 == null) {
                return d2 == null ? 0 : -1;
            } else if (d2 == null) {
                return 1;
            }
            return d1.doubleValue() < d2.doubleValue() ? -1 : d1.doubleValue() > d2.doubleValue() ? 1 : 0;
        }
    };

    public static final Comparator<Character> compareCharacter = new Comparator<Character>() {

        @Override
        public int compare(final Character c1, final Character c2) {
            if (c1 == null) {
                return c2 == null ? 0 : -1;
            } else if (c2 == null) {
                return 1;
            }
            return c1.charValue() < c2.charValue() ? -1 : c1.charValue() > c2.charValue() ? 1 : 0;
        }
    };

    public static final Comparator<String> compareStringLength = new Comparator<String>() {

        @Override
        public int compare(final String s1, final String s2) {
            if (s1 == null) {
                return s2 == null ? 0 : -1;
            } else if (s2 == null) {
                return 1;
            }
            return s1.length() < s2.length() ? -1 : s1.length() > s2.length() ? 1 : 0;
        }
    };

    private static final int RANDOM_SEGMENTS = 32;

    private static final int R32_SHIFT = 5;

    private static final int R32_RANGE = 1 << R32_SHIFT;

    private static final int R32_SIZE = RANDOM_SEGMENTS * R32_RANGE;

    private static final int R32_MOD = R32_SIZE - 1;

    private static final int R04_SHIFT = 2;

    private static final int R04_RANGE = 1 << R04_SHIFT;

    private static final int R04_SIZE = RANDOM_SEGMENTS * R04_RANGE;

    private static final int R04_MOD = R04_SIZE - 1;

    private static final int[] RND32 = new int[R32_SIZE];

    private static final int[] RND04 = new int[R04_SIZE];

    private static int r = 0;

    static {
        initPseudoRandom();
    }

    private static void initPseudoRandom() {
        final FastRandom rnd = new FastRandom();
        for (int segment = 0, random[] = RND32; segment < RANDOM_SEGMENTS; segment++) {
            final int offset = segment * R32_RANGE, bound = offset + R32_RANGE;
            for (int i = offset; i < bound; i++) random[i] = i - offset;
            for (int i = offset; i < bound; i++) swap(random, i, offset + rnd.nextInt(R32_RANGE));
        }
        for (int segment = 0, random[] = RND04; segment < RANDOM_SEGMENTS; segment++) {
            final int offset = segment * R04_RANGE, bound = offset + R04_RANGE;
            for (int i = offset; i < bound; i++) random[i] = i - offset;
            for (int i = offset; i < bound; i++) swap(random, i, offset + rnd.nextInt(R04_RANGE));
        }
    }

    private static int spotTestIndex32Left(final int mid, final int multiplier) {
        return mid - RND32[++r & R32_MOD] * multiplier;
    }

    private static int spotTestIndex32Right(final int mid, final int multiplier) {
        return mid + RND32[++r & R32_MOD] * multiplier;
    }

    private static int spotTestIndex04Left(final int mid, final int multiplier) {
        return mid - RND04[++r & R04_MOD] * multiplier;
    }

    private static int spotTestIndex04Right(final int mid, final int multiplier) {
        return mid + RND04[++r & R04_MOD] * multiplier;
    }

    private static int log2(final int length) {
        return length < 8 ? 3 : length < 16 ? 4 : length < 32 ? 5 : length < 64 ? 6 : length < 128 ? 7 : length < 256 ? 8 : length < 512 ? 9 : length < 1024 ? 10 : length < 2048 ? 11 : length < 4096 ? 12 : length < 8192 ? 13 : length < 16384 ? 14 : length < 32768 ? 15 : length < 65536 ? 16 : length < 131072 ? 17 : length < 262144 ? 18 : length < 524288 ? 19 : length < 1048576 ? 20 : length < 2097152 ? 21 : length < 4194304 ? 22 : length < 8388608 ? 23 : length < 16777216 ? 24 : length < 33554432 ? 25 : length < 67108864 ? 26 : length < 134217728 ? 27 : length < 268435456 ? 28 : length < 536870912 ? 29 : 30;
    }

    private static void swap(final int[] values, final int l, final int r) {
        final int t = values[l];
        values[l] = values[r];
        values[r] = t;
    }

    private static void swap(final Object[] values, final int l, final int r) {
        final Object t = values[l];
        values[l] = values[r];
        values[r] = t;
    }

    private static void checkRange(final int[] values, final int start, final int bound) {
        if (start < 0) throw new ArrayIndexOutOfBoundsException(start);
        if (start >= bound) throw new IllegalArgumentException("invalid sorting range");
        if (bound > values.length) throw new ArrayIndexOutOfBoundsException(bound);
    }

    private static boolean checkRange(final Object[] values, final int start, final int bound) {
        if (start < 0) throw new ArrayIndexOutOfBoundsException(start);
        if (bound > values.length) throw new ArrayIndexOutOfBoundsException(bound);
        if (start > bound) throw new IllegalArgumentException("invalid sorting range");
        return bound - start > 1;
    }

    private static <E> E[] mergesortCache(final E[] buffer, final E[] values) {
        if (buffer == null) return values.clone();
        if (buffer == values) throw new IllegalArgumentException("buffer may not be identical to values array");
        if (buffer.length < values.length) {
            return values.clone();
        }
        System.arraycopy(values, 0, buffer, 0, values.length);
        return buffer;
    }

    private static int checkIterationRange(final int[] values, final int startIndex, final int endIndex) {
        if (startIndex == endIndex) {
            if (startIndex < 0 || startIndex >= values.length) throw new ArrayIndexOutOfBoundsException(startIndex);
            return 0;
        } else if (startIndex < endIndex) {
            if (startIndex < 0) throw new ArrayIndexOutOfBoundsException(startIndex);
            if (endIndex >= values.length) throw new ArrayIndexOutOfBoundsException(endIndex);
            return 1;
        }
        if (startIndex >= values.length) throw new ArrayIndexOutOfBoundsException(startIndex);
        if (endIndex < 0) throw new ArrayIndexOutOfBoundsException(endIndex);
        return -1;
    }

    private static int checkIterationRange(final Object[] values, final int startIndex, final int endIndex) {
        if (startIndex == endIndex) {
            if (startIndex < 0 || startIndex >= values.length) throw new ArrayIndexOutOfBoundsException(startIndex);
            return 0;
        } else if (startIndex < endIndex) {
            if (startIndex < 0) throw new ArrayIndexOutOfBoundsException(startIndex);
            if (endIndex >= values.length) throw new ArrayIndexOutOfBoundsException(endIndex);
            return 1;
        }
        if (startIndex >= values.length) throw new ArrayIndexOutOfBoundsException(startIndex);
        if (endIndex < 0) throw new ArrayIndexOutOfBoundsException(endIndex);
        return -1;
    }

    private static void insertionsort0(final int[] values, final int start, final int bound) {
        for (int i = start; i < bound; i++) {
            for (int j = i; j > start && values[j - 1] > values[j]; j--) {
                swap(values, j, j - 1);
            }
        }
    }

    private static <E> void insertionsort0(final E[] values, final int start, final int bound, final Comparator<? super E> comparator) {
        for (int i = start; i < bound; i++) {
            for (int j = i; j > start && comparator.compare(values[j - 1], values[j]) > 0; j--) {
                swap(values, j, j - 1);
            }
        }
    }

    private static <E> boolean tryInsertionsort(final E[] values, final int start, final int bound, final Comparator<? super E> comparator) {
        int c = bound - start;
        for (int i = start; i < bound; i++) {
            for (int j = i; j > start && comparator.compare(values[j - 1], values[j]) > 0; j--) {
                swap(values, j, j - 1);
                if (--c == 0) {
                    return false;
                }
            }
        }
        return true;
    }

    @SafeVarargs
    public static final <T> void sortAll(final Comparator<? super T> sortation, final Sortable<T>... sortables) {
        for (int i = 0; i < sortables.length; i++) {
            if (sortables[i] == null) continue;
            sortables[i].sort(sortation);
        }
    }

    public static void insertionsort(final boolean[] values) {
        for (int i = 0, j = i, len = values.length - 1; i < len; j = ++i) {
            final boolean ai = values[i + 1];
            while (!ai && values[j]) {
                values[j + 1] = values[j];
                if (j-- == 0) break;
            }
            values[j + 1] = ai;
        }
    }

    public static void insertionsort(final byte[] values) {
        for (int i = 0, j = i, len = values.length - 1; i < len; j = ++i) {
            final byte ai = values[i + 1];
            while (ai < values[j]) {
                values[j + 1] = values[j];
                if (j-- == 0) break;
            }
            values[j + 1] = ai;
        }
    }

    public static void insertionsort(final short[] values) {
        for (int i = 0, j = i, len = values.length - 1; i < len; j = ++i) {
            final short ai = values[i + 1];
            while (ai < values[j]) {
                values[j + 1] = values[j];
                if (j-- == 0) break;
            }
            values[j + 1] = ai;
        }
    }

    public static void insertionsort(final int[] values) {
        for (int i = 0, j = i, len = values.length - 1; i < len; j = ++i) {
            final int ai = values[i + 1];
            while (ai < values[j]) {
                values[j + 1] = values[j];
                if (j-- == 0) break;
            }
            values[j + 1] = ai;
        }
    }

    public static void insertionsort(final long[] values) {
        for (int i = 0, j = i, len = values.length - 1; i < len; j = ++i) {
            final long ai = values[i + 1];
            while (ai < values[j]) {
                values[j + 1] = values[j];
                if (j-- == 0) break;
            }
            values[j + 1] = ai;
        }
    }

    public static void insertionsort(final float[] values) {
        for (int i = 0, j = i, len = values.length - 1; i < len; j = ++i) {
            final float ai = values[i + 1];
            while (ai < values[j]) {
                values[j + 1] = values[j];
                if (j-- == 0) break;
            }
            values[j + 1] = ai;
        }
    }

    public static void insertionsort(final double[] values) {
        for (int i = 0, j = i, len = values.length - 1; i < len; j = ++i) {
            final double ai = values[i + 1];
            while (ai < values[j]) {
                values[j + 1] = values[j];
                if (j-- == 0) break;
            }
            values[j + 1] = ai;
        }
    }

    public static void insertionsort(final char[] values) {
        for (int i = 0, j = i, len = values.length - 1; i < len; j = ++i) {
            final char ai = values[i + 1];
            while (ai < values[j]) {
                values[j + 1] = values[j];
                if (j-- == 0) break;
            }
            values[j + 1] = ai;
        }
    }

    public static <E> void insertionsort(final E[] values, final Comparator<? super E> comparator) {
        for (int i = 0, j = i, len = values.length - 1; i < len; j = ++i) {
            final E ai = values[i + 1];
            while (comparator.compare(ai, values[j]) < 0) {
                values[j + 1] = values[j];
                if (j-- == 0) break;
            }
            values[j + 1] = ai;
        }
    }

    /**
	 * Sorts the passed array as true instances (i.e. with a stable sorting algorithm) that adapts to already sorted
	 * or nearly sorted order.
	 * <p>
	 * This method is best used as a general purpose sort.
	 * <p>
	 * For a subranged version, see {@link #sort(E[], int, int, Comparator)}.
	 * <p>
	 * Due to sorting the passed array in a stable and fast (O(n log(n)) fashion, each call of this method instantiates
	 * an internal buffer array with the same size as the passed array. If the repeated creation of buffer arrays shall
	 * be prevented, use {@link #bufferSort(E[], E[], Comparator)} to explicitely provide a reusable buffer.
	 * <p>
	 * If maintaining the orginal order of equal elements (stability) is not required,
	 * {@link #valueSort(Object[], Comparator)} usually yields better performance and also does not require additional
	 * storage space by using an unstable sorting algorithm.<br>
	 * Note that unstable sorting algorithms can achieve stable results, too, if the passed array contains only distinct
	 * elements (e.g. the content of a set) or if all equal elements are just references to the same instance.
	 *
	 * @param <E> the type of the elements to be sorted.
	 * @param elements the elements to be sorted.
	 * @param comparator the {@link Comparator} defining the sortation order of the elements.
	 * @see #valueSort(E[], Comparator)
	 * @see #bufferSort(E[], E[], Comparator)
	 * @see #sort(E[], int, int, Comparator)
	 */
    public static <E> void sort(final E[] elements, final Comparator<? super E> comparator) {
        adaptiveMergesort(elements.clone(), elements, 0, elements.length, comparator, log2(elements.length));
    }

    /**
	 * Subranged version of {@link #sort(E[], Comparator)}.
	 * <p>
	 * Example: {@code sort(myElements, 0, 5, myElementComparator} sorts the first 5 elements of array
	 * {@code myElements} (indices 0 to 4).
	 * <p>
	 * For further information, see {@link #sort(E[], Comparator)}.
	 *
	 * @param <E> the type of the elements to be sorted.
	 * @param elements the elements to be sorted.
	 * @param start the starting index (inclusive) of the subrange to be sorted.
	 * @param bound the bounding index (exclusive) of the subrange to be sorted.
	 * @param comparator the {@link Comparator} defining the sortation order of the elements.
	 */
    public static <E> void sort(final E[] elements, final int start, final int bound, final Comparator<? super E> comparator) {
        if (checkRange(elements, start, bound)) {
            adaptiveMergesort(elements.clone(), elements, start, bound, comparator, log2(bound - start));
        }
    }

    /**
	 * Sorts the passed array as values (i.e. with an unstable sorting algorithm).
	 * <p>
	 * This method is best used for sorting arrays where stability is not important of that consist only of
	 * distinct values of equal values that actually are just duplicate references to the same instance.
	 * <p>
	 * For a subranged version, see {@link #valueSort(V[], int, int, Comparator)}.
	 * <p>
	 * The used algorithm works inplace, i.e. does not instantiate any additional instances.
	 * <p>
	 * If maintaining the orginal order of equal elements (stability) is required, {@link #sort(Object[], Comparator)}
	 * has to be used instead, which maintains stability at the cost of performance and the need of additional
	 * temporary storage.
	 *
	 * @param <V> the type of the values to be sorted.
	 * @param values the values to be sorted.
	 * @param comparator the {@link Comparator} defining the sortation order of the values.
	 */
    public static <V> void valueSort(final V[] values, final Comparator<? super V> comparator) {
        dualPivotQuicksort(values, 0, values.length - 1, comparator);
    }

    /**
	 * Subranged version of {@link #valueSort(V[], Comparator)}.
	 * <p>
	 * Example: {@code valueSort(myValues, 0, 5, myvalueComparator} sorts the first 5 values of array
	 * {@code myElements} (indices 0 to 4).
	 * <p>
	 * For further information, see {@link #sort(V[], Comparator)}.
	 *
	 * @param <V> the type of the values to be sorted.
	 * @param values the values to be sorted.
	 * @param start the starting index (inclusive) of the subrange to be sorted.
	 * @param bound the bounding index (exclusive) of the subrange to be sorted.
	 * @param comparator the {@link Comparator} defining the sortation order of the elements.
	 */
    public static <V> void valueSort(final V[] values, final int start, final int bound, final Comparator<? super V> comparator) {
        if (checkRange(values, start, bound)) {
            dualPivotQuicksort(values, start, bound - 1, comparator);
        }
    }

    /**
	 * Variation of {@link #sort(E[], Comparator)} that allows to pass an additional array to be used as the sorting
	 * algorithm's internal buffer.
	 * <p>
	 * Use this method if the repeated buffer array instantiation of {@link #sort(E[], Comparator)} causes problems.
	 * <p>
	 * If the passed buffer array is too small, a new buffer array of appropriate size is instantiated. The buffer
	 * (NOT the sorted array!) is returned.
	 * <p>
	 * The buffer array is NOT cleared after the sorting is completed and may contain partial and inconsistent
	 * intermediate results. It is the external buffer array provider's responsibility to ensure programmatical
	 * correctness and memory leak avoidance for any content that might remain in the buffer array.
	 *
	 * @param <E> the type of the elements to be sorted.
	 * @param elements the array containing the elements to be sorted.
	 * @param buffer the array to be used as a buffer by the sorting algorithm.
	 * @param comparator the comparator defining the sortation order of the elements.
	 * @return the passed buffer array or the newly created buffer array if the passed buffer array was too small.
	 * @see #sort(E[], Comparator)
	 * @see #bufferSort(E[], E[], int, int, Comparator)
	 */
    public static <E> E[] bufferSort(final E[] elements, E[] buffer, final Comparator<? super E> comparator) {
        adaptiveMergesort(buffer = mergesortCache(buffer, elements), elements, 0, elements.length, comparator, log2(elements.length));
        return buffer;
    }

    /**
	 * Subranged version of {@link #bufferSort(E[], Comparator)}.
	 * <p>
	 * Example: {@code bufferSort(myElements, 0, 5, myElementComparator} sorts the first 5 elements of array
	 * {@code myElements} (indices 0 to 4).
	 * <p>
	 * For further information, see {@link #sort(E[], Comparator)}.
	 *
	 * @param <E> the type of the elements to be sorted.
	 * @param elements the elements to be sorted.
	 * @param start the starting index (inclusive) of the subrange to be sorted.
	 * @param bound the bounding index (exclusive) of the subrange to be sorted.
	 * @param comparator the {@link Comparator} defining the sortation order of the elements.
	 * @return the passed buffer array or the newly created buffer array if the passed buffer array was too small.
	 */
    public static <E> E[] bufferSort(final E[] elements, E[] buffer, final int start, final int bound, final Comparator<? super E> comparator) {
        if (checkRange(elements, start, bound)) {
            adaptiveMergesort(buffer = mergesortCache(buffer, elements), elements, start, bound, comparator, log2(bound - start));
        }
        return buffer;
    }

    private static <E> void adaptiveMergesort(final E[] buffer, final E[] values, final int start, final int bound, final Comparator<? super E> c, final int log2) {
        if (bound - start < 7) {
            insertionsort0(values, start, bound, c);
            return;
        }
        final int mid;
        if (log2 > 6) {
            final int mult = (mid = start + bound >>> 1) - start >>> R32_SHIFT;
            checkFordwardSorted: {
                for (int i = 0; i < log2; i++) {
                    if (c.compare(values[spotTestIndex32Left(mid, mult)], values[spotTestIndex32Right(mid, mult)]) > 0) break checkFordwardSorted;
                }
                if (tryInsertionsort(values, start, bound, c)) return;
                System.arraycopy(values, start, buffer, start, bound - start);
            }
        } else {
            final int mult = (mid = start + bound >>> 1) - start >>> R04_SHIFT;
            checkFordwardSorted: {
                for (int i = 1; i < log2; i++) {
                    if (c.compare(values[spotTestIndex04Left(mid, mult)], values[spotTestIndex04Right(mid, mult)]) > 0) break checkFordwardSorted;
                }
                if (tryInsertionsort(values, start, bound, c)) return;
                System.arraycopy(values, start, buffer, start, bound - start);
            }
        }
        adaptiveMergesort(values, buffer, start, mid, c, log2 - 1);
        adaptiveMergesort(values, buffer, mid, bound, c, log2 - 1);
        if (c.compare(buffer[mid - 1], buffer[mid]) <= 0) System.arraycopy(buffer, start, values, start, bound - start); else {
            for (int l, i = l = start, r = mid; i < bound; i++) {
                if (r >= bound || l < mid && c.compare(buffer[l], buffer[r]) <= 0) {
                    values[i] = buffer[l];
                    l++;
                } else {
                    values[i] = buffer[r];
                    r++;
                }
            }
        }
    }

    /**
	 * Experimental parallel sorting that splits the sorting work up into two parts.
	 * <p>
	 * As the work to do has to be big enough to pay off, the algorithm is capped at a certain minimum length
	 * (hardcoded 8192 at the moment) under which a normal singlethreaded sorting is executed. Really notable
	 * performance gain is achieved for lengths above 10.000. Length above 1 million yields performance boosts
	 * around 40% to 100%.
	 *
	 * @param <E>
	 * @param elements
	 * @param comparator
	 */
    public static <E> void parallelSort(final E[] elements, final Comparator<? super E> comparator) {
        if (elements.length < 8192) {
            adaptiveMergesort(elements.clone(), elements, 0, elements.length, comparator, log2(elements.length));
            return;
        }
        final E[] buffer = elements.clone();
        final int bound, log2length = log2(bound = elements.length);
        final int mid = bound >>> 1;
        final boolean[] board = new boolean[2];
        new Thread() {

            @Override
            public void run() {
                adaptiveMergesort(elements, buffer, mid, elements.length, comparator, log2length);
                board[1] = true;
                if (board[0]) {
                    synchronized (board) {
                        board.notify();
                    }
                }
            }
        }.start();
        adaptiveMergesort(elements, buffer, 0, mid, comparator, log2length);
        board[0] = true;
        if (!board[1]) {
            synchronized (board) {
                try {
                    board.wait();
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        if (comparator.compare(buffer[mid - 1], buffer[mid]) <= 0) System.arraycopy(buffer, mid, elements, mid, bound - mid); else {
            for (int l, i = l = 0, r = mid; i < bound; i++) {
                if (r >= bound || l < mid && comparator.compare(buffer[l], buffer[r]) <= 0) {
                    elements[i] = buffer[l];
                    l++;
                } else {
                    elements[i] = buffer[r];
                    r++;
                }
            }
        }
    }

    /**
	 * Sorts the passed values array.
	 * <p>
	 * For a subranged version, see {@link #sort(int[], int, int)}.
	 * <p>
	 * The used algorithm works inplace, i.e. does not instantiate any additional instances.
	 *
	 * @param values the values to be sorted.
	 */
    public static final void sort(final int[] values) throws NullPointerException {
        dualPivotQuicksort(values, 0, values.length - 1);
    }

    /**
	 * Subranged version of {@link #valueSort(V[], Comparator)}.
	 * <p>
	 * Example: {@code sort(myValues, 0, 5} sorts the first 5 values of array
	 * {@code myElements} (indices 0 to 4).
	 * <p>
	 * For further information, see {@link #sort(int[])}.
	 *
	 * @param values the values to be sorted.
	 * @param start the starting index (inclusive) of the subrange to be sorted.
	 * @param bound the bounding index (exclusive) of the subrange to be sorted.
	 * @param comparator the {@link Comparator} defining the sortation order of the elements.
	 */
    public static final void sort(final int[] values, final int start, final int bound) throws NullPointerException, ArrayIndexOutOfBoundsException {
        checkRange(values, start, bound);
        dualPivotQuicksort(values, start, bound - 1);
    }

    public static final <E> void quicksort(final E[] values, final Comparator<? super E> comparator) throws NullPointerException {
        dualPivotQuicksort(values, 0, values.length - 1, comparator);
    }

    public static final <E> void quicksort(final E[] values, final int start, final int bound, final Comparator<? super E> comparator) throws NullPointerException {
        if (checkRange(values, start, bound)) {
            dualPivotQuicksort(values, start, bound - 1, comparator);
        }
    }

    private static void simpleQuicksort(final int[] values, final int low, final int high) throws NullPointerException, ArrayIndexOutOfBoundsException {
        if (high - low < 8) {
            insertionsort0(values, low, high + 1);
            return;
        }
        int left, right;
        for (final int pivot = values[(left = low) + (right = high) >>> 1]; left <= right; ) {
            while (values[left] < pivot) left++;
            while (values[right] > pivot) right--;
            if (left > right) break;
            swap(values, left++, right--);
        }
        if (left < high) simpleQuicksort(values, left, high);
    }

    private static <E> void simpleQuicksort(final E[] values, final int low, final int high, final Comparator<? super E> cmp) throws NullPointerException, ArrayIndexOutOfBoundsException {
        if (high - low < 8) {
            insertionsort0(values, low, high + 1, cmp);
            return;
        }
        int left, right;
        for (final E pivot = values[(left = low) + (right = high) >>> 1]; left <= right; ) {
            while (cmp.compare(values[left], pivot) < 0) left++;
            while (cmp.compare(values[right], pivot) > 0) right--;
            if (left > right) break;
            swap(values, left++, right--);
        }
        if (right > low) simpleQuicksort(values, low, right, cmp);
        if (left < high) simpleQuicksort(values, left, high, cmp);
    }

    public static <E> void mergesort(final E[] values, final Comparator<? super E> comparator) {
        mergesort0(values.clone(), values, 0, values.length, comparator);
    }

    public static <E> void mergesort(final E[] values, final int start, final int bound, final Comparator<? super E> comparator) {
        if (checkRange(values, start, bound)) {
            mergesort0(values.clone(), values, start, bound, comparator);
        }
    }

    public static <E> E[] bufferMergesort(final E[] values, E[] buffer, final Comparator<? super E> comparator) {
        mergesort0(buffer = mergesortCache(buffer, values), values, 0, values.length, comparator);
        return buffer;
    }

    public static <E> E[] bufferMergesort(final E[] values, E[] buffer, final int start, final int bound, final Comparator<? super E> comparator) {
        if (checkRange(values, start, bound)) {
            mergesort0(buffer = mergesortCache(buffer, values), values, start, bound, comparator);
        }
        return buffer;
    }

    private static <E> void mergesort0(final E[] buffer, final E[] values, final int start, final int bound, final Comparator<? super E> c) {
        if (bound - start < 7) {
            insertionsort0(values, start, bound, c);
            return;
        }
        final int mid;
        mergesort0(values, buffer, start, mid = start + bound >>> 1, c);
        mergesort0(values, buffer, mid, bound, c);
        if (c.compare(buffer[mid - 1], buffer[mid]) <= 0) System.arraycopy(buffer, start, values, start, bound - start); else {
            for (int l, i = l = start, r = mid; i < bound; i++) {
                if (r >= bound || l < mid && c.compare(buffer[l], buffer[r]) <= 0) {
                    values[i] = buffer[l];
                    l++;
                } else {
                    values[i] = buffer[r];
                    r++;
                }
            }
        }
    }

    public static void distinctsort(final int[] values) {
        final int[] buffer = new int[values.length];
        distinctsortInto0(values, buffer);
        System.arraycopy(buffer, 0, values, 0, values.length);
    }

    public static void copyDistinctsort(final int[] values, final int[] target) {
        distinctsortInto0(values, target);
    }

    public static void bufferDistinctsort(final int[] values, final int[] buffer) {
        distinctsortInto0(values, buffer);
        System.arraycopy(buffer, 0, values, 0, values.length);
    }

    private static void distinctsortInto0(final int[] values, final int[] target) {
        int distinctsLowBound;
        final int targetLast, length = values.length;
        target[distinctsLowBound = targetLast = target.length - 1] = values[0];
        distinction: for (int i = 1; i < length; i++) {
            for (int t = targetLast; t >= distinctsLowBound; t--) if (target[t] == values[i]) continue distinction;
            target[--distinctsLowBound] = values[i];
        }
        JaSort.simpleQuicksort(target, distinctsLowBound, targetLast);
        for (int targetIndex = -1; distinctsLowBound <= targetLast; distinctsLowBound++) {
            final int current = target[distinctsLowBound];
            for (int i = 0; i < length; i++) if (values[i] == current) target[++targetIndex] = values[i];
        }
    }

    @SuppressWarnings("unchecked")
    public static <E> void distinctsort(final E[] values, final Comparator<? super E> comparator) {
        final E[] buffer = (E[]) new Object[values.length];
        distinctsortInto(values, buffer, 0, values.length, comparator);
        System.arraycopy(buffer, 0, values, 0, values.length);
    }

    public static <E> void bufferDistinctsort(final E[] values, final E[] buffer, final Comparator<? super E> comparator) {
        distinctsortInto(values, buffer, 0, values.length, comparator);
        System.arraycopy(buffer, 0, values, 0, values.length);
    }

    public static <E> void distinctsortInto(final E[] values, final E[] target, final Comparator<? super E> comparator) {
        distinctsortInto(values, target, 0, values.length, comparator);
    }

    private static <E> void distinctsortInto(final E[] values, final E[] target, final int start, final int bound, final Comparator<? super E> comparator) {
        E current, last1, last2, last3, last4, last5, last6, last7;
        int distinctsLowBound, distinctScanBound;
        final int targetLast;
        target[distinctsLowBound = targetLast = bound - 1] = current = last1 = last2 = last3 = last4 = last5 = last6 = last7 = values[start];
        distinctScanBound = targetLast + 7;
        distinction: for (int i = start + 1; i < bound; i++) {
            if (comparator.compare(current, current = values[i]) == 0 || comparator.compare(current, last1) == 0 || comparator.compare(current, last2) == 0 || comparator.compare(current, last3) == 0 || comparator.compare(current, last4) == 0 || comparator.compare(current, last5) == 0 || comparator.compare(current, last6) == 0 || comparator.compare(current, last7) == 0) continue;
            for (int t = targetLast; t >= distinctScanBound; t--) if (comparator.compare(current, target[t]) == 0) continue distinction;
            target[--distinctsLowBound] = current;
            last7 = last6;
            last6 = last5;
            last5 = last4;
            last4 = last3;
            last3 = last2;
            last2 = last1;
            last1 = current;
            distinctScanBound--;
        }
        JaSort.simpleQuicksort(target, distinctsLowBound, targetLast, comparator);
        for (int targetIndex = start - 1; distinctsLowBound <= targetLast; distinctsLowBound++) {
            current = target[distinctsLowBound];
            for (int i = start; i < bound; i++) if (comparator.compare(current, values[i]) == 0) target[++targetIndex] = values[i];
        }
    }

    public static void quicksortDualPivot(final int[] values) {
        dualPivotQuicksort(values, 0, values.length - 1);
    }

    public static <E> void quicksortDualPivot(final E[] values, final Comparator<? super E> comparator) {
        dualPivotQuicksort(values, 0, values.length - 1, comparator);
    }

    private static void dualPivotQuicksort(final int[] a, final int low, final int high) {
        if (high - low < 31) {
            insertionsort0(a, low, high + 1);
            return;
        }
        final int seventh = (high - low + 1 >>> 3) + (high - low + 1 >>> 6) + 1;
        final int e3 = low + high >>> 1;
        final int e2 = e3 - seventh;
        final int e1 = e2 - seventh;
        final int e4 = e3 + seventh;
        final int e5 = e4 + seventh;
        if (a[e2] < a[e1]) {
            final int t = a[e2];
            a[e2] = a[e1];
            a[e1] = t;
        }
        if (a[e3] < a[e2]) {
            final int t = a[e3];
            a[e3] = a[e2];
            a[e2] = t;
            if (t < a[e1]) {
                a[e2] = a[e1];
                a[e1] = t;
            }
        }
        if (a[e4] < a[e3]) {
            final int t = a[e4];
            a[e4] = a[e3];
            a[e3] = t;
            if (t < a[e2]) {
                a[e3] = a[e2];
                a[e2] = t;
                if (t < a[e1]) {
                    a[e2] = a[e1];
                    a[e1] = t;
                }
            }
        }
        if (a[e5] < a[e4]) {
            final int t = a[e5];
            a[e5] = a[e4];
            a[e4] = t;
            if (t < a[e3]) {
                a[e4] = a[e3];
                a[e3] = t;
                if (t < a[e2]) {
                    a[e3] = a[e2];
                    a[e2] = t;
                    if (t < a[e1]) {
                        a[e2] = a[e1];
                        a[e1] = t;
                    }
                }
            }
        }
        final int pivot1, pivot2;
        int left = low;
        int right = high;
        if ((pivot1 = a[e2]) != (pivot2 = a[e4])) {
            a[e2] = a[low];
            a[e4] = a[high];
            while (a[++left] < pivot1) {
            }
            while (a[--right] > pivot2) {
            }
            outer: for (int k = left; k <= right; k++) {
                final int ak = a[k];
                if (ak < pivot1) {
                    a[k] = a[left];
                    a[left] = ak;
                    left++;
                } else if (ak > pivot2) {
                    while (a[right] > pivot2) {
                        if (right-- == k) break outer;
                    }
                    if (a[right] < pivot1) {
                        a[k] = a[left];
                        a[left] = a[right];
                        left++;
                    } else {
                        a[k] = a[right];
                    }
                    a[right] = ak;
                    right--;
                }
            }
            a[low] = a[left - 1];
            a[left - 1] = pivot1;
            a[high] = a[right + 1];
            a[right + 1] = pivot2;
            dualPivotQuicksort(a, low, left - 2);
            dualPivotQuicksort(a, right + 2, high);
            if (left < e1 && e5 < right) {
                while (a[left] == pivot1) left++;
                while (a[right] == pivot2) right--;
                outer: for (int k = left; k <= right; k++) {
                    final int ak = a[k];
                    if (ak == pivot1) {
                        a[k] = a[left];
                        a[left] = ak;
                        left++;
                    } else if (ak == pivot2) {
                        while (a[right] == pivot2) {
                            if (right-- == k) break outer;
                        }
                        if (a[right] == pivot1) {
                            a[k] = a[left];
                            a[left] = pivot1;
                            left++;
                        } else {
                            a[k] = a[right];
                        }
                        a[right] = ak;
                        right--;
                    }
                }
            }
            dualPivotQuicksort(a, left, right);
        } else {
            for (int k = low; k <= right; k++) {
                if (a[k] == pivot1) continue;
                final int ak = a[k];
                if (ak < pivot1) {
                    a[k] = a[left];
                    a[left] = ak;
                    left++;
                } else {
                    while (a[right] > pivot1) {
                        right--;
                    }
                    if (a[right] < pivot1) {
                        a[k] = a[left];
                        a[left] = a[right];
                        left++;
                    } else {
                        a[k] = pivot1;
                    }
                    a[right] = ak;
                    right--;
                }
            }
            dualPivotQuicksort(a, low, left - 1);
            dualPivotQuicksort(a, right + 1, high);
        }
    }

    private static <E> void dualPivotQuicksort(final E[] a, final int low, final int high, final Comparator<? super E> cmp) {
        if (high - low < 16) {
            insertionsort0(a, low, high + 1, cmp);
            return;
        }
        final int seventh = (high - low + 1 >>> 3) + (high - low + 1 >>> 6) + 1;
        final int e3 = low + high >>> 1;
        final int e2 = e3 - seventh;
        final int e1 = e2 - seventh;
        final int e4 = e3 + seventh;
        final int e5 = e4 + seventh;
        if (cmp.compare(a[e2], a[e1]) < 0) {
            final E t = a[e2];
            a[e2] = a[e1];
            a[e1] = t;
        }
        if (cmp.compare(a[e3], a[e2]) < 0) {
            final E t = a[e3];
            a[e3] = a[e2];
            a[e2] = t;
            if (cmp.compare(t, a[e1]) < 0) {
                a[e2] = a[e1];
                a[e1] = t;
            }
        }
        if (cmp.compare(a[e4], a[e3]) < 0) {
            final E t = a[e4];
            a[e4] = a[e3];
            a[e3] = t;
            if (cmp.compare(t, a[e2]) < 0) {
                a[e3] = a[e2];
                a[e2] = t;
                if (cmp.compare(t, a[e1]) < 0) {
                    a[e2] = a[e1];
                    a[e1] = t;
                }
            }
        }
        if (cmp.compare(a[e5], a[e4]) < 0) {
            final E t = a[e5];
            a[e5] = a[e4];
            a[e4] = t;
            if (cmp.compare(t, a[e3]) < 0) {
                a[e4] = a[e3];
                a[e3] = t;
                if (cmp.compare(t, a[e2]) < 0) {
                    a[e3] = a[e2];
                    a[e2] = t;
                    if (cmp.compare(t, a[e1]) < 0) {
                        a[e2] = a[e1];
                        a[e1] = t;
                    }
                }
            }
        }
        final E pivot1, pivot2;
        int left = low;
        int right = high;
        if (cmp.compare(pivot1 = a[e2], pivot2 = a[e4]) != 0) {
            a[e2] = a[low];
            a[e4] = a[high];
            while (cmp.compare(a[++left], pivot1) < 0) {
            }
            while (cmp.compare(a[--right], pivot2) > 0) {
            }
            outer: for (int k = left; k <= right; k++) {
                final E ak = a[k];
                if (cmp.compare(ak, pivot1) < 0) {
                    a[k] = a[left];
                    a[left] = ak;
                    left++;
                } else if (cmp.compare(ak, pivot2) > 0) {
                    while (cmp.compare(a[right], pivot2) > 0) {
                        if (right-- == k) break outer;
                    }
                    if (cmp.compare(a[right], pivot1) < 0) {
                        a[k] = a[left];
                        a[left] = a[right];
                        left++;
                    } else {
                        a[k] = a[right];
                    }
                    a[right] = ak;
                    right--;
                }
            }
            a[low] = a[left - 1];
            a[left - 1] = pivot1;
            a[high] = a[right + 1];
            a[right + 1] = pivot2;
            dualPivotQuicksort(a, low, left - 2, cmp);
            dualPivotQuicksort(a, right + 2, high, cmp);
            if (left < e1 && e5 < right) {
                while (cmp.compare(a[left], pivot1) == 0) left++;
                while (cmp.compare(a[right], pivot2) == 0) right--;
                outer: for (int k = left; k <= right; k++) {
                    final E ak = a[k];
                    if (cmp.compare(ak, pivot1) == 0) {
                        a[k] = a[left];
                        a[left] = ak;
                        left++;
                    } else if (cmp.compare(ak, pivot2) == 0) {
                        while (cmp.compare(a[right], pivot2) == 0) {
                            if (right-- == k) break outer;
                        }
                        if (cmp.compare(a[right], pivot1) == 0) {
                            a[k] = a[left];
                            a[left] = pivot1;
                            left++;
                        } else {
                            a[k] = a[right];
                        }
                        a[right] = ak;
                        right--;
                    }
                }
            }
            dualPivotQuicksort(a, left, right, cmp);
        } else {
            for (int k = low; k <= right; k++) {
                if (cmp.compare(a[k], pivot1) == 0) continue;
                final E ak = a[k];
                if (cmp.compare(ak, pivot1) < 0) {
                    a[k] = a[left];
                    a[left] = ak;
                    left++;
                } else {
                    while (cmp.compare(a[right], pivot1) > 0) right--;
                    if (cmp.compare(a[right], pivot1) < 0) {
                        a[k] = a[left];
                        a[left] = a[right];
                        left++;
                    } else {
                        a[k] = pivot1;
                    }
                    a[right] = ak;
                    right--;
                }
            }
            dualPivotQuicksort(a, low, left - 1, cmp);
            dualPivotQuicksort(a, right + 1, high, cmp);
        }
    }

    public static boolean isIncreasing(final int[] values) {
        for (int i = 1; i < values.length; i++) {
            if (values[i - 1] > values[i]) return false;
        }
        return true;
    }

    public static boolean isStrictlyIncreasing(final int[] values) {
        for (int i = 1; i < values.length; i++) {
            if (values[i - 1] >= values[i]) return false;
        }
        return true;
    }

    public static boolean isDecreasing(final int[] values) {
        for (int i = 1; i < values.length; i++) {
            if (values[i - 1] < values[i]) return false;
        }
        return true;
    }

    public static boolean isStrictlyDecreasing(final int[] values) {
        for (int i = 1; i < values.length; i++) {
            if (values[i - 1] <= values[i]) return false;
        }
        return true;
    }

    public static boolean isIncreasing(final int[] values, final int startIndex, final int endIndex) {
        final int d;
        if ((d = checkIterationRange(values, startIndex, endIndex)) == 0) return true;
        for (int i = startIndex; i != endIndex; ) {
            if (values[i] > values[i += d]) return false;
        }
        return true;
    }

    public static boolean isStrictlyIncreasing(final int[] values, final int startIndex, final int endIndex) {
        final int d;
        if ((d = checkIterationRange(values, startIndex, endIndex)) == 0) return true;
        for (int i = startIndex; i != endIndex; ) {
            if (values[i] >= values[i += d]) return false;
        }
        return true;
    }

    public static boolean isDecreasing(final int[] values, final int startIndex, final int endIndex) {
        final int d;
        if ((d = checkIterationRange(values, startIndex, endIndex)) == 0) return true;
        for (int i = startIndex; i != endIndex; ) {
            if (values[i] < values[i += d]) return false;
        }
        return true;
    }

    public static boolean isStrictlyDecreasing(final int[] values, final int startIndex, final int endIndex) {
        final int d;
        if ((d = checkIterationRange(values, startIndex, endIndex)) == 0) return true;
        for (int i = startIndex; i != endIndex; ) {
            if (values[i] <= values[i += d]) return false;
        }
        return true;
    }

    public static <E> boolean isSorted(final E[] values, final Comparator<? super E> comparator) {
        for (int i = 1; i < values.length; i++) {
            if (comparator.compare(values[i - 1], values[i]) > 0) return false;
        }
        return true;
    }

    public static <E> boolean isStrictlySorted(final E[] values, final Comparator<? super E> comparator) {
        for (int i = 1; i < values.length; i++) {
            if (comparator.compare(values[i - 1], values[i]) >= 0) return false;
        }
        return true;
    }

    public static <E> boolean isReverseSorted(final E[] values, final int startIndex, final int endIndex, final Comparator<? super E> comparator) {
        final int d;
        if ((d = checkIterationRange(values, startIndex, endIndex)) == 0) return true;
        for (int i = startIndex; i != endIndex; ) {
            if (comparator.compare(values[i], values[i += d]) < 0) return false;
        }
        return true;
    }

    public static <E> boolean isStrictlyReverseSorted(final E[] values, final int startIndex, final int endIndex, final Comparator<? super E> comparator) {
        final int d;
        if ((d = checkIterationRange(values, startIndex, endIndex)) == 0) return true;
        for (int i = startIndex; i != endIndex; ) {
            if (comparator.compare(values[i], values[i += d]) <= 0) return false;
        }
        return true;
    }

    /**
	 * Returns the number of subsequent equal values.
	 * <p>
	 * Examples:<br>
	 * {@code uniformity(1,2,3,4,5) == 0}<br>
	 * {@code uniformity(1,1,1,1,1) == 5}<br>
	 * {@code uniformity(1,1,1,2,2) == 3}<br>
	 * {@code uniformity(1,1,3,2,2) == 2}<br>
	 *
	 * @param values the values whose uniformity shall be calculated
	 * @return the uniformity count.
	 */
    public static int uniformitySimple(final int... values) {
        int uniformity = 0;
        int current, last = values[0];
        for (int i = 1; i < values.length; i++) {
            if ((current = values[i]) == last) uniformity++;
            last = current;
        }
        return uniformity;
    }

    public static double uniformity(final int... values) {
        int uniformity = 0;
        int growth = 1;
        int current, last = values[0];
        final int length = values.length;
        for (int i = 1; i < length; i++) {
            if ((current = values[i]) == last) {
                uniformity += ++growth;
            } else {
                growth = 0;
            }
            last = current;
        }
        return (double) uniformity * 2 / (length * (length + 1));
    }
}
