package jp.go.ipa.jgcl;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;

/**
 * Knot vector data
 *
 * @author Information-technology Promotion Agency, Japan
 */
public class BsplineKnotVector extends java.lang.Object implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
	 * Degree
	 */
    private int degree;

    /**
	 * Number of control points
	 */
    private int nControlPoints;

    /**
	 * Type specification (KnotType)
	 */
    private KnotType knotSpec;

    /**
	 * Periodic flag
	 */
    private boolean periodic;

    /**
	 * Array of multiplicities
	 */
    private int[] knotMultiplicities;

    /**
	 * Array of knots
	 */
    private double[] knots;

    /**
	 * Parameter space continuity
	 */
    private int continuity;

    /**
	 * Constructor
	 *
	 * @param degree
	 * @param knotSpec 
	 * @param periodic
	 * @param knots
	 * @param knotMultiplicities
	 * @param nControlPoints
	 */
    public BsplineKnotVector(int degree, KnotType knotSpec, boolean periodic, int[] knotMultiplicities, double[] knots, int nControlPoints) {
        this(degree, knotSpec, periodic, knotMultiplicities, knots, nControlPoints, true);
    }

    /**
	 * Constructor
	 */
    BsplineKnotVector(int degree, KnotType knotSpec, boolean periodic, int[] knotMultiplicities, double[] knots, int nControlPoints, boolean doCheck) {
        super();
        setData(degree, knotSpec, periodic, knotMultiplicities, knots, nControlPoints, doCheck);
    }

    /**
	 * Constructor
	 */
    BsplineKnotVector(int degree, KnotType knotSpec, boolean periodic, int nKnots, int[] knotMultiplicities, double[] knots, int nControlPoints) {
        this(degree, knotSpec, periodic, nKnots, knotMultiplicities, knots, nControlPoints, true);
    }

    /**
	 * Constructor
	 */
    BsplineKnotVector(int degree, KnotType knotSpec, boolean periodic, int nKnots, int[] knotMultiplicities, double[] knots, int nControlPoints, boolean doCheck) {
        super();
        int[] new_multi = new int[nKnots];
        double[] new_knots = new double[nKnots];
        if (knots.length < nKnots || knotMultiplicities.length < nKnots) {
            throw new ExceptionGeometryInvalidArgumentValue();
        }
        for (int i = 0; i < nKnots; i++) {
            new_multi[i] = knotMultiplicities[i];
            new_knots[i] = knots[i];
        }
        setData(degree, knotSpec, periodic, new_multi, new_knots, nControlPoints, doCheck);
    }

    /**
	 * Initialize data
	 */
    private void setData(int degree, KnotType knotSpec, boolean periodic, int[] knotMultiplicities, double[] knots, int nControlPoints, boolean doCheck) {
        ConditionOfOperation condition = ConditionOfOperation.getCondition();
        double p_tol = condition.getToleranceForParameter();
        this.degree = degree;
        this.nControlPoints = nControlPoints;
        this.knotSpec = knotSpec;
        this.periodic = periodic;
        this.continuity = -1;
        if (!doCheck) {
            if (knotSpec != KnotType.UNSPECIFIED) {
                this.knotMultiplicities = null;
                this.knots = null;
            } else {
                int uik = knotMultiplicities.length;
                this.knotMultiplicities = new int[uik];
                this.knots = new double[uik];
                for (int i = 0; i < uik; i++) {
                    this.knotMultiplicities[i] = knotMultiplicities[i];
                    this.knots[i] = knots[i];
                }
            }
            return;
        }
        if (degree < 1) {
            throw new ExceptionGeometryInvalidArgumentValue();
        }
        switch(knotSpec) {
            case UNIFORM_KNOTS:
                if (knotMultiplicities != null) {
                    throw new ExceptionGeometryInvalidArgumentValue();
                }
                if (knots != null) {
                    throw new ExceptionGeometryInvalidArgumentValue();
                }
                this.knotMultiplicities = null;
                this.knots = null;
                this.continuity = degree - 1;
                break;
            case UNSPECIFIED:
                {
                    int uik;
                    int sum;
                    int mx = 0;
                    if (knotMultiplicities == null) {
                        throw new ExceptionGeometryInvalidArgumentValue();
                    }
                    if ((uik = knotMultiplicities.length) < 1) {
                        throw new ExceptionGeometryInvalidArgumentValue();
                    }
                    if (knots == null) {
                        throw new ExceptionGeometryInvalidArgumentValue();
                    }
                    if (knots.length != uik) {
                        throw new ExceptionGeometryInvalidArgumentValue();
                    }
                    this.knotMultiplicities = new int[uik];
                    this.knots = new double[uik];
                    sum = 0;
                    for (int i = 0; i < uik; i++) {
                        this.knotMultiplicities[i] = knotMultiplicities[i];
                        this.knots[i] = knots[i];
                        sum += knotMultiplicities[i];
                        if (i > 0 && knots[i - 1] > knots[i]) {
                            throw new ExceptionGeometryInvalidArgumentValue();
                        }
                        if (knotMultiplicities[i] > degree + 1) {
                            throw new ExceptionGeometryInvalidArgumentValue();
                        }
                        if (knotMultiplicities[i] > mx) mx = knotMultiplicities[i];
                    }
                    this.continuity = degree - mx;
                    if ((periodic && 2 * degree + nControlPoints + 1 != sum) || (!periodic && degree + nControlPoints + 1 != sum)) {
                        throw new ExceptionGeometryInvalidArgumentValue();
                    }
                    if (periodic) {
                        double h_intvl, h_start, h_end;
                        double t_intvl, t_start, t_end;
                        int n2 = 2 * degree;
                        int i, j;
                        h_end = knotValueAt(i = 0);
                        t_end = knotValueAt(j = nControlPoints);
                        while (i < n2) {
                            h_start = h_end;
                            t_start = t_end;
                            h_intvl = (h_end = knotValueAt(i++)) - h_start;
                            t_intvl = (t_end = knotValueAt(j++)) - t_start;
                            if (Math.abs(h_intvl - t_intvl) > p_tol) {
                                throw new ExceptionGeometryInvalidArgumentValue();
                            }
                        }
                    }
                }
                break;
            default:
                throw new ExceptionGeometryInvalidArgumentValue();
        }
    }

    /**
	 * Returns degree
	 *
	 * @return
	 */
    public int degree() {
        return degree;
    }

    /**
	 * Returns number of control points
	 *
	 * @return
	 */
    public int nControlPoints() {
        return nControlPoints;
    }

    /**
	 * Returns knot specification
	 *
	 * @return
	 */
    public KnotType knotSpec() {
        return knotSpec;
    }

    /**
	 * Returns number of knots
	 *
	 * @return
	 */
    public int nKnots() {
        if (knotSpec() != KnotType.UNSPECIFIED) {
            throw new ExceptionGeometryFatal();
        }
        return knots.length;
    }

    /**
	 * Returns copy of knot multiplicities array
	 *
	 * @return
	 */
    public int[] knotMultiplicities() {
        if (knotSpec() != KnotType.UNSPECIFIED) {
            throw new ExceptionGeometryFatal();
        }
        return knotMultiplicities.clone();
    }

    /**
	 * Returns copy of knot array
	 *
	 * @return
	 */
    public double[] knots() {
        if (knotSpec() != KnotType.UNSPECIFIED) {
            throw new ExceptionGeometryFatal();
        }
        return knots.clone();
    }

    /**
	 * Returns knot multiplicity at i
	 *
	 * @param i
	 * @return
	 */
    public int knotMultiplicityAt(int i) {
        if (knotSpec() != KnotType.UNSPECIFIED) {
            throw new ExceptionGeometryFatal();
        }
        return knotMultiplicities[i];
    }

    /**
	 * Returns knot multiplicity at knot
	 *
	 * @param knot
	 * @return
	 */
    public int knotMultiplicityAt(double knot) {
        double pTol = ConditionOfOperation.getCondition().getToleranceForParameter();
        if (this.knotSpec() != KnotType.UNSPECIFIED) {
            double lower = Math.floor(knot);
            double upper = Math.ceil(knot);
            double lowerLimit = 0 - this.degree();
            double upperLimit = (this.nKnotValues() - 1) - this.degree();
            if ((knot - lower) < pTol) {
                if ((lowerLimit <= lower) && (lower <= upperLimit)) {
                    return 1;
                }
            } else {
                if ((upper - knot) < pTol) {
                    if ((lowerLimit <= upper) && (upper <= upperLimit)) {
                        return 1;
                    }
                }
            }
        } else {
            for (int i = 0; i < this.nKnots(); i++) {
                double ithKnot = this.knotAt(i);
                if (Math.abs(knot - ithKnot) < pTol) {
                    return this.knotMultiplicityAt(i);
                }
                if (knot < ithKnot) {
                    return 0;
                }
            }
        }
        return 0;
    }

    /**
	 * Returns knot at i
	 *
	 * @param i
	 * @return
	 */
    public double knotAt(int i) {
        if (knotSpec() != KnotType.UNSPECIFIED) {
            throw new ExceptionGeometryFatal();
        }
        return knots[i];
    }

    /**
	 * Returns true if periocidc (closed)
	 *
	 * @return
	 */
    public boolean isPeriodic() {
        return periodic;
    }

    /**
	 * Returns true if not periodic (open)
	 *
	 * @return
	 */
    public boolean isNonPeriodic() {
        return !periodic;
    }

    /**
	 * Returns parameter domain for knots
	 */
    ParameterDomain getParameterDomain() {
        double start, increase;
        start = knotValueAt(degree);
        increase = knotValueAt(degree + nSegments()) - start;
        try {
            return new ParameterDomain(periodic, start, increase);
        } catch (ExceptionGeometryInvalidArgumentValue e) {
            throw new ExceptionGeometryFatal();
        }
    }

    /**
	 * Returns parametric continuity 0=C0 1=C1 and so on.
	 * @return
	 */
    public int continuity() {
        return continuity;
    }

    /**
	 * Return number of segments (number of control points - degree or number of control points for periodic)
	 *
	 * @return
	 */
    public int nSegments() {
        if (periodic) {
            return nControlPoints;
        } else {
            return nControlPoints - degree;
        }
    }

    /**
	 * Returns number of knot values (2*degree + segments + 1)
	 *
	 * @return
	 */
    public int nKnotValues() {
        return 2 * degree + nSegments() + 1;
    }

    /**
	 * Returns knot value
	 */
    static double knotValueAt(int[] knotMultiplicities, double[] knots, int n) {
        int sum = 0;
        int i = 0;
        while (n >= sum) {
            sum += knotMultiplicities[i++];
        }
        return (knots[i - 1]);
    }

    /**
	 * Returns knot value
	 *
	 * @param n
	 * @return
	 */
    public double knotValueAt(int n) {
        if (knotSpec == KnotType.UNIFORM_KNOTS) {
            return (double) (n - degree);
        } else {
            return BsplineKnotVector.knotValueAt(knotMultiplicities, knots, n);
        }
    }

    /**
	 * Returns segment index for knot param
	 *
	 * @param param
	 * @return
	 */
    public int segmentIndex(double param) {
        ConditionOfOperation condition = ConditionOfOperation.getCondition();
        double p_tol = condition.getToleranceForParameter();
        int r;
        int sum;
        int i;
        r = nSegments();
        if (Math.abs(knotValueAt(degree + r) - param) < p_tol) {
            while ((--r >= 0) && (Math.abs(knotValueAt(degree + r) - param) < p_tol)) ;
            return r;
        }
        if (knotSpec == KnotType.UNIFORM_KNOTS) {
            return ((int) param);
        }
        sum = i = 0;
        while (!(param < knots[i])) {
            sum += knotMultiplicities[i++];
            if (i >= knots.length) {
                if (param > (knots[--i] + p_tol)) {
                    return (-1);
                }
                param = knots[i];
                while (!(param > knots[i])) {
                    sum -= knotMultiplicities[i--];
                }
                break;
            }
        }
        r = sum - degree - 1;
        if (r >= 0) {
            return (r);
        } else {
            return (-1);
        }
    }

    /**
	 * Returns valid segments
	 */
    ValidSegmentInfo validSegments() {
        int nseg_p1;
        int[] sn;
        double[] kp;
        int nvseg;
        double kval, pval;
        int i, k;
        double tol_p = ConditionOfOperation.getCondition().getToleranceForParameter();
        nseg_p1 = nSegments() + 1;
        sn = new int[nseg_p1];
        kp = new double[nseg_p1];
        nvseg = (-1);
        i = degree;
        pval = (knotValueAt(i) - 1.0);
        for (k = 0; k < nseg_p1; k++) {
            if (((kval = knotValueAt(i)) - pval) > tol_p) {
                sn[++nvseg] = i - degree;
                kp[nvseg] = kval;
            } else {
                sn[nvseg] = i - degree;
            }
            i++;
            pval = kval;
        }
        return new ValidSegmentInfo(nvseg, sn, kp);
    }

    class ValidSegmentInfo {

        private int[] segmentNumber;

        private double[] knotPoint;

        ValidSegmentInfo(int nSegments, int[] segmentNumber, double[] knotPoint) {
            this.segmentNumber = new int[nSegments];
            this.knotPoint = new double[nSegments + 1];
            for (int i = 0; i < nSegments; i++) {
                this.segmentNumber[i] = segmentNumber[i];
                this.knotPoint[i] = knotPoint[i];
            }
            this.knotPoint[nSegments] = knotPoint[nSegments];
        }

        int nSegments() {
            return segmentNumber.length;
        }

        int segmentNumber(int n) {
            return segmentNumber[n];
        }

        double[] knotPoint(int n) {
            double[] prms = new double[2];
            prms[0] = knotPoint[n];
            prms[1] = knotPoint[n + 1];
            return prms;
        }

        double headKnotPoint(int n) {
            return knotPoint[n];
        }

        double tailKnotPoint(int n) {
            return knotPoint[n + 1];
        }

        int segmentIndex(double param) {
            double tol_p = ConditionOfOperation.getCondition().getToleranceForParameter();
            int nseg;
            int i;
            if (param < knotPoint[0]) {
                return -1;
            }
            nseg = nSegments();
            for (i = 0; i < nseg; i++) {
                if (param < knotPoint[i + 1]) {
                    return i;
                }
            }
            if (Math.abs(param - knotPoint[nseg]) < tol_p) {
                return (nseg - 1);
            }
            return -1;
        }

        int isValidSegment(int seg) {
            int klm;
            int nvseg = nSegments();
            for (klm = 0; klm < nvseg; klm++) {
                if (segmentNumber(klm) == seg) {
                    return klm;
                }
            }
            return -1;
        }

        double l2Gw(int index, double local) {
            double[] knots = knotPoint(index);
            return (knots[1] - knots[0]) * local;
        }

        double l2Gp(int index, double local) {
            double[] knots = knotPoint(index);
            return knots[0] + (knots[1] - knots[0]) * local;
        }

        int getSegmentNumberThatStartIsEqualTo(double param) {
            double pTol = ConditionOfOperation.getCondition().getToleranceForParameter();
            for (int i = 0; i <= this.segmentNumber.length; i++) {
                if (Math.abs(param - this.knotPoint[i]) < pTol) {
                    return segmentNumber[i];
                }
            }
            return (-1);
        }
    }

    int getSegmentNumberThatStartIsEqualTo(double param) {
        double pTol = ConditionOfOperation.getCondition().getToleranceForParameter();
        for (int i = (this.degree + this.nSegments()); i >= this.degree; i--) {
            if (Math.abs(param - this.knotValueAt(i)) < pTol) {
                return (i - this.degree);
            }
        }
        return (-1);
    }

    /**
	 * Create closed knot array
	 */
    BsplineKnotVector makeKnotsClosed() {
        int n_kel = this.nKnotValues();
        int lower_idx = this.degree();
        int upper_idx = this.degree() + this.nSegments();
        double intvl;
        int i, j, k;
        double[] simple_knots = new double[n_kel];
        int[] simple_knot_multi = new int[n_kel];
        for (i = 0; i < n_kel; i++) {
            simple_knots[i] = this.knotValueAt(i);
            simple_knot_multi[i] = 1;
        }
        j = lower_idx;
        k = upper_idx;
        for (i = 0; i < this.degree(); i++) {
            intvl = simple_knots[j + 1] - simple_knots[j];
            simple_knots[k + 1] = simple_knots[k] + intvl;
            j++;
            k++;
        }
        j = lower_idx;
        k = upper_idx;
        for (i = 0; i < this.degree(); i++) {
            intvl = simple_knots[k] - simple_knots[k - 1];
            simple_knots[j - 1] = simple_knots[j] - intvl;
            j--;
            k--;
        }
        int uik = BsplineKnotVector.beautify(n_kel, simple_knots, simple_knot_multi);
        return new BsplineKnotVector(this.degree(), KnotType.UNSPECIFIED, this.isPeriodic(), uik, simple_knot_multi, simple_knots, this.nControlPoints());
    }

    static int beautify(int uik, double[] knots, int[] knotMultiplicities) {
        double kval = knots[0] - 1.0;
        double p_tol = ConditionOfOperation.getCondition().getToleranceForParameter();
        int j = 0;
        for (int i = 0; i < uik; i++) {
            if (Math.abs(kval - knots[i]) > p_tol) {
                kval = knots[j] = knots[i];
                knotMultiplicities[j] = knotMultiplicities[i];
                j++;
            } else {
                knotMultiplicities[j - 1] += knotMultiplicities[i];
            }
        }
        return j;
    }

    /**
	 *
	 * @return
	 */
    public BsplineKnotVector beautify() {
        if (knotSpec != KnotType.UNSPECIFIED) {
            return this;
        }
        int uik = nKnots();
        double[] new_knots = knots();
        int[] new_knot_multi = knotMultiplicities();
        int n_new_knots = BsplineKnotVector.beautify(uik, new_knots, new_knot_multi);
        try {
            return new BsplineKnotVector(degree, knotSpec, periodic, n_new_knots, new_knot_multi, new_knots, nControlPoints);
        } catch (ExceptionGeometryInvalidArgumentValue e) {
            throw new ExceptionGeometryFatal();
        }
    }

    /**
	 * Returns reverse knot array
	 *
	 * @return
	 */
    public BsplineKnotVector reverse() {
        if (knotSpec != KnotType.UNSPECIFIED) {
            return this;
        }
        int n_kel = nKnotValues();
        int[] new_multi = new int[n_kel];
        double[] new_knots = new double[n_kel];
        int i, j;
        new_multi[degree] = 1;
        new_knots[degree] = 0.0;
        int lk_idx = degree + nSegments();
        for (i = degree - 1, j = lk_idx; i >= 0; i--, j++) {
            new_knots[i] = new_knots[i + 1] - (knotValueAt(j + 1) - knotValueAt(j));
            new_multi[i] = 1;
        }
        for (i = degree + 1, j = lk_idx; i < n_kel; i++, j--) {
            new_knots[i] = new_knots[i - 1] + (knotValueAt(j) - knotValueAt(j - 1));
            new_multi[i] = 1;
        }
        return new BsplineKnotVector(degree, knotSpec, periodic, new_multi, new_knots, nControlPoints).beautify();
    }

    BsplineKnotVector shift(int firstSegment) {
        if (this.periodic != true) {
            throw new ExceptionGeometryFatal("knots should be closed form.");
        }
        if ((firstSegment < 0) || (this.nSegments() < firstSegment)) {
            throw new ExceptionGeometryFatal("given index is wrong.");
        }
        if (this.knotSpec == KnotType.UNIFORM_KNOTS) {
            return this;
        }
        int nKnots = this.nKnotValues();
        double[] newKnots = new double[nKnots];
        int[] newKnotMultiplicities = new int[nKnots];
        double upperParam = this.knotValueAt(this.degree + this.nSegments());
        double plusFactor = 0.0;
        double minusFactor = this.knotValueAt(this.degree + firstSegment) - this.knotValueAt(this.degree);
        for (int i = 0; i < nKnots; i++) {
            int j = i + firstSegment;
            if (j >= nKnots) {
                j += (2 * this.degree) + 1;
                plusFactor = upperParam;
            }
            newKnots[i] = this.knotValueAt(j % nKnots) + plusFactor - minusFactor;
            newKnotMultiplicities[i] = 1;
        }
        BsplineKnotVector knotData = new BsplineKnotVector(this.degree, this.knotSpec, true, newKnotMultiplicities, newKnots, this.nControlPoints);
        return knotData.beautify();
    }

    BsplineKnotVector makeExplicit() {
        switch(knotSpec()) {
            case UNSPECIFIED:
                return this;
            case UNIFORM_KNOTS:
                int uik = nKnotValues();
                double[] new_knots = new double[uik];
                int[] new_knot_multi = new int[uik];
                for (int uj = 0; uj < uik; uj++) {
                    new_knots[uj] = uj - degree();
                    new_knot_multi[uj] = 1;
                }
                return new BsplineKnotVector(degree(), KnotType.UNSPECIFIED, periodic, new_knot_multi, new_knots, nControlPoints());
        }
        throw new ExceptionGeometryFatal();
    }

    private void evalUniform(int k0, int n, int i, double t, double[] r) {
        int j, k;
        if (n == 0) {
            r[0] = 1.0;
        } else {
            double[] rTmp = new double[r.length - 1];
            for (j = 0; j < r.length - 1; j++) {
                rTmp[j] = r[j + 1];
            }
            evalUniform(k0, (n - 1), (i + 1), t, rTmp);
            for (j = 0; j < r.length - 1; j++) {
                r[j + 1] = rTmp[j];
            }
            for (j = 0, k = i; j <= n; j++, k++) {
                if (j == 0) {
                    r[j] = (k0 + (k + n + 1) - t) * r[j + 1] / n;
                } else {
                    if (j == n) {
                        r[j] = (t - k0 + k) * r[j] / n;
                    } else {
                        r[j] = ((t - k0 + k) * r[j] + (k0 + (k + n + 1) - t) * r[j + 1]) / n;
                    }
                }
            }
        }
    }

    private int evalBsplineU(double t, double[] r) {
        int n_seg = nSegments();
        ParameterDomain dmn = getParameterDomain();
        if (!dmn.isValid(t)) {
            throw new ExceptionGeometryFatal();
        }
        t = dmn.wrap(dmn.force(t));
        int isckt;
        int i;
        for (isckt = 1; !(t < isckt); isckt++) ;
        isckt--;
        if (isckt >= n_seg) {
            isckt = n_seg - 1;
        }
        for (i = 0; i <= degree(); i++) {
            r[i] = 0.0;
        }
        evalUniform((-degree()), degree(), isckt, t, r);
        return isckt;
    }

    private void evalNonUniform(int n, int i, double t, double[] r, double pTol) {
        double tk;
        double w1 = 0.0;
        double w2 = 0.0;
        int j, k;
        if (n == 0) {
            r[0] = 1.0;
        } else {
            double[] rTmp = new double[r.length - 1];
            for (j = 0; j < r.length - 1; j++) {
                rTmp[j] = r[j + 1];
            }
            evalNonUniform((n - 1), (i + 1), t, rTmp, pTol);
            for (j = 0; j < r.length - 1; j++) {
                r[j + 1] = rTmp[j];
            }
            for (j = 0, k = i; j <= n; j++, k++) {
                if (j != 0) {
                    if ((w1 = knotValueAt(k + n) - (tk = knotValueAt(k))) < pTol) {
                        w1 = 0.0;
                    } else {
                        w1 = (t - tk) / w1;
                    }
                }
                if (j != n) {
                    if ((w2 = (tk = knotValueAt(k + n + 1)) - knotValueAt(k + 1)) < pTol) {
                        w2 = 0.0;
                    } else {
                        w2 = (tk - t) / w2;
                    }
                }
                if (j == 0) {
                    r[j] = w2 * r[j + 1];
                } else {
                    if (j == n) {
                        r[j] = w1 * r[j];
                    } else {
                        r[j] = w1 * r[j] + w2 * r[j + 1];
                    }
                }
            }
        }
    }

    private int evalBspline(double t, double[] r) {
        int isckt;
        int i;
        int n_seg = nSegments();
        int n_seg_pd = n_seg + degree();
        ParameterDomain dmn = getParameterDomain();
        if (!dmn.isValid(t)) {
            throw new ExceptionGeometryFatal();
        }
        t = dmn.wrap(dmn.force(t));
        for (isckt = degree() + 1; isckt <= n_seg_pd; isckt++) {
            if (isckt < n_seg_pd) {
                if (t < knotValueAt(isckt)) {
                    break;
                }
            } else {
                if (!(t > knotValueAt(isckt))) {
                    break;
                }
            }
        }
        if (isckt > n_seg_pd) {
            throw new ExceptionGeometryFatal();
        }
        isckt -= degree() + 1;
        double pTol = ConditionOfOperation.getCondition().getToleranceForParameter();
        for (i = 0; i <= degree(); i++) {
            r[i] = 0.0;
        }
        evalNonUniform(degree(), isckt, t, r, pTol);
        return isckt;
    }

    int evaluateBsplineFunction(double t, double[] r) {
        if (knotSpec() == KnotType.UNIFORM_KNOTS) {
            return evalBsplineU(t, r);
        } else {
            return evalBspline(t, r);
        }
    }

    static final String[][] keyWords = { { "\tdegree\t", "\tnControlPoints\t", "\tknotSpec\t", "\tknotMultiplicities\t", "\tknots\t", "\tperiodic\t" }, { "\tuDegree\t", "\tuNControlPoints\t", "\tuKnotSpec\t", "\tuKnotMultiplicities\t", "\tuKnots\t", "\tuPeriodic\t" }, { "\tvDegree\t", "\tvNControlPoints\t", "\tvKnotSpec\t", "\tvKnotMultiplicities\t", "\tvKnots\t", "\tvPeriodic\t" } };

    /**
	 *
	 * @param writer
	 * @param indent
	 * @param index
	 */
    protected void output(PrintWriter writer, int indent, int index) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < indent; i++) {
            buf.append("\t");
        }
        String indent_tab = new String(buf);
        writer.println(indent_tab + keyWords[index][0] + degree);
        writer.println(indent_tab + keyWords[index][1] + nControlPoints);
        writer.println(indent_tab + keyWords[index][2] + knotSpec);
        if (knotSpec == KnotType.UNSPECIFIED) {
            writer.print(indent_tab + keyWords[index][3]);
            int i = 0;
            while (true) {
                writer.print(knotMultiplicities[i++]);
                for (int j = 0; j < 20 && i < knotMultiplicities.length; j++, i++) {
                    writer.print(" " + knotMultiplicities[i]);
                }
                writer.println();
                if (i < knotMultiplicities.length) {
                    writer.print(indent_tab + "\t\t");
                } else {
                    break;
                }
            }
            writer.print(indent_tab + keyWords[index][4]);
            i = 0;
            while (true) {
                writer.print(knots[i++]);
                for (int j = 0; j < 3 && i < knots.length; j++, i++) {
                    writer.print(" " + knots[i]);
                }
                writer.println();
                if (i < knots.length) {
                    writer.print(indent_tab + "\t\t");
                } else {
                    break;
                }
            }
        }
        writer.println(indent_tab + keyWords[index][5] + periodic);
    }

    /**
	 *
	 * @param out
	 */
    public void output(OutputStream out) {
        PrintWriter writer = new PrintWriter(out, true);
        output(writer, 0, 0);
    }

    static BsplineKnotVector quasiUniformKnotsOfLinearOneSegment;

    /**
	 * Initialize static data
	 */
    static {
        int[] knotMultiplicities = { 2, 2 };
        double[] knots = { 0.0, 1.0 };
        quasiUniformKnotsOfLinearOneSegment = new BsplineKnotVector(1, KnotType.UNSPECIFIED, false, knotMultiplicities, knots, 2);
    }

    @Override
    public String toString() {
        int i;
        String k = "\n[";
        for (i = 0; i < knots.length; i++) {
            if (knotMultiplicities != null) {
                String m = "";
                if (knotMultiplicities[i] > 1) m = "*" + knotMultiplicities[i];
                if (i > 0) k += knots[i] + m; else k += " " + knots[i] + m;
            } else {
                if (i > 0) k += knots[i]; else k += " " + knots[i];
            }
        }
        return "KnotVector degree=" + degree + " nctrl=" + nControlPoints + " spec=" + knotSpec + " CN=" + continuity + " periodic=" + periodic + " knots=" + knots.length + k + "]";
    }
}
