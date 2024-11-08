package pckt.Test;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class pcktTest extends Activity {

    public static String MODO = "Espera";

    public static long TEMPO_ESPERA = 200;

    private final String TAG = "pcktTest";

    private static final boolean D = true;

    private final String UUID_STRING = "00001101-0000-1000-8000-00805F9B34FB";

    public static final int MESSAGE_READ = 0;

    public static final int MESSAGE_WRITE = 1;

    private BluetoothAdapter myBt;

    private Set<BluetoothDevice> unbondedDevices = null;

    private BluetoothSocket sckt;

    ConnectedThreadAndroid thread;

    private static TextView statusView, sentView, receivedView;

    private RadioGroup radioGroupModo, radioGroupDados;

    private Button btEnviar;

    private RadioButton radioBt;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        unbondedDevices = new HashSet<BluetoothDevice>();
        statusView = (TextView) findViewById(R.id.statusView);
        sentView = (TextView) findViewById(R.id.sentView);
        receivedView = (TextView) findViewById(R.id.receivedView);
        radioGroupModo = (RadioGroup) findViewById(R.id.radioModo);
        radioGroupDados = (RadioGroup) findViewById(R.id.radioDados);
        btEnviar = (Button) findViewById(R.id.btEnviar);
        radioBt = (RadioButton) findViewById(R.id.bt4megas);
        myBt = BTInicialization();
        if (myBt == null) {
            Toast.makeText(this, "Couldn't intialize BT in this device", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        btEnviar.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                switch(radioGroupModo.getCheckedRadioButtonId()) {
                    case R.id.btDireto:
                        MODO = "Direto";
                        break;
                    case R.id.btEspera:
                        MODO = "Espera";
                        break;
                }
                switch(radioGroupDados.getCheckedRadioButtonId()) {
                    case R.id.bt200k:
                        sendFile("200k.txt");
                        break;
                    case R.id.bt500k:
                        sendFile("500k.txt");
                        break;
                    case R.id.bt2megas:
                        sendFile("2megas.txt");
                        break;
                    case R.id.bt4megas:
                        sendFile("4megas.txt");
                        break;
                    case R.id.bt8megas:
                        sendFile("8megas.txt");
                        break;
                }
            }
        });
        try {
            unbondedDevices.add(myBt.getRemoteDevice("00:1F:81:00:08:30"));
        } catch (Exception e) {
            Log.i(TAG, "Erro no unbounded", e);
        }
        try {
            conecta(unbondedDevices, UUID.fromString(UUID_STRING));
        } catch (IOException e) {
            Log.i(TAG, "Fudeu na hr de fazer minha treta de conectar direto");
        }
        thread = new ConnectedThreadAndroid(sckt, mHandler);
        thread.start();
    }

    private boolean sendFile(String name) {
        StorageHandler sh = new StorageHandler(name, this.getApplicationContext());
        Date before = new Date();
        if (!sh.openRead()) return false;
        String str = sh.read();
        while (str != null) {
            thread.write(str);
            if (MODO.equalsIgnoreCase("Espera")) {
                try {
                    Thread.sleep(TEMPO_ESPERA);
                } catch (Exception e) {
                    Log.i(TAG, "Caiu na exce��o.", e);
                }
            }
            str = sh.read();
        }
        sh.close();
        Date after = new Date();
        long deltaTime = after.getTime() - before.getTime();
        Log.i(TAG, "Enviou " + thread.getBytesSent() + " bytes em " + deltaTime + " milissegundos.");
        sentView.setText("Enviou " + thread.getBytesSent() + " bytes em " + deltaTime + " milissegundos.");
        thread.close();
        this.close();
        return true;
    }

    /**
     * Initialize the bluetooth stack in the device
     * @return The BluetoothAdapter
     */
    private BluetoothAdapter BTInicialization() {
        if (D) Log.i(TAG, "Initializing Bluetooth Adapter");
        BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
        if (bt == null) {
            if (D) Log.i(TAG, "Bluetooth is not availiable in this device");
        } else if (!bt.isEnabled()) {
            if (D) Log.i(TAG, "Starting Intent to activate BT in the device");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
        return bt;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Now BT is enabled", Toast.LENGTH_LONG).show();
                return;
            }
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "BT inicialization canceled by the user", Toast.LENGTH_LONG).show();
                return;
            }
        }
    }

    private Boolean BTScan() {
        if (D) Log.i(TAG, "Starting Bluetooth Scan");
        statusView.setText("Scanning...");
        IntentFilter filterFound = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filterFound);
        IntentFilter filterFinished = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filterFinished);
        if (myBt.getState() != BluetoothAdapter.STATE_ON) {
            if (D) Log.i(TAG, "BT is not turned on.");
            return false;
        }
        return myBt.startDiscovery();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice dev = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (D) Log.i(TAG, "Device found: " + dev.getName());
                statusView.setText("Device found: " + dev.getName());
                if (dev.getBondState() != BluetoothDevice.BOND_BONDED) {
                    if (D) Log.i(TAG, "Device " + dev.getName() + "'s not bonded.");
                    unbondedDevices.add(dev);
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (D) Log.i(TAG, "Bluetooth Scan finished");
                statusView.setText("Scan finished.");
                myBt.cancelDiscovery();
                try {
                    tryToConnect();
                } catch (Exception e) {
                }
            }
        }
    };

    /**
	 * Try to connect with de devices in the environment
	 * @throws IOException
	 */
    private void tryToConnect() throws IOException {
        this.unregisterReceiver(mReceiver);
        UUID uuid = UUID.fromString(UUID_STRING);
        Set<BluetoothDevice> bondedDevices = new HashSet<BluetoothDevice>();
        bondedDevices = myBt.getBondedDevices();
        if (D) Log.i(TAG, "Bonded: " + bondedDevices.size() + ". Unbonded: " + unbondedDevices.size());
        sckt = conecta(bondedDevices, uuid);
        if (sckt == null) {
            sckt = conecta(unbondedDevices, uuid);
            if (sckt == null) {
                makeToast("Couldn't connect to any device");
                if (D) Log.i(TAG, "Couldn't connect to any device");
                finish();
                return;
            }
        }
        if (D) Log.i(TAG, "Creating thread to manage the connection");
        thread = new ConnectedThreadAndroid(sckt, mHandler);
        thread.start();
    }

    /**
	 * Run a Set<BluetoothDevice> trying to connect to a BluetoothDevice in the Set
	 * @param s The Set<BluetoothDevice>
	 * @param u The UUID of the service to connect
	 * @return A socket connecting the devices
	 * @throws IOException
	 */
    private BluetoothSocket conecta(Set<BluetoothDevice> s, UUID u) throws IOException {
        BluetoothDevice dev;
        for (Iterator<BluetoothDevice> it = s.iterator(); it.hasNext(); ) {
            dev = it.next();
            if (D) Log.i(TAG, "Connecting to " + dev.getName() + "...");
            statusView.setText("Connecting to: " + dev.getName() + "...");
            sckt = dev.createRfcommSocketToServiceRecord(u);
            try {
                sckt.connect();
            } catch (Exception e) {
                if (D) Log.i(TAG, "Exception: couldn't connect to " + dev.getName() + ". Causa: " + e.getCause().toString());
                if (D) Log.e(TAG, "Exception: couldn't connect to " + dev.getName(), e);
            }
            if (D) Log.i(TAG, "Connected to " + dev.getName());
            statusView.setText("Connected to " + dev.getName());
            return sckt;
        }
        return null;
    }

    private void makeToast(String str) {
        Toast.makeText(this, str, Toast.LENGTH_LONG).show();
    }

    private final Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    break;
                case MESSAGE_WRITE:
                    break;
            }
        }
    };

    private void close() {
        if (thread != null) thread.close();
        finish();
    }

    @Override
    public void onBackPressed() {
        close();
    }
}
