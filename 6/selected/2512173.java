package mobileobserve.notification.xmpp;

import mobileobserve.app.R;
import mobileobserve.location.LocationProviderManager;
import mobileobserve.logging.ErrorFacilities;
import mobileobserve.logging.Incident;
import mobileobserve.logging.Logger;
import mobileobserve.persistence.PreferenceManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.IBinder;
import android.util.Log;

/**
 * Service der bei einem Vorfall eine Nachricht über XMPP an einen angegebenen Empfänger sendet
 *
 * @author Martin Flatau
 */
public class XmppClient extends Service {

    public static final String INCIDENT = "mobileobserve.notification.xmpp.INCIDENT";

    private XMPPConnection connection;

    private String to;

    private String text;

    private String host;

    private String port;

    private String service;

    private String username;

    private String password;

    private Logger serviceBinder;

    private Incident incident;

    private final ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName name, IBinder service) {
            XmppClient.this.serviceBinder = ((Logger.LoggingBinder) service).getService();
            sendMessage();
        }

        public void onServiceDisconnected(ComponentName name) {
            XmppClient.this.serviceBinder = null;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
    }

    /**
     * holt sich die benötigten Werte aus den Preferences und ruft die sendMessage Methode auf
     */
    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        this.incident = intent.getParcelableExtra(INCIDENT);
        SharedPreferences pref = getSharedPreferences(PreferenceManager.DEFAULT_PREFERENCE_FILE, MODE_PRIVATE);
        host = pref.getString(PreferenceManager.PREF_GTALK_HOST, null);
        port = pref.getString(PreferenceManager.PREF_GTALK_PORT, null);
        service = pref.getString(PreferenceManager.PREF_GTALK_SERVICE, null);
        username = pref.getString(PreferenceManager.PREF_GTALK_USER, null);
        password = pref.getString(PreferenceManager.PREF_GTALK_PWD, null);
        to = pref.getString(PreferenceManager.PREF_GTALK_RECEIVER, null);
        text = pref.getString(PreferenceManager.PREF_NOTIFICATION_TEXT, null);
        if (serviceBinder != null) {
            sendMessage();
        } else {
            Intent sIntent = new Intent(this, Logger.class);
            bindService(sIntent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    /**
     * startet das Senden der Nachricht in einen Thread um UI Thread nicht zu belasten
     */
    private void sendMessage() {
        new Thread() {

            @Override
            public void run() {
                sendMessageInThread();
            }
        }.start();
    }

    /**
	 * sendet die Nachticht wenn eine Verbindung zum Empfänger aufgebaut werden konnte
	 */
    private void sendMessageInThread() {
        connection = connect();
        if (connection != null) {
            if (to.length() > 0 && text.length() > 0) {
                Message msg = new Message(to, Message.Type.chat);
                SharedPreferences pref = getSharedPreferences(PreferenceManager.DEFAULT_PREFERENCE_FILE, MODE_PRIVATE);
                if (pref.getBoolean(PreferenceManager.PREF_STATE_LOCATION, false)) {
                    LocationProviderManager locDec = new LocationProviderManager(this);
                    LocationManager lm = locDec.getLocationManager();
                    String lastKnownLocText = (String) getText(R.string.xmpp_text_last_known_location);
                    text += " " + lastKnownLocText + " " + locDec.getLocationLink(lm);
                }
                msg.setBody(text);
                connection.sendPacket(msg);
                Log.d("XMPPClient", "Nachticht versendet");
            } else {
                String error = getString(R.string.xmpp_error_text_bad_config);
                Log.d("XMPPClient", error);
                serviceBinder.addErrorForObservation(incident.getParentId(), ErrorFacilities.XMPP, error);
            }
        }
    }

    /**
     * baut eine XMPP-Verbindung zum Empfänger auf
     * @return gibt die Verbindung zurück
     */
    public XMPPConnection connect() {
        ConnectionConfiguration connConfig = new ConnectionConfiguration(host, Integer.parseInt(port), service);
        XMPPConnection connection = new XMPPConnection(connConfig);
        try {
            connection.connect();
            Log.i("XMPPClient", "[SettingsDialog] Connected to " + connection.getHost());
        } catch (XMPPException ex) {
            String error = getString(R.string.xmpp_error_text_connection);
            Log.e("XMPPClient", error + " " + connection.getHost());
            Log.e("XMPPClient", ex.toString());
            serviceBinder.addErrorForObservation(incident.getParentId(), ErrorFacilities.XMPP, error + " " + connection.getHost());
            serviceBinder.addErrorForObservation(incident.getParentId(), ErrorFacilities.XMPP, ex.toString());
            return null;
        }
        try {
            connection.login(username, password);
            Log.i("XMPPClient", "Logged in as " + connection.getUser());
            Presence presence = new Presence(Presence.Type.available);
            connection.sendPacket(presence);
        } catch (XMPPException ex) {
            String error = getString(R.string.xmpp_error_text_login);
            Log.e("XMPPClient", error + " " + username);
            Log.e("XMPPClient", ex.toString());
            serviceBinder.addErrorForObservation(incident.getParentId(), ErrorFacilities.XMPP, error + " " + username);
            serviceBinder.addErrorForObservation(incident.getParentId(), ErrorFacilities.XMPP, ex.toString());
            return null;
        } catch (IllegalStateException e) {
            Log.e("XMPPClient", e.toString());
            return null;
        }
        return connection;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
