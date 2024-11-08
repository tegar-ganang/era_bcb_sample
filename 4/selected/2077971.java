package net.hypotenubel.jaicwain.gui.swing;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicMenuItemUI;
import javax.swing.text.*;
import net.hypotenubel.ctcp.*;
import net.hypotenubel.ctcp.msgutils.*;
import net.hypotenubel.irc.*;
import net.hypotenubel.irc.msgutils.*;
import net.hypotenubel.irc.net.*;
import net.hypotenubel.jaicwain.*;
import net.hypotenubel.jaicwain.gui.docking.*;
import net.hypotenubel.jaicwain.gui.actions.ActionManager;
import net.hypotenubel.jaicwain.options.*;
import net.hypotenubel.jaicwain.session.irc.*;
import net.hypotenubel.util.swing.*;

/**
 * Provides a nice panel with the typical chat interface. Can be used with
 * {@code JaicWainIRCChannel}s and derived classes only. Provides a user
 * list, a text area and an input field.
 * 
 * @author Christoph Daniel Schulze
 * @version $Id: IRCChatPanel.java 155 2006-10-08 22:11:13Z captainnuss $
 */
public class IRCChatPanel extends DockingPanel implements ActionListener, FocusListener, IRCChannelListener, IRCSessionListener, KeyListener, MouseListener, OptionsChangedListener {

    /**
     * {@code JaicWainIRCChannel} this panel visualizes.
     */
    private JaicWainIRCChannel channel = null;

    /**
     * Our list model that will keep the user list up-to-date.
     */
    private EncapsulatingListModel listModel = null;

    /**
     * Local copy of the user list.
     */
    private String[] users = new String[0];

    /**
     * Used to set the text styles.
     */
    private MutableAttributeSet currStyle = new SimpleAttributeSet();

    /**
     * Font style bold.
     */
    private boolean bold = false;

    /**
     * Font style italic.
     */
    private boolean italic = false;

    /**
     * Font style underlined.
     */
    private boolean underline = false;

    /**
     * IRC indexed colors.
     */
    public static final Color[] IRC_TEXT_COLORS = { Color.white, Color.black, new Color(0x00007f), new Color(0x009300), Color.red, new Color(0x7f0000), new Color(0x9c009c), new Color(0xfc7f00), Color.yellow, Color.green, new Color(0x009393), Color.cyan, Color.blue, Color.magenta, Color.darkGray, Color.lightGray };

    private JSplitPane splitPane;

    private JScrollPane logScrollPane;

    private JScrollPane lstScrollPane;

    private JTextPane logTextPane;

    private JList userList;

    private JHistoryTextField commandField;

    /**
     * Creates a new {@code IRCChatPanel} object and initializes it.
     * 
     * @param channel {@code JaicWainIRCChannel} this panel shall display.
     */
    public IRCChatPanel(JaicWainIRCChannel channel) {
        super();
        if (channel == null) throw new NullPointerException("channel can't be null");
        this.channel = channel;
        channel.addIRCChannelListener(this);
        channel.getParentSession().addIRCSessionListener(this);
        createUI();
        adaptUI();
        addFocusListener(this);
        setTitle(channel.getName());
        setLongTitle(channel.getName());
        setDescription(IRCUtils.getDecodedString(channel.getTopic()));
        if (IRCUtils.isChannel(channel.getName())) {
        } else {
        }
        App.options.addOptionsChangedListener(this);
    }

    /**
     * Sets up the user interface.
     */
    private void createUI() {
        logTextPane = new JTextPane();
        logTextPane.setEditable(false);
        logScrollPane = new JScrollPane(logTextPane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        logScrollPane.setMinimumSize(new Dimension(100, 50));
        userList = new JList(listModel = new EncapsulatingListModel());
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.addMouseListener(this);
        userList.addKeyListener(this);
        lstScrollPane = new JScrollPane(userList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        lstScrollPane.setMinimumSize(new Dimension(100, 50));
        commandField = new JHistoryTextField(App.options.getIntOption("gui", "historytextfield.size", 25));
        commandField.setEditable(true);
        commandField.setName("field");
        commandField.setVisible(true);
        commandField.addActionListener(this);
        commandField.addKeyListener(this);
        TreeSet<KeyStroke> keys = new TreeSet<KeyStroke>();
        keys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.CTRL_MASK));
        commandField.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, keys);
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false);
        splitPane.setBorder(null);
        splitPane.setContinuousLayout(true);
        splitPane.setDividerSize(4);
        splitPane.setOneTouchExpandable(false);
        int location = 0;
        if (App.options.getBooleanOption("gui", "chatsplitpane.userlistleft", false)) {
            location = 90;
        } else {
            location = splitPane.getWidth() - 90;
        }
        splitPane.setDividerLocation(App.options.getIntOption("gui", "chatsplitpane.divider.location", location));
        GridBagLayout g = new GridBagLayout();
        GridBagConstraints c = null;
        setLayout(g);
        c = new GridBagConstraints();
        c.weightx = 1.0f;
        c.weighty = 1.0f;
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        g.setConstraints(splitPane, c);
        add(splitPane);
        c = new GridBagConstraints();
        c.weightx = 1.0f;
        c.weighty = 0.0f;
        c.gridx = 0;
        c.gridy = 1;
        c.anchor = GridBagConstraints.SOUTH;
        c.fill = GridBagConstraints.HORIZONTAL;
        g.setConstraints(commandField, c);
        add(commandField);
        this.addKeyListener(this);
    }

    /**
     * As soon as the "gui" options package is changed, we'll have to update our
     * stuff.
     */
    private void adaptUI() {
        Font font = new Font(App.options.getStringOption("gui", "chattextarea.font.name", "Monospaced"), App.options.getIntOption("gui", "chattextarea.font.style", 0), App.options.getIntOption("gui", "chattextarea.font.size", 12));
        logTextPane.setFont(font);
        font = new Font(App.options.getStringOption("gui", "chattextfield.font.name", "Monospaced"), App.options.getIntOption("gui", "chattextfield.font.style", 0), App.options.getIntOption("gui", "chattextfield.font.size", 12));
        commandField.setFont(font);
        commandField.setHistorySize(App.options.getIntOption("gui", "historytextfield.size", 25));
        if (App.options.getBooleanOption("gui", "chatsplitpane.userlistleft", false)) {
            splitPane.setLeftComponent(lstScrollPane);
            splitPane.setRightComponent(logScrollPane);
            splitPane.setResizeWeight(0);
        } else {
            splitPane.setLeftComponent(logScrollPane);
            splitPane.setRightComponent(lstScrollPane);
            splitPane.setResizeWeight(1);
        }
    }

    /**
     * Returns the beginning of the word the carret's currently in. The boundary
     * of a word is marked by the first character that isn't allowed for nick
     * names or the string boundary.
     * 
     * @param offset {@code int} specifying the caret position.
     * @return {@code int} containing the start index of the word.
     */
    private int getWordStart(int offset) {
        String s = commandField.getText();
        if (offset == s.length()) offset--;
        for (; offset >= 0; offset--) {
            if (IRCUtils.NICK_CHARS.indexOf(s.charAt(offset)) == -1) return ++offset;
        }
        return 0;
    }

    /**
     * Returns the end of the word the carret's currently in. The boundary of a
     * word is marked by the first character that isn't allowed for nick names
     * or the string boundary.
     * 
     * @param offset {@code int} specifying the caret position.
     * @return {@code int} containing the end index of the word.
     */
    private int getWordEnd(int offset) {
        String s = commandField.getText();
        if (offset == s.length()) return offset;
        for (; offset < s.length(); offset++) {
            if (IRCUtils.NICK_CHARS.indexOf(s.charAt(offset)) == -1) return offset;
        }
        return s.length();
    }

    /**
     * Returns this panel's channel.
     * 
     * @return {@code JaicWainIRCChannel} this panel visualizes.
     */
    public JaicWainIRCChannel getChannel() {
        return channel;
    }

    /**
     * Returns an array of custom menu stuff related to this panel.
     * 
     * @return {@code JMenuItem} array or {@code null} if it doesn't
     *         have any.
     */
    public JMenuItem[] getMenuItems() {
        return new JMenuItem[] { new JMenuItem(ActionManager.partAction) };
    }

    /**
     * Appends the given message to the text area.
     *
     * @param message String object containing the message that is to be
     *                appended. It does not contain any line seperators at its
     *                end.
     * @param type The message type.
     * @param flag {@code boolean} indicating whether the user's nick is
     *             in this message ({@code true}) or not. If it is, the
     *             message will have a certain style.
     */
    public synchronized void append(String message, String type, boolean flag) {
        boolean flagContentsChanged = false;
        if (!type.equals("PRIVMSG")) {
            message = "* " + message;
        }
        if (type.equals("NOTICE")) {
            message = "**" + message;
            flagContentsChanged = true;
        }
        if (type.equals("PRIVMSG") || type.equals("ACTION")) flagContentsChanged = true;
        if (flag) {
            setStyleBold(App.options.getBooleanOption("irc", "styles.FLAGGED.bold", false));
            setStyleItalic(App.options.getBooleanOption("irc", "styles.FLAGGED.italic", false));
            setStyleUnderline(App.options.getBooleanOption("irc", "styles.FLAGGED.underline", false));
            setStyleForeground(App.options.getColorOption("irc", "styles.FLAGGED.foreground", Color.BLACK));
            setStyleBackground(App.options.getColorOption("irc", "styles.FLAGGED.background", new Color(-921360)));
        } else {
            setStyleBold(App.options.getBooleanOption("irc", "styles." + type + ".bold", false));
            setStyleItalic(App.options.getBooleanOption("irc", "styles." + type + ".italic", false));
            setStyleUnderline(App.options.getBooleanOption("irc", "styles." + type + ".underline", false));
            setStyleForeground(App.options.getColorOption("irc", "styles." + type + ".foreground", Color.BLACK));
            setStyleBackground(App.options.getColorOption("irc", "styles." + type + ".background", Color.WHITE));
        }
        decode(message, type);
        if (flagContentsChanged) panelContainer.flagContentChange(this, flag);
    }

    /**
     * Writes the message to the text area.
     *
     * @param message String containing the message.
     */
    private void write(String message) {
        try {
            logTextPane.getDocument().insertString(logTextPane.getDocument().getLength(), message, currStyle);
        } catch (BadLocationException e) {
        }
        logTextPane.setCaretPosition(logTextPane.getDocument().getLength());
    }

    /**
     * Appends the current buffer's contents to the output area while decoding
     * a message. The StringBuffer will be setLength(0) afterwards.
     */
    protected void writeDecodedMessage(StringBuffer buf) {
        write(buf.toString());
        buf.setLength(0);
    }

    /**
     * Decodes the given message.
     *
     * @param message String containing the message.
     * @param type The message type.
     */
    private void decode(String message, String type) {
        StringBuffer buf = new StringBuffer();
        int len = message.length();
        int i;
        char c;
        for (i = 0; i < len; i++) {
            c = message.charAt(i);
            switch(c) {
                case '\002':
                    writeDecodedMessage(buf);
                    toggleStyleBold();
                    break;
                case '\003':
                    boolean comma = false;
                    char tmp;
                    int j = 1;
                    StringBuffer fgCol = new StringBuffer("");
                    StringBuffer bgCol = new StringBuffer("");
                    for (j = 1; j <= 5; j++) {
                        if (i + j < len) {
                            tmp = message.charAt(i + j);
                            if ((tmp >= '0') && (tmp <= '9')) {
                                if (comma) {
                                    if (bgCol.length() == 2) {
                                        break;
                                    } else {
                                        bgCol.append(tmp);
                                    }
                                } else {
                                    if (fgCol.length() == 2) {
                                        break;
                                    } else {
                                        fgCol.append(tmp);
                                    }
                                }
                            } else if (tmp == ',') {
                                comma = true;
                            } else {
                                break;
                            }
                        }
                    }
                    i += j - 1;
                    writeDecodedMessage(buf);
                    setStyleForeground(App.options.getColorOption("irc", "styles." + type + ".foreground", Color.BLACK));
                    setStyleBackground(App.options.getColorOption("irc", "styles." + type + ".background", Color.WHITE));
                    break;
                case '\026':
                    writeDecodedMessage(buf);
                    setStyleForeground(App.options.getColorOption("irc", "styles.PRIVMSG.background", Color.WHITE));
                    setStyleBackground(App.options.getColorOption("irc", "styles.PRIVMSG.foreground", Color.BLACK));
                    break;
                case '\037':
                    writeDecodedMessage(buf);
                    toggleStyleUnderline();
                    break;
                default:
                    buf.append(c);
                    break;
            }
        }
        writeDecodedMessage(buf);
        writeDecodedMessage(new StringBuffer("\n"));
    }

    /**
     * Enable or disable boldface mode for subsequent messages.
     */
    protected synchronized void setStyleBold(boolean bold) {
        currStyle.removeAttribute(StyleConstants.Bold);
        if (bold) currStyle.addAttribute(StyleConstants.Bold, Boolean.TRUE);
        this.bold = bold;
    }

    /**
     * Toggle boldface mode for subsequent messages.
     */
    protected void toggleStyleBold() {
        setStyleBold(!bold);
    }

    /**
     * Enable or disable italic mode for subsequent messages.
     */
    protected synchronized void setStyleItalic(boolean italic) {
        currStyle.removeAttribute(StyleConstants.Italic);
        if (italic) currStyle.addAttribute(StyleConstants.Italic, Boolean.TRUE);
        this.italic = italic;
    }

    /**
     * Toggle italic mode for subsequent messages.
     */
    protected void toggleStyleItalic() {
        setStyleItalic(!italic);
    }

    /**
     * Enable or disable underline mode for subsequent messages.
     */
    protected synchronized void setStyleUnderline(boolean underline) {
        currStyle.removeAttribute(StyleConstants.Underline);
        if (underline) currStyle.addAttribute(StyleConstants.Underline, Boolean.TRUE);
        this.underline = underline;
    }

    /**
     * Toggle underline mode for subsequent messages.
     */
    protected void toggleStyleUnderline() {
        setStyleUnderline(!underline);
    }

    /**
     * Set foreground for subsequent messages (IRC standard indexed color).
     */
    void setStyleForeground(int index) {
        setStyleForeground(IRC_TEXT_COLORS[index]);
    }

    /**
     * Set foreground for subsequent messages.
     */
    protected void setStyleForeground(Color col) {
        currStyle.removeAttribute(StyleConstants.Foreground);
        currStyle.addAttribute(StyleConstants.Foreground, col);
    }

    /**
     * Set background for subsequent messages (IRC standard indexed color).
     */
    protected void setStyleBackground(int index) {
        setStyleBackground(IRC_TEXT_COLORS[index]);
    }

    /**
     * Set background for subsequent messages.
     */
    protected void setStyleBackground(Color col) {
        currStyle.removeAttribute(StyleConstants.Background);
        currStyle.addAttribute(StyleConstants.Background, col);
    }

    /**
     * Parses the given colour String part.
     *
     * @param col String containing the colour value.
     * @param defaultColor {@code Color} to return if something goes wrong
     *                     with the parsing.
     * @return {@code Color} object for the String.
     */
    private Color parseColorString(String col, Color defaultColor) {
        try {
            int color = Integer.parseInt(col);
            while (color > 16) color -= 16;
            return IRC_TEXT_COLORS[color];
        } catch (NumberFormatException e) {
            return defaultColor;
        }
    }

    /**
     * Invoked when the user hits enter in the command field.
     */
    public void actionPerformed(ActionEvent e) {
        if (commandField.getText().equals("")) return;
        String command = commandField.getText();
        commandField.setText("");
        String result = channel.execute(command);
        if (result != null) append(result, "ERROR", false);
    }

    public void focusGained(FocusEvent e) {
        commandField.requestFocusInWindow();
    }

    public void focusLost(FocusEvent e) {
    }

    public void messageProcessed(AbstractIRCChannel chann, IRCMessage msg) {
        if (msg.getType().equals(IRCMessageTypes.MSG_JOIN)) {
            String name = msg.getNick();
            for (int i = 0; i < users.length; i++) if (users[i].equals(name)) return;
            users = chann.getUsers();
            for (int i = 0; i < users.length; i++) if (users[i].equals(name)) listModel.add(i, name);
            if (msg.getNick().equals(chann.getParentSession().getNickName())) {
                Object args[] = new Object[] { new Date() };
                String txt = App.localization.localize("app", "irc.msg_join.self", "Joined the channel on {0,date,full} at {0,time,full}", args);
                append(txt, "JOIN", false);
            } else {
                String args[] = new String[] { msg.getNick() };
                String txt = App.localization.localize("app", "irc.msg_join", "{0} has joined the channel", args);
                append(txt, "JOIN", false);
            }
        } else if (msg.getType().equals(IRCMessageTypes.MSG_KICK)) {
            String name = KickMessage.getUser(msg);
            for (int i = 0; i < users.length; i++) if (IRCUtils.stripNickStatus(users[i]).equals(name)) listModel.remove(i);
            users = chann.getUsers();
            String comment = KickMessage.getComment(msg);
            String txt;
            if (comment.equals("") || comment.equals(msg.getNick())) {
                String args[] = new String[] { name, msg.getNick() };
                txt = App.localization.localize("app", "irc.msg_kick.nocomment", "{0} has been kicked by {1}", args);
            } else {
                String args[] = new String[] { name, msg.getNick(), comment };
                txt = App.localization.localize("app", "irc.msg_kick.comment", "{0} has been kicked by {1} ({2})", args);
            }
            append(txt, "KICK", false);
        } else if (msg.getType().equals(IRCMessageTypes.MSG_MODE)) {
        } else if (msg.getType().equals(IRCMessageTypes.MSG_NICK)) {
            for (int i = 0; i < users.length; i++) if (IRCUtils.stripNickStatus(users[i]).equals(msg.getNick())) {
                listModel.remove(i);
                break;
            }
            users = chann.getUsers();
            for (int i = 0; i < users.length; i++) if (IRCUtils.stripNickStatus(users[i]).equals(NickMessage.getNickname(msg))) {
                listModel.add(i, users[i]);
                break;
            }
            String args[] = new String[] { msg.getNick(), NickMessage.getNickname(msg) };
            String txt = App.localization.localize("app", "irc.msg_nick", "{0} changed his nick to {1}", args);
            append(txt, "NICK", false);
        } else if (msg.getType().equals(IRCMessageTypes.MSG_NOTICE)) {
            String args[] = new String[] { msg.getNick(), NoticeMessage.getText(msg) };
            String txt = App.localization.localize("app", "irc.msg_notice", "{0}: {1}", args);
            append(txt, "NOTICE", false);
        } else if (msg.getType().equals(IRCMessageTypes.MSG_PART)) {
            if (msg.getNick().equals(chann.getParentSession().getNickName())) {
                Object args[] = new Object[] { new Date() };
                String txt = App.localization.localize("app", "irc.msg_part.self", "Left the channel on {0,date,full} at {0,time,full}", args);
                append(txt, "PART", false);
            }
            String name = msg.getNick();
            for (int i = 0; i < users.length; i++) if (IRCUtils.stripNickStatus(users[i]).equals(name)) listModel.remove(i);
            users = chann.getUsers();
            String txt = PartMessage.getMessage(msg);
            if (txt.equals("")) txt = App.localization.localize("app", "irc.msg_part.nopartmessage", "No part message");
            String args[] = new String[] { msg.getNick(), txt };
            txt = App.localization.localize("app", "irc.msg_part", "{0} has left the channel ({1})", args);
            append(txt, "PART", false);
        } else if (msg.getType().equals(IRCMessageTypes.MSG_PRIVMSG)) {
            if (CTCPMessage.isCTCPMessage(PrivateMessage.getText(msg))) {
                CTCPMessage ctcp = CTCPMessage.parseMessageString(PrivateMessage.getText(msg));
                if (ctcp.getType().equals(CTCPMessageTypes.CTCP_ACTION)) {
                    String args[] = new String[] { msg.getNick(), ActionMessage.getText(ctcp) };
                    String txt = App.localization.localize("app", "irc.msg_privmsg.ctcp_action", "{0} {1}", args);
                    boolean flag = (ActionMessage.getText(ctcp).indexOf(chann.getParentSession().getNickName()) != -1);
                    append(txt, "ACTION", flag);
                }
            } else {
                String args[] = new String[] { msg.getNick(), PrivateMessage.getText(msg) };
                String txt = App.localization.localize("app", "irc.msg_privmsg", "<{0}> {1}", args);
                boolean flag = (PrivateMessage.getText(msg).indexOf(chann.getParentSession().getNickName()) != -1);
                append(txt, "PRIVMSG", flag);
            }
        } else if (msg.getType().equals(IRCMessageTypes.MSG_QUIT)) {
            if (msg.getNick().equals(chann.getParentSession().getNickName())) {
                Object args[] = new Object[] { new Date() };
                String txt = App.localization.localize("app", "irc.msg_quit.self", "Left IRC on {0,date,full} at {0,time,full}", args);
                append(txt, "QUIT", false);
                panelContainer.removeDockingPanel(this);
                return;
            }
            String name = msg.getNick();
            for (int i = 0; i < users.length; i++) if (IRCUtils.stripNickStatus(users[i]).equals(name)) listModel.remove(i);
            users = chann.getUsers();
            String txt = QuitMessage.getQuitMessage(msg);
            if (txt.equals("")) txt = App.localization.localize("app", "irc.msg_quit.noquitmessage", "No quit message");
            String args[] = new String[] { msg.getNick(), txt };
            txt = App.localization.localize("app", "irc.msg_quit", "{0} has quit IRC ({1})", args);
            append(txt, "QUIT", false);
        } else if (msg.getType().equals(IRCMessageTypes.MSG_TOPIC)) {
            setDescription(IRCUtils.getDecodedString(TopicMessage.getTopic(msg)));
            String args[] = new String[] { chann.getTopic() };
            String txt = App.localization.localize("app", "irc.msg_topic", "The channel''s topic has been changed to: {0}", args);
            append(txt, "TOPIC", false);
        } else if (msg.getType().equals(IRCMessageTypes.ERR_NOSUCHNICK)) {
            String args[] = new String[] { NosuchnickError.getNickname(msg) };
            String txt = App.localization.localize("app", "irc.err_nosuchnick", "No such nick: {0}", args);
            append(txt, "ERROR", false);
        } else if (msg.getType().equals(IRCMessageTypes.ERR_NOSUCHCHANNEL)) {
            String args[] = new String[] { NosuchchannelError.getChannelname(msg) };
            String txt = App.localization.localize("app", "irc.err_nosuchchannel", "No such channel: {0}", args);
            append(txt, "ERROR", false);
        } else if (msg.getType().equals(IRCMessageTypes.ERR_CANNOTSENDTOCHAN)) {
            String txt = App.localization.localize("app", "irc.err_cannotsendtochan", "The message couldn't be sent to the channel");
            append(txt, "ERROR", false);
        } else if (msg.getType().equals(IRCMessageTypes.RPL_AWAY)) {
            String args[] = new String[] { AwayReply.getNick(msg), AwayReply.getMessage(msg) };
            String txt = App.localization.localize("app", "irc.rpl_away", "{0} is away ({1})", args);
            append(txt, "REPLY", false);
        } else if (msg.getType().equals(IRCMessageTypes.RPL_NOTOPIC)) {
            setDescription("");
            String txt = App.localization.localize("app", "irc.rpl_notopic", "Nobody dared to set a channel topic yet");
            append(txt, "TOPIC", false);
        } else if (msg.getType().equals(IRCMessageTypes.RPL_TOPIC)) {
            setDescription(IRCUtils.getDecodedString(TopicMessage.getTopic(msg)));
            String args[] = new String[] { chann.getTopic() };
            String txt = App.localization.localize("app", "irc.rpl_topic", "The channel''s topic is {0}", args);
            append(txt, "TOPIC", false);
        } else if (msg.getType().equals(IRCMessageTypes.RPL_TOPICSET)) {
            Object args[] = new Object[] { TopicsetReply.getUserName(msg), TopicsetReply.getTimecode(msg) };
            String txt = App.localization.localize("app", "irc.msg_topic", "The topic has been set by {0} on {1,date,EEE',' MMM d',' yyyy} at " + "{1,time,HH:mm:ss a}", args);
            append(txt, "TOPIC", false);
        } else if (msg.getType().equals(IRCMessageTypes.RPL_ENDOFNAMES)) {
            users = new String[0];
            listModel.clear();
            users = chann.getUsers();
            listModel.addAll(users);
        }
    }

    public void messageProcessed(AbstractIRCSession session, IRCMessage msg) {
    }

    public void channelCreated(AbstractIRCSession session, AbstractIRCChannel chann) {
    }

    public void channelRemoved(AbstractIRCSession session, AbstractIRCChannel chann) {
        if (chann == this.channel) {
            chann.getParentSession().removeIRCSessionListener(this);
            App.options.removeOptionsChangedListener(this);
            panelContainer.removeDockingPanel(this);
        }
    }

    /**
     * Invoked when a key has been typed.
     * See the class description for {@link KeyEvent} for a definition of 
     * a key typed event.
     */
    public void keyTyped(KeyEvent e) {
    }

    /**
     * Invoked when a key has been pressed. 
     * See the class description for {@link KeyEvent} for a definition of 
     * a key pressed event.
     */
    public void keyPressed(KeyEvent e) {
        if (e.getSource() == userList) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                String name = IRCUtils.stripNickStatus((String) userList.getSelectedValue());
                if (!IRCUtils.stripNickStatus(name).equals(channel.getParentSession().getNickName())) {
                    channel.getParentSession().addChannel(name);
                }
            }
            return;
        }
        if ((e.getKeyCode() == KeyEvent.VK_U) && e.isControlDown()) {
            commandField.setText("");
        } else if (e.getKeyCode() == KeyEvent.VK_TAB) {
            int offs = 0;
            int begOffs = 0;
            int endOffs = 0;
            offs = commandField.getCaretPosition();
            begOffs = getWordStart(offs);
            endOffs = getWordEnd(offs);
            if (endOffs - begOffs == 0) return;
            commandField.moveCaretPosition(begOffs);
            commandField.setSelectionStart(begOffs);
            commandField.setSelectionEnd(endOffs);
            String[] channelUsers = channel.getUsers();
            String text = commandField.getSelectedText();
            ArrayList<String> matches = new ArrayList<String>(10);
            String item = "";
            for (int i = 0; i < channelUsers.length; i++) {
                item = channelUsers[i];
                if (IRCUtils.stripNickStatus(item).equalsIgnoreCase(channel.getParentSession().getNickName())) continue;
                if (IRCUtils.stripNickStatus(item).toLowerCase().startsWith(text.toLowerCase())) {
                    matches.add(IRCUtils.stripNickStatus(item));
                }
            }
            if (matches.size() == 1) {
                if (begOffs == 0) commandField.replaceSelection(matches.get(0) + ": "); else commandField.replaceSelection(matches.get(0));
            } else if (matches.size() > 1) {
                JPopupMenu popup = new JPopupMenu();
                JMenuItem mnu = new JMenuItem();
                mnu.setText(App.localization.localize("app", "ircchatpanel.messages.multiplenicksfound", "Multiple nicks found"));
                mnu.setEnabled(false);
                popup.add(mnu);
                popup.addSeparator();
                JMenuItem firstItem = null;
                for (int i = 0; i < matches.size(); i++) {
                    if (i == 0) {
                        firstItem = popup.add(new ReplaceNickAction(matches.get(i)));
                    } else {
                        popup.add(new ReplaceNickAction(matches.get(i)));
                    }
                }
                popup.addSeparator();
                mnu = new JMenuItem();
                mnu.setText(App.localization.localize("app", "general.cancel", "Cancel"));
                popup.add(mnu);
                popup.show(commandField, 0, commandField.getHeight());
                MenuSelectionManager.defaultManager().setSelectedPath(((BasicMenuItemUI) firstItem.getUI()).getPath());
            }
        }
    }

    /**
     * Invoked when a key has been released.
     * See the class description for {@link KeyEvent} for a definition of 
     * a key released event.
     */
    public void keyReleased(KeyEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
        if ((e.getClickCount() % 2 == 0) && SwingUtilities.isLeftMouseButton(e)) {
            String name = IRCUtils.stripNickStatus((String) userList.getSelectedValue());
            if (!IRCUtils.stripNickStatus(name).equals(channel.getParentSession().getNickName())) {
                channel.getParentSession().addChannel(name);
            }
        }
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void optionsChanged(String set) {
        if (set.equals("gui")) adaptUI();
    }

    /**
     * Represents a menu item that will replace the current commandField
     * selection with the stuff its been said to replace it with. Used for the
     * nick name autocomplete feature if multiple nicks with the same beginning
     * are found.
     * 
     * @author Christoph Daniel Schulze
     * @version $Id: IRCChatPanel.java 155 2006-10-08 22:11:13Z captainnuss $
     */
    private class ReplaceNickAction extends AbstractAction {

        /**
         * Text to replace the selection with. This is used as the action's name
         * property too.
         */
        private String replacement = "";

        /**
         * Creates a new {@code ReplaceNickAction} object and initializes
         * it.
         * 
         * @param replacement {@code String} to replace the selection with.
         */
        public ReplaceNickAction(String replacement) {
            this.replacement = replacement;
            putValue(NAME, replacement);
        }

        public void actionPerformed(ActionEvent e) {
            int begOffs = commandField.getSelectionStart();
            if (begOffs == 0) commandField.replaceSelection(replacement + ": "); else commandField.replaceSelection(replacement);
        }
    }
}
