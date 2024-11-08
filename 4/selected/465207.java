package net.sourceforge.filera.spi.outbound;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.resource.spi.SharingViolationException;
import javax.security.auth.Subject;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import net.sourceforge.filera.api.DefaultFileResourceAdapterConnection;
import net.sourceforge.filera.api.FileResourceAdapterException;
import net.sourceforge.rafc.spi.outbound.AbstractManagedConnection;

/**
 * Managed connection of FileResourceAdapter.
 * 
 * @author Markus KARG (markus-karg@users.sourceforge.net)
 */
public final class FileResourceAdapterManagedConnection extends AbstractManagedConnection {

    public FileResourceAdapterManagedConnection(final PrintWriter logWriter) {
        super(logWriter);
    }

    public final ManagedConnectionMetaData getMetaData() throws ResourceException {
        final ManagedConnectionMetaData managedConnectionMetaData = new FileResourceAdapterManagedConnectionMetaData();
        return managedConnectionMetaData;
    }

    public final Object getConnection(final Subject subject, final ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        final Object connection = new DefaultFileResourceAdapterConnection(this);
        return connection;
    }

    public final void associateConnection(final Object connection) throws ResourceException {
        if (!(connection instanceof DefaultFileResourceAdapterConnection)) throw new SharingViolationException();
        final DefaultFileResourceAdapterConnection connectionHandle = (DefaultFileResourceAdapterConnection) connection;
        connectionHandle.setManagedConnection(this);
    }

    public final InputStream getInputStream(final File file) throws FileNotFoundException {
        final InputStream inputStream = new FileInputStream(file);
        return inputStream;
    }

    public final OutputStream getOutputStream(final File file) throws FileNotFoundException {
        this.createParentPathIfNeeded(file);
        final OutputStream outputStream = new FileOutputStream(file);
        return outputStream;
    }

    public final boolean removeFile(final File file) {
        final boolean fileIsRemoved = file.delete();
        return fileIsRemoved;
    }

    public final String createFile(final File file) throws IOException {
        this.createParentPathIfNeeded(file);
        file.createNewFile();
        final String canonicalName = file.getCanonicalPath();
        return canonicalName;
    }

    /**
     * Default prefix for temporary files. <br>
     * TODO Should be configurable by administrator.
     */
    private static final String TEMPORARY_FILE_PREFIX = "filera";

    public final File createTemporaryFile() throws IOException {
        final File file = File.createTempFile(FileResourceAdapterManagedConnection.TEMPORARY_FILE_PREFIX, null, null);
        file.deleteOnExit();
        return file;
    }

    public final File createTemporaryFile(String prefix, String suffix) throws IOException {
        final File file = File.createTempFile(prefix, suffix, null);
        file.deleteOnExit();
        return file;
    }

    /**
     * Internal helper method which copies a file.
     * 
     * @param fromFile
     *                   The file to copy.
     * @param toFile
     *                   The target file.
     * @throws IOException
     *                   If the copy could not be performed.
     */
    public final void copyFile(final File fromFile, final File toFile) throws IOException {
        this.createParentPathIfNeeded(toFile);
        final FileChannel sourceChannel = new FileInputStream(fromFile).getChannel();
        final FileChannel targetChannel = new FileOutputStream(toFile).getChannel();
        final long sourceFileSize = sourceChannel.size();
        sourceChannel.transferTo(0, sourceFileSize, targetChannel);
    }

    public final void moveFile(final File fromFile, final File toFile) throws IOException {
        this.createParentPathIfNeeded(toFile);
        final boolean renamedSuccessfully = fromFile.renameTo(toFile);
        if (renamedSuccessfully) return;
        this.copyFile(fromFile, toFile);
        this.removeFile(fromFile);
    }

    public final boolean existsFile(final File file) throws FileResourceAdapterException {
        final boolean fileExists = file.exists();
        return fileExists;
    }

    /**
     * Internal helper method which ensures that the parent path of the path
     * identified by <code>pathName</code> is existing. If necessary, it
     * creates all superordinate directories.
     * 
     * @param file
     *                   The path whose parent directories will be created if needed.
     */
    private final void createParentPathIfNeeded(final File file) {
        final File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists()) parentFile.mkdirs();
    }

    public final File getFile(final String pathName) {
        final File file = new File(pathName);
        return file;
    }

    public final String getPathName(final File file) throws IOException {
        final String pathName = file.getCanonicalPath();
        return pathName;
    }

    private final XAResource xaResource = new XAResource() {

        private int transactionTimeout = Integer.MAX_VALUE;

        public final int getTransactionTimeout() throws XAException {
            return this.transactionTimeout;
        }

        public final boolean setTransactionTimeout(final int seconds) throws XAException {
            if (seconds == 0) this.transactionTimeout = Integer.MAX_VALUE; else this.transactionTimeout = seconds;
            return true;
        }

        public final boolean isSameRM(final XAResource xares) throws XAException {
            return this == xares;
        }

        private final Xid[] XIDS = new Xid[] {};

        public final Xid[] recover(final int flag) throws XAException {
            return this.XIDS;
        }

        public final int prepare(final Xid xid) throws XAException {
            return XAResource.XA_OK;
        }

        public final void forget(final Xid xid) throws XAException {
        }

        public final void rollback(final Xid xid) throws XAException {
        }

        public final void end(final Xid xid, final int flags) throws XAException {
        }

        public final void start(final Xid xid, final int flags) throws XAException {
        }

        public final void commit(final Xid xid, final boolean onePhase) throws XAException {
        }
    };

    public XAResource getXAResource() throws ResourceException {
        return this.xaResource;
    }
}
