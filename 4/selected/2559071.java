package yarfraw.rss20.io;

import java.io.File;
import java.net.URI;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEventHandler;
import yarfraw.rss20.datamodel.Channel;
import yarfraw.rss20.datamodel.YarfrawException;
import yarfraw.rss20.elements.TRss;
import yarfraw.rss20.elements.TRssChannel;
import yarfraw.rss20.mapping.ChannelMapperImpl;
import yarfraw.rss20.utils.Utils;

/**
 * Provides a set of function to facilitate reading of an RSS 2.0 feed.
 * @author jliang
 *
 */
public class Rss20Reader extends AbstractBaseIO {

    Unmarshaller _u;

    public Rss20Reader(File file) {
        super(file);
    }

    public Rss20Reader(String pathName) {
        this(new File(pathName));
    }

    public Rss20Reader(URI uri) {
        this(new File(uri));
    }

    @SuppressWarnings("unchecked")
    public Channel readChannel(ValidationEventHandler validationEventHandler) throws YarfrawException {
        Unmarshaller u;
        try {
            u = getUnMarshaller();
            u.setEventHandler(validationEventHandler);
            JAXBElement<TRss> o = (JAXBElement<TRss>) u.unmarshal(_file);
            TRss rss = o.getValue();
            TRssChannel channel = rss.getChannel();
            return ChannelMapperImpl.getInstance().execute(channel);
        } catch (JAXBException e) {
            throw new YarfrawException("Unable to unmarshal file", e);
        }
    }

    public Channel readChannel() throws YarfrawException {
        return readChannel(null);
    }

    private Unmarshaller getUnMarshaller() throws JAXBException {
        if (_u == null) {
            _u = JAXBContext.newInstance(Utils.JAXB_CONTEXT).createUnmarshaller();
        }
        return _u;
    }
}
