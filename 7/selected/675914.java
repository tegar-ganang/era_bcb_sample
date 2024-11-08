package solver.genetic;

import org.jgap.Configuration;
import org.jgap.Gene;
import org.jgap.IUniversalRateCalculator;
import org.jgap.InvalidConfigurationException;
import org.jgap.RandomGenerator;
import org.jgap.impl.SwappingMutationOperator;

/**
 * This MutationOperator cuts a single gene from a chromosome and inserts it
 * at an other position in the chromosome.
 */
public class InsertMutationOperator extends SwappingMutationOperator {

    /**
     * Constructs a new instance of this operator.
     * 
     * <br/>Attention: The configuration used is the one set with the static
     * method Genotype.setConfiguration.
     * 
     * @throws InvalidConfigurationException
     * 
     */
    public InsertMutationOperator() throws InvalidConfigurationException {
        super();
    }

    /**
     * 
     * @param a_config
     *                the configuration to use
     * @throws InvalidConfigurationException
     */
    public InsertMutationOperator(final Configuration a_config) throws InvalidConfigurationException {
        super(a_config);
    }

    /**
     * Constructs a new instance of this operator with a specified mutation rate
     * calculator, which results in dynamic mutation being turned on.
     * 
     * @param a_config
     *                the configuration to use
     * @param a_mutationRateCalculator
     *                calculator for dynamic mutation rate computation
     * @throws InvalidConfigurationException
     */
    public InsertMutationOperator(final Configuration a_config, final IUniversalRateCalculator a_mutationRateCalculator) throws InvalidConfigurationException {
        super(a_config, a_mutationRateCalculator);
    }

    /**
     * Constructs a new instance of this MutationOperator with the given
     * mutation rate.
     * 
     * @param a_config
     *                the configuration to use
     * @param a_desiredMutationRate
     *                desired rate of mutation, expressed as the denominator of
     *                the 1 / X fraction. For example, 1000 would result in
     *                1/1000 genes being mutated on average. A mutation rate of
     *                zero disables mutation entirely
     * @throws InvalidConfigurationException
     */
    public InsertMutationOperator(final Configuration a_config, final int a_desiredMutationRate) throws InvalidConfigurationException {
        super(a_config, a_desiredMutationRate);
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    protected Gene[] operate(final RandomGenerator a_generator, final int cutHere, final Gene[] a_genes) {
        int insertHere;
        do {
            insertHere = a_generator.nextInt(a_genes.length);
        } while (insertHere == cutHere);
        Gene g = a_genes[cutHere];
        if (insertHere < cutHere) {
            this.shiftToRight(a_genes, cutHere, insertHere);
        } else {
            this.shiftToLeft(a_genes, cutHere, insertHere);
        }
        a_genes[insertHere] = g;
        return a_genes;
    }

    /**
     * Shifts all genes in the array beginning at begin and ending at end one
     * position to the right.
     * 
     * @param genes
     *                the genes to shift
     * @param begin
     *                begin of genes to shift
     * @param end
     *                end of genes to shift
     */
    private void shiftToRight(final Gene[] genes, final int begin, final int end) {
        for (int i = begin; i > end; i--) {
            genes[i] = genes[i - 1];
        }
    }

    /**
     * Shifts all genes in the array beginning at begin and ending at end one
     * position to the left.
     * 
     * @param genes
     *                the genes to shift
     * @param begin
     *                begin of genes to shift
     * @param end
     *                end of genes to shift
     */
    private void shiftToLeft(final Gene[] genes, final int begin, final int end) {
        for (int i = begin; i < end; i++) {
            genes[i] = genes[i + 1];
        }
    }
}
