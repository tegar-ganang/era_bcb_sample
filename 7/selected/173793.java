package net.sourceforge.jfl.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.sourceforge.jfl.JFLConfiguration;
import net.sourceforge.jfl.core.interval.Interval;
import net.sourceforge.jfl.core.operators.IBinaryOperator;

/**
 * This class implements the <code>IFuzzySet</code> interface over <code>Double</code> type members.
 * This implementation not allows <code>null</code> element.<br>
 * <br>
 * It implements the set backed by and array. The membership function is a linear interpolation 
 * of elements into the set.<br>
 * <br>
 * Add an element into the set means add a point to the interpolated membership function.<br>
 * <br>
 * Call m the min element present into the set, all x < m has a membership value = u(m).<br>
 * Call M the max element present into the set, all x > M has a membership value = u(M).<br>
 * 
 * @author arons777@users.sourceforge.net
 *
 */
public class DoubleInterpolatedFuzzySet implements IFuzzySet<Double> {

    protected static final int DEFAULT_ARRAY_INCREMENT = 3;

    protected String label;

    protected double[] support;

    protected double[] membership;

    protected int size;

    protected boolean complementary;

    public DoubleInterpolatedFuzzySet() {
        this(3);
    }

    /**
     * Initialize the set with the passed capacity.
     * @param initialCapacity
     */
    public DoubleInterpolatedFuzzySet(int initialCapacity) {
        complementary = false;
        size = 0;
        support = new double[initialCapacity];
        membership = new double[initialCapacity];
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void addAllElement(Set<Double> elements, double membershipValue) {
        if (elements == null) return;
        for (Double element : elements) {
            insert(element, membershipValue);
        }
    }

    public void addElement(Double element, double membershipValue) {
        if (element == null || element.isNaN()) return;
        double value = complementary ? 1.0 - membershipValue : membershipValue;
        insert(element, value);
    }

    public double removeElement(Double element) {
        if (element == null) return 0.0;
        return remove(element);
    }

    public double getMembership(Double element) {
        if (element == null) return 0.0;
        double result = membership(element);
        return complementary ? 1.0 - result : result;
    }

    public Set<Double> getSupport() {
        return null;
    }

    public Set<Interval> getSupportIntervals() {
        Set<Interval> result = new HashSet<Interval>();
        Interval interval = null;
        double memb = getMembership(Double.NEGATIVE_INFINITY);
        if (memb > 0.0) {
            interval = new Interval();
            interval.setMin(Double.NEGATIVE_INFINITY);
            interval.setMinClose(false);
        }
        for (int i = 0; i < size; i++) {
            memb = getMembership(support[i]);
            if (interval != null) {
                if (memb <= 0.0) {
                    interval.setMax(support[i]);
                    interval.setMaxClose(false);
                    result.add(interval);
                    interval = null;
                    if (i + 1 < size && getMembership(support[i + 1]) > 0.0) {
                        interval = new Interval();
                        interval.setMin(support[i]);
                        interval.setMinClose(false);
                    }
                }
            } else {
                if (memb <= 0.0 && i + 1 < size && getMembership(support[i + 1]) > 0.0) {
                    interval = new Interval();
                    interval.setMin(support[i]);
                    interval.setMinClose(false);
                }
            }
        }
        if (interval != null) {
            interval.setMax(Double.POSITIVE_INFINITY);
            interval.setMaxClose(false);
            result.add(interval);
            interval = null;
        }
        return result;
    }

    public DoubleInterpolatedFuzzySet complement() {
        DoubleInterpolatedFuzzySet result = new DoubleInterpolatedFuzzySet(this.size);
        result.complementary = !this.complementary;
        System.arraycopy(this.support, 0, result.support, 0, this.size);
        System.arraycopy(this.membership, 0, result.membership, 0, this.size);
        result.size = this.size;
        return result;
    }

    /**
     * Retrieve membership 
     * @param element
     * @return
     */
    protected double membership(double element) {
        if (size == 0) return 0.0;
        int index = insertionPoint(element);
        if (index >= 0 && element == support[index]) return membership[index];
        if (index < 0) return membership[0];
        if (index >= size - 1) return membership[size - 1];
        double A = support[index];
        double B = support[index + 1];
        double mA = membership[index];
        double mB = membership[index + 1];
        if (A == Double.NEGATIVE_INFINITY) return mB;
        if (B == Double.POSITIVE_INFINITY) return mA;
        double result = ((mB - mA) / (B - A)) * (element - A) + mA;
        return result;
    }

    /**
     * Return index of the first element less than passed.
     * Use binary search.
     * @param d 
     * @return index, -1 if all elements are greather of the one passet.
     * 
     */
    protected int insertionPoint(double key) {
        if (size == 0) return -1;
        int low = 0;
        int high = size - 1;
        while (low <= high) {
            int mid = (low + high) >> 1;
            double midVal = support[mid];
            int cmp;
            if (midVal < key) {
                cmp = -1;
            } else if (midVal > key) {
                cmp = 1;
            } else {
                long midBits = Double.doubleToLongBits(midVal);
                long keyBits = Double.doubleToLongBits(key);
                cmp = (midBits == keyBits ? 0 : (midBits < keyBits ? -1 : 1));
            }
            if (cmp < 0) low = mid + 1; else if (cmp > 0) high = mid - 1; else return mid;
        }
        return low - 1;
    }

    /**
     * Insert the passed element with the passed memebership value.
     * @param element
     * @param membership
     */
    protected void insert(double element, double membershipValue) {
        int index = insertionPoint(element);
        insert(element, membershipValue, index);
    }

    /**
     * 
     * @param element
     * @param membership
     * @param index index of element after insert the new one.
     */
    protected void insert(double element, double membershipValue, int index) {
        if (index > 0 && support[index] == element) {
            membership[index] = membershipValue;
            return;
        }
        if (size == support.length) extend();
        for (int i = size - 1; i > index; i--) {
            support[i + 1] = support[i];
            membership[i + 1] = membership[i];
        }
        support[index + 1] = element;
        membership[index + 1] = membershipValue;
        size++;
    }

    /**
     * Remove a point of interpolation from the set.
     * @param element to be removed
     * @return previous membership value.
     */
    protected double remove(double element) {
        int index = insertionPoint(element);
        double value = getMembership(element);
        if ((index > 0 && support[index] == element) || (index <= 0 && support[0] == element)) {
            for (int i = index; i < size - 1; i++) {
                support[i] = support[i + 1];
                membership[i] = membership[i + 1];
            }
            size--;
        }
        return value;
    }

    /**
     * Extend the array by default value.
     */
    protected void extend() {
        extend(DEFAULT_ARRAY_INCREMENT);
    }

    /**
     * Extend the array by passed value.
     * @param incrementSize increment size
     */
    protected void extend(int incrementSize) {
        double[] newSupport = new double[support.length + incrementSize];
        double[] newMembership = new double[membership.length + incrementSize];
        System.arraycopy(support, 0, newSupport, 0, support.length);
        System.arraycopy(membership, 0, newMembership, 0, membership.length);
        support = newSupport;
        membership = newMembership;
    }

    public DoubleInterpolatedFuzzySet and(IFuzzySet<Double> anotherSet) {
        if (anotherSet == null) return null;
        DoubleInterpolatedFuzzySet castedSet = (DoubleInterpolatedFuzzySet) anotherSet;
        return computeBinaryOperatorOverSets(this, castedSet, JFLConfiguration.getAnd());
    }

    public DoubleInterpolatedFuzzySet or(IFuzzySet<Double> anotherSet) {
        if (anotherSet == null) return null;
        DoubleInterpolatedFuzzySet castedSet = (DoubleInterpolatedFuzzySet) anotherSet;
        return computeBinaryOperatorOverSets(this, castedSet, JFLConfiguration.getOr());
    }

    /**
     * Compute a IBinaryOperator passed over the two DoubleInterpolatedFuzzySet.
     * @param one
     * @param two
     * @param operator
     * @return
     */
    private DoubleInterpolatedFuzzySet computeBinaryOperatorOverSets(DoubleInterpolatedFuzzySet one, DoubleInterpolatedFuzzySet two, IBinaryOperator operator) {
        DoubleInterpolatedFuzzySet result = new DoubleInterpolatedFuzzySet();
        double res = operator.compute(one.getMembership(Double.NEGATIVE_INFINITY), two.getMembership(Double.NEGATIVE_INFINITY));
        result.addElement(Double.NEGATIVE_INFINITY, res);
        for (double d : one.support) {
            res = operator.compute(one.getMembership(d), two.getMembership(d));
            result.addElement(d, res);
        }
        for (double d : two.support) {
            res = operator.compute(one.getMembership(d), two.getMembership(d));
            result.addElement(d, res);
        }
        res = operator.compute(one.getMembership(Double.POSITIVE_INFINITY), two.getMembership(Double.POSITIVE_INFINITY));
        result.addElement(Double.POSITIVE_INFINITY, res);
        return result;
    }
}
