package de.jlab.lab.runs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import de.jlab.boards.Board;
import de.jlab.boards.DCGBoard;
import de.jlab.boards.DDSBoard;
import de.jlab.boards.EDLBoard;
import de.jlab.config.runs.Run;
import de.jlab.config.runs.RunSet;
import de.jlab.lab.Lab;
import de.jlab.lab.runs.executionhandler.LinearRunHandler;
import de.jlab.lab.runs.executionhandler.RunExecutionHandler;

public class RunsExecutor extends Thread {

    volatile boolean doContinue = true;

    Lab theLab = null;

    private int precision = 100;

    RunExecutorCallback callback = null;

    public RunsExecutor(Lab theLab, RunExecutorCallback callback) {
        super();
        this.theLab = theLab;
        this.callback = callback;
    }

    public void run() {
        List<Run> runs = theLab.getConfig().getRunConfiguration().getRuns();
        List<RunDefinitionAnalyzed> analysedRuns = new ArrayList<RunDefinitionAnalyzed>();
        HashMap<Integer, RunExecutionHandler> handlerPerRun = new HashMap<Integer, RunExecutionHandler>();
        int runCount = runs.size();
        int finishedRuns = 0;
        int[] runIndices = new int[runCount];
        for (int i = 0; i < runCount; ++i) {
            runIndices[i] = 0;
            analysedRuns.add(theLab.getRunSetsPerRunDefinition().get(runs.get(i).getName()));
        }
        long executionStartTime = System.currentTimeMillis();
        while (doContinue) {
            long cycleStartTime = System.currentTimeMillis();
            long relativeTime = cycleStartTime - executionStartTime;
            for (int i = 0; i < runCount; ++i) {
                RunExecutionHandler handler = handlerPerRun.get(i);
                Run currRun = runs.get(i);
                if (handler != null) {
                    boolean completed = handler.process(relativeTime);
                    sendValueToBoard(analysedRuns.get(i).boardType, currRun.getChannel(), currRun.getAddress(), analysedRuns.get(i).getParameter(), handler.getCurrentValue());
                    if (completed) handlerPerRun.remove(i);
                } else {
                    if (analysedRuns.get(i).getRunSets().size() > runIndices[i]) {
                        RunSetAnalyzed set = (RunSetAnalyzed) analysedRuns.get(i).getRunSets().get(runIndices[i]);
                        if (relativeTime >= set.getTimestamp()) {
                            if (set.getForm() == RunSet.FORM.LINEAR) {
                                RunSetAnalyzed previousSet = (RunSetAnalyzed) analysedRuns.get(i).getRunSets().get(runIndices[i] - 1);
                                LinearRunHandler newHandler = new LinearRunHandler(previousSet.getValue(), set.getValue(), set.getFormDuration(), set.getForm(), theLab, relativeTime, precision);
                                handlerPerRun.put(i, newHandler);
                                boolean completed = newHandler.process(relativeTime);
                                sendValueToBoard(analysedRuns.get(i).boardType, currRun.getChannel(), currRun.getAddress(), analysedRuns.get(i).getParameter(), newHandler.getCurrentValue());
                                if (completed) handlerPerRun.remove(i);
                            } else {
                                sendValueToBoard(analysedRuns.get(i).boardType, currRun.getChannel(), currRun.getAddress(), analysedRuns.get(i).getParameter(), set.getValue());
                            }
                            runIndices[i]++;
                            if (analysedRuns.get(i).getRunSets().size() == runIndices[i]) {
                                finishedRuns++;
                                if (finishedRuns == runCount) {
                                    if (callback != null) callback.runFinished();
                                    return;
                                }
                            }
                        }
                    }
                }
            }
            long processingTime = System.currentTimeMillis() - cycleStartTime;
            if (processingTime < precision) {
                try {
                    Thread.sleep(precision - processingTime);
                } catch (InterruptedException e) {
                }
            } else {
                System.err.println("Precision problem");
            }
        }
    }

    private void sendValueToBoard(String boardType, String channel, int address, String parameter, double value) {
        Board board = theLab.getBoardForCommChannelNameAndAddress(channel, address);
        if (boardType.equals(DCGBoard.BOARD_IDENTIFIER)) {
            if (parameter.equals("Voltage")) {
                ((DCGBoard) board).setVoltage(value);
            }
            if (parameter.equals("Current")) {
                ((DCGBoard) board).setCurrent(value);
            }
        }
        if (boardType.equals(EDLBoard.BOARD_IDENTIFIER)) {
            if (parameter.equals("Power")) {
                ((EDLBoard) board).setNominalPower(value);
            }
            if (parameter.equals("Current")) {
                ((EDLBoard) board).setNominalCurrent(value);
            }
            if (parameter.equals("Resistance")) {
                ((EDLBoard) board).setNominalResistance(value);
            }
        }
        if (boardType.equals(DDSBoard.BOARD_IDENTIFIER)) {
            if (parameter.equals("Vss")) {
                ((DDSBoard) board).setVpeak(value);
            }
            if (parameter.equals("Veff")) {
                ((DDSBoard) board).setVeff(value);
            }
            if (parameter.equals("Frequency")) {
                ((DDSBoard) board).setFrequency(value);
            }
            if (parameter.equals("DCOffset")) {
                ((DDSBoard) board).setDCOffset(value);
            }
        }
        if (callback != null) callback.setModule(boardType, channel, address, parameter, value);
    }

    public void setDoContinue(boolean doContinue) {
        this.doContinue = doContinue;
    }
}
