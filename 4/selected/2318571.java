package org.pagger.util.config;

import java.beans.ExceptionListener;
import java.io.IOException;

/**
 * @author Franz Wilhelmstötter
 */
class ReadWriteConfigurationListener implements ConfigurationListener {

    private final Class<?> type;

    private final ConfigurationReaderFactory readerFactory;

    private final ConfigurationWriterFactory writerFactory;

    private final ExceptionListener exceptionListener;

    public ReadWriteConfigurationListener(final Class<?> type, final ConfigurationReaderFactory readerFactory, final ConfigurationWriterFactory writerFactory, final ExceptionListener exceptionListener) {
        this.type = type;
        this.readerFactory = readerFactory;
        this.writerFactory = writerFactory;
        this.exceptionListener = exceptionListener;
    }

    @Override
    public void entryChanged(final ConfigurationEvent event) {
        final EntryData entry = event.getEntryData();
        try {
            writerFactory.newWriter(type).write(entry.getSection().getConfiguration());
        } catch (IOException e) {
            exceptionListener.exceptionThrown(e);
        }
    }

    @Override
    public void entryRead(final ConfigurationEvent event) {
        try {
            ConfigurationData that = event.getEntryData().getSection().getConfiguration();
            ConfigurationData other = readerFactory.newReader(type).read(that.getName());
            that.update(other);
        } catch (IOException e) {
            exceptionListener.exceptionThrown(e);
        }
    }
}
