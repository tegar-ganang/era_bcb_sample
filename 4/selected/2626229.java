package logicswarm.net.irc;

import java.io.IOException;
import logicswarm.net.irc.datatypes.ircData;
import org.jibble.pircbot.DccChat;
import org.jibble.pircbot.DccFileTransfer;
import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.User;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.Calendar;
import logicswarm.util.Arrays;

public class IrcBot extends PircBot implements Serializable {

    public static final long serialVersionUID = 1;

    public static final double version = 0.30;

    private String strHost;

    private String strPass;

    private int intPort;

    private IrcModule[] objMods;

    private String[][][] modChannels;

    private boolean[] modsUseAllChannels;

    private IrcFilter[] objFilters;

    private String[][][] filterChannels;

    private boolean[] filtUseAllChannels;

    private String[][] channelCache;

    private boolean verbose = false;

    private boolean modsOverride = false;

    private boolean filterOverride = true;

    public IrcBot(String name, String server, String pass, int port) {
        setName(name);
        strHost = server;
        strPass = pass;
        intPort = port;
        initialize();
    }

    public IrcBot(String name, String server, String pass, int port, boolean verb) {
        setName(name);
        strHost = server;
        strPass = pass;
        intPort = port;
        verbose = verb;
        initialize();
    }

    private void initialize() {
        objMods = new IrcModule[0];
        modChannels = new String[0][0][0];
        modsUseAllChannels = new boolean[0];
        objFilters = new IrcFilter[0];
        filterChannels = new String[0][0][0];
        filtUseAllChannels = new boolean[0];
        setVerbose(verbose);
        _login = "Sodal";
        _version = "Sodal " + version + " Java IRC Bot - http://code.google.com/p/sodal/";
        _finger = "You ought to be arrested for fingering a bot, pervert!";
    }

    @Override
    protected void finalize() {
        for (int i = 0; i < objFilters.length; i++) objFilters[i].dispose();
        for (int i = 0; i < objMods.length; i++) objMods[i].dispose();
    }

    public final void verbose(boolean verb) {
        verbose = verb;
        super.setVerbose(verb);
    }

    public final boolean isUserInChannel(String nick, String chan) {
        User[] list = this.getUsers(chan);
        for (int i = 0; i < list.length; i++) {
            if (list[i].getNick().equalsIgnoreCase(nick)) {
                return true;
            }
        }
        return false;
    }

    public final int addMod(IrcModule mod) {
        IrcModule[] temp = new IrcModule[objMods.length + 1];
        int newLen = objMods.length + 1;
        for (int i = 0; i < objMods.length; i++) temp[i] = objMods[i];
        temp[newLen - 1] = mod;
        temp[newLen - 1].botIndex = (newLen - 1);
        objMods = temp;
        String[][][] tmp = { { { null, null } } };
        modChannels = Arrays.appendArray(modChannels, tmp);
        modsUseAllChannels = Arrays.appendArray(modsUseAllChannels, true);
        log("Loaded module: '" + mod.getName() + "' at index " + (newLen - 1));
        return (newLen - 1);
    }

    public final void removeMod(int index) {
        String str = "unspecified";
        IrcModule[] temp = new IrcModule[objMods.length - 1];
        for (int i = 0, j = 0; i < (objMods.length); i++) {
            if (i != index) {
                temp[j++] = objMods[i];
            } else {
                str = objMods[i].getName();
            }
        }
        objMods = temp;
        log("Removed module: '" + str + "' at index " + index);
    }

    public final boolean activateMod(int index) {
        if (index >= objMods.length) {
            return false;
        } else {
            objMods[index].Activate();
            log("Activated module: '" + objMods[index].getName() + "' at index " + index);
            return true;
        }
    }

    public final boolean deactivateMod(int index) {
        if (index >= objMods.length) {
            return false;
        } else {
            objMods[index].Deactivate();
            log("Deactivated module: '" + objMods[index].getName() + "' at index " + index);
            return true;
        }
    }

    public final IrcModule getModule(int index) {
        return objMods[index];
    }

    public final IrcModule getModule(String name) {
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].getName().equals(name)) return objMods[i];
        }
        return null;
    }

    public final int countMods() {
        return objMods.length;
    }

    public final void loadModule(String path) throws IOException, ClassNotFoundException {
        ObjectInputStream objin = new ObjectInputStream(new FileInputStream(path));
        IrcModule mod = (IrcModule) objin.readObject();
        if (mod instanceof IrcModule) addMod(mod);
    }

    public final void saveModule(String path, int index) throws FileNotFoundException, IOException {
        ObjectOutputStream objout = new ObjectOutputStream(new FileOutputStream(path));
        objout.writeObject(objMods[index]);
    }

    public final void addModChannel(int index, String chan) {
        addModChannel(index, chan, "");
    }

    public final void addModChannel(int index, String chan, String pass) {
        if (modChannels[index] == null) {
            String[][] out = { { chan, pass } };
            modChannels[index] = out;
        } else if (modHasChannel(index, chan)) {
            return;
        } else {
            String[][] out = { { chan, pass } };
            modChannels[index] = Arrays.appendArray(modChannels[index], out);
        }
    }

    public final void addModChannels(int index, String[][] chanArray) {
        for (int i = 0; i < chanArray.length; i++) {
            addModChannel(index, chanArray[i][0]);
        }
    }

    public final void setModChannels(int index, String[][] chan) {
        modChannels[index] = chan;
    }

    public final boolean modHasChannel(int index, String chan) {
        if (modsUseAllChannels[index]) {
            if (channelCache == null || channelCache.length <= 0) {
                return false;
            } else {
                for (int i = 0; i < channelCache.length; i++) {
                    if (channelCache[i][0] != null && channelCache[i][0].equals(chan)) return true;
                }
            }
        } else {
            if (modChannels == null || modChannels.length <= 0) {
                return false;
            } else {
                for (int i = 0; i < modChannels[index].length; i++) {
                    if (modChannels[index][i][0] != null && modChannels[index][i][0].equalsIgnoreCase(chan)) return true;
                }
            }
        }
        return false;
    }

    public final void applyModToAllChannels(int index, boolean state) {
        modsUseAllChannels[index] = state;
    }

    public final boolean userIsInModChannel(int index, String nick) {
        for (int i = 0; i < modChannels.length; i++) {
            if (isUserInChannel(nick, modChannels[index][i][0])) {
                return true;
            }
        }
        return false;
    }

    public final String[][] getModChannels(int index) {
        if (modsUseAllChannels[index]) {
            return channelCache;
        } else {
            return modChannels[index];
        }
    }

    public final void addFilter(IrcFilter mod) {
        IrcFilter[] temp = new IrcFilter[objFilters.length + 1];
        int newLen = objFilters.length + 1;
        for (int i = 0; i < objFilters.length; i++) temp[i] = objFilters[i];
        temp[newLen - 1] = mod;
        objFilters = temp;
        String[][][] tmp = { { { null, null } } };
        filterChannels = Arrays.appendArray(filterChannels, tmp);
        filtUseAllChannels = Arrays.appendArray(filtUseAllChannels, true);
        log("Loaded filter: '" + mod.getName() + "' at index " + (newLen - 1));
    }

    public final void removeFilter(int index) {
        String str = "unspecified";
        IrcFilter[] temp = new IrcFilter[objFilters.length - 1];
        for (int i = 0, j = 0; i < (objFilters.length); i++) {
            if (i != index) {
                temp[j++] = objFilters[i];
            } else {
                str = objFilters[i].getName();
            }
        }
        objFilters = temp;
        log("Removed filter: '" + str + "' at index " + index);
    }

    public final boolean activateFilter(int index) {
        if (index >= objFilters.length) {
            return false;
        } else {
            objFilters[index].Activate();
            log("Activated filter: '" + objFilters[index].getName() + "' at index " + index);
            return true;
        }
    }

    public final boolean deactivateFilter(int index) {
        if (index >= objFilters.length) {
            return false;
        } else {
            objFilters[index].Deactivate();
            log("Deactivated module: '" + objFilters[index].getName() + "' at index " + index);
            return true;
        }
    }

    public final IrcFilter getFilter(int index) {
        return objFilters[index];
    }

    public final IrcFilter getFilter(String name) {
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].getName().equals(name)) return objFilters[i];
        }
        return null;
    }

    public final int countFilters() {
        return objFilters.length;
    }

    public final void addFilterChannel(int index, String chan) {
        addFilterChannel(index, chan, "");
    }

    public final void addFilterChannel(int index, String chan, String pass) {
        if (filterChannels[index] == null) {
            String[][] out = { { chan, pass } };
            filterChannels[index] = out;
        } else if (filterHasChannel(index, chan)) {
            return;
        } else {
            String[][] out = { { chan, pass } };
            filterChannels[index] = Arrays.appendArray(filterChannels[index], out);
        }
    }

    public final void addFilterChannels(int index, String[][] chanArray) {
        for (int i = 0; i < chanArray.length; i++) {
            addFilterChannel(index, chanArray[i][0]);
        }
    }

    public final void setFilterChannels(int index, String[][] chan) {
        modChannels[index] = chan;
    }

    public final boolean filterHasChannel(int index, String chan) {
        if (filtUseAllChannels[index]) {
            if (getChannels() == null || getChannels().length <= 0) {
                return false;
            } else {
                for (int i = 0; i < getChannels().length; i++) {
                    if (getChannels()[i].equalsIgnoreCase(chan)) return true;
                }
            }
        } else {
            if (filterChannels == null || filterChannels.length <= 0) {
                return false;
            } else {
                for (int i = 0; i < filterChannels[index].length; i++) {
                    if (filterChannels[index][i][0] != null && filterChannels[index][i][0].equalsIgnoreCase(chan)) return true;
                }
            }
        }
        return false;
    }

    public final void applyFilterToAllChannels(int index, boolean state) {
        filtUseAllChannels[index] = state;
    }

    public final boolean userIsInFilterChannel(int index, String nick) {
        for (int i = 0; i < filterChannels.length; i++) {
            if (isUserInChannel(nick, filterChannels[index][i][0])) {
                return true;
            }
        }
        return false;
    }

    public final String[][] getFilterChannels(int index) {
        return filterChannels[index];
    }

    public final boolean connect() {
        try {
            this.connect(strHost, intPort, strPass);
        } catch (NickAlreadyInUseException e) {
            log("Connect abandoned, nick already in use.");
            onNickInUse();
            return false;
        } catch (IOException e) {
            onConnectError(e);
            if (e.getMessage().equals(strHost)) {
                log("Connection error, host unreachable.");
                onHostUnreachable();
            } else if (e.getMessage().equalsIgnoreCase("connection reset")) {
                log("Connection error, connection was reset.");
                onConnectionReset();
            } else if (e.getMessage().equals("connect")) {
                log("Connection error, host unreachable.");
                onHostUnreachable();
            } else {
                log("Connect abandoned, IOException occurred: " + e.getMessage());
            }
            return false;
        } catch (IrcException e) {
            log("Connect abandoned, IrcException occurred: " + e.getMessage());
            return false;
        }
        return true;
    }

    public final void onNickInUse() {
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status) objMods[i].onNickInUse();
        }
    }

    public final void onConnectError(IOException e) {
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status) objMods[i].onConnectError(e);
        }
    }

    public final void onHostUnreachable() {
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status) objMods[i].onHostUnreachable();
        }
    }

    public final void onConnectionReset() {
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status) objMods[i].onConnectionReset();
        }
    }

    public final void SendAction(String target, String action) {
        if (!filterOverride && !modsOverride) super.sendAction(target, action);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onSendAction(target, action);
                target = str[0];
                action = str[1];
            }
        }
        if (filterOverride && !modsOverride) super.sendAction(target, action);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status) objMods[i].onSendAction(target, action);
        }
    }

    public final void SendMessage(String target, String message) {
        if (!filterOverride && !modsOverride) super.sendMessage(target, message);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onSendMessage(target, message);
                target = str[0];
                message = str[1];
            }
        }
        if (filterOverride && !modsOverride) super.sendMessage(target, message);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status) objMods[i].onSendMessage(target, message);
        }
    }

    public final void SendNotice(String target, String notice) {
        if (!filterOverride && !modsOverride) super.sendNotice(target, notice);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onSendNotice(target, notice);
                target = str[0];
                notice = str[1];
            }
        }
        if (filterOverride && !modsOverride) super.sendNotice(target, notice);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status) objMods[i].onSendNotice(target, notice);
        }
    }

    public final void joinChannels(String[][] chan) {
        for (int i = 0; i < chan.length; i++) {
            log("Joining Channel : '" + chan[i][0] + "' with pass '" + "'");
            this.joinChannel(chan[i][0], chan[i][1]);
        }
    }

    @Override
    public void log(String line) {
        if (verbose) System.out.println("[" + Calendar.HOUR + ":" + Calendar.MINUTE + ":" + Calendar.SECOND + "." + Calendar.MILLISECOND + "] " + line);
    }

    public synchronized void dispose() {
        log(_name + " is being disposed...");
        finalize();
        super.dispose();
        System.exit(0);
    }

    @Override
    public final void onAction(String sender, String login, String hostname, String target, String action) {
        if (!filterOverride && !modsOverride) super.onAction(sender, login, hostname, target, action);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onAction(sender, login, hostname, target, action);
                sender = str[0];
                login = str[1];
                hostname = str[2];
                target = str[3];
                action = str[4];
            }
        }
        if (filterOverride && !modsOverride) super.onAction(sender, login, hostname, target, action);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status) objMods[i].onAction(sender, login, hostname, target, action);
        }
    }

    @Override
    public final void onChannelInfo(String channel, int userCount, String topic) {
        if (!filterOverride && !modsOverride) super.onChannelInfo(channel, userCount, topic);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                ircData data = objFilters[i].onChannelInfo(channel, userCount, topic);
                channel = data.channel;
                userCount = data.userCount;
                topic = data.topic;
            }
        }
        if (filterOverride && !modsOverride) super.onChannelInfo(channel, userCount, topic);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status && modHasChannel(i, channel)) objMods[i].onChannelInfo(channel, userCount, topic);
        }
    }

    @Override
    public final void onConnect() {
        if (!modsOverride) super.onConnect();
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status) objMods[i].onConnect();
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public final void onDccChatRequest(String sourceNick, String sourceLogin, String sourceHostname, long address, int port) {
        if (!filterOverride && !modsOverride) super.onDccChatRequest(sourceNick, sourceLogin, sourceHostname, address, port);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                ircData data = objFilters[i].onDccChatRequest(sourceNick, sourceLogin, sourceHostname, address, port);
                sourceNick = data.sourceNick;
                sourceLogin = data.sourceLogin;
                sourceHostname = data.sourceHostname;
                address = data.address;
                port = data.port;
            }
        }
        if (filterOverride && !modsOverride) super.onDccChatRequest(sourceNick, sourceLogin, sourceHostname, address, port);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status) objMods[i].onDccChatRequest(sourceNick, sourceLogin, sourceHostname, address, port);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public final void onDccSendRequest(String sourceNick, String sourceLogin, String sourceHostname, String filename, long address, int port, int size) {
        if (!filterOverride && !modsOverride) super.onDccSendRequest(sourceNick, sourceLogin, sourceHostname, filename, address, port, size);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                ircData data = objFilters[i].onDccSendRequest(sourceNick, sourceLogin, sourceHostname, filename, address, port, size);
                sourceNick = data.sourceNick;
                sourceLogin = data.sourceLogin;
                sourceHostname = data.sourceHostname;
                filename = data.filename;
                address = data.address;
                port = data.port;
                size = data.size;
            }
        }
        if (filterOverride && !modsOverride) super.onDccSendRequest(sourceNick, sourceLogin, sourceHostname, filename, address, port, size);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status) objMods[i].onDccSendRequest(sourceNick, sourceLogin, sourceHostname, filename, address, port, size);
        }
    }

    @Override
    public final void onDeop(String channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient) {
        if (!filterOverride && !modsOverride) super.onDeop(channel, sourceNick, sourceLogin, sourceHostname, recipient);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onDeop(channel, sourceNick, sourceLogin, sourceHostname, recipient);
                channel = str[0];
                sourceLogin = str[1];
                sourceHostname = str[2];
                recipient = str[3];
            }
        }
        if (filterOverride && !modsOverride) super.onDeop(channel, sourceNick, sourceLogin, sourceHostname, recipient);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status && modHasChannel(i, channel)) objMods[i].onDeop(channel, sourceNick, sourceLogin, sourceHostname, recipient);
        }
    }

    @Override
    public final void onDeVoice(String channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient) {
        if (!filterOverride && !modsOverride) super.onDeVoice(channel, sourceNick, sourceLogin, sourceHostname, recipient);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onDeVoice(channel, sourceNick, sourceLogin, sourceHostname, recipient);
                channel = str[0];
                sourceLogin = str[1];
                sourceHostname = str[2];
                recipient = str[3];
            }
        }
        if (filterOverride && !modsOverride) super.onDeVoice(channel, sourceNick, sourceLogin, sourceHostname, recipient);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status && modHasChannel(i, channel)) objMods[i].onDeVoice(channel, sourceNick, sourceLogin, sourceHostname, recipient);
        }
    }

    @Override
    public final void onDisconnect() {
        if (!modsOverride) super.onDisconnect();
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status) objMods[i].onDisconnect();
        }
    }

    @Override
    public final void onFileTransferFinished(DccFileTransfer transfer, Exception e) {
        if (!filterOverride && !modsOverride) super.onFileTransferFinished(transfer, e);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                ircData data = objFilters[i].onFileTransferFinished(transfer, e);
                transfer = data.transfer;
                e = data.e;
            }
        }
        if (filterOverride && !modsOverride) super.onFileTransferFinished(transfer, e);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status) objMods[i].onFileTransferFinished(transfer, e);
        }
    }

    @Override
    public final void onFinger(String sourceNick, String sourceLogin, String sourceHostname, String target) {
        if (!filterOverride && !modsOverride) super.onFinger(sourceNick, sourceLogin, sourceHostname, target);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onFinger(sourceNick, sourceLogin, sourceHostname, target);
                sourceNick = str[0];
                sourceLogin = str[1];
                sourceHostname = str[2];
                target = str[3];
            }
        }
        if (filterOverride && !modsOverride) super.onFinger(sourceNick, sourceLogin, sourceHostname, target);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status) objMods[i].onFinger(sourceNick, sourceLogin, sourceHostname, target);
        }
    }

    @Override
    public final void onIncomingChatRequest(DccChat chat) {
        if (!filterOverride && !modsOverride) super.onIncomingChatRequest(chat);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                chat = objFilters[i].onIncomingChatRequest(chat);
            }
        }
        if (filterOverride && !modsOverride) super.onIncomingChatRequest(chat);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status) objMods[i].onIncomingChatRequest(chat);
        }
    }

    @Override
    public final void onIncomingFileTransfer(DccFileTransfer transfer) {
        if (!filterOverride && !modsOverride) super.onIncomingFileTransfer(transfer);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                transfer = objFilters[i].onIncomingFileTransfer(transfer);
            }
        }
        if (filterOverride && !modsOverride) super.onIncomingFileTransfer(transfer);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status) objMods[i].onIncomingFileTransfer(transfer);
        }
    }

    @Override
    public final void onInvite(String targetNick, String sourceNick, String sourceLogin, String sourceHostname, String channel) {
        if (!filterOverride && !modsOverride) super.onInvite(targetNick, sourceNick, sourceLogin, sourceHostname, channel);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onInvite(targetNick, sourceNick, sourceLogin, sourceHostname, channel);
                targetNick = str[0];
                sourceNick = str[1];
                sourceLogin = str[2];
                sourceHostname = str[3];
                channel = str[4];
            }
        }
        if (filterOverride && !modsOverride) super.onInvite(targetNick, sourceNick, sourceLogin, sourceHostname, channel);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status && modHasChannel(i, channel)) objMods[i].onInvite(targetNick, sourceNick, sourceLogin, sourceHostname, channel);
        }
    }

    @Override
    public final void onJoin(String channel, String sender, String login, String hostname) {
        channelCache = Arrays.uniqueAppendArray(channelCache, channel);
        if (!filterOverride && !modsOverride) super.onJoin(channel, sender, login, hostname);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onJoin(channel, sender, login, hostname);
                channel = str[0];
                sender = str[1];
                login = str[2];
                hostname = str[3];
            }
        }
        if (filterOverride && !modsOverride) super.onJoin(channel, sender, login, hostname);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status && modHasChannel(i, channel)) objMods[i].onJoin(channel, sender, login, hostname);
        }
    }

    @Override
    public final void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason) {
        if (!filterOverride && !modsOverride) super.onKick(channel, kickerNick, kickerLogin, kickerHostname, recipientNick, reason);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onKick(channel, kickerNick, kickerLogin, kickerHostname, recipientNick, reason);
                channel = str[0];
                kickerNick = str[1];
                kickerLogin = str[2];
                kickerHostname = str[3];
                recipientNick = str[3];
                reason = str[3];
            }
        }
        if (filterOverride && !modsOverride) super.onKick(channel, kickerNick, kickerLogin, kickerHostname, recipientNick, reason);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status && modHasChannel(i, channel)) objMods[i].onKick(channel, kickerNick, kickerLogin, kickerHostname, recipientNick, reason);
        }
        channelCache = Arrays.removeFromArray(channelCache, channel);
    }

    @Override
    public final void onMessage(String channel, String sender, String login, String hostname, String message) {
        if (!filterOverride && !modsOverride) super.onMessage(channel, sender, login, hostname, message);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onMessage(channel, sender, login, hostname, message);
                channel = str[0];
                sender = str[1];
                login = str[2];
                hostname = str[3];
                message = str[4];
            }
        }
        if (filterOverride && !modsOverride) super.onMessage(channel, sender, login, hostname, message);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status && modHasChannel(i, channel)) objMods[i].onMessage(channel, sender, login, hostname, message);
        }
    }

    @Override
    public final void onMode(String channel, String sourceNick, String sourceLogin, String sourceHostname, String mode) {
        if (!filterOverride && !modsOverride) super.onMode(channel, sourceNick, sourceLogin, sourceHostname, mode);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onMode(channel, sourceNick, sourceLogin, sourceHostname, mode);
                channel = str[0];
                sourceNick = str[1];
                sourceLogin = str[2];
                sourceHostname = str[3];
                mode = str[3];
            }
        }
        if (filterOverride && !modsOverride) super.onMode(channel, sourceNick, sourceLogin, sourceHostname, mode);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status && modHasChannel(i, channel)) objMods[i].onMode(channel, sourceNick, sourceLogin, sourceHostname, mode);
        }
    }

    @Override
    public final void onNickChange(String oldNick, String login, String hostname, String newNick) {
        if (!filterOverride && !modsOverride) super.onNickChange(oldNick, login, hostname, newNick);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onNickChange(oldNick, login, hostname, newNick);
                oldNick = str[0];
                login = str[1];
                hostname = str[2];
                newNick = str[3];
            }
        }
        if (filterOverride && !modsOverride) super.onNickChange(oldNick, login, hostname, newNick);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status) objMods[i].onNickChange(oldNick, login, hostname, newNick);
        }
    }

    @Override
    public final void onNotice(String sourceNick, String sourceLogin, String sourceHostname, String target, String notice) {
        if (!filterOverride && !modsOverride) super.onNotice(sourceNick, sourceLogin, sourceHostname, target, notice);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onNotice(sourceNick, sourceLogin, sourceHostname, target, notice);
                sourceNick = str[0];
                sourceLogin = str[1];
                sourceHostname = str[2];
                target = str[3];
                notice = str[4];
            }
        }
        if (filterOverride && !modsOverride) super.onNotice(sourceNick, sourceLogin, sourceHostname, target, notice);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status) objMods[i].onNotice(sourceNick, sourceLogin, sourceHostname, target, notice);
        }
    }

    @Override
    public final void onOp(String channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient) {
        if (!filterOverride && !modsOverride) super.onOp(channel, sourceNick, sourceLogin, sourceHostname, recipient);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onOp(channel, sourceNick, sourceLogin, sourceHostname, recipient);
                channel = str[0];
                sourceNick = str[1];
                sourceLogin = str[2];
                sourceHostname = str[3];
                recipient = str[3];
            }
        }
        if (filterOverride && !modsOverride) super.onOp(channel, sourceNick, sourceLogin, sourceHostname, recipient);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status && modHasChannel(i, channel)) objMods[i].onOp(channel, sourceNick, sourceLogin, sourceHostname, recipient);
        }
    }

    @Override
    public final void onPart(String channel, String sender, String login, String hostname) {
        if (!filterOverride && !modsOverride) super.onPart(channel, sender, login, hostname);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onPart(channel, sender, login, hostname);
                channel = str[0];
                sender = str[1];
                login = str[2];
                hostname = str[3];
            }
        }
        if (filterOverride && !modsOverride) super.onPart(channel, sender, login, hostname);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status && modHasChannel(i, channel)) objMods[i].onPart(channel, sender, login, hostname);
        }
        channelCache = Arrays.removeFromArray(channelCache, channel);
    }

    @Override
    public final void onPing(String sourceNick, String sourceLogin, String sourceHostname, String target, String pingValue) {
        if (!filterOverride && !modsOverride) super.onPing(sourceNick, sourceLogin, sourceHostname, target, pingValue);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onPing(sourceNick, sourceLogin, sourceHostname, target, pingValue);
                sourceNick = str[0];
                sourceLogin = str[1];
                sourceHostname = str[2];
                target = str[3];
                pingValue = str[4];
            }
        }
        if (filterOverride && !modsOverride) super.onPing(sourceNick, sourceLogin, sourceHostname, target, pingValue);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status) objMods[i].onPing(sourceNick, sourceLogin, sourceHostname, target, pingValue);
        }
    }

    @Override
    public final void onPrivateMessage(String sender, String login, String hostname, String message) {
        if (!filterOverride && !modsOverride) super.onPrivateMessage(sender, login, hostname, message);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onPrivateMessage(sender, login, hostname, message);
                sender = str[0];
                login = str[1];
                hostname = str[2];
                message = str[3];
            }
        }
        if (filterOverride && !modsOverride) super.onPrivateMessage(sender, login, hostname, message);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status) objMods[i].onPrivateMessage(sender, login, hostname, message);
        }
    }

    @Override
    public final void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason) {
        if (!filterOverride && !modsOverride) super.onQuit(sourceNick, sourceLogin, sourceHostname, reason);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onQuit(sourceNick, sourceLogin, sourceHostname, reason);
                sourceNick = str[0];
                sourceLogin = str[1];
                sourceHostname = str[2];
                reason = str[3];
            }
        }
        if (filterOverride && !modsOverride) super.onQuit(sourceNick, sourceLogin, sourceHostname, reason);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status) objMods[i].onQuit(sourceNick, sourceLogin, sourceHostname, reason);
        }
    }

    @Override
    public final void onRemoveChannelBan(String channel, String sourceNick, String sourceLogin, String sourceHostname, String hostmask) {
        if (!filterOverride && !modsOverride) super.onRemoveChannelBan(channel, sourceNick, sourceLogin, sourceHostname, hostmask);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onRemoveChannelBan(channel, sourceNick, sourceLogin, sourceHostname, hostmask);
                channel = str[0];
                sourceNick = str[1];
                sourceLogin = str[2];
                sourceHostname = str[3];
                hostmask = str[4];
            }
        }
        if (filterOverride && !modsOverride) super.onRemoveChannelBan(channel, sourceNick, sourceLogin, sourceHostname, hostmask);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status && modHasChannel(i, channel)) objMods[i].onRemoveChannelBan(channel, sourceNick, sourceLogin, sourceHostname, hostmask);
        }
    }

    @Override
    public final void onRemoveChannelKey(String channel, String sourceNick, String sourceLogin, String sourceHostname, String key) {
        if (!filterOverride && !modsOverride) super.onRemoveChannelKey(channel, sourceNick, sourceLogin, sourceHostname, key);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onRemoveChannelKey(channel, sourceNick, sourceLogin, sourceHostname, key);
                channel = str[0];
                sourceNick = str[1];
                sourceLogin = str[2];
                sourceHostname = str[3];
                key = str[4];
            }
        }
        if (filterOverride && !modsOverride) super.onRemoveChannelKey(channel, sourceNick, sourceLogin, sourceHostname, key);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status && modHasChannel(i, channel)) objMods[i].onRemoveChannelKey(channel, sourceNick, sourceLogin, sourceHostname, key);
        }
    }

    @Override
    public final void onRemoveChannelLimit(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
        if (!filterOverride && !modsOverride) super.onRemoveChannelLimit(channel, sourceNick, sourceLogin, sourceHostname);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onRemoveChannelLimit(channel, sourceNick, sourceLogin, sourceHostname);
                channel = str[0];
                sourceNick = str[1];
                sourceLogin = str[2];
                sourceHostname = str[3];
            }
        }
        if (filterOverride && !modsOverride) super.onRemoveChannelLimit(channel, sourceNick, sourceLogin, sourceHostname);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status && modHasChannel(i, channel)) objMods[i].onRemoveChannelLimit(channel, sourceNick, sourceLogin, sourceHostname);
        }
    }

    @Override
    public final void onRemoveInviteOnly(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
        if (!filterOverride && !modsOverride) super.onRemoveInviteOnly(channel, sourceNick, sourceLogin, sourceHostname);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onRemoveInviteOnly(channel, sourceNick, sourceLogin, sourceHostname);
                channel = str[0];
                sourceNick = str[1];
                sourceLogin = str[2];
                sourceHostname = str[3];
            }
        }
        if (filterOverride && !modsOverride) super.onRemoveInviteOnly(channel, sourceNick, sourceLogin, sourceHostname);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status && modHasChannel(i, channel)) objMods[i].onRemoveInviteOnly(channel, sourceNick, sourceLogin, sourceHostname);
        }
    }

    @Override
    public final void onRemoveModerated(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
        if (!filterOverride && !modsOverride) super.onRemoveModerated(channel, sourceNick, sourceLogin, sourceHostname);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onRemoveModerated(channel, sourceNick, sourceLogin, sourceHostname);
                channel = str[0];
                sourceNick = str[1];
                sourceLogin = str[2];
                sourceHostname = str[3];
            }
        }
        if (filterOverride && !modsOverride) super.onRemoveModerated(channel, sourceNick, sourceLogin, sourceHostname);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status && modHasChannel(i, channel)) objMods[i].onRemoveModerated(channel, sourceNick, sourceLogin, sourceHostname);
        }
    }

    @Override
    public final void onRemoveNoExternalMessages(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
        if (!filterOverride && !modsOverride) super.onRemoveNoExternalMessages(channel, sourceNick, sourceLogin, sourceHostname);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onRemoveNoExternalMessages(channel, sourceNick, sourceLogin, sourceHostname);
                channel = str[0];
                sourceNick = str[1];
                sourceLogin = str[2];
                sourceHostname = str[3];
            }
        }
        if (filterOverride && !modsOverride) super.onRemoveNoExternalMessages(channel, sourceNick, sourceLogin, sourceHostname);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status && modHasChannel(i, channel)) objMods[i].onRemoveNoExternalMessages(channel, sourceNick, sourceLogin, sourceHostname);
        }
    }

    @Override
    public final void onRemovePrivate(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
        if (!filterOverride && !modsOverride) super.onRemovePrivate(channel, sourceNick, sourceLogin, sourceHostname);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onRemovePrivate(channel, sourceNick, sourceLogin, sourceHostname);
                channel = str[0];
                sourceNick = str[1];
                sourceLogin = str[2];
                sourceHostname = str[3];
            }
        }
        if (filterOverride && !modsOverride) super.onRemovePrivate(channel, sourceNick, sourceLogin, sourceHostname);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status && modHasChannel(i, channel)) objMods[i].onRemovePrivate(channel, sourceNick, sourceLogin, sourceHostname);
        }
    }

    @Override
    public final void onRemoveSecret(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
        if (!filterOverride && !modsOverride) super.onRemoveSecret(channel, sourceNick, sourceLogin, sourceHostname);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onRemoveSecret(channel, sourceNick, sourceLogin, sourceHostname);
                channel = str[0];
                sourceNick = str[1];
                sourceLogin = str[2];
                sourceHostname = str[3];
            }
        }
        if (filterOverride && !modsOverride) super.onRemoveSecret(channel, sourceNick, sourceLogin, sourceHostname);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status && modHasChannel(i, channel)) objMods[i].onRemoveSecret(channel, sourceNick, sourceLogin, sourceHostname);
        }
    }

    @Override
    public final void onRemoveTopicProtection(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
        if (!filterOverride && !modsOverride) super.onRemoveTopicProtection(channel, sourceNick, sourceLogin, sourceHostname);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onRemoveTopicProtection(channel, sourceNick, sourceLogin, sourceHostname);
                channel = str[0];
                sourceNick = str[1];
                sourceLogin = str[2];
                sourceHostname = str[3];
            }
        }
        if (filterOverride && !modsOverride) super.onRemoveTopicProtection(channel, sourceNick, sourceLogin, sourceHostname);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status && modHasChannel(i, channel)) objMods[i].onRemoveTopicProtection(channel, sourceNick, sourceLogin, sourceHostname);
        }
    }

    @Override
    public final void onServerPing(String response) {
        if (!filterOverride && !modsOverride) super.onServerPing(response);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                response = objFilters[i].onServerPing(response);
            }
        }
        if (filterOverride && !modsOverride) super.onServerPing(response);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status) objMods[i].onServerPing(response);
        }
    }

    @Override
    public final void onServerResponse(int code, String response) {
        if (!filterOverride && !modsOverride) super.onServerResponse(code, response);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                ircData data = objFilters[i].onServerResponse(code, response);
                code = data.code;
                response = data.response;
            }
        }
        if (filterOverride && !modsOverride) super.onServerResponse(code, response);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status) objMods[i].onServerResponse(code, response);
        }
    }

    @Override
    public final void onSetChannelBan(String channel, String sourceNick, String sourceLogin, String sourceHostname, String hostmask) {
        if (!filterOverride && !modsOverride) super.onSetChannelBan(channel, sourceNick, sourceLogin, sourceHostname, hostmask);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onSetChannelBan(channel, sourceNick, sourceLogin, sourceHostname, hostmask);
                channel = str[0];
                sourceNick = str[1];
                sourceLogin = str[2];
                sourceHostname = str[3];
                hostmask = str[4];
            }
        }
        if (filterOverride && !modsOverride) super.onSetChannelBan(channel, sourceNick, sourceLogin, sourceHostname, hostmask);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status && modHasChannel(i, channel)) objMods[i].onSetChannelBan(channel, sourceNick, sourceLogin, sourceHostname, hostmask);
        }
    }

    @Override
    public final void onSetChannelKey(String channel, String sourceNick, String sourceLogin, String sourceHostname, String key) {
        if (!filterOverride && !modsOverride) super.onSetChannelKey(channel, sourceNick, sourceLogin, sourceHostname, key);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onSetChannelKey(channel, sourceNick, sourceLogin, sourceHostname, key);
                channel = str[0];
                sourceNick = str[1];
                sourceLogin = str[2];
                sourceHostname = str[3];
                key = str[4];
            }
        }
        if (filterOverride && !modsOverride) super.onSetChannelKey(channel, sourceNick, sourceLogin, sourceHostname, key);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status && modHasChannel(i, channel)) objMods[i].onSetChannelKey(channel, sourceNick, sourceLogin, sourceHostname, key);
        }
    }

    @Override
    public final void onSetChannelLimit(String channel, String sourceNick, String sourceLogin, String sourceHostname, int limit) {
        if (!filterOverride && !modsOverride) super.onSetChannelLimit(channel, sourceNick, sourceLogin, sourceHostname, limit);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                ircData data = objFilters[i].onSetChannelLimit(channel, sourceNick, sourceLogin, sourceHostname, limit);
                channel = data.channel;
                sourceNick = data.sourceNick;
                sourceLogin = data.sourceLogin;
                sourceHostname = data.sourceHostname;
                limit = data.limit;
            }
        }
        if (filterOverride && !modsOverride) super.onSetChannelLimit(channel, sourceNick, sourceLogin, sourceHostname, limit);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status && modHasChannel(i, channel)) objMods[i].onSetChannelLimit(channel, sourceNick, sourceLogin, sourceHostname, limit);
        }
    }

    @Override
    public final void onSetInviteOnly(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
        if (!filterOverride && !modsOverride) super.onSetInviteOnly(channel, sourceNick, sourceLogin, sourceHostname);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onSetInviteOnly(channel, sourceNick, sourceLogin, sourceHostname);
                channel = str[0];
                sourceNick = str[1];
                sourceLogin = str[2];
                sourceHostname = str[3];
            }
        }
        if (filterOverride && !modsOverride) super.onSetInviteOnly(channel, sourceNick, sourceLogin, sourceHostname);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status && modHasChannel(i, channel)) objMods[i].onSetInviteOnly(channel, sourceNick, sourceLogin, sourceHostname);
        }
    }

    @Override
    public final void onSetModerated(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
        if (!filterOverride && !modsOverride) super.onSetModerated(channel, sourceNick, sourceLogin, sourceHostname);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onSetModerated(channel, sourceNick, sourceLogin, sourceHostname);
                channel = str[0];
                sourceNick = str[1];
                sourceLogin = str[2];
                sourceHostname = str[3];
            }
        }
        if (filterOverride && !modsOverride) super.onSetModerated(channel, sourceNick, sourceLogin, sourceHostname);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status && modHasChannel(i, channel)) objMods[i].onSetModerated(channel, sourceNick, sourceLogin, sourceHostname);
        }
    }

    @Override
    public final void onSetNoExternalMessages(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
        if (!filterOverride && !modsOverride) super.onSetNoExternalMessages(channel, sourceNick, sourceLogin, sourceHostname);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onSetNoExternalMessages(channel, sourceNick, sourceLogin, sourceHostname);
                channel = str[0];
                sourceNick = str[1];
                sourceLogin = str[2];
                sourceHostname = str[3];
            }
        }
        if (filterOverride && !modsOverride) super.onSetNoExternalMessages(channel, sourceNick, sourceLogin, sourceHostname);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status && modHasChannel(i, channel)) objMods[i].onSetNoExternalMessages(channel, sourceNick, sourceLogin, sourceHostname);
        }
    }

    @Override
    public final void onSetPrivate(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
        if (!filterOverride && !modsOverride) super.onSetPrivate(channel, sourceNick, sourceLogin, sourceHostname);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onSetPrivate(channel, sourceNick, sourceLogin, sourceHostname);
                channel = str[0];
                sourceNick = str[1];
                sourceLogin = str[2];
                sourceHostname = str[3];
            }
        }
        if (filterOverride && !modsOverride) super.onSetPrivate(channel, sourceNick, sourceLogin, sourceHostname);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status && modHasChannel(i, channel)) objMods[i].onSetPrivate(channel, sourceNick, sourceLogin, sourceHostname);
        }
    }

    @Override
    public final void onSetSecret(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
        if (!filterOverride && !modsOverride) super.onSetSecret(channel, sourceNick, sourceLogin, sourceHostname);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onSetSecret(channel, sourceNick, sourceLogin, sourceHostname);
                channel = str[0];
                sourceNick = str[1];
                sourceLogin = str[2];
                sourceHostname = str[3];
            }
        }
        if (filterOverride && !modsOverride) super.onSetSecret(channel, sourceNick, sourceLogin, sourceHostname);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status && modHasChannel(i, channel)) objMods[i].onSetSecret(channel, sourceNick, sourceLogin, sourceHostname);
        }
    }

    @Override
    public final void onSetTopicProtection(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
        if (!filterOverride && !modsOverride) super.onSetTopicProtection(channel, sourceNick, sourceLogin, sourceHostname);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onSetTopicProtection(channel, sourceNick, sourceLogin, sourceHostname);
                channel = str[0];
                sourceLogin = str[1];
                sourceHostname = str[1];
            }
        }
        if (filterOverride && !modsOverride) super.onSetTopicProtection(channel, sourceNick, sourceLogin, sourceHostname);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status && modHasChannel(i, channel)) objMods[i].onSetTopicProtection(channel, sourceNick, sourceLogin, sourceHostname);
        }
    }

    @Override
    public final void onTime(String sourceNick, String sourceLogin, String sourceHostname, String target) {
        if (!filterOverride && !modsOverride) super.onTime(sourceNick, sourceLogin, sourceHostname, target);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onTime(sourceNick, sourceLogin, sourceHostname, target);
                sourceNick = str[0];
                sourceLogin = str[1];
                sourceHostname = str[2];
                target = str[3];
            }
        }
        if (filterOverride && !modsOverride) super.onTime(sourceNick, sourceLogin, sourceHostname, target);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status) objMods[i].onTime(sourceNick, sourceLogin, sourceHostname, target);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public final void onTopic(String channel, String topic) {
        if (!filterOverride && !modsOverride) super.onTopic(channel, topic);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onTopic(channel, topic);
                channel = str[0];
                topic = str[1];
            }
        }
        if (filterOverride && !modsOverride) super.onTopic(channel, topic);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status && modHasChannel(i, channel)) objMods[i].onTopic(channel, topic);
        }
    }

    @Override
    public final void onTopic(String channel, String topic, String setBy, long date, boolean changed) {
        if (!filterOverride && !modsOverride) super.onTopic(channel, topic, setBy, date, changed);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                ircData data = objFilters[i].onTopic(channel, topic, setBy, date, changed);
                channel = data.channel;
                topic = data.topic;
                setBy = data.setBy;
                date = data.date;
                changed = data.changed;
            }
        }
        if (filterOverride && !modsOverride) super.onTopic(channel, topic, setBy, date, changed);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status && modHasChannel(i, channel)) objMods[i].onTopic(channel, topic, setBy, date, changed);
        }
    }

    @Override
    public final void onUnknown(String line) {
        if (!filterOverride && !modsOverride) super.onUnknown(line);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                line = objFilters[i].onUnknown(line);
            }
        }
        if (filterOverride && !modsOverride) super.onUnknown(line);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status) objMods[i].onUnknown(line);
        }
    }

    @Override
    public final void onUserList(String channel, User[] users) {
        if (!filterOverride && !modsOverride) super.onUserList(channel, users);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                ircData data = objFilters[i].onUserList(channel, users);
                channel = data.channel;
                users = data.users;
            }
        }
        if (filterOverride && !modsOverride) super.onUserList(channel, users);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status && modHasChannel(i, channel)) objMods[i].onUserList(channel, users);
        }
    }

    @Override
    public final void onUserMode(String targetNick, String sourceNick, String sourceLogin, String sourceHostname, String mode) {
        if (!filterOverride && !modsOverride) super.onUserMode(targetNick, sourceNick, sourceLogin, sourceHostname, mode);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onUserMode(targetNick, sourceNick, sourceLogin, sourceHostname, mode);
                targetNick = str[0];
                sourceNick = str[1];
                sourceLogin = str[2];
                sourceHostname = str[3];
                mode = str[4];
            }
        }
        if (filterOverride && !modsOverride) super.onUserMode(targetNick, sourceNick, sourceLogin, sourceHostname, mode);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status) objMods[i].onUserMode(targetNick, sourceNick, sourceLogin, sourceHostname, mode);
        }
    }

    @Override
    public final void onVersion(String sourceNick, String sourceLogin, String sourceHostname, String target) {
        if (!filterOverride && !modsOverride) super.onVersion(sourceNick, sourceLogin, sourceHostname, target);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onVersion(sourceNick, sourceLogin, sourceHostname, target);
                sourceNick = str[0];
                sourceLogin = str[1];
                sourceHostname = str[2];
                target = str[3];
            }
        }
        if (filterOverride && !modsOverride) super.onVersion(sourceNick, sourceLogin, sourceHostname, target);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status) objMods[i].onVersion(sourceNick, sourceLogin, sourceHostname, target);
        }
    }

    @Override
    public void onVoice(String channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient) {
        if (!filterOverride && !modsOverride) super.onVoice(channel, sourceNick, sourceLogin, sourceHostname, recipient);
        for (int i = 0; i < objFilters.length; i++) {
            if (objFilters[i].status) {
                String str[] = objFilters[i].onVoice(channel, sourceNick, sourceLogin, sourceHostname, recipient);
                channel = str[0];
                sourceNick = str[1];
                sourceLogin = str[2];
                sourceHostname = str[3];
                recipient = str[4];
            }
        }
        if (filterOverride && !modsOverride) super.onVoice(channel, sourceNick, sourceLogin, sourceHostname, recipient);
        for (int i = 0; i < objMods.length; i++) {
            if (objMods[i].status && modHasChannel(i, channel)) objMods[i].onVoice(channel, sourceNick, sourceLogin, sourceHostname, recipient);
        }
    }
}
