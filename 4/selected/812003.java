package jmetal.qualityIndicator;

import jmetal.core.Problem;
import jmetal.core.SolutionSet;

/**
 * QualityIndicator class
 */
public class QualityIndicator {

    SolutionSet trueParetoFront_;

    double trueParetoFrontHypervolume_;

    Problem problem_;

    jmetal.qualityIndicator.util.MetricsUtil utilities_;

    /**
   * Constructor
   * @param paretoFrontFile
   */
    public QualityIndicator(Problem problem, String paretoFrontFile) {
        problem_ = problem;
        utilities_ = new jmetal.qualityIndicator.util.MetricsUtil();
        trueParetoFront_ = utilities_.readNonDominatedSolutionSet(paretoFrontFile);
        trueParetoFrontHypervolume_ = new Hypervolume().hypervolume(trueParetoFront_.writeObjectivesToMatrix(), trueParetoFront_.writeObjectivesToMatrix(), problem_.getNumberOfObjectives());
    }

    /**
   * Returns the hypervolume of solution set
   * @param solutionSet
   * @return The value of the hypervolume indicator
   */
    public double getHypervolume(SolutionSet solutionSet) {
        return new Hypervolume().hypervolume(solutionSet.writeObjectivesToMatrix(), trueParetoFront_.writeObjectivesToMatrix(), problem_.getNumberOfObjectives());
    }

    /**
   * Returns the hypervolume of the true Pareto front
   * @return The hypervolume of the true Pareto front
   */
    public double getTrueParetoFrontHypervolume() {
        return trueParetoFrontHypervolume_;
    }

    /**
   * Returns the inverted generational distance of solution set
   * @param solutionSet
   * @return The value of the hypervolume indicator
   */
    public double getIGD(SolutionSet solutionSet) {
        return new InvertedGenerationalDistance().invertedGenerationalDistance(solutionSet.writeObjectivesToMatrix(), trueParetoFront_.writeObjectivesToMatrix(), problem_.getNumberOfObjectives());
    }

    /**
   * Returns the generational distance of solution set
   * @param solutionSet
   * @return The value of the hypervolume indicator
   */
    public double getGD(SolutionSet solutionSet) {
        return new GenerationalDistance().generationalDistance(solutionSet.writeObjectivesToMatrix(), trueParetoFront_.writeObjectivesToMatrix(), problem_.getNumberOfObjectives());
    }

    /**
   * Returns the spread of solution set
   * @param solutionSet
   * @return The value of the hypervolume indicator
   */
    public double getSpread(SolutionSet solutionSet) {
        return new Spread().spread(solutionSet.writeObjectivesToMatrix(), trueParetoFront_.writeObjectivesToMatrix(), problem_.getNumberOfObjectives());
    }

    /**
   * Returns the epsilon indicator of solution set
   * @param solutionSet
   * @return The value of the hypervolume indicator
   */
    public double getEpsilon(SolutionSet solutionSet) {
        return new Epsilon().epsilon(solutionSet.writeObjectivesToMatrix(), trueParetoFront_.writeObjectivesToMatrix(), problem_.getNumberOfObjectives());
    }
}
