package jenes.tutorials.ga3;

import jenes.Random;
import jenes.chromosome.Chromosome;
import jenes.chromosome.IntegerChromosome;
import jenes.population.Individual;
import jenes.stage.operator.Mutator;

/**Algorithm description:
 * Two random indexes, i1 and i2, are choosed; the order of the elements within the
 * range [i1,i2] changes randomly.
 *
 * e.g.
 * i1=0; i2=3
 * 				position:	 0 1 2 3 4 5
 *				start_chrom: 5 2 1 4 6 3
 *  			end_chrom:   2 5 4 1 6 3
 */
public class TSPScrambleMutator<T extends Chromosome> extends Mutator<T> {

    public TSPScrambleMutator(double pMut) {
        super(pMut);
    }

    @Override
    protected void mutate(Individual<T> t) {
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
        randomize(t, min, max);
    }

    /**
     * Randomizes the elements chromosome within the range [min,max]
     * <p>
     * @param c the individual to mutate
     * @param min the lower bound
     * @param max the upper bound
     */
    public void randomize(Individual<T> c, int min, int max) {
        IntegerChromosome chrom = (IntegerChromosome) c.getChromosome();
        int len = max - min + 1;
        int[] base = new int[len];
        for (int i = 0; i < len; i++) base[i] = chrom.getValue(min + i);
        int i = 0;
        while (len != 0) {
            int pos = Random.getInstance().nextInt(0, len);
            chrom.setValue(min + i, base[pos]);
            for (int j = pos; j < (len - 1); j++) {
                base[j] = base[j + 1];
            }
            len--;
            i++;
        }
    }
}
