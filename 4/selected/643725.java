package gov.sns.apps.energymanager;

import gov.sns.application.*;
import gov.sns.xal.smf.application.*;
import gov.sns.xal.smf.*;
import gov.sns.tools.data.*;
import gov.sns.tools.apputils.SimpleProbeEditor;
import gov.sns.tools.apputils.files.RecentFileTracker;
import javax.swing.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.*;
import javax.swing.event.*;
import java.text.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.io.*;

/** Main window. */
public class EnergyManagerWindow extends AcceleratorWindow implements EnergyManagerListener, SwingConstants {

    /** date format for message view */
    private static final SimpleDateFormat MESSAGE_DATE_FORMAT;

    /** table of parameters */
    private final JTable _parameterTable;

    /** table model for displaying the live parameters */
    private final LiveParameterTableModel _parameterTableModel;

    /** view for displaying messages */
    private final JLabel _messageView;

    /** live parameter view */
    private LiveParameterInspector _parameterInspector;

    /** sort ordering for ordering the live parameters */
    private SortOrdering _parameterOrdering;

    /** dialog for configuring a solve */
    protected SolverConfigDialog _solverConfigDialog;

    /** dialog for specifying the evaluaton node range in terms of position */
    protected EvaluationRangeDialog _evaluationRangeDialog;

    /** a dialog for specifying simple energy scaling information */
    protected SimpleEnergyScaleDialog _energyScaleDialog;

    /** optimizer window */
    protected OptimizerWindow _optimizerWindow;

    /** optimal results file chooser */
    protected JFileChooser _exportsFileChooser;

    /** file tracker to keep track of file exports */
    protected RecentFileTracker _exportsTracker;

    /** file imports file chooser */
    protected JFileChooser _importsFileChooser;

    /** file tracker to keep track of file imports */
    protected RecentFileTracker _importsTracker;

    static {
        MESSAGE_DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss");
    }

    /** Constructor */
    public EnergyManagerWindow(final XalDocument aDocument) {
        super(aDocument);
        _parameterOrdering = new SortOrdering(NodeAgent.POSITION_KEY);
        _parameterTableModel = new LiveParameterTableModel();
        _parameterTable = new JTable(_parameterTableModel);
        _messageView = new JLabel("messages...");
        makeContents();
        setupParameterTable();
    }

    /**
	 * Overrides the super class method to disable the toolbar.
	 * @return false
     */
    @Override
    public boolean usesToolbar() {
        return false;
    }

    /**
	 * Get the energy manager document.
	 * @return this window's document as an energy manager document
	 */
    protected EnergyManagerDocument getEnergyManagerDocument() {
        return (EnergyManagerDocument) document;
    }

    /**
	 * Get the energy manager model.
	 * @return the energy manager model
	 */
    protected EnergyManager getModel() {
        return getEnergyManagerDocument().getModel();
    }

    /** Make the window contents */
    private void makeContents() {
        setSize(1200, 900);
        final Box mainView = new Box(BoxLayout.Y_AXIS);
        getContentPane().add(mainView);
        mainView.add(makeQualifierView());
        mainView.add(new JScrollPane(_parameterTable));
        mainView.add(makeMessageView());
    }

    /**
	 * Register actions for the custom menu items.
     * @param commander The commander with which to register the custom commands.
     */
    @Override
    protected void customizeCommands(final Commander commander) {
        final Action specifyEvaluationRangeAction = new AbstractAction("specify-evaluation-range") {

            public void actionPerformed(final ActionEvent event) {
                if (_evaluationRangeDialog == null) {
                    _evaluationRangeDialog = new EvaluationRangeDialog(EnergyManagerWindow.this);
                }
                final double[] range = _evaluationRangeDialog.present(getEnergyManagerDocument().getModel().getEvaluationRange());
                if (range != null) {
                    try {
                        getEnergyManagerDocument().getModel().setEvaluationRange(range[0], range[1]);
                    } catch (Exception exception) {
                        displayError(exception);
                    }
                }
            }
        };
        commander.registerAction(specifyEvaluationRangeAction);
        final Action editEntranceProbeAction = new AbstractAction("edit-entrance-probe") {

            public void actionPerformed(final ActionEvent event) {
                try {
                    final SimpleProbeEditor probeEditor = new SimpleProbeEditor(EnergyManagerWindow.this);
                    final JDialog probeEditorDialog = probeEditor.createSimpleProbeEditor(getModel().getEntranceProbe());
                    getModel().setEntranceProbe(probeEditor.getProbe());
                } catch (Exception exception) {
                    displayError(exception);
                }
            }
        };
        commander.registerAction(editEntranceProbeAction);
        final Action importCavitySettingsAction = new AbstractAction("import-cavity-settings") {

            public void actionPerformed(final ActionEvent event) {
                importRFCavitySettings();
            }
        };
        commander.registerAction(importCavitySettingsAction);
        final Action applyBestLongitudinalGuessAction = new AbstractAction("apply-best-longitudinal-guess") {

            public void actionPerformed(final ActionEvent event) {
                postMessage("Begin calculating best longitudinal guess...");
                getEnergyManagerDocument().getModel().guessRFPhaseToPreserveLongitudinalFocusing();
                postMessage("Applied best longitudinal guess...");
            }
        };
        commander.registerAction(applyBestLongitudinalGuessAction);
        final Action applyBestTransverseGuessAction = new AbstractAction("apply-best-transverse-guess") {

            public void actionPerformed(final ActionEvent event) {
                postMessage("Begin calculating best transverse guess...");
                getEnergyManagerDocument().getModel().scaleMagnetFieldsToEnergy();
                postMessage("Applied best transverse guess...");
            }
        };
        commander.registerAction(applyBestTransverseGuessAction);
        final Action scaleMagnetsForEnergyChangeAction = new AbstractAction("scale-magnets-for-energy-change") {

            public void actionPerformed(final ActionEvent event) {
                if (_energyScaleDialog == null) {
                    _energyScaleDialog = new SimpleEnergyScaleDialog(EnergyManagerWindow.this, 1000.0);
                }
                if (_energyScaleDialog.present()) {
                    try {
                        final String source = _energyScaleDialog.getSource();
                        final double initialKineticEnergy = _energyScaleDialog.getInitialKineticEnergy();
                        final double targetKineticEnergy = _energyScaleDialog.getTargetKineticEnergy();
                        getEnergyManagerDocument().getModel().scaleMagneticFieldsForEnergyChange(source, initialKineticEnergy, targetKineticEnergy);
                        postMessage("Scaled magnets for the specified energy change...");
                    } catch (Exception exception) {
                        displayError(exception);
                    }
                }
            }
        };
        commander.registerAction(scaleMagnetsForEnergyChangeAction);
        final Action importOptimalValuesAction = new AbstractAction("import-optimal-values") {

            public void actionPerformed(final ActionEvent event) {
                getEnergyManagerDocument().getModel().importOptimalValues();
                postMessage("Optimal values imported...");
            }
        };
        commander.registerAction(importOptimalValuesAction);
        final Action uploadMagnetFieldsAction = new AbstractAction("upload-magnet-fields") {

            public void actionPerformed(final ActionEvent event) {
                uploadMagnetFields();
            }
        };
        commander.registerAction(uploadMagnetFieldsAction);
        final Action exportParametersAction = new AbstractAction("export-parameters") {

            public void actionPerformed(final ActionEvent event) {
                exportParameters();
            }
        };
        commander.registerAction(exportParametersAction);
        final Action exportOpticsAction = new AbstractAction("export-optics") {

            public void actionPerformed(final ActionEvent event) {
                exportOpticsChanges();
            }
        };
        commander.registerAction(exportOpticsAction);
        final Action makeSelectedVariableAction = new AbstractAction("make-selected-variable") {

            public void actionPerformed(final ActionEvent event) {
                setAsVariable(getSelectedParameters(), true);
            }
        };
        commander.registerAction(makeSelectedVariableAction);
        final Action makeSelectedFixedAction = new AbstractAction("make-selected-fixed") {

            public void actionPerformed(final ActionEvent event) {
                setAsVariable(getSelectedParameters(), false);
            }
        };
        commander.registerAction(makeSelectedFixedAction);
        final Action makeDisabledFixedAction = new AbstractAction("make-disabled-fixed") {

            public void actionPerformed(final ActionEvent event) {
                getEnergyManagerDocument().getModel().freezeParametersOfDisabledCavities();
            }
        };
        commander.registerAction(makeDisabledFixedAction);
        final Action useSelectedDesignAction = new AbstractAction("use-selected-design") {

            public void actionPerformed(final ActionEvent event) {
                setParameterSource(getSelectedParameters(), LiveParameter.DESIGN_SOURCE);
            }
        };
        commander.registerAction(useSelectedDesignAction);
        final Action useSelectedControlAction = new AbstractAction("use-selected-control") {

            public void actionPerformed(final ActionEvent event) {
                setParameterSource(getSelectedParameters(), LiveParameter.CONTROL_SOURCE);
            }
        };
        commander.registerAction(useSelectedControlAction);
        final Action useSelectedCustomAction = new AbstractAction("use-selected-custom") {

            public void actionPerformed(final ActionEvent event) {
                setParameterSource(getSelectedParameters(), LiveParameter.CUSTOM_SOURCE);
            }
        };
        commander.registerAction(useSelectedCustomAction);
        final Action copySelectedDesignAction = new AbstractAction("copy-selected-design") {

            public void actionPerformed(final ActionEvent event) {
                copyDesignSettings(getSelectedParameters());
            }
        };
        commander.registerAction(copySelectedDesignAction);
        final Action copySelectedControlAction = new AbstractAction("copy-selected-control") {

            public void actionPerformed(final ActionEvent event) {
                copyControlSettings(getSelectedParameters());
            }
        };
        commander.registerAction(copySelectedControlAction);
        final Action copySelectedControlLimitsAction = new AbstractAction("copy-selected-control-limits") {

            public void actionPerformed(final ActionEvent event) {
                copyControlLimits(getSelectedParameters());
            }
        };
        commander.registerAction(copySelectedControlLimitsAction);
        final Action solveAction = new AbstractAction("solve") {

            public void actionPerformed(final ActionEvent event) {
                showSolverConfigDialog();
            }
        };
        commander.registerAction(solveAction);
        final Action evaluateAction = new AbstractAction("evaluate") {

            public void actionPerformed(final ActionEvent event) {
                getModel().getOptimizer().evaluateInitialPoint();
            }
        };
        commander.registerAction(evaluateAction);
        final Action solvingProgressAction = new AbstractAction("optimization-progress") {

            public void actionPerformed(final ActionEvent event) {
                showOptimizerWindow();
            }
        };
        commander.registerAction(solvingProgressAction);
        final Action exportOptimalResultsAction = new AbstractAction("export-optimal-results") {

            public void actionPerformed(final ActionEvent event) {
                exportOptimalResults();
            }
        };
        commander.registerAction(exportOptimalResultsAction);
    }

    /** upload quadrupole fields to the control system */
    protected void uploadMagnetFields() {
        final String UPLOAD_ALL = "Upload All Magnetic Fields";
        final String UPLOAD_SELECTED = "Upload Selected Magnetic Fields";
        final String[] OPTIONS = { UPLOAD_ALL, UPLOAD_SELECTED };
        final Object selection = JOptionPane.showInputDialog(this, "Upload quadrupole fields to the accelerator? \n Please take a SCORE save set before continuing!", "Magnetic Field Upload", JOptionPane.QUESTION_MESSAGE, null, OPTIONS, UPLOAD_SELECTED);
        if (selection == null) return;
        final Qualifier qualifier = new KeyValueQualifier(LiveParameter.NAME_KEY, ElectromagnetFieldAdaptor.NAME);
        List<LiveParameter> parameters;
        if (selection == UPLOAD_ALL) {
            parameters = getEnergyManagerDocument().getModel().getLiveParameters(qualifier);
        } else if (selection == UPLOAD_SELECTED) {
            parameters = EnergyManager.getFilteredLiveParameters(getSelectedParameters(), qualifier);
        } else {
            return;
        }
        final int[] result = EnergyManager.uploadInitialValues(parameters);
        if (result[0] != result[1]) {
            final int numFailed = result[1] - result[0];
            displayWarning("Some update requests failed", numFailed + " of " + result[1] + " requests failed! \nSee log for details...");
        } else {
            JOptionPane.showMessageDialog(this, result[0] + " magnet field upload requests performed! \n Please take a SCORE save set!");
        }
    }

    /** 
	 * Copy the design initial value and limits of selected variables to their custom limits. 
	 * @param parameters the parameters on which to perform the action
	 */
    protected void copyDesignSettings(final List<LiveParameter> parameters) {
        final int count = parameters.size();
        for (int index = 0; index < count; index++) {
            final LiveParameter parameter = parameters.get(index);
            parameter.copyDesignToCustom();
        }
    }

    /** 
	 * Copy the control initial value and limits of selected variables to their custom limits. 
	 * @param parameters the parameters on which to perform the action
	 */
    protected void copyControlSettings(final List<LiveParameter> parameters) {
        final int count = parameters.size();
        for (int index = 0; index < count; index++) {
            final LiveParameter parameter = parameters.get(index);
            parameter.copyControlToCustom();
        }
    }

    /** 
	 * Copy the control limits of selected variables to their custom limits. 
	 * @param parameters the parameters on which to perform the action
	 */
    protected void copyControlLimits(final List<LiveParameter> parameters) {
        final int count = parameters.size();
        for (int index = 0; index < count; index++) {
            final LiveParameter parameter = parameters.get(index);
            parameter.copyControlLimitsToCustom();
        }
    }

    /**
	 * Set the selected parameters as variable or fixed depending on the value passed.
	 * @param parameters the parameters on which to perform the action
	 * @param shouldVary true for variable and false for fixed
	 */
    protected void setAsVariable(final List<LiveParameter> parameters, final boolean shouldVary) {
        final int count = parameters.size();
        for (int index = 0; index < count; index++) {
            final LiveParameter parameter = parameters.get(index);
            parameter.setIsVariable(shouldVary);
        }
    }

    /**
	 * Set the parameters to have the specified source.
	 * @param parameters the parameters on which to perform the action
	 * @param source DESIGN_SOURCE, CONTROL_SOURCE or CUSTOM_SOURCE
	 */
    protected void setParameterSource(final List<LiveParameter> parameters, final int source) {
        final int count = parameters.size();
        for (int index = 0; index < count; index++) {
            final LiveParameter parameter = parameters.get(index);
            parameter.setActiveSource(source);
        }
    }

    /** 
	 * Choose the file to which to export the results.
	 * @return the file to which to export results
	 */
    protected File chooseExportFile() {
        if (_exportsTracker == null) {
            _exportsTracker = new RecentFileTracker(1, this.getClass(), "EXPORT_URL");
        }
        if (_exportsFileChooser == null) {
            _exportsFileChooser = new JFileChooser();
            _exportsTracker.applyRecentFolder(_exportsFileChooser);
        }
        final int status = _exportsFileChooser.showSaveDialog(this);
        switch(status) {
            case JFileChooser.APPROVE_OPTION:
                break;
            default:
                return null;
        }
        final File file = _exportsFileChooser.getSelectedFile();
        if (file.exists()) {
            final int continueStatus = JOptionPane.showConfirmDialog(this, "The file: \"" + file.getPath() + "\" already exits!\n Overwrite this file?");
            switch(continueStatus) {
                case JOptionPane.YES_OPTION:
                    break;
                case JOptionPane.NO_OPTION:
                    return chooseExportFile();
                default:
                    return null;
            }
        }
        _exportsTracker.cacheURL(file);
        return file;
    }

    /** Export the current parameters. */
    protected void exportParameters() {
        final File file = chooseExportFile();
        if (file == null) return;
        try {
            final Writer writer = new FileWriter(file);
            getEnergyManagerDocument().getModel().exportInitialParameters(writer);
            writer.flush();
        } catch (java.io.IOException exception) {
            displayError("Write exception", "Error writing out parameters.", exception);
        }
    }

    /** Generate a new optics extra input file with the current parameter changes from the design. */
    protected void exportOpticsChanges() {
        final File file = chooseExportFile();
        if (file == null) return;
        try {
            OpticsExporter.exportChanges(getModel(), new FileWriter(file));
        } catch (java.io.IOException exception) {
            displayError("Write exception", "Error exporting the optics changes.", exception);
        }
    }

    /** Export the optimal results to the user selected file. */
    protected void exportOptimalResults() {
        final File file = chooseExportFile();
        if (file == null) return;
        try {
            final Writer writer = new FileWriter(file);
            getEnergyManagerDocument().getModel().exportOptimalResults(writer);
            writer.flush();
        } catch (java.io.IOException exception) {
            displayError("Write exception", "Error writing out optimal results.", exception);
        }
    }

    /** 
	 * Allow the user to choose the import file.
	 * @return the file to import
	 */
    protected File chooseImportFile() {
        if (_importsTracker == null) {
            _importsTracker = new RecentFileTracker(1, this.getClass(), "IMPORT_URL");
        }
        if (_importsFileChooser == null) {
            _importsFileChooser = new JFileChooser();
            _importsFileChooser.setMultiSelectionEnabled(false);
            _importsTracker.applyRecentFolder(_importsFileChooser);
        }
        final int status = _importsFileChooser.showOpenDialog(this);
        switch(status) {
            case JFileChooser.APPROVE_OPTION:
                break;
            default:
                return null;
        }
        final File file = _importsFileChooser.getSelectedFile();
        _importsTracker.cacheURL(file);
        return file;
    }

    /** Import the RF Amplitude and phase settings from a file selected by the user */
    protected void importRFCavitySettings() {
        final File file = chooseImportFile();
        if (file == null) return;
        final Map amplitudeTable = new HashMap();
        final Map phaseTable = new HashMap();
        try {
            final BufferedReader reader = new BufferedReader(new FileReader(file));
            while (reader.ready()) {
                try {
                    final String line = reader.readLine();
                    final StringTokenizer tokenizer = new StringTokenizer(line);
                    if (tokenizer.hasMoreTokens()) {
                        try {
                            final String cavityID = tokenizer.nextToken();
                            if (tokenizer.hasMoreTokens()) {
                                final double amplitude = Double.parseDouble(tokenizer.nextToken());
                                final double[] limits = new double[] { 0.0, amplitude };
                                final Map fieldSetting = new HashMap();
                                fieldSetting.put("value", amplitude);
                                fieldSetting.put("limits", limits);
                                amplitudeTable.put(cavityID, fieldSetting);
                                if (tokenizer.hasMoreTokens()) {
                                    final double cavityPhase = Double.parseDouble(tokenizer.nextToken());
                                    final Map phaseSetting = new HashMap();
                                    phaseSetting.put("value", cavityPhase);
                                    phaseTable.put(cavityID, phaseSetting);
                                }
                            }
                        } catch (NumberFormatException exception) {
                            displayError(exception);
                        }
                    }
                } catch (java.io.IOException exception) {
                    displayError(exception);
                }
            }
            final Qualifier amplitudeQualifier = new KeyValueQualifier(LiveParameter.NAME_KEY, RFCavityAgent.AMPLITUDE_ADAPTOR.getName());
            final List<LiveParameter> amplitudeParameters = getEnergyManagerDocument().getModel().getLiveParameters(amplitudeQualifier);
            getEnergyManagerDocument().getModel().loadCustomSettings(amplitudeTable, amplitudeParameters);
            final Qualifier phaseQualifier = new KeyValueQualifier(LiveParameter.NAME_KEY, RFCavityAgent.PHASE_ADAPTOR.getName());
            final List<LiveParameter> phaseParameters = getEnergyManagerDocument().getModel().getLiveParameters(phaseQualifier);
            getEnergyManagerDocument().getModel().loadCustomSettings(phaseTable, phaseParameters);
            postMessage("RF Amplitude and Phase settings imported from:  " + file.getCanonicalPath());
        } catch (java.io.FileNotFoundException exception) {
            displayError(exception);
        } catch (java.io.IOException exception) {
            displayError(exception);
        }
    }

    /**
	 * Make the view for filtering parameters.
	 */
    private Component makeQualifierView() {
        final int SPACE = 10;
        final Box view = new Box(BoxLayout.X_AXIS);
        view.setBorder(BorderFactory.createTitledBorder("Filter"));
        final LiveParameterQualifier qualifier = _parameterTableModel.getQualifier();
        final JCheckBox passFixedButton = new JCheckBox("Fixed");
        view.add(passFixedButton);
        passFixedButton.setSelected(qualifier.getPassFixed());
        passFixedButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent event) {
                qualifier.setPassFixed(passFixedButton.isSelected());
            }
        });
        final JCheckBox passVariableButton = new JCheckBox("Variable");
        view.add(passVariableButton);
        passVariableButton.setSelected(qualifier.getPassVariables());
        passVariableButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent event) {
                qualifier.setPassVariables(passVariableButton.isSelected());
            }
        });
        view.add(Box.createHorizontalStrut(SPACE));
        final JCheckBox passBendsButton = new JCheckBox("Bend Field");
        view.add(passBendsButton);
        passBendsButton.setSelected(qualifier.getPassBendField());
        passBendsButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent event) {
                qualifier.setPassBendField(passBendsButton.isSelected());
            }
        });
        view.add(Box.createHorizontalStrut(SPACE));
        final JCheckBox passQuadsButton = new JCheckBox("Quadrupole Field");
        view.add(passQuadsButton);
        passQuadsButton.setSelected(qualifier.getPassQuadField());
        passQuadsButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent event) {
                qualifier.setPassQuadField(passQuadsButton.isSelected());
            }
        });
        final JCheckBox passRFAmplitudeButton = new JCheckBox("RF Amplitude");
        view.add(passRFAmplitudeButton);
        passRFAmplitudeButton.setSelected(qualifier.getPassRFAmplitude());
        passRFAmplitudeButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent event) {
                qualifier.setPassRFAmplitude(passRFAmplitudeButton.isSelected());
            }
        });
        final JCheckBox passRFPhaseButton = new JCheckBox("RF Phase");
        view.add(passRFPhaseButton);
        passRFPhaseButton.setSelected(qualifier.getPassRFPhase());
        passRFPhaseButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent event) {
                qualifier.setPassRFPhase(passRFPhaseButton.isSelected());
            }
        });
        view.setMaximumSize(new Dimension(10000, view.getPreferredSize().height));
        return view;
    }

    /** 
	 * Make and return a view for displaying messages.
	 * @return the message view
	 */
    private Component makeMessageView() {
        final Box box = new Box(BoxLayout.X_AXIS);
        box.add(_messageView);
        box.add(Box.createHorizontalGlue());
        return box;
    }

    /** setup the parameter table  */
    private void setupParameterTable() {
        _parameterTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        _parameterTable.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(final MouseEvent event) {
                if (event.getClickCount() == 2) {
                    showParameterInspector(_parameterTable.rowAtPoint(event.getPoint()), true);
                }
            }
        });
        _parameterTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(final ListSelectionEvent event) {
                if (isParameterInspectorVisible()) {
                    showParameterInspector(_parameterTable.getSelectedRow(), false);
                }
            }
        });
    }

    /** make parameter table cell renderers */
    private void makeParameterTableCellRenderers() {
        final int variableWidth = new JLabel("Variable").getPreferredSize().width;
        _parameterTable.getColumnModel().getColumn(LiveParameterTableModel.VARIABLE_COLUMN).setMaxWidth(variableWidth);
    }

    /**
	 * Determine whether the parameter inspector is visible.
	 * @return true if the inspector is visible and false if not
	 */
    protected boolean isParameterInspectorVisible() {
        return (_parameterInspector != null && _parameterInspector.isVisible());
    }

    /**
	 * Show the parameter inspector for the parameter at the specified parameter table row.
	 * @param row the parameter table row identifying the parameter to display
	 * @param bringToFront if true, then bring the inspector to the front
	 */
    protected void showParameterInspector(final int row, final boolean bringToFront) {
        if (row >= 0) {
            showParameterInspector(_parameterTableModel.getParameter(row), bringToFront);
        }
    }

    /**
	 * Show the parameter inspector for the specified parameter.
	 * @param parameter the parameter for which to display the inspector
	 * @param bringToFront if true, then bring the inspector to the front
	 */
    protected void showParameterInspector(final LiveParameter parameter, final boolean bringToFront) {
        if (_parameterInspector == null) {
            _parameterInspector = new LiveParameterInspector(EnergyManagerWindow.this);
        }
        _parameterInspector.setParameter(parameter);
        if (!_parameterInspector.isVisible() || bringToFront) {
            _parameterInspector.setVisible(true);
        }
    }

    /** 
	 * Get the selected parameters 
	 * @return the selected parameters
	 */
    protected List<LiveParameter> getSelectedParameters() {
        final List<LiveParameter> parameters = new ArrayList<LiveParameter>();
        final List<LiveParameter> filteredParameters = _parameterTableModel.getFilteredParameters();
        final int[] selectedRows = _parameterTable.getSelectedRows();
        for (int index = 0; index < selectedRows.length; index++) {
            parameters.add(filteredParameters.get(selectedRows[index]));
        }
        return parameters;
    }

    /**
	 * Post a normal informational message to the message view.
	 * @param message the message to display
	 */
    protected void postMessage(final String message) {
        postMessage(message, false);
    }

    /**
	 * Post a message to the message view.
	 * @param message the message to display
	 * @param isWarning indicates whether this message is a warning message or just informational
	 */
    protected void postMessage(final String message, final boolean isWarning) {
        final String timestamp = MESSAGE_DATE_FORMAT.format(new Date());
        final String messageColor = isWarning ? "#ff0000" : "#000000";
        final String text = "<html><body><font color=\"#808080\"> " + timestamp + "</font><font color=\"" + messageColor + "\"> " + message + " </font></body></html>";
        _messageView.setText(text);
        _messageView.setForeground(isWarning ? Color.RED : Color.BLACK);
        validate();
    }

    /** Show the solver config dialog */
    public void showSolverConfigDialog() {
        if (_solverConfigDialog == null) {
            _solverConfigDialog = new SolverConfigDialog(EnergyManagerWindow.this, getEnergyManagerDocument().getModel().getOptimizer());
            _solverConfigDialog.setLocationRelativeTo(EnergyManagerWindow.this);
        } else {
            _solverConfigDialog.setOptimizer(getEnergyManagerDocument().getModel().getOptimizer());
        }
        _solverConfigDialog.setVisible(true);
    }

    /** Show the optimizer window */
    public void showOptimizerWindow() {
        if (_optimizerWindow == null) {
            _optimizerWindow = new OptimizerWindow(getEnergyManagerDocument().getModel().getOptimizer());
            _optimizerWindow.setLocationRelativeTo(this);
        }
        _optimizerWindow.setVisible(true);
    }

    /**
	 * Handle the event indicating that the list of evaluation nodes has changed.
	 * @param model the model posting the event
	 * @param range the new position range of evaluation nodes (first position, last position)
	 * @param nodes the new evaluation nodes
	 */
    public void evaluationNodesChanged(final EnergyManager model, final double[] range, final List<AcceleratorNode> nodes) {
    }

    /**
	 * Handle the event indicating that the model's entrance probe has changed.
	 * @param model the model posting the event
	 * @param entranceProbe the new entrance probe
	 */
    public void entranceProbeChanged(final EnergyManager model, final gov.sns.xal.model.probe.Probe entranceProbe) {
    }

    /** 
	* Handle the event indicating that the model's sequence has changed. 
	* @param model the model posting the event
	* @param sequence the model's new sequence
	*/
    public void sequenceChanged(final EnergyManager model, final AcceleratorSeq sequence, final List nodeAgents, final List parameters) {
        ((JComponent) getContentPane().getComponent(0)).setBorder(BorderFactory.createTitledBorder((sequence != null) ? sequence.getId() : ""));
        List sortedParameters;
        if (parameters != null) {
            sortedParameters = new ArrayList(parameters);
            Collections.sort(sortedParameters, _parameterOrdering);
        } else {
            sortedParameters = Collections.EMPTY_LIST;
        }
        _parameterTableModel.setParameters(sortedParameters);
        makeParameterTableCellRenderers();
        if (_parameterInspector != null && _parameterInspector.isVisible()) {
            _parameterInspector.setVisible(false);
            _parameterInspector.setParameter(null);
        }
        _optimizerWindow = null;
        postMessage("sequence changed to " + ((sequence != null) ? sequence.getId() : "none"));
        validate();
    }

    /**
	 * Event indicating that a live parameter has been modified.
	 * @param model the source of the event.
	 * @param parameter the parameter which has changed.
	 */
    public void liveParameterModified(final EnergyManager model, final LiveParameter parameter) {
    }

    /**
	 * Event indicating that the optimizer settings have changed.
	 * @param model the source of the event.
	 * @param optimizer the optimizer whose settings have changed.
	 */
    public void optimizerSettingsChanged(final EnergyManager model, final OpticsOptimizer optimizer) {
    }

    /**
	 * Event indicating that the optimizer has found a new optimal solution.
	 * @param model the source of the event.
	 * @param optimizer the optimizer which has found a new optimial solution.
	 */
    public void newOptimalSolutionFound(EnergyManager model, OpticsOptimizer optimizer) {
    }

    /**
	 * Event indicating that the optimizer has started.
	 * @param model the source of the event.
	 * @param optimizer the optimizer which has started.
	 */
    public void optimizerStarted(final EnergyManager model, final OpticsOptimizer optimizer) {
        showOptimizerWindow();
    }

    /**
	 * Select the next parameter in the table.
	 */
    public void selectNextParameter() {
        final int selectedRow = _parameterTable.getSelectedRow();
        final int nextRow = (selectedRow < _parameterTable.getRowCount() - 1) ? selectedRow + 1 : selectedRow;
        _parameterTable.getSelectionModel().setSelectionInterval(nextRow, nextRow);
    }

    /**
	 * Select the next parameter in the table.
	 */
    public void selectPreviousParameter() {
        final int selectedRow = _parameterTable.getSelectedRow();
        final int nextRow = (selectedRow > 0) ? selectedRow - 1 : selectedRow;
        _parameterTable.getSelectionModel().setSelectionInterval(nextRow, nextRow);
    }
}
