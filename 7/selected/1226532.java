package dr.evomodel.coalescent;

import dr.evolution.coalescent.DemographicFunction;
import dr.evolution.coalescent.IntervalType;
import dr.evolution.coalescent.TreeIntervals;
import dr.evolution.tree.Tree;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Joseph Heled
 */
public class VDdemographicFunction extends DemographicFunction.Abstract {

    private double[] values;

    private double[] times;

    private double[] intervals;

    private double[][] ttimes;

    private double[] alltimes;

    private boolean[] dirtyTrees;

    boolean dirty;

    private final VariableDemographicModel.Type type;

    TreeIntervals[] ti;

    public VDdemographicFunction(Tree[] trees, VariableDemographicModel.Type type, double[] indicatorParameter, double[] popSizeParameter, boolean logSpace, boolean mid) {
        super(trees[0].getUnits());
        this.type = type;
        ti = new TreeIntervals[trees.length];
        dirtyTrees = new boolean[trees.length];
        Arrays.fill(dirtyTrees, true);
        ttimes = new double[ti.length][];
        int tot = 0;
        for (int k = 0; k < ti.length; ++k) {
            ttimes[k] = new double[trees[k].getTaxonCount() - 1];
            tot += ttimes[k].length;
        }
        alltimes = new double[tot];
        setDirty();
        assert !(type == VariableDemographicModel.Type.EXPONENTIAL && !logSpace);
        setup(trees, indicatorParameter, popSizeParameter, logSpace, mid);
    }

    /**
     * Reduce memory footprint of object. After a call to freeze only population/intensity
     * are allowed.
     */
    public void freeze() {
        ttimes = null;
        alltimes = null;
        dirtyTrees = null;
        ti = null;
    }

    public VDdemographicFunction(VDdemographicFunction demoFunction) {
        super(demoFunction.getUnits());
        type = demoFunction.type;
        this.ti = demoFunction.ti.clone();
        this.values = demoFunction.values.clone();
        this.times = demoFunction.times.clone();
        this.intervals = demoFunction.intervals.clone();
        this.ttimes = demoFunction.ttimes.clone();
        for (int k = 0; k < ttimes.length; ++k) {
            ttimes[k] = ttimes[k].clone();
        }
        this.alltimes = demoFunction.alltimes.clone();
        this.dirtyTrees = demoFunction.dirtyTrees.clone();
        this.dirty = demoFunction.dirty;
    }

    public VDdemographicFunction(double[] t, double[] p, Type units) {
        this(t, p, units, VariableDemographicModel.Type.LINEAR);
    }

    public VDdemographicFunction(double[] t, double[] p, Type units, VariableDemographicModel.Type type) {
        super(units);
        this.type = type;
        final int tot = p.length;
        times = new double[tot + 1];
        values = p;
        intervals = new double[tot - 1];
        times[0] = 0.0;
        times[tot] = Double.POSITIVE_INFINITY;
        System.arraycopy(t, 0, times, 1, t.length);
        for (int n = 0; n < intervals.length; ++n) {
            intervals[n] = times[n + 1] - times[n];
        }
        dirty = false;
    }

    public int numberOfChanges() {
        return values.length - 1;
    }

    public void treeChanged(int nt) {
        dirtyTrees[nt] = true;
        setDirty();
    }

    public void setDirty() {
        dirty = true;
    }

    private boolean setTreeTimes(int nt, Tree[] trees) {
        if (dirtyTrees[nt]) {
            ti[nt] = new TreeIntervals(trees[nt]);
            TreeIntervals nti = ti[nt];
            nti.setMultifurcationLimit(0);
            final int nLineages = nti.getIntervalCount();
            assert nLineages >= ttimes[nt].length : nLineages + " " + ttimes[nt].length;
            int iCount = 0;
            for (int k = 0; k < ttimes[nt].length; ++k) {
                double timeToCoal = nti.getInterval(iCount);
                while (nti.getIntervalType(iCount) != IntervalType.COALESCENT) {
                    ++iCount;
                    timeToCoal += nti.getInterval(iCount);
                }
                int linAtStart = nti.getLineageCount(iCount);
                ++iCount;
                assert !(iCount == nLineages && linAtStart != 2);
                int linAtEnd = (iCount == nLineages) ? 1 : nti.getLineageCount(iCount);
                while (linAtStart <= linAtEnd) {
                    ++iCount;
                    timeToCoal += nti.getInterval(iCount);
                    linAtStart = linAtEnd;
                    ++iCount;
                    linAtEnd = nti.getLineageCount(iCount);
                }
                ttimes[nt][k] = timeToCoal + (k == 0 ? 0 : ttimes[nt][k - 1]);
            }
            dirtyTrees[nt] = false;
            return true;
        }
        return false;
    }

    void setup(Tree[] trees, double[] indicatorParameter, double[] popSizes, boolean logSpace, boolean mid) {
        if (dirty) {
            if (type == VariableDemographicModel.Type.EXPONENTIAL) logSpace = false;
            boolean any = false;
            for (int nt = 0; nt < ti.length; ++nt) {
                if (setTreeTimes(nt, trees)) {
                    any = true;
                }
            }
            final int nd = indicatorParameter.length;
            assert nd == alltimes.length + (type == VariableDemographicModel.Type.STEPWISE ? -1 : 0) : " nd=" + nd + " alltimes.length=" + alltimes.length + " type=" + type;
            if (any) {
                int[] inds = new int[ttimes.length];
                for (int k = 0; k < alltimes.length; ++k) {
                    int j = 0;
                    while (inds[j] == ttimes[j].length) {
                        ++j;
                    }
                    for (int l = j + 1; l < inds.length; ++l) {
                        if (inds[l] < ttimes[l].length) {
                            if (ttimes[l][inds[l]] < ttimes[j][inds[j]]) {
                                j = l;
                            }
                        }
                    }
                    alltimes[k] = ttimes[j][inds[j]];
                    inds[j]++;
                }
            }
            int tot = 1;
            for (int k = 0; k < nd; ++k) {
                if (indicatorParameter[k] > 0) {
                    ++tot;
                }
            }
            times = new double[tot + 1];
            values = new double[tot];
            intervals = new double[tot - 1];
            times[0] = 0.0;
            times[tot] = Double.POSITIVE_INFINITY;
            final boolean xx = type == VariableDemographicModel.Type.LINEAR && !logSpace && false;
            if (xx) {
                double[] a = alltimes;
                if (mid) {
                    a = new double[alltimes.length];
                    for (int k = 0; k < a.length; ++k) {
                        a[k] = ((alltimes[k] + (k > 0 ? alltimes[k - 1] : 0)) / 2);
                    }
                }
                bestLinearFit(a, popSizes, indicatorParameter, times, values);
                for (int n = 0; n < intervals.length; ++n) {
                    intervals[n] = times[n + 1] - times[n];
                }
                for (int n = 0; n < values.length; ++n) {
                    if (values[n] <= 0) {
                        values[n] = 1e-30;
                    }
                }
            }
            if (!xx) {
                values[0] = logSpace ? Math.exp(popSizes[0]) : popSizes[0];
                int n = 0;
                for (int k = 0; k < nd && n + 1 < tot; ++k) {
                    if (indicatorParameter[k] > 0) {
                        times[n + 1] = mid ? ((alltimes[k] + (k > 0 ? alltimes[k - 1] : 0)) / 2) : alltimes[k];
                        values[n + 1] = logSpace ? Math.exp(popSizes[k + 1]) : popSizes[k + 1];
                        intervals[n] = times[n + 1] - times[n];
                        ++n;
                    }
                }
            }
            dirty = false;
        }
    }

    private int ti2f(int i, int j) {
        return (i == 0) ? j : 2 * i + j + 1;
    }

    private void bestLinearFit(double[] xs, double[] ys, double[] use, double[] ot, double[] oz) {
        assert (xs.length + 1) == ys.length;
        assert ys.length == use.length + 2 || ys.length == use.length + 1;
        int N = ys.length;
        if (N == 2) {
            assert xs.length == ot.length;
            assert ys.length == oz.length;
            System.arraycopy(xs, 0, ot, 0, xs.length);
            System.arraycopy(ys, 0, oz, 0, ys.length);
        }
        List<Integer> iv = new ArrayList<Integer>(2);
        iv.add(0);
        for (int k = 0; k < N - 1; ++k) {
            if (use[k] > 0) {
                iv.add(k + 1);
            }
        }
        double[] ati = new double[xs.length + 1];
        ati[0] = 0.0;
        System.arraycopy(xs, 0, ati, 1, xs.length);
        int n = iv.size();
        double[] a = new double[3 * n];
        double[] v = new double[n];
        for (int k = 0; k < n - 1; ++k) {
            int i0 = iv.get(k);
            int i1 = iv.get(k + 1);
            double u0 = ati[i0];
            double u1 = ati[i1] - ati[i0];
            final int l = ti2f(k, k);
            final int l1 = ti2f(k + 1, k);
            for (int j = i0; j < i1; ++j) {
                double t = ati[j];
                double y = ys[j];
                double z = (t - u0) / u1;
                v[k] += y * (1 - z);
                a[l] += (1 - z) * (1 - z);
                a[l + 1] += z * (1 - z);
                a[l1] += z * (1 - z);
                a[l1 + 1] += z * z;
                v[k + 1] += y * z;
            }
        }
        {
            int k = n - 1;
            int i0 = iv.get(k);
            int i1 = ys.length;
            final int l = ti2f(k, k);
            for (int j = i0; j < i1; ++j) {
                a[l] += 1;
                v[k] += ys[j];
            }
        }
        for (int k = 0; k < n - 1; ++k) {
            final double r = a[ti2f(k + 1, k)] / a[ti2f(k, k)];
            for (int j = k; j < k + 3; ++j) {
                a[ti2f((k + 1), j)] -= a[ti2f(k, j)] * r;
            }
            v[k + 1] -= v[k] * r;
        }
        if (oz.length != n) {
            n = 3;
        }
        assert oz.length == n;
        for (int k = n - 1; k > 0; --k) {
            oz[k] = v[k] / a[ti2f(k, k)];
            v[k - 1] -= a[ti2f((k - 1), k)] * oz[k];
        }
        oz[0] = v[0] / a[ti2f(0, 0)];
        assert ot.length - 2 == iv.size() - 1;
        for (int j = 1; j < ot.length - 1; ++j) {
            ot[j] = ati[iv.get(j)];
        }
    }

    private int getIntervalIndexStep(final double t) {
        int j = 0;
        final float tf = (float) t;
        while (tf > (float) times[j + 1]) ++j;
        return j;
    }

    private int getIntervalIndexLin(final double t) {
        int j = 0;
        while (t > times[j + 1]) ++j;
        return j;
    }

    private double linPop(double t) {
        final int j = getIntervalIndexLin(t);
        if (j == values.length - 1) {
            return values[j];
        }
        final double a = (t - times[j]) / (intervals[j]);
        return a * values[j + 1] + (1 - a) * values[j];
    }

    public double getDemographic(double t) {
        double p;
        switch(type) {
            case STEPWISE:
                {
                    final int j = getIntervalIndexStep(t);
                    p = values[j];
                    break;
                }
            case LINEAR:
                {
                    p = linPop(t);
                    break;
                }
            case EXPONENTIAL:
                {
                    p = Math.exp(linPop(t));
                    break;
                }
            default:
                throw new IllegalArgumentException("");
        }
        return p;
    }

    public double getIntensity(double t) {
        return getIntegral(0, t);
    }

    public double getInverseIntensity(double x) {
        assert false;
        return 0;
    }

    private double intensityLinInterval(double start, double end, int index) {
        final double dx = end - start;
        if (dx == 0) {
            return 0;
        }
        final double popStart = values[index];
        final double popDiff = (index < values.length - 1) ? values[index + 1] - popStart : 0.0;
        if (popDiff == 0.0) {
            return dx / popStart;
        }
        final double time0 = times[index];
        final double interval = intervals[index];
        assert (float) start <= (float) (time0 + interval) && start >= time0 && (float) end <= (float) (time0 + interval) && end >= time0;
        final double p1minusp0 = ((end - start) / interval) * popDiff;
        final double v = interval * (popStart / popDiff);
        final double p1overp0 = (v + (end - time0)) / (v + (start - time0));
        if (p1minusp0 == 0.0 || p1overp0 <= 0) {
            final double pop0 = popStart + ((start - time0) / interval) * popDiff;
            return dx / pop0;
        }
        return dx * Math.log(p1overp0) / p1minusp0;
    }

    private double intensityLinInterval(int index) {
        final double interval = intervals[index];
        final double pop0 = values[index];
        final double pop1 = values[index + 1];
        if (pop0 == pop1) {
            return interval / pop0;
        }
        return interval * Math.log(pop1 / pop0) / (pop1 - pop0);
    }

    private double intensityExpInterval(double start, double end, int index) {
        final double pop0 = values[index];
        if (index == intervals.length) {
            return (end - start) / Math.exp(pop0);
        }
        final double interval = intervals[index];
        final double pop1 = values[index + 1];
        final double time0 = times[index];
        assert start >= time0 && (float) start <= (float) (time0 + interval) && (float) end <= (float) (time0 + interval) && end >= time0;
        final double a = (pop0 - pop1) / interval;
        if (a == 0) {
            return (end - start) / Math.exp(pop0);
        }
        return (Math.exp((end - time0) * a - pop0) - Math.exp((start - time0) * a - pop0)) / a;
    }

    private double intensityExpInterval(int index) {
        final double interval = intervals[index];
        final double pop0 = values[index];
        final double pop1 = values[index + 1];
        final double a = (pop0 - pop1) / interval;
        if (a == 0) {
            return interval / Math.exp(pop0);
        }
        return Math.exp(-pop0) / a * (Math.exp(interval * a) - 1);
    }

    public double getIntegral(double start, double finish) {
        double intensity = 0.0;
        switch(type) {
            case STEPWISE:
                {
                    final int first = getIntervalIndexStep(start);
                    final int last = getIntervalIndexStep(finish);
                    final double popStart = values[first];
                    if (first == last) {
                        intensity = (finish - start) / popStart;
                    } else {
                        intensity = (times[first + 1] - start) / popStart;
                        for (int k = first + 1; k < last; ++k) {
                            intensity += intervals[k] / values[k];
                        }
                        intensity += (finish - times[last]) / values[last];
                    }
                    break;
                }
            case LINEAR:
                {
                    final int first = getIntervalIndexLin(start);
                    final int last = getIntervalIndexLin(finish);
                    if (first == last) {
                        intensity += intensityLinInterval(start, finish, first);
                    } else {
                        intensity += intensityLinInterval(start, times[first + 1], first);
                        for (int k = first + 1; k < last; ++k) {
                            intensity += intensityLinInterval(k);
                        }
                        intensity += intensityLinInterval(times[last], finish, last);
                    }
                    break;
                }
            case EXPONENTIAL:
                {
                    final int first = getIntervalIndexLin(start);
                    final int last = getIntervalIndexLin(finish);
                    if (first == last) {
                        intensity += intensityExpInterval(start, finish, first);
                    } else {
                        intensity += intensityExpInterval(start, times[first + 1], first);
                        for (int k = first + 1; k < last; ++k) {
                            intensity += intensityExpInterval(k);
                        }
                        intensity += intensityExpInterval(times[last], finish, last);
                    }
                    break;
                }
        }
        return intensity;
    }

    public int getNumArguments() {
        assert false;
        return 0;
    }

    public String getArgumentName(int n) {
        assert false;
        return null;
    }

    public double getArgument(int n) {
        assert false;
        return 0;
    }

    public void setArgument(int n, double value) {
        assert false;
    }

    public double getLowerBound(int n) {
        return 0.0;
    }

    public double getUpperBound(int n) {
        return Double.POSITIVE_INFINITY;
    }

    public DemographicFunction getCopy() {
        return null;
    }

    public double getThreshold() {
        return 1E-12;
    }

    public double value(double x) {
        return 1.0 / getDemographic(x);
    }

    public TreeIntervals getTreeIntervals(int nt) {
        return ti[nt];
    }

    public double[] allTimePoints() {
        return alltimes;
    }

    public double[] times() {
        double[] valuesCopy = new double[times.length - 2];
        System.arraycopy(times, 1, valuesCopy, 0, times.length - 2);
        return valuesCopy;
    }

    /**
     * @return population values transformed depending on type (i.e. exp(value) for Type.EXPONENTIAL)
     */
    public double[] values() {
        double[] valuesCopy = new double[values.length];
        if (type == VariableDemographicModel.Type.EXPONENTIAL) {
            for (int i = 0; i < values.length; i++) {
                valuesCopy[i] = Math.exp(values[i]);
            }
        } else {
            System.arraycopy(values, 0, valuesCopy, 0, values.length);
        }
        return valuesCopy;
    }

    public double naturalLimit() {
        return times[times.length - 2];
    }
}
