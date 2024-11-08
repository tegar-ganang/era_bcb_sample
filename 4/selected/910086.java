package ufpr.mestrado.ais.qualityIndicator;

import jmetal.base.Problem;
import jmetal.base.SolutionSet;
import jmetal.qualityIndicator.Epsilon;
import jmetal.qualityIndicator.GenerationalDistance;
import jmetal.qualityIndicator.Hypervolume;
import jmetal.qualityIndicator.InvertedGenerationalDistance;
import jmetal.qualityIndicator.Spread;
import jmetal.qualityIndicator.util.MetricsUtil;

/**
 * QualityIndicatorMNO class
 */
public class QualityIndicatorMNO {

    SolutionSet trueParetoFront_;

    Problem problem_;

    MetricsUtil utilities_;

    /**
	 * Constructor
	 * 
	 * @param paretoFrontFile
	 */
    public QualityIndicatorMNO(Problem problem, String paretoFrontFile) {
        problem_ = problem;
        utilities_ = new MetricsUtil();
        trueParetoFront_ = utilities_.readNonDominatedSolutionSet(paretoFrontFile);
    }

    /**
	 * Returns the hypervolume of solution set
	 * 
	 * @param solutionSet
	 * @return The value of the hypervolume indicator
	 */
    public double getHypervolume(SolutionSet solutionSet) {
        return new Hypervolume().hypervolume(solutionSet.writeObjectivesToMatrix(), trueParetoFront_.writeObjectivesToMatrix(), problem_.getNumberOfObjectives());
    }

    /**
	 * Returns the inverted generational distance of solution set
	 * 
	 * @param solutionSet
	 * @return The value of the hypervolume indicator
	 */
    public double getIGD(SolutionSet solutionSet) {
        return new InvertedGenerationalDistance().invertedGenerationalDistance(solutionSet.writeObjectivesToMatrix(), trueParetoFront_.writeObjectivesToMatrix(), problem_.getNumberOfObjectives());
    }

    /**
	 * Returns the generational distance of solution set
	 * 
	 * @param solutionSet
	 * @return The value of the hypervolume indicator
	 */
    public double getGD(SolutionSet solutionSet) {
        return new GenerationalDistance().generationalDistance(solutionSet.writeObjectivesToMatrix(), trueParetoFront_.writeObjectivesToMatrix(), problem_.getNumberOfObjectives());
    }

    /**
	 * Returns the spread of solution set
	 * 
	 * @param solutionSet
	 * @return The value of the hypervolume indicator
	 */
    public double getSpread(SolutionSet solutionSet) {
        return new Spread().spread(solutionSet.writeObjectivesToMatrix(), trueParetoFront_.writeObjectivesToMatrix(), problem_.getNumberOfObjectives());
    }

    /**
	 * Returns the epsilon indicator of solution set
	 * 
	 * @param solutionSet
	 * @return The value of the hypervolume indicator
	 */
    public double getEpsilon(SolutionSet solutionSet) {
        return new Epsilon().epsilon(solutionSet.writeObjectivesToMatrix(), trueParetoFront_.writeObjectivesToMatrix(), problem_.getNumberOfObjectives());
    }
}
