package hu.sztaki.lpds.pgportal.wfeditor.client;

import java.io.*;
import java.util.Iterator;
import javax.swing.*;
import javax.swing.JFileChooser;
import hu.sztaki.lpds.pgportal.wfeditor.client.utils.Util;
import hu.sztaki.lpds.pgportal.wfeditor.client.utils.MyJOptionPane;
import hu.sztaki.lpds.pgportal.wfeditor.client.utils.InputFile;
import hu.sztaki.lpds.pgportal.wfeditor.client.utils.RestrictionChecker;
import hu.sztaki.lpds.pgportal.wfeditor.client.dialog.*;
import hu.sztaki.lpds.pgportal.wfeditor.client.communication.http.HTTPCommunication;
import hu.sztaki.lpds.pgportal.wfeditor.common.jdl.JDLDocument;
import hu.sztaki.lpds.pgportal.wfeditor.common.jdl.JDLList;
import hu.sztaki.lpds.pgportal.wfeditor.client.jdl.util.JDLGenerator;
import hu.sztaki.lpds.pgportal.wfeditor.client.jdl.util.JDLValidator;
import hu.sztaki.lpds.pgportal.wfeditor.client.communication.http.CommunicationException;
import hu.sztaki.lpds.pgportal.wfeditor.client.communication.http.Command;
import hu.sztaki.lpds.pgportal.wfeditor.client.dialog.help.Help2;
import hu.sztaki.lpds.pgportal.wfeditor.client.pstudy.autogen.AutoGeneratorJob;

public class MenuButtonEventHandler {

    private hu.sztaki.lpds.pgportal.wfeditor.client.WorkflowEditor parent;

    private int newJobJobNumber;

    private javax.swing.Timer timer;

    private Help2 help = null;

    public MenuButtonEventHandler(WorkflowEditor newParent) {
        parent = newParent;
        newJobJobNumber = 1;
        String s = new String("");
    }

    public void setTimer() {
        timer = new javax.swing.Timer(parent.getVisualisationRefreshTime() * 1000, new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                doRefresh();
            }
        });
    }

    public void setTimerDelay() {
        timer.setDelay(parent.getVisualisationRefreshTime() * 1000);
    }

    public int confirmDialog() {
        return JOptionPane.showConfirmDialog(parent.getFrame(), "You have not saved yet.Do you want to save now?", "Confirmation", JOptionPane.YES_NO_CANCEL_OPTION);
    }

    private void workflowNotExistDialog() {
        int ret = JOptionPane.showConfirmDialog(parent.getFrame(), "This workflow does not exist. Do you want to save as?", "Confirmation", JOptionPane.YES_NO_OPTION);
        switch(ret) {
            case JOptionPane.YES_OPTION:
                doSaveAsRemote();
                break;
            case JOptionPane.NO_OPTION:
                parent.getGraph().setIsSaved(true);
                doNew();
                break;
        }
    }

    private void parseErrorMessageDialog(String parseErrorStr) {
        if (parseErrorStr.equals(hu.sztaki.lpds.pgportal.wfeditor.client.Graph.ERROR_NO_JOB_IN_GRAPH)) {
            JOptionPane.showMessageDialog(parent.getFrame(), "No job is created. Cannot save workflow.", "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            hu.sztaki.lpds.pgportal.wfeditor.client.dialog.MyTextPaneDialog.showTextPaneDialog(parent.getFrame(), " Save", "The following error(s) occured:", parseErrorStr, "Warning: the saved workflow will not be submitable " + "until the(se) error(s) have been corrected.");
        }
    }

    private boolean isFatalparseErrorMessageDialog(String parseErrorStr, boolean showDialog) {
        boolean ret = true;
        if (parseErrorStr.equals(hu.sztaki.lpds.pgportal.wfeditor.client.Graph.ERROR_NO_JOB_IN_GRAPH)) {
            if (showDialog) JOptionPane.showMessageDialog(parent.getFrame(), "No job is created. Cannot save workflow.", "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            ret = false;
            if (showDialog) hu.sztaki.lpds.pgportal.wfeditor.client.dialog.MyTextPaneDialog.showTextPaneDialog(parent.getFrame(), " Save", "The following error(s) occured:", parseErrorStr, "Warning: the saved workflow will not be submitable " + "until the(se) error(s) have been corrected!");
        }
        return ret;
    }

    private void createNewWorkflow() {
        stopAutomaticRefresh();
        parent.newGraph();
        if (parent.getMode() == WorkflowEditor.MODE_VIEW || parent.getMode() == WorkflowEditor.MODE_RESCUE) {
            changeMode(WorkflowEditor.MODE_EDIT);
        }
        parent.getGraph().setIsSaved(true);
        setRefreshFuctionsEnabled(false);
    }

    public void doNew() {
        if (parent.getGraph().getIsSaved() == false) {
            switch(confirmDialog()) {
                case JOptionPane.YES_OPTION:
                    doSaveRemote();
                    if (!parent.getGraph().isParseError()) {
                        createNewWorkflow();
                    }
                    break;
                case JOptionPane.NO_OPTION:
                    createNewWorkflow();
                    break;
                case JOptionPane.CANCEL_OPTION:
                    break;
            }
        } else {
            createNewWorkflow();
        }
    }

    private void showOpenRemoteDialog(String pWorkflowList) {
        OpenRemoteDialog openRemoteDialog = new OpenRemoteDialog(parent.getFrame(), true);
        openRemoteDialog.setWorkflowListComboBox(pWorkflowList);
        openRemoteDialog.pack();
        openRemoteDialog.setLocation((int) (parent.getFrame().getX() + parent.getFrame().getWidth() / 2 - openRemoteDialog.getWidth() / 2), (int) (parent.getFrame().getY() + parent.getFrame().getHeight() / 2 - openRemoteDialog.getHeight() / 2));
        openRemoteDialog.show();
        if (openRemoteDialog.getStatus(openRemoteDialog.STATUS_TYPE_VALID) == openRemoteDialog.STATUS_VALUE_TRUE) {
            parent.setAttachedWorkflowName(openRemoteDialog.getSelectedWorkflowName());
            doConnectToWorkflow();
        } else {
            if (!parent.getGraph().getIsNewWorkflow()) {
                startAutomaticRefresh();
            }
        }
    }

    public void OpenRemote() {
        this.stopAutomaticRefresh();
        String returnStr = cmdGetWorkflowList();
        if (!returnStr.equals("COMM_ERROR") && !returnStr.equals("EXEPTION")) {
            showOpenRemoteDialog(returnStr);
        }
    }

    public void doOpenRemote() {
        if (parent.getGraph().getIsSaved() == false) {
            switch(confirmDialog()) {
                case JOptionPane.YES_OPTION:
                    doSaveRemote();
                    if (!parent.getGraph().isParseError()) {
                        OpenRemote();
                    }
                    break;
                case JOptionPane.NO_OPTION:
                    OpenRemote();
                    break;
                case JOptionPane.CANCEL_OPTION:
                    break;
            }
        } else {
            OpenRemote();
        }
    }

    private void checkUploadedFiles(boolean showDialog) {
        java.util.Vector v = parent.graph.getAllFile(Port.PORT_TYPE_IN, Port.PORT_FILE_STORAGE_TYPE_ALL);
        if (v != null) {
            int ret = JOptionPane.YES_OPTION;
            if (showDialog) ret = JOptionPane.showConfirmDialog(parent.getFrame(), "Do you want to upload necessary files?", "Confirmation", JOptionPane.YES_NO_OPTION);
            if (ret == JOptionPane.YES_OPTION) {
                doUploadIntern(v, showDialog);
            }
        }
    }

    private void checkUpdateFiles() {
        if (!parent.getGraph().getUpdatActionStore().isStoreEmpty()) hu.sztaki.lpds.pgportal.wfeditor.client.communication.http.Command.cmdUpdateFiles(parent);
    }

    private void saveImportedWorkflow(boolean incompleteDefinition) {
        String returnStr;
        System.out.println("saveImportedWorkflow() called (save pressed - W_N_E)");
        returnStr = cmdSaveAsWorkflow("", parent.getGraph().getWorkflowName(), parent.getGraph().getGraphWKF("remote"), parent.getGraph().getOutputFilesRemotePath(), parent.getGraph().getRequiredWorkflowFileList(), incompleteDefinition);
        System.out.println("saveImportedWorkflow() - returnStr:" + returnStr);
        if (returnStr.equals("SUCCESS") || returnStr.equals("SOURCE_WORKFLOW_DIR_NOT_EXIST")) {
            parent.getGraph().setIsSaved(true);
            parent.getGraph().setIsImportedWorkflow(false);
            this.checkUpdateFiles();
            if (parent.getIsSupportBroker()) {
                jdl_cmdSaveJDL();
            }
            checkUploadedFiles(true);
            setRefreshFuctionsEnabled(true);
            startAutomaticRefresh();
        } else {
        }
    }

    private void saveImportedWorkflowRemote(boolean incompleteDefinition) {
        System.out.println("TMP saveImportedWorkflowRemote()  incompleteDefinition =" + incompleteDefinition);
        String returnStr = cmdRefresh();
        if (returnStr.equals("WORKFLOW_NOT_EXIST")) {
            saveImportedWorkflow(incompleteDefinition);
        } else if (returnStr.equals("RUNNING")) {
            System.out.println("saveImportedWorkflowRemote() called (save pressed - W_R)");
            int ret = JOptionPane.showConfirmDialog(parent.getFrame(), "This workflow is running. Do you want to save as different name?", "Confirmation", JOptionPane.YES_NO_OPTION);
            switch(ret) {
                case JOptionPane.YES_OPTION:
                    saveAsRemote(incompleteDefinition, true);
                    break;
            }
        } else if (returnStr.equals("NOT_RUNNING")) {
            Object[] options = { "Overwrite", "Save as", "Cancel" };
            int returnOption = JOptionPane.showOptionDialog(parent.getFrame(), "This workflow has already existed. Do you want to overwrite or save as it in different name?", "Confirmation", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
            switch(returnOption) {
                case 0:
                    System.out.println("saveImportedWorkflowRemote() called (save pressed - W_N_R, owerwrite)");
                    saveRemote(incompleteDefinition, true);
                    break;
                case 1:
                    System.out.println("saveImportedWorkflowRemote() called (save pressed - W_N_R, save as)");
                    saveAsRemote(incompleteDefinition, true);
                    break;
                case 2:
                    break;
            }
        }
    }

    private void saveRemote(boolean incompleteDefinition, boolean showDialog) {
        System.out.println("TMP saveRemote(incompleteDefinition,showDialog) incompleteDefinition=" + incompleteDefinition + " showDialog= " + showDialog);
        String returnStr = cmdSaveWorkflow(parent.getGraph().getWorkflowName(), parent.getGraph().getGraphWKF("remote"), parent.getGraph().getOutputFilesRemotePath(), parent.getGraph().getRequiredWorkflowFileList(), incompleteDefinition);
        System.out.println("saveRemote()  - returnStr:" + returnStr);
        if (returnStr.equals("WORKFLOW_IS_RUNNING")) {
            System.out.println("Save_remote: Failed - reason: WORKFLOW_IS_RUNNING");
        } else if (returnStr.equals("WORKFLOW_NOT_EXIST")) {
            System.out.println("Save_remote: Failed - reason: WORKFLOW_NOT_EXIST");
            workflowNotExistDialog();
        } else if (returnStr.equals("SUCCESS")) {
            System.out.println("Save_remote: Success");
            if (parent.getIsSupportBroker()) {
                jdl_cmdSaveJDL();
            }
            this.checkUpdateFiles();
            parent.getGraph().setIsNewWorkflow(false);
            parent.getGraph().setIsSaved(true);
            checkUploadedFiles(showDialog);
            if (parent.getGraph().getIsImportedWorkflow()) {
                setRefreshFuctionsEnabled(true);
                this.startAutomaticRefresh();
            }
            parent.getGraph().setIsImportedWorkflow(false);
        }
    }

    public void doSaveRemote() {
        doSaveRemoteIntern(true);
        doSaveRemoteIntern(false);
    }

    private void doSaveRemoteIntern(boolean showDialog) {
        if (parent.getGraph().getIsNewWorkflow()) {
            doSaveAsRemote();
        } else if (parent.getGraph().getIsImportedWorkflow()) {
            if (parent.getIsSupportBroker()) {
                jdl_reGenerateJdls();
            }
            parent.getGraph().parseGraph();
            String parseErrorStr = parent.getGraph().getParseErrorStr();
            saveImportedWorkflowRemote(parseErrorStr.equals("") ? false : !isFatalparseErrorMessageDialog(parseErrorStr, showDialog));
        } else {
            if (parent.getIsSupportBroker()) {
                jdl_reGenerateJdls();
            }
            parent.getGraph().parseGraph();
            String parseErrorStr = parent.getGraph().getParseErrorStr();
            boolean incomplete = false;
            if (!parseErrorStr.equals("")) {
                incomplete = !isFatalparseErrorMessageDialog(parseErrorStr, showDialog);
            }
            System.out.println("TMP doSaveRemote(showDialog) showDialog=" + showDialog + " incomplete=" + incomplete + " parseErrorStr=[" + parseErrorStr + "]");
            saveRemote(incomplete, showDialog);
        }
    }

    public void doSetResources() {
        if (parent.getIsSupportBroker()) {
            jdl_reGenerateJdls();
        }
        System.out.println("MenuButtonEventHandler.doSetResources() - START");
        String returnStr = cmdSetResources(parent.getGraph().getWorkflowName(), parent.getGraph().getGraphWKF("remote"), parent.getGraph().getOutputFilesRemotePath(), parent.getGraph().getRequiredWorkflowFileList(), false);
        System.out.println("MenuButtonEventHandler.doSetResources() - after cmdSetResources");
        System.out.println("cmdSetResources()  - returnStr:" + returnStr);
        if (returnStr.equals("WORKFLOW_IS_RUNNING")) {
            System.out.println("cmdSetResources: Failed - reason: WORKFLOW_IS_RUNNING");
        } else if (returnStr.equals("WORKFLOW_NOT_EXIST")) {
            System.out.println("cmdSetResources: Failed - reason: WORKFLOW_NOT_EXIST");
            workflowNotExistDialog();
        } else if (returnStr.equals("SUCCESS")) {
            System.out.println("cmdSetResources: Success");
            if (parent.getIsSupportBroker()) {
                jdl_cmdSaveJDL();
            }
            this.checkUpdateFiles();
            parent.getGraph().setIsSaved(true);
            checkUploadedFiles(true);
            if (parent.getGraph().getIsImportedWorkflow()) {
                setRefreshFuctionsEnabled(true);
                this.startAutomaticRefresh();
            }
            parent.getGraph().setIsImportedWorkflow(false);
        }
    }

    private String getWorkflowName(String originalName) {
        String newWorkflowName = null;
        String name;
        boolean isShowInputDialog = true;
        while (isShowInputDialog) {
            if (newWorkflowName == null) {
                name = originalName;
            } else {
                name = newWorkflowName;
            }
            newWorkflowName = (String) JOptionPane.showInputDialog(parent.getFrame(), "Workflow name: ", "Save as", JOptionPane.PLAIN_MESSAGE, null, null, name);
            System.out.println("WorkflowName:" + newWorkflowName);
            if (newWorkflowName == null) {
                isShowInputDialog = false;
            } else if (!RestrictionChecker.isWorkflowNameValid(newWorkflowName)) {
                isShowInputDialog = true;
                JOptionPane.showMessageDialog(parent.getFrame(), "                                 The workflow name format wrong!\n" + "               The first character must be only word character and digit,\n" + "      the next ones must be the same plus underscore and dash characters!\n", "Warning", JOptionPane.WARNING_MESSAGE);
            } else if (newWorkflowName.equals(parent.getGraph().getWorkflowName())) {
                isShowInputDialog = true;
                JOptionPane.showMessageDialog(parent.getFrame(), "The workflow cannot save the same name!", "Warning", JOptionPane.WARNING_MESSAGE);
            } else {
                isShowInputDialog = false;
            }
        }
        return newWorkflowName;
    }

    private void saveAsRemote(boolean incompleteDefinition, boolean showDialog) {
        boolean isSaveAsWorkflowRemoteSuccess = false;
        String oldWorkflowName = parent.getGraph().getWorkflowName();
        String newWorkflowName = null;
        String returnStr;
        String saveAsOldWName = new String("");
        String workflowName;
        while (!isSaveAsWorkflowRemoteSuccess) {
            if (newWorkflowName != null) {
                workflowName = newWorkflowName;
            } else {
                workflowName = oldWorkflowName;
            }
            if ((newWorkflowName = this.getWorkflowName(workflowName)) != null) {
                stopAutomaticRefresh();
                parent.getGraph().setWorkflowName(newWorkflowName);
                if (parent.getGraph().getIsNewWorkflow()) saveAsOldWName = ""; else if (parent.getGraph().getIsImportedWorkflow()) saveAsOldWName = ""; else saveAsOldWName = oldWorkflowName;
                long summSize = 0;
                if (saveAsOldWName.compareTo("") != 0) {
                    String stat = cmdGetSizeOfWorkflow(saveAsOldWName);
                    if (stat.substring(0, 2).compareTo("OK") == 0) {
                        try {
                            summSize = Long.parseLong(stat.substring(2));
                        } catch (NumberFormatException e) {
                        }
                    }
                }
                if (summSize != 0) {
                    String status1 = cmdGetSizeAcceptance(saveAsOldWName, summSize);
                    if (status1.equals("OK")) ; else {
                        isSaveAsWorkflowRemoteSuccess = false;
                        String str = "";
                        if (status1.indexOf("SIZE_ERROR") == 0) {
                            String[] linem = status1.split(";");
                            String quota = "";
                            String occupied = "";
                            if (linem.length > 1) quota = linem[1];
                            if (linem.length > 2) occupied = linem[2];
                            str = "Not enough space to upload files:\n quota = " + quota + " occupied = " + occupied + " needed = " + summSize + " byte";
                        } else str = status1 + " during upload operation";
                        JOptionPane.showMessageDialog(parent.getFrame(), str, "Error", JOptionPane.ERROR_MESSAGE);
                        parent.getGraph().setWorkflowName(saveAsOldWName);
                        return;
                    }
                }
                returnStr = cmdSaveAsWorkflow(saveAsOldWName, newWorkflowName, parent.getGraph().getGraphWKF("remote"), parent.getGraph().getOutputFilesRemotePath(), parent.getGraph().getRequiredWorkflowFileList(), incompleteDefinition);
                System.out.println("saveAsRemote() - returnStr:" + returnStr);
                if (returnStr.equals("SUCCESS") || returnStr.equals("SOURCE_WORKFLOW_DIR_NOT_EXIST")) {
                    if (parent.getMode() == WorkflowEditor.MODE_VIEW || parent.getMode() == WorkflowEditor.MODE_RESCUE) {
                        changeMode(WorkflowEditor.MODE_EDIT);
                        parent.getGraph().setWorkflowName(newWorkflowName);
                        parent.getGraph().setJobColorDefault();
                    }
                    this.checkUpdateFiles();
                    if (parent.getGraph().getIsNewWorkflow()) {
                        setRefreshFuctionsEnabled(true);
                    }
                    if (parent.getGraph().getIsImportedWorkflow()) {
                        setRefreshFuctionsEnabled(true);
                    }
                    parent.getGraph().setIsNewWorkflow(false);
                    parent.getGraph().setIsSaved(true);
                    parent.getGraph().setIsImportedWorkflow(false);
                    if (parent.getIsSupportBroker()) {
                        jdl_cmdSaveJDL();
                    }
                    checkUploadedFiles(showDialog);
                    startAutomaticRefresh();
                    isSaveAsWorkflowRemoteSuccess = true;
                } else if (returnStr.equals("TARGET_WORKFLOW_EXIST")) {
                    isSaveAsWorkflowRemoteSuccess = false;
                    parent.getGraph().setWorkflowName(oldWorkflowName);
                    startAutomaticRefresh();
                } else {
                    isSaveAsWorkflowRemoteSuccess = true;
                    parent.getGraph().setWorkflowName(oldWorkflowName);
                    startAutomaticRefresh();
                }
            } else isSaveAsWorkflowRemoteSuccess = true;
        }
    }

    public void doSaveAsRemote() {
        doSaveAsRemoteIntern();
        doSaveRemoteIntern(false);
    }

    private void doSaveAsRemoteIntern() {
        if (parent.getIsSupportBroker()) {
            jdl_reGenerateJdls();
        }
        parent.getGraph().parseGraph();
        String parseErrorStr = parent.getGraph().getParseErrorStr();
        saveAsRemote(parseErrorStr.equals("") ? false : !isFatalparseErrorMessageDialog(parseErrorStr, true), true);
    }

    public void doExit() {
        if (parent.getGraph().getIsSaved()) {
            java.lang.System.exit(0);
        }
        int ret = JOptionPane.showConfirmDialog(parent.getFrame(), "You have not saved yet. Do you want to save now?", "Confirmation", JOptionPane.YES_NO_CANCEL_OPTION);
        switch(ret) {
            case JOptionPane.YES_OPTION:
                doSaveRemote();
                if (!parent.getGraph().isParseError()) {
                    java.lang.System.exit(0);
                }
                break;
            case JOptionPane.NO_OPTION:
                java.lang.System.exit(0);
                break;
            case JOptionPane.CANCEL_OPTION:
                break;
        }
    }

    private boolean getValidWorkflowNameForimport(String name) {
        return true;
    }

    private String checkWorkflowNameValidAtImport(String name) {
        boolean isCancelPressed = false;
        boolean isNameValid = false;
        while (!(isCancelPressed || (isNameValid = RestrictionChecker.isWorkflowNameValid(name)))) {
            if (!isNameValid) {
                JOptionPane.showMessageDialog(parent.getFrame(), "            The workflow name format wrong!\n" + "The name have to contain only word characters, digits,\n" + "          underscore, dash and dot characters!", "Warning", JOptionPane.WARNING_MESSAGE);
                name = JOptionPane.showInputDialog(parent.getFrame(), "New workflow name: ", "Set name", JOptionPane.PLAIN_MESSAGE);
                if (name == null) isCancelPressed = true;
            } else {
                isCancelPressed = false;
            }
        }
        if (isNameValid) return name; else return null;
    }

    private void importWorkflow() {
        JFileChooser fc = new JFileChooser(parent.getLastDir());
        fc.setDialogTitle("Import workflow");
        fc.setFileFilter(new hu.sztaki.lpds.pgportal.wfeditor.client.utils.WRKFilter());
        int returnVal = fc.showOpenDialog(parent.getFrame());
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            parent.setLastDir(fc.getCurrentDirectory());
            File file = fc.getSelectedFile();
            String workflowName = this.checkWorkflowNameValidAtImport(Util.chopExtension(file.getName()));
            if (workflowName != null) {
                parent.newGraph();
                if (this.parent.getMode() == WorkflowEditor.MODE_VIEW) changeMode(WorkflowEditor.MODE_EDIT);
                parent.getGraph().setJobColorDefault();
                parent.getGraph().setWorkflowTitle();
                parent.getWKFParser().parseFile(file.getAbsolutePath());
                if (!parent.getWKFParser().getIsParseError()) {
                    parent.getGraph().setFilesIsUploadedAtImport();
                    parent.getGraph().setUpFileNameAtImport();
                    parent.getGraph().setWorkflowName(workflowName);
                    parent.getGraph().setIsSaved(false);
                    parent.getGraph().setIsNewWorkflow(false);
                    parent.getGraph().setIsImportedWorkflow(true);
                    parent.getGraph().setAllJobResourcesToEmpty();
                    JOptionPane.showMessageDialog(parent.getFrame(), "Please set up resources for jobs.\n", "Information", JOptionPane.INFORMATION_MESSAGE);
                    this.doWorkflowProperties();
                    setRefreshFuctionsEnabled(false);
                    stopAutomaticRefresh();
                } else {
                    createNewWorkflow();
                }
            }
        }
    }

    public void doImportWorkflow() {
        if (parent.getGraph().getIsSaved() == false) {
            switch(confirmDialog()) {
                case JOptionPane.YES_OPTION:
                    doSaveRemote();
                    if (!parent.getGraph().isParseError()) {
                        importWorkflow();
                    }
                    break;
                case JOptionPane.NO_OPTION:
                    importWorkflow();
                    break;
                case JOptionPane.CANCEL_OPTION:
                    break;
            }
        } else {
            importWorkflow();
        }
    }

    private boolean isAllPortFileExist(java.util.Vector v) {
        boolean ret = true;
        InputFile f;
        String errorStr = new String("");
        for (int i = 0; i < v.size(); i++) {
            f = (InputFile) (v.get(i));
            if (!f.getIsPortFileExist()) {
                errorStr = errorStr + f.getJobName() + " - " + f.getPortId() + " - " + f.getPath() + "\n";
                ret = false;
            }
        }
        if (ret == false) {
            errorStr = "These ports' file name does not exist or the path incorrect!\n\n" + errorStr;
            JOptionPane.showMessageDialog(parent.getFrame(), "\n" + errorStr + "\n", "Warning", JOptionPane.WARNING_MESSAGE);
        }
        return ret;
    }

    public void doUpload(java.util.Vector v) {
        doUploadIntern(v, true);
    }

    private void doUploadIntern(java.util.Vector v, boolean showDialog) {
        boolean isUpload = false;
        if (parent.getGraph().getIsSaved() == false) {
            isUpload = false;
            JOptionPane.showMessageDialog(parent.getFrame(), "Please save before upload.", "Information", JOptionPane.INFORMATION_MESSAGE);
        } else {
            if (v != null) {
                isUpload = true;
            } else {
                v = parent.graph.getAllFile(Port.PORT_TYPE_IN, Port.PORT_FILE_STORAGE_TYPE_ALL);
                if (v != null) {
                    isUpload = true;
                } else {
                    isUpload = false;
                    JOptionPane.showMessageDialog(parent.getFrame(), "         No files to upload.", "Information", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }
        if (isUpload) {
            String status = cmdGetWorkflowStatus(parent.getGraph().getWorkflowName());
            if (status.equals("WORKFLOW_NOT_EXIST")) {
                isUpload = false;
                JOptionPane.showMessageDialog(parent.getFrame(), "The '" + parent.getGraph().getWorkflowName() + "' workflow does not exist so cannot upload files.", "Error", JOptionPane.ERROR_MESSAGE);
            } else if (status.equals("RUNNING")) {
                isUpload = false;
                JOptionPane.showMessageDialog(parent.getFrame(), "The '" + parent.getGraph().getWorkflowName() + "' workflow is running so cannot upload files.", "Warning", JOptionPane.WARNING_MESSAGE);
            } else if (status.equals("NOT_RUNNING")) {
                isUpload = true;
            }
        }
        if (isUpload == true) {
            try {
                InputFile f;
                InputFile currentFile;
                for (int i = 0; i < v.size(); i++) {
                    currentFile = (InputFile) (v.get(i));
                    System.out.println("Files[" + i + "]: " + currentFile.getJobName() + "-" + currentFile.getPortId());
                    System.out.println("Files[" + i + "]: " + currentFile.getRelativePath());
                    if (currentFile == null) continue;
                    currentFile.setWorkflowName(parent.getGraph().getWorkflowName() + "_files");
                }
                long summSize = 0;
                for (int fileSize = 0; fileSize < v.size(); fileSize++) {
                    currentFile = (InputFile) v.get(fileSize);
                    if (currentFile != null) {
                        long locSize = currentFile.length();
                        summSize += locSize;
                        try {
                            System.out.println("Size of" + currentFile.getAbsolutePath() + " = " + locSize);
                        } catch (SecurityException e) {
                        }
                    }
                }
                String status1 = cmdGetSizeAcceptance(parent.getGraph().getWorkflowName(), summSize);
                if (status1.equals("OK")) ; else {
                    isUpload = false;
                    String str = "";
                    if (status1.indexOf("SIZE_ERROR") == 0) {
                        String[] linem = status1.split(";");
                        String quota = "";
                        String occupied = "";
                        if (linem.length > 1) quota = linem[1];
                        if (linem.length > 2) occupied = linem[2];
                        str = "Not enough space to upload files: quota = " + quota + " occupied =" + occupied;
                    } else str = status1 + " during upload operation";
                    JOptionPane.showMessageDialog(parent.getFrame(), str, "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (!showDialog) {
                    HTTPCommunication comm = new HTTPCommunication(parent.getPostURL(), parent);
                    comm.setVector(v);
                    comm.parameters.put("command", "upload");
                    comm.parameters.put("userName", parent.getUsersDir());
                    comm.parameters.put("workflow", parent.getGraph().getWorkflowName());
                    comm.uploadFiles();
                    int status;
                    do {
                        status = comm.getStatus();
                    } while (!comm.done());
                    if (comm.isErrorOccured()) {
                        JOptionPane.showMessageDialog(parent.getFrame(), "Upload error =" + comm.getErrorStr(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    UploadProgressDialog temp = new UploadProgressDialog(parent, true, v);
                    temp.setTransferDirection("upload");
                    temp.setLocation((int) (parent.getFrame().getX() + parent.getFrame().getWidth() / 2 - temp.getWidth() / 2), (int) (parent.getFrame().getY() + parent.getFrame().getHeight() / 2 - temp.getHeight() / 2));
                    temp.show();
                }
            } finally {
            }
        }
    }

    public void startAutomaticRefresh() {
        System.out.println("Start automatic Refresh...");
        if (!timer.isRunning()) timer.start();
        parent.getGraph().setRefreshState(Graph.REFRESH_STATE_VALUE_REFRESH_ON);
    }

    public void stopAutomaticRefresh() {
        System.out.println("Stop automatic Refresh...");
        if (timer.isRunning()) timer.stop();
        parent.getGraph().setRefreshState(Graph.REFRESH_STATE_VALUE_REFRESH_OFF);
    }

    private void setRefreshFuctionsEnabled(boolean flag) {
        parent.getMyButtonBar().setRefreshButtonEnabled(flag);
        parent.getMyButtonBar().setRefreshStateButtonEnabled(flag);
        parent.getMyMenuBar().setRefreshMenuItemEnabled(flag);
    }

    public void doRefresh() {
        String returnStr = cmdRefresh();
        if (parent.getMode() == WorkflowEditor.MODE_EDIT) {
            if (returnStr.equals("RUNNING")) {
                changeMode(WorkflowEditor.MODE_VIEW);
                parent.getGraph().setWorkflowTitle();
                startAutomaticRefresh();
                String ekieki = cmdRefresh();
            } else if (returnStr.equals("RESCUE")) {
                changeMode(WorkflowEditor.MODE_RESCUE);
                parent.getGraph().setWorkflowTitle();
                startAutomaticRefresh();
                String ekieki = cmdRefresh();
            } else if (returnStr.equals("NOT_RUNNING")) {
            } else if (returnStr.equals("WORKFLOW_NOT_EXIST")) {
                JOptionPane.showMessageDialog(parent.getFrame(), "The '" + parent.getGraph().getWorkflowName() + "' workflow was deleted.\n" + " Cannot continue editing this workflow.", "ERROR", JOptionPane.ERROR_MESSAGE);
                this.createNewWorkflow();
            }
        } else if (parent.getMode() == WorkflowEditor.MODE_VIEW) {
            if (returnStr.equals("RUNNING")) {
            } else if (returnStr.equals("RESCUE")) {
                changeMode(WorkflowEditor.MODE_RESCUE);
                parent.getGraph().setWorkflowTitle();
                startAutomaticRefresh();
                String ekieki = cmdRefresh();
            } else if (returnStr.equals("NOT_RUNNING")) {
                changeMode(WorkflowEditor.MODE_EDIT);
                parent.getGraph().setWorkflowTitle();
                parent.getGraph().setJobColorDefault();
            } else if (returnStr.equals("WORKFLOW_NOT_EXIST")) {
                JOptionPane.showMessageDialog(parent.getFrame(), "The '" + parent.getGraph().getWorkflowName() + "' workflow was deleted.\n" + " Cannot continue editing this workflow.", "ERROR", JOptionPane.ERROR_MESSAGE);
                this.createNewWorkflow();
                changeMode(WorkflowEditor.MODE_EDIT);
            }
        } else if (parent.getMode() == WorkflowEditor.MODE_RESCUE) {
            if (returnStr.equals("RUNNING")) {
                changeMode(WorkflowEditor.MODE_VIEW);
                parent.getGraph().setWorkflowTitle();
                startAutomaticRefresh();
                String ekieki = cmdRefresh();
            } else if (returnStr.equals("RESCUE")) {
            } else if (returnStr.equals("NOT_RUNNING")) {
                changeMode(WorkflowEditor.MODE_EDIT);
                parent.getGraph().setWorkflowTitle();
                parent.getGraph().setJobColorDefault();
            } else if (returnStr.equals("WORKFLOW_NOT_EXIST")) {
                JOptionPane.showMessageDialog(parent.getFrame(), "The '" + parent.getGraph().getWorkflowName() + "' workflow was deleted.\n" + " Cannot continue editing this workflow.", "ERROR", JOptionPane.ERROR_MESSAGE);
                this.createNewWorkflow();
                changeMode(WorkflowEditor.MODE_EDIT);
            }
        }
    }

    public void doProperties() {
        parent.showWorkflowDialog();
    }

    public void doWorkflowProperties() {
        hu.sztaki.lpds.pgportal.wfeditor.client.dialog.workflowProperties.WorkflowPropertyDialog wPDialog = new hu.sztaki.lpds.pgportal.wfeditor.client.dialog.workflowProperties.WorkflowPropertyDialog(parent, true);
        wPDialog.setLocation((int) (parent.getFrame().getX() + parent.getFrame().getWidth() / 2 - wPDialog.getWidth() / 2), (int) (parent.getFrame().getY() + parent.getFrame().getHeight() / 2 - wPDialog.getHeight() / 2));
        wPDialog.pack();
        wPDialog.initDialog();
        wPDialog.show();
        if (wPDialog.getStatus(wPDialog.STATUS_TYPE_VALID) == wPDialog.STATUS_VALUE_TRUE) {
            if (wPDialog.isDialogPropertiesChanged()) parent.getGraph().somethingHappened();
        }
    }

    public void doAbout() {
        AboutDialog dial = new AboutDialog(parent.getFrame(), true, parent.getVersionNo());
        dial.pack();
        dial.setLocation((int) (parent.getFrame().getX() + parent.getFrame().getWidth() / 2 - dial.getWidth() / 2), (int) (parent.getFrame().getY() + parent.getFrame().getHeight() / 2 - dial.getHeight() / 2));
        dial.show();
    }

    public void doColorCodes() {
        ColorCodesDialog dial = new ColorCodesDialog(parent.getFrame(), true);
        dial.pack();
        dial.setLocation((int) (parent.getFrame().getX() + parent.getFrame().getWidth() / 2 - dial.getWidth() / 2), (int) (parent.getFrame().getY() + parent.getFrame().getHeight() / 2 - dial.getHeight() / 2));
        dial.show();
    }

    public void doContents() {
        if (this.help == null) {
            help = new Help2(parent, true);
        }
        help.show();
    }

    public void doPSProperties() {
        PSPropertiesDialog psDialog = new PSPropertiesDialog(parent);
        psDialog.pack();
        psDialog.setLocation((int) (parent.getFrame().getX() + parent.getFrame().getWidth() / 2 - psDialog.getWidth() / 2), (int) (parent.getFrame().getY() + parent.getFrame().getHeight() / 2 - psDialog.getHeight() / 2));
        psDialog.show();
        if (psDialog.getStatus() == PSPropertiesDialog.STATUS_VALUE_TRUE) {
            System.out.println("PSDialog:ok");
        }
    }

    public void doZoom(double zf) {
        parent.getGraph().setZoomFactor(zf);
    }

    public void doRefreshState() {
        if (parent.getGraph().getStatus(Graph.REFRESH_STATE_TYPE) == Graph.REFRESH_STATE_VALUE_REFRESH_ON) {
            this.stopAutomaticRefresh();
        } else if (parent.getGraph().getStatus(Graph.REFRESH_STATE_TYPE) == Graph.REFRESH_STATE_VALUE_REFRESH_OFF) {
            this.startAutomaticRefresh();
        }
    }

    public void doNewJob() {
        parent.getGraph().createNewJob();
    }

    public void doNewPort() {
        Object o = parent.graph.getActiveObject();
        if (o == null) return;
        if (o.getClass().getName().equals("hu.sztaki.lpds.pgportal.wfeditor.client.Job")) {
            Job job;
            job = (Job) o;
            if (!job.ps_isAutoGenerator()) {
                job.createPort();
            }
        }
    }

    public void doCut() {
        GridObject o = parent.graph.getActiveObject();
        if (o == null) return;
        if (o instanceof Job || o instanceof Port) {
            parent.graph.cutSelectedObjects(o);
        }
    }

    public void doCopy() {
        GridObject o = parent.graph.getActiveObject();
        if (o == null) return;
        if (o instanceof Job || o instanceof Port) {
            parent.graph.copySelectedObjects(o);
        }
    }

    public void doPaste() {
        parent.graph.pasteSelectedObjects();
    }

    public void doDelete() {
        Object o = new Object();
        o = parent.graph.getActiveObject();
        if (o == null) return;
        if (o instanceof Job) {
            Job job;
            job = (Job) o;
            parent.graph.deleteJob(job, hu.sztaki.lpds.pgportal.wfeditor.client.utils.UpdateActionStore.USER_ACTION_DELETE);
        } else if (o instanceof Port) {
            Port p;
            p = (Port) o;
            if (!p.getParentJob().ps_isAutoGenerator()) {
                p.getParentObj().deletePort(p, hu.sztaki.lpds.pgportal.wfeditor.client.utils.UpdateActionStore.USER_ACTION_DELETE);
            }
        } else if (o instanceof PortConnection) {
            PortConnection p;
            p = (PortConnection) o;
            p.source.parent.getGraph().deleteConnectionFromConnection(p);
        }
    }

    private String cmdGetWorkflowList() {
        String returnStr = new String("");
        String commReturnStr;
        HTTPCommunication comm = new HTTPCommunication(parent.getPostURL(), parent);
        comm.parameters.put("command", "getWorkflowList");
        comm.parameters.put("sessionId", parent.getSessionId());
        try {
            comm.doPostRequest("Transferring data (list of workflows) from server...");
            commReturnStr = comm.getReturnValue();
            System.out.println("cmdGetWorkflowList(): commReturnStr:" + commReturnStr);
            boolean isError = false;
            String errorStr = "";
            if (comm.isErrorOccured()) {
                JOptionPane.showMessageDialog(parent.getFrame(), comm.getErrorStr(), "Error", JOptionPane.ERROR_MESSAGE);
                returnStr = "COMM_ERROR";
            } else if (comm.isResponseErrorOccured()) {
                int responseError;
                switch((responseError = comm.getResponseError())) {
                    case HTTPCommunication.ERROR_NO_USERNAME_TO_SESSION:
                        parent.closingWorkflow(responseError);
                        break;
                    case HTTPCommunication.ERROR_NO_DATA_FROM_SERVER:
                        MyJOptionPane.showMessageDialog(parent.getFrame(), "No workflow to open.", "Error", JOptionPane.ERROR_MESSAGE, false);
                        returnStr = "COMM_ERROR";
                        break;
                }
            } else returnStr = commReturnStr;
        } catch (Exception e) {
            MyJOptionPane.showMessageDialog(parent.getFrame(), "Error while trying to get workflow list from server:\n\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE, true);
            returnStr = "EXEPTION";
        }
        return returnStr;
    }

    private String cmdSaveWorkflow(String workflowName, String workflowStr, String outputFilesRemotePathStr, String requiredWorkflowFileListStr, boolean incompleteDefinition) {
        String returnStr = new String("");
        String commReturnStr;
        HTTPCommunication comm = new HTTPCommunication(parent.getPostURL(), parent);
        comm.parameters.put("command", "saveWorkflow");
        comm.parameters.put("sessionId", parent.getSessionId());
        comm.parameters.put("workflowName", workflowName);
        System.out.println("Graph uploaded:" + parent.getGraph().getIsUploaded());
        comm.parameters.put("workflowIncomplete", String.valueOf(incompleteDefinition || !parent.getGraph().getIsUploaded()));
        comm.parameters.put("workflowStr", workflowStr);
        comm.parameters.put("outputFilesRemotePathStr", outputFilesRemotePathStr);
        comm.parameters.put("jobParams", parent.getGraph().getJopParamString());
        if (WorkflowEditor.GRID_RELATION == WorkflowEditor.GRID_TO_WORKFLOW) {
            comm.parameters.put("gridName", ((Job) parent.getGraph().getJobList().get(0)).getJobProperties("grid"));
        }
        try {
            commReturnStr = comm.postRequest();
            System.out.println("cmdSaveWorkflow(): commReturnStr:" + commReturnStr);
            String errorStr = "";
            if (comm.isErrorOccured()) {
                JOptionPane.showMessageDialog(parent.getFrame(), comm.getErrorStr(), "Error", JOptionPane.ERROR_MESSAGE);
                returnStr = "COMM_ERROR";
            } else if (comm.isResponseErrorOccured()) {
                int responseError;
                switch((responseError = comm.getResponseError())) {
                    case HTTPCommunication.ERROR_NO_USERNAME_TO_SESSION:
                        parent.closingWorkflow(responseError);
                        break;
                    case HTTPCommunication.ERROR_NO_DATA_FROM_SERVER:
                        MyJOptionPane.showMessageDialog(parent.getFrame(), "No data from server.", "Error", JOptionPane.ERROR_MESSAGE, false);
                        returnStr = "COMM_ERROR";
                        break;
                }
            } else {
                String[] lines = commReturnStr.split("\n");
                String[] line = lines[0].split(";");
                if (line[0].equals("Error")) {
                    if (line[1].equals("WORKFLOW_IS_RUNNING")) {
                        errorStr = "Workflow is already running so cannot save!";
                        returnStr = line[1];
                        JOptionPane.showMessageDialog(parent.getFrame(), errorStr, "Error", JOptionPane.ERROR_MESSAGE);
                    } else if (line[1].equals("WORKFLOW_DIR_NOT_EXIST") || line[1].equals("WORKFLOW_FILE_NOT_EXIST") || line[1].equals("OUTPUT_FILES_REMOTE_PATH_NOT_EXIST")) {
                        returnStr = "WORKFLOW_NOT_EXIST";
                        if (parent.getDebugErrorMessage().equals("on")) {
                            System.out.println("cmdSaveWorkflow() - Error:" + line[1]);
                        }
                    }
                } else if (line[0].equals("Status")) {
                    if (line[1].equals("SUCCESS")) {
                        returnStr = "SUCCESS";
                    }
                }
            }
        } catch (Exception e) {
            MyJOptionPane.showMessageDialog(parent.getFrame(), "Error while trying to save workflow to server:\n\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE, true);
            returnStr = "EXEPTION";
        }
        return returnStr;
    }

    private String cmdSetResources(String workflowName, String workflowStr, String outputFilesRemotePathStr, String requiredWorkflowFileListStr, boolean incompleteDefinition) {
        String returnStr = new String("");
        String commReturnStr;
        HTTPCommunication comm = new HTTPCommunication(parent.getPostURL(), parent);
        comm.parameters.put("command", "setResources");
        comm.parameters.put("sessionId", parent.getSessionId());
        comm.parameters.put("workflowName", workflowName);
        comm.parameters.put("workflowIncomplete", String.valueOf(incompleteDefinition || !parent.getGraph().getIsUploaded()));
        comm.parameters.put("workflowStr", workflowStr);
        comm.parameters.put("outputFilesRemotePathStr", outputFilesRemotePathStr);
        comm.parameters.put("jobParams", parent.getGraph().getJopParamString());
        if (WorkflowEditor.GRID_RELATION == WorkflowEditor.GRID_TO_WORKFLOW) {
            comm.parameters.put("gridName", ((Job) parent.getGraph().getJobList().get(0)).getJobProperties("grid"));
        }
        try {
            commReturnStr = comm.postRequest();
            System.out.println("cmdSetResources(): commReturnStr:" + commReturnStr);
            String errorStr = "";
            if (comm.isErrorOccured()) {
                JOptionPane.showMessageDialog(parent.getFrame(), comm.getErrorStr(), "Error", JOptionPane.ERROR_MESSAGE);
                returnStr = "COMM_ERROR";
            } else if (comm.isResponseErrorOccured()) {
                int responseError;
                switch((responseError = comm.getResponseError())) {
                    case HTTPCommunication.ERROR_NO_USERNAME_TO_SESSION:
                        parent.closingWorkflow(responseError);
                        break;
                    case HTTPCommunication.ERROR_NO_DATA_FROM_SERVER:
                        MyJOptionPane.showMessageDialog(parent.getFrame(), "No data from server.", "Error", JOptionPane.ERROR_MESSAGE, false);
                        returnStr = "COMM_ERROR";
                        break;
                }
            } else {
                String[] lines = commReturnStr.split("\n");
                String[] line = lines[0].split(";");
                if (line[0].equals("Error")) {
                    if (line[1].equals("WORKFLOW_IS_RUNNING")) {
                        errorStr = "Workflow is already running so cannot save!";
                        returnStr = line[1];
                        JOptionPane.showMessageDialog(parent.getFrame(), errorStr, "Error", JOptionPane.ERROR_MESSAGE);
                    } else if (line[1].equals("WORKFLOW_DIR_NOT_EXIST") || line[1].equals("WORKFLOW_FILE_NOT_EXIST") || line[1].equals("OUTPUT_FILES_REMOTE_PATH_NOT_EXIST")) {
                        returnStr = "WORKFLOW_NOT_EXIST";
                        if (parent.getDebugErrorMessage().equals("on")) {
                            System.out.println("cmdSetResources() - Error:" + line[1]);
                        }
                    }
                } else if (line[0].equals("Status")) {
                    if (line[1].equals("SUCCESS")) {
                        returnStr = "SUCCESS";
                    }
                }
            }
        } catch (Exception e) {
            MyJOptionPane.showMessageDialog(parent.getFrame(), "Error while trying to set resources for workflow to server:\n\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE, true);
            returnStr = "EXEPTION";
        }
        return returnStr;
    }

    private String cmdSaveAsWorkflow(String oldWorkflowName, String newWorkflowName, String workflowStr, String outputFilesRemotePathStr, String requiredWorkflowFileListStr, boolean incompleteDefinition) {
        String returnStr = new String("");
        String commReturnStr;
        HTTPCommunication comm = new HTTPCommunication(parent.getPostURL(), parent);
        comm.parameters.put("command", "saveAsWorkflow");
        comm.parameters.put("sessionId", parent.getSessionId());
        comm.parameters.put("sourceWorkflowName", oldWorkflowName);
        comm.parameters.put("targetWorkflowName", newWorkflowName);
        comm.parameters.put("workflowIncomplete", String.valueOf(incompleteDefinition || !parent.getGraph().getIsUploaded()));
        comm.parameters.put("workflowStr", workflowStr);
        comm.parameters.put("outputFilesRemotePathStr", outputFilesRemotePathStr);
        comm.parameters.put("jobParams", parent.getGraph().getJopParamString());
        if (WorkflowEditor.GRID_RELATION == WorkflowEditor.GRID_TO_WORKFLOW) {
            comm.parameters.put("gridName", ((Job) parent.getGraph().getJobList().get(0)).getJobProperties("grid"));
        }
        try {
            commReturnStr = comm.postRequest();
            System.out.println("saveAsWorkflowRemote(): commReturnStr:" + commReturnStr);
            String errorStr = "";
            if (comm.isErrorOccured()) {
                JOptionPane.showMessageDialog(parent.getFrame(), comm.getErrorStr(), "Error", JOptionPane.ERROR_MESSAGE);
                returnStr = "COMM_ERROR";
            } else if (comm.isResponseErrorOccured()) {
                int responseError;
                switch((responseError = comm.getResponseError())) {
                    case HTTPCommunication.ERROR_NO_USERNAME_TO_SESSION:
                        parent.closingWorkflow(responseError);
                        break;
                    case HTTPCommunication.ERROR_NO_DATA_FROM_SERVER:
                        MyJOptionPane.showMessageDialog(parent.getFrame(), "No data from server.", "Error", JOptionPane.ERROR_MESSAGE, false);
                        returnStr = "COMM_ERROR";
                        break;
                }
            } else {
                String[] lines = commReturnStr.split("\n");
                String[] line = lines[0].split(";");
                if (line[0].equals("Error")) {
                    returnStr = line[1];
                    if (line[1].equals("SOURCE_WORKFLOW_DIR_NOT_EXIST")) {
                        if (parent.getDebugErrorMessage().equals("on")) {
                            errorStr = "Cannot copy needed files (input,executable) only can save workflow because the original workflow directory does not exist!";
                        } else errorStr = "Cannot save different name this workflow.";
                    } else if (line[1].equals("TARGET_WORKFLOW_EXIST")) {
                        errorStr = "This workflow name already exist!\n" + "Please choose another name!";
                    } else if (line[1].equals("ERROR_IN_COPY")) {
                        System.out.println("cmdSaveAsWorkflow() retrunStr:" + returnStr);
                    } else if (line[1].equals("CANT_CREATE_TARGET_DIR")) {
                        System.out.println("cmdSaveAsWorkflow() retrunStr:" + returnStr);
                    }
                    if (!errorStr.equals("")) {
                        JOptionPane.showMessageDialog(parent.getFrame(), errorStr, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else if (line[0].equals("Status")) {
                    if (line[1].equals("SUCCESS")) {
                        returnStr = "SUCCESS";
                    }
                }
            }
        } catch (Exception e) {
            MyJOptionPane.showMessageDialog(parent.getFrame(), "Error while trying to save workflow to server:\n\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE, true);
            returnStr = "EXEPTION";
        }
        System.out.println("cmdSaveAsWorkflow() returnStr:" + returnStr);
        return returnStr;
    }

    /**
     * Saves the jdls of the jobs to the server.
     * Jdls are represented in JDLList object.
     */
    private void jdl_cmdSaveJDL() {
        HTTPCommunication comm = new HTTPCommunication(parent.getPostURL(), parent);
        comm.parameters.put("command", "uploadObject");
        comm.parameters.put("sessionId", parent.getSessionId());
        comm.parameters.put("targetDirectory", "");
        comm.parameters.put("workflowName", parent.getGraph().getWorkflowName());
        comm.parameters.put("isSave", "true");
        comm.parameters.put("isActivate", "true");
        comm.parameters.put("isDeActivateIfExists", "true");
        JDLList list = this.jdl_getJobsJDLList();
        if (list.size() <= 0) {
            System.out.println("jdl_cmdSaveJDL() : JDLList is empty, nothing have to save.");
            return;
        }
        try {
            String returnStr = comm.uploadObject(list, "jdllist.object");
            System.out.println("jdl_cmdSaveJDL() :" + returnStr);
        } catch (Exception e) {
            System.out.println("jdl_cmdSaveJDL() : Error: " + e.toString());
            JOptionPane.showMessageDialog(parent.getFrame(), "Error while trying to save the jdls of the jobs to the server:\n\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Downloads the jdls of the jobs, and sets them to the parent jobs.
     */
    private void jdl_cmdOpenJDL() {
        HTTPCommunication comm = new HTTPCommunication(parent.getPostURL(), parent);
        comm.parameters.put("command", "downloadObject");
        comm.parameters.put("sessionId", parent.getSessionId());
        comm.parameters.put("workflowName", parent.getAttachedWorkflowName());
        comm.parameters.put("sourceDirectory", "");
        comm.parameters.put("sourceFile", "jdllist.object");
        try {
            JDLList jdlList = (JDLList) comm.downloadObject();
            if (jdlList != null) {
                for (int i = 0; i < jdlList.size(); i++) {
                    JDLDocument jdl = jdlList.getJDL(i);
                    jdl.check();
                    Job job = parent.getGraph().getJobByName(jdl.getJobName());
                    if (job != null && jdl != null) {
                        job.getJobProperties().put("jdl", jdl);
                    }
                }
            }
        } catch (FileNotFoundException fnfe) {
            System.out.println("jdl_cmdOpenJDL() : jdlList not found: " + fnfe.getMessage());
        } catch (ClassCastException cce) {
            System.out.println("jdl_cmdOpenJDL() : Class cast exception: " + cce.getMessage());
        } catch (ClassNotFoundException cnfe) {
            System.out.println("jdl_cmdOpenJDL() : Class not found exception: " + cnfe.getMessage());
        } catch (InvalidClassException ice) {
            System.out.println("jdl_cmdOpenJDL() : Invalid class exception: " + ice.getMessage());
        } catch (CommunicationException e) {
            System.out.println("jdl_cmdOpenJDL() : Error: " + e.toString());
            JOptionPane.showMessageDialog(parent.getFrame(), "Error while trying to open the jdls of the jobs from the server:\n\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            System.out.println("jdl_cmdOpenJDL() : Error: " + ex);
        }
    }

    private void ps_cmdGetPSProps() {
        String returnStr = "";
        String errorStr = "";
        HTTPCommunication comm = new HTTPCommunication(parent.getPostURL(), parent);
        comm.parameters.put("command", "getPSProps");
        comm.parameters.put("sessionId", parent.getSessionId());
        comm.parameters.put("workflowName", parent.getAttachedWorkflowName());
        try {
            returnStr = comm.postRequest();
            if (comm.isErrorOccured()) {
                JOptionPane.showMessageDialog(parent.getFrame(), comm.getErrorStr(), "Error", JOptionPane.ERROR_MESSAGE);
            } else if (comm.isResponseErrorOccured()) {
                int responseError;
                switch((responseError = comm.getResponseError())) {
                    case HTTPCommunication.ERROR_NO_USERNAME_TO_SESSION:
                        parent.closingWorkflow(responseError);
                        break;
                    case HTTPCommunication.ERROR_NO_DATA_FROM_SERVER:
                        MyJOptionPane.showMessageDialog(parent.getFrame(), "No data from server.", "Error", JOptionPane.ERROR_MESSAGE, false);
                        break;
                }
            } else {
                String[] lines = returnStr.split("\n");
                String[] line = lines[0].split(";");
                if (line[0].equals("Error")) {
                    errorStr = line[1];
                    JOptionPane.showMessageDialog(parent.getFrame(), errorStr, "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    String[] params = lines[0].split(" ");
                    parent.getGraph().ps_setPsGridName(params[3]);
                    parent.getGraph().ps_setPsOutputDir(params[4]);
                    if (lines.length > 1) {
                        params = lines[1].split(" ");
                        parent.getGraph().ps_setPsLFCHost(params[3]);
                        parent.getGraph().ps_setPsLCGCatalogType("lfc");
                        String se = "";
                        if (params[4].compareTo("undef") != 0) se = params[4];
                        parent.getGraph().ps_setPsStorageElement(se);
                    }
                }
            }
        } catch (Exception e) {
            MyJOptionPane.showMessageDialog(parent.getFrame(), "Error while trying to download PS Properties from server:\n\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE, true);
        }
    }

    private void ps_downloadGeneratorJobs() {
        Iterator iterator = parent.getGraph().getJobList().iterator();
        while (iterator.hasNext()) {
            Job job = (Job) iterator.next();
            if (job.ps_isAutoGenerator()) {
                ps_getGeneratorJobProperties(job);
            }
        }
    }

    private void ps_getGeneratorJobProperties(Job job) {
        HTTPCommunication comm = new HTTPCommunication(parent.getPostURL(), parent);
        comm.parameters.put("command", "downloadObject");
        comm.parameters.put("sessionId", parent.getSessionId());
        comm.parameters.put("workflowName", parent.getAttachedWorkflowName());
        comm.parameters.put("sourceDirectory", job.getName());
        comm.parameters.put("sourceFile", "generator.object");
        try {
            AutoGeneratorJob genJob = (AutoGeneratorJob) comm.downloadObject();
            job.setProperty("inputText", genJob.getInputText());
            job.setProperty("leftDelimiter", genJob.getLeftDelimiter());
            job.setProperty("rightDelimiter", genJob.getRightDelimiter());
            job.getJobProperties().put("parameterKeyList", genJob.getKeyList());
        } catch (FileNotFoundException fnfe) {
            System.out.println("ps_getGeneratorJob() : jdlList not found: " + fnfe.getMessage());
        } catch (ClassCastException cce) {
            System.out.println("ps_getGeneratorJob() : Class cast exception: " + cce.getMessage());
        } catch (ClassNotFoundException cnfe) {
            System.out.println("ps_getGeneratorJob() : Class not found exception: " + cnfe.getMessage());
        } catch (InvalidClassException ice) {
            System.out.println("ps_getGeneratorJob() : Invalid class exception: " + ice.getMessage());
        } catch (CommunicationException e) {
            System.out.println("ps_getGeneratorJob() : Error: " + e.toString());
            JOptionPane.showMessageDialog(parent.getFrame(), "Error while trying to open the generator job \"" + job.getName() + "\" properties from the server:\n\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            System.out.println("jdl_cmdOpenJDL() : Error: " + ex);
        }
    }

    private String setJobStatus(String[] lines) {
        String errorStr = new String("");
        short jobStatus = 0;
        if (!parent.getGraph().getIsImportedWorkflow()) {
            for (int i = 1; i < lines.length; i++) {
                java.lang.String[] ssin;
                ssin = lines[i].split(";");
                if (ssin.length == 2) {
                    jobStatus = (short) (Integer.parseInt(ssin[1]));
                    Job tempJob = parent.graph.getJobByName(ssin[0]);
                    if (tempJob != null) {
                        tempJob.setStatus(tempJob.RUNNING_STATE_TYPE, (short) (Integer.parseInt(ssin[1])));
                    }
                } else if (ssin.length == 3) {
                    errorStr += ssin[2] + "\n";
                }
            }
        }
        return errorStr;
    }

    private String cmdRefresh() {
        String returnStr = new String("");
        HTTPCommunication comm = new HTTPCommunication(parent.getPostURL(), parent);
        comm.parameters.put("command", "refresh");
        comm.parameters.put("sessionId", parent.getSessionId());
        comm.parameters.put("workflow", parent.graph.getWorkflowName());
        comm.parameters.put("jobParams", parent.graph.getJopParamStringForRefresh());
        try {
            String commReturnStr = comm.postRequest();
            if (comm.isErrorOccured()) {
                JOptionPane.showMessageDialog(parent.getFrame(), comm.getErrorStr(), "Error", JOptionPane.ERROR_MESSAGE);
                returnStr = "COMM_ERROR";
            } else if (comm.isResponseErrorOccured()) {
                int responseError;
                switch((responseError = comm.getResponseError())) {
                    case HTTPCommunication.ERROR_NO_USERNAME_TO_SESSION:
                        parent.closingWorkflow(responseError);
                        break;
                    case HTTPCommunication.ERROR_NO_DATA_FROM_SERVER:
                        MyJOptionPane.showMessageDialog(parent.getFrame(), "No data from server.", "Error", JOptionPane.ERROR_MESSAGE, false);
                        returnStr = "COMM_ERROR";
                        break;
                }
            } else {
                String[] lines = commReturnStr.split("\n");
                String[] line = lines[0].split(";");
                if (line[0].equals("Error")) {
                    MyJOptionPane.sysPrintln(line[1], true);
                    returnStr = "WORKFLOW_NOT_EXIST";
                    stopAutomaticRefresh();
                } else if (line[0].equals("Status")) {
                    if (line[1].equals("RUNNING") || line[1].equals("RESCUE")) {
                        String errorStr = setJobStatus(lines);
                        if (!errorStr.equals("")) {
                            if (parent.getDebugErrorMessage().equals("on")) {
                                JOptionPane.showMessageDialog(parent.getFrame(), errorStr, "Error", JOptionPane.ERROR_MESSAGE);
                            }
                            stopAutomaticRefresh();
                        }
                        returnStr = line[1];
                    } else if (line[1].equals("NOT_RUNNING")) {
                        returnStr = "NOT_RUNNING";
                    }
                }
            }
        } catch (Exception e) {
            if (parent.getDebugErrorMessage().equals("on")) {
                JOptionPane.showMessageDialog(parent.getFrame(), "Error while trying to get job status information from server.\n\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
            stopAutomaticRefresh();
            returnStr = "EXCEPTION";
        }
        System.out.println("cmdRefresh() - returnStr" + returnStr);
        return returnStr;
    }

    public void setStatusesForPreliminaryUse() {
        System.out.println("MBEH.setStatusesForPreliminaryUse() - CALLED");
        this.cmdRefresh();
    }

    public String cmdGetParametersFromServerAtStartup() {
        String returnStr = new String("SUCCESS");
        HTTPCommunication comm = new HTTPCommunication(parent.getPostURL(), parent);
        comm.parameters.put("command", "getParameters");
        comm.parameters.put("sessionId", parent.getSessionId());
        comm.parameters.put("usersDir", "1");
        comm.parameters.put("debugErrorMessage", "1");
        comm.parameters.put("editorRole", "1");
        comm.parameters.put("isSupportBroker", "1");
        try {
            String commReturnStr = comm.postRequest();
            System.out.println("cmdGetParametersFromServerAtStartup() commReturnStr:" + commReturnStr);
            if (comm.isErrorOccured()) {
                returnStr = "COMM_ERROR";
                System.out.println("cmdGetParametersFromServerAtStartup() Communication error was occured.");
            } else if (comm.isResponseErrorOccured()) {
                int responseError;
                switch((responseError = comm.getResponseError())) {
                    case HTTPCommunication.ERROR_NO_USERNAME_TO_SESSION:
                        parent.closingWorkflow(responseError);
                        break;
                    case HTTPCommunication.ERROR_NO_DATA_FROM_SERVER:
                        returnStr = "ERROR_NO_DATA_FROM_SERVER";
                        System.out.println("cmdGetParametersFromServerAtStartup(): No data from server.");
                        break;
                    case HTTPCommunication.ERROR_NO_EDITOR_ROLE_PARAMETER:
                        returnStr = "ERROR_NO_EDITOR_ROLE_PARAMETER";
                        System.out.println("cmdGetParametersFromServerAtStartup()-'editorRole': failed to get.");
                        break;
                }
            } else {
                String[] lines = commReturnStr.split("\n");
                String[] line = lines[0].split(";");
                if (line[0].equals("Error")) {
                    returnStr = "NO_USERS_DIR_PARAMETER";
                    System.out.println("cmdGetParametersFromServerAtStartup()-'usersDir': failed to get.");
                } else if (line[0].equals("usersDir")) {
                    parent.setUsersDir(line[1]);
                    System.out.println("cmdGetParametersFromServerAtStartup()-usersDir:" + line[1]);
                }
                if (lines.length > 1) {
                    line = lines[1].split(";");
                    if (line[0].equals("Error")) {
                        returnStr = "NO_DEBUG_ERROR_MESSAGE_PARAMETER";
                        System.out.println("cmdGetParametersFromServerAtStartup()-'debugErrorMessage': failed to get.");
                    } else if (line[0].equals("debugErrorMessage")) {
                        parent.setDebugErrorMessage(line[1]);
                        System.out.println("cmdGetParametersFromServerAtStartup()-debugErrorMessage:" + line[1]);
                    }
                } else {
                    returnStr = "NO_DEBUG_ERROR_MESSAGE_PARAMETER";
                    System.out.println("cmdGetParametersFromServerAtStartup()-'debugErrorMessage': failed to get.");
                }
                if (lines[0].length() > 2) {
                    line = lines[2].split(";");
                    if (line[0].equals("editorRole")) {
                        parent.setEditorRole(line[1]);
                        System.out.println("cmdGetParametersFromServerAtStartup()-'editorRole': " + line[1]);
                    }
                }
                if (lines.length > 3) {
                    line = lines[3].split(";");
                    if (line[0].equals("isSupportBroker")) {
                        parent.setIsSupportBroker(new Boolean(line[1]).booleanValue());
                        System.out.println("cmdGetParametersFromServerAtStartup()-'isSupportBroker': " + line[1]);
                    }
                }
            }
        } catch (Exception e) {
            returnStr = "EXCEPTION";
            System.out.println("Getting parameters at startup from server: exception.");
        }
        System.out.println("cmdGetParametersFromServerAtStartup() returnStr:" + returnStr);
        return returnStr;
    }

    private String cmdGetAttachedWrkFileContent() {
        String returnStr;
        HTTPCommunication comm = new HTTPCommunication(parent.getPostURL(), parent);
        comm.parameters.put("command", "getAttachedWrkFileContent");
        comm.parameters.put("sessionId", parent.getSessionId());
        comm.parameters.put("attachedWorkflowName", parent.getAttachedWorkflowName());
        try {
            returnStr = comm.postRequest();
            String errorStr = "";
            if (comm.isErrorOccured()) {
                JOptionPane.showMessageDialog(parent.getFrame(), comm.getErrorStr(), "Error", JOptionPane.ERROR_MESSAGE);
                returnStr = "";
            } else if (comm.isResponseErrorOccured()) {
                int responseError;
                switch((responseError = comm.getResponseError())) {
                    case HTTPCommunication.ERROR_NO_USERNAME_TO_SESSION:
                        parent.closingWorkflow(responseError);
                        break;
                    case HTTPCommunication.ERROR_NO_DATA_FROM_SERVER:
                        MyJOptionPane.showMessageDialog(parent.getFrame(), "No data from server.", "Error", JOptionPane.ERROR_MESSAGE, false);
                        returnStr = "";
                        break;
                }
            } else {
                String[] lines = returnStr.split("\n");
                String[] line = lines[0].split(";");
                if (line[0].equals("Error")) {
                    errorStr = line[1];
                    JOptionPane.showMessageDialog(parent.getFrame(), errorStr, "Error", JOptionPane.ERROR_MESSAGE);
                    returnStr = "";
                }
            }
        } catch (Exception e) {
            MyJOptionPane.showMessageDialog(parent.getFrame(), "Error while trying to download attached workflow from server:\n\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE, true);
            returnStr = "";
        }
        return returnStr;
    }

    private String cmdGetSizeAcceptance(String workflowName, long sizeOfFiles) {
        String returnStr = new String("");
        String commReturnStr;
        HTTPCommunication comm = new HTTPCommunication(parent.getPostURL(), parent);
        comm.parameters.put("command", "getSizeAcceptance");
        comm.parameters.put("sessionId", parent.getSessionId());
        comm.parameters.put("workflowName", workflowName);
        comm.parameters.put("sizeRequired", String.valueOf(sizeOfFiles));
        try {
            commReturnStr = comm.postRequest();
            System.out.println("cmdGetWorkflowStatus(): commReturnStr:" + commReturnStr);
            String errorStr = "";
            if (comm.isErrorOccured()) {
                JOptionPane.showMessageDialog(parent.getFrame(), comm.getErrorStr(), "Error", JOptionPane.ERROR_MESSAGE);
                returnStr = "COMM_ERROR";
            } else if (comm.isResponseErrorOccured()) {
                int responseError;
                switch((responseError = comm.getResponseError())) {
                    case HTTPCommunication.ERROR_NO_USERNAME_TO_SESSION:
                        parent.closingWorkflow(responseError);
                        break;
                    case HTTPCommunication.ERROR_NO_DATA_FROM_SERVER:
                        MyJOptionPane.showMessageDialog(parent.getFrame(), "No data from server.", "Error", JOptionPane.ERROR_MESSAGE, false);
                        returnStr = "COMM_ERROR";
                        break;
                }
            } else {
                String[] lines = commReturnStr.split("\n");
                String[] line = lines[0].split(";");
                returnStr = line[0];
                if (line[0].equals("SIZE_ERROR")) {
                    returnStr = commReturnStr;
                    if (parent.getDebugErrorMessage().equals("on")) {
                        System.out.println("Quota exhausted of uploading " + workflowName + " attempting at " + sizeOfFiles + "Bytes");
                    }
                }
            }
        } catch (Exception e) {
            MyJOptionPane.showMessageDialog(parent.getFrame(), "Error while trying to get workflow status.\n\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE, true);
            returnStr = "EXCEPTION";
        }
        return returnStr;
    }

    public String cmdIsPortInputFileTimeStampOlder(String workflowName, String jobName, String portId, String internalFileName, long timeStampClient) {
        String returnStr = new String("");
        String commReturnStr;
        HTTPCommunication comm = new HTTPCommunication(parent.getPostURL(), parent);
        comm.parameters.put("command", "isPortInputFileTimeStampOlder");
        comm.parameters.put("sessionId", parent.getSessionId());
        comm.parameters.put("workflowName", workflowName);
        comm.parameters.put("jobName", jobName);
        comm.parameters.put("portId", portId);
        comm.parameters.put("internalFileName", internalFileName);
        comm.parameters.put("timeStampClient", String.valueOf(timeStampClient));
        try {
            commReturnStr = comm.postRequest();
            System.out.println("cmdGetWorkflowStatus(): commReturnStr:" + commReturnStr);
            String errorStr = "";
            if (comm.isErrorOccured()) {
                JOptionPane.showMessageDialog(parent.getFrame(), comm.getErrorStr(), "Error", JOptionPane.ERROR_MESSAGE);
                returnStr = "COMM_ERROR";
            } else if (comm.isResponseErrorOccured()) {
                int responseError;
                switch((responseError = comm.getResponseError())) {
                    case HTTPCommunication.ERROR_NO_USERNAME_TO_SESSION:
                        parent.closingWorkflow(responseError);
                        break;
                    case HTTPCommunication.ERROR_NO_DATA_FROM_SERVER:
                        MyJOptionPane.showMessageDialog(parent.getFrame(), "No data from server.", "Error", JOptionPane.ERROR_MESSAGE, false);
                        returnStr = "COMM_ERROR";
                        break;
                }
            } else {
                String[] lines = commReturnStr.split("\n");
                String[] line = lines[0].split(";");
                returnStr = line[0];
            }
        } catch (Exception e) {
            MyJOptionPane.showMessageDialog(parent.getFrame(), "Error while trying to get workflow status.\n\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE, true);
            returnStr = "EXCEPTION";
        }
        return returnStr;
    }

    private String cmdGetSizeOfWorkflow(String workflowName) {
        String returnStr = new String("");
        String commReturnStr;
        HTTPCommunication comm = new HTTPCommunication(parent.getPostURL(), parent);
        comm.parameters.put("command", "getSizeOfWorkflow");
        comm.parameters.put("sessionId", parent.getSessionId());
        comm.parameters.put("workflowName", workflowName);
        try {
            commReturnStr = comm.postRequest();
            System.out.println("cmdGetWorkflowStatus(): commReturnStr:" + commReturnStr);
            String errorStr = "";
            if (comm.isErrorOccured()) {
                JOptionPane.showMessageDialog(parent.getFrame(), comm.getErrorStr(), "Error", JOptionPane.ERROR_MESSAGE);
                returnStr = "COMM_ERROR";
            } else if (comm.isResponseErrorOccured()) {
                int responseError;
                switch((responseError = comm.getResponseError())) {
                    case HTTPCommunication.ERROR_NO_USERNAME_TO_SESSION:
                        parent.closingWorkflow(responseError);
                        break;
                    case HTTPCommunication.ERROR_NO_DATA_FROM_SERVER:
                        MyJOptionPane.showMessageDialog(parent.getFrame(), "No data from server.", "Error", JOptionPane.ERROR_MESSAGE, false);
                        returnStr = "COMM_ERROR";
                        break;
                }
            } else {
                String[] lines = commReturnStr.split("\n");
                String[] line = lines[0].split(";");
                returnStr = line[0];
            }
        } catch (Exception e) {
            MyJOptionPane.showMessageDialog(parent.getFrame(), "Error while trying to get the length of old workflow \"" + workflowName + "\".\n\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE, true);
            returnStr = "EXCEPTION";
        }
        return returnStr;
    }

    private String cmdGetWorkflowStatus(String workflowName) {
        String returnStr = new String("");
        String commReturnStr;
        HTTPCommunication comm = new HTTPCommunication(parent.getPostURL(), parent);
        comm.parameters.put("command", "getWorkflowStatus");
        comm.parameters.put("sessionId", parent.getSessionId());
        comm.parameters.put("workflowName", workflowName);
        try {
            commReturnStr = comm.postRequest();
            System.out.println("cmdGetWorkflowStatus(): commReturnStr:" + commReturnStr);
            String errorStr = "";
            if (comm.isErrorOccured()) {
                JOptionPane.showMessageDialog(parent.getFrame(), comm.getErrorStr(), "Error", JOptionPane.ERROR_MESSAGE);
                returnStr = "COMM_ERROR";
            } else if (comm.isResponseErrorOccured()) {
                int responseError;
                switch((responseError = comm.getResponseError())) {
                    case HTTPCommunication.ERROR_NO_USERNAME_TO_SESSION:
                        parent.closingWorkflow(responseError);
                        break;
                    case HTTPCommunication.ERROR_NO_DATA_FROM_SERVER:
                        MyJOptionPane.showMessageDialog(parent.getFrame(), "No data from server.", "Error", JOptionPane.ERROR_MESSAGE, false);
                        returnStr = "COMM_ERROR";
                        break;
                }
            } else {
                String[] lines = commReturnStr.split("\n");
                String[] line = lines[0].split(";");
                if (line[0].equals("Error")) {
                    returnStr = "WORKFLOW_NOT_EXIST";
                    if (parent.getDebugErrorMessage().equals("on")) {
                        System.out.println("Cannot get " + workflowName + "'s status. Reason: " + line[1]);
                    }
                } else if (line[0].equals("Status")) {
                    returnStr = line[1];
                }
            }
        } catch (Exception e) {
            MyJOptionPane.showMessageDialog(parent.getFrame(), "Error while trying to get workflow status.\n\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE, true);
            returnStr = "EXCEPTION";
        }
        return returnStr;
    }

    private String cmdCopyDemoWorkflowToUsersDir() {
        String returnStr = new String("");
        String commReturnStr;
        HTTPCommunication comm = new HTTPCommunication(parent.getPostURL(), parent);
        comm.parameters.put("command", "copyDemoWorkflowToUsersDir");
        comm.parameters.put("sessionId", parent.getSessionId());
        comm.parameters.put("workflowName", parent.getAttachedWorkflowName());
        try {
            commReturnStr = comm.postRequest();
            System.out.println("cmdCopyDemoWorkflowToUsersDir(): commReturnStr:" + commReturnStr);
            String errorStr = "";
            if (comm.isErrorOccured()) {
                JOptionPane.showMessageDialog(parent.getFrame(), comm.getErrorStr(), "Error", JOptionPane.ERROR_MESSAGE);
                returnStr = "COMM_ERROR";
            } else if (comm.isResponseErrorOccured()) {
                int responseError;
                switch((responseError = comm.getResponseError())) {
                    case HTTPCommunication.ERROR_NO_USERNAME_TO_SESSION:
                        parent.closingWorkflow(responseError);
                        break;
                    case HTTPCommunication.ERROR_NO_DATA_FROM_SERVER:
                        MyJOptionPane.showMessageDialog(parent.getFrame(), "No data from server.", "Error", JOptionPane.ERROR_MESSAGE, false);
                        returnStr = "COMM_ERROR";
                        break;
                }
            } else {
                String[] lines = commReturnStr.split("\n");
                String[] line = lines[0].split(";");
                if (line[0].equals("Error")) {
                    if (line[1].equals("TARGET_WORKFLOW_EXIST")) {
                        returnStr = line[1];
                    }
                    if (parent.getDebugErrorMessage().equals("on")) {
                        System.out.println("cmdCopyDemoWorkflowToUsersDir() - " + parent.getAttachedWorkflowName() + " status:" + line[1]);
                    }
                } else if (line[0].equals("Status")) {
                    if (line[1].equals("SUCCESS")) {
                        returnStr = "SUCCESS";
                    }
                }
            }
        } catch (Exception e) {
            MyJOptionPane.showMessageDialog(parent.getFrame(), "Error while trying to copy workflow to users dir:\n\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE, true);
            returnStr = "EXEPTION";
        }
        System.out.println("cmdCopyDemoWorkflowToUsersDir()-returnStr:" + returnStr);
        return returnStr;
    }

    public String cmdCheckGraphFilesExist(String uploadFilesListReqStr) {
        String returnStr;
        HTTPCommunication comm = new HTTPCommunication(parent.getPostURL(), parent);
        comm.parameters.put("command", "checkGraphFilesExist");
        comm.parameters.put("sessionId", parent.getSessionId());
        comm.parameters.put("workflowName", parent.getGraph().getWorkflowName());
        comm.parameters.put("uploadFilesListReqStr", uploadFilesListReqStr);
        try {
            returnStr = comm.postRequest();
            System.out.println("cmdCheckGraphFilesExist() - returnStr:" + returnStr);
            String errorStr = "";
            if (comm.isErrorOccured()) {
                JOptionPane.showMessageDialog(parent.getFrame(), comm.getErrorStr(), "Error", JOptionPane.ERROR_MESSAGE);
                returnStr = "";
            } else if (comm.isResponseErrorOccured()) {
                int responseError;
                switch((responseError = comm.getResponseError())) {
                    case HTTPCommunication.ERROR_NO_USERNAME_TO_SESSION:
                        parent.closingWorkflow(responseError);
                        break;
                    case HTTPCommunication.ERROR_NO_DATA_FROM_SERVER:
                        MyJOptionPane.showMessageDialog(parent.getFrame(), "No data from server.", "Error", JOptionPane.ERROR_MESSAGE, false);
                        returnStr = "";
                        break;
                }
            } else {
                String[] lines = returnStr.split("\n");
                String[] line = lines[0].split(";");
                if (line[0].equals("Error")) {
                    errorStr = line[1];
                    JOptionPane.showMessageDialog(parent.getFrame(), errorStr, "Error", JOptionPane.ERROR_MESSAGE);
                    returnStr = "";
                }
            }
        } catch (Exception e) {
            MyJOptionPane.showMessageDialog(parent.getFrame(), "Error while trying to get uploaded files list from server:\n\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE, true);
            returnStr = "";
        }
        return returnStr;
    }

    public String cmdCheckJobExecutableIsInstrumented(String jobListReqStr) {
        String returnStr;
        HTTPCommunication comm = new HTTPCommunication(parent.getPostURL(), parent);
        comm.parameters.put("command", "checkJobExecutableIsInstrumented");
        comm.parameters.put("sessionId", parent.getSessionId());
        comm.parameters.put("workflowName", parent.getGraph().getWorkflowName());
        comm.parameters.put("jobListReqStr", jobListReqStr);
        try {
            returnStr = comm.postRequest();
            System.out.println("cmdCheckJobExecutableIsInstrumented()-returnStr:" + returnStr);
            String errorStr = "";
            if (comm.isErrorOccured()) {
                JOptionPane.showMessageDialog(parent.getFrame(), comm.getErrorStr(), "Error", JOptionPane.ERROR_MESSAGE);
                returnStr = "";
            } else if (comm.isResponseErrorOccured()) {
                int responseError;
                switch((responseError = comm.getResponseError())) {
                    case HTTPCommunication.ERROR_NO_USERNAME_TO_SESSION:
                        parent.closingWorkflow(responseError);
                        break;
                    case HTTPCommunication.ERROR_NO_DATA_FROM_SERVER:
                        MyJOptionPane.showMessageDialog(parent.getFrame(), "No data from server.", "Error", JOptionPane.ERROR_MESSAGE, false);
                        returnStr = "";
                        break;
                }
            } else {
                String[] lines = returnStr.split("\n");
                String[] line = lines[0].split(";");
                if (line[0].equals("Error")) {
                    errorStr = line[1];
                    JOptionPane.showMessageDialog(parent.getFrame(), errorStr, "Error", JOptionPane.ERROR_MESSAGE);
                    returnStr = "";
                }
            }
        } catch (Exception e) {
            MyJOptionPane.showMessageDialog(parent.getFrame(), "Error while trying to get information about executable of jobs" + "                are instrumented or not :\n\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE, true);
            returnStr = "";
        }
        return returnStr;
    }

    private void changeMode(int mode) {
        parent.setMode(mode);
        parent.getMyMenuBar().setMenu(mode);
        parent.getMyButtonBar().setButtonBar(mode);
    }

    public void setInitMode() {
        changeMode(parent.getMode());
        parent.getGraph().setWorkflowTitle();
    }

    public void doViewMode() {
        if (parent.getMode() != WorkflowEditor.MODE_VIEW) {
            changeMode(WorkflowEditor.MODE_VIEW);
            parent.getGraph().setWorkflowTitle();
        }
    }

    public void doEditMode() {
        if (parent.getMode() != WorkflowEditor.MODE_EDIT) {
        }
    }

    public void getParametersFromServerAtStartup() {
        cmdGetParametersFromServerAtStartup();
        if (parent.getUsersDir().equals("")) {
            System.out.println("No users dir params.");
        } else if (parent.getDebugErrorMessage().equals("")) {
            parent.setDebugErrorMessage("off");
        }
    }

    private void checkWorkflowParametersFromServer() {
        if (!Command.cmdGetWorkflowParameters(parent)) {
            JOptionPane.showMessageDialog(parent.getFrame(), "    No grid name has been set up for this workflow.\n", "Error", JOptionPane.ERROR_MESSAGE);
            this.doWorkflowProperties();
        }
    }

    private boolean checkGridForJobsFromServer() {
        return Command.cmdGetGridForJobs(parent);
    }

    public String copyDemoWorkflowToUsersDir() {
        return cmdCopyDemoWorkflowToUsersDir();
    }

    private void loadWorkflowInViewMode() {
        String attachedWorkflowStr = cmdGetAttachedWrkFileContent();
        if (!attachedWorkflowStr.equals("")) {
            parent.newGraph();
            changeMode(WorkflowEditor.MODE_VIEW);
            parent.getGraph().setWorkflowTitle();
            parent.getWKFParser().parseStr(attachedWorkflowStr, parent.getAttachedWorkflowName());
            if (!parent.getWKFParser().getIsParseError()) {
                parent.getGraph().checkGraphFilesExistInServer();
                parent.getGraph().setUpFileNameAtLoad();
                if (!parent.getWKFParser().isInstrumentedInfoAvilable()) parent.getGraph().checkJobExecutableIsInstrumented();
                boolean isCheckGridSuccess = this.checkGridForJobsFromServer();
                parent.getGraph().setWorkflowName(parent.getAttachedWorkflowName());
                parent.getGraph().setIsSaved(true);
                parent.getGraph().setIsNewWorkflow(false);
                if (parent.getGraph().ps_isPSGraph()) {
                    this.ps_cmdGetPSProps();
                }
                if (parent.getIsSupportBroker()) {
                    jdl_cmdOpenJDL();
                }
                ps_downloadGeneratorJobs();
                if (!isCheckGridSuccess) {
                    JOptionPane.showMessageDialog(parent.getFrame(), "    No grid name has been set up for some jobs.\n", "Warning", JOptionPane.WARNING_MESSAGE);
                    this.doWorkflowProperties();
                }
                startAutomaticRefresh();
                doRefresh();
            } else createNewWorkflow();
        } else createNewWorkflow();
    }

    private void loadWorkflowInRescueMode() {
        String attachedWorkflowStr = cmdGetAttachedWrkFileContent();
        if (!attachedWorkflowStr.equals("")) {
            parent.newGraph();
            changeMode(WorkflowEditor.MODE_VIEW);
            parent.getGraph().setWorkflowTitle();
            parent.getWKFParser().parseStr(attachedWorkflowStr, parent.getAttachedWorkflowName());
            if (!parent.getWKFParser().getIsParseError()) {
                parent.getGraph().checkGraphFilesExistInServer();
                parent.getGraph().setUpFileNameAtLoad();
                if (!parent.getWKFParser().isInstrumentedInfoAvilable()) parent.getGraph().checkJobExecutableIsInstrumented();
                boolean isCheckGridSuccess = this.checkGridForJobsFromServer();
                parent.getGraph().setWorkflowName(parent.getAttachedWorkflowName());
                parent.getGraph().setIsSaved(true);
                parent.getGraph().setIsNewWorkflow(false);
                if (parent.getGraph().ps_isPSGraph()) {
                    this.ps_cmdGetPSProps();
                }
                if (parent.getIsSupportBroker()) {
                    jdl_cmdOpenJDL();
                }
                ps_downloadGeneratorJobs();
                if (!isCheckGridSuccess) {
                    JOptionPane.showMessageDialog(parent.getFrame(), "    No grid name has been set up for some jobs.\n", "Warning", JOptionPane.WARNING_MESSAGE);
                    this.doWorkflowProperties();
                }
                startAutomaticRefresh();
                doRefresh();
            } else createNewWorkflow();
        } else createNewWorkflow();
    }

    private void loadWorkflowInEditMode() {
        String attachedWorkflowStr = cmdGetAttachedWrkFileContent();
        if (!attachedWorkflowStr.equals("")) {
            parent.newGraph();
            changeMode(WorkflowEditor.MODE_EDIT);
            parent.getGraph().setJobColorDefault();
            parent.getGraph().setWorkflowTitle();
            parent.getWKFParser().parseStr(attachedWorkflowStr, parent.getAttachedWorkflowName());
            if (!parent.getWKFParser().getIsParseError()) {
                parent.getGraph().checkGraphFilesExistInServer();
                parent.getGraph().setUpFileNameAtLoad();
                if (!parent.getWKFParser().isInstrumentedInfoAvilable()) parent.getGraph().checkJobExecutableIsInstrumented();
                boolean isCheckGridSuccess = this.checkGridForJobsFromServer();
                parent.getGraph().setWorkflowName(parent.getAttachedWorkflowName());
                parent.getGraph().setIsSaved(true);
                parent.getGraph().setIsNewWorkflow(false);
                if (parent.getGraph().ps_isPSGraph()) {
                    this.ps_cmdGetPSProps();
                }
                if (parent.getIsSupportBroker()) {
                    jdl_cmdOpenJDL();
                }
                ps_downloadGeneratorJobs();
                if (!isCheckGridSuccess) {
                    JOptionPane.showMessageDialog(parent.getFrame(), "    No grid name has been set up for some jobs.\n", "Warning", JOptionPane.WARNING_MESSAGE);
                    this.doWorkflowProperties();
                }
                startAutomaticRefresh();
                doRefresh();
            } else createNewWorkflow();
        } else createNewWorkflow();
    }

    public void doConnectToWorkflow() {
        System.out.println("doConnectToWorkflow() AttachedWorkflowName:" + parent.getAttachedWorkflowName());
        String status = cmdGetWorkflowStatus(parent.getAttachedWorkflowName());
        if (status.equals("WORKFLOW_NOT_EXIST")) {
            JOptionPane.showMessageDialog(parent.getFrame(), "Cannot attached to " + parent.getAttachedWorkflowName() + " because this workflow does not exist!", "Error", JOptionPane.ERROR_MESSAGE);
        } else if (status.equals("RUNNING")) {
            loadWorkflowInViewMode();
        } else if (status.equals("RESCUE")) {
            loadWorkflowInRescueMode();
        } else if (status.equals("NOT_RUNNING")) {
            loadWorkflowInEditMode();
        }
    }

    public void resetCounter() {
        newJobJobNumber = 1;
    }

    private JDLList jdl_getJobsJDLList() {
        JDLList returnList = new JDLList(parent.getAttachedWorkflowName());
        java.util.ArrayList jobList = this.parent.getGraph().getJobList();
        for (int i = 0; i < jobList.size(); i++) {
            Job job = (Job) jobList.get(i);
            JDLDocument jdl = (JDLDocument) job.getJobProperties().get("jdl");
            if (jdl != null) {
                jdl.setJobName(job.getName());
                returnList.addJDL(jdl);
            }
        }
        return returnList;
    }

    /**
     * This method regenerates the job's jdl
     */
    private void jdl_reGenerateJdls() {
        java.util.ArrayList jobList = this.parent.getGraph().getJobList();
        for (int i = 0; i < jobList.size(); i++) {
            Job job = (Job) jobList.get(i);
            if (job.getJobProperties().get("jdl") != null || JDLValidator.isBrokerResource(job.getJobProperties("grid"))) {
                JDLGenerator generator = new JDLGenerator(job);
                generator.generate();
            }
        }
    }
}
