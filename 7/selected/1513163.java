package org.jquantlib.math.interpolations;

import java.util.Arrays;
import org.jquantlib.math.Array;
import org.jquantlib.math.Closeness;
import org.jquantlib.math.interpolations.factories.CubicSpline;
import org.jquantlib.methods.finitedifferences.TridiagonalOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CubicSplineInterpolation extends AbstractInterpolation {

    private static final Logger logger = LoggerFactory.getLogger(CubicSplineInterpolation.class);

    private final BoundaryCondition leftType;

    private final BoundaryCondition rightType;

    private final double leftValue;

    private final double rightValue;

    private final boolean constrained;

    private int n;

    private double[] vp;

    private double[] va;

    private double[] vb;

    private double[] vc;

    private boolean monotone;

    /**
     * This is a factory method intended to create this interpolation.
     * <p>
     * Interpolations are not instantiated directly by applications, but via a factory class.
     * 
     * @see CubicSpline
     */
    public static Interpolator getInterpolator(final CubicSplineInterpolation.BoundaryCondition leftCondition, final double leftConditionValue, final CubicSplineInterpolation.BoundaryCondition rightCondition, final double rightConditionValue, final boolean monotonicityConstraint) {
        CubicSplineInterpolation cubicSpline = new CubicSplineInterpolation(leftCondition, leftConditionValue, rightCondition, rightConditionValue, monotonicityConstraint);
        return cubicSpline.new CubicSplineInterpolationImpl(cubicSpline);
    }

    /**
     * Constructor for a CubicSpline interpolation.
     * <p>
     * Interpolations are not instantiated directly by applications, but via a factory class.
     * 
     * @see CubicSpline
     */
    private CubicSplineInterpolation(final CubicSplineInterpolation.BoundaryCondition leftCondition, final double leftConditionValue, final CubicSplineInterpolation.BoundaryCondition rightCondition, final double rightConditionValue, final boolean monotonicityConstraint) {
        this.leftType = leftCondition;
        this.rightType = rightCondition;
        this.leftValue = leftConditionValue;
        this.rightValue = rightConditionValue;
        this.monotone = false;
        this.constrained = monotonicityConstraint;
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    @Override
    public void update() {
        reload();
    }

    /**
     * {@inheritDoc}
     * 
     * @note Class factory is responsible for initializing <i>vx</i> and <i>vy</i>  
     */
    @Override
    public void reload() {
        final TridiagonalOperator L = new TridiagonalOperator(n);
        Array tmp = new Array(n);
        final double[] dx = new double[n];
        final double[] S = new double[n];
        int i = 0;
        dx[i] = vx[i + 1] - vx[i];
        S[i] = (vy[i + 1] - vy[i]) / dx[i];
        for (i = 1; i < n - 1; i++) {
            dx[i] = vx[i + 1] - vx[i];
            S[i] = (vy[i + 1] - vy[i]) / dx[i];
            L.setMidRow(i, dx[i], 2.0 * (dx[i] + dx[i - 1]), dx[i - 1]);
            tmp.set(i, 3.0 * (dx[i] * S[i - 1] + dx[i - 1] * S[i]));
        }
        switch(leftType) {
            case NotAKnot:
                L.setFirstRow(dx[1] * (dx[1] + dx[0]), (dx[0] + dx[1]) * (dx[0] + dx[1]));
                tmp.set(0, S[0] * dx[1] * (2.0 * dx[1] + 3.0 * dx[0]) + S[1] * dx[0] * dx[0]);
                break;
            case FirstDerivative:
                L.setFirstRow(1.0, 0.0);
                tmp.set(0, leftValue);
                break;
            case SecondDerivative:
                L.setFirstRow(2.0, 1.0);
                tmp.set(0, 3.0 * S[0] - leftValue * dx[0] / 2.0);
                break;
            case Periodic:
            case Lagrange:
                throw new UnsupportedOperationException("this end condition is not implemented yet");
            default:
                throw new UnsupportedOperationException("unknown end condition");
        }
        switch(rightType) {
            case NotAKnot:
                L.setLastRow(-(dx[n - 2] + dx[n - 3]) * (dx[n - 2] + dx[n - 3]), -dx[n - 3] * (dx[n - 3] + dx[n - 2]));
                tmp.set(n - 1, -S[n - 3] * dx[n - 2] * dx[n - 2] - S[n - 2] * dx[n - 3] * (3.0 * dx[n - 2] + 2.0 * dx[n - 3]));
                break;
            case FirstDerivative:
                L.setLastRow(0.0, 1.0);
                tmp.set(n - 1, rightValue);
                break;
            case SecondDerivative:
                L.setLastRow(1.0, 2.0);
                tmp.set(n - 1, 3.0 * S[n - 2] + rightValue * dx[n - 2] / 2.0);
                break;
            case Periodic:
            case Lagrange:
                throw new UnsupportedOperationException("this end condition is not implemented yet");
            default:
                throw new UnsupportedOperationException("unknown end condition");
        }
        tmp = L.solveFor(tmp);
        if (constrained) {
            double correction;
            double pm, pu, pd, M;
            for (i = 0; i < n; i++) {
                if (i == 0) {
                    if (tmp.get(i) * S[0] > 0.0) {
                        correction = tmp.get(i) / Math.abs(tmp.get(i)) * Math.min(Math.abs(tmp.get(i)), Math.abs(3.0 * S[0]));
                    } else {
                        correction = 0.0;
                    }
                    if (!Closeness.isClose(correction, tmp.get(i))) {
                        tmp.set(i, correction);
                        monotone = true;
                    }
                } else if (i == n - 1) {
                    if (tmp.get(i) * S[n - 2] > 0.0) {
                        correction = tmp.get(i) / Math.abs(tmp.get(i)) * Math.min(Math.abs(tmp.get(i)), Math.abs(3.0 * S[n - 2]));
                    } else {
                        correction = 0.0;
                    }
                    if (!Closeness.isClose(correction, tmp.get(i))) {
                        tmp.set(i, correction);
                        monotone = true;
                    }
                } else {
                    pm = (S[i - 1] * dx[i] + S[i] * dx[i - 1]) / (dx[i - 1] + dx[i]);
                    M = 3.0 * Math.min(Math.min(Math.abs(S[i - 1]), Math.abs(S[i])), Math.abs(pm));
                    if (i > 1) {
                        if ((S[i - 1] - S[i - 2]) * (S[i] - S[i - 1]) > 0.0) {
                            pd = (S[i - 1] * (2.0 * dx[i - 1] + dx[i - 2]) - S[i - 2] * dx[i - 1]) / (dx[i - 2] + dx[i - 1]);
                            if (pm * pd > 0.0 && pm * (S[i - 1] - S[i - 2]) > 0.0) {
                                M = Math.max(M, 1.5 * Math.min(Math.abs(pm), Math.abs(pd)));
                            }
                        }
                    }
                    if (i < n - 2) {
                        if ((S[i] - S[i - 1]) * (S[i + 1] - S[i]) > 0.0) {
                            pu = (S[i] * (2.0 * dx[i] + dx[i + 1]) - S[i + 1] * dx[i]) / (dx[i] + dx[i + 1]);
                            if (pm * pu > 0.0 && -pm * (S[i] - S[i - 1]) > 0.0) {
                                M = Math.max(M, 1.5 * Math.min(Math.abs(pm), Math.abs(pu)));
                            }
                        }
                    }
                    if (tmp.get(i) * pm > 0.0) {
                        correction = tmp.get(i) / Math.abs(tmp.get(i)) * Math.min(Math.abs(tmp.get(i)), M);
                    } else {
                        correction = 0.0;
                    }
                    if (!Closeness.isClose(correction, tmp.get(i))) {
                        tmp.set(i, correction);
                        monotone = true;
                    }
                }
            }
        }
        for (i = 0; i < n - 1; i++) {
            va[i] = tmp.get(i);
            vb[i] = (3.0 * S[i] - tmp.get(i + 1) - 2.0 * tmp.get(i)) / dx[i];
            vc[i] = (tmp.get(i + 1) + tmp.get(i) - 2.0 * S[i]) / (dx[i] * dx[i]);
        }
        vp[0] = 0.0;
        for (i = 1; i < n - 1; i++) {
            vp[i] = vp[i - 1] + dx[i - 1] * (vy[i - 1] + dx[i - 1] * (va[i - 1] / 2.0 + dx[i - 1] * (vb[i - 1] / 3.0 + dx[i - 1] * vc[i - 1] / 4.0)));
        }
    }

    @Override
    protected double evaluateImpl(double x) {
        int j = locate(x);
        double dx = x - vx[j];
        return vy[j] + dx * (va[j] + dx * (vb[j] + dx * vc[j]));
    }

    @Override
    protected double primitiveImpl(double x) {
        int j = locate(x);
        double dx = x - vx[j];
        return vp[j] + dx * (vy[j] + dx * (va[j] / 2.0 + dx * (vb[j] / 3.0 + dx * vc[j] / 4.0)));
    }

    @Override
    protected double derivativeImpl(double x) {
        int j = locate(x);
        double dx = x - vx[j];
        return va[j] + (2.0 * vb[j] + 3.0 * vc[j] * dx) * dx;
    }

    @Override
    protected double secondDerivativeImpl(double x) {
        int j = locate(x);
        double dx = x - vx[j];
        return 2.0 * vb[j] + 6.0 * vc[j] * dx;
    }

    public double[] getVa() {
        return va;
    }

    public double[] getVb() {
        return vb;
    }

    public double[] getVc() {
        return vc;
    }

    /**
     * This class is a default implementation for {@link CubicSplineInterpolation} instances.
     * 
     * @author Richard Gomes
     * @author Daniel Kong
     * 
     */
    private class CubicSplineInterpolationImpl implements Interpolator {

        private CubicSplineInterpolation delegate;

        public CubicSplineInterpolationImpl(final CubicSplineInterpolation delegate) {
            this.delegate = delegate;
        }

        public final Interpolation interpolate(final double[] x, final double[] y) {
            return interpolate(x.length, x, y);
        }

        public final Interpolation interpolate(final int size, final double[] x, final double[] y) {
            delegate.vx = Arrays.copyOfRange(x, 0, size);
            delegate.vy = Arrays.copyOfRange(y, 0, size);
            n = vx.length;
            vp = new double[n - 1];
            va = new double[n - 1];
            vb = new double[n - 1];
            vc = new double[n - 1];
            delegate.reload();
            return delegate;
        }

        public final boolean isGlobal() {
            return true;
        }
    }

    public enum BoundaryCondition {

        /**
         * Make second(-last) point an inactive knot
         */
        NotAKnot, /**
         * Match value of end-slope
         */
        FirstDerivative, /**
         * Match value of second derivative at end
         */
        SecondDerivative, /**
         * Match first and second derivative at either end
         */
        Periodic, /**
         * Match end-slope to the slope of the cubic that matches
         * the first four data at the respective end
         */
        Lagrange
    }
}
