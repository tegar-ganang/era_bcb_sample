package org.photolister.list.copystrategy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import org.photolister.ui.Messages;
import org.photolister.ui.UserLogger;

/**
 * Default base implementation for <code>CopyStrategie</code>s.
 */
public abstract class AbstractCopyStrategy implements CopyStrategy {

    protected AbstractCopyStrategy() {
    }

    protected void copyFile(final File in, final File out) throws IOException {
        final FileChannel inChannel = new FileInputStream(in).getChannel();
        final FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (final IOException e) {
            throw e;
        } finally {
            if (inChannel != null) {
                inChannel.close();
            }
            if (outChannel != null) {
                outChannel.close();
            }
        }
    }

    public void copy(final File sourceFile, final File destinationDirectory, final UserLogger userLogger) throws CopyException {
        try {
            if (destinationDirectory.exists()) {
                if (destinationDirectory.isFile()) {
                    throw new IllegalArgumentException("destinationDirectory should be a directory.");
                }
            } else {
                destinationDirectory.mkdirs();
                userLogger.log(Messages.getString("AbstractCopyStrategy.createdstart") + destinationDirectory.getCanonicalPath() + Messages.getString("AbstractCopyStrategy.createdend"));
            }
            copyInternal(sourceFile, destinationDirectory, userLogger);
        } catch (final IOException e) {
            throw new CopyException("Unable to copy file.", e);
        }
    }

    protected abstract void copyInternal(File sourceFile, File destinationDirectory, final UserLogger userLogger) throws IOException, CopyException;
}
