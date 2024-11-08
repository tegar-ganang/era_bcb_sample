package org.unintelligible.antjnlpwar.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import org.apache.tools.ant.BuildException;

/**
 * @author ngc
 */
public class StreamUtil {

    /**
	 * Copies a source file. Preserves its date stamp across the copy.
	 * @param srcFile
	 * @param destFolder
	 * @throws BuildException a wrapped IOException
	 */
    public static void copyFile(File srcFile, File destFolder) {
        try {
            File destFile = new File(destFolder, srcFile.getName());
            if (destFile.exists()) {
                throw new BuildException("Could not copy " + srcFile + " to " + destFolder + " as " + destFile + " already exists");
            }
            FileChannel srcChannel = null;
            FileChannel destChannel = null;
            try {
                srcChannel = new FileInputStream(srcFile).getChannel();
                destChannel = new FileOutputStream(destFile).getChannel();
                destChannel.transferFrom(srcChannel, 0, srcChannel.size());
            } finally {
                if (srcChannel != null) {
                    srcChannel.close();
                }
                if (destChannel != null) {
                    destChannel.close();
                }
            }
            destFile.setLastModified((srcFile.lastModified()));
        } catch (IOException e) {
            throw new BuildException("Could not copy " + srcFile + " to " + destFolder + ": " + e, e);
        }
    }

    /**
	 * @param filename
	 * @param iStream
	 * @param destFolder
	 * @throws BuildException a wrapped IOException
	 */
    public static void copyFile(String filename, InputStream iStream, File destFolder) {
        File destFile = new File(destFolder, filename);
        if (destFile.exists()) {
            throw new BuildException("Could not copy the stream for " + filename + " to " + destFolder + " as " + destFile + " already exists");
        }
        WritableByteChannel channel = null;
        try {
            channel = new FileOutputStream(destFile).getChannel();
            ByteBuffer buf = ByteBuffer.allocateDirect(10);
            byte[] bytes = new byte[1024];
            int count = 0;
            int index = 0;
            while (count >= 0) {
                if (index == count) {
                    count = iStream.read(bytes);
                    index = 0;
                }
                while (index < count && buf.hasRemaining()) {
                    buf.put(bytes[index++]);
                }
                buf.flip();
                int numWritten = channel.write(buf);
                if (buf.hasRemaining()) {
                    buf.compact();
                } else {
                    buf.clear();
                }
            }
            channel.close();
        } catch (IOException ioe) {
            throw new BuildException("Could not copy " + filename + " to " + destFolder + ": " + ioe, ioe);
        } finally {
            if (iStream != null) {
                try {
                    iStream.close();
                } catch (IOException e) {
                }
            }
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
