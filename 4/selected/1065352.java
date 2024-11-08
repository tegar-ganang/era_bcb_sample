package cn.chengdu.in.android.util;

import cn.chengdu.in.android.R;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.provider.Settings.System;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.widget.Toast;

/**
 * @author Declan.z(declan.zhang@gmail.com)
 * @date 2011-5-12
 */
public class AndroidUtil {

    /**
     * sdcard是否可用
     * 
     * @return
     */
    public static boolean isSDCardAvailable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    /**
     * 定位器是否可用 gps或者netwrok打开一个即可
     * 
     * @param context
     * @return
     */
    public static boolean isLocationProviderAvailable(Context context) {
        String locations = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        return locations != null && (locations.contains(LocationManager.GPS_PROVIDER) || locations.contains(LocationManager.NETWORK_PROVIDER));
    }

    /**
     * 是否打开GPS
     * @param context
     * @return
     */
    public static boolean isGpsProviderAvailable(Context context) {
        String locations = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        return locations != null && locations.contains(LocationManager.GPS_PROVIDER);
    }

    /**
     * 网络连接是否可用
     * 
     * @param context
     * @return
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        NetworkInfo[] netinfo = cm.getAllNetworkInfo();
        if (netinfo == null) {
            return false;
        }
        for (int i = 0; i < netinfo.length; i++) {
            if (netinfo[i].isConnected()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取屏幕宽度
     * @return
     */
    public static int getScreenWidth(Activity context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.widthPixels;
    }

    /**
     * 获取设备唯一编码
     * 规则: android-ANDROID_ID-IMEI-IMSI
     * 如 android-b63f657eab2edea-354316035235342-460006303765728
     * IMEI为null时取ANDROID_ID, IMSI为null时取IMEI
     * @param context
     * @return
     */
    public static String getDeviceId(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String androidId = System.getString(context.getContentResolver(), System.ANDROID_ID);
        String imei = tm.getDeviceId();
        String imsi = tm.getSubscriberId();
        imei = imei == null ? androidId : imei;
        imsi = imsi == null ? imei : imsi;
        StringBuffer sb = new StringBuffer(128);
        sb.append("android-");
        sb.append(androidId);
        sb.append("-");
        sb.append(imei);
        sb.append("-");
        sb.append(imsi);
        return sb.toString();
    }

    public static int getChannelId(Context context) {
        try {
            return context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA).metaData.getInt("ICD_CHANNEL");
        } catch (NameNotFoundException e) {
            return 0;
        }
    }

    /**
     * 手机是否支持google map add-on
     * @return
     */
    public static final boolean isGoogleMapAvailable() {
        try {
            Class.forName("com.google.android.maps.MapActivity");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 显示分享对话框
     * @param context 
     * @param messageId 分享的正文内容模板ID
     * @param values 分享的征文内容数据
     */
    public static final void showShareDialog(Context context, int messageId, String... values) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, context.getResources().getString(R.string.share_title));
        intent.putExtra(Intent.EXTRA_TEXT, StringUtil.format(context, messageId, values));
        context.startActivity(Intent.createChooser(intent, context.getResources().getString(R.string.share_dialog_title)));
    }

    /**
     * 显示拨号界面
     * @param context
     * @param phone
     */
    public static final void showDial(Context context, String phone) {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel://" + phone));
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.toast_setting_dial_error, Toast.LENGTH_LONG).show();
        }
    }

    public static String getVersion() {
        return "Android-" + Build.VERSION.RELEASE;
    }

    public static String getModel() {
        return Build.BRAND + " " + Build.MODEL;
    }

    /**
     * dp -> px 最好不要用, 不要用在字体上, 某些机子如M9会出错的..
     * @param contxt
     * @param dp
     * @return
     */
    public static final int dp2px(Context contxt, int dp) {
        final float scale = contxt.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    public static final String getDpi(Activity act) {
        int width = AndroidUtil.getScreenWidth(act);
        if (width > 480) {
            return "u";
        } else if (width < 480) {
            return "m";
        } else {
            return "h";
        }
    }
}
