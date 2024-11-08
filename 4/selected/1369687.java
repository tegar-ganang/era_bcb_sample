package com.android.cnes.groundsupport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

public class GroundSupportActivity extends Activity implements Runnable {

    private Intent i;

    private int SLEEP_TIME = 2000;

    private File fileToCopy;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        i = new Intent(this, IndexActivity.class);
        Thread thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        File appRootFolder = new File(Environment.getExternalStorageDirectory() + "/Cnes/");
        if (!appRootFolder.exists()) {
            if (appRootFolder.mkdirs()) {
                Log.d("Root folder", "created");
            } else {
                Log.d("Root folder", "not created");
            }
        }
        CopyAssets();
        SystemClock.sleep(SLEEP_TIME);
        Message message = new Message();
        handler.sendMessage(message);
    }

    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            startActivity(i);
            finish();
        }
    };

    /**
	 * To copy the properties file on the sd card
	 */
    private void CopyAssets() {
        AssetManager assetManager = getAssets();
        String[] files = null;
        try {
            files = assetManager.list("");
        } catch (IOException e) {
            Log.e("tag", e.getMessage());
        }
        for (String filename : files) {
            InputStream in = null;
            OutputStream out = null;
            fileToCopy = new File(Environment.getExternalStorageDirectory() + "/Cnes/" + filename);
            if (!fileToCopy.exists()) {
                try {
                    in = assetManager.open(filename);
                    out = new FileOutputStream(fileToCopy);
                    copyFile(in, out);
                    in.close();
                    in = null;
                    out.flush();
                    out.close();
                    out = null;
                } catch (Exception e) {
                    Log.e("tag", e.getMessage());
                }
            }
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }
}
