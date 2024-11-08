package uk.ac.imperial.ma.metric.util;

/**
 * Implements a queue data structure of fixed length
 * @author Daniel J. R. May
 * @version 0.0.1
 */
public class FixedLengthQueue {

    private Object[] objary;

    private int intLength;

    public FixedLengthQueue(final Object[] objary) {
        this.objary = objary;
        this.intLength = objary.length;
    }

    public Object add(Object objNew) {
        Object objOld = objary[0];
        for (int i = 0; i < (intLength - 1); i++) {
            objary[i] = objary[i + 1];
        }
        objary[intLength - 1] = objNew;
        return objOld;
    }

    public Object[] getQueueAsArray() {
        return objary;
    }

    public boolean isInQueue(Object objTest) {
        int i = 0;
        while (i < intLength) {
            if (objTest == objary[i]) {
                return true;
            } else {
                i++;
            }
        }
        return false;
    }
}
