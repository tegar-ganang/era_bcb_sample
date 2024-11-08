package com.goodcodeisbeautiful.opensearch.osrss10;

import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import com.goodcodeisbeautiful.archtea.util.ArchteaUtil;
import com.goodcodeisbeautiful.archtea.util.XMLUtil;
import com.goodcodeisbeautiful.opensearch.OpenSearchException;
import com.goodcodeisbeautiful.util.ReplaceCharReader;

/**
 * @author hata
 *
 */
public class DefaultOpenSearchRss implements OpenSearchRss10 {

    /** a log instance. */
    private static final Log log = LogFactory.getLog(DefaultOpenSearchRss.class);

    private static final String RET = "\n";

    private OpenSearchRssChannel m_channel = null;

    private String m_result = null;

    public DefaultOpenSearchRss() {
    }

    public DefaultOpenSearchRss(byte[] bytes) throws OpenSearchException {
        String encoding = ArchteaUtil.getXmlCharEncoding(bytes);
        Reader r = null;
        try {
            r = new StringReader(new String(bytes, encoding));
        } catch (UnsupportedEncodingException e) {
            r = new StringReader(new String(bytes));
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new ReplaceCharReader(r, ReplaceCharReader.XML_REMOVE_CTRL_CODES)));
            NodeList rssTagList = document.getElementsByTagName("rss");
            if (rssTagList.getLength() > 1) {
                throw new OpenSearchException("There are 2 or more rss tags.");
            }
            Node rssNode = rssTagList.item(0);
            if (rssNode.getAttributes().getNamedItem("version") != null && !"2.0".equals(rssNode.getAttributes().getNamedItem("version").getNodeValue())) {
                throw new OpenSearchException("The rss version is not 2.0. The version is " + rssNode.getAttributes().getNamedItem("version"));
            }
            DefaultOpenSearchRssChannel channel = new DefaultOpenSearchRssChannel();
            NodeList itemTagList = document.getElementsByTagName("item");
            for (int i = 0; i < itemTagList.getLength(); i++) {
                Node node = itemTagList.item(i);
                NodeList children = node.getChildNodes();
                String title = null;
                URL link = null;
                String desc = null;
                for (int k = 0; k < children.getLength(); k++) {
                    Node itemChild = children.item(k);
                    if ("title".equals(itemChild.getNodeName()) && itemChild.getChildNodes().getLength() > 0) {
                        title = XMLUtil.removeTags(itemChild.getFirstChild().getNodeValue());
                    } else if ("link".equals(itemChild.getNodeName()) && itemChild.getChildNodes().getLength() > 0) {
                        String linkText = null;
                        try {
                            linkText = itemChild.getFirstChild().getNodeValue();
                            link = new URL(XMLUtil.removeTags(linkText));
                        } catch (MalformedURLException e) {
                            log.warn(ArchteaUtil.getString("opensearch.osrss10.WrongLinkURL", new Object[] { linkText }), e);
                        }
                    } else if ("description".equals(itemChild.getNodeName()) && itemChild.getChildNodes().getLength() > 0) {
                        desc = itemChild.getFirstChild().getNodeValue();
                    }
                }
                channel.addItem(new DefaultOpenSearchRssItem(title, link, desc));
            }
            NodeList channels = document.getElementsByTagName("channel");
            if (channels.getLength() != 1) throw new OpenSearchException("There are some channel or zero.");
            Node channelNode = channels.item(0);
            NodeList children = channelNode.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node channelElement = children.item(i);
                if ("title".equals(channelElement.getNodeName())) {
                    channel.setTitle(XMLUtil.removeTags(channelElement.getFirstChild().getNodeValue()));
                }
                if ("link".equals(channelElement.getNodeName())) {
                    try {
                        channel.setLink(new URL(XMLUtil.removeTags(channelElement.getFirstChild().getNodeValue())));
                    } catch (MalformedURLException e) {
                        throw new OpenSearchException("Required tag cannot be pased. The tag is url.");
                    }
                }
                if ("description".equals(channelElement.getNodeName())) {
                    channel.setDescription(channelElement.getFirstChild().getNodeValue());
                }
                if ("language".equals(channelElement.getNodeName())) {
                    channel.setLanguage(channelElement.getFirstChild().getNodeValue());
                }
                if ("copyright".equals(channelElement.getNodeName())) {
                    channel.setCopyright(channelElement.getFirstChild().getNodeValue());
                }
                if ("openSearch:totalResults".equals(channelElement.getNodeName())) {
                    try {
                        if (channel != null && channelElement.getFirstChild() != null && channelElement.getFirstChild().getNodeValue() != null) channel.setTotalResult(Long.parseLong(channelElement.getFirstChild().getNodeValue())); else channel.setTotalResult(itemTagList.getLength());
                    } catch (NumberFormatException e) {
                        channel.setTotalResult(itemTagList.getLength());
                    }
                }
                if ("openSearch:startIndex".equals(channelElement.getNodeName())) {
                    try {
                        if (channel != null && channelElement.getFirstChild() != null && channelElement.getFirstChild().getNodeValue() != null) channel.setStartIndex(Long.parseLong(channelElement.getFirstChild().getNodeValue())); else channel.setStartIndex(DefaultOpenSearchRssChannel.DEFAULT_START_INDEX);
                    } catch (NumberFormatException e) {
                        channel.setStartIndex(DefaultOpenSearchRssChannel.DEFAULT_START_INDEX);
                    }
                }
                if ("openSearch:itemsPerPage".equals(channelElement.getNodeName())) {
                    try {
                        if (channel != null && channelElement.getFirstChild() != null && channelElement.getFirstChild().getNodeValue() != null) channel.setItemsPerPage(Long.parseLong(channelElement.getFirstChild().getNodeValue())); else channel.setItemsPerPage(DefaultOpenSearchRssChannel.DEFAULT_ITEMS_PER_PAGE);
                    } catch (NumberFormatException e) {
                        channel.setItemsPerPage(DefaultOpenSearchRssChannel.DEFAULT_ITEMS_PER_PAGE);
                    }
                }
            }
            if (channel.getTotalResult() == 0) channel.setTotalResult(itemTagList.getLength());
            if (channel.getStartIndex() == 0) channel.setStartIndex(DefaultOpenSearchRssChannel.DEFAULT_START_INDEX);
            if (channel.getItemsPerPage() == 0) channel.setItemsPerPage(DefaultOpenSearchRssChannel.DEFAULT_ITEMS_PER_PAGE);
            if (channel.getTitle() == null) throw new OpenSearchException("Required tag rss/channel/title is null.");
            if (channel.getLink() == null) throw new OpenSearchException("Required tag rss/channel/link is null.");
            if (channel.getDescription() == null) throw new OpenSearchException("Required tag rss/channel/description is null.");
            this.setChannel(channel);
        } catch (Exception e) {
            throw new OpenSearchException(e);
        }
    }

    /**
     * 
     */
    public DefaultOpenSearchRss(OpenSearchRssChannel channel) {
        m_channel = channel;
    }

    public OpenSearchRssChannel getChannel() {
        return m_channel;
    }

    public void setChannel(OpenSearchRssChannel channel) {
        m_channel = channel;
    }

    /**
     * get an open search rss namespace.
     * http://a9.com/-/spec/opensearchrss/1.0/ is returned.
     * @return open search rss namespace.
     */
    public String getXmlns() {
        return OpenSearchRss10.XMLNS_OSRSS10;
    }

    public Reader getReader() throws OpenSearchException {
        if (m_result == null) {
            try {
                buildOpenSearchRss10();
            } catch (Exception e) {
                e.printStackTrace();
                throw new OpenSearchRssException(e);
            }
            if (m_result == null) throw new OpenSearchRssException("Cannot get InputStream.");
        }
        return new StringReader(m_result);
    }

    private static String getLanguage(Locale locale) {
        if (locale == null) return null;
        String lang = locale.getLanguage();
        String country = locale.getCountry();
        return (lang + "-" + country).toLowerCase();
    }

    private void buildOpenSearchRss10() throws OpenSearchRssException {
        StringBuffer buff = new StringBuffer();
        OpenSearchRssChannel osrChannel = getChannel();
        if (osrChannel == null) return;
        validateSomeValues(osrChannel);
        buff.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + XMLUtil.RET);
        buff.append("<rss version=\"2.0\"  xmlns:openSearch=\"http://a9.com/-/spec/opensearchrss/1.0/\">" + RET);
        buff.append("	<channel>" + RET);
        buff.append(XMLUtil.buildXMLNodeText(8, "title", osrChannel.getTitle()));
        buff.append(XMLUtil.buildXMLNodeText(8, "link", osrChannel.getLink().toExternalForm()));
        buff.append(XMLUtil.buildXMLNodeText(8, "description", osrChannel.getDescription()));
        if (osrChannel.getLanguage() != null) buff.append(XMLUtil.buildXMLNodeText(8, "language", getLanguage(osrChannel.getLanguage())));
        if (osrChannel.getCopyright() != null) buff.append(XMLUtil.buildXMLNodeText(8, "copyright", osrChannel.getCopyright()));
        buff.append(XMLUtil.buildXMLNodeText(8, "openSearch:totalResults", osrChannel.getTotalResult()));
        buff.append(XMLUtil.buildXMLNodeText(8, "openSearch:startIndex", osrChannel.getStartIndex()));
        buff.append(XMLUtil.buildXMLNodeText(8, "openSearch:itemsPerPage", osrChannel.getItemsPerPage()));
        List items = osrChannel.items();
        Iterator it = items.iterator();
        while (it.hasNext()) {
            OpenSearchRssItem item = (OpenSearchRssItem) it.next();
            buff.append("		<item>" + RET);
            buff.append(XMLUtil.buildXMLNodeText(12, "title", item.getTitle()));
            buff.append(XMLUtil.buildXMLNodeText(12, "link", item.getLink().toExternalForm()));
            buff.append(XMLUtil.buildXMLNodeText(12, "description", item.getDescription()));
            buff.append("		</item>" + RET);
        }
        buff.append("	</channel>" + RET);
        buff.append("</rss>" + RET);
        m_result = new String(buff);
    }

    private void validateSomeValues(OpenSearchRssChannel channel) throws OpenSearchRssException {
        if (channel.getTitle() == null) throw new OpenSearchRssException();
        if (channel.getLink() == null) throw new OpenSearchRssException();
        if (channel.getDescription() == null) throw new OpenSearchRssException();
    }
}
