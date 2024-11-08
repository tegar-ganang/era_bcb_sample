package org.eclipse.core.runtime.internal.adaptor;

import java.io.*;
import java.nio.channels.FileLock;
import org.eclipse.core.runtime.adaptor.EclipseAdaptor;
import org.eclipse.core.runtime.adaptor.EclipseAdaptorMsg;
import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.util.NLS;

/**
 * Internal class.
 */
public class Locker_JavaNio implements Locker {

    private File lockFile;

    private FileLock fileLock;

    private FileOutputStream fileStream;

    public Locker_JavaNio(File lockFile) {
        this.lockFile = lockFile;
    }

    public synchronized boolean lock() throws IOException {
        fileStream = new FileOutputStream(lockFile, true);
        try {
            fileLock = fileStream.getChannel().tryLock();
        } catch (IOException ioe) {
            if (BasicLocation.DEBUG) {
                String basicMessage = NLS.bind(EclipseAdaptorMsg.location_cannotLock, lockFile);
                FrameworkLogEntry basicEntry = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, basicMessage, 0, ioe, null);
                EclipseAdaptor.getDefault().getFrameworkLog().log(basicEntry);
            }
            String specificMessage = NLS.bind(EclipseAdaptorMsg.location_cannotLockNIO, new Object[] { lockFile, ioe.getMessage(), "\"-D" + BasicLocation.PROP_OSGI_LOCKING + "=none\"" });
            throw new IOException(specificMessage);
        }
        if (fileLock != null) return true;
        fileStream.close();
        fileStream = null;
        return false;
    }

    public synchronized void release() {
        if (fileLock != null) {
            try {
                fileLock.release();
            } catch (IOException e) {
            }
            fileLock = null;
        }
        if (fileStream != null) {
            try {
                fileStream.close();
            } catch (IOException e) {
            }
            fileStream = null;
        }
    }
}
