package ti.plato.logcontrol.views;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Vector;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import ti.mcore.Environment;
import ti.plato.logcontrol.ChangeListener;
import ti.plato.logcontrol.IPageModel;
import ti.plato.logcontrol.LogControlPlugin;
import ti.plato.logcontrol.LogControlStatus;
import ti.plato.logcontrol.PageController;
import ti.plato.logcontrol.PageModelItem;
import ti.plato.logcontrol.SelectProfileAction;
import ti.plato.logcontrol.TabControl;
import ti.plato.logcontrol.TableControl;
import ti.plato.logcontrol.TreeControl;
import ti.plato.logcontrol.constants.Constants;
import ti.plato.ui.images.util.ImagesUtil;
import ti.plato.ui.u.PluginUtil;
import ti.plato.ui.views.properties.PropertiesAccess;

public class LogControlView extends ViewPart {

    public static final String ID = "ti.plato.logcontrol.views.LogControlView";

    private final String ZIP_EXT = ".lcp";

    private String lastFileDialogPath = null;

    private Composite top = null;

    private CTabFolder tabFolder = null;

    private Map actionMap = new HashMap();

    private Shell parentShell = null;

    private Action applyAction;

    private Action autoApplyAction;

    private Action createProfileAction;

    private Action selectProfileAction;

    private Action disableAllAction;

    private Action restoreSettingsAction;

    private String selectedPageName;

    public static LogControlView getDefault() {
        LogControlView def = null;
        int wwCount = PlatformUI.getWorkbench().getWorkbenchWindowCount();
        for (int ww = 0; ww < wwCount; ++ww) {
            IWorkbenchWindow wWindow = PlatformUI.getWorkbench().getWorkbenchWindows()[ww];
            int pCount = wWindow.getPages().length;
            for (int i = 0; i < pCount; ++i) {
                IWorkbenchPage page = wWindow.getPages()[i];
                IViewReference viewReferences[] = page.getViewReferences();
                int vCount = viewReferences.length;
                for (int j = 0; j < vCount; ++j) {
                    if (viewReferences[j].getId().equals(ID)) {
                        IViewPart vp = viewReferences[j].getView(false);
                        if (vp instanceof LogControlView) {
                            def = (LogControlView) vp;
                            break;
                        }
                    }
                }
                if (def != null) break;
            }
            if (def != null) break;
        }
        return def;
    }

    public void createPartControl(Composite parent) {
        if (parent == null) {
            LogControlPlugin.getWorkspaceSaveContainer().Clear();
            return;
        }
        parentShell = parent.getShell();
        top = new Composite(parent, SWT.NONE);
        top.setLayout(new FillLayout());
        LogControlStatus.getLogControlStatus().addChangeListener(new LogControlStatus.ChangeListener() {

            public void update() {
                updateStatusLine();
            }
        });
        makeActions();
        contributeToActionBars();
        createPages();
        updateSelectProfileAction();
        updateDisableAllAction();
    }

    public void dispose() {
        int pageCount = PageController.getPages() != null ? PageController.getPages().length : 0;
        for (int i = 0; i < pageCount; ++i) {
            PageController pageController = PageController.getPages()[i];
            if (pageController.getTabControl() != null) {
                pageController.getTabControl().deinit(pageController);
            }
        }
    }

    /**
   */
    public void setFocus() {
        tabFolder.setFocus();
    }

    /**
   * This method initializes composite	
   *
   */
    private void createPages() {
        int pageCount = PageController.getPages() != null ? PageController.getPages().length : 0;
        if (pageCount > 0) {
            tabFolder = new CTabFolder(top, SWT.FLAT | SWT.BOTTOM);
            tabFolder.setBounds(new org.eclipse.swt.graphics.Rectangle(0, 0, 291, 166));
            tabFolder.setSimple(false);
            tabFolder.addSelectionListener(new SelectionListener() {

                public void widgetSelected(SelectionEvent e) {
                    LogControlPlugin.getWorkspaceSaveContainer().setTabIndex(tabFolder.getSelectionIndex());
                    CTabItem item = tabFolder.getSelection();
                    selectedPageName = item.getText();
                    PageController pageController = PageController.getPageController(selectedPageName);
                    updateApplyAction(pageController.hasPendingChanges(), null);
                    updateDisableAllAction();
                    hookContextMenu(pageController.getTabControl() != null ? pageController.getTabControl() : null, pageController);
                }

                public void widgetDefaultSelected(SelectionEvent e) {
                }
            });
            PageController[] pageControllers = sortByStackingOrder();
            for (int i = 0; i < pageControllers.length; ++i) {
                PageController pageController = pageControllers[i];
                TabControl tabControl = null;
                if (pageController.isTreeControl()) {
                    tabControl = new TreeControl(tabFolder, pageController);
                } else {
                    tabControl = new TableControl(tabFolder, pageController);
                }
                final String pageName = pageController.getName();
                tabControl.addChangeListener(new ChangeListener() {

                    public void update(final Object element, final String[] properties) {
                        if (autoApplyAction.isChecked()) {
                            runAction(getGlobalAction(pageName, Constants.actionApplyId));
                        } else {
                            applyAction.setEnabled(true);
                        }
                    }
                });
                GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
                if (pageController.isTreeControl()) tabControl.getControl().setLayoutData(gridData);
                CTabItem tabItem;
                tabItem = new CTabItem(tabFolder, SWT.NONE);
                tabItem.setControl(tabControl.getControl());
                tabItem.setText(pageName);
                addAction(pageController.getName(), pageController.getApplyAction());
                if (pageController.getInput() != null) pageController.getInput().clearDirtyItems();
                pageController.setTabControl(tabControl);
                if (pageController.getStackingOrder() == Constants.DEFAULT_TAB) tabFolder.setSelection(tabItem);
            }
            tabFolder.setSelection(tabFolder.getItem(LogControlPlugin.getWorkspaceSaveContainer().getTabIndex()));
            selectedPageName = tabFolder.getItem(LogControlPlugin.getWorkspaceSaveContainer().getTabIndex()).getText();
            PageController pageController = PageController.getPageController(selectedPageName);
            hookContextMenu(pageController.getTabControl() != null ? pageController.getTabControl() : null, pageController);
        }
    }

    private void addAction(final String pageName, Action newAction) {
        Map map = (Map) actionMap.get(pageName);
        if (map != null) {
            CompositeAction compositeAction = (CompositeAction) map.get(newAction.getId());
            if (compositeAction == null) {
                compositeAction = new CompositeAction();
                compositeAction.add(newAction);
                map.put(newAction.getId(), compositeAction);
            } else {
                compositeAction.add(newAction);
            }
        } else {
            map = new HashMap();
            CompositeAction compositeAction = new CompositeAction();
            compositeAction.add(newAction);
            map.put(newAction.getId(), compositeAction);
            actionMap.put(pageName, map);
        }
    }

    private void contributeToActionBars() {
        if (getViewSite() != null) {
            IActionBars bars = getViewSite().getActionBars();
            fillLocalPullDown(bars.getMenuManager());
            fillLocalToolBar(bars.getToolBarManager());
        }
    }

    private PageController[] sortByStackingOrder() {
        Vector<PageController> v = new Vector<PageController>();
        PageController[] pages = PageController.getPages();
        int[] array = new int[pages.length];
        for (int i = 0; i < pages.length; ++i) {
            PageController pageController = pages[i];
            int stackingOrder = pageController.getStackingOrder();
            array[i] = stackingOrder;
        }
        java.util.Arrays.sort(array);
        for (int i = 0; i < array.length; i++) {
            int temp = array[i];
            for (int j = 0; j < pages.length; j++) {
                PageController tempP = pages[j];
                if (tempP.getStackingOrder() == temp && (!v.contains(tempP))) {
                    v.add(tempP);
                    break;
                }
            }
        }
        return v.toArray(new PageController[v.size()]);
    }

    private void hookContextMenu(TabControl tabControl, final PageController pageController) {
        if (tabControl != null) {
            MenuManager menuMgr = new MenuManager("#PopupMenu");
            menuMgr.setRemoveAllWhenShown(true);
            menuMgr.addMenuListener(new IMenuListener() {

                public void menuAboutToShow(IMenuManager manager) {
                    LogControlView.this.fillContextMenu(manager, pageController);
                }
            });
            if (tabControl.getClass() == TreeControl.class) {
                StructuredViewer viewer = ((TreeControl) tabControl).getViewer();
                Menu menu = menuMgr.createContextMenu(viewer.getControl());
                viewer.getControl().setMenu(menu);
                getSite().registerContextMenu(menuMgr, viewer);
            }
            if (tabControl.getClass() == TableControl.class) {
                Control control = ((TableControl) tabControl).getContextMenuControl();
                Menu menu = menuMgr.createContextMenu(control);
                control.setMenu(menu);
            }
        }
    }

    private void fillLocalPullDown(IMenuManager manager) {
        manager.add(applyAction);
        manager.add(autoApplyAction);
        manager.add(new Separator());
        manager.add(restoreSettingsAction);
        manager.add(new Separator());
        manager.add(disableAllAction);
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        manager.add(createProfileAction);
        manager.add(new Separator());
        manager.add(PropertiesAccess.getActionProperties());
    }

    private void fillContextMenu(IMenuManager manager, PageController pageController) {
        manager.add(applyAction);
        manager.add(autoApplyAction);
        manager.add(new Separator());
        manager.add(restoreSettingsAction);
        manager.add(new Separator());
        manager.add(disableAllAction);
        manager.add(new Separator());
        Action[] specificActions = pageController.getContextMenuActions();
        for (int i = 0; i < specificActions.length; i++) manager.add(specificActions[i]);
        manager.add(new Separator());
        manager.add(createProfileAction);
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        manager.add(PropertiesAccess.getActionProperties());
    }

    private void fillLocalToolBar(IToolBarManager manager) {
        manager.add(applyAction);
        manager.add(new Separator());
        manager.add(disableAllAction);
        manager.add(new Separator());
        manager.add(selectProfileAction);
    }

    private void makeActions() {
        restoreSettingsAction = new Action() {

            public void run() {
            }
        };
        restoreSettingsAction.setId(Constants.actionRestoreSettingsActionId);
        restoreSettingsAction.setText("Restore Settings");
        restoreSettingsAction.setToolTipText("Enables/Disables Restoring Settings upon Next Connection");
        restoreSettingsAction.setChecked(LogControlPlugin.getWorkspaceSaveContainer().restoreSettings);
        restoreSettingsAction.addPropertyChangeListener(new IPropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {
                Object newValue = event.getNewValue();
                if (newValue instanceof Boolean) {
                    boolean checked = ((Boolean) newValue).booleanValue();
                    LogControlPlugin.getWorkspaceSaveContainer().restoreSettings = checked;
                }
            }
        });
        disableAllAction = new Action() {

            public void run() {
                PageController pageController = PageController.getPageController(selectedPageName);
                updateApplyAction(true, null);
                PageController[] pages = PageController.getPages();
                for (int i = 0; i < pages.length; i++) {
                    pages[i].preStateChanged();
                }
                disableAll(pageController, true);
                disableAllAction.setEnabled(false);
            }
        };
        disableAllAction.setId(Constants.actionDisableAllId);
        disableAllAction.setText("Disable All");
        disableAllAction.setToolTipText("Disable All Enabled Traces");
        disableAllAction.setChecked(false);
        try {
            disableAllAction.setDisabledImageDescriptor(LogControlPlugin.getImageDescriptor("icons/d_disable_all.gif"));
            disableAllAction.setImageDescriptor(LogControlPlugin.getImageDescriptor("icons/e_disable_all.gif"));
        } catch (IllegalStateException ex) {
        }
        selectProfileAction = new SelectProfileAction();
        if (getProfileNames().length == 0) selectProfileAction.setEnabled(false);
        selectProfileAction.setToolTipText("Select Profile");
        selectProfileAction.setText("Select Profile");
        selectProfileAction.setId(Constants.actionSelectProfileId);
        createProfileAction = new Action() {

            public void run() {
                new CreateProfileDialog().doModal();
            }
        };
        createProfileAction.setText("Save Profile...");
        applyAction = new Action() {

            public void run() {
                if (selectedPageName != null) {
                    Action action = getGlobalAction(selectedPageName, Constants.actionApplyId);
                    PageController current = PageController.getPageController(selectedPageName);
                    runAction(action);
                    current.setPendingChanges(false);
                    updateApplyAction(current.hasPendingChanges(), null);
                    updateDisableAllAction();
                }
            }
        };
        applyAction.setText("Apply");
        applyAction.setToolTipText("Apply Changes to Target");
        try {
            applyAction.setDisabledImageDescriptor(ImagesUtil.getImageDescriptor("d_apply"));
            applyAction.setImageDescriptor(ImagesUtil.getImageDescriptor("e_apply"));
        } catch (IllegalStateException ex) {
        }
        applyAction.setEnabled(false);
        autoApplyAction = new Action() {

            public void run() {
                if (selectedPageName != null) {
                    Action action = getGlobalAction(selectedPageName, Constants.actionAutoApplyId);
                    runAction(action);
                    PageController.getPageController(selectedPageName).setPendingChanges(false);
                    updateDisableAllAction();
                }
            }
        };
        autoApplyAction.setId(Constants.actionAutoApplyId);
        autoApplyAction.setText("Auto-Apply");
        autoApplyAction.setToolTipText("Enables/Disables Mode Where Changes Are Automatically Applied to Target");
        autoApplyAction.setChecked(true);
        autoApplyAction.addPropertyChangeListener(new IPropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {
                Object newValue = event.getNewValue();
                if (newValue instanceof Boolean) {
                    boolean checked = ((Boolean) newValue).booleanValue();
                    if (checked) {
                        int pageCount = PageController.getPages() != null ? PageController.getPages().length : 0;
                        for (int i = 0; i < pageCount; ++i) {
                            String pageName = PageController.getPageName(i);
                            Action action = getGlobalAction(pageName, Constants.actionApplyId);
                            runAction(action);
                        }
                        autoApplyAction.setChecked(true);
                        applyAction.setEnabled(false);
                    } else {
                        autoApplyAction.setChecked(false);
                    }
                }
            }
        });
    }

    public void updateApplyAction(boolean action, PageModelItem item) {
        PageController pageController = PageController.getPageController(selectedPageName);
        boolean changed = getUnapplyedItems(pageController).length > 0 || (pageController.getTabControl().getCheckedElements().length == 0 && disableAllAction.isEnabled() || hasUncheckedItem(action, item, pageController));
        if (action && (!autoApplyAction.isChecked()) && (changed)) applyAction.setEnabled(true); else applyAction.setEnabled(false);
    }

    private boolean hasUncheckedItem(boolean action, PageModelItem item, PageController page) {
        if (item == null) return true; else {
            PageModelItem temp = item;
            boolean ret = false;
            Map stateMap = LogControlPlugin.getWorkspaceSaveContainer().getPageCheckedStateMap(page.getName());
            while (temp != null) {
                if (stateMap.get(temp.getTreePath()) != null) {
                    ret = true;
                    break;
                } else {
                    temp = temp.getParent();
                }
            }
            return ret;
        }
    }

    public void disableAll() {
        int pageCount = PageController.getPages() != null ? PageController.getPages().length : 0;
        for (int i = 0; i < pageCount; ++i) {
            PageController pageController = PageController.getPages()[i];
            disableAll(pageController, true);
        }
    }

    private boolean isParent(PageModelItem kid, PageModelItem parent) {
        PageModelItem kidParent = kid.getParent();
        boolean ret = false;
        while (kidParent != null) {
            if (kidParent == parent) {
                ret = true;
                break;
            }
            kidParent = kidParent.getParent();
        }
        return ret;
    }

    private PageModelItem[] getUnapplyedItems(PageController pageController) {
        Hashtable<String, PageModelItem> table = new Hashtable<String, PageModelItem>();
        TabControl treeControl = pageController.getTabControl();
        Object[] elements = treeControl.getCheckedElements();
        String pageName = pageController.getName();
        for (int i = 0; i < elements.length; i++) {
            PageModelItem item = (PageModelItem) elements[i];
            PageModelItem parent = item.getParent();
            if (parent == null) {
                boolean isChildIn = false;
                Object[] temp = elements;
                for (int j = 0; j < temp.length; j++) {
                    PageModelItem tempKid = (PageModelItem) temp[j];
                    if (LogControlPlugin.getWorkspaceSaveContainer().getPageCheckedStateMap(pageName).get(tempKid.getTreePath()) != null && isParent(tempKid, item)) {
                        isChildIn = true;
                        break;
                    }
                }
                if (LogControlPlugin.getWorkspaceSaveContainer().getPageCheckedStateMap(pageName).get(item.getTreePath()) == null && (!isChildIn)) table.put(item.getTreePath(), item);
            } else {
                boolean isParentIn = false;
                while (parent != null) {
                    if (LogControlPlugin.getWorkspaceSaveContainer().getPageCheckedStateMap(pageName).get(parent.getTreePath()) != null) {
                        isParentIn = true;
                        break;
                    }
                    parent = parent.getParent();
                }
                boolean isChildIn = false;
                if (item.hasChildren()) {
                    Object[] temp = elements;
                    for (int j = 0; j < temp.length; j++) {
                        PageModelItem tempKid = (PageModelItem) temp[j];
                        if (LogControlPlugin.getWorkspaceSaveContainer().getPageCheckedStateMap(pageName).get(tempKid.getTreePath()) != null && isParent(tempKid, item)) {
                            isChildIn = true;
                            break;
                        }
                    }
                }
                if (!isParentIn && (!isChildIn) && LogControlPlugin.getWorkspaceSaveContainer().getPageCheckedStateMap(pageName).get(item.getTreePath()) == null) table.put(item.getTreePath(), item);
            }
        }
        return table.values().toArray(new PageModelItem[table.size()]);
    }

    private void disableAll(PageController pageController, boolean keepUnapplyedItem) {
        TabControl treeControl = pageController.getTabControl();
        PageModelItem[] notapplied = getUnapplyedItems(pageController);
        pageController.disableLogging(pageController.getRootElements());
        if (treeControl != null && treeControl.isViewerOpen()) {
            if (keepUnapplyedItem) treeControl.setCheckedElements(notapplied); else treeControl.setCheckedElements(new PageModelItem[0]);
        }
    }

    private void runAction(final Action action) {
        if (action != null) {
            Environment.getEnvironment().run(new Runnable() {

                public void run() {
                    action.run();
                }
            }, "updating target");
        }
    }

    protected Action getGlobalAction(String pageName, String id) {
        Map map = (Map) actionMap.get(pageName);
        if (map != null) return (Action) map.get(id); else return null;
    }

    protected class CompositeAction extends Action {

        private LinkedList<Action> actions = new LinkedList<Action>();

        public void run() {
            Iterator it = actions.iterator();
            while (it.hasNext()) {
                ((Action) it.next()).run();
            }
        }

        public void add(Action action) {
            actions.add(action);
            if (actions.size() == 1) {
                setText(action.getText());
                setToolTipText(action.getToolTipText());
                setImageDescriptor(action.getImageDescriptor());
                setDisabledImageDescriptor(action.getDisabledImageDescriptor());
            }
        }

        public void setChecked(boolean checked) {
            super.setChecked(checked);
            for (Action action : actions) action.setChecked(checked);
        }

        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            for (Action action : actions) action.setEnabled(enabled);
        }
    }

    private void updateStatusLine() {
        Display.getDefault().asyncExec(new Runnable() {

            public void run() {
                String message = LogControlStatus.getLogControlStatus().getLastMessage();
                if (message != null && message.length() > 0) {
                    Environment.getEnvironment().showErrorMessage(LogControlStatus.getLogControlStatus().getLastMessage());
                }
            }
        });
    }

    /**
	 * return the state of Auto-apply action
	 *
	 * @author alex.k@ti.com
	 */
    public boolean isAutoApplyOn() {
        return autoApplyAction.isChecked();
    }

    /**
	 * Will pop up a dialog and ask for the profile name, then will import the profile.
	 * @return the profilename;
	 */
    public String importProfile() {
        String name = openProfile();
        if (name != null) {
            if (checkFile(name)) {
                name = LogControlPlugin.getWorkspaceSaveContainer().importState(name);
                updateSelectProfileAction();
                restoreState();
                updateDisableAllAction();
                return name;
            }
        }
        return "";
    }

    private boolean checkFile(String name) {
        File fileCheck = new File(name);
        if ((fileCheck.exists()) && (fileCheck.canRead())) {
            return true;
        }
        ti.mcore.Environment.getEnvironment().error(name + "does not exist or is not accessible!");
        return false;
    }

    public void clearStatePreSwitchProfile() {
        int pageCount = PageController.getPages() != null ? PageController.getPages().length : 0;
        for (int i = 0; i < pageCount; ++i) {
            PageController pageController = PageController.getPages()[i];
            disableAll(pageController, false);
        }
        autoApplyAction.setChecked(true);
    }

    private String openProfile() {
        FileDialog dialog = new FileDialog(PluginUtil.getShell(), SWT.OPEN | SWT.SYSTEM_MODAL);
        dialog.setText("Import Log Control Profile");
        dialog.setFilterPath(".");
        dialog.setFilterExtensions(new String[] { "*" + ZIP_EXT });
        dialog.setFilterNames(new String[] { "Log Control Profile (*" + ZIP_EXT + ")" });
        if (lastFileDialogPath == null) dialog.setFilterPath(System.getProperty("user.home") + "\\Desktop"); else dialog.setFilterPath(lastFileDialogPath);
        String name = dialog.open();
        if (name == null || name.compareTo("") == 0) {
            return null;
        }
        lastFileDialogPath = dialog.getFilterPath();
        return name;
    }

    public void restoreState() {
        tabFolder.setSelection(tabFolder.getItem(LogControlPlugin.getWorkspaceSaveContainer().getTabIndex()));
        selectedPageName = tabFolder.getItem(LogControlPlugin.getWorkspaceSaveContainer().getTabIndex()).getText();
        int pageCount = PageController.getPages() != null ? PageController.getPages().length : 0;
        for (int i = 0; i < pageCount; ++i) {
            PageController pageController = PageController.getPages()[i];
            TabControl treeControl = pageController.getTabControl();
            IPageModel model = pageController.getInput();
            if (model != null) {
                model.clearEnabledItems();
                model.clearDirtyItems();
            }
            if (treeControl != null && treeControl.isViewerOpen()) {
                pageController.startLogging(pageController.getRootElements());
                treeControl.restoreState();
                treeControl.restoreColumnWidth();
            }
        }
    }

    public void exportProfile(String profileName) {
        String name = export2file(profileName);
        if (name.length() > 0) {
            LogControlPlugin.getWorkspaceSaveContainer().exportState(name, profileName);
        }
    }

    private String export2file(String profileName) {
        FileDialog dialog = new FileDialog(PluginUtil.getShell(), SWT.SAVE | SWT.SYSTEM_MODAL);
        dialog.setText("Export Log Control Profile");
        dialog.setFilterExtensions(new String[] { "*" + ZIP_EXT });
        dialog.setFilterNames(new String[] { "Log Control Profile (*" + ZIP_EXT + ")" });
        if (!profileName.endsWith(ZIP_EXT)) profileName = profileName + ZIP_EXT;
        int index = profileName.indexOf(Constants.pathSeparator);
        while (index != -1) {
            profileName = profileName.substring(0, index) + "_" + profileName.substring(index + 1);
            index = profileName.indexOf(Constants.pathSeparator);
        }
        dialog.setFileName(profileName);
        if (lastFileDialogPath == null) dialog.setFilterPath(System.getProperty("user.home") + "\\Desktop"); else dialog.setFilterPath(lastFileDialogPath);
        final String name = dialog.open();
        if (name == null || name.compareTo("") == 0) {
            return "";
        }
        File file = new File(name);
        if (file.exists()) {
            MessageBox mbox = new MessageBox(parentShell, SWT.OK | SWT.CANCEL | SWT.ICON_WARNING | SWT.APPLICATION_MODAL);
            mbox.setText("Warning");
            mbox.setMessage("This file already exists. Do you want to overwrite it?");
            if (mbox.open() == SWT.CANCEL) return "";
        }
        lastFileDialogPath = dialog.getFilterPath();
        return name;
    }

    public void createProfile(String name) {
        Map c = new Hashtable(LogControlPlugin.getWorkspaceSaveContainer().getCheckedStateMap());
        Map e = new Hashtable(LogControlPlugin.getWorkspaceSaveContainer().getExpandedStateMap());
        int version = Constants.profileVersion;
        LogControlPlugin.getWorkspaceSaveContainer().addNewProfile(name, c, e, version);
        updateSelectProfileAction();
    }

    public void removeProfile(String name) {
        LogControlPlugin.getWorkspaceSaveContainer().removeProfile(name);
        updateSelectProfileAction();
    }

    public void removeAllProfiles() {
        LogControlPlugin.getWorkspaceSaveContainer().removeAllProfiles();
        updateSelectProfileAction();
    }

    public String[] getProfileNames() {
        return LogControlPlugin.getWorkspaceSaveContainer().getProfileNames();
    }

    public void applyProfile(String profileName) {
        clearStatePreSwitchProfile();
        LogControlPlugin.getWorkspaceSaveContainer().applyProfile(profileName);
        restoreState();
        updateDisableAllAction();
    }

    private void updateSelectProfileAction() {
        if (getProfileNames().length > 0) selectProfileAction.setEnabled(true); else selectProfileAction.setEnabled(false);
    }

    public void updateDisableAllAction() {
        PageController pageController = PageController.getPageController(selectedPageName);
        if (hasEnabledElements(pageController) && (!applyAction.isEnabled())) {
            disableAllAction.setEnabled(true);
        } else disableAllAction.setEnabled(false);
    }

    public void updatePropertiesDialog() {
        PropertiesAccess.updateView();
    }

    private boolean hasEnabledElements(PageController pageController) {
        return pageController.hasEnabledElements();
    }

    public boolean find(StringBuffer findResultStr, Runnable onFindCompletedNotification, String findCurrent, boolean directionForward, boolean regularExpression, boolean caseSensitive) {
        PageController pageController = PageController.getPageController(selectedPageName);
        boolean ret = false;
        if (pageController.getRootElements().length == 0) {
            findResultStr.append("The selected tab does not contain any item!");
            return false;
        }
        TabControl tabControl = pageController.getTabControl();
        try {
            ret = tabControl.find(onFindCompletedNotification, findCurrent, directionForward, regularExpression, caseSensitive);
        } catch (Exception e) {
            uiFindCompletedNotification(onFindCompletedNotification);
            ti.mcore.Environment.getEnvironment().unhandledException(e);
            findResultStr.append("Got exception during searching!");
            ret = false;
        } finally {
            uiFindCompletedNotification(onFindCompletedNotification);
        }
        if (!ret) {
            ArrayList<String> list = LogControlPlugin.getWorkspaceSaveContainer().findDialogState.list;
            if (list.size() > 0) {
                PageModelItem item = pageController.findItem(list.get(list.size() - 1), pageController.getContentProvider().getRootElements());
                if (item != null) {
                    String name = item.getName();
                    boolean found = false;
                    if (!caseSensitive) {
                        String findStrLowerCase = findCurrent.toLowerCase();
                        name = name.toLowerCase();
                        if (name.indexOf(findStrLowerCase) != -1) {
                            found = true;
                        }
                    } else {
                        if (name.indexOf(findCurrent) != -1) {
                            found = true;
                        }
                    }
                    if (found) {
                        findResultStr.append("name = " + item.getName() + " \n");
                        findResultStr.append("tree path = " + item.getTreePath());
                    } else findResultStr.append("No match has been detected.");
                } else findResultStr.append("No match has been detected.");
            } else findResultStr.append("No match has been detected.");
        }
        return ret;
    }

    private void uiFindCompletedNotification(final Runnable onFindCompletedNotification) {
        try {
            PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

                public void run() {
                    onFindCompletedNotification.run();
                }
            });
        } catch (Exception e) {
        }
    }

    public String getFindResult() {
        PageController pageController = PageController.getPageController(selectedPageName);
        return pageController.getTabControl().getFindResult();
    }

    public static boolean restoreSettings() {
        Iterator itr = ti.sutc.SUTConnectionManager.getSUTConnectionManager().getActiveConnections().iterator();
        if (itr.hasNext()) {
            return LogControlPlugin.getWorkspaceSaveContainer().restoreSettings;
        }
        return false;
    }

    public static void setRestoreSettings(boolean action) {
        LogControlPlugin.getWorkspaceSaveContainer().restoreSettings = action;
    }

    public void updateRestoreSettingsAction() {
        if (restoreSettingsAction != null) restoreSettingsAction.setChecked(LogControlPlugin.getWorkspaceSaveContainer().restoreSettings);
    }
}
