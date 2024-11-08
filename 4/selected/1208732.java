package org.aladdinframework.contextplugin.api.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.IntentSender.SendIntentException;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Secured version of an Android Context, which is provided to ContextPlugins during runtime. A SecuredContext is
 * configured with a set of Permissions, which are used to guard access to critical resources, such as Android system
 * services. <br>
 * Note: This class is currently functional, but under development.
 * 
 * @author Darren Carlson
 */
public class SecuredContext extends Context {

    private Context c;

    private Set<Permission> permissions;

    public SecuredContext(Context c, Set<Permission> permissions) {
        this.c = c;
        this.permissions = permissions;
    }

    @Override
    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
        return false;
    }

    @Override
    public int checkCallingOrSelfPermission(String permission) {
        return 0;
    }

    @Override
    public int checkCallingOrSelfUriPermission(Uri uri, int modeFlags) {
        return 0;
    }

    @Override
    public int checkCallingPermission(String permission) {
        return 0;
    }

    @Override
    public int checkCallingUriPermission(Uri uri, int modeFlags) {
        return 0;
    }

    @Override
    public int checkPermission(String permission, int pid, int uid) {
        return 0;
    }

    @Override
    public int checkUriPermission(Uri uri, int pid, int uid, int modeFlags) {
        return 0;
    }

    @Override
    public int checkUriPermission(Uri uri, String readPermission, String writePermission, int pid, int uid, int modeFlags) {
        return 0;
    }

    @Override
    public void clearWallpaper() throws IOException {
    }

    @Override
    public Context createPackageContext(String packageName, int flags) throws NameNotFoundException {
        return null;
    }

    @Override
    public String[] databaseList() {
        return null;
    }

    @Override
    public boolean deleteDatabase(String name) {
        return false;
    }

    @Override
    public boolean deleteFile(String name) {
        return false;
    }

    @Override
    public void enforceCallingOrSelfPermission(String permission, String message) {
    }

    @Override
    public void enforceCallingOrSelfUriPermission(Uri uri, int modeFlags, String message) {
    }

    @Override
    public void enforceCallingPermission(String permission, String message) {
    }

    @Override
    public void enforceCallingUriPermission(Uri uri, int modeFlags, String message) {
    }

    @Override
    public void enforcePermission(String permission, int pid, int uid, String message) {
    }

    @Override
    public void enforceUriPermission(Uri uri, int pid, int uid, int modeFlags, String message) {
    }

    @Override
    public void enforceUriPermission(Uri uri, String readPermission, String writePermission, int pid, int uid, int modeFlags, String message) {
    }

    @Override
    public String[] fileList() {
        return null;
    }

    @Override
    public Context getApplicationContext() {
        return null;
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        return null;
    }

    @Override
    public AssetManager getAssets() {
        return c.getAssets();
    }

    @Override
    public File getCacheDir() {
        return null;
    }

    @Override
    public ClassLoader getClassLoader() {
        return null;
    }

    @Override
    public ContentResolver getContentResolver() {
        return null;
    }

    @Override
    public File getDatabasePath(String name) {
        return null;
    }

    @Override
    public File getDir(String name, int mode) {
        return null;
    }

    @Override
    public File getFileStreamPath(String name) {
        return null;
    }

    @Override
    public File getFilesDir() {
        return null;
    }

    @Override
    public Looper getMainLooper() {
        return null;
    }

    @Override
    public PackageManager getPackageManager() {
        return null;
    }

    @Override
    public String getPackageName() {
        return null;
    }

    @Override
    public Resources getResources() {
        return c.getResources();
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return null;
    }

    private final String TAG = this.getClass().getSimpleName();

    @Override
    public Object getSystemService(String serviceName) {
        Log.v(TAG, "getSystemService for: " + serviceName);
        for (Permission p : permissions) {
            Log.v(TAG, "Checking Permission: " + p);
            if (p.getPermissionString().equalsIgnoreCase(serviceName) && p.isPermissionGranted()) {
                Log.v(TAG, "Permission GRANTED for: " + serviceName);
                return c.getSystemService(serviceName);
            }
        }
        Log.w(TAG, "Permission DENIED for: " + serviceName);
        return null;
    }

    @Override
    public Theme getTheme() {
        return c.getTheme();
    }

    @Override
    public Drawable getWallpaper() {
        return null;
    }

    @Override
    public int getWallpaperDesiredMinimumHeight() {
        return c.getWallpaperDesiredMinimumHeight();
    }

    @Override
    public int getWallpaperDesiredMinimumWidth() {
        return c.getWallpaperDesiredMinimumWidth();
    }

    @Override
    public void grantUriPermission(String toPackage, Uri uri, int modeFlags) {
    }

    @Override
    public FileInputStream openFileInput(String name) throws FileNotFoundException {
        return null;
    }

    @Override
    public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
        return null;
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, CursorFactory factory) {
        return null;
    }

    @Override
    public Drawable peekWallpaper() {
        return null;
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        for (Permission p : permissions) {
            if (p.getPermissionString().equalsIgnoreCase(AladdinPermissions.MANAGE_BROADCAST_RECEIVERS) && p.isPermissionGranted()) return c.registerReceiver(receiver, filter);
        }
        return null;
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, String broadcastPermission, Handler scheduler) {
        for (Permission p : permissions) {
            if (p.getPermissionString().equalsIgnoreCase(AladdinPermissions.MANAGE_BROADCAST_RECEIVERS) && p.isPermissionGranted()) return c.registerReceiver(receiver, filter, broadcastPermission, scheduler);
        }
        return null;
    }

    @Override
    public void removeStickyBroadcast(Intent intent) {
    }

    @Override
    public void revokeUriPermission(Uri uri, int modeFlags) {
    }

    @Override
    public void sendBroadcast(Intent intent) {
    }

    @Override
    public void sendBroadcast(Intent intent, String receiverPermission) {
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, String receiverPermission) {
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, String receiverPermission, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
    }

    @Override
    public void sendStickyBroadcast(Intent intent) {
    }

    @Override
    public void sendStickyOrderedBroadcast(Intent intent, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
    }

    @Override
    public void setTheme(int resid) {
    }

    @Override
    public void setWallpaper(Bitmap bitmap) throws IOException {
    }

    @Override
    public void setWallpaper(InputStream data) throws IOException {
    }

    @Override
    public void startActivity(Intent intent) {
    }

    @Override
    public boolean startInstrumentation(ComponentName className, String profileFile, Bundle arguments) {
        return false;
    }

    @Override
    public void startIntentSender(IntentSender intent, Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags) throws SendIntentException {
    }

    @Override
    public ComponentName startService(Intent service) {
        return null;
    }

    @Override
    public boolean stopService(Intent service) {
        return false;
    }

    @Override
    public void unbindService(ServiceConnection conn) {
    }

    @Override
    public void unregisterReceiver(BroadcastReceiver receiver) {
        for (Permission p : permissions) {
            if (p.getPermissionString().equalsIgnoreCase(AladdinPermissions.MANAGE_BROADCAST_RECEIVERS) && p.isPermissionGranted()) c.unregisterReceiver(receiver);
        }
    }
}
