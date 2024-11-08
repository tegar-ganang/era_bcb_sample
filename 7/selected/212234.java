package joshua.discriminative.training.risk_annealer.nbest;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import joshua.decoder.BLEU;
import joshua.decoder.NbestMinRiskReranker;
import joshua.discriminative.FileUtilityOld;
import joshua.discriminative.training.risk_annealer.GradientComputer;

/** 
* @author Zhifei Li, <zhifei.work@gmail.com>
* @version $LastChangedDate: 2008-10-20 00:12:30 -0400 $
*/
public class NbestRiskGradientComputer extends GradientComputer {

    private List<Double> gainsWithRespectToRef = new ArrayList<Double>();

    ;

    private List<Double> featureValues = new ArrayList<Double>();

    ;

    private List<Integer> startPoss = new ArrayList<Integer>();

    private List<Integer> endPoss = new ArrayList<Integer>();

    private List<Double> hypProbs = new ArrayList<Double>();

    private List<Double> expectedFeatureValues = new ArrayList<Double>();

    private boolean useLogBleu = false;

    private boolean useGoogleLinearCorpusGain = true;

    double[] linearCorpusGainThetas;

    private int totalNumSent;

    private double expectedGainSum;

    private double entropySum;

    private String nbesFile;

    private String[] refFiles;

    private boolean useShortestRefLen = true;

    private static int bleuOrder = 4;

    private static boolean doNgramClip = true;

    public NbestRiskGradientComputer(String nbesFile, String[] refFiles, boolean useShortestRefLen, int totalNumSent, int numFeatures, double gainFactor, double annealingScale, double coolingTemperature, boolean computeScalingGradient, double[] linearCorpusGainThetas) {
        super(numFeatures, gainFactor, annealingScale, coolingTemperature, computeScalingGradient);
        this.nbesFile = nbesFile;
        this.refFiles = refFiles;
        this.useShortestRefLen = useShortestRefLen;
        this.totalNumSent = totalNumSent;
        this.linearCorpusGainThetas = linearCorpusGainThetas;
        if (this.linearCorpusGainThetas != null) {
            this.useGoogleLinearCorpusGain = true;
        } else this.useGoogleLinearCorpusGain = false;
        preprocessCorpus(this.nbesFile, this.refFiles);
    }

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
        redoCorpusInference(weights2, scalingFactor);
        computeCorpusGradient(weights2, gradientsForTheta, temperature, scalingFactor);
        computeCorpusFuncVal(temperature);
    }

    public void printLastestStatistics() {
        System.out.println("Func value=" + getLatestFunctionValue() + "=" + getLatestExpectedGain() + "*" + gainFactor + "+" + getLatestEntropy() + "*" + temperature);
        System.out.println("AVG Expected_gain=" + (getLatestExpectedGain()) / totalNumSent + "%; avg entropy=" + getLatestEntropy() / totalNumSent);
    }

    private double getLatestEntropy() {
        if (Double.isNaN(entropySum)) {
            System.out.println("func_val isNaN");
            System.exit(1);
        }
        return entropySum;
    }

    private double getLatestExpectedGain() {
        if (Double.isNaN(expectedGainSum)) {
            System.out.println("func_val isNaN");
            System.exit(1);
        }
        return expectedGainSum;
    }

    private void preprocessCorpus(String nbestFile, String[] refFiles) {
        System.out.println("preprocess nbest " + nbestFile + " and ref files " + refFiles);
        BufferedReader nbestReader = FileUtilityOld.getReadFileStream(nbestFile, "UTF-8");
        BufferedReader[] refReaders = new BufferedReader[refFiles.length];
        for (int i = 0; i < refFiles.length; i++) refReaders[i] = FileUtilityOld.getReadFileStream(refFiles[i], "UTF-8");
        String line = null;
        int oldSentId = -1;
        List<String> nbest = new ArrayList<String>();
        while ((line = FileUtilityOld.readLineLzf(nbestReader)) != null) {
            String[] fds = line.split("\\s+\\|{3}\\s+");
            int newSentID = new Integer(fds[0]);
            if (oldSentId != -1 && oldSentId != newSentID) {
                String[] refs = new String[refReaders.length];
                for (int i = 0; i < refReaders.length; i++) refs[i] = FileUtilityOld.readLineLzf(refReaders[i]);
                preprocessSentNbest(nbest, oldSentId, refs);
                nbest.clear();
            }
            oldSentId = newSentID;
            nbest.add(line);
        }
        String[] refs = new String[refReaders.length];
        for (int i = 0; i < refReaders.length; i++) refs[i] = FileUtilityOld.readLineLzf(refReaders[i]);
        preprocessSentNbest(nbest, oldSentId, refs);
        nbest.clear();
        FileUtilityOld.closeReadFile(nbestReader);
        for (int i = 0; i < refReaders.length; i++) FileUtilityOld.closeReadFile(refReaders[i]);
        System.out.println("after proprecessing");
        System.out.println("featureValues size " + featureValues.size());
        System.out.println("gainsWithRespectToRef size " + gainsWithRespectToRef.size());
    }

    private void preprocessSentNbest(List<String> nbest, int sentID, String[] refs) {
        int start_pos = gainsWithRespectToRef.size();
        int end_pos = start_pos + nbest.size();
        startPoss.add(start_pos);
        endPoss.add(end_pos);
        for (String hyp : nbest) {
            String[] fds = hyp.split("\\s+\\|{3}\\s+");
            double gain = 0;
            if (useGoogleLinearCorpusGain) {
                int hypLength = fds[1].split("\\s+").length;
                HashMap<String, Integer> refereceNgramTable = BLEU.constructMaxRefCountTable(refs, bleuOrder);
                HashMap<String, Integer> hypNgramTable = BLEU.constructNgramTable(fds[1], bleuOrder);
                gain = BLEU.computeLinearCorpusGain(linearCorpusGainThetas, hypLength, hypNgramTable, refereceNgramTable);
            } else {
                gain = BLEU.computeSentenceBleu(refs, fds[1], doNgramClip, bleuOrder, useShortestRefLen);
            }
            if (useLogBleu) {
                if (gain == 0) gainsWithRespectToRef.add(0.0); else gainsWithRespectToRef.add(Math.log(gain));
            } else gainsWithRespectToRef.add(gain);
            hypProbs.add(0.0);
            String[] logFeatProb = fds[2].split("\\s+");
            for (int i = 0; i < logFeatProb.length; i++) {
                featureValues.add(new Double(logFeatProb[i]));
            }
        }
        for (int i = 0; i < numFeatures; i++) {
            expectedFeatureValues.add(0.0);
        }
    }

    private void redoCorpusInference(double[] weights, double scaling_factor) {
        for (int i = 0; i < totalNumSent; i++) {
            redoSentInference(i, weights, scaling_factor);
        }
    }

    private void redoSentInference(int sent_id, double[] weights, double scaling_factor) {
        int start_pos = startPoss.get(sent_id);
        int end_pos = endPoss.get(sent_id);
        List<Double> nbestLogProbs = hypProbs.subList(start_pos, end_pos);
        for (int i = 0; i < nbestLogProbs.size(); i++) {
            double final_score = 0;
            ;
            for (int j = 0; j < numFeatures; j++) {
                double hyp_feat_val = getFeatVal(start_pos, i, j);
                final_score += hyp_feat_val * weights[j];
            }
            if (Double.isNaN(final_score)) {
                System.out.println("final_score is NaN, must be wrong; " + final_score);
                for (int t = 0; t < weights.length; t++) System.out.println("weight: " + weights[t]);
                System.exit(1);
            }
            nbestLogProbs.set(i, final_score);
        }
        NbestMinRiskReranker.computeNormalizedProbs(nbestLogProbs, scaling_factor);
        double[] expectedValues = new double[numFeatures];
        for (int i = 0; i < nbestLogProbs.size(); i++) {
            double prob = nbestLogProbs.get(i);
            for (int j = 0; j < numFeatures; j++) {
                double hypFeatVal = getFeatVal(start_pos, i, j);
                expectedValues[j] += hypFeatVal * prob;
            }
        }
        List<Double> expecedFeatScores = getSentExpectedFeatureScoreList(sent_id);
        double t_expected_sum = 0;
        for (int j = 0; j < numFeatures; j++) {
            expecedFeatScores.set(j, expectedValues[j]);
            t_expected_sum += expectedValues[j] * weights[j];
        }
    }

    private void computeCorpusGradient(double[] weights, double[] gradients, double temperature, double scale) {
        for (int i = 0; i < totalNumSent; i++) {
            accumulateSentGradient(i, temperature, weights, gradients, scale);
        }
    }

    private void accumulateSentGradient(int sentID, double temperature, double[] weights, double[] gradients, double scale) {
        int start_pos = startPoss.get(sentID);
        int end_pos = endPoss.get(sentID);
        List<Double> nbestProbs = hypProbs.subList(start_pos, end_pos);
        List<Double> gainWithRespecitToRef = gainsWithRespectToRef.subList(start_pos, end_pos);
        List<Double> expectedFeatureValues = getSentExpectedFeatureScoreList(sentID);
        double expectedHypFinalScore = 0;
        for (int j = 0; j < numFeatures; j++) {
            expectedHypFinalScore += expectedFeatureValues.get(j) * weights[j];
        }
        for (int i = 0; i < nbestProbs.size(); i++) {
            double hypFinalScore = 0;
            double prob = nbestProbs.get(i);
            double gain = gainWithRespecitToRef.get(i) * gainFactor;
            double entropyFactor;
            if (prob == 0) entropyFactor = -temperature * (0 + 1); else entropyFactor = -temperature * (Math.log(prob) + 1);
            double anotherSentGradientForScaling = 0;
            for (int j = 0; j < numFeatures; j++) {
                double hypFeatVal = getFeatVal(start_pos, i, j);
                hypFinalScore += hypFeatVal * weights[j];
                double common = scale * prob * (hypFeatVal - expectedFeatureValues.get(j));
                double sentGradient = common * (gain + entropyFactor);
                gradients[j] += sentGradient;
                anotherSentGradientForScaling += sentGradient * weights[j];
            }
            anotherSentGradientForScaling /= scale;
            if (shouldComputeGradientForScalingFactor) {
                double common = prob * (hypFinalScore - expectedHypFinalScore);
                double sentGradientForScaling = common * (gain + entropyFactor);
                gradientForScalingFactor += sentGradientForScaling;
                if (Math.abs(sentGradientForScaling - anotherSentGradientForScaling) > 1e-2) {
                    System.out.println("gradientForScalingFactor is not equal; " + sentGradientForScaling + "!=" + anotherSentGradientForScaling + "; scale=" + scale);
                    System.exit(1);
                }
            }
        }
    }

    private void computeCorpusFuncVal(double temperature) {
        functionValue = 0;
        expectedGainSum = 0;
        entropySum = 0;
        for (int i = 0; i < totalNumSent; i++) {
            computeSentFuncVal(i, temperature);
        }
    }

    private void computeSentFuncVal(int sentID, double temperature) {
        int start_pos = startPoss.get(sentID);
        int end_pos = endPoss.get(sentID);
        List<Double> nbestGains = gainsWithRespectToRef.subList(start_pos, end_pos);
        List<Double> nbestProbs = hypProbs.subList(start_pos, end_pos);
        double expectedGain = computeExpectedGain(nbestGains, nbestProbs);
        double entropy = computeEntropy(nbestProbs);
        expectedGainSum += expectedGain;
        entropySum += entropy;
        functionValue += expectedGain * gainFactor + entropy * temperature;
    }

    public static double computeEntropy(List<Double> nbestProbs) {
        double entropy = 0;
        double tSum = 0;
        for (double prob : nbestProbs) {
            if (prob != 0) entropy -= prob * Math.log(prob);
            tSum += prob;
        }
        if (Math.abs(tSum - 1.0) > 1e-4) {
            System.out.println("probabilities not sum to one, must be wrong");
            System.exit(1);
        }
        if (Double.isNaN(entropy)) {
            System.out.println("entropy is NaN, must be wrong");
            System.exit(1);
        }
        if (entropy < 0 || entropy > Math.log(nbestProbs.size() + 1e-2)) {
            System.out.println("entropy is negative or above upper bound, must be wrong; " + entropy);
            System.exit(1);
        }
        return entropy;
    }

    public static double computeKLDivergence(List<Double> P, List<Double> Q) {
        double divergence = 0;
        if (P.size() != Q.size()) {
            System.out.println("the size of the event space of two distributions is not the same");
            System.exit(1);
        }
        double pSum = 0;
        double qSum = 0;
        for (int i = 0; i < P.size(); i++) {
            double p = P.get(i);
            double q = Q.get(i);
            double logRatio = 0;
            if (q == 0 && p != 0) {
                System.out.println("q is zero, but p is not, not well defined");
                System.exit(1);
            } else if (p == 0 || q == 0) {
                logRatio = 0;
            } else {
                logRatio = Math.log(p / q);
            }
            divergence += p * logRatio;
            pSum += p;
            qSum += q;
        }
        if (divergence < 0) {
            System.out.println("divergence is negative, must be wrong");
            System.exit(1);
        }
        if (Math.abs(pSum - 1.0) > 1e-4) {
            System.out.println("P is not sum to one, must be wrong");
            System.exit(1);
        }
        if (Math.abs(qSum - 1.0) > 1e-4) {
            System.out.println("Q is not sum to one, must be wrong");
            System.exit(1);
        }
        return divergence;
    }

    private double computeExpectedGain(List<Double> nbestGains, List<Double> nbestProbs) {
        double expectedGain = 0;
        for (int i = 0; i < nbestGains.size(); i++) {
            double gain = nbestGains.get(i);
            double trueProb = nbestProbs.get(i);
            expectedGain += trueProb * gain;
        }
        if (Double.isNaN(expectedGain)) {
            System.out.println("expected_gain isNaN, must be wrong");
            System.exit(1);
        }
        if (useGoogleLinearCorpusGain == false) {
            if (useLogBleu) {
                if (expectedGain > 1e-2) {
                    System.out.println("Warning: expected_gain is not smaller than zero when using logBLEU, must be wrong: " + expectedGain);
                    System.exit(1);
                }
            } else {
                if (expectedGain < -(1e-2) || expectedGain > 1 + 1e-2) {
                    System.out.println("Warning: expected_gain is not within [0,1], must be wrong: " + expectedGain);
                    System.exit(1);
                }
            }
        }
        return expectedGain;
    }

    private List<Double> getSentExpectedFeatureScoreList(int sentID) {
        return expectedFeatureValues.subList(sentID * numFeatures, (sentID + 1) * numFeatures);
    }

    private double getFeatVal(int startPos, int hypID, int featID) {
        return featureValues.get((startPos + hypID) * numFeatures + featID);
    }
}
