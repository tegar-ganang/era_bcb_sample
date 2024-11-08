package nfc.doorlock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.Iterator;
import java.security.Provider;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.widget.TextView;
import android.util.Log;
import android.os.Bundle;

public class NFCDoorKey extends Activity {

    private NfcAdapter mAdapter;

    private PendingIntent mPendingIntent;

    private IntentFilter[] mFilters;

    private String[][] mTechLists;

    private TextView mText;

    private int mCount = 0;

    public static int MAX_FRAME = 250;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mText = (TextView) findViewById(R.id.text);
        mText.setText("Scan a tag");
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndef.addDataType("*/*");
        } catch (MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }
        mFilters = new IntentFilter[] { ndef };
        mTechLists = new String[][] { new String[] { NfcA.class.getName() } };
        Log.d("NFCDoorKey", "End of setup");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("NFCDoorKey", "Foreground NFC dispatch enabled");
        mAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
    }

    @Override
    public void onNewIntent(Intent intent) {
        Log.i("NFCDoorKey", "Discovered tag with intent: " + intent);
        Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        for (int i = 0; i < tagFromIntent.getTechList().length; i++) {
            Log.i("NFCDoorKey", tagFromIntent.getTechList()[i]);
        }
        IsoDep nfc = IsoDep.get(tagFromIntent);
        Log.i("NFCDoorKey", "Tag from intent!!");
        byte[] data = { (byte) 0xAF };
        byte[] returndata;
        try {
            nfc.connect();
            if (nfc.isConnected()) {
                Log.i("NFCDoorKey", "Begin transcevie");
                returndata = nfc.transceive(data);
                if (returndata[0] == (byte) 0x90 && returndata[1] == (byte) 0x00) {
                    byte[] challenge = new byte[returndata.length - 2];
                    System.arraycopy(returndata, 2, challenge, 0, challenge.length);
                    Log.i("NFCDoorKey", "Door Found, ID: ");
                    Log.i("NFCDoorKey", "Challenge received: " + challenge.length);
                    Log.i("NFCDoorKey", "Load Key");
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    FileInputStream fis = openFileInput("door.key");
                    int size = (int) fis.getChannel().size();
                    byte[] key = new byte[size];
                    fis.read(key);
                    String keystr = new String(key);
                    keystr = keystr.replaceAll("-----BEGIN PRIVATE KEY-----\n", "");
                    keystr = keystr.replaceAll("-----END PRIVATE KEY-----\n", "");
                    byte[] keyBytes = android.util.Base64.decode(keystr, android.util.Base64.DEFAULT);
                    fis.close();
                    Log.i("NFCDoorKey", "Load Cert");
                    fis = openFileInput("door.crt");
                    Security.addProvider(new org.bouncycastle2.jce.provider.BouncyCastleProvider());
                    CertificateFactory cf = CertificateFactory.getInstance("X.509", "BC2");
                    X509Certificate cert = (X509Certificate) cf.generateCertificate((InputStream) fis);
                    fis.close();
                    byte[] certificate = cert.getEncoded();
                    output.write((byte) 0xAE);
                    byte blocks = (byte) (certificate.length / 250);
                    byte overflow = (byte) (certificate.length % 250);
                    output.write(blocks);
                    output.write((byte) MAX_FRAME);
                    output.write(overflow);
                    Log.i("NFCDoorKey", "Send Cert info, Length: " + certificate.length + ", Blocks: " + blocks + ", Blocksize: 250, Overflow: " + overflow);
                    returndata = nfc.transceive(output.toByteArray());
                    Log.i("NFCDoorKey", "Send Cert");
                    for (int i = 0; i < blocks + 1; i++) {
                        byte[] block = new byte[Math.min(MAX_FRAME, certificate.length - (i * MAX_FRAME))];
                        if (block.length > 0) {
                            System.arraycopy(certificate, i * MAX_FRAME, block, 0, block.length);
                            Log.i("NFCDoorKey", "Send n bytes: " + block.length);
                            nfc.transceive(block);
                        }
                    }
                    Log.i("NFCDoorKey", "Load Algorithms");
                    Log.i("NFCDoorKey", cert.getSigAlgName());
                    KeyFactory keyFactory = KeyFactory.getInstance(cert.getPublicKey().getAlgorithm(), "BC2");
                    PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(keyBytes);
                    PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
                    Signature s = Signature.getInstance(cert.getSigAlgName(), "BC2");
                    s.initSign(privateKey);
                    s.update(challenge);
                    byte[] sign = s.sign();
                    Log.i("NFCDoorKey", "Signature");
                    output = new ByteArrayOutputStream();
                    Log.i("NFCDoorKey", "Sig length " + sign.length);
                    output.write(sign);
                    returndata = nfc.transceive(output.toByteArray());
                    Log.i("NFCDoorKey", "Complete " + returndata[0]);
                } else {
                    throw new IOException("not a door");
                }
            }
        } catch (Exception e) {
            Log.i("NFCDoorKey", e.getMessage());
            if (nfc.isConnected()) {
                try {
                    nfc.close();
                } catch (IOException er) {
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mAdapter.disableForegroundDispatch(this);
    }
}
