package com.goodcodeisbeautiful.archtea.search.os;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.goodcodeisbeautiful.archtea.search.ArchteaQuery;
import com.goodcodeisbeautiful.archtea.search.ArchteaSearchException;
import com.goodcodeisbeautiful.archtea.search.ArchteaSearcher;
import com.goodcodeisbeautiful.archtea.search.DefaultSearchResult;
import com.goodcodeisbeautiful.archtea.search.OpenSearchArchteaSearcher;
import com.goodcodeisbeautiful.archtea.search.SearchResult;
import com.goodcodeisbeautiful.archtea.util.ArchteaUtil;
import com.goodcodeisbeautiful.opensearch.OpenSearchDescription;
import com.goodcodeisbeautiful.opensearch.OpenSearchException;
import com.goodcodeisbeautiful.opensearch.OpenSearchParam;
import com.goodcodeisbeautiful.opensearch.OpenSearchUrl;
import com.goodcodeisbeautiful.opensearch.osd10.OpenSearchDescription10;
import com.goodcodeisbeautiful.opensearch.osd11.OpenSearch11CommonModule;
import com.goodcodeisbeautiful.opensearch.osd11.OpenSearchDescription11;
import com.goodcodeisbeautiful.syndic8.atom10.Atom10;
import com.goodcodeisbeautiful.syndic8.atom10.Atom10Entry;
import com.goodcodeisbeautiful.syndic8.atom10.Atom10Exception;
import com.goodcodeisbeautiful.syndic8.atom10.Atom10Feed;
import com.goodcodeisbeautiful.syndic8.rss20.Rss20;
import com.goodcodeisbeautiful.syndic8.rss20.Rss20Channel;
import com.goodcodeisbeautiful.syndic8.rss20.Rss20Exception;
import com.goodcodeisbeautiful.syndic8.rss20.Rss20Factory;
import com.goodcodeisbeautiful.syndic8.rss20.Rss20Item;
import com.goodcodeisbeautiful.syndic8.rss20.Rss20Module;

/**
 * @author hata
 *
 */
public class OpenSearch11ArchteaSearcher extends AbstractOpenSearchArchteaSearcher implements ArchteaSearcher {

    /** log */
    private static final Log log = LogFactory.getLog(OpenSearchArchteaSearcher.class);

    /** one of url types and this is rss type. */
    private static final String URL_TYPE_RSS = "application/rss+xml";

    /** one of url types and this is atom type. */
    private static final String URL_TYPE_ATOM = "application/atom+xml";

    private static final Pattern REPLACE_REGEX = Pattern.compile("\\{([^\\}])*(\\??)\\}");

    private volatile Collection<OpenSearch11QuerySyntax> m_querySyntaxList;

    /**
	 * 
	 */
    public OpenSearch11ArchteaSearcher() {
    }

    public SearchResult search(ArchteaQuery query) throws ArchteaSearchException {
        int status = -1;
        HttpClient client = null;
        byte[] responseBodyBytes = null;
        try {
            client = new HttpClient();
            client.setTimeout(getHttpClientTimeout());
            client.setConnectionTimeout(getHttpClientTimeout());
            OpenSearchDescription11 desc = (OpenSearchDescription11) getOpenSearchConfig().getOpenSearchDescription();
            OpenSearchUrl[] urls = desc.getOpenSearchUrls();
            OpenSearchUrl requestUrl = null;
            for (int i = 0; i < urls.length; i++) {
                if (urls[i] != null && (URL_TYPE_RSS.equals(urls[i].getType()) || URL_TYPE_ATOM.equals(urls[i].getType()))) {
                    requestUrl = urls[i];
                    break;
                }
            }
            if (requestUrl == null) throw new OpenSearchException("There is not any url to be able to use this application.");
            long beginSearchTime = System.currentTimeMillis();
            HttpMethod requestMethod = constructQueryMethod(requestUrl, query);
            status = client.executeMethod(requestMethod);
            long endSearchTime = System.currentTimeMillis();
            if (status != 200) {
                throw new ArchteaSearchException("archtea.search.OpenSearchArchteaSearcher.ResponseIsNot200", new Object[] { getOpenSearchConfig().getName(), Integer.valueOf(status), requestMethod.getResponseBodyAsString() });
            }
            responseBodyBytes = ArchteaUtil.readBytes(requestMethod.getResponseBodyAsStream());
            if (URL_TYPE_RSS.equals(requestUrl.getType())) return getRss20SearchResult(getOpenSearchConfig().getOpenSearchDescription(), responseBodyBytes, endSearchTime - beginSearchTime); else if (URL_TYPE_ATOM.equals(requestUrl.getType())) return getAtom10SearchResult(getOpenSearchConfig().getOpenSearchDescription(), responseBodyBytes, endSearchTime - beginSearchTime); else throw new ArchteaSearchException("No response type found. requestUrl is " + requestUrl);
        } catch (Exception e) {
            log.error(ArchteaUtil.getString("archtea.search.FailToCallOpenSearch", new Object[] { "" + status, "" + (responseBodyBytes != null ? responseBodyBytes.length : -1), (responseBodyBytes != null ? new String(responseBodyBytes) : "no data") }), e);
            throw new ArchteaSearchException(e);
        }
    }

    /**
	 * see  http://opensearch.a9.com/spec/1.1/querysyntax/
	 */
    private HttpMethod constructQueryMethod(OpenSearchUrl requestUrl, ArchteaQuery query) throws IOException, OpenSearchException {
        if (OpenSearchUrl.METHOD_POST.equals(requestUrl.getMethod())) {
            PostMethod method = new PostMethod(requestUrl.getTemplate());
            OpenSearchParam[] params = requestUrl.getParams();
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    method.addParameter(params[i].getName(), toRequestParameter(params[i].getValue(), query));
                }
            }
            return method;
        } else {
            String queryUrl = requestUrl.getTemplate();
            return new GetMethod(toRequestParameter(queryUrl, query));
        }
    }

    /**
     * Check openSearchParameters and then replace it using query.
     * @param openSearchParameters is a request parameter to send
     * request to a search provider. This value is like {startIndex}
     * {startIndex?}. And this value can acceptable like 
     * http://hogehoge/search?s={searchTerms}&c={count?}.
     * @param query is a query information from client.
     */
    private String toRequestParameter(String openSearchParameters, ArchteaQuery query) {
        Matcher matcher = REPLACE_REGEX.matcher(openSearchParameters);
        StringBuffer buff = new StringBuffer();
        while (matcher.find()) {
            String matchText = matcher.group();
            String replaceText = null;
            Iterator it = getQuerySyntaxList().iterator();
            while (it.hasNext()) {
                OpenSearch11QuerySyntax syntax = (OpenSearch11QuerySyntax) it.next();
                if (syntax.isSupported(matchText)) {
                    try {
                        replaceText = syntax.getValue((OpenSearchDescription11) getOpenSearchConfig().getOpenSearchDescription(), matchText, query);
                    } catch (Exception e) {
                        log.error("Error happened while applying query syntaxt using " + syntax, e);
                    }
                }
            }
            if (replaceText == null) {
                log.error("No replaced text found. The matchText is " + matchText);
                replaceText = "";
            }
            matcher.appendReplacement(buff, replaceText);
        }
        matcher.appendTail(buff);
        return new String(buff);
    }

    /**
     * Get SearchResult from Open Search Rss XML bytes and search time.
     * 
     * @param desc is an open search description instance.
     * @param responseBodyBytes is the xml text of Open Search RSS.
     * @param searchTimeMills is a search time (millseconds).
     * @return SearchResult instance.
     * @exception OpenSearchException is thrown if some error occured.
     */
    private SearchResult getRss20SearchResult(OpenSearchDescription desc, byte[] responseBodyBytes, long searchTimeMills) throws ArchteaSearchException {
        String searchName = getConfig().getName();
        if (desc instanceof OpenSearchDescription11) {
            searchName = ((OpenSearchDescription10) desc).getShortName();
        }
        Rss20Factory factory = new Rss20Factory();
        InputStream in = null;
        try {
            in = new ByteArrayInputStream(responseBodyBytes);
            OpenSearch11CommonModule opensearch = new OpenSearch11CommonModule();
            Rss20 rss20 = factory.newInstance(in, new Rss20Module[] { opensearch });
            Rss20Channel channel = rss20.getChannel();
            Rss20Item[] items = channel.getItems();
            if (channel.getTitle() != null) searchName = channel.getTitle();
            int hitCount = -1 < opensearch.getTotalResults() ? opensearch.getTotalResults() : items.length;
            int itemsPerPage = 0 < opensearch.getItemsPerPage() ? opensearch.getItemsPerPage() : items.length;
            if (itemsPerPage < OpenSearch11CommonModule.DEFAULT_ITEMS_PER_PAGE) itemsPerPage = OpenSearch11CommonModule.DEFAULT_ITEMS_PER_PAGE;
            int startIndex = 0 < opensearch.getStartIndex() ? opensearch.getStartIndex() : OpenSearch11CommonModule.DEFAULT_START_INDEX;
            DefaultSearchResult searchResult = new DefaultSearchResult(searchName, searchTimeMills, hitCount, itemsPerPage);
            for (int i = 0; i < items.length; i++) {
                searchResult.addFoundItem(new Rss20FoundItem(startIndex + i, items[i]));
            }
            return searchResult;
        } catch (Rss20Exception e) {
            log.error("Error happened while parsing response message.", e);
            throw new ArchteaSearchException(e);
        }
    }

    private SearchResult getAtom10SearchResult(OpenSearchDescription desc, byte[] responseBodyBytes, long searchTimeMills) throws ArchteaSearchException {
        String searchName = getConfig().getName();
        if (desc instanceof OpenSearchDescription11) {
            searchName = ((OpenSearchDescription10) desc).getShortName();
        }
        InputStream in = null;
        try {
            in = new ByteArrayInputStream(responseBodyBytes);
            OpenSearch11CommonModule opensearch = new OpenSearch11CommonModule();
            Atom10 atom10 = new Atom10();
            atom10.addModule(opensearch);
            Atom10Feed feed = atom10.getFeed(in);
            if (feed.getTitle() != null && feed.getTitle().getText() != null) searchName = feed.getTitle().getText();
            Atom10Entry[] items = feed.getEntries();
            int hitCount = -1 < opensearch.getTotalResults() ? opensearch.getTotalResults() : items.length;
            int itemsPerPage = 0 < opensearch.getItemsPerPage() ? opensearch.getItemsPerPage() : items.length;
            if (itemsPerPage < OpenSearch11CommonModule.DEFAULT_ITEMS_PER_PAGE) itemsPerPage = OpenSearch11CommonModule.DEFAULT_ITEMS_PER_PAGE;
            int startIndex = 0 < opensearch.getStartIndex() ? opensearch.getStartIndex() : OpenSearch11CommonModule.DEFAULT_START_INDEX;
            DefaultSearchResult searchResult = new DefaultSearchResult(searchName, searchTimeMills, hitCount, itemsPerPage);
            for (int i = 0; i < items.length; i++) {
                searchResult.addFoundItem(new Atom10FoundItem(startIndex + i, items[i]));
            }
            return searchResult;
        } catch (Atom10Exception e) {
            log.error("Error happened while parsing response message.", e);
            throw new ArchteaSearchException(e);
        }
    }

    /**
     * Get query syntax list.
     * The list contains {@see OpenSearchQuerySyntax} derived classes.
     * @see #m_querySyntaxList
     * @return OpenSearchQuerySyntax derived classes.
     */
    private Collection getQuerySyntaxList() {
        if (m_querySyntaxList == null) {
            synchronized (this) {
                if (m_querySyntaxList == null) {
                    m_querySyntaxList = new ArrayList<OpenSearch11QuerySyntax>();
                    m_querySyntaxList.add(new OpenSearch11CoreQuerySyntax());
                    m_querySyntaxList.add(new OmitssionQuerySyntax());
                }
            }
        }
        return m_querySyntaxList;
    }
}
