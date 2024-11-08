package net.narusas.cafelibrary.bookfactories;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DaumFetcher extends ChannelFetcher {

    public DaumFetcher(String apikey) {
        super("13672e9ee069b904f2e229f5b6e7d2b362d4306b");
    }

    public DaumFetcher() {
        super("13672e9ee069b904f2e229f5b6e7d2b362d4306b");
    }

    protected HttpMethod createMethod(String query, int pageCount) throws UnsupportedEncodingException {
        HttpMethod method = new GetMethod("http://apis.daum.net/search/book?" + "result=19" + "&pageno=" + pageCount + "&apikey=" + apikey + "&q=" + URLEncoder.encode(query, "euc-kr"));
        return method;
    }

    protected Node getChannelNode(Document doc) {
        NodeList childs = doc.getChildNodes();
        Node channel = childs.item(0);
        return channel;
    }

    /**
	 * ���������� Thu, 20 Oct 05 00:00:00 +0900 ���� ������� ���ڸ� ��ȯ�Ѵ�.
	 */
    Date toDate(String src) {
        if (src == null || "".equals(src)) {
            return null;
        }
        String[] tokens = src.split(" ");
        int date = Integer.parseInt(tokens[1]);
        int month = parseMonth(tokens[2]);
        int year = parseYear(tokens[3]);
        return new Date(year - 1900, month, date);
    }

    private int parseMonth(String src) {
        src = src.toLowerCase();
        String[] MONTH = new String[] { "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec" };
        for (int i = 0; i < 12; i++) {
            if (MONTH[i].equals(src)) {
                return i;
            }
        }
        return 0;
    }

    private int parseYear(String src) {
        if (src.startsWith("0") && src.length() == 2) {
            return Integer.parseInt("20" + src);
        }
        if (src.startsWith("9") && src.length() == 2) {
            return Integer.parseInt("19" + src);
        }
        return Integer.parseInt(src);
    }
}
