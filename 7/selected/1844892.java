package jgrx.iface.impl.stat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import jgrx.iface.impl.terms.AbstractTerm;
import jgrx.iface.impl.terms.ArrayTerm;
import jgrx.iface.impl.terms.StatListTerm;

/**
 *
 * @author Ellery
 */
public class StatList {

    public static StatList toStatList(Object obj) {
        if (obj instanceof StatList) {
            return (StatList) obj;
        } else if (obj instanceof double[]) {
            return new StatList((double[]) obj);
        } else if (obj instanceof ArrayTerm) {
            return new StatList(((ArrayTerm) obj).getArray());
        } else if (obj instanceof StatListTerm) {
            return ((StatListTerm) obj).getList();
        } else return null;
    }

    private double mean;

    private double median;

    private double mode;

    private double stanDev;

    private double[] values;

    private int length;

    /** Creates a new instance of StatList */
    public StatList() {
        values = new double[10];
        mean = -1;
        median = -1;
        mode = -1;
        stanDev = -1;
        length = 0;
    }

    public StatList(double[] array) {
        values = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            values[i] = array[i];
        }
        length = array.length;
    }

    public void sort() {
        mergeSort(0, length - 1, new double[length]);
    }

    private void mergeSort(int s, int l, double[] temp) {
        if (l - s >= 1) {
            mergeSort(s, (l + s) / 2, temp);
            mergeSort((l + s) / 2 + 1, l, temp);
            merge(s, l, temp);
        }
    }

    private void merge(int s, int l, double[] temp) {
        int ss = s;
        int m = (l + s) / 2;
        int ll = m + 1;
        int i = s;
        while (ss <= m && ll <= l) {
            if (values[ss] <= values[ll]) {
                temp[i] = values[ss];
                ss++;
                i++;
            } else {
                temp[i] = values[ll];
                ll++;
                i++;
            }
        }
        while (ss <= m) {
            temp[i] = values[ss];
            i++;
            ss++;
        }
        while (ll <= l) {
            temp[i] = values[ll];
            i++;
            ll++;
        }
        for (i = s; i <= l; i++) {
            values[i] = temp[i];
        }
    }

    private void findDeviation() {
        double diff = 0, sum = 0;
        for (int i = 0; i < length; i++) {
            diff = Math.pow((mean - values[i]), 2);
            sum += diff;
        }
        stanDev = Math.sqrt(sum / (length));
    }

    /**finds mean. updates mean to current mean.
     */
    private void findMean() {
        double sum = 0;
        for (int i = 0; i < length; i++) {
            sum += values[i];
        }
        mean = sum / length;
    }

    /**finds middle value. assumes list is ordered. fix this!
     */
    private void findMedian() {
        double sum = 0;
        for (int i = 0; i < length; i++) sum += i;
        median = values[(int) sum / length];
    }

    /**find most common value. assumes list is ordered. fix this!
     */
    private void findMode() {
        int sum = 0, highsum = 0, i = 0, s = 0;
        boolean diff = false;
        while (i < length) {
            sum = 1;
            while (!diff) {
                if (values[i] != values[i + 1]) diff = true; else sum++;
                i++;
            }
            if (sum > highsum) {
                highsum = sum;
                s = i - 1;
            }
            i++;
        }
        mode = values[s];
    }

    public double get(int index) {
        if (index >= length || index < 0) {
            throw new IndexOutOfBoundsException("index out of bounds");
        }
        return values[index];
    }

    public double set(int index, double value) {
        if (index >= length || index < 0) {
            throw new IndexOutOfBoundsException("index out of bounds");
        }
        double ret = values[index];
        values[index] = value;
        findMean();
        findMedian();
        findMode();
        findDeviation();
        return ret;
    }

    public int size() {
        return length;
    }

    public boolean isEmpty() {
        return length == 0;
    }

    public void add(double value) {
        if (length == values.length) {
            expand(1);
        }
        length++;
        values[length - 1] = value;
        findMean();
        findMedian();
        findMode();
        findDeviation();
    }

    /**inserts value at specified lcoation. not tested. should work.
     */
    public void add(int index, double value) {
        if (index >= length || index < 0) {
            throw new IndexOutOfBoundsException("index out of bounds");
        }
        if (length == values.length) {
            expand(1);
        }
        length++;
        int i = length - 1;
        while (i >= index) {
            values[i + 1] = values[i];
            i--;
        }
        values[index] = value;
        findMean();
        findMedian();
        findMode();
        findDeviation();
    }

    /**insert list of elements at end. not tested, does it work?
     */
    public void addAll(double[] values) {
        if (length <= values.length + this.values.length) {
            expand(values.length);
        }
        for (int i = length; i < length + values.length; i++) {
            this.values[i] = this.values[i - length];
        }
        length += values.length;
        findMean();
        findMedian();
        findMode();
        findDeviation();
    }

    /**inserts list of elements in specified location. not tested, does it work?
     */
    public void addAll(int index, double[] values) {
        if (index >= length || index < 0) {
            throw new IndexOutOfBoundsException("index out of bounds");
        }
        if (length <= values.length + this.values.length) {
            expand(values.length);
        }
        int i = index + values.length - 1;
        while (i >= index) {
            this.values[i + 1] = this.values[i];
            i--;
        }
        for (i = index; i < index + values.length; i++) {
            this.values[i] = this.values[i - length];
        }
        length += values.length;
        findMean();
        findMedian();
        findMode();
        findDeviation();
    }

    /**expands the array to twice the array's previous size, or by min elements,
     *whichever is greater.
     */
    private void expand(int min) {
        double newV[];
        if (values.length > min) {
            newV = new double[values.length * 2 + 1];
        } else {
            newV = new double[values.length + min];
        }
        for (int i = 0; i < values.length; i++) {
            newV[i] = values[i];
        }
        values = newV;
    }

    /**finds first occurrance of value, starting from the first element.
     */
    public boolean contains(double value) {
        int i = 0;
        while (i < length) {
            if (values[i] == value) return true;
            i++;
        }
        return false;
    }

    /**removes value at index. returns value that was at index.
     */
    public double remove(int index) {
        if (index >= length || index < 0) {
            throw new IndexOutOfBoundsException("index out of bounds");
        }
        int i = index;
        double ret = values[i];
        while (i < length - 1) {
            values[i] = values[i + 1];
            i++;
        }
        length--;
        findMean();
        findMedian();
        findMode();
        findDeviation();
        return ret;
    }

    /**removes the first instance of value from this list. returns whether value
     *existed.
     */
    public boolean remove(double value) {
        int index = indexOf(value);
        if (index == -1) {
            return false;
        } else {
            remove(index);
            return true;
        }
    }

    /**removes as many of the elements of c as exist in this list. returns whether
     *any were removed.
     */
    public boolean removeAll(double[] c) {
        HashMap<Double, Integer> table = new HashMap();
        int[] indeces = new int[c.length];
        for (int i = 0; i < c.length; i++) {
            int index;
            if (table.containsKey(c[i])) {
                index = indexOf(c[i], table.get(c[i]) + 1);
            } else {
                index = indexOf(c[i]);
            }
            table.put(c[i], index);
            indeces[i] = index;
        }
        int p = 0, q = 0, i = 0, count = 0;
        while (indeces[i] == -1 && i < length) {
            i++;
        }
        if (i < length) count++;
        p = i;
        q = i + 1;
        while (i < indeces.length) {
            do {
                i++;
            } while (indeces[i] == -1 && i < length);
            if (i < length) count++;
            while (q < i) {
                values[p] = values[q];
                q++;
                p++;
            }
        }
        length -= count;
        findMean();
        findMedian();
        findMode();
        findDeviation();
        return count > 0;
    }

    public boolean retainAll(double[] c) {
        throw new UnsupportedOperationException("LDSKjf:LKDSJF:LDSJF:LDSJ:LDSF");
    }

    public void clear() {
        values = new double[10];
        mean = -1;
        median = -1;
        mode = -1;
        stanDev = -1;
        length = 0;
    }

    public int indexOf(double value) {
        return indexOf(value, 0);
    }

    public int indexOf(double value, int start) {
        int i = start;
        while (i < length) {
            if (values[i] == value) return i;
            i++;
        }
        return -1;
    }

    public int lastIndexOf(double value) {
        int i = length - 1;
        while (i > -1) {
            if (values[i] == value) return i;
            i--;
        }
        return i;
    }

    public double[] toArray() {
        double[] ret = new double[length];
        for (int i = 0; i < length; i++) {
            ret[i] = values[i];
        }
        return ret;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(20);
        sb.append("[ ");
        for (int i = 0; i < length; i++) {
            sb.append(AbstractTerm.formatNumber(values[i]));
            if (i < length - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
}
