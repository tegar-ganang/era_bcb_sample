package net.narusas.cafelibrary.bookfactories;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import net.narusas.cafelibrary.Book;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public abstract class ChannelFetcher {

    protected String apikey;

    protected DocumentBuilder builder;

    public ChannelFetcher(String apikey) {
        this.apikey = apikey;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
        }
    }

    public FetchResult query(String query, int pageNo) throws UnsupportedEncodingException, SAXException, IOException {
        if (pageNo <= 0) {
            pageNo = 1;
        }
        List<Book> res = new LinkedList<Book>();
        Node channel = null;
        String xml = fetchXML(query, pageNo);
        Document doc = createDOM(xml);
        channel = getChannelNode(doc);
        NodeList items = channel.getChildNodes();
        res.addAll(parseItems(items));
        FetchResult fetchResult = new FetchResult(res, query);
        parseFetchResult(channel, fetchResult);
        return fetchResult;
    }

    private void parseFetchResult(Node channel, FetchResult res) {
        NodeList childs = channel.getChildNodes();
        for (int i = 0; i < childs.getLength(); i++) {
            Node n = childs.item(i);
            String nodeName = n.getNodeName();
            if ("totalCount".equals(nodeName)) {
                res.setTotalCount(Integer.parseInt(getNodeValue(n)));
            }
            if ("pageno".equals(nodeName)) {
                res.setCurrentPage(Integer.parseInt(getNodeValue(n)));
            }
            if ("result".equals(nodeName)) {
                res.setResultCount(Integer.parseInt(getNodeValue(n)));
            }
        }
    }

    private Book convertItemToBook(Node item) {
        Book book = new Book();
        NodeList childs = item.getChildNodes();
        for (int i = 0; i < childs.getLength(); i++) {
            Node n = childs.item(i);
            String nodeName = n.getNodeName();
            if ("title".equals(nodeName)) {
                book.setTitle(removeTag(getNodeValue(n)));
            } else if ("author".equals(nodeName)) {
                book.setAuthor(removeTag(getNodeValue(n)));
            } else if ("cover_s_url".equals(nodeName)) {
                book.setCoverSmallUrl(removeTag(getNodeValue(n)));
            } else if ("cover_l_url".equals(nodeName)) {
                book.setCoverLargeUrl(removeTag(getNodeValue(n)));
            } else if ("description".equals(nodeName)) {
                book.setDescription(removeTag(getNodeValue(n)));
            } else if ("translator".equals(nodeName)) {
                book.setTranslator(removeTag(getNodeValue(n)));
            } else if ("pub_nm".equals(nodeName)) {
                book.setPublisher(removeTag(getNodeValue(n)));
            } else if ("pub_date".equals(nodeName)) {
                book.setPublishDate(toDate(removeTag(getNodeValue(n))));
            } else if ("category".equals(nodeName)) {
                book.setCategory(removeTag(getNodeValue(n)));
            } else if ("isbn".equals(nodeName)) {
                book.setIsbn(removeTag(getNodeValue(n)));
            } else if ("sale_price".equals(nodeName)) {
                book.setSalePrice(removeTag(getNodeValue(n)));
            } else if ("list_price".equals(nodeName)) {
                book.setOriginalPrice(removeTag(getNodeValue(n)));
            } else if ("image".equals(nodeName)) {
                book.setCoverSmallUrl(removeTag(getNodeValue(n)));
                book.setCoverLargeUrl(removeTag(getNodeValue(n)));
            }
        }
        downCover(book);
        return book;
    }

    private void downCover(Book book) {
        ImageDownloader imgFetcher = new ImageDownloader();
        imgFetcher.download(book);
    }

    abstract Date toDate(String removeTag);

    private Document createDOM(String xml) throws SAXException, IOException, UnsupportedEncodingException {
        return builder.parse(new ByteArrayInputStream(xml.getBytes("utf-8")));
    }

    private String fetchXML(String query, int pageCount) throws UnsupportedEncodingException, IOException, HttpException {
        HttpClient client = new HttpClient();
        HttpMethod method = createMethod(query, pageCount);
        client.executeMethod(method);
        byte[] body = method.getResponseBody();
        String xml = new String(body, "utf-8");
        return xml;
    }

    private String getNodeValue(Node n) {
        return n.getChildNodes().item(0).getNodeValue();
    }

    private List<Book> parseItems(NodeList childs) {
        List<Book> res = new LinkedList<Book>();
        for (int i = 0; i < childs.getLength(); i++) {
            if ("item".equals(childs.item(i).getNodeName())) {
                Book book = convertItemToBook(childs.item(i));
                res.add(book);
            }
        }
        return res;
    }

    private String removeTag(String nodeValue) {
        return nodeValue.replaceAll("<[^>]+>", "");
    }

    protected abstract HttpMethod createMethod(String query, int pageCount) throws UnsupportedEncodingException;

    protected abstract Node getChannelNode(Document doc);
}
