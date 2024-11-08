package cz.jabbim.android.service;

import java.io.ByteArrayOutputStream;
import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.OfflineMessageManager;
import org.jivesoftware.smackx.packet.DelayInformation;
import org.jivesoftware.smackx.packet.VCard;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Bitmap.CompressFormat;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.Vibrator;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;
import cz.jabbim.android.Constants;
import cz.jabbim.android.ConversationList;
import cz.jabbim.android.R;
import cz.jabbim.android.data.JabberoidDbConnector;

public class JabbimConnectionService extends Service {

    private static final String TAG = "JabbimConnectionService";

    private SharedPreferences prefs;

    private ConnectionConfiguration cc = null;

    private XMPPConnection con = null;

    private Roster roster = null;

    private WakeLock wl;

    private Vibrator vibrator;

    private JabbimConnectionService acs = this;

    private static final String CERT_DIR = "/system/etc/security/";

    private static final String CERT_FILE = "cacerts.bks";

    private final ConnectionServiceCall.Stub binder = new ConnectionServiceCall.Stub() {

        public void connect(String state, String type, String mode) {
            connectToServer(state, type, mode);
        }

        public void disconnect() throws RemoteException {
            disconnectFromServer();
        }

        public boolean isLoggedIn() throws RemoteException {
            return isUserLoggedIn();
        }

        public void logOff() throws RemoteException {
        }

        public void login() throws RemoteException {
        }

        public void setStatus(String state, String type, String mode) throws RemoteException {
            setPresenceState(state, type, mode);
        }

        public void sendMessage(String user, String message) throws RemoteException {
            sendMessagePacket(user, message);
        }

        public void getAvatar(String user) throws RemoteException {
            loadAvatar(user);
        }

        public void getRoster() {
            acs.sendRoster();
        }

        public void addEntry(String user, String name, List<String> groups) {
            acs.addEntry(user, name, groups);
        }

        public List<String> getLastStatusMessages() throws RemoteException {
            return acs.getLastStatusMessages();
        }

        public void insertAndUseMessage(String message) throws RemoteException {
            acs.insertAndUseMessage(message);
        }
    };

    private ConnectionListener connectionListener = new ConnectionListener() {

        public void connectionClosed() {
            Intent intent = new Intent("cz.jabbim.android.androidim.CONNECTION_CLOSED");
            sendBroadcast(intent);
        }

        public void connectionClosedOnError(Exception e) {
            Intent intent = new Intent("cz.jabbim.android.androidim.CONNECTION_ERROR_CLOSED");
            sendBroadcast(intent);
        }

        public void reconnectingIn(int seconds) {
            Log.i(TAG, "Reconnect in " + Integer.toString(seconds));
        }

        public void reconnectionFailed(Exception e) {
            Log.i(TAG, "Reconnect failed!");
        }

        public void reconnectionSuccessful() {
            Log.i(TAG, "Reconnect OK!");
        }
    };

    private PacketListener msgListener = new PacketListener() {

        public void processPacket(Packet packet) {
            Log.i(TAG, "message received");
            Message msg = (Message) packet;
            HashMap<String, String> msgInfo = getMessageInfo(msg);
            queueIncomingMessage(msgInfo);
            Intent intent = new Intent("cz.jabbim.android.androidim.NEW_MESSAGE");
            intent.putExtra("test", msgInfo.get("body"));
            intent.putExtra("jid", msgInfo.get("user"));
            intent.putExtra("username", msgInfo.get("username"));
            sendOrderedBroadcast(intent, null);
        }
    };

    private PacketListener presenceListener = new PacketListener() {

        public void processPacket(Packet packet) {
            Presence presence = (Presence) packet;
            Log.i(TAG, "presence received");
            String type = presence.getType().name();
            Mode mode = presence.getMode();
            int typeValue = getType(type);
            int modeValue = getMode(mode);
            Intent intent = new Intent("cz.jabbim.android.androidim.PRESENCE_CHANGED");
            intent.putExtra("jid", StringUtils.parseBareAddress(presence.getFrom()));
            intent.putExtra("resourceName", StringUtils.parseResource(presence.getFrom()));
            intent.putExtra("resourcePriority", presence.getPriority());
            intent.putExtra("presenceType", typeValue);
            intent.putExtra("presenceMode", modeValue);
            intent.putExtra("presenceMessage", presence.getStatus());
            sendOrderedBroadcast(intent, null);
        }
    };

    public void notifyUser(String fromUser, String msg) {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification n = new Notification(R.drawable.stat_notify_chat, msg, System.currentTimeMillis());
        Intent i = new Intent(this, ConversationList.class);
        i.putExtra("newReceived", true);
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);
        n.setLatestEventInfo(this, getString(R.string.Notify2), getString(R.string.NotifyFrom) + fromUser, pi);
        if (prefs.getBoolean("prefNotifSoundKey", true)) {
            n.defaults |= Notification.DEFAULT_SOUND;
            Log.i(TAG, "message received - BEEEEEP");
        } else {
            n.defaults &= ~Notification.DEFAULT_SOUND;
        }
        if (prefs.getBoolean("prefNotifLEDKey", true)) {
            n.ledARGB = 0xffff69b4;
            n.ledOnMS = 300;
            n.ledOffMS = 1000;
            n.flags |= Notification.FLAG_SHOW_LIGHTS;
            Log.i(TAG, "message received - BLIIIIIIK");
        } else {
            n.flags &= ~Notification.FLAG_SHOW_LIGHTS;
        }
        if (prefs.getBoolean("prefNotifVibrationKey", true)) {
            vibrator.vibrate(new long[] { 10L, 100L }, -1);
            Log.i(TAG, "message received - VIBRRRRRVIBRRR");
        } else {
            vibrator.cancel();
        }
        nm.notify(Constants.NEW_MESSAGE_NOTIFICATION, n);
        Log.i(TAG, "message received - notify done");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        BroadcastReceiver csr = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(TAG, "message received - notify called");
                String username = intent.getStringExtra("username");
                ;
                notifyUser(username, getString(R.string.Notify1) + " " + username);
            }
        };
        IntentFilter f = new IntentFilter();
        f.setPriority(5);
        f.addAction("cz.jabbim.android.androidim.NEW_MESSAGE");
        try {
            registerReceiver(csr, f);
        } catch (Exception e) {
        }
        Log.i(TAG, "Jabbim Connection Service created");
        if (prefs.getBoolean("prefAutoStartKey", true)) {
            Log.d(TAG, "Autostart enabled! Trying to connect!");
            connectToServer();
        }
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Log.i(TAG, "Jabbim Connection Service started");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnectFromServer();
        Log.i(TAG, "Jabbim Connection Service destroyed");
    }

    public void connectToServer() {
        connectToServer("", "available", "available");
    }

    public void connectToServer(final String state, final String type, final String mode) {
        SmackConfiguration.setPacketReplyTimeout(15000);
        String username = prefs.getString("prefJabberIdKey", null);
        final String password = prefs.getString("prefPasswordKey", null);
        int port = Integer.parseInt(prefs.getString("prefServerPortKey", "5222"));
        String service = (prefs.getString("prefServerKey", "")).trim();
        String host;
        final String user;
        if (username == null) {
            return;
        } else {
            host = StringUtils.parseServer(username);
            user = username.substring(0, username.indexOf("@"));
        }
        if (username.length() == 0) {
            return;
        }
        if (host.length() == 0) {
            return;
        }
        if (service == "") {
            cc = new ConnectionConfiguration(host, port);
        } else {
            cc = new ConnectionConfiguration(service, port, host);
        }
        cc.setTruststorePath(CERT_DIR + CERT_FILE);
        if (!prefs.getBoolean("prefEnableTlsKey", true)) {
            cc.setSecurityMode(SecurityMode.disabled);
        }
        if (!prefs.getBoolean("prefEnableSaslKey", true)) {
            cc.setSASLAuthenticationEnabled(false);
        }
        cc.setReconnectionAllowed(true);
        con = new XMPPConnection(cc);
        Thread task = new Thread() {

            public void run() {
                try {
                    con.connect();
                } catch (Exception xe) {
                    Log.e(TAG, "Could not connect to server: " + xe.getLocalizedMessage());
                    Intent intent = new Intent("cz.jabbim.android.androidim.CONNECTION_FAILED");
                    sendBroadcast(intent);
                    return;
                }
                try {
                    con.addPacketListener(msgListener, new PacketTypeFilter(Message.class));
                    con.addPacketListener(presenceListener, new PacketTypeFilter(Presence.class));
                    con.addConnectionListener(connectionListener);
                } catch (IllegalStateException e) {
                    Log.e(TAG, e.getLocalizedMessage());
                    return;
                }
                try {
                    con.login(user, password, prefs.getString("prefResourceKey", "Android"));
                } catch (XMPPException xe) {
                    Log.e(TAG, "Could not login with given username(" + user + ") or password(" + password + "): " + xe.getLocalizedMessage());
                    return;
                }
                try {
                    roster = con.getRoster();
                    roster.addRosterListener(new RosterListener() {

                        public void entriesAdded(Collection<String> addresses) {
                        }

                        public void entriesDeleted(Collection<String> addresses) {
                        }

                        public void entriesUpdated(Collection<String> addresses) {
                        }

                        public void presenceChanged(Presence presence) {
                        }
                    });
                    sendRoster();
                    if (prefs.getBoolean("prefWakeLockKey", true)) {
                        wl.acquire();
                        Log.i(TAG, "Prevents from CPU suspend");
                    }
                    OfflineMessageManager omm = new OfflineMessageManager(con);
                    try {
                        omm.deleteMessages();
                    } catch (XMPPException e) {
                        Log.e(TAG, "Could not delete offline messages.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Intent intent = new Intent("cz.jabbim.android.androidim.PRESENCE_CHANGED");
                sendBroadcast(intent);
                Intent loggedIn = new Intent("cz.jabbim.android.androidim.LOGGED_IN");
                sendBroadcast(loggedIn);
            }
        };
        task.start();
    }

    public void disconnectFromServer() {
        new Thread() {

            public void run() {
                if (con != null) {
                    try {
                        con.disconnect();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (wl.isHeld()) {
                    wl.release();
                }
            }
        }.start();
    }

    public void updateRoster() {
        new Thread() {

            public void run() {
                if (con != null && con.getUser() != null) {
                    roster = con.getRoster();
                }
            }
        }.start();
    }

    private void sendRoster() {
        Collection<RosterGroup> rGroups = roster.getGroups();
        Collection<RosterEntry> entries = roster.getEntries();
        Intent rosterPresenceStart = new Intent("cz.jabbim.android.androidim.ROSTER_PRESENCE_START");
        rosterPresenceStart.putExtra("groupsCount", rGroups.size());
        rosterPresenceStart.putExtra("entriesCount", entries.size());
        sendBroadcast(rosterPresenceStart);
        for (RosterGroup grp : rGroups) {
            Intent rosterGroupPresence = new Intent("cz.jabbim.android.androidim.PRESENCE_ROSTER_GROUP");
            rosterGroupPresence.putExtra("groupName", grp.getName());
            sendBroadcast(rosterGroupPresence);
        }
        Presence presence;
        for (RosterEntry entry : entries) {
            presence = roster.getPresence(entry.getUser());
            Intent rosterPresence = new Intent("cz.jabbim.android.androidim.PRESENCE_ROSTER_ENTRY");
            rosterPresence.putExtra("jid", entry.getUser());
            rosterPresence.putExtra("resourceName", StringUtils.parseResource(presence.getFrom()));
            rosterPresence.putExtra("resourcePriority", presence.getPriority());
            rosterPresence.putExtra("name", entry.getName());
            rosterPresence.putExtra("presenceType", getType(presence.getType().name()));
            rosterPresence.putExtra("presenceMode", getMode(presence.getMode()));
            rosterPresence.putExtra("status", entry.getStatus() == null ? "unknown" : entry.getStatus().toString());
            rosterPresence.putExtra("msg", presence.getStatus());
            Collection<RosterGroup> entryGroups = entry.getGroups();
            ArrayList<String> groups = new ArrayList<String>();
            for (RosterGroup group : entryGroups) {
                groups.add(group.getName());
            }
            rosterPresence.putStringArrayListExtra("groups", groups);
            sendBroadcast(rosterPresence);
        }
        Intent rosterPresenceStop = new Intent("cz.jabbim.android.androidim.ROSTER_PRESENCE_STOP");
        sendBroadcast(rosterPresenceStop);
    }

    public boolean isUserLoggedIn() {
        if (con != null && con.getUser() != null) {
            return true;
        }
        return false;
    }

    protected void loadAvatar(final String user) {
        new Thread() {

            @Override
            public void run() {
                if (con != null && con.getUser() != null) {
                    try {
                        VCard vCard = new VCard();
                        vCard.load(con, user);
                        byte[] avatarBytes = vCard.getAvatar();
                        Bitmap avatar = BitmapFactory.decodeByteArray(avatarBytes, 0, avatarBytes.length);
                        int width = avatar.getWidth();
                        int height = avatar.getHeight();
                        int newWidth;
                        int newHeight;
                        float scaleX;
                        float scaleY;
                        if (width > height) {
                            newWidth = 32;
                            scaleX = ((float) newWidth / width);
                            newHeight = Math.round(height * scaleX);
                            scaleY = ((float) newHeight / height);
                        } else if (height > width) {
                            newHeight = 32;
                            scaleY = ((float) newHeight / height);
                            newWidth = Math.round(width * scaleY);
                            scaleX = ((float) newWidth / width);
                        } else {
                            newWidth = 32;
                            newHeight = 32;
                            scaleX = ((float) newWidth / width);
                            scaleY = ((float) newHeight / height);
                        }
                        Matrix matrix = new Matrix();
                        matrix.postScale(scaleX, scaleY);
                        Bitmap resizedAvatar = Bitmap.createBitmap(avatar, 0, 0, width, height, matrix, true);
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        resizedAvatar.compress(CompressFormat.PNG, 100, bos);
                    } catch (XMPPException e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            }
        }.start();
    }

    protected void sendMessagePacket(String user, String message) {
        final Message msg = new Message(user, Message.Type.chat);
        msg.setBody(message);
        new Thread() {

            @Override
            public void run() {
                if (con != null && con.getUser() != null) {
                    con.sendPacket(msg);
                    logOutgoingMessage(msg.getBody(), msg.getTo());
                }
            }
        }.start();
    }

    protected void addEntry(String user, String name, List<String> groups) {
        final String jid = user;
        final String alias = name;
        final String[] grps = (String[]) groups.toArray();
        new Thread() {

            @Override
            public void run() {
                if (con != null && con.getUser() != null) {
                    try {
                        roster.createEntry(jid, alias, grps);
                    } catch (XMPPException e) {
                        Log.e(TAG, "Can't add contact: ", e);
                    }
                }
            }
        }.start();
    }

    public HashMap<String, String> getMessageInfo(Message msg) {
        String userWithRes = msg.getFrom();
        int slash = userWithRes.lastIndexOf("/");
        String resource;
        String user;
        if (slash != -1) {
            resource = userWithRes.substring(slash + 1);
            user = userWithRes.substring(0, slash);
        } else {
            resource = "unknown";
            user = userWithRes;
        }
        String userName = user;
        RosterEntry re = roster.getEntry(user);
        if ((re.getName()) != null) {
            userName = re.getName();
        }
        String body = msg.getBody();
        java.util.Date date = null;
        if (date == null) {
            date = new java.util.Date();
        }
        long time = date.getTime();
        HashMap<String, String> list = new HashMap<String, String>();
        list.put("user", user);
        list.put("resource", resource);
        list.put("username", userName);
        list.put("body", body);
        list.put("time", String.valueOf(time));
        return list;
    }

    public static java.util.Date getDelayedStamp(final Packet packet) {
        DelayInformation delay = (DelayInformation) packet.getExtension("jabber:x:delay");
        if (delay != null) {
            return delay.getStamp();
        }
        return null;
    }

    private void queueIncomingMessage(final HashMap<String, String> msgInfo) {
        try {
            SQLiteDatabase db = new JabberoidDbConnector(acs).getWritableDatabase();
            long time = Long.parseLong(msgInfo.get("time"));
            ContentValues val = new ContentValues();
            if (prefs.getBoolean("prefLogMessagesKey", true)) {
                val.put(Constants.TABLE_LOG_FIELD_DATE, new Date(time).toString());
                val.put(Constants.TABLE_LOG_FIELD_TIME, new Time(time).toString());
                val.put(Constants.TABLE_LOG_FIELD_FROM, msgInfo.get("user"));
                val.put(Constants.TABLE_LOG_FIELD_RESOURCE, msgInfo.get("resource"));
                val.put(Constants.TABLE_LOG_FIELD_MSG, msgInfo.get("body").trim());
                db.insert(Constants.TABLE_LOG, null, val);
            }
            val.clear();
            val.put(Constants.TABLE_CONVERSATION_FIELD_DATE, time);
            val.put(Constants.TABLE_CONVERSATION_FIELD_CHAT, msgInfo.get("user"));
            val.put(Constants.TABLE_CONVERSATION_FIELD_FROM, msgInfo.get("user"));
            val.put(Constants.TABLE_CONVERSATION_FIELD_TO, "me");
            val.put(Constants.TABLE_CONVERSATION_FIELD_MSG, msgInfo.get("body").trim());
            val.put(Constants.TABLE_CONVERSATION_FIELD_NEW, 1);
            db.insert(Constants.TABLE_CONVERSATION, null, val);
            db.close();
        } catch (Exception e) {
        }
    }

    private void logOutgoingMessage(final String body, final String to) {
        SQLiteDatabase db = new JabberoidDbConnector(acs).getWritableDatabase();
        String user = con.getUser();
        int slash = user.lastIndexOf("/");
        String resource = user.substring(slash + 1);
        user = user.substring(0, slash);
        long time = System.currentTimeMillis();
        ContentValues val = new ContentValues();
        if (prefs.getBoolean("prefLogMessagesKey", true)) {
            val.put(Constants.TABLE_LOG_FIELD_DATE, new Date(time).toString());
            val.put(Constants.TABLE_LOG_FIELD_TIME, new Time(time).toString());
            val.put(Constants.TABLE_LOG_FIELD_FROM, user);
            val.put(Constants.TABLE_LOG_FIELD_RESOURCE, resource);
            val.put(Constants.TABLE_LOG_FIELD_MSG, body.trim());
            db.insert(Constants.TABLE_LOG, null, val);
        }
        val.clear();
        val.put(Constants.TABLE_CONVERSATION_FIELD_DATE, time);
        val.put(Constants.TABLE_CONVERSATION_FIELD_CHAT, to);
        val.put(Constants.TABLE_CONVERSATION_FIELD_FROM, "me");
        val.put(Constants.TABLE_CONVERSATION_FIELD_TO, to);
        val.put(Constants.TABLE_CONVERSATION_FIELD_MSG, body.trim());
        val.put(Constants.TABLE_CONVERSATION_FIELD_NEW, 0);
        db.insert(Constants.TABLE_CONVERSATION, null, val);
        db.close();
    }

    public List<String> getLastStatusMessages() {
        SQLiteDatabase db = new JabberoidDbConnector(this).getReadableDatabase();
        String table = Constants.TABLE_STATUSMSG;
        String[] columns = { Constants.TABLE_STATUSMSG_FIELD_MSG };
        String orderBy = Constants.TABLE_STATUSMSG_FIELD_LASTUSED + " DESC LIMIT 10";
        Cursor result = db.query(table, columns, null, null, null, null, orderBy);
        result.moveToFirst();
        List<String> messages = new ArrayList<String>();
        while (!result.isAfterLast()) {
            messages.add(result.getString(result.getColumnIndex(Constants.TABLE_STATUSMSG_FIELD_MSG)));
            result.moveToNext();
        }
        result.close();
        db.close();
        return messages;
    }

    public void setPresenceState(final String state, final String type, final String mode) {
        new Thread() {

            public void run() {
                if (con.getUser() != null) {
                    Presence presence = new Presence(Presence.Type.valueOf(type));
                    if (state != null) presence.setStatus(state);
                    presence.setMode(Presence.Mode.valueOf(mode));
                    presence.setPriority(Integer.parseInt(prefs.getString("prefResourcePriorityKey", "5")));
                    con.sendPacket(presence);
                }
            }
        }.start();
    }

    public void insertAndUseMessage(String message) {
        SQLiteDatabase db = new JabberoidDbConnector(this).getReadableDatabase();
        String table = Constants.TABLE_STATUSMSG;
        String[] columns = { Constants.TABLE_STATUSMSG_FIELD_MSG };
        String where = Constants.TABLE_STATUSMSG_FIELD_MSG + " = '" + message + "'";
        Cursor result = db.query(table, columns, where, null, null, null, null);
        ContentValues cv = new ContentValues();
        if (result.getCount() < 1) {
            cv.put(Constants.TABLE_STATUSMSG_FIELD_ACTIVE, false);
            db.update(table, cv, null, null);
            cv.clear();
            cv.put(Constants.TABLE_STATUSMSG_FIELD_MSG, message);
            cv.put(Constants.TABLE_STATUSMSG_FIELD_ACTIVE, true);
            cv.put(Constants.TABLE_STATUSMSG_FIELD_LASTUSED, new java.util.Date().getTime());
            db.insert(table, null, cv);
        } else if (result.getCount() == 1) {
            cv.put(Constants.TABLE_STATUSMSG_FIELD_LASTUSED, new java.util.Date().getTime());
            db.update(table, cv, where, null);
        }
        result.close();
        db.close();
        String mode = getCurrentMode();
        if (mode != null) {
            setPresenceState(message, "available", mode);
        }
    }

    public String getCurrentMode() {
        String mode = null;
        switch(prefs.getInt("currentSelection", Constants.STATUS_OFFLINE)) {
            case Constants.STATUS_ONLINE:
                mode = "available";
                break;
            case Constants.STATUS_AWAY:
                mode = "away";
                break;
            case Constants.STATUS_E_AWAY:
                mode = "xa";
                break;
            case Constants.STATUS_DND:
                mode = "dnd";
                break;
            case Constants.STATUS_FREE:
                mode = "chat";
                break;
        }
        return mode;
    }

    public int getType(String type) {
        if (type == "available") {
            return Constants.PRESENCETYPE_AVAILABLE;
        } else if (type == "unavailable") {
            return Constants.PRESENCETYPE_UNAVAILABLE;
        }
        return Constants.PRESENCETYPE_NULL;
    }

    public int getMode(Mode mode) {
        if (mode == null) {
            return Constants.PRESENCEMODE_NULL;
        } else if (mode.name() == "away") {
            return Constants.PRESENCEMODE_AWAY;
        } else if (mode.name() == "xa") {
            return Constants.PRESENCEMODE_XA;
        } else if (mode.name() == "chat") {
            return Constants.PRESENCEMODE_CHAT;
        } else if (mode.name() == "dnd") {
            return Constants.PRESENCEMODE_DND;
        }
        return Constants.PRESENCEMODE_NULL;
    }
}
