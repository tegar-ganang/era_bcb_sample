package org.vd.store.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.EventListener;
import java.util.Hashtable;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.slide.authenticate.AuthenticateException;
import org.apache.slide.common.SlideToken;
import org.vd.store.FileChangeListener;
import org.vd.store.NotLoggedInException;
import org.vd.store.ReadOnlyStorageException;
import org.vd.store.StoreFailedException;
import org.vd.store.VirtualFile;
import org.vd.store.VirtualStorage;
import sun.awt.EventListenerAggregate;

public class ReaderWriterVirtualStorage implements VirtualStorage {

    private static final Log LOGGER = LogFactory.getLog(ReaderWriterVirtualStorage.class);

    private EventListenerAggregate m_listeners;

    private final StorageReader m_reader;

    private final StorageWriter m_writer;

    private Hashtable m_properties;

    public ReaderWriterVirtualStorage(StorageReader reader, StorageWriter writer) {
        if (null == reader) throw new NullPointerException("Reader should not be null.");
        m_listeners = new EventListenerAggregate(FileChangeListener.class);
        m_reader = reader;
        if (m_reader instanceof FileChangeListener) m_listeners.add((FileChangeListener) m_reader);
        m_writer = writer;
        if (m_writer instanceof FileChangeListener) m_listeners.add((FileChangeListener) m_writer);
    }

    public void configure(Hashtable properties) {
        m_properties = properties;
        m_reader.configure(properties);
        m_writer.configure(properties);
    }

    public void authenticate(SlideToken slideToken, String login, char[] password) throws AuthenticateException {
        m_reader.authenticate(slideToken, login, password);
        if (m_writer != m_reader) m_writer.authenticate(slideToken, login, password);
    }

    public void delete(VirtualFile file) throws NotLoggedInException, ReadOnlyStorageException, StoreFailedException, IOException {
        if (null != m_writer) {
            m_writer.delete(file);
            return;
        }
        throw new ReadOnlyStorageException();
    }

    public boolean exists(VirtualFile file) throws NotLoggedInException, IOException {
        return m_reader.exists(file);
    }

    public Set<VirtualFile> list(VirtualFile dir) throws NotLoggedInException, IOException {
        return m_reader.list(dir);
    }

    public InputStream load(VirtualFile file) throws NotLoggedInException, IOException {
        return m_reader.load(file);
    }

    public VirtualFile toFile(String path) throws NotLoggedInException, IOException {
        return m_reader.toFile(path);
    }

    public void logout() {
        m_reader.logout();
        if (m_writer != m_reader) m_writer.logout();
    }

    public void mkdirs(VirtualFile dir, String dirs) throws NotLoggedInException, ReadOnlyStorageException, StoreFailedException {
        if (null != m_writer) {
            VirtualFile x = m_writer.mkdirs(dir, dirs);
            fireFileEvent(x, true);
            return;
        }
        throw new ReadOnlyStorageException();
    }

    public void move(VirtualFile from, VirtualFile to) throws NotLoggedInException, ReadOnlyStorageException, StoreFailedException, IOException {
        if (null != m_writer) {
            m_writer.move(from, to);
            fireFileEvent(from, false);
            fireFileEvent(to, true);
            return;
        }
        throw new ReadOnlyStorageException();
    }

    public void save(VirtualFile file, InputStream in) throws NotLoggedInException, ReadOnlyStorageException, StoreFailedException {
        if (null != m_writer) {
            m_writer.save(file, in);
            fireFileEvent(file, true);
            return;
        }
        throw new ReadOnlyStorageException();
    }

    protected void fireFileEvent(VirtualFile file, boolean added) {
        EventListener listeners[] = m_listeners.getListenersCopy();
        if (null == listeners) return;
        for (int i = 0; i < listeners.length; i++) {
            FileChangeListener listener = (FileChangeListener) listeners[i];
            if (added) listener.fileAdded(file); else listener.fileDeleted(file);
        }
    }
}
