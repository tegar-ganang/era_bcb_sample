package torrentjedi.data;

import java.util.ArrayList;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import torrentjedi.core.RSSChannel;
import torrentjedi.core.RSSItem;

/**
 * @author Alper Kokmen
 *
 */
public class RSSHandler implements ContentHandler {

    private static final String TAG_RSS = "rss";

    private static final String TAG_CHANNEL = "channel";

    private static final String TAG_ITEM = "item";

    private static final String TAG_TITLE = "title";

    private static final String TAG_LINK = "link";

    private static final String TAG_DESCRIPTION = "description";

    private static final String TAG_LANGUAGE = "language";

    private static final String TAG_PUB_DATE = "pubDate";

    private static final String TAG_LAST_BUILD_DATE = "lastBuildDate";

    private static final String TAG_GENERATOR = "generator";

    private static final String TAG_MANAGING_EDITOR = "managingEditor";

    private static final String TAG_WEB_MASTER = "webMaster";

    private static final String TAG_CATEGORY = "category";

    private RSSChannel channel;

    private RSSItem item;

    private Integer level;

    private String data;

    private Boolean channelTag;

    private Boolean itemTag;

    public RSSHandler(RSSChannel channel) {
        super();
        this.channel = channel;
    }

    /**
	 * @return the channel
	 */
    public RSSChannel getChannel() {
        return channel;
    }

    /**
	 * @param channel the channel to set
	 */
    public void setChannel(RSSChannel channel) {
        this.channel = channel;
    }

    /**
	 * @return the level
	 */
    public Integer getLevel() {
        return level;
    }

    /**
	 * @param level the level to set
	 */
    public void setLevel(Integer level) {
        this.level = level;
    }

    /**
	 * @return the data
	 */
    public String getData() {
        return data;
    }

    /**
	 * @param data the data to set
	 */
    public void setData(String data) {
        this.data = data;
    }

    /**
	 * @return the itemTag
	 */
    public Boolean getItemTag() {
        return itemTag;
    }

    /**
	 * @param itemTag the itemTag to set
	 */
    public void setItemTag(Boolean itemTag) {
        this.itemTag = itemTag;
    }

    /**
	 * @return the channelTag
	 */
    public Boolean getChannelTag() {
        return channelTag;
    }

    /**
	 * @param channelTag the channelTag to set
	 */
    public void setChannelTag(Boolean channelTag) {
        this.channelTag = channelTag;
    }

    /**
	 * @return the item
	 */
    public RSSItem getItem() {
        return item;
    }

    /**
	 * @param item the item to set
	 */
    public void setItem(RSSItem item) {
        this.item = item;
    }

    @Override
    public void characters(char[] arg0, int arg1, int arg2) throws SAXException {
        if (arg2 == 0) {
            System.out.println("WARNING@TorrentJEDI:RSSHandler characters length = 0");
        } else {
            String value = new String(arg0, arg1, arg2);
            if (!value.trim().isEmpty()) {
                setData(getData() + value);
            }
        }
    }

    @Override
    public void endDocument() throws SAXException {
        setData("");
        if (getLevel() != 0) {
            System.out.println("WARNING:TorrentJEDI:RSSHandler endDocument [corrupted rss]");
        }
    }

    @Override
    public void endElement(String arg0, String arg1, String arg2) throws SAXException {
        if (arg1.equals(TAG_CATEGORY)) {
            item.setCategory(getData());
        } else if (arg1.equals(TAG_DESCRIPTION)) {
            if (getChannelTag()) {
                channel.setDescription(getData());
            } else if (getItemTag()) {
                item.setDescription(getData());
            }
        } else if (arg1.equals(TAG_GENERATOR)) {
            channel.setGenerator(getData());
        } else if (arg1.equals(TAG_LANGUAGE)) {
            channel.setLanguage(getData());
        } else if (arg1.equals(TAG_LAST_BUILD_DATE)) {
            channel.setLastBuildDate(getData());
        } else if (arg1.equals(TAG_LINK)) {
            if (getChannelTag()) {
                channel.setLink(getData());
            } else if (getItemTag()) {
                item.setLink(getData());
            }
        } else if (arg1.equals(TAG_MANAGING_EDITOR)) {
            channel.setManagingEditor(getData());
        } else if (arg1.equals(TAG_PUB_DATE)) {
            if (getChannelTag()) {
                channel.setPubDate(getData());
            } else if (getItemTag()) {
                item.setPubDate(getData());
            }
        } else if (arg1.equals(TAG_TITLE)) {
            if (getChannelTag()) {
                channel.setTitle(getData());
            } else if (getItemTag()) {
                item.setTitle(getData());
            }
        } else if (arg1.equals(TAG_WEB_MASTER)) {
            channel.setWebMaster(getData());
        } else if (arg1.equals(TAG_RSS)) {
        } else if (arg1.equals(TAG_CHANNEL)) {
        } else if (arg1.equals(TAG_ITEM)) {
        } else {
            System.out.println("WARNING@TorrentJEDI:RSSHandler endElement [" + arg1 + "] [ignored tag]");
        }
        setData("");
        setLevel(getLevel() - 1);
    }

    @Override
    public void endPrefixMapping(String arg0) throws SAXException {
        System.out.println("WARNING@TorrentJEDI:RSSHandler endPrefixMapping [this shouldn't have happened]");
    }

    @Override
    public void ignorableWhitespace(char[] arg0, int arg1, int arg2) throws SAXException {
        System.out.println("WARNING@TorrentJEDI:RSSHandler ignorableWhitespace [this shouldn't have happened]");
    }

    @Override
    public void processingInstruction(String arg0, String arg1) throws SAXException {
        System.out.println("WARNING@TorrentJEDI:RSSHandler processingInstruction [this shouldn't have happened]");
    }

    @Override
    public void setDocumentLocator(Locator arg0) {
    }

    @Override
    public void skippedEntity(String arg0) throws SAXException {
        System.out.println("WARNING@TorrentJEDI:RSSHandler skippedEntity [this shouldn't have happened]");
    }

    @Override
    public void startDocument() throws SAXException {
        setData("");
        setLevel(0);
        channel.setItems(new ArrayList<RSSItem>());
    }

    @Override
    public void startElement(String arg0, String arg1, String arg2, Attributes arg3) throws SAXException {
        setData("");
        setLevel(getLevel() + 1);
        if (arg1.equals(TAG_CHANNEL)) {
            setChannelTag(true);
            setItemTag(false);
        } else if (arg1.equals(TAG_ITEM)) {
            setItem(new RSSItem());
            channel.getItems().add(item);
            setChannelTag(false);
            setItemTag(true);
        }
    }

    @Override
    public void startPrefixMapping(String arg0, String arg1) throws SAXException {
    }
}
