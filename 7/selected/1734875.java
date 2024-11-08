package org.jquantlib.math.interpolations;

import java.util.Arrays;
import org.jquantlib.lang.exceptions.LibraryException;
import org.jquantlib.math.Closeness;
import org.jquantlib.math.interpolations.factories.Cubic;
import org.jquantlib.math.matrixutilities.Array;
import org.jquantlib.methods.finitedifferences.TridiagonalOperator;

/**
 * Cubic interpolation between discrete points.
 * <p>
 * Cubic interpolation is fully defined when the ${f_i}$ function values
 * at points ${x_i}$ are supplemented with ${f_i}$ function derivative
 * values.
 * <p>
 * Different type of first derivative approximations are implemented, both
 * local and non-local. Local schemes (Fourth-order, Parabolic,
 * Modified Parabolic, Fritsch-Butland, Akima, Kruger) use only $f$ values
 * near $x_i$ to calculate $f_i$. Non-local schemes (Spline with different
 * boundary conditions) use all ${f_i}$ values and obtain ${f_i}$ by
 * solving a linear system of equations. Local schemes produce $C^1$
 * interpolants, while the spline scheme generates $C^2$ interpolants.
 * <p>
 * Hyman's monotonicity constraint filter is also implemented: it can be
 * applied to all schemes to ensure that in the regions of local
 * monotoniticity of the input (three successive increasing or decreasing
 * values) the interpolating cubic remains monotonic. If the interpolating
 * cubic is already monotonic, the Hyman filter leaves it unchanged
 * preserving all its original features.
 * <p>
 * In the case of $C^2$ interpolants the Hyman filter ensures local
 * monotonicity at the expense of the second derivative of the interpolant
 * which will no longer be continuous in the points where the filter has
 * been applied.
 * <p>
 * While some non-linear schemes (Modified Parabolic, Fritsch-Butland,
 * Kruger) are guaranteed to be locally monotone in their original
 * approximation, all other schemes must be filtered according to the
 * Hyman criteria at the expense of their linearity.
 * <p>
 * See R. L. Dougherty, A. Edelman, and J. M. Hyman,
 * "Nonnegativity-, Monotonicity-, or Convexity-Preserving CubicSpline and
 * Quintic Hermite Interpolation"
 * Mathematics Of Computation, v. 52, n. 186, April 1989, pp. 471-494.
 * <p>
 * @todo implement missing schemes (FourthOrder and ModifiedParabolic) and
 *       missing boundary conditions (Periodic and Lagrange).
 *
 * @test to be adapted from old ones.
 *
 * @author Richard Gomes
 */
public class CubicInterpolation extends AbstractInterpolation {

    public enum DerivativeApprox {

        /**
         * Spline approximation (non-local, non-monotone, linear[?]).
         * Different boundary conditions can be used on the left and right
         * boundaries: see BoundaryCondition.
         */
        Spline, /**
         * Fourth-order approximation (local, non-monotone, linear)
         */
        FourthOrder, /**
         * Parabolic approximation (local, non-monotone, linear)
         */
        Parabolic, /**
         * Modified-Parabolic approximation (local, monotone, non-linear)
         */
        ModifiedParabolic, /**
         * Fritsch-Butland approximation (local, monotone, non-linear)
         */
        FritschButland, /**
         * Akima approximation (local, non-monotone, non-linear)
         */
        Akima, /**
         * Kruger approximation (local, monotone, non-linear)
         */
        Kruger
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

    /**
     * Constructor for a CubicSpline interpolation.
     * <p>
     * Interpolations are not instantiated directly by applications, but via a factory class.
     *
     * @see Cubic
     */
    public CubicInterpolation(final Array vx, final Array vy, final DerivativeApprox da, final boolean monotonic, final BoundaryCondition leftCondition, final double leftConditionValue, final BoundaryCondition rightCondition, final double rightConditionValue) {
        super.impl = new CubicInterpolationImpl(vx, vy, da, monotonic, leftCondition, leftConditionValue, rightCondition, rightConditionValue);
        super.impl.update();
    }

    public Array aCoefficients() {
        return new Array(((CubicInterpolationImpl) (super.impl)).va_);
    }

    public Array bCoefficients() {
        return new Array(((CubicInterpolationImpl) (super.impl)).vb_);
    }

    public Array cCoefficients() {
        return new Array(((CubicInterpolationImpl) (super.impl)).vc_);
    }

    public boolean[] monotonicityAdjustments() {
        return ((CubicInterpolationImpl) (super.impl)).ma_.clone();
    }

    private class CubicInterpolationImpl extends AbstractInterpolation.Impl {

        private final DerivativeApprox da;

        private final boolean monotonic;

        private final BoundaryCondition leftType;

        private final BoundaryCondition rightType;

        private final double leftValue;

        private final double rightValue;

        private final double[] vx_;

        private final double[] vy_;

        private final double[] vp_;

        private final double[] va_;

        private final double[] vb_;

        private final double[] vc_;

        private final boolean[] ma_;

        private final int n;

        protected CubicInterpolationImpl(final Array x, final Array y, final DerivativeApprox da, final boolean monotonic, final CubicInterpolation.BoundaryCondition leftCondition, final double leftConditionValue, final CubicInterpolation.BoundaryCondition rightCondition, final double rightConditionValue) {
            super(x, y);
            this.vx_ = x.$;
            this.vy_ = y.$;
            this.n = vx_.length;
            this.vp_ = new double[n - 1];
            this.va_ = new double[n - 1];
            this.vb_ = new double[n - 1];
            this.vc_ = new double[n - 1];
            this.ma_ = new boolean[n];
            this.da = da;
            this.monotonic = monotonic;
            this.leftType = leftCondition;
            this.rightType = rightCondition;
            this.leftValue = leftConditionValue;
            this.rightValue = rightConditionValue;
        }

        @Override
        public void update() {
            final double[] dx = new double[n - 1];
            final double[] S = new double[n - 1];
            double[] tmp = new double[n];
            for (int i = 0; i < n - 1; ++i) {
                dx[i] = vx_[i + 1] - vx_[i];
                S[i] = (vy_[i + 1] - vy_[i]) / dx[i];
            }
            if (da == CubicInterpolation.DerivativeApprox.Spline) {
                final TridiagonalOperator L = new TridiagonalOperator(n);
                for (int i = 1; i < n - 1; ++i) {
                    L.setMidRow(i, dx[i], 2.0 * (dx[i] + dx[i - 1]), dx[i - 1]);
                    tmp[i] = 3.0 * (dx[i] * S[i - 1] + dx[i - 1] * S[i]);
                }
                switch(leftType) {
                    case NotAKnot:
                        L.setFirstRow(dx[1] * (dx[1] + dx[0]), (dx[0] + dx[1]) * (dx[0] + dx[1]));
                        tmp[0] = S[0] * dx[1] * (2.0 * dx[1] + 3.0 * dx[0]) + S[1] * dx[0] * dx[0];
                        break;
                    case FirstDerivative:
                        L.setFirstRow(1.0, 0.0);
                        tmp[0] = leftValue;
                        break;
                    case SecondDerivative:
                        L.setFirstRow(2.0, 1.0);
                        tmp[0] = 3.0 * S[0] - leftValue * dx[0] / 2.0;
                        break;
                    case Periodic:
                    case Lagrange:
                        throw new LibraryException("this end condition is not implemented yet");
                    default:
                        throw new LibraryException("unknown end condition");
                }
                switch(rightType) {
                    case NotAKnot:
                        L.setLastRow(-(dx[n - 2] + dx[n - 3]) * (dx[n - 2] + dx[n - 3]), -dx[n - 3] * (dx[n - 3] + dx[n - 2]));
                        tmp[n - 1] = -S[n - 3] * dx[n - 2] * dx[n - 2] - S[n - 2] * dx[n - 3] * (3.0 * dx[n - 2] + 2.0 * dx[n - 3]);
                        break;
                    case FirstDerivative:
                        L.setLastRow(0.0, 1.0);
                        tmp[n - 1] = rightValue;
                        break;
                    case SecondDerivative:
                        L.setLastRow(1.0, 2.0);
                        tmp[n - 1] = 3.0 * S[n - 2] + rightValue * dx[n - 2] / 2.0;
                        break;
                    case Periodic:
                    case Lagrange:
                        throw new LibraryException("this end condition is not implemented yet");
                    default:
                        throw new LibraryException("unknown end condition");
                }
                tmp = L.solveFor(tmp);
            } else {
                if (n == 2) {
                    tmp[0] = tmp[1] = S[0];
                } else {
                    switch(da) {
                        case FourthOrder:
                            throw new LibraryException("FourthOrder not implemented yet");
                        case Parabolic:
                            throw new LibraryException("Parabolic not implemented yet");
                        case ModifiedParabolic:
                            throw new LibraryException("ModifiedParabolic not implemented yet");
                        case FritschButland:
                            throw new LibraryException("FritschButland not implemented yet");
                        case Akima:
                            throw new LibraryException("Akima not implemented yet");
                        case Kruger:
                            for (int i = 1; i < n - 1; ++i) {
                                if (S[i - 1] * S[i] < 0.0) {
                                    tmp[i] = 0.0;
                                } else {
                                    tmp[i] = 2.0 / (1.0 / S[i - 1] + 1.0 / S[i]);
                                }
                            }
                            tmp[0] = (3.0 * S[0] - tmp[1]) / 2.0;
                            tmp[n - 1] = (3.0 * S[n - 2] - tmp[n - 2]) / 2.0;
                            break;
                        default:
                            throw new LibraryException("unknown scheme");
                    }
                }
            }
            Arrays.fill(ma_, false);
            if (monotonic) {
                double correction;
                double pm, pu, pd, M;
                for (int i = 0; i < n; ++i) {
                    if (i == 0) {
                        if (tmp[i] * S[0] > 0.0) {
                            correction = tmp[i] / Math.abs(tmp[i]) * Math.min(Math.abs(tmp[i]), Math.abs(3.0 * S[0]));
                        } else {
                            correction = 0.0;
                        }
                        if (!Closeness.isClose(correction, tmp[i])) {
                            tmp[i] = correction;
                            ma_[i] = true;
                        }
                    } else if (i == n - 1) {
                        if (tmp[i] * S[n - 2] > 0.0) {
                            correction = tmp[i] / Math.abs(tmp[i]) * Math.min(Math.abs(tmp[i]), Math.abs(3.0 * S[n - 2]));
                        } else {
                            correction = 0.0;
                        }
                        if (!Closeness.isClose(correction, tmp[i])) {
                            tmp[i] = correction;
                            ma_[i] = true;
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
                        if (tmp[i] * pm > 0.0) {
                            correction = tmp[i] / Math.abs(tmp[i]) * Math.min(Math.abs(tmp[i]), M);
                        } else {
                            correction = 0.0;
                        }
                        if (!Closeness.isClose(correction, tmp[i])) {
                            tmp[i] = correction;
                            ma_[i] = true;
                        }
                    }
                }
            }
            for (int i = 0; i < n - 1; ++i) {
                va_[i] = tmp[i];
                vb_[i] = (3.0 * S[i] - tmp[i + 1] - 2.0 * tmp[i]) / dx[i];
                vc_[i] = (tmp[i + 1] + tmp[i] - 2.0 * S[i]) / (dx[i] * dx[i]);
            }
            vp_[0] = 0.0;
            for (int i = 1; i < n - 1; ++i) {
                vp_[i] = vp_[i - 1] + dx[i - 1] * (vy_[i - 1] + dx[i - 1] * (va_[i - 1] / 2.0 + dx[i - 1] * (vb_[i - 1] / 3.0 + dx[i - 1] * vc_[i - 1] / 4.0)));
            }
        }

        @Override
        public double op(final double val) {
            final int j = locate(val);
            final double dx = val - vx_[j];
            return vy_[j] + dx * (va_[j] + dx * (vb_[j] + dx * vc_[j]));
        }

        @Override
        public double primitive(final double val) {
            final int j = locate(val);
            final double dx = val - vx_[j];
            return vp_[j] + dx * (vy_[j] + dx * (va_[j] / 2.0 + dx * (vb_[j] / 3.0 + dx * vc_[j] / 4.0)));
        }

        @Override
        public double derivative(final double x) {
            final int j = locate(x);
            final double dx = x - vx_[j];
            return va_[j] + (2.0 * vb_[j] + 3.0 * vc_[j] * dx) * dx;
        }

        @Override
        public double secondDerivative(final double val) {
            final int j = locate(val);
            final double dx = val - vx_[j];
            return 2.0 * vb_[j] + 6.0 * vc_[j] * dx;
        }
    }
}
