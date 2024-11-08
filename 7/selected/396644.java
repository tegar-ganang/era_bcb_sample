package net.jadoth.test.sorting;

import java.util.Comparator;

public class MainTestMovesort {

    private static <E> void movesort(final E[] values, final E[] buffer, final int start, final int bound, final Comparator<? super E> comparator) {
        int t = start;
        E v1, v2;
        for (; t < bound; t++) {
            if (comparator.compare(v1 = values[t], v2 = values[t + 1]) <= 0) {
                buffer[t] = values[t];
            } else {
                buffer[t] = values[t + 1];
                values[t + 1] = values[t];
            }
        }
        buffer[t] = values[t];
    }

    public static void main(final String[] args) {
        final SortValue[] values = SortValue.init(5, 4, 3, 2, 1);
        final SortValue[] buffer = new SortValue[values.length];
        SortValue.print(values);
        movesort(values, buffer, 0, values.length - 1, SortValue.compare);
        SortValue.print(buffer);
    }
}
