package jmetal.metaheuristics.abyss;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import jmetal.base.*;
import jmetal.base.archive.CrowdingArchive;
import jmetal.base.Algorithm;
import java.util.*;
import jmetal.util.*;
import jmetal.base.operator.localSearch.LocalSearch;
import jmetal.qualityIndicator.QualityIndicator;

/**
 * This class implements the AbYSS algorithm. This algorithm is an adaptation
 * of the single-objective scatter search template defined by F. Glover in:
 * F. Glover. "A template for scatter search and path relinking", Lecture Notes 
 * in Computer Science, Springer Verlag, 1997.
 */
public class AbYSSDE extends Algorithm {

    /**
   * Stores the problem to solve
   */
    private Problem problem_;

    QualityIndicator indicators;

    int requiredEvaluations = 0;

    FileOutputStream fos_;

    OutputStreamWriter osw_;

    BufferedWriter bw_;

    /**
   * Stores the number of subranges in which each variable is divided. Used in
   * the diversification method. By default it takes the value 4 (see the method
   * <code>initParams</code>).
   */
    int numberOfSubranges_;

    /**
   * These variables are used in the diversification method.
   */
    int[] sumOfFrequencyValues_;

    int[] sumOfReverseFrequencyValues_;

    int[][] frequency_;

    int[][] reverseFrequency_;

    /**
   * Stores the initial solution set
   */
    private SolutionSet solutionSet_;

    /**
   * Stores the external solution archive
   */
    private CrowdingArchive archive_;

    /**
   * Stores the reference set one
   */
    private SolutionSet refSet1_;

    /**
   * Stores the reference set two
   */
    private SolutionSet refSet2_;

    /**
   * Stores the solutions provided by the subset generation method of the
   * scatter search template
   */
    private SolutionSet subSet_;

    /**
   * Maximum number of solution allowed for the initial solution set
   */
    private int solutionSetSize_;

    /**
   * Maximum size of the external archive
   */
    private int archiveSize_;

    /** 
   * Maximum size of the reference set one
   */
    private int refSet1Size_;

    /**
   * Maximum size of the reference set two
   */
    private int refSet2Size_;

    /**
   * Maximum number of getEvaluations to carry out
   */
    private int maxEvaluations;

    /**
   * Stores the current number of performed getEvaluations
   */
    private int evaluations_;

    /**
   * Stores the comparators for dominance and equality, respectively
   */
    private Comparator dominance_;

    private Comparator equal_;

    private Comparator fitness_;

    private Comparator crowdingDistance_;

    /**
   * Stores the crossover operator
   */
    private Operator crossoverOperator_;

    /**
   * Stores the improvement operator
   */
    private LocalSearch improvementOperator_;

    private Operator selectionOperator_;

    /**
   * Stores a <code>Distance</code> object
   */
    private Distance distance_;

    /**
   * Constructor.
   * @param problem Problem to solve
   */
    public AbYSSDE(Problem problem) {
        problem_ = problem;
        solutionSet_ = null;
        archive_ = null;
        refSet1_ = null;
        refSet2_ = null;
        subSet_ = null;
    }

    /**
   * Reads the parameter from the parameter list using the
   * <code>getInputParameter</code> method.
   */
    public void initParam() {
        solutionSetSize_ = ((Integer) getInputParameter("populationSize")).intValue();
        refSet1Size_ = ((Integer) getInputParameter("refSet1Size")).intValue();
        refSet2Size_ = ((Integer) getInputParameter("refSet2Size")).intValue();
        archiveSize_ = ((Integer) getInputParameter("archiveSize")).intValue();
        maxEvaluations = ((Integer) getInputParameter("maxEvaluations")).intValue();
        indicators = (QualityIndicator) getInputParameter("indicators");
        if (indicators == null) System.out.println("Error: Indicators = null");
        solutionSet_ = new SolutionSet(solutionSetSize_);
        archive_ = new CrowdingArchive(archiveSize_, problem_.getNumberOfObjectives());
        refSet1_ = new SolutionSet(refSet1Size_);
        refSet2_ = new SolutionSet(refSet2Size_);
        subSet_ = new SolutionSet(solutionSetSize_ * 1000);
        evaluations_ = 0;
        numberOfSubranges_ = 4;
        dominance_ = new jmetal.base.operator.comparator.DominanceComparator();
        equal_ = new jmetal.base.operator.comparator.EqualSolutions();
        fitness_ = new jmetal.base.operator.comparator.FitnessComparator();
        crowdingDistance_ = new jmetal.base.operator.comparator.CrowdingDistanceComparator();
        distance_ = new Distance();
        sumOfFrequencyValues_ = new int[problem_.getNumberOfVariables()];
        sumOfReverseFrequencyValues_ = new int[problem_.getNumberOfVariables()];
        frequency_ = new int[numberOfSubranges_][problem_.getNumberOfVariables()];
        reverseFrequency_ = new int[numberOfSubranges_][problem_.getNumberOfVariables()];
        crossoverOperator_ = operators_.get("crossover");
        improvementOperator_ = (LocalSearch) operators_.get("improvement");
        improvementOperator_.setParameter("archive", archive_);
        selectionOperator_ = operators_.get("selection");
    }

    /**
   * Returns a <code>Solution</code> using the diversification generation method
   * described in the scatter search template.
   * @throws JMException 
   */
    public Solution diversificationGeneration() throws JMException {
        Solution solution;
        solution = new Solution(problem_);
        double value;
        int range;
        for (int i = 0; i < problem_.getNumberOfVariables(); i++) {
            sumOfReverseFrequencyValues_[i] = 0;
            for (int j = 0; j < numberOfSubranges_; j++) {
                reverseFrequency_[j][i] = sumOfFrequencyValues_[i] - frequency_[j][i];
                sumOfReverseFrequencyValues_[i] += reverseFrequency_[j][i];
            }
            if (sumOfReverseFrequencyValues_[i] == 0) {
                range = PseudoRandom.randInt(0, numberOfSubranges_ - 1);
            } else {
                value = PseudoRandom.randInt(0, sumOfReverseFrequencyValues_[i] - 1);
                range = 0;
                while (value > reverseFrequency_[range][i]) {
                    value -= reverseFrequency_[range][i];
                    range++;
                }
            }
            frequency_[range][i]++;
            sumOfFrequencyValues_[i]++;
            double low = problem_.getLowerLimit(i) + range * (problem_.getUpperLimit(i) - problem_.getLowerLimit(i)) / numberOfSubranges_;
            double high = low + (problem_.getUpperLimit(i) - problem_.getLowerLimit(i)) / numberOfSubranges_;
            value = PseudoRandom.randDouble(low, high);
            solution.getDecisionVariables().variables_[i].setValue(value);
        }
        return solution;
    }

    /** 
   * Implements the referenceSetUpdate method.
   * @param build if true, indicates that the reference has to be build for the
   *        first time; if false, indicates that the reference set has to be
   *        updated with new solutions
   * @throws JMException 
   */
    public void referenceSetUpdate(boolean build) throws JMException {
        if (build) {
            Solution individual;
            (new Spea2Fitness(solutionSet_)).fitnessAssign();
            solutionSet_.sort(fitness_);
            for (int i = 0; i < refSet1Size_; i++) {
                individual = solutionSet_.get(0);
                solutionSet_.remove(0);
                individual.unMarked();
                refSet1_.add(individual);
            }
            for (int i = 0; i < solutionSet_.size(); i++) {
                individual = solutionSet_.get(i);
                individual.setDistanceToSolutionSet(distance_.distanceToSolutionSet(individual, refSet1_));
            }
            int size = refSet2Size_;
            if (solutionSet_.size() < refSet2Size_) {
                size = solutionSet_.size();
            }
            for (int i = 0; i < size; i++) {
                double maxMinimum = 0.0;
                int index = 0;
                for (int j = 0; j < solutionSet_.size(); j++) {
                    if (solutionSet_.get(j).getDistanceToSolutionSet() > maxMinimum) {
                        maxMinimum = solutionSet_.get(j).getDistanceToSolutionSet();
                        index = j;
                    }
                }
                individual = solutionSet_.get(index);
                solutionSet_.remove(index);
                for (int j = 0; j < solutionSet_.size(); j++) {
                    double aux = distance_.distanceBetweenSolutions(solutionSet_.get(j), individual);
                    if (aux < individual.getDistanceToSolutionSet()) {
                        solutionSet_.get(j).setDistanceToSolutionSet(aux);
                    }
                }
                refSet2_.add(individual);
                for (int j = 0; j < refSet2_.size(); j++) {
                    for (int k = 0; k < refSet2_.size(); k++) {
                        if (i != j) {
                            double aux = distance_.distanceBetweenSolutions(refSet2_.get(j), refSet2_.get(k));
                            if (aux < refSet2_.get(j).getDistanceToSolutionSet()) {
                                refSet2_.get(j).setDistanceToSolutionSet(aux);
                            }
                        }
                    }
                }
            }
        } else {
            Solution individual;
            for (int i = 0; i < subSet_.size(); i++) {
                individual = (Solution) improvementOperator_.execute(subSet_.get(i));
                evaluations_ += improvementOperator_.getEvaluations();
                if ((evaluations_ % 100) == 0) {
                    if ((indicators != null) && (requiredEvaluations == 0)) {
                        SolutionSet nonDominatedTmp = (new Ranking(archive_)).getSubfront(0);
                        try {
                            bw_.write(evaluations_ + "");
                            bw_.write(" ");
                            bw_.write(indicators.getParetoOptimalSolutions(nonDominatedTmp) + "");
                            bw_.write(" ");
                            bw_.write(indicators.getGD(nonDominatedTmp) + "");
                            bw_.write(" ");
                            bw_.write(indicators.getIGD(nonDominatedTmp) + "");
                            bw_.write(" ");
                            bw_.write(indicators.getEpsilon(nonDominatedTmp) + "");
                            bw_.write(" ");
                            bw_.write(indicators.getSpread(nonDominatedTmp) + "");
                            bw_.write(" ");
                            bw_.write(indicators.getHypervolume(nonDominatedTmp) + "");
                            bw_.newLine();
                        } catch (Exception e) {
                            Configuration.logger_.severe("Error acceding to the file");
                            e.printStackTrace();
                        }
                    }
                }
                if (refSet1Test(individual)) {
                    for (int indSet2 = 0; indSet2 < refSet2_.size(); indSet2++) {
                        double aux = distance_.distanceBetweenSolutions(individual, refSet2_.get(indSet2));
                        if (aux < refSet2_.get(indSet2).getDistanceToSolutionSet()) {
                            refSet2_.get(indSet2).setDistanceToSolutionSet(aux);
                        }
                    }
                } else {
                    refSet2Test(individual);
                }
            }
            subSet_.clear();
        }
    }

    /** 
   * Tries to update the reference set 2 with a <code>Solution</code>
   * @param solution The <code>Solution</code>
   * @return true if the <code>Solution</code> has been inserted, false 
   * otherwise.
   * @throws JMException 
   */
    public boolean refSet2Test(Solution solution) throws JMException {
        if (refSet2_.size() < refSet2Size_) {
            solution.setDistanceToSolutionSet(distance_.distanceToSolutionSet(solution, refSet1_));
            double aux = distance_.distanceToSolutionSet(solution, refSet2_);
            if (aux < solution.getDistanceToSolutionSet()) {
                solution.setDistanceToSolutionSet(aux);
            }
            refSet2_.add(solution);
            return true;
        }
        solution.setDistanceToSolutionSet(distance_.distanceToSolutionSet(solution, refSet1_));
        double aux = distance_.distanceToSolutionSet(solution, refSet2_);
        if (aux < solution.getDistanceToSolutionSet()) {
            solution.setDistanceToSolutionSet(aux);
        }
        double peor = 0.0;
        int index = 0;
        for (int i = 0; i < refSet2_.size(); i++) {
            aux = refSet2_.get(i).getDistanceToSolutionSet();
            if (aux > peor) {
                peor = aux;
                index = i;
            }
        }
        if (solution.getDistanceToSolutionSet() < peor) {
            refSet2_.remove(index);
            for (int j = 0; j < refSet2_.size(); j++) {
                aux = distance_.distanceBetweenSolutions(refSet2_.get(j), solution);
                if (aux < refSet2_.get(j).getDistanceToSolutionSet()) {
                    refSet2_.get(j).setDistanceToSolutionSet(aux);
                }
            }
            solution.unMarked();
            refSet2_.add(solution);
            return true;
        }
        return false;
    }

    /** 
   * Tries to update the reference set one with a <code>Solution</code>.
   * @param solution The <code>Solution</code>
   * @return true if the <code>Solution</code> has been inserted, false
   * otherwise.
   */
    public boolean refSet1Test(Solution solution) {
        boolean dominated = false;
        int flag;
        int i = 0;
        while (i < refSet1_.size()) {
            flag = dominance_.compare(solution, refSet1_.get(i));
            if (flag == -1) {
                refSet1_.remove(i);
            } else if (flag == 1) {
                dominated = true;
                i++;
            } else {
                flag = equal_.compare(solution, refSet1_.get(i));
                if (flag == 0) {
                    return true;
                }
                i++;
            }
        }
        if (!dominated) {
            solution.unMarked();
            if (refSet1_.size() < refSet1Size_) {
                refSet1_.add(solution);
            } else {
                archive_.add(solution);
            }
        } else {
            return false;
        }
        return true;
    }

    /** 
   * Implements the subset generation method described in the scatter search
   * template
   * @return  Number of solutions created by the method
   * @throws JMException 
   */
    public int subSetGeneration() throws JMException {
        Solution[] parents = new Solution[2];
        Solution[] offSpring;
        subSet_.clear();
        for (int i = 0; i < refSet1_.size(); i++) {
            parents[0] = refSet1_.get(i);
            for (int j = i + 1; j < refSet1_.size(); j++) {
                parents[1] = refSet1_.get(j);
                if (!parents[0].isMarked() || !parents[1].isMarked()) {
                    Solution[] DEparent1;
                    Solution[] DEparent2;
                    if (refSet1_.size() > 4) {
                        DEparent1 = (Solution[]) selectionOperator_.execute(new Object[] { refSet1_, i });
                        DEparent2 = (Solution[]) selectionOperator_.execute(new Object[] { refSet1_, j });
                    } else if (archive_.size() > 4) {
                        DEparent1 = (Solution[]) selectionOperator_.execute(new Object[] { archive_, i });
                        DEparent2 = (Solution[]) selectionOperator_.execute(new Object[] { archive_, j });
                    } else {
                        DEparent1 = new Solution[4];
                        DEparent1[0] = refSet1_.get(i);
                        DEparent1[1] = new Solution(problem_);
                        DEparent1[2] = new Solution(problem_);
                        DEparent1[3] = new Solution(problem_);
                        DEparent2 = new Solution[4];
                        DEparent2[0] = refSet1_.get(j);
                        DEparent2[1] = new Solution(problem_);
                        DEparent2[2] = new Solution(problem_);
                        DEparent2[3] = new Solution(problem_);
                    }
                    offSpring = new Solution[2];
                    offSpring[0] = (Solution) crossoverOperator_.execute(new Object[] { refSet1_.get(i), DEparent1 });
                    offSpring[1] = (Solution) crossoverOperator_.execute(new Object[] { refSet1_.get(i), DEparent2 });
                    problem_.evaluate(offSpring[0]);
                    problem_.evaluate(offSpring[1]);
                    problem_.evaluateConstraints(offSpring[0]);
                    problem_.evaluateConstraints(offSpring[1]);
                    evaluations_++;
                    if ((evaluations_ % 100) == 0) {
                        if ((indicators != null) && (requiredEvaluations == 0)) {
                            SolutionSet nonDominatedTmp = (new Ranking(archive_)).getSubfront(0);
                            try {
                                bw_.write(evaluations_ + "");
                                bw_.write(" ");
                                bw_.write(indicators.getParetoOptimalSolutions(nonDominatedTmp) + "");
                                bw_.write(" ");
                                bw_.write(indicators.getGD(nonDominatedTmp) + "");
                                bw_.write(" ");
                                bw_.write(indicators.getIGD(nonDominatedTmp) + "");
                                bw_.write(" ");
                                bw_.write(indicators.getEpsilon(nonDominatedTmp) + "");
                                bw_.write(" ");
                                bw_.write(indicators.getSpread(nonDominatedTmp) + "");
                                bw_.write(" ");
                                bw_.write(indicators.getHypervolume(nonDominatedTmp) + "");
                                bw_.newLine();
                            } catch (Exception e) {
                                Configuration.logger_.severe("Error acceding to the file");
                                e.printStackTrace();
                            }
                        }
                    }
                    evaluations_++;
                    if ((evaluations_ % 100) == 0) {
                        if ((indicators != null) && (requiredEvaluations == 0)) {
                            SolutionSet nonDominatedTmp = (new Ranking(archive_)).getSubfront(0);
                            try {
                                bw_.write(evaluations_ + "");
                                bw_.write(" ");
                                bw_.write(indicators.getParetoOptimalSolutions(nonDominatedTmp) + "");
                                bw_.write(" ");
                                bw_.write(indicators.getGD(nonDominatedTmp) + "");
                                bw_.write(" ");
                                bw_.write(indicators.getIGD(nonDominatedTmp) + "");
                                bw_.write(" ");
                                bw_.write(indicators.getEpsilon(nonDominatedTmp) + "");
                                bw_.write(" ");
                                bw_.write(indicators.getSpread(nonDominatedTmp) + "");
                                bw_.write(" ");
                                bw_.write(indicators.getHypervolume(nonDominatedTmp) + "");
                                bw_.newLine();
                            } catch (Exception e) {
                                Configuration.logger_.severe("Error acceding to the file");
                                e.printStackTrace();
                            }
                        }
                    }
                    if (evaluations_ < maxEvaluations) {
                        subSet_.add(offSpring[0]);
                        subSet_.add(offSpring[1]);
                    }
                    parents[0].marked();
                    parents[1].marked();
                }
            }
        }
        for (int i = 0; i < refSet2_.size(); i++) {
            parents[0] = refSet2_.get(i);
            for (int j = i + 1; j < refSet2_.size(); j++) {
                parents[1] = refSet2_.get(j);
                if (!parents[0].isMarked() || !parents[1].isMarked()) {
                    Solution[] DEparent1;
                    Solution[] DEparent2;
                    if (refSet2_.size() > 4) {
                        DEparent1 = (Solution[]) selectionOperator_.execute(new Object[] { refSet2_, i });
                        DEparent2 = (Solution[]) selectionOperator_.execute(new Object[] { refSet2_, j });
                    } else if (archive_.size() > 4) {
                        DEparent1 = (Solution[]) selectionOperator_.execute(new Object[] { archive_, i });
                        DEparent2 = (Solution[]) selectionOperator_.execute(new Object[] { archive_, j });
                    } else {
                        DEparent1 = new Solution[4];
                        DEparent1[0] = refSet2_.get(i);
                        DEparent1[1] = new Solution(problem_);
                        DEparent1[2] = new Solution(problem_);
                        DEparent1[3] = new Solution(problem_);
                        DEparent2 = new Solution[4];
                        DEparent2[0] = refSet2_.get(j);
                        DEparent2[1] = new Solution(problem_);
                        DEparent2[2] = new Solution(problem_);
                        DEparent2[3] = new Solution(problem_);
                    }
                    offSpring = new Solution[2];
                    offSpring[0] = (Solution) crossoverOperator_.execute(new Object[] { refSet2_.get(i), DEparent1 });
                    offSpring[1] = (Solution) crossoverOperator_.execute(new Object[] { refSet2_.get(i), DEparent2 });
                    problem_.evaluateConstraints(offSpring[0]);
                    problem_.evaluateConstraints(offSpring[1]);
                    problem_.evaluate(offSpring[0]);
                    problem_.evaluate(offSpring[1]);
                    evaluations_++;
                    if ((evaluations_ % 100) == 0) {
                        if ((indicators != null) && (requiredEvaluations == 0)) {
                            SolutionSet nonDominatedTmp = (new Ranking(archive_)).getSubfront(0);
                            try {
                                bw_.write(evaluations_ + "");
                                bw_.write(" ");
                                bw_.write(indicators.getParetoOptimalSolutions(nonDominatedTmp) + "");
                                bw_.write(" ");
                                bw_.write(indicators.getGD(nonDominatedTmp) + "");
                                bw_.write(" ");
                                bw_.write(indicators.getIGD(nonDominatedTmp) + "");
                                bw_.write(" ");
                                bw_.write(indicators.getEpsilon(nonDominatedTmp) + "");
                                bw_.write(" ");
                                bw_.write(indicators.getSpread(nonDominatedTmp) + "");
                                bw_.write(" ");
                                bw_.write(indicators.getHypervolume(nonDominatedTmp) + "");
                                bw_.newLine();
                            } catch (Exception e) {
                                Configuration.logger_.severe("Error acceding to the file");
                                e.printStackTrace();
                            }
                        }
                    }
                    evaluations_++;
                    if ((evaluations_ % 100) == 0) {
                        if ((indicators != null) && (requiredEvaluations == 0)) {
                            SolutionSet nonDominatedTmp = (new Ranking(archive_)).getSubfront(0);
                            try {
                                bw_.write(evaluations_ + "");
                                bw_.write(" ");
                                bw_.write(indicators.getParetoOptimalSolutions(nonDominatedTmp) + "");
                                bw_.write(" ");
                                bw_.write(indicators.getGD(nonDominatedTmp) + "");
                                bw_.write(" ");
                                bw_.write(indicators.getIGD(nonDominatedTmp) + "");
                                bw_.write(" ");
                                bw_.write(indicators.getEpsilon(nonDominatedTmp) + "");
                                bw_.write(" ");
                                bw_.write(indicators.getSpread(nonDominatedTmp) + "");
                                bw_.write(" ");
                                bw_.write(indicators.getHypervolume(nonDominatedTmp) + "");
                                bw_.newLine();
                            } catch (Exception e) {
                                Configuration.logger_.severe("Error acceding to the file");
                                e.printStackTrace();
                            }
                        }
                    }
                    if (evaluations_ < maxEvaluations) {
                        subSet_.add(offSpring[0]);
                        subSet_.add(offSpring[1]);
                    }
                    parents[0].marked();
                    parents[1].marked();
                }
            }
        }
        return subSet_.size();
    }

    /**   
  * Runs of the AbYSS algorithm.
  * @return a <code>SolutionSet</code> that is a set of non dominated solutions
  * as a result of the algorithm execution  
   * @throws JMException 
  */
    public SolutionSet execute() throws JMException {
        initParam();
        try {
            fos_ = new FileOutputStream("metrics" + ".AbYSS");
            osw_ = new OutputStreamWriter(fos_);
            bw_ = new BufferedWriter(osw_);
            Solution solution;
            for (int i = 0; i < solutionSetSize_; i++) {
                solution = diversificationGeneration();
                problem_.evaluateConstraints(solution);
                problem_.evaluate(solution);
                evaluations_++;
                if ((evaluations_ % 100) == 0) {
                    if ((indicators != null) && (requiredEvaluations == 0)) {
                        SolutionSet nonDominatedTmp = (new Ranking(archive_)).getSubfront(0);
                        try {
                            bw_.write(evaluations_ + "");
                            bw_.write(" ");
                            bw_.write(indicators.getParetoOptimalSolutions(nonDominatedTmp) + "");
                            bw_.write(" ");
                            bw_.write(indicators.getGD(nonDominatedTmp) + "");
                            bw_.write(" ");
                            bw_.write(indicators.getIGD(nonDominatedTmp) + "");
                            bw_.write(" ");
                            bw_.write(indicators.getEpsilon(nonDominatedTmp) + "");
                            bw_.write(" ");
                            bw_.write(indicators.getSpread(nonDominatedTmp) + "");
                            bw_.write(" ");
                            bw_.write(indicators.getHypervolume(nonDominatedTmp) + "");
                            bw_.newLine();
                        } catch (Exception e) {
                            Configuration.logger_.severe("Error acceding to the file");
                            e.printStackTrace();
                        }
                    }
                }
                solution = (Solution) improvementOperator_.execute(solution);
                evaluations_ += improvementOperator_.getEvaluations();
                if ((evaluations_ % 100) == 0) {
                    if ((indicators != null) && (requiredEvaluations == 0)) {
                        SolutionSet nonDominatedTmp = (new Ranking(archive_)).getSubfront(0);
                        try {
                            bw_.write(evaluations_ + "");
                            bw_.write(" ");
                            bw_.write(indicators.getParetoOptimalSolutions(nonDominatedTmp) + "");
                            bw_.write(" ");
                            bw_.write(indicators.getGD(nonDominatedTmp) + "");
                            bw_.write(" ");
                            bw_.write(indicators.getIGD(nonDominatedTmp) + "");
                            bw_.write(" ");
                            bw_.write(indicators.getEpsilon(nonDominatedTmp) + "");
                            bw_.write(" ");
                            bw_.write(indicators.getSpread(nonDominatedTmp) + "");
                            bw_.write(" ");
                            bw_.write(indicators.getHypervolume(nonDominatedTmp) + "");
                            bw_.newLine();
                        } catch (Exception e) {
                            Configuration.logger_.severe("Error acceding to the file");
                            e.printStackTrace();
                        }
                    }
                }
                solutionSet_.add(solution);
            }
            int newSolutions = 0;
            while (evaluations_ < maxEvaluations) {
                referenceSetUpdate(true);
                newSolutions = subSetGeneration();
                while (newSolutions > 0) {
                    referenceSetUpdate(false);
                    if (evaluations_ >= maxEvaluations) {
                        bw_.close();
                        return archive_;
                    }
                    newSolutions = subSetGeneration();
                }
                if (evaluations_ < maxEvaluations) {
                    solutionSet_.clear();
                    for (int i = 0; i < refSet1_.size(); i++) {
                        solution = refSet1_.get(i);
                        solution.unMarked();
                        solution = (Solution) improvementOperator_.execute(solution);
                        evaluations_ += improvementOperator_.getEvaluations();
                        if ((evaluations_ % 100) == 0) {
                            if ((indicators != null) && (requiredEvaluations == 0)) {
                                SolutionSet nonDominatedTmp = (new Ranking(archive_)).getSubfront(0);
                                try {
                                    bw_.write(evaluations_ + "");
                                    bw_.write(" ");
                                    bw_.write(indicators.getParetoOptimalSolutions(nonDominatedTmp) + "");
                                    bw_.write(" ");
                                    bw_.write(indicators.getGD(nonDominatedTmp) + "");
                                    bw_.write(" ");
                                    bw_.write(indicators.getIGD(nonDominatedTmp) + "");
                                    bw_.write(" ");
                                    bw_.write(indicators.getEpsilon(nonDominatedTmp) + "");
                                    bw_.write(" ");
                                    bw_.write(indicators.getSpread(nonDominatedTmp) + "");
                                    bw_.write(" ");
                                    bw_.write(indicators.getHypervolume(nonDominatedTmp) + "");
                                    bw_.newLine();
                                } catch (Exception e) {
                                    Configuration.logger_.severe("Error acceding to the file");
                                    e.printStackTrace();
                                }
                            }
                        }
                        solutionSet_.add(solution);
                    }
                    refSet1_.clear();
                    refSet2_.clear();
                    distance_.crowdingDistanceAssignment(archive_, problem_.getNumberOfObjectives());
                    archive_.sort(crowdingDistance_);
                    int insert = solutionSetSize_ / 2;
                    if (insert > archive_.size()) insert = archive_.size();
                    if (insert > (solutionSetSize_ - solutionSet_.size())) insert = solutionSetSize_ - solutionSet_.size();
                    for (int i = 0; i < insert; i++) {
                        solution = new Solution(archive_.get(i));
                        solution.unMarked();
                        solutionSet_.add(solution);
                    }
                    while (solutionSet_.size() < solutionSetSize_) {
                        solution = diversificationGeneration();
                        problem_.evaluateConstraints(solution);
                        problem_.evaluate(solution);
                        evaluations_++;
                        if ((evaluations_ % 100) == 0) {
                            if ((indicators != null) && (requiredEvaluations == 0)) {
                                SolutionSet nonDominatedTmp = (new Ranking(archive_)).getSubfront(0);
                                try {
                                    bw_.write(evaluations_ + "");
                                    bw_.write(" ");
                                    bw_.write(indicators.getParetoOptimalSolutions(nonDominatedTmp) + "");
                                    bw_.write(" ");
                                    bw_.write(indicators.getGD(nonDominatedTmp) + "");
                                    bw_.write(" ");
                                    bw_.write(indicators.getIGD(nonDominatedTmp) + "");
                                    bw_.write(" ");
                                    bw_.write(indicators.getEpsilon(nonDominatedTmp) + "");
                                    bw_.write(" ");
                                    bw_.write(indicators.getSpread(nonDominatedTmp) + "");
                                    bw_.write(" ");
                                    bw_.write(indicators.getHypervolume(nonDominatedTmp) + "");
                                    bw_.newLine();
                                } catch (Exception e) {
                                    Configuration.logger_.severe("Error acceding to the file");
                                    e.printStackTrace();
                                }
                            }
                        }
                        solution = (Solution) improvementOperator_.execute(solution);
                        evaluations_ += improvementOperator_.getEvaluations();
                        if ((evaluations_ % 100) == 0) {
                            if ((indicators != null) && (requiredEvaluations == 0)) {
                                SolutionSet nonDominatedTmp = (new Ranking(archive_)).getSubfront(0);
                                try {
                                    bw_.write(evaluations_ + "");
                                    bw_.write(" ");
                                    bw_.write(indicators.getParetoOptimalSolutions(nonDominatedTmp) + "");
                                    bw_.write(" ");
                                    bw_.write(indicators.getGD(nonDominatedTmp) + "");
                                    bw_.write(" ");
                                    bw_.write(indicators.getIGD(nonDominatedTmp) + "");
                                    bw_.write(" ");
                                    bw_.write(indicators.getEpsilon(nonDominatedTmp) + "");
                                    bw_.write(" ");
                                    bw_.write(indicators.getSpread(nonDominatedTmp) + "");
                                    bw_.write(" ");
                                    bw_.write(indicators.getHypervolume(nonDominatedTmp) + "");
                                    bw_.newLine();
                                } catch (Exception e) {
                                    Configuration.logger_.severe("Error acceding to the file");
                                    e.printStackTrace();
                                }
                            }
                        }
                        solution.unMarked();
                        solutionSet_.add(solution);
                    }
                }
            }
            bw_.close();
        } catch (IOException e) {
            Configuration.logger_.severe("Error acceding to the file");
            e.printStackTrace();
        }
        return archive_;
    }
}
