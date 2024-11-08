package org.thole.phiirc.client.view;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Arrays;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import org.jdesktop.swingx.JXMultiSplitPane;
import org.jdesktop.swingx.MultiSplitLayout;
import org.jdesktop.swingx.MultiSplitLayout.Divider;
import org.jdesktop.swingx.MultiSplitLayout.Leaf;
import org.jdesktop.swingx.MultiSplitLayout.Node;
import org.jdesktop.swingx.MultiSplitLayout.Split;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;
import org.jdesktop.swingx.util.OS;
import org.thole.phiirc.client.controller.Controller;
import org.thole.phiirc.client.model.Channel;
import org.thole.phiirc.client.view.interfaces.IChannelList;
import org.thole.phiirc.client.view.interfaces.IChatInput;
import org.thole.phiirc.client.view.interfaces.IChatPane;
import org.thole.phiirc.client.view.interfaces.IClientUI;
import org.thole.phiirc.client.view.interfaces.IUserList;
import org.thole.phiirc.client.view.swing.ChannelList;
import org.thole.phiirc.client.view.swing.ChatInput;
import org.thole.phiirc.client.view.swing.ChatPane;
import org.thole.phiirc.client.view.swing.MemoryIndicator;
import org.thole.phiirc.client.view.swing.UserList;
import org.thole.phiirc.client.view.swing.actions.SendListener;
import org.thole.phiirc.client.view.swing.actions.UIWindowListener;
import org.thole.phiirc.client.view.swing.menus.MainMenuBar;
import org.thole.phiirc.client.view.swing.menus.MainToolbar;
import org.thole.phiirc.client.view.swing.tray.Tray;

public class SwingClient extends JFrame implements IClientUI {

    private static final long serialVersionUID = 1286887524200269432L;

    private final ChannelList channelList;

    private final ChatPane chatPane;

    private final UserList userList;

    private final ChatInput chatInput;

    public SwingClient() {
        super("Phi IRC");
        this.setSize(new Dimension(800, 600));
        this.addWindowListener(new UIWindowListener());
        new Tray();
        final Controller controller = Controller.getInstance();
        controller.setClient(this);
        channelList = new ChannelList();
        chatPane = new ChatPane();
        userList = new UserList();
        chatInput = new ChatInput();
        this.add(new MainToolbar(), BorderLayout.NORTH);
        addSplitPanes();
        addBottomPanel();
        if (OS.isMacOSX()) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
        }
        this.setJMenuBar(new MainMenuBar());
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        try {
            if (OS.isMacOSX()) {
                UIManager.setLookAndFeel("ch.randelshofer.quaqua.QuaquaLookAndFeel");
            } else if (OS.isWindows()) {
                UIManager.setLookAndFeel("com.jgoodies.looks.windows.WindowsLookAndFeel");
            } else {
                UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        SwingUtilities.updateComponentTreeUI(getContentPane());
        this.setVisible(true);
    }

    private void addBottomPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        panel.add(chatInput);
        JButton button = new JButton("Send");
        button.addActionListener(new SendListener());
        panel.add(button);
        panel.add(new MemoryIndicator());
        this.add(panel, BorderLayout.SOUTH);
    }

    private void addSplitPanes() {
        JScrollPane scrollPane = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().add(chatPane);
        JScrollPane userScrollPane = new JScrollPane(userList);
        JSplitPane rightSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, userScrollPane);
        rightSplitPane.setDividerSize(1);
        rightSplitPane.setResizeWeight(0.8);
        rightSplitPane.setContinuousLayout(true);
        userScrollPane.setPreferredSize(userList.getPreferredSize());
        JScrollPane channelScrollPane = new JScrollPane(channelList);
        JSplitPane leftSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, channelScrollPane, rightSplitPane);
        leftSplitPane.setDoubleBuffered(true);
        leftSplitPane.setResizeWeight(0.2);
        leftSplitPane.setDividerSize(1);
        leftSplitPane.setContinuousLayout(true);
        channelScrollPane.setPreferredSize(channelList.getPreferredSize());
        this.add(leftSplitPane);
    }

    /**
	 * adds an experimental split layout to the frame
	 */
    private void addMultiSplitPane() {
        Leaf left = new Leaf("left");
        left.setWeight(0.15);
        Leaf center = new Leaf("center");
        center.setWeight(0.7);
        Leaf right = new Leaf("right");
        right.setWeight(0.15);
        Divider divider = new Divider();
        List<Node> children = Arrays.asList(left, divider, center, divider, right);
        MultiSplitLayout.Split modelRoot = new Split();
        modelRoot.setChildren(children);
        JXMultiSplitPane multiSplitPane = new JXMultiSplitPane();
        multiSplitPane.getMultiSplitLayout().setModel(modelRoot);
        multiSplitPane.add(channelList, "left");
        multiSplitPane.add(chatPane, "center");
        multiSplitPane.add(userList, "right");
        multiSplitPane.setPreferredSize(this.getPreferredSize());
        multiSplitPane.setDividerSize(1);
        this.add(multiSplitPane, BorderLayout.CENTER);
    }

    @Override
    public IChannelList getChannelList() {
        return channelList;
    }

    @Override
    public IChatInput getChatInput() {
        return chatInput;
    }

    @Override
    public IChatPane getChatPane() {
        return chatPane;
    }

    @Override
    public IUserList getUserList() {
        return userList;
    }

    @Override
    public void setCurrentChatModel(final Channel channel) {
    }

    public Container getMainPane() {
        return this.getContentPane();
    }
}
