package org.kf.stats;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import org.kf.math.FastLogFactorials;
import org.kf.math.HypergeometricProbability;
import org.kf.math.Sums;

/**
 * Utilities about enumerating compatible tables of a given genotypic 2x3 contingency table
 *
 */
public class GenotypicTableEnumerator {

    public double sum_b_range = 0;

    public double sum_a_range = 0;

    public long nbexps = 0;

    public long nbTotalTables = 0;

    public interface TableVisitor {

        void init(int... t);

        /**
         * Table enumerated. can tell to stop the enumeration by returning false
         *
         * @param t the t
         *
         * @return true, if successful
         */
        boolean tableEnumerated(int... t);
    }

    static class DummyTableVisitor implements TableVisitor {

        public void init(int... t) {
        }

        public boolean tableEnumerated(int... t) {
            return true;
        }
    }

    class ReferenceProbTableVisitor implements TableVisitor {

        private double[] probs;

        int nb = 0;

        HypergeometricProbability prob = new HypergeometricProbability(logFactComputer);

        public void setProbs(double[] probs) {
            this.probs = probs;
        }

        public void init(int... t) {
            nb = 0;
        }

        public boolean tableEnumerated(int... t) {
            probs[nb++] = prob.computeProb(t);
            return true;
        }
    }

    final class ProbTableVisitor implements TableVisitor {

        final HypergeometricProbability prob = new HypergeometricProbability(logFactComputer);

        private double[] probs;

        private int nb;

        public void setProbs(double[] probs) {
            this.probs = probs;
        }

        public final void init(int... t) {
            prob.computeProb(t);
            nb = 0;
        }

        public final boolean tableEnumerated(int... t) {
            probs[nb++] = prob.computeProbForTableWithSameMarginsAsPreviousOne(t);
            return true;
        }
    }

    final class FastProbTableVisitor implements TableVisitor {

        final HypergeometricProbability prob = new HypergeometricProbability(logFactComputer);

        private double[] probs;

        private int nb;

        public void setProbs(double[] probs) {
            this.probs = probs;
        }

        public final void init(int... t) {
            prob.computeProb(t);
            nb = 0;
        }

        public final boolean tableEnumerated(int... t) {
            probs[nb++] = prob.fastComputeProbForTableWithSameMarginsAsPreviousOne(t[0], t[1]);
            return true;
        }
    }

    class MinScoringTableVisitor implements TableVisitor {

        int[] worst = null;

        double worstScore = 0;

        Score stat = null;

        public void setStat(Score stat) {
            this.stat = stat;
        }

        public int[] getWorst() {
            return worst;
        }

        public double getWorstScore() {
            return worstScore;
        }

        public boolean tableEnumerated(int... t) {
            double s = stat.score(t[0], t[1], t[2], t[3], t[4], t[5]);
            if (s < worstScore) {
                worstScore = s;
                worst = t;
            }
            return true;
        }

        public void init(int... t) {
            double s0 = stat.initFromObservedTable(t[0], t[1], t[2], t[3], t[4], t[5]);
            worst = t;
            worstScore = s0;
        }
    }

    HashMap<Class<?>, TableVisitor> singletons = new HashMap<Class<?>, TableVisitor>(10);

    FastLogFactorials logFactComputer;

    HypergeometricProbability prob;

    /**
     * @param maxTableSum the max table sum expected (>0)
     */
    public GenotypicTableEnumerator(int maxTableSum) {
        logFactComputer = new FastLogFactorials(maxTableSum);
        prob = new HypergeometricProbability(logFactComputer);
    }

    TableVisitor getVisitor(Class<?> c) {
        TableVisitor visitor = singletons.get(c);
        if (null == visitor) {
            try {
                visitor = (TableVisitor) c.getDeclaredConstructor(new Class<?>[] { GenotypicTableEnumerator.class }).newInstance(this);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            singletons.put(c, visitor);
        }
        return visitor;
    }

    public int[] findMinimalScoringTable(Score stat, int... t) {
        MinScoringTableVisitor visitor = new MinScoringTableVisitor();
        visitor.setStat(stat);
        enumerateAllCompatibleTables(visitor, t);
        return visitor.getWorst();
    }

    /**
     * Reorder table by exchanging lines and columns so that
     * the enumeration range of the first cell is minimal
     *
     * @param t the table to reorder
     * @param dest the dest
     */
    public void reorderTable(int[] t, int[] dest) {
        final int a = t[0], b = t[1], c = t[2], d = t[3], e = t[4], f = t[5];
        final int c1 = a + d, c2 = b + e, c3 = c + f;
        final int n1 = a + b + c, n2 = d + e + f;
        for (int i = 0; i < t.length; i++) {
            dest[i] = t[i];
        }
        int cmin = Math.min(c1, Math.min(c2, c3));
        if (c1 > cmin) {
            int col = c2 > cmin ? 2 : 1;
            dest[0] = t[col];
            dest[3] = t[col + 3];
            dest[col] = t[0];
            dest[col + 3] = t[3];
        }
        if (n1 > n2) {
            int temp;
            for (int i = 0; i < 3; i++) {
                temp = dest[i];
                dest[i] = dest[i + 3];
                dest[i + 3] = temp;
            }
        }
    }

    /**
     * Compute table hypergeometric probabilities using the reference method (most reliable)
     * , useful for test purposes (see {@link HypergeometricProbability#computeProb(int...)})
     *
     *	@see HypergeometricProbability#computeProb(int...)
     *
     * Beware : no check is done on the size of the array
     *
     * @see #computeTableHypergeometricProbabilities(int...)
     *
     * @param probs the probs, needs to be big enough, so you have to guess the maximum
     * number of compatible tables
     *
     * @param t the t
     *
     * @return the number of enumerated tables, so the number of probs filled into the supplied array
     */
    public int computeTableReferenceHypergeometricProbabilities(double[] probs, int... t) {
        ReferenceProbTableVisitor visitor = (ReferenceProbTableVisitor) getVisitor(ReferenceProbTableVisitor.class);
        visitor.setProbs(probs);
        int n = enumerateAllCompatibleTables(visitor, t);
        visitor.setProbs(null);
        return n;
    }

    /**
     * Compute table hypergeometric probabilities by the standard method
     * (see {@link HypergeometricProbability#computeProbForTableWithSameMarginsAsPreviousOne(int...)})
     *
     * @see HypergeometricProbability#computeProbForTableWithSameMarginsAsPreviousOne(int...)
     *
     * @param probs the probs
     * @param t the t
     *
     * @return the int
     */
    public int computeTableHypergeometricProbabilities(double[] probs, int... t) {
        ProbTableVisitor visitor = (ProbTableVisitor) getVisitor(ProbTableVisitor.class);
        visitor.setProbs(probs);
        int n = enumerateAllCompatibleTables(visitor, t);
        visitor.setProbs(null);
        return n;
    }

    /**
     * compute table hypergeometric probabilities using a fast approximate method
     * (see {@link HypergeometricProbability#fastComputeProbForTableWithSameMarginsAsPreviousOne(int, int)})
     *
     * * @see HypergeometricProbability#fastComputeProbForTableWithSameMarginsAsPreviousOne(int, int)
     *
     * @param probs the probs
     * @param t the t
     *
     * @return the int
     */
    public int fastComputeTableHypergeometricProbabilities(double[] probs, int... t) {
        FastProbTableVisitor visitor = (FastProbTableVisitor) getVisitor(FastProbTableVisitor.class);
        visitor.setProbs(probs);
        int n = enumerateAllCompatibleTables(visitor, t);
        visitor.setProbs(null);
        return n;
    }

    /**
     * Compute quickly nb of compatible tables.
     *
     * @param t the t
     *
     * @return the nb of compatible of tables
     */
    public int computeNbOfCompatibleTables(int... t) {
        int a = t[0], b = t[1], c = t[2], d = t[3], e = t[4], f = t[5];
        int c1 = a + d, c2 = b + e;
        int n1 = a + b + c, n2 = d + e + f, n = n1 + n2;
        int b0 = c2 + n1 + c1 - n;
        int a_min = Math.max(0, c1 - n2), a_max = Math.min(c1, n1);
        int nb = 0;
        int I1_min = Math.max(a_min, b0);
        int I1_max = Math.min(a_max, n1 - c2);
        int size = I1_max - I1_min + 1;
        if (size >= 0) nb += size * (c2 + 1);
        int I2_min = Math.max(I1_min, n1 - c2 + 1);
        int I2_max = a_max;
        size = I2_max - I2_min + 1;
        if (size >= 0) nb += Sums.arithmeticSequelSum(n1 - I2_max + 1, n1 - I2_min + 1, size);
        int I3_min = a_min;
        int I3_max = Math.min(I1_max, b0 - 1);
        size = I3_max - I3_min + 1;
        if (size >= 0) nb += Sums.arithmeticSequelSum(c2 - b0 + I3_min + 1, c2 - b0 + I3_max + 1, size);
        int I4_min = Math.max(a_min, n1 - c2 + 1);
        int I4_max = Math.min(a_max, b0 - 1);
        size = I4_max - I4_min + 1;
        if (size >= 0) nb += size * (n1 - b0 + 1);
        return nb;
    }

    public int enumerateAllCompatibleTables(TableVisitor visitor, int... t) {
        final int a = t[0], b = t[1], c = t[2], d = t[3], e = t[4], f = t[5];
        int b_min = 0, b_max = 0;
        int ai, bi, ci, di, ei, fi;
        final int c1 = a + d, c2 = b + e, c3 = c + f;
        final int n1 = a + b + c, n2 = d + e + f, n = n1 + n2;
        final int b0 = c2 + n1 + c1 - n;
        final int a_min = Math.max(0, c1 - n2), a_max = Math.min(c1, n1);
        visitor.init(t);
        int nbTables = 0;
        for (ai = a_min; ai <= a_max; ai++) {
            b_min = Math.max(0, b0 - ai);
            b_max = Math.min(n1 - ai, c2);
            di = c1 - ai;
            for (bi = b_min; bi <= b_max; bi++) {
                ++nbTables;
                ci = n1 - ai - bi;
                ei = c2 - bi;
                fi = c3 - ci;
                if (!visitor.tableEnumerated(ai, bi, ci, di, ei, fi)) return nbTables;
            }
        }
        return nbTables;
    }

    /**
     * compute table hypergeometric probabilities using the exact same method
     * than {@link #fasterComputeTableHypergeometricProbabilities(double[], int...)}
     * but with the {@link HypergeometricProbability} code inlined for
     * speed purposes
     * @see #fastComputeTableHypergeometricProbabilities(double[], int...)
     *
     * @param probs the probs
     * @param t the table
     *
     * @return the number of tables
     */
    public int ultraFastComputeTableHypergeometricProbabilities(double[] probs, int... t) {
        final int a = t[0], b = t[1], c = t[2], d = t[3], e = t[4], f = t[5];
        int b_min = 0, b_max = 0;
        @SuppressWarnings("unused") int ai, bi, ci;
        final int c1 = a + d, c2 = b + e, c3 = c + f;
        final int n1 = a + b + c, n2 = d + e + f, n = n1 + n2;
        final int b0 = c2 + n1 + c1 - n;
        final int a_min = Math.max(0, c1 - n2), a_max = Math.min(c1, n1);
        final int NB_MULT_MAX = HypergeometricProbability.NB_MULT_MAX;
        logFactComputer.ensureCapacity(n);
        final double[] T = logFactComputer.getLogFactorialTable();
        final double num = T[c1] + T[c2] + T[c3] + T[n1] + T[n2] - T[n];
        double p = 0;
        int nbmults = -1;
        int nbTables = 0;
        for (ai = a_min; ai <= a_max; ai++) {
            b_min = Math.max(0, b0 - ai);
            b_max = Math.min(n1 - ai, c2);
            final int di = c1 - ai;
            final int ki = n2 - c1 - c2 + ai + 1;
            nbmults = NB_MULT_MAX;
            for (bi = b_min; bi <= b_max; bi++) {
                if (nbmults >= NB_MULT_MAX) {
                    ci = n1 - ai - bi;
                    p = Math.exp(num - T[ai] - T[bi] - T[ci] - T[di] - T[c2 - bi] - T[c3 - ci]);
                    nbmults = 0;
                } else {
                    final int j = bi - 1;
                    p *= (n1 - ai - j) * (c2 - j) / ((double) ((j + 1) * (ki + j)));
                    nbmults++;
                }
                probs[nbTables++] = p;
            }
        }
        return nbTables;
    }

    /**
     * just for benchmarking purposes
     * @param probs
     * @param t
     * @return
     */
    public int inlineNoOptComputeTableHypergeometricProbabilities(double[] probs, int... t) {
        final int a = t[0], b = t[1], c = t[2], d = t[3], e = t[4], f = t[5];
        int b_min = 0, b_max = 0;
        @SuppressWarnings("unused") int ai, bi, ci;
        final int c1 = a + d, c2 = b + e, c3 = c + f;
        final int n1 = a + b + c, n2 = d + e + f, n = n1 + n2;
        final int b0 = c2 + n1 + c1 - n;
        final int a_min = Math.max(0, c1 - n2), a_max = Math.min(c1, n1);
        logFactComputer.ensureCapacity(n);
        final double[] T = logFactComputer.getLogFactorialTable();
        final double num = T[c1] + T[c2] + T[c3] + T[n1] + T[n2] - T[n];
        double p = 0;
        int nbmults = -1;
        int nbTables = 0;
        for (ai = a_min; ai <= a_max; ai++) {
            b_min = Math.max(0, b0 - ai);
            b_max = Math.min(n1 - ai, c2);
            final int di = c1 - ai;
            final int ki = n2 - c1 - c2 + ai + 1;
            for (bi = b_min; bi <= b_max; bi++) {
                ci = n1 - ai - bi;
                probs[nbTables++] = Math.exp(num - T[ai] - T[bi] - T[ci] - T[di] - T[c2 - bi] - T[c3 - ci]);
            }
        }
        return nbTables;
    }

    /**
     * compute table hypergeometric probabilities using the exact same method
     * than {@link #fastComputeTableHypergeometricProbabilities(double[], int...)}
     * but using directly the {@link HypergeometricProbability} class, i.e not using
     * the standard {@link #enumerateAllCompatibleTables(org.kf.stats.GenotypicTableEnumerator.TableVisitor, int...)}
     * with an adequate visitor.
     *
     * So it is faster than {@link #fastComputeTableHypergeometricProbabilities(double[], int...)}
     * but not as fast as {@link #ultraFastComputeTableHypergeometricProbabilities(double[], int...)}
     * @param probs the probs
     * @param t the table
     *
     * @return the number of tables
     *
     * @see #fastComputeTableHypergeometricProbabilities(double[], int...)
     */
    public int fasterComputeTableHypergeometricProbabilities(double[] probs, int... t) {
        final int a = t[0], b = t[1], c = t[2], d = t[3], e = t[4], f = t[5];
        int b_min = 0, b_max = 0;
        @SuppressWarnings("unused") int ai, bi, ci, di, ei, fi;
        final int c1 = a + d, c2 = b + e, c3 = c + f;
        final int n1 = a + b + c, n2 = d + e + f, n = n1 + n2;
        final int b0 = c2 + n1 + c1 - n;
        final int a_min = Math.max(0, c1 - n2), a_max = Math.min(c1, n1);
        prob.computeProb(t);
        int nbTables = 0;
        for (ai = a_min; ai <= a_max; ai++) {
            b_min = Math.max(0, b0 - ai);
            b_max = Math.min(n1 - ai, c2);
            for (bi = b_min; bi <= b_max; bi++) probs[nbTables++] = prob.fastComputeProbForTableWithSameMarginsAsPreviousOne(ai, bi);
        }
        return nbTables;
    }

    /**
     * Enumerate all compatible tables : useless, just for benchmarks purposes
     * or for checking  {@link #computeNbOfCompatibleTables(int...)}
     *
     * can be used as a reference template too
     *
     * @param t the t
     *
     * @return the int
     */
    public int enumerateAllCompatibleTables(int... t) {
        final int a = t[0], b = t[1], c = t[2], d = t[3], e = t[4], f = t[5];
        int b_min = 0, b_max = 0;
        @SuppressWarnings("unused") int ai, bi, ci, di, ei, fi;
        final int c1 = a + d, c2 = b + e, c3 = c + f;
        final int n1 = a + b + c, n2 = d + e + f, n = n1 + n2;
        final int b0 = c2 + n1 + c1 - n;
        final int a_min = Math.max(0, c1 - n2), a_max = Math.min(c1, n1);
        int nbTables = 0;
        for (ai = a_min; ai <= a_max; ai++) {
            b_min = Math.max(0, b0 - ai);
            b_max = Math.min(n1 - ai, c2);
            di = c1 - ai;
            for (bi = b_min; bi <= b_max; bi++) {
                ++nbTables;
                ci = n1 - ai - bi;
                ei = c2 - bi;
                fi = c3 - ci;
            }
        }
        return nbTables;
    }
}
