package jenes.tutorials.problem3;

import jenes.utils.Random;
import jenes.chromosome.IntegerChromosome;
import jenes.population.Individual;
import jenes.stage.operator.Mutator;

/**
 * Tutorial showing how to implement problem specific operators.
 * The problem faced in this example is the well known Tavel Salesman Problem (TSP)
 *
 * This class implements a specific mutations aimed at preserving permutations.
 *
 * Algorithm description:
 * Two random indexes, i1 and i2, are choosed; the order of the elements within the
 * range [i1,i2] changes randomly. For example:
 * <pre>
 *       i1=0; i2=3
 *       position:    0 1 2 3 4 5
 *	 start_chrom: 5 2 1 4 6 3
 *       end_chrom:   2 5 4 1 6 3
 * </pre>
 *
 * @version 2.0
 * @since 1.0
 */
public class TSPScrambleMutator extends Mutator<IntegerChromosome> {

    public TSPScrambleMutator(double pMut) {
        super(pMut);
    }

    @Override
    protected void mutate(Individual<IntegerChromosome> t) {
        int size = t.getChromosomeLength();
        int index1, index2;
        do {
            index1 = Random.getInstance().nextInt(0, size);
            index2 = Random.getInstance().nextInt(0, size);
        } while (index2 == index1);
        int min, max;
        if (index1 < index2) {
            min = index1;
            max = index2;
        } else {
            min = index2;
            max = index1;
        }
        randomize(t.getChromosome(), min, max);
    }

    /**
     * Randomizes the elements chromosome within the range [min,max]
     * <p>
     * @param chrom the individual to mutate
     * @param min the lower bound
     * @param max the upper bound
     */
    public void randomize(IntegerChromosome chrom, int min, int max) {
        int len = max - min + 1;
        int[] base = new int[len];
        for (int i = 0; i < len; i++) base[i] = chrom.getValue(min + i);
        for (int i = 0; len > 0; --len, ++i) {
            int pos = Random.getInstance().nextInt(0, len);
            chrom.setValue(min + i, base[pos]);
            for (int j = pos; j < (len - 1); j++) {
                base[j] = base[j + 1];
            }
        }
    }
}
