package com.rapidminer.gui.tools.dialogs.wizards.dataimport.excel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import com.rapidminer.gui.tools.SimpleFileFilter;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.gui.tools.dialogs.wizards.WizardStep;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.DataImportWizard;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.FileSelectionWizardStep;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.MetaDataDeclerationWizardStep;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.RepositoryLocationSelectionWizardStep;
import com.rapidminer.gui.wizards.AbstractConfigurationWizardCreator;
import com.rapidminer.gui.wizards.ConfigurationListener;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.io.ExcelExampleSource;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.Parameters;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.OperatorService;

/**
 * @author Tobias Malbrecht, Sebastian Loh
 */
public class ExcelImportWizard extends DataImportWizard {

    private static final long serialVersionUID = -4308448171060612833L;

    private File file = null;

    private ExcelExampleSource reader = null;

    private Parameters parametersBackup;

    private final WizardStep STEP_FILE_SELECTION = new FileSelectionWizardStep(this, new SimpleFileFilter("Excel File (.xls)", ".xls")) {

        @Override
        protected boolean performEnteringAction(WizardStepDirection direction) {
            if (file != null && file.exists()) {
                this.fileChooser.setSelectedFile(file);
            }
            return true;
        }

        @Override
        protected boolean performLeavingAction(WizardStepDirection direction) {
            ExcelImportWizard.this.reader.setParameter(ExcelExampleSource.PARAMETER_ANNOTATIONS, null);
            file = getSelectedFile();
            File oldFile = null;
            try {
                oldFile = reader.getParameterAsFile(ExcelExampleSource.PARAMETER_EXCEL_FILE);
            } catch (UndefinedParameterError e) {
                oldFile = null;
            }
            if (oldFile == null || !oldFile.equals(file)) {
                reader.clearAllReaderSettings();
            }
            reader.setParameter(ExcelExampleSource.PARAMETER_EXCEL_FILE, file.getAbsolutePath());
            return true;
        }
    };

    private static class ExcelWorkSheetSelection extends WizardStep {

        private final ExcelWorkbookPane workbookSelectionPanel;

        private final JLabel errorLabel = new JLabel("");

        ExcelExampleSource reader;

        public ExcelWorkSheetSelection(ExcelExampleSource reader) {
            super("excel_data_selection");
            this.reader = reader;
            this.workbookSelectionPanel = new ExcelWorkbookPane(this, reader);
        }

        @Override
        protected boolean canGoBack() {
            return true;
        }

        @Override
        protected boolean canProceed() {
            return true;
        }

        @Override
        protected boolean performEnteringAction(WizardStepDirection direction) {
            reader.stopReading();
            reader.setParameter(ExcelExampleSource.PARAMETER_FIRST_ROW_AS_NAMES, Boolean.FALSE.toString());
            reader.skipNameAnnotationRow(false);
            boolean flag = reader.attributeNamesDefinedByUser();
            workbookSelectionPanel.loadWorkbook();
            reader.setAttributeNamesDefinedByUser(flag);
            return true;
        }

        @Override
        protected boolean performLeavingAction(WizardStepDirection direction) {
            if (reader.attributeNamesDefinedByUser()) {
                reader.loadMetaDataFromParameters();
            }
            reader.stopReading();
            reader.setParameter(ExcelExampleSource.PARAMETER_SHEET_NUMBER, Integer.toString(workbookSelectionPanel.getSelection().getSheetIndex() + 1));
            List<String[]> annotationParameter = new LinkedList<String[]>();
            boolean nameAnnotationFound = false;
            for (Map.Entry<Integer, String> entry : workbookSelectionPanel.getSelection().getAnnotationMap().entrySet()) {
                annotationParameter.add(new String[] { entry.getKey().toString(), entry.getValue() });
                if (entry.getValue().equals(Annotations.ANNOTATION_NAME)) {
                    nameAnnotationFound = true;
                }
            }
            reader.setParameter(ExcelExampleSource.PARAMETER_ANNOTATIONS, ParameterTypeList.transformList2String(annotationParameter));
            if (nameAnnotationFound) {
                reader.setAttributeNamesDefinedByUser(false);
                reader.skipNameAnnotationRow(false);
            } else {
                reader.skipNameAnnotationRow(true);
            }
            return true;
        }

        @Override
        protected JComponent getComponent() {
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(workbookSelectionPanel, BorderLayout.CENTER);
            panel.add(errorLabel, BorderLayout.SOUTH);
            return panel;
        }
    }

    @Override
    public void cancel() {
        reader.getParameters().setAll(parametersBackup);
        reader.stopReading();
        super.cancel();
    }

    @Override
    public void finish() {
        reader.stopReading();
        super.finish();
    }

    public ExcelImportWizard(String i18nKey, ConfigurationListener listener, File preselectedFile, final boolean showStoreInRepositoryStep, RepositoryLocation preselectedLocation, Object... i18nArgs) {
        super(i18nKey, i18nArgs);
        file = preselectedFile;
        if (listener != null) {
            reader = (ExcelExampleSource) listener;
        } else {
            try {
                reader = OperatorService.createOperator(com.rapidminer.operator.io.ExcelExampleSource.class);
            } catch (OperatorCreationException e) {
                throw new RuntimeException("Failed to create excel reader: " + e, e);
            }
        }
        parametersBackup = (Parameters) reader.getParameters().clone();
        addStep(STEP_FILE_SELECTION);
        addStep(new ExcelWorkSheetSelection(reader));
        addStep(new MetaDataDeclerationWizardStep("select_attributes", reader) {

            @Override
            protected JComponent getComponent() {
                JPanel typeDetection = new JPanel(ButtonDialog.createGridLayout(1, 2));
                typeDetection.setBorder(ButtonDialog.createTitledBorder("Value Type Detection"));
                typeDetection.add(new JLabel("Guess the value types of all attributes"));
                typeDetection.add(guessingButtonsPanel);
                Component[] superComponents = super.getComponent().getComponents();
                JPanel upperPanel = new JPanel(new BorderLayout());
                upperPanel.add(typeDetection, BorderLayout.NORTH);
                upperPanel.add(superComponents[0], BorderLayout.CENTER);
                JPanel panel = new JPanel(new BorderLayout(0, ButtonDialog.GAP));
                panel.add(upperPanel, BorderLayout.NORTH);
                panel.add(superComponents[1], BorderLayout.CENTER);
                return panel;
            }

            @Override
            protected void doAfterEnteringAction() {
                reader.setAttributeNamesDefinedByUser(true);
                ((ExcelExampleSource) reader).skipNameAnnotationRow(true);
            }

            @Override
            protected boolean performLeavingAction(WizardStepDirection direction) {
                reader.stopReading();
                reader.writeMetaDataInParameter();
                return true;
            }
        });
        if (showStoreInRepositoryStep) {
            addStep(new RepositoryLocationSelectionWizardStep(this, preselectedLocation != null ? preselectedLocation.getAbsoluteLocation() : null) {

                @Override
                protected boolean performLeavingAction(WizardStepDirection direction) {
                    synchronized (reader) {
                        boolean flag = transferData(reader, getRepositoryLocation());
                        return flag;
                    }
                }
            });
        }
        layoutDefault(HUGE);
    }

    public ExcelImportWizard(String i18nKey, Object... i18nArgs) {
        this(i18nKey, (ConfigurationListener) null, (File) null, true, (RepositoryLocation) null, i18nArgs);
    }

    public ExcelImportWizard(String i18nKey, File preselectedFile, RepositoryLocation preselectedLocation, Object... i18nArgs) {
        this(i18nKey, preselectedFile, true, preselectedLocation, i18nArgs);
    }

    public ExcelImportWizard(String i18nKey, File preselectedFile, ConfigurationListener listener, Object... i18nArgs) {
        this(i18nKey, preselectedFile, false, null, i18nArgs);
    }

    public ExcelImportWizard(String i18nKey, ExcelExampleSource reader, Object... i18nArgs) {
        super(i18nKey, i18nArgs);
        this.reader = reader;
        addStep(STEP_FILE_SELECTION);
        addStep(new ExcelWorkSheetSelection(reader));
        layoutDefault(LARGE);
    }

    /**
	 * Creates a {@link ExcelImportWizard}.
	 * 
	 * @author Sebastian Loh (06.05.2010)
	 * 
	 */
    public static class ExcelExampleSourceConfigurationWizardCreator extends AbstractConfigurationWizardCreator {

        private static final long serialVersionUID = 1L;

        @Override
        public void createConfigurationWizard(ParameterType type, ConfigurationListener listener) {
            String fileLocation = "";
            try {
                fileLocation = listener.getParameters().getParameter(ExcelExampleSource.PARAMETER_EXCEL_FILE);
                if (fileLocation == null) {
                    throw new UndefinedParameterError("");
                }
                File file = new File(fileLocation);
                (new ExcelImportWizard(getI18NKey(), listener, file, false, null)).setVisible(true);
            } catch (UndefinedParameterError e) {
                (new ExcelImportWizard(getI18NKey(), listener, null, false, null)).setVisible(true);
            }
        }

        @Override
        public String getI18NKey() {
            return "data_import_wizard";
        }
    }
}
