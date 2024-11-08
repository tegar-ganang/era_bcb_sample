package net.sf.kengoo.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Created by IntelliJ IDEA.
 * User: maestro
 * Date: 21.10.2007
 * Time: 23:25:36
 * To change this template use File | Settings | File Templates.
 */
public abstract class FileSystemUtils {

    /**
	 * Max block size is 1Gb.
	 */
    private static final long MAX_BLOCK_SIZE = 1073741824L;

    public static boolean checkFile(File file, boolean isDirectory, boolean checkExists) {
        boolean fileAccepted;
        if (checkExists) {
            fileAccepted = file.exists();
            if (fileAccepted) {
                if (isDirectory) {
                    fileAccepted = file.isDirectory();
                } else {
                    fileAccepted = !file.isDirectory();
                }
            }
        } else {
            if (file.exists()) {
                if (isDirectory) {
                    fileAccepted = file.isDirectory();
                } else {
                    fileAccepted = !file.isDirectory();
                }
            } else {
                fileAccepted = true;
            }
        }
        return fileAccepted;
    }

    public static boolean copyFile(File sourceFile, File destFile) {
        FileChannel srcChannel = null;
        FileChannel dstChannel = null;
        try {
            srcChannel = new FileInputStream(sourceFile).getChannel();
            dstChannel = new FileOutputStream(destFile).getChannel();
            long pos = 0;
            long count = srcChannel.size();
            if (count > MAX_BLOCK_SIZE) {
                count = MAX_BLOCK_SIZE;
            }
            long transferred = Long.MAX_VALUE;
            while (transferred > 0) {
                transferred = dstChannel.transferFrom(srcChannel, pos, count);
                pos = transferred;
            }
        } catch (IOException e) {
            return false;
        } finally {
            if (srcChannel != null) {
                try {
                    srcChannel.close();
                } catch (IOException e) {
                }
            }
            if (dstChannel != null) {
                try {
                    dstChannel.close();
                } catch (IOException e) {
                }
            }
        }
        return true;
    }
}
