package processes;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import subjectsData.Subject;
import subjectsData.Grouping.BijectionPairing;
import displayObjects.DisplayPage;
import displayObjects.QueryField;
import displayObjects.Text;
import displayObjects.XMLprinter;
import displayObjects.Specifics.IntegerQueryField;
import displayObjects.Specifics.IntegerQueryForm;
import displayObjects.Specifics.Pages.PayoffPage;
import displayObjects.Specifics.Pages.WaitScreen;
import exceptions.NamingCollusionException;
import exceptions.WrongParentException;
import exceptions.WrongStageExpception;

public class GiftExchangeStrForm extends ExperimentProcess {

    public static final int EXPERIMENT_STAGE = 1;

    public static final int INITIAL_ENDOWMENT = 100;

    public static final int TRANSFER_UNITS = 10;

    public static final int NUM_OF_CONDITIONAL_BOXES = 11;

    public static final String FIRST_MOVER_RESPONSE_DATA = "FirstMoverResponseData";

    public static final String SECOND_MOVER_RESPONSE_DATA = "SecondMoverResponseData";

    public static final String PAIRING_FOR_STAGE = "Pairing_for_giftExchange_stage_";

    private static final int MAX_TRANSFER = 100;

    private static final int Min_TRANSFER = 0;

    public int maxSubjects = 3;

    /**
	 * This variable is used to control the flow of pages in the experiment.
	 * Setting it to false would hold subjects from progressing to future
	 * stages.
	 */
    public boolean startNewStages = false;

    public GiftExchangeStrForm() {
        super();
    }

    @Override
    public boolean updateCurrentStageScreen(Subject subject) throws WrongParentException, WrongStageExpception, NamingCollusionException {
        if (!subject.isReadyForNextPage()) {
            return false;
        } else {
            int stage = subject.getCurrentStage();
            switch(stage) {
                case (EXPERIMENT_STAGE - 1):
                    if (isReadyToStart()) {
                        progressToStage(EXPERIMENT_STAGE, 0);
                        subject.setCurrentStage(EXPERIMENT_STAGE);
                        subject.setCurrentScreen(0);
                        return true;
                    }
                    return false;
                case EXPERIMENT_STAGE:
                    int screen = subject.getCurrentScreen();
                    switch(screen) {
                        case 0:
                            subject.setCurrentScreen(1);
                            return true;
                        case 1:
                            subject.setCurrentScreen(2);
                            return true;
                        case 2:
                            subject.setCurrentScreen(3);
                            return true;
                        case 3:
                            if (subjectsData.getMinScreen() >= 3 && subjectsData.getMinStage() >= EXPERIMENT_STAGE) {
                                progressToStage(EXPERIMENT_STAGE, 3);
                                subject.setCurrentScreen(4);
                                return true;
                            } else {
                                return false;
                            }
                        case 4:
                            subject.setCurrentScreen(5);
                            return true;
                        default:
                            return false;
                    }
            }
            return false;
        }
    }

    @Override
    public DisplayPage getCurrentDisplay(Subject subject, int stage, int screen) throws WrongParentException, NamingCollusionException, WrongStageExpception {
        if (EXPERIMENT_STAGE == subject.getCurrentStage()) {
            switch(screen) {
                case 0:
                    DisplayPage waitPage = new WaitScreen("first Wait Screen", EXPERIMENT_STAGE, 0, subject);
                    subject.setCurrentPage(waitPage);
                    return waitPage;
                case 1:
                    DisplayPage firstMoverPage = new DisplayPage("First Mover Page", EXPERIMENT_STAGE, 1, subject);
                    Text heading = new Text("You have an endowment of " + INITIAL_ENDOWMENT + " Tokens.  Please enter the amount you wish to transfer, in multiples of " + TRANSFER_UNITS, firstMoverPage);
                    firstMoverPage.addDisplayObject(heading);
                    IntegerQueryForm queryQuestion = new IntegerQueryForm("How much do you wish to transfer?", "FirstMoveTrasferQuery", firstMoverPage);
                    IntegerQueryField qf = new IntegerQueryField("First Mover Transfer", queryQuestion);
                    queryQuestion.addQueryField(qf);
                    queryQuestion.setMultipleOf(TRANSFER_UNITS);
                    queryQuestion.setMaximum(MAX_TRANSFER);
                    queryQuestion.setMinumum(Min_TRANSFER);
                    firstMoverPage.setQuery(queryQuestion);
                    subject.setCurrentPage(firstMoverPage);
                    return firstMoverPage;
                case 2:
                    DisplayPage responderPage = new DisplayPage("Responder Page", EXPERIMENT_STAGE, 2, subject);
                    Text responderHeading = new Text("You have an endowment of " + INITIAL_ENDOWMENT + " Tokens, in addition to a possible transfer from the first mover." + "\n Please instruct the computer how much you want to transfer back for each possible transfer of the first mover." + " The transfer must be a multiple of " + TRANSFER_UNITS, responderPage);
                    responderPage.addDisplayObject(responderHeading);
                    IntegerQueryForm responderQueryQuestion = new IntegerQueryForm("In response to every possible transfer by the first mover," + " how much do you wish to instruct the computer to transfer?", "ResponderTrasferQuery", responderPage);
                    responderQueryQuestion.addNumberedFileds(NUM_OF_CONDITIONAL_BOXES, 0, TRANSFER_UNITS);
                    responderQueryQuestion.setMultipleOf(TRANSFER_UNITS);
                    responderPage.setQuery(responderQueryQuestion);
                    subject.setCurrentPage(responderPage);
                    return responderPage;
                case 3:
                    DisplayPage waitPage2 = new WaitScreen("WaitAferScreen2", EXPERIMENT_STAGE, 3, subject);
                    subject.setCurrentPage(waitPage2);
                    return waitPage2;
                case 4:
                    DisplayPage payoffPage = new PayoffPage("GiftExPayoff", EXPERIMENT_STAGE, 4, subject);
                    subject.setCurrentScreen(4);
                    subject.setCurrentPage(payoffPage);
                    return payoffPage;
                case 5:
                    DisplayPage endWaitPage = new WaitScreen("EndWaitPage", EXPERIMENT_STAGE, 5, subject);
                    subject.setCurrentPage(endWaitPage);
                    return endWaitPage;
                default:
                    return subject.getCurrentPage();
            }
        }
        return subject.getCurrentPage();
    }

    @Override
    public void processResponse(Subject subject, Map<String, String> response) {
        super.processResponse(subject, response);
        if (subject.getCurrentStage() == EXPERIMENT_STAGE) {
            switch(subject.getCurrentScreen()) {
                case 1:
                    int[] firstMoverTransfers = (int[]) subjectsData.getStoredData(FIRST_MOVER_RESPONSE_DATA);
                    int agentsContributionForFirstStage = ((IntegerQueryForm) subject.getCurrentQuery()).getSubmittedValues()[0];
                    firstMoverTransfers[subject.getSubjectIndex()] = agentsContributionForFirstStage;
                    subjectsData.storeData(FIRST_MOVER_RESPONSE_DATA, firstMoverTransfers);
                    break;
                case 2:
                    int[][] secondMoverTransferSchemes = (int[][]) subjectsData.getStoredData(SECOND_MOVER_RESPONSE_DATA);
                    int[] agentsContributionForSecondStage = ((IntegerQueryForm) subject.getCurrentQuery()).getSubmittedValues();
                    secondMoverTransferSchemes[subject.getSubjectIndex()] = agentsContributionForSecondStage;
                    subjectsData.storeData(SECOND_MOVER_RESPONSE_DATA, secondMoverTransferSchemes);
                    break;
            }
        }
    }

    @Override
    public void setup(int stage, int screen) {
        switch(stage) {
            case EXPERIMENT_STAGE:
                if (screen == 0) {
                    int N = subjectsData.getNumberOfSubjects();
                    subjectsData.storeData(FIRST_MOVER_RESPONSE_DATA, new int[N]);
                    subjectsData.storeData(SECOND_MOVER_RESPONSE_DATA, new int[N][NUM_OF_CONDITIONAL_BOXES]);
                }
                if (screen == 3) {
                    BijectionPairing pairing = new BijectionPairing(subjectsData.getNumberOfSubjects());
                    subjectsData.storeData(PAIRING_FOR_STAGE + EXPERIMENT_STAGE, pairing);
                    int[] firstMoverTransfers = (int[]) subjectsData.getStoredData(FIRST_MOVER_RESPONSE_DATA);
                    int[][] secondMoverTransferSchemes = (int[][]) subjectsData.getStoredData(SECOND_MOVER_RESPONSE_DATA);
                    for (int subjectIndex = 0; subjectIndex < subjectsData.getNumberOfSubjects(); subjectIndex++) {
                        Subject subject = subjectsData.getSubjectByIDNumber(subjectIndex);
                        int pairedResponder = pairing.getRightPairing(subjectIndex);
                        int transferToSecondMover = firstMoverTransfers[subjectIndex];
                        int transferFromSecondMover = secondMoverTransferSchemes[pairedResponder][transferToSecondMover / TRANSFER_UNITS];
                        int payoffAsFirstMover = INITIAL_ENDOWMENT - transferToSecondMover + 2 * transferFromSecondMover;
                        subject.addPayment(payoffAsFirstMover);
                        int pairedFirstMover = pairing.getLeftPairing(subjectIndex);
                        int transferFromFirstMover = firstMoverTransfers[pairedFirstMover];
                        int transferToFirstMover = secondMoverTransferSchemes[subjectIndex][transferFromFirstMover / TRANSFER_UNITS];
                        int payoffAsResponder = INITIAL_ENDOWMENT + 2 * transferFromFirstMover - transferToFirstMover;
                        subject.addPayment(payoffAsResponder);
                    }
                }
        }
    }

    /**
	 * Print the payoffs to screen (for debug)
	 * 
	 * @param pr
	 */
    public void printOutput(PrintStream pr) {
        int[] firstMoverTransfers = (int[]) subjectsData.getStoredData(FIRST_MOVER_RESPONSE_DATA);
        int[][] secondMoverTransferSchemes = (int[][]) subjectsData.getStoredData(SECOND_MOVER_RESPONSE_DATA);
        pr.println("\n");
        pr.println("Experiment Results: ");
        pr.println("################################");
        for (int subjectIndex = 0; subjectIndex < subjectsData.getNumberOfSubjects(); subjectIndex++) {
            Subject subject = subjectsData.getSubjectByIDNumber(subjectIndex);
            pr.println(subjectIndex + " got " + subject.getPayoff());
        }
        pr.println("\n First Mover array:");
        for (int i = 0; i < firstMoverTransfers.length; i++) {
            pr.print(firstMoverTransfers[i] + " ");
        }
        pr.println("\n Second Mover array (a row per subject):");
        for (int i = 0; i < secondMoverTransferSchemes.length; i++) {
            for (int j = 0; j < secondMoverTransferSchemes[i].length; j++) {
                pr.print(secondMoverTransferSchemes[i][j] + " ");
            }
            pr.println();
        }
        pr.print("\n\n");
    }

    @Override
    public void initSubject(Subject subject) throws WrongParentException, WrongStageExpception {
        subject.setCurrentStage(0);
        subject.setCurrentScreen(0);
        DisplayPage waitPage = new WaitScreen("first Wait Screen", 0, 0, subject);
        subject.setCurrentPage(waitPage);
    }

    public Map<String, Object> getProcessData() {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put(FIRST_MOVER_RESPONSE_DATA, subjectsData.getStoredData(FIRST_MOVER_RESPONSE_DATA));
        data.put(SECOND_MOVER_RESPONSE_DATA, subjectsData.getStoredData(SECOND_MOVER_RESPONSE_DATA));
        return data;
    }

    public static void main(String[] args) throws WrongParentException, NamingCollusionException {
        DisplayPage firstMoverPage = new DisplayPage("First Mover Page", 1, 1, null);
        Text heading = new Text("You have an endowment of " + INITIAL_ENDOWMENT + " Tokens.  Please enter the amount you wish to transfer, in multiples of " + TRANSFER_UNITS, firstMoverPage);
        firstMoverPage.addDisplayObject(heading);
        IntegerQueryForm queryQuestion = new IntegerQueryForm("How much do you wish to transfer?", "FirstMoveTrasferQuery", firstMoverPage);
        QueryField qf = new QueryField("First Mover Transfer", queryQuestion);
        queryQuestion.addQueryField(qf);
        firstMoverPage.setQuery(queryQuestion);
        XMLprinter.printXML(firstMoverPage, System.out);
    }
}
