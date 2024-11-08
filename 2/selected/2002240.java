package net.ontopia.topicmaps.impl.utils;

import java.io.IOException;
import java.io.Reader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.util.Collection;
import java.util.Collections;
import java.net.URL;
import org.xml.sax.InputSource;
import net.ontopia.infoset.core.LocatorIF;
import net.ontopia.topicmaps.core.TopicMapIF;
import net.ontopia.topicmaps.core.TopicMapReaderIF;
import net.ontopia.topicmaps.core.TopicMapStoreIF;
import net.ontopia.topicmaps.core.TopicMapStoreFactoryIF;
import net.ontopia.topicmaps.core.TopicMapImporterIF;
import net.ontopia.topicmaps.impl.basic.InMemoryStoreFactory;
import net.ontopia.topicmaps.utils.SameStoreFactory;

/**
 * INTERNAL: Common abstract superclass for topic map readers.
 */
public abstract class AbstractTopicMapReader implements TopicMapReaderIF, TopicMapImporterIF {

    protected InputSource source;

    protected LocatorIF base_address;

    protected TopicMapStoreFactoryIF store_factory;

    /**
   * PUBLIC: Gets the SAX input source used by the reader.
   */
    public InputSource getInputSource() {
        return source;
    }

    /**
   * PUBLIC: Sets the SAX input source used by the reader.
   */
    public void setInputSource(InputSource source) {
        this.source = source;
    }

    /**
   * PUBLIC: Gets the top level base address of the input source.
   */
    public LocatorIF getBaseAddress() {
        return base_address;
    }

    /**
   * PUBLIC: Sets the top level base address of the input source.</p>
   *
   * The top level base address is used to resolve relative addresses
   * during input source processing. This property need not be set if
   * the input source specifies the base address.</p>   
   */
    public void setBaseAddress(LocatorIF base_address) {
        this.base_address = base_address;
    }

    /**
   * PUBLIC: Gets the store factory which will be used to create stores.
   */
    public TopicMapStoreFactoryIF getStoreFactory() {
        if (store_factory == null) {
            store_factory = new InMemoryStoreFactory();
        }
        return store_factory;
    }

    /**
   * PUBLIC: Sets the store factory which will be used to create stores.</p>
   *
   * <p>Default: {@link
   * net.ontopia.topicmaps.impl.basic.InMemoryStoreFactory}</p>
   *
   * @param store_factory The store factory to use. If the parameter
   * is null the default store factory will be used.
   */
    public void setStoreFactory(TopicMapStoreFactoryIF store_factory) {
        this.store_factory = store_factory;
    }

    public TopicMapIF read() throws IOException {
        return read(getStoreFactory());
    }

    public Collection<TopicMapIF> readAll() throws IOException {
        return readAll(getStoreFactory());
    }

    protected Collection<TopicMapIF> readAll(TopicMapStoreFactoryIF store_factory) throws IOException {
        return Collections.singleton(read(store_factory));
    }

    protected abstract TopicMapIF read(TopicMapStoreFactoryIF store_factory) throws IOException;

    public void importInto(TopicMapIF topicmap) throws IOException {
        TopicMapStoreIF store = topicmap.getStore();
        if (store == null) throw new IOException("Topic map not connected to a store.");
        readAll(new SameStoreFactory(store));
    }

    /**
   * INTERNAL: Creates a correctly configured Reader from a SAX
   * InputSource object.
   */
    public static Reader makeReader(InputSource source, EncodingSnifferIF sniffer) throws IOException {
        Reader reader = source.getCharacterStream();
        if (reader == null) {
            if (source.getByteStream() != null) reader = makeReader(source.getByteStream(), source.getEncoding(), sniffer); else {
                URL url = new URL(source.getSystemId());
                reader = makeReader(url.openStream(), null, sniffer);
            }
        }
        return reader;
    }

    public static Reader makeReader(InputStream stream, String encoding, EncodingSnifferIF sniffer) throws IOException {
        if (encoding == null) {
            stream = new PushbackInputStream(stream, 50);
            encoding = sniffer.guessEncoding((PushbackInputStream) stream);
        }
        return new InputStreamReader(stream, encoding);
    }
}
