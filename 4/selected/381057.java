package cn.chengdu.in.android;

import java.lang.reflect.Method;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import cn.chengdu.in.android.app.ICDPreference;
import cn.chengdu.in.android.config.Config;
import cn.chengdu.in.android.util.AndroidUtil;
import cn.chengdu.in.android.util.StringUtil;
import cn.chengdu.in.type.Version;

/**
 * SDK3 不能兼容
 * @author Declan.Z(declan.zhang@gmail.com)
 * @date 2011-2-25
 */
public class MainAct extends TabActivity implements OnClickListener {

    private static final boolean DEBUG = Config.DEBUG;

    private static final String TAG = "MainAct";

    public static final int DIALOG_EXIT = 1001;

    private TabHost mTabHost;

    public static final int TAB_INDEX_HOME = 0;

    public static final int TAB_INDEX_PLACE = 1;

    public static final int TAB_INDEX_EVENT = 2;

    public static final int TAB_INDEX_ME = 3;

    public static final int TAB_INDEX_EXIT = 4;

    private static final int DIALOG_UPDATE = 1003;

    private static final int DIALOG_UPDATE_REQUIRE = 1004;

    private ServiceConnection mServiceConnection;

    private App mApp;

    private Version mVersion;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) Log.d(TAG, "onReceive: " + intent);
            if (App.INTENT_ACTION_LOGGED_OUT.equals(intent.getAction())) {
                boolean isLogout = intent.getBooleanExtra("isLogout", false);
                if (isLogout) {
                    stopService(new Intent(MainAct.this, MessageService.class));
                }
                finish();
            } else if (App.INTENT_ACTION_VERSION_UPDATE.equals(intent.getAction())) {
                mVersion = (Version) intent.getSerializableExtra("version");
                if (mVersion != null) {
                    mApp.setNewestVersion(mVersion);
                    if (mVersion.getLatestVer() == null || mApp.getCurrentVersion().equals(mVersion.getLatestVer())) {
                        return;
                    }
                    if (mVersion.isUpdateRequired()) {
                        showDialog(DIALOG_UPDATE_REQUIRE);
                    } else {
                        showDialog(DIALOG_UPDATE);
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerReceiver(mReceiver, new IntentFilter(App.INTENT_ACTION_LOGGED_OUT));
        registerReceiver(mReceiver, new IntentFilter(App.INTENT_ACTION_VERSION_UPDATE));
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        mApp = (App) getApplicationContext();
        initTabHost();
        bindBackgroundService();
        startService(new Intent(this, MessageService.class));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch(keyCode) {
            case KeyEvent.KEYCODE_BACK:
                showDialog(DIALOG_EXIT);
                return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch(id) {
            case DIALOG_EXIT:
                return new AlertDialog.Builder(this).setTitle(R.string.dialog_title_exit).setMessage(R.string.dialog_exit).setPositiveButton(getResources().getString(R.string.button_text_confirm), new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(App.INTENT_ACTION_LOGGED_OUT);
                        sendBroadcast(intent);
                    }
                }).setNegativeButton(getResources().getString(R.string.button_text_cancel), null).create();
            case DIALOG_UPDATE:
                return new AlertDialog.Builder(this).setTitle(StringUtil.format(this, R.string.dialog_title_version, mVersion.getLatestVer())).setMessage(StringUtil.format(this, R.string.dialog_version, mVersion.getChangeLog().replace("\\n", "\n"), mApp.getCurrentVersion())).setPositiveButton(getResources().getString(R.string.button_text_confirm), new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        beginDownload();
                    }
                }).setNegativeButton(getResources().getString(R.string.button_text_later), new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).setCancelable(false).create();
            case DIALOG_UPDATE_REQUIRE:
                return new AlertDialog.Builder(this).setTitle(StringUtil.format(this, R.string.dialog_title_version, mVersion.getLatestVer())).setMessage(StringUtil.format(this, R.string.dialog_version_no_cancel, mVersion.getChangeLog().replace("\\n", "\n"), mApp.getCurrentVersion())).setPositiveButton(getResources().getString(R.string.button_text_confirm), new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        beginDownload();
                        System.exit(0);
                    }
                }).setNegativeButton(getResources().getString(R.string.button_text_exit), new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        System.exit(0);
                    }
                }).setCancelable(false).create();
        }
        return super.onCreateDialog(id);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        unbindService(mServiceConnection);
        mApp.clearLastLocation();
        if (mApp.getPreference().getBoolean(ICDPreference.KEY_EXIT_CLEAN, false)) {
            mApp.getmRrm().clear();
            System.exit(0);
        }
    }

    @Override
    public void onClick(View v) {
    }

    private void initTabHost() {
        if (mTabHost != null) {
            if (DEBUG) Log.e(TAG, "tabhost already exist");
        }
        mTabHost = getTabHost();
        String startPage = mApp.getPreference().getString(ICDPreference.KEY_START_PAGE);
        if ("1".equals(startPage)) {
            addTab(TAB_INDEX_PLACE, R.string.tab_title_place, new Intent(this, PlaceListAct.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            addTab(TAB_INDEX_HOME, R.string.tab_title_home, new Intent(this, TimeLineAct.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        } else {
            addTab(TAB_INDEX_HOME, R.string.tab_title_home, new Intent(this, TimeLineAct.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            addTab(TAB_INDEX_PLACE, R.string.tab_title_place, new Intent(this, PlaceListAct.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }
        addTab(TAB_INDEX_EVENT, R.string.tab_title_badge, new Intent(this, BadgeGroupAct.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        addTab(TAB_INDEX_ME, R.string.tab_title_me, new Intent(this, UserInfoAct.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    private void addTab(int index, int title, Intent intent) {
        TabSpec tabSpec = mTabHost.newTabSpec("tab" + index).setIndicator(createTabIndicatorView(title, index)).setContent(intent);
        mTabHost.addTab(tabSpec);
    }

    private View createTabIndicatorView(int title, int index) {
        View v = LayoutInflater.from(this).inflate(R.layout.main_nav, null);
        TextView tv = (TextView) v.findViewById(R.id.text);
        tv.setText(title);
        ImageView iv = (ImageView) v.findViewById(R.id.image);
        switch(index) {
            case 0:
                iv.setImageResource(R.drawable.main_tab_1);
                break;
            case 1:
                iv.setImageResource(R.drawable.main_tab_2);
                break;
            case 2:
                iv.setImageResource(R.drawable.main_tab_3);
                break;
            case 3:
                iv.setImageResource(R.drawable.main_tab_4);
                break;
        }
        return v;
    }

    private void bindBackgroundService() {
        Intent intent = new Intent(this, BackgroundService.class);
        mServiceConnection = new ServiceConnection() {

            public void onServiceDisconnected(ComponentName name) {
            }

            public void onServiceConnected(ComponentName name, IBinder service) {
            }
        };
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void sendMessageToYouMi() {
        int code = -1;
        try {
            code = AndroidUtil.getChannelId(this);
            Class clazz = Class.forName("net.youmi.activate.Counter");
            Method method = clazz.getDeclaredMethod("asyncActivate", Context.class, int.class);
            method.invoke(null, this, code);
            Log.d("Youmi", "channelId:" + code + "发送成功");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("YoumiError", "channelId:" + code + "发送失败");
        }
    }

    private void toggleViews(int visibility, View... views) {
        for (View v : views) {
            v.setVisibility(visibility);
        }
    }

    /**
     * 启动下载任务
     */
    private void beginDownload() {
        Uri uri = Uri.parse(mVersion.getDownloadUri());
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }
}
