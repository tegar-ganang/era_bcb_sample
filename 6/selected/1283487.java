package android.client;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

/**
 * Classe che implementa la schermata e le operazioni di login. Si tratta della classe principale che viene fatta partire quando si clicca sull'icona
 * @author  Nicolas Tagliani
 * @author  Vincenzo Frascino
 */
public class MyContatsClient extends Activity implements OnClickListener, ServiceConnection {

    /**
	 * Intent a cui e' sensibile questa classe
	 */
    public static final String LOGIN_ACTION = "android.client.action.LOGIN";

    /**
	 * @uml.property  name="s"
	 * @uml.associationEnd  
	 */
    private ServiceInterface s;

    private Button login;

    private Button register;

    private CheckBox forcelogin;

    private EditText username;

    private EditText password;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        startService(new Intent("android.client.MY_SERVICE"));
        bindService(new Intent("android.client.MY_SERVICE"), this, 0);
    }

    @Override
    public void onClick(View arg0) {
        if (arg0 == login) {
            bindService(new Intent("android.client.MY_SERVICE"), this, 0);
            final String u = username.getText().toString();
            final String p = password.getText().toString();
            if (u != "" && p != "") {
                try {
                    this.s.connect(Settings.SERVER_ADDR);
                    boolean ret;
                    if (forcelogin.isChecked()) {
                        ret = this.s.forcelogin(u, p);
                    } else ret = this.s.login(u, p);
                    if (ret) {
                        startActivity(new Intent(android.client.FriendsList.PENDING_ACTION, getIntent().getData()));
                        finish();
                    } else {
                        AlertD.show(this, "Error", 0, "Error occurred while logging in. Check your data or try later", "BACK", false);
                    }
                } catch (Exception e) {
                    AlertD.show(this, "Error", 0, "Error occurred while connecting to server.", "BACK", false);
                }
            } else {
                AlertD.show(this, "Errore", 0, "Please fill in the username and the password field", "BACK", false);
            }
        }
        if (arg0 == register) {
            startActivity(new Intent(android.client.RegisterActivity.REGISTER_ACTION, getIntent().getData()));
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        s = ServiceInterface.Stub.asInterface(service);
        try {
            if (s.isRunning()) {
                startActivity(new Intent(MainLoopActivity.MAIN_LOOP_ACTION, getIntent().getData()));
                finish();
            } else {
                setContentView(R.layout.login);
                username = (EditText) findViewById(R.id.Username);
                password = (EditText) findViewById(R.id.Password);
                login = (Button) findViewById(R.id.Login);
                register = (Button) findViewById(R.id.Register);
                forcelogin = (CheckBox) findViewById(R.id.forcelogincheckbox);
                login.setOnClickListener(this);
                register.setOnClickListener(this);
            }
        } catch (RemoteException e) {
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
    }
}
