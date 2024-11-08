package verinec.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.logging.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import clixml.JDomComparator;
import verinec.VerinecException;
import verinec.gui.core.*;
import verinec.data.repository.*;
import verinec.util.*;

/**
 * The main class of the Gui. Provides an interface for the modules to add
 * toolbars and menues, store and save configuration data and more.
 * 
 * <p>This class is a singleton, that is you have to use the static
 *    method getInstance() to get the analyser. There is only one
 *    instance of VerinecStudio in the application.
 * </p>
 * 
 * <p>The following keyboard shortcuts are defined:</p>
 * <ul>
 * <li>ctrl-n: New network</li>
 * <li>ctrl-o: Open network</li>
 * <li>ctrl-s: Save network</li>
 * <li>ctrl-shift-s: Save network as...</li>
 * <li>alt-F4: Exit application</li>
 * <li>F1: help (currently just the about box)</li>
 * <li>esc: Unselect all nodes</li>
 * </ul>
 * Note that some modules define additional keyboard shortcuts, i.e. the 
 * {@link verinec.gui.configurator.Configurator} module.
 * 
 * @see verinec.netsim.gui.SimulatorThread to learn how to run simulation in endless loop
 * 
 * @author Renato Loeffel
 * @author Patrick Aebischer
 * @author david.buchmann at unifr.ch
 *  
 */
public class VerinecStudio extends JFrame implements IConfigurable {

    private static VerinecStudio _instance = null;

    private JMenuBar menuBar;

    private JMenu menuNetwork;

    private JMenu menuPreferences;

    private JMenu menuAbout;

    private JComboBox moduleSelector;

    private Action moduleSelectAction;

    private Action newAction;

    private Action loadAction;

    private Action saveAction;

    private Action saveAsAction;

    private StandardToolbar stdToolbar;

    private JPanel toolbarPanel;

    private JLabel lblStatus;

    private Logger logger = Logger.getLogger(getClass().getName());

    /**
     * This is the panel where the objects are painted. Many parts of the system
     * need direct acces to it, so we just have it public.
     */
    public DrawPanel drawPanel;

    /**
     * This is the panel to the right, where information / editors can be
     * attached. Modules can add panels using
     * {@link #setTopComponent(Component)}and
     * {@link #setBottomComponent(Component)}of the VerinecStudio.
     */
    private InfoSplitPane infoSplitPane;

    /**
     * This panel contains the drawpanel. A reference to it is kept here, because some modules remove this component from the {@link #analyserSplitPane}. When loading a new module, this scrollpane will be reset by {@link #initializeGui()}.  
     * */
    private JScrollPane drawScrollPanel;

    /**
     * This panel contains the {@link #drawScrollPanel} and the {@link #infoSplitPane}. When loading a new module, the left pane {@link #drawScrollPanel} will be set by {@link #initializeGui()}.
     */
    private JSplitPane analyserSplitPane;

    /**
     * The currently active module in the GUI.
     */
    private IVerinecModule currentModule;

    /**
     * Whether the analyser is currently busy changing from one module to an
     * other. During this, {@link #getActiveModule()}throws an error.
     */
    private boolean inTransition = true;

    /** The module configuration. */
    private Document config;

    /** Wether modifying is currently allowed. Defaults to true. */
    private boolean modifyAllowed;

    /**
     * The selection state change listeners. Will be notified every time a
     * NwComponent changes its selection state.
     */
    private HashSet nwComponentChangeListeners;

    /** Key listener for esc key. */
    private Action escKey;

    /** The current state of the network is stored in form of a repository. */
    private StudioRepository currentState;

    /** The currently used repository, or null if this is a new project. */
    private IVerinecRepository repo;

    private Hashtable nameToNode;

    /**
     * Location where the configuration will be saved to and loaded from. (Could
     * be users home, but currently is just ./data)
     */
    private static final File storage = new File("./data");

    private final String title = "VeriNeC Studio";

    /** Icon for the load action.
     * Taken from Bluecurve /usr/share/icons/Bluecurve/16x16/actions/ 
      */
    private static final String LOAD_ICON = "/res/pictures/fileopen.png";

    /** Icon for the new action.
     * Taken from Bluecurve /usr/share/icons/Bluecurve/16x16/actions/ 
      */
    private static final String NEW_ICON = "/res/pictures/filenew.png";

    /** Icon for the save action.
     * Taken from Bluecurve /usr/share/icons/Bluecurve/16x16/actions/ 
      */
    private static final String SAVE_ICON = "/res/pictures/filesave.png";

    /** Icon for the save as action.
     * Taken from Bluecurve /usr/share/icons/Bluecurve/16x16/actions/ 
      */
    private static final String SAVEAS_ICON = "/res/pictures/filesaveas.png";

    /**
     * Icon for the configure action. Taken from Bluecurve
     * /usr/share/icons/Bluecurve/16x16/actions/
     */
    private static final String CONFIGURE_ICON = "/res/pictures/configure.png";

    private static final String CLOSE_ICON = "/res/pictures/close.png";

    /**
     * Icon for the help action. Taken from Bluecurve
     * /usr/share/icons/Bluecurve/16x16/actions/
     */
    private static final String HELP_ICON = "/res/pictures/help.png";

    /** The name for the configuration data used in 
     * {@link #saveConfig(String, Document)}.
     */
    private static final String configName = "verinec.gui.modules";

    /**
     * Initializes the main frame, opens a window, and creates menus and
     * toolbars.
     * <ul>
     * <li>a ButtonToolBar</li>
     * <li>InfoSplitPane</li>
     * <li>a DrawPanel(infoSplitPane, buttonToolbar)</li>
     * <li>a StandardToolbar(drawPanel)</li>
     * </ul>
     * Be aware that you can not change the order of this initialization,
     * because there is a dependency between the instances.
     * 
     * @throws VerinecException If initialization fails.
     */
    private VerinecStudio() throws VerinecException {
        super();
        if (!storage.exists()) if (!storage.mkdir()) System.err.println("Could not create data dir (this is bad)");
        setTitle(title);
        NetworkTypes.initialize(this);
        newAction = new NewAction();
        loadAction = new LoadAction();
        saveAction = new SaveAction();
        saveAsAction = new SaveAsAction();
        menuBar = new JMenuBar();
        menuNetwork = new JMenu("VeriNeC");
        menuNetwork.add(newAction);
        menuNetwork.add(loadAction);
        menuNetwork.add(saveAction);
        menuNetwork.add(saveAsAction);
        menuNetwork.add(new ExitAction());
        menuPreferences = new JMenu("Preferences");
        menuPreferences.add(new OptionsAction());
        menuAbout = new JMenu("Help");
        JMenuItem about = new JMenuItem(new AboutAction());
        menuAbout.add(about);
        escKey = new EscKeyAction();
        config = getConfig();
        moduleSelector = new JComboBox();
        resetModules();
        moduleSelectAction = new ModuleSelectAction();
        moduleSelector.setAction(moduleSelectAction);
        toolbarPanel = new JPanel();
        toolbarPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        stdToolbar = new StandardToolbar();
        stdToolbar.addSeparator();
        stdToolbar.add(moduleSelector);
        stdToolbar.addSeparator(new Dimension(20, 20));
        stdToolbar.add(newAction);
        stdToolbar.add(loadAction);
        stdToolbar.add(saveAction);
        stdToolbar.add(saveAsAction);
        lblStatus = new JLabel("New Project");
        Border statusBorder = new CompoundBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED), BorderFactory.createEmptyBorder(2, 5, 2, 5));
        lblStatus.setBorder(statusBorder);
        infoSplitPane = new InfoSplitPane(this);
        drawPanel = new DrawPanel(this, stdToolbar);
        drawScrollPanel = new JScrollPane(drawPanel);
        drawScrollPanel.getVerticalScrollBar().setUnitIncrement(8);
        analyserSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, drawScrollPanel, infoSplitPane);
        analyserSplitPane.setResizeWeight(0.9);
        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(toolbarPanel, BorderLayout.NORTH);
        this.getContentPane().add(analyserSplitPane);
        this.getContentPane().add(lblStatus, BorderLayout.SOUTH);
        this.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                exit();
            }
        });
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setSize(Toolkit.getDefaultToolkit().getScreenSize());
        cleanUp();
        setDefaultModule();
        this.pack();
    }

    /** 
     * Get the VerinecStudio instance.
     * As VerinecStudio is a singleton, only one instance may exist at any time.
     * The main routine must call show() on the analyser in order to display 
     * the window.
     *  
     * @return The instance of VerinecStudio.
     * @throws VerinecException If creation of VerinecStudio fails.
     */
    public static VerinecStudio getInstance() throws VerinecException {
        if (_instance == null) {
            _instance = new VerinecStudio();
        }
        return _instance;
    }

    /** Set up the default menues etc. 
     *  Called whenever the module is changed. 
     */
    private void initializeGui() {
        nwComponentChangeListeners = new HashSet();
        this.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ESCAPE"), "escKey");
        this.getRootPane().getActionMap().put("escKey", escKey);
        modifyAllowed = true;
        toolbarPanel.removeAll();
        toolbarPanel.add(stdToolbar);
        menuBar.removeAll();
        menuBar.add(menuNetwork);
        menuBar.add(menuPreferences);
        menuBar.add(menuAbout);
        this.setJMenuBar(menuBar);
        setStatus("");
        analyserSplitPane.setLeftComponent(drawScrollPanel);
        drawPanel.initialize();
        Component[] components = getNetworkComponents();
        for (int i = 0; i < components.length; i++) {
            ((NwComponent) components[i]).initialize();
        }
        infoSplitPane.initialize();
        enableSaveFromMenu();
    }

    /** Reset the modules drop down. */
    private void resetModules() {
        moduleSelector.removeAllItems();
        Iterator iter = config.getRootElement().getChildren("module").iterator();
        while (iter.hasNext()) {
            Element mod = (Element) iter.next();
            try {
                IVerinecModule m = (IVerinecModule) Class.forName(mod.getAttributeValue("class")).newInstance();
                moduleSelector.addItem(m);
            } catch (Throwable t) {
                logger.warning("Could not load module " + mod.getAttributeValue("name") + ": " + t.getMessage());
                logger.throwing(getClass().getName(), "resetModules", t);
            }
        }
    }

    /**
     * Add a toolbar to the application window (will be removed whenever the
     * module is changed).
     * 
     * @param bar The Toolbar to add.
     */
    public void addToolBar(JToolBar bar) {
        toolbarPanel.add(bar);
    }

    /**
     * Add a menu to the application window (will be removed whenever the module
     * is changed).
     * 
     * @param menu The JMenu to add.
     */
    public void addMenu(JMenu menu) {
        menuBar.add(menu);
    }

    /**
     * Set a component to the upper right corner of the application. Typically
     * this could be a JPanel.
     * 
     * @param c The awt Component to put into right upper part. Pass null to
     *            remove the component.
     */
    public void setTopComponent(Component c) {
        infoSplitPane.setTopComponent(c);
    }

    /**
     * Set a component to the bottom right corner of the application. Typically
     * this could be a JPanel.
     * 
     * @param c The awt Component to put into right lower part. Pass null to
     *            remove the component.
     */
    public void setBottomComponent(Component c) {
        infoSplitPane.setBottomComponent(c);
    }

    /** Change the ratio between upper and lower part of InfoSplitPane.
     * 
     * @param w The new ratio, 1 meaning only upper part visible, 0 only lower part.
     */
    public void setResizeWeight(double w) {
        infoSplitPane.setResizeWeight(w);
    }

    /**
     * Get the repository representing the current state of the network in the GUI.
     *
     * Note that updates to this repository are NOT reflected in the gui!
     * 
     * Use new NwHub, new PCNode resp. addNode and the add... methods in PCNode to create new elements or delete elements.
     * 
     * @return Get the network information.
     */
    public IVerinecRepository getRepository() {
        return currentState;
    }

    /**
     * Get all network components used in this simulation as java.awt objects.
     * 
     * @return NwComponents currently loaded.
     */
    public Component[] getNetworkComponents() {
        return drawPanel.getComponents();
    }

    /**
     * Get a pcnode by its name.
     * 
     * @param name The hostname of the node to return.
     * @return The PCNode with that name.
     * @throws VerinecException if no such node is existing.
     */
    public PCNode getNodeByName(String name) throws VerinecException {
        PCNode n = (PCNode) nameToNode.get(name);
        if (n == null) throw new VerinecException("No node with name " + name + " found");
        return n;
    }

    /**
     * Create a new PCNode and add its XML to the tree.
     * If the node is to have context menues, use your modules update
     * Component method or something similar.
     * 
     * @param x Horizontal coordinate for center of node
     * @param y Vertical coordinate for center of node
     * @return The newly created node
     */
    public PCNode addNode(int x, int y) {
        PCNode n = new PCNode(x, y, this);
        try {
            currentState.setNode(n.getConfig());
        } catch (Throwable t) {
            logger.warning("Unexpected exception caught when adding node xml definition. " + t.getMessage());
            logger.throwing(getClass().getName(), "addNode", t);
        }
        return n;
    }

    /**
     * Used internally by NwHub when created without element. Adds a network
     * connection (hub) element to the networks tree. Use detach on the XML to
     * delete from the tree. If you want to make the GUI add a Hub, just create
     * a new {@link verinec.gui.core.NwHub}. It will register
     * itselves in the analyser.
     * 
     * @param e The connection element to add to the XML tree.
     */
    public void addNetworkElement(Element e) {
        currentState.addPhysicalNetwork(e);
    }

    /**
     * For the module to set if modifying is allowed.
     * 
     * @param b True if modifying should be allowed, false otherwise.
     */
    public void setModifyAllowed(boolean b) {
        modifyAllowed = b;
    }

    /**
     * Find out whether the current module allows modifications to be made.
     * 
     * @return True if modifying is allowed, false otherwise.
     */
    public boolean isModifyAllowed() {
        return modifyAllowed;
    }

    /** Disable the save and new actions in the menu (used by some modules).  */
    public void disbleSaveFromMenu() {
        newAction.setEnabled(false);
        saveAction.setEnabled(false);
        saveAsAction.setEnabled(false);
    }

    /** Enable the save and new actions in the menu (used by some modules).  */
    public void enableSaveFromMenu() {
        newAction.setEnabled(true);
        saveAction.setEnabled(true);
        saveAsAction.setEnabled(true);
    }

    /** Load the default module, that is the first module in the configuration. */
    public void setDefaultModule() {
        setActiveModule((IVerinecModule) moduleSelector.getItemAt(0));
    }

    /**
     * Changes the module of the application, letting the module load itselves.
     * Internal methods also set the active module to null during some
     * operations.
     * 
     * @param newMod The module to load
     * @return Whether the new module was loaded successfully.
     */
    private boolean setActiveModule(IVerinecModule newMod) {
        inTransition = true;
        if (currentModule != null) currentModule.unload();
        initializeGui();
        if (newMod != null) {
            try {
                newMod.load(this);
            } catch (Throwable e) {
                setStatus("Could not load new module " + newMod.toString() + ". " + e.getMessage());
                logger.throwing(getClass().getName(), "setActiveModule", e);
                try {
                    if (currentModule != null) currentModule.load(this);
                    return false;
                } catch (VerinecException ex) {
                    setStatus("Man the lifeboats. Women and children first! (Could not load old module after failure with new module. Now you best restart the application.)");
                    logger.throwing(getClass().getName(), "setActiveModule", ex);
                    return false;
                }
            }
            setStatus("Switched to module " + newMod.toString());
        }
        currentModule = newMod;
        inTransition = false;
        this.validate();
        return true;
    }

    /**
     * Get the currently selected module. May be null.
     * 
     * @return The currently active module (may be null).
     */
    public IVerinecModule getActiveModule() {
        return currentModule;
    }

    /**
     * Get all modules currently available in the application.
     * 
     * @return The available modules.
     */
    public Vector getModules() {
        Vector v = new Vector(moduleSelector.getItemCount());
        for (int i = 0; i < moduleSelector.getItemCount(); i++) v.add(moduleSelector.getItemAt(i));
        return v;
    }

    /** Delete all nodes, globals and networks (Hubs). */
    private void cleanUp() {
        Component[] components = getNetworkComponents();
        for (int i = 0; i < components.length; i++) {
            try {
                if (components[i] instanceof NwNode) ((NwComponent) components[i]).delete();
            } catch (Throwable t) {
                logger.warning("Error during delete of a component: " + t.getMessage());
                logger.throwing(getClass().getName(), "cleanUp", t);
            }
        }
        drawPanel.repaint();
        currentState = new StudioRepository("New Project");
        repo = null;
        nameToNode = new Hashtable();
    }

    /** Determines if changes to the network definitions have been made.
     * A new network with 0 nodes is considered unchanged.
     * 
     * @return true if the network definitions are modified, false otherwise
     */
    public boolean isProjectModified() {
        if (repo == null) {
            return (currentState.getNodes().getChildren().size() > 0);
        }
        JDomComparator comparator = new JDomComparator();
        try {
            if (comparator.compare(repo.getNodes(), currentState.getNodes()) != 0) {
                return true;
            } else if (comparator.compare(repo.getPhysicalNetworks(), currentState.getPhysicalNetworks()) != 0) {
                return true;
            }
        } catch (VerinecException e) {
            return true;
        }
        return false;
    }

    /** Check if it is ok to unload the current project and unloads it.
     * 
     * Asks the user if the currently loaded network contains unsaved changes.
     * 
     * @return True if the network has been unloaded, false otherwise.
     */
    private boolean unloadProject() {
        int result = JOptionPane.OK_OPTION;
        if (isProjectModified()) {
            result = JOptionPane.showConfirmDialog(this, "Do you want to save the current Project?", "Project modified", JOptionPane.YES_NO_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                saveProject();
            }
        }
        if (result == JOptionPane.OK_OPTION || result == JOptionPane.NO_OPTION) {
            IVerinecModule mod = getActiveModule();
            setActiveModule(null);
            cleanUp();
            setActiveModule(mod);
            return true;
        } else {
            return false;
        }
    }

    /** Set the editor to an empty network. 
     * Asks the user if he wants to save the current network and creates a 
     * clean empty space.
     */
    public void newProject() {
        if (unloadProject()) {
            setTitle(title);
            setStatus("New Project");
        }
    }

    /**
     * Reloads the network from the repository. It does not ask but just discard
     * the current state. If the network was not previously saved, it is
     * completely removed.
     */
    public void reloadProject() {
        try {
            IVerinecRepository theRepo = repo;
            cleanUp();
            if (theRepo != null) {
                loadProject(theRepo);
                repo = theRepo;
            }
        } catch (VerinecException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Internal Error", JOptionPane.ERROR_MESSAGE);
            logger.throwing(getClass().getName(), "reloadNetwork", e);
            try {
                setStatus("Could not load project " + repo.getProjectName() + ": " + e.getMessage());
            } catch (VerinecException ex) {
            }
        }
    }

    /**
     * Display a dialog to choose a network from the repository to load.
     * If the current network has been changed, asks the user wants to save.
     * 
     * Internally calls {@link #loadProject(IVerinecRepository)} to do the 
     * actual work and {@link #newProject()} to unload current network.
     */
    public void loadProject() {
        String[] repNames;
        try {
            repNames = RepositoryFactory.getProjectNames();
        } catch (VerinecException e) {
            setStatus("Could not get project names");
            logger.throwing(getClass().getName(), "loadNetwork", e);
            JOptionPane.showMessageDialog(this, "Could not get project names\n" + e.getMessage(), "Internal Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (repNames.length < 1) {
            JOptionPane.showMessageDialog(this, "There are no saved projects available.", "No projects to open", JOptionPane.WARNING_MESSAGE);
            setStatus("There are no saved projects available");
            return;
        }
        if (!unloadProject()) {
            setStatus("Load Network cancelled");
            return;
        }
        Arrays.sort(repNames);
        String choice = (String) JOptionPane.showInputDialog(this, "Choose a project:", "Available Projects", JOptionPane.QUESTION_MESSAGE, null, repNames, null);
        if (choice != null) {
            try {
                setStatus("Please wait...");
                loadProject(RepositoryFactory.createRepository(choice));
            } catch (VerinecException e) {
                String m = e.getMessage();
                if (e.getCause() != null) m += "\n" + e.getCause().getMessage();
                JOptionPane.showMessageDialog(this, m + "\nIf you have non-english letters in the path you installed the application, you will need Internet access for the XML schemas to be loaded.", "Internal Error", JOptionPane.ERROR_MESSAGE);
                repo = null;
                setStatus("Could not load project " + choice);
                logger.throwing(getClass().getName(), "loadNetwork", e);
            }
        }
    }

    /**
     * Actually load a stored network from the repository.
     * Does not ask the user anything, drops the old network.
     * 
     * @param newRep The repository to load.
     * @throws VerinecException If any error occurs.
     */
    private void loadProject(IVerinecRepository newRep) throws VerinecException {
        IVerinecModule mod = null;
        try {
            if (!inTransition) {
                mod = getActiveModule();
                setActiveModule(null);
            }
            repo = newRep;
            currentState = new StudioRepository(repo.getProjectName());
            currentState.setGlobals(repo.getGlobals());
            currentState.setNodes(repo.getNodes());
            createNodes();
            currentState.setPhysicalNetworks(repo.getPhysicalNetworks());
            createHubs();
        } finally {
            if (!inTransition) setActiveModule(mod);
        }
        setTitle(title + ": " + repo.getProjectName());
        setStatus("Project " + repo.getProjectName() + " loaded.");
    }

    /** Creates the PCNode objects from this.nodes */
    private void createNodes() {
        Iterator iter = currentState.getNodes().getChildren("node", VerinecNamespaces.NS_NODE).iterator();
        while (iter.hasNext()) {
            Element currentNode = (Element) iter.next();
            PCNode n = new PCNode(currentNode, this);
            nameToNode.put(n.getName(), n);
        }
    }

    /** Creates the Hub objects from this.physical_networks */
    private void createHubs() {
        Component[] components = getNetworkComponents();
        Iterator hubs = currentState.getPhysicalNetworks().getChildren("physical_network", VerinecNamespaces.NS_NETWORK).iterator();
        try {
            while (hubs.hasNext()) {
                Element currentNetwork = (Element) hubs.next();
                Element child = currentNetwork.getChild("connected", VerinecNamespaces.NS_NETWORK);
                String idref = child.getAttributeValue("binding");
                for (int k = 0; k < components.length; k++) {
                    if (components[k] instanceof NwBinding) {
                        NwBinding aBinding = (NwBinding) components[k];
                        if (aBinding.getId().equals(idref)) {
                            new NwHub(currentNetwork, this);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            logger.warning("Misteriously, an exception occured while creating the network connections.");
            logger.throwing(getClass().getName(), "createHubs", t);
        }
    }

    /**
     * Save the current network data under its name or ask for a name if it does
     * not yet have one.
     */
    public void saveProject() {
        setStatus("Please wait...");
        if (repo == null) {
            saveProjectAs();
        } else {
            try {
                repo.setGlobals(currentState.getGlobals());
                repo.setNodes(currentState.getNodes());
                repo.setPhysicalNetworks(currentState.getPhysicalNetworks());
                setStatus("Saved project " + repo.getProjectName());
            } catch (Throwable e) {
                logger.throwing(getClass().getName(), "saveNetwork", e);
                JOptionPane.showMessageDialog(this, e.getMessage(), "Internal Error", JOptionPane.ERROR_MESSAGE);
                LogUtil.logJdom(logger, currentState.getPhysicalNetworks());
            }
        }
    }

    /**
     * Saves the network under a new name.
     */
    public void saveProjectAs() {
        setStatus("Please wait...");
        int answer = JOptionPane.OK_OPTION;
        String projectName;
        String[] existingProjects;
        try {
            existingProjects = RepositoryFactory.getProjectNames();
        } catch (VerinecException e) {
            logger.throwing(getClass().getName(), "saveAsNetwork", e);
            JOptionPane.showMessageDialog(this, "Could not get existing project names\n" + e.getMessage(), "Internal Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        while ((projectName = JOptionPane.showInputDialog(this, "Enter Project Name:")) != null) {
            if (projectName.length() == 0) continue;
            for (int i = 0; i < existingProjects.length; i++) {
                if (projectName.equals(existingProjects[i])) {
                    answer = JOptionPane.showConfirmDialog(this, "The project '" + projectName + "' exists already\nYes to overwrite, no to enter other name, or cancel.");
                    if (answer == JOptionPane.CANCEL_OPTION) return;
                    logger.fine("Erasing project to overwrite: " + projectName);
                    try {
                        repo = RepositoryFactory.createRepository(projectName);
                        repo.drop();
                        repo = null;
                    } catch (Throwable e) {
                        logger.throwing(getClass().getName(), "saveAsNetwork", e);
                        JOptionPane.showMessageDialog(this, e.getMessage(), "Internal Error", JOptionPane.ERROR_MESSAGE);
                    }
                    break;
                }
            }
            if (answer == JOptionPane.OK_OPTION) {
                setTitle(title + " " + projectName);
                try {
                    repo = RepositoryFactory.createRepository(projectName);
                } catch (Throwable e) {
                    logger.throwing(getClass().getName(), "saveAsNetwork", e);
                    JOptionPane.showMessageDialog(this, e.getMessage(), "Internal Error", JOptionPane.ERROR_MESSAGE);
                }
                saveProject();
                break;
            }
        }
    }

    /**
     * Save a module or application part configuration permanently. 
     * The XML is not validated against any schema or dtd. 
     * 
     * @param modulename The name of the module storing this configuration.
     * @param config The xml configuration data to save.
     * 
     * @see IConfigurable
     */
    public void saveConfig(String modulename, Document config) {
        try {
            FileOutputStream out = new FileOutputStream(new File(storage, modulename));
            XMLOutputter o = new XMLOutputter(Format.getPrettyFormat());
            o.output(config, out);
            out.flush();
            out.close();
        } catch (java.io.IOException e) {
            System.err.println("could not create config file!");
        }
    }

    /**
     * Get a configuration previously saved with {@link #saveConfig(String, Document)}.
     * The XML is not validated against any schema or dtd.
     * 
     * @param modulename The name of the module the configuration was stored for.
     * @return The configuration data or null if there is no config with such
     *         name or the file is invalid xml.
     * 
     * @see IConfigurable
     */
    public Document loadConfig(String modulename) {
        File f = new File(storage, modulename);
        if (!f.exists()) {
            logger.info("Config file does not exist: " + modulename);
            return null;
        }
        try {
            SAXBuilder parser = new SAXBuilder();
            return parser.build(f);
        } catch (Exception e) {
            logger.warning("Could not read config file " + modulename + ".\n" + e.getMessage());
            logger.throwing(getClass().getName(), "loadConfig", e);
            return null;
        }
    }

    /**
     * Set the text for the status bar. The status is also logged with level
     * finer.
     * 
     * @param status Text to appear in the status bar.
     */
    public void setStatus(String status) {
        if (status.length() > 0) logger.finer("GUI set to status: " + status);
        lblStatus.setText(status);
    }

    /**
     * Tell the analyser that a NwComponent has changed its state. VerinecStudio will
     * notify all its ChangeListeners, using c as source of the ChangeEvent.
     * 
     * @param c The NwComponent of which the state has changed.
     */
    public void nwComponentStateChanged(NwComponent c) {
        if (c instanceof PCNode) {
            if (nameToNode.containsValue(c)) {
                Iterator set = nameToNode.entrySet().iterator();
                while (set != null && set.hasNext()) {
                    Map.Entry e = (Map.Entry) set.next();
                    if (e.getValue().equals(c)) {
                        set.remove();
                        set = null;
                    }
                }
            }
            nameToNode.put(((PCNode) c).getName(), c);
        }
        Iterator iter = nwComponentChangeListeners.iterator();
        while (iter.hasNext()) {
            try {
                ((ChangeListener) iter.next()).stateChanged(new ChangeEvent(c));
            } catch (Throwable t) {
                logger.warning("ChangeListener has thrown exception, ignored.");
                logger.throwing(getClass().getName(), "nwComponentStateChanged", t);
            }
        }
    }

    /**
     * Register a class as NwComponent change listener. It will get notified
     * every time one of the NwComponent gets selected, unselected or modified
     * using the context menue. The ChangeEvent passed to the listener contains
     * the NwComponent which changed as source.
     * 
     * @param l The listener to register.
     */
    public void addNwComponentStateListener(ChangeListener l) {
        nwComponentChangeListeners.add(l);
    }

    /**
     * Remove a class from NwComponent change listener. It wont get notified any
     * longer.
     * 
     * @param l The listener to remove.
     */
    public void removeNwComponentStateListener(ChangeListener l) {
        nwComponentChangeListeners.remove(l);
    }

    /** Tell application to exit. 
     * If there is unsaved data, asks the user wheter to save, discard or cancel.
     * 
     * @return True if exiting took place, false otherwise
     */
    public boolean exit() {
        if (isProjectModified()) {
            if (!unloadProject()) return false;
        }
        dispose();
        System.exit(0);
        return true;
    }

    /**
     * Start the program. See the documentation of the class {@link VerinecStudio} 
     * for environment and keyboard options. 
     * <p>
     * Creates an instance of this class. As long as the window stays open, the
     * program keeps running. There are currently no command line arguments.
     * </p>
     * 
     * @param args The command line arguments. They are simply ignored and have no effect.
     */
    public static void main(String[] args) {
        try {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            VerinecStudio.getInstance().setVisible(true);
        } catch (Exception e) {
            System.err.println("A problem occured while loading Verinec Studio: " + e.getMessage());
            e.printStackTrace();
            Logger.global.severe("A problem occured while loading the UI Manager: " + e.getMessage());
            Logger.global.throwing("VerinecStudio", "main", e);
            return;
        }
    }

    /** Load module configuration. (Used in the constructor.)
     * If there exists none, create the configuration with default modules 
     * (Configurator and Simulator)
     * Saves the configuration after creation. The XML is very simple: a 
     * modules root element holds module tags with attributes name and class.
     * 
     * @return The configuration document.
     */
    private Document getConfig() {
        Document newconfig = loadConfig(configName);
        if (newconfig == null) {
            Element mods = new Element("modules");
            Element mod = new Element("module");
            mod.setAttribute("name", new verinec.gui.configurator.Configurator().toString());
            mod.setAttribute("class", "verinec.gui.configurator.Configurator");
            mods.addContent(mod);
            newconfig = new Document(mods);
        }
        return newconfig;
    }

    /**
     * Get the modules configuration panel.
     * 
     * @return An instance of ConfigPanel.
     */
    public ConfigPanel getConfigPanel() {
        return new ModulesConfig(this, config);
    }

    /**
     * Save the modules configuration.
     * 
     * @param config The modules configuration document.
     */
    public void saveConfiguration(Document config) {
        this.config = config;
        resetModules();
        saveConfig(configName, config);
    }

    /** Action to exit the application. */
    class ExitAction extends AbstractAction {

        /** Instantiate the action with name Exit VeriNeC.
         * @throws VerinecException If icon can not be loaded.
         */
        public ExitAction() throws VerinecException {
            super("Exit VeriNeC", GuiUtil.loadIcon(VerinecStudio.this, CLOSE_ICON));
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F4, KeyEvent.ALT_DOWN_MASK));
        }

        /** Called when action is performed, delegates to the analyser.exit() method.
         * @param e The event.
         */
        public void actionPerformed(ActionEvent e) {
            VerinecStudio.this.exit();
        }
    }

    /** Execute new network configuration. Calls newNetwork of the VerinecStudio. 
      * @author david.buchmann at unifr.ch
      */
    class NewAction extends AbstractAction {

        /** Instantiate with name New. 
          * @throws VerinecException If the icon can not be loaded.
          */
        public NewAction() throws VerinecException {
            super("New", GuiUtil.loadIcon(VerinecStudio.this, NEW_ICON));
            putValue(SHORT_DESCRIPTION, "Create new network");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
        }

        /** Execute action 
          * @param e unused 
          */
        public void actionPerformed(ActionEvent e) {
            newProject();
        }
    }

    /** Execute loading of data. Calls loadNetwork of the VerinecStudio. 
      * @author david.buchmann at unifr.ch
      */
    class LoadAction extends AbstractAction {

        /** Instantiate with name Load. 
          * @throws VerinecException If the icon can not be loaded.
          */
        public LoadAction() throws VerinecException {
            super("Open", GuiUtil.loadIcon(VerinecStudio.this, LOAD_ICON));
            putValue(SHORT_DESCRIPTION, "Load a stored network");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
        }

        /** Execute action 
          * @param e unused 
          */
        public void actionPerformed(ActionEvent e) {
            loadProject();
        }
    }

    /** Execute saving of data. Calls saveNetwork of the VerinecStudio. 
      * @author david.buchmann at unifr.ch
      */
    class SaveAction extends AbstractAction {

        /** Instantiate with name Save. 
          * @throws VerinecException If the icon can not be loaded.
          */
        public SaveAction() throws VerinecException {
            super("Save", GuiUtil.loadIcon(VerinecStudio.this, SAVE_ICON));
            putValue(SHORT_DESCRIPTION, "Save the network");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        }

        /** Execute action 
          * @param e unused 
          */
        public void actionPerformed(ActionEvent e) {
            saveProject();
        }
    }

    /** Execute save as of data. Calls saveAsNetwork of the VerinecStudio. 
      * @author david.buchmann at unifr.ch
      */
    class SaveAsAction extends AbstractAction {

        /** Instantiate with name Save as... 
          * @throws VerinecException If the icon can not be loaded.
          */
        public SaveAsAction() throws VerinecException {
            super("Save as...", GuiUtil.loadIcon(VerinecStudio.this, SAVEAS_ICON));
            putValue(SHORT_DESCRIPTION, "Save the network under new name");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
        }

        /** Execute action 
          * @param e unused 
          */
        public void actionPerformed(ActionEvent e) {
            saveProjectAs();
        }
    }

    /**
     * Show preferences dialog.
     * 
     * @author david.buchmann at unifr.ch
     */
    class OptionsAction extends AbstractAction {

        /**
         * Instantiate with name "Options...".
         * 
         * @throws VerinecException
         *             If the icon can not be loaded.
         */
        public OptionsAction() throws VerinecException {
            super("Options...", GuiUtil.loadIcon(VerinecStudio.this, CONFIGURE_ICON));
        }

        /**
         * Execute action
         * 
         * @param e
         *            unused
         */
        public void actionPerformed(ActionEvent e) {
            ConfigDialog.showDialog(VerinecStudio.this);
        }
    }

    /**
     * Displays an "About" box.
     * 
     * @author david.buchmann at unifr.ch
     */
    class AboutAction extends AbstractAction {

        /**
         * Instantiate with name "About...".
         * 
         * @throws VerinecException If the icon can not be loaded.
         */
        public AboutAction() throws VerinecException {
            super("About...", GuiUtil.loadIcon(VerinecStudio.this, HELP_ICON));
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        }

        /**
         * Execute action
         * 
         * @param e unused
         */
        public void actionPerformed(ActionEvent e) {
            String message = "<html><body>" + "<h1><font color=\"red\">VeriNeC Studio</font></h1>" + "<h2>Verified Network Configuration</h2>" + "<p>Prototype implementation of a network management system.<br>" + "The aim is to improve security and reliability in large, heterogenous<br>" + "networks, while at the same time facilitate their administration.<br>" + "Verinec uses a centralised database of the entire network and services <br>" + "configuration and validates correctness before configuring the devices.<br>" + "The configuration is represented in XML and abstracted from specific <br>" + "implementations. XML technologies are used to translate the abstracted <br>" + "configuration into implementation specific formats.<br>" + "The translated configuration is automatically distributed using existing<br>" + "remote administration protocols.</p>" + "<p><br>Dissertation by Dominik Jungo and David Buchmann</p>" + "<p>Contains work from our bachelor and master students: Patrick Aebischer,<br>" + "Geraldine Antener, Robert Awesso, Christoph Ehret, Jason Hug, Renato <br>" + "Loeffel, Martial Seifriz, Damian Vogel, Nadine Zurkinden.</p>" + "<p><br><br><font color =\"blue\">T</font>" + "<font color =\"green\">N</font>" + "<font color =\"red\">S</font> Group, University of Fribourg, 2003-2007<br>" + "<br>Project website: <a href=\"http://diuf.unifr.ch/tns/projects/verinec/\">diuf.unifr.ch/tns/projects/verinec/</a></p>" + "</body></html>";
            JOptionPane.showMessageDialog(VerinecStudio.this, message, "About", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /** Listener for keyboard typing, reacts on esc key to unselect nodes. */
    class EscKeyAction extends AbstractAction {

        /** See if action is esc key and unselect nodes if it is. 
         * @param e The key information.
         */
        public void actionPerformed(ActionEvent e) {
            drawPanel.unselectAllComponents();
        }
    }

    /**
     * Switch to other module.
     * 
     * @author david.buchmann at unifr.ch
     */
    class ModuleSelectAction extends AbstractAction {

        /** Instantiate . */
        public ModuleSelectAction() {
            super("Select Mode");
            putValue(SHORT_DESCRIPTION, "Select module");
        }

        /**
         * Change module if the selected module is different from the currently
         * active one.
         * 
         * @param e unused
         */
        public void actionPerformed(ActionEvent e) {
            if (getActiveModule() != moduleSelector.getSelectedItem()) {
                if (!setActiveModule((IVerinecModule) moduleSelector.getSelectedItem())) {
                    moduleSelector.setSelectedItem(getActiveModule());
                }
            }
        }
    }

    /** Configuration Dialog for adding and removing modules.
     * Contains buttons to add or remove modules and to choose a module to configure.
     */
    class ModulesConfig extends ConfigPanel implements ActionListener {

        private JComboBox cboModules;

        private JTextField txtClass;

        private JButton btnNew;

        private JButton btnUpdate;

        private JButton btnRemove;

        private int oldSelection;

        /** Instantiate the panel.
         *  @param analyser The parent for this dialog.
         *  @param config The default configuration.
         */
        public ModulesConfig(VerinecStudio analyser, Document config) {
            super("Modules", analyser, config, new BorderLayout());
            JPanel center = new JPanel(new BorderLayout());
            txtClass = new JTextField(30);
            JPanel brpfs = new JPanel();
            brpfs.add(txtClass);
            center.add(brpfs, BorderLayout.WEST);
            center.add(new JLabel("Class (including package name):"), BorderLayout.NORTH);
            add(center, BorderLayout.CENTER);
            JPanel south = new JPanel(new BorderLayout());
            btnNew = new JButton("New");
            btnNew.addActionListener(this);
            south.add(btnNew, BorderLayout.WEST);
            btnUpdate = new JButton("Find all Modules in classpath");
            btnUpdate.addActionListener(this);
            south.add(btnUpdate, BorderLayout.CENTER);
            btnRemove = new JButton("Remove");
            btnRemove.addActionListener(this);
            south.add(btnRemove, BorderLayout.EAST);
            add(south, BorderLayout.SOUTH);
            cboModules = new JComboBox();
            reset();
            JPanel west = new JPanel();
            west.add(cboModules);
            add(west, BorderLayout.WEST);
        }

        /** Update the XML with information from dialog window. */
        public void updateConfig() {
            try {
                if (oldSelection < 0) return;
                Element mod = createModuleElement(txtClass.getText());
                Element oldMod = (Element) config.getRootElement().getChildren("module").get(oldSelection);
                if (!(oldMod.getAttributeValue("name").equals(mod.getAttributeValue("name")) && oldMod.getAttributeValue("class").equals(mod.getAttributeValue("class")))) {
                    oldMod.setAttribute((Attribute) mod.getAttribute("name").clone());
                    oldMod.setAttribute((Attribute) mod.getAttribute("class").clone());
                    int pos = cboModules.getSelectedIndex();
                    cboModules.removeItemAt(oldSelection);
                    cboModules.insertItemAt(mod.getAttributeValue("name"), oldSelection);
                    cboModules.setSelectedIndex(pos);
                }
            } catch (Throwable t) {
                logger.throwing(getClass().getName(), "updateConfig", t);
                JOptionPane.showMessageDialog(VerinecStudio.this, "Could not load specified module class.\nSetting not changed.\n" + t.getMessage());
            }
        }

        /** Update the dialog from the configuration information. */
        public void updateDialog() {
            cboModules.removeActionListener(this);
            cboModules.removeAllItems();
            Iterator iter = config.getRootElement().getChildren("module").iterator();
            while (iter.hasNext()) {
                Element mod = (Element) iter.next();
                try {
                    IVerinecModule m = (IVerinecModule) Class.forName(mod.getAttributeValue("class")).newInstance();
                    cboModules.addItem(mod.getAttributeValue("name") + (mod.getAttributeValue("name").equals(m.toString()) ? "" : "(" + m.toString() + ")"));
                } catch (Throwable t) {
                    logger.warning("Could not load module " + mod.getAttributeValue("name") + ": " + t.getMessage());
                    logger.throwing(getClass().getName(), "updateDialog", t);
                    cboModules.addItem(mod.getAttributeValue("name") + " (invalid)");
                }
            }
            cboModules.addActionListener(this);
            oldSelection = -1;
            cboModules.setSelectedIndex(0);
        }

        /**
         * Something happened
         * 
         * @param e unused
         */
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == btnNew) {
                String classname;
                while ((classname = JOptionPane.showInputDialog(VerinecStudio.this, "Enter full class name (with package):")) != null) {
                    try {
                        Element mod = createModuleElement(classname);
                        config.getRootElement().addContent(mod);
                        cboModules.addItem(mod.getAttributeValue("name"));
                        cboModules.setSelectedIndex(cboModules.getItemCount() - 1);
                        return;
                    } catch (Throwable t) {
                        logger.throwing(getClass().getName(), "actionPerformed", t);
                        JOptionPane.showMessageDialog(VerinecStudio.this, "Could not load specified module class.\nPlease try again or cancel input prompt.\n" + t.getMessage());
                    }
                }
            } else if (e.getSource() == btnRemove) {
                int pos = cboModules.getSelectedIndex();
                cboModules.removeItemAt(pos);
                ((Element) config.getRootElement().getChildren("module").get(pos)).detach();
                cboModules.setSelectedIndex((pos > 0) ? pos - 1 : 0);
            } else if (e.getSource() == cboModules) {
                updateConfig();
                int pos = cboModules.getSelectedIndex();
                List l = config.getRootElement().getChildren("module");
                if (pos < l.size()) {
                    Element mod = (Element) l.get(pos);
                    txtClass.setText(mod.getAttributeValue("class"));
                    oldSelection = pos;
                }
            } else if (e.getSource() == btnUpdate) {
                FindClassThread myThread = new FindClassThread(this);
                SearchingDialog sd = new SearchingDialog((Dialog) getTopLevelAncestor(), myThread);
                myThread.setDialog(sd);
                myThread.start();
                btnUpdate.setText("...please wait. searching...");
                sd.setVisible(true);
            }
        }

        private void updateFoundClasses(ArrayList modules) {
            Iterator iter = modules.iterator();
            while (iter.hasNext()) {
                boolean found = false;
                String modClass = ((String) iter.next());
                Iterator iter2 = config.getRootElement().getChildren("module").iterator();
                while (iter2.hasNext()) {
                    if (modClass.equals(((Element) iter2.next()).getAttributeValue("class"))) {
                        found = true;
                    }
                }
                if (!found) {
                    try {
                        Element mod = createModuleElement(modClass);
                        config.getRootElement().addContent(mod);
                        cboModules.addItem(mod.getAttributeValue("name"));
                        cboModules.setSelectedIndex(cboModules.getItemCount() - 1);
                    } catch (Throwable t) {
                        logger.throwing(getClass().getName(), "updateFoundClasses", t);
                        JOptionPane.showMessageDialog(VerinecStudio.this, "Could not load specified module class.\nPlease try again or cancel input prompt.\n" + t.getMessage());
                    }
                }
            }
            btnUpdate.setText("Find all Modules in classpath");
        }

        private void resetUpdateBtn() {
            btnUpdate.setText("Find all Modules in classpath");
        }

        /**
         * Create the JDOM element for a module, containing attributes name and
         * class.
         * 
         * @param classname A fully qualified class name for a IVerinecModule
         * @return The JDOM element for the IVerinecModule.
         * @throws Throwable If the class can not be loaded or is no IVerinecModule.
         */
        private Element createModuleElement(String classname) throws Throwable {
            Object o = Class.forName(classname).newInstance();
            if (!(o instanceof IVerinecModule)) {
                throw new VerinecException("Specified class does not implement IVerinecModule.");
            }
            IVerinecModule m = (IVerinecModule) o;
            Element mod = new Element("module");
            mod.setAttribute("name", m.toString());
            mod.setAttribute("class", classname);
            return mod;
        }
    }

    /**
     * Thread that searches for IVerinecModules
     * 
     * @author Dominik Jungo
     */
    class FindClassThread extends Thread {

        private ModulesConfig moduleC;

        private boolean search;

        private SearchingDialog sdialog;

        /**
         * Instanciate the thread
         * 
         * @param moduleC
         *            the Config Dialog
         */
        public FindClassThread(ModulesConfig moduleC) {
            setName("FindClassThread");
            this.moduleC = moduleC;
        }

        private void cancelSearch() {
            search = false;
            moduleC.resetUpdateBtn();
        }

        private void setDialog(SearchingDialog sdialog) {
            this.sdialog = sdialog;
        }

        /**
         * Searches for the IVerinecModules and updates them in the Config
         * Dialog.
         * 
         * The Thread can be cancelled by calling <i>cancelSearch() </i>. The
         * callceled thread continues to run searching but doesn't update it's
         * results.
         * 
         * @see java.lang.Thread#run()
         */
        public void run() {
            search = true;
            try {
                ArrayList modules = InterfaceUtility.getClassResources("verinec.gui.IVerinecModule");
                if (search) {
                    moduleC.updateFoundClasses(modules);
                }
                sdialog.setVisible(false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Modal Dialog that is shown during searching for Modules.
     * 
     * @author Dominik Jungo
     */
    class SearchingDialog extends PleaseWaitDialog {

        FindClassThread thread;

        /**
         * Instanciates a new Modal Dialog that is shown during searching for
         * Modules. After searching the thread closes this dialog. If this
         * dialog is closed manually, the search is calcelled.
         * 
         * @param parent the Parent Dialog
         * @param thread the searching thread
         */
        public SearchingDialog(Dialog parent, FindClassThread thread) {
            super(parent, "Searching", "Searching, please wait...");
            this.thread = thread;
        }

        /** Cancel the search thread. 
         * @return Always returns true.
         */
        protected boolean cancel() {
            thread.cancelSearch();
            return true;
        }
    }

    /** A repository to interact with the network currently represented in the gui.
     * Note that updates are NOT reflected in the gui!
     * 
     * Use new NwHub, new PCNode resp. addNode and the add... methods in PCNode to create new elements or delete elements.
     *  
     * Implementation: Relies on the {@link #repo} for handling the lists of other projects.
     * 
     * @author david.buchmann at unifr.ch
     */
    class StudioRepository extends VerinecRepository {

        /** Create the studio repository.
    	 * @param name Project name.
    	 */
        public StudioRepository(String name) {
            super(name);
        }

        /** Get the names of all existing projects that can be opened with
          * openProject(). There is no defined order for the names.
          *
          * <p>All implementations should also contain a similar method sgetProjectNames
          * which is static, for use in the factory. (As Java does not allow static methods to be declared in
          * an Interface, this feature can not be enforced.)</p>
          *
          * @return Array of Strings with the names of all existing projects.
          * @throws VerinecException if some error occurs while trying to collect the project names.
          */
        public String[] getProjectNames() throws VerinecException {
            return RepositoryFactory.getProjectNames();
        }

        /** Drop this repository, that is remove all nodes and connections and globals and start with an empty network.
         *  This is the same as {@link VerinecStudio#newProject()} 
         * 
         * @throws VerinecException If the project could not be cleaned.
         */
        public void drop() throws VerinecException {
            newProject();
        }

        /** Set a network Node. 
         * Do not use this to create new nodes in the gui, rather use 
         * {@link VerinecStudio#addNode(int, int)}.
          *
          * @param node The Jdom Element of the node.
          * @throws VerinecException if the node is not valid or can not be stored.
          */
        public void setNode(Element node) throws VerinecException {
            super.setNode(node);
        }

        /** Overwrite to declare no exception.
         * @return The nodes element.
         * @see VerinecRepository#getNodes()
         */
        public Element getNodes() {
            try {
                return super.getNodes();
            } catch (VerinecException e) {
                logger.warning("Unexpected exception, returning null");
                logger.throwing(getClass().getName(), "getNodes", e);
                return null;
            }
        }

        /** Get the networks data of this project.
          * Returns a copy of the data, to modify use setPhysicalNetworks. 
          * <b>todo</b>: What should we do if the network is invalid because a node has
          *  been deleted? We do not want to loose the whole network.
          * 
          * @return The networks element, containing the definition of the network of this project.
          */
        public Element getPhysicalNetworks() {
            try {
                return super.getPhysicalNetworks();
            } catch (VerinecException e) {
                throw new InternalError("Could not get physical networks " + e.toString());
            }
        }

        /** Add a physical_network to the list of physical networks. 
         * 
         * @param e The network element to add to nw:physical.
         */
        public void addPhysicalNetwork(Element e) {
            networks.getChild("physical", VerinecNamespaces.NS_NETWORK).addContent(e);
        }
    }
}
