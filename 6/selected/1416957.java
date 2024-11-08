package de.tudresden.inf.rn.mobilis.mxa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.filter.OrFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.provider.PrivacyProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ChatState;
import org.jivesoftware.smackx.ChatStateManager;
import org.jivesoftware.smackx.GroupChatInvitation;
import org.jivesoftware.smackx.PrivateDataManager;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.bytestreams.ibb.provider.CloseIQProvider;
import org.jivesoftware.smackx.bytestreams.ibb.provider.DataPacketProvider;
import org.jivesoftware.smackx.bytestreams.ibb.provider.OpenIQProvider;
import org.jivesoftware.smackx.bytestreams.socks5.provider.BytestreamsProvider;
import org.jivesoftware.smackx.packet.ChatStateExtension;
import org.jivesoftware.smackx.packet.LastActivity;
import org.jivesoftware.smackx.packet.OfflineMessageInfo;
import org.jivesoftware.smackx.packet.OfflineMessageRequest;
import org.jivesoftware.smackx.pubsub.packet.PubSub;
import org.jivesoftware.smackx.packet.SharedGroupsInfo;
import org.jivesoftware.smackx.provider.DataFormProvider;
import org.jivesoftware.smackx.provider.DelayInformationProvider;
import org.jivesoftware.smackx.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverItemsProvider;
import org.jivesoftware.smackx.provider.MUCAdminProvider;
import org.jivesoftware.smackx.provider.MUCOwnerProvider;
import org.jivesoftware.smackx.provider.MUCUserProvider;
import org.jivesoftware.smackx.provider.MessageEventProvider;
import org.jivesoftware.smackx.provider.MultipleAddressesProvider;
import org.jivesoftware.smackx.provider.RosterExchangeProvider;
import org.jivesoftware.smackx.provider.StreamInitiationProvider;
import org.jivesoftware.smackx.provider.VCardProvider;
import org.jivesoftware.smackx.provider.XHTMLExtensionProvider;
import org.jivesoftware.smackx.search.UserSearch;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import de.tudresden.inf.rn.mobilis.mxa.ConstMXA.MessageItems;
import de.tudresden.inf.rn.mobilis.mxa.ConstMXA.RosterItems;
import de.tudresden.inf.rn.mobilis.mxa.callbacks.IChatStateCallback;
import de.tudresden.inf.rn.mobilis.mxa.callbacks.IConnectionCallback;
import de.tudresden.inf.rn.mobilis.mxa.callbacks.IXMPPIQCallback;
import de.tudresden.inf.rn.mobilis.mxa.callbacks.IXMPPMessageCallback;
import de.tudresden.inf.rn.mobilis.mxa.parcelable.XMPPIQ;
import de.tudresden.inf.rn.mobilis.mxa.parcelable.XMPPMessage;
import de.tudresden.inf.rn.mobilis.mxa.parcelable.XMPPPresence;
import de.tudresden.inf.rn.mobilis.mxa.services.collabedit.CollabEditingService;
import de.tudresden.inf.rn.mobilis.mxa.services.collabedit.ICollabEditingService;
import de.tudresden.inf.rn.mobilis.mxa.services.filetransfer.FileTransferService;
import de.tudresden.inf.rn.mobilis.mxa.services.filetransfer.IFileTransferService;
import de.tudresden.inf.rn.mobilis.mxa.services.multiuserchat.IMultiUserChatService;
import de.tudresden.inf.rn.mobilis.mxa.services.multiuserchat.MultiUserChatService;
import de.tudresden.inf.rn.mobilis.mxa.services.pubsub.IPubSubService;
import de.tudresden.inf.rn.mobilis.mxa.services.pubsub.PubSubService;
import de.tudresden.inf.rn.mobilis.mxa.services.servicediscovery.IServiceDiscoveryService;
import de.tudresden.inf.rn.mobilis.mxa.services.servicediscovery.ServiceDiscoveryService;
import de.tudresden.inf.rn.mobilis.mxa.util.Debugger;
import de.tudresden.inf.rn.mobilis.mxa.util.FilteredCallbackList;
import de.tudresden.inf.rn.mobilis.mxa.util.JIDFilteredCallbackList;
import de.tudresden.inf.rn.mobilis.mxa.xmpp.IQImpl;
import de.tudresden.inf.rn.mobilis.mxa.xmpp.IQImplFilter;
import de.tudresden.inf.rn.mobilis.mxa.xmpp.IQImplProvider;
import de.tudresden.inf.rn.mobilis.mxa.xmpp.MXAIdentExtension;
import de.tudresden.inf.rn.mobilis.mxa.xmpp.MXAIdentExtensionProvider;
import de.tudresden.inf.rn.mobilis.mxa.xmpp.MessageExtension;
import de.tudresden.inf.rn.mobilis.mxa.xmpp.MessageExtensionProvider;

/**
 * @author Istvan Koren, Christian Magenheimer
 */
public class XMPPRemoteService extends Service {

    private static final String TAG = "XMPPRemoteService";

    private static final int XMPPSERVICE_STATUS = 1;

    private SharedPreferences mPreferences;

    private XMPPConnection mConn;

    private WriterThread xmppWriteWorker;

    private ReaderThread xmppReadWorker;

    ExecutorService mWriteExecutor;

    FileTransferService mFileTransferService;

    MultiUserChatService mMultiUserChatService;

    PubSubService mPubSubService;

    ServiceDiscoveryService mServiceDiscoveryService;

    CollabEditingService mCollabEditingService;

    private XMPPRemoteService instance;

    private NetworkMonitor mNetworkMonitor;

    private LostIQQueue mIQQueue;

    private Timer mIQQueueTimer;

    private boolean mReconnect = false;

    private final JIDFilteredCallbackList<IChatStateCallback> mChatStateCallbacks = new JIDFilteredCallbackList<IChatStateCallback>();

    final FilteredCallbackList<IXMPPMessageCallback> mMsgCallbacks = new FilteredCallbackList<IXMPPMessageCallback>();

    final RemoteCallbackList<IConnectionCallback> mConnectionCallbacks = new RemoteCallbackList<IConnectionCallback>();

    final FilteredCallbackList<IXMPPIQCallback> mIQCallbacks = new FilteredCallbackList<IXMPPIQCallback>();

    private static long PACKET_TIMEOUT = 5000;

    static {
        System.setProperty("smack.debuggerClass", "de.tudresden.inf.rn.mobilis.mxa.util.Debugger");
        XMPPConnection.DEBUG_ENABLED = true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        XMPPConnection.addConnectionCreationListener(new ConnectionCreationListener() {

            @Override
            public void connectionCreated(Connection connection) {
                new ServiceDiscoveryManager(connection);
            }
        });
        xmppWriteWorker = new WriterThread();
        xmppWriteWorker.start();
        xmppReadWorker = new ReaderThread();
        xmppReadWorker.start();
        mPreferences = getSharedPreferences("de.tudresden.inf.rn.mobilis.mxa_preferences", Context.MODE_PRIVATE);
        mWriteExecutor = Executors.newCachedThreadPool();
        mNetworkMonitor = new NetworkMonitor(this);
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mNetworkMonitor, filter);
        mIQQueue = new LostIQQueue();
        mIQQueueTimer = new Timer();
        Log.v(TAG, "count: " + mIQQueue.getCount());
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(XMPPSERVICE_STATUS);
        instance = this;
    }

    /**
	 * This method is just a quick fix to get hold of the current XMPPConnection object. It may
	 * be removed as soon as the WMP-CollaborationService has been properly integrated into Mobilis
	 * and into this service.
	 */
    public XMPPRemoteService getInstance() {
        return instance;
    }

    public XMPPConnection getXMPPConnection() {
        return mConn;
    }

    public void setXMPPConnection(XMPPConnection mConn) {
        this.mConn = mConn;
    }

    public ExecutorService getWriteExecutor() {
        return mWriteExecutor;
    }

    /**
	 * The WriterThread is responsible for sending XMPP stanzas to the server.
	 * 
	 * @author koren
	 * 
	 */
    private class WriterThread extends Thread {

        public Handler mHandler;

        public void run() {
            setName("MXA Writer Thread");
            Looper.prepare();
            mHandler = new Handler() {

                public void handleMessage(Message msg) {
                    Message msg2 = Message.obtain(msg);
                    switch(msg.what) {
                        case ConstMXA.MSG_CONNECT:
                            mReconnect = true;
                            if (mConn != null && mConn.isConnected() && mConn.isAuthenticated()) {
                                msg2.arg1 = ConstMXA.MSG_STATUS_SUCCESS;
                                xmppResults.sendMessage(msg2);
                                break;
                            }
                            String host = mPreferences.getString("pref_host", null);
                            int port = Integer.parseInt(mPreferences.getString("pref_port", "5222"));
                            String serviceName = mPreferences.getString("pref_service", "");
                            boolean useEncryption = mPreferences.getBoolean("pref_xmpp_encryption", true);
                            boolean useCompression = mPreferences.getBoolean("pref_xmpp_compression", false);
                            ConnectionConfiguration config = new ConnectionConfiguration(host, port, serviceName);
                            if (!useEncryption) config.setSecurityMode(SecurityMode.disabled); else config.setSecurityMode(SecurityMode.enabled);
                            mConn = new XMPPConnection(config);
                            String username = mPreferences.getString("pref_xmpp_user", "");
                            String password = mPreferences.getString("pref_xmpp_password", "");
                            String resource = mPreferences.getString("pref_resource", "MXA");
                            if (mPreferences.getBoolean("pref_xmpp_debug_enabled", false)) {
                                Debugger.setEnabled(true);
                                Debugger.setDirectory(mPreferences.getString("pref_xmpp_debug_directory", null));
                            }
                            try {
                                mConn.connect();
                                mIQQueue.setMaxRetryTime(Integer.valueOf(mPreferences.getString("pref_xmpp_lost_timeout", "60")));
                                mIQQueue.setMaxRetryCount(Integer.valueOf(mPreferences.getString("pref_xmpp_retry_count", "10")));
                                mIQQueue.setRetryInterval(Integer.valueOf(mPreferences.getString("pref_xmpp_retry_timeout", "10")));
                                ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(mConn);
                                sdm.addFeature("http://jabber.org/protocol/chatstates");
                                PACKET_TIMEOUT = (Integer.valueOf(mPreferences.getString("pref_xmpp_interval_packet", "5"))) * 1000;
                                ChatStateManager.getInstance(mConn);
                                ProviderManager pm = ProviderManager.getInstance();
                                configureProviderManager(pm);
                                MXAIdentExtensionProvider.install(pm);
                                mConn.addPacketListener(xmppReadWorker, new OrFilter(new OrFilter(new PacketTypeFilter(org.jivesoftware.smack.packet.Message.class), new PacketTypeFilter(Presence.class)), new PacketTypeFilter(IQ.class)));
                                mConn.addConnectionListener(xmppReadWorker);
                                mConn.login(username, password, resource);
                                getContentResolver().delete(ConstMXA.RosterItems.CONTENT_URI, "1", new String[] {});
                                final Roster r = XMPPRemoteService.this.mConn.getRoster();
                                r.addRosterListener(xmppReadWorker);
                                Collection<RosterEntry> rosterEntries = r.getEntries();
                                List<String> entries = new ArrayList<String>(rosterEntries.size());
                                for (RosterEntry re : rosterEntries) entries.add(re.getUser());
                                xmppReadWorker.entriesAdded(entries);
                                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                                Notification status = new Notification(R.drawable.stat_notify_chat, getString(R.string.sb_txt_text), System.currentTimeMillis());
                                status.setLatestEventInfo(XMPPRemoteService.this, getString(R.string.sb_txt_title), getString(R.string.sb_txt_text), PendingIntent.getActivity(XMPPRemoteService.this, 0, new Intent(ConstMXA.INTENT_SERVICEMONITOR), 0));
                                status.flags |= Notification.FLAG_ONGOING_EVENT;
                                status.icon = R.drawable.stat_notify_chat;
                                nm.notify(XMPPSERVICE_STATUS, status);
                                mFileTransferService = new FileTransferService(XMPPRemoteService.this);
                                mMultiUserChatService = new MultiUserChatService(XMPPRemoteService.this);
                                mPubSubService = new PubSubService(XMPPRemoteService.this);
                                mServiceDiscoveryService = new ServiceDiscoveryService(XMPPRemoteService.this);
                                mCollabEditingService = new CollabEditingService(XMPPRemoteService.this);
                                msg2.arg1 = ConstMXA.MSG_STATUS_SUCCESS;
                                mIQQueueTimer = new Timer();
                                mIQQueueTimer.schedule(new IQQueueCheckBackgroundRunner(), 0);
                            } catch (XMPPException e) {
                                msg2.arg1 = ConstMXA.MSG_STATUS_ERROR;
                                Bundle b = msg2.getData();
                                String errorMessage = e.getMessage();
                                if (e.getCause() != null && e.getCause().getMessage() != null) errorMessage += e.getCause().getMessage();
                                b.putString(ConstMXA.EXTRA_ERROR_MESSAGE, errorMessage);
                                msg2.setData(b);
                            }
                            xmppResults.sendMessage(msg2);
                            break;
                        case ConstMXA.MSG_RECONNECT:
                            if (!mReconnect) break;
                            Log.v(TAG, "Reconnection wish");
                            try {
                                if (mConn != null) {
                                    Log.v(TAG, "Trying to reconnect");
                                    String host2 = mPreferences.getString("pref_host", null);
                                    int port2 = Integer.parseInt(mPreferences.getString("pref_port", "5222"));
                                    String serviceName2 = mPreferences.getString("pref_service", "");
                                    boolean useEncryption2 = mPreferences.getBoolean("pref_xmpp_encryption", true);
                                    boolean useCompressio2n = mPreferences.getBoolean("pref_xmpp_compression", false);
                                    ConnectionConfiguration config2 = new ConnectionConfiguration(host2, port2, serviceName2);
                                    if (!useEncryption2) config2.setSecurityMode(SecurityMode.disabled); else config2.setSecurityMode(SecurityMode.enabled);
                                    mConn = new XMPPConnection(config2);
                                    mConn.connect();
                                    String username2 = mPreferences.getString("pref_xmpp_user", "");
                                    String password2 = mPreferences.getString("pref_xmpp_password", "");
                                    String resource2 = mPreferences.getString("pref_resource", "MXA");
                                    mConn.login(username2, password2, resource2);
                                } else break;
                            } catch (Exception e) {
                                if (!(e instanceof IllegalStateException)) {
                                    Message reconnect = new Message();
                                    reconnect.what = ConstMXA.MSG_CONNECT;
                                    xmppWriteWorker.mHandler.sendMessage(reconnect);
                                    Log.e(TAG, "hard reconnect, reason: " + e.getMessage());
                                }
                            }
                            if (mConn != null && mConn.isAuthenticated()) {
                                Log.v(TAG, "Connection established to " + mConn.getServiceName());
                                Message msgResend = new Message();
                                msgResend.what = ConstMXA.MSG_IQ_RESEND;
                                xmppWriteWorker.mHandler.sendMessage(msgResend);
                            } else {
                                Log.v(TAG, "Connection still broken " + mConn.getServiceName());
                                if (mNetworkMonitor.isConnected()) {
                                    Message reconnect = new Message();
                                    reconnect.what = ConstMXA.MSG_CONNECT;
                                    xmppWriteWorker.mHandler.sendMessage(reconnect);
                                    Log.e(TAG, "hard reconnect because of failure");
                                }
                            }
                            break;
                        case ConstMXA.MSG_DISCONNECT:
                            if (mConn == null || !mConn.isConnected()) {
                                msg2.arg1 = ConstMXA.MSG_STATUS_SUCCESS;
                                xmppResults.sendMessage(msg2);
                                break;
                            }
                            mReconnect = false;
                            mIQQueueTimer.cancel();
                            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                            nm.cancel(XMPPSERVICE_STATUS);
                            mConn.disconnect();
                            msg2.arg1 = ConstMXA.MSG_STATUS_SUCCESS;
                            xmppResults.sendMessage(msg2);
                            break;
                        case ConstMXA.MSG_SEND_IQ:
                            Bundle data = msg.getData();
                            XMPPIQ payloadIQ = data.getParcelable("PAYLOAD");
                            IQImpl iq = new IQImpl(payloadIQ.payload);
                            iq.setTo(payloadIQ.to);
                            switch(payloadIQ.type) {
                                case XMPPIQ.TYPE_GET:
                                    iq.setType(Type.GET);
                                    break;
                                case XMPPIQ.TYPE_SET:
                                    iq.setType(Type.SET);
                                    break;
                                case XMPPIQ.TYPE_RESULT:
                                    iq.setType(Type.RESULT);
                                    break;
                                case XMPPIQ.TYPE_ERROR:
                                    iq.setType(Type.ERROR);
                                    break;
                                default:
                                    iq.setType(Type.GET);
                            }
                            iq.setPacketID(payloadIQ.packetID);
                            if ((payloadIQ.namespace != null) || (payloadIQ.token != null)) {
                                MXAIdentExtension mie = new MXAIdentExtension(payloadIQ.namespace, payloadIQ.token);
                                iq.addExtension(mie);
                            }
                            mConn.sendPacket(iq);
                            break;
                        case ConstMXA.MSG_IQ_RESEND:
                            if (mIQQueue.getCount() == 0 || !mConn.isAuthenticated()) break;
                            mIQQueueTimer.cancel();
                            mIQQueueTimer = new Timer();
                            mIQQueueTimer.schedule(new IQQueueCheckBackgroundRunner(), mIQQueue.getRetryInterval() * 1000, mIQQueue.getRetryInterval() * 1000);
                            resendIQ();
                            break;
                        case ConstMXA.MSG_SEND_PRESENCE:
                            Bundle dataPresence = msg.getData();
                            XMPPPresence payloadPresence = dataPresence.getParcelable("PAYLOAD");
                            Presence presence = new Presence(Presence.Type.available);
                            presence.setStatus(payloadPresence.status);
                            presence.setPriority(payloadPresence.priority);
                            switch(payloadPresence.mode) {
                                case XMPPPresence.MODE_AVAILABLE:
                                    presence.setMode(Mode.available);
                                    break;
                                case XMPPPresence.MODE_AWAY:
                                    presence.setMode(Mode.away);
                                    break;
                                case XMPPPresence.MODE_CHAT:
                                    presence.setMode(Mode.chat);
                                    break;
                                case XMPPPresence.MODE_DND:
                                    presence.setMode(Mode.dnd);
                                    break;
                                case XMPPPresence.MODE_XA:
                                    presence.setMode(Mode.xa);
                                    break;
                                default:
                                    presence.setMode(Mode.available);
                            }
                            try {
                                mConn.sendPacket(presence);
                                msg2.arg1 = ConstMXA.MSG_STATUS_DELIVERED;
                            } catch (IllegalStateException e) {
                                msg2.arg1 = ConstMXA.MSG_STATUS_ERROR;
                            }
                            xmppResults.sendMessage(msg2);
                            Intent i = new Intent(ConstMXA.BROADCAST_PRESENCE);
                            i.putExtra("STATUS_TEXT", payloadPresence.status);
                            sendBroadcast(i);
                            break;
                    }
                }
            };
            Looper.loop();
        }
    }

    /**
	 * An IQ runner thread sends a GET or SET IQ Message to the XMPP server,
	 * constructs a PacketListener for the result with the specific packet ID
	 * and notifies a handler. This happens for XMPP standard compliance
	 * reasons, as GET or SET IQ Messages require a RESULT or ERROR IQ stanza
	 * from the XMPP partner. Developers not wanting the result/error to be
	 * handled by a PacketCollector should set the result messenger to null.
	 * 
	 * @author Istvan Koren
	 * 
	 */
    private class IQRunner implements Runnable {

        private Message msg;

        /**
		 * Constructs a new IQ runner.
		 * 
		 * @param result
		 *            the handler to be notified upon IQ result
		 * @param iq
		 *            the iq to be sent, must be of type GET or SET, as RESULT
		 *            and ERROR don't expect results.
		 */
        public IQRunner(Message msg) {
            this.msg = msg;
        }

        /**
		 * 
		 */
        @Override
        public void run() {
            Bundle data = msg.getData();
            XMPPIQ iq = data.getParcelable("PAYLOAD");
            IQImpl iqPacket = new IQImpl(iq.payload);
            iqPacket.fromXMPPIQ(iq);
            PacketCollector coll;
            iqPacket.setFrom(mConn.getUser());
            try {
                coll = mConn.createPacketCollector(new PacketIDFilter(iqPacket.getPacketID()));
                mConn.sendPacket(iqPacket);
            } catch (Exception error) {
                LostIQQueueEntry e = new LostIQQueueEntry();
                e.mMessage = Message.obtain(msg);
                e.mTime = System.currentTimeMillis();
                e.mXMPPIQ = iq;
                e.mSending = false;
                if (msg.what == ConstMXA.MSG_IQ_RESEND) {
                    e.mID = data.getLong(ConstMXA.IQ_RESEND_ID);
                }
                mIQQueue.insert(e);
                Message resultMsg = Message.obtain(msg);
                resultMsg.arg1 = ConstMXA.MSG_STATUS_ERROR;
                xmppResults.sendMessage(resultMsg);
                Log.v(TAG, "iq failed:" + e.mID + " " + e.mXMPPIQ.toString());
                return;
            }
            Message msgAck = Message.obtain(msg);
            msgAck.arg1 = ConstMXA.MSG_STATUS_DELIVERED;
            xmppResults.sendMessage(msgAck);
            msgAck.getData().putParcelable("MSN_RESULT", msg.getData().getParcelable("MSN_RESULT"));
            if (msgAck.getData().getParcelable("MSN_RESULT") != null) {
                Packet resultPacket = coll.nextResult(PACKET_TIMEOUT);
                coll.cancel();
                Message resultMsg = Message.obtain(msg);
                if (resultPacket == null) {
                    resultMsg.arg1 = ConstMXA.MSG_STATUS_ERROR;
                    LostIQQueueEntry e = new LostIQQueueEntry();
                    e.mMessage = Message.obtain(msg);
                    e.mTime = System.currentTimeMillis();
                    e.mXMPPIQ = iq;
                    e.mSending = false;
                    if (msg.what == ConstMXA.MSG_IQ_RESEND) {
                        e.mID = data.getLong(ConstMXA.IQ_RESEND_ID);
                    }
                    mIQQueue.insert(e);
                    Log.v(TAG, "timeout sending iq:" + e.mID + " " + e.mXMPPIQ.toString());
                    return;
                } else {
                    XMPPError err = resultPacket.getError();
                    if (err != null) {
                        resultMsg.arg1 = ConstMXA.MSG_STATUS_ERROR;
                        LostIQQueueEntry e = new LostIQQueueEntry();
                        e.mMessage = Message.obtain(msg);
                        e.mTime = System.currentTimeMillis();
                        e.mXMPPIQ = iq;
                        e.mSending = false;
                        if (msg.what == ConstMXA.MSG_IQ_RESEND) {
                            e.mID = data.getLong(ConstMXA.IQ_RESEND_ID);
                        }
                        mIQQueue.insert(e);
                        Log.v(TAG, "error sending iq:" + e.mID + " " + e.mXMPPIQ.toString());
                        return;
                    } else {
                        Log.i(TAG, "Success IQ: " + resultPacket.toXML());
                        if (msg.what == ConstMXA.MSG_IQ_RESEND) {
                            long id = msg.getData().getLong(ConstMXA.IQ_RESEND_ID);
                            boolean deleted = mIQQueue.delete(id);
                        }
                        resultMsg.arg1 = ConstMXA.MSG_STATUS_SUCCESS;
                        if (resultPacket instanceof IQImpl) {
                            IQImpl resultIQ = (IQImpl) resultPacket;
                            data.putParcelable("PAYLOAD", new XMPPIQ(resultIQ.getFrom(), resultIQ.getTo(), XMPPIQ.TYPE_RESULT, resultIQ.getChildNamespace(), resultIQ.getChildElementName(), resultIQ.getChildElementXML()));
                        }
                    }
                }
                xmppResults.sendMessage(resultMsg);
            } else Log.v(TAG, "no handler for " + iq.packetID);
        }
    }

    private class MessageRunner implements Runnable {

        private Message msg;

        public MessageRunner(Message msg) {
            this.msg = msg;
        }

        @Override
        public void run() {
            Message msgAck = Message.obtain(msg);
            Bundle dataMsg = msg.getData();
            XMPPMessage payloadMsg = dataMsg.getParcelable("PAYLOAD");
            org.jivesoftware.smack.packet.Message xmppMsg = new org.jivesoftware.smack.packet.Message();
            xmppMsg.setTo(payloadMsg.to);
            if (payloadMsg.type == XMPPMessage.TYPE_CHAT) {
                xmppMsg.setBody(payloadMsg.body);
                xmppMsg.setType(org.jivesoftware.smack.packet.Message.Type.chat);
                ContentValues values = new ContentValues();
                Long now = Long.valueOf(System.currentTimeMillis());
                values.put(MessageItems.SENDER, mConn.getUser());
                values.put(MessageItems.RECIPIENT, xmppMsg.getTo());
                if (xmppMsg.getSubject() != null) values.put(MessageItems.SUBJECT, xmppMsg.getSubject());
                if (xmppMsg.getBody() != null) values.put(MessageItems.BODY, xmppMsg.getBody());
                values.put(MessageItems.DATE_SENT, now);
                values.put(MessageItems.READ, 0);
                values.put(MessageItems.TYPE, "chat");
                values.put(MessageItems.STATUS, "sent");
                Log.i(TAG, "saving chat message");
                getContentResolver().insert(MessageItems.CONTENT_URI, values);
                if (mConn.isAuthenticated()) mConn.sendPacket(xmppMsg);
                msgAck.arg1 = ConstMXA.MSG_STATUS_DELIVERED;
            } else if (payloadMsg.type == XMPPMessage.TYPE_NORMAL) {
                xmppMsg.setType(org.jivesoftware.smack.packet.Message.Type.normal);
                String xml = dataMsg.getString("PAYLOAD_XML");
                MessageExtension me = new MessageExtension(payloadMsg.token, payloadMsg.namespace, xml);
                xmppMsg.addExtension(me);
                if (mConn.isAuthenticated()) mConn.sendPacket(xmppMsg); else msgAck.arg1 = ConstMXA.MSG_STATUS_ERROR;
                msgAck.arg1 = ConstMXA.MSG_STATUS_DELIVERED;
            } else if (payloadMsg.type == XMPPMessage.TYPE_GROUPCHAT) {
                Log.e(TAG, "we cannot handle groupchat at this time");
                msgAck.arg1 = ConstMXA.MSG_STATUS_ERROR;
            } else {
                msgAck.arg1 = ConstMXA.MSG_STATUS_ERROR;
            }
            xmppResults.sendMessage(msgAck);
        }
    }

    private Handler xmppResults = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            Message msg2 = Message.obtain(msg);
            try {
                Bundle data = msg.getData();
                if (msg.what == ConstMXA.MSG_CONNECT && msg.arg1 == ConstMXA.MSG_STATUS_SUCCESS) {
                    Messenger ack = data.getParcelable("MSN_ACK");
                    if (ack != null) ack.send(msg2);
                } else if (msg.what == ConstMXA.MSG_CONNECT && msg.arg1 == ConstMXA.MSG_STATUS_ERROR) {
                    Messenger ack = data.getParcelable("MSN_ACK");
                    if (ack != null) ack.send(msg2);
                } else if (msg.what == ConstMXA.MSG_DISCONNECT && msg.arg1 == ConstMXA.MSG_STATUS_SUCCESS) {
                    Messenger ack = data.getParcelable("MSN_ACK");
                    if (ack != null) ack.send(msg2);
                } else if (msg.arg1 == ConstMXA.MSG_STATUS_DELIVERED) {
                    Messenger ack = data.getParcelable("MSN_ACK");
                    if (ack != null) ack.send(msg2);
                } else if (msg.arg1 == ConstMXA.MSG_STATUS_SUCCESS) {
                    Messenger result = data.getParcelable("MSN_RESULT");
                    if (result != null) result.send(msg2);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    };

    public Handler getXMPPResultsHandler() {
        return xmppResults;
    }

    private class ReaderThread extends Thread implements PacketListener, ConnectionListener, RosterListener {

        public void run() {
            setName("MXA Reader Thread");
            Looper.prepare();
            Looper.loop();
        }

        @Override
        public void processPacket(Packet packet) {
            Log.i(TAG, "reading packet");
            String packetXML = packet.toXML();
            if (packet instanceof org.jivesoftware.smack.packet.Message) {
                org.jivesoftware.smack.packet.Message m = (org.jivesoftware.smack.packet.Message) packet;
                if (m.getType().equals(org.jivesoftware.smack.packet.Message.Type.normal)) {
                    Log.i(TAG, "--> message type=normal");
                    XMPPMessage xMsg = new XMPPMessage(m.getFrom(), m.getTo(), m.getBody(), m.getType().ordinal());
                    PacketExtension pubsubExtension = m.getExtension("event", "http://jabber.org/protocol/pubsub#event");
                    if (pubsubExtension != null) {
                    }
                    Collection<PacketExtension> pec = m.getExtensions();
                    for (int i = mMsgCallbacks.beginBroadcast() - 1; i >= 0; i--) try {
                        IXMPPMessageCallback callback = mMsgCallbacks.getBroadcastItem(i);
                        for (PacketExtension e : pec) {
                            for (PacketFilter filter : mMsgCallbacks.getFilters(callback)) {
                                if (filter.accept(new IQImpl(e.getElementName(), e.getNamespace(), null))) {
                                    XMPPIQ iq = new XMPPIQ(m.getFrom(), m.getTo(), XMPPIQ.TYPE_RESULT, e.getElementName(), e.getNamespace(), m.getExtension(e.getElementName(), e.getNamespace()).toXML());
                                    if (e instanceof MessageExtension) {
                                        MessageExtension me = (MessageExtension) e;
                                        Log.v(TAG, "message Extension: " + me.getElementName() + " " + me.getNamespace() + " " + me.getPayload());
                                        iq.payload = me.getPayload();
                                    }
                                    callback.processIQ(iq);
                                    Log.v(TAG, "message data received, notifies the application for  " + iq.toString());
                                }
                            }
                        }
                    } catch (RemoteException error) {
                        Log.e(TAG, "RemoteException!");
                        error.printStackTrace();
                    }
                    mMsgCallbacks.finishBroadcast();
                } else if (m.getType().equals(org.jivesoftware.smack.packet.Message.Type.chat)) {
                    ContentValues values = new ContentValues();
                    Long now = Long.valueOf(System.currentTimeMillis());
                    values.put(MessageItems.SENDER, m.getFrom());
                    values.put(MessageItems.RECIPIENT, m.getTo());
                    if (m.getSubject() != null) values.put(MessageItems.SUBJECT, m.getSubject());
                    if (m.getBody() != null) values.put(MessageItems.BODY, m.getBody());
                    values.put(MessageItems.DATE_SENT, now);
                    values.put(MessageItems.READ, 0);
                    values.put(MessageItems.TYPE, "chat");
                    values.put(MessageItems.STATUS, "received");
                    Log.i(TAG, "Chat Message, XML: " + m.toXML());
                    if (m.getBody() != null) {
                        Uri uri = getContentResolver().insert(MessageItems.CONTENT_URI, values);
                    }
                    PacketExtension pe = m.getExtension(new ChatStateExtension(ChatState.active).getNamespace());
                    if (pe.getElementName() != null) {
                        Log.v(TAG, "notify the listeners about the chatstate+ " + pe.getElementName());
                        for (int i = mChatStateCallbacks.beginBroadcast() - 1; i >= 0; i--) try {
                            IChatStateCallback callback = mChatStateCallbacks.getBroadcastItem(i);
                            String jid = m.getFrom();
                            Log.v(TAG, "checking jid: " + jid);
                            if (mChatStateCallbacks.appliesToJid(callback, jid)) {
                                callback.chatEventReceived(pe.getElementName());
                                Log.v(TAG, "chatStateCallback notified for " + jid);
                            }
                        } catch (RemoteException e) {
                            Log.e(TAG, "RemoteException!");
                            e.printStackTrace();
                        }
                        mChatStateCallbacks.finishBroadcast();
                    }
                } else if (m.getType().equals(org.jivesoftware.smack.packet.Message.Type.groupchat)) {
                    Log.i(TAG, "message type=groupchat");
                    ContentValues values = new ContentValues();
                    Long now = Long.valueOf(System.currentTimeMillis());
                    values.put(MessageItems.SENDER, m.getFrom());
                    values.put(MessageItems.RECIPIENT, m.getTo());
                    if (m.getSubject() != null) values.put(MessageItems.SUBJECT, m.getSubject());
                    if (m.getBody() != null) values.put(MessageItems.BODY, m.getBody());
                    values.put(MessageItems.DATE_SENT, now);
                    values.put(MessageItems.READ, 0);
                    values.put(MessageItems.TYPE, "groupchat");
                    values.put(MessageItems.STATUS, "received");
                    Uri uri = getContentResolver().insert(MessageItems.CONTENT_URI, values);
                    Log.i(TAG, "saved groupchat message to " + uri.toString());
                } else if (m.getType().equals(org.jivesoftware.smack.packet.Message.Type.error)) {
                    Log.i(TAG, "message type=error");
                } else {
                    Log.i(TAG, "message type=? -->" + m.getType().toString());
                    sendXMPPErrorMessage(m.getFrom(), XMPPError.Condition.feature_not_implemented, "No service available for this kind of request.");
                }
            } else if (packet instanceof IQImpl) {
                Log.i(TAG, "packet instance of IQImpl");
                final XMPPIQ parcelable = ((IQImpl) packet).toXMPPIQ();
                for (int i = mIQCallbacks.beginBroadcast() - 1; i >= 0; i--) try {
                    IXMPPIQCallback callback = mIQCallbacks.getBroadcastItem(i);
                    for (PacketFilter filter : mIQCallbacks.getFilters(callback)) if (filter.accept(packet)) callback.processIQ(parcelable);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException!");
                    e.printStackTrace();
                }
                mIQCallbacks.finishBroadcast();
            } else if (packet instanceof PubSub) {
                Log.i(TAG, "packet instance of PubSub");
                PubSub pubsub = (PubSub) packet;
                IQ.Type typeFrom = pubsub.getType();
                int typeTo = XMPPIQ.TYPE_SET;
                if (typeFrom == IQ.Type.SET) typeTo = XMPPIQ.TYPE_SET; else if (typeFrom == IQ.Type.GET) typeTo = XMPPIQ.TYPE_GET; else if (typeFrom == IQ.Type.ERROR) typeTo = XMPPIQ.TYPE_ERROR; else if (typeFrom == IQ.Type.RESULT) typeTo = XMPPIQ.TYPE_RESULT;
                XMPPIQ parcelable = new XMPPIQ(pubsub.getFrom(), pubsub.getTo(), typeTo, pubsub.getElementName(), pubsub.getNamespace(), pubsub.getChildElementXML());
                parcelable.packetID = pubsub.getPacketID();
                for (int i = mIQCallbacks.beginBroadcast() - 1; i >= 0; i--) try {
                    IXMPPIQCallback callback = mIQCallbacks.getBroadcastItem(i);
                    for (PacketFilter filter : mIQCallbacks.getFilters(callback)) {
                        filter.toString();
                        if (filter.accept(packet)) callback.processIQ(parcelable);
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException!");
                    e.printStackTrace();
                }
                mIQCallbacks.finishBroadcast();
            } else if (packet instanceof Presence) {
                Log.i(TAG, "received presence in reader Thread: " + ((Presence) packet).getFrom());
            } else {
                Log.e(TAG, "Packet unknown. XML: " + packet.toXML());
                if (packet.getError() != null) {
                    Log.e(TAG, "ERROR Message: " + packet.getError());
                }
            }
        }

        @Override
        public void connectionClosed() {
            notifyConnectionListeners(false);
            Log.v(TAG, "CL:connection closed");
        }

        @Override
        public void connectionClosedOnError(Exception e) {
            notifyConnectionListeners(false);
            Log.v(TAG, "CL:connection closed on error " + e.getMessage());
            if (mNetworkMonitor.isConnected()) reconnect();
        }

        @Override
        public void reconnectingIn(int seconds) {
            notifyConnectionListeners(false);
            Log.v(TAG, "CL:reconnecting in " + seconds + " s");
        }

        @Override
        public void reconnectionFailed(Exception e) {
            notifyConnectionListeners(false);
            Log.v(TAG, "CL:reconnection failed due to: " + e.getMessage());
        }

        @Override
        public void reconnectionSuccessful() {
            notifyConnectionListeners(true);
            Log.v(TAG, "CL:reconnection succesful");
        }

        @Override
        public void entriesAdded(Collection<String> entries) {
            final ContentResolver cr = XMPPRemoteService.this.getContentResolver();
            ContentValues[] cvs = getFromStrings(entries);
            cr.bulkInsert(ConstMXA.RosterItems.CONTENT_URI, cvs);
        }

        @Override
        public void entriesDeleted(Collection<String> entries) {
            final ContentResolver cr = XMPPRemoteService.this.getContentResolver();
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (String e : entries) {
                if (!first) sb.append(" or "); else first = false;
                sb.append(ConstMXA.RosterItems.XMPP_ID + "='" + e + "'");
            }
            cr.delete(ConstMXA.RosterItems.CONTENT_URI, sb.toString(), null);
        }

        @Override
        public void entriesUpdated(Collection<String> entries) {
            final ContentResolver cr = XMPPRemoteService.this.getContentResolver();
            ContentValues[] cvs = getFromStrings(entries);
            int i = 0;
            for (String e : entries) {
                String whereClause = RosterItems.XMPP_ID + "='" + cvs[i].getAsString(RosterItems.XMPP_ID) + "' AND " + RosterItems.RESSOURCE + "='" + cvs[i].getAsString(RosterItems.RESSOURCE) + "' ";
                cr.update(ConstMXA.RosterItems.CONTENT_URI, cvs[i], whereClause, null);
                i++;
            }
        }

        @Override
        public void presenceChanged(Presence presence) {
            final ContentResolver cr = XMPPRemoteService.this.getContentResolver();
            ContentValues cv = this.getFromPresences(presence);
            String whereClause = RosterItems.XMPP_ID + "='" + cv.getAsString(RosterItems.XMPP_ID) + "' AND " + RosterItems.RESSOURCE + "='" + cv.getAsString(RosterItems.RESSOURCE) + "' ";
            cr.update(ConstMXA.RosterItems.CONTENT_URI, cv, whereClause, null);
        }

        /**
		 * 
		 * @param entries
		 *            The XMPP addresses of the contacts that have been added to
		 *            the roster.
		 */
        public ContentValues[] getFromStrings(Collection<String> entries) {
            Log.v(TAG, "getFromStrings");
            final Roster r = XMPPRemoteService.this.mConn.getRoster();
            ContentValues[] cvs = new ContentValues[entries.size()];
            int i = 0;
            for (String e : entries) {
                final RosterEntry re = r.getEntry(e);
                final Presence p = r.getPresence(e);
                cvs[i] = new ContentValues();
                cvs[i].put(ConstMXA.RosterItems.XMPP_ID, StringUtils.parseBareAddress(e));
                cvs[i].put(ConstMXA.RosterItems.RESSOURCE, StringUtils.parseResource(e));
                cvs[i].put(ConstMXA.RosterItems.NAME, re.getName());
                if (p.getMode() == null && p.isAvailable()) cvs[i].put(ConstMXA.RosterItems.PRESENCE_MODE, RosterItems.MODE_AVAILABLE);
                if (p.getMode() == null && !p.isAvailable()) cvs[i].put(ConstMXA.RosterItems.PRESENCE_MODE, RosterItems.MODE_UNAVAILABLE);
                cvs[i].put(ConstMXA.RosterItems.PRESENCE_STATUS, p.getStatus());
                cvs[i].put(ConstMXA.RosterItems.UPDATED_DATE, System.currentTimeMillis());
                i++;
            }
            return cvs;
        }

        /**
		 * 
		 * @param presence
		 * @return
		 */
        public ContentValues getFromPresences(Presence presence) {
            ContentValues cv = new ContentValues();
            cv.put(ConstMXA.RosterItems.XMPP_ID, StringUtils.parseBareAddress(presence.getFrom()));
            cv.put(ConstMXA.RosterItems.RESSOURCE, StringUtils.parseResource(presence.getFrom()));
            if (presence.getMode() != null) {
                cv.put(ConstMXA.RosterItems.PRESENCE_MODE, presence.getMode().name());
                cv.put(ConstMXA.RosterItems.PRESENCE_STATUS, presence.getStatus());
            } else if (presence.isAvailable()) cv.put(ConstMXA.RosterItems.PRESENCE_MODE, RosterItems.MODE_AVAILABLE); else cv.put(ConstMXA.RosterItems.PRESENCE_MODE, RosterItems.MODE_UNAVAILABLE);
            cv.put(ConstMXA.RosterItems.UPDATED_DATE, System.currentTimeMillis());
            return cv;
        }

        /**
		 * Sends an XMPP Error Message to the recipient.
		 */
        private void sendXMPPErrorMessage(String to, XMPPError.Condition condition, String errorText) {
            org.jivesoftware.smack.packet.Message msg = new org.jivesoftware.smack.packet.Message(to, org.jivesoftware.smack.packet.Message.Type.error);
            msg.setError(new XMPPError(condition, errorText));
            mConn.sendPacket(msg);
        }

        /**
		 * Notifies all remote connection listeners on connection changes.
		 * 
		 * @param connected
		 *            If XMPP is connected or not.
		 */
        private void notifyConnectionListeners(boolean connected) {
            int i = mConnectionCallbacks.beginBroadcast();
            while (i > 0) {
                i--;
                try {
                    mConnectionCallbacks.getBroadcastItem(i).onConnectionChanged(connected);
                } catch (RemoteException e) {
                }
            }
            mConnectionCallbacks.finishBroadcast();
        }
    }

    public IXMPPService getXMPPService() {
        return mBinder;
    }

    private final IXMPPService.Stub mBinder = new IXMPPService.Stub() {

        @Override
        public void connect(Messenger acknowledgement) throws RemoteException {
            Log.i(TAG, "connect to XMPP server");
            Message msg = new Message();
            msg.what = ConstMXA.MSG_CONNECT;
            Bundle data = new Bundle();
            data.putParcelable("MSN_ACK", acknowledgement);
            msg.setData(data);
            xmppWriteWorker.mHandler.sendMessage(msg);
        }

        @Override
        public void disconnect(Messenger acknowledgement) throws RemoteException {
            Log.i(TAG, "disconnect from XMPP server");
            Message msg = new Message();
            msg.what = ConstMXA.MSG_DISCONNECT;
            Bundle data = new Bundle();
            data.putParcelable("MSN_ACK", acknowledgement);
            msg.setData(data);
            xmppWriteWorker.mHandler.sendMessage(msg);
        }

        @Override
        public void sendMessage(Messenger acknowledgement, int requestCode, XMPPMessage message) throws RemoteException {
            Message msg = Message.obtain();
            msg.what = ConstMXA.MSG_SEND_MESSAGE;
            msg.arg2 = requestCode;
            Bundle data = new Bundle();
            data.putParcelable("MSN_ACK", acknowledgement);
            data.putParcelable("PAYLOAD", message);
            msg.setData(data);
            MessageRunner mr = new MessageRunner(msg);
            mWriteExecutor.execute(mr);
        }

        @Override
        public void sendIQ(Messenger acknowledgement, Messenger result, int requestCode, XMPPIQ iq) throws RemoteException {
            Message msg = new Message();
            msg.what = ConstMXA.MSG_SEND_IQ;
            msg.arg1 = ConstMXA.MSG_STATUS_REQUEST;
            msg.arg2 = requestCode;
            Bundle data = new Bundle();
            data.putParcelable("MSN_ACK", acknowledgement);
            data.putParcelable("MSN_RESULT", result);
            data.putParcelable("PAYLOAD", iq);
            msg.setData(data);
            if (iq.type == XMPPIQ.TYPE_GET || iq.type == XMPPIQ.TYPE_SET) {
                IQRunner iqRunJob = new IQRunner(msg);
                mWriteExecutor.execute(iqRunJob);
            } else {
                xmppWriteWorker.mHandler.sendMessage(msg);
            }
        }

        @Override
        public void sendPresence(Messenger acknowledgement, int requestCode, XMPPPresence presence) throws RemoteException {
            Message msg = new Message();
            msg.what = ConstMXA.MSG_SEND_PRESENCE;
            msg.arg2 = requestCode;
            Bundle data = new Bundle();
            data.putParcelable("MSN_ACK", acknowledgement);
            data.putParcelable("PAYLOAD", presence);
            msg.setData(data);
            xmppWriteWorker.mHandler.sendMessage(msg);
        }

        @Override
        public void registerDataMessageCallback(IXMPPMessageCallback cb, String namespace, String token) throws RemoteException {
            if (cb != null) {
                IQImplFilter iif = new IQImplFilter(token, namespace);
                mMsgCallbacks.register(cb, iif);
                MessageExtensionProvider mep = new MessageExtensionProvider(namespace, token);
                ProviderManager.getInstance().addExtensionProvider(token, namespace, mep);
            }
        }

        @Override
        public void unregisterDataMessageCallback(IXMPPMessageCallback cb, String namespace, String token) throws RemoteException {
            if (cb != null) {
                IQImplFilter iif = new IQImplFilter(token, namespace);
                mMsgCallbacks.unregister(cb, iif);
            }
        }

        @Override
        public String getUsername() throws RemoteException {
            return mConn.getUser();
        }

        @Override
        public boolean isConnected() throws RemoteException {
            if (mConn != null) {
                return mConn.isAuthenticated();
            } else {
                return false;
            }
        }

        @Override
        public void registerConnectionCallback(IConnectionCallback cb) throws RemoteException {
            mConnectionCallbacks.register(cb);
        }

        @Override
        public void unregisterConnectionCallback(IConnectionCallback cb) throws RemoteException {
            mConnectionCallbacks.unregister(cb);
        }

        @Override
        public IFileTransferService getFileTransferService() throws RemoteException {
            IBinder b = mFileTransferService.onBind(null);
            return (IFileTransferService) b;
        }

        @Override
        public IMultiUserChatService getMultiUserChatService() throws RemoteException {
            IBinder b = mMultiUserChatService.onBind(null);
            return (IMultiUserChatService) b;
        }

        @Override
        public IPubSubService getPubSubService() throws RemoteException {
            IBinder b = mPubSubService.onBind(null);
            return (IPubSubService) b;
        }

        public IServiceDiscoveryService getServiceDiscoveryService() throws RemoteException {
            IBinder b = mServiceDiscoveryService.onBind(null);
            return (IServiceDiscoveryService) b;
        }

        public ICollabEditingService getCollabEditingService() throws RemoteException {
            IBinder b = mCollabEditingService.onBind(null);
            return (ICollabEditingService) b;
        }

        @Override
        public void registerIQCallback(IXMPPIQCallback cb, String elementName, String namespace) throws RemoteException {
            if (cb != null) {
                IQImplProvider iqProvider = new IQImplProvider(namespace, elementName);
                ProviderManager.getInstance().addIQProvider(elementName, namespace, iqProvider);
                IQImplFilter iqFilter = new IQImplFilter(elementName, namespace);
                boolean result = mIQCallbacks.register(cb, iqFilter);
                Log.i(TAG, "registerIQCallback(). elementName=" + elementName + " namespace=" + namespace + ". result=" + result);
            }
        }

        @Override
        public void unregisterIQCallback(IXMPPIQCallback cb, String elementName, String namespace) throws RemoteException {
            if (cb != null) {
                IQImplFilter iqFilter = new IQImplFilter(elementName, namespace);
                mIQCallbacks.unregister(cb, iqFilter);
            }
        }

        @Override
        public Bundle getXMPPConnectionParameters() throws RemoteException {
            String host = mPreferences.getString("pref_host", null);
            int port = Integer.parseInt(mPreferences.getString("pref_port", null));
            String serviceName = mPreferences.getString("pref_service", null);
            String username = mPreferences.getString("pref_xmpp_user", null);
            String password = mPreferences.getString("pref_xmpp_password", null);
            String resource = mPreferences.getString("pref_resource", null);
            Bundle connectionParams = new Bundle();
            connectionParams.putString("xmpp_host", host);
            connectionParams.putInt("xmpp_port", port);
            connectionParams.putString("xmpp_service", serviceName);
            connectionParams.putString("xmpp_user", username);
            connectionParams.putString("xmpp_password", password);
            connectionParams.putString("xmpp_resource", resource);
            connectionParams.putInt("lostiqqueue_count", mIQQueue.getCount());
            return connectionParams;
        }

        @Override
        public void registerChatStateCallback(IChatStateCallback cb, String jid) throws RemoteException {
            if (cb != null && jid != null) {
                if (mChatStateCallbacks.register(cb, jid)) Log.v(TAG, "registered callback for chatstate notification for jid:" + jid);
            }
        }

        @Override
        public void unregisterChatStateCallback(IChatStateCallback cb, String jid) throws RemoteException {
            if (cb != null && jid != null) {
                if (mChatStateCallbacks.unregister(cb, jid)) Log.v(TAG, "unregistered callback for chatstate notification for jid:" + jid);
            }
        }

        @Override
        public void sendIQInMessage(Messenger acknowledgement, XMPPIQ iq) throws RemoteException {
            Message msg = Message.obtain();
            msg.what = ConstMXA.MSG_SEND_MESSAGE;
            Bundle data = new Bundle();
            data.putParcelable("MSN_ACK", acknowledgement);
            XMPPMessage m = new XMPPMessage(iq.from, iq.to, null, XMPPMessage.TYPE_NORMAL);
            m.namespace = iq.namespace;
            m.token = iq.element;
            data.putParcelable("PAYLOAD", m);
            data.putString("PAYLOAD_MSG", iq.payload);
            msg.setData(data);
            MessageRunner mr = new MessageRunner(msg);
            mWriteExecutor.execute(mr);
        }
    };

    /**
	 * WORKAROUND for Android only! The necessary configuration files for Smack
	 * library are not included in Android's apk-Package.
	 * 
	 * @param pm
	 *            A ProviderManager instance.
	 */
    private void configureProviderManager(ProviderManager pm) {
        pm.addIQProvider("query", "jabber:iq:private", new PrivateDataManager.PrivateDataIQProvider());
        try {
            pm.addIQProvider("query", "jabber:iq:time", Class.forName("org.jivesoftware.smackx.packet.Time"));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        pm.addExtensionProvider("x", "jabber:x:roster", new RosterExchangeProvider());
        pm.addExtensionProvider("x", "jabber:x:event", new MessageEventProvider());
        pm.addExtensionProvider("active", "http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
        pm.addExtensionProvider("composing", "http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
        pm.addExtensionProvider("paused", "http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
        pm.addExtensionProvider("inactive", "http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
        pm.addExtensionProvider("gone", "http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
        pm.addExtensionProvider("html", "http://jabber.org/protocol/xhtml-im", new XHTMLExtensionProvider());
        pm.addExtensionProvider("x", "jabber:x:conference", new GroupChatInvitation.Provider());
        pm.addIQProvider("query", "http://jabber.org/protocol/disco#items", new DiscoverItemsProvider());
        pm.addIQProvider("query", "http://jabber.org/protocol/disco#info", new DiscoverInfoProvider());
        pm.addExtensionProvider("x", "jabber:x:data", new DataFormProvider());
        pm.addExtensionProvider("x", "http://jabber.org/protocol/muc#user", new MUCUserProvider());
        pm.addIQProvider("query", "http://jabber.org/protocol/muc#admin", new MUCAdminProvider());
        pm.addIQProvider("query", "http://jabber.org/protocol/muc#owner", new MUCOwnerProvider());
        pm.addExtensionProvider("x", "jabber:x:delay", new DelayInformationProvider());
        try {
            pm.addIQProvider("query", "jabber:iq:version", Class.forName("org.jivesoftware.smackx.packet.Version"));
        } catch (ClassNotFoundException e) {
        }
        pm.addIQProvider("vCard", "vcard-temp", new VCardProvider());
        pm.addIQProvider("offline", "http://jabber.org/protocol/offline", new OfflineMessageRequest.Provider());
        pm.addExtensionProvider("offline", "http://jabber.org/protocol/offline", new OfflineMessageInfo.Provider());
        pm.addIQProvider("query", "jabber:iq:last", new LastActivity.Provider());
        pm.addIQProvider("query", "jabber:iq:search", new UserSearch.Provider());
        pm.addIQProvider("sharedgroup", "http://www.jivesoftware.org/protocol/sharedgroup", new SharedGroupsInfo.Provider());
        pm.addExtensionProvider("addresses", "http://jabber.org/protocol/address", new MultipleAddressesProvider());
        pm.addIQProvider("si", "http://jabber.org/protocol/si", new StreamInitiationProvider());
        pm.addIQProvider("query", "http://jabber.org/protocol/bytestreams", new BytestreamsProvider());
        pm.addIQProvider("open", "http://jabber.org/protocol/ibb", new OpenIQProvider());
        pm.addIQProvider("close", "http://jabber.org/protocol/ibb", new CloseIQProvider());
        pm.addExtensionProvider("data", "http://jabber.org/protocol/ibb", new DataPacketProvider());
        pm.addIQProvider("query", "jabber:iq:privacy", new PrivacyProvider());
    }

    /**
	 * Forwards the MSG_RECONNECT to the XMPPRemoteService.
	 */
    protected void reconnect() {
        Message msg = new Message();
        msg.what = ConstMXA.MSG_RECONNECT;
        xmppWriteWorker.mHandler.sendMessage(msg);
    }

    /**
	 * Get all packets that are in the LostIQQueue and sends them.
	 * Assumes that for every packet there is a corresponding thread.
	 */
    public void resendIQ() {
        Log.v(TAG, "start resending iqs");
        LostIQQueueEntry e;
        int size = mIQQueue.getCount();
        for (int i = 0; i < size; i++) {
            e = mIQQueue.getOldestEntry();
            if (e == null) return;
            if (e.mSending) continue;
            e.mSending = true;
            Message msg = Message.obtain(e.mMessage);
            msg.what = ConstMXA.MSG_IQ_RESEND;
            msg.getData().putLong(ConstMXA.IQ_RESEND_ID, e.mID);
            IQRunner iqRunJob = new IQRunner(msg);
            mWriteExecutor.execute(iqRunJob);
        }
        Log.v(TAG, "end resending iqs");
    }

    /**
	 * Just forwards a call to the write worker that handles that message
	 * @author Christian Magenheimer
	 *
	 */
    private class IQQueueCheckBackgroundRunner extends TimerTask {

        @Override
        public void run() {
            Message msg = new Message();
            msg.what = ConstMXA.MSG_IQ_RESEND;
            xmppWriteWorker.mHandler.sendMessage(msg);
        }
    }
}
