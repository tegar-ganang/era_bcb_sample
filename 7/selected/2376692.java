package org.systemsbiology.chem;

import org.systemsbiology.math.*;
import org.systemsbiology.util.*;

/**
 * Simulates the dynamics of a set of coupled chemical reactions
 * described by {@link Reaction} objects using the Sorting stochastic
 * method, "direct method".
 *
 * @author Stephen Ramsey
 */
public final class SimulatorStochasticSDM extends SimulatorStochasticBase implements IAliasableClass, ISimulator {

    public static final String CLASS_ALIAS = "sorting-direct";

    private static final long NUMBER_FIRINGS = 1;

    private int[] orderSearch;

    private Object[] mReactionDependencies;

    private int lastReactionOrderSearch = 1;

    private DependencyGraphCreator dependcreat;

    private boolean deadlock;

    protected void prepareForStochasticSimulation(double pStartTime, SimulatorParameters pSimulatorParameters) {
        deadlock = false;
    }

    protected final int chooseIndexOfNextOrderedReaction(double pAggregateReactionProbabilityDensity) throws IllegalArgumentException {
        double randomNumberUniformInterval = getRandomNumberUniformInterval(mRandomNumberGenerator);
        double cumulativeReactionProbabilityDensity = 0.0;
        double fractionOfAggregateReactionProbabilityDensity = randomNumberUniformInterval * pAggregateReactionProbabilityDensity;
        if (pAggregateReactionProbabilityDensity <= 0.0) {
            throw new IllegalArgumentException("invalid aggregate reaction probability density: " + pAggregateReactionProbabilityDensity);
        }
        int numReactions = mReactions.length;
        int reactionIndex = -1;
        for (int reactionCtr = numReactions - 1; reactionCtr >= 0; --reactionCtr) {
            double reactionProbability = mReactionProbabilities[orderSearch[reactionCtr]];
            cumulativeReactionProbabilityDensity += reactionProbability;
            if (cumulativeReactionProbabilityDensity >= fractionOfAggregateReactionProbabilityDensity) {
                reactionIndex = orderSearch[reactionCtr];
                lastReactionOrderSearch = reactionCtr;
                actualaverageSearchDepthArray = numReactions - reactionCtr;
                break;
            }
        }
        return (reactionIndex);
    }

    protected double iterate(MutableInteger pLastReactionIndex) throws DataNotFoundException, IllegalStateException {
        deadlock = false;
        double time = mSymbolEvaluator.getTime();
        int lastReactionIndex = pLastReactionIndex.getValue();
        if (NULL_REACTION != lastReactionIndex) {
            updateSymbolValuesForReaction(lastReactionIndex, mDynamicSymbolValues, mDynamicSymbolDelayedReactionAssociations, NUMBER_FIRINGS);
            if (mUseExpressionValueCaching) {
                clearExpressionValueCaches();
            }
            if ((mReactions.length - 1) != lastReactionOrderSearch) {
                int temp = orderSearch[lastReactionOrderSearch];
                orderSearch[lastReactionOrderSearch] = orderSearch[lastReactionOrderSearch + 1];
                orderSearch[lastReactionOrderSearch + 1] = temp;
            }
            mReactionProbabilities[lastReactionIndex] = computeReactionRate(lastReactionIndex);
            Integer[] dependentReactions = (Integer[]) mReactionDependencies[lastReactionIndex];
            int numDependentReactions = dependentReactions.length;
            for (int ctr = numDependentReactions; --ctr >= 0; ) {
                Integer dependentReactionCtrObj = dependentReactions[ctr];
                int dependentReactionCtr = dependentReactionCtrObj.intValue();
                mReactionProbabilities[dependentReactionCtr] = computeReactionRate(dependentReactionCtr);
            }
        } else computeReactionProbabilities();
        double aggregateReactionProbability = DoubleVector.sumElements(mReactionProbabilities);
        double deltaTimeToNextReaction = Double.POSITIVE_INFINITY;
        if (aggregateReactionProbability == 0) deadlock = true;
        if (aggregateReactionProbability > 0.0) {
            deltaTimeToNextReaction = chooseDeltaTimeToNextReaction(aggregateReactionProbability);
        }
        int reactionIndex = -1;
        if (null != mDelayedReactionSolvers) {
            int nextDelayedReactionIndex = getNextDelayedReactionIndex(mDelayedReactionSolvers);
            if (nextDelayedReactionIndex >= 0) {
                DelayedReactionSolver solver = mDelayedReactionSolvers[nextDelayedReactionIndex];
                double nextDelayedReactionTime = solver.peekNextReactionTime();
                if (nextDelayedReactionTime < time + deltaTimeToNextReaction) {
                    deltaTimeToNextReaction = nextDelayedReactionTime - time;
                    reactionIndex = solver.getReactionIndex();
                    solver.pollNextReactionTime();
                }
            }
        }
        if (-1 == reactionIndex && aggregateReactionProbability > 0.0) {
            reactionIndex = chooseIndexOfNextOrderedReaction(aggregateReactionProbability);
        }
        if (-1 != reactionIndex) {
            pLastReactionIndex.setValue(reactionIndex);
            time += deltaTimeToNextReaction;
        } else {
            time = Double.POSITIVE_INFINITY;
        }
        mSymbolEvaluator.setTime(time);
        return (time);
    }

    public void initialize(Model pModel) throws DataNotFoundException, InvalidInputException {
        initializeSimulator(pModel);
        initializeSimulatorStochastic(pModel);
        setInitialized(true);
        computeReactionProbabilities();
        orderSearch = new int[mReactions.length];
        for (int i = 0; i < mReactions.length; i++) orderSearch[i] = i;
        dependcreat = new DependencyGraphCreator(this);
        mReactionDependencies = dependcreat.getReactionDependencies();
    }

    protected void modifyDefaultSimulatorParameters(SimulatorParameters pSimulatorParameters) {
    }

    public String getAlias() {
        return (CLASS_ALIAS);
    }

    public boolean isExactSimulator() {
        return true;
    }

    public boolean getDeadlock() {
        return deadlock;
    }
}
