package client.game.ui.chat;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.List;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import client.communication.GameContext;
import client.communication.exceptions.SendMessageException;
import client.game.hud.HudManager;
import client.game.hud.HudStateType;
import client.game.ui.HudPopUp;
import common.elearning.chat.ChatCommands;
import common.messages.requests.MsgChat;

/**
 * ChatChannelFrame presents a GUI so that a user can interact with a channel.
 * The users connected to the channel are displayed in a list on the right side.
 * Messages can be sent on the channel via an input area on the bottom of the
 * left side.
 */
public class ChatChannelFrame extends HudPopUp implements ActionListener {

    /** The version of the serialized form of this class. */
    private static final long serialVersionUID = 1L;

    /** The {@code ChatClient} that is the parent of this frame. */
    private final ChatClient myChatClient;

    /** The {@code MultiList} containing this channel's members. */
    private final MultiList<String> multiList;

    /** The pm mouse listener. */
    private final MouseListener pmMouseListener;

    /** The input field. */
    private final JTextField inputField;

    /** The output area for channel messages. */
    private final JTextArea outputArea;

    /** The channel name. */
    private final String channelName;

    private static String hudid = "ChannelChat";

    /**
	 * Constructs a new {@code ChatChannelFrame} as a wrapper around the given
	 * channel.
	 * 
	 * @param cc
	 *            the parent {@code ChatClient} of this frame.
	 * @param name
	 *            the name
	 */
    public ChatChannelFrame(ChatClient cc, String name) {
        super(hudid + name, HudStateType.WALKING_HUD, HudManager.getInstance());
        this.channelName = name;
        this.setTitle(name);
        pmMouseListener = new PMMouseListener(this);
        myChatClient = cc;
        Container c = getContentPane();
        c.setLayout(new BorderLayout());
        JPanel eastPanel = new JPanel();
        eastPanel.setLayout(new BorderLayout());
        c.add(eastPanel, BorderLayout.EAST);
        eastPanel.add(new JLabel("Users"), BorderLayout.NORTH);
        multiList = new MultiList<String>(String.class);
        multiList.addMouseListener(getPMMouseListener());
        eastPanel.add(new JScrollPane(multiList), BorderLayout.CENTER);
        JPanel southPanel = new JPanel();
        c.add(southPanel, BorderLayout.SOUTH);
        southPanel.setLayout(new GridLayout(1, 0));
        inputField = new JTextField();
        southPanel.add(inputField);
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        c.add(new JScrollPane(outputArea), BorderLayout.CENTER);
        inputField.addActionListener(this);
        setSize(400, 400);
        setClosable(true);
        if (name.equals(ChatCommands.GLOBAL_CHANNEL_NAME)) {
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        } else {
            setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);
        }
        addInternalFrameListener(new FrameClosingListener(this));
        attach();
    }

    @Override
    protected String getHudId() {
        return hudid + channelName;
    }

    /**
	 * Gets the pM mouse listener.
	 * 
	 * @return the pM mouse listener
	 */
    private MouseListener getPMMouseListener() {
        return pmMouseListener;
    }

    /**
	 * Gets the channel name.
	 * 
	 * @return the channel name
	 */
    public String getChannelName() {
        return this.channelName;
    }

    /**
	 * Adds the channel msg.
	 * 
	 * @param sender
	 *            the sender
	 * @param msg
	 *            the msg
	 */
    public void addChannelMsg(String sender, String msg) {
        this.outputArea.setText(outputArea.getText() + "\n" + sender + "> " + msg);
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    /**
	 * Updates the channel list with the initial members.
	 * 
	 * @param members
	 *            the members
	 */
    void updateMembers(String members) {
        List<String> memberList = Arrays.asList(members.split("\\s+"));
        if (!memberList.isEmpty()) {
            multiList.addAllItems(memberList);
            multiList.invalidate();
            repaint();
        }
    }

    /**
	 * Updates the channel list when a member leaves.
	 * 
	 * @param member
	 *            the member who left this channel
	 */
    void memberLeft(String member) {
        multiList.removeItem(member);
    }

    /**
	 * {@inheritDoc}
	 * <p>
	 * Broadcasts on this channel the text entered by the user.
	 */
    public void actionPerformed(ActionEvent action) {
        String message = inputField.getText();
        MsgChat msg = new MsgChat();
        msg.setCommand("/channelMsg ");
        msg.setMsg(this.channelName + " " + message);
        try {
            GameContext.getClientCommunication().send(msg);
        } catch (SendMessageException e) {
            e.printStackTrace();
        }
        inputField.setText("");
    }

    /**
	 * Do single private message.
	 */
    public void doSinglePrivateMessage() {
        String target = multiList.getSelected();
        if (target == null) {
            this.myChatClient.showMessage("Must select a user");
            return;
        }
        if (target.equals(GameContext.getUserName())) {
            this.myChatClient.showMessage("You can't send private messages to yourself");
            return;
        }
        MsgChat m = new MsgChat();
        m.setCommand("/pm ");
        m.setMsg(target);
        m.setSender(GameContext.getUserName());
        try {
            GameContext.getClientCommunication().send(m);
        } catch (SendMessageException e) {
            e.printStackTrace();
        }
    }

    /**
	 * The listener interface for receiving PMMouse events. The class that is
	 * interested in processing a PMMouse event implements this interface, and
	 * the object created with that class is registered with a component using
	 * the component's <code>addPMMouseListener<code> method. When
	 * the PMMouse event occurs, that object's appropriate
	 * method is invoked.
	 * 
	 * @see PMMouseEvent
	 */
    static final class PMMouseListener extends MouseAdapter {

        /** The client. */
        private final ChatChannelFrame client;

        /**
		 * Creates a new {@code PMMouseListener} for the given {@code
		 * ChatClient}.
		 * 
		 * @param client
		 *            the client to notify when a double-click should trigger a
		 *            PM dialog.
		 */
        PMMouseListener(ChatChannelFrame client) {
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
                client.doSinglePrivateMessage();
            }
        }
    }

    /**
	 * Listener that requests to leave the channel when the frame closes.
	 */
    static final class FrameClosingListener extends InternalFrameAdapter {

        /** The frame. */
        private final ChatChannelFrame frame;

        /**
		 * Creates a new {@code FrameClosingListener} for the given {@code
		 * ChatChannelFrame}.
		 * 
		 * @param frame
		 *            the {@code ChatChannelFrame} notify when it is closing.
		 */
        FrameClosingListener(ChatChannelFrame frame) {
            this.frame = frame;
        }

        /**
		 * {@inheritDoc}
		 * <p>
		 * Requests that the server remove this client from this channel.
		 */
        @Override
        public void internalFrameClosing(InternalFrameEvent event) {
            if (!frame.getChannelName().equals(ChatCommands.GLOBAL_CHANNEL_NAME)) {
                frame.myChatClient.leaveChannel(frame.getChannelName());
            } else {
                frame.myChatClient.showMessage("No puede abandonar el canal Global. Debe cerrar el chat principal");
            }
        }
    }
}
