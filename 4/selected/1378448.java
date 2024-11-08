package org.fsconnector.spi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.HashSet;
import java.util.Iterator;
import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.EISSystemException;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;
import org.fsconnector.cci.FSConnectionImpl;

public class FSManagedConnection implements ManagedConnection {

    private HashSet<ConnectionEventListener> listeners = new HashSet<ConnectionEventListener>();

    private PrintWriter logWriter;

    private Subject subject;

    private FSConnectionRequestInfo info;

    private FSConnectionImpl conn;

    private File chroot;

    public FSManagedConnection(Subject subject, ConnectionRequestInfo cxRequestInfo) {
        this.subject = subject;
        this.info = (FSConnectionRequestInfo) cxRequestInfo;
    }

    public void addConnectionEventListener(ConnectionEventListener listener) {
        listeners.add(listener);
    }

    public void associateConnection(Object connection) throws ResourceException {
        if (logWriter != null) logWriter.println("associateConnection(" + connection + ")");
        conn = (FSConnectionImpl) connection;
        conn.setManager(this);
    }

    public void cleanup() throws ResourceException {
        if (logWriter != null) logWriter.println("cleanup()");
    }

    public void destroy() throws ResourceException {
        if (logWriter != null) logWriter.println("destroy()");
    }

    public Object getConnection(Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        if (logWriter != null) logWriter.println("getConnection(" + subject + ", " + cxRequestInfo + ")");
        if (cxRequestInfo == null && conn != null) return conn;
        if (cxRequestInfo != null) {
            FSConnectionRequestInfo oldInfo = info;
            this.subject = subject;
            this.info = (FSConnectionRequestInfo) cxRequestInfo;
            if (logWriter != null) logWriter.println("Replaced old info, " + oldInfo + ", with " + info);
        }
        if (info.getRootDir() == null || info.getRootDir().equals("")) throw new EISSystemException("Root Directory configuration is missing");
        if (!info.getRootDir().exists()) throw new EISSystemException("Root Directory \"" + info.getRootDir().getAbsolutePath() + "\" Does Not Exist");
        File oldChroot = chroot;
        chroot = info.getRootDir();
        if (logWriter != null) logWriter.println("Replaced old chroot, " + oldChroot + ", with " + chroot);
        FSConnectionImpl oldConn = conn;
        conn = new FSConnectionImpl(this);
        if (logWriter != null) logWriter.println("Replaced old conn, " + oldConn + ", with " + conn);
        return conn;
    }

    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new NotSupportedException("Local Transactions are not supported");
    }

    public PrintWriter getLogWriter() throws ResourceException {
        return logWriter;
    }

    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        return new ManagedConnectionMetaData() {

            public String getEISProductName() throws ResourceException {
                return "Local File System";
            }

            public String getEISProductVersion() throws ResourceException {
                return System.getProperty("os.version");
            }

            public int getMaxConnections() throws ResourceException {
                return 16;
            }

            public String getUserName() throws ResourceException {
                return System.getProperty("user.name");
            }
        };
    }

    public XAResource getXAResource() throws ResourceException {
        throw new NotSupportedException("XA Transactions are not supported");
    }

    public void removeConnectionEventListener(ConnectionEventListener listener) {
        listeners.remove(listener);
    }

    public void setLogWriter(PrintWriter out) throws ResourceException {
        this.logWriter = out;
    }

    protected void fireConnectionEvent(ConnectionEvent evt) {
        if (logWriter != null) logWriter.println("fireConnectionEvent(" + evt + ")");
        Iterator<ConnectionEventListener> it;
        for (it = listeners.iterator(); it.hasNext(); ) {
            ConnectionEventListener listener = it.next();
            if (evt.getId() == ConnectionEvent.CONNECTION_CLOSED) listener.connectionClosed(evt); else if (evt.getId() == ConnectionEvent.CONNECTION_ERROR_OCCURRED) listener.connectionErrorOccurred(evt);
        }
    }

    protected ConnectionRequestInfo getInfo() {
        return info;
    }

    protected Subject getSubject() {
        return subject;
    }

    protected File getChroot() {
        return chroot;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FSManagedConnection)) return false;
        FSManagedConnection other = (FSManagedConnection) obj;
        return chroot.equals(other.chroot);
    }

    public void close() {
        ConnectionEvent evt = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
        evt.setConnectionHandle(conn);
        fireConnectionEvent(evt);
    }

    public void copy(File from, File to) throws IOException {
        copy(from, to, false);
    }

    public void copy(File from, File to, boolean append) throws IOException {
        if (logWriter != null) logWriter.println("copy(" + from + ", " + to + ", " + append + ")");
        OutputStream out = openOutputStream(to, append);
        try {
            InputStream in = openInputStream(from);
            try {
                if (from.length() > 65536L) {
                    byte[] buff = new byte[65536];
                    int len;
                    while ((len = in.read(buff)) > -1) if (len != 0) out.write(buff, 0, len);
                } else {
                    byte[] buff = new byte[(int) from.length()];
                    int len = in.read(buff);
                    out.write(buff, 0, len);
                }
            } finally {
                try {
                    in.close();
                } catch (Exception e) {
                }
            }
        } finally {
            try {
                out.close();
            } catch (Exception e) {
            }
        }
    }

    public File getFile(String filename) {
        return getFile(chroot, filename);
    }

    public File getFile(File parent, String filename) {
        if (logWriter != null) logWriter.println("getFile(" + parent + ", " + filename + ")");
        return new File(parent, filename);
    }

    public File[] listFiles() {
        return listFiles(null);
    }

    public File[] listFiles(FilenameFilter filter) {
        if (logWriter != null) logWriter.println("listFiles(" + filter + ")");
        return chroot.listFiles(filter);
    }

    public InputStream openInputStream(String filename) throws FileNotFoundException {
        return openInputStream(getFile(filename));
    }

    public InputStream openInputStream(File file) throws FileNotFoundException {
        if (logWriter != null) logWriter.println("openInputStream(" + file + ")");
        return new FileInputStream(file);
    }

    public OutputStream openOutputStream(String filename) throws IOException {
        return openOutputStream(filename, false);
    }

    public OutputStream openOutputStream(String filename, boolean append) throws IOException {
        return openOutputStream(getFile(filename), append);
    }

    public OutputStream openOutputStream(File file) throws IOException {
        return openOutputStream(file, false);
    }

    public OutputStream openOutputStream(File file, boolean append) throws IOException {
        if (logWriter != null) logWriter.println("openOutputStream(" + file + ", " + append + ")");
        return new FileOutputStream(file, append);
    }

    public Reader openReader(String filename) throws FileNotFoundException {
        return openReader(getFile(filename));
    }

    public Reader openReader(File file) throws FileNotFoundException {
        if (logWriter != null) logWriter.println("openReader(" + file + ")");
        return new FileReader(file);
    }

    public Writer openWriter(String filename) throws IOException {
        return openWriter(filename, false);
    }

    public Writer openWriter(String filename, boolean append) throws IOException {
        return openWriter(getFile(filename), append);
    }

    public Writer openWriter(File file) throws IOException {
        return openWriter(file, false);
    }

    public Writer openWriter(File file, boolean append) throws IOException {
        if (logWriter != null) logWriter.println("openWriter(" + file + ", " + append + ")");
        return new FileWriter(file, append);
    }
}
