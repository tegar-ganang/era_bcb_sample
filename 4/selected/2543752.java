package de.htwaalen.macker.rss.structure;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

/**
 * This class involves all detailed informations
 * of a RSS feed file, e.g. the feed type or the version.
 * <p>
 * It's also responsible for saving the RSS feed file to a 
 * directory or loading an existing RSS feed from a directory chosen by
 * a client.
 * 
 * @see FeedChannel
 *   
 * @author Damien Meersman, Aleksej Kniss, Philipp Kilic
 * @version 1.0
 * @since 1.0
 * 
 */
@XmlRootElement(name = "rss")
@XmlType(propOrder = { "xmlns", "version", "channel" })
public class RSS {

    /**
	 * The RSS type, in version 2.0 it means Really Simple Syndication.
	 */
    @XmlTransient
    private static final String FEED_TYPE = "rss_2.0";

    /**
	 * This represents the current XML version.
	 */
    private static final String XML_VERSION = "http://purl.org/dc/elements/1.1/";

    /**
	 * This variable is responsible to distinctly define
	 * the vocabulary of a RSS feed file.
	 */
    private String xmlns;

    /**
	 * This variable defines the version of a RSS feed
	 * file, e.g. rss_1.0 or rss_2.0.
	 */
    private String version;

    /**
	 * This is an object of a feed channel.
	 */
    private FeedChannel channel;

    /**
	 * Constructs a new {@code RSS}.
	 */
    public RSS() {
        setXmlns(XML_VERSION);
        setVersion(FEED_TYPE);
    }

    /**
	 * Constructs a new {@code RSS} with a {@code FeedChannel}.
	 * 
	 * @param channel this is a {@link FeedChannel} object to get
	 * informations about a feed item, e.g. title, link
	 * or description
	 */
    public RSS(final FeedChannel channel) {
        this();
        this.channel = channel;
    }

    /**
	 * Constructs a new {@code RSS}.
	 * 
	 * @param xmlns this is responsible to distinctly define
	 * the vocabulary of a RSS feed file.
	 * @param version this is the version of a feed item
	 * @param channel this is a {@link FeedChannel} object to get
	 * informations about a feed item, e.g. title, link
	 * or description
	 */
    public RSS(final String xmlns, final String version, final FeedChannel channel) {
        this.xmlns = xmlns;
        this.version = version;
        this.channel = channel;
    }

    /**
	 * This method loads a RSS feed file from the file system.
	 * 
	 * @param rssFile contains the path and the RSS feed 
	 * file where it is located
	 * @return returns the RSS feed file given by a client
	 * 
	 * @throws JAXBException This is the root exception 
	 * class for all JAXB exceptions
	 */
    public static RSS loadRSS(final String rssFile) throws JAXBException {
        final JAXBContext context = JAXBContext.newInstance(RSS.class);
        final Unmarshaller um = context.createUnmarshaller();
        return (RSS) um.unmarshal(new File(rssFile));
    }

    /**
	 * This method saves a RSS feed file to a path.
	 * 
	 * @param rssFile the directory path, where the RSS feed file 
	 * will be saved to
	 * 
	 * @throws JAXBException This is the root exception class 
	 * for all JAXB exceptions
	 * @throws IOException signals that an I/O exception of 
	 * some sort has occurred
	 */
    public void saveRSS(final String rssFile) throws JAXBException, IOException {
        final JAXBContext context = JAXBContext.newInstance(RSS.class);
        final Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        final FileWriter writer = new FileWriter(rssFile);
        m.marshal(this, writer);
        writer.close();
    }

    /**
	 * Get the channel of a RSS feed file.
	 * 
	 * @return returns the channel of a RSS feed file
	 */
    @XmlElement(name = "channel")
    public FeedChannel getChannel() {
        return this.channel;
    }

    /**
	 * Sets the channel of a RSS feed file.
	 * 
	 * @param channel this contains a list of feed items with 
	 * the informations for each feed item
	 */
    public void setChannel(final FeedChannel channel) {
        this.channel = channel;
    }

    /**
	 * Get the distinctly identifier of a RSS feed file.
	 * 
	 * @return returns the distinctly identifier for a RSS feed file
	 */
    @XmlAttribute(name = "xmlns:dc")
    public String getXmlns() {
        return this.xmlns;
    }

    /**
	 * Sets the distinctly identifier of a RSS feed file.
	 * 
	 * @param xmlns the distinctly identifier for a RSS feed file
	 */
    public void setXmlns(final String xmlns) {
        this.xmlns = xmlns;
    }

    /**
	 * Get the version of a RSS feed file.
	 *  
	 * @return returns the version of a RSS file
	 */
    @XmlAttribute(name = "version")
    public String getVersion() {
        return this.version;
    }

    /**
	 * Sets the version of a RSS feed file.
	 * 
	 * @param version the version of a RSS file
	 */
    public void setVersion(final String version) {
        this.version = version;
    }
}
