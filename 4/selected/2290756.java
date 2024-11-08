package com.phbc.lyricsMystro.dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 *
 * @author jkruger
 */
public abstract class FileDao {

    protected File baseDirectory;

    protected String generateFilename(Object obj, String fileExt) {
        return generateFilename(obj, "", fileExt);
    }

    protected String generateFilename(Object obj, String tag, String fileExt) {
        int hash = obj.hashCode();
        char negativeBit = '0';
        if (hash < 0) {
            hash = Math.abs(hash);
            negativeBit = '1';
        }
        return negativeBit + Integer.toString(hash) + tag + "." + fileExt;
    }

    protected void verifyDirectory() {
        if (baseDirectory.exists() && !baseDirectory.isDirectory()) {
            System.err.println(baseDirectory.getName() + " Storage directory is a file");
            System.exit(2);
        }
        if (!baseDirectory.exists()) {
            baseDirectory.mkdirs();
        }
    }

    protected boolean copyFile(File sourceFile, File destinationFile) {
        try {
            FileChannel srcChannel = new FileInputStream(sourceFile).getChannel();
            FileChannel dstChannel = new FileOutputStream(destinationFile).getChannel();
            dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
            srcChannel.close();
            dstChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
