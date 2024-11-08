package com.android.crepe.qr;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import android.util.Base64;
import android.util.Log;

public class Zipper {

    private static final String TAG = "Zipper";

    public static byte[] decoded;

    public static byte[] decodeAndUnzip(String data) {
        byte[] decoded = Base64.decode(data, Base64.DEFAULT);
        ByteArrayOutputStream unzipped = new ByteArrayOutputStream();
        byte[] buffer = new byte[512];
        int count;
        try {
            GZIPInputStream zis = new GZIPInputStream(new ByteArrayInputStream(decoded));
            while ((count = zis.read(buffer)) != -1) unzipped.write(buffer, 0, count);
            unzipped.flush();
            zis.close();
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return unzipped.toByteArray();
    }
}
