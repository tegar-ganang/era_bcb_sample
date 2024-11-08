package uk.ac.shef.wit.aleph.algorithm.graphlab.util;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.Matrices;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;
import uk.ac.shef.wit.aleph.DenseVector;

/**
 * Implements an absorbing random walk model.
 *
 * It is described in the paper:
 * {@code X. Zhu et al. Improving diversity in ranking using absorbing random walks. NAACL-HLT, 2007. }
 *
 * This absorbing random walk model is the basis of a ranking algorithm that is similar to PageRank but
 * encourages diversity in top ranked items, by turning already ranked items into absorbing states to
 * penalize remaining similar items. It also has several other applications.
 *
 * @author Jose' Iria, NLP Group, University of Sheffield
 *         (<a  href="mailto:J.Iria@dcs.shef.ac.uk" >email</a>)
 */
public class AbsorbingRandomWalk {

    private static final double CONVERGENCE_ERROR = 0.0001;

    public int[] rank(final Matrix similarities, final Vector prior, final double tradeoff) {
        assert similarities.isSquare() : "matrix needs to be square";
        final Matrix mP = prepareMarkovChain(similarities, prior, tradeoff);
        Vector stationary = initStationaryVector(similarities.numRows());
        stationary = doRandomWalk(mP, stationary);
        int highest = findHighestRanked(stationary);
        return doAbsorbingRandowWalk(mP, highest);
    }

    private Matrix prepareMarkovChain(final Matrix similarities, final Vector prior, final double tradeoff) {
        final Matrix mP = similarities.copy();
        for (int i = 0, size = mP.numRows(); i < size; ++i) {
            double sum = 0.0;
            for (int j = 0; j < size; ++j) sum += mP.get(i, j);
            for (int j = 0; j < size; ++j) mP.set(i, j, tradeoff * (mP.get(i, j) / sum) + (1.0 - tradeoff) * prior.get(j));
        }
        return mP;
    }

    private Vector initStationaryVector(final int size) {
        final Vector stationary = new DenseVector(size);
        for (int i = 0; i < size; ++i) stationary.set(i, 1.0 / size);
        return stationary;
    }

    private int findHighestRanked(final Vector v) {
        int highest = 0;
        for (int i = 1, size = v.size(); i < size; ++i) if (v.get(i) > v.get(highest)) highest = i;
        return highest;
    }

    private Vector doRandomWalk(final Matrix mP, Vector stationary) {
        boolean converged;
        do {
            final Vector update = new DenseVector(mP.numRows());
            mP.mult(stationary, update);
            converged = CONVERGENCE_ERROR > update.add(-1.0, stationary).norm(Vector.Norm.One);
            stationary = update;
        } while (!converged);
        return stationary;
    }

    private int[] doAbsorbingRandowWalk(final Matrix mP, int highest) {
        final int size = mP.numRows();
        final int[] ranking = new int[size];
        ranking[0] = highest;
        final int[] map = new int[size];
        for (int i = 0; i < highest; ++i) map[i] = i;
        for (int i = highest; i < size - 1; ++i) map[i] = i + 1;
        for (int absorbed = 1; absorbed < size; ++absorbed) {
            System.out.println("absorbed " + absorbed);
            final int sizeOfQ = size - absorbed;
            final Matrix mIminusQ = new DenseMatrix(sizeOfQ, sizeOfQ);
            for (int i = 0; i < sizeOfQ; ++i) for (int j = 0; j < sizeOfQ; ++j) mIminusQ.set(i, j, (i == j ? 1.0 : 0.0) - mP.get(map[i], map[j]));
            final Matrix mI = Matrices.identity(sizeOfQ);
            final Matrix mN = mI.copy();
            mIminusQ.solve(mI, mN);
            final Vector v = new DenseVector(sizeOfQ);
            for (int i = 0; i < sizeOfQ; ++i) {
                for (int j = 0; j < sizeOfQ; ++j) v.add(i, mN.get(i, j));
                v.set(i, v.get(i) / sizeOfQ);
            }
            highest = findHighestRanked(v);
            ranking[absorbed] = map[highest];
            for (int i = highest; i < size - 1; ++i) map[i] = map[i + 1];
        }
        return ranking;
    }

    public static void main(final String[] args) {
        final int testsize = 100;
        final Matrix similarities = new DenseMatrix(testsize, testsize);
        for (int i = 0; i < testsize; ++i) for (int j = 0; j < testsize; ++j) similarities.set(i, j, 100 * Math.random());
        final Vector prior = new DenseVector(testsize);
        final int[] ranking = new AbsorbingRandomWalk().rank(similarities, prior, 1.0);
        for (final int node : ranking) System.out.println("node = " + node);
    }
}
