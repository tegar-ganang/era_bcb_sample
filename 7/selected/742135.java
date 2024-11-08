package joshua.discriminative.training.risk_annealer.hypergraph.deprecated;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import joshua.discriminative.semiring_parsing.MinRiskDAFuncValSemiringParser;
import joshua.discriminative.semiring_parsing.MinRiskDAGradientSemiringParser;
import joshua.discriminative.training.risk_annealer.GradientComputer;
import joshua.discriminative.training.risk_annealer.hypergraph.FeatureForest;

@Deprecated
public class HGRiskGradientComputerBasedOnSemringV1 extends GradientComputer {

    private int numSentence;

    private boolean fixFirstFeature = false;

    private FeatureForestFactory hgFactory;

    private double sumGain = 0;

    private double sumEntropy = 0;

    int numCalls = 0;

    /** Logger for this class. */
    private static final Logger logger = Logger.getLogger(HGRiskGradientComputerBasedOnSemringV1.class.getName());

    public HGRiskGradientComputerBasedOnSemringV1(int numSent_, int numFeatures_, double gainFactor_, double scale_, double temperature_, boolean computeScalingGradient, boolean fixFirstFeature_, FeatureForestFactory hgFactory_) {
        super(numFeatures_, gainFactor_, scale_, temperature_, computeScalingGradient);
        this.numSentence = numSent_;
        this.fixFirstFeature = fixFirstFeature_;
        this.hgFactory = hgFactory_;
        System.out.println("use HGRiskGradientComputerBasedOnSemringV1====");
    }

    @Override
    public void reComputeFunctionValueAndGradient(double[] weights) {
        double[] weights2 = weights;
        if (shouldComputeGradientForScalingFactor) {
            if (weights.length != numFeatures + 1) {
                System.out.println("number of weights is not right");
                System.exit(1);
            }
            scalingFactor = weights[0];
            weights2 = new double[numFeatures];
            for (int i = 0; i < numFeatures; i++) weights2[i] = weights[i + 1];
        }
        for (int i = 0; i < numFeatures; i++) gradientsForTheta[i] = 0;
        if (shouldComputeGradientForScalingFactor) gradientForScalingFactor = 0;
        functionValue = 0;
        sumGain = 0;
        sumEntropy = 0;
        hgFactory.startLoop();
        reComputeFunctionValueAndGradientHelper(weights2);
        hgFactory.endLoop();
        printLastestStatistics();
        numCalls++;
        logger.info("numTimesCalled=" + numCalls);
    }

    @Override
    public void printLastestStatistics() {
        System.out.println("Func value=" + functionValue + "=" + sumGain + "*" + gainFactor + "+" + temperature + "*" + sumEntropy);
    }

    private void reComputeFunctionValueAndGradientHelper(double[] weightsForTheta) {
        MinRiskDAGradientSemiringParser gradientSemiringParser = new MinRiskDAGradientSemiringParser(1, 0, scalingFactor, temperature);
        MinRiskDAFuncValSemiringParser funcValSemiringParser = new MinRiskDAFuncValSemiringParser(1, 0, scalingFactor, temperature);
        for (int sentID = 0; sentID < numSentence; sentID++) {
            FeatureForest fForest = hgFactory.nextHG(sentID);
            fForest.setFeatureWeights(weightsForTheta);
            fForest.setScale(scalingFactor);
            gradientSemiringParser.setHyperGraph(fForest);
            HashMap<Integer, Double> gradients = gradientSemiringParser.computeGradientForTheta();
            for (Map.Entry<Integer, Double> feature : gradients.entrySet()) {
                gradientsForTheta[feature.getKey()] -= feature.getValue();
            }
            if (this.fixFirstFeature) gradientsForTheta[0] = 0;
            if (shouldComputeGradientForScalingFactor) gradientForScalingFactor -= computeGradientForScalingFactor(gradients, weightsForTheta, scalingFactor);
            funcValSemiringParser.setHyperGraph(fForest);
            functionValue -= funcValSemiringParser.computeFunctionVal();
            sumGain += -funcValSemiringParser.getRisk();
            sumEntropy += funcValSemiringParser.getEntropy();
            if (sentID % 1000 == 0) {
                logger.info("======processed sentID =" + sentID);
            }
        }
    }

    private double computeGradientForScalingFactor(HashMap<Integer, Double> gradientForTheta, double[] weightsForTheta, double scale) {
        double gradientForScale = 0;
        for (Map.Entry<Integer, Double> feature : gradientForTheta.entrySet()) {
            gradientForScale += weightsForTheta[feature.getKey()] * feature.getValue();
        }
        gradientForScale /= scale;
        if (Double.isNaN(gradientForScale)) {
            System.out.println("gradient value for scaling is NaN");
            System.exit(1);
        }
        return gradientForScale;
    }
}
