package de.teamwork.jaicwain.gui.swing;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import de.teamwork.ctcp.*;
import de.teamwork.ctcp.msgutils.*;
import de.teamwork.irc.*;
import de.teamwork.irc.msgutils.*;
import de.teamwork.jaicwain.App;
import de.teamwork.jaicwain.gui.*;
import de.teamwork.jaicwain.options.*;
import de.teamwork.jaicwain.session.irc.*;
import de.teamwork.util.swing.JHistoryTextField;

/**
 * Provides a tabbed pane control for <code>ExtendedPanel</code>s. The component
 * is specialised on managing <code>IRCChatPanel</code>s and
 * <code>IRCServerPanel</code>s. Panels added to this component are always
 * visible and can not be detached. This component shouldn't be used in direct
 * conjunction with a <code>WindowManager</code> object. 
 * 
 * @author Christoph Daniel Schulze
 * @version $Id: TabbedChannelContainer.java 3 2003-01-07 14:16:38Z captainnuss $
 */
public class TabbedChannelContainer extends JComponent implements ExtendedPanelContainer, ChangeListener {

    /**
     * Parent <code>WindowManager</code>.
     */
    protected WindowManager manager = null;

    /**
     * This is the component we're trying to manage here, a
     * <code>JTabbedPane</code>.
     */
    protected JTabbedPane tab;

    /**
     * Creates a new <code>TabbedContainer</code> object and initializes it.
     * Calling this one causes the tabs to be placed at the bottom.
     */
    public TabbedChannelContainer() {
        this(JTabbedPane.BOTTOM);
    }

    /**
     * Creates a new <code>TabbedChannelContainer</code> object and initializes it.
     * 
     * @param tabPlacement <code>int</code> specifying where the tabs of the
     *                     <code>JTabbedPane</code> shall be put. One of 
     *                     <code>JTabbedPane.TOP</code>,
     *                     <code>JTabbedPane.BOTTOM</code>,
     *                     <code>JTabbedPane.LEFT</code>, or
     *                     <code>JTabbedPane.RIGHT</code>.
     */
    public TabbedChannelContainer(int tabPlacement) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder());
        tab = new JTabbedPane(tabPlacement);
        tab.addChangeListener(this);
        add(tab, BorderLayout.CENTER);
        tab.addMouseListener(new MouseInputAdapter() {

            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    if (manager != null) {
                        if (tab.getSelectedComponent() == null) return;
                        JPopupMenu menu = manager.createPopupMenu((ExtendedPanel) tab.getSelectedComponent());
                        menu.show(tab, e.getX(), e.getY());
                    }
                }
            }
        });
        setVisible(false);
    }

    /**
     * Returns the <code>JTabbedPane</code> component used by this container.
     * 
     * @return <code>JTabbedPane</code> used by this container.
     */
    public JTabbedPane getTabbedPane() {
        return tab;
    }

    /**
     * Computes the index where the new panel will have to be inserted. Channel
     * windows are grouped by their server.
     * 
     * @param panel <code>ExtendedPanel</code> to compute the index for.
     */
    private int computeIndex(ExtendedPanel panel) {
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
                    if (((IRCChatPanel) c).getChannel().getName().compareTo(p.getChannel().getName()) > 0) {
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
            String[] args = new String[] { ((ExtendedPanel) tab.getSelectedComponent()).getPanelTitle() };
            return App.localization.localize("app", "tabbedcontainer.fulltitle", "Chat Area", args);
        }
    }

    public boolean canAdd(ExtendedPanel panel) {
        if ((panel instanceof IRCChatPanel || panel instanceof IRCSessionPanel) && !containsPanel(panel)) return true; else return false;
    }

    public boolean addExtendedPanel(ExtendedPanel panel) {
        if (panel == null) return false;
        if (containsPanel(panel)) return true;
        int index = computeIndex(panel);
        tab.add(panel, index);
        panel.setPanelContainer(this);
        tab.setTitleAt(index, panel.getPanelTitle());
        tab.setIconAt(index, panel.getPanelSmallIcon());
        tab.setToolTipTextAt(index, panel.getPanelDescription());
        bringToFront(panel);
        showContainer();
        return true;
    }

    public void showExtendedPanel(ExtendedPanel panel) {
        addExtendedPanel(panel);
    }

    public void hideExtendedPanel(ExtendedPanel panel) {
        removeExtendedPanel(panel);
    }

    public boolean isVisible(ExtendedPanel panel) {
        return containsPanel(panel);
    }

    public ExtendedPanel[] getVisibleExtendedPanels() {
        return getExtendedPanels();
    }

    public void bringToFront(ExtendedPanel panel) {
        if (panel == null) return;
        if (!containsPanel(panel)) return;
        tab.setSelectedIndex(tab.indexOfComponent(panel));
    }

    public void removeExtendedPanel(ExtendedPanel panel) {
        if (panel == null) return;
        if (!containsPanel(panel)) return;
        tab.remove(panel);
        if (tab.getTabCount() == 0) hideContainer();
    }

    public ExtendedPanel[] getExtendedPanels() {
        ExtendedPanel[] panels = new ExtendedPanel[tab.getTabCount()];
        for (int i = 0; i < tab.getTabCount(); i++) panels[i] = (ExtendedPanel) tab.getComponentAt(i);
        return panels;
    }

    public void panelInformationUpdated(ExtendedPanel panel) {
        int index = tab.indexOfComponent(panel);
        if (index == -1) return;
        tab.setTitleAt(index, panel.getPanelTitle());
        tab.setIconAt(index, panel.getPanelSmallIcon());
        tab.setToolTipTextAt(index, panel.getPanelDescription());
    }

    public boolean containsPanel(ExtendedPanel panel) {
        return tab.indexOfComponent(panel) != -1;
    }

    public void flagContentChange(ExtendedPanel panel, boolean important) {
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
}
