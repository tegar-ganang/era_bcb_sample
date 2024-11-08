package com.ibm.tuningfork.core.figure.axis;

public class AxisSet {

    private static final int INITIAL_AXES_SIZE = 2;

    private Axis[] axes;

    private int size;

    public AxisSet() {
        this.axes = new Axis[INITIAL_AXES_SIZE];
        this.size = 0;
    }

    public synchronized void add(Axis Axis) {
        if (size == axes.length) {
            Axis[] newAxiss = new Axis[axes.length + INITIAL_AXES_SIZE];
            System.arraycopy(axes, 0, newAxiss, 0, axes.length);
            axes = newAxiss;
        }
        axes[size] = Axis;
        size++;
    }

    public synchronized void remove(Axis Axis) {
        for (int i = 0; i < size - 1; i++) {
            if (axes[i] == Axis) {
                for (; i < size - 1; i++) {
                    axes[i] = axes[i + 1];
                }
            }
        }
        size--;
        axes[size] = null;
    }

    public AxisIterator iterator() {
        return new AxisIterator(this);
    }

    private class AxisIterator {

        private AxisSet set;

        private int index;

        public AxisIterator(AxisSet set) {
            this.set = set;
            this.index = 0;
        }

        public Axis getNext() {
            synchronized (set) {
                if (index >= size) {
                    return null;
                }
                return set.axes[index++];
            }
        }
    }
}
