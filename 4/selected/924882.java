package de.ios.framework.basic;

import java.util.*;

/**
 * Class to perform sorts on list of Comparable-items.
 *
 * @see Sortable
 * @see Comparable
 */
public class Sorter {

    /** Sort upwards. */
    public static final int ASCENDING = 1;

    /** Sort downwards. */
    public static final int DESCENDING = 2;

    /**
   * Sorts vector with quick-sort.
   *
   * @param list       Vector of Comparable elements.
   * @param direction  Direction (ASCENDING, DESCENDING).
   */
    public static Vector quickSort(Vector elements, int direction) {
        if (measureTimes) initStart = System.currentTimeMillis();
        RingList rl = new RingList(elements);
        if (measureTimes) sortStart = System.currentTimeMillis();
        rl.sort(direction);
        if (measureTimes) resultStart = System.currentTimeMillis();
        elements = rl.getVector();
        if (measureTimes) cleanStart = System.currentTimeMillis();
        rl.removeAllElements();
        if (measureTimes) endTime = System.currentTimeMillis();
        if (debug && measureTimes) {
            Debug.println(Debug.BIGINFO, "Sorter", "Used time for sorting " + elements.size() + " items: " + (endTime - initStart) + " ms (" + "init " + (sortStart - initStart) + " ms, " + "sort " + (resultStart - sortStart) + " ms, " + "result " + (cleanStart - resultStart) + " ms, " + "cleanup " + (endTime - cleanStart) + " ms)");
        }
        return elements;
    }

    /**
   * Sorts with quick-sort.
   *
   * @param list       Sortable object.
   * @param direction  Direction (ASCENDING, DESCENDING).
   */
    public static void quickSort(Sortable list, int direction) {
        if (measureTimes) readStart = System.currentTimeMillis();
        Vector elements = list.getVector();
        elements = quickSort(elements, direction);
        if (measureTimes) writeStart = System.currentTimeMillis();
        list.setVector(elements);
        if (measureTimes) endTime = System.currentTimeMillis();
        if (debug && measureTimes) {
            Debug.println(Debug.BIGINFO, "Sorter", "Additional time for sorting Sortable: " + "scan " + (initStart - readStart) + " ms, " + "write " + (endTime - writeStart) + " ms.");
        }
    }

    /** 
   * Flags for debugging-output. If true some statistics are printed to Debug.BIGINFO-channel. 
   * "measureTimes" must be true.
   */
    public static final boolean debug = false;

    /** Calculate times needed for sorting. */
    public static final boolean measureTimes = true;

    /** Starttime for reading Vector from Sortable-instance. */
    public static long readStart = 0;

    /** Starttime for initialisation or RingList. */
    public static long initStart = 0;

    /** Starttime for sorting. */
    public static long sortStart = 0;

    /** Starttime for retrievung result-vector from RingList. */
    public static long resultStart = 0;

    /** Starttime for cleanup on RingList. */
    public static long cleanStart = 0;

    /** Starttime for writing Vector to Sortable-instance. */
    public static long writeStart = 0;

    /** Endtime for sorting. */
    public static long endTime = 0;
}
