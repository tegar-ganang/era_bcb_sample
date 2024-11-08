package jp.go.ipa.jgcl;

import org.magiclight.common.MLUtil;

/**
 * Base class for approximations of BSplines
 */
class Approximation {

    protected boolean isClosed;

    /**
	 * Number of points
	 */
    protected int nPoints;

    /**
	 * Parameters
	 */
    protected double[] params;

    /**
	 * Degree
	 */
    protected static final int degree = 3;

    /**
	 * Constructor
	 */
    protected Approximation(int nPoints, double[] params, boolean isClosed) {
        if (nPoints < 2 || (isClosed && nPoints < 3)) {
            throw new ExceptionGeometryInvalidArgumentValue();
        }
        if ((!isClosed && nPoints != params.length) || (isClosed && (nPoints + 1) != params.length)) {
            throw new ExceptionGeometryInvalidArgumentValue();
        }
        this.nPoints = nPoints;
        this.params = params;
        this.isClosed = isClosed;
    }

    static int minSegmentNumber(boolean isClosed, int degree) {
        if (isClosed) {
            return degree + 1;
        } else {
            return 1;
        }
    }

    protected int minSegmentNumber() {
        return minSegmentNumber(isClosed, degree);
    }

    static int maxSegmentNumber(int nPoints, boolean isClosed, int degree) {
        if (isClosed) {
            return nPoints;
        } else {
            return nPoints - degree;
        }
    }

    protected int maxSegmentNumber() {
        return maxSegmentNumber(nPoints, isClosed, degree);
    }

    private int nseg_numer = 1;

    private int nseg_denom = 2;

    protected int initSegmentNumber() {
        nseg_numer = 1;
        nseg_denom = 2;
        int nseg = (nPoints * nseg_numer) / nseg_denom;
        int min_nseg = minSegmentNumber();
        if (nseg < min_nseg) {
            nseg = min_nseg;
        }
        if (nseg > maxSegmentNumber()) {
            return -1;
        }
        return nseg;
    }

    protected boolean reNewSegmentNumber(int[] nsegs, int nsegI, boolean is_tolerated) {
        if (MLUtil.DEBUG) {
            System.err.println("// nseg = " + nsegs[nsegI] + ", tolerated = " + is_tolerated);
        }
        nseg_denom *= 2;
        nseg_numer *= 2;
        if (is_tolerated) {
            nseg_numer--;
        } else {
            nseg_numer++;
        }
        nsegs[++nsegI] = (nPoints * nseg_numer) / nseg_denom;
        int min_nseg = minSegmentNumber();
        int max_nseg = maxSegmentNumber();
        if (max_nseg < min_nseg) {
            max_nseg = min_nseg;
        }
        if (nsegs[nsegI] < min_nseg) {
            nsegs[nsegI] = min_nseg;
        }
        if (nsegs[nsegI] > max_nseg) {
            nsegs[nsegI] = max_nseg;
        }
        for (int i = 0; i < nsegI; i++) {
            if (nsegs[i] == nsegs[nsegI]) {
                return false;
            }
        }
        return true;
    }

    protected boolean compKnots(double sp, double ep, int nseg, int lower, int upper, double curvatures[], double sorted_curvatures[], double[] knots) {
        double threshold;
        int i, k;
        if (nseg == 1) {
            knots[0] = sp;
            knots[1] = ep;
        } else {
            if ((k = upper - (nseg - 2)) < lower) {
                if (MLUtil.DEBUG) {
                    System.err.println("nseg is too large\n");
                }
                return false;
            }
            threshold = sorted_curvatures[k];
            if (MLUtil.DEBUG) {
                System.err.println("threshold : " + threshold);
            }
            k = 0;
            knots[k++] = sp;
            for (i = lower; i <= upper; i++) {
                if (!(curvatures[i] < threshold) && k < nseg) {
                    knots[k++] = params[i];
                }
            }
            knots[k] = ep;
            if (nseg != k) {
                if (MLUtil.DEBUG) {
                    System.err.println("something wrong\n");
                }
                return false;
            }
        }
        return true;
    }

    protected boolean tolerated(double tol, double res[]) {
        int i;
        if (MLUtil.DEBUG) {
            double max_r;
            int max_i;
            max_r = res[0];
            max_i = 0;
            for (i = 0; i < nPoints; i++) {
                if (res[i] > max_r) {
                    max_r = res[i];
                    max_i = i;
                }
            }
            System.err.println("max res : " + max_r + " [" + max_i + "]");
        }
        for (i = 0; i < nPoints; i++) {
            if (res[i] > tol) {
                return false;
            }
        }
        return true;
    }

    protected static final int MARGIN = 4;

    private double[] getKnotArray(int uik, double[] orig, int nSegments) {
        double[] knots = new double[uik];
        if (isClosed) {
            int i, j;
            for (i = (this.degree - 1), j = (nSegments - 1); i >= 0; i--, j--) {
                knots[i] = knots[i + 1] - (orig[j + 1] - orig[j]);
            }
            for (i = (this.degree + 1), j = 1; j < (nSegments + 1); i++, j++) {
                knots[i] = orig[j];
            }
            for (j = 0; j < this.degree; i++, j++) {
                knots[i] = knots[i - 1] + (orig[j + 1] - orig[j]);
            }
        } else {
            for (int i = 0; i < knots.length; i++) {
                knots[i] = orig[i];
            }
        }
        return knots;
    }

    private int[] getKnotMultiplicities(int length) {
        int[] knotMultiplicities = new int[length];
        for (int i = 0; i < knotMultiplicities.length; i++) {
            knotMultiplicities[i] = 1;
        }
        if (!this.isClosed) {
            knotMultiplicities[0] = knotMultiplicities[length - 1] = this.degree + 1;
        }
        return knotMultiplicities;
    }

    protected BsplineKnotVector getKnotData(int nsegs, double[] knots) {
        int uicp, uik;
        if (isClosed) {
            uicp = nsegs;
            uik = (2 * degree) + nsegs + 1;
        } else {
            uicp = nsegs + degree;
            uik = nsegs + 1;
        }
        knots = getKnotArray(uik, knots, nsegs);
        if (MLUtil.DEBUG) {
            for (int i = 0; i < knots.length; i++) {
                System.err.println("knots[" + i + "] = " + knots[i]);
            }
        }
        int[] knotMultiplicities = getKnotMultiplicities(knots.length);
        if (MLUtil.DEBUG) {
            for (int i = 0; i < knotMultiplicities.length; i++) {
                System.err.println("knotMultiplicities[" + i + "] = " + knotMultiplicities[i]);
            }
        }
        return new BsplineKnotVector(degree, KnotType.UNSPECIFIED, isClosed, uik, knotMultiplicities, knots, uicp, MLUtil.DEBUG);
    }

    protected MatrixDouble getDesignMatrix(BsplineKnotVector knotData) {
        int uicp = knotData.nControlPoints();
        int nseg = knotData.nSegments();
        int npnts = nPoints;
        MatrixDouble designMatrix = new MatrixDouble(npnts, uicp);
        double[] bcoef = new double[degree + 1];
        if (MLUtil.DEBUG) {
            System.err.println("<start getDesignMatrix()>");
        }
        for (int i = 0; i < npnts; i++) {
            int cseg = knotData.evaluateBsplineFunction(params[i], bcoef);
            if (this.isClosed) {
                int j, m;
                for (j = 0; j < cseg; j++) {
                    designMatrix.setElementAt(i, j, 0.0);
                }
                for (int l = 0; l <= degree; l++, j++) {
                    m = j % uicp;
                    designMatrix.setElementAt(i, m, bcoef[l]);
                }
                for (; j < uicp; j++) {
                    designMatrix.setElementAt(i, j, 0.0);
                }
            } else {
                int j, k;
                for (j = 0, k = 0; j < cseg; j++, k++) {
                    designMatrix.setElementAt(i, k, 0.0);
                }
                for (int l = 0; l <= degree; l++, j++, k++) {
                    designMatrix.setElementAt(i, k, bcoef[l]);
                }
                for (; j < uicp; j++, k++) {
                    designMatrix.setElementAt(i, k, 0.0);
                }
            }
        }
        if (MLUtil.DEBUG) {
            for (int i = 0; i < designMatrix.getRowSize(); i++) {
                System.err.print("<" + designMatrix.getElementAt(i, 0));
                for (int j = 1; j < designMatrix.getColumnSize(); j++) {
                    System.err.print(", " + designMatrix.getElementAt(i, j));
                }
                System.err.println(">");
            }
        }
        return designMatrix;
    }
}
