package org.vd.test.yahoo;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;
import javax.security.sasl.AuthenticationException;
import junit.framework.TestCase;
import org.vd.store.StorageCache;
import org.vd.store.VirtualFile;
import org.vd.store.VirtualStorage;
import org.vd.store.impl.ReaderWriterVirtualStorage;
import org.vd.store.impl.StorageReader;
import org.vd.store.impl.StorageWriter;
import org.vd.store.impl.reader.yahoo.YahooStorageReader;
import org.vd.store.impl.writer.yahoo.YahooStorageWriter;

public class YahooTest extends TestCase {

    VirtualStorage storage;

    public void setUp() {
        StorageReader reader = new YahooStorageReader();
        Properties prop = new Properties();
        StorageWriter writer = new YahooStorageWriter(prop, reader);
        storage = new ReaderWriterVirtualStorage(reader, writer);
        storage.configure(StorageCache.getInstance().getParameters());
        try {
            storage.authenticate("", "".toCharArray());
            System.out.println("Authenticated User");
        } catch (AuthenticationException e) {
            boolean authenticated = false;
            assertFalse(authenticated);
        }
    }

    public void testList() throws Exception {
        VirtualFile root = storage.toFile("/");
        Set<VirtualFile> files = storage.list(root);
        InputStream in = new ByteArrayInputStream("This is a test line.".getBytes("UTF-8"));
        try {
            storage.save(root, in);
        } finally {
            in.close();
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (storage != null) {
            storage.logout();
            storage = null;
        }
    }
}
