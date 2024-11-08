package com.piwi.stickeroid;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class Collection {

    public static final byte FILE_VERSION = 100;

    public static final int NO_FILTER = 0;

    public static final int DISPLAY_MISSING = 1;

    public static final int DISPLAY_DUPLICATED = 2;

    private static final String EXTENSION = ".col";

    private String mName;

    private byte[] mBytes;

    public static String getFileName(String name) {
        return name + EXTENSION;
    }

    public static String getCollectionName(String name) {
        int pos = name.indexOf(EXTENSION);
        if (pos > 0) {
            return name.substring(0, pos);
        }
        return name;
    }

    public static boolean match(String name) {
        return name.toLowerCase().endsWith(EXTENSION);
    }

    public Collection(String name) {
        mName = name;
    }

    public Collection(String name, int nbElements) {
        mName = name;
        mBytes = new byte[nbElements];
    }

    public Collection(String name, int nbElements, Collection col) {
        mName = name;
        mBytes = new byte[nbElements];
        byte[] src = col.getData();
        int nb = src.length;
        if (nb > nbElements) {
            nb = nbElements;
        }
        for (int i = 0; i < nb; i++) {
            mBytes[i] = src[i];
        }
    }

    public String getName() {
        return mName;
    }

    public byte[] getData() {
        return mBytes;
    }

    public boolean load(FileInputStream fis) {
        try {
            int version = fis.read();
            if (version != FILE_VERSION) {
                return false;
            }
            fis.read();
            FileChannel fc = fis.getChannel();
            int sz = (int) (fc.size() - fc.position());
            mBytes = new byte[sz];
            fis.read(mBytes);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public boolean save(FileOutputStream fos) {
        try {
            fos.write(FILE_VERSION);
            fos.write(0);
            fos.write(mBytes);
            fos.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public String getFileName() {
        return mName + EXTENSION;
    }
}
