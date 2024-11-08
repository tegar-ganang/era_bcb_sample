package com.googlecode.kanzaki.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import com.googlecode.kanzaki.controller.RootController;

public class MainFrame extends JFrame {

    /**
	 * 
	 */
    private static final long serialVersionUID = -7198943603293790697L;

    public static final String appName = "kanzaki";

    public static final int defaultWidth = 400;

    public static final int defaultHeigth = 300;

    private RootController rootController;

    private MainMenuBar mainMenuBar;

    private JTabbedPane tabbedPane;

    private JSplitPane splitPane;

    private JPanel bottomPanel;

    private HashMap channelViews;

    public MainFrame() {
        channelViews = new HashMap();
        rootController = new RootController(this);
        setSize(defaultWidth, defaultHeigth);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle(appName);
        mainMenuBar = new MainMenuBar(rootController);
        setJMenuBar(mainMenuBar);
        setLocationRelativeTo(null);
    }

    /**
	 * Calling this method will disable all connect controls in this frame
	 */
    public void disableConnectControls() {
        mainMenuBar.disableConnectItems();
    }

    /**
	 * Calling this method will enable all connect controls in this frame
	 */
    public void enableConnectControls() {
        mainMenuBar.enableConnectItems();
    }

    /**
	 * Adds channel view container. If channel with such name exists,
	 * it is replaced and previous container is lost.
	 * @param container Container that need to be added
	 */
    public void addChannelViewContainer(ChannelViewContainer container) {
        if (tabbedPane == null) {
            Container contentPane = getContentPane();
            tabbedPane = new JTabbedPane();
            splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(container.getChannelPane()), container.getUsersPanel());
            splitPane.setResizeWeight(1.0);
            tabbedPane.add(container.getChannelName(), splitPane);
            bottomPanel = new JPanel();
            bottomPanel.setLayout(new BorderLayout());
            bottomPanel.add(container.getMessageInputField(), BorderLayout.CENTER);
            bottomPanel.add(container.getSendButton(), BorderLayout.EAST);
            bottomPanel.add(container.getEmotionComboBox(), BorderLayout.WEST);
            contentPane.setLayout(new BorderLayout());
            contentPane.add(splitPane, BorderLayout.CENTER);
            contentPane.add(container.getTopPanel(), BorderLayout.NORTH);
            contentPane.add(bottomPanel, BorderLayout.SOUTH);
            channelViews.put(container.getChannelName(), container);
        } else {
            Set keys = channelViews.keySet();
            Iterator iterator = keys.iterator();
            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                if (key.equals(container.getChannelName())) {
                    channelViews.remove(key);
                    channelViews.put(container.getChannelName(), container);
                    return;
                }
            }
        }
        channelViews.put(container.getChannelName(), container);
    }
}
