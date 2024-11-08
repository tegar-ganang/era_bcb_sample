package net.hypotenubel.jaicwain.gui.swing;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import net.hypotenubel.jaicwain.App;
import net.hypotenubel.jaicwain.gui.docking.*;

/**
 * Provides a tabbed pane control for {@code ExtendedPanel}s. The component
 * is specialised on managing {@code IRCChatPanel}s and
 * {@code IRCServerPanel}s. Panels added to this component are always
 * visible and can not be detached. This component shouldn't be used in direct
 * conjunction with a {@code WindowManager} object. 
 * 
 * @author Christoph Daniel Schulze
 * @version $Id: TabbedChannelContainer.java 111 2006-09-20 16:29:08Z captainnuss $
 */
public class TabbedChannelContainer extends JComponent implements ChangeListener, DockingPanelContainer, MouseListener {

    /**
     * Parent {@code WindowManager}.
     */
    protected WindowManager manager = null;

    /**
     * This is the component we're trying to manage here, a
     * {@code JTabbedPane}.
     */
    protected JTabbedPane tab;

    /**
     * Creates a new {@code TabbedContainer} object and initializes it.
     * Calling this one causes the tabs to be placed at the bottom.
     */
    public TabbedChannelContainer() {
        this(JTabbedPane.BOTTOM);
    }

    /**
     * Creates a new {@code TabbedChannelContainer} object and initializes it.
     * 
     * @param tabPlacement {@code int} specifying where the tabs of the
     *                     {@code JTabbedPane} shall be put. One of 
     *                     {@code JTabbedPane.TOP},
     *                     {@code JTabbedPane.BOTTOM},
     *                     {@code JTabbedPane.LEFT}, or
     *                     {@code JTabbedPane.RIGHT}.
     */
    public TabbedChannelContainer(int tabPlacement) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder());
        tab = new JTabbedPane(tabPlacement);
        tab.addChangeListener(this);
        tab.putClientProperty(com.jgoodies.looks.Options.EMBEDDED_TABS_KEY, Boolean.TRUE);
        add(tab, BorderLayout.CENTER);
        tab.addMouseListener(this);
        setVisible(false);
    }

    /**
     * Returns the {@code JTabbedPane} component used by this container.
     * 
     * @return {@code JTabbedPane} used by this container.
     */
    public JTabbedPane getTabbedPane() {
        return tab;
    }

    /**
     * Computes the index where the new panel will have to be inserted. Channel
     * windows are grouped by their server.
     * 
     * @param panel {@code DockingPanel} to compute the index for.
     */
    private int computeIndex(DockingPanel panel) {
        if (tab.getTabCount() == 0) return 0;
        if (panel instanceof IRCSessionPanel) {
            return tab.getTabCount();
        } else if (panel instanceof IRCChatPanel) {
            int index = 0;
            boolean session = false;
            Component c = null;
            IRCChatPanel p = (IRCChatPanel) panel;
            for (index = 0; index < tab.getTabCount(); index++) {
                c = tab.getComponentAt(index);
                if (c instanceof IRCSessionPanel) {
                    if (((IRCSessionPanel) c).getSession() == p.getChannel().getParentSession()) {
                        session = true;
                    } else {
                        if (session) return index;
                    }
                } else if (c instanceof IRCChatPanel) {
                    if (((IRCChatPanel) c).getChannel().getName().compareTo(p.getChannel().getName()) > 0 && session) {
                        return index;
                    }
                }
            }
        }
        return tab.getTabCount();
    }

    public void setWindowManager(WindowManager manager) {
        if (manager != null) this.manager = manager;
    }

    public WindowManager getWindowManager() {
        return manager;
    }

    public String getContainerTitle() {
        if (tab.getSelectedComponent() == null) {
            return App.localization.localize("app", "tabbedcontainer.emptytitle", "Chat Area");
        } else {
            String[] args = new String[] { ((DockingPanel) tab.getSelectedComponent()).getTitle() };
            return App.localization.localize("app", "tabbedcontainer.fulltitle", "Chat Area", args);
        }
    }

    public boolean canAdd(DockingPanel panel) {
        if ((panel instanceof IRCChatPanel || panel instanceof IRCSessionPanel) && !containsPanel(panel)) return true; else return false;
    }

    public boolean addDockingPanel(DockingPanel panel) {
        if (panel == null) return false;
        if (containsPanel(panel)) return true;
        int index = computeIndex(panel);
        tab.add(panel, index);
        panel.setContainer(this);
        tab.setTitleAt(index, panel.getTitle());
        tab.setIconAt(index, panel.getSmallIcon());
        tab.setToolTipTextAt(index, panel.getDescription());
        bringToFront(panel);
        showContainer();
        return true;
    }

    public void showDockingPanel(DockingPanel panel) {
        addDockingPanel(panel);
    }

    public void hideDockingPanel(DockingPanel panel) {
        removeDockingPanel(panel);
    }

    public boolean isVisible(DockingPanel panel) {
        return containsPanel(panel);
    }

    public DockingPanel[] getVisibleDockingPanels() {
        return getDockingPanels();
    }

    public void bringToFront(DockingPanel panel) {
        if (panel == null) return;
        if (!containsPanel(panel)) return;
        tab.setSelectedIndex(tab.indexOfComponent(panel));
    }

    public void removeDockingPanel(DockingPanel panel) {
        if (panel == null) return;
        if (!containsPanel(panel)) return;
        tab.remove(panel);
        if (tab.getTabCount() == 0) hideContainer();
    }

    public DockingPanel[] getDockingPanels() {
        DockingPanel[] panels = new DockingPanel[tab.getTabCount()];
        for (int i = 0; i < tab.getTabCount(); i++) panels[i] = (DockingPanel) tab.getComponentAt(i);
        return panels;
    }

    public void panelInformationUpdated(DockingPanel panel) {
        int index = tab.indexOfComponent(panel);
        if (index == -1) return;
        tab.setTitleAt(index, panel.getTitle());
        tab.setIconAt(index, panel.getSmallIcon());
        tab.setToolTipTextAt(index, panel.getDescription());
    }

    public boolean containsPanel(DockingPanel panel) {
        return tab.indexOfComponent(panel) != -1;
    }

    public void flagContentChange(DockingPanel panel, boolean important) {
        if (panel == null) return;
        if (!containsPanel(panel)) return;
        int index = tab.indexOfComponent(panel);
        if (index == -1) return;
        if (tab.getSelectedIndex() == index) return;
        if (important) tab.setForegroundAt(index, App.options.getColorOption("gui", "tabbedcontainer.flagcolors.important", new Color(-16777012))); else tab.setForegroundAt(index, App.options.getColorOption("gui", "tabbedcontainer.flagcolors.unimportant", new Color(-3407872)));
    }

    public void saveContainerState() {
    }

    public boolean saveContainerPresence() {
        return false;
    }

    public void showContainer() {
        setVisible(true);
    }

    public void hideContainer() {
        setVisible(false);
    }

    public void closeContainer() {
    }

    public void stateChanged(ChangeEvent e) {
        if (tab.getSelectedIndex() != -1) tab.setForegroundAt(tab.getSelectedIndex(), null);
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    /**
     * Displays the extended panel's menu items, if any.
     */
    public void mousePressed(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON3) return;
        if (!(tab.getSelectedComponent() instanceof DockingPanel)) return;
        DockingPanel panel = (DockingPanel) tab.getSelectedComponent();
        JPopupMenu pMenu = new JPopupMenu();
        JMenuItem[] items = panel.getMenuItems();
        if (items == null) return;
        for (int i = 0; i < items.length; i++) pMenu.add(items[i]);
        pMenu.show(tab, e.getX(), e.getY());
    }

    public void mouseReleased(MouseEvent e) {
    }
}
