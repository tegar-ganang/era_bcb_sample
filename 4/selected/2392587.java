package net.sourceforge.purrpackage.recording;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Java NIO copy using a {@link Closer}.
 */
public class Copier {

    public void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }
        FileChannel source = null;
        FileChannel destination = null;
        Closer c = new Closer();
        try {
            source = c.register(new FileInputStream(sourceFile).getChannel());
            destination = c.register(new FileOutputStream(destFile).getChannel());
            destination.transferFrom(source, 0, source.size());
        } catch (IOException e) {
            c.doNotThrow();
            throw e;
        } finally {
            c.closeAll();
        }
    }
}
