package org.ourgrid.broker.scheduler.workqueue.stateMachine;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.ourgrid.broker.commands.SchedulerData;
import org.ourgrid.broker.controller.messages.WorkerClientMessages;
import org.ourgrid.broker.controller.operations.GetOperation;
import org.ourgrid.broker.controller.operations.GridProcessOperations;
import org.ourgrid.broker.scheduler.extensions.GenericTransferProgress;
import org.ourgrid.broker.scheduler.extensions.IncomingHandle;
import org.ourgrid.broker.scheduler.extensions.OutgoingHandle;
import org.ourgrid.broker.scheduler.workqueue.WorkQueueExecutionController;
import org.ourgrid.broker.scheduler.workqueue.xmlcreator.LoggerXMLCreator;
import org.ourgrid.common.executor.ExecutorResult;
import org.ourgrid.common.filemanager.FileInfo;
import org.ourgrid.common.interfaces.to.GridProcessErrorTypes;
import org.ourgrid.common.interfaces.to.GridProcessState;
import org.ourgrid.common.job.GridProcess;
import org.ourgrid.worker.controller.GridProcessError;

/**
 *
 */
public class FinalState extends AbstractRunningState {

    private final String STATE_NAME = "Final";

    public FinalState(WorkQueueExecutionController heuristic) {
        super(heuristic);
    }

    public void errorOcurred(GridProcessError error, GridProcess gridProcess) {
        fail(error, gridProcess);
    }

    public void fileRejected(OutgoingHandle handle, GridProcess gridProcess) {
        getCollector().addData(new SchedulerData(getXmlCreator(LoggerXMLCreator.class).getXML(WorkerClientMessages.getRunningStateInvalidOperation("fileRejected", STATE_NAME), LoggerXMLCreator.WARN)));
    }

    public void fileTransferRequestReceived(IncomingHandle handle, GridProcess gridProcess) {
        GetOperation getOperation = gridProcess.getOperations().getFinalPhaseOperations().get(handle);
        getOperation.setHandle(handle);
        if (getOperation.isTransferActive()) {
            getCollector().addData(new SchedulerData(getXmlCreator(LoggerXMLCreator.class).getXML(WorkerClientMessages.getRunningStateInvalidOperation("fileTransferRequestReceived", STATE_NAME), LoggerXMLCreator.ERROR)));
            return;
        }
        runOperation(getOperation, gridProcess);
    }

    public void hereIsExecutionResult(ExecutorResult result, GridProcess gridProcess) {
        getCollector().addData(new SchedulerData(getXmlCreator(LoggerXMLCreator.class).getXML(WorkerClientMessages.getRunningStateInvalidOperation("hereIsExecutionResult", STATE_NAME), LoggerXMLCreator.WARN)));
    }

    public void hereIsFileInfo(long handle, FileInfo fileInfo, GridProcess gridProcess) {
        getCollector().addData(new SchedulerData(getXmlCreator(LoggerXMLCreator.class).getXML(WorkerClientMessages.getRunningStateInvalidOperation("hereIsFileInfo", STATE_NAME), LoggerXMLCreator.WARN)));
    }

    public void incomingTransferCompleted(IncomingHandle handle, long amountWritten, GridProcess gridProcess) {
        GridProcessOperations operations = gridProcess.getOperations();
        GetOperation getOperation = operations.getFinalPhaseOperation(handle);
        File received = new File(getOperation.getRemoteFilePath());
        File renamed = new File(getOperation.getLocalFilePath());
        try {
            FileUtils.copyFile(received, renamed);
            FileUtils.forceDelete(received);
        } catch (IOException e) {
            GridProcessError error = new GridProcessError(e, GridProcessErrorTypes.BROKER_ERROR);
            fail(error, gridProcess);
        }
        if (!getOperation.isTransferActive()) {
            getCollector().addData(new SchedulerData(getXmlCreator(LoggerXMLCreator.class).getXML(WorkerClientMessages.getRunningStateInvalidOperation("incomingTransferCompleted", STATE_NAME), LoggerXMLCreator.ERROR)));
            return;
        }
        getOperation.getGridResult().getGetOperationTransferTime(getOperation).setEndTime();
        gridProcess.incDataTransfered(amountWritten);
        operations.removeFinalPhaseOperation(handle);
        if (operations.areAllFinalPhaseOperationsFinished()) {
            gridProcess.getResult().setFinalPhaseEndTime();
            sabotageCheck(gridProcess);
            if (!gridProcess.getResult().wasSabotaged()) {
                finish(gridProcess);
            }
        }
    }

    public void incomingTransferFailed(IncomingHandle handle, Exception failCause, long amountWritten, GridProcess gridProcess) {
        getCollector().addData(new SchedulerData(getXmlCreator(LoggerXMLCreator.class).getXML(WorkerClientMessages.getIncomingTransferFailedMessage(failCause, handle), LoggerXMLCreator.ERROR)));
        startFinalStateOnSisterProcess(gridProcess);
        if (gridProcess.getState().equals(GridProcessState.RUNNING)) {
            fail(new GridProcessError(failCause, GridProcessErrorTypes.FILE_TRANSFER_ERROR), gridProcess);
        }
    }

    private void startFinalStateOnSisterProcess(GridProcess gridProcess) {
        List<GridProcess> gridProcesses = gridProcess.getTask().getGridProcesses();
        GridProcess finalStateProcess = null;
        for (GridProcess eachProcess : gridProcesses) {
            if (!eachProcess.equals(gridProcess) && eachProcess.hasFinalStateStarted() && eachProcess.getState().isRunnable()) {
                if (finalStateProcess == null) {
                    finalStateProcess = eachProcess;
                } else {
                    if (eachProcess.getResult().getFinalData().getStartTime() < finalStateProcess.getResult().getFinalData().getStartTime()) {
                        finalStateProcess = eachProcess;
                    }
                }
            }
        }
        if (finalStateProcess != null) {
            startFinalState(finalStateProcess);
        }
    }

    public void outgoingTransferCancelled(OutgoingHandle handle, long amountWritten, GridProcess gridProcess) {
        getCollector().addData(new SchedulerData(getXmlCreator(LoggerXMLCreator.class).getXML(WorkerClientMessages.getRunningStateInvalidOperation("outgoingTransferCancelled", STATE_NAME), LoggerXMLCreator.ERROR)));
    }

    public void outgoingTransferCompleted(OutgoingHandle handle, long amountWritten, GridProcess gridProcess) {
        getCollector().addData(new SchedulerData(getXmlCreator(LoggerXMLCreator.class).getXML(WorkerClientMessages.getRunningStateInvalidOperation("outgoingTransferCompleted", STATE_NAME), LoggerXMLCreator.ERROR)));
    }

    public void outgoingTransferFailed(OutgoingHandle handle, String failCause, long amountWritten, GridProcess gridProcess) {
        getCollector().addData(new SchedulerData(getXmlCreator(LoggerXMLCreator.class).getXML(WorkerClientMessages.getRunningStateInvalidOperation("outgoingTransferFailed", STATE_NAME), LoggerXMLCreator.ERROR)));
    }

    public void updateTransferProgress(GenericTransferProgress fileTransferProgress, GridProcess gridProcess) {
        gridProcess.fileTransferProgressUpdate(fileTransferProgress);
    }

    public void workerIsReady(GridProcess gridProcess) {
        getCollector().addData(new SchedulerData(getXmlCreator(LoggerXMLCreator.class).getXML(WorkerClientMessages.getRunningStateInvalidOperation("workerIsReady", STATE_NAME), LoggerXMLCreator.ERROR)));
    }
}
