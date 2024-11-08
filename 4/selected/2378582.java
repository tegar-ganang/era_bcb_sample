package de.jlab.ui.main;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.beans.PropertyVetoException;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import de.jlab.GlobalsLocator;
import de.jlab.ScriptExecuter;
import de.jlab.ScriptInitializer;
import de.jlab.boards.Board;
import de.jlab.config.ConnectionConfig;
import de.jlab.config.FrameConfig;
import de.jlab.config.ScriptConfig;
import de.jlab.config.ValueWatchConfig;
import de.jlab.config.WorkspaceConfig;
import de.jlab.config.external.ExternalFrameConfig;
import de.jlab.external.measurement.model.ExternalModel;
import de.jlab.lab.CommandFileProcessor;
import de.jlab.lab.Lab;
import de.jlab.ui.JLabUIComponent;
import de.jlab.ui.about.AboutDialog;
import de.jlab.ui.connectionsetting.ConnectionSelectionDialog;
import de.jlab.ui.external.UIExternalModule;
import de.jlab.ui.modules.UILabModule;
import de.jlab.ui.tools.NameDialog;
import de.jlab.ui.tools.Ordering;
import de.jlab.ui.tools.StatusBar;
import de.jlab.ui.valuewatch.ValueWatch;
import de.jlab.ui.valuewatch.ValueWatchConfiguratorDialog;
import de.jlab.ui.valuewatch.ValueWatchManager;

public class JLabMainUI implements ValueWatchManager {

    public enum APPTYPE {

        STANDALONE, APPLET
    }

    ;

    JPanel mainPanel = new JPanel();

    StatusBar mainStatusBar = new StatusBar();

    private static Logger stdlog = Logger.getLogger(JLabMainUI.class.toString());

    Lab theLab = null;

    JDesktopPane desktop = new JDesktopPane();

    List<JLabWorkspace> workspaces = new ArrayList<JLabWorkspace>();

    JMenuBar menuBar = new JMenuBar();

    JMenu menuFile = new JMenu(GlobalsLocator.translate("main-menu-file"));

    JMenu menuModules = new JMenu(GlobalsLocator.translate("main-menu-modules"));

    JMenu menuExternalModules = new JMenu(GlobalsLocator.translate("main-menu-external-modules"));

    JMenu menuWindows = new JMenu(GlobalsLocator.translate("main-menu-windows"));

    JMenu menuScripts = new JMenu(GlobalsLocator.translate("main-menu-skipts"));

    JMenu menuWorkspaces = new JMenu(GlobalsLocator.translate("main-menu-workspaces"));

    JMenu menuHelp = new JMenu(GlobalsLocator.translate("main-menu-help"));

    JMenu menuMem = new JMenu("");

    JCheckBoxMenuItem jCheckBoxSendValueWatch = null;

    JCheckBoxMenuItem jCheckBoxValueWatchAsynchonously = null;

    JCheckBoxMenuItem jCheckBoxMenuItemToggleCommandConfirmation = null;

    JCheckBoxMenuItem jCheckBoxMenuItemToggleCommandProtocol = null;

    JCheckBoxMenuItem jCheckBoxMenuItemToggleCheckSum = null;

    Map<JInternalFrame, UILabModule> frameToModule = new HashMap<JInternalFrame, UILabModule>();

    Map<JCheckBoxMenuItem, UILabModule> windowMenuItemToModule = new HashMap<JCheckBoxMenuItem, UILabModule>();

    Map<JLabWorkspace, JCheckBoxMenuItem> workspaceToMenuItem = new HashMap<JLabWorkspace, JCheckBoxMenuItem>();

    Map<JInternalFrame, UIExternalModule> frameToExternalModule = new HashMap<JInternalFrame, UIExternalModule>();

    Map<JCheckBoxMenuItem, UIExternalModule> windowMenuItemToExternalModule = new HashMap<JCheckBoxMenuItem, UIExternalModule>();

    JLabWorkspace selectedWorkspace = null;

    ButtonGroup workspaceGroup = new ButtonGroup();

    List<UILabModule> uiModules = new ArrayList<UILabModule>();

    Map<String, UIExternalModule> uiExternalModules = new HashMap<String, UIExternalModule>();

    Set<Integer> alreadySentWatches = new HashSet<Integer>();

    ValueWatchThread valueWatcher = null;

    public static String DEFAULT_WORKSPACE_NAME = "Default";

    private boolean valueWatchesEnabled = true;

    private boolean valueWatchesAsynchronous = true;

    private APPTYPE appType;

    ButtonGroup valueWatchIntervallGroup = new ButtonGroup();

    DecimalFormat memoryFormat = new DecimalFormat("###,###,###.#kB");

    Map<Board, List<UILabModule>> boardToUIModules = new HashMap<Board, List<UILabModule>>();

    JMenuItem jMenuItemConfigureValueWatches = new JMenuItem(new ConfigureValueWatchesAction(this));

    Set<Integer> valueWatchActiveSet = new HashSet<Integer>();

    JMenuItem jMenuItemNewScript = new JMenuItem(new NewScriptAction(this));

    JMenuItem jMenuExecuteCommandScript = new JMenuItem(new NewScriptAction(this));

    long windowCounter = 1;

    ScriptExecuter scriptExecuter = null;

    public JLabMainUI(Lab lab, APPTYPE appType) {
        theLab = lab;
        this.appType = appType;
        this.scriptExecuter = ScriptInitializer.getScriptExecuter();
        if (scriptExecuter != null) scriptExecuter.init(this, lab);
        initUI();
    }

    private void initUI() {
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.add(desktop, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        mainPanel.add(mainStatusBar, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        valueWatcher = new ValueWatchThread(this);
        menuBar.add(menuFile);
        menuBar.add(menuModules);
        menuBar.add(menuExternalModules);
        menuBar.add(menuWorkspaces);
        if (scriptExecuter != null) menuBar.add(menuScripts);
        menuBar.add(menuWindows);
        menuBar.add(menuHelp);
        menuBar.add(Box.createHorizontalGlue());
        menuBar.add(menuMem);
        menuHelp.add(new AboutAction(this));
        menuHelp.addSeparator();
        menuHelp.add(new GarbageCollectAction());
        menuHelp.addSeparator();
        menuHelp.add(new ShowConsoleAction(GlobalsLocator.getConsoleFrame()));
        prepareStaticWindowEntries();
        if (appType == APPTYPE.STANDALONE) {
            menuFile.add(new ConfigureConnectionAction(this));
            menuFile.addSeparator();
        }
        menuFile.add(new StoreConfigAction(this));
        jCheckBoxSendValueWatch = new JCheckBoxMenuItem(new ToggleValueWatchAction(this));
        jCheckBoxValueWatchAsynchonously = new JCheckBoxMenuItem(new ToggleValueWatchSynchonouslyAction(this));
        jCheckBoxMenuItemToggleCommandConfirmation = new JCheckBoxMenuItem(new ToggleCommandConfirmationAction(this));
        jCheckBoxMenuItemToggleCommandProtocol = new JCheckBoxMenuItem(new ToggleCommandDebugAction(this));
        jCheckBoxMenuItemToggleCheckSum = new JCheckBoxMenuItem(new ToggleChecksumAction(this));
        menuFile.addSeparator();
        menuFile.add(jCheckBoxMenuItemToggleCommandConfirmation);
        menuFile.add(jCheckBoxMenuItemToggleCommandProtocol);
        menuFile.add(jCheckBoxMenuItemToggleCheckSum);
        menuFile.addSeparator();
        menuFile.add(jCheckBoxSendValueWatch);
        menuFile.add(jCheckBoxValueWatchAsynchonously);
        menuFile.add(jMenuItemConfigureValueWatches);
        Integer[] intervalls = new Integer[] { 100, 200, 250, 500, 1000 };
        JCheckBoxMenuItem defaultItem = null;
        for (int i = 0; i < intervalls.length; ++i) {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem();
            item.setAction(new SelectValueWatchIntervallAction(valueWatcher, intervalls[i], item));
            valueWatchIntervallGroup.add(item);
            menuFile.add(item);
            if (intervalls[i] == valueWatcher.getWaitCycle()) defaultItem = item;
        }
        defaultItem.setSelected(true);
        menuFile.addSeparator();
        menuFile.add(new ExecuteCommandScriptAction(this));
        menuFile.addSeparator();
        menuFile.add(new CloseJLabAction(this));
        jCheckBoxSendValueWatch.setSelected(true);
        prepareStaticWorkspaceMenuEntries();
        JLabWorkspace defaultWorkspace = new JLabWorkspace(DEFAULT_WORKSPACE_NAME);
        workspaces.add(defaultWorkspace);
        defaultWorkspace.setDefaultWorkspace(true);
        selectedWorkspace = defaultWorkspace;
        JCheckBoxMenuItem defaultMenuItem = new JCheckBoxMenuItem(new SelectWorkspaceAction(defaultWorkspace, this));
        workspaceGroup.add(defaultMenuItem);
        defaultMenuItem.setSelected(true);
        menuWorkspaces.add(defaultMenuItem);
        workspaceToMenuItem.put(defaultWorkspace, defaultMenuItem);
        setupScriptMenu();
        valueWatcher.start();
    }

    private void setupScriptMenu() {
        menuScripts.removeAll();
        menuScripts.add(jMenuItemNewScript);
        menuScripts.addSeparator();
        if (theLab.getConfig().getScripts() != null) {
            List<ScriptConfig> scripts = theLab.getConfig().getScripts();
            for (int i = 0; i < scripts.size(); ++i) {
                menuScripts.add(new StartScriptAction(scripts.get(i).getLocation(), this));
            }
        }
    }

    public Component getMainComponent() {
        return mainPanel;
    }

    public JMenuBar getMenuBar() {
        return menuBar;
    }

    public void enableValueWatches(boolean enable) {
        this.valueWatchesEnabled = enable;
    }

    /**
    * shut down the lab. stop all threads, disconnect etc.
    */
    public void stopJLab() {
        int option = JOptionPane.showConfirmDialog(GlobalsLocator.getMainFrame(), GlobalsLocator.translate("save-workspace-msg"), GlobalsLocator.translate("save-workspace-title"), JOptionPane.YES_NO_OPTION);
        if (option == JOptionPane.YES_OPTION) {
            syncCurrentWorkspace();
            this.storeConfig();
        }
        System.exit(0);
    }

    /**
    * add a ui-Module to the main menu
    * 
    * @param newModule
    */
    public void addLabModule(Board board, UILabModule newModule) {
        JMenu menuForModule = createMenuByName(menuModules, newModule.getMenuPath());
        String titleComp = "";
        if (board != null) titleComp = board.getBoardInstanceIdentifier();
        StartUIModuleAction newModuleAction = new StartUIModuleAction(titleComp, this, newModule);
        menuForModule.add(newModuleAction);
        uiModules.add(newModule);
        if (board != null) {
            List<UILabModule> modulesForBoard = boardToUIModules.get(board);
            if (modulesForBoard == null) {
                modulesForBoard = new ArrayList<UILabModule>();
                boardToUIModules.put(board, modulesForBoard);
            }
            modulesForBoard.add(newModule);
        }
    }

    /**
    * add a ui-Module to the main menu
    * 
    * @param newModule
    */
    public void addExternalModule(UIExternalModule newModule, String menuPath) {
        JMenu menuForModule = createMenuByName(menuExternalModules, menuPath);
        StartUIExternalModuleAction newModuleAction = new StartUIExternalModuleAction(this, newModule);
        menuForModule.add(newModuleAction);
        uiExternalModules.put(newModule.getModuleId(), newModule);
    }

    /**
    * Add a Menu within a given submenu path.
    * 
    * @param parentMenu the parent menu to add the menu to (in its subpath)
    * @param path A Path where to install the menu Paths are separated by slashes like Menu/SubMenu1/SubMenu2 etc.
    * @return
    */
    private JMenu createMenuByName(JMenu parentMenu, String path) {
        JMenu createdMenu = null;
        int separatorPos = path.indexOf('/');
        String neededSubMenu = null;
        if (separatorPos == -1) {
            neededSubMenu = path;
        } else {
            neededSubMenu = path.substring(0, separatorPos);
        }
        JMenu foundMenu = null;
        for (Component currComponent : parentMenu.getMenuComponents()) {
            if (currComponent instanceof JMenu) {
                JMenu currMenu = (JMenu) currComponent;
                if (currMenu.getText().equals(neededSubMenu)) {
                    foundMenu = (JMenu) currComponent;
                    break;
                }
            }
        }
        if (foundMenu == null) {
            foundMenu = new JMenu(neededSubMenu);
            parentMenu.add(foundMenu);
        }
        if (separatorPos != -1) {
            return createMenuByName(foundMenu, path.substring(separatorPos + 1));
        } else {
            createdMenu = foundMenu;
        }
        return createdMenu;
    }

    private void buildValueWatchListFromConfig() {
        valueWatchActiveSet.clear();
        List<ValueWatchConfig> watches = theLab.getConfig().getEnabledValueWatches();
        if (watches != null) {
            for (ValueWatchConfig currWatch : watches) {
                valueWatchActiveSet.add(currWatch.getAddress() * 1000 + currWatch.getSubchannel());
            }
        }
    }

    /**
    * All operations necessary to create a new workspace
    * 
    * @return the created workspace info
    */
    public JLabWorkspace createNewWorkspace() {
        NameDialog dlg = new NameDialog();
        dlg.pack();
        Ordering.centerDlgInFrame(GlobalsLocator.getMainFrame(), dlg);
        dlg.setVisible(true);
        JLabWorkspace createdWorkspace = null;
        if (dlg.isOKPressed()) {
            syncCurrentWorkspace();
            selectedWorkspace = createWorkspace(dlg.getName());
            restoreCurrentWorkspace();
        }
        return createdWorkspace;
    }

    private JLabWorkspace createWorkspace(String name) {
        JLabWorkspace createdWorkspace = new JLabWorkspace(name);
        JCheckBoxMenuItem newWorkspaceItem = new JCheckBoxMenuItem(new SelectWorkspaceAction(createdWorkspace, this));
        workspaceGroup.add(newWorkspaceItem);
        menuWorkspaces.add(newWorkspaceItem);
        workspaceToMenuItem.put(createdWorkspace, newWorkspaceItem);
        workspaces.add(createdWorkspace);
        newWorkspaceItem.setSelected(true);
        return createdWorkspace;
    }

    /**
    * all operations necessary to delete the selected workapace
    */
    public void deleteSelectedWorkspace() {
        String workspaceName = selectedWorkspace.getName();
        if (DEFAULT_WORKSPACE_NAME.equals(workspaceName)) {
            JOptionPane.showMessageDialog(GlobalsLocator.getMainFrame(), GlobalsLocator.translate("delete-default-workspace-warning"));
            return;
        }
        int approve = JOptionPane.showConfirmDialog(GlobalsLocator.getMainFrame(), GlobalsLocator.translate("delete-workspace-warning"), GlobalsLocator.translate("delete-workspace-title"), JOptionPane.YES_NO_OPTION);
        if (approve != JOptionPane.OK_OPTION) return;
        JCheckBoxMenuItem item = workspaceToMenuItem.get(selectedWorkspace);
        workspaceToMenuItem.remove(selectedWorkspace);
        workspaceGroup.remove(item);
        menuWorkspaces.remove(item);
        for (int i = 0; i < workspaces.size(); ++i) {
            if (workspaces.get(i).getName().equals(workspaceName)) {
                workspaces.remove(i);
                break;
            }
        }
        selectedWorkspace = workspaces.get(0);
        workspaceToMenuItem.get(selectedWorkspace).setSelected(true);
        restoreCurrentWorkspace();
    }

    void executeCommandScript() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(GlobalsLocator.getMainFrame());
        if (result == JFileChooser.APPROVE_OPTION) {
            File fileToRead = chooser.getSelectedFile();
            CommandFileProcessor.processFile(fileToRead, theLab);
        }
    }

    /**
    * store the JLab Configuration including connectioninfo, Users UI-Modules and workspace infos.
    */
    public void storeConfig() {
        theLab.getConfig().setWorkspaces(this.getWorkspaceConfig());
        theLab.getConfig().setActiveWorkspace(selectedWorkspace.getName());
        theLab.getConfig().setValueWatchActive(valueWatchesEnabled);
        theLab.getConfig().setValueWatchCycle(valueWatcher.getWaitCycle());
        theLab.storeConfig();
    }

    /**
    * switch to another workspace
    * 
    * @param selectedWorkspace the workspace to switch to
    */
    public void selectWorkspace(JLabWorkspace selectedWorkspace) {
        syncCurrentWorkspace();
        this.selectedWorkspace = selectedWorkspace;
        workspaceToMenuItem.get(selectedWorkspace).setSelected(true);
        restoreCurrentWorkspace();
    }

    /**
    * create a new Internal Frame for the given UI-Module Info.
    * 
    * @param module the module to create the frame for
    */
    public JInternalFrame createUIModuleFrame(String windowTitle, UILabModule module, boolean useWindowCounter) {
        JInternalFrame newModuleFrame = createFrame(windowTitle, module.createLabComponent(), useWindowCounter);
        frameToModule.put(newModuleFrame, module);
        return newModuleFrame;
    }

    public JInternalFrame createUIExternalModuleFrame(String windowTitle, UIExternalModule module, boolean useWindowCounter) {
        JInternalFrame newModuleFrame = createFrame(windowTitle, module.createUIComponent(), useWindowCounter);
        frameToExternalModule.put(newModuleFrame, module);
        return newModuleFrame;
    }

    public JInternalFrame createFrame(String name, JComponent content, boolean useWindowCounter) {
        String title = name;
        if (useWindowCounter) title = name + "." + windowCounter;
        JInternalFrame newModuleFrame = new JInternalFrame(title, true, true, true, true);
        windowCounter++;
        selectedWorkspace.setWindowCount(windowCounter);
        newModuleFrame.setContentPane(content);
        desktop.add(newModuleFrame);
        newModuleFrame.pack();
        newModuleFrame.setVisible(true);
        JMenuItem jMenuItemNewWindow = new JMenuItem(new SelectModuleFrameAction(newModuleFrame, this));
        menuWindows.add(jMenuItemNewWindow);
        newModuleFrame.addInternalFrameListener(new FrameClosedListener(newModuleFrame, this));
        return newModuleFrame;
    }

    /**
    * do all actions necessary to close the internal frame. Called by the window closed listener of the internal frame
    * 
    * @param internalFrame
    */
    public void closeInternalFrame(JInternalFrame internalFrame) {
        UILabModule module = frameToModule.get(internalFrame);
        if (module != null) module.close(internalFrame.getContentPane());
        frameToModule.remove(internalFrame);
        Component windowMenuEntry = findMenuItemByTitle(internalFrame.getTitle());
        if (windowMenuEntry != null) menuWindows.remove(windowMenuEntry);
    }

    private JMenuItem findMenuItemByTitle(String name) {
        JMenuItem windowMenuEntry = null;
        for (Component currItem : menuWindows.getMenuComponents()) {
            if (currItem instanceof JMenuItem) {
                if (name.equals(((JMenuItem) currItem).getText())) {
                    windowMenuEntry = (JMenuItem) currItem;
                    break;
                }
            }
        }
        return windowMenuEntry;
    }

    /**
    * activate a module Frame. Called by the Window menu.
    * 
    * @param moduleFrame
    */
    public void selectModuleFrame(JInternalFrame moduleFrame) {
        try {
            moduleFrame.setIcon(false);
            moduleFrame.setClosed(false);
            moduleFrame.setSelected(true);
        } catch (PropertyVetoException e) {
        }
    }

    /**
    * store the Window Positions in the Workspace
    */
    private void syncCurrentWorkspace() {
        if (selectedWorkspace == null) return;
        selectedWorkspace.getWindowInfos().clear();
        selectedWorkspace.getExternalWindowInfos().clear();
        for (JInternalFrame moduleFrame : desktop.getAllFrames()) {
            int posX = moduleFrame.getX();
            int posY = moduleFrame.getY();
            Dimension dim = moduleFrame.getSize();
            int width = dim.width;
            int height = dim.height;
            UILabModule module = frameToModule.get(moduleFrame);
            if (module != null) {
                Container cont = moduleFrame.getContentPane();
                HashMap<String, String> params = module.getParametersForUIComponent(cont);
                ModuleWindowInfo newInfo = new ModuleWindowInfo(posX, posY, width, height, module, params);
                newInfo.setIconified(moduleFrame.isIcon());
                newInfo.setWindowName(moduleFrame.getTitle());
                selectedWorkspace.getWindowInfos().add(newInfo);
            } else {
                UIExternalModule externalModule = frameToExternalModule.get(moduleFrame);
                if (externalModule != null) {
                    JLabUIComponent component = (JLabUIComponent) moduleFrame.getContentPane();
                    ExternalModuleWindowInfo newInfo = new ExternalModuleWindowInfo(posX, posY, width, height, externalModule.getModuleId(), externalModule.getModelId(), component.getParameters());
                    newInfo.setIconified(moduleFrame.isIcon());
                    newInfo.setWindowName(moduleFrame.getTitle());
                    selectedWorkspace.getExternalWindowInfos().add(newInfo);
                }
            }
        }
    }

    /**
    * build up all internal frames for the current workspace (necessary after a workspace switch)
    */
    private void restoreCurrentWorkspace() {
        desktop.removeAll();
        menuWindows.removeAll();
        prepareStaticWindowEntries();
        frameToModule.clear();
        frameToExternalModule.clear();
        for (ModuleWindowInfo currInfo : selectedWorkspace.getWindowInfos()) {
            JInternalFrame moduleFrame = createUIModuleFrame(currInfo.getWindowName(), currInfo.getModule(), false);
            currInfo.getModule().setParametersForUIComponent(moduleFrame.getContentPane(), currInfo.getParameters());
            moduleFrame.setLocation(currInfo.getPosX(), currInfo.getPosY());
            Dimension dim = new Dimension(currInfo.getWidth(), currInfo.getHeight());
            moduleFrame.setSize(dim);
            try {
                moduleFrame.setIcon(currInfo.isIconified());
            } catch (PropertyVetoException e) {
            }
        }
        for (ExternalModuleWindowInfo currInfo : selectedWorkspace.getExternalWindowInfos()) {
            ExternalModel model = theLab.getExternalModelByIdentifier(currInfo.getModelIdentifier());
            UIExternalModule module = uiExternalModules.get(currInfo.getModuleIdentifier());
            if (model != null && module != null) {
                JInternalFrame externalModuleFrame = createUIExternalModuleFrame(currInfo.getWindowName(), module, false);
                externalModuleFrame.setLocation(currInfo.getPosX(), currInfo.getPosY());
                Dimension dim = new Dimension(currInfo.getWidth(), currInfo.getHeight());
                externalModuleFrame.setSize(dim);
                try {
                    externalModuleFrame.setIcon(currInfo.isIconified());
                } catch (PropertyVetoException e) {
                }
            }
        }
        this.windowCounter = selectedWorkspace.getWindowCount();
        desktop.revalidate();
        desktop.repaint();
    }

    private void prepareStaticWindowEntries() {
        menuWindows.add(new RenameFrameAction(this));
        menuWindows.addSeparator();
    }

    /**
    * get the info about all open Frames for later storage as config file.
    * 
    * @return
    */
    public ArrayList<WorkspaceConfig> getWorkspaceConfig() {
        syncCurrentWorkspace();
        ArrayList<WorkspaceConfig> workspaceDatas = new ArrayList<WorkspaceConfig>();
        for (JLabWorkspace currWorkspace : this.workspaces) {
            WorkspaceConfig newWorkspaceData = new WorkspaceConfig();
            newWorkspaceData.setName(currWorkspace.getName());
            ArrayList<FrameConfig> frames = new ArrayList<FrameConfig>();
            ArrayList<ExternalFrameConfig> externalFrames = new ArrayList<ExternalFrameConfig>();
            newWorkspaceData.setFrames(frames);
            newWorkspaceData.setExternalFrames(externalFrames);
            workspaceDatas.add(newWorkspaceData);
            newWorkspaceData.setWindowCount(currWorkspace.getWindowCount());
            for (ModuleWindowInfo currInfo : currWorkspace.getWindowInfos()) {
                FrameConfig newFrameData = new FrameConfig();
                newFrameData.setHeight(currInfo.getHeight());
                newFrameData.setWidth(currInfo.getWidth());
                newFrameData.setPosX(currInfo.getPosX());
                newFrameData.setPosY(currInfo.getPosY());
                newFrameData.setModuleClassName(currInfo.getModule().getClass().getName());
                newFrameData.setModuleId(currInfo.getModule().getId());
                newFrameData.setFrameName(currInfo.getWindowName());
                newFrameData.setParametersAsHashMap(currInfo.getParameters());
                if (currInfo.getModule().getBoard() != null) {
                    newFrameData.setAddress(currInfo.getModule().getBoard().getAddress());
                    newFrameData.setCommChannel(currInfo.getModule().getBoard().getCommChannel().getChannelName());
                } else {
                    newFrameData.setAddress(-1);
                    newFrameData.setCommChannel("-");
                }
                frames.add(newFrameData);
            }
            for (ExternalModuleWindowInfo currInfo : currWorkspace.getExternalWindowInfos()) {
                ExternalFrameConfig newFrameData = new ExternalFrameConfig();
                newFrameData.setHeight(currInfo.getHeight());
                newFrameData.setWidth(currInfo.getWidth());
                newFrameData.setPosX(currInfo.getPosX());
                newFrameData.setPosY(currInfo.getPosY());
                newFrameData.setUiModuleIdentifier(currInfo.getModuleIdentifier());
                newFrameData.setModelIdentifier(currInfo.getModelIdentifier());
                newFrameData.setFrameName(currInfo.getWindowName());
                newFrameData.setParametersAsMap(currInfo.getParameters());
                externalFrames.add(newFrameData);
            }
        }
        return workspaceDatas;
    }

    /**
    * initialize the UI by the given config.
    */
    public void initByConfig() {
        if (theLab.getConfig().getWorkspaces() == null) {
            return;
        }
        this.valueWatcher.setWaitCycle(theLab.getConfig().getValueWatchCycle());
        this.valueWatchesEnabled = theLab.getConfig().isValueWatchActive();
        this.valueWatchesAsynchronous = theLab.getConfig().isValueWatchAsynchronous();
        jCheckBoxSendValueWatch.setSelected(valueWatchesEnabled);
        jCheckBoxValueWatchAsynchonously.setSelected(valueWatchesAsynchronous);
        for (Component currMenuComponent : menuFile.getMenuComponents()) {
            if (currMenuComponent instanceof JCheckBoxMenuItem) {
                JCheckBoxMenuItem item = (JCheckBoxMenuItem) currMenuComponent;
                if (item.getAction() instanceof SelectValueWatchIntervallAction) {
                    SelectValueWatchIntervallAction action = (SelectValueWatchIntervallAction) item.getAction();
                    if (action.getIntervall() == theLab.getConfig().getValueWatchCycle()) {
                        item.setSelected(true);
                        break;
                    }
                }
            }
        }
        workspaces.clear();
        frameToModule.clear();
        workspaceToMenuItem.clear();
        removeWorkspacesFromMenu();
        desktop.removeAll();
        menuWindows.removeAll();
        for (WorkspaceConfig currWorkspace : theLab.getConfig().getWorkspaces()) {
            JLabWorkspace newWorkspace = createWorkspace(currWorkspace.getName());
            Set<ModuleWindowInfo> infos = new HashSet<ModuleWindowInfo>();
            Set<ExternalModuleWindowInfo> externalinfos = new HashSet<ExternalModuleWindowInfo>();
            newWorkspace.setWindowInfos(infos);
            newWorkspace.setExternalWindowInfos(externalinfos);
            if (currWorkspace.getFrames() != null) {
                for (FrameConfig currFrame : currWorkspace.getFrames()) {
                    UILabModule module = getModuleByIDAndClassnameAndAddressAndChannel(currFrame.getModuleId(), currFrame.getModuleClassName(), currFrame.getAddress(), currFrame.getCommChannel());
                    if (module != null) {
                        ModuleWindowInfo newInfo = new ModuleWindowInfo(currFrame.getPosX(), currFrame.getPosY(), currFrame.getWidth(), currFrame.getHeight(), module, currFrame.getParametersAsHashMap());
                        newInfo.setWindowName(currFrame.getFrameName());
                        newWorkspace.getWindowInfos().add(newInfo);
                    }
                }
            }
            if (currWorkspace.getExternalFrames() != null) {
                for (ExternalFrameConfig currFrame : currWorkspace.getExternalFrames()) {
                    ExternalModuleWindowInfo newInfo = new ExternalModuleWindowInfo(currFrame.getPosX(), currFrame.getPosY(), currFrame.getWidth(), currFrame.getHeight(), currFrame.getUiModuleIdentifier(), currFrame.getModelIdentifier(), currFrame.getParametersAsHashMap());
                    newInfo.setWindowName(currFrame.getFrameName());
                    newWorkspace.getExternalWindowInfos().add(newInfo);
                }
            }
        }
        if (theLab.getConfig().getActiveWorkspace() != null) {
            for (JLabWorkspace newWorkspace : workspaces) {
                if (newWorkspace.getName().equals(theLab.getConfig().getActiveWorkspace())) {
                    selectWorkspace(newWorkspace);
                    break;
                }
            }
        } else {
            selectWorkspace(workspaces.get(0));
        }
        this.buildValueWatchListFromConfig();
        boolean value = theLab.getConfig().getConnParameter().isEnableCheckSum();
        jCheckBoxMenuItemToggleCheckSum.setSelected(value);
        theLab.setEnableChecksum(value);
        value = theLab.getConfig().getConnParameter().isEnableCommandConfirmation();
        jCheckBoxMenuItemToggleCommandConfirmation.setSelected(value);
        theLab.setEnableCommandConfirmation(value);
        value = theLab.getConfig().getConnParameter().isEnableCommandProtocol();
        jCheckBoxMenuItemToggleCommandProtocol.setSelected(value);
        theLab.setEnableCommandProtocol(value);
    }

    private void removeWorkspacesFromMenu() {
        menuWorkspaces.removeAll();
        prepareStaticWorkspaceMenuEntries();
    }

    private void prepareStaticWorkspaceMenuEntries() {
        menuWorkspaces.add(new NewWorkspaceAction(this));
        menuWorkspaces.add(new DeleteWorkspaceAction(this));
        menuWorkspaces.addSeparator();
    }

    /**
    * get a module given by class, identifier and address for later creation of a Frame.
    * 
    * @param identifier identifier of the module
    * @param classname classname of the module
    * @param address c't-Lab address its installed to.
    * @return
    */
    private UILabModule getModuleByIDAndClassnameAndAddressAndChannel(String identifier, String classname, int address, String channel) {
        UILabModule foundModule = null;
        for (UILabModule currModule : this.uiModules) {
            if (currModule.getBoard() == null) {
                if (currModule.getClass().getName().equals(classname) && address == -1 && currModule.getId().equals(identifier)) {
                    foundModule = currModule;
                    break;
                }
            } else {
                if (currModule.getClass().getName().equals(classname) && currModule.getBoard().getAddress() == address && currModule.getBoard().getCommChannel().getChannelName().equals(channel) && currModule.getId().equals(identifier)) {
                    foundModule = currModule;
                    break;
                }
            }
        }
        return foundModule;
    }

    /**
    * show the about dialog.
    */
    public void showAbout() {
        AboutDialog dlg = new AboutDialog(GlobalsLocator.getMainFrame(), GlobalsLocator.translate("main-manu-item-about"), true);
        dlg.pack();
        Ordering.centerDlgInFrame(GlobalsLocator.getMainFrame(), dlg);
        dlg.setVisible(true);
    }

    /**
    * bring up the connection dialog
    */
    public void configureConnection() {
        ConnectionSelectionDialog dlg = new ConnectionSelectionDialog(GlobalsLocator.getMainFrame(), GlobalsLocator.translate("connection-dialog-title"));
        dlg.setSize(540, 280);
        Ordering.centerDlgInFrame(GlobalsLocator.getMainFrame(), dlg);
        dlg.setConnectionConfig(theLab.getConfig().getConnParameter());
        dlg.setVisible(true);
        if (dlg.isOkPressed()) {
            ConnectionConfig newConfig = dlg.getConnectionConfig();
            theLab.getConfig().setConnParameter(newConfig);
            this.storeConfig();
            JOptionPane.showMessageDialog(GlobalsLocator.getMainFrame(), GlobalsLocator.translate("changes-active-after-restart-msg"));
        }
    }

    public void toggleValueWatch() {
        jCheckBoxSendValueWatch.setSelected(!valueWatchesEnabled);
        enableValueWatches(!valueWatchesEnabled);
    }

    public void toggleValueWatchAsync() {
        valueWatchesAsynchronous = !valueWatchesAsynchronous;
        jCheckBoxValueWatchAsynchonously.setSelected(valueWatchesAsynchronous);
        theLab.getConfig().setValueWatchAsynchronous(valueWatchesAsynchronous);
    }

    public void sendValueWatches(int delta, Set<ValueWatch> watches) {
        long necessaryTime = System.currentTimeMillis();
        if (!valueWatchesEnabled) return;
        for (ValueWatch watch : watches) {
            long duration = System.currentTimeMillis();
            if (theLab.getConfig().isValueWatchAsynchronous()) watch.getBoard().queryValueAsynchronously(watch.getSubchannel()); else watch.getBoard().queryStringValue(watch.getSubchannel());
            duration = System.currentTimeMillis() - duration;
            long sleepDelta = delta - duration;
            if (sleepDelta > 0) {
                try {
                    Thread.sleep(sleepDelta);
                } catch (InterruptedException e) {
                }
            }
        }
        mainStatusBar.setValueWatchProcessTime(System.currentTimeMillis() - necessaryTime);
        return;
    }

    public Set<ValueWatch> getAllValueWatches() {
        Set<ValueWatch> watches = new HashSet<ValueWatch>();
        boolean valueWatchConfigPresent = theLab.getConfig().getEnabledValueWatches() != null;
        if (!valueWatchesEnabled) return watches;
        for (UILabModule currModule : frameToModule.values()) {
            List<ValueWatch> moduleWatches = currModule.getValueWatches();
            if (moduleWatches != null) {
                for (ValueWatch currWatch : moduleWatches) {
                    watches.add(currWatch);
                }
            }
        }
        return watches;
    }

    public void calcMemory() {
        Runtime rt = Runtime.getRuntime();
        long used = (rt.totalMemory() - rt.freeMemory()) / 1024;
        long maxFree = (rt.maxMemory() - (rt.totalMemory() - rt.freeMemory())) / 1024;
        menuMem.setText("U " + memoryFormat.format(used) + " F " + memoryFormat.format(maxFree));
    }

    public void configureValueWatches() {
        ValueWatchConfiguratorDialog dlg = new ValueWatchConfiguratorDialog(GlobalsLocator.translate("configure-value-watches-dialog-title"), theLab);
        dlg.pack();
        Ordering.centerDlgInFrame(GlobalsLocator.getMainFrame(), dlg);
        dlg.setCheckBoxesForConfig(theLab.getConfig().getEnabledValueWatches());
        dlg.setVisible(true);
        if (dlg.isOKPressed()) {
            theLab.getConfig().setEnabledValueWatches(dlg.getEnabledWatches());
            this.buildValueWatchListFromConfig();
        }
    }

    public void renameCurrentFrame() {
        JInternalFrame selectedFrame = desktop.getSelectedFrame();
        if (selectedFrame == null) return;
        NameDialog dlg = new NameDialog(GlobalsLocator.getMainFrame(), GlobalsLocator.translate("dlg-enter-windowname"), true);
        dlg.pack();
        Ordering.centerDlgInFrame(GlobalsLocator.getMainFrame(), dlg);
        dlg.setVisible(true);
        if (dlg.isOKPressed()) {
            String name = dlg.getName();
            JMenuItem windowMenuEntry = findMenuItemByTitle(selectedFrame.getTitle());
            selectedFrame.setTitle(name);
            windowMenuEntry.setText(name);
        }
    }

    public void startScript(String location) {
        if (scriptExecuter != null) scriptExecuter.startScript(location);
    }

    public void scriptStopped(ScriptPanel scriptPanel) {
        scriptPanel.setStopped();
    }

    public void newScript() {
        JFileChooser ch = new JFileChooser();
        int response = ch.showOpenDialog(GlobalsLocator.getMainFrame());
        if (response != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File selectedFile = ch.getSelectedFile();
        ScriptConfig newScript = new ScriptConfig();
        newScript.setLocation(selectedFile.getAbsolutePath());
        if (theLab.getConfig().getScripts() == null) {
            theLab.getConfig().setScripts(new ArrayList<ScriptConfig>());
        }
        theLab.getConfig().getScripts().add(newScript);
        if (theLab.getConfig().getScripts().size() > 10) {
            theLab.getConfig().getScripts().remove(0);
        }
        setupScriptMenu();
    }

    public void toggleCommandConfirmation() {
        boolean newValue = !theLab.isEnabledCommandConfirmation();
        jCheckBoxMenuItemToggleCommandConfirmation.setSelected(newValue);
        theLab.setEnableCommandConfirmation(newValue);
        theLab.getConfig().getConnParameter().setEnableCommandConfirmation(newValue);
    }

    public void toggleCommandprotocol() {
        boolean newValue = !theLab.isEnabledCommandDebug();
        jCheckBoxMenuItemToggleCommandProtocol.setSelected(newValue);
        theLab.setEnableCommandProtocol(newValue);
        theLab.getConfig().getConnParameter().setEnableCommandProtocol(newValue);
    }

    public void toggleCommandChecksum() {
        boolean newValue = !theLab.isEnabledChecksum();
        jCheckBoxMenuItemToggleCheckSum.setSelected(newValue);
        theLab.setEnableChecksum(newValue);
        theLab.getConfig().getConnParameter().setEnableCheckSum(newValue);
    }
}

class FrameClosedListener extends InternalFrameAdapter {

    JInternalFrame moduleFrame = null;

    JLabMainUI mainFrame = null;

    public FrameClosedListener(JInternalFrame moduleFrame, JLabMainUI mainFrame) {
        this.moduleFrame = moduleFrame;
        this.mainFrame = mainFrame;
    }

    public void internalFrameClosed(InternalFrameEvent e) {
        mainFrame.closeInternalFrame(moduleFrame);
    }
}

@SuppressWarnings("serial")
class StartUIModuleAction extends AbstractAction {

    JLabMainUI mainFrame = null;

    UILabModule module = null;

    String frameTitle = null;

    public StartUIModuleAction(String moduleId, JLabMainUI mainFrame, UILabModule module) {
        super(module.getId());
        this.mainFrame = mainFrame;
        this.module = module;
        if (module.getBoard() == null) frameTitle = moduleId + "-" + module.getId(); else frameTitle = module.getBoard().getBoardInstanceIdentifier() + "_" + module.getId();
    }

    public void actionPerformed(ActionEvent e) {
        mainFrame.createUIModuleFrame(frameTitle, module, true);
    }
}

@SuppressWarnings("serial")
class StartUIExternalModuleAction extends AbstractAction {

    JLabMainUI mainFrame = null;

    UIExternalModule module = null;

    public StartUIExternalModuleAction(JLabMainUI mainFrame, UIExternalModule module) {
        super(module.getModuleId());
        this.mainFrame = mainFrame;
        this.module = module;
    }

    public void actionPerformed(ActionEvent e) {
        mainFrame.createUIExternalModuleFrame(module.getModuleId(), module, true);
    }
}

@SuppressWarnings("serial")
class SelectWorkspaceAction extends AbstractAction {

    JLabWorkspace workspace = null;

    JLabMainUI mainFrame = null;

    public SelectWorkspaceAction(JLabWorkspace workspace, JLabMainUI mainFrame) {
        super(workspace.getName());
        this.workspace = workspace;
        this.mainFrame = mainFrame;
    }

    public void actionPerformed(ActionEvent e) {
        mainFrame.selectWorkspace(workspace);
    }
}

@SuppressWarnings("serial")
class DeleteWorkspaceAction extends AbstractAction {

    JLabMainUI mainFrame = null;

    public DeleteWorkspaceAction(JLabMainUI mainFrame) {
        super(GlobalsLocator.translate("main-menu-item-delete-workspace"));
        this.mainFrame = mainFrame;
    }

    public void actionPerformed(ActionEvent e) {
        mainFrame.deleteSelectedWorkspace();
    }
}

@SuppressWarnings("serial")
class NewWorkspaceAction extends AbstractAction {

    JLabMainUI mainFrame = null;

    public NewWorkspaceAction(JLabMainUI mainFrame) {
        super(GlobalsLocator.translate("main-menu-item-new-workspace"));
        this.mainFrame = mainFrame;
    }

    public void actionPerformed(ActionEvent e) {
        mainFrame.createNewWorkspace();
    }
}

@SuppressWarnings("serial")
class SelectModuleFrameAction extends AbstractAction {

    JInternalFrame moduleFrame = null;

    JLabMainUI mainFrame = null;

    public SelectModuleFrameAction(JInternalFrame moduleFrame, JLabMainUI mainFrame) {
        super(moduleFrame.getTitle());
        this.moduleFrame = moduleFrame;
        this.mainFrame = mainFrame;
    }

    public void actionPerformed(ActionEvent e) {
        mainFrame.selectModuleFrame(moduleFrame);
    }
}

@SuppressWarnings("serial")
class StoreConfigAction extends AbstractAction {

    JLabMainUI mainFrame = null;

    public StoreConfigAction(JLabMainUI mainFrame) {
        super(GlobalsLocator.translate("main-menu-item-store-config"));
        this.mainFrame = mainFrame;
    }

    public void actionPerformed(ActionEvent e) {
        mainFrame.storeConfig();
    }
}

@SuppressWarnings("serial")
class CloseJLabAction extends AbstractAction {

    JLabMainUI mainFrame = null;

    public CloseJLabAction(JLabMainUI mainFrame) {
        super(GlobalsLocator.translate("main-menu-item-close-jlab"));
        this.mainFrame = mainFrame;
    }

    public void actionPerformed(ActionEvent e) {
        mainFrame.stopJLab();
    }
}

@SuppressWarnings("serial")
class ConfigureConnectionAction extends AbstractAction {

    JLabMainUI mainFrame = null;

    public ConfigureConnectionAction(JLabMainUI mainFrame) {
        super(GlobalsLocator.translate("main-menu-item-configure-configuration"));
        this.mainFrame = mainFrame;
    }

    public void actionPerformed(ActionEvent e) {
        mainFrame.configureConnection();
    }
}

@SuppressWarnings("serial")
class ToggleValueWatchAction extends AbstractAction {

    JLabMainUI mainFrame = null;

    public ToggleValueWatchAction(JLabMainUI mainFrame) {
        super(GlobalsLocator.translate("main-menu-item-send-value-watches"));
        this.mainFrame = mainFrame;
    }

    public void actionPerformed(ActionEvent e) {
        mainFrame.toggleValueWatch();
    }
}

@SuppressWarnings("serial")
class ToggleValueWatchSynchonouslyAction extends AbstractAction {

    JLabMainUI mainFrame = null;

    public ToggleValueWatchSynchonouslyAction(JLabMainUI mainFrame) {
        super(GlobalsLocator.translate("main-menu-item-value-watches-asynchonously"));
        this.mainFrame = mainFrame;
    }

    public void actionPerformed(ActionEvent e) {
        mainFrame.toggleValueWatchAsync();
    }
}

@SuppressWarnings("serial")
class AboutAction extends AbstractAction {

    JLabMainUI mainFrame = null;

    public AboutAction(JLabMainUI mainFrame) {
        super(GlobalsLocator.translate("main-menu-item-about"));
        this.mainFrame = mainFrame;
    }

    public void actionPerformed(ActionEvent e) {
        mainFrame.showAbout();
    }
}

@SuppressWarnings("serial")
class ConfigureValueWatchesAction extends AbstractAction {

    JLabMainUI mainFrame = null;

    public ConfigureValueWatchesAction(JLabMainUI mainFrame) {
        super(GlobalsLocator.translate("main-menu-item-configure-value-watches"));
        this.mainFrame = mainFrame;
    }

    public void actionPerformed(ActionEvent e) {
        mainFrame.configureValueWatches();
    }
}

@SuppressWarnings("serial")
class SelectValueWatchIntervallAction extends AbstractAction {

    ValueWatchThread valueWatchThread = null;

    int intervall = 100;

    JCheckBoxMenuItem item = null;

    public SelectValueWatchIntervallAction(ValueWatchThread valueWatchThread, int intervall, JCheckBoxMenuItem item) {
        super(intervall + " ms");
        this.valueWatchThread = valueWatchThread;
        this.intervall = intervall;
        this.item = item;
    }

    public void actionPerformed(ActionEvent e) {
        valueWatchThread.setWaitCycle(intervall);
        item.setSelected(true);
    }

    public int getIntervall() {
        return intervall;
    }
}

@SuppressWarnings("serial")
class StartScriptAction extends AbstractAction {

    String location = null;

    JLabMainUI mainFrame = null;

    public void actionPerformed(ActionEvent e) {
        mainFrame.startScript(location);
    }

    public StartScriptAction(String location, JLabMainUI mainFrame) {
        super(GlobalsLocator.translate("main-menu-item-start-script") + location);
        this.location = location;
        this.mainFrame = mainFrame;
    }
}

@SuppressWarnings("serial")
class NewScriptAction extends AbstractAction {

    JLabMainUI mainFrame = null;

    public void actionPerformed(ActionEvent e) {
        mainFrame.newScript();
    }

    public NewScriptAction(JLabMainUI mainFrame) {
        super(GlobalsLocator.translate("main-menu-item-new-script"));
        this.mainFrame = mainFrame;
    }
}

@SuppressWarnings("serial")
class ExecuteCommandScriptAction extends AbstractAction {

    JLabMainUI mainFrame = null;

    public void actionPerformed(ActionEvent e) {
        mainFrame.executeCommandScript();
    }

    public ExecuteCommandScriptAction(JLabMainUI mainFrame) {
        super(GlobalsLocator.translate("main-menu-item-execute-command-script"));
        this.mainFrame = mainFrame;
    }
}

@SuppressWarnings("serial")
class RenameFrameAction extends AbstractAction {

    JLabMainUI mainFrame = null;

    public void actionPerformed(ActionEvent e) {
        mainFrame.renameCurrentFrame();
    }

    public RenameFrameAction(JLabMainUI mainFrame) {
        super(GlobalsLocator.translate("main-menu-item-rename-frame"));
        this.mainFrame = mainFrame;
    }
}

@SuppressWarnings("serial")
class ToggleCommandConfirmationAction extends AbstractAction {

    JLabMainUI mainFrame = null;

    public void actionPerformed(ActionEvent e) {
        mainFrame.toggleCommandConfirmation();
    }

    public ToggleCommandConfirmationAction(JLabMainUI mainFrame) {
        super(GlobalsLocator.translate("main-menu-item-toggle-command-confirmation"));
        this.mainFrame = mainFrame;
    }
}

@SuppressWarnings("serial")
class ToggleCommandDebugAction extends AbstractAction {

    JLabMainUI mainFrame = null;

    public void actionPerformed(ActionEvent e) {
        mainFrame.toggleCommandprotocol();
    }

    public ToggleCommandDebugAction(JLabMainUI mainFrame) {
        super(GlobalsLocator.translate("main-menu-item-toggle-command-debug"));
        this.mainFrame = mainFrame;
    }
}

@SuppressWarnings("serial")
class ToggleChecksumAction extends AbstractAction {

    JLabMainUI mainFrame = null;

    public void actionPerformed(ActionEvent e) {
        mainFrame.toggleCommandChecksum();
    }

    public ToggleChecksumAction(JLabMainUI mainFrame) {
        super(GlobalsLocator.translate("main-menu-item-toggle-checksum"));
        this.mainFrame = mainFrame;
    }
}

@SuppressWarnings("serial")
class GarbageCollectAction extends AbstractAction {

    public GarbageCollectAction() {
        super(GlobalsLocator.translate("main-menu-item-cleanup-mem"));
    }

    public void actionPerformed(ActionEvent e) {
        System.gc();
    }
}

@SuppressWarnings("serial")
class ShowConsoleAction extends AbstractAction {

    JFrame console = null;

    public ShowConsoleAction(JFrame console) {
        super(GlobalsLocator.translate("main-menu-item-show-console"));
        this.console = console;
    }

    public void actionPerformed(ActionEvent e) {
        console.setVisible(true);
    }
}

class ValueWatchThread extends Thread {

    JLabMainUI mainUI;

    int prio = 0;

    int waitCycle = 500;

    volatile boolean continueThread = true;

    public ValueWatchThread(JLabMainUI mainUI) {
        super("Value Watcher Thread");
        this.mainUI = mainUI;
    }

    public void run() {
        while (continueThread) {
            Set<ValueWatch> watches = mainUI.getAllValueWatches();
            int watchCount = watches.size();
            long processStart = System.currentTimeMillis();
            if (watchCount != 0) {
                int delta = waitCycle / watchCount;
                mainUI.sendValueWatches(delta, watches);
            }
            long processTime = System.currentTimeMillis() - processStart;
            if (processTime < waitCycle) {
                try {
                    Thread.sleep(waitCycle - processTime);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public boolean isContinueThread() {
        return continueThread;
    }

    public void setContinueThread(boolean continueThread) {
        this.continueThread = continueThread;
    }

    public int getWaitCycle() {
        return waitCycle;
    }

    public void setWaitCycle(int waitCycle) {
        this.waitCycle = waitCycle;
    }
}
