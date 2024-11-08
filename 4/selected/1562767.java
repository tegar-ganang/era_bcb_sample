package org.apache.nutch.parse.rss;

import org.apache.commons.feedparser.DefaultFeedParserListener;
import org.apache.commons.feedparser.FeedParserState;
import org.apache.commons.feedparser.FeedParserException;
import java.util.List;
import java.util.Vector;
import org.apache.nutch.parse.rss.structs.RSSChannel;
import org.apache.nutch.parse.rss.structs.RSSItem;

/**
 * 
 * @author mattmann
 * @version 1.0
 * 
 * <p>
 * Feed parser listener class which builds up an RSS Channel model that can be
 * iterated through to retrieve the parsed information.
 * </p>
 */
public class FeedParserListenerImpl extends DefaultFeedParserListener {

    private List fRssChannels = null;

    private RSSChannel fCurrentChannel = null;

    /**
     * <p>
     * Default Constructor
     * </p>
     */
    public FeedParserListenerImpl() {
        fRssChannels = new Vector();
    }

    /**
     * <p>
     * Gets a {@link List}of {@link RSSChannel}s that the listener parsed from
     * the RSS document.
     * </p>
     * 
     * @return A {@link List}of {@link RSSChannel}s.
     */
    public List getChannels() {
        if (fRssChannels.size() > 0) {
            return fRssChannels;
        } else {
            fRssChannels.add(fCurrentChannel);
            return fRssChannels;
        }
    }

    /**
     * <p>
     * Callback method when the parser encounters an RSS Channel.
     * </p>
     * 
     * @param state
     *            The current state of the FeedParser.
     * @param title
     *            The title of the RSS Channel.
     * @param link
     *            A hyperlink to the RSS Channel.
     * @param description
     *            The description of the RSS Channel.
     */
    public void onChannel(FeedParserState state, String title, String link, String description) throws FeedParserException {
        if (fCurrentChannel != null) {
            fRssChannels.add(fCurrentChannel);
        }
        fCurrentChannel = new RSSChannel(title, link, description);
    }

    /**
     * <p>
     * Callback method when the parser encounters an RSS Item.
     * </p>
     * 
     * @param state
     *            The current state of the FeedParser.
     * @param title
     *            The title of the RSS Item.
     * @param link
     *            A hyperlink to the RSS Item.
     * @param description
     *            The description of the RSS Item.
     * @param permalink
     *            A permanent link to the RSS Item.
     */
    public void onItem(FeedParserState state, String title, String link, String description, String permalink) throws FeedParserException {
        if (fCurrentChannel != null) {
            fCurrentChannel.getItems().add(new RSSItem(title, link, description, permalink));
        }
    }
}
