package com.common;

import android.content.Context;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileManager {

    public static boolean saveAs(Context context, int ressound, String dir, String filename) {
        byte[] buffer = null;
        int size = 0;
        boolean exists = (new File(dir)).exists();
        if (!exists) {
            new File(dir).mkdirs();
        }
        exists = (new File(dir + "/" + filename)).exists();
        if (exists) {
            return true;
        }
        InputStream fIn = null;
        FileOutputStream save = null;
        try {
            fIn = context.getResources().openRawResource(ressound);
            save = new FileOutputStream(dir + "/" + filename);
            int length = fIn.available();
            if (length > 0) {
                byte[] buff = new byte[length];
                int read = fIn.read(buff);
                for (int i = 0; i < read; i += 1000) {
                    int count = ((i + 1000) < read) ? 1000 : read - i;
                    save.write(buff, i, count);
                }
            }
            fIn.close();
            save.flush();
            save.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static boolean saveZipAs(Context context, int ressound, String dir, String filename) {
        int len = 1024;
        byte[] buffer = new byte[1024];
        int size = 0;
        boolean exists = (new File(dir)).exists();
        if (!exists) {
            new File(dir).mkdirs();
        }
        exists = (new File(dir + "/" + filename)).exists();
        if (exists) {
            return true;
        }
        InputStream inputStream = null;
        FileOutputStream output = null;
        try {
            int StreamLen, readCount, readSum;
            inputStream = context.getResources().openRawResource(ressound);
            output = new FileOutputStream(dir + "/" + filename);
            ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(inputStream));
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            BufferedInputStream b = new BufferedInputStream(zipInputStream);
            StreamLen = (int) zipEntry.getSize();
            while ((readCount = b.read(buffer)) != -1) {
                output.write(buffer, 0, readCount);
            }
            inputStream.close();
            output.flush();
            output.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }
}
