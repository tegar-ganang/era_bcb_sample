package org.processmining.mining.organizationmining.util;

import java.util.Arrays;
import java.util.BitSet;

/**
 * An efficient, fast, and sparse storage for indexed doubles. (as used in trace
 * clustering profiles)
 * 
 * @author Christian W. Guenther (christian@deckfour.org)
 * 
 */
public class EfficientSparseDoubleArray {

    protected double sparseValue;

    protected BitSet indexUsedMap;

    protected int[] indices;

    protected double[] values;

    protected int realSize;

    public EfficientSparseDoubleArray(double sparseValue) {
        this.sparseValue = sparseValue;
        indexUsedMap = new BitSet();
        indexUsedMap.clear();
        indices = new int[10];
        Arrays.fill(indices, -1);
        values = new double[10];
        realSize = 0;
    }

    public synchronized double get(int virtualIndex) {
        if (indexUsedMap.get(virtualIndex) == false) {
            return sparseValue;
        } else {
            int realIndex = findIndex(virtualIndex);
            return values[realIndex];
        }
    }

    public synchronized void set(int virtualIndex, double value) {
        if (value != sparseValue) {
            if (indexUsedMap.get(virtualIndex) == true) {
                int realIndex = findIndex(virtualIndex);
                values[realIndex] = value;
            } else {
                if (realSize == indices.length) {
                    growArray();
                }
                if (realSize == 0 || virtualIndex > indices[realSize - 1]) {
                    indices[realSize] = virtualIndex;
                    values[realSize] = value;
                } else {
                    for (int i = realSize - 1; i >= 0; i--) {
                        if (indices[i] > virtualIndex) {
                            indices[i + 1] = indices[i];
                            values[i + 1] = values[i];
                            if (i == 0) {
                                indices[i] = virtualIndex;
                                values[i] = value;
                                break;
                            }
                        } else if (indices[i] < virtualIndex) {
                            indices[i + 1] = virtualIndex;
                            values[i + 1] = value;
                            break;
                        } else {
                            System.out.println("ERROR 6: Index already contained?");
                        }
                    }
                }
                indexUsedMap.set(virtualIndex, true);
                realSize++;
            }
        } else if (indexUsedMap.get(virtualIndex) == true) {
            int realIndex = findIndex(virtualIndex);
            int maxIndex = realSize - 1;
            for (int i = realIndex; i < maxIndex; i++) {
                indices[i] = indices[i + 1];
                values[i] = values[i + 1];
            }
            indices[maxIndex] = -1;
            values[maxIndex] = 0.0;
            indexUsedMap.set(virtualIndex, false);
            realSize--;
        }
    }

    protected synchronized void growArray() {
        int largerSize = indices.length + (indices.length / 2);
        int largerIndices[] = new int[largerSize];
        Arrays.fill(largerIndices, -1);
        double largerValues[] = new double[largerSize];
        for (int i = 0; i < realSize; i++) {
            largerIndices[i] = indices[i];
            largerValues[i] = values[i];
        }
        indices = largerIndices;
        values = largerValues;
    }

    protected synchronized int findIndex(int virtualIndex) {
        int low = 0;
        int high = realSize - 1;
        int mid;
        while (low <= high) {
            mid = (low + high) / 2;
            if (indices[mid] > virtualIndex) {
                high = mid - 1;
            } else if (indices[mid] < virtualIndex) {
                low = mid + 1;
            } else {
                return mid;
            }
        }
        throw new AssertionError("Addressing error! (could not find proper index)");
    }
}
