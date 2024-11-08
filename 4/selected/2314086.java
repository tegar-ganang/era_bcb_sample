package net.narusas.cafelibrary.bookfactories;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;
import net.narusas.cafelibrary.Book;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class NaverFetcher extends ChannelFetcher {

    public NaverFetcher() {
        super("41fbf00c99adcd9293d95acb9383c28b");
    }

    public NaverFetcher(String apikey) {
        super(apikey);
    }

    public FetchResult queryByISBN(String isbn, int pageNo) throws UnsupportedEncodingException, SAXException, IOException {
        return query(isbn, pageNo);
    }

    protected HttpMethod createMethod(String isbn, int pageCount) throws UnsupportedEncodingException {
        HttpMethod method = new GetMethod("http://openapi.naver.com/search?" + "key=" + apikey + "&target=book_adv" + "&display=50" + "&start=1" + "&d_isbn=" + URLEncoder.encode(isbn, "utf-8") + "&query=" + URLEncoder.encode(isbn, "utf-8"));
        return method;
    }

    protected Node getChannelNode(Document doc) {
        NodeList childs = doc.getChildNodes();
        Node channel = childs.item(0).getChildNodes().item(0);
        return channel;
    }

    Date toDate(String removeTag) {
        return null;
    }
}
