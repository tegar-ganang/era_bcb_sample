package joshua.discriminative.training.risk_annealer.hypergraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;
import joshua.corpus.vocab.SymbolTable;
import joshua.discriminative.feature_related.feature_template.FeatureTemplate;
import joshua.discriminative.training.parallel.ProducerConsumerModel;
import joshua.discriminative.training.risk_annealer.GradientComputer;
import joshua.discriminative.training.risk_annealer.hypergraph.parallel.GradientConsumer;
import joshua.discriminative.training.risk_annealer.hypergraph.parallel.HGProducer;

public class HGRiskGradientComputer extends GradientComputer {

    private int numSentence;

    private boolean fixFirstFeature = false;

    private HyperGraphFactory hgFactory;

    private double sumGain = 0;

    private double sumEntropy = 0;

    int numCalls = 0;

    int maxNumHGInQueue = 100;

    int numThreads = 5;

    boolean useSemiringV2 = false;

    SymbolTable symbolTbl;

    int ngramStateID;

    int baselineLMOrder;

    HashMap<String, Integer> featureStringToIntegerMap;

    List<FeatureTemplate> featTemplates;

    double[] linearCorpusGainThetas;

    boolean haveRefereces = true;

    double minFactor = 1.0;

    /** Logger for this class. */
    private static final Logger logger = Logger.getLogger(HGRiskGradientComputer.class.getSimpleName());

    public HGRiskGradientComputer(boolean useSemiringV2, int numSentence, int numFeatures, double gainFactor, double scale, double temperature, boolean computeScalingGradient, boolean fixFirstFeature, HyperGraphFactory hgFactory, int maxNumHGInQueue, int numThreads, int ngramStateID, int baselineLMOrder, SymbolTable symbolTbl, HashMap<String, Integer> featureStringToIntegerMap, List<FeatureTemplate> featTemplates, double[] linearCorpusGainThetas, boolean haveRefereces) {
        super(numFeatures, gainFactor, scale, temperature, computeScalingGradient);
        this.useSemiringV2 = useSemiringV2;
        this.numSentence = numSentence;
        this.fixFirstFeature = fixFirstFeature;
        this.hgFactory = hgFactory;
        this.maxNumHGInQueue = maxNumHGInQueue;
        this.numThreads = numThreads;
        this.ngramStateID = ngramStateID;
        this.baselineLMOrder = baselineLMOrder;
        this.symbolTbl = symbolTbl;
        this.featureStringToIntegerMap = featureStringToIntegerMap;
        this.featTemplates = featTemplates;
        this.linearCorpusGainThetas = linearCorpusGainThetas;
        this.haveRefereces = haveRefereces;
        this.minFactor = this.haveRefereces ? -1 : 1;
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
        for (int i = 0; i < gradientsForTheta.length; i++) gradientsForTheta[i] = 0;
        if (shouldComputeGradientForScalingFactor) gradientForScalingFactor = 0;
        functionValue = 0;
        sumGain = 0;
        sumEntropy = 0;
        hgFactory.startLoop();
        if (numThreads <= 1) reComputeFunctionValueAndGradientHelperSingleThread(weights2); else reComputeFunctionValueAndGradientHelper(weights2);
        hgFactory.endLoop();
        printLastestStatistics();
        numCalls++;
    }

    @Override
    public void printLastestStatistics() {
        System.out.println("Func value=" + functionValue + "=" + sumGain + "*" + gainFactor + "+" + temperature + "*" + sumEntropy);
    }

    private void reComputeFunctionValueAndGradientHelper(double[] weightsForTheta) {
        BlockingQueue<HGAndReferences> queue = new ArrayBlockingQueue<HGAndReferences>(maxNumHGInQueue);
        HGProducer producer = new HGProducer(hgFactory, queue, numThreads, numSentence);
        List<GradientConsumer> consumers = new ArrayList<GradientConsumer>();
        for (int i = 0; i < numThreads; i++) {
            RiskAndFeatureAnnotationOnLMHG riskAnnotatorNoEquiv = new RiskAndFeatureAnnotationOnLMHG(this.baselineLMOrder, this.ngramStateID, this.linearCorpusGainThetas, this.symbolTbl, this.featureStringToIntegerMap, this.featTemplates, this.haveRefereces);
            GradientConsumer c = new GradientConsumer(this.useSemiringV2, this, queue, weightsForTheta, riskAnnotatorNoEquiv, this.temperature, this.scalingFactor, this.shouldComputeGradientForScalingFactor, this.symbolTbl);
            consumers.add(c);
        }
        ProducerConsumerModel<HGAndReferences, HGProducer, GradientConsumer> model = new ProducerConsumerModel<HGAndReferences, HGProducer, GradientConsumer>(queue, producer, consumers);
        model.runParallel();
        System.out.println("BlockingQueue size=" + queue.size());
    }

    private void reComputeFunctionValueAndGradientHelperSingleThread(double[] weightsForTheta) {
        System.out.println("Non parallel version");
        RiskAndFeatureAnnotationOnLMHG riskAnnotatorNoEquiv = new RiskAndFeatureAnnotationOnLMHG(this.baselineLMOrder, this.ngramStateID, this.linearCorpusGainThetas, this.symbolTbl, this.featureStringToIntegerMap, this.featTemplates, this.haveRefereces);
        GradientConsumer consumer = new GradientConsumer(this.useSemiringV2, this, null, weightsForTheta, riskAnnotatorNoEquiv, this.temperature, this.scalingFactor, this.shouldComputeGradientForScalingFactor, this.symbolTbl);
        for (int i = 0; i < numSentence; ++i) {
            consumer.consume(hgFactory.nextHG());
        }
    }

    /**The inputs are for risk-T*entropy*/
    public synchronized void accumulateGradient(HashMap<Integer, Double> gradients, double gradientForScalingFactor, double funcVal, double risk, double entropy) {
        for (Map.Entry<Integer, Double> feature : gradients.entrySet()) {
            gradientsForTheta[feature.getKey()] += minFactor * feature.getValue();
        }
        if (shouldComputeGradientForScalingFactor) this.gradientForScalingFactor += minFactor * gradientForScalingFactor;
        if (this.fixFirstFeature) gradientsForTheta[0] = 0;
        functionValue += minFactor * funcVal;
        sumGain += -1.0 * risk;
        sumEntropy += entropy;
    }
}
