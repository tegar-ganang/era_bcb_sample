package org.vd.store;

import java.util.Properties;
import org.vd.store.impl.ReaderWriterVirtualStorage;
import org.vd.store.impl.StorageReader;
import org.vd.store.impl.StorageWriter;
import org.vd.store.impl.reader.google.GoogleStorageReader;
import org.vd.store.impl.writer.SMTPWebWriter;
import org.vd.store.impl.writer.SMTPWriter;
import org.vd.store.session.VirtualStorageSession;

/**
 * Created on Oct 9, 2006 11:21:40 PM by Ajay
 */
public class StorageFactory {

    public static VirtualStorage createStorage(String domain) {
        if (null == domain) return null;
        if ("gmail.com".equalsIgnoreCase(domain)) {
            StorageReader reader = new GoogleStorageReader();
            Properties prop = new Properties();
            prop.setProperty("mail.smtp.host", "smtp.gmail.com");
            prop.setProperty("mail.smtp.port", "465");
            prop.setProperty("mail.smtp.socketFactory.fallback", Boolean.FALSE.toString());
            prop.setProperty("mail.smtp.socketFactory.class", SMTPWriter.SSL_FACTORY);
            prop.setProperty("mail.smtp.auth", Boolean.TRUE.toString());
            prop.setProperty("mail.smtp.socketFactory.port", "465");
            StorageWriter writer = new SMTPWebWriter(prop, reader);
            VirtualStorage storage = new ReaderWriterVirtualStorage(reader, writer);
            storage.configure(StorageCache.getInstance().getParameters());
            return storage;
        }
        return null;
    }
}
