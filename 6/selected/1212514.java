package net.sf.andhsli.hotspotlogin;

import org.json.JSONException;
import net.sf.andhsli.hotspotlogin.HotspotUtilities.HotspotStatus;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.widget.Toast;
import net.sf.andhsli.hotspotlogin.IHotspotLoginService;
import net.sf.andhsli.hotspotlogin.R;

/**
 * This is an example of implementing an application service that will run in
 * response to an alarm, allowing us to move long duration work out of an
 * intent receiver.
 * 
 * @see AlarmService
 * @see AlarmService_Alarm
 */
public class HotspotLogin_Service extends Service {

    private static final String TAG = "HSLI.SVC";

    private static final String WIFI_LOCK_NAME = "net.sf.andhsli.HotspotLogin";

    private static final int MAX_FAILED_LOGINS = 3;

    private static final int SSIDNOTFOUNDWAIT = 60;

    private static final int INTERNETWAIT = 60;

    private static final int LONGWAIT = 60;

    private static final int MEDIUMWAIT = 15;

    private static final int SHORTWAIT = 5;

    private long WIFIDISABLED_POLL_INTERVAL = LONGWAIT;

    private long SSIDNOTFOUND_POLL_INTERVAL = SSIDNOTFOUNDWAIT;

    private long NETCONFIGNOTFOUND_POLL_INTERVAL = LONGWAIT;

    private long DISCONNECTED_POLL_INTERVAL = SHORTWAIT;

    private long CONNECTING_POLL_INTERVAL = SHORTWAIT;

    private long SWITCHTOWIFI_POLL_INTERVAL = SHORTWAIT;

    private long LOGINFAILED_POLL_INTERVAL = MEDIUMWAIT;

    private long LOGINOK_POLL_INTERVAL = SHORTWAIT;

    private long LOGINERROR_POLL_INTERVAL = MEDIUMWAIT;

    private long INTERNET_POLL_INTERVAL = INTERNETWAIT;

    private long ERROR_POLL_INTERVAL = MEDIUMWAIT;

    private long RESCAN_INTERVAL = SSIDNOTFOUNDWAIT;

    private Params params;

    private String login;

    private String pw;

    private boolean wifiLockEnabled;

    private boolean autoLoginEnabled;

    private boolean quitThread;

    private int cntFailedLogins;

    private HotspotStatus lastHsStatus;

    private long lastSsidScan;

    private long sleepSeconds;

    private NotificationManager mNM;

    protected WifiLock hsLock;

    private SimplePersistence persist;

    private SimplePersistence persistSettings;

    @Override
    public void onCreate() {
        Log.i(TAG, Long.toString(System.currentTimeMillis() & 0x3ffff));
        persist = new SimplePersistence(this, HotspotLoginActivity.PREFS_NAME);
        persistSettings = new SimplePersistence(this, HotspotLoginActivity.SETTINGPREFS_NAME);
        params = new Params();
        login = "";
        pw = "";
        wifiLockEnabled = false;
        autoLoginEnabled = false;
        quitThread = false;
        cntFailedLogins = 0;
        lastHsStatus = HotspotStatus.DISCONNECTED;
        sleepSeconds = -1;
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        hsLock = null;
        String toastMsg = "Hotspot Login Service started";
        boolean ok = loadSettings();
        if (!ok) toastMsg = "HotspotLoginService started with invalid settings (json format)";
        loadValues();
        Thread thr = new Thread(null, mTask, "HotspotLogin_Service");
        thr.start();
        Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show();
        registerReceiver(mBR, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
        registerReceiver(mBR, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
        registerReceiver(mBR, new IntentFilter(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION));
        registerReceiver(mBR, new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION));
        registerReceiver(mBR, new IntentFilter(WifiManager.NETWORK_IDS_CHANGED_ACTION));
        registerReceiver(mBR, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        registerReceiver(mBR, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    private void setWaitTimes() {
        long shortWait = params.getNumShortWait(SHORTWAIT);
        long mediumWait = params.getNumMediumWait(MEDIUMWAIT);
        long longWait = params.getNumLongWait(LONGWAIT);
        long ssidnotfoundWait = params.getNumWaitForSsidPollIntervall(SSIDNOTFOUNDWAIT);
        long internetWait = params.getNumInternetPollIntervall(INTERNETWAIT);
        WIFIDISABLED_POLL_INTERVAL = longWait;
        SSIDNOTFOUND_POLL_INTERVAL = ssidnotfoundWait;
        NETCONFIGNOTFOUND_POLL_INTERVAL = longWait;
        DISCONNECTED_POLL_INTERVAL = shortWait;
        CONNECTING_POLL_INTERVAL = shortWait;
        SWITCHTOWIFI_POLL_INTERVAL = shortWait;
        LOGINFAILED_POLL_INTERVAL = mediumWait;
        LOGINOK_POLL_INTERVAL = shortWait;
        LOGINERROR_POLL_INTERVAL = mediumWait;
        INTERNET_POLL_INTERVAL = internetWait;
        ERROR_POLL_INTERVAL = mediumWait;
        RESCAN_INTERVAL = ssidnotfoundWait;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        quitThread = true;
        autoLoginEnabled = false;
        synchronized (HotspotLogin_Service.this) {
            HotspotLogin_Service.this.notifyAll();
        }
        unregisterReceiver(mBR);
        mNM.cancel(R.string.hsli_svc_started);
        WifiLock wifiLock = hsLock;
        hsLock = null;
        if (wifiLock != null) {
            synchronized (wifiLock) {
                if (wifiLock.isHeld()) wifiLock.release();
            }
        }
        Toast.makeText(this, "HotspotLoginService stopped", Toast.LENGTH_SHORT).show();
    }

    /**
     * The function that runs in our worker thread
     */
    Runnable mTask = new Runnable() {

        public void run() {
            Log.i(TAG, "STARTING AUTOLOGIN TASK");
            while (!quitThread) {
                Log.i(TAG, "HotspotLogin_Service loop: auto-login=" + Boolean.toString(autoLoginEnabled));
                sleepSeconds = -1;
                if (autoLoginEnabled) {
                    try {
                        WifiManager wifiMan = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                        ConnectivityManager conyMan = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                        if (wifiLockEnabled) {
                            if (hsLock == null) {
                                try {
                                    WifiLock wl = wifiMan.createWifiLock(WIFI_LOCK_NAME);
                                    wl.acquire();
                                    Log.i(TAG, "acquired WifiLock");
                                    hsLock = wl;
                                } catch (Exception e) {
                                    Log.e(TAG, "acquire WIFILOCK threw: " + e.getMessage(), e);
                                }
                            }
                        } else {
                            WifiLock wl = hsLock;
                            hsLock = null;
                            if (wl != null) {
                                try {
                                    synchronized (wl) {
                                        if (wl.isHeld()) {
                                            wl.release();
                                            Log.i(TAG, "released WifiLock");
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "release WIFILOCK threw: " + e.getMessage(), e);
                                }
                            }
                        }
                        HotspotStatus hsStatus = HotspotUtilities.getHotspotStatus(params, wifiMan, conyMan);
                        autoLogin(hsStatus, wifiMan, hsStatus != lastHsStatus);
                        if (hsStatus != lastHsStatus) sleepSeconds = 2;
                        lastHsStatus = hsStatus;
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                } else {
                    if (lastHsStatus.equals(HotspotStatus.INTERNET) || lastHsStatus.equals(HotspotStatus.HOTSPOTCONNECTED) || lastHsStatus.equals(HotspotStatus.UNKNOWNHOTSPOT)) {
                        HotspotUtilities.logout(params);
                        lastHsStatus = HotspotStatus.DISCONNECTED;
                    }
                }
                synchronized (HotspotLogin_Service.this) {
                    try {
                        Log.i(TAG, "WAIT: " + sleepSeconds + " - " + lastHsStatus.toString());
                        if (sleepSeconds == -1) HotspotLogin_Service.this.wait(); else HotspotLogin_Service.this.wait(sleepSeconds * 1000);
                    } catch (Exception ignore) {
                    }
                }
            }
            Log.i(TAG, "LEAVING AUTOLOGIN TASK");
        }

        private void autoLogin(HotspotStatus hsStatus, WifiManager wifiMan, boolean hsStateChanged) {
            int iconResId = R.drawable.hsli_red;
            String msg = "unknown hsStatus: " + hsStatus;
            Intent intent = new Intent(HotspotLogin_Service.this, HotspotLoginActivity.class);
            switch(hsStatus) {
                case WIFIDISABLED:
                    {
                        iconResId = R.drawable.hsli_red;
                        msg = "wifi is disabled, please enable in settings";
                        intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                        sleepSeconds = WIFIDISABLED_POLL_INTERVAL;
                        if (hsStateChanged) HotspotUtilities.playSound();
                        break;
                    }
                case SSIDNOTFOUND:
                    {
                        iconResId = R.drawable.hsli_red;
                        msg = "waiting for SSID '" + params.getSSID() + "'";
                        sleepSeconds = SSIDNOTFOUND_POLL_INTERVAL;
                        if (System.currentTimeMillis() - lastSsidScan > RESCAN_INTERVAL * 1000) {
                            wifiMan.startScan();
                            lastSsidScan = System.currentTimeMillis();
                        }
                        if (hsStateChanged) HotspotUtilities.playSound();
                        break;
                    }
                case NETCONFIGNOTFOUND:
                    {
                        iconResId = R.drawable.hsli_yellow;
                        msg = "no config found for '" + params.getSSID() + "', please connect manually";
                        intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                        sleepSeconds = NETCONFIGNOTFOUND_POLL_INTERVAL;
                        if (hsStateChanged) HotspotUtilities.playSound();
                        break;
                    }
                case DISCONNECTED:
                    {
                        iconResId = R.drawable.hsli_yellow;
                        String connectResult = HotspotUtilities.connect(params, wifiMan);
                        msg = "connect to '" + params.getSSID() + "': " + connectResult;
                        sleepSeconds = DISCONNECTED_POLL_INTERVAL;
                        break;
                    }
                case CONNECTING:
                    {
                        String info = " - state=?";
                        try {
                            WifiInfo conInfo = wifiMan.getConnectionInfo();
                            if (conInfo != null) {
                                SupplicantState supState = conInfo.getSupplicantState();
                                if (supState != null) info = " - state=" + supState.toString();
                            }
                        } catch (Exception ignore) {
                        }
                        iconResId = R.drawable.hsli_yellow;
                        msg = "connecting to '" + params.getSSID() + "'" + info;
                        sleepSeconds = CONNECTING_POLL_INTERVAL;
                        break;
                    }
                case SWITCHTOWIFI:
                    {
                        iconResId = R.drawable.hsli_yellow;
                        msg = "waiting for WIFI to become active";
                        sleepSeconds = SWITCHTOWIFI_POLL_INTERVAL;
                        break;
                    }
                case HOTSPOTCONNECTED:
                    {
                        if (cntFailedLogins == -1) {
                            iconResId = R.drawable.hsli_yellow;
                            msg = "login failed, wrong username/password";
                            sleepSeconds = LOGINFAILED_POLL_INTERVAL;
                            break;
                        }
                        if (cntFailedLogins >= MAX_FAILED_LOGINS) {
                            iconResId = R.drawable.hsli_yellow;
                            msg = "login failed, max retry (" + MAX_FAILED_LOGINS + ") reached";
                            sleepSeconds = LOGINFAILED_POLL_INTERVAL;
                            break;
                        }
                        String loginResult = HotspotUtilities.login(params, login, pw);
                        if (loginResult.equals("OK")) {
                            cntFailedLogins = 0;
                            iconResId = R.drawable.hsli_yellow;
                            msg = "login succeeded";
                            sleepSeconds = LOGINOK_POLL_INTERVAL;
                            break;
                        }
                        if (loginResult.equals("WRONG")) {
                            cntFailedLogins = -1;
                            iconResId = R.drawable.hsli_yellow;
                            msg = "login failed, wrong username/password";
                            sleepSeconds = LOGINFAILED_POLL_INTERVAL;
                            break;
                        }
                        cntFailedLogins += 1;
                        iconResId = R.drawable.hsli_yellow;
                        msg = "unknown login response";
                        sleepSeconds = LOGINERROR_POLL_INTERVAL;
                        break;
                    }
                case INTERNET:
                    {
                        iconResId = R.drawable.hsli_green;
                        msg = "";
                        sleepSeconds = INTERNET_POLL_INTERVAL;
                        if (hsStateChanged) HotspotUtilities.playSound();
                        break;
                    }
                case UNKNOWNHOTSPOT:
                    {
                        iconResId = R.drawable.hsli_red;
                        msg = "unknown hotspot type";
                        sleepSeconds = ERROR_POLL_INTERVAL;
                        break;
                    }
                case ERROR:
                    {
                        HotspotUtilities.disconnect(params, wifiMan);
                        iconResId = R.drawable.hsli_red;
                        msg = "error querying hotspot state";
                        sleepSeconds = ERROR_POLL_INTERVAL;
                        break;
                    }
            }
            if ((hsStatus != HotspotStatus.WIFIDISABLED) && (hsStatus != HotspotStatus.SSIDNOTFOUND) && (hsStatus != HotspotStatus.NETCONFIGNOTFOUND) && (hsStatus != HotspotStatus.INTERNET)) {
                HotspotUtilities.playSound();
            }
            showNotification(iconResId, msg, intent);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * IHotspotLoginService is defined through aidl
     */
    private final IHotspotLoginService.Stub mBinder = new IHotspotLoginService.Stub() {

        @Override
        public void setSettingParams(String jsonParams) throws RemoteException {
            Log.i(TAG, "setSettingParams('" + jsonParams + "')");
            persistSettings.putString("params", jsonParams);
            persistSettings.commit();
            loadSettings();
            notifyService();
        }

        @Override
        public void setLoginParams(String login, String pw, boolean enableWifiLock) throws RemoteException {
            Log.i(TAG, "setLoginParams('" + login + "',****," + enableWifiLock + ")");
            persist.putString("login", login);
            if (pw != null) persist.putEncryptedString("pw", pw);
            persist.putBoolean("lockWifi", enableWifiLock);
            persist.commit();
            loadValues();
            notifyService();
        }

        @Override
        public void start() throws RemoteException {
            Log.i(TAG, "start()");
            persist.putBoolean("autoLoginEnabled", true);
            persist.commit();
            loadSettings();
            loadValues();
            Log.i(TAG, "login=" + login + ",pw=****,autoLoginEnabled=" + autoLoginEnabled + ",wifiLockEnabled=" + wifiLockEnabled);
            try {
                Log.i(TAG, "params=" + params.toJSON(1));
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
            }
            notifyService();
            Log.i(TAG, "start finished");
        }

        @Override
        public void stop() throws RemoteException {
            persist.putBoolean("autoLoginEnabled", false);
            persist.commit();
            loadValues();
            notifyService();
            mNM.cancel(R.string.hsli_svc_started);
        }

        @Override
        public String getJsonParams() throws RemoteException {
            String result = null;
            try {
                result = params.toJSON(0);
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
            }
            return result;
        }

        @Override
        public String getLogin() throws RemoteException {
            return login;
        }

        @Override
        public String getEncryptedPW() throws RemoteException {
            String encryptedPw = "";
            try {
                encryptedPw = SimpleCrypto.encrypt(HotspotLoginActivity.TRANSMIT_SEED, pw);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
            return encryptedPw;
        }

        @Override
        public boolean getWifiLockEnabled() throws RemoteException {
            return wifiLockEnabled;
        }

        @Override
        public boolean isRunning() throws RemoteException {
            return autoLoginEnabled;
        }

        @Override
        public int getCurrentHotspotStatus() throws RemoteException {
            return lastHsStatus.ordinal();
        }

        @Override
        public String getCurrentHotspotStatusName() throws RemoteException {
            return lastHsStatus.toString();
        }

        @Override
        public int getFailedLoginCount() throws RemoteException {
            return cntFailedLogins;
        }
    };

    /**
     * Show a notification while this service is running.
     */
    private void showNotification(int iconResId, String text, Intent intent) {
        Log.i(TAG, "showNotification: '" + text + "'");
        Notification notification = new Notification(iconResId, text, System.currentTimeMillis());
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
        notification.setLatestEventInfo(this, getText(R.string.hsli_svc_name), text, contentIntent);
        mNM.notify(R.string.hsli_svc_started, notification);
    }

    private void loadValues() {
        login = persist.getString("login", "");
        pw = persist.getDecryptedString("pw", "");
        wifiLockEnabled = persist.getBoolean("lockWifi", false);
        autoLoginEnabled = persist.getBoolean("autoLoginEnabled", false);
    }

    private boolean loadSettings() {
        String jsonText = persistSettings.getString("params", null);
        try {
            params.fromJSON(jsonText);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return false;
        }
        setWaitTimes();
        return true;
    }

    BroadcastReceiver mBR = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (autoLoginEnabled) {
                Log.i(TAG + ".BROADCAST", intent.getAction());
                if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                    int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
                    Log.i(TAG + ".BROADCAST", "wifiState=" + wifiState);
                    if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
                        if (lastHsStatus == HotspotStatus.WIFIDISABLED) {
                            notifyService();
                        }
                    }
                    if ((wifiState == WifiManager.WIFI_STATE_DISABLED)) {
                        if (lastHsStatus != HotspotStatus.WIFIDISABLED) {
                            notifyService();
                        }
                    }
                } else if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                    if (lastHsStatus == HotspotStatus.SSIDNOTFOUND) {
                        lastSsidScan = System.currentTimeMillis();
                        notifyService();
                    }
                } else if (intent.getAction().equals(WifiManager.NETWORK_IDS_CHANGED_ACTION)) {
                    if (lastHsStatus == HotspotStatus.NETCONFIGNOTFOUND) {
                        notifyService();
                    }
                } else if (intent.getAction().equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
                    if (lastHsStatus == HotspotStatus.DISCONNECTED) {
                        notifyService();
                    }
                    if (lastHsStatus == HotspotStatus.SSIDNOTFOUND) {
                        notifyService();
                    }
                } else if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                    if (lastHsStatus == HotspotStatus.CONNECTING) {
                        notifyService();
                    }
                } else if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                    if (lastHsStatus == HotspotStatus.SWITCHTOWIFI) {
                        notifyService();
                    } else if (lastHsStatus == HotspotStatus.INTERNET) {
                        notifyService();
                    } else if (lastHsStatus == HotspotStatus.SSIDNOTFOUND) {
                        notifyService();
                    }
                }
            }
        }
    };

    public void notifyService() {
        synchronized (HotspotLogin_Service.this) {
            HotspotLogin_Service.this.notifyAll();
        }
    }
}
