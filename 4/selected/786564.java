package syndication.rss;

import org.xml.sax.Attributes;
import syndication.ElementData;
import syndication.MalformedFeedElementException;

/**
 * At the top level, a RSS document is a <rss> element, with a mandatory
 * attribute called version, that specifies the version of RSS that the
 * document conforms to.
 * 
 * @author Omistaja
 */
public class Rss {

    private String version = null;

    private Channel channel = null;

    public Rss(ElementData element) throws MalformedFeedElementException {
        if (element == null) {
            throw new MalformedFeedElementException("Null element data passed to constructor.");
        }
        Attributes atts = element.getAtts();
        int attsCount = atts.getLength();
        if (element.getLocalName().equalsIgnoreCase("rss") == false) {
            throw new MalformedFeedElementException("Element name does not match.");
        }
        int versionIndex;
        for (versionIndex = 0; versionIndex < attsCount; versionIndex++) {
            if (atts.getLocalName(versionIndex).equalsIgnoreCase("version") == false) {
                throw new MalformedFeedElementException("Element missing mandatory attribute.");
            } else {
                break;
            }
        }
        version = atts.getValue(versionIndex);
        int childCount = element.getChildCount();
        if (childCount == 0) {
            throw new MalformedFeedElementException("Missing channel element.");
        } else if (childCount > 1) {
            throw new MalformedFeedElementException("Too many child elements for root.");
        }
        channel = new Channel(element.getChildAt(0));
    }

    public String getVersion() {
        return version;
    }

    public Channel getChannel() {
        return channel;
    }

    @Override
    public String toString() {
        String s = "Rss feed version " + version + '\n';
        s += channel.toString();
        return s;
    }
}
