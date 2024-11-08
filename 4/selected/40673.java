package org.parosproxy.paros.model;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class FileCopier {

    /**
     * 
     */
    public FileCopier() {
        super();
    }

    public void copy(File in, File out) throws IOException {
        try {
            copyNIO(in, out);
        } catch (IOException e) {
            copyLegacy(in, out);
        }
    }

    public void copyLegacy(File in, File out) throws IOException {
        FileInputStream inStream = null;
        FileOutputStream outStream = null;
        BufferedInputStream inBuf = null;
        BufferedOutputStream outBuf = null;
        try {
            inStream = new FileInputStream(in);
            outStream = new FileOutputStream(out);
            inBuf = new BufferedInputStream(inStream);
            outBuf = new BufferedOutputStream(outStream);
            byte[] buf = new byte[10240];
            int len = 1;
            while (len > 0) {
                len = inBuf.read(buf);
                if (len > 0) {
                    outBuf.write(buf, 0, len);
                }
            }
        } finally {
            if (inBuf != null) inBuf.close();
            if (outBuf != null) outBuf.close();
            if (inStream != null) inStream.close();
            if (outStream != null) outStream.close();
        }
    }

    public void copyNIO(File in, File out) throws IOException {
        FileInputStream inStream = null;
        FileOutputStream outStream = null;
        FileChannel sourceChannel = null;
        FileChannel destinationChannel = null;
        try {
            inStream = new FileInputStream(in);
            outStream = new FileOutputStream(out);
            sourceChannel = inStream.getChannel();
            destinationChannel = outStream.getChannel();
            destinationChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        } finally {
            if (sourceChannel != null) sourceChannel.close();
            if (destinationChannel != null) destinationChannel.close();
            if (inStream != null) inStream.close();
            if (outStream != null) outStream.close();
        }
    }
}
