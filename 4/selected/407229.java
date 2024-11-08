package de.teamwork.jaicwain.gui.swing;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import de.teamwork.jaicwain.App;
import de.teamwork.jaicwain.gui.*;
import de.teamwork.jaicwain.session.irc.*;
import de.teamwork.util.swing.DesktopLayout;

/**
 * Jaic Wain's main window class. It's managed by a <code>WindowManager</code>
 * and features stuff.
 * 
 * @author Christoph Daniel Schulze
 * @version $Id: MainFrame.java 3 2003-01-07 14:16:38Z captainnuss $
 */
public class MainFrame extends ManagedJFrame implements ChangeListener, ComponentListener {

    /**
     * <code>JPanel</code> containing the tabbed channel container.
     */
    private JPanel centerPanel = null;

    /**
     * <code>TabbedChannelContainer</code> that will take care of displaying
     * stuff.
     */
    private TabbedChannelContainer tab = null;

    /**
     * <code>int</code> indicating this frame's x position. If this frame is
     * maximized, this variable stores the former x position.
     */
    private int frameX = 0;

    /**
     * <code>int</code> indicating this frame's y position. If this frame is
     * maximized, this variable stores the former y position.
     */
    private int frameY = 0;

    /**
     * <code>int</code> indicating this frame's height. If this frame is
     * maximized, this variable stores the former height.
     */
    private int frameHeight = 0;

    /**
     * <code>int</code> indicating this frame's width. If this frame is
     * maximized, this variable stores the former width.
     */
    private int frameWidth = 0;

    /**
     * <code>int</code> indicating this frame's extended state.
     */
    private int extendedState = Frame.NORMAL;

    /**
     * Creates a new <code>MainFrame</code> object and initializes it.
     * 
     * @param bar <code>JMenuBar</code> to use.
     * @param actions <code>ActionMap</code> to use.
     * @param inputs <code>InputMap</code> to use.
     */
    public MainFrame(JMenuBar bar, ActionMap actions, InputMap inputs) {
        super();
        addComponentListener(this);
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                App.options.beginUpdate("gui");
                App.options.setIntOption("gui", "mainframe.x", frameX);
                App.options.setIntOption("gui", "mainframe.y", frameY);
                App.options.setIntOption("gui", "mainframe.height", frameHeight);
                App.options.setIntOption("gui", "mainframe.width", frameWidth);
                App.options.setIntOption("gui", "mainframe.state", getExtendedState());
                App.options.endUpdate("gui", false);
                closing();
            }
        });
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle(App.APP_SHORT_NAME + " " + App.APP_VERSION);
        setJMenuBar(bar);
        centerPanel = new JPanel();
        centerPanel.setLayout(new BorderLayout());
        getWindowPanel().add(centerPanel, DesktopLayout.CENTER);
        tab = new TabbedChannelContainer();
        tab.getTabbedPane().addChangeListener(this);
        centerPanel.add(tab, BorderLayout.CENTER);
        show();
        setSize(App.options.getIntOption("gui", "mainframe.width", 600), App.options.getIntOption("gui", "mainframe.height", 400));
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        screen.setSize((screen.getWidth() / 2 - getWidth() / 2), (screen.getHeight() / 2 - getHeight() / 2));
        setLocation(App.options.getIntOption("gui", "mainframe.x", (int) screen.getWidth()), App.options.getIntOption("gui", "mainframe.y", (int) screen.getHeight()));
        setExtendedState(App.options.getIntOption("gui", "mainframe.state", Frame.NORMAL));
    }

    /**
     * Removes this <code>MainFrame</code> from the collection of frames.
     */
    private void closing() {
        tab.getTabbedPane().removeChangeListener(this);
        Component c;
        IRCSessionPanel s;
        for (int i = 0; i < tab.getTabbedPane().getTabCount(); i++) {
            c = tab.getTabbedPane().getComponentAt(i);
            if (c instanceof IRCSessionPanel) {
                s = (IRCSessionPanel) c;
                if (s.getSession().isActive()) s.getSession().disconnect();
            }
        }
        App.gui.removeMainFrame(this);
    }

    /**
     * Returns the <code>TabbedChannelContainer</code> instance used by this
     * <code>MainFrame</code>.
     * 
     * @return <code>TabbedChannelContainer</code> instance used by this
     *         <code>MainFrame</code>.
     */
    public TabbedChannelContainer getTabbedChannelContainer() {
        return tab;
    }

    public void stateChanged(ChangeEvent e) {
        if (tab.getTabbedPane().getTabCount() == 0) {
            setTitle(App.APP_SHORT_NAME + " " + App.APP_VERSION);
        } else {
            DefaultIRCSession s = null;
            Component c = tab.getTabbedPane().getSelectedComponent();
            String stuff = "";
            if (c instanceof IRCChatPanel) {
                AbstractIRCSession session = ((IRCChatPanel) c).getChannel().getParentSession();
                s = (DefaultIRCSession) session;
            } else if (c instanceof IRCSessionPanel) s = ((IRCSessionPanel) c).getSession();
            if (s != null) stuff = s.getConnection().getRemoteAddress().toString() + " (" + s.getUser().getNick() + ") - " + App.APP_SHORT_NAME + " " + App.APP_VERSION; else stuff = App.APP_SHORT_NAME + " " + App.APP_VERSION;
            setTitle(stuff);
        }
        App.gui.tabStateChanged(this);
    }

    public void componentResized(ComponentEvent e) {
        if (getExtendedState() != MAXIMIZED_BOTH) {
            frameHeight = getHeight();
            frameWidth = getWidth();
        }
    }

    public void componentMoved(ComponentEvent e) {
        if (getExtendedState() != MAXIMIZED_BOTH) {
            frameX = getX();
            frameY = getY();
        }
    }

    public void componentShown(ComponentEvent e) {
    }

    public void componentHidden(ComponentEvent e) {
    }
}
