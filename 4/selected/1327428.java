package repast.simphony.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.apache.log4j.Level;
import org.apache.velocity.exception.ParseErrorException;
import org.java.plugin.PluginLifecycleException;
import repast.simphony.engine.controller.Controller;
import repast.simphony.engine.controller.DefaultController;
import repast.simphony.engine.controller.ScheduledMethodControllerAction;
import repast.simphony.engine.controller.TickListener;
import repast.simphony.engine.controller.WatcherControllerAction;
import repast.simphony.engine.environment.ControllerAction;
import repast.simphony.engine.environment.ControllerRegistry;
import repast.simphony.engine.environment.DefaultRunEnvironmentBuilder;
import repast.simphony.engine.environment.RunEnvironmentBuilder;
import repast.simphony.engine.environment.RunListener;
import repast.simphony.engine.environment.RunState;
import repast.simphony.parameter.Parameters;
import repast.simphony.parameter.ParametersWriter;
import repast.simphony.plugin.CompositeControllerActionCreator;
import repast.simphony.plugin.ModelPluginLoader;
import repast.simphony.render.Renderer;
import repast.simphony.scenario.Scenario;
import repast.simphony.scenario.ScenarioConstants;
import repast.simphony.scenario.ScenarioLoader;
import repast.simphony.scenario.ScenarioSaver;
import repast.simphony.scenario.data.ContextData;
import repast.simphony.space.SpatialException;
import repast.simphony.space.gis.FeatureAgentFactoryFinder;
import repast.simphony.ui.newscenario.NewScenarioWizard;
import repast.simphony.ui.plugin.UIActionExtensions;
import repast.simphony.ui.probe.Probe;
import repast.simphony.ui.probe.ProbeManager;
import repast.simphony.ui.tree.ScenarioTree;
import repast.simphony.ui.widget.ErrorLog;
import repast.simphony.util.FileUtils;
import repast.simphony.util.Settings;
import saf.core.runtime.PluginDefinitionException;
import saf.core.ui.dock.DockingManager;
import saf.core.ui.util.FileChooserUtilities;
import simphony.settings.SettingsRegistry;
import simphony.util.messages.MessageCenter;
import simphony.util.messages.MessageEvent;
import simphony.util.messages.MessageEventListener;

/**
 * Main application mediator type object for Repast Simphony runtime.
 * 
 * @author Nick Collier
 */
public class RSApplication implements TickListener, RunListener {

    private static final String LAST_LOADED_SCENARIO_KEY = "LastLoadedScenario";

    private static final String DEFAULT_LAYOUT_FILE = "default_frame_layout.xml";

    private MessageCenter msgCenter = MessageCenter.getMessageCenter(RSApplication.class);

    private File defaultLayoutFile = null;

    private Controller controller;

    private RunEnvironmentBuilder runEnvironmentBuilder;

    private boolean initRequired = true;

    private boolean startSim = true;

    private RSGui gui;

    private ScenarioTree scenarioTree;

    private Scenario scenario = null;

    private ProbeManager probeManager;

    public ProbeManager getProbeManager() {
        return probeManager;
    }

    private UIActionExtensions actionExts;

    private ModelPluginLoader modelPluginLoader;

    private GUIParametersManager paramsManager;

    private SettingsRegistry settingsRegistry;

    private ErrorLog errorLog;

    private RunOptionsModel runOptions = new RunOptionsModel();

    private static RSApplication rSApplication;

    public static boolean isGui() {
        return (rSApplication != null);
    }

    public static RSApplication getRSApplicationInstance() {
        return rSApplication;
    }

    public void addCustomUserPanel(JPanel panel) {
        gui.addCustomUserPanel(panel);
    }

    public boolean hasCustomUserPanelDefined() {
        return gui.hasCustomUserPanelDefined();
    }

    public void removeCustomUserPanel() {
        gui.removeCustomUserPanel();
    }

    public RSApplication(UIActionExtensions actionExts, ModelPluginLoader loader) {
        rSApplication = this;
        this.actionExts = actionExts;
        scenarioTree = new ScenarioTree(actionExts);
        modelPluginLoader = loader;
        settingsRegistry = Settings.getRegistry();
        MessageCenter.addMessageListener(new MessageEventListener() {

            public void messageReceived(MessageEvent event) {
                if (event.getLevel().isGreaterOrEqual(Level.WARN)) {
                    if (errorLog != null) errorLog.addError(event);
                    if (gui != null) gui.warn();
                }
                if (event.getMetaData() != null && event.getMetaData().length > 0 && event.getMetaData()[0].equals("STATUS")) {
                    gui.setStatusBarText(event.getMessage().toString());
                }
            }
        });
    }

    /**
	 * Sets up this RSApplication. This only be called by the RSAppConfigurator.
	 */
    void setupApplication() {
        GUIScheduleRunner scheduleRunner = new GUIScheduleRunner();
        scheduleRunner.addRunListener(this);
        runEnvironmentBuilder = new DefaultRunEnvironmentBuilder(scheduleRunner, false);
        controller = new DefaultController(runEnvironmentBuilder);
        scheduleRunner.setTickListener(this);
        controller.setScheduleRunner(scheduleRunner);
    }

    /**
	 * Initializes a simulation run. This initializes both the run mechanism and
	 * the gui.
	 */
    public void initSim() {
        if (initRequired) {
            try {
                controller.batchInitialize();
                controller.runParameterSetters(paramsManager.getParameters());
                RunState runState = controller.runInitialize(paramsManager.getParameters());
                GUIScheduleRunner runner = (GUIScheduleRunner) controller.getScheduleRunner();
                for (Renderer renderer : runState.getGUIRegistry().getDisplays()) {
                    runner.addRenderer(renderer);
                }
                gui.addViewsFromRegistry(runState.getGUIRegistry(), probeManager);
                gui.setGUIForPostSimInit();
                initRequired = false;
            } catch (SpatialException ex) {
                msgCenter.error("Projection error", ex);
            } catch (RuntimeException ex) {
                msgCenter.error("Error while initializing simulation", ex);
            }
        }
    }

    /**
	 * Checks to see the state of startSim. Should be called from Event Dispatch Thread.
	 */
    public boolean isStartSim() {
        return startSim;
    }

    /**
	 * Starts a simulation run. Initializes the run if necessary.
	 */
    public void start() {
        gui.setGUIForStarted();
        if (initRequired) {
            initSim();
            gui.setGUIForStarted();
        }
        if (startSim) {
            runOptions.simStarted();
            controller.execute();
            startSim = false;
        } else {
            controller.getScheduleRunner().setPause(false);
        }
    }

    /**
	 * Steps a simulation run forward a single step.
	 */
    public void step() {
        gui.setGUIForStepped();
        controller.getScheduleRunner().step();
        if (initRequired) {
            initSim();
            gui.setGUIForStepped();
        }
        if (startSim) {
            controller.execute();
            startSim = false;
        }
    }

    /**
	 * Pauses the simulation run.
	 */
    public void pause() {
        gui.setGUIForPaused();
        controller.getScheduleRunner().setPause(true);
    }

    /**
	 * Stops the simulation run.
	 */
    public void stop() {
        gui.setGUIForStopped();
        controller.getScheduleRunner().stop();
        runOptions.simStopped();
    }

    /**
	 * Resets the layout to the initial layout.
	 */
    public void resetLayout() {
        InputStream stream;
        if (defaultLayoutFile == null) {
            stream = getClass().getResourceAsStream(DEFAULT_LAYOUT_FILE);
        } else {
            try {
                stream = new FileInputStream(defaultLayoutFile);
            } catch (FileNotFoundException ex) {
                msgCenter.error("Error resetting default layout from file", ex);
                return;
            }
        }
        gui.resetLayout(stream);
    }

    /**
	 * Saves the current layout to a user selected file.
	 */
    public void saveLayout() {
        File dir = new File(".");
        Scenario scenario = getCurrentScenario();
        if (scenario != null) {
            dir = scenario.getScenarioDirectory();
        }
        File file = FileChooserUtilities.getSaveFile(dir);
        if (file != null) {
            gui.saveLayout(file);
        }
    }

    /**
	 * Saves the current view layout as the default layout for the current
	 * scenario.
	 */
    public void saveAsDefaultLayout() {
        Scenario scenario = getCurrentScenario();
        if (scenario != null) {
            File dir = scenario.getScenarioDirectory();
            File layoutFile = new File(dir, DEFAULT_LAYOUT_FILE);
            gui.saveLayout(layoutFile);
        }
    }

    /**
	 * Loads and resets the current layout from a user selected file.
	 */
    public void loadLayout() {
        File dir = new File(".");
        Scenario scenario = getCurrentScenario();
        if (scenario != null) {
            dir = scenario.getScenarioDirectory();
        }
        File file = FileChooserUtilities.getOpenFile(dir);
        if (file != null && file.exists()) {
            try {
                gui.resetLayout(new FileInputStream(file));
            } catch (FileNotFoundException ex) {
            }
        }
    }

    /**
	 * Creates a new scenario through the new scenario wizard.
	 */
    public void createNewScenario() {
        NewScenarioWizard wizard = new NewScenarioWizard();
        wizard.display(gui.getFrame());
        try {
            if (!wizard.wasCanceled()) {
                if (!initRequired) reset();
                wizard.createScenario();
                open(wizard.getScenarioPath());
            }
        } catch (Exception ex) {
            msgCenter.error("Error while creating scenario", ex);
        }
    }

    /**
	 * Opens a scenario via a directory chooser dialog.
	 */
    public void open() {
        String dir = ".";
        if (settingsRegistry.get(LAST_LOADED_SCENARIO_KEY) != null) {
            dir = settingsRegistry.get(LAST_LOADED_SCENARIO_KEY).toString();
        }
        File f = FileChooserUtilities.getOpenDirectory(new File(dir));
        if (f != null) {
            if (!initRequired) reset();
            open(f);
        }
    }

    /**
	 * Opens a scenario contained in the specified directory.
	 * 
	 * @param dir
	 *            the scenario directory
	 */
    void open(File dir) {
        try {
            if (dir != null) {
                ScenarioLoader loader = new ScenarioLoader(dir, actionExts);
                controller.setControllerRegistry(loader.load(runEnvironmentBuilder));
                Scenario scenario = loader.getScenario();
                if (scenario != null) {
                    ControllerRegistry reg = controller.getControllerRegistry();
                    ContextData rootContext = scenario.getContext();
                    for (CompositeControllerActionCreator creator : actionExts.getActionExts().parentActionCreators()) {
                        for (ContextData node : rootContext.getAllContexts()) {
                            ControllerAction action = reg.findAction(node.getId(), creator.getID());
                            if (action != null) {
                                actionExts.getCompositeEditorExts().addUI(action, creator);
                            }
                        }
                    }
                    actionExts.addDefaultUI(WatcherControllerAction.class, "Watcher Initialization");
                    actionExts.addDefaultUI(ScheduledMethodControllerAction.class, "Schedule Initialization");
                    scenarioTree.setControllerRegistry(scenario, controller.getControllerRegistry());
                    gui.removeParameterViews();
                    Parameters params = loader.getParameters();
                    Probe probe = gui.addParameterView(params, scenario.getScenarioDirectory(), this);
                    paramsManager = updateGuiParamsManager(params, probe);
                    reg.addParameterSetter(paramsManager);
                    gui.setStatusBarText(scenario.getModelData().getName() + " loaded");
                    gui.setTitle(scenario.getModelData().getName() + " - Repast Simphony");
                    loadModelPlugins(scenario);
                    gui.setGUIForModelLoaded();
                    this.scenario = scenario;
                    settingsRegistry.put(LAST_LOADED_SCENARIO_KEY, scenario.getScenarioDirectory().getAbsolutePath());
                    defaultLayoutFile = null;
                    for (File file : dir.listFiles()) {
                        if (file.getName().equals(DEFAULT_LAYOUT_FILE)) {
                            defaultLayoutFile = file;
                            resetLayout();
                            break;
                        }
                    }
                }
            } else {
                msgCenter.warn("Attempted to open a new scenario with a null dir.");
            }
        } catch (Exception e) {
            msgCenter.error("Scenario Load Error", e);
            gui.showError("Scenario Load Error", "Error while loading scenario. See the error log (view -> the error log) for details");
        }
    }

    public GUIParametersManager updateGuiParamsManager(Parameters params, Probe probe) {
        paramsManager = new GUIParametersManager(params, probe);
        return paramsManager;
    }

    private void loadModelPlugins(Scenario scenario) throws PluginDefinitionException, PluginLifecycleException {
        modelPluginLoader.removePlugins();
        File modelPluginPath = scenario.getModelPluginPath();
        if (modelPluginPath != null) {
            modelPluginLoader.addPath(modelPluginPath);
            modelPluginLoader.publishPlugins();
        }
        UIExtPointLoader loader = new UIExtPointLoader(gui);
        loader.processExtPoints(modelPluginLoader.getManager());
        if (!loader.isTickFormatterLoaded()) {
            gui.setTickCountFormatter(new DefaultTickCountFormatter());
        }
    }

    /**
	 * Resets the gui and runtime infrastructure for another simulation run.
	 */
    public void reset() {
        initRequired = true;
        startSim = true;
        ((GUIScheduleRunner) controller.getScheduleRunner()).clearRenderers();
        controller.runCleanup();
        controller.batchCleanup();
        paramsManager.reset();
        probeManager.reset();
        gui.reset();
        FeatureAgentFactoryFinder.getInstance().clearAdapters();
    }

    /**
	 * Saves the current scenario into a user specified scenario directory. The
	 * directory itself is selected via directory chooser widget.
	 */
    public void saveAs() {
        String dir = ".";
        if (scenario != null) dir = scenario.getScenarioDirectory().getAbsolutePath();
        File f = FileChooserUtilities.getSaveDirectory(new File(dir));
        if (f != null) {
            doSave(f, false, true);
        }
    }

    /**
	 * Saves the current parameters to a parameters.xml file in the current
	 * scenario directory.
	 */
    public void saveCurrentParameters() {
        try {
            if (scenario != null) {
                File paramFile = new File(scenario.getScenarioDirectory(), "parameters.xml");
                if (paramFile.exists()) {
                    FileUtils.copyFile(paramFile, new File(paramFile.getParentFile(), "parameters_backup.xml"));
                }
                ParametersWriter pw = new ParametersWriter();
                pw.writeSpecificationToFile(paramsManager.getParameters(), paramFile);
            }
        } catch (Exception ex) {
            msgCenter.error("Error while saving current scenario parameters.", ex);
        }
    }

    /**
	 * Saves the current scenario. If the current scenario has not been saved,
	 * this is the equivalent of save as.
	 */
    public void save() {
        if (scenario == null) saveAs(); else doSave(scenario.getScenarioDirectory(), true, false);
    }

    private void doSave(File scenarioDir, boolean saveParams, boolean saveModelSpec) {
        if (scenarioDir != null) {
            File backup = null;
            try {
                if (!scenarioDir.getAbsolutePath().endsWith(".rs")) {
                    scenarioDir = new File(scenarioDir + ".rs");
                }
                if (scenarioDir.exists()) {
                    backup = FileUtils.backupDir(scenarioDir);
                    if (saveModelSpec) {
                        FileUtils.deleteIgnoreVC(scenarioDir);
                    } else {
                        FileUtils.deleteIgnoreVC(scenarioDir, ScenarioConstants.LEGACY_SCORE_FILE_NAME, ScenarioConstants.CONTEXT_FILE_NAME, ScenarioConstants.USER_PATH_FILE_NAME, ScenarioConstants.DEFAULT_FRAME_LAYOUT);
                    }
                    scenarioDir.mkdir();
                } else {
                    scenarioDir.mkdirs();
                }
                ScenarioSaver scenarioSaver = new ScenarioSaver(controller.getControllerRegistry(), scenarioDir, scenario.getContext());
                scenarioSaver.save(actionExts, scenario, saveModelSpec);
                scenario.setScenarioDirectory(scenarioDir);
                File paramFile = new File(scenarioDir, "parameters.xml");
                ParametersWriter pw = new ParametersWriter();
                pw.writeSpecificationToFile(paramsManager.getParameters(), paramFile);
                if (backup != null) {
                    File source = new File(backup, "plugin_jpf.xml");
                    if (source.exists()) FileUtils.copyFile(source, new File(scenarioDir, "plugin_jpf.xml"));
                    source = new File(backup, "styles");
                    if (source.exists()) FileUtils.copyDirs(source, new File(scenarioDir, "styles"));
                    FileUtils.delete(backup);
                }
            } catch (IOException e) {
                if (backup != null) restoreScenario(backup, scenarioDir);
                msgCenter.error("Error while saving scenario", e);
            } catch (ParseErrorException e) {
                if (backup != null) restoreScenario(backup, scenarioDir);
                msgCenter.error("Error while saving scenario", e);
            } catch (Exception e) {
                if (backup != null) restoreScenario(backup, scenarioDir);
                msgCenter.error("Error while saving scenario", e);
            }
        }
    }

    private void restoreScenario(File backup, File scenarioDir) {
        if (scenarioDir.exists()) FileUtils.delete(scenarioDir);
        backup.renameTo(scenarioDir);
    }

    /**
	 * Closes the runtime application.
	 * 
	 * @return true
	 */
    public boolean close() {
        stop();
        storeSettings();
        return true;
    }

    /**
   *
   */
    protected void storeSettings() {
        try {
            Settings.storeSettings();
        } catch (IOException e) {
            msgCenter.warn("There was a problem saving Repast's settings, the current settings will not " + "not be saved.", e);
        }
    }

    /**
	 * Creates the gui layout.
	 * 
	 * @param manager
	 */
    public void createLayout(DockingManager manager) {
        this.gui = new RSGui(manager, scenarioTree);
    }

    /**
	 * Called when the schedule increments the tick count.
	 * 
	 * @param newTick
	 *            the new tick value.
	 */
    public void tickCountUpdated(double newTick) {
        gui.updateTickCountLabel(newTick);
        probeManager.update();
    }

    /**
	 * Returns the current controller.
	 * 
	 * @return the current controller.
	 */
    public Controller getController() {
        return this.controller;
    }

    /**
	 * Returns the application gui
	 * 
	 * @return the application gui
	 */
    public RSGui getGui() {
        return gui;
    }

    /**
	 * Gets the settings registry.
	 * 
	 * @return the settings registry.
	 */
    public SettingsRegistry getSettingsRegistry() {
        return settingsRegistry;
    }

    public Scenario getCurrentScenario() {
        return scenario;
    }

    /**
	 * Initializes the gui.
	 */
    void initGui() {
        errorLog = new ErrorLog(gui);
        gui.init(errorLog);
        probeManager = new ProbeManager(gui);
        gui.addPlaceHolderUserPanel();
        gui.addRunOptionsView(runOptions);
    }

    /**
	 * Invoked when the current run has been paused.
	 */
    public void paused() {
        if (SwingUtilities.isEventDispatchThread()) gui.setGUIForPaused(); else {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    gui.setGUIForPaused();
                }
            });
        }
    }

    /**
	 * Invoked when the current run has been restarted after a pause.
	 */
    public void restarted() {
        if (SwingUtilities.isEventDispatchThread()) gui.setGUIForStarted(); else {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    gui.setGUIForStarted();
                }
            });
        }
    }

    /**
	 * Invoked when the current run has been started.
	 */
    public void started() {
        if (SwingUtilities.isEventDispatchThread()) gui.setGUIForStarted(); else {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    gui.setGUIForStarted();
                }
            });
        }
    }

    /**
	 * Invoked when the current run has been stopped.
	 */
    public void stopped() {
        if (SwingUtilities.isEventDispatchThread()) gui.setGUIForStopped(); else {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    gui.setGUIForStopped();
                }
            });
        }
    }

    /**
	 * Gets the error log.
	 * 
	 * @return
	 */
    public ErrorLog getErrorLog() {
        return errorLog;
    }
}
