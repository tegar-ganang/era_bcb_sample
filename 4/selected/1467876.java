package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.io.File;
import java.net.URL;
import javax.swing.*;
import programLogic.FollowTailAdjustmentListener;
import programLogic.GuiController;
import programLogic.TabChangeListener;
import programLogic.rightClickMenus.TabPopupMenuListener;
import programLogic.rightClickMenus.TabPopupMenuMouseAdapter;

/**The main GUI for the program.
 * 
 * @author Fahmi A
 */
public class DynaReaderWindow {

    private JFrame window;

    private JMenuBar menuBar;

    private JTabbedPane tabbedPane;

    private JPopupMenu tabPopupmenu;

    private GuiController guiListener;

    private final String programName;

    private final ImageIcon logo;

    private TabChangeListener tabChangeListener;

    private static final String VERSION = "1.2a";

    /**Creates a Graphical User Interface for the program.
	 * 
	 * @param width The width in pixels of the window frame.
	 * @param height The height in pixels of the window frame.
	 */
    public DynaReaderWindow(int width, int height) {
        programName = "DynaReader";
        window = new JFrame(programName);
        guiListener = new GuiController(this);
        window.addWindowListener(guiListener);
        window.setPreferredSize(new Dimension(width, height));
        window.getContentPane().setLayout(new BorderLayout());
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.pack();
        menuBar = new JMenuBar();
        initMenuBar();
        window.setJMenuBar(menuBar);
        tabbedPane = new JTabbedPane();
        tabChangeListener = new TabChangeListener(this);
        tabbedPane.addChangeListener(tabChangeListener);
        initTabPopupMenu();
        TransferHandler handler = new DnDHandler(this);
        tabbedPane.setTransferHandler(handler);
        window.add(tabbedPane);
        logo = loadIcon();
        window.setIconImage(logo.getImage());
        window.setVisible(true);
    }

    /**Initializes the menu bar with sub menus and buttons, and puts the menu bar on the window.*/
    private void initMenuBar() {
        JMenu fileMenu = new JMenu("File");
        addMenuBarButton("Open", fileMenu);
        addMenuBarButton("Save As", fileMenu);
        addMenuBarButton("Close Current Tab", fileMenu);
        addMenuBarButton("Exit", fileMenu);
        JMenu optionsMenu = new JMenu("Options");
        addMenuBarButton("Clear Text", optionsMenu);
        addMenuBarButton("Reload File", optionsMenu);
        addCheckBoxMenuBarItem("Stay On Top", optionsMenu, false);
        addCheckBoxMenuBarItem("Follow Tail", optionsMenu, true);
        JMenu helpMenu = new JMenu("Help");
        addMenuBarButton("About", helpMenu);
        menuBar.add(fileMenu);
        menuBar.add(optionsMenu);
        menuBar.add(helpMenu);
    }

    /**Initialises the right-click popup menu for the tabs.*/
    private void initTabPopupMenu() {
        tabPopupmenu = new JPopupMenu("Tab Options");
        MouseAdapter popupMenuMouseAdapter = new TabPopupMenuMouseAdapter(tabPopupmenu, tabbedPane);
        TabPopupMenuListener tabPopupMenuListener = new TabPopupMenuListener(this);
        JMenuItem menuItem = new JMenuItem(TabPopupMenuListener.RENAMETAB_OPTION);
        menuItem.addActionListener(tabPopupMenuListener);
        tabPopupmenu.add(menuItem);
        menuItem = new JMenuItem(TabPopupMenuListener.CLOSE_OPTION);
        menuItem.addActionListener(tabPopupMenuListener);
        tabPopupmenu.add(menuItem);
        menuItem = new JMenuItem(TabPopupMenuListener.CLOSEALLBUTME_OPTION);
        menuItem.addActionListener(tabPopupMenuListener);
        tabPopupmenu.add(menuItem);
        menuItem = new JMenuItem(TabPopupMenuListener.CLOSEALL_OPTION);
        menuItem.addActionListener(tabPopupMenuListener);
        tabPopupmenu.add(menuItem);
        menuItem = new JMenuItem(TabPopupMenuListener.RELOAD_OPTION);
        menuItem.addActionListener(tabPopupMenuListener);
        tabPopupmenu.add(menuItem);
        menuItem = new JMenuItem(TabPopupMenuListener.CLEAR_OPTION);
        menuItem.addActionListener(tabPopupMenuListener);
        tabPopupmenu.add(menuItem);
        menuItem = new JCheckBoxMenuItem(TabPopupMenuListener.LOAD_OPTION);
        menuItem.addActionListener(tabPopupMenuListener);
        tabPopupmenu.add(menuItem);
        tabbedPane.addMouseListener(popupMenuMouseAdapter);
    }

    /**Loads the DynaReader icon from within the executable.
	 * 
	 * @return The DynaReader icon.
	 */
    private ImageIcon loadIcon() {
        URL url = ClassLoader.getSystemClassLoader().getResource("resources/Logo.png");
        return new ImageIcon(url);
    }

    /**Creates a new button to add to a sub menu.
	 * Also adds guiListener as the button's action listener. 
	 * @param buttonName The name the button will have.
	 * @param subMenu The sub menu to add the button to.
	 */
    private void addMenuBarButton(String buttonName, JMenu subMenu) {
        JMenuItem item = new JMenuItem(buttonName);
        item.addActionListener(guiListener);
        subMenu.add(item);
    }

    /**Creates a new check box to add to a sub menu.
	 * Also adds guiListener as the check box's action listener. 
	 * @param checkBoxName The name the check box will have.
	 * @param subMenu The sub menu to add the check box to.
	 * @param checked Weather or not the check box will be checked by default or not (True, if it will be checked by default. False otherwise).
	 */
    private void addCheckBoxMenuBarItem(String checkBoxName, JMenu subMenu, boolean checked) {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(checkBoxName, checked);
        item.addActionListener(guiListener);
        subMenu.add(item);
    }

    /**Adds a new tab to the window.
	 * 
	 * @param file The file that will be loaded.
	 * @param followTail Weather or not the scroll bar should stay at the bottom.
	 */
    public void addNewTab(File file, boolean followTail) {
        DynaReaderTab newTab = new DynaReaderTab(file, new FollowTailAdjustmentListener(followTail), this);
        tabbedPane.addTab(newTab.getTitle(), newTab);
        tabbedPane.setSelectedComponent(newTab);
        tabbedPane.setTabComponentAt(tabbedPane.getSelectedIndex(), new StatusTabComponent(tabbedPane));
    }

    /**Will notify the user that there has been an update from a tab.
	 * 
	 * @param tab The tab that has generated the update.
	 */
    public void notifyOfUpdate(DynaReaderTab tab) {
        int index = tabbedPane.indexOfComponent(tab);
        if (index == -1) System.out.println("notifyOfUpdate() - index = " + index);
        StatusTabComponent status = (StatusTabComponent) tabbedPane.getTabComponentAt(index);
        status.setState(tab.getState());
    }

    /**Removes the currently selected tab and releases all resources associated with it*/
    public void removeCurrentTab() {
        int tabIndex = tabbedPane.getSelectedIndex();
        DynaReaderTab tab = getCurrentTab();
        if (tab != null) {
            tab.dispose();
            tabbedPane.remove(tabIndex);
        }
    }

    /**Removes that tab that at a specified tab index.
	 * 
	 * @param tabIndex The index of the tab to remove.
	 */
    public boolean removeTabAtIndex(int tabIndex) {
        if (tabIndex < 0 || tabIndex >= tabbedPane.getTabCount()) return false;
        DynaReaderTab tab = (DynaReaderTab) tabbedPane.getComponentAt(tabIndex);
        if (tab != null) {
            tab.dispose();
            tabbedPane.remove(tabIndex);
            return true;
        }
        return false;
    }

    /**Gets the tab that is found at a given index.
	 * 
	 * @param index The index of the tab.
	 * @return The tab at the given index. Null, if not tab is at the given index.
	 */
    public DynaReaderTab getTabAtIndex(int index) {
        return (DynaReaderTab) tabbedPane.getComponentAt(index);
    }

    /**Gets the tab index of the currently selected tab.
	 * 
	 * @return Tab index. -1 if not tabs are open.
	 */
    public int getCurrentTabIndex() {
        return tabbedPane.getSelectedIndex();
    }

    /**Closes the window and performs and clean-up to be done.*/
    public void close() {
        window.dispose();
        tabbedPane.removeChangeListener(tabChangeListener);
        boolean deleted = true;
        while (deleted == true) {
            deleted = removeTabAtIndex(0);
        }
        System.exit(0);
    }

    /**Opens the about dialog.
	 * The dialog is used to show information about this program.
	 * The About dialog is found under Help -> About.
	 */
    public void showAboutDialog() {
        String message = "Created to view growing program log files. v" + VERSION + "\nBy: Fahmi";
        JOptionPane.showMessageDialog(window, message, "About DynaReader", JOptionPane.INFORMATION_MESSAGE, logo);
    }

    /**Opens the load file window to let the user load a file.
	 * A file may be given to open the load file dialog at the directory of the file.
	 * @param lastFile The last file that was loaded (can be null).
	 * @return The File object of the file the user selected to open.
	 */
    public File showLoadFileDialog(File lastFile) {
        JFileChooser fileChooser;
        fileChooser = new JFileChooser(lastFile);
        int result = fileChooser.showOpenDialog(window);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file.exists()) {
                return file;
            } else {
                showWarningDialog("The file does not exist.");
            }
        }
        return null;
    }

    /**Opens the save file dialog to save a file.
	 * 
	 * @param lastFile The last file opened (can by null).
	 * @return The directory string of the file to be saved (null if operation could not be completed).
	 */
    public File showSaveFileDialog(File lastFile) {
        JFileChooser fileChooser;
        fileChooser = new JFileChooser(lastFile);
        int result = fileChooser.showSaveDialog(window);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file.exists()) {
                int overwriteResult = JOptionPane.showConfirmDialog(window, "The file already exists, would you like to overwrite it?");
                if (overwriteResult != JOptionPane.OK_OPTION) {
                    showWarningDialog("File will not be overwritten.");
                    return null;
                }
            }
            return file;
        }
        return null;
    }

    /**Gets the currently selected tab.
	 * 
	 * @return The currently selected tab. Null, if no tabs are open.
	 */
    public DynaReaderTab getCurrentTab() {
        return (DynaReaderTab) tabbedPane.getSelectedComponent();
    }

    /**Set weather or not the scroll bars for each tab should stick to the bottom or not.
	 * 
	 * @param followTail True, if the scroll bars should follow the tail of there text area. False, otherwise.
	 */
    public void setFollowTail(boolean followTail) {
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            DynaReaderTab tab = (DynaReaderTab) tabbedPane.getSelectedComponent();
            tab.setFollowTail(followTail);
        }
    }

    /**Sets the subtitle for the name found in the title bar.
	 * 
	 * @param subTitle The text to append to the program name.
	 */
    public void setSubTitle(String subTitle) {
        window.setTitle(programName + " - " + subTitle);
    }

    /**Changes weather the window should stay above all other windows, or not.
	 * 
	 * @param onTop True, if you want the window to display on top of all other windows. False, otherwise.
	 */
    public void setAlwaysOnTop(boolean onTop) {
        window.setAlwaysOnTop(onTop);
        System.out.println("DW: Always on top: " + onTop);
    }

    /**Re-titles the name of a tab as shown to the user.
	 * 
	 * @param tabIndex The index of the tab.
	 * @param newTabTitle The new title to be given to the tab.
	 */
    public void retitleTab(int tabIndex, String newTabTitle) {
        DynaReaderTab tab = getTabAtIndex(tabIndex);
        tab.setTitle(newTabTitle);
        tabbedPane.setTitleAt(tabIndex, newTabTitle);
        setSubTitle(newTabTitle);
    }

    public void setTabTailLoad(boolean loadTail) {
        DynaReaderTab tab = (DynaReaderTab) tabbedPane.getSelectedComponent();
        tab.updateTail(loadTail);
    }

    /**Shows a warning dialog box with specified text.
	 * 
	 * @param textToDisplay The text the warning dialog will display.
	 */
    public void showWarningDialog(String textToDisplay) {
        JOptionPane.showMessageDialog(window, textToDisplay);
    }

    /**Displays an input dialog wich asks the user for text as input.
	 * 
	 * @param textToDisplay The text to display in the dialog.
	 * @param title The title of the dialog window.
	 * @param defaultInput The default text to display as input.
	 * @return What the user has entered as input.
	 */
    public String showInputDialog(String textToDisplay, String title, String defaultInput) {
        return (String) JOptionPane.showInputDialog(window, textToDisplay, title, JOptionPane.PLAIN_MESSAGE, logo, null, defaultInput);
    }
}
