package com.android.crepe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.Properties;
import android.util.Log;

public class CrepeCertificateManager {

    private static CrepeCertificateManager mSelf = null;

    private static final String PROPERTIES_FILE = "/data/crepe/certificates/cert_cache.properties";

    private static final String PATH_PREFIX = "/data/crepe/certificates/";

    private static final String EXT = ".crt";

    private static final String TAG = "CrepeCertificateManager";

    private Properties certProps;

    public static CrepeCertificateManager getInstance() {
        if (mSelf == null) mSelf = new CrepeCertificateManager();
        return mSelf;
    }

    public CrepeCertificateManager() {
        certProps = new Properties();
        File temp = new File(PROPERTIES_FILE);
        if (temp.exists()) {
            try {
                FileInputStream propsFile = new FileInputStream(PROPERTIES_FILE);
                certProps.load(propsFile);
                propsFile.close();
            } catch (FileNotFoundException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            } catch (IOException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
    }

    public void close() {
        try {
            FileOutputStream propsFile = new FileOutputStream(PROPERTIES_FILE);
            certProps.save(propsFile, "saving cert props file");
            propsFile.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public boolean checkCache(String certName) {
        if (certProps.containsKey(certName)) return true; else return false;
    }

    public void cacheCertificate(String certName, String data) {
        certProps.put(certName, PATH_PREFIX + certName + EXT);
        try {
            FileOutputStream fout = new FileOutputStream(PATH_PREFIX + certName + EXT);
            fout.write(data.getBytes());
            fout.flush();
            fout.close();
            Log.i(TAG, "Certificate " + certName + " has been cached!");
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public String getCachedCertificate(String certName) {
        ByteArrayOutputStream cert = new ByteArrayOutputStream();
        try {
            FileInputStream fin = new FileInputStream(PATH_PREFIX + certName + EXT);
            byte[] buff = new byte[2048];
            int readLen = 0;
            while ((readLen = fin.read(buff)) != -1) {
                cert.write(buff, 0, readLen);
            }
            fin.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return cert.toString();
    }
}
