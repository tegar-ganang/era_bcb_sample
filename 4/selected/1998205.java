package org.nigelk.pdfupdateframe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Utilities {

    private static Log log = LogFactory.getLog(Utilities.class);

    public static boolean copyToFile(InputStream from, File to) {
        try {
            FileChannel toChannel = new FileOutputStream(to).getChannel();
            byte[] buf = new byte[1024 * 8];
            int howMany = 0;
            while ((howMany = from.read(buf, 0, buf.length)) >= 0) {
                toChannel.write(ByteBuffer.wrap(buf, 0, howMany));
            }
            toChannel.close();
        } catch (IOException e) {
            log.error("failed to copy input stream to " + to.getAbsolutePath() + ": caught exception", e);
            return false;
        }
        return true;
    }

    public static boolean copyFile(File from, File to) {
        try {
            FileChannel fromChannel = new FileInputStream(from).getChannel();
            FileChannel toChannel = new FileOutputStream(to).getChannel();
            toChannel.transferFrom(fromChannel, 0, fromChannel.size());
            fromChannel.close();
            toChannel.close();
        } catch (IOException e) {
            log.error("failed to copy " + from.getAbsolutePath() + " to " + to.getAbsolutePath() + ": caught exception", e);
            return false;
        }
        return true;
    }
}
