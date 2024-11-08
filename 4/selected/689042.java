package gui.windows;

import gui.Resources;
import gui.channels.Channel;
import gui.channels.ChannelGroup;
import gui.channels.ChannelPrivate;
import gui.channels.ChannelPublic;
import gui.components.Image;
import gui.components.TabIcon;
import gui.components.list.FriendListRenderer;
import gui.components.list.GroupListRenderer;
import gui.thirdparty.SortedListModel;
import gui.thirdparty.SortedListModel.SortOrder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import cc.slx.java.string.StringHelper;
import models.requests.RequestAvatarTrackingSetState;
import models.requests.RequestFriendshipTermination;
import models.requests.RequestGroupLeave;
import models.requests.RequestImageSetState;
import models.requests.RequestTeleportHome;
import models.requests.RequestTypingStatusChange;
import models.secondlife.Agent;
import models.secondlife.Group;
import whisper.Observable;
import whisper.Observer;
import whisper.Whisper;

/**
 * This is the main window of {@link Whisper}
 * 
 * @author Thomas Pedley
 */
public class MainWindow extends JFrame implements Observer {

    /** The serialisation UID. */
    private static final long serialVersionUID = -8370375647824723197L;

    /** The content panel. */
    private JPanel jContentPane = null;

    /** The tabbed pane for displaying {@link Channel} object. */
    private JTabbedPane jtpChannels = null;

    /** The list of friends of the logged in agent. */
    private JList jlFriends = null;

    /** The list of groups to which the logged in agent is a member. */
    private JList jlGroups = null;

    /** The scroll pane for the list of friends. */
    private JScrollPane jspFriends = null;

    /** The scroll pane for the list of groups. */
    private JScrollPane jspGroups = null;

    /** The tabbed pane for displaying the list of friends and groups. */
    private JTabbedPane jtpFriendsGroups = null;

    /** The text field for sending text. */
    private JTextField jtfInput = null;

    /** The button for sending the text field text. */
    private JButton jbSend = null;

    /** The toolbar. */
    private JToolBar jtMainBar = null;

    /** The exit button. */
    private JButton jbExit = null;

    /** The login button. */
    private JButton jbLogin = null;

    /** The logout button. */
    private JButton jbLogout = null;

    /** The teleport home button. */
    private JButton jbTeleportHome = null;

    /** The tracking state button. */
    private JButton jbTracking = null;

    /** The image state button. */
    private JButton jbImage = null;

    /** The add friend button. */
    private JButton jbFriendAdd = null;

    /** The remove friend button. */
    private JButton jbFriendRemove = null;

    /** The add group button. */
    private JButton jbGroupAdd = null;

    /** The remove group button. */
    private JButton jbGroupRemove = null;

    /** The security image (denotes whether we're secure or not). */
    private Image securityImage = null;

    /** The map of active channels mapped to name. */
    private HashMap<String, Channel> channels = new HashMap<String, Channel>();

    /** The map of active channels mapped to UUID. */
    private HashMap<UUID, Channel> channelsUUIDs = new HashMap<UUID, Channel>();

    /** The public channel. */
    private ChannelPublic publicChannel;

    /** The friend search dialog. */
    private AvatarSearchDialog friendSearchDialog;

    /** The friend search dialog. */
    private GroupSearchDialog groupSearchDialog;

    /** The currently typed word. */
    private String currentTypedWord = null;

    /** The currently completed name. */
    private String currentCompletedName = null;

    /** The inserted completed name. */
    private String insertedCompletedName = null;

    /** The index of the avatar whose name was last completed. */
    private int lastCompletedIndex = 0;

    /** Flag to indicate whether a completion was done at the start of a line. */
    private boolean wasStartOfLine = false;

    /**
	 * This is the default constructor
	 */
    public MainWindow() {
        super();
        initialize();
        Whisper.getClient().addObserver(this);
        this.setIconImage(Resources.IMAGE_WINDOW_ICON);
    }

    /**
	 * Initialise the window.
	 */
    private void initialize() {
        this.setSize(700, 500);
        this.setMinimumSize(new Dimension(700, 500));
        this.setContentPane(getJContentPane());
        this.setTitle("Whisper");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        this.addWindowFocusListener(new WindowFocusListener() {

            /**
			 * Called when focus is lost.
			 * 
			 * @param e The FocusEvent.
			 */
            @Override
            public void windowLostFocus(WindowEvent e) {
                Whisper.getClient().setFocusState(false);
            }

            /**
			 * Called when focus is gained.
			 * 
			 * @param e The FocusEvent.
			 */
            @Override
            public void windowGainedFocus(WindowEvent e) {
                Whisper.getClient().setFocusState(true);
                Whisper.getClient().setTrayIcon(Resources.IMAGE_WINDOW_ICON, true);
            }
        });
    }

    /**
	 * Get the content pane. If it has not been initialised, it is initialised upon first call.
	 * 
	 * @return The content pane.
	 */
    private JPanel getJContentPane() {
        if (jContentPane == null) {
            jContentPane = new JPanel();
            jContentPane.setLayout(new BorderLayout());
            jContentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            JPanel jpCenterSouthPanel = new JPanel();
            jpCenterSouthPanel.setLayout(new BorderLayout());
            jpCenterSouthPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 10, 5));
            JPanel jpSecurityImage = new JPanel(new FlowLayout());
            jpSecurityImage.add(getSecurityImage());
            jpCenterSouthPanel.add(jpSecurityImage, BorderLayout.WEST);
            jpCenterSouthPanel.add(getJtfInput(), BorderLayout.CENTER);
            jpCenterSouthPanel.add(getJbSend(), BorderLayout.EAST);
            JPanel jpCenter = new JPanel(new BorderLayout());
            jpCenter.add(getJtpChannels(), BorderLayout.CENTER);
            jpCenter.add(jpCenterSouthPanel, BorderLayout.SOUTH);
            jContentPane.add(jpCenter, BorderLayout.CENTER);
            jContentPane.add(getJtpFriendsGroups(), BorderLayout.WEST);
            jContentPane.add(getJtMainBar(), BorderLayout.PAGE_START);
        }
        return jContentPane;
    }

    /**
	 * Get the security image. If it has not been initialised, it is initialised upon first call.
	 * 
	 * @return The security image.
	 */
    private Image getSecurityImage() {
        if (securityImage == null) {
            securityImage = new Image(Whisper.getClient().getConnection().getIsEncrypted() ? Resources.IMAGE_SECURE : Resources.IMAGE_INSECURE);
            securityImage.setSize(new Dimension(16, 16));
            securityImage.setPreferredSize(new Dimension(16, 16));
            securityImage.setToolTipText(Whisper.getClient().getConnection().getIsEncrypted() ? "Secure" : "Insecure");
        }
        return securityImage;
    }

    /**
	 * Update the security image.
	 * 
	 * @param secure True if secure, false if insecure.
	 */
    public void updateSecurityImage(boolean secure) {
        getSecurityImage().setImage(secure ? Resources.IMAGE_SECURE : Resources.IMAGE_INSECURE);
        getSecurityImage().setToolTipText(Whisper.getClient().getConnection().getIsEncrypted() ? "Secure" : "Insecure");
        getSecurityImage().repaint();
    }

    /**
	 * Get the tool bar. If it has not been initialised, it is initialised upon first call.
	 * 
	 * @return The tool bar.
	 */
    private JToolBar getJtMainBar() {
        if (jtMainBar == null) {
            jtMainBar = new JToolBar();
            jtMainBar.setFloatable(false);
            jtMainBar.add(getJbExit());
            jtMainBar.add(getJbLogin());
            jtMainBar.add(getJbLogout());
            jtMainBar.add(getJbTeleportHome());
            jtMainBar.add(getJbTracking());
            jtMainBar.add(getJbImage());
        }
        return jtMainBar;
    }

    /**
	 * Get the exit button. If it has not been initialised, it is initialised upon first call.
	 * 
	 * @return The exit button.
	 */
    private JButton getJbExit() {
        if (jbExit == null) {
            jbExit = new JButton("Exit");
            jbExit.addActionListener(new ActionListener() {

                /**
				 * Called when an action is performed.
				 * 
				 * @param e The ActionEvent.
				 */
                @Override
                public void actionPerformed(ActionEvent e) {
                    Whisper.exit(0);
                }
            });
            jbExit.setIcon(Resources.ICON_BUTTON_EXIT);
        }
        return jbExit;
    }

    /**
	 * Get the login button. If it has not been initialised, it is initialised upon first call.
	 * 
	 * @return The logout button.
	 */
    private JButton getJbLogin() {
        if (jbLogin == null) {
            jbLogin = new JButton("Login");
            jbLogin.addActionListener(new ActionListener() {

                /**
				 * Called when an action is performed.
				 * 
				 * @param e The ActionEvent.
				 */
                @Override
                public void actionPerformed(ActionEvent e) {
                    Whisper.getClient().disconnect(false);
                    Whisper.getClient().showLoginWindow();
                }
            });
            jbLogin.setIcon(Resources.ICON_BUTTON_LOGIN);
        }
        return jbLogin;
    }

    /**
	 * Get the logout button. If it has not been initialised, it is initialised upon first call.
	 * 
	 * @return The logout button.
	 */
    private JButton getJbLogout() {
        if (jbLogout == null) {
            jbLogout = new JButton("Logout");
            jbLogout.addActionListener(new ActionListener() {

                /**
				 * Called when an action is performed.
				 * 
				 * @param e The ActionEvent.
				 */
                @Override
                public void actionPerformed(ActionEvent e) {
                    Whisper.getClient().setRequestedLogout();
                    Whisper.getClient().disconnect(false);
                    getJbLogout().setEnabled(false);
                    getJbLogin().setEnabled(true);
                    getJbTracking().setEnabled(false);
                    getJbImage().setEnabled(false);
                    getJbTeleportHome().setEnabled(false);
                }
            });
            jbLogout.setIcon(Resources.ICON_BUTTON_LOGOUT);
        }
        return jbLogout;
    }

    /**
	 * Get the tracking state button. If it has not been initialised, it is initialised upon first call.
	 * 
	 * @return The tracking state button.
	 */
    private JButton getJbTracking() {
        if (jbTracking == null) {
            jbTracking = new JButton("Tracking");
            jbTracking.addActionListener(new ActionListener() {

                /**
				 * Called when an action is performed.
				 * 
				 * @param e The ActionEvent.
				 */
                @Override
                public void actionPerformed(ActionEvent e) {
                    Whisper.getClient().setAvatarTrackingState(!Whisper.getClient().getTrackingState());
                    new RequestAvatarTrackingSetState(Whisper.getClient().getConnection(), Whisper.getClient().getTrackingState()).execute();
                    getJbTracking().setEnabled(false);
                    if (Whisper.getClient().getTrackingState()) {
                        getJbTracking().setIcon(Resources.ICON_TRACKING_ON);
                    } else {
                        getJbTracking().setIcon(Resources.ICON_TRACKING_OFF);
                    }
                }
            });
            jbTracking.setIcon(Resources.ICON_TRACKING_ON);
        }
        return jbTracking;
    }

    /**
	 * Get the image state button. If it has not been initialised, it is initialised upon first call.
	 * 
	 * @return The image state button.
	 */
    private JButton getJbImage() {
        if (jbImage == null) {
            jbImage = new JButton("Images");
            jbImage.addActionListener(new ActionListener() {

                /**
				 * Called when an action is performed.
				 * 
				 * @param e The ActionEvent.
				 */
                @Override
                public void actionPerformed(ActionEvent e) {
                    Whisper.getClient().setImageState(!Whisper.getClient().getImageState());
                    new RequestImageSetState(Whisper.getClient().getConnection(), Whisper.getClient().getImageState()).execute();
                    getJbImage().setEnabled(false);
                    if (Whisper.getClient().getImageState()) {
                        getJbImage().setIcon(Resources.ICON_IMAGES_ON);
                    } else {
                        getJbImage().setIcon(Resources.ICON_IMAGES_OFF);
                    }
                }
            });
            jbImage.setIcon(Resources.ICON_IMAGES_ON);
        }
        return jbImage;
    }

    /**
	 * Get the teleport home button. If it has not been initialised, it is initialised upon first call.
	 * 
	 * @return The teleport home button.
	 */
    private JButton getJbTeleportHome() {
        if (jbTeleportHome == null) {
            jbTeleportHome = new JButton("Teleport Home");
            jbTeleportHome.addActionListener(new ActionListener() {

                /**
				 * Called when an action is performed.
				 * 
				 * @param e The ActionEvent.
				 */
                @Override
                public void actionPerformed(ActionEvent e) {
                    new RequestTeleportHome(Whisper.getClient().getConnection()).execute();
                }
            });
            jbTeleportHome.setIcon(Resources.ICON_TELEPORT_HOME);
        }
        return jbTeleportHome;
    }

    /**
	 * Get the input text field. If it has not been initialised, it is initialised upon first call.
	 * 
	 * @return The input text field.
	 */
    private JTextField getJtfInput() {
        if (jtfInput == null) {
            jtfInput = new JTextField();
            jtfInput.setFocusTraversalKeysEnabled(false);
            jtfInput.setPreferredSize(new Dimension(200, 0));
            jtfInput.addKeyListener(new KeyAdapter() {

                /**
				 * Called when a key is typed.
				 * 
				 * @param e The KeyEvent.
				 */
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        sendMessage();
                    } else if (e.getKeyCode() == KeyEvent.VK_TAB) {
                        tabCompleteName();
                    } else {
                        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                            if (currentCompletedName != null) {
                                getJtfInput().setSelectionStart(getJtfInput().getText().length());
                                getJtfInput().setSelectionEnd(getJtfInput().getText().length());
                            }
                        }
                        triggerTyping();
                        resetTabCompletion();
                    }
                }
            });
            jtfInput.addFocusListener(new FocusAdapter() {

                /**
				 * Called when the focus is gained.
				 * 
				 * @param e The FocusEvent.
				 */
                @Override
                public void focusGained(FocusEvent e) {
                    jtfInput.setSelectionStart(jtfInput.getSelectionEnd());
                    jtfInput.setSelectionEnd(jtfInput.getSelectionEnd());
                }
            });
        }
        return jtfInput;
    }

    /**
	 * Get the send button button. If it has not been initialised, it is initialised upon first call.
	 * 
	 * @return The send button.
	 */
    private JButton getJbSend() {
        if (jbSend == null) {
            jbSend = new JButton();
            jbSend.setText("Send");
            jbSend.addActionListener(new ActionListener() {

                /**
				 * Called when an action is performed on the button.
				 * 
				 * @param e The action events.
				 */
                @Override
                public void actionPerformed(ActionEvent e) {
                    sendMessage();
                    resetTabCompletion();
                }
            });
        }
        return jbSend;
    }

    /**
	 * Get the channels tab pane. If it has not been initialised, it is initialised upon first call.
	 * 	
	 * @return The channels tab pane.
	 */
    private JTabbedPane getJtpChannels() {
        if (jtpChannels == null) {
            jtpChannels = new JTabbedPane();
            jtpChannels.addChangeListener(new ChangeListener() {

                /**
				 * Called when the state of the tab picker changes.
				 * 
				 * @param e The ChangeEvent.
				 */
                @Override
                public void stateChanged(ChangeEvent e) {
                    MainWindow.this.setInputState(((Channel) getJtpChannels().getSelectedComponent()).acceptsInput() && Whisper.getClient().getConnection().getActive());
                    Channel selectedChannel = (Channel) getJtpChannels().getSelectedComponent();
                    if (selectedChannel instanceof ChannelPublic) {
                        setTabIconForChannel(selectedChannel, Whisper.getClient().getLoginStatus() ? Resources.ICON_ONLINE : Resources.ICON_OFFLINE, false);
                    }
                    UUID selectedUUID = selectedChannel.getUUID();
                    if (selectedUUID != null) {
                        Agent friend = Whisper.getClient().getFriends().get(selectedUUID);
                        if (friend != null) {
                            friend.clearPendingMessages();
                            setTabIconForChannel(selectedChannel, friend.getTyping() ? Resources.ICON_TYPING : friend.getOnline() ? Resources.ICON_ONLINE : Resources.ICON_OFFLINE, false);
                        }
                        Agent nearbyAgent = Whisper.getClient().getNearbyAgents().get(selectedUUID);
                        if (nearbyAgent != null && nearbyAgent.getPresent()) {
                            setTabIconForChannel(selectedChannel, Resources.ICON_ONLINE, false);
                        } else {
                            nearbyAgent = null;
                        }
                        Group group = Whisper.getClient().getGroups().get(selectedUUID);
                        if (group != null) {
                            group.clearPendingMessages();
                            setTabIconForChannel(selectedChannel, Resources.ICON_ONLINE, false);
                            ((SortedListModel) getJlGroups().getModel()).forceUpdate();
                        }
                        if (!Whisper.getClient().getLoginStatus()) {
                            setTabIconForChannel(selectedChannel, Resources.ICON_OFFLINE, false);
                        } else if (group == null && friend == null && nearbyAgent == null) {
                            setTabIconForChannel(selectedChannel, Resources.ICON_UNKNOWN_STATUS, false);
                        }
                    }
                }
            });
            jtpChannels.addMouseListener(new MouseAdapter() {

                /**
				 * Handle the mouse clicking the control.
				 * 
				 * @param e The MouseEvent.
				 */
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (getJtpChannels().getSelectedIndex() < 0) return;
                    TabIcon icon = (TabIcon) getJtpChannels().getIconAt(getJtpChannels().getSelectedIndex());
                    if (icon == null) return;
                    Rectangle rect = icon.getBounds();
                    if (rect.contains(e.getX(), e.getY())) {
                        Channel c = (Channel) getJtpChannels().getSelectedComponent();
                        if (!c.canClose()) return;
                        removeChannel(c);
                        getJtpChannels().removeTabAt(getJtpChannels().getSelectedIndex());
                    }
                }
            });
        }
        return jtpChannels;
    }

    /**
	 * Indicate whether the selected channel accepts input or not.
	 * 
	 * @param acceptsInput True if the channel accepts input, false if not.
	 */
    protected void setInputState(boolean acceptsInput) {
        getJtfInput().setEnabled(acceptsInput);
        getJbSend().setEnabled(acceptsInput);
    }

    /**
	 * Get the friends and groups tab pane. If it has not been initialised, it is initialised upon first call.
	 * 
	 * @return The friends and groups tab pane.
	 */
    private JTabbedPane getJtpFriendsGroups() {
        if (jtpFriendsGroups == null) {
            jtpFriendsGroups = new JTabbedPane();
            JPanel jpFriends = new JPanel(new BorderLayout());
            jpFriends.add(getJspFriends(), BorderLayout.CENTER);
            JPanel jpFriendsButtons = new JPanel(new GridLayout());
            jpFriendsButtons.add(getJbFriendAdd());
            jpFriendsButtons.add(getJbFriendRemove());
            jpFriends.add(jpFriendsButtons, BorderLayout.SOUTH);
            JPanel jpGroups = new JPanel(new BorderLayout());
            jpGroups.add(getJspGroups(), BorderLayout.CENTER);
            JPanel jpGroupsButtons = new JPanel(new GridLayout());
            jpGroupsButtons.add(getJbGroupAdd());
            jpGroupsButtons.add(getJbGroupRemove());
            jpGroups.add(jpGroupsButtons, BorderLayout.SOUTH);
            jtpFriendsGroups.addTab("Friends", jpFriends);
            jtpFriendsGroups.addTab("Groups", jpGroups);
        }
        return jtpFriendsGroups;
    }

    /**
	 * Get the add friend button. If it has not been initialised, it is initialised upon first call.
	 * 
	 * @return The add friend button.
	 */
    private JButton getJbFriendAdd() {
        if (jbFriendAdd == null) {
            jbFriendAdd = new JButton("Add");
            jbFriendAdd.addActionListener(new ActionListener() {

                /**
				 * Called when an action is performed.
				 * 
				 * @param e The ActionEvent.
				 */
                @Override
                public void actionPerformed(ActionEvent e) {
                    friendSearchDialog = new AvatarSearchDialog(MainWindow.this, "Friend Search");
                    friendSearchDialog.setVisible(true);
                }
            });
        }
        return jbFriendAdd;
    }

    /**
	 * Get the remove friend button. If it has not been initialised, it is initialised upon first call.
	 * 
	 * @return The remove friend button.
	 */
    private JButton getJbFriendRemove() {
        if (jbFriendRemove == null) {
            jbFriendRemove = new JButton("Remove");
            jbFriendRemove.addActionListener(new ActionListener() {

                /**
				 * Called when an action is received.
				 * 
				 * @param e The ActionEvent.
				 */
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (getJlFriends().getSelectedIndex() >= 0) {
                        Agent selectedAgent = (Agent) getJlFriends().getSelectedValue();
                        new RequestFriendshipTermination(Whisper.getClient().getConnection(), selectedAgent.getUUID()).execute();
                    }
                }
            });
        }
        return jbFriendRemove;
    }

    /**
	 * Get the add group button. If it has not been initialised, it is initialised upon first call.
	 * 
	 * @return The add group button.
	 */
    private JButton getJbGroupAdd() {
        if (jbGroupAdd == null) {
            jbGroupAdd = new JButton("Add");
            jbGroupAdd.addActionListener(new ActionListener() {

                /**
				 * Called when an action is performed.
				 * 
				 * @param e The ActionEvent.
				 */
                @Override
                public void actionPerformed(ActionEvent e) {
                    groupSearchDialog = new GroupSearchDialog(MainWindow.this, "Group Search");
                    groupSearchDialog.setVisible(true);
                }
            });
        }
        return jbGroupAdd;
    }

    /**
	 * Get the remove group button. If it has not been initialised, it is initialised upon first call.
	 * 
	 * @return The remove group button.
	 */
    private JButton getJbGroupRemove() {
        if (jbGroupRemove == null) {
            jbGroupRemove = new JButton("Remove");
            jbGroupRemove.addActionListener(new ActionListener() {

                /**
				 * Called when an action is received.
				 * 
				 * @param e The ActionEvent.
				 */
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (getJlGroups().getSelectedIndex() >= 0) {
                        Group selectedGroup = (Group) getJlGroups().getSelectedValue();
                        new RequestGroupLeave(Whisper.getClient().getConnection(), selectedGroup.getUUID()).execute();
                    }
                }
            });
        }
        return jbGroupRemove;
    }

    /**
	 * Get the friends scroll pane. If it has not been initialised, it is initialised upon first call.
	 * 
	 * @return The friends scroll pane.
	 */
    private JScrollPane getJspFriends() {
        if (jspFriends == null) {
            jspFriends = new JScrollPane();
            jspFriends.setPreferredSize(new Dimension(200, 0));
            jspFriends.getViewport().add(getJlFriends());
        }
        return jspFriends;
    }

    /**
	 * Get the groups scroll pane. If it has not been initialised, it is initialised upon first call.
	 * 
	 * @return The groups scroll pane.
	 */
    private JScrollPane getJspGroups() {
        if (jspGroups == null) {
            jspGroups = new JScrollPane();
            jspGroups.setPreferredSize(new Dimension(200, 0));
            jspGroups.getViewport().add(getJlGroups());
        }
        return jspGroups;
    }

    /**
	 * Get the friends list. If it has not been initialised, it is initialised upon first call.
	 * 
	 * @return The friends list.
	 */
    private JList getJlFriends() {
        if (jlFriends == null) {
            jlFriends = new JList(new SortedListModel(new DefaultListModel(), SortOrder.ASCENDING, Agent.comparatorOnline));
            jlFriends.addMouseListener(new MouseAdapter() {

                /**
				 * Handle the mouse clicking the control.
				 * 
				 * @param e The MouseEvent.
				 */
                @Override
                public void mouseClicked(MouseEvent e) {
                    getJlFriends().setSelectedIndex(getJlFriends().locationToIndex(e.getPoint()));
                    if (getJlFriends().getSelectedIndex() >= 0) {
                        if (SwingUtilities.isLeftMouseButton(e)) {
                            if (e.getClickCount() >= 2) {
                                Agent tmpAgent = (Agent) getJlFriends().getSelectedValue();
                                if (tmpAgent.isResolved()) {
                                    ChannelPrivate c = new ChannelPrivate(tmpAgent.getName(), MainWindow.this, true);
                                    c.setUUID(tmpAgent.getUUID());
                                    addChannel(c, true);
                                    focusTextInput((char) 0);
                                }
                            }
                        } else if (SwingUtilities.isRightMouseButton(e)) {
                            AgentPopupMenu apm = new AgentPopupMenu((Agent) (getJlFriends().getSelectedValue()));
                            apm.show(getJlFriends(), e.getX(), e.getY());
                        }
                    }
                }
            });
            jlFriends.setCellRenderer(new FriendListRenderer());
            jlFriends.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }
        return jlFriends;
    }

    /**
	 * Send the current input text to the currently selected channel.
	 */
    private void sendMessage() {
        String message = getJtfInput().getText();
        getJtfInput().setText("");
        Channel c = (Channel) jtpChannels.getSelectedComponent();
        if (message.trim().toLowerCase().equals("/c") || message.trim().toLowerCase().equals("/clear")) {
            c.clearText();
            return;
        }
        if (message.trim().toLowerCase().equals("/wc") || message.trim().toLowerCase().equals("/close")) {
            closeChannel(c.getUUID());
            return;
        }
        c.sendMessage(message);
        if (c instanceof ChannelPrivate) {
            new RequestTypingStatusChange(Whisper.getClient().getConnection(), c.getUUID(), false).execute();
        }
    }

    /**
	 * Alert the current channel that it is being typed into.
	 */
    private void triggerTyping() {
        Channel c = (Channel) jtpChannels.getSelectedComponent();
        c.triggerTyping();
    }

    /**
	 * Add a friend.
	 * 
	 * @param friend The friend to add.
	 */
    private void addFriend(Agent friend) {
        synchronized (getJlFriends()) {
            DefaultListModel m = ((SortedListModel) getJlFriends().getModel()).getUnsortedList();
            m.add(m.size(), friend);
            friend.addObserver(this);
        }
    }

    /**
	 * Clear the list of friends.
	 */
    private void clearFriends() {
        synchronized (getJlFriends()) {
            DefaultListModel m = ((SortedListModel) getJlFriends().getModel()).getUnsortedList();
            for (int i = 0; i < m.getSize(); i++) {
                Agent tmpAgent = (Agent) m.get(i);
                tmpAgent.removeObserver(this);
            }
            m.clear();
        }
    }

    /**
	 * Determine whether the passed in friend is already listed.
	 * 
	 * @param friend The friend.
	 * @return True if already listed, false if not.
	 */
    private boolean containsFriend(Agent friend) {
        synchronized (jlFriends) {
            DefaultListModel m = ((SortedListModel) jlFriends.getModel()).getUnsortedList();
            return m.contains(friend);
        }
    }

    /**
	 * Remove a friend.
	 * 
	 * @param friend The friend to remove.
	 */
    private void removeFriend(Agent friend) {
        synchronized (getJlFriends()) {
            DefaultListModel m = ((SortedListModel) getJlFriends().getModel()).getUnsortedList();
            for (int i = 0; i < m.getSize(); i++) {
                Agent listedFriend = (Agent) m.get(i);
                if (listedFriend.equals(friend)) {
                    m.remove(i);
                    listedFriend.removeObserver(this);
                    return;
                }
            }
        }
    }

    /**
	 * Get the group list. If it has not been initialised, it is initialised upon first call.
	 * 
	 * @return The group list.
	 */
    private JList getJlGroups() {
        if (jlGroups == null) {
            jlGroups = new JList(new SortedListModel(new DefaultListModel(), SortOrder.ASCENDING, Group.comparator));
            jlGroups.addMouseListener(new MouseAdapter() {

                /**
				 * Handle the mouse clicking the control.
				 * 
				 * @param e The MouseEvent.
				 */
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (getJlGroups().getSelectedIndex() >= 0) {
                        if (e.getClickCount() >= 2) {
                            Group tmpGroup = (Group) getJlGroups().getSelectedValue();
                            if (tmpGroup != null && tmpGroup.chatIsJoined() && tmpGroup.isResolved()) {
                                ChannelGroup c = new ChannelGroup(tmpGroup.getName(), MainWindow.this, true);
                                c.setUUID(tmpGroup.getUUID());
                                addChannel(c, true);
                                focusTextInput((char) 0);
                                tmpGroup.addObserver(c);
                                tmpGroup.updateObservers(false);
                            }
                        }
                    }
                }
            });
        }
        jlGroups.setCellRenderer(new GroupListRenderer());
        jlGroups.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        return jlGroups;
    }

    /**
	 * Add a group.
	 * 
	 * @param group The name of the group to add.
	 */
    private void addGroup(Group group) {
        synchronized (getJlGroups()) {
            DefaultListModel m = ((SortedListModel) getJlGroups().getModel()).getUnsortedList();
            m.add(m.size(), group);
        }
    }

    /**
	 * Determine whether the passed in group is already listed.
	 * 
	 * @param group The group.
	 * @return True if already listed, false if not.
	 */
    private boolean containsGroup(Group group) {
        synchronized (getJlGroups()) {
            DefaultListModel m = ((SortedListModel) getJlGroups().getModel()).getUnsortedList();
            return m.contains(group);
        }
    }

    /**
	 * Remove a group.
	 * 
	 * @param group The friend to get.
	 */
    private void removeGroup(Group group) {
        synchronized (getJlGroups()) {
            DefaultListModel m = ((SortedListModel) getJlGroups().getModel()).getUnsortedList();
            for (int i = 0; i < m.getSize(); i++) {
                if (m.get(i).equals(group)) {
                    m.remove(i);
                    return;
                }
            }
        }
    }

    /**
	 * Clear the list of groups.
	 */
    private void clearGroups() {
        synchronized (getJlGroups()) {
            DefaultListModel m = ((SortedListModel) getJlGroups().getModel()).getUnsortedList();
            m.clear();
        }
    }

    /**
	 * Add a channel.
	 * 
	 * @param channel The channel to add.
	 * @param focus True to focus the channel, false not to.
	 */
    public void addChannel(final Channel channel, boolean focus) {
        synchronized (channels) {
            if (channel instanceof ChannelPrivate) {
                if (!channels.containsKey(channel.getName())) {
                    getJtpChannels().addTab(channel.getName(), (ChannelPrivate) channel);
                    if (channel.getUUID() != null) {
                        Agent tmpAgent = Whisper.getClient().getFriends().get(channel.getUUID());
                        if (tmpAgent != null) {
                            setTabIconForChannel(channel, tmpAgent.getOnline() ? Resources.ICON_ONLINE : Resources.ICON_OFFLINE, false);
                        } else {
                            if (Whisper.getClient().getNearbyAgents().containsKey(channel.getUUID())) {
                                tmpAgent = Whisper.getClient().getNearbyAgents().get(channel.getUUID());
                                if (tmpAgent != null && tmpAgent.getPresent()) {
                                    setTabIconForChannel(channel, Resources.ICON_ONLINE, false);
                                }
                            } else {
                                setTabIconForChannel(channel, Resources.ICON_UNKNOWN_STATUS, false);
                            }
                        }
                    } else {
                        setTabIconForChannel(channel, Resources.ICON_OFFLINE, false);
                    }
                    channels.put(channel.getName(), channel);
                }
            } else if (channel instanceof ChannelPublic) {
                if (this.publicChannel == null) {
                    getJtpChannels().addTab(channel.getName(), (ChannelPublic) channel);
                    setTabIconForChannel(channel, Resources.ICON_ONLINE, false);
                    this.publicChannel = (ChannelPublic) channel;
                }
            } else if (channel instanceof ChannelGroup) {
                if (!channels.containsKey(channel.getName())) {
                    getJtpChannels().addTab(channel.getName(), (ChannelGroup) channel);
                    setTabIconForChannel(channel, Resources.ICON_ONLINE, false);
                    channels.put(channel.getName(), channel);
                }
            }
            if (channel.getUUID() != null) {
                if (!channelsUUIDs.containsKey(channel.getUUID())) {
                    channelsUUIDs.put(channel.getUUID(), channel);
                }
            }
            if (focus) {
                if (!SwingUtilities.isEventDispatchThread()) {
                    try {
                        SwingUtilities.invokeAndWait(new Runnable() {

                            /**
							 * Called when the thread is run.
							 */
                            @Override
                            public void run() {
                                getJtpChannels().setSelectedComponent((Component) channels.get(channel.getName()));
                            }
                        });
                    } catch (Exception ex) {
                    }
                } else {
                    getJtpChannels().setSelectedComponent((Component) channels.get(channel.getName()));
                }
            }
        }
    }

    /**
	 * Remove a channel.
	 * 
	 * @param channel The channel to remove.
	 */
    public void removeChannel(Channel channel) {
        synchronized (channels) {
            if (channels.containsKey(channel.getName())) {
                channels.remove(channel.getName());
            }
            if (channelsUUIDs.containsKey(channel.getUUID())) {
                channelsUUIDs.remove(channel.getUUID());
            }
        }
    }

    /**
	 * Get a channel from the list of currently active channels.
	 * 
	 * @param name The channel name (case sensitive).
	 * @return The channel, null if not found.
	 */
    public Channel getChannel(String name) {
        return channels.get(name);
    }

    /**
	 * Get a channel from the list of currently active channels.
	 * 
	 * @param UUID The channel UUID.
	 * @return The channel, null if not found.
	 */
    public Channel getChannel(UUID UUID) {
        return channelsUUIDs.get(UUID);
    }

    /**
	 * Receive an update from the {@link Observable} object.
	 * 
	 * @param purgeFirst True indicates that the friends and groups should be purged before updating.
	 */
    @Override
    public void updateObserver(final boolean purgeFirst) {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    /**
					 * Called when the thread is run.
					 */
                    @Override
                    public void run() {
                        updateObserver(purgeFirst);
                    }
                });
            } catch (Exception e) {
                if (Whisper.isDebugging()) {
                    e.printStackTrace();
                }
            }
        } else {
            if (purgeFirst) {
                clearFriends();
                clearGroups();
            }
            for (Agent tmpFriend : Whisper.getClient().getFriends().values()) {
                if (!containsFriend(tmpFriend)) {
                    addFriend(tmpFriend);
                }
            }
            ArrayList<Agent> friendsToRemove = new ArrayList<Agent>();
            SortedListModel slm = (SortedListModel) getJlFriends().getModel();
            DefaultListModel dlm = slm.getUnsortedList();
            for (int i = 0; i < getJlFriends().getModel().getSize(); i++) {
                boolean found = false;
                for (Agent tmpFriend : Whisper.getClient().getFriends().values()) {
                    if (tmpFriend.equals(dlm.get(i))) {
                        found = true;
                        break;
                    }
                }
                if (!found) friendsToRemove.add((Agent) dlm.get(i));
            }
            for (Agent tmpAgent : friendsToRemove) {
                removeFriend(tmpAgent);
                tmpAgent.removeObserver(this);
            }
            ((SortedListModel) jlFriends.getModel()).forceUpdate();
            for (Group tmpGroup : Whisper.getClient().getGroups().values()) {
                if (!containsGroup(tmpGroup)) {
                    addGroup(tmpGroup);
                }
            }
            ArrayList<Group> groupsToRemove = new ArrayList<Group>();
            slm = (SortedListModel) getJlGroups().getModel();
            dlm = slm.getUnsortedList();
            for (int i = 0; i < getJlGroups().getModel().getSize(); i++) {
                boolean found = false;
                for (Group tmpGroup : Whisper.getClient().getGroups().values()) {
                    if (tmpGroup.equals(dlm.get(i))) {
                        found = true;
                        break;
                    }
                }
                if (!found) groupsToRemove.add((Group) dlm.get(i));
            }
            for (Group tmpGroup : groupsToRemove) {
                removeGroup(tmpGroup);
                tmpGroup.removeObserver(this);
            }
            ((SortedListModel) jlGroups.getModel()).forceUpdate();
            if (!Whisper.getClient().getConnection().getActive() || !Whisper.getClient().getLoginStatus()) {
                setInputState(false);
            } else {
                Channel c = ((Channel) getJtpChannels().getSelectedComponent());
                if (c != null) {
                    setInputState(c.acceptsInput());
                }
            }
            getJbLogin().setEnabled(!Whisper.getClient().getLoginStatus());
            getJbLogout().setEnabled(Whisper.getClient().getLoginStatus());
            getJbTracking().setEnabled(Whisper.getClient().getLoginStatus() && Whisper.getClient().getAvatarTrackingCanSetState());
            getJbImage().setEnabled(Whisper.getClient().getLoginStatus() && Whisper.getClient().getImageCanSetState());
            getJbTeleportHome().setEnabled(Whisper.getClient().getLoginStatus());
            synchronized (channels) {
                for (Channel channel : channels.values()) {
                    if (channel instanceof ChannelPrivate) {
                        Agent tmpAgent = Whisper.getClient().getFriends().get(channel.getUUID());
                        if (tmpAgent != null) {
                            setTabIconForChannel(channel, tmpAgent.getTyping() ? Resources.ICON_TYPING : tmpAgent.getPendingMessages() > 0 ? Resources.ICON_PENDING_MESSAGES : tmpAgent.getOnline() ? Resources.ICON_ONLINE : Resources.ICON_OFFLINE, false);
                        } else {
                            if (Whisper.getClient().getNearbyAgents().containsKey(channel.getUUID())) {
                                tmpAgent = Whisper.getClient().getNearbyAgents().get(channel.getUUID());
                                if (tmpAgent != null && tmpAgent.getPresent()) {
                                    setTabIconForChannel(channel, Resources.ICON_ONLINE, false);
                                }
                            } else {
                                setTabIconForChannel(channel, Resources.ICON_UNKNOWN_STATUS, false);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
	 * Focus on the text input box.
	 * 
	 * @param c The character to set in the input box upon focus.
	 */
    public void focusTextInput(char c) {
        if (getJtfInput().isEnabled()) {
            getJtfInput().requestFocusInWindow();
            if (!Character.isISOControl(c) && (c >= 32 && c <= 126)) {
                getJtfInput().setText(getJtfInput().getText() + c);
            }
        }
    }

    /**
	 * Update the pending messages count.
	 * 
	 * @param fromUUID The UUID of the message sender.
	 */
    public void updatePendingMessages(UUID fromUUID) {
        Channel c = ((Channel) getJtpChannels().getSelectedComponent());
        if (c.getUUID() == null || !c.getUUID().equals(fromUUID)) {
            Agent friend = Whisper.getClient().getFriends().get(fromUUID);
            if (friend != null) {
                friend.incPendingMessages();
                getJlFriends().repaint();
            }
            Group group = Whisper.getClient().getGroups().get(fromUUID);
            if (group != null) {
                group.incPendingMessages();
                getJlGroups().repaint();
            }
        }
    }

    /**
	 * Repaint the list icons.
	 */
    public void repaintListIcons() {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    /**
					 * Called when the thread is run.
					 */
                    @Override
                    public void run() {
                        repaintListIcons();
                    }
                });
            } catch (Exception e) {
            }
        } else {
            getJlFriends().repaint();
            getJlGroups().repaint();
        }
    }

    /**
	 * Get the public channel.
	 * 
	 * @return The public channel.
	 */
    public ChannelPublic getPublicChannel() {
        return publicChannel;
    }

    /**
	 * Set whether the avatar tracking state may be set by the client.
	 * 
	 * @param canSet True if it may be set, otherwise false.
	 */
    public void setAvatarTrackingCanSetState(boolean canSet) {
        if (canSet) {
            getJbTracking().setEnabled(true);
            if (Whisper.getClient().getTrackingState()) {
                getJbTracking().setIcon(Resources.ICON_TRACKING_ON);
                getPublicChannel().showParticipants(true);
            } else {
                getJbTracking().setIcon(Resources.ICON_TRACKING_OFF);
                getPublicChannel().showParticipants(false);
            }
        } else {
            getJbTracking().setEnabled(false);
            getJbTracking().setIcon(Resources.ICON_TRACKING_DISABLED);
        }
    }

    /**
	 * Set the avatar tracking state
	 * 
	 * @param enabled True if it is enabled, otherwise false.
	 */
    public void setAvatarTrackingState(final boolean enabled) {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    /**
					 * Called when the thread is executed.
					 */
                    @Override
                    public void run() {
                        setAvatarTrackingState(enabled);
                    }
                });
            } catch (Exception ex) {
                if (Whisper.isDebugging()) {
                    ex.printStackTrace();
                }
            }
        } else {
            getJbTracking().setEnabled(Whisper.getClient().getAvatarTrackingCanSetState());
            if (enabled) {
                getJbTracking().setIcon(Resources.ICON_TRACKING_ON);
            } else {
                getJbTracking().setIcon(Resources.ICON_TRACKING_DISABLED);
            }
        }
    }

    /**
	 * Receive an avatar tracking set state response.
	 * 
	 * @param success True if the request was successful, otherwise false.
	 */
    public void receiveAvatarTrackingSetStateResponse(final boolean success) {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    /**
					 * Called when the thread is executed.
					 */
                    @Override
                    public void run() {
                        receiveAvatarTrackingSetStateResponse(success);
                    }
                });
            } catch (Exception ex) {
                if (Whisper.isDebugging()) {
                    ex.printStackTrace();
                }
            }
        } else {
            if (Whisper.getClient().getAvatarTrackingCanSetState()) {
                getJbTracking().setEnabled(true);
                if (!success) {
                    Whisper.getClient().setAvatarTrackingState(!Whisper.getClient().getTrackingState());
                }
                if (Whisper.getClient().getTrackingState()) {
                    getJbTracking().setIcon(Resources.ICON_TRACKING_ON);
                    getPublicChannel().showParticipants(true);
                } else {
                    getJbTracking().setIcon(Resources.ICON_TRACKING_OFF);
                    getPublicChannel().showParticipants(false);
                }
            } else {
                getJbTracking().setEnabled(false);
            }
        }
    }

    /**
	 * Clear all nearby avatars.
	 */
    public void clearNearbyAvatars() {
        getPublicChannel().clearParticipants();
    }

    /**
	 * Get the friend search dialog.
	 * 
	 * @return The friend search dialog.
	 */
    public AvatarSearchDialog getFriendSearchDialog() {
        return friendSearchDialog;
    }

    /**
	 * Get the group search dialog.
	 * 
	 * @return The group search dialog.
	 */
    public GroupSearchDialog getGroupSearchDialog() {
        return groupSearchDialog;
    }

    /**
	 * Close a channel based on its UUID.
	 * 
	 * @param channelUUID The UUID of the channel to close.
	 */
    public void closeChannel(UUID channelUUID) {
        if (channelUUID == null) return;
        synchronized (channels) {
            for (int i = 0; i < getJtpChannels().getTabCount(); i++) {
                Channel c = (Channel) getJtpChannels().getComponentAt(i);
                if (c.getUUID() != null && c.getUUID().equals(channelUUID) && c.canClose()) {
                    removeChannel(c);
                    getJtpChannels().removeTabAt(i);
                    break;
                }
            }
        }
    }

    /**
	 * Set whether the image state may be set by the client.
	 * 
	 * @param canSet True if it may be set, otherwise false.
	 */
    public void setImageCanSetState(boolean canSet) {
        if (canSet) {
            getJbImage().setEnabled(true);
            if (Whisper.getClient().getImageState()) {
                getJbImage().setIcon(Resources.ICON_IMAGES_ON);
            } else {
                getJbImage().setIcon(Resources.ICON_IMAGES_OFF);
            }
        } else {
            getJbImage().setEnabled(false);
            getJbImage().setIcon(Resources.ICON_IMAGES_DISABLED);
        }
    }

    /**
	 * Set the image state
	 * 
	 * @param enabled True if it is enabled, otherwise false.
	 */
    public void setImageState(final boolean enabled) {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    /**
					 * Called when the thread is executed.
					 */
                    @Override
                    public void run() {
                        setImageState(enabled);
                    }
                });
            } catch (Exception ex) {
                if (Whisper.isDebugging()) {
                    ex.printStackTrace();
                }
            }
        } else {
            getJbImage().setEnabled(Whisper.getClient().getImageCanSetState());
            if (enabled) {
                getJbImage().setIcon(Resources.ICON_IMAGES_ON);
            } else {
                getJbImage().setIcon(Resources.ICON_IMAGES_DISABLED);
            }
            getPublicChannel().showMap(enabled);
        }
    }

    /**
	 * Receive an image set state response.
	 * 
	 * @param success True if the request was successful, otherwise false.
	 */
    public void receiveImageSetStateResponse(final boolean success) {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    /**
					 * Called when the thread is executed.
					 */
                    @Override
                    public void run() {
                        receiveImageSetStateResponse(success);
                    }
                });
            } catch (Exception ex) {
                if (Whisper.isDebugging()) {
                    ex.printStackTrace();
                }
            }
        } else {
            if (Whisper.getClient().getImageCanSetState()) {
                getJbImage().setEnabled(true);
                if (!success) {
                    Whisper.getClient().setImageState(!Whisper.getClient().getImageState());
                }
                if (Whisper.getClient().getImageState()) {
                    getJbImage().setIcon(Resources.ICON_IMAGES_ON);
                    getPublicChannel().showMap(true);
                } else {
                    getJbImage().setIcon(Resources.ICON_IMAGES_OFF);
                    getPublicChannel().showMap(false);
                }
            } else {
                getJbImage().setEnabled(false);
            }
        }
    }

    /**
	 * Set the tab icon for the given channel.
	 * 
	 * @param onlyIfNotFocused True to set only if the channel is not focused, false to set regardless.
	 * @param channel The channel.
	 * @param icon The icon.
	 */
    public void setTabIconForChannel(Channel channel, ImageIcon icon, boolean onlyIfNotFocused) {
        if (!onlyIfNotFocused || (onlyIfNotFocused && !getJtpChannels().getSelectedComponent().equals(channel))) {
            for (int i = 0; i < getJtpChannels().getTabCount(); i++) {
                Channel c = (Channel) getJtpChannels().getComponentAt(i);
                if (c.equals(channel)) {
                    TabIcon ti = (TabIcon) getJtpChannels().getIconAt(i);
                    if (ti == null) {
                        ti = new TabIcon(icon);
                        getJtpChannels().setIconAt(i, ti);
                    } else {
                        ti.setIcon(icon);
                        getJtpChannels().repaint();
                    }
                    break;
                }
            }
        }
    }

    /**
	 * Set all channels statuses to disconnected.
	 */
    public void setAllChannelsDisconnected() {
        for (int i = 0; i < getJtpChannels().getTabCount(); i++) {
            Channel channel = (Channel) getJtpChannels().getComponentAt(i);
            TabIcon ti = (TabIcon) getJtpChannels().getIconAt(i);
            if (ti == null || (ti.getIcon() != null && !ti.getIcon().equals(Resources.ICON_PENDING_MESSAGES))) {
                setTabIconForChannel(channel, Resources.ICON_OFFLINE, false);
            }
        }
    }

    /**
	 * Dispose of the {@link MainWindow}.
	 */
    @Override
    public void dispose() {
        if (getPublicChannel() != null) {
            getPublicChannel().dispose();
        }
        super.dispose();
    }

    /**
	 * Reset tab completion.
	 */
    private void resetTabCompletion() {
        currentTypedWord = null;
        currentCompletedName = null;
        insertedCompletedName = null;
        lastCompletedIndex = 0;
        wasStartOfLine = false;
    }

    /**
	 * Called to tab complete a participant's name.
	 */
    private void tabCompleteName() {
        if (currentTypedWord == null) {
            String currentText = getJtfInput().getText();
            int lastSpace = currentText.lastIndexOf(' ');
            if (lastSpace >= 0) {
                currentTypedWord = currentText.substring(lastSpace).trim();
            } else {
                currentTypedWord = currentText;
            }
        }
        ArrayList<String> names = new ArrayList<String>();
        if (getJtpChannels().getSelectedComponent() instanceof ChannelPublic) {
            for (Agent tmpAgent : Whisper.getClient().getNearbyAgents().values()) {
                if (tmpAgent.getName().toLowerCase().equals(Whisper.getClient().getName().toLowerCase())) continue;
                names.add(tmpAgent.getName());
            }
        } else if (getJtpChannels().getSelectedComponent() instanceof ChannelGroup) {
            Group tmpGroup = Whisper.getClient().getGroups().get(((Channel) getJtpChannels().getSelectedComponent()).getUUID());
            if (tmpGroup != null) {
                for (Agent tmpAgent : tmpGroup.getChatParticipants().values()) {
                    if (tmpAgent.getName().toLowerCase().equals(Whisper.getClient().getName().toLowerCase())) continue;
                    names.add(tmpAgent.getName());
                }
            }
        } else if (getJtpChannels().getSelectedComponent() instanceof ChannelPrivate) {
            names.add(((Channel) getJtpChannels().getSelectedComponent()).getName());
        }
        Collections.sort(names, StringHelper.caseInsensitiveComparator);
        int matchCount = 0;
        for (String name : names) {
            if (name.toLowerCase().startsWith(currentTypedWord.toLowerCase())) {
                matchCount++;
            }
        }
        int count = 0;
        for (String name : names) {
            if (name.toLowerCase().startsWith(currentTypedWord.toLowerCase())) {
                if (count < lastCompletedIndex) {
                    count++;
                    continue;
                }
                count++;
                currentCompletedName = name;
                lastCompletedIndex = count;
                break;
            }
        }
        if (lastCompletedIndex >= matchCount && matchCount > 1) {
            lastCompletedIndex = 0;
        }
        if (currentCompletedName != null) {
            String currentText = getJtfInput().getText();
            boolean startOfLine = false;
            if (wasStartOfLine || currentText.trim().length() == currentTypedWord.length()) {
                startOfLine = true;
            }
            int modifier = 0;
            if (wasStartOfLine) modifier = 1;
            if (insertedCompletedName == null) {
                currentText = currentText.substring(0, currentText.length() - (currentTypedWord.length() + modifier));
            } else {
                currentText = currentText.substring(0, currentText.length() - (insertedCompletedName.length() + modifier));
            }
            int selectionStart = currentText.length() + currentTypedWord.length();
            currentText = currentText + currentCompletedName;
            if (startOfLine) {
                currentText += ":";
            }
            getJtfInput().setText(currentText);
            getJtfInput().setSelectionStart(selectionStart);
            getJtfInput().setSelectionEnd(currentText.length());
            insertedCompletedName = currentCompletedName;
            if (startOfLine) {
                wasStartOfLine = true;
            }
        }
    }
}
