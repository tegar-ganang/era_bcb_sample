package de.fhkl.mHelloWorld.implementation;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.LogRecord;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Application Name: Generic Login Screen for the Android Platform (back end)
 * Description: This is a generic login screen which catches the username and
 * password values Created on: November 23, 2007 Created by: Pogz Ortile
 * Contact: pogz(at)redhat(dot)polarhome(dot)com Notes: The string values for
 * username and password are assigned to sUserName and sPassword respectively
 * You arve free to distribute, modify, and wreck for all I care. GPL ya!
 * */
public class HelloWorldLoginWithWriter_backup extends Activity implements Button.OnClickListener {

    private String dataDir;

    private Button mAcceptButton;

    private Button mCancelButton;

    private TextView mUsername;

    private TextView mPassword;

    private TextView mError;

    private static final String I = "======================= [HELLO-WORLD] " + "ProfileManager" + ": ";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.login);
        init();
        String str = "gibt es eine Möglichkeit aus einem String einen InputStream zu erzeugen.";
        InputStream in = new ByteArrayInputStream(str.getBytes());
        try {
            writeFile(in, "imKlartext.xml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void init() {
        mCancelButton = (Button) findViewById(R.id.login_button_cancel);
        mCancelButton.setOnClickListener(this);
        mAcceptButton = (Button) findViewById(R.id.login_button_login);
        mAcceptButton.setOnClickListener(this);
        mUsername = (EditText) findViewById(R.id.login_username_input);
        mPassword = (EditText) findViewById(R.id.login_password_input);
    }

    protected InputStream getProfileFromUrl(String url) {
        Log.i(I, "try to download url: " + url);
        HttpClient client = new DefaultHttpClient();
        HttpGet getMethod = new HttpGet(url);
        Log.i(I, "start download");
        HttpResponse response;
        InputStream in = null;
        try {
            Log.i(I, "try to execute ...");
            response = client.execute(getMethod);
            Log.i(I, "download executed. Getting the Response ...");
            in = response.getEntity().getContent();
            Log.i(I, "opened");
        } catch (Exception e) {
            Log.i(I, "Exception bei execute oder getContent");
        }
        return in;
    }

    private void writeFile(InputStream is, String filename) throws IOException {
        dataDir = getFileStreamPath("").getAbsolutePath() + "/../neu-erstellter-ordner/";
        byte[] buffer = new byte[1024];
        int read;
        File file;
        String directory;
        filename = dataDir + filename;
        Log.i(I, "filename: " + filename);
        if (filename.lastIndexOf('/') != filename.length() - 1) {
            directory = filename.substring(0, filename.lastIndexOf('/'));
            file = new File(directory);
            if (!file.exists()) file.mkdirs();
        } else {
            return;
        }
        OutputStream os = null;
        os = new FileOutputStream(filename);
        while ((read = is.read(buffer)) > 0) {
            Log.i(I, "try to write file ...");
            os.write(buffer, 0, read);
        }
        os.close();
        is.close();
    }

    public void onClick(View v) {
        Intent intent;
        switch(v.getId()) {
            case R.id.login_button_login:
                Log.v(I, "test");
                int i = v.getId();
                String s = String.valueOf(i);
                Bundle bundle = new Bundle();
                bundle.putString("username", mUsername.getText().toString());
                bundle.putString("password", mPassword.getText().toString());
                bundle.putBoolean("isProfile", true);
                bundle.putBoolean("filled", false);
                intent = new Intent(this, ProfileManager.class);
                intent.putExtras(bundle);
                startActivityForResult(intent, 1);
                break;
            case R.id.login_button_cancel:
                intent = new Intent(this, ContactDownloadForm.class);
                startActivity(intent);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        Bundle bundle = intent.getExtras();
        TextView error_field = (TextView) findViewById(R.id.error_field);
        error_field.setText(bundle.getString("error_value"));
    }
}
