package org.ejml.example;

import org.ejml.alg.dense.linsol.AdjustableLinearSolver;
import org.ejml.alg.dense.linsol.LinearSolverFactory;
import org.ejml.data.DenseMatrix64F;

/**
 * <p>
 * This example demonstrates how a polynomial can be fit to a set of data.  This is done by
 * using a least squares solver that is adjustable.  By using an adjustable solver elements
 * can be inexpensively removed and the coefficients recomputed.  This is much less expensive
 * than resolving the whole system from scratch.
 * </p>
 * <p>
 * The following is demonstrated:<br>
 * <ol>
 *  <li>Creating a solver using LinearSolverFactory</li>
 *  <li>Using an adjustable solver</li>
 *  <li>reshaping</li>
 * </ol>
 * @author Peter Abeles
 */
public class PolynomialFit {

    DenseMatrix64F A;

    DenseMatrix64F coef;

    DenseMatrix64F y;

    AdjustableLinearSolver solver;

    /**
     * Constructor.
     *
     * @param degree The polynomial's degree which is to be fit to the observations.
     */
    public PolynomialFit(int degree) {
        coef = new DenseMatrix64F(degree + 1, 1);
        A = new DenseMatrix64F(1, degree + 1);
        y = new DenseMatrix64F(1, 1);
        solver = LinearSolverFactory.adjustable();
    }

    /**
     * Returns the computed coefficients
     *
     * @return polynomial coefficients that best fit the data.
     */
    public double[] getCoef() {
        return coef.data;
    }

    /**
     * Computes the best fit set of polynomial coefficients to the provided observations.
     *
     * @param samplePoints where the observations were sampled.
     * @param observations A set of observations.
     */
    public void fit(double samplePoints[], double[] observations) {
        y.reshape(observations.length, 1, false);
        System.arraycopy(observations, 0, y.data, 0, observations.length);
        A.reshape(y.numRows, coef.numRows, false);
        for (int i = 0; i < observations.length; i++) {
            double obs = 1;
            for (int j = 0; j < coef.numRows; j++) {
                A.set(i, j, obs);
                obs *= samplePoints[i];
            }
        }
        if (!solver.setA(A)) throw new RuntimeException("Solver failed");
        solver.solve(y, coef);
    }

    /**
     * Removes the observation that fits the model the worst and recomputes the coefficients.
     * This is done efficiently by using an adjustable solver.  Often times the elements with
     * the largest errors are outliers and not part of the system being modeled.  By removing them
     * a more accurate set of coefficients can be computed.
     */
    public void removeWorstFit() {
        int worstIndex = -1;
        double worstError = -1;
        for (int i = 0; i < y.numRows; i++) {
            double predictedObs = 0;
            for (int j = 0; j < coef.numRows; j++) {
                predictedObs += A.get(i, j) * coef.get(j, 0);
            }
            double error = Math.abs(predictedObs - y.get(i, 0));
            if (error > worstError) {
                worstError = error;
                worstIndex = i;
            }
        }
        if (worstIndex == -1) return;
        removeObservation(worstIndex);
        solver.removeRowFromA(worstIndex);
        solver.solve(y, coef);
    }

    /**
     * Removes an element from the observation matrix.
     *
     * @param index which element is to be removed
     */
    private void removeObservation(int index) {
        final int N = y.numRows - 1;
        final double d[] = y.data;
        for (int i = index; i < N; i++) {
            d[i] = d[i + 1];
        }
        y.numRows--;
    }
}
