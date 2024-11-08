package de.htwaalen.macker.rss;

import de.htwaalen.macker.rss.structure.FeedChannel;
import de.htwaalen.macker.rss.structure.FeedItem;
import de.htwaalen.macker.rss.structure.RSS;
import java.io.File;
import java.io.IOException;
import javax.xml.bind.JAXBException;

/**
 * The FeedWriter class is responsible to
 * save a RSS feed in a output file.
 * 
 * @author Damien Meersman, Aleksej Kniss, Philipp Kilic
 * @version 1.0
 * @since 1.0
 *
 */
public final class FeedWriter {

    /**
	 * This is the title for the whole RSS feed,
	 * which is fix defined for all created RSS feeds.
	 */
    private static final String FEED_TITLE = "A RSS feed for the Macker Webservice.";

    /**
	 * This is the description for the whole RSS feed,
	 * which is fix defined for all created RSS feeds. 
	 */
    private static final String FEED_DESCRIPTION = "This RSS feed shows all actions for the webservice on " + "the Macker WebService!";

    /**
	 * Constructs a new {@code FeedWriter}.
	 */
    private FeedWriter() {
    }

    /**
	 * This method saves a RSS feed to a {@code path} which
	 * have to be chosen by the client.
	 * 
	 * @param path the path where the client wants to save 
	 * the RSS feed file
	 * @param item a new {@link FeedItem}
	 * 
	 * @throws JAXBException this is the root exception 
	 * class for all JAXB exceptions
	 * @throws IOException Signals that an I/O exception 
	 * of some sort has occurred
	 */
    public static void writeFeed(final String path, final FeedItem item) throws JAXBException, IOException {
        RSS feed = null;
        if (new File(path).exists()) feed = RSS.loadRSS(path); else feed = new RSS();
        FeedChannel channel = feed.getChannel();
        if (channel == null) {
            channel = new FeedChannel(FEED_TITLE, path, FEED_DESCRIPTION);
            feed.setChannel(channel);
        }
        channel.addItem(item);
        feed.saveRSS(path);
    }
}
