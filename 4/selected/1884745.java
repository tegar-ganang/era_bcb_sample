package logicswarm.net.irc;

import java.io.IOException;
import java.io.Serializable;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.FileNotFoundException;
import org.jibble.pircbot.DccChat;
import org.jibble.pircbot.DccFileTransfer;
import org.jibble.pircbot.User;

public class IrcModule extends Thread implements Serializable {

    public static final long serialVersionUID = 1;

    public IrcBot parent = null;

    public boolean status = true;

    public boolean verbose = true;

    public int botIndex = -1;

    public IrcModule(IrcBot owner) {
        parent = owner;
    }

    public IrcModule(IrcBot owner, boolean debug) {
        parent = owner;
        verbose = debug;
    }

    public void initialize(String strname) {
        setName(strname);
        onInitialize();
    }

    @Override
    public void finalize() {
    }

    protected void dispose() {
        this.finalize();
        interrupt();
    }

    public final void Activate() {
        status = true;
    }

    public final void Deactivate() {
        status = false;
    }

    public final void saveModule(String path) throws FileNotFoundException, IOException {
        ObjectOutputStream objout = new ObjectOutputStream(new FileOutputStream(path));
        objout.writeObject(this);
    }

    public final void log(String line) {
        if (verbose) parent.log("[" + getName() + "] " + line);
    }

    public boolean isModLoaded() {
        return (botIndex >= 0 && parent.isConnected());
    }

    public String[][] getChannels() {
        return parent.getModChannels(botIndex);
    }

    public void onNickInUse() {
    }

    public void onConnectError(IOException e) {
    }

    public void onHostUnreachable() {
    }

    public void onConnectionReset() {
    }

    public void onSendAction(String target, String action) {
    }

    public void onSendMessage(String target, String message) {
    }

    public void onSendNotice(String target, String notice) {
    }

    public void onInitialize() {
    }

    public void onAction(String sender, String login, String hostname, String target, String action) {
    }

    public void onChannelInfo(String channel, int userCount, String topic) {
    }

    public void onConnect() {
    }

    public void onDccChatRequest(String sourceNick, String sourceLogin, String sourceHostname, long address, int port) {
    }

    public void onDccSendRequest(String sourceNick, String sourceLogin, String sourceHostname, String filename, long address, int port, int size) {
    }

    public void onDeop(String channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient) {
    }

    public void onDeVoice(String channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient) {
    }

    public void onDisconnect() {
    }

    public void onFileTransferFinished(DccFileTransfer transfer, Exception e) {
    }

    public void onFinger(String sourceNick, String sourceLogin, String sourceHostname, String target) {
    }

    public void onIncomingChatRequest(DccChat chat) {
    }

    public void onIncomingFileTransfer(DccFileTransfer transfer) {
    }

    public void onInvite(String targetNick, String sourceNick, String sourceLogin, String sourceHostname, String channel) {
    }

    public void onJoin(String channel, String sender, String login, String hostname) {
    }

    public void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason) {
    }

    public void onMessage(String channel, String sender, String login, String hostname, String message) {
    }

    public void onMode(String channel, String sourceNick, String sourceLogin, String sourceHostname, String mode) {
    }

    public void onNickChange(String oldNick, String login, String hostname, String newNick) {
    }

    public void onNotice(String sourceNick, String sourceLogin, String sourceHostname, String target, String notice) {
    }

    public void onOp(String channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient) {
    }

    public void onPart(String channel, String sender, String login, String hostname) {
    }

    public void onPing(String sourceNick, String sourceLogin, String sourceHostname, String target, String pingValue) {
    }

    public void onPrivateMessage(String sender, String login, String hostname, String message) {
    }

    public void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason) {
    }

    public void onRemoveChannelBan(String channel, String sourceNick, String sourceLogin, String sourceHostname, String hostmask) {
    }

    public void onRemoveChannelKey(String channel, String sourceNick, String sourceLogin, String sourceHostname, String key) {
    }

    public void onRemoveChannelLimit(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    public void onRemoveInviteOnly(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    public void onRemoveModerated(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    public void onRemoveNoExternalMessages(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    public void onRemovePrivate(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    public void onRemoveSecret(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    public void onRemoveTopicProtection(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    public void onServerPing(String response) {
    }

    public void onServerResponse(int code, String response) {
    }

    public void onSetChannelBan(String channel, String sourceNick, String sourceLogin, String sourceHostname, String hostmask) {
    }

    public void onSetChannelKey(String channel, String sourceNick, String sourceLogin, String sourceHostname, String key) {
    }

    public void onSetChannelLimit(String channel, String sourceNick, String sourceLogin, String sourceHostname, int limit) {
    }

    public void onSetInviteOnly(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    public void onSetModerated(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    public void onSetNoExternalMessages(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    public void onSetPrivate(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    public void onSetSecret(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    public void onSetTopicProtection(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    public void onTime(String sourceNick, String sourceLogin, String sourceHostname, String target) {
    }

    public void onTopic(String channel, String topic) {
    }

    public void onTopic(String channel, String topic, String setBy, long date, boolean changed) {
    }

    public void onUnknown(String line) {
    }

    public void onUserList(String channel, User[] users) {
    }

    public void onUserMode(String targetNick, String sourceNick, String sourceLogin, String sourceHostname, String mode) {
    }

    public void onVersion(String sourceNick, String sourceLogin, String sourceHostname, String target) {
    }

    public void onVoice(String channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient) {
    }
}
