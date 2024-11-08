package com.spring.rssReader;

import com.spring.rssReader.jdbc.ChannelController;
import com.spring.rssReader.util.DateFinderPattern;
import com.spring.rssReader.util.UrlPatcherFilter;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.apache.xerces.xni.parser.XMLParserConfiguration;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.tidy.Tidy;
import org.cyberneko.html.filters.Writer;
import org.cyberneko.html.HTMLConfiguration;
import org.cyberneko.html.HTMLTagBalancer;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Item extends GeneralItem implements IItem {

    private static final Pattern blockquotePattern = Pattern.compile("<blockquote>(.|\\s)*?</blockquote>");

    private static final Pattern codePattern = Pattern.compile("<code>(.|\\s)*?</code>");

    private String comments;

    private String signature;

    private boolean articleRead;

    /**
     * fetched tells whether this item was manually fetched by the user. It is needed to determine the kind of delete
     * action on an item. Fetched items can be completely deleted whereas items inserted from a rss feed should be
     * nulled to avoid getting them again.
     */
    private boolean fetched;

    private Long channelID = null;

    private Channel channel;

    private List images;

    public Item() {
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
        this.setChannelID(channel.getId());
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public boolean isArticleRead() {
        return articleRead;
    }

    public void setArticleRead(boolean articleRead) {
        this.articleRead = articleRead;
    }

    public Long getChannelID() {
        return channelID;
    }

    public void setChannelID(Long channelID) {
        this.channelID = channelID;
    }

    public void remove() {
        this.setRemove(true);
        this.setDescription(null);
        this.setCategories(null);
    }

    public boolean isFetched() {
        return fetched;
    }

    public void setFetched(boolean fetched) {
        this.fetched = fetched;
    }

    public List getImages() {
        if (images == null) {
            images = new ArrayList();
        }
        return images;
    }

    public void setImages(List images) {
        this.images = images;
    }

    public void addImage(Image image) {
        image.setItem(this);
        this.getImages().add(image);
    }

    /**
     * This method will get the item as html page by getting the content from the items url. The title of this item
     * will become "fetched:" followed by the title.
     */
    public void getItemAsHTML() throws IOException {
        this.setDescription(this.load());
        if (this.getDescription() == null) {
            return;
        }
        processHtml(this.getDescription());
    }

    /**
     * This method will process the retrieved html output of an item/channel/website.
     */
    public void processHtml(String textToSave) {
        if (textToSave != null) {
            Matcher matcher = titleSplitter.matcher(textToSave);
            if (matcher.find()) {
                String title = textToSave.substring(matcher.start(), matcher.end());
                title = title.substring(title.indexOf(">") + 1, title.lastIndexOf("<"));
                this.setTitle(title.trim());
            }
            this.setDescription(handleHTML(textToSave));
            this.findDate(null);
        }
    }

    /**
     * This method will treat the given text as html. It will use tidy to clean up the (possible) html errors and remove
     * evertything before and after the body tags.
     *
     * @param description
     * @return
     */
    private String handleHTML(String description) {
        if (description == null) {
            return "";
        }
        String content = description;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            XMLDocumentFilter writer = new Writer(bos, "ISO-8859-1");
            XMLDocumentFilter urlPatcher = new UrlPatcherFilter(this.getUrl());
            XMLParserConfiguration parser = new HTMLConfiguration();
            parser.setFeature("http://cyberneko.org/html/features/balance-tags", false);
            XMLDocumentFilter[] filters = { urlPatcher, new HTMLTagBalancer(), writer };
            parser.setProperty("http://cyberneko.org/html/properties/filters", filters);
            parser.parse(new XMLInputSource("", "", "", new StringReader(description), ""));
            content = new String(bos.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
        description = removeTagsBeforeBody(content);
        return description;
    }

    /**
     * This method will remove everyhing before the first body tag.
     * @param content
     * @return
     */
    public static String removeTagsBeforeBody(String content) {
        String[] save = bodySplitter.split(content);
        if (save != null && save.length > 1) {
            StringBuffer sb = new StringBuffer();
            for (int i = 1; i < save.length; i++) {
                sb.append(save[i]);
            }
            content = sb.toString();
            if (content.lastIndexOf("</body>") > -1) {
                content = content.substring(0, content.lastIndexOf("</body>"));
            }
        }
        return content;
    }

    /**
     * This method will loop over the html nodes and replace the urls as found in a href tags and image tags by the
     * absolute urls.
     *
     * @param node
     * @return
     */
    private String parse(org.w3c.dom.Node node) {
        StringBuffer sb = new StringBuffer();
        if (node == null) {
            return null;
        }
        int type = node.getNodeType();
        if (type == org.w3c.dom.Node.ELEMENT_NODE) {
            if (node.getNodeName().equals("pre")) {
                int x = 3;
            }
            if (node.getNodeName().equalsIgnoreCase("img")) {
                NamedNodeMap attrs = node.getAttributes();
                for (int i = 0; i < attrs.getLength(); i++) {
                    if (attrs.item(i).getNodeName().equalsIgnoreCase("src")) {
                        try {
                            String url = createUrl(attrs.item(i).getNodeValue());
                            attrs.item(i).setNodeValue(url);
                        } catch (MalformedURLException e) {
                        }
                    }
                }
            } else if (node.getNodeName().equalsIgnoreCase("a")) {
                NamedNodeMap attrs = node.getAttributes();
                for (int i = 0; i < attrs.getLength(); i++) {
                    if (attrs.item(i).getNodeName().equalsIgnoreCase("href")) {
                        try {
                            String url = createUrl(attrs.item(i).getNodeValue());
                            attrs.item(i).setNodeValue(url);
                        } catch (MalformedURLException e) {
                        }
                    }
                }
            }
            sb.append("<").append(node.getNodeName());
            NamedNodeMap attrs = node.getAttributes();
            for (int i = 0; i < attrs.getLength(); i++) {
                sb.append(" ").append(attrs.item(i).getNodeName()).append("=\"").append(attrs.item(i).getNodeValue()).append("\"");
            }
            sb.append(">");
        }
        if (type != org.w3c.dom.Node.COMMENT_NODE) {
            sb.append(node.getNodeValue());
            NodeList children = node.getChildNodes();
            if (children != null) {
                int len = children.getLength();
                for (int i = 0; i < len; i++) {
                    sb.append(parse(children.item(i)));
                }
            }
            if (type == org.w3c.dom.Node.ELEMENT_NODE) {
                if (!node.getNodeName().equals("br")) {
                    sb.append("</").append(node.getNodeName()).append(">");
                }
            }
        }
        return sb.toString();
    }

    /**
     * due to errors (?) in the used xml parser, every xml entity inside a tag will be replaced by its xml equivalent.
     * This is very nice, but not inside pre tags,therefor we replace any < and > sign with its html equivalent, so when
     * reading <pre>&lt;html&gt;</pre> this will be saved with the & entities. If the pre starts with code or blockquote
     * tag, then this will be skipped.
     * @param sb
     * @return
     */
    private StringBuffer createEscapedPreTag(StringBuffer sb) {
        StringBuffer pre = new StringBuffer();
        Matcher blockquoteMatcher = blockquotePattern.matcher(sb);
        Matcher codeMatcher = codePattern.matcher(sb);
        int start = -1;
        int end = -1;
        int length = -1;
        if (blockquoteMatcher.find()) {
            start = blockquoteMatcher.start();
            end = blockquoteMatcher.end();
            length = "<blockquote>".length();
        }
        if (codeMatcher.find()) {
            if (start == -1) {
                start = codeMatcher.start();
                end = codeMatcher.end();
                length = "<code>".length();
            } else {
                start = Math.max(start, codeMatcher.start());
                end = Math.max(end, codeMatcher.end());
            }
        }
        if (start != -1 && end != -1) {
            pre.append(sb.substring(0, start + length));
            pre.append(sb.substring(start + length, end - (length + 1)).replaceAll("<", "&lt;").replaceAll(">", "&gt;"));
            pre.append(sb.substring(end - (length + 1)));
            return pre;
        } else {
            pre.append(sb.substring(0, 5));
            pre.append(sb.substring(5, sb.length() - 6).replaceAll("<", "&lt;").replaceAll(">", "&gt;"));
            pre.append("</pre>");
            return pre;
        }
    }

    private Image loadBinary(String url) {
        try {
            if (url != null) {
                HttpClient client = new HttpClient();
                client.setConnectionTimeout(HTTP_CONNECTION_TIMEOUT);
                client.setTimeout(HTTP_CONNECTION_TIMEOUT);
                GetMethod getMethod = new GetMethod(this.getUrl());
                client.executeMethod(getMethod);
                InputStream is = getMethod.getResponseBodyAsStream();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] bytes = new byte[1024];
                while (is.read(bytes, 0, 1024) != -1) {
                    bos.write(bytes);
                }
                is.close();
                Image image = new Image();
                image.setImage(bos.toByteArray());
                return image;
            }
        } catch (IOException e) {
        }
        return null;
    }

    private String createUrl(String urlImage) throws MalformedURLException {
        URL urlLink = new URL(this.getUrl());
        String strLink = null;
        try {
            urlLink = new URL(urlLink, urlImage);
            strLink = urlLink.toString();
        } catch (MalformedURLException e) {
        }
        return strLink;
    }

    /**
	 * This method will try to get the date, based on the description. If the searching of dates yields in a date that
	 * is older then 2 years, we declare it not valid.
	 */
    public void findDate(String stringDate) {
        Date now = new Date();
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.add(Calendar.YEAR, -2);
        Date postedDate = null;
        if (stringDate != null) {
            try {
                postedDate = ChannelController.rssFormatDateVersion2.parse(stringDate);
            } catch (ParseException e) {
                try {
                    postedDate = ChannelController.rssFormatDate.parse(stringDate);
                } catch (ParseException e1) {
                    try {
                        postedDate = ChannelController.rssFormatDateWithoutSeconds.parse(stringDate);
                    } catch (ParseException e2) {
                    }
                }
            }
        }
        if (postedDate == null || postedDate.getTime() >= now.getTime() || postedDate.getTime() < calendar.getTime().getTime()) {
            postedDate = getDate(stringDate, channel.getLanguage());
        }
        if (postedDate == null) {
            postedDate = new Date();
        } else {
            if (postedDate.getTime() >= now.getTime() || postedDate.getTime() < calendar.getTime().getTime()) {
                postedDate = now;
            }
        }
        this.setPostedDate(postedDate.getTime());
    }

    /**
	 * Try to find a date in the item (or channel or article).
	 * @param language
	 * @return
	 */
    private Date getDate(String parsedStringDate, String language) {
        Date postedDate = null;
        if (!"".equals(parsedStringDate)) {
            postedDate = DateFinderPattern.getInstance().findDateClosestFromStart(parsedStringDate, language);
        }
        if (postedDate == null) {
            if (this.getUrl() != null && !this.getUrl().equals("")) {
                postedDate = DateFinderPattern.getInstance().findDateByPreferance(this.getUrl(), language);
            }
        }
        if (postedDate == null) {
            if (this.getDescription() != null && !this.getDescription().equals("")) {
                postedDate = DateFinderPattern.getInstance().findDateClosestFromStart(this.getDescription(), language);
            }
        }
        return postedDate;
    }
}
