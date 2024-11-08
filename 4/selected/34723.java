package org.modyna.modyna.gui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import org.modyna.modyna.gui.action.ActionManager;
import org.modyna.modyna.gui.utils.MessageDialogs;
import org.modyna.modyna.gui.utils.SimpleFileFilter;
import org.modyna.modyna.model.DefaultModelDocument;
import org.modyna.modyna.model.ModelDocument;
import org.modyna.modyna.model.ModelDocumentFactory;
import org.modyna.modyna.persistence.FileHandler;
import org.modyna.modyna.persistence.PersistenceException;
import org.modyna.modyna.simulation.CoreException;
import org.modyna.modyna.simulation.Sample;
import org.modyna.modyna.simulation.Simulation;
import org.modyna.modyna.simulation.integration.DefaultSimulation;

/**
 * Central class for managing the GUI, holds references to the modeldocument and
 * to other central managing classes as well as core elements of the GUI
 * (presentation layer). Starts the GUI.
 * 
 * The GuiManager observes all changes in the modelDocument (as a listener) in
 * order to reflect the model state in the GUI.
 * 
 * @author Dr. Rupert Rebentisch
 */
public class GuiManager implements PropertyChangeListener {

    /**
	 * Reference to all toolbar buttons and menue items, the actionManager
	 * enables and disables these elements according to the state of the system.
	 * The actionManager also serves as a facade for the business logic that is
	 * triggered by the toolbar buttons and menue elements.
	 */
    protected ActionManager actionManager = null;

    /**
	 * Dialog to specify which data series are going to be plotted.
	 */
    private AnalysisDialog anld = null;

    /**
	 * The main window of the app.
	 */
    protected MainAppFrame maf = null;

    /**
	 * Reference to the model and the corresponding core functionality
	 */
    protected ModelDocument modelDocument = null;

    /**
	 * File that stores the model (e.g. reference used for save functionality)
	 */
    protected File modelFile;

    /**
	 * Reference to the data series returned from the integration
	 */
    private Sample sample = null;

    /**
	 * Reference to the functionality to integrate the model
	 */
    private Simulation simulator = null;

    /**
	 * Represents the state of the GUI triggers various GUI changes to reflect
	 * the state (e.g. disabling simulation when there is no model, or disabling
	 * the analysis functionality when there has not yet been any simulation.
	 */
    protected StateManager stateManager = null;

    /**
	 * Status bar in the main application window.
	 */
    protected MainStatusBar statusBar = null;

    /**
	 * Initializes the main gui elements like the main application frame.
	 */
    public GuiManager() {
        actionManager = new ActionManager(this);
        stateManager = new StateManager(this);
        statusBar = new MainStatusBar();
        maf = new MainAppFrame(this);
        simulator = new DefaultSimulation();
    }

    /**
	 * Allows the user to close the application after he confirms this choice.
	 */
    public void closeApplication() {
        int exit = JOptionPane.showConfirmDialog(maf, "Do you want to close DynSysSim?", "Do you want to close DynSysSim?", JOptionPane.YES_NO_OPTION);
        if (exit == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    /**
	 * Instantiates a new ModelDocument by using a factory
	 */
    public void createNewModelDocument() {
        setModelDocument(ModelDocumentFactory.createModelDocument());
    }

    /**
	 * Provides reference to the ActionManager.
	 * 
	 * @return ActionManager reference to the main actions the user can choose.
	 */
    public ActionManager getActionManager() {
        return actionManager;
    }

    /**
	 * Reference to the Analysis dialog.
	 * 
	 * @return AnalysisDialog dialog that enables the user to plot the various
	 *         quantities versus time. With each change in the model the list of
	 *         quantities might change, so it is important to keep the dialog in
	 *         sync with the model. There should only be one analysis dialog
	 *         active in the application.
	 */
    public AnalysisDialog getAnalysisDialog() {
        return this.anld;
    }

    /**
	 * Provides reference to ModelDocument.
	 * 
	 * @return ModelDocument contains the model beeing simulated, provides
	 *         standard interfaces for parametrization, integration etc.
	 */
    public ModelDocument getModelDocument() {
        return modelDocument;
    }

    /**
	 * Returns the file that stores the model.
	 * 
	 * @return File
	 */
    public File getModelFile() {
        return modelFile;
    }

    /**
	 * Provides reference to Sample.
	 * 
	 * @return Sample Collection of time/y-values that have been sampled during
	 *         simulation.
	 */
    public Sample getSample() {
        return this.sample;
    }

    /**
	 * Provides reference to Simulator.
	 * 
	 * @return Simulation Generic simulator to integrate / simulate behaviour
	 *         with time.
	 */
    public Simulation getSimulator() {
        return this.simulator;
    }

    /**
	 * Provides reference to the StateManager.
	 * 
	 * @return StateManager manages the state of the GUI.
	 */
    public StateManager getStateManager() {
        return this.stateManager;
    }

    /**
	 * Provides reference to the status bar in the main application frame.
	 * 
	 * @return MainStatusBar status bar in the main application frame. Contains
	 *         information that is updated from various parts of the program.
	 */
    public MainStatusBar getStatusBar() {
        return this.statusBar;
    }

    /**
	 * Triggers building of ODEs, displays any problems in this process (e.g.
	 * parsing errors, algebraic circles)
	 */
    private void modelSetUp() {
        try {
            modelDocument.setUp();
        } catch (CoreException e) {
            String errorMessage = "Model can not be build" + e.getMessage() + e.getStackTrace();
            MessageDialogs.openErrorMessageBox("Model can not be build", errorMessage);
        }
    }

    /**
	 * Listener method to observe any changes of the model. The state of the
	 * model is shown as a traffic light in the right corner of the status bar
	 * of the main application window.
	 * 
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
    public void propertyChange(PropertyChangeEvent event) {
        if (((Integer) event.getNewValue()).intValue() == ModelDocument.ERRORNOUS_STATE) {
            this.getStatusBar().signalErrornousState();
        } else if (((Integer) event.getNewValue()).intValue() == ModelDocument.UNCHECKED_STATE) {
            this.getStatusBar().signalUncheckedState();
        } else if (((Integer) event.getNewValue()).intValue() == ModelDocument.VALID_STATE) {
            this.getStatusBar().signalValidState();
        } else {
            System.err.println("Unknown property change");
        }
    }

    /**
	 * Read model from file, sets the model reference, displays any errors.
	 * Reading stands for retrieving the model contents from a file whereas
	 * loading stands for the user action of loading a model file for analysis.
	 */
    public void readFromFile() {
        FileHandler fileLoader = new FileHandler();
        try {
            setModelDocument(fileLoader.loadFile(this.modelFile));
        } catch (PersistenceException e) {
            MessageDialogs.openErrorMessageBox("An error occured while reading the file: ", e.getMessage() + e.getCause() + e.getStackTrace());
        }
    }

    /**
	 * Saves the ModelDocument to a xml file
	 */
    public void saveToCurrentDefaultFile() {
        if (getModelFile() != null) {
            writeToFile();
        } else {
            showAndHandleSaveToFileDialog();
        }
    }

    /**
	 * Save model to a specified file, which is specified as a parameter (used
	 * by save-as functionality).
	 * 
	 * @param file
	 *            to which the current model is going to be saved.
	 */
    public void saveToSpecifiedFileAndSetAsCurrentDefaultFile(File file) {
        setModelFile(file);
        writeToFile();
    }

    /**
	 * Sets the analysis dialog.
	 * 
	 * @param analysisDialog
	 *            dialog that enables the user to plot the various quantities
	 *            versus time. With each change in the model the list of
	 *            quantities might change, so it is important to keep the dialog
	 *            in sync with the model. There should only be one analysis
	 *            dialog active in the application.
	 */
    public void setAnalysisDialog(AnalysisDialog analysisDialog) {
        this.anld = analysisDialog;
    }

    /**
	 * Sets the model (as collection of all relevant interfaces).
	 * 
	 * @param modelDocument
	 *            contains the model beeing simulated, provides standard
	 *            interfaces for parametrization, integration etc.
	 */
    public void setModelDocument(ModelDocument modelDocument) {
        this.modelDocument = modelDocument;
        this.modelDocument.addPropertyChangeListener(this);
    }

    /**
	 * Sets the modelFile.
	 * 
	 * @param modelFile
	 *            The modelFile to set
	 */
    public void setModelFile(File modelFile) {
        this.modelFile = modelFile;
    }

    /**
	 * Sets the Sample containing the data from the simulation.
	 * 
	 * @param sample
	 *            Collection of time/y-values that have been sampled during
	 *            simulation.
	 */
    public void setSample(Sample sample) {
        this.sample = sample;
    }

    public void showAnalysisDialog() {
        Sample sample = getSample();
        if (getAnalysisDialog() == null) {
            setAnalysisDialog(new AnalysisDialog(sample));
            getAnalysisDialog().show();
        } else {
            getAnalysisDialog().show();
        }
    }

    /**
	 * Facilitates a file handler dialog to load a model from a xml file. Only
	 * .xml files are listed.
	 */
    public void showAndHandleFileLoadDialog() {
        setModelDocument(ModelDocumentFactory.createModelDocument());
        this.stateManager.setState(StateManager.INITIAL_STATE);
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(false);
        String[] modelfiles = new String[] { "xml", "XML" };
        chooser.setFileFilter(new SimpleFileFilter(modelfiles, "*.xml"));
        int option = chooser.showOpenDialog(null);
        if (option == JFileChooser.APPROVE_OPTION) {
            setModelFile(chooser.getSelectedFile());
            readFromFile();
            this.stateManager.setState(StateManager.DEFINITION_STATE);
        }
    }

    /**
	 * Shows a dialog to chose a directory and to give a filename to which the
	 * ModelDocument is saved.
	 */
    public void showAndHandleSaveToFileDialog() {
        JFileChooser chooser = new JFileChooser();
        String[] modelfiles = new String[] { "xml", "XML" };
        chooser.setFileFilter(new SimpleFileFilter(modelfiles, "*.xml"));
        int option = chooser.showSaveDialog(maf);
        if (option == JFileChooser.APPROVE_OPTION) {
            if (chooser.getSelectedFile() != null) {
                File file = chooser.getSelectedFile();
                String fileName = file.getAbsolutePath();
                if (!fileName.endsWith(".xml")) {
                    fileName = fileName.concat(".xml");
                    file = new File(fileName);
                }
                if (file.exists()) {
                    int exit = JOptionPane.showConfirmDialog(chooser, "File already exists. Overwrite?", "File already exists. Overwrite?", JOptionPane.YES_NO_OPTION);
                    if (exit == JOptionPane.YES_OPTION) {
                        saveToSpecifiedFileAndSetAsCurrentDefaultFile(file);
                    }
                } else {
                    saveToSpecifiedFileAndSetAsCurrentDefaultFile(file);
                }
            }
        }
    }

    /**
	 * Instantiates and shows dialog to configure the model, show error message
	 * in case that the corresponding set of equations can not be built
	 */
    public void showConfigureDialog() {
        ConfigurationDialog cfgmd = null;
        ModelDocument modelDocument = getModelDocument();
        modelSetUp();
        cfgmd = new ConfigurationDialog(modelDocument);
        cfgmd.setModal(true);
        cfgmd.show();
    }

    /**
	 * Instantiates and shows a dialog that gives the user the opportunity to
	 * change the model, i.e. change the model equations and insert or delete
	 * model quantities
	 */
    public void showModelDefinitionDialog() {
        DefinitionDialog defd = null;
        ModelDocument model = getModelDocument();
        if (model != null) {
            getStateManager().setState(StateManager.DEFINITION_STATE);
            defd = new DefinitionDialog((DefaultModelDocument) model);
            defd.show();
        }
    }

    /**
	 * Triggers preparation of building the model ODEs and shows a dialog which
	 * gives the user the opportunity to start the simulation within a specified
	 * interval.
	 */
    public void showSimulationDialog() {
        SimulationDialog simd = null;
        modelSetUp();
        if (this.modelDocument.getState() == DefaultModelDocument.VALID_STATE) {
            simd = new SimulationDialog(this);
            simd.show();
        }
    }

    /**
	 * Choses the Look and Feel, sets the initial state and shows the main
	 * application frame.
	 */
    public void start() {
        try {
            UIManager.setLookAndFeel("com.jgoodies.looks.plastic.PlasticXPLookAndFeel");
        } catch (Exception e) {
            System.err.println("Warning: UIManager could not set look and feel.");
        }
        this.stateManager.setState(StateManager.INITIAL_STATE);
        maf.show();
    }

    /**
	 * Writes the model to the file, which is referenced by the GUI manager
	 * (used by save functionality). Writing stands for the process of writing
	 * the model content to a file, whereas saving stands for the user action of
	 * saving his changes.
	 */
    public void writeToFile() {
        FileHandler fileLoader = new FileHandler();
        try {
            fileLoader.saveFile(this.modelFile, this.modelDocument);
        } catch (PersistenceException e) {
            JOptionPane.showMessageDialog(null, "An error occured while writing the file: " + e.getMessage());
        }
    }

    public void showModelPropertyChangeDialog() {
        System.out.println("change properties");
        PropertyChangeDialog propChangeDialog = new PropertyChangeDialog(this);
        propChangeDialog.show();
    }
}
