package jbnc.util;

import BayesianNetworks.DiscreteVariable;
import BayesianNetworks.ProbabilityFunction;

final class FrequencyNode {

    /**  Description of the Field */
    public int[] nJ = null;

    /**  Description of the Field */
    public int[][] nJK = null;

    protected int id = -1;

    protected int[] parents = null;

    protected int[] parentMult = null;

    /**
   *  Constructor for the FrequencyNode object
   *
   * @param  func  Description of Parameter
   */
    public FrequencyNode(ProbabilityFunction func) {
        DiscreteVariable[] vars = func.get_variables();
        id = vars[0].get_index();
        int maxK = vars[0].number_values();
        if (vars.length > 1) {
            parents = new int[vars.length - 1];
            int maxJ = 1;
            for (int p = 0; p < parents.length; ++p) {
                parents[p] = vars[p + 1].get_index();
                maxJ *= vars[p + 1].number_values();
            }
            parentMult = new int[vars.length - 1];
            nJ = new int[maxJ];
            nJK = new int[maxJ][maxK];
            for (int p = 0; p < parentMult.length; ++p) {
                maxJ /= vars[p + 1].number_values();
                parentMult[p] = maxJ;
            }
        } else {
            parents = null;
            parentMult = null;
            nJ = new int[1];
            nJK = new int[1][maxK];
        }
    }

    /**
   *  Adds a feature to the Case attribute of the FrequencyNode object
   *
   * @param  aCase  The feature to be added to the Case attribute
   */
    public void addCase(int[] aCase) {
        int j = 0;
        if (parents != null) {
            for (int p = 0; p < parents.length; ++p) {
                j += aCase[parents[p]] * parentMult[p];
            }
        }
        ++nJ[j];
        ++nJK[j][aCase[id]];
    }

    /**
   *  Description of the Method
   *
   * @param  aCase  Description of Parameter
   */
    public void removeCase(int[] aCase) {
        int j = 0;
        if (parents != null) {
            for (int p = 0; p < parents.length; ++p) {
                j += aCase[parents[p]] * parentMult[p];
            }
        }
        --nJ[j];
        --nJK[j][aCase[id]];
    }

    /**  Description of the Method */
    public void removeAllCases() {
        for (int j = 0; j < nJ.length; ++j) {
            nJ[j] = 0;
            int[] njK = nJK[j];
            for (int k = 0; k < njK.length; ++k) {
                njK[k] = 0;
            }
        }
    }

    /**
   *  Description of the Method
   *
   * @param  vals       Description of Parameter
   * @param  usePriors  Description of Parameter
   * @param  alphaK     Description of Parameter
   */
    public final void learnNodeParam(double[] vals, boolean usePriors, double alphaK) {
        int maxK = nJK[0].length;
        if (usePriors) {
            double alpha = alphaK * maxK;
            for (int j = 0; j < nJ.length; ++j) {
                double denom = alpha + nJ[j];
                int[] nJKj = nJK[j];
                int index = j;
                for (int k = 0; k < maxK; ++k) {
                    vals[index] = (nJKj[k] + alphaK) / denom;
                    index += nJ.length;
                }
            }
        } else {
            double beta_ij = jbnc.util.BNTools.beta_ijk * maxK;
            for (int j = 0; j < nJ.length; ++j) {
                double denom = beta_ij + nJ[j];
                int[] nJKj = nJK[j];
                int index = j;
                for (int k = 0; k < maxK; ++k) {
                    vals[index] = (nJKj[k] + jbnc.util.BNTools.beta_ijk) / denom;
                    index += nJ.length;
                }
            }
        }
    }
}
