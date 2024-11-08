package client.game.ui.chat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyVetoException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import client.communication.GameContext;
import client.communication.exceptions.SendMessageException;
import client.game.hud.HudManager;
import client.game.hud.HudStateType;
import client.game.ui.HudPopUp;
import com.jme.system.DisplaySystem;
import common.elearning.chat.ChatCommands;
import common.messages.requests.MsgChat;

public class ChatClient extends HudPopUp implements ActionListener {

    /** The version of the serialized form of this class. */
    private static final long serialVersionUID = 1L;

    /** The hudid. */
    public static String hudid = "chatClient";

    /** The open channel button. */
    private final JButton openChannelButton;

    /** The status message. */
    private final JLabel statusMessage;

    /** The {@link Charset} encoding for client/server messages. */
    public static final String MESSAGE_CHARSET = "UTF-8";

    /** The channel list. */
    private final MultiList<String> channelList;

    /** The listener for double-clicks on a memberlist. */
    private final MouseListener pmMouseListener;

    /**
	 * A map of channel names to their channel frames, so we can update
	 * membership lists.
	 */
    private final ConcurrentHashMap<String, ChatChannelFrame> channelMap = new ConcurrentHashMap<String, ChatChannelFrame>();

    /**
	 * Creates a new {@code ChatClient}.
	 */
    public ChatClient() {
        super(hudid, HudStateType.WALKING_HUD, HudManager.getInstance());
        pmMouseListener = new PMMouseListener(this);
        Container c = getContentPane();
        c.setLayout(new BorderLayout());
        JPanel buttonPanel = new JPanel();
        JPanel southPanel = new JPanel();
        southPanel.setLayout(new GridLayout(0, 1));
        statusMessage = new JLabel("Status: Not Connected");
        southPanel.add(buttonPanel);
        southPanel.add(statusMessage);
        southPanel.setBackground(new Color(192, 192, 192));
        southPanel.setForeground(Color.black);
        c.add(southPanel, BorderLayout.SOUTH);
        JPanel eastPanel = new JPanel();
        eastPanel.setLayout(new BorderLayout());
        eastPanel.add(new JLabel("Channels"), BorderLayout.NORTH);
        channelList = new MultiList<String>(String.class);
        channelList.addMouseListener(getPMMouseListener());
        eastPanel.add(new JScrollPane(channelList), BorderLayout.CENTER);
        eastPanel.setBackground(new Color(192, 192, 192));
        eastPanel.setForeground(Color.black);
        c.add(eastPanel, BorderLayout.CENTER);
        buttonPanel.setLayout(new GridLayout(1, 0));
        openChannelButton = new JButton("Open Channel");
        openChannelButton.setActionCommand("openChannel");
        openChannelButton.addActionListener(this);
        openChannelButton.setEnabled(false);
        openChannelButton.setToolTipText("Click to join a channel");
        openChannelButton.setBackground(new Color(69, 69, 69));
        openChannelButton.setForeground(new Color(255, 255, 255));
        openChannelButton.setFont(new Font("TimesRoman", Font.BOLD, 14));
        buttonPanel.add(openChannelButton);
        addInternalFrameListener(new QuitWindowListener(this));
        setSize(140, 400);
        setLocation(DisplaySystem.getDisplaySystem().getWidth() - 256, 0);
        this.loggedIn();
        attach();
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    }

    /** The instance. */
    private static ChatClient instance;

    /**
	 * Gets the single instance of ChatClient.
	 * 
	 * @return single instance of ChatClient
	 */
    public static ChatClient getInstance() {
        if (null == instance) {
            instance = new ChatClient();
        }
        return instance;
    }

    /**
	 * Runs a new {@code ChatClient} application.
	 * 
	 * @param args
	 *            the commandline arguments (not used)
	 */
    public static void main(String[] args) {
        ChatClient pepe = new ChatClient();
        pepe.attach();
    }

    /**
	 * Adds the user to channel.
	 * 
	 * @param user
	 *            the user
	 * @param channelName
	 *            the channel name
	 */
    public void addUserToChannel(String user, String channelName) {
        ChatChannelFrame frame = channelMap.get(channelName);
        frame.updateMembers(user);
    }

    /**
	 * Removes the user from channel.
	 * 
	 * @param user
	 *            the user
	 * @param channelName
	 *            the channel name
	 */
    public void removeUserFromChannel(String user, String channelName) {
        ChatChannelFrame frame = channelMap.get(channelName);
        frame.memberLeft(user);
    }

    /**
	 * Adds the channel message.
	 * 
	 * @param channel
	 *            the channel
	 * @param sender
	 *            the sender
	 * @param msg
	 *            the msg
	 */
    public void addChannelMessage(String channel, String sender, String msg) {
        ChatChannelFrame frame = channelMap.get(channel);
        frame.addChannelMsg(sender, msg);
    }

    /**
	 * Adds the channel.
	 * 
	 * @param name
	 *            the name
	 */
    public void addChannel(String name) {
        if ((!name.isEmpty()) && (!name.startsWith(ChatCommands.PM_PREFIX))) {
            List<String> channelList = Arrays.asList(name.split("\\s+"));
            this.channelList.addAllItems(channelList);
            this.channelList.invalidate();
            instance.attach();
            repaint();
        }
    }

    /**
	 * Removes the channel.
	 * 
	 * @param name
	 *            the name
	 */
    public void removeChannel(String name) {
        this.channelList.removeItem(name);
        this.channelList.invalidate();
        repaint();
    }

    /**
	 * Sets the buttons enabled.
	 * 
	 * @param enable
	 *            the new buttons enabled
	 */
    private void setButtonsEnabled(boolean enable) {
        setSessionButtonsEnabled(enable);
    }

    /**
	 * Sets the session buttons enabled.
	 * 
	 * @param enable
	 *            the new session buttons enabled
	 */
    private void setSessionButtonsEnabled(boolean enable) {
        openChannelButton.setEnabled(enable);
    }

    /**
	 * Gets the user input.
	 * 
	 * @param prompt
	 *            the prompt
	 * 
	 * @return the user input
	 */
    public String getUserInput(String prompt) {
        setButtonsEnabled(false);
        try {
            return JOptionPane.showInputDialog(this, prompt);
        } finally {
            setButtonsEnabled(true);
        }
    }

    /**
	 * Show message.
	 * 
	 * @param message
	 *            the message
	 */
    public void showMessage(String message) {
        setButtonsEnabled(false);
        try {
            JOptionPane.showMessageDialog(this, message);
        } finally {
            setButtonsEnabled(true);
        }
    }

    /**
	 * Gets the pM mouse listener.
	 * 
	 * @return the pM mouse listener
	 */
    MouseListener getPMMouseListener() {
        return pmMouseListener;
    }

    /**
	 * Do open channel.
	 */
    private void doOpenChannel() {
        joinChannel(getUserInput("Enter channel name:"));
    }

    /**
	 * Do server message.
	 */
    private void doServerMessage() {
        MsgChat msg = new MsgChat();
        msg.setCommand(ChatCommands.PING);
        msg.setMsg(getUserInput("Enter server message:"));
        try {
            GameContext.getClientCommunication().send(msg);
        } catch (SendMessageException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Do enter channel.
	 */
    private void doEnterChannel() {
        String target = this.channelList.getSelected();
        if (target == null) {
            showMessage("Must select a channel in main Chat panel");
            return;
        }
        ChatChannelFrame vent = this.channelMap.get(target);
        if (vent != null) vent.setVisible(true);
        for (String name : this.channelMap.keySet()) {
            ChatChannelFrame frame = channelMap.get(name);
            try {
                frame.setVisible(true);
                frame.setSelected(true);
            } catch (PropertyVetoException e) {
                e.printStackTrace();
            }
        }
        this.joinChannel(target);
    }

    /**
	 * Join channel.
	 * 
	 * @param channelName
	 *            the channel name
	 */
    public void joinChannel(String channelName) {
        if (channelName == null || channelName.matches("^\\s*$")) {
            showMessage("Channel name must be provided.");
            return;
        }
        channelName = channelName.replaceAll(" ", "_");
        ChatChannelFrame frame = this.channelMap.get(channelName);
        if (frame != null) {
            showMessage("Channel already exists");
            try {
                frame.setSelected(true);
            } catch (PropertyVetoException e) {
            }
            return;
        }
        MsgChat msg = new MsgChat();
        msg.setCommand(ChatCommands.JOIN);
        msg.setMsg(channelName);
        try {
            GameContext.getClientCommunication().send(msg);
        } catch (SendMessageException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Leave channel.
	 * 
	 * @param channel
	 *            the channel
	 */
    void leaveChannel(String channel) {
        ChatChannelFrame frame = this.channelMap.remove(channel);
        frame.deattach();
        MsgChat msg = new MsgChat();
        msg.setCommand(ChatCommands.LEAVE);
        msg.setMsg(channel);
        try {
            GameContext.getClientCommunication().send(msg);
        } catch (SendMessageException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Leave all channels.
	 */
    public void leaveAllChannels() {
        for (String name : this.channelMap.keySet()) {
            ChatChannelFrame frame = channelMap.get(name);
            leaveChannel(frame.getChannelName());
        }
    }

    /**
	 * User logout.
	 * 
	 * @param idString
	 *            the id string
	 */
    public void userLogout(String idString) {
        for (String name : this.channelMap.keySet()) {
            ChatChannelFrame frame = channelMap.get(name);
            frame.memberLeft(idString);
        }
    }

    /**
	 * {@inheritDoc}
	 */
    public void loggedIn() {
        statusMessage.setText("Status: Connected");
        setButtonsEnabled(true);
    }

    /**
	 * Creates the channel frame.
	 * 
	 * @param channelName
	 *            the channel name
	 */
    public void createChannelFrame(String channelName) {
        ChatChannelFrame cframe = channelMap.get(channelName);
        if (cframe == null) {
            cframe = new ChatChannelFrame(this, channelName);
            cframe.attach();
            if (channelName.equals(ChatCommands.GLOBAL_CHANNEL_NAME)) cframe.setClosable(false);
            channelMap.put(channelName, cframe);
            try {
                cframe.setSelected(true);
            } catch (PropertyVetoException e) {
                e.printStackTrace();
            }
        } else try {
            cframe.attach();
            cframe.setSelected(true);
        } catch (PropertyVetoException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Displays a private message from another client in a popup window.
	 * 
	 * @param message
	 *            the message received
	 */
    public void pmReceived(String message) {
        String[] args = message.split(" ", 3);
        JOptionPane.showMessageDialog(this, args[2], "Message from " + args[0], JOptionPane.INFORMATION_MESSAGE);
    }

    /**
	 * Do quit.
	 */
    public void doQuit() {
        for (String name : this.channelMap.keySet()) {
            ChatChannelFrame frame = channelMap.get(name);
            this.channelMap.remove(frame.getChannelName());
            frame.dispose();
            this.getDesktopPane().remove(frame);
        }
        this.channelList.removeAllItems();
        this.deattach();
        this.dispose();
    }

    /**
	 * {@inheritDoc}
	 */
    public void actionPerformed(ActionEvent action) {
        final String command = action.getActionCommand();
        if (command.equals("openChannel")) {
            doOpenChannel();
        } else if (command.equals("directSend")) {
            doServerMessage();
        } else {
            System.err.format("ChatClient: Error, unknown GUI command [%s]\n", command);
        }
    }

    /**
	 * Listener that brings up a PM send dialog when a {@code MultiList} is
	 * double-clicked.
	 */
    static final class PMMouseListener extends MouseAdapter {

        /** The client. */
        private final ChatClient client;

        /**
		 * Creates a new {@code PMMouseListener} for the given {@code
		 * ChatClient}.
		 * 
		 * @param client
		 *            the client to notify when a double-click should trigger a
		 *            PM dialog.
		 */
        PMMouseListener(ChatClient client) {
            this.client = client;
        }

        /**
		 * {@inheritDoc}
		 * <p>
		 * Brings up a PM send dialog when a double-click is received.
		 */
        @Override
        public void mouseClicked(MouseEvent evt) {
            if (evt.getClickCount() == 2) {
                client.doEnterChannel();
            }
        }
    }

    /**
	 * Listener that quits the {@code ChatClient} when the window is closed.
	 */
    static final class QuitWindowListener extends InternalFrameAdapter {

        /** The client. */
        private final ChatClient client;

        /**
		 * Creates a new {@code QuitWindowListener} for the given {@code
		 * ChatClient}.
		 * 
		 * @param client
		 *            the client to notify on windowClosing
		 */
        QuitWindowListener(ChatClient client) {
            this.client = client;
        }

        /**
		 * (non-Javadoc)
		 * 
		 * 
		 * @see javax.swing.event.InternalFrameAdapter#internalFrameClosing(javax.swing.event.InternalFrameEvent)
		 */
        @Override
        public void internalFrameClosing(InternalFrameEvent e) {
            client.leaveAllChannels();
            client.doQuit();
            client.setVisible(false);
        }
    }
}
