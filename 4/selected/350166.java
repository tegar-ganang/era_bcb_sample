package yarfraw.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpURL;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import yarfraw.core.datamodel.ChannelFeed;
import yarfraw.core.datamodel.FeedFormat;
import yarfraw.core.datamodel.YarfrawException;
import yarfraw.generated.atom10.elements.FeedType;
import yarfraw.generated.rss10.elements.RDF;
import yarfraw.generated.rss20.elements.TRss;
import yarfraw.mapping.backward.impl.ToChannelAtom03Impl;
import yarfraw.mapping.backward.impl.ToChannelAtom10Impl;
import yarfraw.mapping.backward.impl.ToChannelRss10Impl;
import yarfraw.mapping.backward.impl.ToChannelRss20Impl;
import yarfraw.utils.JAXBUtils;

/**
 * Provides a set of function to facilitate reading of a RSS feed.
 * 
 * @author jliang
 *
 */
public class FeedReader extends AbstractBaseFeedParser {

    private static final Log LOG = LogFactory.getLog(FeedReader.class);

    /**
   * Constructs a {@link FeedReader} to read from a local file.
   * @param file - the local file to be read from 
   * @param format - the {@link FeedFormat} of the feed.
   */
    public FeedReader(File file, FeedFormat format) {
        super(file, format);
    }

    /**
   * Constructs a {@link FeedReader} to read from a local file.
   * @param pathName - full path of the file
   * @param format - the {@link FeedFormat} of the feed.
   */
    public FeedReader(String pathName, FeedFormat format) {
        super(new File(pathName), format);
    }

    /**
   * Constructs a {@link FeedReader} to read from a local file.
   * @param uri - the {@link URI} that points to the file
   * @param format - the {@link FeedFormat} of the feed.
   */
    public FeedReader(URI uri, FeedFormat format) {
        super(new File(uri), format);
    }

    /**
   * Constructs a {@link FeedReader} to read from a local file.
   * <br/>
   * Note the {@link FeedFormat} will be set to default which is RSS 2.0
   * @param file - a local file
   */
    public FeedReader(File file) {
        super(file);
    }

    /**
   * Constructs a {@link FeedReader} to read from a local file.
   * <br/>
   * Note the {@link FeedFormat} will be set to default which is RSS 2.0
   * @param pathName - full path of the file
   */
    public FeedReader(String pathName) {
        super(new File(pathName));
    }

    /**
   * Constructs a {@link FeedReader} to read from a local file.
   * <br/>
   * Note the {@link FeedFormat} will be set to default which is RSS 2.0
   * @param uri - the uril that points to the file
   */
    public FeedReader(URI uri) {
        super(new File(uri));
    }

    /**
   * Constructs a {@link FeedReader} to read from a remote source using Http.
   * <br/>
   * Format detection will be automatically performed. 
   * @param httpUrl - the {@link HttpURL} of the remote source
   * @param params - any {@link HttpClientParams}
   * @throws YarfrawException - if parse failed
   * @throws IOException - if format detection failed
   * @Deprecated use {@link CachedFeedReader} for remote feed reading, it offers the same set of 
   * features as this class as well as HTTP conditional get
   */
    public FeedReader(HttpURL httpUrl, HttpClientParams params) throws YarfrawException, IOException {
        super(httpUrl, params);
    }

    /**
   * Constructs a {@link FeedReader} to read from a remote source using Http.
   * <br/>
   * Format detection will be automatically performed.
   * @param httpUrl - the {@link HttpURL} of the remote source
   * @throws YarfrawException - if parse failed
   * @throws IOException - if format detection failed
   * @Deprecated use {@link CachedFeedReader} for remote feed reading, it offers the same set of 
   * features as this class as well as HTTP conditional get
   */
    public FeedReader(HttpURL httpUrl) throws YarfrawException, IOException {
        super(httpUrl, null);
    }

    /**
   * Constructs a {@link FeedReader} to read from a remote source using Http.
   * <br/>
   * Format detection will be automatically performed.
   * @param getMethod
   * @throws YarfrawException - if parse failed
   * @throws IOException - if format detection failed
   * 
   */
    public FeedReader(GetMethod getMethod) throws YarfrawException, IOException {
        super(getMethod);
    }

    /**
   * Reads a channel from a local or remote feed with a custom {@link ValidationEventHandler}
   *
   * @param format any supported {@link yarfraw.core.datamodel.FeedFormat}
   * @param inputStream any {@link java.io.InputStream}
   * @return a {@link ChannelFeed} object
   * @throws YarfrawException if read operation failed.
   */
    public static ChannelFeed readChannel(FeedFormat format, InputStream inputStream) throws YarfrawException {
        Unmarshaller u;
        try {
            u = getUnMarshaller(format);
            return toChannel(format, u.unmarshal(inputStream));
        } catch (JAXBException e) {
            throw new YarfrawException("Unable to unmarshal file", e);
        }
    }

    /**
   * Reads a channel from a local or remote feed with a custom {@link ValidationEventHandler}
   *
   * @param validationEventHandler a custom {@link javax.xml.bind.ValidationEventHandler}
   * @return a {@link ChannelFeed} object
   * @throws YarfrawException if read operation failed.
   */
    public ChannelFeed readChannel(ValidationEventHandler validationEventHandler) throws YarfrawException {
        Unmarshaller u;
        InputStream input = null;
        try {
            input = getStream();
            if (input == null) {
                LOG.warn("Unable to read from null stream, returning null");
                return null;
            }
            u = getUnMarshaller(_format);
            if (validationEventHandler != null) {
                u.setEventHandler(validationEventHandler);
            }
            return toChannel(_format, u.unmarshal(input));
        } catch (JAXBException e) {
            throw new YarfrawException("Unable to unmarshal file", e);
        } catch (HttpException e) {
            throw new YarfrawException("Unable to read from remote url", e);
        } catch (IOException e) {
            throw new YarfrawException("Unable to read", e);
        } finally {
            IOUtils.closeQuietly(input);
        }
    }

    @SuppressWarnings("unchecked")
    private static ChannelFeed toChannel(FeedFormat format, Object o) throws YarfrawException {
        if (format == FeedFormat.RSS20) {
            return ToChannelRss20Impl.getInstance().execute(((JAXBElement<TRss>) o).getValue().getChannel());
        } else if (format == FeedFormat.RSS10) {
            return ToChannelRss10Impl.getInstance().execute((RDF) o);
        } else if (format == FeedFormat.ATOM10) {
            return ToChannelAtom10Impl.getInstance().execute(((JAXBElement<FeedType>) o).getValue());
        } else if (format == FeedFormat.ATOM03) {
            return ToChannelAtom03Impl.getInstance().execute(((JAXBElement<yarfraw.generated.atom03.elements.FeedType>) o).getValue());
        } else {
            throw new UnsupportedOperationException("Unknown Feed Format");
        }
    }

    /**
   * Reads a channel from a local or remote feed.
   *
   * @return a {@link ChannelFeed} object
   * @throws YarfrawException if read operation failed.
   */
    public ChannelFeed readChannel() throws YarfrawException {
        return readChannel(null);
    }

    private static class WarningHandler implements ValidationEventHandler {

        public boolean handleEvent(ValidationEvent event) {
            DefaultValidationEventHandler d = new DefaultValidationEventHandler();
            d.handleEvent(event);
            return event.getSeverity() == ValidationEvent.FATAL_ERROR;
        }
    }

    private static Unmarshaller getUnMarshaller(FeedFormat format) throws JAXBException {
        JAXBContext context = JAXBUtils.getContext(format);
        Unmarshaller u = context.createUnmarshaller();
        u.setEventHandler(new WarningHandler());
        return u;
    }
}
