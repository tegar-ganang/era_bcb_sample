package slojj.dotsbox.parser;

import org.jdom.Document;
import org.jdom.Namespace;

public class FeedParser extends AbstractFeedParser {

    public static final String FEED_RDF10 = "RDF 1.0";

    public static final String FEED_ATOM10 = "Atom 1.0";

    public static final String FEED_RSS091 = "RSS 0.91";

    public static final String FEED_RSS092 = "RSS 0.92";

    public static final String FEED_RSS10 = "RSS 1.0";

    public static final String FEED_RSS20 = "RSS 2.0";

    private static final String ATOM10_NAMESPACE = "http://www.w3.org/2005/Atom";

    public FeedParser(Document document, String link) throws ChannelBuilderException {
        super(document, new Channel(), link);
    }

    @Override
    protected void parse() throws ChannelBuilderException {
        String feedformat = getFeedformat();
        if (feedformat.equals("")) {
            throw new ChannelBuilderException(link, ChannelBuilderException.ERROR_INVALID_NEWSFEED, "Invalid feed version or we cannot parse this!");
        }
        AbstractFeedParser parser = null;
        if (feedformat.equals(FEED_RSS20) || feedformat.equals(FEED_RSS091) || feedformat.equals(FEED_RSS092) || feedformat.equals(FEED_RDF10)) {
            parser = new FeedParserRss(document, channel, link, namespaces);
        } else if (feedformat.equals(FEED_ATOM10)) {
            parser = new FeedParserAtom(document, channel, link, namespaces);
        }
        assert (parser != null);
        parser.parse();
    }

    protected String getFeedformat() {
        String name = rootElement.getName();
        if (name.equalsIgnoreCase("rdf")) {
            channel.setFeedFormat(FEED_RDF10);
            return FEED_RDF10;
        }
        if (name.equalsIgnoreCase("feed")) {
            Namespace ns = rootElement.getNamespace();
            if (ns != null && ns.getURI().equalsIgnoreCase(ATOM10_NAMESPACE)) {
                channel.setFeedFormat(FEED_ATOM10);
                return FEED_ATOM10;
            }
            return "";
        }
        String version = getAttributeValue(rootElement, "version");
        if (version != null) {
            String feedformat = "RSS " + version;
            channel.setFeedFormat(feedformat);
            return feedformat;
        }
        return "";
    }

    public Channel getChannel() {
        return channel;
    }
}
