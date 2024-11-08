package com.buckosoft.fibs.BuckoFIBS;

import com.buckosoft.fibs.BuckoFIBS.AudioCue;
import com.buckosoft.fibs.BuckoFIBS.ClientReceiveParser.Mode;
import com.buckosoft.fibs.BuckoFIBS.gui.AboutDialog;
import com.buckosoft.fibs.BuckoFIBS.gui.MainDialog;
import com.buckosoft.fibs.BuckoFIBS.gui.PreferencesDialog;
import com.buckosoft.fibs.BuckoFIBS.gui.PreferencesForMessagesDialog;
import com.buckosoft.fibs.BuckoFIBS.gui.SystemMessagesTextPane;
import com.buckosoft.fibs.BuckoFIBS.gui.account.ConnectToServerDialog;
import com.buckosoft.fibs.BuckoFIBS.gui.account.CreateAccountDialog;
import com.buckosoft.fibs.BuckoFIBS.gui.account.CreateAccountStatusInterface;
import com.buckosoft.fibs.domain.Line;
import com.buckosoft.fibs.domain.Player;
import com.buckosoft.fibs.domain.SavedMatch;
import com.buckosoft.fibs.net.ClientConnection;
import com.buckosoft.fibs.net.FIBSMessages;

/** Dispatch commands through the system.
 * @author Dick Balaska
 * @since 2008/03/30
 * @version $Revision: 1.28 $ <br> $Date: 2009/03/16 18:52:17 $
 * @see <a href="http://cvs.buckosoft.com/Projects/BuckoFIBS/BuckoFIBS/src/com/buckosoft/fibs/BuckoFIBS/CommandDispatcherImpl.java">cvs CommandDispatcherImpl.java</a>
 */
public class CommandDispatcherImpl implements CommandDispatcher, FIBSMessages {

    private static final boolean DEBUG = false;

    private MainDialog mainDialog;

    private BFProperties properties;

    private ClientConnection clientConnection = null;

    private CreateAccountStatusInterface createAccountStatusInterface = null;

    private static final String eol = "\r\n";

    /** Set the reference to the instance of MainDialog
	 * @param mainDialog The MainDialog that is running
	 */
    public void setMainDialog(MainDialog mainDialog) {
        this.mainDialog = mainDialog;
    }

    /** Set the Properties/User preferences
	 * @param properties The loaded properties
	 */
    public void setProperties(BFProperties properties) {
        this.properties = properties;
    }

    /** Get a reference to our properties
	 * @return The properties object
	 */
    public BFProperties getProperties() {
        return (this.properties);
    }

    /** Primary entry point to the command dispatcher
	 * @param command The command to execute
	 */
    public void dispatch(Command command) {
        switch(command) {
            case SHOW_CONNECTION_DIALOG:
                onShowConnectionDialog();
                break;
            case SHOW_NEW_ACCOUNT_DIALOG:
                onShowNewAccountDialog();
                break;
            case DISCONNECT_FROM_NETWORK:
                onNetworkDisconnect();
                break;
            case CONNECT_TO_SERVER:
                onConnectToServer();
                break;
            case SHOW_PREFERENCES_DIALOG:
                onShowPreferencesDialog();
                break;
            case SHOW_PREFS4MSGS_DIALOG:
                onShowPrefs4MsgsDialog();
                break;
            case SHOW_ABOUT_DIALOG:
                onShowAboutDialog();
                break;
            case START_GAME:
                onStartGame();
                break;
            case SHUTTING_DOWN:
                onShutdown();
                break;
            case NETWORK_CONNECTED:
                onNetworkConnected();
                break;
            case WATCHING:
                onWatching();
                break;
            case ROLL_OR_DOUBLE:
                onRollOrDouble();
                break;
            case ACCEPT_OR_DECLINE_DOUBLE:
                onAcceptOrDeclineDouble();
                break;
            case TOGGLE_READY_TO_PLAY:
                onToggleReadyToPlay();
                break;
            case BEAR_OFF:
                this.mainDialog.getBoard().clearDice();
                this.mainDialog.playSound(AudioCue.PickUpDice);
                this.mainDialog.youCantMove();
                break;
            case SEND_ROLL:
                this.writeNetworkMessageln("roll");
                this.mainDialog.getBoard().setYourTurnToRollOrDouble(false);
                this.mainDialog.updateBoard();
                break;
            case SEND_DOUBLE:
                this.writeNetworkMessageln("double");
                this.mainDialog.getBoard().setYourTurnToRollOrDouble(false);
                this.mainDialog.updateBoard();
                break;
            case SEND_ACCEPT:
                this.writeNetworkMessageln("accept");
                break;
            case SEND_REJECT:
                this.writeNetworkMessageln("reject");
                break;
            default:
                writeSystemMessageln("Dispatcher: Unhandled command " + command);
                throw new RuntimeException("Unhandled command " + command);
        }
    }

    /** Primary entry point to the command dispatcher
	 * what takes a single string as an argument
	 * @param command The Command to Execute
	 * @param arg1 A string that the dispatched function wants
	 */
    public void dispatch(Command command, String arg1) {
        switch(command) {
            case SYSTEM_MESSAGE:
                writeSystemMessage(arg1);
                break;
            case PLAYER_GONE:
                this.mainDialog.getPlayerListPane().playerGone(arg1);
                this.mainDialog.getInviterTableModel().uninvited(arg1);
                break;
            case SAVED_MATCH:
                onSavedMatch(arg1);
                break;
            case MISS_MANNERS:
                onMissManners(arg1);
                break;
            case WATCH:
                onWatch(arg1);
                break;
            case GET_PLAYER_REPORT:
                onGetPlayerReport(arg1);
                break;
            case ACCEPT_INVITATION:
                onAcceptInvitation(arg1);
                this.mainDialog.getInviterTableModel().uninvited(arg1);
                break;
            case UNINVITED:
                onUninvited(arg1);
                break;
            case OWN_INFO:
                parseOwnInfo(arg1);
                break;
            case FIBS_BOARD:
                parseFibsBoard(arg1);
                break;
            case DOUBLE:
                break;
            case YOUR_MOVE:
                onYourMove(Integer.parseInt(arg1));
                break;
            case BAD_NEW_USER:
                this.createAccountStatusInterface.setStatus(arg1);
                this.createAccountStatusInterface.onRegisterFailure();
                break;
            case SEND_RESIGN:
                this.writeNetworkMessageln("resign " + arg1);
                break;
            case SEND_MOVE:
                this.mainDialog.getBoard().clearDice();
                this.mainDialog.playSound(AudioCue.PickUpDice);
                this.mainDialog.updateBoard();
                this.writeNetworkMessageln(arg1);
                this.mainDialog.writeGameMessageln(SystemMessagesTextPane.NETWORKOUT, arg1);
                break;
            case SEND_COMMAND:
                this.writeNetworkMessageln(arg1);
                break;
            default:
                writeSystemMessageln("Dispatcher: Unhandled command " + command);
                throw new RuntimeException("Unhandled command " + command);
        }
    }

    /** Primary entry point to the command dispatcher
	 * what takes two strings as an argument
	 * @param command The Command to Execute
	 * @param arg1 A string that the dispatched function wants
	 * @param arg2 Another string that the dispatched function wants
	 */
    public void dispatch(Command command, String arg1, String arg2) {
        switch(command) {
            case INVITED:
                onInvited(arg1, arg2);
                break;
            case INVITE:
                onInvite(arg1, arg2);
                break;
            case INVITE_WARNING:
                onInviteWarning(arg1, arg2);
                break;
            case MATCH_OVER:
                this.mainDialog.onMatchOver(arg1, arg2);
                break;
            default:
                writeSystemMessageln("Dispatcher: Unhandled command " + command);
                throw new RuntimeException("Unhandled command" + command);
        }
    }

    /** Primary entry point to the command dispatcher
	 * what takes an Object as an argument
	 * @param command The Command to Execute
	 * @param obj An Object that the dispatched function wants
	 */
    public void dispatch(Command command, Object obj) {
        boolean b;
        switch(command) {
            case GAME_MOVE:
                this.mainDialog.getGameManager().addLine((Line) obj);
                break;
            case PLAYER_CHANGED:
                onPlayerChanged((Player) obj);
                break;
            case REGISTER_NEW_USER:
                createAccountStatusInterface = (CreateAccountStatusInterface) obj;
                onRegisterNewUser();
                break;
            case READY_TO_PLAY:
                b = (Boolean) obj;
                this.mainDialog.setReadyToPlay(b);
                break;
            case TOGGLE_DOUBLE:
                b = (Boolean) obj;
                this.mainDialog.setAskDouble(b);
                break;
            case PLAY_CUE:
                AudioCue cue = (AudioCue) obj;
                this.mainDialog.playSound(cue);
                break;
            default:
                writeSystemMessageln("Dispatcher: Unhandled command " + command);
                throw new RuntimeException("Unhandled command" + command);
        }
    }

    /** Write a message to the System message pane in the normal color
	 * @param s The message to write
	 */
    public void writeSystemMessage(String s) {
        this.mainDialog.getSystemMessagesTextPane().appendMessage(s);
        this.mainDialog.systemMessagesScrollToBottom();
    }

    /** Write a message to the System message pane in the normal color.
	 * Terminate the message with a crlf.
	 * @param s The message to write
	 */
    public void writeSystemMessageln(String s) {
        this.mainDialog.getSystemMessagesTextPane().appendMessage(s + eol);
        this.mainDialog.systemMessagesScrollToBottom();
    }

    /** Write a message to the status widget in the registerUser dialog
	 * @param s The message to write
	 */
    public void writeRegisterUserMessage(String s) {
        if (this.createAccountStatusInterface != null) this.createAccountStatusInterface.setStatus(s);
    }

    /** Write a message to the System message pane in the specified color.
	 * @param type The style of the text
	 * @param s The message to write
	 */
    public void writeSystemMessage(int type, String s) {
        this.mainDialog.getSystemMessagesTextPane().appendMessage(type, s);
        this.mainDialog.systemMessagesScrollToBottom();
    }

    /** Write a message to the System message pane in the specified color.
	 * Terminate the message with a crlf.
	 * @param type The style of the text
	 * @param s The message to write
	 */
    public void writeSystemMessageln(int type, String s) {
        this.mainDialog.getSystemMessagesTextPane().appendMessage(type, s + eol);
        this.mainDialog.systemMessagesScrollToBottom();
        if (type == SystemMessagesTextPane.ERROR) this.mainDialog.setSystemMessagesTabVisible();
    }

    /** Write a message to the chat message pane in the normal color.
	 * Terminate the message with a crlf.
	 * @param name The user what sent the message
	 * @param cookie The mode used to send the message (shout, whisper, kibitz, etc)
	 * @param text The message to write
	 */
    public void writeChatMessageln(String name, int cookie, String text) {
        final String mode[] = { "says", "shouts", "whispers", "kibitzes", "say", "shout", "whisper", "kibitz" };
        int cookieMode = cookie - CLIP_SAYS;
        if (cookie == CLIP_YOU_SAY) {
            String[] ss = text.split(" ", 2);
            this.mainDialog.addChatMessage("You tell " + ss[0] + ": " + ss[1] + eol);
            return;
        }
        this.mainDialog.addChatMessage(name + " " + mode[cookieMode] + ": " + text + eol);
    }

    public void writeGameMessageln(String s) {
        this.mainDialog.writeGameMessageln(s);
    }

    /** Send a message to the fibs server
	 * @param s The message to send
	 * @return success
	 */
    public boolean writeNetworkMessage(String s) {
        if (this.clientConnection != null) {
            this.clientConnection.sendMessage(s);
            return (true);
        } else return (false);
    }

    /** Send a message to the fibs server appending a crlf to the end
	 * @param s The message to send
	 * @return success
	 */
    public boolean writeNetworkMessageln(String s) {
        return (this.writeNetworkMessage(s + eol));
    }

    private void onShowConnectionDialog() {
        ConnectToServerDialog dialog = new ConnectToServerDialog(mainDialog, this);
        dialog.setVisible(true);
    }

    private void onShowNewAccountDialog() {
        CreateAccountDialog dialog = new CreateAccountDialog(mainDialog, this);
        dialog.setVisible(true);
    }

    private void onConnectToServer() {
        if (createAccountStatusInterface != null) {
            createAccountStatusInterface.onRegisterSuccess();
            createAccountStatusInterface = null;
        }
        startClientConnection(Mode.Run);
    }

    private void onRegisterNewUser() {
        int profileId = this.properties.getSelectedProfile();
        String s = "Connecting to " + this.properties.getServerName(profileId) + ":" + this.properties.getServerPort(profileId);
        this.createAccountStatusInterface.setStatus(s);
        startClientConnection(Mode.Register);
    }

    private void startClientConnection(Mode mode) {
        if (this.clientConnection != null) {
            if (DEBUG) System.out.println("Tear down old ClientConnection");
            this.clientConnection.shutDown();
            this.clientConnection = null;
        }
        if (DEBUG) System.out.println("Create new ClientConnection");
        this.clientConnection = new ClientConnection();
        ClientReceiveParser clientReceiveParser = this.mainDialog.getClientReceiveParser();
        this.clientConnection.setClientAdapter(clientReceiveParser);
        this.clientConnection.setFibsAttributes(properties);
        clientReceiveParser.setClientConnection(this.clientConnection);
        clientReceiveParser.setMode(mode);
        this.clientConnection.start();
    }

    private void onShowPreferencesDialog() {
        PreferencesDialog dialog = new PreferencesDialog(mainDialog, this);
        dialog.setVisible(true);
    }

    private void onShowAboutDialog() {
        AboutDialog aboutDialog = new AboutDialog(mainDialog);
        aboutDialog.setVersion(Version.version);
        aboutDialog.setVisible(true);
    }

    private void onShowPrefs4MsgsDialog() {
        PreferencesForMessagesDialog dialog = new PreferencesForMessagesDialog(mainDialog, this);
        dialog.setVisible(true);
    }

    private void onShutdown() {
        onNetworkDisconnect();
    }

    private void onNetworkConnected() {
        this.writeNetworkMessageln("set boardstyle 3");
        this.mainDialog.setConnected(true);
    }

    private void onNetworkDisconnect() {
        if (this.clientConnection != null) {
            this.clientConnection.shutDown();
            this.clientConnection = null;
        }
        this.mainDialog.setConnected(false);
        this.mainDialog.getInviterTableModel().removeAll();
        this.mainDialog.getPlayerTableModel().removeAll();
    }

    private void onPlayerChanged(Player p) {
        p.setSavedMatch(this.mainDialog.getBoard().getSavedMatchString(p.getName()));
        p.setMissManners(this.mainDialog.getBoard().getMissMannersString(p.getName()));
        this.mainDialog.getDB().updateOpponent(p);
        this.mainDialog.getPlayerListPane().playerChanged(p);
        if (p.getOpponent().length() > 0) onUninvited(p.getName());
        this.mainDialog.getInviterTableModel().playerChanged(p);
        if (p.getName().equals(this.properties.getUserName())) {
            this.mainDialog.getBoard().setReady(p.isReady());
            this.mainDialog.redrawOwnInfo();
        }
    }

    private void onInvite(String playerName, String length) {
        this.writeNetworkMessageln("invite " + playerName + " " + length);
    }

    private void onSavedMatch(String s) {
        SavedMatch sm = this.mainDialog.getBoard().parseSavedMatch(s);
        Player p = this.mainDialog.getPlayerTableModel().getPlayer(sm.getOpponentName());
        if (p != null) {
            p.setSavedMatch(this.mainDialog.getBoard().getSavedMatchString(p.getName()));
            this.mainDialog.getPlayerListPane().playerChanged(p);
        }
    }

    private void onMissManners(String s) {
        SavedMatch sm = this.mainDialog.getBoard().parseMissManners(s);
        if (sm == null) return;
        Player p = this.mainDialog.getPlayerTableModel().getPlayer(sm.getOpponentName());
        if (p != null) {
            p.setMissManners(this.mainDialog.getBoard().getSavedMatchString(p.getName()));
            this.mainDialog.getPlayerListPane().playerChanged(p);
        }
    }

    private void onGetPlayerReport(String playerName) {
        this.mainDialog.getPlayerReportPane().setPlayer(playerName);
    }

    private void onWatch(String playerName) {
        this.writeNetworkMessageln("watch " + playerName);
        if (false) {
            Player p;
            p = this.mainDialog.getPlayerTableModel().getPlayer(playerName);
            Player p1;
            p1 = this.mainDialog.getDB().getPlayer(playerName);
            if (p1 == null) this.mainDialog.getDB().store(p);
            System.out.println("playerId = " + p.getId());
        }
    }

    private void onWatching() {
        this.mainDialog.setBoardTabVisible();
        this.mainDialog.getGameManager().reset();
        this.writeNetworkMessageln("board");
    }

    private void onAcceptInvitation(String playerName) {
        this.writeNetworkMessageln("join " + playerName);
    }

    private void onRollOrDouble() {
        this.mainDialog.getBoard().setYourTurnToRollOrDouble(true);
        this.mainDialog.playSound(AudioCue.RollOrDouble);
        this.mainDialog.updateBoard();
    }

    private void onAcceptOrDeclineDouble() {
        this.mainDialog.getBoard().setAcceptDeclineDouble();
        this.mainDialog.updateBoard();
    }

    private void onToggleReadyToPlay() {
        this.writeNetworkMessageln("toggle ready");
    }

    private void onInvited(String playerName, String matchLength) {
        Player p = this.mainDialog.getPlayerTableModel().getPlayer(playerName);
        if (p == null) {
            this.writeSystemMessageln(SystemMessagesTextPane.ERROR, "Unknown player '" + playerName + "' tried to invite you");
            p = new Player();
            p.setName(playerName);
            this.dispatch(CommandDispatcher.Command.PLAYER_CHANGED, p);
            this.writeNetworkMessageln("who " + playerName);
        }
        this.mainDialog.playSound(AudioCue.Invited);
        this.mainDialog.getInviterTableModel().invited(p, matchLength);
        this.mainDialog.setInvitedTabVisible();
    }

    private void onInviteWarning(String playerName, String warning) {
        this.mainDialog.getInviterTableModel().setWarning(playerName, warning);
    }

    private void onUninvited(String playerName) {
        this.mainDialog.getInviterTableModel().uninvited(playerName);
    }

    private void onStartGame() {
        this.mainDialog.getGameManager().reset();
        this.mainDialog.getBoard().resetGame();
        this.mainDialog.redrawOwnInfo();
        this.mainDialog.setBoardTabVisible();
        this.writeNetworkMessageln("board");
    }

    private void onYourMove(int diceToMove) {
        this.mainDialog.getBoard().setYourTurnToRollOrDouble(false);
        this.mainDialog.getGameManager().yourMove(diceToMove);
    }

    private void parseFibsBoard(String board) {
        this.mainDialog.getBoard().parseFibsBoard(board);
        this.mainDialog.updateBoard();
    }

    private void parseOwnInfo(String s) {
        this.mainDialog.getBoard().parseOwnInfo(s);
        if (this.properties.isFibsReadyToPlay() != this.mainDialog.getBoard().isReady()) this.writeNetworkMessageln("toggle ready"); else this.mainDialog.setReadyToPlay(this.properties.isFibsReadyToPlay());
        this.mainDialog.redrawOwnInfo();
    }

    @Override
    public void buttonPressed(Button button) {
        if (button == Button.First) this.mainDialog.getGameManager().goFirst(); else if (button == Button.Prev) this.mainDialog.getGameManager().goPrev(); else if (button == Button.Next) this.mainDialog.getGameManager().goNext(); else if (button == Button.Last) this.mainDialog.getGameManager().goLast(); else if (button == Button.Undo) this.mainDialog.getGameManager().goUndo(); else if (button == Button.Greedy) this.writeNetworkMessageln("toggle greedy"); else if (button == Button.AskDoubles) this.writeNetworkMessageln("toggle double"); else if (button == Button.Resign) this.mainDialog.showResignOutDialog();
    }

    @Override
    public void ropChanged(boolean[] rop) {
        this.properties.setROP(rop);
        this.mainDialog.getPlayerListPane().setROP(rop);
    }
}
