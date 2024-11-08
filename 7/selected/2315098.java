package org.benetech.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang.ArrayUtils;

/**
 * Utilities for dealing with numbers in various ways.
 * @author Reuben Firmin
 */
public final class NumberUtils {

    /**
	 * Large number for initialization.
	 */
    public static final int LARGE_INT = 999999999;

    /**
	 * Non constructor.
	 */
    private NumberUtils() {
    }

    /**
	 * Find the first group in this set of numbers. This is the longest group of numbers where the mean increase is
	 * equal or less to the overall mean.
	 * @param numbers The numbers to find the group in
	 * @return Acceptable group, sorted
	 */
    public static int[] getFirstGroup(final int[] numbers) {
        final Range[] ranges = findGroupRangesWithLessOrEqualMeanGrowth(numbers);
        final Range out = new Range();
        for (int i = 0; i < ranges.length; i++) {
            boolean addRange = false;
            if (i == 0) {
                out.start = ranges[i].start;
                addRange = true;
            } else {
                final double currentMean = getMean(getFirstDifference(numbers), out);
                final Range proposedRange = new Range();
                proposedRange.start = out.start;
                proposedRange.end = ranges[i].end;
                final double combinedMean = getMean(getFirstDifference(numbers), proposedRange);
                final int significance = (int) Math.floor(combinedMean / currentMean);
                if (significance <= 1) {
                    addRange = true;
                }
            }
            if (addRange) {
                out.end = ranges[i].end;
            } else {
                break;
            }
        }
        final int[] outArray = new int[out.end + 1 - out.start];
        for (int i = 0; i < outArray.length; i++) {
            outArray[i] = numbers[out.start + i];
        }
        return outArray;
    }

    /**
	 * The variance in a population.
	 * @param population an array, the population
	 * @return the variance
	 */
    public static double getVariance(final double[] population) {
        long n = 0;
        double mean = 0;
        double s = 0.0;
        for (double x : population) {
            n++;
            final double delta = x - mean;
            mean += delta / n;
            s += delta * (x - mean);
        }
        return (s / n);
    }

    /**
	 * The standard deviation of a population.
	 * @param population an array, the population
	 * @return the standard deviation
	 */
    public static double getStandardDeviation(final double[] population) {
        return Math.sqrt(getVariance(population));
    }

    /**
	 * Get the first difference of the population. This is an array of differences across the population. Population
	 * array is sorted during the process.
	 * @param population The population of numbers
	 * @return int array of differences; size is one less than the total population; e.g. index 0 is  population[1] -
	 * population[0]
	 */
    public static int[] getFirstDifference(final int[] population) {
        Arrays.sort(population);
        final int[] jumps = new int[population.length - 1];
        for (int i = 0; i < jumps.length; i++) {
            jumps[i] = population[i + 1] - population[i];
        }
        return jumps;
    }

    /**
	 * Return the mean (average) of the population.
	 * @param population The population of numbers
	 * @return The mean
	 */
    public static double getMean(final int[] population) {
        final Range range = new Range();
        range.start = 0;
        range.end = population.length - 1;
        return getMean(population, range);
    }

    /**
	 * Return the mean (average) across a section of the population.
	 * @param population The population of numbers
	 * @param range The range of the section to consider, inclusive
	 * @return The mean
	 */
    public static double getMean(final int[] population, final Range range) {
        int sum = 0;
        for (int i = range.start; i <= range.end; i++) {
            sum += population[i];
        }
        return ((double) sum) / ((double) population.length);
    }

    /**
	 * Get the median (midway) value from this sorted population.
	 * @param population The population of numbers, sorted
	 * @return The median
	 */
    public static double getMedian(final int[] population) {
        if (population.length % 2 == 0) {
            final int i1 = population.length / 2;
            final int i2 = i1 - 1;
            return ((double) (population[i1] + population[i2])) / 2;
        } else {
            return population[(int) population.length / 2];
        }
    }

    /**
	 * Return the mean (average) of the population.
	 * @param population The population of numbers
	 * @return The mean
	 */
    public static double getMean(final double[] population) {
        double sum = 0;
        for (double member : population) {
            sum += member;
        }
        return sum / ((double) population.length);
    }

    /**
	 * Get the ranges within this population where any two adjacent members are different by equal or less to the
	 * overall mean growth of the population.
	 * @param population The overall population in which to find the ranges. Sorted ascending
	 * @return Array of ranges
	 */
    public static Range[] findGroupRangesWithLessOrEqualMeanGrowth(final int[] population) {
        final double firstDifferenceMean = getMean(getFirstDifference(population));
        final double maxJump = Math.ceil(firstDifferenceMean);
        final List<Range> ranges = new ArrayList<Range>();
        boolean scanning = true;
        int index = 0;
        while (scanning) {
            final Range range = getNextRangeWithLessOrEqualGrowth(population, index, maxJump);
            if (range != null) {
                index = range.end;
                ranges.add(range);
            } else {
                scanning = false;
            }
        }
        return ranges.toArray(new Range[0]);
    }

    /**
	 * Find the next range of indices on the population where the growth between any two population members is less
	 * or equal to the maxGrowth. Ranges must be 3+ in size.
	 * @param population The overall population, in which to find the range. Sorted ascending
	 * @param startIndex The index in the population to start looking for the range.
	 * @param maxGrowth The maximum amount of difference that can be between two adjacent members in a range.
	 * @return Null if no range found
	 */
    public static Range getNextRangeWithLessOrEqualGrowth(final int[] population, final int startIndex, final double maxGrowth) {
        boolean scanning = false;
        final Range r1 = new Range();
        for (int i = startIndex; i < population.length; i++) {
            if (i == population.length - 1) {
                r1.end = i;
                break;
            }
            if (population[i + 1] - population[i] > maxGrowth) {
                if (scanning) {
                    r1.end = i;
                    break;
                }
            } else if (!scanning) {
                scanning = true;
                r1.start = i;
            }
        }
        if (!scanning || r1.end - r1.start <= 2) {
            return null;
        }
        return r1;
    }

    /**
     * This is simply a convenience so we can take advantage of the "..." syntax.
     * @param numbers Zero or more.
     * @return Value of {@link org.apache.commons.lang.math.NumberUtils#min(int[])}. Null if array is empty.
     */
    public static Integer min(final int... numbers) {
        return ArrayUtils.isEmpty(numbers) ? null : org.apache.commons.lang.math.NumberUtils.min(numbers);
    }

    /**
	 * A start and end position in an array.
	 * @author Reuben Firmin
	 */
    public static class Range {

        int start;

        int end;
    }
}
