package com.narunas;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import com.narunas.R;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;
import org.apache.commons.net.ftp.*;
import com.narunas.sdSaver;

public class WXdroid extends Activity {

    /** Called when the activity is first created. */
    public ImageLoader img_loader;

    public sdSaver SDSaver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Button btn = (Button) findViewById(R.id.Button01);
        btn.setOnClickListener(onSDCardClick);
        Spinner s1 = (Spinner) findViewById(R.id.sp_surface_fax);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.surface_hourly_forecast, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s1.setAdapter(adapter);
        Spinner s2 = (Spinner) findViewById(R.id.sp_500mb_fax);
        adapter = ArrayAdapter.createFromResource(this, R.array.highalt_hourly_forecast, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s2.setAdapter(adapter);
        Spinner s3 = (Spinner) findViewById(R.id.sp_windwave);
        adapter = ArrayAdapter.createFromResource(this, R.array.wave_hourly_forecast, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s3.setAdapter(adapter);
        Spinner s4 = (Spinner) findViewById(R.id.sp_satellite);
        adapter = ArrayAdapter.createFromResource(this, R.array.satellite_images, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s4.setAdapter(adapter);
    }

    public OnItemSelectedListener onSpinItemSelectListener = new OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView adapter, View arg1, int arg2, long arg3) {
            img_loader.viewFile(2);
        }

        @Override
        public void onNothingSelected(AdapterView adapter) {
        }
    };

    private void onSDImageSaved() {
        String file_path = "ftp://tgftp.nws.noaa.gov/fax/PJAI10.gif";
        Uri uri_d = Uri.parse("file:///sdcard/NOAAWX/evnt99.jpg");
        Intent i = new Intent();
        i.setDataAndType(uri_d, "image/*");
        i.setAction(Intent.ACTION_VIEW);
        startActivity(i);
    }

    public void showImage() {
        Uri uri_d = Uri.parse("file:///sdcard/NOAAWX/PYAA10.gif");
        Intent i = new Intent();
        i.setDataAndType(uri_d, "image/*");
        i.setAction(Intent.ACTION_VIEW);
        startActivity(i);
    }

    private void showProgress() {
        Intent intent = new Intent();
        intent.setClass(WXdroid.this, showProgressFTP.class);
        startActivity(intent);
    }

    private OnClickListener onSDCardClick = new OnClickListener() {

        public void onClick(View v) {
            img_loader.viewFile(2);
        }

        public void viewFile(int file_nx) {
            FTPClient ftp = new FTPClient();
            boolean error = false;
            try {
                int reply;
                ftp.connect("tgftp.nws.noaa.gov");
                ftp.login("anonymous", "");
                Log.d("WXDroid", "Connected to tgftp.nws.noaa.gov.");
                Log.d("WXDroid", ftp.getReplyString());
                reply = ftp.getReplyCode();
                if (!FTPReply.isPositiveCompletion(reply)) {
                    ftp.disconnect();
                    System.err.println("FTP server refused connection.");
                    System.exit(1);
                }
                ftp.changeWorkingDirectory("fax");
                Log.d("WXDroid", "working directory: " + ftp.printWorkingDirectory());
                ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
                InputStream img_file = ftp.retrieveFileStream("PYAA10.gif");
                Intent intent = new Intent();
                intent.setClass(WXdroid.this, showProgressFTP.class);
                startActivity(intent);
                String storage_state = Environment.getExternalStorageState();
                if (storage_state.contains("mounted")) {
                    String filepath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/NOAAWX/";
                    File imageDirectory = new File(filepath);
                    File local_file = new File(filepath + "PYAA10.gif");
                    OutputStream out = new FileOutputStream(local_file);
                    byte[] buffer = new byte[1024];
                    int count;
                    while ((count = img_file.read(buffer)) != -1) {
                        if (Thread.interrupted() == true) {
                            String functionName = Thread.currentThread().getStackTrace()[2].getMethodName() + "()";
                            throw new InterruptedException("The function " + functionName + " was interrupted.");
                        }
                        out.write(buffer, 0, count);
                    }
                    showImage();
                    out.flush();
                    out.close();
                    img_file.close();
                    Log.d("WXDroid", "file saved: " + filepath + " " + local_file);
                } else {
                    Log.d("WXDroid", "The SD card is not mounted");
                }
                ftp.logout();
                ftp.disconnect();
            } catch (IOException e) {
                error = true;
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                if (ftp.isConnected()) {
                    try {
                        ftp.disconnect();
                    } catch (IOException ioe) {
                    }
                }
            }
        }

        ;
    };
}
