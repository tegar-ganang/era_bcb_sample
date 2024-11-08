package webirc.client;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.WindowCloseListener;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupListener;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;
import webirc.client.commands.*;
import webirc.client.gui.contactpanel.ContactPanel;
import webirc.client.gui.dialogs.*;
import webirc.client.gui.menu.MenuController;
import webirc.client.gui.messagepanel.MessagePanel;
import webirc.client.gui.tabbedpanel.TabbedBarItem;
import webirc.client.synchronization.*;
import webirc.client.utils.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

/**
 * The main class in the project. It works as pattern "Mediator".
 *
 * @author Ayzen
 */
public class MainSystem {

    private static MainSystem instance;

    public static MainSystem getInstance(HashMap params) {
        if (instance == null) instance = new MainSystem(params);
        return instance;
    }

    public static MainSystem getInstance() {
        if (instance == null) instance = new MainSystem(null);
        return instance;
    }

    private GUIController gui;

    private String locale;

    private String server = "127.0.0.1";

    private int serverPort = 6667;

    private String startupChannels = null;

    private boolean connected = false;

    private boolean closing = false;

    private String selectedNick;

    private final User user = new User();

    private boolean messsageNotification = false;

    private Timer notificationTimer = new Timer() {

        private boolean upper = false;

        public void run() {
            String title;
            if (upper) {
                title = "! " + WebIRC.mainMessages.message() + " !";
                GUIController.setIcon(GUIController.ICON_WEBIRC);
            } else {
                title = "| " + WebIRC.mainMessages.message().toUpperCase() + " |";
                GUIController.setIcon(GUIController.ICON_MESSAGE);
            }
            Window.setTitle(title);
            upper = !upper;
        }
    };

    private final ConnectDialog connectDialog = new ConnectDialog();

    private final MessageBox connectingBox = new MessageBox("Connecting", "Connecting to the server, please wait...", true);

    private Synchronizer synch;

    private MainSystem(HashMap params) {
        gui = GUIController.getInstance();
        Panel loadingPanel = RootPanel.get("Loading");
        if (loadingPanel != null) loadingPanel.setVisible(false);
        if (params != null && !params.isEmpty()) {
            String param = (String) params.get("host");
            if (param != null && param.trim().length() > 0) server = param;
            param = (String) params.get("port");
            if (param != null) try {
                serverPort = Integer.parseInt(param);
            } catch (NumberFormatException e) {
                showError(WebIRC.errorMessages.notCorrectParam("port"));
            }
            param = (String) params.get("debug");
            if ("true".equalsIgnoreCase(param)) WebIRC.DEBUG = true;
            param = (String) params.get("channels");
            if (param != null && param.trim().length() > 0) startupChannels = Utils.unescape(param);
            locale = (String) params.get("locale");
            param = (String) params.get("synch");
            if ("continuation".equalsIgnoreCase(param)) synch = new ContinuationSynchronizer(new SynchronizeEventsHandler()); else if ("socket".equalsIgnoreCase(param)) {
                String policyFile = (String) params.get("policy");
                policyFile = policyFile != null && policyFile.trim().length() > 0 ? Utils.unescape(policyFile) : null;
                if (synch == null) try {
                    synch = new SocketSynchronizer(new SynchronizeEventsHandler(), policyFile);
                } catch (NullPointerException e) {
                    showError("It seems like your browser doesn't support sockets.\nPlease, install Macromedia Flash or Java Plugin.");
                }
            }
        }
        if (synch == null) synch = new TimerSynchronizer(new SynchronizeEventsHandler());
        synch.addCommandListener(new MessagesHandler(gui));
        Aliases.getInstance();
        Smileys.getInstance();
        MenuController.getInstance();
        Window.addWindowCloseListener(new WindowCloseListener() {

            public String onWindowClosing() {
                return WebIRC.mainMessages.exitConfirmation();
            }

            public void onWindowClosed() {
                sendQuitMessage();
            }
        });
        connectDialog.addDialogListener(new DialogListener() {

            public void onFirstButtonPressed() {
                if (connectDialog.isValidated()) {
                    if (!connected) {
                        selectedNick = connectDialog.getNick();
                        connect();
                        connectingBox.show();
                    }
                }
            }

            public void onSecondButtonPressed() {
            }
        });
    }

    private void setConnected(boolean connected) {
        this.connected = connected;
        gui.getSettingsPanel().switchButtons();
    }

    private void connect() {
        synch.connect(server, serverPort);
    }

    private void closeConnect() {
        if (connected) {
            String message = WebIRC.eventMessages.youQuit();
            gui.getMessagePanel().addSystemMessage(new MessageCommand(message, MessageCommand.TYPE_EXIT));
            gui.cleanup();
        } else connectingBox.hide();
        user.clear();
        closing = false;
        setConnected(false);
        synch.stop();
    }

    public static void reconnect() {
        instance.gui.cleanup();
        instance.connectDialog.show();
    }

    public static void showError(final String message) {
        MessageBox errorDlg = new MessageBox(WebIRC.dialogMessages.error(), message, MessageBox.TYPE_ERROR);
        errorDlg.show();
    }

    public static void showError(final String message, final Command doAfter) {
        MessageBox errorDlg = new MessageBox(WebIRC.dialogMessages.error(), message, MessageBox.TYPE_ERROR);
        errorDlg.addPopupListener(new PopupListener() {

            public void onPopupClosed(PopupPanel sender, boolean autoClosed) {
                doAfter.execute();
            }
        });
        errorDlg.show();
    }

    public static void showFatalError(final String message) {
        if (!instance.closing) {
            if (!instance.gui.isGuiCreated()) instance.connectDialog.show();
            new MessageBox(WebIRC.dialogMessages.error(), message, MessageBox.TYPE_ERROR).show();
        } else log(message);
        instance.closeConnect();
    }

    public static void showFatalError(String message, Throwable exception) {
        if (exception != null) message += '\n' + exception.getMessage();
        showFatalError(message);
    }

    /**
   * Sends the command to message using sendMessage method. Better to use this method than sendMessage.
   *
   * @param command the command to send
   */
    public static void sendCommand(IRCCommand command) {
        sendMessage(command.toString());
        if (command instanceof MessageCommand) {
            MessageCommand msg = (MessageCommand) command;
            if (msg.getType() == MessageCommand.TYPE_CTCP) {
                String message = WebIRC.eventMessages.ctcpResponseSent(msg.getCtcpText());
                MessageCommand msgCommand = new MessageCommand(message, MessageCommand.TYPE_EVENT);
                instance.gui.getMessagePanel().addMessage(msg.getReceiver(), msgCommand);
            }
        }
    }

    /**
   * Directly sends the message to server. Before sending it parses aliases.
   *
   * @param message the message to send
   */
    public static void sendMessage(String message) {
        try {
            String msg = Aliases.getInstance().parseAlias(message);
            if (msg.charAt(0) == '/') msg = msg.substring(1);
            CommandParser parser = new CommandParser();
            IRCCommand command = parser.parseMessage(msg);
            if (command instanceof MessageCommand) {
                MessageCommand msgCmd = (MessageCommand) command;
                if (CTCPHandler.COMMAND_ACTION.equalsIgnoreCase(msgCmd.getCtcpCommand())) instance.gui.getMessagePanel().addMessage(msgCmd.getReceiver(), msgCmd);
            }
            String cmd = msg.substring(0, 4);
            if (cmd.equalsIgnoreCase("quit")) instance.closing = true;
            instance.synch.synchronize(msg);
        } catch (AliasException e) {
            MessageCommand command = new MessageCommand(e.getMessage(), MessageCommand.TYPE_EVENT);
            instance.gui.getMessagePanel().addMessage(command);
        }
    }

    public static void sendQuitMessage() {
        if (isConnected()) sendCommand(new QuitCommand(WebIRC.mainMessages.webIRCAbout()));
    }

    public void setMessageNotification(Object entity, boolean light) {
        TabbedBarItem tab = gui.getMessagePanel().getTabBar().getItem(gui.getMessagePanel().getEntityIndex(entity));
        if (tab == null) return;
        if (!GUIController.isWindowFocused()) {
            Window.setTitle("! " + WebIRC.mainMessages.message() + " !");
            messsageNotification = true;
        }
        if (!light) {
            if (!MessagePanel.isSystemTab(tab)) SoundManager.playSound("sound/Message.wav");
            if (!tab.isSelected()) tab.setNotified(true);
            if (!GUIController.isWindowFocused()) notificationTimer.scheduleRepeating(750);
        }
    }

    public static Channel getActiveChannel() {
        return GUIController.getInstance().getContactPanel().getSelectedChannel();
    }

    public static void log(String message) {
        MessagePanel messagePanel = instance.gui.getMessagePanel();
        if (WebIRC.DEBUG && messagePanel != null && messagePanel.isAttached()) instance.gui.getMessagePanel().addSystemMessage(new MessageCommand(message, MessageCommand.TYPE_EVENT));
    }

    public void onWindowFocus() {
        if (messsageNotification) {
            notificationTimer.cancel();
            GUIController.setIcon(GUIController.ICON_WEBIRC);
            Object entity = gui.getMessagePanel().getSelectedTabEntity();
            if (entity != null) Window.setTitle(entity.toString()); else Window.setTitle("WebIRC " + WebIRC.VERSION);
        }
    }

    public static String getLocale() {
        return instance.locale;
    }

    public static User getUser() {
        return instance.user;
    }

    public static String getUserNick() {
        return instance.user.getNickname();
    }

    public static boolean isConnected() {
        return instance.connected;
    }

    /**
   * Handles all messages
   */
    private class MessagesHandler implements CommandListener {

        private MessagePanel messagePanel;

        private ContactPanel contactPanel;

        private HashMap incomingUsers = new HashMap();

        private ChannelsDialog channelsDialod;

        private boolean channelsIsShowing = false;

        public MessagesHandler(GUIController gui) {
            messagePanel = gui.getMessagePanel();
            contactPanel = gui.getContactPanel();
        }

        public void onNickCommand(NickCommand command) {
            User sender = command.getSender();
            String newNick = command.getNick();
            String message;
            if (user.equals(sender)) {
                user.setNickname(newNick);
                message = WebIRC.eventMessages.youChangedNick(newNick);
            } else message = WebIRC.eventMessages.someoneChangedNick(sender.getNickname(), newNick);
            messagePanel.changeEntityName(sender, newNick);
            Collection channels = contactPanel.userChangesNick(sender, newNick);
            for (Iterator it = channels.iterator(); it.hasNext(); ) {
                Channel channel = (Channel) it.next();
                messagePanel.addMessage(channel, new MessageCommand(message, MessageCommand.TYPE_EVENT));
            }
        }

        public void onMessageCommand(MessageCommand command) {
            Object messageFor;
            if (command.getReceiver().equals(user)) {
                messageFor = command.getSender();
                messagePanel.addMessage(messageFor, command);
                if (command.getType() == MessageCommand.TYPE_CTCP) CTCPHandler.handleCTCP(command.getCtcpCommand(), command.getCtcpParameters(), (User) messageFor);
            } else {
                messageFor = command.getReceiver();
                if (messageFor instanceof Channel) {
                    User sender = command.getSender();
                    sender.setType(contactPanel.getUserType((Channel) messageFor, sender));
                }
                messagePanel.addMessage(messageFor, command);
            }
            if (messageFor instanceof Channel) setMessageNotification(messageFor, true); else setMessageNotification(messageFor, false);
        }

        public void onNoticeCommand(NoticeCommand command) {
            Object selectedTab = messagePanel.getSelectedTabEntity();
            if (selectedTab != null) messagePanel.addMessage(selectedTab, command); else {
                messagePanel.addSystemMessage(command);
                setMessageNotification(MessagePanel.SYSTEM_TAB, false);
            }
        }

        public void onQuitCommand(QuitCommand command) {
            User sender = command.getSender();
            if (!sender.equals(user)) {
                Collection channels = contactPanel.userQuit(sender);
                String quitMessage = command.getMessage();
                String message;
                if (quitMessage != null && quitMessage.trim().length() > 0) message = WebIRC.eventMessages.userQuit(sender.getNickname(), WebIRC.eventMessages.saying(quitMessage)); else message = WebIRC.eventMessages.userQuit(sender.getNickname(), "");
                MessageCommand messageCommand = new MessageCommand(message, MessageCommand.TYPE_EXIT);
                if (messagePanel.isTabExist(sender)) messagePanel.addMessage(sender, messageCommand);
                for (Iterator it = channels.iterator(); it.hasNext(); ) {
                    Channel channel = (Channel) it.next();
                    messagePanel.addMessage(channel, messageCommand);
                }
            }
        }

        public void onJoinCommand(JoinCommand command) {
            if (command.getSender().equals(user)) {
                Channel channel = command.getChannel();
                if (!messagePanel.isTabExist(channel)) messagePanel.addTab(channel);
                contactPanel.addChannel(channel);
                messagePanel.selectTab(channel);
            } else {
                Channel channel = command.getChannel();
                User sender = command.getSender();
                contactPanel.userJoined(sender, channel);
                String message = WebIRC.eventMessages.userJoins(sender.getNickname());
                messagePanel.addMessage(channel, new MessageCommand(message, MessageCommand.TYPE_ENTER));
            }
        }

        public void onPartCommand(PartCommand command) {
            if (command.getSender().equals(user)) {
                Collection channels = command.getChannels();
                String message = WebIRC.eventMessages.youPart();
                for (Iterator it = channels.iterator(); it.hasNext(); ) {
                    Channel channel = (Channel) it.next();
                    contactPanel.removeChannel(channel);
                    messagePanel.addMessage(channel, new MessageCommand(message, MessageCommand.TYPE_EXIT));
                }
            } else {
                User sender = command.getSender();
                Collection channels = command.getChannels();
                String message = WebIRC.eventMessages.userPart(sender.getNickname());
                for (Iterator it = channels.iterator(); it.hasNext(); ) {
                    Channel channel = (Channel) it.next();
                    contactPanel.userLeft(sender, channel);
                    messagePanel.addMessage(channel, new MessageCommand(message, MessageCommand.TYPE_EXIT));
                }
            }
        }

        public void onKickCommand(KickCommand command) {
            String message;
            String whoNick = command.getSender().getNickname();
            User victim = command.getUser();
            final Channel channel = command.getChannel();
            String reason = command.getKickMessage();
            if (user.equals(victim)) {
                contactPanel.removeChannel(channel);
                message = WebIRC.eventMessages.youHaveBeenKickedMsg(whoNick, reason);
                String confirmMessage = WebIRC.eventMessages.youHaveBeenKicked(channel.toString(), whoNick, reason);
                new ConfirmDialog(confirmMessage, new Command() {

                    public void execute() {
                        sendCommand(new JoinCommand(channel));
                    }
                }).show();
            } else {
                contactPanel.userLeft(victim, channel);
                message = WebIRC.eventMessages.someoneHasBeenKicked(victim.getNickname(), whoNick, reason);
            }
            messagePanel.addMessage(channel, new MessageCommand(message, MessageCommand.TYPE_KICK));
        }

        public void onModeCommand(ModeCommand command) {
            messagePanel.addSystemMessage(command);
            if (command.isChannelMode()) {
                Channel channel = command.getChannel();
                HashMap modes = command.getModes();
                for (Iterator it = modes.keySet().iterator(); it.hasNext(); ) {
                    MessageCommand msg;
                    Character mode = (Character) it.next();
                    String argument = (String) modes.get(mode);
                    if (ModeCommand.isUserTypeMode(mode.charValue())) {
                        User user = new User(argument);
                        contactPanel.userModeChange(channel, user, mode.charValue(), command.isAddingModes());
                        if (command.isAddingModes()) msg = new MessageCommand(WebIRC.eventMessages.addUserMode(command.getSender().getNickname(), User.getModeName(mode.charValue()), user.getNickname()), MessageCommand.TYPE_ADDUSERMODE); else msg = new MessageCommand(WebIRC.eventMessages.removeUserMode(command.getSender().getNickname(), User.getModeName(mode.charValue()), user.getNickname()), MessageCommand.TYPE_REMOVEUSERMODE);
                    } else if (ModeCommand.isBanMode(mode.charValue())) {
                        if (command.isAddingModes()) msg = new MessageCommand(WebIRC.eventMessages.ban(command.getSender().getNickname(), argument), MessageCommand.TYPE_BAN); else msg = new MessageCommand(WebIRC.eventMessages.unban(command.getSender().getNickname(), argument), MessageCommand.TYPE_UNBAN);
                    } else msg = new MessageCommand(WebIRC.eventMessages.changeMode(command.getSender().getNickname(), command.getMode()), MessageCommand.TYPE_MODE);
                    messagePanel.addMessage(channel, msg);
                }
            } else {
            }
        }

        public void onTopicCommand(TopicCommand command) {
            String senderName = command.getSender().getNickname();
            String text = WebIRC.eventMessages.topicChanged(senderName, command.getTopic());
            MessageCommand msg = new MessageCommand(text, MessageCommand.TYPE_TOPIC);
            messagePanel.addMessage(command.getChannel(), msg);
        }

        public void onErrorCommand(ErrorCommand command) {
            showFatalError(command.getMessage());
        }

        public void onUnknownCommand(UnknownCommand command) {
            messagePanel.addMessage(MessagePanel.SYSTEM_TAB, command);
        }

        public void onConnectReplyCommand(ConnectReplyCommand command) {
            if (command.isWelcomeReply()) {
                Iterator paramsIt = command.getParameters().iterator();
                if (paramsIt.hasNext()) {
                    user.setNickname((String) paramsIt.next());
                    if (paramsIt.hasNext()) {
                        user.setUserName((String) paramsIt.next());
                        if (paramsIt.hasNext()) user.setHostName((String) paramsIt.next());
                    }
                }
                if (startupChannels != null && startupChannels.trim().length() > 0) sendCommand(new JoinCommand(startupChannels));
            }
            messagePanel.addMessage(MessagePanel.SYSTEM_TAB, command);
        }

        public void onCommandResponseReplyCommand(CommandResponseReplyCommand command) {
            Iterator propertiesIt = command.getParameters().iterator();
            if (command.isNamReply()) {
                Channel channel = (Channel) propertiesIt.next();
                String[] users = (String[]) propertiesIt.next();
                Vector incUsers = (Vector) incomingUsers.get(channel);
                if (incUsers == null) {
                    incUsers = new Vector();
                    incomingUsers.put(channel, incUsers);
                }
                for (int i = 0; i < users.length; i++) User.insertUser(incUsers, new User(users[i]));
            } else if (command.isEndOfNames()) {
                Channel channel = (Channel) propertiesIt.next();
                channel.setUsers((Vector) incomingUsers.get(channel));
                contactPanel.updateChannel(channel);
                incomingUsers.remove(channel);
            } else if (command.isListStart()) {
                channelsIsShowing = true;
                channelsDialod = new ChannelsDialog();
                channelsDialod.show();
            } else if (command.isList()) {
                if (!channelsIsShowing) {
                    channelsIsShowing = true;
                    channelsDialod = new ChannelsDialog();
                    channelsDialod.show();
                }
                String channelName = (String) propertiesIt.next();
                int users = Integer.parseInt((String) propertiesIt.next());
                String topic = (String) propertiesIt.next();
                channelsDialod.addChannelInfo(channelName, users, topic);
            } else if (command.isTopic()) {
                Channel channel = (Channel) propertiesIt.next();
                String topic = (String) propertiesIt.next();
                if (topic != null && topic.trim().length() > 0) {
                    MessageCommand msg = new MessageCommand(WebIRC.eventMessages.topic(topic), MessageCommand.TYPE_TOPIC);
                    messagePanel.addMessage(channel, msg);
                }
            }
            messagePanel.addMessage(MessagePanel.SYSTEM_TAB, command);
        }

        public void onErrorReplyCommand(ErrorReplyCommand command) {
            if (command.isBadChannelKey()) {
                final Channel channel = (Channel) command.getParameters().iterator().next();
                final InputDialog dlg = new InputDialog(WebIRC.dialogMessages.channelNeedsKey(channel.toString()), true);
                dlg.setText(WebIRC.dialogMessages.enterTheKey());
                dlg.addCommand(new Command() {

                    public void execute() {
                        String key = dlg.getInputText();
                        sendCommand(new JoinCommand(channel, key));
                    }
                });
                dlg.show();
            } else if (command.isNoNicknameGiven() || command.isErroneusNickname() || command.isNicknameInUse() || command.isNickCollision() || command.isUnavailResource()) {
                String nickname = command.isNoNicknameGiven() ? "" : (String) command.getParameters().iterator().next();
                String msg = WebIRC.errorMessages.nicknameInUse(nickname);
                if (user.getNickname() != null) showError(msg); else showError(msg, new Command() {

                    public void execute() {
                        final ChangeNickDialog dlg = new ChangeNickDialog();
                        dlg.addDialogListener(new DialogListener() {

                            public void onFirstButtonPressed() {
                                sendCommand(new NickCommand(dlg.getNickname()));
                            }

                            public void onSecondButtonPressed() {
                            }
                        });
                        dlg.show();
                    }
                });
            } else {
                String message = "";
                String params = "";
                for (Iterator it = command.getParameters().iterator(); it.hasNext(); ) {
                    String param = (String) it.next();
                    if (it.hasNext()) {
                        if (params.length() > 0) params = params + ", ";
                        params = params + param;
                    } else message = param;
                }
                if (params.length() > 0) message = message + ".\n(" + params + ")"; else message = message + ".";
                showError(message);
            }
            messagePanel.addMessage(MessagePanel.SYSTEM_TAB, command);
        }

        public void onUnknownReplyCommand(UnknownReplyCommand command) {
            messagePanel.addMessage(MessagePanel.SYSTEM_TAB, command);
        }
    }

    private class SynchronizeEventsHandler implements SynchronizeListener {

        public void onReady() {
            SoundManager.playSound("sound/Done.wav");
            connectDialog.show();
        }

        public void onConnected() {
            connectingBox.hide();
            String connectMessage = "NICK " + selectedNick + "\r\n" + "USER user_name 0 * :" + selectedNick;
            sendMessage(connectMessage);
            setConnected(true);
            gui.initGUI();
        }

        public void onNotConnected() {
            connectingBox.hide();
            showFatalError("Couldn't connect to IRC server.");
        }
    }
}
