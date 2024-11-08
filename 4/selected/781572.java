package ao.droid;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.util.Log;
import android.view.*;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.widget.*;
import ao.chat.ChatBot;
import ao.protocol.packets.*;
import ao.protocol.packets.Packet.Direction;
import ao.protocol.packets.bi.*;
import ao.protocol.packets.toclient.*;
import ao.misc.*;
import ao.protocol.Bot;
import ao.protocol.Bot.State;
import ao.protocol.BotListener;
import ao.protocol.BotLogger;
import ao.chat.modules.*;
import ao.chat.modules.ModuleList.ModuleChannel;
import ao.droid.color.Color;
import ao.droid.dialog.DialogFactory;
import ao.droid.parsers.*;
import java.util.Vector;

public class DDAO extends Activity implements Runnable, BotListener, BotLogger, ModuleUser {

    private static final int TEXT = 0;

    private static final int ADD_CHANNEL = 1;

    private static final int REM_CHANNEL = 2;

    private static final int MUTE_CHANNEL = 3;

    private static final int UNMUTE_CHANNEL = 4;

    private static final int FRIEND_LOGON = 5;

    private static final int FRIEND_LOGOFF = 6;

    private static final int FRIEND_TEMP_LOGON = 7;

    private static final int FRIEND_TEMP_LOGOFF = 8;

    private static final int PRIVATE_JOIN = 9;

    private static final int PRIVATE_PART = 10;

    private static final int CLEAR_CHANNELS = 11;

    private static final int CLEAR_FRIENDS = 12;

    private static final int GROUP_INVITE = 13;

    private static final int CHAR_SELECT = 14;

    private ChatBot chatbot = new ChatBot(5000);

    private ModuleList modules = new ModuleList(chatbot, this, "$", true);

    private FriendList friends = new FriendList();

    private ChannelList channels = new ChannelList();

    private Vector<Message> messages = new Vector<Message>();

    private Vector<Link> links = new Vector<Link>();

    private DialogFactory dialogfactory = new DialogFactory(this);

    private TagParser tagparser = new TagParser();

    private FontParser fontparser = new FontParser();

    private LinkParser linkparser = new LinkParser(this);

    private CharParser charparser = new CharParser();

    private Thread thread;

    private ArrayAdapter<CharSequence> adapter;

    private Activity DDAO;

    private ScrollView sv;

    private LinearLayout svl;

    private EditText in;

    private Spinner spin;

    private boolean run = true;

    private boolean autoscroll = true;

    private DBType database = DBType.XYPHOS_XML;

    private String prefname = "DDAO";

    private String accname = "";

    private String accpass = "";

    private int server = -1;

    private boolean prefsave = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat);
        loadPrefs();
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        DDAO = this;
        chatbot.addLogger(this);
        chatbot.addListener(this);
        modules.add(new DDAOCoreModule());
        sv = (ScrollView) findViewById(R.id.scrollview);
        svl = (LinearLayout) findViewById(R.id.scrollviewlayout);
        in = (EditText) findViewById(R.id.input);
        in.setOnTouchListener(new OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                if (sv.getHeight() + sv.getScrollY() >= svl.getHeight()) {
                    autoscroll = true;
                }
                in.onTouchEvent(event);
                if (autoscroll) autoScroll(sv, svl);
                return true;
            }
        });
        spin = (Spinner) findViewById(R.id.spinner);
        adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin.setAdapter(adapter);
        sv.setOnTouchListener(new OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                sv.onTouchEvent(event);
                autoscroll = false;
                return true;
            }
        });
        in.setOnKeyListener(new OnKeyListener() {

            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    autoscroll = true;
                    autoScroll(sv, svl);
                    String input = in.getText().toString();
                    in.setText("");
                    if (input.startsWith("/")) {
                        String[] temp = input.substring(1).split(" ");
                        String[] args = new String[temp.length - 1];
                        for (int i = 1; i < temp.length; i++) {
                            args[i - 1] = temp[i];
                        }
                        parseCommand(temp[0], args);
                        String out = "";
                        for (String part : args) {
                            out = out + part + ", ";
                        }
                    } else {
                        try {
                            String channel = getChannel();
                            if (channels.get(channel).isMute()) {
                                println(chatbot, "This channel is currently muted");
                            } else if (channels.get(channel).getID().length < 5) {
                                chatbot.sendPrivateChannelMessage(channel, input);
                            } else {
                                chatbot.sendChannelMessage(channel, input);
                            }
                        } catch (Exception e) {
                            exception(chatbot, e);
                        }
                    }
                    return true;
                }
                return false;
            }
        });
        if (autoscroll) autoScroll(sv, svl);
        try {
            thread = new Thread(this);
            thread.start();
        } catch (Exception e) {
            Log.e("System.err", "Failed to create thread", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences settings = getSharedPreferences(prefname, 0);
        prefsave = settings.getBoolean("save", prefsave);
        accname = settings.getString("name", accname);
        accpass = settings.getString("pass", accpass);
        server = settings.getInt("server", server);
    }

    @Override
    protected void onPause() {
        super.onPause();
        savePrefs();
    }

    @Override
    protected void onStop() {
        super.onStop();
        savePrefs();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        if (sv != null) {
            if (sv.getHeight() + sv.getScrollY() >= svl.getHeight()) {
                autoscroll = true;
            }
            if (autoscroll) {
                sv.post(new Runnable() {

                    public void run() {
                        sv.smoothScrollTo(0, svl.getHeight());
                    }
                });
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            switch(item.getItemId()) {
                case R.id.connect:
                    if (chatbot.getState() == State.DISCONNECTED) {
                        dialogfactory.buildAccountSelectDialog(this, prefsave, accname, accpass, server, chatbot);
                    } else {
                        println(chatbot, "You are already connected");
                    }
                    return true;
                case R.id.disconnect:
                    chatbot.disconnect();
                    return true;
                case R.id.friends:
                    dialogfactory.buildFriendDialog(friends, in);
                    return true;
                case R.id.channels:
                    dialogfactory.buildChannelDialog(channels);
                    return true;
                case R.id.settings:
                    dialogfactory.buildSettingsDialog();
                    return true;
                case R.id.quit:
                    chatbot.disconnect();
                    finish();
                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
        } catch (Exception e) {
            exception(chatbot, e);
            return super.onOptionsItemSelected(item);
        }
    }

    public void run() {
        try {
            run = true;
            while (run) {
                synchronized (messages) {
                    if (!messages.isEmpty()) {
                        handler.sendMessage(messages.remove((messages.size() - 1)));
                    }
                }
                Thread.sleep(1);
            }
        } catch (Exception e) {
            exception(chatbot, e);
        }
    }

    public void updateConfig(boolean save, String name, String pass, int dimension) {
        prefsave = save;
        if (save) {
            accname = name;
            accpass = pass;
            server = dimension;
        } else {
            accname = "";
            accpass = "";
            server = -1;
        }
    }

    private void savePrefs() {
        SharedPreferences settings = getSharedPreferences(prefname, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("save", prefsave);
        if (prefsave) {
            editor.putString("name", accname);
            editor.putString("pass", accpass);
            editor.putInt("server", server);
        } else {
            editor.putString("name", "");
            editor.putString("pass", "");
            editor.putInt("server", -1);
        }
        switch(database) {
            case XYPHOS_XML:
                editor.putString("dbtype", "XYPHOS_XML");
                break;
            case XYPHOS_COM:
                editor.putString("dbtype", "XYPHOS_COM");
                break;
            case AUNO_COM:
                editor.putString("dbtype", "AUNO_COM");
                break;
        }
        editor.commit();
    }

    private void loadPrefs() {
        SharedPreferences settings = getSharedPreferences(prefname, 0);
        prefsave = settings.getBoolean("save", prefsave);
        accname = settings.getString("name", accname);
        accpass = settings.getString("pass", accpass);
        server = settings.getInt("server", server);
        String dbtype = settings.getString("dbtype", "XYPHOS_XML");
        if (dbtype.matches("XYPHOS_XML")) {
            database = DBType.XYPHOS_XML;
        } else if (dbtype.matches("XYPHOS_COM")) {
            database = DBType.XYPHOS_COM;
        } else if (dbtype.matches("AUNO_COM")) {
            database = DBType.AUNO_COM;
        }
    }

    private void parseCommand(String command, String[] args) {
        try {
            modules.execute(ModuleChannel.CON, ByteConvert.intToByte(-1), command, args);
        } catch (Exception e) {
            exception(chatbot, e);
        }
    }

    private String getChannel() {
        return (String) spin.getSelectedItem();
    }

    public DBType getDBType() {
        return database;
    }

    public void setDBType(DBType type) {
        database = type;
    }

    /**
	 * Android Activity functions
	 */
    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String text = "";
            Integer id = -1;
            byte[] groupID;
            Channel channel;
            switch(msg.what) {
                case TEXT:
                    text = (String) msg.obj;
                    if (sv != null && msg != null) {
                        if (sv.getHeight() + sv.getScrollY() >= svl.getHeight()) {
                            autoscroll = true;
                        }
                        try {
                            Tag tags = new Tag(text);
                            CharSequence cs = tagparser.parse(tags, links);
                            cs = fontparser.parse(cs);
                            cs = linkparser.parse(cs, links);
                            cs = charparser.parse(cs);
                            TextView tempview = new TextView(DDAO);
                            MovementMethod m = tempview.getMovementMethod();
                            if ((m == null) || !(m instanceof LinkMovementMethod)) {
                                tempview.setMovementMethod(LinkMovementMethod.getInstance());
                            }
                            tempview.setText(cs);
                            svl.addView(tempview);
                            if (autoscroll) autoScroll(sv, svl);
                            links.clear();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case ADD_CHANNEL:
                    channel = (Channel) msg.obj;
                    channels.add(channel);
                    groupID = channel.getID();
                    if (groupID.length < 5 || (groupID[0] != 0x0A && groupID[0] != 0x0C)) {
                        adapter.add(channel.getName());
                        if (groupID[0] == 0x03) {
                            spin.setSelection(spin.getCount() - 1);
                        }
                    }
                    break;
                case REM_CHANNEL:
                    text = (String) msg.obj;
                    channels.rem(text);
                    adapter.remove(text);
                    break;
                case MUTE_CHANNEL:
                    groupID = (byte[]) msg.obj;
                    channels.get(groupID).mute(true);
                    println(chatbot, "Channel " + channels.get(groupID).getName() + " Muted");
                    break;
                case UNMUTE_CHANNEL:
                    groupID = (byte[]) msg.obj;
                    channels.get(groupID).mute(false);
                    println(chatbot, "Channel " + channels.get(groupID).getName() + " Unmuted");
                    break;
                case FRIEND_LOGON:
                    text = (String) msg.obj;
                    friends.logon(text);
                    break;
                case FRIEND_LOGOFF:
                    text = (String) msg.obj;
                    friends.logoff(text);
                    break;
                case FRIEND_TEMP_LOGON:
                    text = (String) msg.obj;
                    if (friends.find(text) != -1) {
                        friends.remove(friends.find(text));
                    }
                    break;
                case FRIEND_TEMP_LOGOFF:
                    text = (String) msg.obj;
                    if (friends.find(text) != -1) {
                        friends.remove(friends.find(text));
                    }
                    break;
                case PRIVATE_JOIN:
                    if (!channels.contains(chatbot.getCharacter().getName())) {
                        String botname = chatbot.getCharacter().getName();
                        byte[] botid = ByteConvert.intToByte(chatbot.getCharacter().getID());
                        channels.add(new Channel(botid, botname, false));
                        adapter.add(botname);
                    }
                    break;
                case PRIVATE_PART:
                    break;
                case CLEAR_FRIENDS:
                    friends.clear();
                    break;
                case CLEAR_CHANNELS:
                    channels.clear();
                    adapter.clear();
                    break;
                case GROUP_INVITE:
                    id = (Integer) msg.obj;
                    dialogfactory.buildInviteDialog(id.intValue(), chatbot);
                    break;
                case CHAR_SELECT:
                    CharacterListPacket p = (CharacterListPacket) msg.obj;
                    dialogfactory.buildCharSelectDialog(p, chatbot);
                    break;
            }
        }
    };

    private void autoScroll(final ScrollView scrollview, final LinearLayout layout) {
        scrollview.post(new Runnable() {

            public void run() {
                scrollview.smoothScrollTo(0, layout.getHeight());
            }
        });
    }

    public DialogFactory getDialogFactory() {
        return dialogfactory;
    }

    /**
     * Messages being passed to the UI thread
     */
    private void addChannel(Channel c) {
        messages.add(Message.obtain(handler, ADD_CHANNEL, c));
    }

    private void remChannel(String name) {
        messages.add(Message.obtain(handler, REM_CHANNEL, name));
    }

    public void muteChannel(byte[] b) {
        messages.add(Message.obtain(handler, MUTE_CHANNEL, b));
    }

    public void unMuteChannel(byte[] b) {
        messages.add(Message.obtain(handler, UNMUTE_CHANNEL, b));
    }

    private void clearChannels() {
        messages.add(Message.obtain(handler, CLEAR_CHANNELS));
    }

    private void friendLogon(String name) {
        messages.add(Message.obtain(handler, FRIEND_LOGON, name));
    }

    private void friendLogoff(String name) {
        messages.add(Message.obtain(handler, FRIEND_LOGOFF, name));
    }

    private void friendTempLogon(String name) {
        messages.add(Message.obtain(handler, FRIEND_TEMP_LOGON, name));
    }

    private void friendTempLogoff(String name) {
        messages.add(Message.obtain(handler, FRIEND_TEMP_LOGOFF, name));
    }

    private void clearFriends() {
        messages.add(Message.obtain(handler, CLEAR_FRIENDS));
        handler.sendEmptyMessage(6);
    }

    private void pgroupJoin(String name) {
        messages.add(Message.obtain(handler, PRIVATE_JOIN, name));
    }

    private void pgroupPart(String name) {
        messages.add(Message.obtain(handler, PRIVATE_PART, name));
    }

    private void groupInvite(int id) {
        messages.add(Message.obtain(handler, GROUP_INVITE, id));
    }

    private void charSelect(CharacterListPacket p) {
        messages.add(Message.obtain(handler, CHAR_SELECT, p));
    }

    /** Called after a bot is connected. */
    public void connected(Bot bot) {
    }

    /** Called after a bot is authenticated. */
    public void authenticated(Bot bot) {
    }

    /** Called after a bot is logged in. */
    public void loggedIn(Bot bot) {
    }

    /** Called after a bot is started. */
    public void started(Bot bot) {
    }

    /** Called after a bot is disconnected. */
    public void disconnected(Bot bot) {
        clearFriends();
        clearChannels();
    }

    /** Called after a bot receives a packet. */
    public void packet(Bot bot, Packet packet) {
        try {
            if (packet instanceof CharacterListPacket) {
                CharacterListPacket p = (CharacterListPacket) packet;
                charSelect(p);
            } else if (packet instanceof LoginOkPacket) {
                bot.start();
            } else if (packet instanceof PrivateMessagePacket) {
                PrivateMessagePacket msgPacket = (PrivateMessagePacket) packet;
                println(bot, "<font color=#" + Color.getColor("Tell") + ">" + msgPacket.display(chatbot.getCharTable(), chatbot.getGroupTable()));
            } else if (packet instanceof ChannelMessagePacket) {
                ChannelMessagePacket msgPacket = (ChannelMessagePacket) packet;
                Channel channel = channels.get(msgPacket.getGroupID());
                if (channel != null && channel.isMute()) {
                } else {
                    println(bot, "<font color=#" + Color.getColor(msgPacket.getGroupID()) + ">" + msgPacket.display(chatbot.getCharTable(), chatbot.getGroupTable()));
                }
            } else if (packet instanceof PrivateChannelMessagePacket) {
                PrivateChannelMessagePacket msgPacket = (PrivateChannelMessagePacket) packet;
                Channel channel = channels.get(ByteConvert.intToByte(msgPacket.getGroupID()));
                if (channel != null && channel.isMute()) {
                } else {
                    println(bot, "<font color=#" + Color.getColor("PrivateGroup") + ">" + msgPacket.display(chatbot.getCharTable(), chatbot.getGroupTable()));
                }
            } else if (packet instanceof PrivateChannelInvitePacket) {
                PrivateChannelInvitePacket invite = (PrivateChannelInvitePacket) packet;
                if (invite.getDirection() == Direction.IN) {
                    String channel = chatbot.getCharTable().getName(invite.getGroupID());
                    println(bot, "You have been invite to " + channel + "'s private group channel");
                    groupInvite(invite.getGroupID());
                } else {
                    String character = chatbot.getCharTable().getName(invite.getGroupID());
                    println(bot, "You invited " + character + " to your private group channel");
                }
            } else if (packet instanceof PrivateChannelKickPacket) {
                PrivateChannelKickPacket kick = (PrivateChannelKickPacket) packet;
                if (kick.getDirection() == Direction.IN) {
                    String channel = chatbot.getCharTable().getName(kick.getGroupID());
                    println(bot, "You have been kicked from " + channel + "'s private group channel");
                } else {
                    String character = chatbot.getCharTable().getName(kick.getGroupID());
                    println(bot, "You kicked " + character + " from your private group channel");
                }
            } else if (packet instanceof ChannelUpdatePacket) {
                ChannelUpdatePacket groupPacket = (ChannelUpdatePacket) packet;
                addChannel(new Channel(groupPacket.getGroupID(), groupPacket.getGroupName(), false));
            } else if (packet instanceof FriendUpdatePacket) {
                FriendUpdatePacket friendPacket = (FriendUpdatePacket) packet;
                if (friendPacket.isFriend()) {
                    if (friendPacket.isOnline()) {
                        friendLogon(chatbot.getCharTable().getName(friendPacket.getCharID()));
                    } else {
                        friendLogoff(chatbot.getCharTable().getName(friendPacket.getCharID()));
                    }
                } else {
                    if (friendPacket.isOnline()) {
                        friendTempLogon(chatbot.getCharTable().getName(friendPacket.getCharID()));
                    } else {
                        friendTempLogoff(chatbot.getCharTable().getName(friendPacket.getCharID()));
                    }
                }
            } else if (packet instanceof PrivateChannelCharacterJoinPacket) {
                PrivateChannelCharacterJoinPacket joinPacket = (PrivateChannelCharacterJoinPacket) packet;
                if (joinPacket.getGroupID() == bot.getCharacter().getID()) {
                    pgroupJoin(chatbot.getCharTable().getName(joinPacket.getCharID()));
                } else if (joinPacket.getCharID() == bot.getCharacter().getID()) {
                    addChannel(new Channel(ByteConvert.intToByte(joinPacket.getGroupID()), chatbot.getCharTable().getName(joinPacket.getGroupID()), false));
                    String channel = chatbot.getCharTable().getName(joinPacket.getGroupID());
                    println(bot, "You have joined " + channel + "'s private group channel");
                }
            } else if (packet instanceof PrivateChannelCharacterLeavePacket) {
                PrivateChannelCharacterLeavePacket partPacket = (PrivateChannelCharacterLeavePacket) packet;
                if (partPacket.getGroupID() == bot.getCharacter().getID()) {
                    pgroupPart(chatbot.getCharTable().getName(partPacket.getCharID()));
                } else if (partPacket.getCharID() == bot.getCharacter().getID()) {
                    remChannel(chatbot.getCharTable().getName(partPacket.getGroupID()));
                }
            }
        } catch (Exception e) {
            exception(bot, e);
        }
    }

    /** Called after a bot receives a exception. */
    public void exception(Bot bot, Exception e) {
        e.printStackTrace();
        println(bot, e.getMessage());
    }

    /** Prints a line terminator. */
    public void println(Bot bot) {
        String msg = "\n";
        messages.add(Message.obtain(handler, TEXT, msg));
    }

    /** Prints a string followed by a line terminator. */
    public void println(Bot bot, String msg) {
        if (msg != null) {
            if (!msg.startsWith("<font")) {
                msg = "<font color=#FFFFFF>" + msg + "</font>";
            }
            msg = msg + "\n";
            messages.add(Message.obtain(handler, TEXT, msg));
        }
    }

    /** Prints a string. */
    public void print(Bot bot, String msg) {
        if (msg != null) {
            if (!msg.startsWith("<font")) {
                msg = "<font color=#FFFFFF>" + msg + "</font>";
            }
            messages.add(Message.obtain(handler, TEXT, msg));
        }
    }
}
