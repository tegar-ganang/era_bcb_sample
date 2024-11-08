package unbbayes.evaluation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import unbbayes.evaluation.exception.EvaluationException;
import unbbayes.prs.bn.TreeVariable;

public class ExactEvaluation extends AEvaluation {

    protected TreeVariable targetNode;

    /**
	 * As this is an exact evaluation there is no error.
	 * @return There is no error, so it always returns zero.
	 */
    public float getError() {
        return 0;
    }

    protected float[][] computeCM(List<String> targetNodeNameList, List<String> evidenceNodeNameList) throws EvaluationException {
        init(targetNodeNameList, evidenceNodeNameList);
        targetNode = targetNodeList[0];
        if (targetNodeList.length != 1) {
            throw new EvaluationException("For now, just one target node is accepted!");
        }
        float[][] postTGivenE = new float[targetNode.getStatesSize()][evidenceStatesProduct];
        float[][] postEGivenT = new float[evidenceStatesProduct][targetNode.getStatesSize()];
        for (int row = 0; row < statesProduct; row++) {
            int[] states = getMultidimensionalCoord(row);
            int indexTarget = states[0];
            int indexEvidence = getEvidenceLinearCoord(states);
            float probTGivenE = getProbTargetGivenEvidence(states);
            postTGivenE[indexTarget][indexEvidence] = probTGivenE;
            int[] evidencesStates = new int[states.length - 1];
            for (int i = 0; i < evidencesStates.length; i++) {
                evidencesStates[i] = states[i + 1];
            }
            float probE = getEvidencesJointProbability(evidencesStates);
            float probT = getTargetPriorProbability(states[0]);
            float probEGivenT = probTGivenE * probE / probT;
            postEGivenT[indexEvidence][indexTarget] = probEGivenT;
        }
        int N = targetNode.getStatesSize();
        float[][] CM = new float[N][N];
        for (int i = 0; i < N; i++) {
            float[] arowi = postTGivenE[i];
            float[] crowi = CM[i];
            for (int k = 0; k < evidenceStatesProduct; k++) {
                float[] browk = postEGivenT[k];
                float aik = arowi[k];
                for (int j = 0; j < N; j++) {
                    crowi[j] += aik * browk[j];
                }
            }
        }
        return CM;
    }

    protected float[] getExatProbTargetGivenEvidence() throws EvaluationException {
        TreeVariable targetNode = targetNodeList[0];
        try {
            net.compile();
        } catch (Exception e) {
            throw new EvaluationException(e.getMessage());
        }
        float[] postProbList = new float[statesProduct];
        int sProd = targetNode.getStatesSize();
        byte[][] stateCombinationMatrix = new byte[statesProduct][1 + evidenceNodeList.length];
        int state = 0;
        for (int row = 0; row < statesProduct; row++) {
            stateCombinationMatrix[row][0] = (byte) (row / (statesProduct / sProd));
            for (int j = 0; j < evidenceNodeList.length; j++) {
                sProd *= evidenceNodeList[j].getStatesSize();
                state = (row / (statesProduct / sProd)) % evidenceNodeList[j].getStatesSize();
                evidenceNodeList[j].addFinding(state);
                stateCombinationMatrix[row][j + 1] = (byte) state;
            }
            sProd = targetNode.getStatesSize();
            try {
                net.updateEvidences();
                postProbList[row] = targetNode.getMarginalAt(stateCombinationMatrix[row][0]);
            } catch (Exception e) {
                postProbList[row] = 0;
            }
            try {
                net.compile();
            } catch (Exception e) {
                throw new EvaluationException(e.getMessage());
            }
        }
        return postProbList;
    }

    protected float getProbTargetGivenEvidence(int[] states) throws EvaluationException {
        try {
            net.compile();
        } catch (Exception e) {
            throw new EvaluationException(e.getMessage());
        }
        for (int i = 0; i < evidenceNodeList.length; i++) {
            evidenceNodeList[i].addFinding(states[1 + i]);
        }
        try {
            net.updateEvidences();
            return targetNode.getMarginalAt(states[0]);
        } catch (Exception e) {
            return 0;
        }
    }

    protected float getEvidencesJointProbability(int states[]) throws EvaluationException {
        try {
            net.compile();
        } catch (Exception e) {
            throw new EvaluationException(e.getMessage());
        }
        float prob = 1;
        for (int j = 0; j < evidenceNodeList.length; j++) {
            if (j > 0) {
                evidenceNodeList[j - 1].addFinding(states[j - 1]);
            }
            try {
                net.updateEvidences();
                prob *= evidenceNodeList[j].getMarginalAt(states[j]);
            } catch (Exception e) {
                return 0;
            }
        }
        return prob;
    }

    protected float[] getEvidencesJointProbability() throws EvaluationException {
        try {
            net.compile();
        } catch (Exception e) {
            throw new EvaluationException(e.getMessage());
        }
        float[] jointProbability = new float[evidenceStatesProduct];
        int sProd = 1;
        int stateCurrentNode = 0;
        int statePreviousNode = 0;
        for (int row = 0; row < evidenceStatesProduct; row++) {
            jointProbability[row] = 1;
            for (int j = 0; j < evidenceNodeList.length; j++) {
                sProd *= evidenceNodeList[j].getStatesSize();
                stateCurrentNode = (row / (evidenceStatesProduct / sProd)) % evidenceNodeList[j].getStatesSize();
                if (j > 0) {
                    evidenceNodeList[j - 1].addFinding(statePreviousNode);
                }
                try {
                    net.updateEvidences();
                    jointProbability[row] *= evidenceNodeList[j].getMarginalAt(stateCurrentNode);
                } catch (Exception e) {
                    jointProbability[row] = 0;
                }
                statePreviousNode = stateCurrentNode;
            }
            sProd = 1;
            try {
                net.compile();
            } catch (Exception e) {
                throw new EvaluationException(e.getMessage());
            }
        }
        return jointProbability;
    }

    protected float getTargetPriorProbability(int state) throws EvaluationException {
        try {
            net.compile();
        } catch (Exception e) {
            throw new EvaluationException(e.getMessage());
        }
        return targetNode.getMarginalAt(state);
    }

    protected float[] getTargetPriorProbability() throws EvaluationException {
        try {
            net.compile();
        } catch (Exception e) {
            throw new EvaluationException(e.getMessage());
        }
        float[] priorProb = new float[targetNode.getStatesSize()];
        for (int i = 0; i < targetNode.getStatesSize(); i++) {
            priorProb[i] = targetNode.getMarginalAt(i);
        }
        return priorProb;
    }

    public static void main(String[] args) throws Exception {
        boolean runSmallTest = false;
        boolean onlyGCM = true;
        List<String> targetNodeNameList = new ArrayList<String>();
        List<String> evidenceNodeNameList = new ArrayList<String>();
        String netFileName = "";
        if (runSmallTest) {
            targetNodeNameList = new ArrayList<String>();
            targetNodeNameList.add("Springler");
            evidenceNodeNameList = new ArrayList<String>();
            evidenceNodeNameList.add("Cloudy");
            evidenceNodeNameList.add("Rain");
            evidenceNodeNameList.add("Wet");
            netFileName = "src/test/resources/testCases/evaluation/WetGrass.xml";
        } else {
            targetNodeNameList = new ArrayList<String>();
            targetNodeNameList.add("TargetType");
            evidenceNodeNameList = new ArrayList<String>();
            evidenceNodeNameList.add("UHRR_Confusion");
            evidenceNodeNameList.add("ModulationFrequency");
            evidenceNodeNameList.add("CenterFrequency");
            evidenceNodeNameList.add("PRI");
            evidenceNodeNameList.add("PRF");
            netFileName = "src/test/resources/testCases/evaluation/AirID.xml";
        }
        ExactEvaluation evaluationExact = new ExactEvaluation();
        evaluationExact.evaluate(netFileName, targetNodeNameList, evidenceNodeNameList, onlyGCM);
        System.out.println("----TOTAL------");
        System.out.println("LCM:\n");
        show(evaluationExact.getEvidenceSetCM());
        System.out.println("\n");
        System.out.println("PCC: ");
        System.out.printf("%2.2f\n", evaluationExact.getEvidenceSetPCC() * 100);
        if (!onlyGCM) {
            System.out.println("\n\n\n");
            System.out.println("----MARGINAL------");
            System.out.println("\n\n");
            List<EvidenceEvaluation> list = evaluationExact.getBestMarginalImprovement();
            for (EvidenceEvaluation evidenceEvaluation : list) {
                System.out.println("-" + evidenceEvaluation.getName() + "-");
                System.out.println("\n\n");
                System.out.println("LCM:\n");
                show(evidenceEvaluation.getMarginalCM());
                System.out.println("\n");
                System.out.println("PCC: ");
                System.out.printf("%2.2f\n", evidenceEvaluation.getMarginalPCC() * 100);
                System.out.println("\n");
                System.out.println("Marginal Improvement: ");
                System.out.printf("%2.2f\n", evidenceEvaluation.getMarginalImprovement() * 100);
                System.out.println("\n\n");
            }
            System.out.println("\n");
            System.out.println("----INDIVIDUAL PCC------");
            System.out.println("\n\n");
            list = evaluationExact.getBestIndividualPCC();
            for (EvidenceEvaluation evidenceEvaluation : list) {
                System.out.println("-" + evidenceEvaluation.getName() + "-");
                System.out.println("\n\n");
                System.out.println("LCM:\n");
                show(evidenceEvaluation.getIndividualLCM());
                System.out.println("\n");
                System.out.println("PCC: ");
                System.out.printf("%2.2f\n", evidenceEvaluation.getIndividualPCC() * 100);
                System.out.println("\n\n");
                evidenceEvaluation.setCost((new Random()).nextFloat() * 1000);
            }
            System.out.println("\n");
            System.out.println("----INDIVIDUAL PCC------");
            System.out.println("\n\n");
            list = evaluationExact.getBestIndividualCostRate();
            for (EvidenceEvaluation evidenceEvaluation : list) {
                System.out.println("-" + evidenceEvaluation.getName() + "-");
                System.out.println("\n\n");
                System.out.println("PCC: ");
                System.out.printf("%2.2f\n", evidenceEvaluation.getIndividualPCC() * 100);
                System.out.println("\n");
                System.out.println("Cost: ");
                System.out.printf("%2.2f\n", evidenceEvaluation.getCost());
                System.out.println("\n");
                System.out.println("Cost Rate: ");
                System.out.printf("%2.2f\n", evidenceEvaluation.getMarginalCost() * 100);
                System.out.println("\n\n");
            }
        }
    }
}
