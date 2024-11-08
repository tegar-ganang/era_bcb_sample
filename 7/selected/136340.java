package org.apache.batik.test;

import java.util.Vector;

/**
 * This abstract <code>Test</code> implementation instruments performance
 * testing.
 *
 * Derived classes need only implement the <code>runOp</code> and,
 * optionally, the <code>runRef</code> methods.
 *
 * The <code>setReferenceScore</code> method is used to specify
 * the last recorded score for the performance test and the
 * <code>setAllowedScoreDeviation</code> method is used to specify
 * the allowed deviation from the reference score.
 *
 * @author <a href="mailto:vincent.hardy@sun.com">Vincent Hardy</a>
 * @version $Id: PerformanceTest.java 482121 2006-12-04 10:00:39Z dvholten $
 */
public abstract class PerformanceTest extends AbstractTest {

    /**
     * Reference score. -1 means there is no reference score
     */
    protected double referenceScore = -1;

    /**
     * Allowed deviation from the reference score. 10% by default
     */
    protected double allowedScoreDeviation = 0.1;

    /**
     * Score during last run
     */
    protected double lastScore = -1;

    public double getLastScore() {
        return lastScore;
    }

    public double getReferenceScore() {
        return referenceScore;
    }

    public void setReferenceScore(double referenceScore) {
        this.referenceScore = referenceScore;
    }

    public double getAllowedScoreDeviation() {
        return allowedScoreDeviation;
    }

    public void setAllowedScoreDeviation(double allowedScoreDeviation) {
        this.allowedScoreDeviation = allowedScoreDeviation;
    }

    /**
     * Force implementations to only implement <code>runOp</code>
     * and other performance specific methods.
     */
    public final TestReport run() {
        return super.run();
    }

    /**
     * Force implementations to only implement <code>runOp</code>
     * and other performance specific methods.
     */
    public final boolean runImplBasic() throws Exception {
        return false;
    }

    /**
     * This implementation of runImpl runs the reference
     * operation (with <code>runRef</code>), then runs
     * the operation (with <code>runOp</code>) and checks whether
     * or not the score is within the allowed deviation of the
     * reference score.
     *
     * @see #runRef
     * @see #runOp
     */
    public final TestReport runImpl() throws Exception {
        int iter = 50;
        double refUnit = 0;
        long refStart = 0;
        long refEnd = 0;
        long opEnd = 0;
        long opStart = 0;
        double opLength = 0;
        runRef();
        runOp();
        double[] scores = new double[iter];
        for (int i = 0; i < iter; i++) {
            if (i % 2 == 0) {
                refStart = System.currentTimeMillis();
                runRef();
                refEnd = System.currentTimeMillis();
                runOp();
                opEnd = System.currentTimeMillis();
                refUnit = refEnd - refStart;
                opLength = opEnd - refEnd;
            } else {
                opStart = System.currentTimeMillis();
                runOp();
                opEnd = System.currentTimeMillis();
                runRef();
                refEnd = System.currentTimeMillis();
                refUnit = refEnd - opEnd;
                opLength = opEnd - opStart;
            }
            scores[i] = opLength / refUnit;
            System.err.println(".");
            System.gc();
        }
        System.err.println();
        sort(scores);
        double score = 0;
        int trim = 5;
        for (int i = trim; i < scores.length - trim; i++) {
            score += scores[i];
        }
        score /= (iter - 2 * trim);
        this.lastScore = score;
        if (referenceScore == -1) {
            TestReport report = reportError("no.reference.score.set");
            report.addDescriptionEntry("computed.score", "" + score);
            return report;
        } else {
            double scoreMin = referenceScore * (1 - allowedScoreDeviation);
            double scoreMax = referenceScore * (1 + allowedScoreDeviation);
            if (score > scoreMax) {
                TestReport report = reportError("performance.regression");
                report.addDescriptionEntry("reference.score", "" + referenceScore);
                report.addDescriptionEntry("computed.score", "" + score);
                report.addDescriptionEntry("score.deviation", "" + 100 * ((score - referenceScore) / referenceScore));
                return report;
            } else if (score < scoreMin) {
                TestReport report = reportError("unexpected.performance.improvement");
                report.addDescriptionEntry("reference.score", "" + referenceScore);
                report.addDescriptionEntry("computed.score", "" + score);
                report.addDescriptionEntry("score.deviation", "" + 100 * ((score - referenceScore) / referenceScore));
                return report;
            } else {
                return reportSuccess();
            }
        }
    }

    protected void sort(double[] a) throws Exception {
        for (int i = a.length - 1; i >= 0; i--) {
            boolean swapped = false;
            for (int j = 0; j < i; j++) {
                if (a[j] > a[j + 1]) {
                    double d = a[j];
                    a[j] = a[j + 1];
                    a[j + 1] = d;
                    swapped = true;
                }
            }
            if (!swapped) return;
        }
    }

    /**
     * Runs the reference operation.
     * By default, this runs the same BufferedImage drawing
     * operation 10000 times
     */
    protected void runRef() {
        Vector v = new Vector();
        for (int i = 0; i < 10000; i++) {
            v.addElement("" + i);
        }
        for (int i = 0; i < 10000; i++) {
            if (v.contains("" + i)) {
                v.remove("" + i);
            }
        }
    }

    /**
     * Runs the tested operation
     */
    protected abstract void runOp() throws Exception;
}
