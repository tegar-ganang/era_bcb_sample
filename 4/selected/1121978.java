package org.wsmostudio.integration.ssb.ui;

import java.io.File;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.wsmostudio.integration.ssb.deployment.Activator;
import org.wsmostudio.integration.ssb.deployment.IOUtils;
import org.wsmostudio.runtime.LogManager;

public class SelectProjectPage extends WizardPage {

    private Text bpmoLabel, sbpelLabel, xmlLabel;

    private Text resultProjectField;

    private Combo projectProcessDirSelector;

    private Label statusLabel;

    private File bpmoFile, sbpelFile, xmlFile = null;

    private File bundleFile = null;

    private IProject targetProject = null;

    private String tagretFolder = null;

    public SelectProjectPage() {
        super("Select Target Project");
    }

    public boolean isPageComplete() {
        return bundleFile != null;
    }

    public void createControl(Composite parent) {
        Composite mainPanel = new Composite(parent, SWT.NONE);
        mainPanel.setLayout(new GridLayout(1, false));
        Group inputsPanel = new Group(mainPanel, SWT.NONE);
        inputsPanel.setText("Input Artefacts");
        inputsPanel.setLayout(new GridLayout(2, false));
        inputsPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        new Label(inputsPanel, SWT.NONE).setText("BPMO Process : ");
        bpmoLabel = new Text(inputsPanel, SWT.READ_ONLY | SWT.BORDER);
        bpmoLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        new Label(inputsPanel, SWT.NONE).setText("sBPEL Process : ");
        sbpelLabel = new Text(inputsPanel, SWT.READ_ONLY | SWT.BORDER);
        sbpelLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        new Label(inputsPanel, SWT.NONE).setText("BPEL4SWS Process : ");
        xmlLabel = new Text(inputsPanel, SWT.READ_ONLY | SWT.BORDER);
        xmlLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Group resultProjectPanel = new Group(mainPanel, SWT.NONE);
        resultProjectPanel.setText("Target SPAB Project");
        resultProjectPanel.setLayout(new GridLayout(3, false));
        resultProjectPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        new Label(resultProjectPanel, SWT.NONE).setText("Project : ");
        resultProjectField = new Text(resultProjectPanel, SWT.READ_ONLY | SWT.BORDER);
        resultProjectField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Button selectProjectBut = new Button(resultProjectPanel, SWT.NONE);
        selectProjectBut.setText("Select Project");
        selectProjectBut.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                IProject selection = GUIUtils.selectIProject(getShell());
                if (selection == null) {
                    return;
                }
                if (selection.equals(targetProject)) {
                    return;
                }
                targetProject = selection;
                resultProjectField.setText(targetProject.getName());
                deleteBundleFile();
                updateProjectDirChooser();
                updateBundleUI();
                getWizard().getContainer().updateButtons();
            }
        });
        new Label(resultProjectPanel, SWT.NONE).setText("Processes directory : ");
        projectProcessDirSelector = new Combo(resultProjectPanel, SWT.READ_ONLY);
        projectProcessDirSelector.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        projectProcessDirSelector.setEnabled(false);
        projectProcessDirSelector.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                if (projectProcessDirSelector.getText().equals(tagretFolder)) {
                    return;
                }
                deleteBundleFile();
                tagretFolder = projectProcessDirSelector.getText();
                updateBundleUI();
                getWizard().getContainer().updateButtons();
            }
        });
        Composite buildPanel = new Composite(mainPanel, SWT.NONE);
        buildPanel.setLayout(new GridLayout(1, false));
        buildPanel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
        Button buildBut = new Button(buildPanel, SWT.NONE);
        buildBut.setText(" Build SPAB ");
        buildBut.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                buildBundle();
            }
        });
        buildBut.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
        statusLabel = new Label(buildPanel, SWT.NONE);
        updateBundleUI();
        statusLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
        setControl(mainPanel);
    }

    public String getMessage() {
        return "Select Target Project";
    }

    public String getTitle() {
        return "SBP Deployment Wizard";
    }

    public void setVisible(boolean visible) {
        if (visible) {
            String bpmoText;
            if (getBPMOFile() != null) {
                bpmoText = getBPMOFile().getName();
            } else {
                bpmoText = " <not specified> ";
            }
            String sbpelText;
            File sourceSBPEL = getSBPELFile();
            if (sourceSBPEL != null) {
                sbpelText = sourceSBPEL.getName();
            } else {
                sbpelText = "";
            }
            String xmlText;
            File xmlFile = getBPEL4SWSFile();
            if (xmlFile != null) {
                xmlText = xmlFile.getName();
            } else {
                xmlText = "";
            }
            bpmoLabel.setText(bpmoText);
            sbpelLabel.setText(sbpelText);
            xmlLabel.setText(xmlText);
            if (false == validateBundle()) {
                deleteBundleFile();
                bpmoFile = null;
                sbpelFile = null;
                xmlFile = null;
                updateBundleUI();
            }
            getWizard().getContainer().updateButtons();
        }
        super.setVisible(visible);
    }

    public File getSBPELFile() {
        return ((sBPEL2BPEL4SWSPage) getPreviousPage()).getSBPELFile();
    }

    public File getBPMOFile() {
        return ((sBPEL2BPEL4SWSPage) getPreviousPage()).getBPMOFile();
    }

    public File getBPEL4SWSFile() {
        return ((sBPEL2BPEL4SWSPage) getPreviousPage()).getBPEL4SWSFile();
    }

    public File getBundleFile() {
        return this.bundleFile;
    }

    public void deleteBundleFile() {
        if (bundleFile == null || false == bundleFile.exists()) {
            return;
        }
        bundleFile.delete();
        bundleFile = null;
    }

    private void buildBundle() {
        if (targetProject == null) {
            MessageDialog.openError(getShell(), "Error", "No target SPAB project selected!");
            return;
        }
        if (projectProcessDirSelector.getText().trim().length() == 0) {
            MessageDialog.openError(getShell(), "Error", "No process directory selected for project " + targetProject.getName() + "!");
            return;
        }
        deleteBundleFile();
        try {
            File projectDir = targetProject.getLocation().toFile();
            File projectProcessesDir = new File(projectDir, projectProcessDirSelector.getText());
            boolean bpmoCopied = IOUtils.copyProcessFilesSecure(getBPMOFile(), projectProcessesDir);
            boolean sbpelCopied = IOUtils.copyProcessFilesSecure(getSBPELFile(), projectProcessesDir);
            boolean xmlCopied = IOUtils.copyProcessFilesSecure(getBPEL4SWSFile(), projectProcessesDir);
            bundleFile = IOUtils.archiveBundle(projectDir, Activator.getDefault().getStateLocation().toFile());
            if (bpmoCopied) {
                new File(projectProcessesDir, getBPMOFile().getName()).delete();
            }
            if (sbpelCopied) {
                new File(projectProcessesDir, getSBPELFile().getName()).delete();
            }
            if (xmlCopied) {
                new File(projectProcessesDir, getBPEL4SWSFile().getName()).delete();
            }
        } catch (Throwable anyError) {
            LogManager.logError(anyError);
            MessageDialog.openError(getShell(), "Error", "Error building SPAB :\n" + anyError.getMessage());
            updateBundleUI();
            return;
        }
        bpmoFile = getBPMOFile();
        sbpelFile = getSBPELFile();
        xmlFile = getBPEL4SWSFile();
        updateBundleUI();
        getWizard().getContainer().updateButtons();
    }

    private boolean validateBundle() {
        if (bundleFile == null) {
            return false;
        }
        if (bpmoFile != null) {
            if (false == bpmoFile.equals(getBPMOFile())) {
                return false;
            }
        } else {
            if (getBPMOFile() != null) {
                return false;
            }
        }
        if (sbpelFile != null) {
            if (false == sbpelFile.equals(getSBPELFile())) {
                return false;
            }
        } else {
            if (getSBPELFile() != null) {
                return false;
            }
        }
        if (xmlFile != null) {
            if (false == xmlFile.equals(getBPEL4SWSFile())) {
                return false;
            }
        } else {
            if (getBPEL4SWSFile() != null) {
                return false;
            }
        }
        return true;
    }

    private void updateProjectDirChooser() {
        projectProcessDirSelector.removeAll();
        if (targetProject == null) {
            projectProcessDirSelector.setEnabled(false);
            return;
        }
        File projectDir = targetProject.getLocation().toFile();
        String[] files = projectDir.list();
        projectProcessDirSelector.setEnabled(true);
        int selectionIndex = -1;
        for (int i = 0; i < files.length; i++) {
            File testDir = new File(projectDir, files[i]);
            if (false == testDir.isDirectory()) {
                continue;
            }
            projectProcessDirSelector.add(testDir.getName());
            if (testDir.getName().toLowerCase().startsWith("sbpelee-su")) {
                selectionIndex = projectProcessDirSelector.getItemCount() - 1;
            }
        }
        if (selectionIndex != -1) {
            projectProcessDirSelector.select(selectionIndex);
        }
    }

    private void updateBundleUI() {
        statusLabel.setText((bundleFile != null) ? "Bundle built successfully" : "   Bundle not build yet   ");
        statusLabel.setForeground((bundleFile != null) ? Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN) : Display.getCurrent().getSystemColor(SWT.COLOR_RED));
    }
}
