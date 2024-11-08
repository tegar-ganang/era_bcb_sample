package com.jpatch.entity;

import java.util.*;

public class MorphTarget_binarysearch {

    private final Set<Object> objects = new HashSet<Object>();

    private final Set<Object> objectsRO = Collections.unmodifiableSet(objects);

    private Accumulator[] accumulators = new Accumulator[0];

    private double[] vectors = new double[0];

    private int[] indices = new int[] { 0 };

    public void addObject(Object object) {
        objects.add(object);
    }

    public Set<Object> getObjects() {
        return objectsRO;
    }

    public void addAccumulator(Accumulator accumulator) {
        if (accumulator.isZero()) {
            return;
        }
        int position = Arrays.binarySearch(accumulators, accumulator);
        if (position < 0) {
            position = -position - 1;
            addAccumulator(accumulator, position);
        } else {
            accumulator.readout(vectors, indices[position]);
            if (isZero(position)) {
                removeAccumulator(position);
            }
        }
    }

    private void addAccumulator(Accumulator accumulator, int position) {
        accumulators = arrayInsert(accumulators, position, accumulator);
        final int offset = accumulator.getDimensions();
        final int index = indices[position];
        vectors = arrayInsert(vectors, index, offset);
        accumulator.readout(vectors, index);
        insertIndex(position, offset);
    }

    private void removeAccumulator(int position) {
        vectors = arrayRemove(vectors, indices[position], accumulators[position].getDimensions());
        accumulators = arrayRemove(accumulators, position);
        removeIndex(position);
    }

    private boolean isZero(int position) {
        for (int i = indices[position], n = indices[position + 1]; i < n; i++) {
            if (vectors[i] != 0.0) {
                return false;
            }
        }
        return true;
    }

    void apply() {
        for (int i = 0; i < accumulators.length; i++) {
            accumulators[i].accumulate(vectors, indices[i]);
        }
    }

    public void reset() {
        for (int i = 0; i < accumulators.length; i++) {
            accumulators[i].reset();
        }
    }

    private void insertIndex(final int position, final int offset) {
        final int[] tmp = new int[indices.length + 1];
        System.arraycopy(indices, 0, tmp, 0, position + 1);
        for (int i = position; i < indices.length; i++) {
            tmp[i + 1] = indices[i] + offset;
        }
        indices = tmp;
    }

    private void removeIndex(final int position) {
        final int[] tmp = new int[indices.length - 1];
        System.arraycopy(indices, 0, tmp, 0, position);
        final int offset = indices[position] - indices[position + 1];
        for (int i = position; i < tmp.length; i++) {
            tmp[i] = indices[i + 1] + offset;
        }
        indices = tmp;
    }

    private static final double[] arrayInsert(double[] array, int position, int length) {
        final double[] tmp = new double[array.length + length];
        System.arraycopy(array, 0, tmp, 0, position);
        System.arraycopy(array, position, tmp, position + length, array.length - position);
        return tmp;
    }

    private static final double[] arrayRemove(double[] array, int position, int length) {
        final double[] tmp = new double[array.length - length];
        System.arraycopy(array, 0, tmp, 0, position);
        System.arraycopy(array, position + length, tmp, position, tmp.length - position);
        return tmp;
    }

    private static final int[] arrayInsert(int[] array, int position, int value) {
        final int[] tmp = new int[array.length + 1];
        System.arraycopy(array, 0, tmp, 0, position);
        tmp[position] = value;
        System.arraycopy(array, position, tmp, position + 1, array.length - position);
        return tmp;
    }

    private static final int[] arrayRemove(int[] array, int position) {
        final int[] tmp = new int[array.length - 1];
        System.arraycopy(array, 0, tmp, 0, position);
        System.arraycopy(array, position + 1, tmp, position, tmp.length - position);
        return tmp;
    }

    private static final Accumulator[] arrayInsert(Accumulator[] array, int position, Accumulator value) {
        final Accumulator[] tmp = new Accumulator[array.length + 1];
        System.arraycopy(array, 0, tmp, 0, position);
        tmp[position] = value;
        System.arraycopy(array, position, tmp, position + 1, array.length - position);
        return tmp;
    }

    private static final Accumulator[] arrayRemove(Accumulator[] array, int position) {
        final Accumulator[] tmp = new Accumulator[array.length - 1];
        System.arraycopy(array, 0, tmp, 0, position);
        System.arraycopy(array, position + 1, tmp, position, tmp.length - position);
        return tmp;
    }

    private void dump() {
        for (int i = 0; i < accumulators.length; i++) {
            System.out.print(i + "\t" + accumulators[i] + "\t");
            for (int j = indices[i]; j < indices[i + 1]; j++) {
                System.out.print(vectors[j] + " ");
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        MorphTarget_binarysearch mt = new MorphTarget_binarysearch();
        Accumulator[] accs = new Accumulator[] { new ScalarAccumulator(), new ScalarAccumulator(), new Tuple3Accumulator(), new Tuple3Accumulator(), new ScalarAccumulator(), new Tuple3Accumulator() };
        ((ScalarAccumulator) accs[0]).setValue(1);
        ((ScalarAccumulator) accs[1]).setValue(2);
        ((Tuple3Accumulator) accs[2]).setValue(3, 4, 5);
        ((Tuple3Accumulator) accs[3]).setValue(6, 7, 8);
        ((ScalarAccumulator) accs[4]).setValue(9);
        ((Tuple3Accumulator) accs[5]).setValue(10, 11, 12);
        Random rnd = new Random();
        for (int k = 0; k < 2; k++) {
            for (int i = 0; i < accs.length; i++) {
                mt.addAccumulator(accs[i]);
                mt.dump();
                System.out.println();
            }
        }
        ((Tuple3Accumulator) accs[5]).setValue(-20, -22, -24);
        mt.addAccumulator(accs[5]);
        mt.dump();
        mt.removeAccumulator(4);
        mt.dump();
    }
}
